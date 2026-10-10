package com.example.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.data.model.BadgePosition
import com.example.data.model.BadgeStyle
import com.example.data.model.HeaderFormatMode
import com.example.data.model.OverlayConfig
import com.example.data.model.OverlayFont
import com.example.data.model.PushUpRep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

class VideoReelExporter(private val context: Context) {

    companion object {
        private const val TAG = "VideoReelExporter"
    }

    @Volatile
    private var isCancelled = false

    fun cancel() {
        isCancelled = true
    }

    suspend fun exportReel(
        videoUri: Uri?,
        videoFilePath: String?,
        overlayConfig: OverlayConfig,
        reps: List<PushUpRep>,
        outputWidth: Int = 720,
        outputHeight: Int = 1280,
        fps: Int = 30,
        onProgress: (Float, Int, Int) -> Unit
    ): Pair<String, Uri> = withContext(Dispatchers.IO) {
        isCancelled = false
        val startTime = System.currentTimeMillis()

        val retriever = MediaMetadataRetriever()
        var tempSourceFile: File? = null

        try {
            if (videoFilePath != null && File(videoFilePath).exists()) {
                retriever.setDataSource(videoFilePath)
            } else if (videoUri != null) {
                retriever.setDataSource(context, videoUri)
            } else {
                val sampleFile = SampleVideoGenerator.getOrCreateSampleVideo(context)
                retriever.setDataSource(sampleFile.absolutePath)
                tempSourceFile = sampleFile
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falling back to sample video dataSource", e)
            val sampleFile = SampleVideoGenerator.getOrCreateSampleVideo(context)
            retriever.setDataSource(sampleFile.absolutePath)
            tempSourceFile = sampleFile
        }

        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val durationMs = durationStr?.toLongOrNull() ?: 10000L
        val totalFrames = ((durationMs / 1000f) * fps).toInt().coerceIn(15, 900)

        val outputFile = File(context.cacheDir, "reel_export_${System.currentTimeMillis()}.mp4")
        if (outputFile.exists()) outputFile.delete()

        val bitRate = 4_500_000 // 4.5 Mbps crisp 720x1280
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, outputWidth, outputHeight).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var videoTrackIndex = -1
        var muxerStarted = false
        var samplesWritten = 0

        // Prepare frame canvas & buffers
        val renderBitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(renderBitmap)
        val yuvBuffer = ByteArray(outputWidth * outputHeight * 3 / 2)
        val pixelBuffer = IntArray(outputWidth * outputHeight)
        val bufferInfo = MediaCodec.BufferInfo()

        // Prepare Paints matching Preview styling
        val bgPaint = Paint().apply {
            color = overlayConfig.backgroundColor.toInt()
            style = Paint.Style.FILL
        }

        val scale = outputWidth / 360f

        val headerTypeface = resolveTypeface(overlayConfig.headerFont)
        val quoteTypeface = resolveTypeface(overlayConfig.quoteFont)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = overlayConfig.headerColor.toInt()
            textSize = overlayConfig.headerTextSizeSp * scale
            typeface = headerTypeface
            textAlign = Paint.Align.CENTER
            if (overlayConfig.textShadow) {
                setShadowLayer(10f, 2f, 4f, Color.argb(190, 0, 0, 0))
            }
        }

        val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = overlayConfig.quoteColor.toInt()
            textSize = overlayConfig.quoteTextSizeSp * scale
            typeface = quoteTypeface
            textAlign = Paint.Align.CENTER
            if (overlayConfig.textShadow) {
                setShadowLayer(8f, 2f, 3f, Color.argb(190, 0, 0, 0))
            }
        }

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val videoDestRect = Rect()
        var lastValidFrame: Bitmap? = null

        // Drain helper
        fun drainOutput(endOfStream: Boolean): Boolean {
            var eosReceived = false
            while (true) {
                val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 2500)
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (!muxerStarted) {
                        videoTrackIndex = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                } else if (outputBufferIndex >= 0) {
                    // Codec config data (SPS/PPS) is already in the format from INFO_OUTPUT_FORMAT_CHANGED.
                    // Release immediately to prevent "Already have codec specific data" errors.
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        encoder.releaseOutputBuffer(outputBufferIndex, false)
                        continue
                    }

                    val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                    if (outputBuffer != null && muxerStarted && bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                        samplesWritten++
                    }
                    encoder.releaseOutputBuffer(outputBufferIndex, false)

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        eosReceived = true
                        break
                    }
                } else {
                    break
                }
            }
            return eosReceived
        }

        try {
            // Main encoding loop
            for (frameIdx in 0 until totalFrames) {
                if (isCancelled) break

                val timeMs = ((frameIdx.toFloat() / fps.toFloat()) * 1000f).toLong()
                val timeUs = timeMs * 1000L

                // 1. Fetch smooth source frame at EXACT timestamp
                // OPTION_CLOSEST retrieves the real continuous frames (unlike OPTION_CLOSEST_SYNC which only returns keyframes)
                val sourceFrame = try {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                        ?: retriever.getFrameAtTime(timeUs)
                } catch (e: Exception) {
                    null
                }

                if (sourceFrame != null) {
                    lastValidFrame?.recycle()
                    lastValidFrame = sourceFrame
                }

                val frameToRender = sourceFrame ?: lastValidFrame

                // 2. Clear canvas with background color (matches preview background)
                canvas.drawRect(0f, 0f, outputWidth.toFloat(), outputHeight.toFloat(), bgPaint)

                // 3. Draw centered horizontal video frame (letterbox centered matching 16:9 in preview)
                if (frameToRender != null && !frameToRender.isRecycled) {
                    val srcWidth = frameToRender.width
                    val srcHeight = frameToRender.height
                    val targetVideoHeight = (outputWidth.toFloat() / srcWidth.toFloat() * srcHeight).toInt()
                    val topY = (outputHeight - targetVideoHeight) / 2
                    videoDestRect.set(0, topY, outputWidth, topY + targetVideoHeight)

                    canvas.drawBitmap(frameToRender, null, videoDestRect, null)
                }

                // 4. Current reps completed at timestamp
                val currentReps = reps.count { it.timestampMs <= timeMs }

                // 5. Draw Header text - exact top offset aligned with Compose preview
                val headerText = formatHeaderText(overlayConfig, currentReps)
                val headerYTop = outputHeight * overlayConfig.headerYPercent
                val headerBaseline = headerYTop - headerPaint.fontMetrics.ascent
                canvas.drawText(headerText, outputWidth / 2f, headerBaseline, headerPaint)

                // 6. Draw Quote text - exact top offset aligned with Compose preview
                val quoteYTop = outputHeight * overlayConfig.quoteYPercent
                val quoteBaseline = quoteYTop - quotePaint.fontMetrics.ascent
                canvas.drawText(overlayConfig.quoteText, outputWidth / 2f, quoteBaseline, quotePaint)

                // 7. Draw Floating Counter Badge (positioned at exact draggable X/Y coordinates)
                if (overlayConfig.showBadgeCounter) {
                    drawCounterBadge(
                        canvas = canvas,
                        config = overlayConfig,
                        currentReps = currentReps,
                        outputWidth = outputWidth,
                        outputHeight = outputHeight,
                        scale = scale,
                        paint = badgePaint
                    )
                }

                // 8. Draw Progress Bar at bottom if enabled
                if (overlayConfig.showProgressBar) {
                    val target = max(1, overlayConfig.targetPushups)
                    val progress = (currentReps.toFloat() / target.toFloat()).coerceIn(0f, 1f)
                    val barHeight = 8f * scale
                    val barMargin = 28f * scale
                    val barY = outputHeight - (32f * scale)

                    // Track background
                    badgePaint.color = Color.argb(120, 50, 50, 50)
                    badgePaint.style = Paint.Style.FILL
                    canvas.drawRoundRect(RectF(barMargin, barY, outputWidth - barMargin, barY + barHeight), 4f * scale, 4f * scale, badgePaint)

                    // Fill progress
                    if (progress > 0.01f) {
                        badgePaint.color = overlayConfig.badgeColor.toInt()
                        val fillWidth = (outputWidth - 2 * barMargin) * progress
                        canvas.drawRoundRect(RectF(barMargin, barY, barMargin + fillWidth, barY + barHeight), 4f * scale, 4f * scale, badgePaint)
                    }
                }

                // 9. Convert composite ARGB frame to NV12 using fast pre-allocated pixel buffer
                SampleVideoGenerator.argbToNv12(renderBitmap, yuvBuffer, outputWidth, outputHeight, pixelBuffer)

                // 10. Queue frame to encoder with reliable retries and output draining
                var queued = false
                var queueRetries = 0
                val isLastFrame = (frameIdx == totalFrames - 1)

                while (!queued && queueRetries < 50 && !isCancelled) {
                    val inputBufferIndex = encoder.dequeueInputBuffer(10_000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
                        inputBuffer?.clear()
                        inputBuffer?.put(yuvBuffer)
                        val ptsUs = (frameIdx * 1_000_000L) / fps
                        val flags = if (isLastFrame) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                        encoder.queueInputBuffer(inputBufferIndex, 0, yuvBuffer.size, ptsUs, flags)
                        queued = true
                    } else {
                        queueRetries++
                        drainOutput(false)
                    }
                }

                // 11. Drain encoder output
                drainOutput(false)

                if (frameIdx % 5 == 0 || frameIdx == totalFrames - 1) {
                    val p = (frameIdx.toFloat() / totalFrames.toFloat()).coerceIn(0f, 1f)
                    withContext(Dispatchers.Main) {
                        onProgress(p, frameIdx + 1, totalFrames)
                    }
                }
            }

            // 12. Drain any remaining output buffers after EOS
            var eosDrained = false
            var eosRetries = 0
            while (!eosDrained && eosRetries < 30 && muxerStarted) {
                if (drainOutput(true)) {
                    eosDrained = true
                    break
                }
                eosRetries++
            }

        } catch (e: Exception) {
            Log.e(TAG, "Encoding error during reel export", e)
            throw e
        } finally {
            // Cleanup video encoder and muxer safely
            try {
                encoder.stop()
            } catch (ignored: Exception) {}
            try {
                encoder.release()
            } catch (ignored: Exception) {}

            if (muxerStarted && samplesWritten > 0) {
                try {
                    muxer.stop()
                } catch (e: Exception) {
                    Log.w(TAG, "Muxer stop warning", e)
                }
            }
            try {
                muxer.release()
            } catch (ignored: Exception) {}

            try {
                retriever.release()
                lastValidFrame?.recycle()
                renderBitmap.recycle()
            } catch (ignored: Exception) {}
        }

        // Save to public gallery via MediaStore
        val publicUri = saveToGallery(outputFile, "PushUpReel_${System.currentTimeMillis()}.mp4")

        Pair(outputFile.absolutePath, publicUri)
    }

    private fun formatHeaderText(config: OverlayConfig, currentReps: Int): String {
        val text = when (config.headerFormat) {
            HeaderFormatMode.DAY_EQUALS_COUNT -> "DAY ${config.dayNumber} = ${config.targetPushups} PUSH UP"
            HeaderFormatMode.DYNAMIC_LIVE -> "DAY ${config.dayNumber} = $currentReps PUSH UP"
            HeaderFormatMode.REPS_FRACTION -> "$currentReps / ${config.targetPushups} PUSH UPS"
            HeaderFormatMode.CUSTOM -> config.customHeaderText
        }
        return if (config.allCapsHeader) text.uppercase() else text
    }

    private fun drawCounterBadge(
        canvas: Canvas,
        config: OverlayConfig,
        currentReps: Int,
        outputWidth: Int,
        outputHeight: Int,
        scale: Float,
        paint: Paint
    ) {
        val centerX = outputWidth * config.badgeXPercent
        val centerY = outputHeight * config.badgeYPercent

        when (config.badgeStyle) {
            BadgeStyle.PILL_ACCENT -> {
                val textToDraw = "$currentReps REPS"
                paint.textSize = 15f * scale
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

                val textW = paint.measureText(textToDraw)
                val badgeW = textW + 36f * scale
                val badgeH = 38f * scale

                val rectLeft = (centerX - badgeW / 2f).coerceIn(16f, outputWidth - badgeW - 16f)
                val rectTop = (centerY - badgeH / 2f).coerceIn(16f, outputHeight - badgeH - 16f)
                val badgeRect = RectF(rectLeft, rectTop, rectLeft + badgeW, rectTop + badgeH)

                // Dark background pill
                paint.color = Color.argb(238, 22, 22, 22)
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(badgeRect, badgeH / 2f, badgeH / 2f, paint)

                // Accent border
                paint.color = config.badgeColor.toInt()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2.5f * scale
                canvas.drawRoundRect(badgeRect, badgeH / 2f, badgeH / 2f, paint)

                // White bold text
                paint.style = Paint.Style.FILL
                paint.textAlign = Paint.Align.CENTER
                paint.color = Color.WHITE
                val baseline = badgeRect.centerY() - ((paint.descent() + paint.ascent()) / 2f)
                canvas.drawText(textToDraw, badgeRect.centerX(), baseline, paint)
            }
            BadgeStyle.CIRCULAR_GLOW -> {
                val circleRadius = 27f * scale
                val cx = centerX.coerceIn(circleRadius + 16f, outputWidth - circleRadius - 16f)
                val cy = centerY.coerceIn(circleRadius + 16f, outputHeight - circleRadius - 16f)

                // Dark fill
                paint.color = Color.argb(238, 22, 22, 22)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(cx, cy, circleRadius, paint)

                // Accent ring
                paint.color = config.badgeColor.toInt()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f * scale
                canvas.drawCircle(cx, cy, circleRadius, paint)

                // Bold count number
                paint.style = Paint.Style.FILL
                paint.textSize = 20f * scale
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textAlign = Paint.Align.CENTER
                paint.color = Color.WHITE
                val baseline = cy - ((paint.descent() + paint.ascent()) / 2f)
                canvas.drawText("$currentReps", cx, baseline, paint)
            }
            BadgeStyle.MINIMAL_TAG -> {
                val textToDraw = "REP #$currentReps"
                paint.textSize = 13f * scale
                paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

                val textW = paint.measureText(textToDraw)
                val badgeW = textW + 20f * scale
                val badgeH = 30f * scale

                val rectLeft = (centerX - badgeW / 2f).coerceIn(16f, outputWidth - badgeW - 16f)
                val rectTop = (centerY - badgeH / 2f).coerceIn(16f, outputHeight - badgeH - 16f)
                val badgeRect = RectF(rectLeft, rectTop, rectLeft + badgeW, rectTop + badgeH)

                // Dark box
                paint.color = Color.argb(238, 26, 26, 26)
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(badgeRect, 6f * scale, 6f * scale, paint)

                // Border
                paint.color = Color.argb(200, 68, 68, 68)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.5f * scale
                canvas.drawRoundRect(badgeRect, 6f * scale, 6f * scale, paint)

                // Monospace text in accent color
                paint.style = Paint.Style.FILL
                paint.textAlign = Paint.Align.CENTER
                paint.color = config.badgeColor.toInt()
                val baseline = badgeRect.centerY() - ((paint.descent() + paint.ascent()) / 2f)
                canvas.drawText(textToDraw, badgeRect.centerX(), baseline, paint)
            }
            BadgeStyle.NEON_HUD -> {
                val textToDraw = "PUSH-UP $currentReps"
                paint.textSize = 14f * scale
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

                val textW = paint.measureText(textToDraw)
                val badgeW = textW + 36f * scale
                val badgeH = 34f * scale

                val rectLeft = (centerX - badgeW / 2f).coerceIn(16f, outputWidth - badgeW - 16f)
                val rectTop = (centerY - badgeH / 2f).coerceIn(16f, outputHeight - badgeH - 16f)
                val badgeRect = RectF(rectLeft, rectTop, rectLeft + badgeW, rectTop + badgeH)

                // Dark gym tint
                paint.color = Color.argb(240, 16, 20, 16)
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(badgeRect, 8f * scale, 8f * scale, paint)

                // Accent border
                paint.color = config.badgeColor.toInt()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f * scale
                canvas.drawRoundRect(badgeRect, 8f * scale, 8f * scale, paint)

                // Glowing indicator dot
                val dotRadius = 4f * scale
                val dotX = badgeRect.left + 12f * scale
                val dotY = badgeRect.centerY()
                paint.style = Paint.Style.FILL
                canvas.drawCircle(dotX, dotY, dotRadius, paint)

                // Text
                paint.textAlign = Paint.Align.LEFT
                val baseline = badgeRect.centerY() - ((paint.descent() + paint.ascent()) / 2f)
                canvas.drawText(textToDraw, dotX + 8f * scale, baseline, paint)
            }
        }
    }

    private fun resolveTypeface(font: OverlayFont): Typeface {
        return when (font) {
            OverlayFont.IMPACT_CONDENSED -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            OverlayFont.ATHLETIC_BLACK -> Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            OverlayFont.CLEAN_SANS -> Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            OverlayFont.TECH_MONO -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            OverlayFont.CLASSIC_SERIF -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
    }

    private fun saveToGallery(file: File, displayName: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/PushUpReels")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val uri = context.contentResolver.insert(collection, values) ?: Uri.fromFile(file)

        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                FileInputStream(file).use { input ->
                    input.copyTo(out)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return uri
    }
}

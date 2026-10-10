package com.example.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.sin

/**
 * Generates an on-device sample MP4 workout video featuring an animated athlete
 * executing push-ups. This ensures the app is 100% testable out of the box.
 */
object SampleVideoGenerator {

    private const val TAG = "SampleVideoGenerator"

    suspend fun getOrCreateSampleVideo(context: Context): File = withContext(Dispatchers.IO) {
        val sampleFile = File(context.cacheDir, "sample_pushup_workout.mp4")
        if (sampleFile.exists() && sampleFile.length() > 50_000) {
            return@withContext sampleFile
        }

        val tempFile = File(context.cacheDir, "sample_temp_${System.currentTimeMillis()}.mp4")

        val width = 640
        val height = 360
        val fps = 30
        val durationSec = 10
        val totalFrames = fps * durationSec
        val bitRate = 2_000_000

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var videoTrackIndex = -1
        var muxerStarted = false
        var samplesWritten = 0

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }

        val yuvBuffer = ByteArray(width * height * 3 / 2)
        val pixelBuffer = IntArray(width * height)
        val bufferInfo = MediaCodec.BufferInfo()

        try {
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            for (frameIdx in 0 until totalFrames) {
                // Push-up animation cycle: ~1.25s per rep -> 8 reps across 10s
                val repPhase = (frameIdx % (fps * 1.25f)) / (fps * 1.25f)
                val descent = (sin(repPhase * 2.0 * Math.PI - Math.PI / 2.0).toFloat() + 1f) / 2f

                // Render workout scene
                drawWorkoutScene(canvas, width, height, descent, frameIdx, (frameIdx / (fps * 1.25f)).toInt() + 1, paint, textPaint)

                // Convert to NV12
                argbToNv12(bitmap, yuvBuffer, width, height, pixelBuffer)

                var queued = false
                var retries = 0
                while (!queued && retries < 50) {
                    val inputBufferIndex = encoder.dequeueInputBuffer(10_000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
                        inputBuffer?.clear()
                        inputBuffer?.put(yuvBuffer)
                        val ptsUs = (frameIdx * 1_000_000L) / fps
                        val isLast = (frameIdx == totalFrames - 1)
                        val flags = if (isLast) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                        encoder.queueInputBuffer(inputBufferIndex, 0, yuvBuffer.size, ptsUs, flags)
                        queued = true
                    } else {
                        retries++
                    }

                    // Drain output
                    while (true) {
                        val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 2000)
                        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            if (!muxerStarted) {
                                videoTrackIndex = muxer.addTrack(encoder.outputFormat)
                                muxer.start()
                                muxerStarted = true
                            }
                        } else if (outputBufferIndex >= 0) {
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
                                break
                            }
                        } else {
                            break
                        }
                    }
                }
            }

            // Drain remaining frames after EOS
            var eosRetries = 0
            while (muxerStarted && eosRetries < 25) {
                val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
                if (outputBufferIndex >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        encoder.releaseOutputBuffer(outputBufferIndex, false)
                        continue
                    }
                    val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                        samplesWritten++
                    }
                    encoder.releaseOutputBuffer(outputBufferIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                } else {
                    eosRetries++
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error encoding sample video", e)
        } finally {
            try {
                encoder?.stop()
            } catch (ignored: Exception) {}
            try {
                encoder?.release()
            } catch (ignored: Exception) {}

            if (muxerStarted && samplesWritten > 0) {
                try {
                    muxer?.stop()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to stop muxer safely", e)
                }
            }
            try {
                muxer?.release()
            } catch (ignored: Exception) {}

            bitmap.recycle()
        }

        if (tempFile.exists() && tempFile.length() > 5000 && samplesWritten > 0) {
            tempFile.renameTo(sampleFile)
            sampleFile
        } else {
            tempFile
        }
    }

    private fun drawWorkoutScene(
        canvas: Canvas,
        width: Int,
        height: Int,
        descent: Float, // 0.0 (top) to 1.0 (bottom)
        frameIdx: Int,
        currentRep: Int,
        paint: Paint,
        textPaint: Paint
    ) {
        // Dark gym background
        paint.color = Color.rgb(24, 26, 28)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Gym floor
        val floorY = height * 0.78f
        paint.color = Color.rgb(45, 48, 52)
        canvas.drawRect(0f, floorY, width.toFloat(), height.toFloat(), paint)

        // Workout mat
        val matRect = RectF(width * 0.15f, floorY - 6f, width * 0.85f, floorY + 4f)
        paint.color = Color.rgb(220, 80, 30) // Athletic flame color
        canvas.drawRoundRect(matRect, 4f, 4f, paint)

        // Athlete body coordinates
        val feetX = width * 0.28f
        val feetY = floorY - 14f

        val handsX = width * 0.68f
        val handsY = floorY - 10f

        // Torso descent
        val torsoDrop = descent * 55f
        val shoulderX = width * 0.64f
        val shoulderY = (floorY - 70f) + torsoDrop

        val hipX = width * 0.45f
        val hipY = (floorY - 45f) + (torsoDrop * 0.55f)

        val headX = shoulderX + 22f
        val headY = shoulderY - 8f

        // Draw arms
        paint.color = Color.rgb(240, 160, 120)
        paint.strokeWidth = 10f
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND

        // Elbow coordinate (flares out as descent increases)
        val elbowX = shoulderX - (15f * descent)
        val elbowY = (shoulderY + handsY) / 2f + (20f * descent)

        canvas.drawLine(shoulderX, shoulderY, elbowX, elbowY, paint)
        canvas.drawLine(elbowX, elbowY, handsX, handsY, paint)

        // Legs
        paint.color = Color.rgb(40, 42, 46) // Shorts
        paint.strokeWidth = 16f
        canvas.drawLine(feetX, feetY, hipX, hipY, paint)

        // Torso
        paint.color = Color.rgb(255, 90, 40) // Athletic jersey
        paint.strokeWidth = 18f
        canvas.drawLine(hipX, hipY, shoulderX, shoulderY, paint)

        // Head
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(240, 160, 120)
        canvas.drawCircle(headX, headY, 14f, paint)

        // HUD Rep Indicator
        paint.color = Color.argb(190, 0, 0, 0)
        canvas.drawRoundRect(RectF(width * 0.05f, 20f, width * 0.40f, 70f), 12f, 12f, paint)

        textPaint.color = Color.rgb(255, 180, 0) // Gold
        textPaint.textSize = 22f
        canvas.drawText("SAMPLE WORKOUT #$currentRep", width * 0.225f, 52f, textPaint)
    }

    fun argbToNv12(bitmap: Bitmap, nv12: ByteArray, width: Int, height: Int) {
        val pixelBuffer = IntArray(width * height)
        argbToNv12(bitmap, nv12, width, height, pixelBuffer)
    }

    fun argbToNv12(bitmap: Bitmap, nv12: ByteArray, width: Int, height: Int, pixelBuffer: IntArray) {
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize

        bitmap.getPixels(pixelBuffer, 0, width, 0, 0, width, height)

        for (j in 0 until height) {
            val rowOffset = j * width
            val isEvenRow = (j and 1) == 0
            for (i in 0 until width) {
                val pixel = pixelBuffer[rowOffset + i]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                // RGB to Y
                var y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                if (y < 16) y = 16 else if (y > 235) y = 235
                nv12[yIndex++] = y.toByte()

                // Subsample UV 2x2
                if (isEvenRow && (i and 1) == 0) {
                    var u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    var v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    if (u < 16) u = 16 else if (u > 240) u = 240
                    if (v < 16) v = 16 else if (v > 240) v = 240

                    nv12[uvIndex++] = u.toByte()
                    nv12[uvIndex++] = v.toByte()
                }
            }
        }
    }
}

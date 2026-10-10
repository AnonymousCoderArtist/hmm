package com.example.ui.components

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.BadgePosition
import com.example.data.model.BadgeStyle
import com.example.data.model.HeaderFormatMode
import com.example.data.model.OverlayConfig
import com.example.data.model.OverlayFont
import com.example.data.model.PushUpRep
import com.example.data.model.VideoMediaInfo
import com.example.ui.theme.AthleticGold
import com.example.ui.theme.AthleticGreen
import com.example.ui.theme.AthleticOrange
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.max

@Composable
fun ReelPreviewSurface(
    videoMedia: VideoMediaInfo?,
    overlayConfig: OverlayConfig,
    reps: List<PushUpRep>,
    onCurrentTimeChanged: (Long) -> Unit,
    onOverlayConfigChanged: ((OverlayConfig) -> OverlayConfig) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentOverlayConfig by rememberUpdatedState(overlayConfig)
    val onConfigChangeUpdated by rememberUpdatedState(onOverlayConfigChanged)

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(10000L) }
    var showControls by remember { mutableStateOf(true) }
    var currentSurface by remember { mutableStateOf<Surface?>(null) }

    // Periodically update position from MediaPlayer safely
    LaunchedEffect(isPlaying, mediaPlayer) {
        while (isPlaying && mediaPlayer != null) {
            try {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val pos = player.currentPosition.toLong()
                        currentPositionMs = pos
                        onCurrentTimeChanged(pos)
                    }
                }
            } catch (e: Exception) {
                // Ignore transient states during seeking
            }
            delay(60) // Smooth ~16Hz UI slider updates
        }
    }

    // Auto-hide controls after idle playback
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(2800)
            showControls = false
        }
    }

    // Current reps completed up to now
    val currentReps = remember(reps, currentPositionMs) {
        reps.count { it.timestampMs <= currentPositionMs }
    }

    // Stable MediaPlayer initialization keyed strictly on media file and surface
    val mediaFilePath = videoMedia?.filePath
    val mediaUriString = videoMedia?.uri?.toString()

    LaunchedEffect(mediaFilePath, mediaUriString, currentSurface) {
        val surface = currentSurface
        val media = videoMedia

        if (surface != null && surface.isValid && media != null) {
            val filePath = media.filePath
            val uri = media.uri
            val fileValid = filePath != null && File(filePath).exists() && File(filePath).length() > 0
            val uriValid = uri != null

            if (fileValid || uriValid) {
                try {
                    // Cleanly dispose previous player
                    mediaPlayer?.let { oldPlayer ->
                        oldPlayer.setOnPreparedListener(null)
                        oldPlayer.setOnErrorListener(null)
                        if (oldPlayer.isPlaying) oldPlayer.stop()
                        oldPlayer.release()
                    }
                    mediaPlayer = null
                    isPlaying = false

                    val player = MediaPlayer().apply {
                        setSurface(surface)
                        if (fileValid) {
                            setDataSource(filePath)
                        } else if (uri != null) {
                            setDataSource(context, uri)
                        }
                        isLooping = true
                        setOnPreparedListener { mp ->
                            try {
                                durationMs = mp.duration.toLong().coerceAtLeast(1000L)
                                mp.start()
                                isPlaying = true
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        setOnErrorListener { _, what, extra ->
                            // Handled gracefully without crash
                            true
                        }
                        prepareAsync()
                    }
                    mediaPlayer = player
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(overlayConfig.backgroundColor))
            .border(1.5.dp, Color(0xFF2C2C2C), RoundedCornerShape(16.dp))
            .clickable { showControls = !showControls }
            .testTag("reel_preview_surface")
    ) {
        // 9:16 vertical ratio container
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val totalHeight = maxHeight
            val totalWidth = maxWidth

            // 1. Centered Horizontal Video View (16:9 in the middle of 9:16)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .aspectRatio(16f / 9f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        TextureView(ctx).apply {
                            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                                    currentSurface = Surface(st)
                                }

                                override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {}

                                override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                    try {
                                        mediaPlayer?.let { p ->
                                            p.setOnPreparedListener(null)
                                            p.setOnErrorListener(null)
                                            if (p.isPlaying) p.stop()
                                            p.release()
                                        }
                                    } catch (ignored: Exception) {}
                                    mediaPlayer = null
                                    currentSurface?.release()
                                    currentSurface = null
                                    isPlaying = false
                                    return true
                                }

                                override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 2. Top Header Text (e.g., "DAY 29 = 29 PUSH UP")
            val headerText = remember(overlayConfig, currentReps) {
                val raw = when (overlayConfig.headerFormat) {
                    HeaderFormatMode.DAY_EQUALS_COUNT -> "DAY ${overlayConfig.dayNumber} = ${overlayConfig.targetPushups} PUSH UP"
                    HeaderFormatMode.DYNAMIC_LIVE -> "DAY ${overlayConfig.dayNumber} = $currentReps PUSH UP"
                    HeaderFormatMode.REPS_FRACTION -> "$currentReps / ${overlayConfig.targetPushups} PUSH UPS"
                    HeaderFormatMode.CUSTOM -> overlayConfig.customHeaderText
                }
                if (overlayConfig.allCapsHeader) raw.uppercase() else raw
            }

            val headerFontFamily = resolveFontFamily(overlayConfig.headerFont)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset {
                        IntOffset(0, (totalHeight.toPx() * overlayConfig.headerYPercent).toInt())
                    }
                    .pointerInput(totalHeight) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newY = (currentOverlayConfig.headerYPercent + (dragAmount.y / totalHeight.toPx())).coerceIn(0.04f, 0.38f)
                            onConfigChangeUpdated { it.copy(headerYPercent = newY) }
                        }
                    }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = headerText,
                    color = Color(overlayConfig.headerColor),
                    fontSize = overlayConfig.headerTextSizeSp.sp,
                    fontFamily = headerFontFamily,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    lineHeight = (overlayConfig.headerTextSizeSp + 4).sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (overlayConfig.textShadow) {
                                Modifier.shadow(8.dp)
                            } else Modifier
                        )
                )
            }

            // 3. Bottom Quote Text (e.g. "Posting everyday until i forget her")
            val quoteFontFamily = resolveFontFamily(overlayConfig.quoteFont)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset {
                        IntOffset(0, (totalHeight.toPx() * overlayConfig.quoteYPercent).toInt())
                    }
                    .pointerInput(totalHeight) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newY = (currentOverlayConfig.quoteYPercent + (dragAmount.y / totalHeight.toPx())).coerceIn(0.58f, 0.92f)
                            onConfigChangeUpdated { it.copy(quoteYPercent = newY) }
                        }
                    }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = overlayConfig.quoteText,
                    color = Color(overlayConfig.quoteColor),
                    fontSize = overlayConfig.quoteTextSizeSp.sp,
                    fontFamily = quoteFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = (overlayConfig.quoteTextSizeSp + 6).sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (overlayConfig.textShadow) {
                                Modifier.shadow(6.dp)
                            } else Modifier
                        )
                )
            }

            // 4. Floating Push-up Counter Badge (Fully 2D Draggable anywhere)
            if (overlayConfig.showBadgeCounter) {
                Box(
                    modifier = Modifier
                        .offset {
                            // Center the badge around badgeXPercent and badgeYPercent
                            val badgePxX = (totalWidth.toPx() * overlayConfig.badgeXPercent) - 52.dp.toPx()
                            val badgePxY = (totalHeight.toPx() * overlayConfig.badgeYPercent) - 18.dp.toPx()
                            IntOffset(
                                badgePxX.toInt().coerceIn(12, (totalWidth.toPx() - 110.dp.toPx()).toInt()),
                                badgePxY.toInt().coerceIn(12, (totalHeight.toPx() - 50.dp.toPx()).toInt())
                            )
                        }
                        .pointerInput(totalWidth, totalHeight) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val newX = ((totalWidth.toPx() * currentOverlayConfig.badgeXPercent + dragAmount.x) / totalWidth.toPx()).coerceIn(0.08f, 0.92f)
                                val newY = ((totalHeight.toPx() * currentOverlayConfig.badgeYPercent + dragAmount.y) / totalHeight.toPx()).coerceIn(0.05f, 0.94f)
                                onConfigChangeUpdated { it.copy(badgeXPercent = newX, badgeYPercent = newY) }
                            }
                        }
                ) {
                    FloatingBadgeComposable(
                        config = overlayConfig,
                        currentReps = currentReps,
                        modifier = Modifier.testTag("draggable_counter_badge")
                    )
                }
            }

            // 5. Progress Bar at bottom of Reel
            if (overlayConfig.showProgressBar) {
                val target = max(1, overlayConfig.targetPushups)
                val progress = (currentReps.toFloat() / target.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(overlayConfig.badgeColor),
                        trackColor = Color(0x55333333)
                    )
                }
            }

            // 6. Play / Pause & Scrubber Overlay
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x66000000))
                ) {
                    // Center Play/Pause button
                    IconButton(
                        onClick = {
                            mediaPlayer?.let { player ->
                                try {
                                    if (player.isPlaying) {
                                        player.pause()
                                        isPlaying = false
                                    } else {
                                        player.start()
                                        isPlaying = true
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .align(Alignment.Center)
                            .background(Color(0x99000000), CircleShape)
                            .border(2.dp, AthleticOrange, CircleShape)
                            .testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Scrubber with rep markers at bottom
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(currentPositionMs),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Rep $currentReps / ${reps.size} detected",
                                color = AthleticGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatTime(durationMs),
                                color = Color(0xFFAAAAAA),
                                fontSize = 12.sp
                            )
                        }

                        Slider(
                            value = currentPositionMs.toFloat().coerceIn(0f, max(1000f, durationMs.toFloat())),
                            onValueChange = { newPos ->
                                currentPositionMs = newPos.toLong()
                                try {
                                    mediaPlayer?.seekTo(newPos.toInt())
                                } catch (ignored: Exception) {}
                                onCurrentTimeChanged(newPos.toLong())
                            },
                            valueRange = 0f..max(1000f, durationMs.toFloat()),
                            colors = SliderDefaults.colors(
                                thumbColor = AthleticOrange,
                                activeTrackColor = AthleticOrange,
                                inactiveTrackColor = Color(0x66888888)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("playback_scrubber")
                        )
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.let { p ->
                    p.setOnPreparedListener(null)
                    p.setOnErrorListener(null)
                    if (p.isPlaying) p.stop()
                    p.release()
                }
            } catch (ignored: Exception) {}
            mediaPlayer = null
        }
    }
}

@Composable
private fun FloatingBadgeComposable(
    config: OverlayConfig,
    currentReps: Int,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(config.badgeColor)

    when (config.badgeStyle) {
        BadgeStyle.PILL_ACCENT -> {
            Row(
                modifier = modifier
                    .background(Color(0xEE161616), RoundedCornerShape(20.dp))
                    .border(2.dp, accentColor, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "🔥",
                    fontSize = 15.sp
                )
                Text(
                    text = "$currentReps REPS",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
        }
        BadgeStyle.CIRCULAR_GLOW -> {
            Box(
                modifier = modifier
                    .size(54.dp)
                    .background(Color(0xEE161616), CircleShape)
                    .border(3.dp, accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$currentReps",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
            }
        }
        BadgeStyle.MINIMAL_TAG -> {
            Box(
                modifier = modifier
                    .background(Color(0xEE1A1A1A), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFF444444), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "REP #$currentReps",
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            }
        }
        BadgeStyle.NEON_HUD -> {
            Row(
                modifier = modifier
                    .background(Color(0xF0101410), RoundedCornerShape(8.dp))
                    .border(1.5.dp, accentColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(accentColor, CircleShape)
                )
                Text(
                    text = "PUSH-UP $currentReps",
                    color = accentColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }
        }
    }
}

private fun resolveFontFamily(font: OverlayFont): FontFamily {
    return when (font) {
        OverlayFont.IMPACT_CONDENSED -> FontFamily.SansSerif
        OverlayFont.ATHLETIC_BLACK -> FontFamily.Default
        OverlayFont.CLEAN_SANS -> FontFamily.SansSerif
        OverlayFont.TECH_MONO -> FontFamily.Monospace
        OverlayFont.CLASSIC_SERIF -> FontFamily.Serif
    }
}

private fun formatTime(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / 60000)
    return String.format("%02d:%02d", min, sec)
}

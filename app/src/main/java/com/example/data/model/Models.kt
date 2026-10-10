package com.example.data.model

import android.net.Uri

/**
 * Individual push-up repetition identified by computer vision or user override.
 */
data class PushUpRep(
    val id: Int,
    val timestampMs: Long,
    val depthPercent: Float = 100f,
    val durationMs: Long = 1000L,
    val confidence: Float = 0.95f
)

/**
 * Pose detection orientation and sensitivity settings.
 */
enum class PoseMode(val displayName: String, val description: String) {
    AUTO("Auto Multi-Pose", "Detects reps from both front and side camera angles"),
    FRONT_POSE("Front Pose", "Optimized for chest-to-camera angle & elbow flare"),
    SIDE_POSE("Side Pose", "Optimized for lateral arm flexion & torso descent"),
    RAPID_PACED("Rapid Paced", "Ultra-fast response for quick consecutive reps")
}

enum class Sensitivity(val displayName: String, val thresholdFactor: Float) {
    HIGH("High (Fast Reps)", 0.65f),
    BALANCED("Balanced (Standard)", 0.80f),
    STRICT("Strict (Deep Chest)", 0.95f)
}

/**
 * Typography styling presets for Reel text overlays.
 */
enum class OverlayFont(val displayName: String, val fontKey: String) {
    IMPACT_CONDENSED("Impact Bold", "impact"),
    ATHLETIC_BLACK("Athletic Heavy", "sans_black"),
    CLEAN_SANS("Modern Sans", "sans_bold"),
    TECH_MONO("Monospace Sports", "mono"),
    CLASSIC_SERIF("Classic Serif", "serif")
}

/**
 * Display format for the top bold title.
 */
enum class HeaderFormatMode(val displayName: String) {
    DAY_EQUALS_COUNT("DAY X = X PUSH UP"),       // e.g. "DAY 29 = 29 PUSH UP"
    DYNAMIC_LIVE("DAY X = {reps} PUSH UP"),      // Increments dynamically with video progress!
    REPS_FRACTION("{reps} / {target} PUSH UPS"), // e.g. "14 / 29 PUSH UPS"
    CUSTOM("Custom Header Text")
}

/**
 * Position of the floating live counter badge.
 */
enum class BadgePosition(val displayName: String) {
    TOP_RIGHT("Top Right"),
    TOP_LEFT("Top Left"),
    BOTTOM_RIGHT("Bottom Right"),
    BOTTOM_LEFT("Bottom Left"),
    OVER_VIDEO_TOP("Above Video"),
    OVER_VIDEO_BOTTOM("Below Video")
}

/**
 * Style of the counter badge.
 */
enum class BadgeStyle(val displayName: String) {
    PILL_ACCENT("Flame Pill"),
    CIRCULAR_GLOW("Gold Ring"),
    MINIMAL_TAG("Minimal Box"),
    NEON_HUD("Neon Gym HUD")
}

/**
 * Color preset representing non-blue, non-purple athletic shades.
 */
data class ColorPreset(
    val name: String,
    val colorHex: Long
)

val OVERLAY_COLOR_PRESETS = listOf(
    ColorPreset("Pure White", 0xFFFFFFFF),
    ColorPreset("Flame Orange", 0xFFFF5722),
    ColorPreset("Athletic Gold", 0xFFFFB300),
    ColorPreset("Neon Lime", 0xFF00E676),
    ColorPreset("Electric Yellow", 0xFFFFEA00),
    ColorPreset("Gym Crimson", 0xFFFF1744),
    ColorPreset("Silver Grey", 0xFFE0E0E0),
    ColorPreset("Pitch Black", 0xFF000000)
)

val BACKGROUND_COLOR_PRESETS = listOf(
    ColorPreset("Reel Pitch Black", 0xFF000000),
    ColorPreset("Charcoal Matte", 0xFF121212),
    ColorPreset("Gym Slate", 0xFF1F1F1F),
    ColorPreset("Warm Sand (Light)", 0xFFF5F5F7)
)

/**
 * Full configuration for the Instagram Reel 9:16 video overlay.
 */
data class OverlayConfig(
    val dayNumber: Int = 29,
    val targetPushups: Int = 29,
    val headerFormat: HeaderFormatMode = HeaderFormatMode.DAY_EQUALS_COUNT,
    val customHeaderText: String = "DAY 29 = 29 PUSH UP",
    val quoteText: String = "Posting everyday until i forget her",
    val headerFont: OverlayFont = OverlayFont.IMPACT_CONDENSED,
    val quoteFont: OverlayFont = OverlayFont.CLEAN_SANS,
    val headerColor: Long = 0xFFFFFFFF,
    val quoteColor: Long = 0xFFFFFFFF,
    val backgroundColor: Long = 0xFF000000,
    val headerYPercent: Float = 0.16f,          // 16% from top
    val quoteYPercent: Float = 0.67f,           // 67% from top (just below center video)
    val headerTextSizeSp: Int = 38,
    val quoteTextSizeSp: Int = 22,
    val showDynamicCounter: Boolean = true,
    val showBadgeCounter: Boolean = true,
    val badgePosition: BadgePosition = BadgePosition.TOP_RIGHT,
    val badgeXPercent: Float = 0.78f,           // 2D draggable X (0.0 to 1.0)
    val badgeYPercent: Float = 0.10f,           // 2D draggable Y (0.0 to 1.0)
    val badgeStyle: BadgeStyle = BadgeStyle.PILL_ACCENT,
    val badgeColor: Long = 0xFFFF5722,
    val showProgressBar: Boolean = true,
    val textShadow: Boolean = true,
    val allCapsHeader: Boolean = true
)

/**
 * Representation of video under editing.
 */
data class VideoMediaInfo(
    val uri: Uri? = null,
    val filePath: String? = null,
    val isSampleDemo: Boolean = false,
    val durationMs: Long = 0L,
    val width: Int = 1280,
    val height: Int = 720,
    val displayName: String = "Workout Video"
)

/**
 * Computer vision detection summary.
 */
data class CvAnalysisResult(
    val detectedReps: List<PushUpRep>,
    val averageRepTimeMs: Long,
    val fastestRepTimeMs: Long,
    val maxDepthPercent: Float,
    val consistencyScore: Int, // 0 - 100
    val analysisDurationMs: Long
)

/**
 * Video export state.
 */
sealed class ExportState {
    object Idle : ExportState()
    data class InProgress(val progress: Float, val currentFrame: Int, val totalFrames: Int, val message: String) : ExportState()
    data class Success(val outputPath: String, val outputUri: Uri, val durationMs: Long) : ExportState()
    data class Error(val message: String) : ExportState()
}

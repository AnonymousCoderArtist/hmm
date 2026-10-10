package com.example.cv

import android.graphics.Bitmap
import com.example.data.model.PoseMode
import com.example.data.model.PushUpRep
import com.example.data.model.Sensitivity
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Biometric trajectory extractor and peak-valley state machine for push-up detection.
 * Designed to accurately handle both FRONT POSE and SIDE POSE angles, as well as RAPID movements.
 */
class PoseBiometrics(
    private val poseMode: PoseMode = PoseMode.AUTO,
    private val sensitivity: Sensitivity = Sensitivity.BALANCED
) {

    data class FrameMotionSample(
        val timestampMs: Long,
        val verticalCentroid: Float, // Normalized 0.0 (top) to 1.0 (bottom)
        val motionEnergy: Float,
        val lateralSpread: Float     // Useful for front-pose elbow flare
    )

    /**
     * Extracts vertical centroid and motion energy by comparing current frame with previous frame.
     * Operates on downscaled bitmaps (e.g. 120x80) for near-instant execution.
     */
    fun computeFrameMotion(
        currentBitmap: Bitmap,
        previousBitmap: Bitmap?,
        timestampMs: Long
    ): FrameMotionSample {
        if (previousBitmap == null) {
            return FrameMotionSample(timestampMs, 0.5f, 0f, 0.5f)
        }

        val width = min(currentBitmap.width, previousBitmap.width)
        val height = min(currentBitmap.height, previousBitmap.height)

        var totalDiffWeight = 0f
        var weightedYSum = 0f
        var minActiveX = width
        var maxActiveX = 0
        val diffThreshold = 24 // Noise filter

        // Sample grid for speed
        val step = 4
        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val currPixel = currentBitmap.getPixel(x, y)
                val prevPixel = previousBitmap.getPixel(x, y)

                // Fast grayscale luminance calculation
                val currLum = ((currPixel shr 16 and 0xFF) * 299 + (currPixel shr 8 and 0xFF) * 587 + (currPixel and 0xFF) * 114) / 1000
                val prevLum = ((prevPixel shr 16 and 0xFF) * 299 + (prevPixel shr 8 and 0xFF) * 587 + (prevPixel and 0xFF) * 114) / 1000

                val diff = abs(currLum - prevLum)
                if (diff > diffThreshold) {
                    val weight = diff.toFloat()
                    totalDiffWeight += weight
                    weightedYSum += y * weight
                    if (x < minActiveX) minActiveX = x
                    if (x > maxActiveX) maxActiveX = x
                }
            }
        }

        val verticalCentroid = if (totalDiffWeight > 100f) {
            (weightedYSum / totalDiffWeight) / height.toFloat()
        } else {
            0.5f
        }

        val lateralSpread = if (maxActiveX > minActiveX) {
            (maxActiveX - minActiveX).toFloat() / width.toFloat()
        } else {
            0.5f
        }

        val normalizedEnergy = totalDiffWeight / ((width * height) / (step * step))

        return FrameMotionSample(
            timestampMs = timestampMs,
            verticalCentroid = verticalCentroid.coerceIn(0f, 1f),
            motionEnergy = normalizedEnergy,
            lateralSpread = lateralSpread.coerceIn(0f, 1f)
        )
    }

    /**
     * Analyzes a time-series of motion samples and identifies completed push-up reps.
     * Uses dynamic range calibration and a 4-state hysteresis state machine:
     * TOP -> DESCENDING -> BOTTOM (INFLECTION) -> ASCENDING -> TOP (REP COMPLETE)
     */
    fun processTrajectory(samples: List<FrameMotionSample>): List<PushUpRep> {
        if (samples.size < 6) return emptyList()

        // 1. Smooth vertical trajectory using Exponential Moving Average
        val smoothed = mutableListOf<Float>()
        var ema = samples.first().verticalCentroid
        val alpha = when (poseMode) {
            PoseMode.RAPID_PACED -> 0.45f // Lower smoothing for rapid reps responsiveness
            else -> 0.30f
        }

        for (sample in samples) {
            // In front pose, blend vertical descent with lateral expansion (elbow flare)
            val signal = if (poseMode == PoseMode.FRONT_POSE) {
                sample.verticalCentroid * 0.7f + sample.lateralSpread * 0.3f
            } else {
                sample.verticalCentroid
            }
            ema = alpha * signal + (1f - alpha) * ema
            smoothed.add(ema)
        }

        // 2. Compute dynamic range (baseline top vs deepest bottom)
        var globalMin = smoothed.minOrNull() ?: 0.3f // Top lockout position (lower Y)
        var globalMax = smoothed.maxOrNull() ?: 0.7f // Bottom chest-to-floor (higher Y)
        val dynamicRange = globalMax - globalMin

        // If the movement amplitude is too low to be a real workout, return empty
        if (dynamicRange < 0.04f) {
            return emptyList()
        }

        // 3. Sensitivity thresholds
        val minDepthFraction = when (sensitivity) {
            Sensitivity.HIGH -> 0.40f
            Sensitivity.BALANCED -> 0.55f
            Sensitivity.STRICT -> 0.70f
        }
        val resetThresholdFraction = 0.25f

        val downThreshold = globalMin + dynamicRange * minDepthFraction
        val topThreshold = globalMin + dynamicRange * resetThresholdFraction

        val minRepDurationMs = when (poseMode) {
            PoseMode.RAPID_PACED -> 280L
            else -> 400L
        }

        // 4. State Machine
        var state = State.AT_TOP
        var repStartMs = samples.first().timestampMs
        var bottomMs = 0L
        var deepestY = globalMin
        val detectedReps = mutableListOf<PushUpRep>()
        var repCounter = 1

        for (i in samples.indices) {
            val y = smoothed[i]
            val timeMs = samples[i].timestampMs

            when (state) {
                State.AT_TOP -> {
                    if (y > downThreshold) {
                        state = State.DESCENDING
                        repStartMs = timeMs
                        deepestY = y
                    }
                }
                State.DESCENDING -> {
                    if (y > deepestY) {
                        deepestY = y
                    }
                    // Inflection point: starts moving back up
                    if (y < deepestY - (dynamicRange * 0.10f)) {
                        state = State.ASCENDING
                        bottomMs = timeMs
                    }
                }
                State.ASCENDING -> {
                    if (y < topThreshold) {
                        // Reached top lockout! Verify minimum duration
                        val duration = timeMs - repStartMs
                        if (duration >= minRepDurationMs) {
                            val depthPercent = ((deepestY - globalMin) / dynamicRange * 100f).coerceIn(60f, 100f)
                            detectedReps.add(
                                PushUpRep(
                                    id = repCounter++,
                                    timestampMs = timeMs, // timestamp when rep is achieved
                                    depthPercent = depthPercent,
                                    durationMs = duration,
                                    confidence = 0.94f
                                )
                            )
                        }
                        state = State.AT_TOP
                        deepestY = globalMin
                    } else if (y > deepestY) {
                        // Re-descended before lockout
                        deepestY = y
                        state = State.DESCENDING
                    }
                }
            }
        }

        return detectedReps
    }

    private enum class State {
        AT_TOP,
        DESCENDING,
        ASCENDING
    }
}

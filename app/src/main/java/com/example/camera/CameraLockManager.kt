package com.example.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.RggbChannelVector
import android.util.Log
import com.example.model.CameraLockConfig

/**
 * Manages manual locking of camera hardware parameters (AF, AE, AWB, Stabilization).
 * Eliminates lens movements, sensor gain changes, and exposure variations between frames.
 */
class CameraLockManager(
    var currentConfig: CameraLockConfig = CameraLockConfig()
) {
    companion object {
        private const val TAG = "CameraLockManager"
    }

    var isLocked: Boolean = true
        private set

    /**
     * Applies manual locked settings to a CaptureRequest.Builder.
     */
    fun applyLockedParameters(builder: CaptureRequest.Builder, characteristics: CameraCharacteristics?) {
        try {
            // 1. Lock Auto Focus (AF) to fixed focus distance
            builder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF)
            
            // Check valid focus distance range
            val minFocusDistance = characteristics?.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 5.0f
            val safeFocus = currentConfig.focusDistance.coerceIn(0f, minFocusDistance)
            builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, safeFocus)
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE)

            // 2. Lock Auto Exposure (AE) to manual exposure time and sensitivity (ISO)
            builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
            
            // Validate against sensor limits
            val expRange = characteristics?.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
            val safeExp = if (expRange != null) {
                currentConfig.exposureTimeNs.coerceIn(expRange.lower, expRange.upper)
            } else {
                currentConfig.exposureTimeNs
            }
            builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, safeExp)

            val isoRange = characteristics?.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
            val safeIso = if (isoRange != null) {
                currentConfig.sensitivityIso.coerceIn(isoRange.lower, isoRange.upper)
            } else {
                currentConfig.sensitivityIso
            }
            builder.set(CaptureRequest.SENSOR_SENSITIVITY, safeIso)
            builder.set(CaptureRequest.CONTROL_AE_LOCK, true)

            // 3. Lock Auto White Balance (AWB) to manual RGB channel gains
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_OFF)
            val gains = RggbChannelVector(
                currentConfig.rGain,
                currentConfig.gEvenGain,
                currentConfig.gOddGain,
                currentConfig.bGain
            )
            builder.set(CaptureRequest.COLOR_CORRECTION_GAINS, gains)
            builder.set(CaptureRequest.CONTROL_AWB_LOCK, true)

            // 4. Disable Optical and Video Stabilization (prevents focal plane shift)
            builder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF)
            builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF)

            isLocked = true
            Log.d(TAG, "Successfully applied locked parameters: Focus=${safeFocus}d, Exp=${safeExp}ns, ISO=$safeIso")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying locked parameters", e)
        }
    }

    /**
     * Updates locked values from a live metered capture result.
     * Freezes auto-exposure, auto-focus, and AWB to the current scene lighting.
     */
    fun freezeFromCaptureResult(result: CaptureResult): CameraLockConfig {
        val expTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME) ?: currentConfig.exposureTimeNs
        val sensitivity = result.get(CaptureResult.SENSOR_SENSITIVITY) ?: currentConfig.sensitivityIso
        val focusDist = result.get(CaptureResult.LENS_FOCUS_DISTANCE) ?: currentConfig.focusDistance
        val gains = result.get(CaptureResult.COLOR_CORRECTION_GAINS)

        currentConfig = CameraLockConfig(
            focusDistance = if (focusDist > 0f) focusDist else 1.5f,
            exposureTimeNs = expTime,
            sensitivityIso = sensitivity,
            rGain = gains?.red ?: currentConfig.rGain,
            gEvenGain = gains?.greenEven ?: currentConfig.gEvenGain,
            gOddGain = gains?.greenOdd ?: currentConfig.gOddGain,
            bGain = gains?.blue ?: currentConfig.bGain,
            isLocked = true,
            lockedAtTimestamp = System.currentTimeMillis()
        )
        isLocked = true
        Log.i(TAG, "Frozen camera parameters: $currentConfig")
        return currentConfig
    }

    fun updateConfig(config: CameraLockConfig) {
        currentConfig = config
        isLocked = config.isLocked
    }
}

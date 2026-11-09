package com.example.model

import org.json.JSONObject

/**
 * Hardware camera parameters strictly locked during calibration and depth estimation.
 * Prevents autofocus, auto-exposure, auto-white-balance, and stabilization drift.
 */
data class CameraLockConfig(
    val focusDistance: Float = 1.5f, // diopters: 1.5 ~ 0.67m (ideal distance for arm's length)
    val exposureTimeNs: Long = 16_666_666L, // ~1/60 sec
    val sensitivityIso: Int = 200,
    val rGain: Float = 1.8f,
    val gEvenGain: Float = 1.0f,
    val gOddGain: Float = 1.0f,
    val bGain: Float = 1.6f,
    val isLocked: Boolean = true,
    val lockedAtTimestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("focusDistance", focusDistance.toDouble())
        obj.put("exposureTimeNs", exposureTimeNs)
        obj.put("sensitivityIso", sensitivityIso)
        obj.put("rGain", rGain.toDouble())
        obj.put("gEvenGain", gEvenGain.toDouble())
        obj.put("gOddGain", gOddGain.toDouble())
        obj.put("bGain", bGain.toDouble())
        obj.put("isLocked", isLocked)
        obj.put("lockedAtTimestamp", lockedAtTimestamp)
        return obj.toString(2)
    }

    companion object {
        fun fromJson(jsonStr: String): CameraLockConfig {
            return try {
                val obj = JSONObject(jsonStr)
                CameraLockConfig(
                    focusDistance = obj.optDouble("focusDistance", 1.5).toFloat(),
                    exposureTimeNs = obj.optLong("exposureTimeNs", 16_666_666L),
                    sensitivityIso = obj.optInt("sensitivityIso", 200),
                    rGain = obj.optDouble("rGain", 1.8).toFloat(),
                    gEvenGain = obj.optDouble("gEvenGain", 1.0).toFloat(),
                    gOddGain = obj.optDouble("gOddGain", 1.0).toFloat(),
                    bGain = obj.optDouble("bGain", 1.6).toFloat(),
                    isLocked = obj.optBoolean("isLocked", true),
                    lockedAtTimestamp = obj.optLong("lockedAtTimestamp", System.currentTimeMillis())
                )
            } catch (e: Exception) {
                CameraLockConfig()
            }
        }
    }
}

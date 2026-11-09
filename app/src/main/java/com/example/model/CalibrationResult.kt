package com.example.model

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Stores full intrinsic and distortion calibration result produced by OpenCV calibrateCamera.
 */
data class CalibrationResult(
    val fx: Double,
    val fy: Double,
    val cx: Double,
    val cy: Double,
    val k1: Double,
    val k2: Double,
    val p1: Double,
    val p2: Double,
    val k3: Double,
    val reprojectionError: Double,
    val imageWidth: Int,
    val imageHeight: Int,
    val imageCount: Int,
    val perViewErrors: List<Double> = emptyList(),
    val dateTimestamp: Long = System.currentTimeMillis(),
    val sampleImagePath: String? = null,
    val sampleUndistortedPath: String? = null,
    val cameraLockConfig: CameraLockConfig? = null
) {
    val formattedDate: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(dateTimestamp))

    val isGoodQuality: Boolean get() = reprojectionError <= 0.50

    val qualityRating: String get() = when {
        reprojectionError < 0.25 -> "Excellent (< 0.25 px)"
        reprojectionError <= 0.50 -> "Good (≤ 0.50 px)"
        reprojectionError <= 0.80 -> "Fair (> 0.50 px)"
        else -> "Poor (> 0.80 px, Recalibration advised)"
    }

    fun toJson(): String {
        val obj = JSONObject()
        obj.put("fx", fx)
        obj.put("fy", fy)
        obj.put("cx", cx)
        obj.put("cy", cy)
        obj.put("k1", k1)
        obj.put("k2", k2)
        obj.put("p1", p1)
        obj.put("p2", p2)
        obj.put("k3", k3)
        obj.put("reprojectionError", reprojectionError)
        obj.put("imageWidth", imageWidth)
        obj.put("imageHeight", imageHeight)
        obj.put("imageCount", imageCount)
        obj.put("dateTimestamp", dateTimestamp)
        obj.put("formattedDate", formattedDate)
        sampleImagePath?.let { obj.put("sampleImagePath", it) }
        sampleUndistortedPath?.let { obj.put("sampleUndistortedPath", it) }

        val errorsArr = JSONArray()
        perViewErrors.forEach { errorsArr.put(it) }
        obj.put("perViewErrors", errorsArr)

        cameraLockConfig?.let {
            obj.put("cameraLockConfig", JSONObject(it.toJson()))
        }
        return obj.toString(2)
    }

    /**
     * Standard OpenCV FileStorage YAML representation.
     */
    fun toYaml(): String {
        return buildString {
            append("%YAML:1.0\n")
            append("---\n")
            append("calibration_time: \"$formattedDate\"\n")
            append("image_width: $imageWidth\n")
            append("image_height: $imageHeight\n")
            append("image_count: $imageCount\n")
            append("avg_reprojection_error: $reprojectionError\n")
            append("camera_matrix: !!opencv-matrix\n")
            append("   rows: 3\n")
            append("   cols: 3\n")
            append("   dt: d\n")
            append("   data: [ $fx, 0., $cx, 0., $fy, $cy, 0., 0., 1. ]\n")
            append("distortion_coefficients: !!opencv-matrix\n")
            append("   rows: 5\n")
            append("   cols: 1\n")
            append("   dt: d\n")
            append("   data: [ $k1, $k2, $p1, $p2, $k3 ]\n")
            if (cameraLockConfig != null) {
                append("locked_camera_settings:\n")
                append("   focus_distance_diopters: ${cameraLockConfig.focusDistance}\n")
                append("   exposure_time_ns: ${cameraLockConfig.exposureTimeNs}\n")
                append("   iso_sensitivity: ${cameraLockConfig.sensitivityIso}\n")
                append("   r_gain: ${cameraLockConfig.rGain}\n")
                append("   b_gain: ${cameraLockConfig.bGain}\n")
            }
        }
    }

    companion object {
        fun fromJson(jsonStr: String): CalibrationResult? {
            return try {
                val obj = JSONObject(jsonStr)
                val errs = mutableListOf<Double>()
                val errArr = obj.optJSONArray("perViewErrors")
                if (errArr != null) {
                    for (i in 0 until errArr.length()) {
                        errs.add(errArr.getDouble(i))
                    }
                }
                val lockConfig = obj.optJSONObject("cameraLockConfig")?.let {
                    CameraLockConfig.fromJson(it.toString())
                }

                CalibrationResult(
                    fx = obj.getDouble("fx"),
                    fy = obj.getDouble("fy"),
                    cx = obj.getDouble("cx"),
                    cy = obj.getDouble("cy"),
                    k1 = obj.optDouble("k1", 0.0),
                    k2 = obj.optDouble("k2", 0.0),
                    p1 = obj.optDouble("p1", 0.0),
                    p2 = obj.optDouble("p2", 0.0),
                    k3 = obj.optDouble("k3", 0.0),
                    reprojectionError = obj.optDouble("reprojectionError", 0.0),
                    imageWidth = obj.optInt("imageWidth", 1920),
                    imageHeight = obj.optInt("imageHeight", 1080),
                    imageCount = obj.optInt("imageCount", 30),
                    perViewErrors = errs,
                    dateTimestamp = obj.optLong("dateTimestamp", System.currentTimeMillis()),
                    sampleImagePath = obj.optString("sampleImagePath", null.toString()).takeIf { it != "null" },
                    sampleUndistortedPath = obj.optString("sampleUndistortedPath", null.toString()).takeIf { it != "null" },
                    cameraLockConfig = lockConfig
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

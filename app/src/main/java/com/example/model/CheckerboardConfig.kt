package com.example.model

import org.json.JSONObject

/**
 * Checkerboard calibration target dimensions, corner configuration, and capture goals.
 */
data class CheckerboardConfig(
    val patternCols: Int = 9,         // Inner corner columns
    val patternRows: Int = 6,         // Inner corner rows
    val squareSizeMm: Float = 25.0f,  // Physical square size in millimeters
    val targetImageCount: Int = 15    // User-selected number of images to capture (e.g. 10, 15, 20, 25, 30)
) {
    val totalCorners: Int get() = patternCols * patternRows

    fun toJson(): String {
        val obj = JSONObject()
        obj.put("patternCols", patternCols)
        obj.put("patternRows", patternRows)
        obj.put("squareSizeMm", squareSizeMm.toDouble())
        obj.put("targetImageCount", targetImageCount)
        return obj.toString(2)
    }

    companion object {
        fun fromJson(jsonStr: String): CheckerboardConfig {
            return try {
                val obj = JSONObject(jsonStr)
                CheckerboardConfig(
                    patternCols = obj.optInt("patternCols", 9),
                    patternRows = obj.optInt("patternRows", 6),
                    squareSizeMm = obj.optDouble("squareSizeMm", 25.0).toFloat(),
                    targetImageCount = obj.optInt("targetImageCount", 15)
                )
            } catch (e: Exception) {
                CheckerboardConfig()
            }
        }
    }
}

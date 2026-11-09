package com.example.model

import android.graphics.Bitmap

/**
 * Result of stereo depth estimation from two calibrated views.
 */
data class DepthResult(
    val leftImage: Bitmap,
    val rightImage: Bitmap,
    val colorizedDepthMap: Bitmap,
    val depthMapMeters: Array<FloatArray>, // [row][col] metric depth in meters
    val width: Int,
    val height: Int,
    val minDepthMeters: Float,
    val maxDepthMeters: Float,
    val baselineMeters: Float,
    val pointCloudPath: String? = null,
    val computationTimeMs: Long = 0L
) {
    /**
     * Look up measured metric depth at pixel coordinate (x, y) relative to left image.
     */
    fun getDepthAt(normX: Float, normY: Float): Float? {
        val col = (normX * width).toInt().coerceIn(0, width - 1)
        val row = (normY * height).toInt().coerceIn(0, height - 1)
        val d = depthMapMeters[row][col]
        return if (d > 0.05f && d < 20.0f) d else null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DepthResult
        return width == other.width && height == other.height && computationTimeMs == other.computationTimeMs
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + computationTimeMs.hashCode()
        return result
    }
}

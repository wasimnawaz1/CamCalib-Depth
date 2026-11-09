package com.example.model

import org.opencv.core.MatOfPoint2f
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Poses needed for high-quality camera calibration coverage.
 * A diverse set of board rotations, tilts, and distances ensures accurate
 * estimation of both focal lengths and radial/tangential distortion parameters.
 */
enum class CalibrationPose(
    val title: String,
    val description: String,
    val iconName: String
) {
    FRONT("Front", "Facing camera directly at center", "crop_free"),
    LEFT_30("Left 30°", "Tilted left towards camera", "rotate_left"),
    RIGHT_30("Right 30°", "Tilted right towards camera", "rotate_right"),
    TOP("Top Tilt", "Angled upwards towards top edge", "arrow_upward"),
    BOTTOM("Bottom Tilt", "Angled downwards towards bottom edge", "arrow_downward"),
    TILTED("Rotated 45°", "Rotated diagonally in plane", "change_circle"),
    NEAR("Close Up", "Board fills ~50% of the screen", "zoom_in"),
    FAR("Far Away", "Board at distance (~25% of screen)", "zoom_out")
}

data class PoseDiversityState(
    val coveredPoses: Set<CalibrationPose> = emptySet(),
    val totalCaptured: Int = 0,
    val targetCount: Int = 35
) {
    val progress: Float get() = (totalCaptured.toFloat() / targetCount.toFloat()).coerceIn(0f, 1f)
    val diversityScore: Float get() = (coveredPoses.size.toFloat() / CalibrationPose.entries.size.toFloat())
    val isReadyForCalibration: Boolean get() = totalCaptured >= 15 // minimum viable, 30-35 recommended
}

object PoseClassifier {
    /**
     * Estimates board orientation relative to image frame from detected corner points.
     */
    fun classifyPose(corners: MatOfPoint2f, patternCols: Int, patternRows: Int, frameWidth: Int, frameHeight: Int): CalibrationPose {
        val points = corners.toArray()
        if (points.size < patternCols * patternRows) return CalibrationPose.FRONT

        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE

        for (pt in points) {
            val x = pt.x.toFloat()
            val y = pt.y.toFloat()
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }

        val boxWidth = maxX - minX
        val boxHeight = maxY - minY
        val boardArea = boxWidth * boxHeight
        val frameArea = frameWidth.toFloat() * frameHeight.toFloat()
        val areaRatio = boardArea / frameArea

        // Distance checks
        if (areaRatio > 0.35f) return CalibrationPose.NEAR
        if (areaRatio < 0.12f) return CalibrationPose.FAR

        // Corner extrema: top-left, top-right, bottom-left, bottom-right
        val topLeft = points[0]
        val topRight = points[patternCols - 1]
        val bottomLeft = points[(patternRows - 1) * patternCols]
        val bottomRight = points[points.size - 1]

        val leftHeight = abs(bottomLeft.y - topLeft.y).toFloat()
        val rightHeight = abs(bottomRight.y - topRight.y).toFloat()
        val topWidth = abs(topRight.x - topLeft.x).toFloat()
        val bottomWidth = abs(bottomRight.x - bottomLeft.x).toFloat()

        // In-plane rotation angle
        val angleRad = atan2(topRight.y - topLeft.y, topRight.x - topLeft.x)
        val angleDeg = Math.toDegrees(angleRad).toFloat()

        if (abs(angleDeg) > 20f && abs(angleDeg) < 70f) {
            return CalibrationPose.TILTED
        }

        // Perspective foreshortening
        val horizontalRatio = if (rightHeight > 0f) leftHeight / rightHeight else 1f
        val verticalRatio = if (bottomWidth > 0f) topWidth / bottomWidth else 1f

        return when {
            horizontalRatio > 1.25f -> CalibrationPose.LEFT_30
            horizontalRatio < 0.80f -> CalibrationPose.RIGHT_30
            verticalRatio > 1.25f -> CalibrationPose.TOP
            verticalRatio < 0.80f -> CalibrationPose.BOTTOM
            else -> CalibrationPose.FRONT
        }
    }
}

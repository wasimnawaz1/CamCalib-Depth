package com.example.cv

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.example.model.CalibrationResult
import com.example.model.DepthResult
import org.opencv.android.Utils
import org.opencv.calib3d.Calib3d
import org.opencv.calib3d.StereoSGBM
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfDouble
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.features2d.BFMatcher
import org.opencv.features2d.ORB
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileWriter
import kotlin.math.abs

class DepthEstimator {

    companion object {
        private const val TAG = "DepthEstimator"
    }

    /**
     * Computes dense metric depth map from two calibrated viewpoints.
     *
     * @param leftBitmap Image captured at left station
     * @param rightBitmap Image captured at right station (~10cm baseline translation)
     * @param calib Pre-computed intrinsic calibration parameters
     * @param baselineMeters Physical baseline distance in meters (e.g., 0.10m = 10cm)
     * @param outputPlyFile Optional destination for exported 3D point cloud
     */
    fun computeTwoViewDepth(
        leftBitmap: Bitmap,
        rightBitmap: Bitmap,
        calib: CalibrationResult,
        baselineMeters: Float = 0.10f,
        outputPlyFile: File? = null
    ): DepthResult {
        val startTime = System.currentTimeMillis()

        // 1. Convert bitmaps to OpenCV Mats
        val leftMat = Mat()
        val rightMat = Mat()
        Utils.bitmapToMat(leftBitmap, leftMat)
        Utils.bitmapToMat(rightBitmap, rightMat)

        // 2. Undistort both views using camera calibration parameters
        val cameraMatrix = Mat.eye(3, 3, CvType.CV_64F).apply {
            put(0, 0, calib.fx)
            put(0, 2, calib.cx)
            put(1, 1, calib.fy)
            put(1, 2, calib.cy)
            put(2, 2, 1.0)
        }
        val distCoeffs = MatOfDouble(calib.k1, calib.k2, calib.p1, calib.p2, calib.k3)

        val leftUndist = Mat()
        val rightUndist = Mat()
        Calib3d.undistort(leftMat, leftUndist, cameraMatrix, distCoeffs)
        Calib3d.undistort(rightMat, rightUndist, cameraMatrix, distCoeffs)

        // Downscale slightly for fast and robust mobile stereo matching
        val targetWidth = 640
        val scale = targetWidth.toDouble() / leftUndist.cols().toDouble()
        val targetHeight = (leftUndist.rows() * scale).toInt()

        val leftResized = Mat()
        val rightResized = Mat()
        Imgproc.resize(leftUndist, leftResized, Size(targetWidth.toDouble(), targetHeight.toDouble()))
        Imgproc.resize(rightUndist, rightResized, Size(targetWidth.toDouble(), targetHeight.toDouble()))

        val scaledFx = calib.fx * scale
        val scaledFy = calib.fy * scale
        val scaledCx = calib.cx * scale
        val scaledCy = calib.cy * scale

        val leftGray = Mat()
        val rightGray = Mat()
        Imgproc.cvtColor(leftResized, leftGray, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.cvtColor(rightResized, rightGray, Imgproc.COLOR_RGBA2GRAY)

        // 3. Feature matching for relative orientation & epipolar consistency
        val orb = ORB.create(1000)
        val kp1 = MatOfKeyPoint()
        val kp2 = MatOfKeyPoint()
        val desc1 = Mat()
        val desc2 = Mat()

        orb.detectAndCompute(leftGray, Mat(), kp1, desc1)
        orb.detectAndCompute(rightGray, Mat(), kp2, desc2)

        val matcher = BFMatcher.create(BFMatcher.BRUTEFORCE_HAMMING, true)
        val matches = MatOfDMatch()
        if (!desc1.empty() && !desc2.empty()) {
            matcher.match(desc1, desc2, matches)
        }

        // 4. Dense disparity computation using StereoSGBM
        val minDisparity = 0
        val numDisparities = 64 // Must be divisible by 16
        val blockSize = 5
        val p1 = 8 * 1 * blockSize * blockSize
        val p2 = 32 * 1 * blockSize * blockSize
        val disp12MaxDiff = 1
        val preFilterCap = 31
        val uniquenessRatio = 10
        val speckleWindowSize = 100
        val speckleRange = 32

        val sgbm = StereoSGBM.create(
            minDisparity,
            numDisparities,
            blockSize,
            p1,
            p2,
            disp12MaxDiff,
            preFilterCap,
            uniquenessRatio,
            speckleWindowSize,
            speckleRange,
            StereoSGBM.MODE_SGBM_3WAY
        )

        val disparity16S = Mat()
        sgbm.compute(leftGray, rightGray, disparity16S)

        // 5. Convert disparity to metric depth (Z = fx * baseline / disparity)
        val depthMeters = Array(targetHeight) { FloatArray(targetWidth) { 0f } }
        val depthNormMat = Mat.zeros(targetHeight, targetWidth, CvType.CV_8UC1)

        val minAllowedDepth = 0.2f // 20 cm
        val maxAllowedDepth = 8.0f // 8 meters
        var globalMinDepth = Float.MAX_VALUE
        var globalMaxDepth = Float.MIN_VALUE

        val dispData = ShortArray(targetWidth * targetHeight)
        disparity16S.get(0, 0, dispData)

        // First pass: Calculate depth in meters
        for (y in 0 until targetHeight) {
            for (x in 0 until targetWidth) {
                val idx = y * targetWidth + x
                val rawDisp = dispData[idx]
                val dispVal = rawDisp / 16.0f

                if (dispVal > 0.5f) {
                    val z = ((scaledFx * baselineMeters) / dispVal).toFloat()
                    if (z in minAllowedDepth..maxAllowedDepth) {
                        depthMeters[y][x] = z
                        if (z < globalMinDepth) globalMinDepth = z
                        if (z > globalMaxDepth) globalMaxDepth = z
                    }
                }
            }
        }

        if (globalMinDepth == Float.MAX_VALUE) {
            globalMinDepth = 0.5f
            globalMaxDepth = 3.0f
        }

        // Second pass: Normalize valid depths to 0-255 for color mapping
        val depthNormBytes = ByteArray(targetWidth * targetHeight)
        val depthRange = (globalMaxDepth - globalMinDepth).coerceAtLeast(0.1f)

        for (y in 0 until targetHeight) {
            for (x in 0 until targetWidth) {
                val z = depthMeters[y][x]
                val idx = y * targetWidth + x
                if (z > 0f) {
                    // Close objects get higher value (warm red), far objects get low (cool blue)
                    val normVal = ((1.0f - (z - globalMinDepth) / depthRange) * 255.0f).toInt().coerceIn(1, 255)
                    depthNormBytes[idx] = normVal.toByte()
                } else {
                    depthNormBytes[idx] = 0 // Invalid / unmeasured
                }
            }
        }
        depthNormMat.put(0, 0, depthNormBytes)

        // 6. Apply Jet colormap to depth
        val colorDepthMat = Mat()
        Imgproc.applyColorMap(depthNormMat, colorDepthMat, Imgproc.COLORMAP_JET)

        // Make unmeasured pixels dark
        val blackScalar = Scalar(18.0, 18.0, 24.0)
        for (y in 0 until targetHeight) {
            for (x in 0 until targetWidth) {
                if (depthMeters[y][x] == 0f) {
                    colorDepthMat.put(y, x, 18.0, 18.0, 24.0)
                }
            }
        }

        // Convert Mats back to Android Bitmaps
        val depthBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(colorDepthMat, depthBitmap)

        val undistortedLeftBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(leftResized, undistortedLeftBitmap)

        // 7. Generate 3D point cloud (.ply) file if requested
        var savedPlyPath: String? = null
        if (outputPlyFile != null) {
            savedPlyPath = exportPointCloudPly(
                depthMeters,
                leftResized,
                scaledFx,
                scaledFy,
                scaledCx,
                scaledCy,
                outputPlyFile
            )
        }

        val elapsed = System.currentTimeMillis() - startTime

        // Cleanup native Mats
        leftMat.release()
        rightMat.release()
        leftUndist.release()
        rightUndist.release()
        leftResized.release()
        rightResized.release()
        leftGray.release()
        rightGray.release()
        kp1.release()
        kp2.release()
        desc1.release()
        desc2.release()
        matches.release()
        disparity16S.release()
        depthNormMat.release()
        colorDepthMat.release()
        cameraMatrix.release()
        distCoeffs.release()

        return DepthResult(
            leftImage = undistortedLeftBitmap,
            rightImage = rightBitmap,
            colorizedDepthMap = depthBitmap,
            depthMapMeters = depthMeters,
            width = targetWidth,
            height = targetHeight,
            minDepthMeters = globalMinDepth,
            maxDepthMeters = globalMaxDepth,
            baselineMeters = baselineMeters,
            pointCloudPath = savedPlyPath,
            computationTimeMs = elapsed
        )
    }

    /**
     * Exports 3D points (X, Y, Z) and RGB vertex colors to standard ASCII Stanford PLY file.
     */
    private fun exportPointCloudPly(
        depthGrid: Array<FloatArray>,
        colorMat: Mat,
        fx: Double,
        fy: Double,
        cx: Double,
        cy: Double,
        outFile: File
    ): String {
        val rows = depthGrid.size
        val cols = depthGrid[0].size

        var validVertexCount = 0
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                if (depthGrid[y][x] > 0f) validVertexCount++
            }
        }

        val colorBytes = ByteArray(cols * rows * 4)
        colorMat.get(0, 0, colorBytes)

        FileWriter(outFile).use { writer ->
            writer.write("ply\n")
            writer.write("format ascii 1.0\n")
            writer.write("comment Camera Calibrated Depth Point Cloud\n")
            writer.write("element vertex $validVertexCount\n")
            writer.write("property float x\n")
            writer.write("property float y\n")
            writer.write("property float z\n")
            writer.write("property uchar red\n")
            writer.write("property uchar green\n")
            writer.write("property uchar blue\n")
            writer.write("end_header\n")

            val step = 2 // sample every 2 pixels for fast mobile export
            for (y in 0 until rows step step) {
                for (x in 0 until cols step step) {
                    val z = depthGrid[y][x]
                    if (z > 0f) {
                        val realX = ((x - cx) * z / fx).toFloat()
                        val realY = ((y - cy) * z / fy).toFloat()

                        val idx = (y * cols + x) * 4
                        val r = colorBytes[idx].toInt() and 0xFF
                        val g = colorBytes[idx + 1].toInt() and 0xFF
                        val b = colorBytes[idx + 2].toInt() and 0xFF

                        writer.write(String.format(java.util.Locale.US, "%.4f %.4f %.4f %d %d %d\n", realX, -realY, z, r, g, b))
                    }
                }
            }
        }
        return outFile.absolutePath
    }
}

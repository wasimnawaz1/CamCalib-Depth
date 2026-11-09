package com.example.cv

import android.graphics.Bitmap
import android.util.Log
import com.example.model.CalibrationPose
import com.example.model.CalibrationResult
import com.example.model.CameraLockConfig
import com.example.model.CheckerboardConfig
import com.example.model.PoseClassifier
import org.opencv.android.Utils
import org.opencv.calib3d.Calib3d
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.MatOfPoint2f
import org.opencv.core.MatOfPoint3f
import org.opencv.core.Point3
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.core.TermCriteria
import org.opencv.imgproc.Imgproc
import kotlin.math.sqrt

class CalibrationManager {

    companion object {
        private const val TAG = "CalibrationManager"
        const val BLUR_THRESHOLD = 90.0 // Laplacian variance threshold
    }

    data class DetectionResult(
        val isFound: Boolean,
        val corners: MatOfPoint2f? = null,
        val blurScore: Double = 0.0,
        val isSharp: Boolean = false,
        val detectedPose: CalibrationPose = CalibrationPose.FRONT,
        val debugBitmap: Bitmap? = null
    )

    data class CapturedFrame(
        val id: Int,
        val bitmap: Bitmap,
        val corners: MatOfPoint2f,
        val pose: CalibrationPose,
        val blurScore: Double,
        var reprojectionError: Double = 0.0
    )

    /**
     * Computes variance of Laplacian on grayscale image to estimate sharpness.
     */
    fun computeLaplacianBlurScore(grayMat: Mat): Double {
        val laplacian = Mat()
        Imgproc.Laplacian(grayMat, laplacian, CvType.CV_64F)
        val mean = MatOfDouble()
        val stddev = MatOfDouble()
        Core.meanStdDev(laplacian, mean, stddev)
        val sd = stddev.toArray()[0]
        val variance = sd * sd
        laplacian.release()
        return variance
    }

    /**
     * Detects checkerboard corners on an Android Bitmap and measures image quality.
     */
    fun detectCorners(
        bitmap: Bitmap,
        config: CheckerboardConfig,
        generateDebugBitmap: Boolean = true
    ): DetectionResult {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)

        val blurScore = computeLaplacianBlurScore(gray)
        val isSharp = blurScore >= BLUR_THRESHOLD

        val patternSize = Size(config.patternCols.toDouble(), config.patternRows.toDouble())
        val corners = MatOfPoint2f()

        val flags = Calib3d.CALIB_CB_ADAPTIVE_THRESH or
                Calib3d.CALIB_CB_NORMALIZE_IMAGE or
                Calib3d.CALIB_CB_FAST_CHECK

        val isFound = Calib3d.findChessboardCorners(gray, patternSize, corners, flags)

        var detectedPose = CalibrationPose.FRONT

        if (isFound) {
            // Sub-pixel refinement
            val termCriteria = TermCriteria(
                TermCriteria.EPS or TermCriteria.MAX_ITER,
                30,
                0.01
            )
            Imgproc.cornerSubPix(gray, corners, Size(11.0, 11.0), Size(-1.0, -1.0), termCriteria)

            detectedPose = PoseClassifier.classifyPose(
                corners,
                config.patternCols,
                config.patternRows,
                bitmap.width,
                bitmap.height
            )
        }

        var debugBitmap: Bitmap? = null
        if (generateDebugBitmap) {
            val drawMat = mat.clone()
            if (isFound) {
                Calib3d.drawChessboardCorners(drawMat, patternSize, corners, true)
            }
            debugBitmap = Bitmap.createBitmap(drawMat.cols(), drawMat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(drawMat, debugBitmap)
            drawMat.release()
        }

        mat.release()
        gray.release()

        return DetectionResult(
            isFound = isFound,
            corners = if (isFound) corners else null,
            blurScore = blurScore,
            isSharp = isSharp,
            detectedPose = detectedPose,
            debugBitmap = debugBitmap
        )
    }

    /**
     * Executes camera calibration across all captured frames.
     * Computes Intrinsic Matrix, Distortion Coefficients, and Reprojection Errors.
     */
    fun runCalibration(
        capturedFrames: List<CapturedFrame>,
        config: CheckerboardConfig,
        lockConfig: CameraLockConfig?,
        onProgress: (Float) -> Unit = {}
    ): CalibrationResult {
        require(capturedFrames.size >= 6) { "At least 6 checkerboard frames required for calibration (10-25 recommended)" }

        val imageSize = Size(
            capturedFrames.first().bitmap.width.toDouble(),
            capturedFrames.first().bitmap.height.toDouble()
        )

        // Generate 3D coordinates for checkerboard in board coordinate space (Z = 0)
        val objectPointList = mutableListOf<Point3>()
        for (i in 0 until config.patternRows) {
            for (j in 0 until config.patternCols) {
                objectPointList.add(
                    Point3(
                        (j * config.squareSizeMm).toDouble(),
                        (i * config.squareSizeMm).toDouble(),
                        0.0
                    )
                )
            }
        }
        val singleObjMat = MatOfPoint3f()
        singleObjMat.fromList(objectPointList)

        val objectPoints = mutableListOf<Mat>()
        val imagePoints = mutableListOf<Mat>()

        for (frame in capturedFrames) {
            objectPoints.add(singleObjMat)
            imagePoints.add(frame.corners)
        }

        val cameraMatrix = Mat.eye(3, 3, CvType.CV_64F)
        val distCoeffs = Mat.zeros(5, 1, CvType.CV_64F)
        val rvecs = mutableListOf<Mat>()
        val tvecs = mutableListOf<Mat>()

        onProgress(0.3f)

        // Calibrate
        val calibFlags = 0 // standard pinhole model with k1, k2, p1, p2, k3
        Calib3d.calibrateCamera(
            objectPoints,
            imagePoints,
            imageSize,
            cameraMatrix,
            distCoeffs,
            rvecs,
            tvecs,
            calibFlags
        )

        onProgress(0.7f)

        // Calculate individual and total reprojection errors
        val perViewErrors = mutableListOf<Double>()
        var totalError = 0.0
        var totalPoints = 0

        for (i in capturedFrames.indices) {
            val projPoints = MatOfPoint2f()
            Calib3d.projectPoints(
                objectPoints[i] as MatOfPoint3f,
                rvecs[i],
                tvecs[i],
                cameraMatrix,
                distCoeffs as MatOfDouble,
                projPoints
            )

            val detected = (imagePoints[i] as MatOfPoint2f).toArray()
            val projected = projPoints.toArray()

            var viewErrSum = 0.0
            for (p in detected.indices) {
                val dx = detected[p].x - projected[p].x
                val dy = detected[p].y - projected[p].y
                viewErrSum += sqrt(dx * dx + dy * dy)
            }

            val viewMeanErr = viewErrSum / detected.size
            perViewErrors.add(viewMeanErr)
            capturedFrames[i].reprojectionError = viewMeanErr

            totalError += viewErrSum
            totalPoints += detected.size

            projPoints.release()
        }

        val meanReprojectionError = if (totalPoints > 0) totalError / totalPoints else 0.0

        val fx = cameraMatrix.get(0, 0)[0]
        val fy = cameraMatrix.get(1, 1)[0]
        val cx = cameraMatrix.get(0, 2)[0]
        val cy = cameraMatrix.get(1, 2)[0]

        val k1 = distCoeffs.get(0, 0)[0]
        val k2 = distCoeffs.get(1, 0)[0]
        val p1 = distCoeffs.get(2, 0)[0]
        val p2 = distCoeffs.get(3, 0)[0]
        val k3 = if (distCoeffs.rows() > 4) distCoeffs.get(4, 0)[0] else 0.0

        onProgress(1.0f)

        return CalibrationResult(
            fx = fx,
            fy = fy,
            cx = cx,
            cy = cy,
            k1 = k1,
            k2 = k2,
            p1 = p1,
            p2 = p2,
            k3 = k3,
            reprojectionError = meanReprojectionError,
            imageWidth = imageSize.width.toInt(),
            imageHeight = imageSize.height.toInt(),
            imageCount = capturedFrames.size,
            perViewErrors = perViewErrors,
            cameraLockConfig = lockConfig
        )
    }

    /**
     * Undistorts an image using calibrated intrinsics and distortion coefficients.
     */
    fun undistortBitmap(
        srcBitmap: Bitmap,
        calib: CalibrationResult
    ): Bitmap {
        val srcMat = Mat()
        Utils.bitmapToMat(srcBitmap, srcMat)

        val cameraMatrix = Mat.eye(3, 3, CvType.CV_64F).apply {
            put(0, 0, calib.fx)
            put(0, 2, calib.cx)
            put(1, 1, calib.fy)
            put(1, 2, calib.cy)
            put(2, 2, 1.0)
        }

        val distCoeffs = MatOfDouble(calib.k1, calib.k2, calib.p1, calib.p2, calib.k3)
        val dstMat = Mat()

        Calib3d.undistort(srcMat, dstMat, cameraMatrix, distCoeffs)

        val outBitmap = Bitmap.createBitmap(dstMat.cols(), dstMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(dstMat, outBitmap)

        srcMat.release()
        dstMat.release()
        cameraMatrix.release()
        distCoeffs.release()

        return outBitmap
    }
}

package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.model.CalibrationResult
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class AiCalibrationAssistant {

    companion object {
        private const val TAG = "AiCalibAssistant"
    }

    private val model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    /**
     * Uses Gemini 3.8 Flash to evaluate optical calibration parameters and provide
     * expert computer vision diagnostic insights.
     */
    suspend fun analyzeCalibrationParameters(calib: CalibrationResult): String {
        return withContext(Dispatchers.Default) {
            try {
                val fovX = 2 * Math.toDegrees(Math.atan((calib.imageWidth / 2.0) / calib.fx))
                val fovY = 2 * Math.toDegrees(Math.atan((calib.imageHeight / 2.0) / calib.fy))
                val opticalCenterXOffset = calib.cx - (calib.imageWidth / 2.0)
                val opticalCenterYOffset = calib.cy - (calib.imageHeight / 2.0)
                val focalRatio = calib.fx / calib.fy

                val prompt = """
                    You are an expert Optical Computer Vision and Photogrammetry AI assistant.
                    Analyze this smartphone camera calibration computed with OpenCV:
                    
                    - Focal Length: fx = ${calib.fx} px, fy = ${calib.fy} px (Ratio fx/fy = $focalRatio)
                    - Optical Center (cx, cy): (${calib.cx}, ${calib.cy}) in ${calib.imageWidth}x${calib.imageHeight} frame
                    - Optical Center Offset from geometric center: Δx = $opticalCenterXOffset px, Δy = $opticalCenterYOffset px
                    - Field of View: %.1f° H × %.1f° V
                    - Lens Distortion: k1 = ${calib.k1}, k2 = ${calib.k2}, p1 = ${calib.p1}, p2 = ${calib.p2}, k3 = ${calib.k3}
                    - Mean Reprojection Error: ${calib.reprojectionError} pixels
                    - Images Used: ${calib.imageCount}
                    - Locked Hardware: Focus=${calib.cameraLockConfig?.focusDistance ?: 1.5} diopters, ISO=${calib.cameraLockConfig?.sensitivityIso ?: 200}
                    
                    Provide a concise, professional assessment:
                    1. Quality Verdict (Rating: Excellent/Good/Needs Improvement and why)
                    2. Lens Characteristics (Barrel vs Pincushion distortion severity, sensor alignment)
                    3. Stereo Depth Suitability (Expected accuracy for baseline triangulation)
                    4. Practical Advice for achieving millimeter precision in real-world measurements
                    Keep the response formatted cleanly with bullet points and bold headers.
                """.trimIndent().format(java.util.Locale.US, fovX, fovY)

                withTimeout(60_000L) {
                    val response = model.generateContent(prompt)
                    response.text ?: "No diagnostic assessment returned by AI model."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Calibration analysis failed", e)
                "AI Diagnostic unavailable: ${e.localizedMessage}. Verify network connection."
            }
        }
    }

    /**
     * Multimodal scene depth analysis: Evaluates scene image + stereo disparity,
     * identifying objects, expected scale, and recommending optimal baseline and lighting.
     */
    suspend fun analyzeDepthScene(
        sceneBitmap: Bitmap,
        measuredCenterDepthMeters: Float?,
        baselineMeters: Float
    ): String {
        return withContext(Dispatchers.Default) {
            try {
                val depthText = if (measuredCenterDepthMeters != null) {
                    "Measured center object depth: %.2f meters (%.0f cm)".format(measuredCenterDepthMeters, measuredCenterDepthMeters * 100f)
                } else {
                    "Center object depth is currently unmeasured or occluded"
                }

                val promptContent = content {
                    image(sceneBitmap)
                    text("""
                        You are a 3D Computer Vision and Spatial AI engineer.
                        I captured this scene using a calibrated stereo camera with a baseline displacement of ${baselineMeters * 100} cm.
                        $depthText.
                        
                        Analyze this scene image:
                        1. Identify the primary objects in the foreground, midground, and background.
                        2. Estimate the relative physical distance and depth gradient of key elements.
                        3. Review whether a ${baselineMeters * 100} cm baseline is suitable for this scene scale (and suggest if a wider/narrower baseline is recommended).
                        4. Provide practical tips to avoid stereo matching noise (e.g., textureless surfaces, reflective glare, repetitive patterns).
                        
                        Format with concise bullet points and bold highlights.
                    """.trimIndent())
                }

                withTimeout(60_000L) {
                    val response = model.generateContent(promptContent)
                    response.text ?: "No scene depth analysis returned by AI model."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Multimodal depth analysis failed", e)
                "AI Scene Analysis unavailable: ${e.localizedMessage}. Check network connection."
            }
        }
    }

    /**
     * Ask any calibration or computer vision question.
     */
    suspend fun askAssistant(question: String): String {
        return withContext(Dispatchers.Default) {
            try {
                val prompt = """
                    You are CamCalib AI, an expert assistant for Android camera calibration (OpenCV) and stereo depth estimation.
                    Answer the user's question with practical, scientifically accurate guidance:
                    
                    User Question: "$question"
                    
                    Give a helpful, clear, and direct explanation.
                """.trimIndent()

                withTimeout(60_000L) {
                    val response = model.generateContent(prompt)
                    response.text ?: "No response generated."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Assistant error", e)
                "Assistant error: ${e.localizedMessage}"
            }
        }
    }
}

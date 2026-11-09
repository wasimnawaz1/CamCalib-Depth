package com.example.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import com.example.model.CameraLockConfig

class Camera2Manager(
    private val context: Context,
    val lockManager: CameraLockManager
) {
    companion object {
        private const val TAG = "Camera2Manager"
    }

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var activeCameraId: String? = null
    var characteristics: CameraCharacteristics? = null
        private set

    var onParametersLocked: ((CameraLockConfig) -> Unit)? = null
    private var isPendingAutoLock = false

    private fun startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("CameraBackground").apply { start() }
            backgroundHandler = Handler(backgroundThread!!.looper)
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background thread", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun openCamera(textureView: TextureView, onReady: () -> Unit = {}) {
        startBackgroundThread()

        try {
            // Find back-facing camera
            for (id in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    activeCameraId = id
                    characteristics = chars
                    break
                }
            }

            if (activeCameraId == null && cameraManager.cameraIdList.isNotEmpty()) {
                activeCameraId = cameraManager.cameraIdList[0]
                characteristics = cameraManager.getCameraCharacteristics(activeCameraId!!)
            }

            val camId = activeCameraId ?: return

            cameraManager.openCamera(camId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    startPreviewSession(textureView, onReady)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera error: $error")
                    camera.close()
                    cameraDevice = null
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to open camera", e)
        }
    }

    private fun startPreviewSession(textureView: TextureView, onReady: () -> Unit) {
        val device = cameraDevice ?: return
        val surfaceTexture = textureView.surfaceTexture ?: return

        try {
            surfaceTexture.setDefaultBufferSize(1920, 1080)
            val surface = Surface(surfaceTexture)

            previewRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
                // Apply manual lock parameters directly to preview
                lockManager.applyLockedParameters(this, characteristics)
            }

            device.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        updateRepeatingRequest()
                        onReady()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "CaptureSession configuration failed")
                    }
                },
                backgroundHandler
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating capture session", e)
        }
    }

    fun updateRepeatingRequest() {
        val session = captureSession ?: return
        val builder = previewRequestBuilder ?: return

        try {
            lockManager.applyLockedParameters(builder, characteristics)
            session.setRepeatingRequest(
                builder.build(),
                object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        super.onCaptureCompleted(session, request, result)
                        if (isPendingAutoLock) {
                            isPendingAutoLock = false
                            val frozen = lockManager.freezeFromCaptureResult(result)
                            // Re-apply immediately in manual mode
                            lockManager.applyLockedParameters(builder, characteristics)
                            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
                            onParametersLocked?.invoke(frozen)
                        }
                    }
                },
                backgroundHandler
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update repeating request", e)
        }
    }

    /**
     * Temporarily enables auto exposure/focus for 1.5s to meter the room, then firmly freezes
     * the exact hardware exposure time, ISO, focus distance, and color gains.
     */
    fun triggerAutoMeterAndLock() {
        val session = captureSession ?: return
        val builder = previewRequestBuilder ?: return

        try {
            // Put in auto mode temporarily
            builder.set(CaptureRequest.CONTROL_AF_MODE, CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            builder.set(CaptureRequest.CONTROL_AE_MODE, CameraCharacteristics.CONTROL_AE_MODE_ON)
            builder.set(CaptureRequest.CONTROL_AE_LOCK, false)
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CameraCharacteristics.CONTROL_AWB_MODE_AUTO)
            builder.set(CaptureRequest.CONTROL_AWB_LOCK, false)

            session.setRepeatingRequest(builder.build(), null, backgroundHandler)

            // After 1.5 seconds, schedule the freeze on capture
            backgroundHandler?.postDelayed({
                isPendingAutoLock = true
            }, 1500)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger auto-meter", e)
        }
    }

    fun applyCustomLock(config: CameraLockConfig) {
        lockManager.updateConfig(config)
        updateRepeatingRequest()
        onParametersLocked?.invoke(config)
    }

    fun closeCamera() {
        try {
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            stopBackgroundThread()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing camera", e)
        }
    }
}

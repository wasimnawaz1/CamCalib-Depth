package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.view.TextureView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.camera.Camera2Manager
import com.example.camera.CameraLockManager
import com.example.cv.CalibrationManager
import com.example.model.CalibrationPose
import com.example.model.CalibrationResult
import com.example.model.CameraLockConfig
import com.example.model.CheckerboardConfig
import com.example.storage.CalibrationStorage
import com.example.ui.components.CameraLockBadge
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.HudBackground
import com.example.ui.theme.RoseError
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CalibrationCaptureScreen(
    checkerboardConfig: CheckerboardConfig,
    cameraLockConfig: CameraLockConfig,
    onCalibrationComplete: (CalibrationResult) -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storage = remember { CalibrationStorage(context) }
    val cvManager = remember { CalibrationManager() }

    val lockManager = remember { CameraLockManager(cameraLockConfig) }
    val cameraManager = remember { Camera2Manager(context, lockManager) }

    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }
    var isCameraReady by remember { mutableStateOf(false) }

    // Visual shutter flash effect
    var isShutterFlashing by remember { mutableStateOf(false) }

    // Status banner state
    var statusMessage by remember { mutableStateOf("Position checkerboard and tap Shutter to capture") }
    var isStatusError by remember { mutableStateOf(false) }
    var lastSavedPath by remember { mutableStateOf<String?>(null) }

    // Captured frames collection
    val capturedFrames = remember { mutableStateListOf<CalibrationManager.CapturedFrame>() }
    val savedFiles = remember { mutableStateListOf<File>() }

    // Calibration processing state
    var isCalibrating by remember { mutableStateOf(false) }
    var isProcessingSnapshot by remember { mutableStateOf(false) }
    var calibProgress by remember { mutableFloatStateOf(0f) }
    var calibErrorMessage by remember { mutableStateOf<String?>(null) }

    var lockState by remember { mutableStateOf(cameraLockConfig) }

    cameraManager.onParametersLocked = { newLock ->
        lockState = newLock
        storage.saveCameraLock(newLock)
    }

    // Load any previously saved calibration images on launch
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val existing = storage.loadCalibrationImageFiles()
            if (existing.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    savedFiles.clear()
                    savedFiles.addAll(existing)
                    statusMessage = "Loaded ${existing.size} saved images from storage"
                }
            }
        }
    }

    // Clean up camera on exit
    DisposableEffect(Unit) {
        onDispose {
            cameraManager.closeCamera()
        }
    }

    val targetCount = checkerboardConfig.targetImageCount

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepVoid)
    ) {
        // 1. Camera2 High-Speed Native Preview
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                            textureViewRef = this@apply
                            cameraManager.openCamera(this@apply) {
                                isCameraReady = true
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            cameraManager.closeCamera()
                            textureViewRef = null
                            isCameraReady = false
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("camera_preview_view")
        )

        // 2. High-Performance Viewfinder Guide Overlay (Zero CPU overhead)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 140.dp, top = 90.dp, start = 28.dp, end = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        2.dp,
                        CyanAccent.copy(alpha = 0.4f),
                        RoundedCornerShape(16.dp)
                    )
            ) {
                // Viewfinder corner marks
                Text(
                    text = "${checkerboardConfig.patternCols}x${checkerboardConfig.patternRows} Checkerboard Guide",
                    color = CyanAccent.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .background(HudBackground, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // 3. Shutter Flash Effect
        AnimatedVisibility(
            visible = isShutterFlashing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.65f))
            )
        }

        // 4. Top Control Bar: Back, Camera Lock Badge, Auto-Meter Lock
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(HudBackground)
                    .size(40.dp)
                    .testTag("capture_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            CameraLockBadge(
                config = lockState,
                onAutoMeterAndLock = {
                    cameraManager.triggerAutoMeterAndLock()
                }
            )

            IconButton(
                onClick = { cameraManager.triggerAutoMeterAndLock() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(HudBackground)
                    .size(40.dp)
                    .testTag("trigger_auto_lock_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Re-Meter & Freeze Light",
                    tint = CyanAccent
                )
            }
        }

        // 5. Status Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 96.dp, start = 20.dp, end = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(HudBackground)
                    .border(
                        1.dp,
                        if (isStatusError) RoseError else CyanAccent.copy(alpha = 0.6f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isStatusError) Icons.Default.Warning else Icons.Default.Done,
                        contentDescription = null,
                        tint = if (isStatusError) RoseError else CyanAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = statusMessage,
                        color = if (isStatusError) RoseError else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 6. Bottom Control Sheet: Thumbnail Gallery, Shutter Button, Calibrate Now Action
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(HudBackground)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Captured Frames Thumbnail Strip
            if (capturedFrames.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    capturedFrames.forEachIndexed { index, frame ->
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        ) {
                            Image(
                                bitmap = frame.bitmap.asImageBitmap(),
                                contentDescription = "Captured Frame $index",
                                modifier = Modifier.fillMaxSize()
                            )
                            // Delete button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                    .clickable {
                                        capturedFrames.removeAt(index)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                            Text(
                                text = "#${index + 1}",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Shutter Button & Calibrate Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Image counter (Current / Target)
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "${capturedFrames.size} / $targetCount",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (capturedFrames.size >= 6) "Ready to calibrate" else "Need ${6 - capturedFrames.size} more",
                        color = if (capturedFrames.size >= 6) EmeraldSuccess else Color.Gray,
                        fontSize = 10.sp
                    )
                }

                // Shutter Button (ALWAYS RESPONSIVE & CLICKABLE)
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(if (isProcessingSnapshot) Color.Gray else CyanAccent)
                        .clickable(enabled = !isProcessingSnapshot && isCameraReady) {
                            val tv = textureViewRef
                            if (tv != null && tv.isAvailable) {
                                val snapBitmap = tv.bitmap
                                if (snapBitmap != null) {
                                    isProcessingSnapshot = true
                                    isShutterFlashing = true

                                    scope.launch {
                                        delay(80)
                                        isShutterFlashing = false
                                    }

                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            // 1. Immediately save image to mobile Android storage
                                            val savedFile = storage.saveCalibrationImage(
                                                snapBitmap,
                                                capturedFrames.size + 1
                                            )

                                            // 2. Perform OpenCV corner detection in background
                                            val detect = cvManager.detectCorners(
                                                snapBitmap,
                                                checkerboardConfig,
                                                generateDebugBitmap = false
                                            )

                                            withContext(Dispatchers.Main) {
                                                isProcessingSnapshot = false
                                                savedFiles.add(savedFile)
                                                lastSavedPath = savedFile.name

                                                if (detect.isFound && detect.corners != null) {
                                                    val frame = CalibrationManager.CapturedFrame(
                                                        id = capturedFrames.size + 1,
                                                        bitmap = snapBitmap,
                                                        corners = detect.corners,
                                                        pose = detect.detectedPose,
                                                        blurScore = detect.blurScore
                                                    )
                                                    capturedFrames.add(frame)
                                                    isStatusError = false
                                                    statusMessage = "Saved #${frame.id}: Sharp 9x6 corners detected (${detect.detectedPose.title})"
                                                } else {
                                                    isStatusError = true
                                                    statusMessage = "Image saved to storage, but checkerboard not detected. Retake with board fully visible."
                                                }
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                isProcessingSnapshot = false
                                                isStatusError = true
                                                statusMessage = "Capture error: ${e.message}"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        .testTag("capture_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(DeepVoid)
                            .border(3.dp, CyanAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isProcessingSnapshot) {
                            CircularProgressIndicator(
                                color = CyanAccent,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = "Capture Image",
                                tint = CyanAccent,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                // Calibrate Button
                val canCalibrate = capturedFrames.size >= 6

                Button(
                    onClick = {
                        isCalibrating = true
                        calibProgress = 0f
                        calibErrorMessage = null

                        scope.launch(Dispatchers.Default) {
                            try {
                                val result = cvManager.runCalibration(
                                    capturedFrames = capturedFrames.toList(),
                                    config = checkerboardConfig,
                                    lockConfig = lockState,
                                    onProgress = { p -> calibProgress = p }
                                )

                                // Save sample images for before/after comparison
                                val sampleFrame = capturedFrames.firstOrNull()?.bitmap
                                val undistortedSample = if (sampleFrame != null) {
                                    cvManager.undistortBitmap(sampleFrame, result)
                                } else null

                                val finalResult = result.copy(
                                    sampleImagePath = sampleFrame?.let { bmp ->
                                        val f = File(storage.getCalibrationDir(), "sample_distorted.png")
                                        java.io.FileOutputStream(f).use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 95, out) }
                                        f.absolutePath
                                    },
                                    sampleUndistortedPath = undistortedSample?.let { bmp ->
                                        val f = File(storage.getCalibrationDir(), "sample_undistorted.png")
                                        java.io.FileOutputStream(f).use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 95, out) }
                                        f.absolutePath
                                    }
                                )

                                storage.saveCalibration(finalResult)

                                withContext(Dispatchers.Main) {
                                    isCalibrating = false
                                    onCalibrationComplete(finalResult)
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isCalibrating = false
                                    calibErrorMessage = e.message ?: "Calibration failed"
                                }
                            }
                        }
                    },
                    enabled = canCalibrate && !isCalibrating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldSuccess,
                        contentColor = DeepVoid,
                        disabledContainerColor = SurfaceElevated,
                        disabledContentColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("calibrate_now_button")
                ) {
                    Text(
                        text = if (capturedFrames.size >= targetCount) "Calibrate Now" else "Calibrate (${capturedFrames.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 7. Processing Modal Overlay
        if (isCalibrating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepVoid.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { calibProgress },
                        color = CyanAccent,
                        strokeWidth = 6.dp,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "Calibrating Camera with OpenCV...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Computing camera intrinsic matrix fx, fy, cx, cy and distortion coefficients",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Error message modal
        calibErrorMessage?.let { err ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RoseError)
                    .padding(16.dp)
            ) {
                Column {
                    Text("Calibration Error", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(err, color = Color.White, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { calibErrorMessage = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = RoseError)
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}

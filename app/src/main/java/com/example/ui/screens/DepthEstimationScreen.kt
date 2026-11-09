package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.cv.DepthEstimator
import com.example.model.CalibrationResult
import com.example.model.CameraLockConfig
import com.example.model.DepthResult
import com.example.storage.CalibrationStorage
import com.example.ui.components.CameraLockBadge
import com.example.ui.components.DepthTapViewer
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class DepthWorkflowStep {
    CAPTURE_LEFT,
    CAPTURE_RIGHT,
    COMPUTING,
    RESULT
}

@Composable
fun DepthEstimationScreen(
    calibrationResult: CalibrationResult,
    cameraLockConfig: CameraLockConfig,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storage = remember { CalibrationStorage(context) }
    val depthEstimator = remember { DepthEstimator() }

    val lockManager = remember { CameraLockManager(cameraLockConfig) }
    val cameraManager = remember { Camera2Manager(context, lockManager) }

    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }
    var isCameraReady by remember { mutableStateOf(false) }

    var currentStep by remember { mutableStateOf(DepthWorkflowStep.CAPTURE_LEFT) }
    var leftBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rightBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var baselineCm by remember { mutableFloatStateOf(storage.loadBaselineMeters() * 100f) }
    var depthResult by remember { mutableStateOf<DepthResult?>(null) }
    var showColorizedDepthMap by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val assistant = remember { com.example.ai.AiCalibrationAssistant() }
    var isAnalyzingScene by remember { mutableStateOf(false) }
    var aiSceneAnalysisResult by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            cameraManager.closeCamera()
        }
    }

    Scaffold(
        containerColor = DeepVoid,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("depth_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Stereo Depth Estimation",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                CameraLockBadge(config = cameraLockConfig)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentStep) {
                DepthWorkflowStep.CAPTURE_LEFT, DepthWorkflowStep.CAPTURE_RIGHT -> {
                    // Camera Preview with step instructions
                    Box(modifier = Modifier.fillMaxSize()) {
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
                                .testTag("depth_camera_preview")
                        )

                        // Top Instruction Box
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .align(Alignment.TopCenter),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val instructionTitle = if (currentStep == DepthWorkflowStep.CAPTURE_LEFT) {
                                "Step 1: Capture Left View"
                            } else {
                                "Step 2: Move Phone ~${baselineCm.toInt()}cm Right"
                            }

                            val instructionSubtitle = if (currentStep == DepthWorkflowStep.CAPTURE_LEFT) {
                                "Frame the target object/scene and tap capture"
                            } else {
                                "Shift camera horizontally right along baseline, then tap capture"
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(HudBackground)
                                    .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (currentStep == DepthWorkflowStep.CAPTURE_LEFT) Icons.Default.Camera else Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = CyanAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = instructionTitle,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = instructionSubtitle,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Ghost overlay of Left Image during Right capture
                            if (currentStep == DepthWorkflowStep.CAPTURE_RIGHT && leftBitmap != null) {
                                Spacer(Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .size(80.dp, 60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(2.dp, CyanAccent, RoundedCornerShape(8.dp))
                                        .align(Alignment.Start)
                                ) {
                                    Image(
                                        bitmap = leftBitmap!!.asImageBitmap(),
                                        contentDescription = "Left View Thumbnail",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Text(
                                        "Left View",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .background(Color.Black.copy(alpha = 0.7f))
                                            .padding(horizontal = 3.dp)
                                    )
                                }
                            }
                        }

                        // Bottom Capture Controls
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(HudBackground)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Baseline Slider
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Stereo Baseline Displacement:", color = TextSecondary, fontSize = 11.sp)
                                    Text("${baselineCm.toInt()} cm", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Slider(
                                    value = baselineCm,
                                    onValueChange = {
                                        baselineCm = it
                                        storage.saveBaselineMeters(it / 100f)
                                    },
                                    valueRange = 5f..25f,
                                    steps = 19,
                                    colors = SliderDefaults.colors(
                                        thumbColor = CyanAccent,
                                        activeTrackColor = CyanAccent,
                                        inactiveTrackColor = SurfaceElevated
                                    )
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            // Shutter Button
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(CyanAccent)
                                    .clickable {
                                        val tv = textureViewRef
                                        if (tv != null && tv.isAvailable) {
                                            val snap = tv.bitmap
                                            if (snap != null) {
                                                if (currentStep == DepthWorkflowStep.CAPTURE_LEFT) {
                                                    leftBitmap = snap
                                                    currentStep = DepthWorkflowStep.CAPTURE_RIGHT
                                                } else {
                                                    rightBitmap = snap
                                                    currentStep = DepthWorkflowStep.COMPUTING
                                                    // Trigger depth estimation
                                                    scope.launch(Dispatchers.Default) {
                                                        try {
                                                            val plyFile = File(storage.getCalibrationDir(), "depth_pointcloud.ply")
                                                            val result = depthEstimator.computeTwoViewDepth(
                                                                leftBitmap = leftBitmap!!,
                                                                rightBitmap = snap,
                                                                calib = calibrationResult,
                                                                baselineMeters = baselineCm / 100f,
                                                                outputPlyFile = plyFile
                                                            )
                                                            withContext(Dispatchers.Main) {
                                                                depthResult = result
                                                                currentStep = DepthWorkflowStep.RESULT
                                                            }
                                                        } catch (e: Exception) {
                                                            withContext(Dispatchers.Main) {
                                                                errorMessage = e.message ?: "Depth estimation failed"
                                                                currentStep = DepthWorkflowStep.CAPTURE_LEFT
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .testTag("depth_shutter_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(58.dp)
                                        .clip(CircleShape)
                                        .background(DeepVoid)
                                        .border(2.dp, CyanAccent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Camera,
                                        contentDescription = "Capture",
                                        tint = CyanAccent,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                DepthWorkflowStep.COMPUTING -> {
                    // Processing Spinner
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DeepVoid),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            CircularProgressIndicator(
                                color = CyanAccent,
                                strokeWidth = 5.dp,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(20.dp))
                            Text(
                                text = "Computing Metric Stereo Depth...",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Applying camera matrix undistortion, feature matching, and StereoSGBM disparity triangulation",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                DepthWorkflowStep.RESULT -> {
                    // Interactive Result Screen
                    depthResult?.let { res ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // View Switch (Colormap vs Original)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (showColorizedDepthMap) "JET DEPTH COLORMAP" else "ORIGINAL UNDISTORTED",
                                    color = CyanAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Jet Map", color = TextSecondary, fontSize = 11.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Switch(
                                        checked = showColorizedDepthMap,
                                        onCheckedChange = { showColorizedDepthMap = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = CyanAccent,
                                            checkedTrackColor = SurfaceElevated
                                        )
                                    )
                                }
                            }

                            // Interactive Tap-to-Measure Viewer
                            DepthTapViewer(
                                depthResult = res,
                                showDepthMap = showColorizedDepthMap
                            )

                            // Point Cloud & Metrics Card
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        res.pointCloudPath?.let { path ->
                                            val plyFile = File(path)
                                            if (plyFile.exists()) {
                                                val intent = storage.createShareIntent(
                                                    file = plyFile,
                                                    mimeType = "text/plain",
                                                    title = "Share 3D Point Cloud (.PLY)"
                                                )
                                                context.startActivity(intent)
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("export_ply_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated, contentColor = CyanAccent)
                                ) {
                                    Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Export 3D .PLY", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        currentStep = DepthWorkflowStep.CAPTURE_LEFT
                                        leftBitmap = null
                                        rightBitmap = null
                                        depthResult = null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("depth_retake_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DeepVoid)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("New Capture", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // AI Scene Analysis Button
                            OutlinedButton(
                                onClick = {
                                    isAnalyzingScene = true
                                    scope.launch {
                                        val centerDepth = res.getDepthAt(0.5f, 0.5f)
                                        aiSceneAnalysisResult = assistant.analyzeDepthScene(
                                            sceneBitmap = res.leftImage,
                                            measuredCenterDepthMeters = centerDepth,
                                            baselineMeters = res.baselineMeters
                                        )
                                        isAnalyzingScene = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ai_scene_analysis_button"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp), tint = CyanAccent)
                                Spacer(Modifier.width(8.dp))
                                Text(if (isAnalyzingScene) "Gemini is analyzing scene..." else "AI Scene & Depth Analysis (Gemini)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            // Stats Grid
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceDark)
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("METRIC STATS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Min Depth Range:", color = TextSecondary, fontSize = 11.sp)
                                        Text(String.format(java.util.Locale.US, "%.2f m", res.minDepthMeters), color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Max Depth Range:", color = TextSecondary, fontSize = 11.sp)
                                        Text(String.format(java.util.Locale.US, "%.2f m", res.maxDepthMeters), color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Stereo Baseline:", color = TextSecondary, fontSize = 11.sp)
                                        Text("${(res.baselineMeters * 100).toInt()} cm", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Computation Time:", color = TextSecondary, fontSize = 11.sp)
                                        Text("${res.computationTimeMs} ms", color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Error Overlay
            errorMessage?.let { err ->
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(RoseError)
                        .padding(16.dp)
                ) {
                    Column {
                        Text("Depth Calculation Error", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(err, color = Color.White, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { errorMessage = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = RoseError)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            // AI Scene Analysis Dialog
            aiSceneAnalysisResult?.let { analysis ->
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { aiSceneAnalysisResult = null },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanAccent)
                            Spacer(Modifier.width(8.dp))
                            Text("Gemini Scene & Depth Analysis", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(
                                text = analysis,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { aiSceneAnalysisResult = null },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DeepVoid)
                        ) {
                            Text("Close")
                        }
                    },
                    containerColor = SurfaceElevated
                )
            }
        }
    }
}

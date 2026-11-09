package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.CalibrationResult
import com.example.model.CameraLockConfig
import com.example.model.CheckerboardConfig
import com.example.storage.CalibrationStorage
import com.example.ui.screens.CalibrationCaptureScreen
import com.example.ui.screens.CalibrationResultScreen
import com.example.ui.screens.DepthEstimationScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PrintCheckerboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextSecondary
import org.opencv.android.OpenCVLoader

enum class AppScreen {
    HOME,
    CALIBRATE_CAPTURE,
    CALIBRATION_RESULT,
    DEPTH_ESTIMATION,
    PRINT_CHECKERBOARD,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before any UI or Firebase operations
        com.example.ai.configureAppCheck(this, intent)

        enableEdgeToEdge()

        // Initialize OpenCV Android native binaries
        try {
            if (OpenCVLoader.initLocal()) {
                Log.i(TAG, "OpenCV native library loaded successfully via initLocal()")
            } else {
                Log.w(TAG, "OpenCVLoader.initLocal() returned false, calling initDebug()")
                OpenCVLoader.initDebug()
            }
        } catch (e: Exception) {
            Log.e(TAG, "OpenCVLoader initialization error, falling back to initDebug()", e)
            OpenCVLoader.initDebug()
        }

        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val context = LocalContext.current
    val storage = remember { CalibrationStorage(context) }

    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var activeCalibration by remember { mutableStateOf(storage.loadCalibration()) }
    var activeLockConfig by remember { mutableStateOf(storage.loadCameraLock()) }
    var activeBoardConfig by remember { mutableStateOf(storage.loadCheckerboardConfig()) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        CameraPermissionDeniedView(
            onRequestPermission = {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        )
        return
    }

    var showUserGuide by remember { mutableStateOf(false) }
    var showAiAssistant by remember { mutableStateOf(false) }

    when (currentScreen) {
        AppScreen.HOME -> {
            HomeScreen(
                calibrationResult = activeCalibration,
                cameraLockConfig = activeLockConfig,
                checkerboardConfig = activeBoardConfig,
                onNavigateToCalibrate = { currentScreen = AppScreen.CALIBRATE_CAPTURE },
                onNavigateToDepth = { currentScreen = AppScreen.DEPTH_ESTIMATION },
                onNavigateToResults = { currentScreen = AppScreen.CALIBRATION_RESULT },
                onNavigateToPrintBoard = { currentScreen = AppScreen.PRINT_CHECKERBOARD },
                onNavigateToSettings = { currentScreen = AppScreen.SETTINGS },
                onOpenUserGuide = { showUserGuide = true },
                onOpenAiAssistant = { showAiAssistant = true }
            )
        }

        AppScreen.CALIBRATE_CAPTURE -> {
            CalibrationCaptureScreen(
                checkerboardConfig = activeBoardConfig,
                cameraLockConfig = activeLockConfig,
                onCalibrationComplete = { result ->
                    activeCalibration = result
                    currentScreen = AppScreen.CALIBRATION_RESULT
                },
                onNavigateBack = { currentScreen = AppScreen.HOME }
            )
        }

        AppScreen.CALIBRATION_RESULT -> {
            val result = activeCalibration
            if (result != null) {
                CalibrationResultScreen(
                    result = result,
                    onNavigateToDepth = { currentScreen = AppScreen.DEPTH_ESTIMATION },
                    onNavigateBack = { currentScreen = AppScreen.HOME },
                    onOpenAiAssistant = { showAiAssistant = true }
                )
            } else {
                currentScreen = AppScreen.HOME
            }
        }

        AppScreen.DEPTH_ESTIMATION -> {
            val result = activeCalibration
            if (result != null) {
                DepthEstimationScreen(
                    calibrationResult = result,
                    cameraLockConfig = activeLockConfig,
                    onNavigateBack = { currentScreen = AppScreen.HOME }
                )
            } else {
                currentScreen = AppScreen.CALIBRATE_CAPTURE
            }
        }

        AppScreen.PRINT_CHECKERBOARD -> {
            PrintCheckerboardScreen(
                config = activeBoardConfig,
                onNavigateBack = { currentScreen = AppScreen.HOME }
            )
        }

        AppScreen.SETTINGS -> {
            SettingsScreen(
                currentBoardConfig = activeBoardConfig,
                currentLockConfig = activeLockConfig,
                onSaveConfigs = { board, lock ->
                    activeBoardConfig = board
                    activeLockConfig = lock
                    storage.saveCheckerboardConfig(board)
                    storage.saveCameraLock(lock)
                },
                onClearCalibration = {
                    storage.clearCalibration()
                    activeCalibration = null
                },
                onNavigateBack = { currentScreen = AppScreen.HOME }
            )
        }
    }

    if (showUserGuide) {
        com.example.ui.components.NewUserGuideDialog(
            onDismiss = { showUserGuide = false },
            onStartCalibration = {
                showUserGuide = false
                currentScreen = AppScreen.CALIBRATE_CAPTURE
            },
            onOpenCheckerboard = {
                showUserGuide = false
                currentScreen = AppScreen.PRINT_CHECKERBOARD
            }
        )
    }

    if (showAiAssistant) {
        com.example.ui.components.AiAssistantDialog(
            calibrationResult = activeCalibration,
            onDismiss = { showAiAssistant = false }
        )
    }
}

@Composable
fun CameraPermissionDeniedView(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepVoid)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = CyanAccent,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Camera Access Required",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "CamCalib requires raw Camera2 hardware access to lock autofocus and exposure for optical calibration and depth estimation.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DeepVoid),
                modifier = Modifier.testTag("grant_camera_permission_button")
            ) {
                Text("Grant Camera Permission", fontWeight = FontWeight.Bold)
            }
        }
    }
}

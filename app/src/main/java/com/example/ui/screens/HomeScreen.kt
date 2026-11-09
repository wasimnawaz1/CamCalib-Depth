package com.example.ui.screens

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalibrationResult
import com.example.model.CameraLockConfig
import com.example.ui.components.CameraLockBadge
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanDim
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    calibrationResult: CalibrationResult?,
    cameraLockConfig: CameraLockConfig,
    checkerboardConfig: com.example.model.CheckerboardConfig = com.example.model.CheckerboardConfig(),
    onNavigateToCalibrate: () -> Unit,
    onNavigateToDepth: () -> Unit,
    onNavigateToResults: () -> Unit,
    onNavigateToPrintBoard: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenUserGuide: () -> Unit,
    onOpenAiAssistant: () -> Unit
) {
    Scaffold(
        containerColor = DeepVoid
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(listOf(CyanAccent, CyanDim))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "App Icon",
                            tint = DeepVoid,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "CamCalib Depth",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "OpenCV Calibration & Stereo Depth",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                CameraLockBadge(config = cameraLockConfig)
            }

            // Status Card
            CalibrationStatusCard(
                result = calibrationResult,
                targetCount = checkerboardConfig.targetImageCount,
                onViewDetails = onNavigateToResults,
                onStartCalibration = onNavigateToCalibrate
            )

            // Primary Navigation Grid
            Text(
                text = "OPERATIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent,
                letterSpacing = 1.sp
            )

            // 1. Calibrate Camera Action
            ActionCard(
                title = "Calibrate Camera",
                subtitle = "Capture ${checkerboardConfig.targetImageCount} checkerboard images with locked AF/AE to compute intrinsic fx, fy, cx, cy & distortion",
                icon = Icons.Default.GridOn,
                accentColor = CyanAccent,
                tag = "home_calibrate_button",
                onClick = onNavigateToCalibrate
            )

            // 2. Depth Estimation Action
            ActionCard(
                title = "Two-View Depth Estimation",
                subtitle = if (calibrationResult != null) {
                    "Measure real metric distance (cm/m) using calibrated stereo baseline and interactive tap-to-measure"
                } else {
                    "Requires calibration first. Tap to calibrate your camera parameters"
                },
                icon = Icons.Default.Straighten,
                accentColor = if (calibrationResult != null) EmeraldSuccess else Color.Gray,
                tag = "home_depth_button",
                onClick = {
                    if (calibrationResult != null) onNavigateToDepth() else onNavigateToCalibrate()
                }
            )

            // 3. View Calibration Parameters (if calibrated)
            if (calibrationResult != null) {
                ActionCard(
                    title = "View Calibration Matrix",
                    subtitle = "Inspect intrinsic parameters, distortion coefficients, undistortion comparison, and export YAML/JSON",
                    icon = Icons.Default.TableChart,
                    accentColor = Color(0xFFB388FF),
                    tag = "home_view_calib_button",
                    onClick = onNavigateToResults
                )
            }

            // Secondary Actions Row: Checkerboard & Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    title = "Checkerboard",
                    caption = "Printable A4 / Screen",
                    icon = Icons.Default.Print,
                    modifier = Modifier.weight(1f),
                    tag = "home_print_pattern_button",
                    onClick = onNavigateToPrintBoard
                )

                QuickActionCard(
                    title = "Settings",
                    caption = "Hardware & Locks",
                    icon = Icons.Default.Settings,
                    modifier = Modifier.weight(1f),
                    tag = "home_settings_button",
                    onClick = onNavigateToSettings
                )
            }

            // AI Capabilities & User Instructions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    title = "AI Assistant",
                    caption = "Gemini 3.8 Flash CV",
                    icon = Icons.Default.AutoAwesome,
                    modifier = Modifier.weight(1f),
                    tag = "home_ai_assistant_button",
                    onClick = onOpenAiAssistant
                )

                QuickActionCard(
                    title = "User Guide",
                    caption = "5-Step Walkthrough",
                    icon = Icons.Default.HelpOutline,
                    modifier = Modifier.weight(1f),
                    tag = "home_user_guide_button",
                    onClick = onOpenUserGuide
                )
            }

            // Guide Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Why Hardware Lock Is Crucial",
                            color = CyanAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Mobile cameras automatically shift focal distance and optical stabilization between frames. This changes the focal length (fx, fy) constantly. CamCalib locks your phone's lens focus, exposure time, sensitivity, and white balance to guarantee mathematically invariant geometry.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CalibrationStatusCard(
    result: CalibrationResult?,
    targetCount: Int = 15,
    onViewDetails: () -> Unit,
    onStartCalibration: () -> Unit
) {
    val isCalibrated = result != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calibration_status_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCalibrated) EmeraldSuccess.copy(alpha = 0.5f) else RoseError.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCalibrated) EmeraldSuccess.copy(alpha = 0.15f) else RoseError.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCalibrated) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isCalibrated) EmeraldSuccess else RoseError,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isCalibrated) "Camera Calibrated" else "Not Calibrated",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isCalibrated) "Active profile saved" else "$targetCount images required",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                if (isCalibrated) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmeraldSuccess.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = result!!.qualityRating.substringBefore(" ("),
                            color = EmeraldSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isCalibrated) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatPill("Error", String.format(java.util.Locale.US, "%.3f px", result!!.reprojectionError))
                    StatPill("Focal (fx)", String.format(java.util.Locale.US, "%.1f", result.fx))
                    StatPill("Focal (fy)", String.format(java.util.Locale.US, "%.1f", result.fy))
                    StatPill("Images", "${result.imageCount}")
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onViewDetails,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("status_view_parameters_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DeepVoid)
                    ) {
                        Text("View Parameters", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onStartCalibration,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("status_recalibrate_button")
                    ) {
                        Text("Recalibrate")
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No valid camera matrix found. Run checkerboard calibration with locked camera parameters to enable metric depth estimation.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onStartCalibration,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("status_start_calibration_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DeepVoid)
                ) {
                    Text("Start Calibration ($targetCount images)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column {
        Text(label, color = TextSecondary, fontSize = 10.sp)
        Text(value, color = CyanAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(tag),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    caption: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(tag),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(caption, color = TextSecondary, fontSize = 10.sp)
            }
        }
    }
}

package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalibrationResult
import com.example.storage.CalibrationStorage
import com.example.ui.components.BeforeAfterSlider
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

@Composable
fun CalibrationResultScreen(
    result: CalibrationResult,
    onNavigateToDepth: () -> Unit,
    onNavigateBack: () -> Unit,
    onOpenAiAssistant: () -> Unit = {}
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val storage = remember { CalibrationStorage(context) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val sampleOriginal = remember(result.sampleImagePath) {
        result.sampleImagePath?.let { BitmapFactory.decodeFile(it) }
    }
    val sampleUndistorted = remember(result.sampleUndistortedPath) {
        result.sampleUndistortedPath?.let { BitmapFactory.decodeFile(it) }
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
                    modifier = Modifier.testTag("result_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Calibration Parameters",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = {
                        val (jsonFile, _) = storage.saveCalibration(result)
                        val intent = storage.createShareIntent(
                            file = jsonFile,
                            mimeType = "application/json",
                            title = "Share Camera Calibration JSON"
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier.testTag("share_calibration_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Calibration",
                        tint = CyanAccent
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quality & Error Card
            val isHighError = result.reprojectionError > 0.50
            val statusColor = if (isHighError) AmberWarning else EmeraldSuccess

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("result_summary_card"),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isHighError) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isHighError) "High Reprojection Error" else "Calibration Optimal",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${result.imageCount} images • ${result.imageWidth}x${result.imageHeight}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(statusColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = String.format(java.util.Locale.US, "%.3f px", result.reprojectionError),
                                color = statusColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isHighError) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Warning: Mean reprojection error > 0.50 px. Recapturing checkerboard frames with steadier hands or improved lighting is recommended for millimeter-accurate depth estimation.",
                            color = AmberWarning,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Tab Navigation
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceDark,
                contentColor = CyanAccent
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Parameters", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Undistortion", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("YAML / JSON", fontSize = 12.sp) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // Intrinsic Camera Matrix (K)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "INTRINSIC CAMERA MATRIX (K)",
                                color = CyanAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            ParamTableItem("fx (Horizontal Focal)", String.format(java.util.Locale.US, "%.2f px", result.fx))
                            ParamTableItem("fy (Vertical Focal)", String.format(java.util.Locale.US, "%.2f px", result.fy))
                            ParamTableItem("cx (Optical Center X)", String.format(java.util.Locale.US, "%.2f px", result.cx))
                            ParamTableItem("cy (Optical Center Y)", String.format(java.util.Locale.US, "%.2f px", result.cy))
                            val fovX = 2 * Math.toDegrees(Math.atan((result.imageWidth / 2.0) / result.fx))
                            val fovY = 2 * Math.toDegrees(Math.atan((result.imageHeight / 2.0) / result.fy))
                            ParamTableItem("Calculated FOV", String.format(java.util.Locale.US, "%.1f° H × %.1f° V", fovX, fovY))
                        }
                    }

                    // Lens Distortion Coefficients
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "LENS DISTORTION COEFFICIENTS",
                                color = CyanAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            ParamTableItem("k1 (Radial 1)", String.format(java.util.Locale.US, "%.6f", result.k1))
                            ParamTableItem("k2 (Radial 2)", String.format(java.util.Locale.US, "%.6f", result.k2))
                            ParamTableItem("p1 (Tangential 1)", String.format(java.util.Locale.US, "%.6f", result.p1))
                            ParamTableItem("p2 (Tangential 2)", String.format(java.util.Locale.US, "%.6f", result.p2))
                            ParamTableItem("k3 (Radial 3)", String.format(java.util.Locale.US, "%.6f", result.k3))
                        }
                    }
                }

                1 -> {
                    // Before / After Undistortion Slider
                    if (sampleOriginal != null && sampleUndistorted != null) {
                        Column {
                            Text(
                                text = "LENS DISTORTION REMOVAL",
                                color = CyanAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            BeforeAfterSlider(
                                originalBitmap = sampleOriginal,
                                undistortedBitmap = sampleUndistorted
                            )
                        }
                    } else {
                        Text(
                            text = "No sample image saved during calibration.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                2 -> {
                    // Code View (YAML / JSON)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = result.toYaml(),
                            color = EmeraldSuccess,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val (_, ymlFile) = storage.saveCalibration(result)
                                val intent = storage.createShareIntent(ymlFile, "text/yaml", "Export calibration.yml")
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_yaml_button")
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export .YML")
                        }

                        OutlinedButton(
                            onClick = {
                                val (jsonFile, _) = storage.saveCalibration(result)
                                val intent = storage.createShareIntent(jsonFile, "application/json", "Export calibration.json")
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_json_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export .JSON")
                        }
                    }
                }
            }

            // AI Lens Diagnostics button
            OutlinedButton(
                onClick = onOpenAiAssistant,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_diagnostic_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp), tint = CyanAccent)
                Spacer(Modifier.width(8.dp))
                Text("AI Lens Assessment (Gemini 3.8 Flash)", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(6.dp))

            // Action to proceed to depth estimation
            Button(
                onClick = onNavigateToDepth,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("result_proceed_depth_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DeepVoid)
            ) {
                Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Proceed to Two-View Depth Estimation", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ParamTableItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(value, color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

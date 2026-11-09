package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CameraLockConfig
import com.example.model.CheckerboardConfig
import com.example.storage.CalibrationStorage
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    currentBoardConfig: CheckerboardConfig,
    currentLockConfig: CameraLockConfig,
    onSaveConfigs: (CheckerboardConfig, CameraLockConfig) -> Unit,
    onClearCalibration: () -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val storage = remember { CalibrationStorage(context) }

    var cols by remember { mutableIntStateOf(currentBoardConfig.patternCols) }
    var rows by remember { mutableIntStateOf(currentBoardConfig.patternRows) }
    var squareMm by remember { mutableFloatStateOf(currentBoardConfig.squareSizeMm) }
    var targetCount by remember { mutableIntStateOf(currentBoardConfig.targetImageCount) }

    var selectedFocusDiopter by remember { mutableFloatStateOf(currentLockConfig.focusDistance) }
    var selectedIso by remember { mutableIntStateOf(currentLockConfig.sensitivityIso) }
    var selectedExpNs by remember { mutableStateOf(currentLockConfig.exposureTimeNs) }

    var savedImageCount by remember { mutableIntStateOf(0) }
    var saveToast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        savedImageCount = storage.loadCalibrationImageFiles().size
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
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Calibration Settings",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = {
                        val newBoard = CheckerboardConfig(cols, rows, squareMm, targetCount)
                        val newLock = currentLockConfig.copy(
                            focusDistance = selectedFocusDiopter,
                            sensitivityIso = selectedIso,
                            exposureTimeNs = selectedExpNs
                        )
                        onSaveConfigs(newBoard, newLock)
                        saveToast = "Settings saved successfully!"
                    },
                    modifier = Modifier.testTag("save_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save Settings",
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
            // Section 1: Target Image Count (Configurable by user)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "TARGET CAPTURE COUNT",
                            color = CyanAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Choose how many calibration images you want to capture. Recommended: 10 to 20 for standard calibration.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 15, 20, 25, 30).forEach { count ->
                            val isSelected = targetCount == count
                            OutlinedButton(
                                onClick = { targetCount = count },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) CyanAccent.copy(alpha = 0.25f) else Color.Transparent
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) CyanAccent else Color.DarkGray
                                )
                            ) {
                                Text(
                                    text = "$count",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) CyanAccent else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = targetCount.toString(),
                        onValueChange = { targetCount = it.toIntOrNull()?.coerceIn(6, 100) ?: targetCount },
                        label = { Text("Custom image count (min 6)") },
                        modifier = Modifier.fillMaxWidth().testTag("setting_target_count_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )
                }
            }

            // Section 2: Checkerboard Dimensions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GridOn, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "CHECKERBOARD PATTERN",
                            color = CyanAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = cols.toString(),
                        onValueChange = { cols = it.toIntOrNull() ?: cols },
                        label = { Text("Inner Corner Columns (default 9)") },
                        modifier = Modifier.fillMaxWidth().testTag("setting_cols_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = rows.toString(),
                        onValueChange = { rows = it.toIntOrNull() ?: rows },
                        label = { Text("Inner Corner Rows (default 6)") },
                        modifier = Modifier.fillMaxWidth().testTag("setting_rows_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = squareMm.toString(),
                        onValueChange = { squareMm = it.toFloatOrNull() ?: squareMm },
                        label = { Text("Square Size in Millimeters (e.g. 25.0)") },
                        modifier = Modifier.fillMaxWidth().testTag("setting_square_size_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )
                }
            }

            // Section 3: Saved Images & Mobile Device Storage
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "DEVICE STORAGE & SAVED PHOTOS",
                            color = EmeraldSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Captured calibration images are saved directly to your phone's storage in:\n${storage.getCalibrationImagesDir().absolutePath}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Currently saved on phone: $savedImageCount photos",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val shareIntent = storage.createShareAllImagesIntent()
                                if (shareIntent != null) {
                                    context.startActivity(shareIntent)
                                } else {
                                    Toast.makeText(context, "No saved photos to share", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                            border = BorderStroke(1.dp, CyanAccent)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share Photos", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                storage.clearAllCalibrationImages()
                                savedImageCount = 0
                                saveToast = "Cleared saved calibration images from storage."
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseError),
                            border = BorderStroke(1.dp, RoseError)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Clear Photos", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Section 4: Camera2 Hardware Lock Presets
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "CAMERA2 HARDWARE LOCK PRESETS",
                            color = EmeraldSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    Text("Fixed Lens Focus Distance:", color = TextSecondary, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Pair("0.5m", 2.0f),
                            Pair("0.67m", 1.5f),
                            Pair("1.0m", 1.0f),
                            Pair("Infinity", 0.0f)
                        ).forEach { (label, diopter) ->
                            val isSel = selectedFocusDiopter == diopter
                            OutlinedButton(
                                onClick = { selectedFocusDiopter = diopter },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSel) CyanAccent.copy(alpha = 0.2f) else Color.Transparent
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSel) CyanAccent else Color.DarkGray
                                )
                            ) {
                                Text(label, fontSize = 10.sp, color = if (isSel) CyanAccent else TextSecondary)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text("Fixed Sensor Sensitivity (ISO):", color = TextSecondary, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(100, 200, 400, 800).forEach { iso ->
                            val isSel = selectedIso == iso
                            OutlinedButton(
                                onClick = { selectedIso = iso },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSel) CyanAccent.copy(alpha = 0.2f) else Color.Transparent
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSel) CyanAccent else Color.DarkGray
                                )
                            ) {
                                Text("$iso", fontSize = 10.sp, color = if (isSel) CyanAccent else TextSecondary)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text("Fixed Exposure Shutter Time:", color = TextSecondary, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Pair("1/30s", 33_333_333L),
                            Pair("1/60s", 16_666_666L),
                            Pair("1/120s", 8_333_333L)
                        ).forEach { (label, ns) ->
                            val isSel = selectedExpNs == ns
                            OutlinedButton(
                                onClick = { selectedExpNs = ns },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSel) CyanAccent.copy(alpha = 0.2f) else Color.Transparent
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSel) CyanAccent else Color.DarkGray
                                )
                            ) {
                                Text(label, fontSize = 10.sp, color = if (isSel) CyanAccent else TextSecondary)
                            }
                        }
                    }
                }
            }

            // Save confirmation toast
            saveToast?.let {
                Text(it, color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            // Section 5: Reset / Clear Calibration
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("DATA MANAGEMENT", color = RoseError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Delete the active calibration matrix and all stored parameters if you wish to start completely fresh.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            onClearCalibration()
                            saveToast = "Active calibration cleared."
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseError),
                        border = BorderStroke(1.dp, RoseError),
                        modifier = Modifier.testTag("clear_calibration_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Clear Saved Calibration")
                    }
                }
            }
        }
    }
}

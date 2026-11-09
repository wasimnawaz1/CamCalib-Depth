package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CameraLockConfig
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.HudBackground
import com.example.ui.theme.SurfaceElevated

@Composable
fun CameraLockBadge(
    config: CameraLockConfig,
    onAutoMeterAndLock: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(HudBackground)
            .border(1.dp, EmeraldSuccess.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .clickable { showDialog = true }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("camera_lock_badge")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Camera Locked",
                tint = EmeraldSuccess,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "AF/AE/AWB LOCKED",
                color = EmeraldSuccess,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "• ISO ${config.sensitivityIso}",
                color = CyanAccent,
                fontSize = 11.sp
            )
        }
    }

    if (showDialog) {
        val expFraction = if (config.exposureTimeNs > 0) {
            val secFraction = 1_000_000_000.0 / config.exposureTimeNs
            "1/${secFraction.toInt()}s"
        } else "1/60s"

        val distanceMeters = if (config.focusDistance > 0f) {
            String.format(java.util.Locale.US, "%.2f m", 1.0f / config.focusDistance)
        } else "Infinity"

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldSuccess)
                    Spacer(Modifier.width(8.dp))
                    Text("Hardware Parameters Locked")
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Auto-focus, auto-exposure, and white balance are locked so intrinsic focal lengths and principal points remain strictly fixed.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    ParamRow("Focus Distance:", "$distanceMeters (${config.focusDistance} diopters)")
                    ParamRow("Exposure Time:", "$expFraction (${config.exposureTimeNs / 1_000_000} ms)")
                    ParamRow("Sensor ISO:", "${config.sensitivityIso}")
                    ParamRow("Color Gains (R/B):", String.format(java.util.Locale.US, "R: %.2f | B: %.2f", config.rGain, config.bGain))
                    ParamRow("Optical Stabilization:", "DISABLED (OIS OFF)")
                    ParamRow("Video Stabilization:", "DISABLED (EIS OFF)")

                    if (onAutoMeterAndLock != null) {
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                onAutoMeterAndLock()
                                showDialog = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("meter_and_lock_button")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Re-Meter & Lock To Current Light")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DeepVoid)
                ) {
                    Text("OK")
                }
            },
            containerColor = SurfaceElevated
        )
    }
}

@Composable
private fun ParamRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Text(value, color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

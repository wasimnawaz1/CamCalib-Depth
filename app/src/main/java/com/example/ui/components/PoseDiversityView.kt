package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalibrationPose
import com.example.model.PoseDiversityState
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.HudBackground
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextSecondary

@Composable
fun PoseDiversityView(
    state: PoseDiversityState,
    currentDetectedPose: CalibrationPose?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HudBackground)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ANGLE DIVERSITY: ${(state.diversityScore * 100).toInt()}%",
                color = CyanAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "${state.totalCaptured} / ${state.targetCount} images",
                color = if (state.isReadyForCalibration) EmeraldSuccess else TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = if (state.isReadyForCalibration) EmeraldSuccess else CyanAccent,
            trackColor = SurfaceElevated
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal checklist of required angles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CalibrationPose.entries.forEach { pose ->
                val isCovered = state.coveredPoses.contains(pose)
                val isCurrent = currentDetectedPose == pose

                val borderColor = when {
                    isCurrent -> CyanAccent
                    isCovered -> EmeraldSuccess
                    else -> Color.DarkGray
                }

                val bgColor = when {
                    isCurrent -> CyanAccent.copy(alpha = 0.15f)
                    isCovered -> EmeraldSuccess.copy(alpha = 0.12f)
                    else -> SurfaceElevated.copy(alpha = 0.5f)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isCovered) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isCovered) EmeraldSuccess else TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = pose.title,
                            color = if (isCovered) EmeraldSuccess else if (isCurrent) CyanAccent else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isCurrent || isCovered) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

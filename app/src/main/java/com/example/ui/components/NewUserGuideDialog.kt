package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun NewUserGuideDialog(
    onDismiss: () -> Unit,
    onStartCalibration: () -> Unit,
    onOpenCheckerboard: () -> Unit
) {
    var stepIndex by remember { mutableIntStateOf(0) }

    val steps = listOf(
        GuideStep(
            title = "1. Prepare Checkerboard",
            subtitle = "Print on A4 paper or open on a tablet screen",
            icon = Icons.Default.Print,
            details = listOf(
                "Tap 'Checkerboard' on the home screen to view the 9x6 (25mm) pattern.",
                "Print on A4 paper at 100% scale (uncheck 'Fit to page').",
                "Tape the sheet onto a rigid clipboard or flat book so it doesn't bend.",
                "Verify the printed ruler measures exactly 10 cm.",
                "Tip: You can also display it on a tablet or laptop screen directly!"
            )
        ),
        GuideStep(
            title = "2. Hardware Locking",
            subtitle = "Why AF, AE, and AWB must be frozen",
            icon = Icons.Default.Lock,
            details = listOf(
                "Phone cameras dynamically adjust lens focus distance and exposure.",
                "This alters optical focal length (fx, fy) between frames, ruining calibration.",
                "CamCalib locks Auto-Focus, Auto-Exposure, and White Balance.",
                "Tap the wand icon anytime to meter the room once and freeze parameters.",
                "The exact same locked parameters are reused in Depth Estimation."
            )
        ),
        GuideStep(
            title = "3. Capture 30-35 Angles",
            subtitle = "Follow the Pose Diversity checklist",
            icon = Icons.Default.GridOn,
            details = listOf(
                "Point camera at the checkerboard until green corners appear.",
                "The app automatically rejects blurry photos (sharpness < 90).",
                "Capture diverse poses: Front, Left 30°, Right 30°, Top, Bottom, Tilted 45°, Close Up, and Far Away.",
                "Each pose chip turns green with a checkmark once satisfied.",
                "Once you reach 30+ frames, tap 'Calibrate Now'."
            )
        ),
        GuideStep(
            title = "4. Review OpenCV Matrix",
            subtitle = "Verify Reprojection Error and Distortion",
            icon = Icons.Default.CheckCircle,
            details = listOf(
                "OpenCV solves for Intrinsic Matrix (fx, fy, cx, cy) and Distortion (k1, k2, p1, p2, k3).",
                "Mean Reprojection Error should be ≤ 0.50 px (optimal: < 0.3 px).",
                "Use the interactive Before/After slider to see the curved board lines get straightened.",
                "Tap 'AI Optical Assessment' for Gemini's deep analysis of your camera lens.",
                "Export calibration as .YML or .JSON for external tools."
            )
        ),
        GuideStep(
            title = "5. Stereo Depth & Measurement",
            subtitle = "Measure real-world distances in cm/meters",
            icon = Icons.Default.Straighten,
            details = listOf(
                "In Depth Estimation, tap Shutter to capture the Left View.",
                "Slide your phone horizontally ~10 cm to the right (keep it at the same height).",
                "Tap Shutter to capture the Right View.",
                "OpenCV StereoSGBM computes the dense metric depth map with Jet colormap.",
                "Tap ANY object on screen to place a reticle and view exact distance (e.g. 142 cm)!",
                "Export 3D Point Cloud (.PLY) file to open in Blender or MeshLab."
            )
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Camera, contentDescription = null, tint = CyanAccent)
                    Spacer(Modifier.width(8.dp))
                    Text("User Guide & Walkthrough", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Step Carousel Tabs
                TabRow(
                    selectedTabIndex = stepIndex,
                    containerColor = SurfaceDark,
                    contentColor = CyanAccent
                ) {
                    steps.forEachIndexed { i, step ->
                        Tab(
                            selected = stepIndex == i,
                            onClick = { stepIndex = i },
                            text = { Text("Step ${i + 1}", fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                val cur = steps[stepIndex]

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CyanAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(cur.icon, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(cur.title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                                Text(cur.subtitle, color = TextSecondary, fontSize = 11.sp)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        cur.details.forEach { bullet ->
                            Row(
                                modifier = Modifier.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("• ", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(bullet, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Navigation buttons between steps
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (stepIndex > 0) {
                        OutlinedButton(
                            onClick = { stepIndex-- },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Previous")
                        }
                        Spacer(Modifier.width(8.dp))
                    }

                    if (stepIndex < steps.size - 1) {
                        Button(
                            onClick = { stepIndex++ },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DeepVoid),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Next Step")
                        }
                    } else {
                        Button(
                            onClick = {
                                onDismiss()
                                onStartCalibration()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = DeepVoid),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Start Calibration", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        containerColor = SurfaceElevated
    )
}

data class GuideStep(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val details: List<String>
)

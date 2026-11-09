package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DepthResult
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.HudBackground
import com.example.ui.theme.SurfaceDark

@Composable
fun DepthTapViewer(
    depthResult: DepthResult,
    showDepthMap: Boolean = true,
    modifier: Modifier = Modifier
) {
    var tapNormalizedPos by remember { mutableStateOf<Offset?>(Offset(0.5f, 0.5f)) }
    val currentDepth = remember(tapNormalizedPos, depthResult) {
        tapNormalizedPos?.let { pos ->
            depthResult.getDepthAt(pos.x, pos.y)
        }
    }

    val displayBitmap = if (showDepthMap) depthResult.colorizedDepthMap else depthResult.leftImage
    val imageBitmap = remember(displayBitmap) { displayBitmap.asImageBitmap() }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .pointerInput(depthResult) {
                    detectTapGestures { offset ->
                        val normX = (offset.x / size.width).coerceIn(0f, 1f)
                        val normY = (offset.y / size.height).coerceIn(0f, 1f)
                        tapNormalizedPos = Offset(normX, normY)
                    }
                }
                .testTag("depth_tap_canvas")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height

                drawImage(
                    image = imageBitmap,
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(canvasW.toInt(), canvasH.toInt())
                )

                // Draw target reticle crosshairs if tapped
                tapNormalizedPos?.let { norm ->
                    val cx = norm.x * canvasW
                    val cy = norm.y * canvasH

                    // Outer ring
                    drawCircle(
                        color = Color.White,
                        radius = 16.dp.toPx(),
                        center = Offset(cx, cy),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )

                    // Inner dot
                    drawCircle(
                        color = CyanAccent,
                        radius = 3.dp.toPx(),
                        center = Offset(cx, cy)
                    )

                    // Reticle lines
                    val lineLen = 10.dp.toPx()
                    drawLine(Color.White, Offset(cx - 20.dp.toPx(), cy), Offset(cx - 20.dp.toPx() + lineLen, cy), strokeWidth = 2.dp.toPx())
                    drawLine(Color.White, Offset(cx + 20.dp.toPx() - lineLen, cy), Offset(cx + 20.dp.toPx(), cy), strokeWidth = 2.dp.toPx())
                    drawLine(Color.White, Offset(cx, cy - 20.dp.toPx()), Offset(cx, cy - 20.dp.toPx() + lineLen), strokeWidth = 2.dp.toPx())
                    drawLine(Color.White, Offset(cx, cy + 20.dp.toPx() - lineLen), Offset(cx, cy + 20.dp.toPx()), strokeWidth = 2.dp.toPx())
                }
            }

            // Top instruction pill
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AdsClick, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Tap any object on screen to measure distance",
                    color = Color.White,
                    fontSize = 11.sp
                )
            }

            // Depth Scale Bar at bottom
            if (showDepthMap) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NEAR (${String.format(java.util.Locale.US, "%.1fm", depthResult.minDepthMeters)})",
                        color = Color(0xFFFF3D00),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue)
                                )
                            )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "FAR (${String.format(java.util.Locale.US, "%.1fm", depthResult.maxDepthMeters)})",
                        color = Color(0xFF2979FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Large Readout Card for Tapped Coordinate
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(HudBackground)
                .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "MEASURED DEPTH AT CROSSHAIR",
                            color = CyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        if (currentDepth != null) {
                            Text(
                                text = String.format(java.util.Locale.US, "%.2f meters (%.0f cm)", currentDepth, currentDepth * 100f),
                                color = EmeraldSuccess,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        } else {
                            Text(
                                text = "Occluded / Insufficient parallax texture",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Baseline", color = Color.Gray, fontSize = 10.sp)
                    Text("${(depthResult.baselineMeters * 100).toInt()} cm", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

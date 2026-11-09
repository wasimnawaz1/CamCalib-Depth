package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SurfaceDark

@Composable
fun BeforeAfterSlider(
    originalBitmap: Bitmap,
    undistortedBitmap: Bitmap,
    modifier: Modifier = Modifier
) {
    var splitFraction by remember { mutableFloatStateOf(0.5f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val x = change.position.x
                    splitFraction = (x / size.width.toFloat()).coerceIn(0.05f, 0.95f)
                }
            }
            .testTag("before_after_slider")
    ) {
        val origImg = remember(originalBitmap) { originalBitmap.asImageBitmap() }
        val undImg = remember(undistortedBitmap) { undistortedBitmap.asImageBitmap() }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            val splitX = canvasW * splitFraction

            val srcSize = IntSize(origImg.width, origImg.height)
            val dstSize = IntSize(canvasW.toInt(), canvasH.toInt())

            // 1. Draw Undistorted (Right / Full background)
            drawImage(
                image = undImg,
                dstOffset = IntOffset.Zero,
                dstSize = dstSize
            )

            // 2. Draw Original clipped to Left of splitX
            clipRect(left = 0f, top = 0f, right = splitX, bottom = canvasH) {
                drawImage(
                    image = origImg,
                    dstOffset = IntOffset.Zero,
                    dstSize = dstSize
                )
            }

            // 3. Draw Split Line
            drawLine(
                color = CyanAccent,
                start = Offset(splitX, 0f),
                end = Offset(splitX, canvasH),
                strokeWidth = 3.dp.toPx()
            )

            // Split handle circle
            drawCircle(
                color = CyanAccent,
                radius = 14.dp.toPx(),
                center = Offset(splitX, canvasH / 2f)
            )
            drawCircle(
                color = DeepVoid,
                radius = 8.dp.toPx(),
                center = Offset(splitX, canvasH / 2f)
            )
        }

        // Left Label: ORIGINAL (DISTORTED)
        Text(
            text = "ORIGINAL",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )

        // Right Label: UNDISTORTED
        Text(
            text = "UNDISTORTED",
            color = EmeraldSuccess,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )

        Text(
            text = "← Drag line to compare distortion removal →",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

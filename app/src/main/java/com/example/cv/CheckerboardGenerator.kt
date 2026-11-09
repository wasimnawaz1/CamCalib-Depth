package com.example.cv

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.model.CheckerboardConfig
import java.io.File
import java.io.FileOutputStream

object CheckerboardGenerator {

    /**
     * Generates a high-resolution printable bitmap for A4 (300 DPI: ~2480 x 3508 pixels or scaled).
     * Draws 10x7 black/white squares yielding 9x6 inner corners with 25mm physical size markers.
     */
    fun generateCheckerboardBitmap(
        config: CheckerboardConfig = CheckerboardConfig(),
        widthPx: Int = 2000,
        heightPx: Int = 1500
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // White background
        canvas.drawColor(Color.WHITE)

        val totalCols = config.patternCols + 1 // 10 squares
        val totalRows = config.patternRows + 1 // 7 squares

        val marginX = widthPx * 0.10f
        val marginY = heightPx * 0.12f

        val availableW = widthPx - 2 * marginX
        val availableH = heightPx - 2 * marginY

        val squareSize = (availableW / totalCols).coerceAtMost(availableH / totalRows)

        val startX = marginX + (availableW - (squareSize * totalCols)) / 2f
        val startY = marginY + (availableH - (squareSize * totalRows)) / 2f

        val blackPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        val textPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 28f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 42f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // Title
        canvas.drawText("CAMERA CALIBRATION CHECKERBOARD PATTERN", widthPx / 2f, marginY * 0.45f, headerPaint)
        canvas.drawText(
            "Inner Corners: ${config.patternCols} x ${config.patternRows} (${totalCols}x${totalRows} squares) | Square: ${config.squareSizeMm.toInt()}mm",
            widthPx / 2f,
            marginY * 0.75f,
            textPaint
        )

        // Outer border
        canvas.drawRect(
            startX - 2f,
            startY - 2f,
            startX + totalCols * squareSize + 2f,
            startY + totalRows * squareSize + 2f,
            borderPaint
        )

        // Draw alternating checkerboard squares
        for (r in 0 until totalRows) {
            for (c in 0 until totalCols) {
                if ((r + c) % 2 == 1) { // Standard checkerboard: black on odd sum
                    val left = startX + c * squareSize
                    val top = startY + r * squareSize
                    canvas.drawRect(left, top, left + squareSize, top + squareSize, blackPaint)
                }
            }
        }

        // Reference Ruler at the bottom (10 cm = 100 mm reference to verify print scale)
        val rulerY = startY + totalRows * squareSize + 50f
        val rulerStartX = widthPx / 2f - 250f
        val rulerEndX = widthPx / 2f + 250f
        val rulerPaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 4f
        }
        canvas.drawLine(rulerStartX, rulerY, rulerEndX, rulerY, rulerPaint)
        for (i in 0..10) {
            val tickX = rulerStartX + (i * 50f)
            val tickH = if (i % 5 == 0) 25f else 14f
            canvas.drawLine(tickX, rulerY - tickH, tickX, rulerY, rulerPaint)
            if (i % 5 == 0) {
                canvas.drawText("${i}cm", tickX, rulerY + 30f, textPaint)
            }
        }
        canvas.drawText("Scale Verification: Ensure this ruler measures exactly 10 cm when printed", widthPx / 2f, rulerY + 70f, textPaint)

        return bitmap
    }

    fun saveBitmapToFile(bitmap: Bitmap, outFile: File): File {
        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        return outFile
    }
}

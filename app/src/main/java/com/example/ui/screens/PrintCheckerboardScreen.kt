package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cv.CheckerboardGenerator
import com.example.model.CheckerboardConfig
import com.example.storage.CalibrationStorage
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

@Composable
fun PrintCheckerboardScreen(
    config: CheckerboardConfig,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val storage = remember { CalibrationStorage(context) }

    val boardBitmap = remember(config) {
        CheckerboardGenerator.generateCheckerboardBitmap(config = config, widthPx = 1600, heightPx = 1200)
    }

    var saveMessage by remember { mutableStateOf<String?>(null) }

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
                    modifier = Modifier.testTag("print_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Printable Target Pattern",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = {
                        val file = File(storage.getCalibrationDir(), "checkerboard_pattern.png")
                        CheckerboardGenerator.saveBitmapToFile(boardBitmap, file)
                        val intent = storage.createShareIntent(file, "image/png", "Share Calibration Checkerboard")
                        context.startActivity(intent)
                    },
                    modifier = Modifier.testTag("share_checkerboard_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Pattern",
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
            // Preview Image Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = boardBitmap.asImageBitmap(),
                    contentDescription = "Checkerboard Pattern",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Specs Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceElevated)
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "PATTERN SPECIFICATIONS",
                        color = CyanAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Inner Corners:", color = TextSecondary, fontSize = 12.sp)
                        Text("${config.patternCols} × ${config.patternRows} (${config.totalCorners} points)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Squares Total:", color = TextSecondary, fontSize = 12.sp)
                        Text("${config.patternCols + 1} × ${config.patternRows + 1} squares", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Square Size:", color = TextSecondary, fontSize = 12.sp)
                        Text("${config.squareSizeMm.toInt()} mm (2.5 cm)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Printing Instructions
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("PRINTING GUIDELINES", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("1. Print on standard A4 or Letter paper at 100% scale (Do not fit/shrink to page).", color = TextSecondary, fontSize = 11.sp)
                    Text("2. Mount or tape the printed sheet onto a stiff, flat board (e.g. cardboard or clipboard) to prevent bending.", color = TextSecondary, fontSize = 11.sp)
                    Text("3. Verify with a ruler that the printed calibration bar matches exactly 10 cm.", color = TextSecondary, fontSize = 11.sp)
                    Text("4. Alternatively, you can display this full-screen on a tablet or laptop without printing!", color = EmeraldSuccess, fontSize = 11.sp)
                }
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val file = File(storage.getCalibrationDir(), "checkerboard_pattern.png")
                        CheckerboardGenerator.saveBitmapToFile(boardBitmap, file)
                        saveMessage = "Saved to ${file.name} in Documents"
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_pattern_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DeepVoid)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save Image", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val file = File(storage.getCalibrationDir(), "checkerboard_pattern.png")
                        CheckerboardGenerator.saveBitmapToFile(boardBitmap, file)
                        val intent = storage.createShareIntent(file, "image/png", "Print Checkerboard")
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("print_share_button")
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share / Print")
                }
            }

            saveMessage?.let { msg ->
                Text(
                    text = msg,
                    color = EmeraldSuccess,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

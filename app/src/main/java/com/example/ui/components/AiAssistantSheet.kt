package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiCalibrationAssistant
import com.example.model.CalibrationResult
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

data class ChatMessage(val sender: String, val text: String, val isAi: Boolean)

@Composable
fun AiAssistantDialog(
    calibrationResult: CalibrationResult?,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val assistant = remember { AiCalibrationAssistant() }

    var userQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<ChatMessage>() }

    // Initial greeting and diagnostic check
    LaunchedEffect(calibrationResult) {
        if (messages.isEmpty()) {
            if (calibrationResult != null) {
                isLoading = true
                messages.add(
                    ChatMessage(
                        sender = "CamCalib AI",
                        text = "Analyzing your camera's calibration matrix ($calibrationResult.imageWidth x $calibrationResult.imageHeight, fx=${calibrationResult.fx.toInt()})...",
                        isAi = true
                    )
                )
                scope.launch {
                    val analysis = assistant.analyzeCalibrationParameters(calibrationResult)
                    messages.removeAt(messages.size - 1)
                    messages.add(ChatMessage(sender = "CamCalib AI", text = analysis, isAi = true))
                    isLoading = false
                }
            } else {
                messages.add(
                    ChatMessage(
                        sender = "CamCalib AI",
                        text = "Hello! I am your Optical & Depth AI assistant powered by Gemini 3.8 Flash. I can help you calibrate your phone's camera, explain distortion parameters, or guide your stereo baseline measurements. How can I assist?",
                        isAi = true
                    )
                )
            }
        }
    }

    val suggestedQuestions = listOf(
        "Why is my reprojection error > 0.5px?",
        "What baseline distance should I use?",
        "Explain k1, k2 and p1 distortion",
        "How to achieve millimeter accuracy?",
        "Why must AF and AE be locked?"
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
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CyanAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("AI Optical Assistant", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Powered by Gemini 3.8 Flash", fontSize = 10.sp, color = TextSecondary)
                    }
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
                    .height(380.dp)
            ) {
                // Chat Message Stream
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    messages.forEach { msg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (msg.isAi) SurfaceDark else CyanAccent.copy(alpha = 0.15f))
                                .border(1.dp, if (msg.isAi) SurfaceElevated else CyanAccent.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = msg.sender,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (msg.isAi) CyanAccent else EmeraldSuccess
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = msg.text,
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    if (isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CyanAccent, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Gemini is analyzing optical parameters...", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }

                // Suggested Questions Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestedQuestions.forEach { q ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceDark)
                                .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .clickable(enabled = !isLoading) {
                                    messages.add(ChatMessage(sender = "You", text = q, isAi = false))
                                    isLoading = true
                                    scope.launch {
                                        val answer = assistant.askAssistant(q)
                                        messages.add(ChatMessage(sender = "CamCalib AI", text = answer, isAi = true))
                                        isLoading = false
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(q, color = CyanAccent, fontSize = 10.sp)
                        }
                    }
                }

                // Input Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = userQuery,
                        onValueChange = { userQuery = it },
                        placeholder = { Text("Ask about calibration or depth...", fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_query_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (userQuery.isNotBlank() && !isLoading) {
                                val query = userQuery.trim()
                                userQuery = ""
                                messages.add(ChatMessage(sender = "You", text = query, isAi = false))
                                isLoading = true
                                scope.launch {
                                    val answer = assistant.askAssistant(query)
                                    messages.add(ChatMessage(sender = "CamCalib AI", text = answer, isAi = true))
                                    isLoading = false
                                }
                            }
                        },
                        enabled = userQuery.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (userQuery.isNotBlank() && !isLoading) CyanAccent else Color.DarkGray)
                            .size(40.dp)
                            .testTag("ai_send_button")
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = DeepVoid,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        containerColor = SurfaceElevated
    )
}

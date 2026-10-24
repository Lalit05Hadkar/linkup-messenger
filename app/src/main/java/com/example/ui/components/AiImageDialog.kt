package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.gemini.GeminiClient
import com.example.ui.theme.AccentBlue
import kotlinx.coroutines.launch

enum class AiImageMode {
    CREATE, EDIT
}

@Composable
fun AiImageDialog(
    onDismiss: () -> Unit,
    onSendImageToChat: (base64Image: String, prompt: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val geminiClient = remember { GeminiClient(context) }

    var mode by remember { mutableStateOf(AiImageMode.CREATE) }
    var promptInput by remember { mutableStateOf("") }
    var selectedAspectRatio by remember { mutableStateOf("1:1") }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedBase64 by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AccentBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI",
                                tint = AccentBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Gemini Image Studio",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Powered by gemini-nano-banana-2.1",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentBlue
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode Selector: Create | Edit
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (mode == AiImageMode.CREATE) AccentBlue else Color.Transparent)
                            .clickable { mode = AiImageMode.CREATE }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Create Image",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (mode == AiImageMode.CREATE) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (mode == AiImageMode.CREATE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (mode == AiImageMode.EDIT) AccentBlue else Color.Transparent)
                            .clickable { mode = AiImageMode.EDIT }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Edit Image",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (mode == AiImageMode.EDIT) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (mode == AiImageMode.EDIT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Prompt Input
                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    label = {
                        Text(if (mode == AiImageMode.CREATE) "Image prompt" else "Edit instructions (e.g. Add neon glasses)")
                    },
                    placeholder = {
                        Text(if (mode == AiImageMode.CREATE) "A sleek futuristic 3D messenger icon with neon cyan accents" else "Change lighting to golden sunset and add a party hat")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Aspect Ratio Selector
                if (mode == AiImageMode.CREATE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("1:1", "16:9", "9:16").forEach { ratio ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = 1.dp,
                                        color = if (selectedAspectRatio == ratio) AccentBlue else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .background(
                                        if (selectedAspectRatio == ratio) AccentBlue.copy(alpha = 0.1f) else Color.Transparent
                                    )
                                    .clickable { selectedAspectRatio = ratio }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ratio,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selectedAspectRatio == ratio) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (selectedAspectRatio == ratio) AccentBlue else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Generate / Edit Button
                Button(
                    onClick = {
                        if (promptInput.isNotBlank()) {
                            isGenerating = true
                            errorMessage = null
                            scope.launch {
                                val result = geminiClient.generateOrEditImage(
                                    prompt = promptInput.trim(),
                                    base64InputImage = if (mode == AiImageMode.EDIT) generatedBase64 else null,
                                    aspectRatio = selectedAspectRatio
                                )
                                isGenerating = false
                                result.onSuccess { base64 ->
                                    generatedBase64 = base64
                                }.onFailure { err ->
                                    errorMessage = err.localizedMessage ?: "Failed to generate image"
                                }
                            }
                        }
                    },
                    enabled = promptInput.isNotBlank() && !isGenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentBlue,
                        contentColor = Color.White
                    )
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (mode == AiImageMode.CREATE) "Generating with gemini-nano-banana-2.1..." else "Editing image...")
                    } else {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (mode == AiImageMode.CREATE) "Generate Image" else "Apply AI Edit")
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Result Preview
                if (generatedBase64 != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    val bitmap = remember(generatedBase64) {
                        try {
                            val clean = generatedBase64!!.substringAfter("base64,")
                            val bytes = Base64.decode(clean, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } catch (_: Exception) {
                            null
                        }
                    }

                    if (bitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Generated Image",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                onSendImageToChat(generatedBase64!!, promptInput)
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            )
                        ) {
                            Text(text = "Send to Conversation")
                        }
                    }
                }
            }
        }
    }
}

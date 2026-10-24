package com.example.ui.screens.voice_live

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.gemini.GeminiClient
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.DangerRed
import com.example.ui.theme.OnlineGreen
import kotlinx.coroutines.launch

enum class LiveVoiceStatus {
    CONNECTING, LISTENING, THINKING, SPEAKING, MUTED
}

data class VoiceTurn(
    val speaker: String,
    val text: String,
    val isGemini: Boolean
)

@Composable
fun LiveVoiceScreen(
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val geminiClient = remember { GeminiClient(context) }

    var status by remember { mutableStateOf(LiveVoiceStatus.LISTENING) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(true) }
    var transcriptTurns by remember {
        mutableStateOf(
            listOf(
                VoiceTurn(
                    speaker = "Gemini 3.8 Live",
                    text = "Hello! I'm connected via gemini-3.8-live. What would you like to talk about?",
                    isGemini = true
                )
            )
        )
    }

    var quickInputText by remember { mutableStateOf("") }

    // Pulsing visual wave animation for the voice orb
    val orbScale = remember { Animatable(1f) }
    val outerRingScale = remember { Animatable(1f) }

    LaunchedEffect(status) {
        if (status == LiveVoiceStatus.SPEAKING || status == LiveVoiceStatus.LISTENING) {
            orbScale.animateTo(
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            orbScale.snapTo(1f)
        }
    }

    LaunchedEffect(status) {
        if (status == LiveVoiceStatus.SPEAKING || status == LiveVoiceStatus.LISTENING) {
            outerRingScale.animateTo(
                targetValue = 1.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }

    fun submitVoiceTurn(userText: String) {
        if (userText.isBlank()) return
        val currentHistory = transcriptTurns.map { it.speaker to it.text }
        transcriptTurns = transcriptTurns + VoiceTurn("You", userText, isGemini = false)
        status = LiveVoiceStatus.THINKING

        scope.launch {
            val result = geminiClient.liveVoiceTurn(userText, currentHistory)
            val geminiResponse = result.getOrDefault("I'm here with Gemini 3.8 Live. What else would you like to explore?")
            status = LiveVoiceStatus.SPEAKING
            transcriptTurns = transcriptTurns + VoiceTurn("Gemini 3.8 Live", geminiResponse, isGemini = true)
            kotlinx.coroutines.delay(2200)
            status = LiveVoiceStatus.LISTENING
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0D14))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E2330))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (status == LiveVoiceStatus.MUTED) DangerRed else OnlineGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE API",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = { isSpeakerOn = !isSpeakerOn },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2330))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speaker",
                            tint = if (isSpeakerOn) AccentBlue else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Gemini 3.8 Live",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when (status) {
                        LiveVoiceStatus.CONNECTING -> "Connecting..."
                        LiveVoiceStatus.LISTENING -> "Listening to your voice..."
                        LiveVoiceStatus.THINKING -> "Gemini 3.8 Live is thinking..."
                        LiveVoiceStatus.SPEAKING -> "Gemini 3.8 Live is speaking..."
                        LiveVoiceStatus.MUTED -> "Microphone Muted"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentBlue
                )
            }

            // Central Animated Voice Orb
            Box(
                modifier = Modifier
                    .size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer glowing pulse ring
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .scale(outerRingScale.value)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    AccentBlue.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Middle ring
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(orbScale.value)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    AccentBlue.copy(alpha = 0.4f),
                                    Color(0xFF6366F1).copy(alpha = 0.2f)
                                )
                            )
                        )
                )

                // Main Core Orb
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF3B5BFD),
                                    Color(0xFF8B5CF6),
                                    Color(0xFF06B6D4)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.GraphicEq,
                        contentDescription = "Wave",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Live Transcript Turns
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    reverseLayout = true
                ) {
                    items(transcriptTurns.reversed()) { turn ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = if (turn.isGemini) Color(0xFF171C26) else AccentBlue.copy(alpha = 0.2f)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text(
                                    text = turn.speaker,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (turn.isGemini) AccentBlue else Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = turn.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFECEEF2)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Speech Turn Input / Suggestions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = quickInputText,
                    onValueChange = { quickInputText = it },
                    placeholder = { Text("Speak or type prompt...", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (quickInputText.isNotBlank()) {
                            submitVoiceTurn(quickInputText.trim())
                            quickInputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AccentBlue)
                ) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = "Send", tint = Color.White)
                }
            }

            // Bottom Call Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Mute Button
                IconButton(
                    onClick = {
                        isMuted = !isMuted
                        status = if (isMuted) LiveVoiceStatus.MUTED else LiveVoiceStatus.LISTENING
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) DangerRed.copy(alpha = 0.2f) else Color(0xFF1E2330))
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute",
                        tint = if (isMuted) DangerRed else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // End Call Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(DangerRed)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

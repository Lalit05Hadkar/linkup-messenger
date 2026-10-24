package com.example.ui.screens.call

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import coil.compose.AsyncImage
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.DangerRed
import com.example.ui.theme.OnlineGreen
import kotlinx.coroutines.delay

enum class CallState {
    CALLING,
    RINGING,
    CONNECTED
}

@Composable
fun VoiceCallScreen(
    friendUid: String,
    friendName: String,
    friendAvatar: String = "",
    onEndCall: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    var callState by remember { mutableStateOf(CallState.CALLING) }
    var callSeconds by remember { mutableIntStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }

    // Pulsing circle wave animations
    val pulseScale1 = remember { Animatable(1f) }
    val pulseScale2 = remember { Animatable(1f) }

    // Cleanup audio on exit
    DisposableEffect(Unit) {
        onDispose {
            try {
                audioManager?.isSpeakerphoneOn = false
                audioManager?.mode = AudioManager.MODE_NORMAL
            } catch (_: Exception) {}
        }
    }

    BackHandler {
        onEndCall()
    }

    // Call state transitions & duration timer
    LaunchedEffect(Unit) {
        // Phase 1: Calling for 2.2s
        delay(2200)
        callState = CallState.RINGING

        // Phase 2: Ringing for 2.8s
        delay(2800)
        callState = CallState.CONNECTED

        // Phase 3: Connected timer
        while (true) {
            delay(1000)
            callSeconds++
        }
    }

    // Ripple animations around avatar
    LaunchedEffect(callState) {
        pulseScale1.animateTo(
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    LaunchedEffect(callState) {
        delay(400)
        pulseScale2.animateTo(
            targetValue = 1.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    fun vibrateShort() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                v?.vibrate(80)
            }
        } catch (_: Exception) {}
    }

    fun toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn
        try {
            audioManager?.isSpeakerphoneOn = isSpeakerOn
        } catch (_: Exception) {}
        vibrateShort()
    }

    fun toggleMute() {
        isMuted = !isMuted
        try {
            audioManager?.isMicrophoneMute = isMuted
        } catch (_: Exception) {}
        vibrateShort()
    }

    fun handleHangup() {
        vibrateShort()
        try {
            audioManager?.isSpeakerphoneOn = false
            audioManager?.mode = AudioManager.MODE_NORMAL
        } catch (_: Exception) {}
        onEndCall()
    }

    val formattedDuration = remember(callSeconds) {
        val minutes = callSeconds / 60
        val secs = callSeconds % 60
        String.format("%02d:%02d", minutes, secs)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF090D16),
                        Color(0xFF0F1524),
                        Color(0xFF080C14)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Security Badge & Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF1B2335))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted",
                        tint = OnlineGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "End-to-End Encrypted Voice Call",
                        color = Color(0xFFC7D2FE),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = friendName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (callState) {
                        CallState.CALLING -> "Calling..."
                        CallState.RINGING -> "Ringing..."
                        CallState.CONNECTED -> formattedDuration
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = when (callState) {
                        CallState.CONNECTED -> OnlineGreen
                        else -> AccentBlue
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Central Pulsing Avatar Orb
            Box(
                modifier = Modifier.size(260.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer Pulse Ring
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .scale(pulseScale2.value)
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

                // Middle Pulse Ring
                Box(
                    modifier = Modifier
                        .size(165.dp)
                        .scale(pulseScale1.value)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    AccentBlue.copy(alpha = 0.4f),
                                    Color(0xFF6366F1).copy(alpha = 0.15f)
                                )
                            )
                        )
                )

                // Main Avatar Circle
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF2563EB),
                                    Color(0xFF7C3AED)
                                )
                            )
                        )
                        .border(3.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (friendAvatar.isNotBlank()) {
                        AsyncImage(
                            model = friendAvatar,
                            contentDescription = friendName,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        val initials = friendName
                            .split(" ")
                            .take(2)
                            .mapNotNull { it.firstOrNull()?.uppercase() }
                            .joinToString("")
                            .ifBlank { "U" }
                        Text(
                            text = initials,
                            color = Color.White,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bottom Actions Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute / Unmute Button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { toggleMute() },
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(if (isMuted) DangerRed.copy(alpha = 0.25f) else Color(0xFF1E2638))
                                .border(
                                    1.dp,
                                    if (isMuted) DangerRed else Color.White.copy(alpha = 0.1f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = if (isMuted) DangerRed else Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isMuted) "Muted" else "Mute",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }

                    // End Call Hangup Button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { handleHangup() },
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(DangerRed)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "End Call",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "End",
                            color = DangerRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Speakerphone Toggle Button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { toggleSpeaker() },
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(if (isSpeakerOn) AccentBlue.copy(alpha = 0.25f) else Color(0xFF1E2638))
                                .border(
                                    1.dp,
                                    if (isSpeakerOn) AccentBlue else Color.White.copy(alpha = 0.1f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Outlined.VolumeDown,
                                contentDescription = if (isSpeakerOn) "Speaker On" else "Speaker Off",
                                tint = if (isSpeakerOn) AccentBlue else Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isSpeakerOn) "Speaker" else "Earpiece",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

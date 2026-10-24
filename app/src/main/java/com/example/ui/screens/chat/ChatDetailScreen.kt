package com.example.ui.screens.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Message
import com.example.data.model.MessageStatus
import com.example.data.model.ReplyPreview
import com.example.data.model.User
import com.example.ui.components.AiImageDialog
import com.example.ui.components.LinkUpAvatar
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.BubbleReceivedDark
import com.example.ui.theme.BubbleReceivedLight
import com.example.ui.theme.OnlineGreen
import com.example.viewmodel.AppScreen
import com.example.viewmodel.LinkUpViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatDetailScreen(
    friendUid: String,
    chatId: String,
    viewModel: LinkUpViewModel
) {
    BackHandler {
        viewModel.navigateTo(AppScreen.Home)
    }

    val allUsers by viewModel.repository.allUsers.collectAsState()
    val friend = allUsers.find { it.uid == friendUid } ?: User(fullName = "Friend")
    val allMessages by viewModel.repository.messages.collectAsState()
    val chatMessages = allMessages[chatId] ?: emptyList()
    val typingStatus by viewModel.repository.typingStatus.collectAsState()
    val isFriendTyping = typingStatus[chatId] == true
    val activeReply by viewModel.activeReply.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var showAiImageDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Scroll to bottom on new messages
    LaunchedEffect(chatMessages.size, isFriendTyping) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        // Chat Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(AppScreen.Home) },
                    modifier = Modifier.testTag("chat_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                LinkUpAvatar(
                    name = friend.fullName,
                    size = 40.dp,
                    isOnline = friend.isOnline,
                    showOnlineDot = true
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = friend.fullName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isFriendTyping) "typing..." else if (friend.isOnline) "Online" else "last seen recently",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFriendTyping || friend.isOnline) OnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // AI Live Voice Call
                IconButton(
                    onClick = { viewModel.navigateTo(AppScreen.LiveVoice) },
                    modifier = Modifier.testTag("live_voice_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Live Voice Conversation (gemini-3.8-live)",
                        tint = AccentBlue
                    )
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Date Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(chatMessages, key = { it.messageId }) { message ->
                val isMine = message.senderId != friendUid
                MessageBubbleItem(
                    message = message,
                    isMine = isMine,
                    friendName = friend.fullName,
                    onReply = {
                        viewModel.setReplyTo(message, if (isMine) "You" else friend.fullName)
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Typing Indicator Bubble
            if (isFriendTyping) {
                item {
                    TypingIndicatorBubble()
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Reply bar above input
        AnimatedVisibility(
            visible = activeReply != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val reply = activeReply
            if (reply != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Reply,
                            contentDescription = "Replying",
                            tint = AccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Replying to ${reply.senderName}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AccentBlue
                            )
                            Text(
                                text = reply.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearReply() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel reply",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Input Row
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gemini Studio Image Generator / Editor button
                IconButton(
                    onClick = { showAiImageDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("ai_image_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Gemini Image Studio (gemini-nano-banana-2.1)",
                        tint = AccentBlue
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = {
                        Text(
                            text = "Message",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = AccentBlue
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Send Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AccentBlue)
                        .clickable {
                            if (textInput.isNotBlank()) {
                                viewModel.sendMessage(chatId, textInput)
                                textInput = ""
                            }
                        }
                        .testTag("send_message_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (showAiImageDialog) {
            AiImageDialog(
                onDismiss = { showAiImageDialog = false },
                onSendImageToChat = { base64, prompt ->
                    viewModel.sendMessage(chatId = chatId, text = prompt, imageUrl = base64)
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubbleItem(
    message: Message,
    isMine: Boolean,
    friendName: String,
    onReply: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface == Color(0xFF171A21)
    val bubbleColor = if (isMine) AccentBlue else if (isDark) BubbleReceivedDark else BubbleReceivedLight
    val textColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
    val timeColor = if (isMine) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant

    val shape = if (isMine) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bubbleColor)
                .combinedClickable(
                    onClick = onReply,
                    onLongClick = onReply
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
        ) {
            // Quoted reply block inside message
            if (message.replyTo != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isMine) Color.White.copy(alpha = 0.2f) else AccentBlue.copy(alpha = 0.1f)
                        )
                        .border(
                            width = 2.dp,
                            color = if (isMine) Color.White.copy(alpha = 0.5f) else AccentBlue,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column {
                        Text(
                            text = message.replyTo.senderName,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isMine) Color.White else AccentBlue
                        )
                        Text(
                            text = message.replyTo.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isMine) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Image payload if present
            if (message.imageUrl.isNotBlank()) {
                val bitmap = remember(message.imageUrl) {
                    try {
                        val clean = message.imageUrl.substringAfter("base64,")
                        val bytes = Base64.decode(clean, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } catch (_: Exception) {
                        null
                    }
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Shared image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                } else if (message.imageUrl.startsWith("http")) {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = "Shared image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Message text
            if (message.text.isNotBlank()) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Timestamp & Delivery status
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = timeColor
                )

                if (isMine) {
                    Spacer(modifier = Modifier.width(4.dp))
                    when (message.status) {
                        MessageStatus.SENT -> {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Sent",
                                tint = timeColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        MessageStatus.DELIVERED -> {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Delivered",
                                tint = timeColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        MessageStatus.SEEN -> {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Seen",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingIndicatorBubble() {
    val isDark = MaterialTheme.colorScheme.surface == Color(0xFF171A21)
    val bubbleColor = if (isDark) BubbleReceivedDark else BubbleReceivedLight

    val dot1Anim = remember { Animatable(0f) }
    val dot2Anim = remember { Animatable(0f) }
    val dot3Anim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        dot1Anim.animateTo(
            targetValue = -5f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(130)
        dot2Anim.animateTo(
            targetValue = -5f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(260)
        dot3Anim.animateTo(
            targetValue = -5f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bubbleColor)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .offset(y = dot1Anim.value.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Box(
                modifier = Modifier
                    .offset(y = dot2Anim.value.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Box(
                modifier = Modifier
                    .offset(y = dot3Anim.value.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

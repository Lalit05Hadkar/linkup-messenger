package com.example.data.model

data class User(
    val uid: String = "",
    val fullName: String = "",
    val fullNameLower: String = "",
    val email: String = "",
    val userId: String = "",
    val profileImage: String = "",
    val bio: String = "",
    val lastSeen: Long = System.currentTimeMillis(),
    val isOnline: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val pinnedChats: List<String> = emptyList(),
    val allowRequests: Boolean = true,
    val showLastSeen: Boolean = true,
    val fcmTokens: List<String> = emptyList()
)

enum class MessageStatus {
    SENT, DELIVERED, SEEN
}

data class ReplyPreview(
    val messageId: String = "",
    val text: String = "",
    val senderName: String = "",
    val senderId: String = ""
)

data class Message(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT,
    val replyTo: ReplyPreview? = null
)

data class Chat(
    val chatId: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageSenderId: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCounts: Map<String, Int> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class RequestStatus {
    PENDING, ACCEPTED, REJECTED
}

data class FriendRequest(
    val requestId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val senderName: String = "",
    val senderUserId: String = "",
    val senderAvatar: String = "",
    val receiverName: String = "",
    val receiverUserId: String = "",
    val receiverAvatar: String = "",
    val status: RequestStatus = RequestStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)

enum class UserIdStatus {
    EMPTY, CHECKING, AVAILABLE, TAKEN, INVALID
}

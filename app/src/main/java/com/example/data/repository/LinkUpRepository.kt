package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.insforge.InsForgeClient
import com.example.data.model.Chat
import com.example.data.model.FriendRequest
import com.example.data.model.Message
import com.example.data.model.MessageStatus
import com.example.data.model.ReplyPreview
import com.example.data.model.RequestStatus
import com.example.data.model.User
import com.example.data.model.UserIdStatus
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class LinkUpRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("linkup_messenger_prefs", Context.MODE_PRIVATE)
    private val repositoryScope = CoroutineScope(Dispatchers.Default)

    // InsForge Backend Client
    val insforge = InsForgeClient()

    // Current logged-in user
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // App Theme Mode
    private val _themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    // All known users in the LinkUp network for search and discovery
    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

    // Friends of the current user
    private val _friends = MutableStateFlow<List<User>>(emptyList())
    val friends: StateFlow<List<User>> = _friends.asStateFlow()

    // Friend requests
    private val _friendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val friendRequests: StateFlow<List<FriendRequest>> = _friendRequests.asStateFlow()

    // Active Chats
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    // Messages mapped by chatId
    private val _messages = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
    val messages: StateFlow<Map<String, List<Message>>> = _messages.asStateFlow()

    // Typing status per chat
    private val _typingStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val typingStatus: StateFlow<Map<String, Boolean>> = _typingStatus.asStateFlow()

    private var syncJob: Job? = null

    init {
        loadPersistedData()
        startInsForgeSync()
    }

    private fun loadPersistedData() {
        val savedTheme = prefs.getString("theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        _themeMode.value = try {
            AppThemeMode.valueOf(savedTheme)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }

        val savedToken = prefs.getString("insforge_access_token", null)
        if (!savedToken.isNullOrBlank()) {
            insforge.currentAccessToken = savedToken
        }

        val savedUid = prefs.getString("user_uid", null)
        if (savedUid != null) {
            val user = User(
                uid = savedUid,
                fullName = prefs.getString("user_fullName", "Alex") ?: "Alex",
                fullNameLower = (prefs.getString("user_fullName", "Alex") ?: "Alex").lowercase(),
                email = prefs.getString("user_email", "user@linkup.chat") ?: "user@linkup.chat",
                userId = prefs.getString("user_userId", "") ?: "",
                profileImage = prefs.getString("user_profileImage", "") ?: "",
                bio = prefs.getString("user_bio", "Hey there! I am using LinkUp Messenger.") ?: "",
                pinnedChats = prefs.getStringSet("user_pinnedChats", emptySet())?.toList() ?: emptyList(),
                allowRequests = prefs.getBoolean("user_allowRequests", true),
                showLastSeen = prefs.getBoolean("user_showLastSeen", true)
            )
            _currentUser.value = user
        }
    }

    fun startInsForgeSync() {
        syncJob?.cancel()
        syncJob = repositoryScope.launch {
            _currentUser.value?.let { user ->
                if (user.userId.isNotBlank()) {
                    val ok = insforge.upsertUser(user)
                    Log.d("LinkUpRepository", "Initial user upsert to InsForge: $ok (${user.userId})")
                }
            }
            while (true) {
                try {
                    // 1. Fetch all network users
                    val remoteUsers = insforge.getAllUsers()
                    Log.d("LinkUpRepository", "Fetched remote users count: ${remoteUsers.size}")
                    if (remoteUsers.isNotEmpty()) {
                        _allUsers.value = remoteUsers
                    }

                    val current = _currentUser.value
                    if (current != null) {
                        // 2. Fetch friend requests
                        val remoteRequests = insforge.getFriendRequests(current.uid)
                        _friendRequests.value = remoteRequests

                        // Compute friends from accepted requests
                        val acceptedSenderIds = remoteRequests
                            .filter { it.status == RequestStatus.ACCEPTED && it.receiverId == current.uid }
                            .map { it.senderId }
                        val acceptedReceiverIds = remoteRequests
                            .filter { it.status == RequestStatus.ACCEPTED && it.senderId == current.uid }
                            .map { it.receiverId }
                        val friendIds = (acceptedSenderIds + acceptedReceiverIds).toSet()

                        val friendsList = _allUsers.value.filter { friendIds.contains(it.uid) }
                        _friends.value = friendsList

                        // 3. Fetch chats
                        val remoteChats = insforge.getChats(current.uid)
                        if (remoteChats.isNotEmpty()) {
                            _chats.value = remoteChats

                            // 4. Fetch messages for chats
                            remoteChats.forEach { chat ->
                                val remoteMsgs = insforge.getMessages(chat.chatId)
                                if (remoteMsgs.isNotEmpty()) {
                                    val currentMap = _messages.value.toMutableMap()
                                    currentMap[chat.chatId] = remoteMsgs
                                    _messages.value = currentMap
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("LinkUpRepository", "InsForge sync note: ${e.message}")
                }
                delay(3000)
            }
        }
    }

    // --- Authentication ---

    fun signInWithGoogle(
        email: String,
        displayName: String? = null,
        googleUid: String? = null,
        idToken: String? = null,
        onSuccess: (isNewUser: Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        val resolvedEmail = email.trim()
        if (resolvedEmail.isEmpty() || !resolvedEmail.contains("@")) {
            onError("Invalid Google account email.")
            return
        }
        val resolvedName = displayName ?: (if (resolvedEmail.contains("@")) resolvedEmail.substringBefore("@").replace(".", " ").capitalizeWords() else "LinkUp User")
        val uid = googleUid ?: ("google_" + UUID.nameUUIDFromBytes(resolvedEmail.toByteArray()).toString().take(12))

        repositoryScope.launch {
            try {
                val remoteUser = insforge.getUser(uid)
                val isExistingUser = remoteUser != null && remoteUser.userId.isNotBlank()

                val user = remoteUser ?: User(
                    uid = uid,
                    fullName = resolvedName,
                    fullNameLower = resolvedName.lowercase(),
                    email = resolvedEmail,
                    userId = "",
                    profileImage = "",
                    bio = "",
                    createdAt = System.currentTimeMillis()
                )
                saveUser(user)
                insforge.upsertUser(user)
                startInsForgeSync()
                withContext(Dispatchers.Main) {
                    onSuccess(!isExistingUser)
                }
            } catch (e: Exception) {
                Log.e("LinkUpRepository", "signInWithGoogle error", e)
                withContext(Dispatchers.Main) {
                    onError(e.localizedMessage ?: "Google Sign-In failed.")
                }
            }
        }
    }

    fun signInWithEmail(
        email: String,
        password: String,
        onSuccess: (isNewUser: Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            onError("Please enter a valid email address.")
            return
        }
        if (password.length < 6) {
            onError("Password must be at least 6 characters.")
            return
        }

        repositoryScope.launch {
            val res = insforge.signIn(trimmedEmail, password)
            if (res.isSuccess) {
                val authUser = res.getOrNull()!!
                prefs.edit()
                    .putString("insforge_access_token", authUser.accessToken)
                    .putString("insforge_refresh_token", authUser.refreshToken)
                    .apply()
                insforge.currentAccessToken = authUser.accessToken

                val remoteUser = insforge.getUser(authUser.id)
                val isExistingUser = remoteUser != null && remoteUser.userId.isNotBlank()
                val user = remoteUser ?: User(
                    uid = authUser.id,
                    fullName = if (authUser.name.isNotBlank()) authUser.name else trimmedEmail.substringBefore("@").replace(".", " ").capitalizeWords(),
                    fullNameLower = (if (authUser.name.isNotBlank()) authUser.name else trimmedEmail.substringBefore("@")).lowercase(),
                    email = trimmedEmail,
                    userId = "",
                    createdAt = System.currentTimeMillis()
                )
                saveUser(user)
                startInsForgeSync()
                withContext(Dispatchers.Main) {
                    onSuccess(!isExistingUser)
                }
            } else {
                val errMsg = res.exceptionOrNull()?.message ?: "Invalid email or password. Please try again."
                withContext(Dispatchers.Main) {
                    onError(errMsg)
                }
            }
        }
    }

    fun createAccountWithEmail(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            onError("Please enter a valid email address.")
            return
        }
        if (password.length < 6) {
            onError("Password must be at least 6 characters.")
            return
        }

        repositoryScope.launch {
            val name = trimmedEmail.substringBefore("@").replace(".", " ").capitalizeWords()
            val signUpRes = insforge.signUp(trimmedEmail, password, name)
            if (signUpRes.isSuccess) {
                val authUser = signUpRes.getOrNull()!!
                prefs.edit()
                    .putString("insforge_access_token", authUser.accessToken)
                    .putString("insforge_refresh_token", authUser.refreshToken)
                    .apply()
                insforge.currentAccessToken = authUser.accessToken

                val user = User(
                    uid = authUser.id,
                    fullName = if (authUser.name.isNotBlank()) authUser.name else name,
                    fullNameLower = (if (authUser.name.isNotBlank()) authUser.name else name).lowercase(),
                    email = trimmedEmail,
                    userId = "",
                    createdAt = System.currentTimeMillis()
                )
                saveUser(user)
                insforge.upsertUser(user)
                startInsForgeSync()
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } else {
                val errMsg = signUpRes.exceptionOrNull()?.message ?: "Account registration failed. Please try a different email."
                withContext(Dispatchers.Main) {
                    onError(errMsg)
                }
            }
        }
    }

    // --- Profile & Privacy ---

    fun completeProfileSetup(fullName: String, userId: String, bio: String, avatar: String) {
        val current = _currentUser.value ?: User(uid = "user_" + UUID.randomUUID().toString().take(8))
        val cleanUserId = userId.trim().removePrefix("@").lowercase()
        val updated = current.copy(
            fullName = fullName.trim(),
            fullNameLower = fullName.trim().lowercase(),
            userId = cleanUserId,
            bio = bio.trim(),
            profileImage = avatar
        )
        saveUser(updated)

        repositoryScope.launch {
            insforge.upsertUser(updated)
            startInsForgeSync()
        }
    }

    fun updateProfile(fullName: String, bio: String, avatar: String, newUserId: String) {
        val current = _currentUser.value ?: return
        val cleanNewId = newUserId.trim().removePrefix("@").lowercase()
        val updated = current.copy(
            fullName = fullName.trim(),
            fullNameLower = fullName.trim().lowercase(),
            bio = bio.trim(),
            profileImage = avatar,
            userId = cleanNewId
        )
        saveUser(updated)

        repositoryScope.launch {
            insforge.upsertUser(updated)
            startInsForgeSync()
        }
    }

    fun updatePrivacySettings(showLastSeen: Boolean, allowRequests: Boolean) {
        val current = _currentUser.value ?: return
        val updated = current.copy(
            showLastSeen = showLastSeen,
            allowRequests = allowRequests
        )
        saveUser(updated)

        repositoryScope.launch {
            insforge.upsertUser(updated)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun logout() {
        syncJob?.cancel()
        _currentUser.value = null
        insforge.currentAccessToken = null
        prefs.edit().clear().apply()
    }

    fun deleteAccount() {
        val current = _currentUser.value
        if (current != null) {
            repositoryScope.launch {
                insforge.deleteUser(current.uid)
            }
        }
        logout()
    }

    // --- User ID Validation ---

    suspend fun validateUserId(rawId: String, currentUid: String): UserIdStatus {
        val trimmed = rawId.trim().removePrefix("@").lowercase()
        if (trimmed.isEmpty()) return UserIdStatus.EMPTY

        val regex = Regex("^(?!\\.)[a-z0-9._]{4,20}(?<!\\.)$")
        if (!regex.matches(trimmed)) {
            return UserIdStatus.INVALID
        }

        // Check local known users first
        val isLocalTaken = _allUsers.value.any { it.userId.equals(trimmed, ignoreCase = true) && it.uid != currentUid }
        if (isLocalTaken) return UserIdStatus.TAKEN

        // Check InsForge users table
        return try {
            val available = insforge.checkUserIdAvailable(trimmed, currentUid)
            if (available) UserIdStatus.AVAILABLE else UserIdStatus.TAKEN
        } catch (_: Exception) {
            UserIdStatus.AVAILABLE
        }
    }

    // --- Friend Requests & Friends ---

    fun sendFriendRequest(targetUser: User) {
        val current = _currentUser.value ?: return
        val requestId = "${current.uid}_${targetUser.uid}"
        val request = FriendRequest(
            requestId = requestId,
            senderId = current.uid,
            receiverId = targetUser.uid,
            senderName = current.fullName,
            senderUserId = current.userId,
            senderAvatar = current.profileImage,
            receiverName = targetUser.fullName,
            receiverUserId = targetUser.userId,
            receiverAvatar = targetUser.profileImage,
            status = RequestStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )

        _friendRequests.value = _friendRequests.value.filter { it.requestId != requestId } + request

        repositoryScope.launch {
            insforge.upsertFriendRequest(request)
        }
    }

    fun acceptFriendRequest(request: FriendRequest) {
        _friendRequests.value = _friendRequests.value.map {
            if (it.requestId == request.requestId) it.copy(status = RequestStatus.ACCEPTED) else it
        }

        val friend = _allUsers.value.find { it.uid == request.senderId }
            ?: User(
                uid = request.senderId,
                fullName = request.senderName,
                userId = request.senderUserId,
                profileImage = request.senderAvatar
            )

        if (_friends.value.none { it.uid == friend.uid }) {
            _friends.value = _friends.value + friend
        }

        val current = _currentUser.value ?: return
        val chatId = getChatId(current.uid, friend.uid)
        val newChat = Chat(
            chatId = chatId,
            participants = listOf(current.uid, friend.uid),
            lastMessage = "You are now connected on LinkUp!",
            lastMessageSenderId = current.uid,
            lastMessageTime = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )
        if (_chats.value.none { it.chatId == chatId }) {
            _chats.value = listOf(newChat) + _chats.value
        }

        repositoryScope.launch {
            insforge.updateFriendRequestStatus(request.requestId, RequestStatus.ACCEPTED)
            insforge.upsertChat(newChat)
        }
    }

    fun rejectFriendRequest(request: FriendRequest) {
        _friendRequests.value = _friendRequests.value.map {
            if (it.requestId == request.requestId) it.copy(status = RequestStatus.REJECTED) else it
        }

        repositoryScope.launch {
            insforge.updateFriendRequestStatus(request.requestId, RequestStatus.REJECTED)
        }
    }

    fun removeFriend(friendUid: String) {
        _friends.value = _friends.value.filter { it.uid != friendUid }
        val current = _currentUser.value ?: return
        val requestId = "${friendUid}_${current.uid}"
        val reqAlt = "${current.uid}_$friendUid"
        repositoryScope.launch {
            insforge.updateFriendRequestStatus(requestId, RequestStatus.REJECTED)
            insforge.updateFriendRequestStatus(reqAlt, RequestStatus.REJECTED)
        }
    }

    // --- Chat & Messaging ---

    fun togglePinChat(chatId: String): Boolean {
        val current = _currentUser.value ?: return false
        val currentPinned = current.pinnedChats.toMutableList()
        val isPinned = currentPinned.contains(chatId)

        if (isPinned) {
            currentPinned.remove(chatId)
        } else {
            if (currentPinned.size >= 3) {
                return false
            }
            currentPinned.add(chatId)
        }

        val updated = current.copy(pinnedChats = currentPinned)
        saveUser(updated)

        repositoryScope.launch {
            insforge.upsertUser(updated)
        }
        return true
    }

    fun markChatRead(chatId: String) {
        val current = _currentUser.value ?: return
        _chats.value = _chats.value.map { chat ->
            if (chat.chatId == chatId) {
                val updatedUnread = chat.unreadCounts.toMutableMap()
                updatedUnread[current.uid] = 0
                chat.copy(unreadCounts = updatedUnread)
            } else chat
        }

        val chatMsgs = _messages.value[chatId] ?: return
        val updatedMsgs = chatMsgs.map { msg ->
            if (msg.senderId != current.uid && msg.status != MessageStatus.SEEN) {
                msg.copy(status = MessageStatus.SEEN)
            } else msg
        }
        val newMap = _messages.value.toMutableMap()
        newMap[chatId] = updatedMsgs
        _messages.value = newMap
    }

    fun sendMessage(chatId: String, text: String, replyTo: ReplyPreview?, imageUrl: String = "") {
        val current = _currentUser.value ?: return
        val messageId = "msg_" + UUID.randomUUID().toString()
        val newMessage = Message(
            messageId = messageId,
            chatId = chatId,
            senderId = current.uid,
            text = text.trim(),
            imageUrl = imageUrl,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENT,
            replyTo = replyTo
        )

        val currentList = _messages.value[chatId] ?: emptyList()
        val updatedList = currentList + newMessage
        val newMessagesMap = _messages.value.toMutableMap()
        newMessagesMap[chatId] = updatedList
        _messages.value = newMessagesMap

        val otherParticipant = getOtherParticipantId(chatId, current.uid)
        val previewText = when {
            text.isNotBlank() -> text.trim()
            imageUrl.isNotBlank() -> "📷 Image"
            else -> "Message"
        }

        val updatedChat = Chat(
            chatId = chatId,
            participants = listOf(current.uid, otherParticipant),
            lastMessage = previewText,
            lastMessageSenderId = current.uid,
            lastMessageTime = System.currentTimeMillis()
        )

        _chats.value = (_chats.value.filter { it.chatId != chatId } + updatedChat)
            .sortedByDescending { it.lastMessageTime }

        repositoryScope.launch {
            // Write to InsForge Backend
            insforge.upsertChat(updatedChat)
            insforge.sendMessage(newMessage)
        }
    }

    private fun setTyping(chatId: String, isTyping: Boolean) {
        val map = _typingStatus.value.toMutableMap()
        map[chatId] = isTyping
        _typingStatus.value = map
    }

    private fun updateMessageStatus(chatId: String, messageId: String, status: MessageStatus) {
        val currentList = _messages.value[chatId] ?: return
        val updated = currentList.map { msg ->
            if (msg.messageId == messageId) msg.copy(status = status) else msg
        }
        val map = _messages.value.toMutableMap()
        map[chatId] = updated
        _messages.value = map
    }

    fun getChatId(uidA: String, uidB: String): String {
        return if (uidA < uidB) "${uidA}__${uidB}" else "${uidB}__${uidA}"
    }

    fun getOtherParticipantId(chatId: String, myUid: String): String {
        val chat = _chats.value.find { it.chatId == chatId }
        if (chat != null && chat.participants.isNotEmpty()) {
            val other = chat.participants.firstOrNull { it != myUid }
            if (!other.isNullOrBlank()) return other
        }
        if (chatId.contains("__")) {
            val parts = chatId.split("__")
            return if (parts.firstOrNull() == myUid) parts.getOrNull(1) ?: "" else parts.firstOrNull() ?: ""
        }
        if (chatId.startsWith("${myUid}_")) {
            return chatId.removePrefix("${myUid}_")
        }
        if (chatId.endsWith("_$myUid")) {
            return chatId.removeSuffix("_$myUid")
        }
        val parts = chatId.split("_")
        return if (parts.firstOrNull() == myUid) parts.getOrNull(1) ?: "" else parts.firstOrNull() ?: ""
    }

    private fun saveUser(user: User) {
        _currentUser.value = user
        persistUserFields(user)
    }

    private fun persistUserFields(user: User) {
        prefs.edit()
            .putString("user_uid", user.uid)
            .putString("user_fullName", user.fullName)
            .putString("user_email", user.email)
            .putString("user_userId", user.userId)
            .putString("user_profileImage", user.profileImage)
            .putString("user_bio", user.bio)
            .putStringSet("user_pinnedChats", user.pinnedChats.toSet())
            .putBoolean("user_allowRequests", user.allowRequests)
            .putBoolean("user_showLastSeen", user.showLastSeen)
            .apply()
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}

package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Chat
import com.example.data.model.FriendRequest
import com.example.data.model.Message
import com.example.data.model.ReplyPreview
import com.example.data.model.RequestStatus
import com.example.data.model.User
import com.example.data.model.UserIdStatus
import com.example.data.repository.LinkUpRepository
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AppScreen {
    object Splash : AppScreen
    object Auth : AppScreen
    object ProfileSetup : AppScreen
    object Home : AppScreen
    data class ChatDetail(val friendUid: String, val chatId: String) : AppScreen
    object EditProfile : AppScreen
    data class VoiceCall(
        val friendUid: String,
        val friendName: String,
        val friendAvatar: String = ""
    ) : AppScreen
    object LiveVoice : AppScreen
}

enum class HomeTab {
    CHATS, FRIENDS, SEARCH, PROFILE
}

enum class FriendsTab {
    ALL_FRIENDS, REQUESTS, SENT
}

class LinkUpViewModel(application: Application) : AndroidViewModel(application) {
    val repository = LinkUpRepository(application.applicationContext)

    // Current screen navigation
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Splash)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Current tab inside Home
    private val _currentTab = MutableStateFlow(HomeTab.CHATS)
    val currentTab: StateFlow<HomeTab> = _currentTab.asStateFlow()

    // Friends Screen Tab
    private val _currentFriendsTab = MutableStateFlow(FriendsTab.ALL_FRIENDS)
    val currentFriendsTab: StateFlow<FriendsTab> = _currentFriendsTab.asStateFlow()

    // Search query & results
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    // Chats filter query
    private val _chatsSearchQuery = MutableStateFlow("")
    val chatsSearchQuery: StateFlow<String> = _chatsSearchQuery.asStateFlow()

    // Live User ID validation status for Profile Setup / Edit Profile
    private val _userIdValidation = MutableStateFlow(UserIdStatus.EMPTY)
    val userIdValidation: StateFlow<UserIdStatus> = _userIdValidation.asStateFlow()

    // Currently active reply inside chat detail
    private val _activeReply = MutableStateFlow<ReplyPreview?>(null)
    val activeReply: StateFlow<ReplyPreview?> = _activeReply.asStateFlow()

    // Transient UI toast / snackbar message
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Auth state & errors
    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    fun clearAuthError() {
        _authErrorMessage.value = null
    }

    fun startVoiceCall(friendUid: String, friendName: String, friendAvatar: String = "") {
        _currentScreen.value = AppScreen.VoiceCall(friendUid, friendName, friendAvatar)
    }

    private var searchJob: Job? = null
    private var validationJob: Job? = null

    // Filtered chats combining search query & pinned status
    val filteredChats: StateFlow<List<Chat>> = combine(
        repository.chats,
        _chatsSearchQuery,
        repository.allUsers
    ) { chats, query, users ->
        if (query.isBlank()) {
            chats
        } else {
            chats.filter { chat ->
                val otherUid = repository.getOtherParticipantId(chat.chatId, repository.currentUser.value?.uid ?: "")
                val friend = users.find { it.uid == otherUid }
                val nameMatch = friend?.fullName?.contains(query, ignoreCase = true) == true
                val msgMatch = chat.lastMessage.contains(query, ignoreCase = true)
                nameMatch || msgMatch
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Handle splash timer & routing
        viewModelScope.launch {
            delay(1200)
            val user = repository.currentUser.value
            when {
                user == null -> _currentScreen.value = AppScreen.Auth
                user.userId.isBlank() -> _currentScreen.value = AppScreen.ProfileSetup
                else -> _currentScreen.value = AppScreen.Home
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun switchTab(tab: HomeTab) {
        _currentTab.value = tab
    }

    fun setFriendsTab(tab: FriendsTab) {
        _currentFriendsTab.value = tab
    }

    fun onChatsSearchQueryChanged(query: String) {
        _chatsSearchQuery.value = query
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()

        val cleanQuery = query.trim().removePrefix("@").lowercase()
        if (cleanQuery.isEmpty()) {
            _searchResults.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // 300ms debounce as specified
            val myUid = repository.currentUser.value?.uid ?: ""
            val matches = repository.allUsers.value.filter {
                it.uid != myUid && (it.userId.lowercase().contains(cleanQuery) || it.fullNameLower.contains(cleanQuery))
            }
            _searchResults.value = matches
        }
    }

    fun onUserIdInputChanged(input: String, currentUid: String = repository.currentUser.value?.uid ?: "") {
        validationJob?.cancel()
        val clean = input.trim().removePrefix("@").lowercase()
        if (clean.isEmpty()) {
            _userIdValidation.value = UserIdStatus.EMPTY
            return
        }

        _userIdValidation.value = UserIdStatus.CHECKING
        validationJob = viewModelScope.launch {
            val status = repository.validateUserId(clean, currentUid)
            _userIdValidation.value = status
        }
    }

    fun signInWithGoogle(
        email: String,
        displayName: String? = null,
        googleUid: String? = null,
        idToken: String? = null
    ) {
        _authLoading.value = true
        _authErrorMessage.value = null
        repository.signInWithGoogle(
            email = email,
            displayName = displayName,
            googleUid = googleUid,
            idToken = idToken,
            onSuccess = { isNewUser ->
                _authLoading.value = false
                val user = repository.currentUser.value
                if (!isNewUser && user != null && user.userId.isNotBlank()) {
                    _currentScreen.value = AppScreen.Home
                } else {
                    _currentScreen.value = AppScreen.ProfileSetup
                }
            },
            onError = { err ->
                _authLoading.value = false
                _authErrorMessage.value = err
                _toastMessage.value = err
            }
        )
    }

    fun signInWithEmail(email: String, password: String) {
        _authLoading.value = true
        _authErrorMessage.value = null
        repository.signInWithEmail(
            email = email,
            password = password,
            onSuccess = { isNewUser ->
                _authLoading.value = false
                val user = repository.currentUser.value
                if (user != null && user.userId.isNotBlank()) {
                    _currentScreen.value = AppScreen.Home
                } else {
                    _currentScreen.value = AppScreen.ProfileSetup
                }
            },
            onError = { err ->
                _authLoading.value = false
                _authErrorMessage.value = err
                _toastMessage.value = err
            }
        )
    }

    fun createAccountWithEmail(email: String, password: String) {
        _authLoading.value = true
        _authErrorMessage.value = null
        repository.createAccountWithEmail(
            email = email,
            password = password,
            onSuccess = {
                _authLoading.value = false
                _currentScreen.value = AppScreen.ProfileSetup
            },
            onError = { err ->
                _authLoading.value = false
                _authErrorMessage.value = err
                _toastMessage.value = err
            }
        )
    }

    fun completeProfileSetup(fullName: String, userId: String, bio: String, avatar: String) {
        val cleanId = userId.trim().removePrefix("@").lowercase()
        repository.completeProfileSetup(fullName, cleanId, bio, avatar)
        _currentScreen.value = AppScreen.Home
    }

    fun updateProfile(fullName: String, bio: String, avatar: String, newUserId: String) {
        val cleanId = newUserId.trim().removePrefix("@").lowercase()
        repository.updateProfile(fullName, bio, avatar, cleanId)
        _currentScreen.value = AppScreen.Home
        showToast("Profile updated successfully")
    }

    fun openChat(friend: User) {
        val myUid = repository.currentUser.value?.uid ?: "current_user"
        val chatId = repository.getChatId(myUid, friend.uid)

        // Ensure chat exists
        if (repository.chats.value.none { it.chatId == chatId }) {
            repository.acceptFriendRequest(
                FriendRequest(
                    requestId = "${friend.uid}_$myUid",
                    senderId = friend.uid,
                    receiverId = myUid,
                    senderName = friend.fullName,
                    senderUserId = friend.userId,
                    senderAvatar = friend.profileImage,
                    status = RequestStatus.ACCEPTED
                )
            )
        }

        repository.markChatRead(chatId)
        _activeReply.value = null
        _currentScreen.value = AppScreen.ChatDetail(friendUid = friend.uid, chatId = chatId)
    }

    fun openChatById(chatId: String) {
        val myUid = repository.currentUser.value?.uid ?: "current_user"
        val otherUid = repository.getOtherParticipantId(chatId, myUid)
        repository.markChatRead(chatId)
        _activeReply.value = null
        _currentScreen.value = AppScreen.ChatDetail(friendUid = otherUid, chatId = chatId)
    }

    fun togglePinChat(chatId: String) {
        val success = repository.togglePinChat(chatId)
        if (!success) {
            showToast("Maximum 3 pinned chats reached")
        } else {
            val isPinned = repository.currentUser.value?.pinnedChats?.contains(chatId) == true
            showToast(if (isPinned) "Chat pinned to top" else "Chat unpinned")
        }
    }

    fun setReplyTo(message: Message, senderName: String) {
        _activeReply.value = ReplyPreview(
            messageId = message.messageId,
            text = message.text,
            senderName = senderName,
            senderId = message.senderId
        )
    }

    fun clearReply() {
        _activeReply.value = null
    }

    fun sendMessage(chatId: String, text: String, imageUrl: String = "") {
        if (text.isBlank() && imageUrl.isBlank()) return
        repository.sendMessage(chatId, text, _activeReply.value, imageUrl)
        _activeReply.value = null
    }

    fun sendFriendRequest(targetUser: User) {
        repository.sendFriendRequest(targetUser)
        showToast("Friend request sent to @${targetUser.userId}")
    }

    fun acceptFriendRequest(request: FriendRequest) {
        repository.acceptFriendRequest(request)
        showToast("Connected with ${request.senderName}!")
    }

    fun rejectFriendRequest(request: FriendRequest) {
        repository.rejectFriendRequest(request)
        showToast("Request declined")
    }

    fun removeFriend(friend: User) {
        repository.removeFriend(friend.uid)
        showToast("Removed ${friend.fullName} from friends")
    }

    fun setThemeMode(mode: AppThemeMode) {
        repository.setThemeMode(mode)
    }

    fun updatePrivacySettings(showLastSeen: Boolean, allowRequests: Boolean) {
        repository.updatePrivacySettings(showLastSeen, allowRequests)
        showToast("Privacy settings updated")
    }

    fun logout() {
        repository.logout()
        _currentScreen.value = AppScreen.Auth
    }

    fun deleteAccount() {
        repository.deleteAccount()
        _currentScreen.value = AppScreen.Auth
        showToast("Account deleted")
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun handleBack(): Boolean {
        return when (val screen = _currentScreen.value) {
            is AppScreen.ChatDetail -> {
                _currentScreen.value = AppScreen.Home
                true
            }
            is AppScreen.EditProfile -> {
                _currentScreen.value = AppScreen.Home
                true
            }
            is AppScreen.ProfileSetup -> {
                _currentScreen.value = AppScreen.Auth
                true
            }
            is AppScreen.Home -> {
                if (_currentTab.value != HomeTab.CHATS) {
                    _currentTab.value = HomeTab.CHATS
                    true
                } else false
            }
            is AppScreen.LiveVoice -> {
                _currentScreen.value = AppScreen.Home
                true
            }
            is AppScreen.VoiceCall -> {
                val user = repository.currentUser.value
                val myUid = user?.uid ?: ""
                val chatId = repository.getChatId(myUid, screen.friendUid)
                _currentScreen.value = AppScreen.ChatDetail(screen.friendUid, chatId)
                true
            }
            else -> false
        }
    }
}

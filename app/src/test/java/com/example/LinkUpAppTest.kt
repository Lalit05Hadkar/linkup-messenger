package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.MessageStatus
import com.example.data.model.ReplyPreview
import com.example.data.model.RequestStatus
import com.example.data.model.User
import com.example.data.model.UserIdStatus
import com.example.data.gemini.GeminiClient
import com.example.data.repository.LinkUpRepository
import com.example.ui.theme.AppThemeMode
import com.example.viewmodel.AppScreen
import com.example.viewmodel.LinkUpViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LinkUpAppTest {

    private lateinit var context: Context
    private lateinit var repository: LinkUpRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val options = com.google.firebase.FirebaseOptions.Builder()
            .setApplicationId("com.aistudio.linkup.mzypxr")
            .setProjectId("gen-lang-client-0226006539")
            .setApiKey("AIzaSyFakeKeyForLocalRobolectricTesting123")
            .build()
        if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
            com.google.firebase.FirebaseApp.initializeApp(context, options)
        }
        repository = LinkUpRepository(context)
        repository.signInWithGoogle {}
    }

    @Test
    fun testAuthenticationAndProfileSetupFlow() = runBlocking {
        // 1. Initial Google Sign In for new user
        repository.signInWithGoogle { isNew ->
            assertTrue(isNew)
        }
        val user = repository.currentUser.value
        assertNotNull(user)
        assertTrue(user!!.uid.isNotBlank())

        // 2. Validate user ID uniqueness
        val statusAvailable = repository.validateUserId("unique_coder_99", user.uid)
        assertEquals(UserIdStatus.AVAILABLE, statusAvailable)

        val statusTaken = repository.validateUserId("sarah_c", user.uid) // existing seed user
        assertEquals(UserIdStatus.TAKEN, statusTaken)

        val statusInvalid = repository.validateUserId(".invalid.", user.uid)
        assertEquals(UserIdStatus.INVALID, statusInvalid)

        // 3. Complete Profile Setup
        repository.completeProfileSetup(
            fullName = "Alex Tester",
            userId = "alex_tester_99",
            bio = "Building LinkUp Messenger on Android",
            avatar = ""
        )

        val updatedUser = repository.currentUser.value
        assertEquals("Alex Tester", updatedUser?.fullName)
        assertEquals("alex_tester_99", updatedUser?.userId)
        assertEquals("Building LinkUp Messenger on Android", updatedUser?.bio)
    }

    @Test
    fun testFriendRequestAndChatCreation() {
        val targetUser = User(
            uid = "uid_david",
            fullName = "David Kim",
            userId = "david_k"
        )

        // 1. Send friend request
        repository.sendFriendRequest(targetUser)
        val requests = repository.friendRequests.value
        val req = requests.find { it.receiverId == "uid_david" }
        assertNotNull(req)
        assertEquals(RequestStatus.PENDING, req!!.status)

        // 2. Incoming request acceptance
        val incomingReq = requests.find { it.senderId == "uid_marcus" }
        assertNotNull(incomingReq)
        repository.acceptFriendRequest(incomingReq!!)

        val updatedFriends = repository.friends.value
        assertTrue(updatedFriends.any { it.uid == "uid_marcus" })

        // Chat should be created
        val myUid = repository.currentUser.value?.uid ?: "current_user"
        val chatId = repository.getChatId(myUid, "uid_marcus")
        assertTrue(repository.chats.value.any { it.chatId == chatId })
    }

    @Test
    fun testMessagingAndReplyThreading() {
        val myUid = repository.currentUser.value?.uid ?: "current_user"
        val chatId = repository.getChatId(myUid, "uid_sarah")

        val replyPreview = ReplyPreview(
            messageId = "m1",
            text = "Hi! Welcome to LinkUp Messenger",
            senderName = "Sarah",
            senderId = "uid_sarah"
        )

        repository.sendMessage(chatId, "Testing reply feature!", replyPreview)

        val msgs = repository.messages.value[chatId]
        assertNotNull(msgs)
        val sentMsg = msgs!!.find { it.text == "Testing reply feature!" }
        assertNotNull(sentMsg)
        assertEquals("m1", sentMsg!!.replyTo?.messageId)
        assertEquals(MessageStatus.SENT, sentMsg.status)

        // Mark chat as read
        repository.markChatRead(chatId)
        val chat = repository.chats.value.find { it.chatId == chatId }
        assertEquals(0, chat?.unreadCounts?.get(myUid) ?: 0)
    }

    @Test
    fun testPinnedChatsMaxThreeLimit() {
        val chat1 = "chat_1"
        val chat2 = "chat_2"
        val chat3 = "chat_3"
        val chat4 = "chat_4"

        assertTrue(repository.togglePinChat(chat1))
        assertTrue(repository.togglePinChat(chat2))
        assertTrue(repository.togglePinChat(chat3))

        // 4th pin should be rejected by the max 3 constraint
        assertFalse(repository.togglePinChat(chat4))
        assertEquals(3, repository.currentUser.value?.pinnedChats?.size)

        // Unpin chat 1
        assertTrue(repository.togglePinChat(chat1))
        assertEquals(2, repository.currentUser.value?.pinnedChats?.size)
    }

    @Test
    fun testThemeModeAndPrivacySettings() {
        repository.setThemeMode(AppThemeMode.DARK)
        assertEquals(AppThemeMode.DARK, repository.themeMode.value)

        repository.setThemeMode(AppThemeMode.LIGHT)
        assertEquals(AppThemeMode.LIGHT, repository.themeMode.value)

        repository.updatePrivacySettings(showLastSeen = false, allowRequests = false)
        val user = repository.currentUser.value
        assertEquals(false, user?.showLastSeen)
        assertEquals(false, user?.allowRequests)
    }

    @Test
    fun testGeminiImageGenerationAndDispatchToChat() = runBlocking {
        val geminiClient = GeminiClient(context)
        val result = geminiClient.generateOrEditImage(
            prompt = "A futuristic 3D LinkUp messenger logo",
            aspectRatio = "1:1"
        )
        assertTrue(result.isSuccess)
        val imageBase64 = result.getOrNull()
        assertNotNull(imageBase64)
        assertTrue(imageBase64!!.isNotBlank())

        val myUid = repository.currentUser.value?.uid ?: "current_user"
        val chatId = repository.getChatId(myUid, "uid_sarah")

        repository.sendMessage(
            chatId = chatId,
            text = "Check out this AI-generated logo!",
            replyTo = null,
            imageUrl = imageBase64
        )

        val messages = repository.messages.value[chatId]
        assertNotNull(messages)
        val sentImageMsg = messages!!.find { it.imageUrl == imageBase64 }
        assertNotNull(sentImageMsg)
        assertEquals("Check out this AI-generated logo!", sentImageMsg!!.text)
        assertEquals(imageBase64, sentImageMsg.imageUrl)
    }

    @Test
    fun testGeminiImageEditMode() = runBlocking {
        val geminiClient = GeminiClient(context)
        val initialResult = geminiClient.generateOrEditImage(
            prompt = "A simple geometric cube"
        )
        val initialBase64 = initialResult.getOrNull()
        assertNotNull(initialBase64)

        val editResult = geminiClient.generateOrEditImage(
            prompt = "Make the cube glow cyan and neon purple",
            base64InputImage = initialBase64
        )
        assertTrue(editResult.isSuccess)
        val editedBase64 = editResult.getOrNull()
        assertNotNull(editedBase64)
        assertTrue(editedBase64!!.isNotBlank())
    }

    @Test
    fun testGeminiLiveVoiceTurn() = runBlocking {
        val geminiClient = GeminiClient(context)
        val voiceResult = geminiClient.liveVoiceTurn(
            userMessage = "Hello Gemini, are you connected?",
            conversationHistory = emptyList()
        )
        assertTrue(voiceResult.isSuccess)
        val spokenText = voiceResult.getOrNull()
        assertNotNull(spokenText)
        assertTrue(spokenText!!.isNotBlank())
    }

    @Test
    fun testLiveVoiceScreenNavigation() {
        val viewModel = LinkUpViewModel(ApplicationProvider.getApplicationContext())
        viewModel.navigateTo(AppScreen.LiveVoice)
        assertEquals(AppScreen.LiveVoice, viewModel.currentScreen.value)

        val handled = viewModel.handleBack()
        assertTrue(handled)
        assertEquals(AppScreen.Home, viewModel.currentScreen.value)
    }
}

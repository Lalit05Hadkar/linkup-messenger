package com.example.data.insforge

import android.util.Log
import com.example.data.model.Chat
import com.example.data.model.FriendRequest
import com.example.data.model.Message
import com.example.data.model.MessageStatus
import com.example.data.model.ReplyPreview
import com.example.data.model.RequestStatus
import com.example.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class InsForgeAuthUser(
    val id: String,
    val email: String,
    val name: String = "",
    val accessToken: String = "",
    val refreshToken: String = ""
)

class InsForgeClient(
    private val baseUrl: String = InsForgeConfig.BASE_URL,
    private val anonKey: String = InsForgeConfig.ANON_KEY
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    var currentAccessToken: String? = null

    private fun getAuthHeader(): String {
        val token = currentAccessToken
        return if (!token.isNullOrBlank()) "Bearer $token" else "Bearer $anonKey"
    }

    // --- Authentication ---

    suspend fun signUp(email: String, pass: String, name: String): Result<InsForgeAuthUser> =
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("email", email.trim())
                    put("password", pass)
                    put("name", name.trim())
                }
                val request = Request.Builder()
                    .url("$baseUrl/api/auth/users?client_type=mobile")
                    .header("Authorization", "Bearer $anonKey")
                    .header("Content-Type", "application/json")
                    .post(json.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val root = JSONObject(responseBody)
                    val userObj = root.optJSONObject("user")
                    val id = userObj?.optString("id") ?: ""
                    val resEmail = userObj?.optString("email") ?: email
                    val profile = userObj?.optJSONObject("profile")
                    val resName = profile?.optString("name") ?: name
                    val accessToken = root.optString("accessToken")
                    val refreshToken = root.optString("refreshToken")
                    if (accessToken.isNotBlank()) currentAccessToken = accessToken
                    Result.success(
                        InsForgeAuthUser(
                            id = id,
                            email = resEmail,
                            name = resName,
                            accessToken = accessToken,
                            refreshToken = refreshToken
                        )
                    )
                } else {
                    val errMsg = try {
                        JSONObject(responseBody).optString("message", "Sign up failed ($responseBody)")
                    } catch (_: Exception) {
                        "Sign up failed (${response.code})"
                    }
                    Result.failure(Exception(errMsg))
                }
            } catch (e: Exception) {
                Log.e("InsForgeClient", "signUp error", e)
                Result.failure(e)
            }
        }

    suspend fun signIn(email: String, pass: String): Result<InsForgeAuthUser> =
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("email", email.trim())
                    put("password", pass)
                    put("method", "password")
                }
                val request = Request.Builder()
                    .url("$baseUrl/api/auth/sessions?client_type=mobile")
                    .header("Authorization", "Bearer $anonKey")
                    .header("Content-Type", "application/json")
                    .post(json.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val root = JSONObject(responseBody)
                    val userObj = root.optJSONObject("user")
                    val id = userObj?.optString("id") ?: ""
                    val resEmail = userObj?.optString("email") ?: email
                    val profile = userObj?.optJSONObject("profile")
                    val resName = profile?.optString("name") ?: ""
                    val accessToken = root.optString("accessToken")
                    val refreshToken = root.optString("refreshToken")
                    if (accessToken.isNotBlank()) currentAccessToken = accessToken
                    Result.success(
                        InsForgeAuthUser(
                            id = id,
                            email = resEmail,
                            name = resName,
                            accessToken = accessToken,
                            refreshToken = refreshToken
                        )
                    )
                } else {
                    val errMsg = try {
                        JSONObject(responseBody).optString("message", "Sign in failed ($responseBody)")
                    } catch (_: Exception) {
                        "Sign in failed (${response.code})"
                    }
                    Result.failure(Exception(errMsg))
                }
            } catch (e: Exception) {
                Log.e("InsForgeClient", "signIn error", e)
                Result.failure(e)
            }
        }

    // --- Users Table ---

    suspend fun getAllUsers(): List<User> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/database/records/users?order=full_name.asc&limit=100")
                .header("Authorization", getAuthHeader())
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                parseUsers(JSONArray(body))
            } else {
                Log.w("InsForgeClient", "getAllUsers failed: ${response.code} $body")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("InsForgeClient", "getAllUsers error", e)
            emptyList()
        }
    }

    suspend fun getUser(uid: String): User? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/database/records/users?id=eq.$uid&limit=1")
                .header("Authorization", getAuthHeader())
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val array = JSONArray(body)
                if (array.length() > 0) parseUser(array.getJSONObject(0)) else null
            } else null
        } catch (e: Exception) {
            Log.e("InsForgeClient", "getUser error", e)
            null
        }
    }

    suspend fun checkUserIdAvailable(userId: String, excludeUid: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/api/database/records/users?user_id=eq.${userId.trim().lowercase()}&limit=5")
                    .header("Authorization", getAuthHeader())
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val array = JSONArray(body)
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        val itemUid = item.optString("id")
                        if (itemUid != excludeUid) {
                            return@withContext false
                        }
                    }
                    true
                } else true
            } catch (e: Exception) {
                Log.e("InsForgeClient", "checkUserIdAvailable error", e)
                true
            }
        }

    suspend fun upsertUser(user: User): Boolean = withContext(Dispatchers.IO) {
        try {
            val pinnedArray = JSONArray().apply {
                user.pinnedChats.forEach { put(it) }
            }
            val fcmArray = JSONArray().apply {
                user.fcmTokens.forEach { put(it) }
            }

            val userObj = JSONObject().apply {
                put("id", user.uid)
                put("user_id", user.userId.trim().lowercase())
                put("full_name", user.fullName.trim())
                put("full_name_lower", user.fullNameLower.trim().lowercase())
                put("email", user.email.trim())
                put("profile_image", user.profileImage)
                put("bio", user.bio.trim())
                put("last_seen", user.lastSeen)
                put("is_online", user.isOnline)
                put("allow_requests", user.allowRequests)
                put("show_last_seen", user.showLastSeen)
                put("pinned_chats", pinnedArray)
                put("fcm_tokens", fcmArray)
                put("created_at", user.createdAt)
            }

            val payload = JSONArray().apply { put(userObj) }

            val request = Request.Builder()
                .url("$baseUrl/api/database/records/users")
                .header("Authorization", getAuthHeader())
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates,return=representation")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w("InsForgeClient", "upsertUser failed: ${response.code} ${response.body?.string()}")
            }
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("InsForgeClient", "upsertUser error", e)
            false
        }
    }

    suspend fun deleteUser(uid: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/database/records/users?id=eq.$uid")
                .header("Authorization", getAuthHeader())
                .delete()
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("InsForgeClient", "deleteUser error", e)
            false
        }
    }

    // --- Friend Requests Table ---

    suspend fun getFriendRequests(uid: String): List<FriendRequest> = withContext(Dispatchers.IO) {
        try {
            // Fetch requests where user is receiver or sender
            val req1 = Request.Builder()
                .url("$baseUrl/api/database/records/friend_requests?receiver_id=eq.$uid&order=created_at.desc")
                .header("Authorization", getAuthHeader())
                .get()
                .build()

            val req2 = Request.Builder()
                .url("$baseUrl/api/database/records/friend_requests?sender_id=eq.$uid&order=created_at.desc")
                .header("Authorization", getAuthHeader())
                .get()
                .build()

            val list = mutableListOf<FriendRequest>()

            val res1 = client.newCall(req1).execute()
            if (res1.isSuccessful) {
                list.addAll(parseFriendRequests(JSONArray(res1.body?.string().orEmpty())))
            }

            val res2 = client.newCall(req2).execute()
            if (res2.isSuccessful) {
                val sent = parseFriendRequests(JSONArray(res2.body?.string().orEmpty()))
                sent.forEach { s ->
                    if (list.none { it.requestId == s.requestId }) list.add(s)
                }
            }

            list
        } catch (e: Exception) {
            Log.e("InsForgeClient", "getFriendRequests error", e)
            emptyList()
        }
    }

    suspend fun upsertFriendRequest(req: FriendRequest): Boolean = withContext(Dispatchers.IO) {
        try {
            val obj = JSONObject().apply {
                put("id", req.requestId)
                put("sender_id", req.senderId)
                put("receiver_id", req.receiverId)
                put("sender_name", req.senderName)
                put("sender_user_id", req.senderUserId)
                put("sender_avatar", req.senderAvatar)
                put("receiver_name", req.receiverName)
                put("receiver_user_id", req.receiverUserId)
                put("receiver_avatar", req.receiverAvatar)
                put("status", req.status.name.lowercase())
                put("created_at", req.createdAt)
            }
            val payload = JSONArray().apply { put(obj) }

            val request = Request.Builder()
                .url("$baseUrl/api/database/records/friend_requests")
                .header("Authorization", getAuthHeader())
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates,return=representation")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("InsForgeClient", "upsertFriendRequest error", e)
            false
        }
    }

    suspend fun updateFriendRequestStatus(requestId: String, status: RequestStatus): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val patch = JSONObject().apply {
                    put("status", status.name.lowercase())
                }
                val request = Request.Builder()
                    .url("$baseUrl/api/database/records/friend_requests?id=eq.$requestId")
                    .header("Authorization", getAuthHeader())
                    .header("Content-Type", "application/json")
                    .patch(patch.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                response.isSuccessful
            } catch (e: Exception) {
                Log.e("InsForgeClient", "updateFriendRequestStatus error", e)
                false
            }
        }

    // --- Chats Table ---

    suspend fun getChats(uid: String): List<Chat> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/database/records/chats?order=last_message_time.desc&limit=100")
                .header("Authorization", getAuthHeader())
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val allChats = parseChats(JSONArray(body))
                // Filter where user is participant
                allChats.filter { it.participants.contains(uid) }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("InsForgeClient", "getChats error", e)
            emptyList()
        }
    }

    suspend fun upsertChat(chat: Chat): Boolean = withContext(Dispatchers.IO) {
        try {
            val partsArray = JSONArray().apply {
                chat.participants.forEach { put(it) }
            }
            val unreadObj = JSONObject().apply {
                chat.unreadCounts.forEach { (k, v) -> put(k, v) }
            }

            val chatObj = JSONObject().apply {
                put("id", chat.chatId)
                put("participants", partsArray)
                put("last_message", chat.lastMessage)
                put("last_message_sender_id", chat.lastMessageSenderId)
                put("last_message_time", chat.lastMessageTime)
                put("unread_counts", unreadObj)
                put("created_at", chat.createdAt)
            }

            val payload = JSONArray().apply { put(chatObj) }

            val request = Request.Builder()
                .url("$baseUrl/api/database/records/chats")
                .header("Authorization", getAuthHeader())
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates,return=representation")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("InsForgeClient", "upsertChat error", e)
            false
        }
    }

    // --- Messages Table ---

    suspend fun getMessages(chatId: String): List<Message> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/database/records/messages?chat_id=eq.$chatId&order=created_at.asc&limit=500")
                .header("Authorization", getAuthHeader())
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                parseMessages(JSONArray(body))
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("InsForgeClient", "getMessages error", e)
            emptyList()
        }
    }

    suspend fun sendMessage(message: Message): Boolean = withContext(Dispatchers.IO) {
        try {
            val msgObj = JSONObject().apply {
                put("id", message.messageId)
                put("chat_id", message.chatId)
                put("sender_id", message.senderId)
                put("text", message.text)
                put("image_url", message.imageUrl)
                put("status", message.status.name.lowercase())
                put("created_at", message.timestamp)
                if (message.replyTo != null) {
                    val reply = JSONObject().apply {
                        put("messageId", message.replyTo.messageId)
                        put("text", message.replyTo.text)
                        put("senderName", message.replyTo.senderName)
                        put("senderId", message.replyTo.senderId)
                    }
                    put("reply_to", reply)
                }
            }

            val payload = JSONArray().apply { put(msgObj) }

            val request = Request.Builder()
                .url("$baseUrl/api/database/records/messages")
                .header("Authorization", getAuthHeader())
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates,return=representation")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("InsForgeClient", "sendMessage error", e)
            false
        }
    }

    suspend fun updateMessageStatus(messageId: String, status: MessageStatus): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val patch = JSONObject().apply {
                    put("status", status.name.lowercase())
                }
                val request = Request.Builder()
                    .url("$baseUrl/api/database/records/messages?id=eq.$messageId")
                    .header("Authorization", getAuthHeader())
                    .header("Content-Type", "application/json")
                    .patch(patch.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                response.isSuccessful
            } catch (e: Exception) {
                Log.e("InsForgeClient", "updateMessageStatus error", e)
                false
            }
        }

    // --- JSON Parsers ---

    private fun parseUsers(array: JSONArray): List<User> {
        val list = mutableListOf<User>()
        for (i in 0 until array.length()) {
            list.add(parseUser(array.getJSONObject(i)))
        }
        return list
    }

    private fun parseUser(obj: JSONObject): User {
        val pinned = mutableListOf<String>()
        val pinnedJson = obj.optJSONArray("pinned_chats")
        if (pinnedJson != null) {
            for (i in 0 until pinnedJson.length()) {
                pinned.add(pinnedJson.optString(i))
            }
        }
        val fcm = mutableListOf<String>()
        val fcmJson = obj.optJSONArray("fcm_tokens")
        if (fcmJson != null) {
            for (i in 0 until fcmJson.length()) {
                fcm.add(fcmJson.optString(i))
            }
        }

        return User(
            uid = obj.optString("id"),
            fullName = obj.optString("full_name"),
            fullNameLower = obj.optString("full_name_lower"),
            email = obj.optString("email"),
            userId = obj.optString("user_id"),
            profileImage = obj.optString("profile_image"),
            bio = obj.optString("bio"),
            lastSeen = obj.optLong("last_seen", System.currentTimeMillis()),
            isOnline = obj.optBoolean("is_online", true),
            createdAt = obj.optLong("created_at", System.currentTimeMillis()),
            pinnedChats = pinned,
            allowRequests = obj.optBoolean("allow_requests", true),
            showLastSeen = obj.optBoolean("show_last_seen", true),
            fcmTokens = fcm
        )
    }

    private fun parseFriendRequests(array: JSONArray): List<FriendRequest> {
        val list = mutableListOf<FriendRequest>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val stStr = obj.optString("status", "pending")
            val status = when (stStr.lowercase()) {
                "accepted" -> RequestStatus.ACCEPTED
                "rejected" -> RequestStatus.REJECTED
                else -> RequestStatus.PENDING
            }
            list.add(
                FriendRequest(
                    requestId = obj.optString("id"),
                    senderId = obj.optString("sender_id"),
                    receiverId = obj.optString("receiver_id"),
                    senderName = obj.optString("sender_name"),
                    senderUserId = obj.optString("sender_user_id"),
                    senderAvatar = obj.optString("sender_avatar"),
                    receiverName = obj.optString("receiver_name"),
                    receiverUserId = obj.optString("receiver_user_id"),
                    receiverAvatar = obj.optString("receiver_avatar"),
                    status = status,
                    createdAt = obj.optLong("created_at", System.currentTimeMillis())
                )
            )
        }
        return list
    }

    private fun parseChats(array: JSONArray): List<Chat> {
        val list = mutableListOf<Chat>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val parts = mutableListOf<String>()
            val partsJson = obj.optJSONArray("participants")
            if (partsJson != null) {
                for (j in 0 until partsJson.length()) {
                    parts.add(partsJson.optString(j))
                }
            }
            val unread = mutableMapOf<String, Int>()
            val unreadObj = obj.optJSONObject("unread_counts")
            if (unreadObj != null) {
                val keys = unreadObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    unread[k] = unreadObj.optInt(k, 0)
                }
            }

            list.add(
                Chat(
                    chatId = obj.optString("id"),
                    participants = parts,
                    lastMessage = obj.optString("last_message"),
                    lastMessageSenderId = obj.optString("last_message_sender_id"),
                    lastMessageTime = obj.optLong("last_message_time", System.currentTimeMillis()),
                    unreadCounts = unread,
                    createdAt = obj.optLong("created_at", System.currentTimeMillis())
                )
            )
        }
        return list
    }

    private fun parseMessages(array: JSONArray): List<Message> {
        val list = mutableListOf<Message>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val stStr = obj.optString("status", "sent")
            val status = when (stStr.lowercase()) {
                "delivered" -> MessageStatus.DELIVERED
                "seen" -> MessageStatus.SEEN
                else -> MessageStatus.SENT
            }
            var replyPreview: ReplyPreview? = null
            val replyObj = obj.optJSONObject("reply_to")
            if (replyObj != null) {
                replyPreview = ReplyPreview(
                    messageId = replyObj.optString("messageId"),
                    text = replyObj.optString("text"),
                    senderName = replyObj.optString("senderName"),
                    senderId = replyObj.optString("senderId")
                )
            }

            list.add(
                Message(
                    messageId = obj.optString("id"),
                    chatId = obj.optString("chat_id"),
                    senderId = obj.optString("sender_id"),
                    text = obj.optString("text"),
                    imageUrl = obj.optString("image_url"),
                    timestamp = obj.optLong("created_at", System.currentTimeMillis()),
                    status = status,
                    replyTo = replyPreview
                )
            )
        }
        return list
    }
}

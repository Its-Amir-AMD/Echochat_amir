package com.example.data.repository

import com.example.data.local.EchoChatDao
import com.example.data.model.ChannelEntity
import com.example.data.model.MessageEntity
import com.example.data.model.UserProfileEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Firebase-backed account and realtime chat data source.
 *
 * Every conversation is scoped by its Firestore membership list. Local Room data is only
 * a cache; Firestore Security Rules remain the authority for reads and writes.
 */
class FirebaseChatBackend(
    private val dao: EchoChatDao,
    private val scope: CoroutineScope,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private var memberChannelsRegistration: ListenerRegistration? = null
    private var publicChannelsRegistration: ListenerRegistration? = null
    private val messageRegistrations = mutableMapOf<String, ListenerRegistration>()

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val currentUserName: String
        get() = auth.currentUser?.displayName?.takeIf(String::isNotBlank)
            ?: auth.currentUser?.email?.substringBefore('@')
            ?: "Echo member"

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        stopRealtimeListeners()
        val user = firebaseAuth.currentUser ?: return@AuthStateListener
        scope.launch {
            runCatching {
                ensureUserDocuments(user)
                ensureAssistantChannel()
                subscribeToChannels(user.uid)
            }
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    fun close() {
        auth.removeAuthStateListener(authStateListener)
        stopRealtimeListeners()
    }

    private fun stopRealtimeListeners() {
        memberChannelsRegistration?.remove()
        publicChannelsRegistration?.remove()
        memberChannelsRegistration = null
        publicChannelsRegistration = null
        messageRegistrations.values.forEach(ListenerRegistration::remove)
        messageRegistrations.clear()
    }

    private suspend fun ensureUserDocuments(user: FirebaseUser) {
        val name = user.displayName?.takeIf(String::isNotBlank)
            ?: user.email?.substringBefore('@')?.takeIf(String::isNotBlank)
            ?: "Echo member"
        val privateProfile = mapOf(
            "uid" to user.uid,
            "email" to (user.email ?: ""),
            "displayName" to name,
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )
        val publicProfile = mapOf(
            "uid" to user.uid,
            "displayName" to name,
            "avatarColor" to DEFAULT_AVATAR_COLOR,
            "updatedAt" to System.currentTimeMillis()
        )
        firestore.collection("users").document(user.uid)
            .set(privateProfile, SetOptions.merge()).await()
        firestore.collection("publicProfiles").document(user.uid)
            .set(publicProfile, SetOptions.merge()).await()
        dao.insertOrUpdateProfile(
            UserProfileEntity(
                displayName = name,
                bio = "عضو EchoChat",
                avatarColor = DEFAULT_AVATAR_COLOR,
                languageCode = "fa"
            )
        )
    }

    private suspend fun ensureAssistantChannel() {
        dao.insertChannels(
            listOf(
                ChannelEntity(
                    id = ASSISTANT_CHANNEL_ID,
                    name = "دستیار هوشمند اکو / Echo AI",
                    description = "گفتگوی خصوصی با دستیار هوش مصنوعی",
                    iconName = "smart_toy",
                    isDirectMessage = true,
                    peerName = "Echo AI",
                    peerAvatarColor = 0xFF6366F1,
                    createdAt = System.currentTimeMillis()
                )
            )
        )
    }

    private fun subscribeToChannels(uid: String) {
        memberChannelsRegistration = firestore.collection("channels")
            .whereArrayContains("members", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                syncChannelSnapshot(snapshot.documents, uid)
            }

        // Public channels are discoverable by authenticated users; membership is added only
        // when a user opens one. Private direct messages are never included in this query.
        publicChannelsRegistration = firestore.collection("channels")
            .whereEqualTo("isPublic", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                syncChannelSnapshot(snapshot.documents, uid)
            }
    }

    private fun syncChannelSnapshot(
        documents: List<com.google.firebase.firestore.DocumentSnapshot>,
        uid: String
    ) {
        scope.launch {
            val mapped = documents.mapNotNull { document -> channelFromDocument(document, uid) }
            if (mapped.isNotEmpty()) dao.insertChannels(mapped)

            documents.forEach { document ->
                val members = document.get("members").asStringList()
                if (uid in members) subscribeToMessages(document.id, uid)
            }
        }
    }

    private fun channelFromDocument(
        document: com.google.firebase.firestore.DocumentSnapshot,
        uid: String
    ): ChannelEntity? {
        val members = document.get("members").asStringList()
        val isPublic = document.getBoolean("isPublic") == true
        if (!isPublic && uid !in members) return null

        val isDirectMessage = document.getString("kind") == "dm"
        val memberNames = document.get("memberNames") as? Map<*, *>
        val peerId = members.firstOrNull { it != uid }
        val peerName = (memberNames?.get(peerId) as? String)
            ?.takeIf(String::isNotBlank)
            ?: document.getString("name").orEmpty()
        val groupName = document.getString("name")?.takeIf(String::isNotBlank)
            ?: "EchoChat channel"

        return ChannelEntity(
            id = document.id,
            name = if (isDirectMessage) peerName else groupName,
            description = document.getString("description").orEmpty(),
            iconName = document.getString("iconName") ?: if (isDirectMessage) "person" else "forum",
            isDirectMessage = isDirectMessage,
            peerName = if (isDirectMessage) peerName else "",
            peerAvatarColor = document.getLong("peerAvatarColor") ?: DEFAULT_AVATAR_COLOR,
            unreadCount = 0,
            lastMessage = document.getString("lastMessage").orEmpty(),
            lastMessageTime = document.getLong("lastMessageTime") ?: 0L,
            createdAt = document.getLong("createdAt") ?: 0L
        )
    }

    private fun subscribeToMessages(channelId: String, uid: String) {
        if (messageRegistrations.containsKey(channelId)) return
        messageRegistrations[channelId] = firestore.collection("channels")
            .document(channelId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limitToLast(MAX_SYNCED_MESSAGES)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                scope.launch {
                    val messages = snapshot.documents.mapNotNull { document ->
                        val senderId = document.getString("senderId") ?: return@mapNotNull null
                        val body = document.getString("content") ?: return@mapNotNull null
                        MessageEntity(
                            id = document.id,
                            channelId = channelId,
                            senderId = senderId,
                            senderName = document.getString("senderName") ?: "Echo member",
                            senderAvatarColor = document.getLong("senderAvatarColor") ?: DEFAULT_AVATAR_COLOR,
                            content = body,
                            timestamp = document.getLong("timestamp") ?: 0L,
                            isOutgoing = senderId == uid,
                            status = document.getString("status") ?: "sent",
                            replyToId = document.getString("replyToId"),
                            replyToSnippet = document.getString("replyToSnippet"),
                            reaction = document.getString("reaction")
                        )
                    }
                    val ids = messages.map { it.id }
                    if (ids.isEmpty()) dao.deleteMessagesForChannel(channelId)
                    else dao.deleteMessagesNotIn(channelId, ids)
                    if (messages.isNotEmpty()) dao.insertMessages(messages)
                }
            }
    }

    suspend fun createPublicChannel(channel: ChannelEntity) {
        val user = requireCurrentUser()
        val memberNames = mapOf(user.uid to currentUserName)
        val record = mapOf(
            "name" to channel.name,
            "description" to channel.description,
            "iconName" to channel.iconName,
            "kind" to "channel",
            "isPublic" to true,
            "members" to listOf(user.uid),
            "memberNames" to memberNames,
            "createdBy" to user.uid,
            "createdAt" to channel.createdAt,
            "lastMessage" to "",
            "lastMessageTime" to channel.lastMessageTime
        )
        firestore.collection("channels").document(channel.id).set(record).await()
        dao.insertChannel(channel)
    }

    suspend fun startDirectMessage(recipientUid: String): ChannelEntity {
        val user = requireCurrentUser()
        val peerUid = recipientUid.trim()
        require(peerUid.isNotEmpty()) { "شناسه کاربری را وارد کنید." }
        require(peerUid != user.uid) { "نمی‌توانید با حساب خودتان گفتگوی خصوصی ایجاد کنید." }

        val peerSnapshot = firestore.collection("publicProfiles").document(peerUid).get().await()
        require(peerSnapshot.exists()) {
            "این شناسه در EchoChat پیدا نشد. شناسه را از بخش پروفایل مخاطب دریافت کنید."
        }
        val peerName = peerSnapshot.getString("displayName")
            ?.takeIf(String::isNotBlank) ?: "Echo member"

        val existing = firestore.collection("channels")
            .whereArrayContains("members", user.uid)
            .get().await()
            .documents.firstOrNull { document ->
                document.getString("kind") == "dm" &&
                    document.get("members").asStringList().toSet() == setOf(user.uid, peerUid)
            }

        if (existing != null) {
            val mapped = channelFromDocument(existing, user.uid)
                ?: throw IllegalStateException("گفتگوی موجود قابل بارگذاری نیست.")
            dao.insertChannel(mapped)
            return mapped
        }

        val id = "dm_" + UUID.randomUUID().toString().replace("-", "").take(16)
        val now = System.currentTimeMillis()
        val names = mapOf(user.uid to currentUserName, peerUid to peerName)
        val record = mapOf(
            "name" to peerName,
            "description" to "گفتگوی خصوصی / Private conversation",
            "iconName" to "person",
            "kind" to "dm",
            "isPublic" to false,
            "members" to listOf(user.uid, peerUid).sorted(),
            "memberNames" to names,
            "createdBy" to user.uid,
            "createdAt" to now,
            "lastMessage" to "",
            "lastMessageTime" to now
        )
        firestore.collection("channels").document(id).set(record).await()

        return ChannelEntity(
            id = id,
            name = peerName,
            description = "گفتگوی خصوصی / Private conversation",
            iconName = "person",
            isDirectMessage = true,
            peerName = peerName,
            peerAvatarColor = peerSnapshot.getLong("avatarColor") ?: 0xFF8B5CF6,
            lastMessageTime = now,
            createdAt = now
        ).also { dao.insertChannel(it) }
    }

    suspend fun joinPublicChannelIfNeeded(channelId: String) {
        if (channelId == ASSISTANT_CHANNEL_ID) return
        val user = requireCurrentUser()
        val reference = firestore.collection("channels").document(channelId)
        val snapshot = reference.get().await()
        if (!snapshot.exists() || snapshot.getBoolean("isPublic") != true) return
        if (user.uid !in snapshot.get("members").asStringList()) {
            reference.update("members", FieldValue.arrayUnion(user.uid)).await()
        }
    }

    suspend fun sendMessage(message: MessageEntity) {
        val user = requireCurrentUser()
        require(message.content.isNotBlank()) { "پیام خالی قابل ارسال نیست." }
        require(message.content.length <= MAX_MESSAGE_LENGTH) {
            "پیام بیش از حد طولانی است (حداکثر $MAX_MESSAGE_LENGTH نویسه)."
        }

        val channelReference = firestore.collection("channels").document(message.channelId)
        val channel = channelReference.get().await()
        require(channel.exists()) { "این گفتگو در سرور پیدا نشد." }

        val members = channel.get("members").asStringList()
        if (user.uid !in members) {
            require(channel.getBoolean("isPublic") == true) {
                "شما عضو این گفتگوی خصوصی نیستید."
            }
            channelReference.update("members", FieldValue.arrayUnion(user.uid)).await()
        }

        val messageRecord = mapOf(
            "senderId" to user.uid,
            "senderName" to currentUserName,
            "senderAvatarColor" to (message.senderAvatarColor),
            "content" to message.content,
            "timestamp" to message.timestamp,
            "status" to "sent",
            "replyToId" to message.replyToId,
            "replyToSnippet" to message.replyToSnippet,
            "reaction" to message.reaction
        )
        val batch = firestore.batch()
        batch.set(channelReference.collection("messages").document(message.id), messageRecord)
        batch.update(
            channelReference,
            mapOf(
                "lastMessage" to message.content,
                "lastMessageTime" to message.timestamp
            )
        )
        batch.commit().await()
    }

    suspend fun updateReaction(message: MessageEntity) {
        requireCurrentUser()
        firestore.collection("channels").document(message.channelId)
            .collection("messages").document(message.id)
            .update("reaction", message.reaction).await()
    }

    suspend fun deleteMessage(message: MessageEntity) {
        val user = requireCurrentUser()
        require(message.senderId == user.uid) { "فقط فرستنده می‌تواند پیام را حذف کند." }
        firestore.collection("channels").document(message.channelId)
            .collection("messages").document(message.id).delete().await()
    }

    suspend fun updatePublicProfile(displayName: String, avatarColor: Long) {
        val user = requireCurrentUser()
        val safeName = displayName.trim().take(60).ifBlank { currentUserName }
        val update = mapOf(
            "displayName" to safeName,
            "avatarColor" to avatarColor,
            "updatedAt" to System.currentTimeMillis()
        )
        firestore.collection("publicProfiles").document(user.uid)
            .set(update, SetOptions.merge()).await()
        firestore.collection("users").document(user.uid)
            .set(update, SetOptions.merge()).await()
        user.updateProfile(
            com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(safeName)
                .build()
        ).await()
    }

    private fun requireCurrentUser(): FirebaseUser =
        auth.currentUser ?: throw IllegalStateException("برای انجام این کار ابتدا وارد حساب شوید.")

    private fun Any?.asStringList(): List<String> =
        (this as? List<*>)?.filterIsInstance<String>() ?: emptyList()

    private companion object {
        const val ASSISTANT_CHANNEL_ID = "bot_echo"
        const val MAX_MESSAGE_LENGTH = 4_000
        const val MAX_SYNCED_MESSAGES = 500
        const val DEFAULT_AVATAR_COLOR = 0xFF4F46E5L
    }
}

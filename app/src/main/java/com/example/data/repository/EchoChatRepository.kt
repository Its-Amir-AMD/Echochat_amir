package com.example.data.repository

import com.example.ai.EchoAiService
import com.example.data.local.EchoChatDao
import com.example.data.model.ChannelEntity
import com.example.data.model.CommentEntity
import com.example.data.model.MessageEntity
import com.example.data.model.PostEntity
import com.example.data.model.StoryEntity
import com.example.data.model.UserProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class EchoChatRepository(
    private val dao: EchoChatDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val backend: FirebaseChatBackend? = null
) {
    val allChannels: Flow<List<ChannelEntity>> = dao.getAllChannels()
    val userProfile: Flow<UserProfileEntity?> = dao.getUserProfile()
    val allPosts: Flow<List<PostEntity>> = dao.getAllPosts()
    val savedPosts: Flow<List<PostEntity>> = dao.getSavedPosts()
    val allStories: Flow<List<StoryEntity>> = dao.getAllStories()

    // Real-time typing indicators for active channels
    private val _typingStatus = MutableStateFlow<Map<String, String?>>(emptyMap())
    val typingStatus = _typingStatus.asStateFlow()

    fun getChannel(channelId: String): Flow<ChannelEntity?> = dao.getChannelById(channelId)

    fun getMessagesForChannel(channelId: String): Flow<List<MessageEntity>> =
        dao.getMessagesForChannel(channelId)

    fun searchMessages(query: String): Flow<List<MessageEntity>> = dao.searchMessages(query)

    suspend fun clearChannelUnread(channelId: String) = dao.clearChannelUnread(channelId)

    suspend fun sendMessage(
        channelId: String,
        content: String,
        replyToId: String? = null,
        replyToSnippet: String? = null
    ) {
        val cleanContent = content.trim()
        require(cleanContent.isNotEmpty()) { "پیام خالی قابل ارسال نیست." }
        require(cleanContent.length <= 4_000) { "حداکثر طول پیام ۴۰۰۰ نویسه است." }

        val now = System.currentTimeMillis()
        val userMsgId = UUID.randomUUID().toString()
        val profile = dao.getUserProfileOnce()
        val outgoingMsg = MessageEntity(
            id = userMsgId,
            channelId = channelId,
            senderId = backend?.currentUserId ?: "me",
            senderName = backend?.currentUserName
                ?: profile?.displayName?.takeIf(String::isNotBlank)
                ?: "شما / You",
            senderAvatarColor = profile?.avatarColor ?: 0xFF4F46E5,
            content = cleanContent,
            timestamp = now,
            isOutgoing = true,
            status = if (backend != null && channelId != "bot_echo") "sending" else "read",
            replyToId = replyToId,
            replyToSnippet = replyToSnippet
        )

        // Echo AI is private, per-account history. Human conversations use Firestore
        // as the source of truth and are written only after server-side authorization.
        if (backend != null && channelId != "bot_echo") {
            backend.sendMessage(outgoingMsg)
            return
        }

        dao.insertMessage(outgoingMsg)
        dao.updateChannelLastMessage(channelId, cleanContent, now)
        triggerAssistantReply(channelId, cleanContent, userMsgId)
    }

    private fun triggerAssistantReply(channelId: String, userText: String, replyToId: String) {
        if (channelId != "bot_echo") return

        scope.launch {
            val assistantName = "Echo AI"
            _typingStatus.value = _typingStatus.value + (channelId to assistantName)
            try {
                val replyText = EchoAiService().generateReply(userText)
                val now = System.currentTimeMillis()
                val reply = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    channelId = channelId,
                    senderId = "bot_echo",
                    senderName = assistantName,
                    senderAvatarColor = 0xFF6366F1,
                    content = replyText,
                    timestamp = now,
                    isOutgoing = false,
                    status = "read",
                    replyToId = replyToId,
                    replyToSnippet = userText.take(40).let { if (userText.length > 40) "$it..." else it }
                )
                dao.insertMessage(reply)
                dao.updateChannelLastMessage(channelId, replyText, now)
            } catch (_: Exception) {
                // Do not represent a canned response as AI output.
                val now = System.currentTimeMillis()
                val explanation = "پاسخ هوش مصنوعی در دسترس نیست. تنظیمات Firebase AI و App Check را بررسی کنید. / AI unavailable: check Firebase AI and App Check configuration."
                dao.insertMessage(
                    MessageEntity(
                        id = UUID.randomUUID().toString(),
                        channelId = channelId,
                        senderId = "system",
                        senderName = "EchoChat",
                        senderAvatarColor = 0xFF64748B,
                        content = explanation,
                        timestamp = now,
                        isOutgoing = false,
                        status = "failed",
                        replyToId = replyToId,
                        replyToSnippet = userText.take(40)
                    )
                )
                dao.updateChannelLastMessage(channelId, explanation, now)
            } finally {
                _typingStatus.value = _typingStatus.value - channelId
            }
        }
    }

    suspend fun setReaction(messageId: String, reaction: String?) {
        val message = dao.getMessageById(messageId) ?: return
        if (backend != null && message.channelId != "bot_echo") {
            backend.updateReaction(message.copy(reaction = reaction))
        } else {
            dao.updateMessageReaction(messageId, reaction)
        }
    }

    suspend fun deleteMessage(messageId: String) {
        val message = dao.getMessageById(messageId) ?: return
        if (backend != null && message.channelId != "bot_echo") {
            backend.deleteMessage(message)
        } else {
            dao.deleteMessageById(messageId)
        }
    }

    suspend fun joinPublicChannelIfNeeded(channelId: String) {
        backend?.joinPublicChannelIfNeeded(channelId)
    }

    suspend fun clearChat(channelId: String) {
        val now = System.currentTimeMillis()
        dao.deleteMessagesForChannel(channelId)
        dao.updateChannelHiddenBefore(channelId, now)
        dao.updateChannelLastMessage(channelId, "", now)
    }

    suspend fun createChannel(name: String, description: String, iconName: String = "forum") {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "نام کانال را وارد کنید." }
        require(cleanName.length <= 60) { "نام کانال نباید بیشتر از ۶۰ نویسه باشد." }
        require(description.length <= 300) { "توضیحات کانال نباید بیشتر از ۳۰۰ نویسه باشد." }
        val now = System.currentTimeMillis()
        val newChannel = ChannelEntity(
            id = "channel_" + UUID.randomUUID().toString().replace("-", "").take(16),
            name = cleanName,
            description = description.trim(),
            iconName = iconName,
            isDirectMessage = false,
            lastMessage = "",
            lastMessageTime = now,
            createdAt = now
        )
        if (backend != null) backend.createPublicChannel(newChannel)
        else dao.insertChannel(newChannel)
    }

    suspend fun startDirectMessage(recipientUid: String): ChannelEntity {
        val cleanUid = recipientUid.trim()
        require(cleanUid.isNotEmpty()) { "شناسه کاربری مخاطب را وارد کنید." }
        backend?.let { return it.startDirectMessage(cleanUid) }
        val newChannel = ChannelEntity(
            id = "dm_" + UUID.randomUUID().toString().replace("-", "").take(16),
            name = cleanUid,
            description = "گفتگوی دونفره",
            iconName = "person",
            isDirectMessage = true,
            peerName = cleanUid,
            peerAvatarColor = 0xFF8B5CF6,
            lastMessage = "",
            lastMessageTime = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )
        dao.insertChannel(newChannel)
        return newChannel
    }

    // --- Social Posts Operations ---
    suspend fun createPost(
        content: String,
        tags: String = "",
        category: String = "tech"
    ) {
        val cleanContent = content.trim()
        require(cleanContent.isNotEmpty()) { "متن پست نمی‌تواند خالی باشد." }
        require(cleanContent.length <= 4_000) { "حداکثر طول پست ۴۰۰۰ نویسه است." }
        val profile = dao.getUserProfileOnce()
        val newPost = PostEntity(
            id = "post_" + UUID.randomUUID().toString().replace("-", ""),
            authorId = backend?.currentUserId ?: "me",
            authorName = backend?.currentUserName
                ?: profile?.displayName?.takeIf { it.isNotBlank() } ?: "Echo User",
            authorRole = "عضو اکو / Echo Member",
            authorAvatarColor = profile?.avatarColor ?: 0xFF4F46E5,
            content = cleanContent,
            tags = tags.trim().take(300),
            category = category,
            likesCount = 0,
            commentsCount = 0,
            sharesCount = 0,
            isLiked = false,
            isBookmarked = false,
            timestamp = System.currentTimeMillis(),
            cardColor = 0xFF1E293B
        )
        if (backend != null) backend.createPost(newPost) else dao.insertPost(newPost)
    }

    suspend fun toggleLikePost(postId: String, currentLiked: Boolean, currentCount: Int) {
        // The cloud transaction reads server state, avoiding stale UI values after rapid taps.
        if (backend != null) backend.togglePostLike(postId) else dao.togglePostLike(postId)
    }

    fun getCommentsForPost(postId: String): kotlinx.coroutines.flow.Flow<List<CommentEntity>> {
        backend?.observeCommentsForPost(postId)
        return dao.getCommentsForPost(postId)
    }

    fun stopObservingCommentsForPost(postId: String) {
        backend?.stopObservingCommentsForPost(postId)
    }

    suspend fun createComment(postId: String, content: String) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return
        require(trimmed.length <= 2_000) { "حداکثر طول دیدگاه ۲۰۰۰ نویسه است." }
        val profile = dao.getUserProfileOnce()
        val comment = CommentEntity(
            id = UUID.randomUUID().toString(),
            postId = postId,
            authorId = backend?.currentUserId ?: "me",
            authorName = backend?.currentUserName
                ?: profile?.displayName?.takeIf { it.isNotBlank() } ?: "Echo User",
            content = trimmed,
            timestamp = System.currentTimeMillis()
        )
        if (backend != null) backend.createComment(postId, comment)
        else dao.insertCommentAndIncrementCount(comment)
    }

    suspend fun createStory(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        require(trimmed.length <= 500) { "حداکثر طول استوری ۵۰۰ نویسه است." }
        val profile = dao.getUserProfileOnce()
        val story = StoryEntity(
            id = "story_" + UUID.randomUUID().toString().replace("-", ""),
            authorId = backend?.currentUserId ?: "me",
            authorName = backend?.currentUserName
                ?: profile?.displayName?.takeIf { it.isNotBlank() } ?: "Echo User",
            authorAvatarColor = profile?.avatarColor ?: 0xFF4F46E5,
            storyTitle = trimmed,
            isViewed = true,
            timestamp = System.currentTimeMillis()
        )
        if (backend != null) backend.createStory(story) else dao.insertStory(story)
    }

    suspend fun toggleBookmarkPost(postId: String, currentBookmarked: Boolean) {
        if (backend != null) backend.togglePostBookmark(postId) else dao.togglePostBookmark(postId)
    }

    suspend fun deletePost(postId: String) {
        if (backend != null) backend.deletePost(postId) else dao.deletePost(postId)
    }

    suspend fun markStoryViewed(storyId: String) {
        dao.markStoryViewed(storyId)
    }

    suspend fun updateProfile(
        displayName: String,
        bio: String,
        avatarColor: Long,
        languageCode: String,
        isDarkMode: Boolean?
    ) {
        backend?.updatePublicProfile(displayName, avatarColor)
        val existing = dao.getUserProfileOnce() ?: UserProfileEntity()
        dao.insertOrUpdateProfile(
            existing.copy(
                displayName = displayName.trim().ifEmpty { existing.displayName },
                bio = bio.trim(),
                avatarColor = avatarColor,
                languageCode = languageCode,
                isDarkMode = isDarkMode
            )
        )
    }
}

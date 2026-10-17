package com.example.data.repository

import com.example.data.local.EchoChatDao
import com.example.data.model.ChannelEntity
import com.example.data.model.CommentEntity
import com.example.data.model.MessageEntity
import com.example.data.model.PostEntity
import com.example.data.model.StoryEntity
import com.example.data.model.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class EchoChatRepository(private val dao: EchoChatDao) {
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
        val now = System.currentTimeMillis()
        val userMsgId = UUID.randomUUID().toString()

        val outgoingMsg = MessageEntity(
            id = userMsgId,
            channelId = channelId,
            senderId = "me",
            senderName = "شما / You",
            senderAvatarColor = 0xFF4F46E5,
            content = content.trim(),
            timestamp = now,
            isOutgoing = true,
            status = "sent",
            replyToId = replyToId,
            replyToSnippet = replyToSnippet
        )

        dao.insertMessage(outgoingMsg)
        dao.updateChannelLastMessage(channelId, content.trim(), now)

    }

    suspend fun setReaction    suspend fun setReaction(messageId: String, reaction: String?) {
        dao.updateMessageReaction(messageId, reaction)
    }

    suspend fun deleteMessage(messageId: String) {
        dao.deleteMessageById(messageId)
    }

    suspend fun clearChat(channelId: String) {
        dao.deleteMessagesForChannel(channelId)
        dao.updateChannelLastMessage(channelId, "", System.currentTimeMillis())
    }

    suspend fun createChannel(name: String, description: String, iconName: String = "forum") {
        val id = "channel_" + UUID.randomUUID().toString().take(8)
        val newChannel = ChannelEntity(
            id = id,
            name = name,
            description = description,
            iconName = iconName,
            isDirectMessage = false,
            lastMessage = "کانال جدید ایجاد شد",
            lastMessageTime = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )
        dao.insertChannel(newChannel)
    }

    suspend fun startDirectMessage(peerName: String, peerAvatarColor: Long = 0xFF8B5CF6) {
        val id = "dm_" + UUID.randomUUID().toString().take(8)
        val newDm = ChannelEntity(
            id = id,
            name = peerName,
            description = "گفتگوی دونفره",
            iconName = "person",
            isDirectMessage = true,
            peerName = peerName,
            peerAvatarColor = peerAvatarColor,
            lastMessage = "گفتگو آغاز شد",
            lastMessageTime = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )
        dao.insertChannel(newDm)
    }

    // --- Social posts, stories and comments ---
    suspend fun createPost(content: String, tags: String = "", category: String = "tech") {
        val normalizedContent = content.trim()
        if (normalizedContent.isEmpty()) return

        val profile = dao.getUserProfileOnce() ?: UserProfileEntity()
        val post = PostEntity(
            id = "post_" + UUID.randomUUID().toString(),
            authorId = "me",
            authorName = profile.displayName,
            authorRole = "EchoChat member",
            authorAvatarColor = profile.avatarColor,
            content = normalizedContent,
            tags = tags.trim(),
            category = category,
            likesCount = 0,
            commentsCount = 0,
            sharesCount = 0,
            isLiked = false,
            isBookmarked = false,
            timestamp = System.currentTimeMillis(),
            cardColor = 0xFF1E293B
        )
        dao.insertPost(post)
        dao.insertOrUpdateProfile(profile.copy(postsCount = dao.countPostsByAuthor("me")))
    }

    suspend fun toggleLikePost(postId: String) = dao.togglePostLike(postId)

    suspend fun toggleBookmarkPost(postId: String, currentBookmarked: Boolean) {
        dao.updatePostBookmark(postId, !currentBookmarked)
    }

    suspend fun deletePost(postId: String) {
        val post = dao.getPostById(postId) ?: return
        if (post.authorId != "me") return
        dao.deletePost(postId)
        dao.getUserProfileOnce()?.let { profile ->
            dao.insertOrUpdateProfile(profile.copy(postsCount = dao.countPostsByAuthor("me")))
        }
    }

    suspend fun markStoryViewed(storyId: String) = dao.markStoryViewed(storyId)

    suspend fun createStory(title: String) {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isEmpty()) return
        val profile = dao.getUserProfileOnce() ?: UserProfileEntity()
        dao.insertStory(
            StoryEntity(
                id = "story_" + UUID.randomUUID().toString(),
                authorName = profile.displayName,
                authorAvatarColor = profile.avatarColor,
                storyTitle = normalizedTitle,
                isViewed = false,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun getCommentsForPost(postId: String): Flow<List<CommentEntity>> =
        dao.getCommentsForPost(postId)

    suspend fun createComment(postId: String, content: String) {
        val normalizedContent = content.trim()
        if (normalizedContent.isEmpty() || dao.getPostById(postId) == null) return
        val profile = dao.getUserProfileOnce() ?: UserProfileEntity()
        dao.insertComment(
            CommentEntity(
                id = "comment_" + UUID.randomUUID().toString(),
                postId = postId,
                authorId = "me",
                authorName = profile.displayName,
                authorAvatarColor = profile.avatarColor,
                content = normalizedContent,
                timestamp = System.currentTimeMillis()
            )
        )
        dao.updatePostCommentCount(postId, dao.countCommentsForPost(postId))
    }

    suspend fun deleteComment(commentId: String) {
        val comment = dao.getCommentById(commentId) ?: return
        if (comment.authorId != "me") return
        dao.deleteComment(commentId)
        dao.updatePostCommentCount(comment.postId, dao.countCommentsForPost(comment.postId))
    }

    suspend fun updatePreferences(languageCode: String, isDarkMode: Boolean?) {
        val profile = dao.getUserProfileOnce() ?: UserProfileEntity()
        dao.insertOrUpdateProfile(profile.copy(languageCode = languageCode, isDarkMode = isDarkMode))
    }

    suspend fun updateProfile(
        displayName: String,
        bio: String,
        avatarColor: Long,
        languageCode: String,
        isDarkMode: Boolean?
    ) {
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

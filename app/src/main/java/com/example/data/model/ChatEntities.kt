package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val iconName: String = "forum",
    val isDirectMessage: Boolean = false,
    val peerName: String = "",
    val peerAvatarColor: Long = 0xFF4F46E5,
    val unreadCount: Int = 0,
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarColor: Long = 0xFF4F46E5,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isOutgoing: Boolean = false,
    val status: String = "read", // "sent", "delivered", "read"
    val replyToId: String? = null,
    val replyToSnippet: String? = null,
    val reaction: String? = null
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val userId: String = "me",
    val displayName: String = "کاربر اکو / Echo User",
    val bio: String = "علاقه‌مند به تکنولوژی، هوش مصنوعی و ارتباطات مدرن 🚀",
    val avatarColor: Long = 0xFF4F46E5,
    val languageCode: String = "fa",
    val isDarkMode: Boolean? = null,
    val postsCount: Int = 8,
    val followersCount: Int = 1420,
    val followingCount: Int = 385
)

@Entity(tableName = "social_posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val authorName: String,
    val authorRole: String = "عضو اکو / Echo Member",
    val authorAvatarColor: Long = 0xFF4F46E5,
    val content: String,
    val tags: String = "", // comma-separated e.g. "کاتلین,اندروید"
    val category: String = "tech", // "trending", "tech", "design", "ai"
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val cardColor: Long = 0xFF1E293B
)

@Entity(tableName = "user_stories")
data class StoryEntity(
    @PrimaryKey val id: String,
    val authorName: String,
    val authorAvatarColor: Long = 0xFF4F46E5,
    val storyTitle: String,
    val isViewed: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

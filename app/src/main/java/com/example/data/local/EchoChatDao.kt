package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ChannelEntity
import com.example.data.model.CommentEntity
import com.example.data.model.MessageEntity
import com.example.data.model.PostEntity
import com.example.data.model.StoryEntity
import com.example.data.model.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EchoChatDao {

    // --- Channels ---
    @Query("SELECT * FROM channels ORDER BY lastMessageTime DESC")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE id = :channelId LIMIT 1")
    fun getChannelById(channelId: String): Flow<ChannelEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("UPDATE channels SET lastMessage = :lastMsg, lastMessageTime = :time WHERE id = :channelId")
    suspend fun updateChannelLastMessage(channelId: String, lastMsg: String, time: Long)

    @Query("UPDATE channels SET unreadCount = 0 WHERE id = :channelId")
    suspend fun clearChannelUnread(channelId: String)

    @Query("DELETE FROM channels WHERE id = :channelId")
    suspend fun deleteChannel(channelId: String)

    // --- Messages ---
    @Query("SELECT * FROM messages WHERE channelId = :channelId ORDER BY timestamp ASC")
    fun getMessagesForChannel(channelId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("DELETE FROM messages WHERE channelId = :channelId")
    suspend fun deleteMessagesForChannel(channelId: String)

    @Query("UPDATE messages SET reaction = :reaction WHERE id = :messageId")
    suspend fun updateMessageReaction(messageId: String, reaction: String?)

    @Query("SELECT * FROM messages WHERE content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchMessages(query: String): Flow<List<MessageEntity>>

    // --- Social Posts ---
    @Query("SELECT * FROM social_posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM social_posts WHERE isBookmarked = 1 ORDER BY timestamp DESC")
    fun getSavedPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM social_posts WHERE category = :category ORDER BY timestamp DESC")
    fun getPostsByCategory(category: String): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Query("""
        UPDATE social_posts
        SET isLiked = CASE WHEN isLiked = 1 THEN 0 ELSE 1 END,
            likesCount = CASE WHEN isLiked = 1 THEN MAX(likesCount - 1, 0) ELSE likesCount + 1 END
        WHERE id = :postId
    """)
    suspend fun togglePostLike(postId: String)

    @Query("SELECT * FROM social_posts WHERE id = :postId LIMIT 1")
    suspend fun getPostById(postId: String): PostEntity?

    @Query("SELECT COUNT(*) FROM social_posts WHERE authorId = :authorId")
    suspend fun countPostsByAuthor(authorId: String): Int

    @Query("UPDATE social_posts SET commentsCount = :count WHERE id = :postId")
    suspend fun updatePostCommentCount(postId: String, count: Int)

    @Query("UPDATE social_posts SET isBookmarked = :isBookmarked WHERE id = :postId")
    suspend fun updatePostBookmark(postId: String, isBookmarked: Boolean)

    @Query("DELETE FROM social_posts WHERE id = :postId")
    suspend fun deletePost(postId: String)

    // --- Stories ---
    @Query("SELECT * FROM user_stories ORDER BY timestamp DESC")
    fun getAllStories(): Flow<List<StoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStories(stories: List<StoryEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStory(story: StoryEntity)

    @Query("UPDATE user_stories SET isViewed = 1 WHERE id = :storyId")
    suspend fun markStoryViewed(storyId: String)

    // --- Comments ---
    @Query("SELECT * FROM post_comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsForPost(postId: String): Flow<List<CommentEntity>>

    @Query("SELECT * FROM post_comments WHERE id = :commentId LIMIT 1")
    suspend fun getCommentById(commentId: String): CommentEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertComment(comment: CommentEntity)

    @Query("DELETE FROM post_comments WHERE id = :commentId")
    suspend fun deleteComment(commentId: String)

    @Query("SELECT COUNT(*) FROM post_comments WHERE postId = :postId")
    suspend fun countCommentsForPost(postId: String): Int

    // --- Profile ---
    @Query("SELECT * FROM user_profile WHERE userId = 'me' LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE userId = 'me' LIMIT 1")
    suspend fun getUserProfileOnce(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)
}

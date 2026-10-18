package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("SELECT * FROM channels WHERE id = :channelId LIMIT 1")
    suspend fun getChannelByIdOnce(channelId: String): ChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("UPDATE channels SET lastMessage = :lastMsg, lastMessageTime = :time WHERE id = :channelId")
    suspend fun updateChannelLastMessage(channelId: String, lastMsg: String, time: Long)

    @Query("UPDATE channels SET unreadCount = 0 WHERE id = :channelId")
    suspend fun clearChannelUnread(channelId: String)

    @Query("UPDATE channels SET hiddenBeforeTimestamp = :timestamp WHERE id = :channelId")
    suspend fun updateChannelHiddenBefore(channelId: String, timestamp: Long)

    @Query("DELETE FROM channels WHERE id = :channelId")
    suspend fun deleteChannel(channelId: String)

    // --- Messages ---
    @Query("SELECT * FROM messages WHERE channelId = :channelId ORDER BY timestamp ASC")
    fun getMessagesForChannel(channelId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("DELETE FROM messages WHERE channelId = :channelId")
    suspend fun deleteMessagesForChannel(channelId: String)

    @Query("DELETE FROM messages WHERE channelId = :channelId AND id NOT IN (:messageIds)")
    suspend fun deleteMessagesNotIn(channelId: String, messageIds: List<String>)

    @Query("UPDATE messages SET reaction = :reaction WHERE id = :messageId")
    suspend fun updateMessageReaction(messageId: String, reaction: String?)

    @Query("SELECT * FROM messages WHERE content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchMessages(query: String): Flow<List<MessageEntity>>

    // --- Social Posts ---
    @Query("SELECT * FROM social_posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM social_posts")
    suspend fun getAllPostsOnce(): List<PostEntity>

    @Query("SELECT * FROM social_posts WHERE isBookmarked = 1 ORDER BY timestamp DESC")
    fun getSavedPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM social_posts WHERE category = :category ORDER BY timestamp DESC")
    fun getPostsByCategory(category: String): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Query("UPDATE social_posts SET isLiked = CASE WHEN isLiked = 0 THEN 1 ELSE 0 END, likesCount = CASE WHEN isLiked = 0 THEN likesCount + 1 ELSE MAX(likesCount - 1, 0) END WHERE id = :postId")
    suspend fun togglePostLike(postId: String)

    @Query("SELECT * FROM social_posts WHERE id = :postId LIMIT 1")
    suspend fun getPostById(postId: String): PostEntity?

    @Query("DELETE FROM social_posts")
    suspend fun deleteAllPosts()

    @Query("DELETE FROM social_posts WHERE id NOT IN (:postIds)")
    suspend fun deletePostsNotIn(postIds: List<String>)

    @Query("UPDATE social_posts SET isLiked = 0")
    suspend fun clearAllPostLikes()

    @Query("UPDATE social_posts SET isLiked = :isLiked WHERE id = :postId")
    suspend fun updatePostLiked(postId: String, isLiked: Boolean)

    @Query("UPDATE social_posts SET isLiked = 1 WHERE id IN (:postIds)")
    suspend fun markPostsLiked(postIds: List<String>)

    @Query("UPDATE social_posts SET isBookmarked = 0")
    suspend fun clearAllPostBookmarks()

    @Query("UPDATE social_posts SET isBookmarked = 1 WHERE id IN (:postIds)")
    suspend fun markPostsBookmarked(postIds: List<String>)

    @Query("UPDATE social_posts SET isBookmarked = :isBookmarked WHERE id = :postId")
    suspend fun updatePostBookmark(postId: String, isBookmarked: Boolean)

    @Query("UPDATE social_posts SET isBookmarked = CASE WHEN isBookmarked = 0 THEN 1 ELSE 0 END WHERE id = :postId")
    suspend fun togglePostBookmark(postId: String)

    @Query("DELETE FROM social_posts WHERE id = :postId")
    suspend fun deletePost(postId: String)

    @Query("SELECT * FROM post_comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsForPost(postId: String): Flow<List<CommentEntity>>

    @Query("SELECT * FROM post_comments WHERE postId = :postId")
    suspend fun getCommentsForPostOnce(postId: String): List<CommentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<CommentEntity>)

    @Query("DELETE FROM post_comments WHERE postId = :postId")
    suspend fun deleteCommentsForPost(postId: String)

    @Query("DELETE FROM post_comments WHERE postId = :postId AND id NOT IN (:commentIds)")
    suspend fun deleteCommentsNotIn(postId: String, commentIds: List<String>)

    @Query("UPDATE social_posts SET commentsCount = commentsCount + 1 WHERE id = :postId")
    suspend fun incrementPostCommentCount(postId: String)

    @Transaction
    suspend fun insertCommentAndIncrementCount(comment: CommentEntity) {
        insertComment(comment)
        incrementPostCommentCount(comment.postId)
    }

    @Query("DELETE FROM post_comments WHERE id = :commentId")
    suspend fun deleteComment(commentId: String)

    // --- Stories ---
    @Query("SELECT * FROM user_stories ORDER BY timestamp DESC")
    fun getAllStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM user_stories")
    suspend fun getAllStoriesOnce(): List<StoryEntity>

    @Query("SELECT * FROM user_stories WHERE id = :storyId LIMIT 1")
    suspend fun getStoryById(storyId: String): StoryEntity?

    @Query("DELETE FROM user_stories")
    suspend fun deleteAllStories()

    @Query("DELETE FROM user_stories WHERE id = :storyId")
    suspend fun deleteStoryById(storyId: String)

    @Query("DELETE FROM user_stories WHERE id NOT IN (:storyIds)")
    suspend fun deleteStoriesNotIn(storyIds: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStories(stories: List<StoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity)

    @Query("UPDATE user_stories SET isViewed = 1 WHERE id = :storyId")
    suspend fun markStoryViewed(storyId: String)

    // --- Profile ---
    @Query("SELECT * FROM user_profile WHERE userId = 'me' LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE userId = 'me' LIMIT 1")
    suspend fun getUserProfileOnce(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)
}

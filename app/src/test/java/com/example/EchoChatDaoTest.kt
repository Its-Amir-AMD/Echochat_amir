package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.EchoChatDao
import com.example.data.local.EchoDatabase
import com.example.data.model.CommentEntity
import com.example.data.model.PostEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
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
@Config(sdk = [35])
class EchoChatDaoTest {
    private lateinit var database: EchoDatabase
    private lateinit var dao: EchoChatDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, EchoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.chatDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun likeToggleUsesPersistedStateAndNeverProducesNegativeCount() = runBlocking {
        dao.insertPost(
            PostEntity(
                id = "post-test",
                authorId = "user-test",
                authorName = "Test User",
                content = "A test post",
                likesCount = 0,
                isLiked = false
            )
        )

        dao.togglePostLike("post-test")
        val liked = dao.getPostById("post-test")
        assertNotNull(liked)
        assertTrue(requireNotNull(liked).isLiked)
        assertEquals(1, liked.likesCount)

        dao.togglePostLike("post-test")
        val unliked = dao.getPostById("post-test")
        assertNotNull(unliked)
        assertFalse(requireNotNull(unliked).isLiked)
        assertEquals(0, unliked.likesCount)

        dao.togglePostLike("post-test")
        assertEquals(1, dao.getPostById("post-test")?.likesCount)
    }

    @Test
    fun insertingCommentUpdatesPostCountAndCommentFlow() = runBlocking {
        dao.insertPost(
            PostEntity(
                id = "post-comments",
                authorId = "user-test",
                authorName = "Test User",
                content = "Post with comments"
            )
        )

        dao.insertCommentAndIncrementCount(
            CommentEntity(
                id = "comment-test",
                postId = "post-comments",
                authorId = "commenter",
                authorName = "Commenter",
                content = "A meaningful comment"
            )
        )

        assertEquals(1, dao.getPostById("post-comments")?.commentsCount)
        val comments = dao.getCommentsForPost("post-comments").first()
        assertEquals(1, comments.size)
        assertEquals("A meaningful comment", comments.single().content)
    }
}

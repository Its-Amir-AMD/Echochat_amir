package com.example.data.repository

import com.example.data.local.EchoChatDao
import com.example.data.model.ChannelEntity
import com.example.data.model.CommentEntity
import com.example.data.model.MessageEntity
import com.example.data.model.PostEntity
import com.example.data.model.StoryEntity
import com.example.data.model.UserProfileEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val _backendError = MutableStateFlow<String?>(null)
    val backendError: StateFlow<String?> = _backendError.asStateFlow()

    private var memberChannelsRegistration: ListenerRegistration? = null
    private var publicChannelsRegistration: ListenerRegistration? = null
    private var postsRegistration: ListenerRegistration? = null
    private var storiesRegistration: ListenerRegistration? = null
    private var likedPostsRegistration: ListenerRegistration? = null
    private var bookmarkedPostsRegistration: ListenerRegistration? = null
    private val messageRegistrations = mutableMapOf<String, ListenerRegistration>()
    private val commentRegistrations = mutableMapOf<String, ListenerRegistration>()
    @Volatile private var likedPostIds: Set<String> = emptySet()
    @Volatile private var bookmarkedPostIds: Set<String> = emptySet()

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
            try {
                ensureUserDocuments(user)
                ensureAssistantChannel()
                ensureDefaultPublicChannels(user)
                subscribeToChannels(user.uid)
                subscribeToSocialContent(user)
                _backendError.value = null
            } catch (exception: Exception) {
                _backendError.value = exception.localizedMessage
                    ?: "راه‌اندازی همگام‌سازی ابری انجام نشد. قواعد Firebase را بررسی کنید."
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
        postsRegistration?.remove()
        storiesRegistration?.remove()
        likedPostsRegistration?.remove()
        bookmarkedPostsRegistration?.remove()
        memberChannelsRegistration = null
        publicChannelsRegistration = null
        postsRegistration = null
        storiesRegistration = null
        likedPostsRegistration = null
        bookmarkedPostsRegistration = null
        messageRegistrations.values.forEach(ListenerRegistration::remove)
        messageRegistrations.clear()
        commentRegistrations.values.forEach(ListenerRegistration::remove)
        commentRegistrations.clear()
        likedPostIds = emptySet()
        bookmarkedPostIds = emptySet()
    }

    private suspend fun ensureUserDocuments(user: FirebaseUser) {
        val name = user.displayName?.takeIf(String::isNotBlank)
            ?: user.email?.substringBefore('@')?.takeIf(String::isNotBlank)
            ?: "Echo member"
        val now = System.currentTimeMillis()
        val privateRef = firestore.collection("users").document(user.uid)
        val oldPrivate = privateRef.get().await()
        val publicRef = firestore.collection("publicProfiles").document(user.uid)
        val oldPublic = publicRef.get().await()
        val safeDisplayName = oldPublic.getString("displayName")
            ?.takeIf(String::isNotBlank) ?: name
        val avatarColor = oldPublic.getLong("avatarColor") ?: DEFAULT_AVATAR_COLOR
        val privateProfile = mutableMapOf<String, Any>(
            "uid" to user.uid,
            "email" to (user.email ?: ""),
            "displayName" to safeDisplayName,
            "updatedAt" to now
        )
        if (!oldPrivate.exists()) privateProfile["createdAt"] = now
        privateRef.set(privateProfile, SetOptions.merge()).await()
        val publicProfile = mutableMapOf<String, Any>(
            "uid" to user.uid,
            "displayName" to safeDisplayName,
            "avatarColor" to avatarColor,
            "updatedAt" to now
        )
        if (!oldPublic.exists()) publicProfile["createdAt"] = now
        publicRef.set(publicProfile, SetOptions.merge()).await()
        val localProfile = dao.getUserProfileOnce() ?: UserProfileEntity()
        dao.insertOrUpdateProfile(
            localProfile.copy(
                displayName = safeDisplayName,
                avatarColor = avatarColor
            )
        )
    }

    private suspend fun ensureAssistantChannel() {
        if (dao.getChannelByIdOnce(ASSISTANT_CHANNEL_ID) != null) return
        dao.insertChannel(
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
    }

    private suspend fun ensureDefaultPublicChannels(user: FirebaseUser) {
        val defaults = listOf(
            Triple("general", "گفتگوی عمومی / General", "گفت‌وگوی عمومی کاربران EchoChat"),
            Triple("tech", "توسعه و فناوری / Tech Talk", "گفت‌وگو دربارهٔ فناوری و برنامه‌نویسی"),
            Triple("design", "طراحی و رابط کاربری / UI & UX", "گفت‌وگو دربارهٔ طراحی محصول و تجربهٔ کاربری")
        )
        defaults.forEach { (id, name, description) ->
            val reference = firestore.collection("channels").document(id)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(reference)
                if (!snapshot.exists()) {
                    val now = System.currentTimeMillis()
                    transaction.set(
                        reference,
                        mapOf(
                            "name" to name,
                            "description" to description,
                            "iconName" to when (id) {
                                "tech" -> "code"
                                "design" -> "palette"
                                else -> "forum"
                            },
                            "kind" to "channel",
                            "isPublic" to true,
                            "members" to listOf(user.uid),
                            "memberNames" to mapOf(user.uid to currentUserName),
                            "createdBy" to user.uid,
                            "createdAt" to now,
                            "lastMessage" to "",
                            "lastMessageTime" to now
                        )
                    )
                }
                true
            }.await()
        }
    }

    private fun subscribeToChannels(uid: String) {
        memberChannelsRegistration = firestore.collection("channels")
            .whereArrayContains("members", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _backendError.value = error?.localizedMessage ?: "دریافت کانال‌ها ناموفق بود."
                    return@addSnapshotListener
                }
                _backendError.value = null
                syncChannelSnapshot(snapshot.documents, uid)
            }

        // Public channels are discoverable by authenticated users; membership is added only
        // when a user opens one. Private direct messages are never included in this query.
        publicChannelsRegistration = firestore.collection("channels")
            .whereEqualTo("isPublic", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _backendError.value = error?.localizedMessage ?: "دریافت کانال‌های عمومی ناموفق بود."
                    return@addSnapshotListener
                }
                _backendError.value = null
                syncChannelSnapshot(snapshot.documents, uid)
            }
    }

    private fun subscribeToSocialContent(user: FirebaseUser) {
        postsRegistration = firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _backendError.value = error?.localizedMessage ?: "دریافت پست‌ها ناموفق بود."
                    return@addSnapshotListener
                }
                scope.launch(Dispatchers.IO) {
                    val posts = mutableListOf<PostEntity>()
                    for (document in snapshot.documents) {
                        val authorId = document.getString("authorId") ?: continue
                        val content = document.getString("content") ?: continue
                        val local = dao.getPostById(document.id)
                        posts += PostEntity(
                            id = document.id,
                            authorId = authorId,
                            authorName = document.getString("authorName") ?: "Echo member",
                            authorRole = document.getString("authorRole") ?: "Echo member",
                            authorAvatarColor = document.getLong("authorAvatarColor") ?: DEFAULT_AVATAR_COLOR,
                            content = content,
                            tags = document.getString("tags").orEmpty(),
                            category = document.getString("category") ?: "tech",
                            likesCount = (document.getLong("likesCount") ?: 0L).coerceAtLeast(0L).toInt(),
                            commentsCount = (document.getLong("commentsCount") ?: 0L).coerceAtLeast(0L).toInt(),
                            sharesCount = (document.getLong("sharesCount") ?: 0L).coerceAtLeast(0L).toInt(),
                            isLiked = document.id in likedPostIds || local?.isLiked == true,
                            isBookmarked = document.id in bookmarkedPostIds || local?.isBookmarked == true,
                            timestamp = document.getLong("timestamp") ?: 0L,
                            cardColor = document.getLong("cardColor") ?: DEFAULT_POST_COLOR
                        )
                    }
                    if (!snapshot.metadata.isFromCache) {
                        val remoteIds = posts.mapTo(mutableSetOf()) { it.id }
                        dao.getAllPostsOnce().filter { it.id !in remoteIds }.forEach { stale ->
                            dao.deletePost(stale.id)
                        }
                    }
                    if (posts.isNotEmpty()) dao.insertPosts(posts)
                    if (likedPostIds.isNotEmpty()) dao.markPostsLiked(likedPostIds.toList())
                    if (bookmarkedPostIds.isNotEmpty()) dao.markPostsBookmarked(bookmarkedPostIds.toList())
                }
            }

        storiesRegistration = firestore.collection("stories")
            .whereGreaterThan("timestamp", System.currentTimeMillis() - STORY_RETENTION_MS)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _backendError.value = error?.localizedMessage ?: "دریافت استوری‌ها ناموفق بود."
                    return@addSnapshotListener
                }
                scope.launch(Dispatchers.IO) {
                    val stories = mutableListOf<StoryEntity>()
                    for (document in snapshot.documents) {
                        val authorId = document.getString("authorId") ?: continue
                        val title = document.getString("storyTitle") ?: continue
                        val local = dao.getStoryById(document.id)
                        stories += StoryEntity(
                            id = document.id,
                            authorId = authorId,
                            authorName = document.getString("authorName") ?: "Echo member",
                            authorAvatarColor = document.getLong("authorAvatarColor") ?: DEFAULT_AVATAR_COLOR,
                            storyTitle = title,
                            isViewed = local?.isViewed ?: false,
                            timestamp = document.getLong("timestamp") ?: 0L
                        )
                    }
                    if (!snapshot.metadata.isFromCache) {
                        val remoteIds = stories.mapTo(mutableSetOf()) { it.id }
                        dao.getAllStoriesOnce().filter { it.id !in remoteIds }.forEach { stale ->
                            dao.deleteStoryById(stale.id)
                        }
                    }
                    if (stories.isNotEmpty()) dao.insertStories(stories)
                }
            }

        likedPostsRegistration = firestore.collection("users").document(user.uid)
            .collection("likedPosts")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _backendError.value = error?.localizedMessage ?: "همگام‌سازی پسندها ناموفق بود."
                    return@addSnapshotListener
                }
                val ids = snapshot.documents.map { it.id }.toSet()
                likedPostIds = ids
                scope.launch(Dispatchers.IO) {
                    dao.clearAllPostLikes()
                    if (ids.isNotEmpty()) dao.markPostsLiked(ids.toList())
                }
            }

        bookmarkedPostsRegistration = firestore.collection("users").document(user.uid)
            .collection("bookmarks")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _backendError.value = error?.localizedMessage ?: "همگام‌سازی نشانک‌ها ناموفق بود."
                    return@addSnapshotListener
                }
                val ids = snapshot.documents.map { it.id }.toSet()
                bookmarkedPostIds = ids
                scope.launch(Dispatchers.IO) {
                    dao.clearAllPostBookmarks()
                    if (ids.isNotEmpty()) dao.markPostsBookmarked(ids.toList())
                }
            }
    }

    fun observeCommentsForPost(postId: String) {
        if (commentRegistrations.containsKey(postId)) return
        commentRegistrations[postId] = firestore.collection("posts").document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _backendError.value = error?.localizedMessage ?: "دریافت دیدگاه‌ها ناموفق بود."
                    return@addSnapshotListener
                }
                scope.launch(Dispatchers.IO) {
                    val comments = snapshot.documents.mapNotNull { document ->
                        val authorId = document.getString("authorId") ?: return@mapNotNull null
                        val content = document.getString("content") ?: return@mapNotNull null
                        CommentEntity(
                            id = document.id,
                            postId = postId,
                            authorId = authorId,
                            authorName = document.getString("authorName") ?: "Echo member",
                            content = content,
                            timestamp = document.getLong("timestamp") ?: 0L
                        )
                    }
                    if (!snapshot.metadata.isFromCache) {
                        val remoteIds = comments.mapTo(mutableSetOf()) { it.id }
                        dao.getCommentsForPostOnce(postId).filter { it.id !in remoteIds }.forEach { stale ->
                            dao.deleteComment(stale.id)
                        }
                    }
                    if (comments.isNotEmpty()) dao.insertComments(comments)
                }
            }
    }

    fun stopObservingCommentsForPost(postId: String) {
        commentRegistrations.remove(postId)?.remove()
    }

    private fun syncChannelSnapshot(
        documents: List<com.google.firebase.firestore.DocumentSnapshot>,
        uid: String
    ) {
        scope.launch(Dispatchers.IO) {
            val mapped = documents.mapNotNull { document -> channelFromDocument(document, uid) }
                .map { remoteChannel ->
                    val localChannel = dao.getChannelByIdOnce(remoteChannel.id)
                    val hiddenBefore = localChannel?.hiddenBeforeTimestamp ?: 0L
                    remoteChannel.copy(
                        hiddenBeforeTimestamp = hiddenBefore,
                        lastMessage = if (remoteChannel.lastMessageTime <= hiddenBefore) "" else remoteChannel.lastMessage,
                        lastMessageTime = maxOf(remoteChannel.lastMessageTime, hiddenBefore)
                    )
                }
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
                if (error != null || snapshot == null) {
                    _backendError.value = error?.localizedMessage ?: "دریافت پیام‌ها ناموفق بود."
                    return@addSnapshotListener
                }
                scope.launch(Dispatchers.IO) {
                    val channelCache = dao.getChannelByIdOnce(channelId)
                    val hiddenBefore = channelCache?.hiddenBeforeTimestamp ?: 0L
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
                        ).takeIf { it.timestamp > hiddenBefore }
                    }
                    if (!snapshot.metadata.isFromCache) {
                        val ids = messages.map { it.id }
                        if (ids.isEmpty()) dao.deleteMessagesForChannel(channelId)
                        else dao.deleteMessagesNotIn(channelId, ids)
                    }
                    if (messages.isNotEmpty()) dao.insertMessages(messages)
                }
            }
    }

    suspend fun createPost(post: PostEntity) {
        val user = requireCurrentUser()
        val content = post.content.trim()
        require(content.isNotEmpty()) { "متن پست نمی‌تواند خالی باشد." }
        require(content.length <= MAX_POST_LENGTH) { "حداکثر طول پست ۴۰۰۰ نویسه است." }
        val category = post.category.takeIf { it in ALLOWED_POST_CATEGORIES } ?: "tech"
        val now = System.currentTimeMillis()
        val safePost = post.copy(
            authorId = user.uid,
            authorName = currentUserName,
            authorRole = "Echo member",
            authorAvatarColor = dao.getUserProfileOnce()?.avatarColor ?: DEFAULT_AVATAR_COLOR,
            content = content,
            tags = post.tags.trim().take(MAX_TAG_LENGTH),
            category = category,
            likesCount = 0,
            commentsCount = 0,
            sharesCount = 0,
            isLiked = false,
            isBookmarked = false,
            timestamp = now
        )
        val record = mapOf(
            "authorId" to user.uid,
            "authorName" to safePost.authorName,
            "authorRole" to safePost.authorRole,
            "authorAvatarColor" to safePost.authorAvatarColor,
            "content" to safePost.content,
            "tags" to safePost.tags,
            "category" to safePost.category,
            "likesCount" to 0,
            "commentsCount" to 0,
            "sharesCount" to 0,
            "timestamp" to safePost.timestamp,
            "cardColor" to safePost.cardColor,
            "lastCommentId" to ""
        )
        firestore.collection("posts").document(post.id).set(record).await()
        dao.insertPost(safePost)
    }

    suspend fun togglePostLike(postId: String) {
        val user = requireCurrentUser()
        val postReference = firestore.collection("posts").document(postId)
        val likeReference = postReference.collection("likes").document(user.uid)
        val userLikeReference = firestore.collection("users").document(user.uid)
            .collection("likedPosts").document(postId)

        val nowLiked = firestore.runTransaction { transaction ->
            val postSnapshot = transaction.get(postReference)
            val likeSnapshot = transaction.get(likeReference)
            val userLikeSnapshot = transaction.get(userLikeReference)
            require(postSnapshot.exists()) { "این پست دیگر در دسترس نیست." }
            require(likeSnapshot.exists() == userLikeSnapshot.exists()) {
                "وضعیت پسند این پست ناسازگار است؛ صفحه را دوباره باز کنید."
            }
            val currentCount = (postSnapshot.getLong("likesCount") ?: 0L).coerceAtLeast(0L)
            if (likeSnapshot.exists()) {
                transaction.delete(likeReference)
                transaction.delete(userLikeReference)
                transaction.update(postReference, "likesCount", (currentCount - 1L).coerceAtLeast(0L))
                false
            } else {
                val like = mapOf("uid" to user.uid, "postId" to postId, "createdAt" to System.currentTimeMillis())
                transaction.set(likeReference, like)
                transaction.set(userLikeReference, like)
                transaction.update(postReference, "likesCount", currentCount + 1L)
                true
            }
        }.await()
        dao.updatePostLiked(postId, nowLiked)
    }

    suspend fun togglePostBookmark(postId: String) {
        val user = requireCurrentUser()
        val postReference = firestore.collection("posts").document(postId)
        require(postReference.get().await().exists()) { "این پست دیگر در دسترس نیست." }
        val bookmarkReference = firestore.collection("users").document(user.uid)
            .collection("bookmarks").document(postId)
        val isBookmarked = firestore.runTransaction { transaction ->
            val snapshot = transaction.get(bookmarkReference)
            if (snapshot.exists()) {
                transaction.delete(bookmarkReference)
                false
            } else {
                transaction.set(
                    bookmarkReference,
                    mapOf("postId" to postId, "createdAt" to System.currentTimeMillis())
                )
                true
            }
        }.await()
        dao.updatePostBookmark(postId, isBookmarked)
    }

    suspend fun createComment(postId: String, comment: CommentEntity) {
        val user = requireCurrentUser()
        val content = comment.content.trim()
        require(content.isNotEmpty()) { "متن دیدگاه نمی‌تواند خالی باشد." }
        require(content.length <= MAX_COMMENT_LENGTH) { "حداکثر طول دیدگاه ۲۰۰۰ نویسه است." }
        val postReference = firestore.collection("posts").document(postId)
        val commentReference = postReference.collection("comments").document(comment.id)
        val now = System.currentTimeMillis()
        val record = mapOf(
            "postId" to postId,
            "authorId" to user.uid,
            "authorName" to currentUserName,
            "content" to content,
            "timestamp" to now
        )
        firestore.runTransaction { transaction ->
            val postSnapshot = transaction.get(postReference)
            require(postSnapshot.exists()) { "این پست دیگر در دسترس نیست." }
            val currentCount = (postSnapshot.getLong("commentsCount") ?: 0L).coerceAtLeast(0L)
            transaction.set(commentReference, record)
            transaction.update(
                postReference,
                mapOf(
                    "commentsCount" to currentCount + 1L,
                    "lastCommentId" to comment.id
                )
            )
            true
        }.await()
        dao.insertComment(
            comment.copy(
                postId = postId,
                authorId = user.uid,
                authorName = currentUserName,
                content = content,
                timestamp = now
            )
        )
    }

    suspend fun createStory(story: StoryEntity) {
        val user = requireCurrentUser()
        val title = story.storyTitle.trim()
        require(title.isNotEmpty()) { "متن استوری نمی‌تواند خالی باشد." }
        require(title.length <= MAX_STORY_LENGTH) { "حداکثر طول استوری ۵۰۰ نویسه است." }
        val now = System.currentTimeMillis()
        val authorColor = dao.getUserProfileOnce()?.avatarColor ?: DEFAULT_AVATAR_COLOR
        val safeStory = story.copy(
            authorId = user.uid,
            authorName = currentUserName,
            authorAvatarColor = authorColor,
            storyTitle = title,
            timestamp = now,
            isViewed = true
        )
        firestore.collection("stories").document(story.id).set(
            mapOf(
                "authorId" to user.uid,
                "authorName" to currentUserName,
                "authorAvatarColor" to authorColor,
                "storyTitle" to title,
                "timestamp" to now
            )
        ).await()
        dao.insertStory(safeStory)
    }

    suspend fun deletePost(postId: String) {
        val user = requireCurrentUser()
        val postReference = firestore.collection("posts").document(postId)
        val snapshot = postReference.get().await()
        require(snapshot.exists()) { "این پست دیگر در دسترس نیست." }
        require(snapshot.getString("authorId") == user.uid) {
            "فقط نویسنده می‌تواند پست را حذف کند."
        }

        // Firestore does not cascade deletes into subcollections. Remove direct children in
        // batches before deleting the parent document to avoid leaving public orphan records.
        val comments = postReference.collection("comments").get().await().documents
        deleteDocumentsInBatches(comments.map { it.reference })
        val likes = postReference.collection("likes").get().await().documents
        deleteDocumentsInBatches(likes.map { it.reference })
        postReference.delete().await()
        dao.deleteCommentsForPost(postId)
        dao.deletePost(postId)
    }

    private suspend fun deleteDocumentsInBatches(
        references: List<com.google.firebase.firestore.DocumentReference>
    ) {
        references.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { reference -> batch.delete(reference) }
            batch.commit().await()
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
        val user = requireCurrentUser()
        val reference = firestore.collection("channels").document(channelId)
        val snapshot = reference.get().await()
        if (!snapshot.exists() || snapshot.getBoolean("isPublic") != true) return
        if (user.uid in snapshot.get("members").asStringList()) return

        try {
            reference.update("members", FieldValue.arrayUnion(user.uid)).await()
        } catch (exception: Exception) {
            // A concurrent open/send may already have added this UID. Re-read before
            // surfacing the error so idempotent joins do not fail spuriously.
            val latest = reference.get().await()
            if (user.uid !in latest.get("members").asStringList()) throw exception
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
            joinPublicChannelIfNeeded(message.channelId)
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
        val channelReference = firestore.collection("channels").document(message.channelId)
        channelReference.collection("messages").document(message.id).delete().await()

        // Keep the channel preview in sync after a deletion, including the last message.
        val latest = channelReference.collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get().await().documents.firstOrNull()
        channelReference.update(
            mapOf(
                "lastMessage" to (latest?.getString("content") ?: ""),
                "lastMessageTime" to (latest?.getLong("timestamp") ?: 0L)
            )
        ).await()
    }

    suspend fun updatePublicProfile(displayName: String, avatarColor: Long) {
        val user = requireCurrentUser()
        val safeName = displayName.trim().take(60).ifBlank { currentUserName }
        val update = mapOf(
            "uid" to user.uid,
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
        const val MAX_POST_LENGTH = 4_000
        const val MAX_COMMENT_LENGTH = 2_000
        const val MAX_STORY_LENGTH = 500
        const val MAX_TAG_LENGTH = 300
        const val MAX_SYNCED_MESSAGES = 500
        const val STORY_RETENTION_MS = 24 * 60 * 60 * 1000L
        const val DEFAULT_AVATAR_COLOR = 0xFF4F46E5L
        const val DEFAULT_POST_COLOR = 0xFF1E293BL
        val ALLOWED_POST_CATEGORIES = setOf("tech", "design", "ai")
    }
}

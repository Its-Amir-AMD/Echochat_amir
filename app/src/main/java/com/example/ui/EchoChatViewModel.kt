package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.EchoDatabase
import com.example.data.model.ChannelEntity
import com.example.data.model.CommentEntity
import com.example.data.model.MessageEntity
import com.example.data.model.PostEntity
import com.example.data.model.StoryEntity
import com.example.data.model.UserProfileEntity
import com.example.data.repository.EchoChatRepository
import com.example.util.AppLanguage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class EchoChatViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = application.getSharedPreferences("echochat_preferences", 0)
    private val repository: EchoChatRepository

    init {
        val database = EchoDatabase.getInstance(application)
        repository = EchoChatRepository(database.chatDao())
    }

    val channels: StateFlow<List<ChannelEntity>> = repository.allChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val posts: StateFlow<List<PostEntity>> = repository.allPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedPosts: StateFlow<List<PostEntity>> = repository.savedPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stories: StateFlow<List<StoryEntity>> = repository.allStories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bottom Navigation Tab: 0 = Chats, 1 = Explore, 2 = Saved, 3 = Profile
    private val _currentMainTab = MutableStateFlow(0)
    val currentMainTab: StateFlow<Int> = _currentMainTab.asStateFlow()

    // Explore Category Filter: "all", "trending", "tech", "design", "ai"
    private val _selectedExploreCategory = MutableStateFlow("all")
    val selectedExploreCategory: StateFlow<String> = _selectedExploreCategory.asStateFlow()

    private val _selectedChannelId = MutableStateFlow("general")
    val selectedChannelId: StateFlow<String> = _selectedChannelId.asStateFlow()

    private val _currentLanguage = MutableStateFlow(
        if (settings.getString("language", "fa") == "en") AppLanguage.ENGLISH else AppLanguage.PERSIAN
    )
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    // null follows the system theme; explicit preferences survive restarts.
    private val _isDarkMode = MutableStateFlow<Boolean?>(
        if (!settings.contains("dark_mode")) null else settings.getBoolean("dark_mode", true)
    )
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _activeChatCategoryTab = MutableStateFlow(0) // 0: All, 1: Channels, 2: DMs
    val activeChatCategoryTab: StateFlow<Int> = _activeChatCategoryTab.asStateFlow()

    private val _replyingTo = MutableStateFlow<MessageEntity?>(null)
    val replyingTo: StateFlow<MessageEntity?> = _replyingTo.asStateFlow()

    private val _activeCommentsPostId = MutableStateFlow<String?>(null)
    val activeCommentsPostId: StateFlow<String?> = _activeCommentsPostId.asStateFlow()
    val activeComments: StateFlow<List<CommentEntity>> = _activeCommentsPostId
        .flatMapLatest { postId ->
            if (postId.isNullOrBlank()) flowOf(emptyList()) else repository.getCommentsForPost(postId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Migrate preferences from the profile row on older installs, if needed.
        viewModelScope.launch {
            val profile = repository.userProfile.filterNotNull().first()
            if (!settings.contains("language")) {
                _currentLanguage.value = if (profile.languageCode == "en") AppLanguage.ENGLISH else AppLanguage.PERSIAN
            }
            if (!settings.contains("dark_mode")) _isDarkMode.value = profile.isDarkMode
            persistPreferencesLocally()
        }
    }

    // Active Channel details
    val activeChannel: StateFlow<ChannelEntity?> = _selectedChannelId
        .flatMapLatest { id -> repository.getChannel(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Messages for active channel
    val messages: StateFlow<List<MessageEntity>> = _selectedChannelId
        .flatMapLatest { id -> repository.getMessagesForChannel(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search locally stored messages across all channels.
    val searchedMessages: StateFlow<List<MessageEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList()) else repository.searchMessages(query.trim())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Typing user indicator for active channel
    val typingUser: StateFlow<String?> = combine(
        _selectedChannelId,
        repository.typingStatus
    ) { currentId, statusMap ->
        statusMap[currentId]
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Filtered channels based on search and chat category tab
    val filteredChannels: StateFlow<List<ChannelEntity>> = combine(
        channels,
        _searchQuery,
        _activeChatCategoryTab
    ) { allChannels, query, tab ->
        var list = allChannels
        if (tab == 1) {
            list = list.filter { !it.isDirectMessage }
        } else if (tab == 2) {
            list = list.filter { it.isDirectMessage }
        }

        if (query.isNotBlank()) {
            list = list.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true) ||
                it.lastMessage.contains(query, ignoreCase = true)
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered posts based on explore category and search query
    val filteredPosts: StateFlow<List<PostEntity>> = combine(
        posts,
        _selectedExploreCategory,
        _searchQuery
    ) { allPosts, category, query ->
        var list = allPosts
        if (category != "all") {
            list = if (category == "trending") {
                list.sortedByDescending { it.likesCount }
            } else {
                list.filter { it.category == category }
            }
        }

        if (query.isNotBlank()) {
            list = list.filter {
                it.content.contains(query, ignoreCase = true) ||
                it.authorName.contains(query, ignoreCase = true) ||
                it.tags.contains(query, ignoreCase = true)
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setMainTab(tab: Int) {
        _currentMainTab.value = tab
    }

    fun setExploreCategory(category: String) {
        _selectedExploreCategory.value = category
    }

    fun selectChannel(channelId: String) {
        _selectedChannelId.value = channelId
        _replyingTo.value = null
        viewModelScope.launch {
            repository.clearChannelUnread(channelId)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearch(active: Boolean) {
        _isSearchActive.value = active
        if (!active) _searchQuery.value = ""
    }

    fun setActiveChatCategoryTab(tab: Int) {
        _activeChatCategoryTab.value = tab
    }

    fun setReplyingTo(message: MessageEntity?) {
        _replyingTo.value = message
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        val currentChannel = _selectedChannelId.value
        val reply = _replyingTo.value
        viewModelScope.launch {
            repository.sendMessage(
                channelId = currentChannel,
                content = content,
                replyToId = reply?.id,
                replyToSnippet = reply?.content?.take(40)
            )
            _replyingTo.value = null
        }
    }

    fun reactToMessage(messageId: String, emoji: String) {
        viewModelScope.launch {
            repository.setReaction(messageId, emoji)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    fun clearChat() {
        val currentChannel = _selectedChannelId.value
        viewModelScope.launch {
            repository.clearChat(currentChannel)
        }
    }

    fun createChannel(name: String, desc: String, icon: String = "forum") {
        viewModelScope.launch {
            repository.createChannel(name, desc, icon)
        }
    }

    fun startDirectMessage(peerName: String) {
        viewModelScope.launch {
            repository.startDirectMessage(peerName)
        }
    }

    // --- Social Actions ---
    fun createPost(content: String, tags: String, category: String) {
        viewModelScope.launch {
            repository.createPost(content, tags, category)
        }
    }

    fun toggleLikePost(postId: String, currentLiked: Boolean, currentCount: Int) {
        // Update the stored count atomically instead of trusting potentially stale UI state.
        viewModelScope.launch { repository.toggleLikePost(postId) }
    }

    fun toggleBookmarkPost(postId: String, currentBookmarked: Boolean) {
        viewModelScope.launch {
            repository.toggleBookmarkPost(postId, currentBookmarked)
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId)
        }
    }

    fun markStoryViewed(storyId: String) {
        viewModelScope.launch { repository.markStoryViewed(storyId) }
    }

    fun createStory(title: String) {
        viewModelScope.launch { repository.createStory(title) }
    }

    fun openComments(postId: String) { _activeCommentsPostId.value = postId }
    fun closeComments() { _activeCommentsPostId.value = null }

    fun createComment(postId: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch { repository.createComment(postId, content) }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch { repository.deleteComment(commentId) }
    }

    fun toggleLanguage() {
        _currentLanguage.value = if (_currentLanguage.value == AppLanguage.PERSIAN) AppLanguage.ENGLISH else AppLanguage.PERSIAN
        persistPreferences()
    }

    fun setLanguage(lang: AppLanguage) {
        _currentLanguage.value = lang
        persistPreferences()
    }

    fun toggleTheme() {
        _isDarkMode.value = when (_isDarkMode.value) {
            null -> true
            true -> false
            false -> null
        }
        persistPreferences()
    }

    private fun persistPreferencesLocally() {
        val editor = settings.edit().putString("language", _currentLanguage.value.code)
        if (_isDarkMode.value == null) editor.remove("dark_mode") else editor.putBoolean("dark_mode", _isDarkMode.value!!)
        editor.apply()
    }

    private fun persistPreferences() {
        persistPreferencesLocally()
        viewModelScope.launch {
            repository.updatePreferences(_currentLanguage.value.code, _isDarkMode.value)
        }
    }

    fun updateProfile(displayName: String, bio: String, avatarColor: Long) {
        persistPreferencesLocally()
        viewModelScope.launch {
            repository.updateProfile(
                displayName = displayName,
                bio = bio,
                avatarColor = avatarColor,
                languageCode = _currentLanguage.value.code,
                isDarkMode = _isDarkMode.value
            )
        }
    }
}

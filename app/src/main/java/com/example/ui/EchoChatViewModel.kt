package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.EchoDatabase
import com.example.data.model.ChannelEntity
import com.example.data.model.MessageEntity
import com.example.data.model.UserProfileEntity
import com.example.data.repository.EchoChatRepository
import com.example.util.AppLanguage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class EchoChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EchoChatRepository

    init {
        val database = EchoDatabase.getInstance(application)
        repository = EchoChatRepository(database.chatDao(), viewModelScope)
    }

    val channels: StateFlow<List<ChannelEntity>> = repository.allChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedChannelId = MutableStateFlow("general")
    val selectedChannelId: StateFlow<String> = _selectedChannelId.asStateFlow()

    private val _currentLanguage = MutableStateFlow(AppLanguage.PERSIAN)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _isDarkMode = MutableStateFlow<Boolean?>(null) // null = follow system
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _activeTab = MutableStateFlow(0) // 0: All/Chats, 1: Channels, 2: DMs, 3: Settings
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    private val _replyingTo = MutableStateFlow<MessageEntity?>(null)
    val replyingTo: StateFlow<MessageEntity?> = _replyingTo.asStateFlow()

    // Active Channel details
    val activeChannel: StateFlow<ChannelEntity?> = _selectedChannelId
        .flatMapLatest { id -> repository.getChannel(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Messages for active channel
    val messages: StateFlow<List<MessageEntity>> = _selectedChannelId
        .flatMapLatest { id -> repository.getMessagesForChannel(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Typing user indicator for active channel
    val typingUser: StateFlow<String?> = combine(
        _selectedChannelId,
        repository.typingStatus
    ) { currentId, statusMap ->
        statusMap[currentId]
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Filtered channels based on search and tab
    val filteredChannels: StateFlow<List<ChannelEntity>> = combine(
        channels,
        _searchQuery,
        _activeTab
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

    fun setActiveTab(tab: Int) {
        _activeTab.value = tab
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

    fun toggleLanguage() {
        _currentLanguage.value = if (_currentLanguage.value == AppLanguage.PERSIAN) {
            AppLanguage.ENGLISH
        } else {
            AppLanguage.PERSIAN
        }
    }

    fun setLanguage(lang: AppLanguage) {
        _currentLanguage.value = lang
    }

    fun toggleTheme() {
        _isDarkMode.value = when (_isDarkMode.value) {
            null -> true
            true -> false
            false -> null
        }
    }

    fun updateProfile(displayName: String, bio: String, avatarColor: Long) {
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

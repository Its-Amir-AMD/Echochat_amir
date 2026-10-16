package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChannelEntity
import com.example.data.model.MessageEntity
import com.example.ui.EchoChatViewModel
import com.example.ui.components.ChannelListItem
import com.example.ui.components.CreateChannelDialog
import com.example.ui.components.MessageBubble
import com.example.ui.components.MessageInputBar
import com.example.ui.components.ProfileAndSettingsSheet
import com.example.ui.components.StartDirectMessageDialog
import com.example.ui.components.TypingIndicator
import com.example.util.AppLanguage
import com.example.util.EchoStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: EchoChatViewModel,
    modifier: Modifier = Modifier
) {
    val channels by viewModel.filteredChannels.collectAsStateWithLifecycle()
    val activeChannel by viewModel.activeChannel.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val selectedChannelId by viewModel.selectedChannelId.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isSearchActive by viewModel.isSearchActive.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val replyingTo by viewModel.replyingTo.collectAsStateWithLifecycle()
    val typingUser by viewModel.typingUser.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    var showCreateChannelDialog by remember { mutableStateOf(false) }
    var showStartDmDialog by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    // For compact screens, whether we are currently viewing the chat room
    var isViewingChatRoomOnCompact by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    // Handle Android system Back button on compact screens when inside chat room
    BackHandler(enabled = isViewingChatRoomOnCompact) {
        isViewingChatRoomOnCompact = false
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 700.dp

        Scaffold(
            topBar = {
                EchoTopAppBar(
                    activeChannel = if (isWideScreen || isViewingChatRoomOnCompact) activeChannel else null,
                    currentLanguage = currentLanguage,
                    isDarkMode = isDarkMode,
                    isSearchActive = isSearchActive,
                    searchQuery = searchQuery,
                    userAvatarColor = userProfile?.avatarColor ?: 0xFF4F46E5,
                    userName = userProfile?.displayName ?: "Echo",
                    onBackClick = { isViewingChatRoomOnCompact = false },
                    onToggleLanguage = { viewModel.toggleLanguage() },
                    onToggleTheme = { viewModel.toggleTheme() },
                    onSearchClick = { viewModel.toggleSearch(true) },
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onCloseSearch = { viewModel.toggleSearch(false) },
                    onOpenProfile = { showProfileSheet = true },
                    onOpenMenu = { showOptionsMenu = true },
                    onClearChat = { viewModel.clearChat() }
                )
            },
            floatingActionButton = {
                if (!isViewingChatRoomOnCompact || isWideScreen) {
                    FloatingActionButton(
                        onClick = { showCreateChannelDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("create_channel_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = EchoStrings.get("new_channel", currentLanguage)
                        )
                    }
                }
            }
        ) { paddingValues ->
            if (isWideScreen) {
                // Wide Screen: Dual Panel List-Detail
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Left Panel: Channel List
                    Column(
                        modifier = Modifier
                            .width(340.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        CategoryTabs(
                            activeTab = activeTab,
                            language = currentLanguage,
                            onTabSelected = { viewModel.setActiveTab(it) }
                        )

                        ChannelListContent(
                            channels = channels,
                            selectedChannelId = selectedChannelId,
                            language = currentLanguage,
                            onChannelSelected = { channelId ->
                                viewModel.selectChannel(channelId)
                            },
                            onNewDmClick = { showStartDmDialog = true }
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    )

                    // Right Panel: Active Chat Room
                    Box(modifier = Modifier.weight(1f)) {
                        ChatRoomContent(
                            activeChannel = activeChannel,
                            messages = messages,
                            typingUser = typingUser,
                            inputText = inputText,
                            replyingTo = replyingTo,
                            language = currentLanguage,
                            onTextChanged = { inputText = it },
                            onSendMessage = {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            },
                            onReply = { viewModel.setReplyingTo(it) },
                            onCancelReply = { viewModel.setReplyingTo(null) },
                            onDelete = { viewModel.deleteMessage(it) },
                            onReact = { id, emoji -> viewModel.reactToMessage(id, emoji) }
                        )
                    }
                }
            } else {
                // Compact Screen (Mobile): Toggle between Channel List and Chat Room
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    AnimatedContent(
                        targetState = isViewingChatRoomOnCompact,
                        transitionSpec = {
                            if (targetState) {
                                (slideInHorizontally { width -> width } + fadeIn())
                                    .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                            } else {
                                (slideInHorizontally { width -> -width } + fadeIn())
                                    .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                            }
                        },
                        label = "screen_transition"
                    ) { viewingChat ->
                        if (viewingChat) {
                            ChatRoomContent(
                                activeChannel = activeChannel,
                                messages = messages,
                                typingUser = typingUser,
                                inputText = inputText,
                                replyingTo = replyingTo,
                                language = currentLanguage,
                                onTextChanged = { inputText = it },
                                onSendMessage = {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                },
                                onReply = { viewModel.setReplyingTo(it) },
                                onCancelReply = { viewModel.setReplyingTo(null) },
                                onDelete = { viewModel.deleteMessage(it) },
                                onReact = { id, emoji -> viewModel.reactToMessage(id, emoji) }
                            )
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                CategoryTabs(
                                    activeTab = activeTab,
                                    language = currentLanguage,
                                    onTabSelected = { viewModel.setActiveTab(it) }
                                )

                                ChannelListContent(
                                    channels = channels,
                                    selectedChannelId = selectedChannelId,
                                    language = currentLanguage,
                                    onChannelSelected = { channelId ->
                                        viewModel.selectChannel(channelId)
                                        isViewingChatRoomOnCompact = true
                                    },
                                    onNewDmClick = { showStartDmDialog = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs & Sheets
    if (showCreateChannelDialog) {
        CreateChannelDialog(
            language = currentLanguage,
            onDismiss = { showCreateChannelDialog = false },
            onCreate = { name, desc, icon ->
                viewModel.createChannel(name, desc, icon)
                showCreateChannelDialog = false
            }
        )
    }

    if (showStartDmDialog) {
        StartDirectMessageDialog(
            language = currentLanguage,
            onDismiss = { showStartDmDialog = false },
            onStartChat = { peerName ->
                viewModel.startDirectMessage(peerName)
                showStartDmDialog = false
            }
        )
    }

    if (showProfileSheet) {
        ProfileAndSettingsSheet(
            profile = userProfile,
            currentLanguage = currentLanguage,
            isDarkMode = isDarkMode,
            onDismiss = { showProfileSheet = false },
            onSaveProfile = { name, bio, color ->
                viewModel.updateProfile(name, bio, color)
            },
            onToggleLanguage = { viewModel.toggleLanguage() },
            onToggleTheme = { viewModel.toggleTheme() },
            sheetState = sheetState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EchoTopAppBar(
    activeChannel: ChannelEntity?,
    currentLanguage: AppLanguage,
    isDarkMode: Boolean?,
    isSearchActive: Boolean,
    searchQuery: String,
    userAvatarColor: Long,
    userName: String,
    onBackClick: () -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit,
    onSearchClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenMenu: () -> Unit,
    onClearChat: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        navigationIcon = {
            if (activeChannel != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            } else {
                // Branded Echo Chat soundwave icon
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp, end = 4.dp)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = "EchoChat Logo",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        title = {
            if (isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(EchoStrings.get("search_hint", currentLanguage), fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_text_input"),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = onCloseSearch) {
                            Icon(Icons.Default.Close, contentDescription = "Close search")
                        }
                    }
                )
            } else if (activeChannel != null) {
                Column {
                    Text(
                        text = activeChannel.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (activeChannel.isDirectMessage) EchoStrings.get("online", currentLanguage)
                        else activeChannel.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (activeChannel.isDirectMessage) Color(0xFF10B981)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Column {
                    Text(
                        text = EchoStrings.get("app_title", currentLanguage),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = EchoStrings.get("app_subtitle", currentLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        actions = {
            if (!isSearchActive) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = EchoStrings.get("search_hint", currentLanguage)
                    )
                }

                // Fast Language Toggle Pill (FA | EN)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onToggleLanguage() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("language_toggle_pill")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.PERSIAN) "FA" else "EN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Theme Toggle
                IconButton(onClick = onToggleTheme) {
                    Icon(
                        imageVector = if (isDarkMode == true) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Theme"
                    )
                }

                // User Avatar
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(userAvatarColor))
                        .clickable { onOpenProfile() }
                        .testTag("user_profile_avatar"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.take(1).ifEmpty { "U" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Overflow Menu
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(EchoStrings.get("profile_title", currentLanguage)) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onOpenProfile()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(EchoStrings.get("clear_chat", currentLanguage)) },
                            leadingIcon = { Icon(Icons.Default.ClearAll, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onClearChat()
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun CategoryTabs(
    activeTab: Int,
    language: AppLanguage,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        EchoStrings.get("tab_chats", language),
        EchoStrings.get("tab_channels", language),
        EchoStrings.get("tab_contacts", language)
    )

    ScrollableTabRow(
        selectedTabIndex = activeTab,
        edgePadding = 12.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = activeTab == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
            )
        }
    }
}

@Composable
fun ChannelListContent(
    channels: List<ChannelEntity>,
    selectedChannelId: String,
    language: AppLanguage,
    onChannelSelected: (String) -> Unit,
    onNewDmClick: () -> Unit
) {
    if (channels.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = EchoStrings.get("no_channels_found", language),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
        ) {
            items(channels, key = { it.id }) { channel ->
                ChannelListItem(
                    channel = channel,
                    isSelected = channel.id == selectedChannelId,
                    onClick = { onChannelSelected(channel.id) },
                    language = language
                )
            }
        }
    }
}

@Composable
fun ChatRoomContent(
    activeChannel: ChannelEntity?,
    messages: List<MessageEntity>,
    typingUser: String?,
    inputText: String,
    replyingTo: MessageEntity?,
    language: AppLanguage,
    onTextChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onReply: (MessageEntity) -> Unit,
    onCancelReply: () -> Unit,
    onDelete: (String) -> Unit,
    onReact: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Channel Info Header Bar
        if (activeChannel != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (activeChannel.isDirectMessage) "گفتگوی زنده با ${activeChannel.name}"
                        else "کانال فعال: ${activeChannel.name} • ${activeChannel.description}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Messages List
        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = EchoStrings.get("no_messages_yet", language),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            language = language,
                            onReply = onReply,
                            onDelete = onDelete,
                            onReact = onReact
                        )
                    }
                }
            }
        }

        // Live Typing indicator
        AnimatedVisibility(
            visible = !typingUser.isNullOrBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (!typingUser.isNullOrBlank()) {
                TypingIndicator(typingUser = typingUser, language = language)
            }
        }

        // Message input bar
        MessageInputBar(
            inputText = inputText,
            onTextChanged = onTextChanged,
            onSend = onSendMessage,
            replyingTo = replyingTo,
            onCancelReply = onCancelReply,
            language = language
        )
    }
}

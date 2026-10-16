package com.example.data.repository

import com.example.data.local.EchoChatDao
import com.example.data.model.ChannelEntity
import com.example.data.model.MessageEntity
import com.example.data.model.UserProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class EchoChatRepository(
    private val dao: EchoChatDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    val allChannels: Flow<List<ChannelEntity>> = dao.getAllChannels()
    val userProfile: Flow<UserProfileEntity?> = dao.getUserProfile()

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
            status = "read",
            replyToId = replyToId,
            replyToSnippet = replyToSnippet
        )

        dao.insertMessage(outgoingMsg)
        dao.updateChannelLastMessage(channelId, content.trim(), now)

        // Trigger smart automated reply or simulated response
        triggerSimulatedReply(channelId, content.trim(), userMsgId)
    }

    private fun triggerSimulatedReply(channelId: String, userText: String, replyToId: String) {
        scope.launch {
            // Determine respondent based on channel
            val respondentName = when (channelId) {
                "bot_echo" -> "دستیار هوشمند اکو"
                "tech" -> "رضا (توسعه‌دهنده)"
                "design" -> "مونا (طراح UX)"
                "dm_sara" -> "سارا احمدی"
                else -> "علی راد"
            }
            val respondentColor = when (channelId) {
                "bot_echo" -> 0xFF6366F1
                "tech" -> 0xFF3B82F6
                "design" -> 0xFF8B5CF6
                "dm_sara" -> 0xFFEC4899
                else -> 0xFF10B981
            }

            // Show typing indicator
            _typingStatus.value = _typingStatus.value + (channelId to respondentName)
            delay(1600) // Realistic typing delay

            val replyText = generateBotReply(channelId, userText)

            val replyMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                channelId = channelId,
                senderId = "peer_$channelId",
                senderName = respondentName,
                senderAvatarColor = respondentColor,
                content = replyText,
                timestamp = System.currentTimeMillis(),
                isOutgoing = false,
                status = "read",
                replyToId = replyToId,
                replyToSnippet = if (userText.length > 40) userText.take(40) + "..." else userText
            )

            dao.insertMessage(replyMsg)
            dao.updateChannelLastMessage(channelId, replyText, System.currentTimeMillis())

            // Clear typing indicator
            _typingStatus.value = _typingStatus.value - channelId
        }
    }

    private fun generateBotReply(channelId: String, input: String): String {
        val lower = input.lowercase()
        return when {
            channelId == "bot_echo" -> {
                when {
                    "سلام" in input || "hello" in lower || "hi" in lower ->
                        "سلام و درود! چطور می‌تونم در اکو چت کمکتون کنم؟ 🌟"
                    "حالت چطوره" in input || "چطوری" in input || "how are you" in lower ->
                        "عالی هستم، ممنون! آماده‌ام تا تمام پیام‌های شما را پردازش و بازتاب دهم ⚡"
                    "فارسی" in input || "زبان" in input || "persian" in lower || "language" in lower ->
                        "اکو چت به صورت کامل از چینش راست‌به‌چپ (RTL) و زبان فارسی و انگلیسی پشتیبانی می‌کند."
                    "امکانات" in input || "feature" in lower ->
                        "امکانات فعال: ارسال پیام سریع، واکنش با ایموجی، پاسخ (Quote Reply)، جستجو، کانال‌بندی و حالت تاریک/روشن 📱"
                    else ->
                        "اکو دریافت شد: «$input» ✅\nپیام شما با موفقیت ثبت گردید و پردازش شد."
                }
            }
            channelId == "tech" -> {
                when {
                    "compose" in lower || "کاتلین" in input ->
                        "کاتلین با استفاده از StateFlow و Coroutines در کنار Jetpack Compose عملکرد فوق‌العاده‌ای روی اندروید داره!"
                    else ->
                        "نکته فنی جالبی بود! معماری MVVM و پایگاه‌داده Room این سرعت واکنش سریع رو فراهم کرده."
                }
            }
            channelId == "design" -> {
                "ترکیب رنگ‌های Indigo و Slate همراه با انیمیشن‌های نرم Spring، جلوه بسیار شیک و مدرنی به برنامه داده است 🎨"
            }
            channelId == "dm_sara" -> {
                "پیامت رو دیدم! بسیار عالیه، حتماً بخش‌های جدید رو با هم تست و هماهنگ می‌کنیم ✨"
            }
            else -> {
                "ممنون از ارسالت! نظرت در مورد ویژگی‌های اکو چت چیه؟"
            }
        }
    }

    suspend fun setReaction(messageId: String, reaction: String?) {
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

    suspend fun updateProfile(
        displayName: String,
        bio: String,
        avatarColor: Long,
        languageCode: String,
        isDarkMode: Boolean?
    ) {
        dao.insertOrUpdateProfile(
            UserProfileEntity(
                userId = "me",
                displayName = displayName,
                bio = bio,
                avatarColor = avatarColor,
                languageCode = languageCode,
                isDarkMode = isDarkMode
            )
        )
    }
}

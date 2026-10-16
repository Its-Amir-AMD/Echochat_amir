package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ChannelEntity
import com.example.data.model.MessageEntity
import com.example.data.model.UserProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [ChannelEntity::class, MessageEntity::class, UserProfileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class EchoDatabase : RoomDatabase() {
    abstract fun chatDao(): EchoChatDao

    companion object {
        @Volatile
        private var INSTANCE: EchoDatabase? = null

        fun getInstance(context: Context): EchoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EchoDatabase::class.java,
                    "echochat_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default seed data
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = getInstance(context).chatDao()
                                seedInitialData(dao)
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedInitialData(dao: EchoChatDao) {
            val now = System.currentTimeMillis()

            val defaultChannels = listOf(
                ChannelEntity(
                    id = "general",
                    name = "گفتگوی عمومی / General",
                    description = "بحث و گفتگو پیرامون موضوعات مختلف",
                    iconName = "forum",
                    isDirectMessage = false,
                    lastMessage = "به جامعه کاربران اکو چت خوش آمدید! 👋",
                    lastMessageTime = now - 60000 * 5,
                    createdAt = now - 86400000
                ),
                ChannelEntity(
                    id = "tech",
                    name = "توسعه و فناوری / Tech Talk",
                    description = "برنامه‌نویسی اندروید، کاتلین و جت‌پک کامپوز",
                    iconName = "code",
                    isDirectMessage = false,
                    lastMessage = "کاتلین و Compose واقعاً سرعت توسعه رو چند برابر می‌کنند 🚀",
                    lastMessageTime = now - 60000 * 15,
                    createdAt = now - 86400000
                ),
                ChannelEntity(
                    id = "design",
                    name = "طراحی و رابط کاربری / UI & UX",
                    description = "اصول Material 3، رنگ‌ها و تجربه کاربری مدرن",
                    iconName = "palette",
                    isDirectMessage = false,
                    lastMessage = "استفاده از سیستم تم ماتریال دیزاین ۳ خیلی جذاب شده!",
                    lastMessageTime = now - 60000 * 45,
                    createdAt = now - 86400000
                ),
                ChannelEntity(
                    id = "bot_echo",
                    name = "دستیار هوشمند اکو / Echo Bot",
                    description = "پاسخگوی سریع، بازتاب پیام‌ها و دستیار گفتگو",
                    iconName = "smart_toy",
                    isDirectMessage = true,
                    peerName = "Echo Assistant",
                    peerAvatarColor = 0xFF6366F1,
                    lastMessage = "سلام! من دستیار هوشمند اکو هستم. هر پیامی بفرستید فوراً پاسخ می‌دهم ✨",
                    lastMessageTime = now,
                    createdAt = now
                ),
                ChannelEntity(
                    id = "dm_sara",
                    name = "سارا احمدی / Sara",
                    description = "طراح محصول",
                    iconName = "person",
                    isDirectMessage = true,
                    peerName = "سارا احمدی",
                    peerAvatarColor = 0xFFEC4899,
                    lastMessage = "طرح‌های صفحه گفتگو رو دیدی؟ نظرت چیه؟",
                    lastMessageTime = now - 60000 * 120,
                    createdAt = now - 86400000
                )
            )
            dao.insertChannels(defaultChannels)

            val seedMessages = listOf(
                MessageEntity(
                    id = UUID.randomUUID().toString(),
                    channelId = "general",
                    senderId = "system",
                    senderName = "سیستم اکو",
                    senderAvatarColor = 0xFF4F46E5,
                    content = "🎉 به اکو چت خوش آمدید! این یک پیام‌رسان بومی و مدرن ساخته‌شده با Jetpack Compose است.",
                    timestamp = now - 60000 * 60,
                    isOutgoing = false,
                    status = "read"
                ),
                MessageEntity(
                    id = UUID.randomUUID().toString(),
                    channelId = "general",
                    senderId = "ali",
                    senderName = "علی راد",
                    senderAvatarColor = 0xFF10B981,
                    content = "سلام به همه دوستان! پشتیبانی از چیدمان راست‌به‌چپ (RTL) و فارسی عالی کار میکنه 👍",
                    timestamp = now - 60000 * 30,
                    isOutgoing = false,
                    status = "read"
                ),
                MessageEntity(
                    id = UUID.randomUUID().toString(),
                    channelId = "general",
                    senderId = "bot_echo",
                    senderName = "دستیار اکو",
                    senderAvatarColor = 0xFF6366F1,
                    content = "به جامعه کاربران اکو چت خوش آمدید! 👋 هر پیامی ارسال کنید ذخیره و بررسی می‌شود.",
                    timestamp = now - 60000 * 5,
                    isOutgoing = false,
                    status = "read"
                ),
                MessageEntity(
                    id = UUID.randomUUID().toString(),
                    channelId = "tech",
                    senderId = "reza",
                    senderName = "رضا کاظمی",
                    senderAvatarColor = 0xFF3B82F6,
                    content = "کاتلین و Compose واقعاً سرعت توسعه رو چند برابر می‌کنند 🚀",
                    timestamp = now - 60000 * 15,
                    isOutgoing = false,
                    status = "read"
                ),
                MessageEntity(
                    id = UUID.randomUUID().toString(),
                    channelId = "bot_echo",
                    senderId = "bot_echo",
                    senderName = "Echo Assistant",
                    senderAvatarColor = 0xFF6366F1,
                    content = "سلام! من دستیار هوشمند اکو هستم. هر پیامی بفرستید فوراً پاسخ می‌دهم ✨",
                    timestamp = now,
                    isOutgoing = false,
                    status = "read"
                ),
                MessageEntity(
                    id = UUID.randomUUID().toString(),
                    channelId = "dm_sara",
                    senderId = "dm_sara",
                    senderName = "سارا احمدی",
                    senderAvatarColor = 0xFFEC4899,
                    content = "طرح‌های صفحه گفتگو رو دیدی؟ نظرت چیه؟",
                    timestamp = now - 60000 * 120,
                    isOutgoing = false,
                    status = "read"
                )
            )
            dao.insertMessages(seedMessages)

            dao.insertOrUpdateProfile(
                UserProfileEntity(
                    userId = "me",
                    displayName = "کاربر اکو چت",
                    bio = "علاقه‌مند به دنیای چت و ارتباطات هوشمند ✨",
                    avatarColor = 0xFF4F46E5,
                    languageCode = "fa"
                )
            )
        }
    }
}

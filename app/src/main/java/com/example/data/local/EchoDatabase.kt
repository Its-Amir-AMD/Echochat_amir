package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ChannelEntity
import com.example.data.model.MessageEntity
import com.example.data.model.PostEntity
import com.example.data.model.StoryEntity
import com.example.data.model.UserProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        ChannelEntity::class,
        MessageEntity::class,
        UserProfileEntity::class,
        PostEntity::class,
        StoryEntity::class
    ],
    version = 2,
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

            // Seed Channels
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

            // Seed Messages
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

            // Seed Stories
            val seedStories = listOf(
                StoryEntity(
                    id = "story_1",
                    authorName = "دستیار اکو",
                    authorAvatarColor = 0xFF6366F1,
                    storyTitle = "امکانات جدید نسخه ۲.۰ منتشر شد! 🚀",
                    isViewed = false,
                    timestamp = now - 60000 * 20
                ),
                StoryEntity(
                    id = "story_2",
                    authorName = "سارا احمدی",
                    authorAvatarColor = 0xFFEC4899,
                    storyTitle = "پالت رنگی نئومورفیک ایندیگو 🎨",
                    isViewed = false,
                    timestamp = now - 60000 * 45
                ),
                StoryEntity(
                    id = "story_3",
                    authorName = "علی راد",
                    authorAvatarColor = 0xFF10B981,
                    storyTitle = "توسعه کاتلین با معماری تمیز",
                    isViewed = true,
                    timestamp = now - 60000 * 90
                ),
                StoryEntity(
                    id = "story_4",
                    authorName = "مونا فرهمند",
                    authorAvatarColor = 0xFFF59E0B,
                    storyTitle = "تحقیقات کاربری بخش اکسپلور",
                    isViewed = false,
                    timestamp = now - 60000 * 150
                )
            )
            dao.insertStories(seedStories)

            // Seed Social Posts
            val seedPosts = listOf(
                PostEntity(
                    id = "post_1",
                    authorId = "user_ali",
                    authorName = "علی راد",
                    authorRole = "توسعه‌دهنده ارشد اندروید 📱",
                    authorAvatarColor = 0xFF10B981,
                    content = "ترکیب Jetpack Compose با معماری MVI یا MVVM باعث میشه جریان‌های داده خیلی روان‌تر بشن. توی اکو چت همه چیز به شکل ری‌اکتیو با StateFlow و Room مدیریت میشه که زمان پاسخ‌دهی رو به حداقل رسونده!",
                    tags = "کاتلین,اندروید,کامپوز",
                    category = "tech",
                    likesCount = 142,
                    commentsCount = 28,
                    sharesCount = 15,
                    isLiked = true,
                    isBookmarked = false,
                    timestamp = now - 60000 * 35,
                    cardColor = 0xFF1E293B
                ),
                PostEntity(
                    id = "post_2",
                    authorId = "user_sara",
                    authorName = "سارا احمدی",
                    authorRole = "طراح رابط و تجربه کاربری (UI/UX) 🎨",
                    authorAvatarColor = 0xFFEC4899,
                    content = "طراحی برای زبان‌های راست‌به‌چپ (RTL) نیازمند توجه به تقارن معکوس، انحنای حباب‌های چت و آیکون‌های متقارن هست. در طراحی جدید اکو، با یک کلیک چیدمان کاملاً برعکس و استاندارد میشه!",
                    tags = "دیزاین,RTL,ماتریال۳",
                    category = "design",
                    likesCount = 219,
                    commentsCount = 45,
                    sharesCount = 38,
                    isLiked = false,
                    isBookmarked = true,
                    timestamp = now - 60000 * 110,
                    cardColor = 0xFF2E1065
                ),
                PostEntity(
                    id = "post_3",
                    authorId = "user_ai",
                    authorName = "دستیار هوشمند اکو",
                    authorRole = "هوش مصنوعی و پردازش زبان طبیعی 🤖",
                    authorAvatarColor = 0xFF6366F1,
                    content = "آینده شبکه‌های اجتماعی و پیام‌رسان‌ها در تعامل هوشمند محتوا نهفته است. سیستم اکو پیام‌ها رو درک می‌کنه، پیشنهاد پاسخ میده و محتوای اکسپلور رو با توجه به علایق شما دسته‌بندی می‌کنه.",
                    tags = "هوش_مصنوعی,اکو,ترند",
                    category = "ai",
                    likesCount = 380,
                    commentsCount = 64,
                    sharesCount = 82,
                    isLiked = true,
                    isBookmarked = true,
                    timestamp = now - 60000 * 240,
                    cardColor = 0xFF1E1B4B
                ),
                PostEntity(
                    id = "post_4",
                    authorId = "user_reza",
                    authorName = "رضا کاظمی",
                    authorRole = "معمار نرم‌افزار 💻",
                    authorAvatarColor = 0xFF3B82F6,
                    content = "سرعت بیلد در Gradle با KSP و کاتلین ۲.۰ جهش چشمگیری داشته. دیگه نیازی به منتظر موندن‌های طولانی kapt نیست و تست‌های محلی در چند میلی‌ثانیه اجرا میشن.",
                    tags = "تکنولوژی,گریدل,سرعت",
                    category = "tech",
                    likesCount = 98,
                    commentsCount = 12,
                    sharesCount = 7,
                    isLiked = false,
                    isBookmarked = false,
                    timestamp = now - 60000 * 480,
                    cardColor = 0xFF0F172A
                )
            )
            dao.insertPosts(seedPosts)

            // Seed User Profile
            dao.insertOrUpdateProfile(
                UserProfileEntity(
                    userId = "me",
                    displayName = "کاربر اکو چت",
                    bio = "علاقه‌مند به دنیای چت، سوشال مدیا و ارتباطات هوشمند ✨",
                    avatarColor = 0xFF4F46E5,
                    languageCode = "fa",
                    postsCount = 4,
                    followersCount = 1250,
                    followingCount = 340
                )
            )
        }
    }
}

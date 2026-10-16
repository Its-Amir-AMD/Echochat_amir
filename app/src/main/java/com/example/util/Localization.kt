package com.example.util

import androidx.compose.ui.unit.LayoutDirection

enum class AppLanguage(val code: String, val label: String, val layoutDirection: LayoutDirection) {
    PERSIAN("fa", "فارسی", LayoutDirection.Rtl),
    ENGLISH("en", "English", LayoutDirection.Ltr)
}

object EchoStrings {
    private val faStrings = mapOf(
        "app_title" to "اکو چت",
        "app_subtitle" to "پیام‌رسان سریع و امن",
        "tab_chats" to "گفتگوها",
        "tab_channels" to "کانال‌ها",
        "tab_contacts" to "مخاطبین",
        "tab_settings" to "تنظیمات",
        "search_hint" to "جستجوی پیام‌ها و کانال‌ها...",
        "online" to "آنلاین",
        "offline" to "آفلاین",
        "typing" to "در حال نوشتن...",
        "send_hint" to "پیام خود را بنویسید...",
        "send" to "ارسال",
        "reply" to "پاسخ",
        "copy" to "کپی متن",
        "delete" to "حذف پیام",
        "copied_toast" to "پیام کپی شد",
        "deleted_toast" to "پیام حذف شد",
        "react" to "واکنش",
        "new_channel" to "کانال جدید",
        "new_dm" to "گفتگوی جدید",
        "create_channel_title" to "ایجاد کانال جدید",
        "channel_name_label" to "نام کانال",
        "channel_desc_label" to "توضیحات کانال",
        "cancel" to "انصراف",
        "create" to "ایجاد",
        "channel_created" to "کانال با موفقیت ایجاد شد",
        "profile_title" to "پروفایل کاربری",
        "display_name_label" to "نام نمایشی",
        "bio_label" to "درباره من / بیو",
        "save_profile" to "ذخیره تغییرات",
        "profile_saved" to "تغییرات با موفقیت ذخیره شد",
        "appearance" to "ظاهر و پوسته",
        "dark_mode" to "حالت تاریک",
        "light_mode" to "حالت روشن",
        "language_selection" to "زبان برنامه",
        "persian" to "فارسی",
        "english" to "English",
        "clear_chat" to "پاک‌سازی تاریخچه پیام‌ها",
        "clear_chat_confirm" to "آیا از حذف تمام پیام‌های این گفتگو اطمینان دارید؟",
        "members_count" to "عضو",
        "echo_bot_name" to "دستیار هوشمند اکو",
        "echo_bot_desc" to "پاسخگوی خودکار به تمام پیام‌های شما",
        "welcome_general_title" to "به گفتگوی عمومی خوش آمدید!",
        "welcome_general_desc" to "اینجا می‌توانید با دوستان خود و ربات‌های هوشمند چت کنید.",
        "replying_to" to "پاسخ به",
        "no_messages_yet" to "هنوز پیامی ارسال نشده است. اولین پیام را بفرستید!",
        "no_channels_found" to "هیچ موردی یافت نشد",
        "today" to "امروز",
        "yesterday" to "دیروز",
        "status_sent" to "ارسال شد",
        "status_delivered" to "تحویل داده شد",
        "status_read" to "خوانده شد"
    )

    private val enStrings = mapOf(
        "app_title" to "EchoChat",
        "app_subtitle" to "Fast & Secure Messenger",
        "tab_chats" to "Chats",
        "tab_channels" to "Channels",
        "tab_contacts" to "Contacts",
        "tab_settings" to "Settings",
        "search_hint" to "Search messages and channels...",
        "online" to "Online",
        "offline" to "Offline",
        "typing" to "typing...",
        "send_hint" to "Type a message...",
        "send" to "Send",
        "reply" to "Reply",
        "copy" to "Copy Text",
        "delete" to "Delete Message",
        "copied_toast" to "Message copied to clipboard",
        "deleted_toast" to "Message deleted",
        "react" to "React",
        "new_channel" to "New Channel",
        "new_dm" to "New Chat",
        "create_channel_title" to "Create New Channel",
        "channel_name_label" to "Channel Name",
        "channel_desc_label" to "Channel Description",
        "cancel" to "Cancel",
        "create" to "Create",
        "channel_created" to "Channel created successfully",
        "profile_title" to "User Profile",
        "display_name_label" to "Display Name",
        "bio_label" to "About / Bio",
        "save_profile" to "Save Changes",
        "profile_saved" to "Profile updated successfully",
        "appearance" to "Appearance & Theme",
        "dark_mode" to "Dark Mode",
        "light_mode" to "Light Mode",
        "language_selection" to "App Language",
        "persian" to "فارسی",
        "english" to "English",
        "clear_chat" to "Clear Message History",
        "clear_chat_confirm" to "Are you sure you want to clear all messages in this chat?",
        "members_count" to "members",
        "echo_bot_name" to "Echo Smart Bot",
        "echo_bot_desc" to "Instant smart responder to all your messages",
        "welcome_general_title" to "Welcome to General Chat!",
        "welcome_general_desc" to "Here you can chat with colleagues and smart bots.",
        "replying_to" to "Replying to",
        "no_messages_yet" to "No messages yet. Send the first message!",
        "no_channels_found" to "No results found",
        "today" to "Today",
        "yesterday" to "Yesterday",
        "status_sent" to "Sent",
        "status_delivered" to "Delivered",
        "status_read" to "Read"
    )

    fun get(key: String, language: AppLanguage): String {
        val dict = when (language) {
            AppLanguage.PERSIAN -> faStrings
            AppLanguage.ENGLISH -> enStrings
        }
        return dict[key] ?: enStrings[key] ?: key
    }
}

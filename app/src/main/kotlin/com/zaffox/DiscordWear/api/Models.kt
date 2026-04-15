package com.zaffox.discordwear.api

import org.json.JSONArray
import org.json.JSONObject

// ── User ─────────────────────────────────────────────────────────────────────

data class DiscordUser(
    val id: String,
    val username: String,
    val discriminator: String,
    val globalName: String?,
    val avatarHash: String?
) {
    val displayName: String get() = globalName ?: username
    fun avatarUrl(size: Int = 64): String =
        if (avatarHash != null)
            "https://cdn.discordapp.com/avatars/$id/$avatarHash.png?size=$size"//PFP not working...
        else
            "https://cdn.discordapp.com/embed/avatars/${(id.toLongOrNull() ?: 0L) % 5}.png"

    companion object {
        fun fromJson(o: JSONObject) = DiscordUser(
            id            = o.getString("id"),
            username      = o.getString("username"),
            discriminator = o.optString("discriminator", "0"),
            globalName    = o.optString("global_name").takeIf { it.isNotEmpty() },
            avatarHash    = o.optString("avatar").takeIf { it.isNotEmpty() }
        )
    }
}

// ── Guild (Server) ────────────────────────────────────────────────────────────

data class Guild(
    val id: String,
    val name: String,
    val iconHash: String?
) {
    fun iconUrl(size: Int = 64): String =
        if (iconHash != null)
            "https://cdn.discordapp.com/icons/$id/$iconHash.png?size=$size"//lets make the server icon the server icon or banner?
        else
            "https://cdn.discordapp.com/embed/avatars/0.png"

    companion object {
        fun fromJson(o: JSONObject) = Guild(
            id       = o.getString("id"),
            name     = o.getString("name"),
            iconHash = o.optString("icon").takeIf { it.isNotEmpty() }
        )

        fun listFromJson(arr: JSONArray): List<Guild> =
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
    }
}

// ── Channel ───────────────────────────────────────────────────────────────────

enum class ChannelType(val code: Int) {
    GUILD_TEXT(0), DM(1), GUILD_VOICE(2), GROUP_DM(3),
    GUILD_CATEGORY(4), GUILD_NEWS(5), UNKNOWN(-1);

    companion object {
        fun from(code: Int) = entries.firstOrNull { it.code == code } ?: UNKNOWN
    }
}

data class Channel(
    val id: String,
    val type: ChannelType,
    val guildId: String?,
    val name: String,
    val topic: String?,
    val lastMessageId: String?,
    /** For DMs — the other participant(s) */
    val recipients: List<DiscordUser> = emptyList()
) {
    val isDm: Boolean   get() = type == ChannelType.DM || type == ChannelType.GROUP_DM
    val isText: Boolean get() = type == ChannelType.GUILD_TEXT || type == ChannelType.GUILD_NEWS

    /** Human-readable display name (DMs show recipient name) */
    val displayName: String
        get() = if (isDm && name.isEmpty())
            recipients.firstOrNull()?.displayName ?: "Unknown"
        else name

    companion object {
        fun fromJson(o: JSONObject): Channel {
            val recipientsArr = o.optJSONArray("recipients")
            val recipients = if (recipientsArr != null)
                (0 until recipientsArr.length()).map { DiscordUser.fromJson(recipientsArr.getJSONObject(it)) }
            else emptyList()

            return Channel(
                id            = o.getString("id"),
                type          = ChannelType.from(o.getInt("type")),
                guildId       = o.optString("guild_id").takeIf { it.isNotEmpty() },
                name          = o.optString("name"),
                topic         = o.optString("topic").takeIf { it.isNotEmpty() },
                lastMessageId = o.optString("last_message_id").takeIf { it.isNotEmpty() },
                recipients    = recipients
            )
        }

        fun listFromJson(arr: JSONArray): List<Channel> =
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
    }
}

// ── Message ───────────────────────────────────────────────────────────────────

data class DiscordMessage(
    val id: String,
    val channelId: String,
    val author: DiscordUser,
    val content: String,
    val timestamp: String,
    val editedTimestamp: String?
) {
    companion object {
        fun fromJson(o: JSONObject) = DiscordMessage(
            id              = o.getString("id"),
            channelId       = o.getString("channel_id"),
            author          = DiscordUser.fromJson(o.getJSONObject("author")),
            content         = o.getString("content"),
            timestamp       = o.getString("timestamp"),
            editedTimestamp = o.optString("edited_timestamp").takeIf { it.isNotEmpty() }
        )

        fun listFromJson(arr: JSONArray): List<DiscordMessage> =
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
    }
}

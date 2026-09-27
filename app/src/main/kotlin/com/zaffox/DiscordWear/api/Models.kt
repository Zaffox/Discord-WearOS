package com.zaffox.discordwear.api

import org.json.JSONArray
import org.json.JSONObject

data class DiscordClan(
    val tag: String,
    val badge: String?,
    val identityGuildId: String?
) {
    fun badgeUrl(size: Int = 16): String? =
        if (badge != null && identityGuildId != null)
            "https://cdn.discordapp.com/clan-badges/$identityGuildId/$badge.png?size=$size"
        else null

    companion object {
        fun fromJson(o: JSONObject?): DiscordClan? {
            if (o == null) return null
            val tag = o.optString("tag").takeIf { it.isNotEmpty() && it != "null" } ?: return null
            val badge = o.optString("badge").takeIf { it.isNotEmpty() && it != "null" }
            val identityGuildId =
                o.optString("identity_guild_id").takeIf { it.isNotEmpty() && it != "null" }
            return DiscordClan(tag, badge, identityGuildId)
        }
    }
}

data class DiscordUser(
    val id: String,
    val username: String,
    val discriminator: String,
    val globalName: String?,
    val avatarHash: String?,
    val premiumType: Int = 0,
    val nameplateAsset: String? = null,
    val avatarDecorationSkuId: String? = null,
    val bannerHash: String? = null,
    val bio: String? = null,
    val pronouns: String? = null,
    val badges: List<String> = emptyList(),
    val publicFlags: Int = 0,
    val clan: DiscordClan? = null
) {
    val hasNitro: Boolean get() = premiumType > 0
    val displayName: String get() = globalName ?: username
    fun avatarUrl(size: Int = 64): String? =
        if (avatarHash != null)
            "https://cdn.discordapp.com/avatars/$id/$avatarHash.png?size=$size"
        else null

    fun bannerUrl(size: Int = 480): String? =
        if (bannerHash != null)
            "https://cdn.discordapp.com/banners/$id/$bannerHash.png?size=$size"
        else null

    fun nameplateUrl(): String? =
        if (nameplateAsset != null)
            "https://cdn.discordapp.com/media/v1/collectibles-shop/${nameplateAsset}/static"
        else null

    fun avatarDecorationUrl(): String? =
        if (avatarDecorationSkuId != null)
            "https://cdn.discordapp.com/media/v1/collectibles-shop/${avatarDecorationSkuId}/static"
        else null

    companion object {
        private val flagBadgeMap = mapOf(
            0 to "5e7183e244b7d3419965d1d6159f802e",
            1 to "17842d9f1e626bc659b86278ceb15579",
            2 to "bf01d1073934cad147225824c1e40fb9",
            3 to "271cacdad67f8cb290b396906209b552",
            6 to "8a88d63323d471975e54d805001c27ab",
            7 to "011942ebb3dcb2858b9d5c41036f04c6",
            8 to "3aa41de486fa12454bf371c8fcf0a650",
            9 to "642d99dc29fa03e1c6cd79c0944062dc",
            14 to "8f58cc8671f11c8d00e77637a6b7b3b4",
            17 to "6f34351a7b3c209c1b79361a9b2c3427",
            18 to "fee59f481c3c86127bcfb2bf60117d91",
            22 to "6bdc42827a38498926a4b1e8e025ecb1"
        )

        private val badgeIdToAsset = mapOf(
            "hypesquad_bravery" to "8a88d63323d471975e54d805001c27ab",
            "hypesquad_brilliance" to "011942ebb3dcb2858b9d5c41036f04c6",
            "hypesquad_balance" to "3aa41de486fa12454bf371c8fcf0a650",
            "hypesquad" to "8a88d63323d471975e54d805001c27ab",
            "early_supporter" to "642d99dc29fa03e1c6cd79c0944062dc",
            "active_developer" to "6bdc42827a38498926a4b1e8e025ecb1",
            "bug_hunter" to "271cacdad67f8cb290b396906209b552",
            "verified_developer" to "6f34351a7b3c209c1b79361a9b2c3427",
            "nitro" to "2b512c0199d7a224a18018bf03b71f3a",
            "booster" to "5123d9178fa73da8a7b35520e7d58f3c"
        )

        fun fromJson(o: JSONObject, profileJson: JSONObject? = null): DiscordUser {
            val userProfile = profileJson?.optJSONObject("user_profile")
            val pronouns =
                userProfile?.optString("pronouns")?.takeIf { it.isNotEmpty() && it != "null" }
                    ?: o.optString("pronouns").takeIf { it.isNotEmpty() && it != "null" }
            val badgesArr = profileJson?.optJSONArray("badges") ?: o.optJSONArray("badges")
            val badgeUrls = mutableListOf<String>()
            if (badgesArr != null) {
                for (i in 0 until badgesArr.length()) {
                    val item = badgesArr.opt(i)
                    val assetId = if (item is JSONObject) {
                        item.optString("icon").takeIf { it.isNotEmpty() && it != "null" }
                            ?: badgeIdToAsset[item.optString("id")]
                            ?: item.optString("id")
                    } else {
                        val s = item.toString()
                        badgeIdToAsset[s] ?: s
                    }
                    if (!assetId.isNullOrEmpty()) {
                        val url = "https://discordapp.com/assets/$assetId.svg"
                        if (url !in badgeUrls) badgeUrls.add(url)
                    }
                }
            }

            val flags = o.optInt("public_flags", o.optInt("flags", 0))
            flagBadgeMap.forEach { (bit, assetId) ->
                if ((flags and (1 shl bit)) != 0) {
                    val url = "https://discordapp.com/assets/$assetId.svg"
                    if (url !in badgeUrls) badgeUrls.add(url)
                }
            }

            val premiumType = o.optInt("premium_type", 0)
            if (premiumType > 0) {
                val nitroUrl = "https://discordapp.com/assets/2b512c0199d7a224a18018bf03b71f3a.svg"
                if (nitroUrl !in badgeUrls) badgeUrls.add(nitroUrl)
            }

            val clan = DiscordClan.fromJson(
                o.optJSONObject("clan") ?: profileJson?.optJSONObject("clan")
                ?: userProfile?.optJSONObject("clan")
            )

            return DiscordUser(
                id = o.getString("id"),
                username = o.optString("username").takeIf { it.isNotEmpty() && it != "null" }
                    ?: "Unknown",
                discriminator = o.optString("discriminator", "0"),
                globalName = o.optString("global_name").takeIf { it.isNotEmpty() && it != "null" },
                avatarHash = o.optString("avatar").takeIf { it.isNotEmpty() && it != "null" },
                premiumType = premiumType,
                nameplateAsset = o.optJSONObject("collectibles")
                    ?.optJSONObject("nameplate")
                    ?.optString("sku_id")
                    ?.takeIf { it.isNotEmpty() && it != "null" }
                    ?: o.optString("nameplate_asset").takeIf { it.isNotEmpty() && it != "null" },
                avatarDecorationSkuId =
                    o.optJSONObject("collectibles")
                        ?.optJSONObject("avatar_decoration")
                        ?.optString("sku_id")?.takeIf { it.isNotEmpty() && it != "null" }
                        ?: o.optJSONObject("avatar_decoration_data")
                            ?.optString("sku_id")?.takeIf { it.isNotEmpty() && it != "null" },
                bannerHash = o.optString("banner").takeIf { it.isNotEmpty() && it != "null" },
                bio = userProfile?.optString("bio")?.takeIf { it.isNotEmpty() && it != "null" }
                    ?: o.optString("bio").takeIf { it.isNotEmpty() && it != "null" },
                pronouns = pronouns,
                badges = badgeUrls,
                publicFlags = flags,
                clan = clan
            )
        }
    }
}

enum class OnlineStatus { ONLINE, IDLE, DND, INVISIBLE, OFFLINE }


data class ClientStatus(
    val desktop: OnlineStatus? = null,
    val mobile: OnlineStatus? = null,
    val web: OnlineStatus? = null
)

data class UserPresence(
    val userId: String,
    val status: OnlineStatus = OnlineStatus.OFFLINE,
    val clientStatus: ClientStatus = ClientStatus(),
    val customStatusText: String? = null,
    val customStatusEmoji: String? = null
) {
    companion object {
        private fun parseStatus(s: String?) = when (s) {
            "online" -> OnlineStatus.ONLINE
            "idle" -> OnlineStatus.IDLE
            "dnd" -> OnlineStatus.DND
            "invisible" -> OnlineStatus.INVISIBLE
            else -> OnlineStatus.OFFLINE
        }

        fun fromJson(o: JSONObject): UserPresence {
            val userId = o.optJSONObject("user")?.optString("id") ?: return UserPresence("")
            val status = parseStatus(o.optString("status"))
            val cs = o.optJSONObject("client_status")
            val clientStatus = ClientStatus(
                desktop = parseStatus(cs?.optString("desktop")),
                mobile = parseStatus(cs?.optString("mobile")),
                web = parseStatus(cs?.optString("web"))
            )
            val activities = o.optJSONArray("activities")
            var customText: String? = null
            var customEmoji: String? = null
            if (activities != null) {
                for (i in 0 until activities.length()) {
                    val act = activities.getJSONObject(i)
                    if (act.optInt("type") == 4) {
                        customText =
                            act.optString("state").takeIf { it.isNotEmpty() && it != "null" }
                        val emojiObj = act.optJSONObject("emoji")
                        customEmoji = if (emojiObj != null) {
                            val eid =
                                emojiObj.optString("id").takeIf { it.isNotEmpty() && it != "null" }
                            if (eid != null) {
                                val animated = emojiObj.optBoolean("animated", false)
                                val ext = if (animated) "gif" else "webp"
                                "https://cdn.discordapp.com/emojis/$eid.$ext?size=16"
                            } else {
                                emojiObj.optString("name")
                                    .takeIf { it.isNotEmpty() && it != "null" }
                            }
                        } else null
                        break
                    }
                }
            }
            return UserPresence(userId, status, clientStatus, customText, customEmoji)
        }
    }
}


data class Guild(
    val id: String,
    val name: String,
    val iconHash: String?,
    val bannerHash: String? = null
) {
    fun iconUrl(size: Int = 64): String? =
        if (iconHash != null)
            "https://cdn.discordapp.com/icons/$id/$iconHash.png?size=$size"
        else null

    fun bannerUrl(size: Int = 480): String? =
        if (bannerHash != null)
            "https://cdn.discordapp.com/banners/$id/$bannerHash.png?size=$size"
        else null

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("icon", iconHash ?: "")
        .put("banner", bannerHash ?: "")

    companion object {
        fun fromJson(o: JSONObject) = Guild(
            id = o.getString("id"),
            name = o.getString("name"),
            iconHash = o.optString("icon").takeIf { it.isNotEmpty() && it != "null" },
            bannerHash = o.optString("banner").takeIf { it.isNotEmpty() && it != "null" }
        )

        fun listFromJson(arr: JSONArray): List<Guild> =
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
    }
}

enum class ChannelType(val code: Int) {
    GUILD_TEXT(0), DM(1), GUILD_VOICE(2), GROUP_DM(3),
    GUILD_CATEGORY(4), GUILD_NEWS(5), UNKNOWN(-1);

    companion object {
        fun from(code: Int) = entries.firstOrNull { it.code == code } ?: UNKNOWN
    }
}

data class PermissionOverwrite(
    val id: String,
    val type: Int,
    val allow: Long,
    val deny: Long
) {
    companion object {
        fun fromJson(o: JSONObject) = PermissionOverwrite(
            id = o.getString("id"),
            type = o.getInt("type"),
            allow = o.getString("allow").toLongOrNull() ?: 0L,
            deny = o.getString("deny").toLongOrNull() ?: 0L
        )
    }
}

data class GuildMember(
    val userId: String,
    val roleIds: List<String>,
    val avatarHash: String? = null,
    val avatarDecorationSkuId: String? = null
) {
    fun avatarUrl(guildId: String, size: Int = 64): String? =
        if (avatarHash != null)
            "https://cdn.discordapp.com/guilds/$guildId/users/$userId/avatars/$avatarHash.png?size=$size"
        else null

    fun avatarDecorationUrl(): String? =
        if (avatarDecorationSkuId != null)
            "https://cdn.discordapp.com/media/v1/collectibles-shop/${avatarDecorationSkuId}/static"
        else null

    companion object {
        fun fromJson(o: JSONObject): GuildMember {
            val user = o.optJSONObject("user")
            val userId = user?.optString("id") ?: o.optString("user_id", "")
            val rolesArr = o.optJSONArray("roles")
            val roles =
                if (rolesArr != null) (0 until rolesArr.length()).map { rolesArr.getString(it) } else emptyList()
            val avatarHash = o.optString("avatar").takeIf { it.isNotEmpty() && it != "null" }
            val decorSkuId = o.optJSONObject("avatar_decoration_data")?.optString("sku_id")
                ?.takeIf { it.isNotEmpty() && it != "null" }
                ?: o.optJSONObject("avatar_decoration")?.optString("sku_id")
                    ?.takeIf { it.isNotEmpty() && it != "null" }
            return GuildMember(
                userId = userId,
                roleIds = roles,
                avatarHash = avatarHash,
                avatarDecorationSkuId = decorSkuId
            )
        }
    }
}

data class GuildRole(
    val id: String,
    val name: String,
    val color: Int,
    val position: Int,
    val permissions: Long,
    val unicodeEmoji: String? = null,
    val iconHash: String? = null
) {
    fun iconUrl(size: Int = 16): String? =
        iconHash?.let { "https://cdn.discordapp.com/role-icons/$id/$it.png?size=$size" }

    companion object {
        fun fromJson(o: JSONObject) = GuildRole(
            id = o.getString("id"),
            name = o.optString("name", ""),
            color = o.optInt("color", 0),
            position = o.optInt("position", 0),
            permissions = o.getString("permissions").toLongOrNull() ?: 0L,
            unicodeEmoji = o.optString("unicode_emoji").takeIf { it.isNotEmpty() && it != "null" },
            iconHash = o.optString("icon").takeIf { it.isNotEmpty() && it != "null" }
        )

        fun listFromJson(arr: JSONArray): List<GuildRole> =
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
    }
}

object Permissions {
    const val ADMINISTRATOR = 1L shl 3
    const val VIEW_CHANNEL = 1L shl 10
    const val SEND_MESSAGES = 1L shl 11

    fun effectiveForMember(
        member: GuildMember,
        guildId: String,
        everyoneRoleId: String,
        roles: List<GuildRole>,
        channel: Channel
    ): Long {
        val roleMap = roles.associateBy { it.id }
        var perms = roleMap[everyoneRoleId]?.permissions ?: 0L
        for (roleId in member.roleIds) perms = perms or (roleMap[roleId]?.permissions ?: 0L)
        if (perms and ADMINISTRATOR != 0L) return Long.MAX_VALUE

        val overwrites = channel.permissionOverwrites
        overwrites.firstOrNull { it.id == everyoneRoleId }?.let {
            perms = (perms and it.deny.inv()) or it.allow
        }

        var roleDeny = 0L
        var roleAllow = 0L
        for (ow in overwrites.filter { it.type == 0 && it.id in member.roleIds }) {
            roleDeny = roleDeny or ow.deny
            roleAllow = roleAllow or ow.allow
        }
        perms = (perms and roleDeny.inv()) or roleAllow

        overwrites.firstOrNull { it.type == 1 && it.id == member.userId }?.let {
            perms = (perms and it.deny.inv()) or it.allow
        }
        return perms
    }

    fun has(perms: Long, flag: Long) = perms and flag != 0L
}

data class Channel(
    val id: String,
    val type: ChannelType,
    val guildId: String?,
    val name: String,
    val topic: String?,
    val lastMessageId: String?,
    val parentId: String?,
    val position: Int,
    val permissionOverwrites: List<PermissionOverwrite> = emptyList(),
    val recipients: List<DiscordUser> = emptyList(),
    val hasAccess: Boolean = true,
    val slowModeSeconds: Int = 0
) {
    val isDm: Boolean get() = type == ChannelType.DM || type == ChannelType.GROUP_DM
    val isText: Boolean get() = type == ChannelType.GUILD_TEXT || type == ChannelType.GUILD_NEWS
    val isCategory: Boolean get() = type == ChannelType.GUILD_CATEGORY

    val displayName: String
        get() = if (isDm && name.isEmpty())
            recipients.firstOrNull()?.displayName ?: "Unknown"
        else name

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("type", type.code)
        .put("guild_id", guildId ?: "")
        .put("name", name)
        .put("topic", topic ?: "")
        .put("last_message_id", lastMessageId ?: "")
        .put("parent_id", parentId ?: "")
        .put("position", position)
        .put("has_access", hasAccess)
        .put("rate_limit_per_user", slowModeSeconds)
        .put("permission_overwrites", JSONArray(permissionOverwrites.map { ow ->
            JSONObject()
                .put("id", ow.id)
                .put("type", ow.type)
                .put("allow", ow.allow.toString())
                .put("deny", ow.deny.toString())
        }))
        .put("recipients", JSONArray(recipients.map { u ->
            JSONObject().put("id", u.id).put("username", u.username)
                .put("global_name", u.globalName ?: "").put("avatar", u.avatarHash ?: "")
                .put("nameplate_asset", u.nameplateAsset ?: "")
        }))

    companion object {
        fun fromJson(o: JSONObject): Channel {
            val recipientsArr = o.optJSONArray("recipients")
            val recipients = if (recipientsArr != null)
                (0 until recipientsArr.length()).map {
                    DiscordUser.fromJson(
                        recipientsArr.getJSONObject(
                            it
                        )
                    )
                }
            else emptyList()

            val owArr = o.optJSONArray("permission_overwrites")
            val overwrites = if (owArr != null)
                (0 until owArr.length()).map { PermissionOverwrite.fromJson(owArr.getJSONObject(it)) }
            else emptyList()

            return Channel(
                id = o.getString("id"),
                type = ChannelType.from(o.getInt("type")),
                guildId = o.optString("guild_id").takeIf { it.isNotEmpty() },
                name = o.optString("name"),
                topic = o.optString("topic").takeIf { it.isNotEmpty() },
                lastMessageId = o.optString("last_message_id").takeIf { it.isNotEmpty() },
                // org.json's optString() returns the literal string "null" (not Kotlin null)
                // when the JSON value is a JSON null, which is exactly what Discord sends for
                // top-level/"uncategorized" channels. That made those channels group under a
                // bogus "null" parent key instead of the real null key, so they never rendered.
                parentId = if (o.isNull("parent_id")) null
                else o.optString("parent_id").takeIf { it.isNotEmpty() && it != "null" },
                position = o.optInt("position", 0),
                permissionOverwrites = overwrites,
                recipients = recipients,
                slowModeSeconds = o.optInt("rate_limit_per_user", 0),
                hasAccess = o.optBoolean("has_access", true)
            )
        }

        fun listFromJson(arr: JSONArray): List<Channel> =
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
    }
}

data class CategoryGroup(
    val category: Channel?,
    val channels: List<Channel>
)

data class Attachment(
    val id: String,
    val filename: String,
    val url: String,
    val proxyUrl: String,
    val contentType: String?,
    val width: Int?,
    val height: Int?
) {
    val isImage: Boolean
        get() = contentType?.startsWith("image/") == true
                || filename.lowercase().let {
            it.endsWith(".png") || it.endsWith(".jpg")
                    || it.endsWith(".jpeg") || it.endsWith(".gif") || it.endsWith(".webp")
        }

    val isVideo: Boolean
        get() = contentType?.startsWith("video/") == true
                || filename.lowercase().let {
            it.endsWith(".mp4") || it.endsWith(".mov")
                    || it.endsWith(".webm") || it.endsWith(".mkv") || it.endsWith(".avi")
        }

    val isAudio: Boolean
        get() = contentType?.startsWith("audio/") == true
                || filename.lowercase().let {
            it.endsWith(".mp3") || it.endsWith(".ogg")
                    || it.endsWith(".wav") || it.endsWith(".flac") || it.endsWith(".m4a")
        }

    companion object {
        fun fromJson(o: JSONObject) = Attachment(
            id = o.getString("id"),
            filename = o.getString("filename"),
            url = o.getString("url"),
            proxyUrl = o.getString("proxy_url"),
            contentType = o.optString("content_type").takeIf { it.isNotEmpty() },
            width = if (o.has("width")) o.getInt("width") else null,
            height = if (o.has("height")) o.getInt("height") else null
        )
    }
}

data class Embed(
    val type: String,
    val title: String?,
    val description: String?,
    val url: String?,
    val color: Int?,
    val imageUrl: String?,
    val thumbnailUrl: String?,
    val videoUrl: String?,
    val authorName: String?,
    val authorIconUrl: String?,
    val footerText: String?,
    val footerIconUrl: String?,
    val fields: List<EmbedField> = emptyList()
) {
    val displayImageUrl: String? get() = imageUrl ?: thumbnailUrl

    companion object {
        fun fromJson(o: JSONObject) = Embed(
            type = o.optString("type", "rich"),
            title = o.optString("title").takeIf { it.isNotEmpty() },
            description = o.optString("description").takeIf { it.isNotEmpty() },
            url = o.optString("url").takeIf { it.isNotEmpty() },
            color = if (o.has("color")) o.getInt("color") else null,
            imageUrl = o.optJSONObject("image")?.optString("url")?.takeIf { it.isNotEmpty() },
            thumbnailUrl = o.optJSONObject("thumbnail")?.optString("url")
                ?.takeIf { it.isNotEmpty() },
            videoUrl = o.optJSONObject("video")?.optString("url")?.takeIf { it.isNotEmpty() },
            authorName = o.optJSONObject("author")?.optString("name")?.takeIf { it.isNotEmpty() },
            authorIconUrl = o.optJSONObject("author")?.optString("icon_url")
                ?.takeIf { it.isNotEmpty() },
            footerText = o.optJSONObject("footer")?.optString("text")?.takeIf { it.isNotEmpty() },
            footerIconUrl = o.optJSONObject("footer")?.optString("icon_url")
                ?.takeIf { it.isNotEmpty() },
            fields = run {
                val arr = o.optJSONArray("fields")
                if (arr != null) {
                    (0 until arr.length()).mapNotNull { i ->
                        val f = arr.optJSONObject(i) ?: return@mapNotNull null
                        val name =
                            f.optString("name").takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                        val value = f.optString("value").takeIf { it.isNotEmpty() }
                            ?: return@mapNotNull null
                        EmbedField(name, value, f.optBoolean("inline", false))
                    }
                } else emptyList()
            }
        )
    }
}

data class EmbedField(
    val name: String,
    val value: String,
    val inline: Boolean
)

data class StickerItem(
    val id: String,
    val name: String,
    val formatType: Int
) {
    val isDisplayable: Boolean get() = formatType in listOf(1, 2, 3)
    val imageUrl: String
        get() {
            val ext = when (formatType) {
                2 -> "gif"
                3 -> "json"
                else -> "png"
            }
            return "https://media.discordapp.net/stickers/$id.$ext?size=240"
        }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("format_type", formatType)

    companion object {
        fun fromJson(o: JSONObject) = StickerItem(
            id = o.getString("id"),
            name = o.getString("name"),
            formatType = o.optInt("format_type", 1)
        )
    }
}

data class ChannelUnreadState(
    val lastMessageId: String,
    val mentionCount: Int = 0
)

object ContentParser {

    sealed class Part {
        data class PlainText(val text: String) : Part()
        data class CustomEmoji(val name: String, val url: String, val animated: Boolean) : Part()
        data class UserMention(val userId: String, val displayName: String) : Part()
        data class RoleMention(val roleId: String, val roleName: String) : Part()
        data class ChannelMention(val channelId: String, val channelName: String) : Part()
        data class Link(val url: String) : Part()
    }

    data class MarkdownSpan(
        val text: String,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val strikethrough: Boolean = false,
        val code: Boolean = false,
        val spoiler: Boolean = false,
        val subtext: Boolean = false,
        val headingLevel: Int = 0
    )

    fun parseMarkdown(
        text: String,
        subtext: Boolean = false,
        headingLevel: Int = 0
    ): List<MarkdownSpan> {
        if (text.isBlank()) return listOf(
            MarkdownSpan(
                text,
                subtext = subtext,
                headingLevel = headingLevel
            )
        )
        val spans = mutableListOf<MarkdownSpan>()
        val regex = Regex(
            """(\*\*\*(.+?)\*\*\*)""" +
                    """|(\*\*(.+?)\*\*)""" +
                    """|(\*(.+?)\*)""" +
                    """|(~~(.+?)~~)""" +
                    """|(```.+?```)|(`(.+?)`)""" +
                    """|(\|\|(.+?)\|\|)""" +
                    """|(_{2}(.+?)_{2})""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )
        var cursor = 0
        for (match in regex.findAll(text)) {
            if (match.range.first > cursor) {
                spans += MarkdownSpan(
                    text.substring(cursor, match.range.first),
                    subtext = subtext,
                    headingLevel = headingLevel
                )
            }
            val raw = match.value
            when {
                raw.startsWith("***") -> spans += MarkdownSpan(
                    match.groupValues[2],
                    bold = true,
                    italic = true,
                    subtext = subtext,
                    headingLevel = headingLevel
                )

                raw.startsWith("**") -> spans += MarkdownSpan(
                    match.groupValues[4],
                    bold = true,
                    subtext = subtext,
                    headingLevel = headingLevel
                )

                raw.startsWith("*") -> spans += MarkdownSpan(
                    match.groupValues[6],
                    italic = true,
                    subtext = subtext,
                    headingLevel = headingLevel
                )

                raw.startsWith("~~") -> spans += MarkdownSpan(
                    match.groupValues[8],
                    strikethrough = true,
                    subtext = subtext,
                    headingLevel = headingLevel
                )

                raw.startsWith("```") -> spans += MarkdownSpan(
                    raw.removeSurrounding("```"),
                    code = true,
                    subtext = subtext,
                    headingLevel = headingLevel
                )

                raw.startsWith("`") -> spans += MarkdownSpan(
                    match.groupValues[11],
                    code = true,
                    subtext = subtext,
                    headingLevel = headingLevel
                )

                raw.startsWith("||") -> spans += MarkdownSpan(
                    match.groupValues[13],
                    spoiler = true,
                    subtext = subtext,
                    headingLevel = headingLevel
                )

                raw.startsWith("__") -> spans += MarkdownSpan(
                    match.groupValues[15],
                    bold = true,
                    subtext = subtext,
                    headingLevel = headingLevel
                )

                else -> spans += MarkdownSpan(raw, subtext = subtext, headingLevel = headingLevel)
            }
            cursor = match.range.last + 1
        }
        if (cursor < text.length) spans += MarkdownSpan(
            text.substring(cursor),
            subtext = subtext,
            headingLevel = headingLevel
        )
        return spans.filter { it.text.isNotEmpty() }
    }

    private val TOKEN_RE = Regex(
        "<a?:\\w+:\\d+>" +
                "|<@!?\\d+>" +
                "|<@&\\d+>" +
                "|<#\\d+>" +
                "|https?://[^\\s>]+"
    )

    fun parse(
        content: String,
        userNames: Map<String, String> = emptyMap(),
        roleNames: Map<String, String> = emptyMap(),
        channelNames: Map<String, String> = emptyMap()
    ): List<Part> {
        if (content.isBlank()) return emptyList()
        val parts = mutableListOf<Part>()
        var last = 0

        for (match in TOKEN_RE.findAll(content)) {
            if (match.range.first > last)
                parts += Part.PlainText(content.substring(last, match.range.first))
            val token = match.value
            when {
                token.startsWith("<:") || token.startsWith("<a:") -> {
                    val animated = token.startsWith("<a:")
                    val inner = token.removeSurrounding("<", ">").trimStart('a', ':').trimStart(':')
                    val segments = inner.split(":")
                    if (segments.size == 2) {
                        val name = segments[0];
                        val id = segments[1]
                        val ext = if (animated) "gif" else "webp"
                        parts += Part.CustomEmoji(
                            name,
                            "https://cdn.discordapp.com/emojis/$id.$ext?size=64",
                            animated
                        )
                    } else parts += Part.PlainText(token)
                }

                token.startsWith("<@&") -> {
                    val id = token.removeSurrounding("<@&", ">")
                    parts += Part.RoleMention(id, roleNames[id] ?: "@deleted-role")
                }

                token.startsWith("<@") -> {
                    val id = token.removePrefix("<@!").removePrefix("<@").removeSuffix(">")
                    parts += Part.UserMention(id, userNames[id] ?: "@unknown")
                }

                token.startsWith("<#") -> {
                    val id = token.removeSurrounding("<#", ">")
                    parts += Part.ChannelMention(id, channelNames[id] ?: "#unknown")
                }

                token.startsWith("http") -> parts += Part.Link(token)
                else -> parts += Part.PlainText(token)
            }
            last = match.range.last + 1
        }
        if (last < content.length) parts += Part.PlainText(content.substring(last))
        return parts.filter { it !is Part.PlainText || (it as Part.PlainText).text.isNotEmpty() }
    }
}

data class Reaction(
    val emoji: ReactionEmoji,
    val count: Int,
    val me: Boolean,
    val meBurst: Boolean = false,
    val burstCount: Int = 0
)

data class ReactionEmoji(
    val id: String?,
    val name: String,
    val animated: Boolean
) {
    val apiKey: String get() = if (id != null) "$name:$id" else name
    val imageUrl: String?
        get() = if (id != null) {
            val ext = if (animated) "gif" else "webp"
            "https://cdn.discordapp.com/emojis/$id.$ext?size=16"
        } else null

    companion object {
        fun fromJson(o: JSONObject) = ReactionEmoji(
            id = o.optString("id").takeIf { it.isNotEmpty() && it != "null" },
            name = o.optString("name").ifEmpty { "?" },
            animated = o.optBoolean("animated", false)
        )
    }
}

data class GuildEmoji(
    val id: String,
    val name: String,
    val animated: Boolean
) {
    val imageUrl: String
        get() {
            val ext = if (animated) "gif" else "webp"
            return "https://cdn.discordapp.com/emojis/$id.$ext?size=32"
        }
    val insertText: String get() = if (animated) "<a:$name:$id>" else "<:$name:$id>"

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("animated", animated)

    companion object {
        fun fromJson(o: JSONObject) = GuildEmoji(
            id = o.getString("id"),
            name = o.getString("name"),
            animated = o.optBoolean("animated", false)
        )

        fun listFromJson(arr: JSONArray): List<GuildEmoji> =
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
    }
}

data class DiscordMessage(
    val id: String,
    val channelId: String,
    val author: DiscordUser,
    val content: String,
    val timestamp: String,
    val editedTimestamp: String?,
    val attachments: List<Attachment> = emptyList(),
    val embeds: List<Embed> = emptyList(),
    val stickers: List<StickerItem> = emptyList(),
    val reactions: List<Reaction> = emptyList(),
    val mentionedUsers: List<DiscordUser> = emptyList(),
    val mentionedUserIds: List<String> = emptyList(),
    val mentionedRoleIds: List<String> = emptyList(),
    val mentionEveryone: Boolean = false,
    val guildId: String? = null,
    val type: Int = 0,
    val referencedMessage: DiscordMessage? = null,
    val forwardedContent: String? = null,
    val forwardedAuthor: DiscordUser? = null,
    val forwardedAttachments: List<Attachment> = emptyList(),
    val forwardedEmbeds: List<Embed> = emptyList(),
    val forwardedStickers: List<StickerItem> = emptyList(),
    /** Non-null if this message started a thread. */
    val threadId: String? = null,
    val threadMessageCount: Int = 0
) {
    fun pingFor(userId: String, memberRoleIds: List<String> = emptyList()): Boolean =
        mentionEveryone ||
                userId in mentionedUserIds ||
                mentionedRoleIds.any { it in memberRoleIds }

    companion object {
        fun fromJson(o: JSONObject): DiscordMessage {
            val mentionsArr = o.optJSONArray("mentions")
            val mentionedUsers = if (mentionsArr != null)
                (0 until mentionsArr.length()).map {
                    DiscordUser.fromJson(
                        mentionsArr.getJSONObject(
                            it
                        )
                    )
                }
            else emptyList()
            val mentionedUserIds = mentionedUsers.map { it.id }

            val roleArr = o.optJSONArray("mention_roles")
            val mentionedRoles = if (roleArr != null)
                (0 until roleArr.length()).map { roleArr.getString(it) }
            else emptyList()

            val attachArr = o.optJSONArray("attachments")
            val attachments = if (attachArr != null)
                (0 until attachArr.length()).map { Attachment.fromJson(attachArr.getJSONObject(it)) }
            else emptyList()

            val embedArr = o.optJSONArray("embeds")
            val embeds = if (embedArr != null)
                (0 until embedArr.length()).map { Embed.fromJson(embedArr.getJSONObject(it)) }
            else emptyList()

            val stickerArr = o.optJSONArray("sticker_items")
            val stickers = if (stickerArr != null)
                (0 until stickerArr.length()).map { StickerItem.fromJson(stickerArr.getJSONObject(it)) }
            else emptyList()

            val reactArr = o.optJSONArray("reactions")
            val reactions = if (reactArr != null)
                (0 until reactArr.length()).mapNotNull { i ->
                    runCatching {
                        val r = reactArr.getJSONObject(i)
                        val countDetails = r.optJSONObject("count_details")
                        val normalCount = countDetails?.optInt("normal", r.optInt("count", 0))
                            ?: r.optInt("count", 0)
                        val burstCount = countDetails?.optInt("burst", r.optInt("burst_count", 0))
                            ?: r.optInt("burst_count", 0)
                        val totalCount = normalCount + burstCount
                        val me = r.optBoolean("me", false)
                        val meBurst = r.optBoolean("me_burst", false)
                        Reaction(
                            emoji = ReactionEmoji.fromJson(r.getJSONObject("emoji")),
                            count = if (totalCount > 0) totalCount else r.optInt("count", 1),
                            me = me,
                            meBurst = meBurst,
                            burstCount = burstCount
                        )
                    }.getOrNull()
                }
            else emptyList()

            val refMsgObj = o.optJSONObject("referenced_message")
            val refMsg =
                if (refMsgObj != null) runCatching { fromJson(refMsgObj) }.getOrNull() else null

            val msgType = o.optInt("type", 0)
            var fwdContent: String? = null
            var fwdAuthor: DiscordUser? = null
            var fwdAttachments: List<Attachment> = emptyList()
            var fwdEmbeds: List<Embed> = emptyList()
            var fwdStickers: List<StickerItem> = emptyList()

            val snapshots = o.optJSONArray("message_snapshots")
            if (snapshots != null) {
                for (i in 0 until snapshots.length()) {
                    val snapMsg = snapshots.getJSONObject(i)
                        .optJSONObject("message") ?: continue

                    fwdContent = snapMsg.optString("content").takeIf { it.isNotEmpty() }
                    fwdAuthor = snapMsg.optJSONObject("author")?.let { DiscordUser.fromJson(it) }

                    val fwdAttachArr = snapMsg.optJSONArray("attachments")
                    if (fwdAttachArr != null) {
                        fwdAttachments = (0 until fwdAttachArr.length())
                            .map { Attachment.fromJson(fwdAttachArr.getJSONObject(it)) }
                    }

                    val fwdEmbedArr = snapMsg.optJSONArray("embeds")
                    if (fwdEmbedArr != null) {
                        fwdEmbeds = (0 until fwdEmbedArr.length())
                            .map { Embed.fromJson(fwdEmbedArr.getJSONObject(it)) }
                    }

                    val fwdStickerArr = snapMsg.optJSONArray("sticker_items")
                    if (fwdStickerArr != null) {
                        fwdStickers = (0 until fwdStickerArr.length())
                            .map { StickerItem.fromJson(fwdStickerArr.getJSONObject(it)) }
                    }

                    if (fwdContent != null || fwdAuthor != null ||
                        fwdAttachments.isNotEmpty() || fwdEmbeds.isNotEmpty() || fwdStickers.isNotEmpty()
                    ) break
                }
            }

            return DiscordMessage(
                id = o.getString("id"),
                channelId = o.getString("channel_id"),
                author = DiscordUser.fromJson(o.getJSONObject("author")),
                content = o.optString("content", ""),
                timestamp = o.getString("timestamp"),
                editedTimestamp = o.optString("edited_timestamp").takeIf { it.isNotEmpty() },
                attachments = attachments,
                embeds = embeds,
                stickers = stickers,
                reactions = reactions,
                mentionedUsers = mentionedUsers,
                mentionedUserIds = mentionedUserIds,
                mentionedRoleIds = mentionedRoles,
                mentionEveryone = o.optBoolean("mention_everyone", false),
                guildId = o.optString("guild_id").takeIf { it.isNotEmpty() },
                type = msgType,
                referencedMessage = refMsg,
                forwardedContent = fwdContent,
                forwardedAuthor = fwdAuthor,
                forwardedAttachments = fwdAttachments,
                forwardedEmbeds = fwdEmbeds,
                forwardedStickers = fwdStickers,
                threadId = o.optJSONObject("thread")?.optString("id")
                    ?.takeIf { it.isNotEmpty() && it != "null" },
                threadMessageCount = o.optJSONObject("thread")?.optInt("message_count", 0) ?: 0
            )
        }

        fun listFromJson(arr: JSONArray): List<DiscordMessage> =
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
    }
}

data class Ping(
    val message: DiscordMessage,
    val channelName: String,
    val guildName: String?
)

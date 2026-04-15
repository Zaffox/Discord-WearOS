package com.zaffox.discordwear.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thin wrapper around the Discord REST API (v10).
 *
 * All functions are suspend and run on [Dispatchers.IO].
 * They return [Result] so the call-site decides how to handle errors.
 *
 * Usage:
 *   val client = DiscordRestClient(token)
 *   val user   = client.getCurrentUser().getOrThrow()
 */
class DiscordRestClient(private val token: String) {

    // ── HTTP client ───────────────────────────────────────────────────────────

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://discord.com/api/v10"
    private val jsonMime = "application/json; charset=utf-8".toMediaType()

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun buildRequest(path: String): Request.Builder =
        Request.Builder()
            .url("$baseUrl$path")
            .header("Authorization", token)           // token already has "Bot " prefix if bot - Check for bot maybe?
            .header("User-Agent", "DiscordWear/1.0 (WearOS)")//Add device info here, EX: Galaxy Watch7 40MM - help ID device

    /** Execute a request and return the body as a string, or throw on HTTP error. */
    private suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        val response = http.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (!response.isSuccessful) {
            val msg = runCatching { JSONObject(body).optString("message", body) }.getOrDefault(body)
            throw DiscordApiException(response.code, msg)
        }
        body
    }

    private suspend fun get(path: String): String =
        execute(buildRequest(path).get().build())

    private suspend fun post(path: String, body: JSONObject): String =
        execute(buildRequest(path)
            .post(body.toString().toRequestBody(jsonMime))
            .build())

    // ── Public API ────────────────────────────────────────────────────────────

    /** GET /users/@me */
    suspend fun getCurrentUser(): Result<DiscordUser> = runCatching {
        DiscordUser.fromJson(JSONObject(get("/users/@me")))
    }

    /** GET /users/@me/guilds — returns up to 200 guilds */
    suspend fun getGuilds(): Result<List<Guild>> = runCatching {
        Guild.listFromJson(JSONArray(get("/users/@me/guilds?limit=200")))
    }

    /** GET /guilds/{guildId}/channels */
    suspend fun getGuildChannels(guildId: String): Result<List<Channel>> = runCatching {
        Channel.listFromJson(JSONArray(get("/guilds/$guildId/channels")))
            .filter { it.isText }
            .sortedBy { it.name }
    }

    /** GET /users/@me/channels — DM channels */
    suspend fun getDmChannels(): Result<List<Channel>> = runCatching {
        Channel.listFromJson(JSONArray(get("/users/@me/channels")))
            .filter { it.isDm }
    }

    /**
     * GET /channels/{channelId}/messages
     * Returns up to [limit] messages (max 100), newest first.
     * The UI should reverse the list to show oldest-at-top.
     */
     //lets try to get the unread messages and start from there unless mor than ~20, but add quick scroll down- not here, in UI screen kotlin
    suspend fun getMessages(channelId: String, limit: Int = 50): Result<List<DiscordMessage>> = runCatching {
        val safe = limit.coerceIn(1, 100)
        DiscordMessage.listFromJson(JSONArray(get("/channels/$channelId/messages?limit=$safe")))
    }

    /**
     * POST /channels/{channelId}/messages — send a text message.
     * Returns the created [DiscordMessage].
     */
    suspend fun sendMessage(channelId: String, content: String): Result<DiscordMessage> = runCatching {
        val body = JSONObject().put("content", content)
        DiscordMessage.fromJson(JSONObject(post("/channels/$channelId/messages", body)))
    }

    /**
     * POST /channels/{channelId}/typing — sends the typing indicator.
     * Fire-and-forget: ignore errors silently.
     */
    suspend fun sendTyping(channelId: String) {
        runCatching {
            execute(buildRequest("/channels/$channelId/typing")
                .post("".toRequestBody(jsonMime))
                .build())
        }
    }
}

// ── Exception ─────────────────────────────────────────────────────────────────

class DiscordApiException(val httpCode: Int, message: String) :
    IOException("Discord API error $httpCode: $message")

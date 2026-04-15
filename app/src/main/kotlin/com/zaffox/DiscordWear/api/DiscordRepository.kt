package com.zaffox.discordwear.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * DiscordRepository is the single source of truth for the app.
 *
 * It owns:
 *  - [DiscordRestClient] for REST calls
 *  - [DiscordGateway]    for real-time events
 *  - [StateFlow]s that the ViewModels/screens observe
 *
 * Lifecycle: create once (e.g. in Application or a singleton), call [connect]
 * after a token is available, [disconnect] on logout.
 */
class DiscordRepository(token: String) {

    private val scope  = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val rest           = DiscordRestClient(token)
    private val gateway = DiscordGateway(token)

    // ── Exposed state ─────────────────────────────────────────────────────────

    private val _currentUser = MutableStateFlow<DiscordUser?>(null)
    val currentUser: StateFlow<DiscordUser?> = _currentUser.asStateFlow()

    private val _guilds = MutableStateFlow<List<Guild>>(emptyList())
    val guilds: StateFlow<List<Guild>> = _guilds.asStateFlow()

    private val _dmChannels = MutableStateFlow<List<Channel>>(emptyList())
    val dmChannels: StateFlow<List<Channel>> = _dmChannels.asStateFlow()

    /** channelId → messages (newest-last order) */
    private val _messages = MutableStateFlow<Map<String, List<DiscordMessage>>>(emptyMap())
    val messages: StateFlow<Map<String, List<DiscordMessage>>> = _messages.asStateFlow()

    /** channelId → set of userIds currently typing */
    private val _typing = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val typing: StateFlow<Map<String, Set<String>>> = _typing.asStateFlow()

    val gatewayEvents = gateway.events

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun connect() {
        gateway.connect()
        observeGatewayEvents()
        scope.launch { refreshCurrentUser() }
        scope.launch { refreshGuilds() }
        scope.launch { refreshDmChannels() }
    }

    fun disconnect() {
        gateway.disconnect()
    }

    // ── Refresh helpers ───────────────────────────────────────────────────────

    suspend fun refreshCurrentUser() {
        rest.getCurrentUser().onSuccess { _currentUser.value = it } // if 401 then check token
    }

    suspend fun refreshGuilds() {
        rest.getGuilds().onSuccess { _guilds.value = it }
    }

    suspend fun refreshDmChannels() {
        rest.getDmChannels().onSuccess { _dmChannels.value = it }
    }

    /** Fetch messages for a channel and cache them. */
    suspend fun loadMessages(channelId: String) {//if 401, probably staff only channel, ill keep that, but probably hidden in a dev menu lol
        rest.getMessages(channelId).onSuccess { newMsgs ->
            _messages.update { current ->
                current + (channelId to newMsgs.reversed()) // reversed = oldest first
            }
        }
    }

    /** Send a message, optimistically append it, then reconcile from response. */
    suspend fun sendMessage(channelId: String, content: String): Result<DiscordMessage> {
        val result = rest.sendMessage(channelId, content)
        result.onSuccess { msg ->
            _messages.update { current ->
                val list = current[channelId].orEmpty().toMutableList()
                // avoid duplicate if Gateway already pushed it
                if (list.none { it.id == msg.id }) list.add(msg)
                current + (channelId to list)
            }
        }
        return result
    }

    // ── Gateway event handling ────────────────────────────────────────────────

    private fun observeGatewayEvents() {
        scope.launch {
            gateway.events.collect { event ->
                when (event) {
                    is GatewayEvent.Ready -> {
                        _currentUser.value = event.user
                    }

                    is GatewayEvent.MessageCreate -> {
                        val msg = event.message
                        _messages.update { current ->
                            val list = current[msg.channelId].orEmpty().toMutableList()
                            if (list.none { it.id == msg.id }) list.add(msg)
                            current + (msg.channelId to list)
                        }
                        // Clear typing indicator for that user
                        _typing.update { current ->
                            val users = current[msg.channelId].orEmpty() - msg.author.id
                            if (users.isEmpty()) current - msg.channelId
                            else current + (msg.channelId to users)
                        }
                    }

                    is GatewayEvent.MessageUpdate -> {
                        val updated = event.message
                        _messages.update { current ->
                            val list = current[updated.channelId]?.map {
                                if (it.id == updated.id) updated else it
                            } ?: return@update current
                            current + (updated.channelId to list)
                        }
                    }

                    is GatewayEvent.MessageDelete -> {
                        _messages.update { current ->
                            val list = current[event.channelId]?.filter { it.id != event.id }
                                ?: return@update current
                            current + (event.channelId to list)
                        }
                    }

                    is GatewayEvent.TypingStart -> {
                        _typing.update { current ->
                            val users = (current[event.channelId] ?: emptySet()) + event.userId
                            current + (event.channelId to users)
                        }
                    }

                    is GatewayEvent.Unknown -> { /* ignore unknown events */ }
                }
            }
        }
    }
}

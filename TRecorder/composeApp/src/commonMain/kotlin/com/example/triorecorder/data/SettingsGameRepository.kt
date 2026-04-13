package com.example.triorecorder.data

import com.example.triorecorder.model.GameHistory
import com.russhwolf.settings.Settings
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class SettingsGameRepository(
    private val settings: Settings,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : GameRepository {

    private val key = "games_history"
    private val serializer = ListSerializer(GameHistory.serializer())

    override suspend fun getGames(): List<GameHistory> {
        val raw = settings.getStringOrNull(key) ?: return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }
            .getOrDefault(emptyList())
            .reversed()
    }

    override suspend fun addGame(game: GameHistory) {
        val current = getGames().reversed().toMutableList()
        current.add(game)
        settings.putString(key, json.encodeToString(serializer, current))
    }

    override suspend fun deleteGame(index: Int) {
        val current = getGames().toMutableList()
        if (index !in current.indices) return
        current.removeAt(index)
        settings.putString(key, json.encodeToString(serializer, current.reversed()))
    }
}

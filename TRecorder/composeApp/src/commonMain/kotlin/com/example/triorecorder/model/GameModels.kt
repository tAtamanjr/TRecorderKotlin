package com.example.triorecorder.model

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class Player(
    val id: String = randomId(),
    val name: String,
    val dices: Int,
    val score: List<Int> = emptyList(),
    val skip: Boolean = false,
)

@Serializable
data class PlayerHistory(
    val id: String = randomId(),
    val name: String,
    val score: List<Int>,
)

@Serializable
data class GameHistory(
    val id: String = randomId(),
    val date: String,
    val players: List<PlayerHistory>,
) {
    companion object {
        fun fromPlayers(players: List<Player>): GameHistory {
            return GameHistory(
                date = Clock.System.now().toString(),
                players = players
                    .map { PlayerHistory(name = it.name, score = it.score) }
                    .sortedByDescending { it.score.sum() }
            )
        }
    }
}

@Serializable
data class GameData(
    val players: List<Player>,
    val pool: Int,
    val playerToMove: Int = 0,
) {
    val name: String get() = players[playerToMove].name
    val dices: Int get() = players[playerToMove].dices
    val score: List<Int> get() = players[playerToMove].score
    val skip: Boolean get() = players.all { it.skip }

    companion object {
        fun create(names: List<String>): GameData {
            val playerCount = names.size
            val startPool = if (playerCount == 2) 38 else (56 - 7 * playerCount)
            val startDices = if (playerCount == 2) 9 else 7
            return GameData(
                players = names.map { Player(name = it, dices = startDices) },
                pool = startPool,
            )
        }
    }

    fun nextPlayer(): GameData = copy(playerToMove = (playerToMove + 1) % players.size)

    fun updateCurrentPlayer(transform: (Player) -> Player): GameData {
        val updated = players.toMutableList()
        updated[playerToMove] = transform(updated[playerToMove])
        return copy(players = updated)
    }

    fun withCurrentPlayerScoreChange(scoreChange: List<Int>): GameData {
        return updateCurrentPlayer { player -> player.copy(score = player.score + scoreChange) }
    }

    fun withCurrentPlayerDiceChange(delta: Int): GameData {
        return updateCurrentPlayer { player -> player.copy(dices = player.dices + delta) }
    }

    fun withCurrentPlayerSkip(skip: Boolean): GameData {
        return updateCurrentPlayer { player -> player.copy(skip = skip) }
    }
}

@Serializable
data class PlayerMove(
    val penalty: List<Int> = emptyList(),
    val score: Int = -1,
    val bonus: Int = -1,
    val skip: Boolean = false,
) {
    val diceChange: Int get() = if (score == -1) penalty.size else (-1 + penalty.size)
    val poolChange: Int get() = -penalty.size
    val endTurn: Boolean get() = score == -1 && !skip && penalty.size < 3

    fun getScoreChange(pool: Int): List<Int> {
        val scorePart = if (score != -1) {
            listOf(score)
        } else {
            if ((pool - penalty.size) > 0) listOf(-10) else emptyList()
        }
        val bonusPart = if (bonus != -1) listOf(bonus) else emptyList()
        return penalty + scorePart + bonusPart
    }
}

private fun randomId(): String = Random.nextLong().toString()

package com.example.triorecorder.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.triorecorder.data.GameRepository
import com.example.triorecorder.model.GameData
import com.example.triorecorder.model.GameHistory
import com.example.triorecorder.model.PlayerMove

class AppState(
    private val repository: GameRepository,
) {
    var games by mutableStateOf<List<GameHistory>>(emptyList())
        private set

    var currentGame by mutableStateOf<GameData?>(null)
        private set

    var currentMove by mutableStateOf(PlayerMove())
        private set

    var endBonuses by mutableStateOf<List<Int>>(emptyList())
        private set

    suspend fun load() {
        games = repository.getGames()
    }

    fun startGame(players: List<String>) {
        currentGame = GameData.create(players)
        currentMove = PlayerMove()
        endBonuses = emptyList()
    }

    fun resetCurrentMove() {
        currentMove = PlayerMove()
    }

    fun changeMoveScore(value: Int) {
        currentMove = currentMove.copy(score = value)
        currentGame = currentGame?.withCurrentPlayerSkip(false)
    }

    fun addPenalty() {
        val game = currentGame ?: return
        if ((game.pool - currentMove.penalty.size) <= 0) return
        val updatedPenalty = currentMove.penalty + (-5)
        currentMove = currentMove.copy(penalty = updatedPenalty)
        if (updatedPenalty.size == 3) {
            currentGame = game.withCurrentPlayerSkip(true)
        }
    }

    fun skipMove() {
        currentMove = currentMove.copy(skip = true)
        currentGame = currentGame?.withCurrentPlayerSkip(true)
    }

    fun setBonus(value: Int) {
        currentMove = currentMove.copy(bonus = value)
    }

    fun nextTurn() {
        val game = currentGame ?: return
        val updated = game
            .withCurrentPlayerScoreChange(currentMove.getScoreChange(game.pool))
            .withCurrentPlayerDiceChange(currentMove.diceChange)
            .copy(pool = game.pool + currentMove.poolChange)
            .nextPlayer()
        currentGame = updated
        currentMove = PlayerMove()
    }

    fun appendEndBonus(value: Int) {
        endBonuses = endBonuses + value
    }

    fun removeLastEndBonus() {
        if (endBonuses.isNotEmpty()) endBonuses = endBonuses.dropLast(1)
    }

    fun finishEndBonuses() {
        val game = currentGame ?: return
        currentGame = game.withCurrentPlayerScoreChange(endBonuses)
    }

    suspend fun saveCurrentGame(): GameHistory? {
        val game = currentGame ?: return null
        val history = GameHistory.fromPlayers(game.players)
        repository.addGame(history)
        games = repository.getGames()
        currentGame = null
        currentMove = PlayerMove()
        endBonuses = emptyList()
        return history
    }

    suspend fun deleteGame(index: Int) {
        repository.deleteGame(index)
        games = repository.getGames()
    }
}

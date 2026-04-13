package com.example.triorecorder.data

import com.example.triorecorder.model.GameHistory

interface GameRepository {
    suspend fun getGames(): List<GameHistory>
    suspend fun addGame(game: GameHistory)
    suspend fun deleteGame(index: Int)
}

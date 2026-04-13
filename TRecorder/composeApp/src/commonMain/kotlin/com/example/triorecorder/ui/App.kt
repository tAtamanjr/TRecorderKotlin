package com.example.triorecorder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.triorecorder.data.GameRepository
import com.example.triorecorder.model.GameData
import com.example.triorecorder.model.GameHistory
import kotlinx.coroutines.launch

private enum class Screen {
    MainMenu,
    NewGame,
    GameTurn,
    End,
    Result,
    History,
    GameHistory,
}

@Composable
fun TriominosApp(repository: GameRepository) {
    val scope = rememberCoroutineScope()
    val state = remember { AppState(repository) }
    var screen by remember { mutableStateOf(Screen.MainMenu) }
    var selectedHistory by remember { mutableStateOf<GameHistory?>(null) }

    LaunchedEffect(Unit) {
        state.load()
    }

    MaterialTheme {
        when (screen) {
            Screen.MainMenu -> MainMenuScreen(
                onStartGame = { screen = Screen.NewGame },
                onOpenHistory = { screen = Screen.History }
            )

            Screen.NewGame -> NewGameScreen(
                onCancel = { screen = Screen.MainMenu },
                onStart = { players ->
                    state.startGame(players)
                    screen = Screen.GameTurn
                }
            )

            Screen.GameTurn -> GameTurnScreen(
                game = state.currentGame,
                onBack = { screen = Screen.MainMenu },
                onScoreChanged = { state.changeMoveScore(it) },
                onAddPenalty = { state.addPenalty() },
                onSkip = { state.skipMove() },
                onBonusSelected = { state.setBonus(it) },
                onResetMove = { state.resetCurrentMove() },
                onNextTurn = {
                    val game = state.currentGame
                    if (game != null && (game.dices + state.currentMove.diceChange) == 0) {
                        state.nextTurn()
                        screen = Screen.End
                    } else {
                        state.nextTurn()
                    }
                },
                move = state.currentMove,
                onOpenHistory = { screen = Screen.History }
            )

            Screen.End -> EndScreen(
                game = state.currentGame,
                bonuses = state.endBonuses,
                onBack = {
                    state.removeLastEndBonus()
                },
                onAddBonus = { state.appendEndBonus(it) },
                onFinish = {
                    state.finishEndBonuses()
                    screen = Screen.Result
                }
            )

            Screen.Result -> ResultScreen(
                game = state.currentGame,
                onSave = {
                    scope.launch {
                        state.saveCurrentGame()
                        screen = Screen.MainMenu
                    }
                },
                onWithoutSave = { screen = Screen.MainMenu }
            )

            Screen.History -> HistoryScreen(
                games = state.games,
                onBack = { screen = Screen.MainMenu },
                onOpenGame = {
                    selectedHistory = it
                    screen = Screen.GameHistory
                },
                onDelete = { index ->
                    scope.launch { state.deleteGame(index) }
                }
            )

            Screen.GameHistory -> GameHistoryScreen(
                history = selectedHistory,
                onBack = { screen = Screen.History }
            )
        }
    }
}

@Composable
private fun MainMenuScreen(
    onStartGame: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    ScreenScaffold(title = "Main Menu") {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = onStartGame, modifier = Modifier.fillMaxWidth()) {
                Text("Start game")
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) {
                Text("History")
            }
        }
    }
}

@Composable
private fun NewGameScreen(
    onCancel: () -> Unit,
    onStart: (List<String>) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val players = remember { mutableStateListOf<String>() }
    var error by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(title = "New Game", onBack = onCancel) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Player name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val trimmed = name.trim()
                    when {
                        trimmed.isBlank() -> error = "Name cannot be empty"
                        players.contains(trimmed) -> error = "Player already added"
                        else -> {
                            players.add(trimmed)
                            name = ""
                            error = null
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add player")
            }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(players) { index, player ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("${index + 1}. $player")
                        TextButton(onClick = { players.removeAt(index) }) { Text("Delete") }
                    }
                    Divider()
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onStart(players.toList()) },
                enabled = players.size >= 2,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start")
            }
        }
    }
}

@Composable
private fun GameTurnScreen(
    game: GameData?,
    move: com.example.triorecorder.model.PlayerMove,
    onBack: () -> Unit,
    onScoreChanged: (Int) -> Unit,
    onAddPenalty: () -> Unit,
    onSkip: () -> Unit,
    onBonusSelected: (Int) -> Unit,
    onResetMove: () -> Unit,
    onNextTurn: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    if (game == null) {
        MainMenuScreen(onStartGame = onBack, onOpenHistory = onOpenHistory)
        return
    }

    ScreenScaffold(title = "Game Turn", onBack = onBack) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            SummaryCard(title = "Current player") {
                Text(game.name, fontWeight = FontWeight.Bold)
                Text("Tiles: ${game.dices}")
                Text("Pool: ${game.pool}")
                Text("Score: ${game.score.sum()}")
            }

            Spacer(Modifier.height(12.dp))
            SummaryCard(title = "Players") {
                game.players.forEachIndexed { index, player ->
                    Text("${index + 1}. ${player.name} | total: ${player.score.sum()} | tiles: ${player.dices}")
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Move score")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(15, 20, 25, 30, 35, 40).forEach { value ->
                    SmallActionButton(label = value.toString(), onClick = { onScoreChanged(value) })
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Bonus")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(0, 25, 40, 50).forEach { value ->
                    SmallActionButton(label = value.toString(), onClick = { onBonusSelected(value) })
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Penalty count: ${move.penalty.size}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SmallActionButton(label = "Add penalty", onClick = onAddPenalty)
                SmallActionButton(label = "Skip", onClick = onSkip)
                SmallActionButton(label = "Reset", onClick = onResetMove)
            }

            Spacer(Modifier.height(16.dp))
            SummaryCard(title = "Current move") {
                Text("Score: ${if (move.score == -1) "-" else move.score}")
                Text("Bonus: ${if (move.bonus == -1) "-" else move.bonus}")
                Text("Penalty entries: ${move.penalty.size}")
                Text("Dice change: ${move.diceChange}")
                Text("Pool change: ${move.poolChange}")
            }

            Spacer(Modifier.weight(1f))
            Button(
                onClick = onNextTurn,
                enabled = !move.endTurn,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Next turn")
            }
        }
    }
}

@Composable
private fun EndScreen(
    game: GameData?,
    bonuses: List<Int>,
    onBack: () -> Unit,
    onAddBonus: (Int) -> Unit,
    onFinish: () -> Unit,
) {
    ScreenScaffold(title = "End", onBack = onBack) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(game?.name ?: "")
            Spacer(Modifier.height(12.dp))
            Text("End bonuses")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(5, 10, 15, 20, 25).forEach { value ->
                    SmallActionButton(label = value.toString(), onClick = { onAddBonus(value) })
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Selected: ${bonuses.joinToString()}".ifBlank { "Selected: -" })
            Spacer(Modifier.weight(1f))
            Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                Text("Finish")
            }
        }
    }
}

@Composable
private fun ResultScreen(
    game: GameData?,
    onSave: () -> Unit,
    onWithoutSave: () -> Unit,
) {
    val players = game?.players?.sortedByDescending { it.score.sum() }.orEmpty()
    ScreenScaffold(title = "Result") {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(players) { index, player ->
                    SummaryCard(title = "${index + 1}. ${player.name}") {
                        Text("Total: ${player.score.sum()}")
                        Text("Scores: ${player.score.joinToString()}")
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text("Save")
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onWithoutSave, modifier = Modifier.fillMaxWidth()) {
                Text("Back to menu")
            }
        }
    }
}

@Composable
private fun HistoryScreen(
    games: List<GameHistory>,
    onBack: () -> Unit,
    onOpenGame: (GameHistory) -> Unit,
    onDelete: (Int) -> Unit,
) {
    ScreenScaffold(title = "History", onBack = onBack) {
        if (games.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No saved games")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                itemsIndexed(games) { index, game ->
                    SummaryCard(
                        title = game.date,
                        modifier = Modifier.clickable { onOpenGame(game) }
                    ) {
                        Text("Players: ${game.players.size}")
                        Text("Winner: ${game.players.firstOrNull()?.name ?: "-"}")
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { onDelete(index) }) {
                            Text("Delete")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun GameHistoryScreen(
    history: GameHistory?,
    onBack: () -> Unit,
) {
    ScreenScaffold(title = "Game History", onBack = onBack) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(history?.date ?: "")
            Spacer(Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(history?.players.orEmpty()) { index, player ->
                    SummaryCard(title = "${index + 1}. ${player.name}") {
                        Text("Total: ${player.score.sum()}")
                        Text("Scores: ${player.score.joinToString()}")
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        TextButton(onClick = onBack) {
                            Text("Back")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            content()
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun SmallActionButton(
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, textAlign = TextAlign.Center)
    }
}

package com.example.helloworld

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.helloworld.ui.theme.HelloWorldTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HelloWorldTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PredictionScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// Стани екрану
sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val result: PredictResponse) : UiState()
    data class Error(val message: String) : UiState()
}

@Composable
fun PredictionScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var teams by remember { mutableStateOf<List<TeamInfo>>(emptyList()) }
    var teamA by remember { mutableStateOf<String?>(null) }
    var teamB by remember { mutableStateOf<String?>(null) }
    var uiState by remember { mutableStateOf<UiState>(UiState.Idle) }

    // Завантажуємо список команд один раз при відкритті екрану
    LaunchedEffect(Unit) {
        try {
            teams = ApiClient.getTeams()
        } catch (e: Exception) {
            uiState = UiState.Error("Не вдалося завантажити команди: ${e.message}")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("CyberScope — Прогноз матчу CS2", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(24.dp))

        TeamDropdown(
            label = "Команда A",
            teams = teams,
            selected = teamA,
            onSelect = { teamA = it }
        )
        Spacer(modifier = Modifier.height(12.dp))

        TeamDropdown(
            label = "Команда B",
            teams = teams,
            selected = teamB,
            onSelect = { teamB = it }
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val a = teamA
                val b = teamB
                if (a == null || b == null) {
                    uiState = UiState.Error("Оберіть обидві команди")
                    return@Button
                }
                uiState = UiState.Loading
                scope.launch {
                    try {
                        val result = ApiClient.predictMatch(a, b)
                        uiState = UiState.Success(result)
                    } catch (e: Exception) {
                        uiState = UiState.Error(e.message ?: "Невідома помилка")
                    }
                }
            },
            enabled = uiState !is UiState.Loading
        ) {
            Text("Прогнозувати")
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (val state = uiState) {
            is UiState.Idle -> {}
            is UiState.Loading -> CircularProgressIndicator()
            is UiState.Success -> ResultCard(state.result)
            is UiState.Error -> Text(
                text = "Помилка: ${state.message}",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun TeamDropdown(
    label: String,
    teams: List<TeamInfo>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selected ?: label)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            teams.forEach { team ->
                DropdownMenuItem(
                    text = { Text(team.name) },
                    onClick = {
                        onSelect(team.name)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ResultCard(result: PredictResponse) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("${result.team_a_name}: ${(result.team_a_win_probability * 100).toInt()}%")
            Text("${result.team_b_name}: ${(result.team_b_win_probability * 100).toInt()}%")
            Spacer(modifier = Modifier.height(8.dp))
            Text(result.explanation, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Чому такий прогноз?", style = MaterialTheme.typography.titleSmall)
            Text(
                "Формула: win_rate × 0.6 + rating × 0.4",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${result.team_a_name}\n" +
                    "Win rate: ${(result.team_a_win_rate * 100).toInt()}% " +
                    "(внесок ${result.team_a_win_rate_contribution})\n" +
                    "Rating: ${result.team_a_rating} " +
                    "(внесок ${result.team_a_rating_contribution})\n" +
                    "Підсумковий бал: ${result.team_a_score}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${result.team_b_name}\n" +
                    "Win rate: ${(result.team_b_win_rate * 100).toInt()}% " +
                    "(внесок ${result.team_b_win_rate_contribution})\n" +
                    "Rating: ${result.team_b_rating} " +
                    "(внесок ${result.team_b_rating_contribution})\n" +
                    "Підсумковий бал: ${result.team_b_score}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
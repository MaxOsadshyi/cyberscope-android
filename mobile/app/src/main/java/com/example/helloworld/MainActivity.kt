package com.example.helloworld

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.helloworld.ui.theme.HelloWorldTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HelloWorldTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppRoot(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

fun Modifier.verticalScrollbar(
    scrollState: ScrollState,
    width: Dp = 4.dp,
    color: Color = Color.Gray.copy(alpha = 0.55f)
): Modifier = composed {
    drawWithContent {
        drawContent()
        if (scrollState.maxValue > 0) {
            val viewportHeight = size.height
            val totalContentHeight = viewportHeight + scrollState.maxValue
            val barHeight = (viewportHeight * (viewportHeight / totalContentHeight))
                .coerceAtLeast(24.dp.toPx())
            val maxOffset = viewportHeight - barHeight
            val barOffset = (scrollState.value.toFloat() / scrollState.maxValue.toFloat()) * maxOffset
            val barWidth = width.toPx()

            drawRoundRect(
                color = color,
                topLeft = Offset(size.width - barWidth - 2.dp.toPx(), barOffset),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

sealed class Screen {
    object Predict : Screen()
    object History : Screen()
    data class Details(val item: HistoryItem) : Screen()
}

@Composable
fun AppRoot(modifier: Modifier = Modifier) {
    var screen by remember { mutableStateOf<Screen>(Screen.Predict) }

    when (val current = screen) {
        is Screen.Predict -> PredictionScreen(
            modifier = modifier,
            onOpenHistory = { screen = Screen.History }
        )
        is Screen.History -> HistoryScreen(
            modifier = modifier,
            onBack = { screen = Screen.Predict },
            onOpen = { item -> screen = Screen.Details(item) }
        )
        is Screen.Details -> DetailsScreen(
            modifier = modifier,
            item = current.item,
            onBack = { screen = Screen.History }
        )
    }
}

sealed class UiState {
    object Idle : UiState()
    data class Loading(val withAi: Boolean) : UiState()
    data class Success(val result: PredictResponse) : UiState()
    data class Error(val message: String) : UiState()
}

@Composable
fun PredictionScreen(
    modifier: Modifier = Modifier,
    onOpenHistory: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val historyStore = remember { HistoryStore.get(context) }
    val scrollState = rememberScrollState()
    var teams by remember { mutableStateOf<List<TeamInfo>>(emptyList()) }
    var maps by remember { mutableStateOf<List<String>>(emptyList()) }
    var teamA by rememberSaveable { mutableStateOf<String?>(null) }
    var teamB by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedMap by rememberSaveable { mutableStateOf<String?>(null) }
    var useAi by rememberSaveable { mutableStateOf(false) }
    var uiState by remember { mutableStateOf<UiState>(UiState.Idle) }

    LaunchedEffect(Unit) {
        try {
            teams = ApiClient.getTeams()
            maps = ApiClient.getMaps()
        } catch (e: Exception) {
            uiState = UiState.Error("Не вдалося завантажити дані: ${e.message}")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScrollbar(scrollState)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("CyberScope — Прогноз матчу CS2", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(20.dp))

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
        Spacer(modifier = Modifier.height(12.dp))

        MapDropdown(
            maps = maps,
            selected = selectedMap,
            onSelect = { selectedMap = it }
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Тактичний AI-розбір (Gemini)", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = useAi,
                onCheckedChange = { useAi = it }
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val a = teamA
                val b = teamB
                if (a == null || b == null) {
                    uiState = UiState.Error("Оберіть обидві команди")
                    return@Button
                }
                val aiFlag = useAi
                val chosenMap = selectedMap
                uiState = UiState.Loading(withAi = aiFlag)
                scope.launch {
                    try {
                        val result = ApiClient.predictMatch(
                            teamA = a,
                            teamB = b,
                            mapName = chosenMap,
                            useAi = aiFlag
                        )
                        try {
                            historyStore.add(result)
                        } catch (e: Exception) {
                            // історія недоступна, прогноз усе одно показуємо
                        }
                        uiState = UiState.Success(result)
                    } catch (e: Exception) {
                        uiState = UiState.Error(e.message ?: "Невідома помилка")
                    }
                }
            },
            enabled = uiState !is UiState.Loading
        ) {
            Text(if (useAi) "Прогнозувати з AI" else "Прогнозувати")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(onClick = onOpenHistory) {
            Text("Історія прогнозів")
        }

        Spacer(modifier = Modifier.height(20.dp))

        when (val state = uiState) {
            is UiState.Idle -> {}
            is UiState.Loading -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                if (state.withAi) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Генеруємо аналітику Gemini...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            is UiState.Success -> ResultCard(state.result)
            is UiState.Error -> Text(
                text = "Помилка: ${state.message}",
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
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
fun MapDropdown(
    maps: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selected?.let { "Карта: $it" } ?: "Карта: Усі (загальний win rate)")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Усі карти (загальний win rate)") },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            maps.forEach { mapName ->
                DropdownMenuItem(
                    text = { Text(mapName) },
                    onClick = {
                        onSelect(mapName)
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
            if (result.map_name != null) {
                Text(
                    "Карта матчу: ${result.map_name}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text("${result.team_a_name}: ${(result.team_a_win_probability * 100).roundToInt()}%")
            Text("${result.team_b_name}: ${(result.team_b_win_probability * 100).roundToInt()}%")
            Spacer(modifier = Modifier.height(8.dp))
            Text(result.explanation, style = MaterialTheme.typography.bodyMedium)

            if (!result.ai_analysis.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                val title = if (result.ai_status == "fallback") {
                    "AI-аналіз (резервний режим)"
                } else {
                    "Тактичний AI-аналіз (${result.ai_model ?: "Gemini"})"
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (result.ai_status == "fallback") {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(result.ai_analysis, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Чому такий прогноз?", style = MaterialTheme.typography.titleSmall)
            Text(
                "Формула: win_rate × 0.6 + rating × 0.4",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${result.team_a_name}\n" +
                    "Win rate: ${(result.team_a_win_rate * 100).roundToInt()}% " +
                    "(внесок ${result.team_a_win_rate_contribution})\n" +
                    "Rating: ${result.team_a_rating} " +
                    "(внесок ${result.team_a_rating_contribution})\n" +
                    "Підсумковий бал: ${result.team_a_score}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${result.team_b_name}\n" +
                    "Win rate: ${(result.team_b_win_rate * 100).roundToInt()}% " +
                    "(внесок ${result.team_b_win_rate_contribution})\n" +
                    "Rating: ${result.team_b_rating} " +
                    "(внесок ${result.team_b_rating_contribution})\n" +
                    "Підсумковий бал: ${result.team_b_score}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

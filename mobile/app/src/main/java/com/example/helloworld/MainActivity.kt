package com.example.helloworld

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.helloworld.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HelloWorldTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CsBackground
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = CsBackground
                    ) { innerPadding ->
                        AppRoot(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}

fun Modifier.verticalScrollbar(
    scrollState: ScrollState,
    width: Dp = 4.dp,
    color: Color = CsAmberPrimary.copy(alpha = 0.55f)
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
    object Ranking : Screen()
    data class Details(val item: HistoryItem) : Screen()
}

@Composable
fun AppRoot(modifier: Modifier = Modifier) {
    var screen by remember { mutableStateOf<Screen>(Screen.Predict) }

    when (val current = screen) {
        is Screen.Predict -> PredictionScreen(
            modifier = modifier,
            onOpenHistory = { screen = Screen.History },
            onOpenRanking = { screen = Screen.Ranking }
        )
        is Screen.History -> HistoryScreen(
            modifier = modifier,
            onBack = { screen = Screen.Predict },
            onOpen = { item -> screen = Screen.Details(item) }
        )
        is Screen.Ranking -> RankingScreen(
            modifier = modifier,
            onBack = { screen = Screen.Predict }
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
    onOpenHistory: () -> Unit = {},
    onOpenRanking: () -> Unit = {}
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "CyberScope — Прогноз CS2",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = CsTextPrimary
            )
            Surface(
                color = CsAmberPrimary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, CsAmberPrimary.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "CS2",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = CsAmberPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        TeamDropdown(
            label = "Команда A",
            teams = teams,
            selected = teamA,
            onSelect = { teamA = it }
        )
        Spacer(modifier = Modifier.height(10.dp))

        TeamDropdown(
            label = "Команда B",
            teams = teams,
            selected = teamB,
            onSelect = { teamB = it }
        )
        Spacer(modifier = Modifier.height(10.dp))

        MapDropdown(
            maps = maps,
            selected = selectedMap,
            onSelect = { selectedMap = it }
        )
        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CsSurface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, CsBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Тактичний AI-розбір (Gemini)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CsTextPrimary
                )
                Switch(
                    checked = useAi,
                    onCheckedChange = { useAi = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF111827),
                        checkedTrackColor = CsAmberPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                        } catch (_: Exception) {
                        }
                        uiState = UiState.Success(result)
                    } catch (e: Exception) {
                        uiState = UiState.Error(e.message ?: "Невідома помилка")
                    }
                }
            },
            enabled = uiState !is UiState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CsAmberPrimary,
                contentColor = Color(0xFF111827)
            )
        ) {
            Text(
                text = if (useAi) "Прогнозувати з AI" else "Прогнозувати",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onOpenHistory,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CsBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CsTextPrimary)
            ) {
                Text("Історія")
            }
            OutlinedButton(
                onClick = onOpenRanking,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CsCyanSecondary.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CsCyanSecondary)
            ) {
                Text("Рейтинг команд")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        when (val state = uiState) {
            is UiState.Idle -> {}
            is UiState.Loading -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = CsAmberPrimary)
                if (state.withAi) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Генеруємо аналітику Gemini...",
                        style = MaterialTheme.typography.bodySmall,
                        color = CsTextSecondary
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDropdown(
    label: String,
    teams: List<TeamInfo>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            color = CsSurface,
            border = BorderStroke(
                width = if (expanded) 1.5.dp else 1.dp,
                color = if (expanded) CsAmberPrimary else CsBorder
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selected != null) {
                        TeamLogo(teamName = selected, modifier = Modifier.size(30.dp))
                        Text(
                            text = selected,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = CsTextPrimary
                        )
                    } else {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = CsTextSecondary
                        )
                    }
                }
                Text(
                    text = if (expanded) "▲" else "▼",
                    color = if (expanded) CsAmberPrimary else CsTextSecondary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .exposedDropdownSize()
                .background(CsSurfaceVariant)
        ) {
            teams.forEach { team ->
                val isSelected = team.name == selected
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                TeamLogo(teamName = team.name, modifier = Modifier.size(30.dp))
                                Text(
                                    text = team.name,
                                    color = if (isSelected) CsAmberPrimary else CsTextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            Text(
                                text = "WR ${(team.win_rate * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = CsTextSecondary
                            )
                        }
                    },
                    onClick = {
                        onSelect(team.name)
                        expanded = false
                    },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapDropdown(
    maps: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            color = CsSurface,
            border = BorderStroke(
                width = if (expanded) 1.5.dp else 1.dp,
                color = if (expanded) CsCyanSecondary else CsBorder
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selected?.let { "Карта: $it" } ?: "Карта: Усі (загальний win rate)",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected != null) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected != null) CsCyanSecondary else CsTextPrimary
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    color = if (expanded) CsCyanSecondary else CsTextSecondary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .exposedDropdownSize()
                .background(CsSurfaceVariant)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Усі карти (загальний win rate)",
                        color = if (selected == null) CsCyanSecondary else CsTextPrimary,
                        fontWeight = if (selected == null) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            )
            maps.forEach { mapName ->
                val isSelected = mapName == selected
                DropdownMenuItem(
                    text = {
                        Text(
                            text = mapName,
                            color = if (isSelected) CsCyanSecondary else CsTextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelect(mapName)
                        expanded = false
                    },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ResultCard(result: PredictResponse) {
    val probA = result.team_a_win_probability.toFloat().coerceIn(0.05f, 0.95f)
    val probB = (1f - probA).coerceIn(0.05f, 0.95f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CsSurface),
        border = BorderStroke(1.dp, CsBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (result.map_name != null) {
                Text(
                    "Карта матчу: ${result.map_name}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = CsCyanSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${result.team_a_name}: ${(result.team_a_win_probability * 100).roundToInt()}%",
                    fontWeight = FontWeight.Bold,
                    color = CsCyanSecondary
                )
                Text(
                    "${result.team_b_name}: ${(result.team_b_win_probability * 100).roundToInt()}%",
                    fontWeight = FontWeight.Bold,
                    color = CsAmberPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .weight(probA)
                        .fillMaxHeight()
                        .background(CsCyanSecondary)
                )
                Box(
                    modifier = Modifier
                        .weight(probB)
                        .fillMaxHeight()
                        .background(CsAmberPrimary)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                result.explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = CsTextSecondary
            )

            if (!result.ai_analysis.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                val isFallback = result.ai_status == "fallback"
                val accentColor = if (isFallback) MaterialTheme.colorScheme.error else CsAmberPrimary
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CsSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        val title = if (isFallback) {
                            "AI-аналіз (резервний режим)"
                        } else {
                            "AI-аналіз (${result.ai_model ?: "Gemini"})"
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            result.ai_analysis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CsTextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = CsBorder)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Деталізація розрахунку",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = CsTextPrimary
            )
            Text(
                "Формула: win_rate × 0.6 + rating × 0.4",
                style = MaterialTheme.typography.bodySmall,
                color = CsTextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "${result.team_a_name}: WR ${(result.team_a_win_rate * 100).roundToInt()}% (${result.team_a_win_rate_contribution}) · RTG ${result.team_a_rating} (${result.team_a_rating_contribution}) → Бал ${result.team_a_score}",
                style = MaterialTheme.typography.bodySmall,
                color = CsTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${result.team_b_name}: WR ${(result.team_b_win_rate * 100).roundToInt()}% (${result.team_b_win_rate_contribution}) · RTG ${result.team_b_rating} (${result.team_b_rating_contribution}) → Бал ${result.team_b_score}",
                style = MaterialTheme.typography.bodySmall,
                color = CsTextSecondary
            )
        }
    }
}
package com.example.helloworld

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun RankingScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    var teams by remember { mutableStateOf<List<TeamInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTeam by remember { mutableStateOf<TeamInfo?>(null) }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        try {
            teams = ApiClient.getTeamsRanking()
        } catch (e: Exception) {
            errorMessage = e.message ?: "Помилка завантаження рейтингу"
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScrollbar(scrollState)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) {
                Text("← Назад")
            }
            Text(
                text = "Рейтинг команд CS2",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.width(64.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Натисніть на команду, щоб переглянути склад та рейтинг гравців",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Text(
                    text = "Помилка: $errorMessage",
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {
                teams.forEachIndexed { index, team ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { selectedTeam = team }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "#${index + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = team.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Win rate: ${(team.win_rate * 100).roundToInt()}% · Сер. рейтинг: ${team.rating}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = "${team.points} pts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    selectedTeam?.let { team ->
        TeamRosterDialog(
            team = team,
            onDismiss = { selectedTeam = null }
        )
    }
}

@Composable
fun TeamRosterDialog(
    team: TeamInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = team.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Рейтинг команди: ${team.points} pts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Основний склад:",
                    style = MaterialTheme.typography.titleSmall
                )
                HorizontalDivider()
                if (team.roster.isEmpty()) {
                    Text("Склад команди недоступний", style = MaterialTheme.typography.bodyMedium)
                } else {
                    team.roster.forEach { player ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = player.nickname,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Рейтинг гравця: ${player.rating}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрити")
            }
        }
    )
}
package com.example.helloworld

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.helloworld.ui.theme.*
import kotlin.math.roundToInt

private fun teamBadgeColor(teamName: String): Color = when {
    teamName.contains("Vitality", ignoreCase = true) -> Color(0xFFFACC15)
    teamName.contains("Natus", ignoreCase = true) -> Color(0xFFFFD600)
    teamName.contains("Spirit", ignoreCase = true) -> Color(0xFF38BDF8)
    teamName.contains("G2", ignoreCase = true) -> Color(0xFFEF4444)
    teamName.contains("FaZe", ignoreCase = true) -> Color(0xFFF43F5E)
    else -> Color(0xFFFB923C)
}

@Composable
fun TeamLogo(
    teamName: String,
    modifier: Modifier = Modifier
) {
    val accent = teamBadgeColor(teamName)

    Surface(
        modifier = modifier.size(44.dp),
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.65f))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val w = size.width
            val h = size.height

            when {
                // Vitality — стилізована бджола / крила V
                teamName.contains("Vitality", ignoreCase = true) -> {
                    val wings = Path().apply {
                        moveTo(w * 0.12f, h * 0.25f)
                        lineTo(w * 0.44f, h * 0.55f)
                        lineTo(w * 0.22f, h * 0.62f)
                        close()
                        moveTo(w * 0.88f, h * 0.25f)
                        lineTo(w * 0.56f, h * 0.55f)
                        lineTo(w * 0.78f, h * 0.62f)
                        close()
                    }
                    drawPath(wings, color = accent)
                    val body = Path().apply {
                        moveTo(w * 0.50f, h * 0.20f)
                        lineTo(w * 0.60f, h * 0.52f)
                        lineTo(w * 0.50f, h * 0.88f)
                        lineTo(w * 0.40f, h * 0.52f)
                        close()
                    }
                    drawPath(body, color = Color.White)
                }

                // Natus Vincere — фірмовий скошений блок зі смугами
                teamName.contains("Natus", ignoreCase = true) -> {
                    val shield = Path().apply {
                        moveTo(w * 0.22f, h * 0.16f)
                        lineTo(w * 0.90f, h * 0.16f)
                        lineTo(w * 0.78f, h * 0.84f)
                        lineTo(w * 0.10f, h * 0.84f)
                        close()
                    }
                    drawPath(shield, color = accent)
                    drawLine(
                        color = Color(0xFF111827),
                        start = Offset(w * 0.34f, h * 0.30f),
                        end = Offset(w * 0.26f, h * 0.70f),
                        strokeWidth = w * 0.11f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFF111827),
                        start = Offset(w * 0.34f, h * 0.30f),
                        end = Offset(w * 0.54f, h * 0.70f),
                        strokeWidth = w * 0.10f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFF111827),
                        start = Offset(w * 0.62f, h * 0.30f),
                        end = Offset(w * 0.54f, h * 0.70f),
                        strokeWidth = w * 0.11f,
                        cap = StrokeCap.Round
                    )
                }

                // Team Spirit — емблема дракона (вихровий півмісяць + ікло)
                teamName.contains("Spirit", ignoreCase = true) -> {
                    drawArc(
                        color = Color.White,
                        startAngle = 35f,
                        sweepAngle = 265f,
                        useCenter = false,
                        topLeft = Offset(w * 0.16f, h * 0.14f),
                        size = Size(w * 0.68f, h * 0.68f),
                        style = Stroke(width = w * 0.16f, cap = StrokeCap.Round)
                    )
                    val crest = Path().apply {
                        moveTo(w * 0.52f, h * 0.34f)
                        lineTo(w * 0.84f, h * 0.48f)
                        lineTo(w * 0.55f, h * 0.60f)
                        close()
                    }
                    drawPath(crest, color = accent)
                }

                // G2 Esports — самурайська маска / шолом
                teamName.contains("G2", ignoreCase = true) -> {
                    val helmet = Path().apply {
                        moveTo(w * 0.20f, h * 0.22f)
                        lineTo(w * 0.50f, h * 0.36f)
                        lineTo(w * 0.80f, h * 0.22f)
                        lineTo(w * 0.74f, h * 0.72f)
                        lineTo(w * 0.50f, h * 0.88f)
                        lineTo(w * 0.26f, h * 0.72f)
                        close()
                    }
                    drawPath(helmet, color = Color.White)
                    drawLine(
                        color = accent,
                        start = Offset(w * 0.30f, h * 0.52f),
                        end = Offset(w * 0.45f, h * 0.58f),
                        strokeWidth = w * 0.09f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = accent,
                        start = Offset(w * 0.70f, h * 0.52f),
                        end = Offset(w * 0.55f, h * 0.58f),
                        strokeWidth = w * 0.09f,
                        cap = StrokeCap.Round
                    )
                }

                // FaZe Clan — фірмова монограма F
                teamName.contains("FaZe", ignoreCase = true) -> {
                    val fazePath = Path().apply {
                        moveTo(w * 0.20f, h * 0.22f)
                        lineTo(w * 0.86f, h * 0.22f)
                        lineTo(w * 0.78f, h * 0.38f)
                        lineTo(w * 0.42f, h * 0.38f)
                        lineTo(w * 0.38f, h * 0.48f)
                        lineTo(w * 0.70f, h * 0.48f)
                        lineTo(w * 0.62f, h * 0.62f)
                        lineTo(w * 0.33f, h * 0.62f)
                        lineTo(w * 0.24f, h * 0.84f)
                        lineTo(w * 0.12f, h * 0.84f)
                        close()
                    }
                    drawPath(fazePath, color = accent)
                }

                // MOUZ — стилізований щит-миша з вухами
                else -> {
                    drawCircle(color = accent, radius = w * 0.17f, center = Offset(w * 0.30f, h * 0.30f))
                    drawCircle(color = accent, radius = w * 0.17f, center = Offset(w * 0.70f, h * 0.30f))
                    val head = Path().apply {
                        moveTo(w * 0.24f, h * 0.36f)
                        lineTo(w * 0.76f, h * 0.36f)
                        lineTo(w * 0.50f, h * 0.86f)
                        close()
                    }
                    drawPath(head, color = accent)
                }
            }
        }
    }
}

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
            .background(CsBackground)
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
                Text("← Назад", color = CsAmberPrimary, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "Рейтинг команд",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = CsTextPrimary
            )
            Spacer(modifier = Modifier.width(64.dp))
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Топ-3 команди виділено жовтою рамкою · Натисніть для перегляду складу",
            style = MaterialTheme.typography.bodySmall,
            color = CsTextSecondary
        )
        Spacer(modifier = Modifier.height(14.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CsAmberPrimary)
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
                    val isTopThree = index < 3
                    val cardBorderColor = if (isTopThree) CsAmberPrimary else CsBorder

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { selectedTeam = team },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CsSurface),
                        border = BorderStroke(
                            width = if (isTopThree) 1.5.dp else 1.dp,
                            color = cardBorderColor.copy(alpha = if (isTopThree) 0.85f else 0.7f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTopThree) CsAmberPrimary else CsTextSecondary,
                                    modifier = Modifier.width(20.dp)
                                )
                                TeamLogo(teamName = team.name)
                                Column {
                                    Text(
                                        text = team.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = CsTextPrimary
                                    )
                                    Text(
                                        text = "RTG: ${team.rating}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CsTextSecondary
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${team.points} pts",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTopThree) CsAmberPrimary else CsTextPrimary
                                )
                                Text(
                                    text = "WR: ${(team.win_rate * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CsCyanSecondary
                                )
                            }
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
    val badgeColor = teamBadgeColor(team.name)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CsSurface,
        shape = RoundedCornerShape(18.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamLogo(teamName = team.name)
                Column {
                    Text(
                        text = "Склад ${team.name}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CsTextPrimary
                    )
                    Text(
                        text = "Рейтинг: ${team.points} pts",
                        style = MaterialTheme.typography.bodySmall,
                        color = CsAmberPrimary
                    )
                }
            }
        },
        text = {
            if (team.roster.isEmpty()) {
                Text("Склад команди недоступний", color = CsTextSecondary)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    team.roster.chunked(2).forEach { rowPlayers ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowPlayers.forEach { player ->
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = CsSurfaceVariant,
                                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(38.dp),
                                            shape = RoundedCornerShape(19.dp),
                                            color = badgeColor.copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, badgeColor)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = player.nickname.take(2).uppercase(),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = badgeColor
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = player.nickname,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = CsTextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Rating 2.0: ${player.rating}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CsTextSecondary
                                        )
                                    }
                                }
                            }
                            if (rowPlayers.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрити", color = CsAmberPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}
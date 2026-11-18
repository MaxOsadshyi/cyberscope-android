package com.example.helloworld

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Екран списку збережених прогнозів: новіші зверху, порожній стан, видалення й очищення. */
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onOpen: (HistoryItem) -> Unit
) {
    val context = LocalContext.current
    val store = remember { HistoryStore.get(context) }
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<HistoryItem?>(null) }

    BackHandler(onBack = onBack)

    fun reload() {
        scope.launch {
            try {
                items = store.getAll()
                loadFailed = false
            } catch (e: Exception) {
                loadFailed = true
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("← Назад") }
            if (items.isNotEmpty()) {
                TextButton(onClick = { showClearDialog = true }) { Text("Очистити") }
            }
        }
        Text("Історія прогнозів", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        when {
            loading -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            loadFailed -> Text(
                "Не вдалося прочитати історію",
                color = MaterialTheme.colorScheme.error
            )
            items.isEmpty() -> Text(
                "Історія порожня. Зробіть перший прогноз, і він з'явиться тут.",
                style = MaterialTheme.typography.bodyMedium
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.id }) { item ->
                    HistoryRow(
                        item = item,
                        onOpen = { onOpen(item) },
                        onDelete = { itemToDelete = item }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Очистити історію?") },
            text = { Text("Усі збережені прогнози буде видалено з пристрою. Цю дію не можна скасувати.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    scope.launch {
                        try {
                            store.clear()
                        } catch (e: Exception) {
                            // помилка бази: список нижче просто залишиться як є
                        }
                        reload()
                    }
                }) { Text("Очистити") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Скасувати") }
            }
        )
    }

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Видалити запис?") },
            text = { Text("${item.inputSummary}\n${formatTimestamp(item.createdAt)}") },
            confirmButton = {
                TextButton(onClick = {
                    itemToDelete = null
                    scope.launch {
                        try {
                            store.delete(item.id)
                        } catch (e: Exception) {
                            // помилка бази: список нижче просто залишиться як є
                        }
                        reload()
                    }
                }) { Text("Видалити") }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("Скасувати") }
            }
        )
    }
}

@Composable
private fun HistoryRow(
    item: HistoryItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onOpen() }) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.inputSummary, style = MaterialTheme.typography.titleSmall)
                Text(item.resultSummary, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${formatTimestamp(item.createdAt)} · ${item.status}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = onDelete) { Text("Видалити") }
        }
    }
}

/** Екран деталей: показує збережену відповідь без повторного запиту до backend. */
@Composable
fun DetailsScreen(
    modifier: Modifier = Modifier,
    item: HistoryItem,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val store = remember { HistoryStore.get(context) }
    val scope = rememberCoroutineScope()

    var result by remember { mutableStateOf<PredictResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val detailsScrollState = rememberScrollState()

    BackHandler(onBack = onBack)

    LaunchedEffect(item.id) {
        try {
            result = store.getResult(item.id)
        } catch (e: Exception) {
            result = null
        }
        loading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScrollbar(detailsScrollState)
            .verticalScroll(detailsScrollState)
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Назад") }
        Text("Деталі прогнозу", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Вхідні дані: ${item.inputSummary}", style = MaterialTheme.typography.bodyMedium)
        Text(
            "Збережено: ${formatTimestamp(item.createdAt)} · ${item.status}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            "Це збережений результат на момент запиту, прогноз заново не обчислюється.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(16.dp))

        val saved = result
        when {
            loading -> CircularProgressIndicator()
            saved == null -> Text(
                "Не вдалося відкрити збережений результат",
                color = MaterialTheme.colorScheme.error
            )
            else -> ResultCard(saved)
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = { showDeleteDialog = true }) { Text("Видалити запис") }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Видалити запис?") },
            text = { Text("${item.inputSummary}\n${formatTimestamp(item.createdAt)}") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    scope.launch {
                        try {
                            store.delete(item.id)
                        } catch (e: Exception) {
                            // помилка бази: просто повертаємось до списку
                        }
                        onBack()
                    }
                }) { Text("Видалити") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Скасувати") }
            }
        )
    }
}


package com.example.stampcollectionsapp.features.collection.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.stampcollectionsapp.features.collection.data.model.Stamp
import com.example.stampcollectionsapp.features.collection.presentation.components.PriceChart
import com.example.stampcollectionsapp.features.collection.presentation.components.ExportPdfDialog
import com.example.stampcollectionsapp.features.collection.presentation.viewmodel.PriceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StampDetailScreen(
    stamp: Stamp,
    onBack: () -> Unit,
    priceViewModel: PriceViewModel = viewModel(),
    stampViewModel: com.example.stampcollectionsapp.features.collection.presentation.viewmodel.StampViewModel = viewModel()
) {
    val prices by priceViewModel.prices.collectAsStateWithLifecycle()
    var currentStamp by remember { mutableStateOf(stamp) }
    var showExportDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(stamp.id) {
        priceViewModel.loadPricesForStamp(stamp.id)
        currentStamp = stamp
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали марки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val newFavoriteStatus = !currentStamp.isFavorite
                            stampViewModel.toggleFavorite(currentStamp.id, newFavoriteStatus)
                            currentStamp = currentStamp.copy(isFavorite = newFavoriteStatus)
                        }
                    ) {
                        Icon(
                            if (currentStamp.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (currentStamp.isFavorite) "Удалить из избранного" else "Добавить в избранное",
                            tint = if (currentStamp.isFavorite) Color(0xFFFF5252) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Экспорт в PDF")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (stamp.imageUrl != null) {
                val imageModel = try {
                    // Пробуем как URI (локальный файл)
                    android.net.Uri.parse(stamp.imageUrl)
                } catch (e: Exception) {
                    // Если не URI, используем как URL
                    stamp.imageUrl
                }
                AsyncImage(
                    model = imageModel,
                    contentDescription = "Изображение марки",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
            }
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Страна: ${stamp.country}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Год: ${stamp.year}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (stamp.denomination != null) {
                        Text(
                            text = "Номинал: ${stamp.denomination}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    if (stamp.description != null) {
                        Text(
                            text = "Описание: ${stamp.description}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (stamp.condition != null) {
                        Text(
                            text = "Состояние: ${stamp.condition}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (stamp.price != null) {
                        Text(
                            text = "Цена: ${stamp.price} ${stamp.currency ?: "EUR"}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Text(
                text = "График изменения цены",
                style = MaterialTheme.typography.titleLarge
            )
            
            PriceChart(prices = prices)
        }
    }
    
    if (showExportDialog) {
        ExportPdfDialog(
            collection = null,
            stamps = listOf(currentStamp),
            prices = mapOf(currentStamp.id to prices),
            onDismiss = { showExportDialog = false },
            onExport = { showExportDialog = false }
        )
    }
}


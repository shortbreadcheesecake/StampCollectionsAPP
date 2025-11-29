package com.example.stampcollectionsapp.features.collection.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import java.util.Locale
import com.example.stampcollectionsapp.features.collection.data.model.CollectionWithStamps
import com.example.stampcollectionsapp.features.collection.data.model.Stamp
import com.example.stampcollectionsapp.features.collection.presentation.screens.StampDetailScreen
import com.example.stampcollectionsapp.features.collection.presentation.components.ColorPickerDialog
import com.example.stampcollectionsapp.features.collection.presentation.components.ExportPdfDialog
import com.example.stampcollectionsapp.features.collection.presentation.components.CollectionPriceChart
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    collectionId: Long,
    onBack: () -> Unit,
    collectionViewModel: com.example.stampcollectionsapp.features.collection.presentation.viewmodel.CollectionViewModel = viewModel()
) {
    var collectionWithStamps by remember { mutableStateOf<CollectionWithStamps?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedStampId by remember { mutableStateOf<Long?>(null) }
    val stampViewModel: com.example.stampcollectionsapp.features.collection.presentation.viewmodel.StampViewModel = hiltViewModel()
    val stampIsLoading by stampViewModel.isLoading.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var stampToEdit by remember { mutableStateOf<Stamp?>(null) }
    var editError by remember { mutableStateOf<String?>(null) }
    var stampToDelete by remember { mutableStateOf<Stamp?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    val priceViewModel: com.example.stampcollectionsapp.features.collection.presentation.viewmodel.PriceViewModel = hiltViewModel()
    val collectionPrices by priceViewModel.collectionPrices.collectAsStateWithLifecycle()

    suspend fun fetchCollection() {
        isLoading = true
        try {
            collectionWithStamps = collectionViewModel.getCollectionWithStamps(collectionId)
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = "Ошибка при загрузке коллекции: ${e.message}"
        } finally {
            isLoading = false
        }
    }
    
    LaunchedEffect(collectionId) {
        fetchCollection()
    }
    
    LaunchedEffect(collectionWithStamps) {
        collectionWithStamps?.let { cws ->
            val stampIds = cws.stamps.map { it.stamp.id }
            priceViewModel.loadCollectionPricesOverTime(stampIds)
        }
    }

    LaunchedEffect(stampToEdit) {
        if (stampToEdit != null) {
            editError = null
            stampViewModel.clearError()
        }
    }

    LaunchedEffect(stampToDelete) {
        if (stampToDelete != null) {
            deleteError = null
            stampViewModel.clearError()
        }
    }
    
    // Если выбрана марка, показываем экран деталей
    if (selectedStampId != null && collectionWithStamps != null) {
        val stamp = collectionWithStamps!!.stamps.find { it.stamp.id == selectedStampId }?.stamp
        if (stamp != null) {
            StampDetailScreen(
                stamp = stamp,
                onBack = { selectedStampId = null }
            )
            return
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        collectionWithStamps?.collection?.collectionName ?: "Коллекция"
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { showColorPicker = true }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Изменить цвет",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Экспорт в PDF")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else if (collectionWithStamps != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Информация о коллекции
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = collectionWithStamps!!.collection.color?.let { 
                            android.graphics.Color.valueOf(it.toInt()).let { c ->
                                Color(c.red(), c.green(), c.blue(), c.alpha())
                            }
                        }?.copy(alpha = 0.1f) ?: MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = collectionWithStamps!!.collection.collectionName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (collectionWithStamps!!.collection.description != null) {
                            Text(
                                text = collectionWithStamps!!.collection.description!!,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        Text(
                            text = "Марок в коллекции: ${collectionWithStamps!!.stamps.size}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                
                // Список марок с графиком
                if (collectionWithStamps!!.stamps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "В коллекции пока нет марок",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // График стоимости коллекции
                        if (collectionPrices.isNotEmpty()) {
                            item {
                                CollectionPriceChart(
                                    prices = collectionPrices,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                )
                            }
                        }
                        
                        items(collectionWithStamps!!.stamps) { stampWithDetails ->
                            StampItemInCollection(
                                stampWithDetails = stampWithDetails,
                                onClick = { selectedStampId = stampWithDetails.stamp.id },
                                onEdit = { stampToEdit = stampWithDetails.stamp },
                                onDelete = { stampToDelete = stampWithDetails.stamp },
                                onToggleFavorite = { 
                                    scope.launch {
                                        stampViewModel.toggleFavorite(
                                            stampWithDetails.stamp.id,
                                            !stampWithDetails.stamp.isFavorite
                                        )
                                        fetchCollection()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (stampToEdit != null) {
        StampEditDialog(
            stamp = stampToEdit!!,
            isProcessing = stampIsLoading,
            errorMessage = editError,
            onDismiss = {
                stampToEdit = null
                editError = null
                stampViewModel.clearError()
            },
            onConfirm = { updatedStamp ->
                scope.launch {
                    editError = null
                    val success = stampViewModel.updateStamp(updatedStamp)
                    if (success) {
                        stampToEdit = null
                        stampViewModel.clearError()
                        snackbarHostState.showSnackbar("Марка обновлена")
                        fetchCollection()
                    } else {
                        editError = stampViewModel.errorMessage.value
                    }
                }
            }
        )
    }

    if (stampToDelete != null) {
        ConfirmDeleteStampDialog(
            stamp = stampToDelete!!,
            isProcessing = stampIsLoading,
            errorMessage = deleteError,
            onDismiss = {
                stampToDelete = null
                deleteError = null
                stampViewModel.clearError()
            },
            onConfirm = {
                val targetStamp = stampToDelete!!
                scope.launch {
                    deleteError = null
                    val success = stampViewModel.deleteStamp(targetStamp)
                    if (success) {
                        stampToDelete = null
                        selectedStampId = null
                        stampViewModel.clearError()
                        snackbarHostState.showSnackbar("Марка удалена")
                        fetchCollection()
                    } else {
                        deleteError = stampViewModel.errorMessage.value
                    }
                }
            }
        )
    }
    
    // Диалог выбора цвета
    if (showColorPicker && collectionWithStamps != null) {
        ColorPickerDialog(
            currentColor = collectionWithStamps!!.collection.color?.let { 
                android.graphics.Color.valueOf(it.toInt()).let { c ->
                    Color(c.red(), c.green(), c.blue(), c.alpha())
                }
            } ?: MaterialTheme.colorScheme.primary,
            onDismiss = { showColorPicker = false },
            onColorSelected = { color ->
                scope.launch {
                    val colorValue = android.graphics.Color.argb(
                        (color.alpha * 255).toInt(),
                        (color.red * 255).toInt(),
                        (color.green * 255).toInt(),
                        (color.blue * 255).toInt()
                    )
                    val updatedCollection = collectionWithStamps!!.collection.copy(
                        color = colorValue.toLong() and 0xFFFFFFFFL
                    )
                    collectionViewModel.updateCollection(updatedCollection)
                    showColorPicker = false
                    // Обновляем коллекцию после изменения цвета
                    fetchCollection()
                }
            }
        )
    }
    
    // Диалог экспорта PDF
    if (showExportDialog && collectionWithStamps != null) {
        ExportPdfDialog(
            collection = collectionWithStamps!!.collection,
            stamps = collectionWithStamps!!.stamps.map { it.stamp },
            collectionPrices = collectionPrices,
            onDismiss = { showExportDialog = false },
            onExport = { showExportDialog = false }
        )
    }
}

@Composable
fun StampItemInCollection(
    stampWithDetails: com.example.stampcollectionsapp.features.collection.data.model.StampWithDetails,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (stampWithDetails.stamp.imageUrl != null) {
                val imageModel = try {
                    android.net.Uri.parse(stampWithDetails.stamp.imageUrl)
                } catch (e: Exception) {
                    stampWithDetails.stamp.imageUrl
                }
                Image(
                    painter = rememberAsyncImagePainter(model = imageModel),
                    contentDescription = "Марка",
                    modifier = Modifier.size(80.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${stampWithDetails.stamp.country} ${stampWithDetails.stamp.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (stampWithDetails.stamp.denomination != null) {
                    Text(
                        text = "Номинал: ${stampWithDetails.stamp.denomination}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (stampWithDetails.stamp.description != null) {
                    Text(
                        text = stampWithDetails.stamp.description!!,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (stampWithDetails.collectionStamp.currentPrice != null) {
                    Text(
                        text = "Цена: ${stampWithDetails.collectionStamp.currentPrice}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.heightIn(min = 72.dp)
            ) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (stampWithDetails.stamp.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (stampWithDetails.stamp.isFavorite) "Удалить из избранного" else "Добавить в избранное",
                        tint = if (stampWithDetails.stamp.isFavorite) Color(0xFFFF5252) else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Редактировать марку")
                }
                Spacer(modifier = Modifier.height(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить марку")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StampEditDialog(
    stamp: Stamp,
    isProcessing: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (Stamp) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var name by remember(stamp.id) { mutableStateOf(stamp.name.orEmpty()) }
    var country by remember(stamp.id) { mutableStateOf(stamp.country) }
    var year by remember(stamp.id) { mutableStateOf(stamp.year.toString()) }
    var denomination by remember(stamp.id) { mutableStateOf(stamp.denomination.orEmpty()) }
    var description by remember(stamp.id) { mutableStateOf(stamp.description.orEmpty()) }
    var condition by remember(stamp.id) { mutableStateOf(stamp.condition.orEmpty()) }
    var priceInput by remember(stamp.id) {
        mutableStateOf(
            stamp.price?.let { String.format(Locale.US, "%.2f", it) } ?: ""
        )
    }
    var priceUrl by remember(stamp.id) { mutableStateOf(stamp.priceUrl.orEmpty()) }
    var currency by remember(stamp.id) { mutableStateOf(stamp.currency ?: "") }
    var imageUri by remember(stamp.id) {
        mutableStateOf<android.net.Uri?>(
            stamp.imageUrl?.let {
                try {
                    android.net.Uri.parse(it)
                } catch (e: Exception) {
                    null
                }
            }
        )
    }
    var showImagePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var conditionExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    
    val imagePicker = com.example.stampcollectionsapp.features.collection.presentation.utils.rememberImagePicker(
        onImageSelected = { uri ->
            imageUri = uri
        }
    )
    
    if (showImagePicker) {
        com.example.stampcollectionsapp.features.collection.presentation.components.ImagePickerDialog(
            onDismiss = { showImagePicker = false },
            imagePickerState = imagePicker
        )
    }

    Dialog(onDismissRequest = { if (!isProcessing) onDismiss() }) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Редактирование марки",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // Кнопка выбора фото
                Button(
                    onClick = { showImagePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Изменить фото")
                }
                
                // Отображение текущего фото
                if (imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = imageUri),
                        contentDescription = "Фото марки",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("Страна *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = year,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                            year = newValue
                        }
                    },
                    label = { Text("Год *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = denomination,
                    onValueChange = { denomination = it },
                    label = { Text("Номинал") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    maxLines = 4
                )

                ExposedDropdownMenuBox(
                    expanded = conditionExpanded,
                    onExpandedChange = { conditionExpanded = !conditionExpanded }
                ) {
                    OutlinedTextField(
                        value = condition.ifBlank { "Не указано" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Состояние") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = conditionExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = conditionExpanded,
                        onDismissRequest = { conditionExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Не указано") },
                            onClick = {
                                condition = ""
                                conditionExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Гашёная") },
                            onClick = {
                                condition = "Гашёная"
                                conditionExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Неиспользованная") },
                            onClick = {
                                condition = "Неиспользованная"
                                conditionExpanded = false
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { newValue ->
                            val normalized = newValue.replace(',', '.')
                            val regex = Regex("^\\d*\\.?\\d{0,2}$")
                            if (normalized.isEmpty() || regex.matches(normalized)) {
                                priceInput = normalized
                            }
                        },
                        label = { Text("Цена") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        placeholder = { Text("Например: 12.97") }
                    )

                    ExposedDropdownMenuBox(
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = !currencyExpanded }
                    ) {
                        OutlinedTextField(
                            value = when (currency) {
                                "EUR" -> "€"
                                "USD" -> "$"
                                "RUB" -> "₽"
                                "" -> "—"
                                else -> currency
                            },
                            onValueChange = {},
                            readOnly = true,
                            enabled = priceInput.isNotBlank(),
                            label = { Text("Валюта") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                            modifier = Modifier
                                .width(90.dp)
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = currencyExpanded,
                            onDismissRequest = { currencyExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("€ EUR") },
                                onClick = {
                                    currency = "EUR"
                                    currencyExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("$ USD") },
                                onClick = {
                                    currency = "USD"
                                    currencyExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("₽ RUB") },
                                onClick = {
                                    currency = "RUB"
                                    currencyExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Очистить") },
                                onClick = {
                                    currency = ""
                                    currencyExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Ссылка для цены
                OutlinedTextField(
                    value = priceUrl,
                    onValueChange = { priceUrl = it },
                    label = { Text("Ссылка для цены") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://colnect.com/...") }
                )

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isProcessing
                    ) {
                        Text("Отмена")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            validationError = null
                            val trimmedCountry = country.trim()
                            if (trimmedCountry.isEmpty()) {
                                validationError = "Укажите страну"
                                return@TextButton
                            }
                            val yearValue = year.toIntOrNull()
                            if (yearValue == null || yearValue <= 0) {
                                validationError = "Укажите корректный год"
                                return@TextButton
                            }
                            val priceValue = priceInput.toDoubleOrNull()
                            val finalCurrency = if (priceValue != null) {
                                currency.ifBlank { "EUR" }
                            } else {
                                null
                            }
                            val updatedStamp = stamp.copy(
                                name = name.trim().ifBlank { null },
                                country = trimmedCountry,
                                year = yearValue,
                                denomination = denomination.trim().ifBlank { null },
                                description = description.trim().ifBlank { null },
                                condition = condition.trim().ifBlank { null },
                                price = priceValue,
                                currency = finalCurrency,
                                priceUrl = priceUrl.trim().ifBlank { null },
                                imageUrl = imageUri?.toString()
                            )
                            onConfirm(updatedStamp)
                        },
                        enabled = !isProcessing
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteStampDialog(
    stamp: Stamp,
    isProcessing: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val stampName = stamp.name?.takeIf { it.isNotBlank() } ?: "${stamp.country} ${stamp.year}"
    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = { Text("Удалить марку?") },
        text = {
            Column {
                Text("Вы уверены, что хотите удалить \"$stampName\"?")
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isProcessing
            ) {
                Text("Удалить")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isProcessing
            ) {
                Text("Отмена")
            }
        }
    )
}


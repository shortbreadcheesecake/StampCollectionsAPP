package com.example.stampcollectionsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.rememberCoroutineScope
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.tasks.await
import java.io.InputStream
import java.io.File
import com.example.stampcollectionsapp.features.auth.presentation.screens.LoginScreen
import com.example.stampcollectionsapp.features.auth.presentation.screens.RegisterScreen
import com.example.stampcollectionsapp.features.auth.presentation.state.AuthState
import com.example.stampcollectionsapp.features.auth.presentation.viewmodel.AuthViewModel
import com.example.stampcollectionsapp.features.collection.data.model.Stamp
import com.example.stampcollectionsapp.features.collection.presentation.screens.CollectionDetailScreen
import com.example.stampcollectionsapp.ui.theme.StampCollectionsAPPTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StampCollectionsAPPTheme {
                StampCollectionsAPPApp()
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    // Auth Screens
    object Login : Screen("login", "Вход")
    object Register : Screen("register", "Регистрация")
    
    // Main App Screens
    object Home : Screen("home", "Главная", Icons.Default.Home)
    object Collection : Screen("collection", "Коллекции", Icons.Default.Collections)
    object AddItem : Screen("add", "Добавить", Icons.Default.Add)
    object Favorites : Screen("favorites", "Избранное", Icons.Default.Favorite)
    object Profile : Screen("profile", "Профиль", Icons.Default.Person)
}

// Удален старый AuthScreen - используем LoginScreen и RegisterScreen с Firebase
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentScreen.title) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    if (currentScreen == Screen.Profile) {
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.Logout, contentDescription = "Выход")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                listOf(
                    Screen.Home,
                    Screen.Collection,
                    Screen.AddItem,
                    Screen.Favorites,
                    Screen.Profile
                ).forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon!!, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentScreen == screen,
                        onClick = { onScreenSelected(screen) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (currentScreen) {
                is Screen.Login -> { /* Handled separately */ }
                is Screen.Register -> { /* Handled separately */ }
                is Screen.Home -> HomeScreen()
                is Screen.Collection -> CollectionScreen()
                is Screen.AddItem -> AddItemScreen()
                is Screen.Favorites -> FavoritesScreen()
                is Screen.Profile -> ProfileScreen()
            }
        }
    }
}

@Composable
fun ScreenTemplate(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        content()
    }
}

@Composable
fun HomeScreen() {
    val stampViewModel: com.example.stampcollectionsapp.features.collection.presentation.viewmodel.StampViewModel = hiltViewModel()
    val recentStamps by stampViewModel.recentStamps.collectAsStateWithLifecycle()
    val isLoading by stampViewModel.isLoading.collectAsStateWithLifecycle()
    var selectedStampId by remember { mutableStateOf<Long?>(null) }

    // Если выбрана марка, показываем экран деталей
    if (selectedStampId != null) {
        val stamp = recentStamps.find { it.id == selectedStampId }
        if (stamp != null) {
            com.example.stampcollectionsapp.features.collection.presentation.screens.StampDetailScreen(
                stamp = stamp,
                onBack = { selectedStampId = null }
            )
            return
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Добро пожаловать",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Отслеживайте последние добавленные марки и управляйте коллекциями.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        if (isLoading && recentStamps.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (recentStamps.isEmpty()) {
            item {
                Text(
                    text = "Пока нет недавно добавленных марок. Добавьте первую марку в коллекцию, чтобы увидеть её здесь.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            item {
                Text(
                    text = "Последние добавленные марки",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            items(recentStamps) { stamp ->
                RecentStampCard(
                    stamp = stamp,
                    onClick = { selectedStampId = stamp.id }
                )
            }
        }
    }
}

@Composable
fun RecentStampCard(
    stamp: Stamp,
    onClick: () -> Unit
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
            if (!stamp.imageUrl.isNullOrBlank()) {
                val imageModel = try {
                    android.net.Uri.parse(stamp.imageUrl)
                } catch (e: Exception) {
                    stamp.imageUrl
                }

                Image(
                    painter = rememberAsyncImagePainter(model = imageModel),
                    contentDescription = "Марка",
                    modifier = Modifier.size(72.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stamp.name?.takeIf { it.isNotBlank() }
                        ?: "${stamp.country} ${stamp.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stamp.country,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Год: ${stamp.year}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!stamp.denomination.isNullOrBlank()) {
                    Text(
                        text = "Номинал: ${stamp.denomination}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (stamp.price != null) {
                    val priceText = String.format(Locale.US, "%.2f", stamp.price)
                    Text(
                        text = "Цена: $priceText ${stamp.currency ?: "EUR"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CollectionScreen(
    viewModel: com.example.stampcollectionsapp.features.collection.presentation.viewmodel.CollectionViewModel = viewModel(),
    collectorViewModel: com.example.stampcollectionsapp.features.collection.presentation.viewmodel.CollectorViewModel = viewModel()
) {
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val collectors by collectorViewModel.collectors.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var collectionName by remember { mutableStateOf("") }
    var collectionDescription by remember { mutableStateOf("") }
    var selectedCollectionId by remember { mutableStateOf<Long?>(null) }
    var collectionToDelete by remember { mutableStateOf<com.example.stampcollectionsapp.features.collection.data.model.Collection?>(null) }
    var selectedColor by remember { mutableStateOf<Long?>(null) }
    
    // Получаем или создаем коллекционера по умолчанию
    var defaultCollectorId by remember { mutableStateOf<Long?>(null) }
    
    LaunchedEffect(collectors) {
        if (collectors.isEmpty()) {
            // Создаем коллекционера по умолчанию
            val defaultCollector = com.example.stampcollectionsapp.features.collection.data.model.Collector(
                name = "Моя коллекция",
                email = "default@example.com"
            )
            collectorViewModel.addCollector(defaultCollector)
        } else {
            defaultCollectorId = collectors.first().id
        }
    }
    
    LaunchedEffect(collectors) {
        if (collectors.isNotEmpty() && defaultCollectorId == null) {
            defaultCollectorId = collectors.first().id
        }
    }
    
    // Если выбрана коллекция, показываем экран деталей
    if (selectedCollectionId != null) {
        CollectionDetailScreen(
            collectionId = selectedCollectionId!!,
            onBack = { selectedCollectionId = null }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Мои коллекции",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
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

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                if (collections.isEmpty()) {
        Text(
                        text = "У вас пока нет коллекций",
            style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(collections.size) { index ->
                            CollectionItem(
                                collection = collections[index],
                                onDelete = { collectionToDelete = collections[index] },
                                onClick = { selectedCollectionId = collections[index].id }
                            )
                        }
                    }
                }
            }
        }
        
        // Кнопка добавления коллекции
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Добавить коллекцию")
        }
    }
    
    // Диалог добавления коллекции
    if (showAddDialog) {
        AddCollectionDialog(
            collectionName = collectionName,
            collectionDescription = collectionDescription,
            selectedColor = selectedColor,
            onCollectionNameChange = { collectionName = it },
            onCollectionDescriptionChange = { collectionDescription = it },
            onColorChange = { selectedColor = it },
            onDismiss = {
                showAddDialog = false
                collectionName = ""
                collectionDescription = ""
                selectedColor = null
            },
            onConfirm = {
                if (collectionName.isNotBlank() && defaultCollectorId != null) {
                    val newCollection = com.example.stampcollectionsapp.features.collection.data.model.Collection(
                        collectionName = collectionName,
                        description = collectionDescription.ifBlank { null },
                        dateCreated = System.currentTimeMillis(),
                        collectorId = defaultCollectorId!!,
                        color = selectedColor
                    )
                    viewModel.addCollection(newCollection)
                    showAddDialog = false
                    collectionName = ""
                    collectionDescription = ""
                    selectedColor = null
                }
            },
            enabled = collectionName.isNotBlank() && defaultCollectorId != null
        )
    }
    
    // Диалог подтверждения удаления коллекции
    if (collectionToDelete != null) {
        AlertDialog(
            onDismissRequest = { collectionToDelete = null },
            title = { Text("Удалить коллекцию?") },
            text = {
                Column {
                    Text("Вы уверены, что хотите удалить коллекцию \"${collectionToDelete!!.collectionName}\"?")
                    Text(
                        text = "Все марки в этой коллекции также будут удалены.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCollection(collectionToDelete!!)
                        collectionToDelete = null
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { collectionToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun CollectionItem(
    collection: com.example.stampcollectionsapp.features.collection.data.model.Collection,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val backgroundColor = if (collection.color != null) {
        android.graphics.Color.valueOf(collection.color.toInt()).let { c ->
            Color(c.red(), c.green(), c.blue(), c.alpha())
        }
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    // Определяем яркость цвета для выбора цвета текста
    val isDarkBackground = backgroundColor.red * 0.299 + backgroundColor.green * 0.587 + backgroundColor.blue * 0.114 < 0.5
    val textColor = if (isDarkBackground) {
        Color.White
    } else {
        Color.Black
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = collection.collectionName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                if (collection.description != null) {
                    Text(
                        text = collection.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun AddCollectionDialog(
    collectionName: String,
    collectionDescription: String,
    selectedColor: Long?,
    onCollectionNameChange: (String) -> Unit,
    onCollectionDescriptionChange: (String) -> Unit,
    onColorChange: (Long?) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    enabled: Boolean
) {
    val colors = listOf(
        Color(0xFFF44336), // Red
        Color(0xFFE91E63), // Pink
        Color(0xFF9C27B0), // Purple
        Color(0xFF673AB7), // Deep Purple
        Color(0xFF3F51B5), // Indigo
        Color(0xFF2196F3), // Blue
        Color(0xFF03A9F4), // Light Blue
        Color(0xFF00BCD4), // Cyan
        Color(0xFF009688), // Teal
        Color(0xFF4CAF50), // Green
        Color(0xFF8BC34A), // Light Green
        Color(0xFFFFC107), // Amber
        Color(0xFFFF9800), // Orange
        Color(0xFFFF5722), // Deep Orange
        Color(0xFF795548), // Brown
        Color(0xFF9E9E9E), // Grey
        Color(0xFF607D8B), // Blue Grey
        Color(0xFF000000)  // Black
    )
    
    Dialog(onDismissRequest = onDismiss) {
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
                    text = "Создать коллекцию",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = collectionName,
                    onValueChange = onCollectionNameChange,
                    label = { Text("Название коллекции *") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = collectionDescription,
                    onValueChange = onCollectionDescriptionChange,
                    label = { Text("Описание") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    maxLines = 3
                )
                
                Text(
                    text = "Выберите цвет",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Параметр "Без цвета"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .border(
                                width = if (selectedColor == null) 3.dp else 1.dp,
                                color = if (selectedColor == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable { onColorChange(null) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedColor == null) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Без цвета",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Без цвета",
                                modifier = Modifier.size(24.dp)
                            )
                            }
                        }
                    }
                    
                    // Цвета в несколько рядов
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colors.take(9).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .border(
                                    width = if (selectedColor == color.toArgb().toLong()) 3.dp else 1.dp,
                                    color = if (selectedColor == color.toArgb().toLong()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .then(
                                    Modifier
                                        .background(color, CircleShape)
                                        .clickable { onColorChange(color.toArgb().toLong()) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == color.toArgb().toLong()) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Выбрано",
                                        tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                }
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colors.drop(9).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .border(
                                        width = if (selectedColor == color.toArgb().toLong()) 3.dp else 1.dp,
                                        color = if (selectedColor == color.toArgb().toLong()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .then(
                                        Modifier
                                            .background(color, CircleShape)
                                            .clickable { onColorChange(color.toArgb().toLong()) }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColor == color.toArgb().toLong()) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Выбрано",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        enabled = enabled
                    ) {
                        Text("Создать")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    stampViewModel: com.example.stampcollectionsapp.features.collection.presentation.viewmodel.StampViewModel = viewModel(),
    collectionViewModel: com.example.stampcollectionsapp.features.collection.presentation.viewmodel.CollectionViewModel = viewModel()
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var denomination by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("RUB") } // По умолчанию рубль
    var description by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var priceUrl by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showImagePicker by remember { mutableStateOf(false) }
    var showWebView by remember { mutableStateOf(false) }
    var expandedCurrencyDropdown by remember { mutableStateOf(false) }
    var expandedYearDropdown by remember { mutableStateOf(false) }
    var expandedConditionDropdown by remember { mutableStateOf(false) }
    
    val imagePicker = com.example.stampcollectionsapp.features.collection.presentation.utils.rememberImagePicker(
        onImageSelected = { uri ->
            imageUri = uri
        }
    )
    
    val collections by collectionViewModel.collections.collectAsStateWithLifecycle()
    var selectedCollectionId by remember { mutableStateOf<Long?>(null) }
    var expandedCollectionDropdown by remember { mutableStateOf(false) }
    
    val isStampAdded by stampViewModel.isStampAdded.collectAsStateWithLifecycle()
    
    // Автоматически выбираем первую коллекцию, если она есть
    LaunchedEffect(collections) {
        if (collections.isNotEmpty() && selectedCollectionId == null) {
            selectedCollectionId = collections.first().id
        }
    }
    
    // Очищаем поля после успешного добавления марки
    LaunchedEffect(isStampAdded) {
        if (isStampAdded) {
            name = ""
            country = ""
            year = ""
            denomination = ""
            currency = "RUB"
            description = ""
            condition = ""
            price = ""
            priceUrl = ""
            imageUri = null
            stampViewModel.clearStampAddedFlag()
        }
    }
    
    if (showImagePicker) {
        com.example.stampcollectionsapp.features.collection.presentation.components.ImagePickerDialog(
            onDismiss = { showImagePicker = false },
            imagePickerState = imagePicker
        )
    }
    
    if (showWebView) {
        com.example.stampcollectionsapp.features.collection.presentation.screens.StampWebViewScreen(
            url = "https://colnect.com/ru/stamps",
            onBack = { showWebView = false },
            onStampSelected = { stampData ->
                // Заполняем поля данными из сайта для редактирования
                // 1 - Страна
                country = stampData["country"] ?: ""
                // 2 - Название
                name = stampData["name"] ?: ""
                // 3 - Описание
                description = stampData["description"] ?: ""
                // 4 - Год
                year = stampData["year"] ?: ""
                // 5 - Номинал
                denomination = stampData["denomination"] ?: ""
                // 6 - Цена (только если есть значение)
                val priceValue = stampData["price"] ?: ""
                price = if (priceValue.isNotBlank() && priceValue != ".") priceValue else ""
                // Устанавливаем валюту для цены, только если цена указана
                if (price.isNotBlank()) {
                    val priceCurrency = stampData["currency"]
                    if (!priceCurrency.isNullOrBlank()) {
                        currency = when (priceCurrency.uppercase()) {
                            "EUR" -> "EUR"
                            "USD" -> "USD"
                            "RUB" -> "RUB"
                            else -> "EUR" // По умолчанию евро
                        }
                    }
                }
                priceUrl = stampData["priceUrl"] ?: stampData["detailUrl"] ?: ""
                // Обрабатываем изображение из URL
                val imageUrl = stampData["imageUrl"]
                if (!imageUrl.isNullOrBlank()) {
                    try {
                        imageUri = android.net.Uri.parse(imageUrl)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                // WebView закроется автоматически, не закрываем его здесь
            }
        )
        return
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Добавить новую марку",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Выбор коллекции
            if (collections.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = expandedCollectionDropdown,
                    onExpandedChange = { expandedCollectionDropdown = !expandedCollectionDropdown }
                ) {
                    OutlinedTextField(
                        value = collections.find { it.id == selectedCollectionId }?.collectionName ?: "Выберите коллекцию",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Коллекция *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCollectionDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCollectionDropdown,
                        onDismissRequest = { expandedCollectionDropdown = false }
                    ) {
                        collections.forEach { collection ->
                            DropdownMenuItem(
                                text = { Text(collection.collectionName) },
                                onClick = {
                                    selectedCollectionId = collection.id
                                    expandedCollectionDropdown = false
                                }
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Сначала создайте коллекцию",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showImagePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Фото")
                }
                
                Button(
                    onClick = { showWebView = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Language, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Сайт")
                }
            }
            
            if (imageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = imageUri),
                    contentDescription = "Изображение марки",
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
            
            // Поле года с возможностью выбора из списка
            ExposedDropdownMenuBox(
                expanded = expandedYearDropdown,
                onExpandedChange = { expandedYearDropdown = !expandedYearDropdown }
            ) {
                OutlinedTextField(
                    value = year,
                    onValueChange = { newValue ->
                        // Разрешаем только цифры
                        if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                            year = newValue
                        }
                    },
                    label = { Text("Год *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedYearDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedYearDropdown,
                    onDismissRequest = { expandedYearDropdown = false }
                ) {
                    // Генерируем список годов (от текущего года до 1800)
                    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                    (currentYear downTo 1800).forEach { y ->
                        DropdownMenuItem(
                            text = { Text(y.toString()) },
                            onClick = {
                                year = y.toString()
                                expandedYearDropdown = false
                            }
                        )
                    }
                }
            }
            
            // Поле номинала (строка со знаком валюты)
            OutlinedTextField(
                value = denomination,
                onValueChange = { denomination = it },
                label = { Text("Номинал") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Например: 1,10 $ - Австралийский доллар") }
            )
            
            // Поле цены с выбором валюты
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = price,
                    onValueChange = { newValue ->
                        // Разрешаем только числа (целые или плавающие) с точностью до сотых
                        val regex = Regex("^\\d*\\.?\\d{0,2}$")
                        if (newValue.isEmpty() || regex.matches(newValue)) {
                            price = newValue
                        }
                    },
                    label = { Text("Цена") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    placeholder = { Text("Например: 12.97") }
                )
                
                ExposedDropdownMenuBox(
                    expanded = expandedCurrencyDropdown,
                    onExpandedChange = { expandedCurrencyDropdown = !expandedCurrencyDropdown }
                ) {
                    OutlinedTextField(
                        value = when (currency) {
                            "EUR" -> "€"
                            "USD" -> "$"
                            "RUB" -> "₽"
                            else -> currency
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Валюта") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCurrencyDropdown) },
                        modifier = Modifier
                            .width(80.dp)
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCurrencyDropdown,
                        onDismissRequest = { expandedCurrencyDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("€ EUR") },
                            onClick = {
                                currency = "EUR"
                                expandedCurrencyDropdown = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("$ USD") },
                            onClick = {
                                currency = "USD"
                                expandedCurrencyDropdown = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("₽ RUB") },
                            onClick = {
                                currency = "RUB"
                                expandedCurrencyDropdown = false
                            }
                        )
                    }
                }
            }
            
            // Поле со ссылкой на страницу марки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = priceUrl,
                    onValueChange = { priceUrl = it },
                    label = { Text("Цена (ссылка на страницу)") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("https://colnect.com/ru/stamps/stamp/...") }
                )
                if (priceUrl.isNotBlank()) {
                    IconButton(
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                                intent.data = android.net.Uri.parse(priceUrl)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Link, contentDescription = "Открыть ссылку")
                    }
                }
            }
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            
            // Поле состояния с выпадающим списком
            ExposedDropdownMenuBox(
                expanded = expandedConditionDropdown,
                onExpandedChange = { expandedConditionDropdown = !expandedConditionDropdown }
            ) {
                OutlinedTextField(
                    value = condition,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Состояние") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedConditionDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedConditionDropdown,
                    onDismissRequest = { expandedConditionDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Гашёная") },
                        onClick = {
                            condition = "Гашёная"
                            expandedConditionDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Неиспользованная") },
                        onClick = {
                            condition = "Неиспользованная"
                            expandedConditionDropdown = false
                        }
                    )
                }
            }
        }
        
        Button(
            onClick = {
                try {
                    val collectionId = selectedCollectionId
                    if (country.isNotBlank() && year.isNotBlank() && collectionId != null) {
                        // Валидация года
                        val yearValue = year.toIntOrNull()
                        if (yearValue == null || yearValue <= 0) {
                            // Можно показать сообщение об ошибке
                            return@Button
                        }
                        
                        // Валюта всегда относится к цене
                        val stampCurrency = currency.takeIf { price.isNotBlank() }
                        
                        val stamp = com.example.stampcollectionsapp.features.collection.data.model.Stamp(
                            name = name.ifBlank { null },
                            country = country.trim(),
                            year = yearValue,
                            denomination = denomination.ifBlank { null }, // Номинал как строка
                            currency = stampCurrency, // Валюта для цены
                            description = description.ifBlank { null },
                            condition = condition.ifBlank { null },
                            imageUrl = imageUri?.toString(),
                            price = price.toDoubleOrNull(),
                            priceUrl = priceUrl.ifBlank { null }
                        )
                        stampViewModel.addStampToCollection(stamp, collectionId)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Обработка ошибки - можно показать сообщение пользователю
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedCollectionId != null && country.isNotBlank() && year.isNotBlank()
        ) {
            Text("Добавить марку")
        }
    }
}

@Composable
fun FavoritesScreen() {
    val stampViewModel: com.example.stampcollectionsapp.features.collection.presentation.viewmodel.StampViewModel = hiltViewModel()
    val favoriteStamps by stampViewModel.favoriteStamps.collectAsStateWithLifecycle()
    val isLoading by stampViewModel.isLoading.collectAsStateWithLifecycle()
    var selectedStampId by remember { mutableStateOf<Long?>(null) }
    
    // Загружаем избранные марки
    LaunchedEffect(Unit) {
        stampViewModel.loadFavoriteStamps()
    }
    
    // Если выбрана марка, показываем экран деталей
    if (selectedStampId != null) {
        val stamp = favoriteStamps.find { it.id == selectedStampId }
        if (stamp != null) {
            com.example.stampcollectionsapp.features.collection.presentation.screens.StampDetailScreen(
                stamp = stamp,
                onBack = { selectedStampId = null }
            )
            return
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Избранное",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (isLoading && favoriteStamps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (favoriteStamps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "У вас пока нет избранных марок",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(favoriteStamps) { stamp ->
                    RecentStampCard(
                        stamp = stamp,
                        onClick = { selectedStampId = stamp.id }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    collectionViewModel: com.example.stampcollectionsapp.features.collection.presentation.viewmodel.CollectionViewModel = hiltViewModel(),
    stampViewModel: com.example.stampcollectionsapp.features.collection.presentation.viewmodel.StampViewModel = hiltViewModel(),
    priceViewModel: com.example.stampcollectionsapp.features.collection.presentation.viewmodel.PriceViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val user = (authState as? AuthState.Authenticated)?.user
    val collections by collectionViewModel.collections.collectAsStateWithLifecycle()
    val stamps by stampViewModel.stamps.collectAsStateWithLifecycle()
    val allCollectionPrices by priceViewModel.collectionPrices.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showExportDialog by remember { mutableStateOf(false) }
    var showImagePicker by remember { mutableStateOf(false) }
    var showSaveSuccess by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var isDeletingAccount by remember { mutableStateOf(false) }
    
    // Редактируемые поля
    var firstName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var userPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    // Исходные значения для отслеживания изменений
    var originalFirstName by remember { mutableStateOf("") }
    var originalMiddleName by remember { mutableStateOf("") }
    var originalLastName by remember { mutableStateOf("") }
    var originalPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    // Флаг для отслеживания, была ли выполнена инициализация
    var isInitialized by remember { mutableStateOf(false) }
    
    // Инициализация полей из пользователя (Имя, Фамилия, Отчество)
    LaunchedEffect(user?.id, user?.displayName, user?.photoUrl) {
        if (user != null) {
            // Проверяем, изменились ли данные в Firebase
            val currentFullName = listOf(firstName, lastName, middleName)
                .filter { it.isNotBlank() }
                .joinToString(" ")
            val firebaseFullName = user.displayName ?: ""
            
            val currentPhotoUrl = userPhotoUri?.toString() ?: ""
            val firebasePhotoUrl = user.photoUrl ?: ""
            
            // Инициализируем только при первой загрузке
            if (!isInitialized) {
                val nameParts = firebaseFullName.split(" ").filter { it.isNotBlank() }
                // Предполагаем формат: Имя Фамилия Отчество
                when (nameParts.size) {
                    1 -> {
                        firstName = nameParts[0]
                        lastName = ""
                        middleName = ""
                    }
                    2 -> {
                        // Если 2 части, первая - имя, вторая - фамилия
                        firstName = nameParts[0]
                        lastName = nameParts[1]
                        middleName = ""
                    }
                    else -> {
                        // Если 3+ части, первая - имя, вторая - фамилия, остальное - отчество
                        firstName = nameParts[0]
                        lastName = nameParts.getOrNull(1) ?: ""
                        middleName = nameParts.drop(2).joinToString(" ")
                    }
                }
                originalFirstName = firstName
                originalLastName = lastName
                originalMiddleName = middleName
                
                // Загружаем фото: сначала из Firebase, если нет - из локального хранилища
                var photoUri: android.net.Uri? = null
                if (firebasePhotoUrl.isNotBlank()) {
                    // Если это HTTP/HTTPS URL, используем его
                    if (firebasePhotoUrl.startsWith("http://") || firebasePhotoUrl.startsWith("https://")) {
                        photoUri = android.net.Uri.parse(firebasePhotoUrl)
                    } else {
                        // Если это локальный URI, используем его
                        photoUri = android.net.Uri.parse(firebasePhotoUrl)
                    }
                } else {
                    // Если фото нет в Firebase, проверяем локальное хранилище
                    val userId = user?.id
                    if (userId != null) {
                        val imagesDir = context.getExternalFilesDir("profile_images")
                        val profileImageFile = File(imagesDir, "$userId.jpg")
                        if (profileImageFile.exists()) {
                            photoUri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                profileImageFile
                            )
                        }
                    }
                }
                
                userPhotoUri = photoUri
                originalPhotoUri = photoUri
                
                isInitialized = true
            } 
            // Если данные в Firebase изменились и отличаются от исходных, обновляем
            else {
                val originalFullName = listOf(originalFirstName, originalLastName, originalMiddleName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                val originalPhotoUrl = originalPhotoUri?.toString() ?: ""
                
                // Обновляем только если данные в Firebase отличаются от исходных
                // и текущие значения совпадают с исходными (нет незавершенных изменений)
                val hasUnsavedChanges = firstName != originalFirstName ||
                        lastName != originalLastName ||
                        middleName != originalMiddleName ||
                        userPhotoUri != originalPhotoUri
                
                if ((firebaseFullName != originalFullName || firebasePhotoUrl != originalPhotoUrl) &&
                    !hasUnsavedChanges) {
                    val nameParts = firebaseFullName.split(" ").filter { it.isNotBlank() }
                    when (nameParts.size) {
                        1 -> {
                            firstName = nameParts[0]
                            lastName = ""
                            middleName = ""
                        }
                        2 -> {
                            firstName = nameParts[0]
                            lastName = nameParts[1]
                            middleName = ""
                        }
                        else -> {
                            firstName = nameParts[0]
                            lastName = nameParts.getOrNull(1) ?: ""
                            middleName = nameParts.drop(2).joinToString(" ")
                        }
                    }
                    originalFirstName = firstName
                    originalLastName = lastName
                    originalMiddleName = middleName
                    
                    // Загружаем фото: сначала из Firebase, если нет - из локального хранилища
                    var photoUri: android.net.Uri? = null
                    if (firebasePhotoUrl.isNotBlank()) {
                        // Если это HTTP/HTTPS URL, используем его
                        if (firebasePhotoUrl.startsWith("http://") || firebasePhotoUrl.startsWith("https://")) {
                            photoUri = android.net.Uri.parse(firebasePhotoUrl)
                        } else {
                            // Если это локальный URI, используем его
                            photoUri = android.net.Uri.parse(firebasePhotoUrl)
                        }
                    } else {
                        // Если фото нет в Firebase, проверяем локальное хранилище
                        val userId = user?.id
                        if (userId != null) {
                            val imagesDir = context.getExternalFilesDir("profile_images")
                            val profileImageFile = File(imagesDir, "$userId.jpg")
                            if (profileImageFile.exists()) {
                                photoUri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    profileImageFile
                                )
                            }
                        }
                    }
                    
                    userPhotoUri = photoUri
                    originalPhotoUri = photoUri
                }
            }
        }
    }
    
    // Проверяем, были ли изменения
    val hasChanges = remember(firstName, middleName, lastName, userPhotoUri, originalFirstName, originalMiddleName, originalLastName, originalPhotoUri) {
        firstName != originalFirstName ||
        middleName != originalMiddleName ||
        lastName != originalLastName ||
        userPhotoUri != originalPhotoUri
    }
    
    val imagePicker = com.example.stampcollectionsapp.features.collection.presentation.utils.rememberImagePicker(
        onImageSelected = { uri ->
            userPhotoUri = uri
        }
    )
    
    // Загружаем график изменения цены всех коллекций
    LaunchedEffect(stamps) {
        if (stamps.isNotEmpty()) {
            val allStampIds = stamps.map { it.id }
            priceViewModel.loadCollectionPricesOverTime(allStampIds)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Фото и ФИО
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Фото пользователя
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showImagePicker = true },
                contentAlignment = Alignment.Center
            ) {
                if (userPhotoUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = userPhotoUri),
                        contentDescription = "Фото пользователя",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Загрузить фото",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Поля для редактирования ФИО (Имя, Фамилия, Отчество)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("Имя") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Фамилия") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = middleName,
                    onValueChange = { middleName = it },
                    label = { Text("Отчество") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
        
        // Статистика
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
        Text(
                        text = "${collections.size}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Коллекций",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${stamps.size}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Марок",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        
        // График изменения цены всех коллекций
        if (allCollectionPrices.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Изменение стоимости всех коллекций",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    com.example.stampcollectionsapp.features.collection.presentation.components.CollectionPriceChart(
                        prices = allCollectionPrices,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }
        }
        
        // Кнопка экспорта PDF
        Button(
            onClick = { showExportDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Экспортировать все коллекции в PDF")
        }
        
        // Кнопка сохранения профиля
        Button(
            onClick = {
                scope.launch {
                    // Формируем ФИО в правильном порядке: Имя Фамилия Отчество
                    val fullName = listOf(firstName, lastName, middleName)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                        .ifBlank { user?.displayName ?: "" }
                    
                    // Загружаем изображение в Firebase Storage, если это локальный URI
                    var photoUrlString: String? = null
                    val photoUriString = userPhotoUri?.toString() ?: ""
                    
                    if (photoUriString.isNotBlank()) {
                        if (photoUriString.startsWith("http://") || photoUriString.startsWith("https://")) {
                            // Уже URL, используем как есть
                            photoUrlString = photoUriString
                        } else {
                            // Локальный URI - сохраняем локально и используем локальный путь
                            try {
                                android.util.Log.d("ProfileSave", "Сохраняем изображение локально: $photoUriString")
                                
                                // Проверяем, что пользователь аутентифицирован
                                val currentUser = Firebase.auth.currentUser
                                if (currentUser == null) {
                                    android.util.Log.e("ProfileSave", "Пользователь не аутентифицирован")
                                    android.widget.Toast.makeText(
                                        context,
                                        "Ошибка: пользователь не аутентифицирован",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                    return@launch
                                }
                                
                                val userId = currentUser.uid
                                
                                // Сохраняем изображение в локальное хранилище приложения
                                val imagesDir = context.getExternalFilesDir("profile_images")
                                if (imagesDir != null && !imagesDir.exists()) {
                                    imagesDir.mkdirs()
                                }
                                
                                val profileImageFile = File(imagesDir, "$userId.jpg")
                                
                                // Копируем изображение из URI в локальный файл
                                context.contentResolver.openInputStream(userPhotoUri!!)?.use { inputStream ->
                                    profileImageFile.outputStream().use { outputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                                    
                                    // Создаем FileProvider URI для локального файла
                                    val fileProviderUri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        profileImageFile
                                    )
                                    
                                    // Используем локальный URI
                                    photoUrlString = fileProviderUri.toString()
                                    android.util.Log.d("ProfileSave", "Изображение сохранено локально: $photoUrlString")
                                } ?: run {
                                    android.util.Log.e("ProfileSave", "Не удалось открыть InputStream для URI: $userPhotoUri")
                                    android.widget.Toast.makeText(
                                        context,
                                        "Не удалось открыть изображение",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            } catch (e: SecurityException) {
                                android.util.Log.e("ProfileSave", "SecurityException: ${e.message}")
                                android.widget.Toast.makeText(
                                    context,
                                    "Нет доступа к изображению. Проверьте разрешения приложения",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                e.printStackTrace()
                            } catch (e: Exception) {
                                android.util.Log.e("ProfileSave", "Exception при сохранении изображения: ${e.javaClass.simpleName}")
                                android.util.Log.e("ProfileSave", "Exception message: ${e.message}")
                                android.util.Log.e("ProfileSave", "Exception stack trace:", e)
                                android.widget.Toast.makeText(
                                    context,
                                    "Ошибка при сохранении изображения: ${e.message ?: "Неизвестная ошибка"}",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                e.printStackTrace()
                            }
                        }
                    }
                    
                    // Сохраняем профиль (даже если загрузка изображения не удалась, сохраняем ФИО)
                    val result = authViewModel.updateProfile(
                        fullName,
                        photoUrlString
                    )
                    
                    if (result.isSuccess) {
                        // Обновляем исходные значения после успешного сохранения
                        originalFirstName = firstName
                        originalLastName = lastName
                        originalMiddleName = middleName
                        // Обновляем photoUri на URL из Firebase, если он был загружен
                        if (photoUrlString != null) {
                            originalPhotoUri = android.net.Uri.parse(photoUrlString)
                            userPhotoUri = originalPhotoUri
                        } else {
                            // Если изображение не было загружено, но было выбрано, оставляем локальный URI
                            // Если изображение не было выбрано, оставляем как есть
                            originalPhotoUri = userPhotoUri
                        }
                        showSaveSuccess = true
                    } else {
                        // Показываем ошибку, если сохранение не удалось
                        android.widget.Toast.makeText(
                            context,
                            "Ошибка при сохранении профиля: ${result.exceptionOrNull()?.message ?: "Неизвестная ошибка"}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = hasChanges
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Сохранить профиль")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Кнопка удаления аккаунта
        Button(
            onClick = { showDeleteAccountDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Удалить аккаунт")
        }
    }
    
    // Диалог подтверждения удаления аккаунта
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { 
                if (!isDeletingAccount) {
                    showDeleteAccountDialog = false
                }
            },
            title = { Text("Удаление аккаунта") },
            text = { 
                Text(
                    "Вы уверены, что хотите удалить свой аккаунт?\n\n" +
                    "Это действие нельзя отменить. Все ваши данные будут безвозвратно удалены."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isDeletingAccount = true
                            val result = authViewModel.deleteAccount()
                            isDeletingAccount = false
                            showDeleteAccountDialog = false
                            
                            if (result.isSuccess) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Аккаунт успешно удалён",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                // Пользователь автоматически разлогинится через AuthStateListener
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    "Ошибка при удалении аккаунта: ${result.exceptionOrNull()?.message ?: "Неизвестная ошибка"}",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    enabled = !isDeletingAccount,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    if (isDeletingAccount) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onError
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAccountDialog = false },
                    enabled = !isDeletingAccount
                ) {
                    Text("Отмена")
                }
            }
        )
    }
    
    if (showImagePicker) {
        com.example.stampcollectionsapp.features.collection.presentation.components.ImagePickerDialog(
            onDismiss = { showImagePicker = false },
            imagePickerState = imagePicker
        )
    }
    
    if (showExportDialog) {
        val collectionsList = collections
        var allStampsMap by remember { mutableStateOf<Map<Long, List<Stamp>>>(emptyMap()) }
        
        LaunchedEffect(collectionsList) {
            if (collectionsList.isNotEmpty()) {
                val stampsMap = mutableMapOf<Long, List<Stamp>>()
                collectionsList.forEach { collection ->
                    val collectionWithStamps = collectionViewModel.getCollectionWithStamps(collection.id)
                    stampsMap[collection.id] = collectionWithStamps?.stamps?.map { it.stamp } ?: emptyList()
                }
                allStampsMap = stampsMap
            }
        }
        
        com.example.stampcollectionsapp.features.collection.presentation.components.ExportPdfDialog(
            collection = null,
            stamps = stamps,
            allCollections = collectionsList,
            allStamps = allStampsMap,
            allCollectionPrices = allCollectionPrices,
            onDismiss = { showExportDialog = false },
            onExport = { showExportDialog = false }
        )
    }
    
    if (showSaveSuccess) {
        AlertDialog(
            onDismissRequest = { showSaveSuccess = false },
            title = { Text("Успешно") },
            text = { Text("Профиль сохранён") },
            confirmButton = {
                TextButton(onClick = { showSaveSuccess = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun StampCollectionsAPPApp(
    authViewModel: AuthViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                // Переключаем на Home только если еще не выбрали другой экран
                if (currentScreen == Screen.Login || currentScreen == Screen.Register) {
                currentScreen = Screen.Home
                }
            }
            is AuthState.Unauthenticated -> {
                currentScreen = Screen.Login
            }
            else -> {}
        }
    }
    
    when (authState) {
        is AuthState.Authenticated -> {
        MainScreen(
            currentScreen = currentScreen,
            onScreenSelected = { screen -> currentScreen = screen },
            onLogout = { 
                    authViewModel.signOut()
                }
            )
        }
        else -> {
            when (currentScreen) {
                is Screen.Login -> {
                    LoginScreen(
                        onNavigateToRegister = { currentScreen = Screen.Register },
                        onLoginSuccess = { /* AuthState listener обработает */ }
                    )
                }
                is Screen.Register -> {
                    RegisterScreen(
                        onBackClick = { currentScreen = Screen.Login },
                        onRegisterSuccess = { /* AuthState listener обработает */ }
                    )
                }
                else -> {
                    LoginScreen(
                        onNavigateToRegister = { currentScreen = Screen.Register },
                        onLoginSuccess = { /* AuthState listener обработает */ }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AuthPreview() {
    StampCollectionsAPPTheme {
        LoginScreen(
            onNavigateToRegister = {},
            onLoginSuccess = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainAppPreview() {
    StampCollectionsAPPTheme {
        MainScreen(
            currentScreen = Screen.Home,
            onScreenSelected = {},
            onLogout = {}
        )
    }
}

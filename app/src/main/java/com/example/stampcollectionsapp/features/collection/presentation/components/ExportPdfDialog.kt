package com.example.stampcollectionsapp.features.collection.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.stampcollectionsapp.features.collection.data.model.Collection
import com.example.stampcollectionsapp.features.collection.data.model.Stamp
import com.example.stampcollectionsapp.features.collection.domain.service.PdfExportService
import com.example.stampcollectionsapp.features.auth.domain.model.User
import com.example.stampcollectionsapp.features.auth.presentation.viewmodel.AuthViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import kotlinx.coroutines.launch

@Composable
fun ExportPdfDialog(
    collection: Collection?,
    stamps: List<Stamp>,
    prices: Map<Long, List<com.example.stampcollectionsapp.features.collection.data.model.StampPrice>> = emptyMap(),
    collectionPrices: List<com.example.stampcollectionsapp.features.collection.data.model.CollectionPrice> = emptyList(),
    allCollections: List<Collection>? = null,
    allStamps: Map<Long, List<Stamp>>? = null,
    allCollectionPrices: List<com.example.stampcollectionsapp.features.collection.data.model.CollectionPrice> = emptyList(),
    onDismiss: () -> Unit,
    onExport: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    val pdfService = PdfExportService(context)
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val user = (authState as? com.example.stampcollectionsapp.features.auth.presentation.state.AuthState.Authenticated)?.user
    
    AlertDialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        title = { Text("Экспорт в PDF") },
        text = {
            Column {
                Text(
                    when {
                        allCollections != null && allStamps != null -> {
                            "Экспортировать все коллекции (${allCollections.size}) в PDF?"
                        }
                        collection != null -> {
                            "Экспортировать коллекцию \"${collection.collectionName}\" с ${stamps.size} марками?"
                        }
                        else -> {
                            "Экспортировать марку в PDF?"
                        }
                    }
                )
                if (isExporting) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        isExporting = true
                        try {
                            val file = when {
                                allCollections != null && allStamps != null -> {
                                    pdfService.exportAllCollectionsToPdf(
                                        allCollections,
                                        allStamps,
                                        allCollectionPrices,
                                        user
                                    )
                                }
                                collection != null -> {
                                    pdfService.exportCollectionToPdf(collection, stamps, collectionPrices, user)
                                }
                                else -> {
                                    val stampPrices = prices[stamps.first().id] ?: emptyList()
                                    pdfService.exportStampToPdf(stamps.first(), stampPrices, user)
                                }
                            }
                            val filePath = file.absolutePath
                            Toast.makeText(
                                context, 
                                "PDF сохранён: $filePath", 
                                Toast.LENGTH_LONG
                            ).show()
                            onExport()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Ошибка при создании PDF: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isExporting = false
                        }
                    }
                },
                enabled = !isExporting
            ) {
                Text("Экспортировать")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isExporting
            ) {
                Text("Отмена")
            }
        }
    )
}


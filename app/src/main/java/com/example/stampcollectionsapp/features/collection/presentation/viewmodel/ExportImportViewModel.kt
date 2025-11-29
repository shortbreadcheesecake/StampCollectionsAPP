package com.example.stampcollectionsapp.features.collection.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stampcollectionsapp.features.collection.domain.repository.ExportImportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class ExportImportViewModel @Inject constructor(
    private val exportImportRepository: ExportImportRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    fun exportCollectorToJson(collectorId: Long, file: File) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val json = exportImportRepository.exportCollectorToJson(collectorId)
                file.writeText(json)
                _successMessage.value = "Коллекционер успешно экспортирован"
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка при экспорте: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun exportCollectionToJson(collectionId: Long, file: File) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val json = exportImportRepository.exportCollectionToJson(collectionId)
                file.writeText(json)
                _successMessage.value = "Коллекция успешно экспортирована"
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка при экспорте: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun exportCollectorToCsv(collectorId: Long, file: File) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val csv = exportImportRepository.exportCollectorToCsv(collectorId)
                file.writeText(csv)
                _successMessage.value = "Коллекционер успешно экспортирован"
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка при экспорте: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun exportCollectionToCsv(collectionId: Long, file: File) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val csv = exportImportRepository.exportCollectionToCsv(collectionId)
                file.writeText(csv)
                _successMessage.value = "Коллекция успешно экспортирована"
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка при экспорте: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun importCollectorFromJson(file: File): Long? {
        var result: Long? = null
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val json = file.readText()
                result = exportImportRepository.importCollectorFromJson(json)
                if (result != null) {
                    _successMessage.value = "Коллекционер успешно импортирован"
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "Ошибка при импорте: неверный формат данных"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка при импорте: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
        return result
    }
    
    fun importCollectionFromJson(file: File, collectorId: Long): Long? {
        var result: Long? = null
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val json = file.readText()
                result = exportImportRepository.importCollectionFromJson(json, collectorId)
                if (result != null) {
                    _successMessage.value = "Коллекция успешно импортирована"
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "Ошибка при импорте: неверный формат данных"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка при импорте: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
        return result
    }
    
    fun importCollectorFromCsv(file: File): Long? {
        var result: Long? = null
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val csv = file.readText()
                result = exportImportRepository.importCollectorFromCsv(csv)
                if (result != null) {
                    _successMessage.value = "Коллекционер успешно импортирован"
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "Ошибка при импорте: неверный формат данных"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка при импорте: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
        return result
    }
    
    fun importCollectionFromCsv(file: File, collectorId: Long): Long? {
        var result: Long? = null
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val csv = file.readText()
                result = exportImportRepository.importCollectionFromCsv(csv, collectorId)
                if (result != null) {
                    _successMessage.value = "Коллекция успешно импортирована"
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "Ошибка при импорте: неверный формат данных"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка при импорте: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
        return result
    }
    
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}


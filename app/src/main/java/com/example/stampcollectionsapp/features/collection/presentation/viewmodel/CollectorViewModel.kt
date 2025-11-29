package com.example.stampcollectionsapp.features.collection.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stampcollectionsapp.features.collection.data.model.Collector
import com.example.stampcollectionsapp.features.collection.domain.repository.CollectorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectorViewModel @Inject constructor(
    private val collectorRepository: CollectorRepository
) : ViewModel() {
    
    private val _collectors = MutableStateFlow<List<Collector>>(emptyList())
    val collectors: StateFlow<List<Collector>> = _collectors.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        loadCollectors()
    }
    
    fun loadCollectors() {
        viewModelScope.launch {
            collectorRepository.getAllCollectors().collect { collectors ->
                _collectors.value = collectors
            }
        }
    }
    
    fun addCollector(collector: Collector) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                collectorRepository.insertCollector(collector)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка при добавлении коллекционера: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateCollector(collector: Collector) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                collectorRepository.updateCollector(collector)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка при обновлении коллекционера: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteCollector(collector: Collector) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                collectorRepository.deleteCollector(collector)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка при удалении коллекционера: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}


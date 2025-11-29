package com.example.stampcollectionsapp.features.collection.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stampcollectionsapp.features.collection.data.model.Collection
import com.example.stampcollectionsapp.features.collection.domain.repository.CollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository
) : ViewModel() {
    
    private val _collections = MutableStateFlow<List<Collection>>(emptyList())
    val collections: StateFlow<List<Collection>> = _collections.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        loadCollections()
    }
    
    fun loadCollections() {
        viewModelScope.launch {
            collectionRepository.getAllCollections().collect { collections ->
                _collections.value = collections
            }
        }
    }
    
    fun getCollectionsByCollectorId(collectorId: Long) {
        viewModelScope.launch {
            collectionRepository.getCollectionsByCollectorId(collectorId).collect { collections ->
                _collections.value = collections
            }
        }
    }
    
    fun addCollection(collection: Collection) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                collectionRepository.insertCollection(collection)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка при добавлении коллекции: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateCollection(collection: Collection) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                collectionRepository.updateCollection(collection)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка при обновлении коллекции: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteCollection(collection: Collection) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                collectionRepository.deleteCollection(collection)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка при удалении коллекции: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    suspend fun getCollectionWithStamps(collectionId: Long): com.example.stampcollectionsapp.features.collection.data.model.CollectionWithStamps? {
        return collectionRepository.getCollectionWithStamps(collectionId)
    }
}


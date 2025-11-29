package com.example.stampcollectionsapp.features.collection.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stampcollectionsapp.features.collection.data.model.CollectionStamp
import com.example.stampcollectionsapp.features.collection.data.model.Stamp
import com.example.stampcollectionsapp.features.collection.data.model.StampPrice
import com.example.stampcollectionsapp.features.collection.domain.repository.CollectionStampRepository
import com.example.stampcollectionsapp.features.collection.domain.repository.StampPriceRepository
import com.example.stampcollectionsapp.features.collection.domain.repository.StampRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class StampViewModel @Inject constructor(
    private val stampRepository: StampRepository,
    private val collectionStampRepository: CollectionStampRepository,
    private val priceRepository: StampPriceRepository
) : ViewModel() {
    
    private val _stamps = MutableStateFlow<List<Stamp>>(emptyList())
    val stamps: StateFlow<List<Stamp>> = _stamps.asStateFlow()
    
    private val _recentStamps = MutableStateFlow<List<Stamp>>(emptyList())
    val recentStamps: StateFlow<List<Stamp>> = _recentStamps.asStateFlow()
    
    private val _favoriteStamps = MutableStateFlow<List<Stamp>>(emptyList())
    val favoriteStamps: StateFlow<List<Stamp>> = _favoriteStamps.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _isStampAdded = MutableStateFlow(false)
    val isStampAdded: StateFlow<Boolean> = _isStampAdded.asStateFlow()
    
    init {
        loadStamps()
        loadRecentStamps()
        loadFavoriteStamps()
    }
    
    fun loadStamps() {
        viewModelScope.launch {
            stampRepository.getAllStamps().collect { stamps ->
                _stamps.value = stamps
            }
        }
    }
    
    fun loadRecentStamps(limit: Int = 10) {
        viewModelScope.launch {
            stampRepository.getRecentStamps(limit).collect { stamps ->
                _recentStamps.value = stamps
            }
        }
    }
    
    fun loadFavoriteStamps() {
        viewModelScope.launch {
            stampRepository.getFavoriteStamps().collect { stamps ->
                _favoriteStamps.value = stamps
            }
        }
    }
    
    fun addStamp(stamp: Stamp) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                stampRepository.insertStamp(stamp)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка при добавлении марки: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun addStampToCollection(stamp: Stamp, collectionId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                // Валидация данных перед сохранением
                if (stamp.country.isBlank()) {
                    _errorMessage.value = "Страна обязательна для заполнения"
                    return@launch
                }
                if (stamp.year <= 0) {
                    _errorMessage.value = "Год должен быть указан"
                    return@launch
                }
                
                // Сначала добавляем марку
                val stampId = stampRepository.insertStamp(stamp)
                
                // Если у марки есть цена, сохраняем её в историю цен
                if (stamp.price != null && stamp.price > 0) {
                    try {
                        val stampPrice = StampPrice(
                            stampId = stampId,
                            price = stamp.price,
                            currency = stamp.currency ?: "EUR",
                            date = System.currentTimeMillis(),
                            source = "manual"
                        )
                        priceRepository.insertPrice(stampPrice)
                    } catch (e: Exception) {
                        // Логируем ошибку, но не прерываем сохранение марки
                        e.printStackTrace()
                    }
                }
                
                // Затем связываем марку с коллекцией
                val collectionStamp = CollectionStamp(
                    collectionId = collectionId,
                    stampId = stampId,
                    acquisitionDate = System.currentTimeMillis()
                )
                collectionStampRepository.insertCollectionStamp(collectionStamp)
                _errorMessage.value = null
                _isStampAdded.value = true
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Ошибка при добавлении марки в коллекцию: ${e.message ?: "Неизвестная ошибка"}"
                _isStampAdded.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearStampAddedFlag() {
        _isStampAdded.value = false
    }
    
    suspend fun updateStamp(stamp: Stamp): Boolean {
        _isLoading.value = true
        return try {
            withContext(Dispatchers.IO) {
                // Получаем старую марку для сравнения цены
                val oldStamp = stampRepository.getStampById(stamp.id)
                
                // Обновляем марку
                stampRepository.updateStamp(stamp)
                
                // Если цена изменилась, добавляем запись в историю цен
                if (stamp.price != null && stamp.price > 0) {
                    val priceChanged = oldStamp == null || oldStamp.price != stamp.price
                    if (priceChanged) {
                        try {
                            val stampPrice = StampPrice(
                                stampId = stamp.id,
                                price = stamp.price,
                                currency = stamp.currency ?: "EUR",
                                date = System.currentTimeMillis(),
                                source = "manual"
                            )
                            priceRepository.insertPrice(stampPrice)
                        } catch (e: Exception) {
                            // Логируем ошибку, но не прерываем обновление марки
                            e.printStackTrace()
                        }
                    }
                }
            }
            _errorMessage.value = null
            true
        } catch (e: Exception) {
            _errorMessage.value = "Ошибка при обновлении марки: ${e.message}"
            false
        } finally {
            _isLoading.value = false
        }
    }
    
    suspend fun deleteStamp(stamp: Stamp): Boolean {
        _isLoading.value = true
        return try {
            withContext(Dispatchers.IO) {
                stampRepository.deleteStamp(stamp)
            }
            _errorMessage.value = null
            true
        } catch (e: Exception) {
            _errorMessage.value = "Ошибка при удалении марки: ${e.message}"
            false
        } finally {
            _isLoading.value = false
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun toggleFavorite(stampId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                stampRepository.toggleFavorite(stampId, isFavorite)
                // Обновляем список избранных
                loadFavoriteStamps()
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка при обновлении избранного: ${e.message}"
            }
        }
    }
}


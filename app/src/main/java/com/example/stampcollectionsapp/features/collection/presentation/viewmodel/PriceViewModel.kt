package com.example.stampcollectionsapp.features.collection.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stampcollectionsapp.features.collection.data.model.Stamp
import com.example.stampcollectionsapp.features.collection.data.model.StampPrice
import com.example.stampcollectionsapp.features.collection.domain.repository.StampPriceRepository
import com.example.stampcollectionsapp.features.collection.domain.repository.StampRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class PriceViewModel @Inject constructor(
    private val priceRepository: StampPriceRepository,
    private val stampRepository: StampRepository
) : ViewModel() {
    
    private val _prices = MutableStateFlow<List<StampPrice>>(emptyList())
    val prices: StateFlow<List<StampPrice>> = _prices.asStateFlow()
    
    private val _totalCollectionValue = MutableStateFlow(0.0)
    val totalCollectionValue: StateFlow<Double> = _totalCollectionValue.asStateFlow()
    
    private val _collectionPrices = MutableStateFlow<List<com.example.stampcollectionsapp.features.collection.data.model.CollectionPrice>>(emptyList())
    val collectionPrices: StateFlow<List<com.example.stampcollectionsapp.features.collection.data.model.CollectionPrice>> = _collectionPrices.asStateFlow()
    
    fun loadPricesForStamp(stampId: Long) {
        viewModelScope.launch {
            priceRepository.getPricesByStampId(stampId).collect { prices ->
                _prices.value = prices
            }
        }
    }
    
    fun addPrice(stampId: Long, price: Double, currency: String = "USD", source: String? = null) {
        viewModelScope.launch {
            val stampPrice = StampPrice(
                stampId = stampId,
                price = price,
                currency = currency,
                source = source
            )
            priceRepository.insertPrice(stampPrice)
        }
    }
    
    fun calculateTotalCollectionValue(stampIds: List<Long>) {
        viewModelScope.launch {
            var total = 0.0
            stampIds.forEach { stampId ->
                val latestPrice = priceRepository.getLatestPrice(stampId)
                if (latestPrice != null) {
                    total += latestPrice.price
                }
            }
            _totalCollectionValue.value = total
        }
    }
    
    // Обновление цены марки из priceUrl и добавление в историю цен
    suspend fun updatePriceFromUrl(stamp: Stamp, priceValue: Double) {
        // Обновляем цену в марке
        val updatedStamp = stamp.copy(price = priceValue)
        stampRepository.updateStamp(updatedStamp)
        
        // Добавляем цену в историю
        val latestPrice = priceRepository.getLatestPrice(stamp.id)
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        // Добавляем новую цену только если сегодня еще не добавляли или цена изменилась
        if (latestPrice == null || latestPrice.date < today || latestPrice.price != priceValue) {
            val stampPrice = StampPrice(
                stampId = stamp.id,
                price = priceValue,
                currency = stamp.currency ?: "EUR",
                date = System.currentTimeMillis(),
                source = "colnect"
            )
            priceRepository.insertPrice(stampPrice)
        }
    }
    
    // Проверка и обновление цен для всех марок с priceUrl
    fun updatePricesForAllStamps() {
        viewModelScope.launch {
            val stamps = stampRepository.getStampsWithPriceUrl()
            // Обновление цен будет происходить через WebView при открытии детальной страницы
            // или через отдельный сервис обновления
        }
    }
    
    // Расчет стоимости коллекции по дням
    suspend fun calculateCollectionPricesOverTime(stampIds: List<Long>): List<com.example.stampcollectionsapp.features.collection.data.model.CollectionPrice> {
        val allPrices = mutableMapOf<Long, List<StampPrice>>()
        
        // Получаем все цены для всех марок
        stampIds.forEach { stampId ->
            val prices = priceRepository.getAllPricesForStamp(stampId)
            allPrices[stampId] = prices
        }
        
        // Собираем все уникальные даты
        val allDates = allPrices.values.flatMap { it.map { price -> price.date } }.distinct().sorted()
        
        // Для каждой даты считаем общую стоимость
        val collectionPrices = allDates.map { date ->
            var totalValue = 0.0
            var currency = "EUR"
            
            stampIds.forEach { stampId ->
                val prices = allPrices[stampId] ?: emptyList()
                // Находим последнюю цену до или на эту дату
                val priceForDate = prices
                    .filter { it.date <= date }
                    .maxByOrNull { it.date }
                
                if (priceForDate != null) {
                    totalValue += priceForDate.price
                    currency = priceForDate.currency
                }
            }
            
            com.example.stampcollectionsapp.features.collection.data.model.CollectionPrice(
                date = date,
                totalValue = totalValue,
                currency = currency
            )
        }
        
        return collectionPrices
    }
    
    fun loadCollectionPricesOverTime(stampIds: List<Long>) {
        viewModelScope.launch {
            val prices = calculateCollectionPricesOverTime(stampIds)
            _collectionPrices.value = prices
        }
    }
}


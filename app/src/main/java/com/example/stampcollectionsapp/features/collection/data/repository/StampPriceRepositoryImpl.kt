package com.example.stampcollectionsapp.features.collection.data.repository

import com.example.stampcollectionsapp.features.collection.data.dao.StampPriceDao
import com.example.stampcollectionsapp.features.collection.data.model.StampPrice
import com.example.stampcollectionsapp.features.collection.domain.repository.StampPriceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class StampPriceRepositoryImpl @Inject constructor(
    private val stampPriceDao: StampPriceDao
) : StampPriceRepository {
    
    override fun getPricesByStampId(stampId: Long): Flow<List<StampPrice>> =
        stampPriceDao.getPricesByStampId(stampId)
    
    override suspend fun getLatestPrice(stampId: Long): StampPrice? =
        stampPriceDao.getLatestPrice(stampId)
    
    override suspend fun insertPrice(price: StampPrice): Long =
        stampPriceDao.insertPrice(price)
    
    override suspend fun getAllPricesForStamp(stampId: Long): List<StampPrice> =
        stampPriceDao.getAllPricesForStamp(stampId)
    
    override suspend fun updatePrice(price: StampPrice) {
        stampPriceDao.insertPrice(price) // Используем insert с REPLACE стратегией
    }
}


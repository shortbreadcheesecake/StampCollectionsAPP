package com.example.stampcollectionsapp.features.collection.domain.repository

import com.example.stampcollectionsapp.features.collection.data.model.StampPrice
import kotlinx.coroutines.flow.Flow

interface StampPriceRepository {
    fun getPricesByStampId(stampId: Long): Flow<List<StampPrice>>
    suspend fun getLatestPrice(stampId: Long): StampPrice?
    suspend fun insertPrice(price: StampPrice): Long
    suspend fun getAllPricesForStamp(stampId: Long): List<StampPrice>
    suspend fun updatePrice(price: StampPrice)
}


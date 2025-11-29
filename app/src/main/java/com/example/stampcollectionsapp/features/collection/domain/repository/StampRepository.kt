package com.example.stampcollectionsapp.features.collection.domain.repository

import com.example.stampcollectionsapp.features.collection.data.model.Stamp
import kotlinx.coroutines.flow.Flow

interface StampRepository {
    fun getAllStamps(): Flow<List<Stamp>>
    suspend fun getStampById(id: Long): Stamp?
    fun getStampsByCountry(country: String): Flow<List<Stamp>>
    fun getStampsByYear(year: Int): Flow<List<Stamp>>
    fun getRecentStamps(limit: Int = 10): Flow<List<Stamp>>
    suspend fun insertStamp(stamp: Stamp): Long
    suspend fun updateStamp(stamp: Stamp)
    suspend fun deleteStamp(stamp: Stamp)
    suspend fun deleteStampById(id: Long)
    suspend fun getStampsWithPriceUrl(): List<Stamp>
    suspend fun toggleFavorite(stampId: Long, isFavorite: Boolean)
    fun getFavoriteStamps(): Flow<List<Stamp>>
}


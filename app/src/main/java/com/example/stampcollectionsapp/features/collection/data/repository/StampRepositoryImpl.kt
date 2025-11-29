package com.example.stampcollectionsapp.features.collection.data.repository

import com.example.stampcollectionsapp.features.collection.data.dao.StampDao
import com.example.stampcollectionsapp.features.collection.data.model.Stamp
import com.example.stampcollectionsapp.features.collection.domain.repository.StampRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class StampRepositoryImpl @Inject constructor(
    private val stampDao: StampDao
) : StampRepository {
    
    override fun getAllStamps(): Flow<List<Stamp>> = stampDao.getAllStamps()
    
    override suspend fun getStampById(id: Long): Stamp? = stampDao.getStampById(id)
    
    override fun getStampsByCountry(country: String): Flow<List<Stamp>> =
        stampDao.getStampsByCountry("%$country%")
    
    override fun getStampsByYear(year: Int): Flow<List<Stamp>> = stampDao.getStampsByYear(year)
    
    override fun getRecentStamps(limit: Int): Flow<List<Stamp>> = stampDao.getRecentStamps(limit)
    
    override suspend fun insertStamp(stamp: Stamp): Long = stampDao.insertStamp(stamp)
    
    override suspend fun updateStamp(stamp: Stamp) = stampDao.updateStamp(stamp)
    
    override suspend fun deleteStamp(stamp: Stamp) = stampDao.deleteStamp(stamp)
    
    override suspend fun deleteStampById(id: Long) = stampDao.deleteStampById(id)
    
    override suspend fun getStampsWithPriceUrl(): List<Stamp> = stampDao.getStampsWithPriceUrl()
    
    override suspend fun toggleFavorite(stampId: Long, isFavorite: Boolean) = 
        stampDao.updateFavoriteStatus(stampId, isFavorite)
    
    override fun getFavoriteStamps(): Flow<List<Stamp>> = stampDao.getFavoriteStamps()
}


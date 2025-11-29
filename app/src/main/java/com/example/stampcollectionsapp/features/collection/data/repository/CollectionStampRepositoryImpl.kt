package com.example.stampcollectionsapp.features.collection.data.repository

import com.example.stampcollectionsapp.features.collection.data.dao.CollectionStampDao
import com.example.stampcollectionsapp.features.collection.data.model.CollectionStamp
import com.example.stampcollectionsapp.features.collection.domain.repository.CollectionStampRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CollectionStampRepositoryImpl @Inject constructor(
    private val collectionStampDao: CollectionStampDao
) : CollectionStampRepository {
    
    override fun getAllCollectionStamps(): Flow<List<CollectionStamp>> =
        collectionStampDao.getAllCollectionStamps()
    
    override suspend fun getCollectionStampById(id: Long): CollectionStamp? =
        collectionStampDao.getCollectionStampById(id)
    
    override fun getCollectionStampsByCollectionId(collectionId: Long): Flow<List<CollectionStamp>> =
        collectionStampDao.getCollectionStampsByCollectionId(collectionId)
    
    override fun getCollectionStampsByStampId(stampId: Long): Flow<List<CollectionStamp>> =
        collectionStampDao.getCollectionStampsByStampId(stampId)
    
    override suspend fun insertCollectionStamp(collectionStamp: CollectionStamp): Long =
        collectionStampDao.insertCollectionStamp(collectionStamp)
    
    override suspend fun updateCollectionStamp(collectionStamp: CollectionStamp) =
        collectionStampDao.updateCollectionStamp(collectionStamp)
    
    override suspend fun deleteCollectionStamp(collectionStamp: CollectionStamp) =
        collectionStampDao.deleteCollectionStamp(collectionStamp)
    
    override suspend fun deleteCollectionStampById(id: Long) =
        collectionStampDao.deleteCollectionStampById(id)
}


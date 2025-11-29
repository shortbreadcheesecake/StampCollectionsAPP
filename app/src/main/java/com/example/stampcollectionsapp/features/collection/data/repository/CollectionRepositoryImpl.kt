package com.example.stampcollectionsapp.features.collection.data.repository

import com.example.stampcollectionsapp.features.collection.data.dao.CollectionDao
import com.example.stampcollectionsapp.features.collection.data.dao.CollectionStampDao
import com.example.stampcollectionsapp.features.collection.data.dao.StampDao
import com.example.stampcollectionsapp.features.collection.data.model.Collection
import com.example.stampcollectionsapp.features.collection.data.model.CollectionStamp
import com.example.stampcollectionsapp.features.collection.data.model.CollectionWithStamps
import com.example.stampcollectionsapp.features.collection.data.model.StampWithDetails
import com.example.stampcollectionsapp.features.collection.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CollectionRepositoryImpl @Inject constructor(
    private val collectionDao: CollectionDao,
    private val collectionStampDao: CollectionStampDao,
    private val stampDao: StampDao
) : CollectionRepository {
    
    override fun getAllCollections(): Flow<List<Collection>> = collectionDao.getAllCollections()
    
    override suspend fun getCollectionById(id: Long): Collection? = collectionDao.getCollectionById(id)
    
    override fun getCollectionsByCollectorId(collectorId: Long): Flow<List<Collection>> =
        collectionDao.getCollectionsByCollectorId(collectorId)
    
    override suspend fun getCollectionWithStamps(id: Long): CollectionWithStamps? {
        val collection = collectionDao.getCollectionById(id) ?: return null
        val collectionStamps = collectionStampDao.getCollectionStampsByCollectionIdSync(id)
        val stampsWithDetails = collectionStamps.mapNotNull { cs ->
            val stamp = stampDao.getStampById(cs.stampId) ?: return@mapNotNull null
            StampWithDetails(stamp, cs)
        }
        return CollectionWithStamps(collection, stampsWithDetails)
    }
    
    override suspend fun insertCollection(collection: Collection): Long =
        collectionDao.insertCollection(collection)
    
    override suspend fun updateCollection(collection: Collection) =
        collectionDao.updateCollection(collection)
    
    override suspend fun deleteCollection(collection: Collection) =
        collectionDao.deleteCollection(collection)
    
    override suspend fun deleteCollectionById(id: Long) =
        collectionDao.deleteCollectionById(id)
}


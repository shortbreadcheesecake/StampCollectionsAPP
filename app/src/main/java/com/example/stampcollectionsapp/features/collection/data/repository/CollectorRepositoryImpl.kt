package com.example.stampcollectionsapp.features.collection.data.repository

import com.example.stampcollectionsapp.features.collection.data.dao.CollectorDao
import com.example.stampcollectionsapp.features.collection.data.dao.CollectionDao
import com.example.stampcollectionsapp.features.collection.data.model.Collector
import com.example.stampcollectionsapp.features.collection.data.model.CollectorWithCollections
import com.example.stampcollectionsapp.features.collection.domain.repository.CollectorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CollectorRepositoryImpl @Inject constructor(
    private val collectorDao: CollectorDao,
    private val collectionDao: CollectionDao
) : CollectorRepository {
    
    override fun getAllCollectors(): Flow<List<Collector>> = collectorDao.getAllCollectors()
    
    override suspend fun getCollectorById(id: Long): Collector? = collectorDao.getCollectorById(id)
    
    override suspend fun getCollectorWithCollections(id: Long): CollectorWithCollections? {
        val collector = collectorDao.getCollectorById(id) ?: return null
        // Get collections for this collector - using first() to get the list synchronously
        val collectionsList = collectionDao.getCollectionsByCollectorId(id).first()
        return CollectorWithCollections(collector, collectionsList)
    }
    
    override suspend fun insertCollector(collector: Collector): Long =
        collectorDao.insertCollector(collector)
    
    override suspend fun updateCollector(collector: Collector) =
        collectorDao.updateCollector(collector)
    
    override suspend fun deleteCollector(collector: Collector) =
        collectorDao.deleteCollector(collector)
    
    override suspend fun deleteCollectorById(id: Long) =
        collectorDao.deleteCollectorById(id)
}


package com.example.stampcollectionsapp.features.collection.domain.repository

import com.example.stampcollectionsapp.features.collection.data.model.Collection
import com.example.stampcollectionsapp.features.collection.data.model.CollectionWithStamps
import kotlinx.coroutines.flow.Flow

interface CollectionRepository {
    fun getAllCollections(): Flow<List<Collection>>
    suspend fun getCollectionById(id: Long): Collection?
    fun getCollectionsByCollectorId(collectorId: Long): Flow<List<Collection>>
    suspend fun getCollectionWithStamps(id: Long): CollectionWithStamps?
    suspend fun insertCollection(collection: Collection): Long
    suspend fun updateCollection(collection: Collection)
    suspend fun deleteCollection(collection: Collection)
    suspend fun deleteCollectionById(id: Long)
}


package com.example.stampcollectionsapp.features.collection.domain.repository

import com.example.stampcollectionsapp.features.collection.data.model.CollectionStamp
import kotlinx.coroutines.flow.Flow

interface CollectionStampRepository {
    fun getAllCollectionStamps(): Flow<List<CollectionStamp>>
    suspend fun getCollectionStampById(id: Long): CollectionStamp?
    fun getCollectionStampsByCollectionId(collectionId: Long): Flow<List<CollectionStamp>>
    fun getCollectionStampsByStampId(stampId: Long): Flow<List<CollectionStamp>>
    suspend fun insertCollectionStamp(collectionStamp: CollectionStamp): Long
    suspend fun updateCollectionStamp(collectionStamp: CollectionStamp)
    suspend fun deleteCollectionStamp(collectionStamp: CollectionStamp)
    suspend fun deleteCollectionStampById(id: Long)
}


package com.example.stampcollectionsapp.features.collection.domain.repository

import com.example.stampcollectionsapp.features.collection.data.model.Collector
import com.example.stampcollectionsapp.features.collection.data.model.CollectorWithCollections
import kotlinx.coroutines.flow.Flow

interface CollectorRepository {
    fun getAllCollectors(): Flow<List<Collector>>
    suspend fun getCollectorById(id: Long): Collector?
    suspend fun getCollectorWithCollections(id: Long): CollectorWithCollections?
    suspend fun insertCollector(collector: Collector): Long
    suspend fun updateCollector(collector: Collector)
    suspend fun deleteCollector(collector: Collector)
    suspend fun deleteCollectorById(id: Long)
}


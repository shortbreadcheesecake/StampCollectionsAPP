package com.example.stampcollectionsapp.features.collection.data.dao

import androidx.room.*
import com.example.stampcollectionsapp.features.collection.data.model.Collector
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectorDao {
    @Query("SELECT * FROM collectors ORDER BY name ASC")
    fun getAllCollectors(): Flow<List<Collector>>
    
    @Query("SELECT * FROM collectors WHERE id = :id")
    suspend fun getCollectorById(id: Long): Collector?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollector(collector: Collector): Long
    
    @Update
    suspend fun updateCollector(collector: Collector)
    
    @Delete
    suspend fun deleteCollector(collector: Collector)
    
    @Query("DELETE FROM collectors WHERE id = :id")
    suspend fun deleteCollectorById(id: Long)
}


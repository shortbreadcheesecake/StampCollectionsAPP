package com.example.stampcollectionsapp.features.collection.data.dao

import androidx.room.*
import com.example.stampcollectionsapp.features.collection.data.model.Collection
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections ORDER BY dateCreated DESC")
    fun getAllCollections(): Flow<List<Collection>>
    
    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun getCollectionById(id: Long): Collection?
    
    @Query("SELECT * FROM collections WHERE collectorId = :collectorId")
    fun getCollectionsByCollectorId(collectorId: Long): Flow<List<Collection>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: Collection): Long
    
    @Update
    suspend fun updateCollection(collection: Collection)
    
    @Delete
    suspend fun deleteCollection(collection: Collection)
    
    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollectionById(id: Long)
}


package com.example.stampcollectionsapp.features.collection.data.dao

import androidx.room.*
import com.example.stampcollectionsapp.features.collection.data.model.CollectionStamp
import com.example.stampcollectionsapp.features.collection.data.model.StampWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionStampDao {
    @Query("SELECT * FROM collection_stamps")
    fun getAllCollectionStamps(): Flow<List<CollectionStamp>>
    
    @Query("SELECT * FROM collection_stamps WHERE id = :id")
    suspend fun getCollectionStampById(id: Long): CollectionStamp?
    
    @Query("SELECT * FROM collection_stamps WHERE collectionId = :collectionId")
    fun getCollectionStampsByCollectionId(collectionId: Long): Flow<List<CollectionStamp>>
    
    @Query("SELECT * FROM collection_stamps WHERE stampId = :stampId")
    fun getCollectionStampsByStampId(stampId: Long): Flow<List<CollectionStamp>>
    
    @Query("""
        SELECT cs.* 
        FROM collection_stamps cs 
        WHERE cs.collectionId = :collectionId
    """)
    suspend fun getCollectionStampsByCollectionIdSync(collectionId: Long): List<CollectionStamp>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollectionStamp(collectionStamp: CollectionStamp): Long
    
    @Update
    suspend fun updateCollectionStamp(collectionStamp: CollectionStamp)
    
    @Delete
    suspend fun deleteCollectionStamp(collectionStamp: CollectionStamp)
    
    @Query("DELETE FROM collection_stamps WHERE id = :id")
    suspend fun deleteCollectionStampById(id: Long)
    
    @Query("DELETE FROM collection_stamps WHERE collectionId = :collectionId")
    suspend fun deleteCollectionStampsByCollectionId(collectionId: Long)
}


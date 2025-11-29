package com.example.stampcollectionsapp.features.collection.data.dao

import androidx.room.*
import com.example.stampcollectionsapp.features.collection.data.model.Stamp
import kotlinx.coroutines.flow.Flow

@Dao
interface StampDao {
    @Query("SELECT * FROM stamps ORDER BY country ASC, year ASC")
    fun getAllStamps(): Flow<List<Stamp>>
    
    @Query("SELECT * FROM stamps WHERE id = :id")
    suspend fun getStampById(id: Long): Stamp?
    
    @Query("SELECT * FROM stamps WHERE country LIKE :country")
    fun getStampsByCountry(country: String): Flow<List<Stamp>>
    
    @Query("SELECT * FROM stamps WHERE year = :year")
    fun getStampsByYear(year: Int): Flow<List<Stamp>>
    
    @Query("SELECT * FROM stamps WHERE priceUrl IS NOT NULL AND priceUrl != ''")
    suspend fun getStampsWithPriceUrl(): List<Stamp>

    @Query(
        """
        SELECT s.* FROM stamps s
        INNER JOIN (
            SELECT stampId, MAX(COALESCE(acquisitionDate, 0)) AS lastAcquired
            FROM collection_stamps
            GROUP BY stampId
        ) cs ON cs.stampId = s.id
        ORDER BY cs.lastAcquired DESC, s.id DESC
        LIMIT :limit
        """
    )
    fun getRecentStamps(limit: Int = 10): Flow<List<Stamp>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStamp(stamp: Stamp): Long
    
    @Update
    suspend fun updateStamp(stamp: Stamp)
    
    @Delete
    suspend fun deleteStamp(stamp: Stamp)
    
    @Query("DELETE FROM stamps WHERE id = :id")
    suspend fun deleteStampById(id: Long)
    
    @Query("UPDATE stamps SET isFavorite = :isFavorite WHERE id = :stampId")
    suspend fun updateFavoriteStatus(stampId: Long, isFavorite: Boolean)
    
    @Query("SELECT * FROM stamps WHERE isFavorite = 1")
    fun getFavoriteStamps(): Flow<List<Stamp>>
}


package com.example.stampcollectionsapp.features.collection.data.dao

import androidx.room.*
import com.example.stampcollectionsapp.features.collection.data.model.StampPrice
import kotlinx.coroutines.flow.Flow

@Dao
interface StampPriceDao {
    @Query("SELECT * FROM stamp_prices WHERE stampId = :stampId ORDER BY date DESC")
    fun getPricesByStampId(stampId: Long): Flow<List<StampPrice>>
    
    @Query("SELECT * FROM stamp_prices WHERE stampId = :stampId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestPrice(stampId: Long): StampPrice?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrice(price: StampPrice): Long
    
    @Query("DELETE FROM stamp_prices WHERE stampId = :stampId")
    suspend fun deletePricesByStampId(stampId: Long)
    
    @Query("SELECT * FROM stamp_prices WHERE stampId = :stampId ORDER BY date ASC")
    suspend fun getAllPricesForStamp(stampId: Long): List<StampPrice>
}


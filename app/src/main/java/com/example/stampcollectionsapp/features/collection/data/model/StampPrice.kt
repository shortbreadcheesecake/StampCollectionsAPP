package com.example.stampcollectionsapp.features.collection.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stamp_prices",
    foreignKeys = [
        ForeignKey(
            entity = Stamp::class,
            parentColumns = ["id"],
            childColumns = ["stampId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["stampId"])]
)
data class StampPrice(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stampId: Long,
    val price: Double,
    val currency: String = "USD",
    val date: Long = System.currentTimeMillis(),
    val source: String? = null // "stampworld", "colnect", "manual"
)


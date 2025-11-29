package com.example.stampcollectionsapp.features.collection.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "collection_stamps",
    foreignKeys = [
        ForeignKey(
            entity = Collection::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Stamp::class,
            parentColumns = ["id"],
            childColumns = ["stampId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["collectionId"]), Index(value = ["stampId"])]
)
data class CollectionStamp(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val collectionId: Long,
    val stampId: Long,
    val acquisitionDate: Long? = null,
    val acquisitionPrice: Double? = null,
    val currentPrice: Double? = null // Текущая рыночная цена
)


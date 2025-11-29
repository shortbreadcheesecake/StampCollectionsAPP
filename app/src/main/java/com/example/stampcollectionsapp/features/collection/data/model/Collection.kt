package com.example.stampcollectionsapp.features.collection.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "collections",
    foreignKeys = [
        ForeignKey(
            entity = Collector::class,
            parentColumns = ["id"],
            childColumns = ["collectorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["collectorId"])]
)
data class Collection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val collectionName: String,
    val description: String? = null,
    val dateCreated: Long = System.currentTimeMillis(),
    val collectorId: Long,
    val color: Long? = null // Цвет коллекции в формате ARGB
)


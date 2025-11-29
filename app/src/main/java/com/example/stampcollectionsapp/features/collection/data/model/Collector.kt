package com.example.stampcollectionsapp.features.collection.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collectors")
data class Collector(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val email: String,
    val phone: String? = null,
    val address: String? = null,
    val photoUri: String? = null
)


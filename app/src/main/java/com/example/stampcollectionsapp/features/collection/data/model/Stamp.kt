package com.example.stampcollectionsapp.features.collection.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stamps")
data class Stamp(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String? = null,
    val country: String,
    val year: Int,
    val denomination: String? = null, // Номинал как строка со знаком валюты
    val currency: String? = null, // EUR, USD, RUB - валюта для цены
    val description: String? = null,
    val condition: String? = null,
    val imageUrl: String? = null,
    val price: Double? = null,
    val priceUrl: String? = null, // Ссылка на страницу марки
    val isFavorite: Boolean = false // Флаг избранного
)


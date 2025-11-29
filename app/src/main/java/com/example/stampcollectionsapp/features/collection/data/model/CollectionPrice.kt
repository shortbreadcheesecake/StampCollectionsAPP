package com.example.stampcollectionsapp.features.collection.data.model

data class CollectionPrice(
    val date: Long,
    val totalValue: Double,
    val currency: String = "EUR"
)



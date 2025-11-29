package com.example.stampcollectionsapp.features.collection.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class CollectorExportData(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String? = null,
    val address: String? = null,
    val photoUri: String? = null,
    val collections: List<CollectionExportData> = emptyList()
)

@Serializable
data class CollectionExportData(
    val id: Long,
    val collectionName: String,
    val description: String? = null,
    val dateCreated: Long,
    val collectorId: Long,
    val stamps: List<StampExportData> = emptyList()
)

@Serializable
data class StampExportData(
    val id: Long,
    val country: String,
    val year: Int,
    val denomination: String? = null, // Номинал как строка
    val description: String? = null,
    val condition: String? = null,
    val imageUrl: String? = null,
    val acquisitionDate: Long? = null,
    val acquisitionPrice: Double? = null
)


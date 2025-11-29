package com.example.stampcollectionsapp.features.collection.domain.repository

import android.net.Uri
import com.example.stampcollectionsapp.features.collection.domain.model.CollectorExportData
import com.example.stampcollectionsapp.features.collection.domain.model.CollectionExportData
import com.example.stampcollectionsapp.features.collection.domain.model.StampExportData

interface ExportImportRepository {
    suspend fun exportCollectorToJson(collectorId: Long): String
    suspend fun exportCollectionToJson(collectionId: Long): String
    suspend fun exportCollectorToCsv(collectorId: Long): String
    suspend fun exportCollectionToCsv(collectionId: Long): String
    suspend fun importCollectorFromJson(json: String): Long?
    suspend fun importCollectionFromJson(json: String, collectorId: Long): Long?
    suspend fun importCollectorFromCsv(csv: String): Long?
    suspend fun importCollectionFromCsv(csv: String, collectorId: Long): Long?
}


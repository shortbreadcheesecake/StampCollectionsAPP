package com.example.stampcollectionsapp.features.collection.data.repository

import com.example.stampcollectionsapp.features.collection.data.dao.CollectionStampDao
import com.example.stampcollectionsapp.features.collection.data.dao.StampDao
import com.example.stampcollectionsapp.features.collection.domain.model.CollectorExportData
import com.example.stampcollectionsapp.features.collection.domain.model.CollectionExportData
import com.example.stampcollectionsapp.features.collection.domain.model.StampExportData
import com.example.stampcollectionsapp.features.collection.domain.repository.CollectorRepository
import com.example.stampcollectionsapp.features.collection.domain.repository.CollectionRepository
import com.example.stampcollectionsapp.features.collection.domain.repository.ExportImportRepository
import com.example.stampcollectionsapp.features.collection.domain.repository.StampRepository
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ExportImportRepositoryImpl @Inject constructor(
    private val collectorRepository: CollectorRepository,
    private val collectionRepository: CollectionRepository,
    private val stampRepository: StampRepository,
    private val collectionStampDao: CollectionStampDao,
    private val stampDao: StampDao
) : ExportImportRepository {
    
    override suspend fun exportCollectorToJson(collectorId: Long): String {
        val collector = collectorRepository.getCollectorById(collectorId) ?: return ""
        val collections = collectorRepository.getCollectorWithCollections(collectorId)
        
        val exportData = CollectorExportData(
            id = collector.id,
            name = collector.name,
            email = collector.email,
            phone = collector.phone,
            address = collector.address,
            photoUri = collector.photoUri,
            collections = collections?.collections?.map { collection ->
                val collectionWithStamps = collectionRepository.getCollectionWithStamps(collection.id)
                CollectionExportData(
                    id = collection.id,
                    collectionName = collection.collectionName,
                    description = collection.description,
                    dateCreated = collection.dateCreated,
                    collectorId = collection.collectorId,
                    stamps = collectionWithStamps?.stamps?.map { stampWithDetails ->
                        StampExportData(
                            id = stampWithDetails.stamp.id,
                            country = stampWithDetails.stamp.country,
                            year = stampWithDetails.stamp.year,
                            denomination = stampWithDetails.stamp.denomination,
                            description = stampWithDetails.stamp.description,
                            condition = stampWithDetails.stamp.condition,
                            imageUrl = stampWithDetails.stamp.imageUrl,
                            acquisitionDate = stampWithDetails.collectionStamp.acquisitionDate,
                            acquisitionPrice = stampWithDetails.collectionStamp.acquisitionPrice
                        )
                    } ?: emptyList()
                )
            } ?: emptyList()
        )
        
        return Json.encodeToString(exportData)
    }
    
    override suspend fun exportCollectionToJson(collectionId: Long): String {
        val collection = collectionRepository.getCollectionById(collectionId) ?: return ""
        val collectionWithStamps = collectionRepository.getCollectionWithStamps(collectionId) ?: return ""
        
        val exportData = CollectionExportData(
            id = collection.id,
            collectionName = collection.collectionName,
            description = collection.description,
            dateCreated = collection.dateCreated,
            collectorId = collection.collectorId,
            stamps = collectionWithStamps.stamps.map { stampWithDetails ->
                StampExportData(
                    id = stampWithDetails.stamp.id,
                    country = stampWithDetails.stamp.country,
                    year = stampWithDetails.stamp.year,
                    denomination = stampWithDetails.stamp.denomination,
                    description = stampWithDetails.stamp.description,
                    condition = stampWithDetails.stamp.condition,
                    imageUrl = stampWithDetails.stamp.imageUrl,
                    acquisitionDate = stampWithDetails.collectionStamp.acquisitionDate,
                    acquisitionPrice = stampWithDetails.collectionStamp.acquisitionPrice
                )
            }
        )
        
        return Json.encodeToString(exportData)
    }
    
    override suspend fun exportCollectorToCsv(collectorId: Long): String {
        val collector = collectorRepository.getCollectorById(collectorId) ?: return ""
        val collections = collectorRepository.getCollectorWithCollections(collectorId)
        
        val csv = StringBuilder()
        // Header
        csv.appendLine("Collector ID,Name,Email,Phone,Address")
        csv.appendLine("${collector.id},\"${collector.name}\",\"${collector.email}\",\"${collector.phone ?: ""}\",\"${collector.address ?: ""}\"")
        csv.appendLine() // Empty row
        csv.appendLine("Collection ID,Collection Name,Description,Date Created,Stamp ID,Country,Year,Denomination,Description,Condition,Acquisition Date,Acquisition Price")
        
        collections?.collections?.forEach { collection ->
            val collectionWithStamps = collectionRepository.getCollectionWithStamps(collection.id)
            collectionWithStamps?.stamps?.forEach { stampWithDetails ->
                csv.appendLine(
                    "${collection.id},\"${collection.collectionName}\",\"${collection.description ?: ""}\"," +
                    "${collection.dateCreated},${stampWithDetails.stamp.id},\"${stampWithDetails.stamp.country}\"," +
                    "${stampWithDetails.stamp.year},${stampWithDetails.stamp.denomination ?: ""},\"${stampWithDetails.stamp.description ?: ""}\"," +
                    "\"${stampWithDetails.stamp.condition ?: ""}\",${stampWithDetails.collectionStamp.acquisitionDate ?: ""}," +
                    "${stampWithDetails.collectionStamp.acquisitionPrice ?: ""}"
                )
            } ?: run {
                csv.appendLine(
                    "${collection.id},\"${collection.collectionName}\",\"${collection.description ?: ""}\"," +
                    "${collection.dateCreated},,,,,,,,"
                )
            }
        }
        
        return csv.toString()
    }
    
    override suspend fun exportCollectionToCsv(collectionId: Long): String {
        val collection = collectionRepository.getCollectionById(collectionId) ?: return ""
        val collectionWithStamps = collectionRepository.getCollectionWithStamps(collectionId) ?: return ""
        
        val csv = StringBuilder()
        // Header
        csv.appendLine("Collection ID,Collection Name,Description,Date Created")
        csv.appendLine("${collection.id},\"${collection.collectionName}\",\"${collection.description ?: ""}\",${collection.dateCreated}")
        csv.appendLine() // Empty row
        csv.appendLine("Stamp ID,Country,Year,Denomination,Description,Condition,Image URL,Acquisition Date,Acquisition Price")
        
        collectionWithStamps.stamps.forEach { stampWithDetails ->
            csv.appendLine(
                "${stampWithDetails.stamp.id},\"${stampWithDetails.stamp.country}\",${stampWithDetails.stamp.year}," +
                "${stampWithDetails.stamp.denomination ?: ""},\"${stampWithDetails.stamp.description ?: ""}\"," +
                "\"${stampWithDetails.stamp.condition ?: ""}\",\"${stampWithDetails.stamp.imageUrl ?: ""}\"," +
                "${stampWithDetails.collectionStamp.acquisitionDate ?: ""},${stampWithDetails.collectionStamp.acquisitionPrice ?: ""}"
            )
        }
        
        return csv.toString()
    }
    
    override suspend fun importCollectorFromJson(json: String): Long? {
        return try {
            val exportData = Json.decodeFromString<CollectorExportData>(json)
            val collector = com.example.stampcollectionsapp.features.collection.data.model.Collector(
                id = 0, // New ID will be generated
                name = exportData.name,
                email = exportData.email,
                phone = exportData.phone,
                address = exportData.address,
                photoUri = exportData.photoUri
            )
            val collectorId = collectorRepository.insertCollector(collector)
            
            exportData.collections.forEach { collectionData ->
                val collection = com.example.stampcollectionsapp.features.collection.data.model.Collection(
                    id = 0,
                    collectionName = collectionData.collectionName,
                    description = collectionData.description,
                    dateCreated = collectionData.dateCreated,
                    collectorId = collectorId
                )
                val collectionId = collectionRepository.insertCollection(collection)
                
                collectionData.stamps.forEach { stampData ->
                    val stamp = com.example.stampcollectionsapp.features.collection.data.model.Stamp(
                        id = 0,
                        country = stampData.country,
                        year = stampData.year,
                        denomination = stampData.denomination,
                        description = stampData.description,
                        condition = stampData.condition,
                        imageUrl = stampData.imageUrl
                    )
                    val stampId = stampRepository.insertStamp(stamp)
                    
                    val collectionStamp = com.example.stampcollectionsapp.features.collection.data.model.CollectionStamp(
                        id = 0,
                        collectionId = collectionId,
                        stampId = stampId,
                        acquisitionDate = stampData.acquisitionDate,
                        acquisitionPrice = stampData.acquisitionPrice
                    )
                    collectionStampDao.insertCollectionStamp(collectionStamp)
                }
            }
            
            collectorId
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun importCollectionFromJson(json: String, collectorId: Long): Long? {
        return try {
            val exportData = Json.decodeFromString<CollectionExportData>(json)
            val collection = com.example.stampcollectionsapp.features.collection.data.model.Collection(
                id = 0,
                collectionName = exportData.collectionName,
                description = exportData.description,
                dateCreated = exportData.dateCreated,
                collectorId = collectorId
            )
            val collectionId = collectionRepository.insertCollection(collection)
            
            exportData.stamps.forEach { stampData ->
                val stamp = com.example.stampcollectionsapp.features.collection.data.model.Stamp(
                    id = 0,
                    country = stampData.country,
                    year = stampData.year,
                    denomination = stampData.denomination,
                    description = stampData.description,
                    condition = stampData.condition,
                    imageUrl = stampData.imageUrl
                )
                val stampId = stampRepository.insertStamp(stamp)
                
                val collectionStamp = com.example.stampcollectionsapp.features.collection.data.model.CollectionStamp(
                    id = 0,
                    collectionId = collectionId,
                    stampId = stampId,
                    acquisitionDate = stampData.acquisitionDate,
                    acquisitionPrice = stampData.acquisitionPrice
                )
                collectionStampDao.insertCollectionStamp(collectionStamp)
            }
            
            collectionId
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun importCollectorFromCsv(csv: String): Long? {
        return try {
            val rows = csvReader().readAll(csv)
            if (rows.isEmpty()) return null
            
            // First row should be collector data
            val collectorRow = rows[0]
            if (collectorRow.size < 3) return null
            
            val collector = com.example.stampcollectionsapp.features.collection.data.model.Collector(
                id = 0,
                name = collectorRow[1],
                email = collectorRow[2],
                phone = if (collectorRow.size > 3) collectorRow[3] else null,
                address = if (collectorRow.size > 4) collectorRow[4] else null
            )
            val collectorId = collectorRepository.insertCollector(collector)
            
            // Skip header rows and process collections
            var i = 2 // Skip collector row and empty row
            while (i < rows.size) {
                if (rows[i].isEmpty() || rows[i][0].isEmpty()) {
                    i++
                    continue
                }
                
                val collectionName = rows[i][1]
                val collection = com.example.stampcollectionsapp.features.collection.data.model.Collection(
                    id = 0,
                    collectionName = collectionName,
                    description = if (rows[i].size > 2) rows[i][2] else null,
                    dateCreated = if (rows[i].size > 3) rows[i][3].toLongOrNull() ?: System.currentTimeMillis() else System.currentTimeMillis(),
                    collectorId = collectorId
                )
                val collectionId = collectionRepository.insertCollection(collection)
                
                i++
                // Process stamps for this collection
                while (i < rows.size && rows[i].isNotEmpty() && rows[i][0].isNotEmpty()) {
                    if (rows[i].size < 6) {
                        i++
                        continue
                    }
                    
                    val stamp = com.example.stampcollectionsapp.features.collection.data.model.Stamp(
                        id = 0,
                        country = rows[i][5],
                        year = rows[i][6].toIntOrNull() ?: 0,
                        denomination = rows[i].getOrNull(7), // Номинал как строка
                        description = rows[i].getOrNull(8),
                        condition = rows[i].getOrNull(9)
                    )
                    val stampId = stampRepository.insertStamp(stamp)
                    
                    val collectionStamp = com.example.stampcollectionsapp.features.collection.data.model.CollectionStamp(
                        id = 0,
                        collectionId = collectionId,
                        stampId = stampId,
                        acquisitionDate = rows[i].getOrNull(10)?.toLongOrNull(),
                        acquisitionPrice = rows[i].getOrNull(11)?.toDoubleOrNull()
                    )
                    collectionStampDao.insertCollectionStamp(collectionStamp)
                    i++
                }
            }
            
            collectorId
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun importCollectionFromCsv(csv: String, collectorId: Long): Long? {
        return try {
            val rows = csvReader().readAll(csv)
            if (rows.isEmpty()) return null
            
            // First row should be collection data
            val collectionRow = rows[0]
            if (collectionRow.size < 2) return null
            
            val collection = com.example.stampcollectionsapp.features.collection.data.model.Collection(
                id = 0,
                collectionName = collectionRow[1],
                description = if (collectionRow.size > 2) collectionRow[2] else null,
                dateCreated = if (collectionRow.size > 3) collectionRow[3].toLongOrNull() ?: System.currentTimeMillis() else System.currentTimeMillis(),
                collectorId = collectorId
            )
            val collectionId = collectionRepository.insertCollection(collection)
            
            // Skip header row and empty row, process stamps
            var i = 2
            while (i < rows.size) {
                if (rows[i].isEmpty()) {
                    i++
                    continue
                }
                
                if (rows[i].size < 3) {
                    i++
                    continue
                }
                
                val stamp = com.example.stampcollectionsapp.features.collection.data.model.Stamp(
                    id = 0,
                    country = rows[i][1],
                    year = rows[i][2].toIntOrNull() ?: 0,
                    denomination = rows[i].getOrNull(3), // Номинал как строка
                    description = rows[i].getOrNull(4),
                    condition = rows[i].getOrNull(5),
                    imageUrl = rows[i].getOrNull(6)
                )
                val stampId = stampRepository.insertStamp(stamp)
                
                val collectionStamp = com.example.stampcollectionsapp.features.collection.data.model.CollectionStamp(
                    id = 0,
                    collectionId = collectionId,
                    stampId = stampId,
                    acquisitionDate = rows[i].getOrNull(7)?.toLongOrNull(),
                    acquisitionPrice = rows[i].getOrNull(8)?.toDoubleOrNull()
                )
                collectionStampDao.insertCollectionStamp(collectionStamp)
                i++
            }
            
            collectionId
        } catch (e: Exception) {
            null
        }
    }
}


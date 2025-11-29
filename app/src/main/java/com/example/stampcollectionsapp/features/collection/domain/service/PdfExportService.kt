package com.example.stampcollectionsapp.features.collection.domain.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.stampcollectionsapp.features.collection.data.model.Collection
import com.example.stampcollectionsapp.features.collection.data.model.Stamp
import com.example.stampcollectionsapp.features.collection.data.model.StampPrice
import com.example.stampcollectionsapp.features.collection.data.model.CollectionPrice
import com.example.stampcollectionsapp.features.auth.domain.model.User
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import android.os.Environment
import android.os.Build
import android.graphics.Typeface
import java.io.FileInputStream

class PdfExportService(private val context: Context) {
    
    // Получаем шрифт с поддержкой Unicode (кириллицы)
    private fun getCyrillicFont(): PdfFont {
        return try {
            // Используем системный шрифт Android через Typeface
            val typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            
            // Пробуем получить файл шрифта из системных шрифтов Android
            val systemFontPaths = listOf(
                "/system/fonts/Roboto-Regular.ttf",
                "/system/fonts/NotoSans-Regular.ttf",
                "/system/fonts/DroidSans.ttf",
                "/system/fonts/Roboto-Light.ttf"
            )
            
            for (fontPath in systemFontPaths) {
                try {
                    val fontFile = File(fontPath)
                    if (fontFile.exists() && fontFile.canRead()) {
                        val fontBytes = FileInputStream(fontFile).readBytes()
                        return PdfFontFactory.createFont(
                            fontBytes,
                            PdfEncodings.IDENTITY_H,
                            PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
                        )
                    }
                } catch (e: Exception) {
                    continue
                }
            }
            
            // Если системные шрифты недоступны, пробуем assets
            try {
                val assetManager = context.assets
                val fontFiles = listOf(
                    "fonts/roboto.ttf",
                    "fonts/notosans.ttf",
                    "fonts/arial.ttf"
                )
                
                for (fontPath in fontFiles) {
                    try {
                        val fontStream: InputStream = assetManager.open(fontPath)
                        val fontBytes = fontStream.readBytes()
                        fontStream.close()
                        return PdfFontFactory.createFont(
                            fontBytes,
                            PdfEncodings.IDENTITY_H,
                            PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
                        )
                    } catch (e: Exception) {
                        continue
                    }
                }
            } catch (e: Exception) {
                // Игнорируем ошибки assets
            }
            
            // В крайнем случае используем стандартный шрифт с Unicode кодировкой
            // Это может не работать для кириллицы, но попробуем
            PdfFontFactory.createFont(
                com.itextpdf.io.font.constants.StandardFonts.HELVETICA,
                PdfEncodings.IDENTITY_H
            )
        } catch (e: Exception) {
            // В крайнем случае используем стандартный шрифт
            PdfFontFactory.createFont(
                com.itextpdf.io.font.constants.StandardFonts.HELVETICA,
                PdfEncodings.IDENTITY_H
            )
        }
    }
    
    suspend fun exportStampToPdf(
        stamp: Stamp,
        prices: List<StampPrice> = emptyList(),
        user: User? = null
    ): File = withContext(Dispatchers.IO) {
        val fileName = "stamp_${stamp.id}_${System.currentTimeMillis()}.pdf"
        val file = getDownloadsFile(fileName)
        
        val writer = PdfWriter(file)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)
        
        try {
            val font = getCyrillicFont()
            
            // Заголовок
            document.add(
                Paragraph("Отчет о марках")
                    .setFont(font)
                    .setFontSize(24f)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20f)
            )
            
            // Информация о пользователе после заголовка
            if (user != null) {
                addUserInfo(document, user, font)
            }
            
            // Изображение марки
            if (stamp.imageUrl != null) {
                try {
                    val imageUri = Uri.parse(stamp.imageUrl)
                    val inputStream = context.contentResolver.openInputStream(imageUri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    
                    if (bitmap != null) {
                        val tempFile = File(context.cacheDir, "temp_stamp_image.png")
                        FileOutputStream(tempFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        val imageData = ImageDataFactory.create(tempFile.absolutePath)
                        val image = Image(imageData)
                        image.setWidth(200f)
                        image.setAutoScale(true)
                        image.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER)
                        document.add(image)
                        tempFile.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // Информация о марке
            document.add(createInfoTable(stamp, font))
            
            // График изменения цен
            if (prices.isNotEmpty()) {
                document.add(
                    Paragraph("График изменения цен")
                        .setFont(font)
                        .setFontSize(18f)
                        .setBold()
                        .setMarginTop(20f)
                        .setMarginBottom(10f)
                )
                addPriceChart(document, prices, font)
            }
            
            document.close()
        } catch (e: Exception) {
            document.close()
            throw e
        }
        
        file
    }
    
    suspend fun exportCollectionToPdf(
        collection: Collection,
        stamps: List<Stamp>,
        collectionPrices: List<CollectionPrice> = emptyList(),
        user: User? = null
    ): File = withContext(Dispatchers.IO) {
        val fileName = "collection_${collection.id}_${System.currentTimeMillis()}.pdf"
        val file = getDownloadsFile(fileName)
        
        val writer = PdfWriter(file)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)
        
        try {
            val font = getCyrillicFont()
            
            // Заголовок
            document.add(
                Paragraph("Отчет о коллекциях")
                    .setFont(font)
                    .setFontSize(24f)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20f)
            )
            
            // Информация о пользователе после заголовка
            if (user != null) {
                addUserInfo(document, user, font)
            }
            
            // Информация о коллекции
            val title = Paragraph(collection.collectionName)
                .setFont(font)
                .setFontSize(20f)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10f)
            
            if (collection.color != null) {
                val color = android.graphics.Color.valueOf(collection.color.toInt())
                title.setFontColor(
                    com.itextpdf.kernel.colors.DeviceRgb(
                        color.red(),
                        color.green(),
                        color.blue()
                    )
                )
            }
            
            document.add(title)
            
            if (collection.description != null) {
                document.add(
                    Paragraph(collection.description)
                        .setFont(font)
                        .setFontSize(12f)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(20f)
                )
            }
            
            // Статистика
            val totalValue = stamps.sumOf { it.price ?: 0.0 }
            document.add(
                Paragraph("Марок в коллекции: ${stamps.size}")
                    .setFont(font)
                    .setFontSize(14f)
                    .setMarginBottom(5f)
            )
            if (totalValue > 0) {
                document.add(
                    Paragraph("Общая стоимость: ${String.format("%.2f", totalValue)}")
                        .setFont(font)
                        .setFontSize(14f)
                        .setMarginBottom(20f)
                )
            }
            
            // Краткая информация о марках
            document.add(
                Paragraph("Марки коллекции")
                    .setFont(font)
                    .setFontSize(18f)
                    .setBold()
                    .setMarginTop(20f)
                    .setMarginBottom(10f)
            )
            
            stamps.forEachIndexed { index, stamp ->
                val stampInfo = buildString {
                    append("${index + 1}. ")
                    if (stamp.name != null) {
                        append(stamp.name)
                        append(" - ")
                    }
                    append("${stamp.country} ${stamp.year}")
                    if (stamp.price != null) {
                        append(" (${stamp.price} ${stamp.currency ?: "EUR"})")
                    }
                }
                
                document.add(
                    Paragraph(stampInfo)
                        .setFont(font)
                        .setFontSize(12f)
                        .setMarginTop(5f)
                )
            }
            
            // График изменения цен коллекции
            if (collectionPrices.isNotEmpty()) {
                document.add(
                    Paragraph("График изменения цен коллекции")
                        .setFont(font)
                        .setFontSize(18f)
                        .setBold()
                        .setMarginTop(20f)
                        .setMarginBottom(10f)
                )
                addCollectionPriceChart(document, collectionPrices, font)
            }
            
            // Дата создания отчета
            document.add(
                Paragraph("Отчет создан: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())}")
                    .setFont(font)
                    .setFontSize(10f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20f)
            )
            
            document.close()
        } catch (e: Exception) {
            document.close()
            throw e
        }
        
        file
    }
    
    private fun createInfoTable(stamp: Stamp, font: PdfFont): Table {
        val table = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
            .useAllAvailableWidth()
            .setMarginTop(20f)
            .setMarginBottom(10f)
        
        addTableRow(table, "Страна", stamp.country, font)
        addTableRow(table, "Год", stamp.year.toString(), font)
        if (stamp.name != null && stamp.name.isNotBlank()) {
            addTableRow(table, "Название", stamp.name, font)
        }
        if (stamp.denomination != null && stamp.denomination.isNotBlank()) {
            addTableRow(table, "Номинал", stamp.denomination, font)
        }
        if (stamp.description != null && stamp.description.isNotBlank()) {
            addTableRow(table, "Описание", stamp.description, font)
        }
        if (stamp.condition != null && stamp.condition.isNotBlank()) {
            addTableRow(table, "Состояние", stamp.condition, font)
        }
        if (stamp.price != null) {
            addTableRow(table, "Цена", "${String.format("%.2f", stamp.price)} ${stamp.currency ?: "EUR"}", font)
        }
        
        return table
    }
    
    private fun addTableRow(table: Table, label: String, value: String, font: PdfFont) {
        table.addCell(
            Cell().add(Paragraph(label).setFont(font).setBold())
                .setPadding(8f)
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
        )
        table.addCell(
            Cell().add(Paragraph(value).setFont(font))
                .setPadding(8f)
        )
    }
    
    suspend fun exportAllCollectionsToPdf(
        collections: List<Collection>,
        allStamps: Map<Long, List<Stamp>>,
        allCollectionPrices: List<CollectionPrice> = emptyList(),
        user: User? = null
    ): File = withContext(Dispatchers.IO) {
        val fileName = "all_collections_${System.currentTimeMillis()}.pdf"
        val file = getDownloadsFile(fileName)
        
        val writer = PdfWriter(file)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)
        
        try {
            val font = getCyrillicFont()
            
            // Заголовок
            document.add(
                Paragraph("Отчет об аккаунте")
                    .setFont(font)
                    .setFontSize(24f)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20f)
            )
            
            // Информация о пользователе после заголовка
            if (user != null) {
                addUserInfo(document, user, font)
            }
            
            // Общая статистика
            val totalStamps = allStamps.values.sumOf { it.size }
            val totalValue = allStamps.values.flatten().sumOf { it.price ?: 0.0 }
            
            document.add(
                Paragraph("Количество коллекций: ${collections.size}")
                    .setFont(font)
                    .setFontSize(16f)
                    .setMarginBottom(5f)
            )
            document.add(
                Paragraph("Количество марок: $totalStamps")
                    .setFont(font)
                    .setFontSize(16f)
                    .setMarginBottom(5f)
            )
            if (totalValue > 0) {
                document.add(
                    Paragraph("Общая стоимость: ${String.format("%.2f", totalValue)}")
                        .setFont(font)
                        .setFontSize(16f)
                        .setMarginBottom(20f)
                )
            }
            
            // График изменения цен аккаунта
            if (allCollectionPrices.isNotEmpty()) {
                document.add(
                    Paragraph("График изменения цен аккаунта")
                        .setFont(font)
                        .setFontSize(18f)
                        .setBold()
                        .setMarginTop(20f)
                        .setMarginBottom(10f)
                )
                addCollectionPriceChart(document, allCollectionPrices, font)
            }
            
            // Дата создания отчета
            document.add(
                Paragraph("Отчет создан: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())}")
                    .setFont(font)
                    .setFontSize(10f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20f)
            )
            
            document.close()
        } catch (e: Exception) {
            document.close()
            throw e
        }
        
        file
    }
    
    private fun addUserInfo(document: Document, user: User, font: PdfFont) {
        var hasInfo = false
        
        if (user.displayName.isNotBlank()) {
            // Отображаем ФИО как есть (в формате Имя Фамилия Отчество)
            document.add(
                Paragraph("ФИО: ${user.displayName}")
                    .setFont(font)
                    .setFontSize(12f)
                    .setMarginBottom(5f)
            )
            hasInfo = true
        }
        
        if (user.email.isNotBlank()) {
            document.add(
                Paragraph("Email: ${user.email}")
                    .setFont(font)
                    .setFontSize(12f)
                    .setMarginBottom(5f)
            )
            hasInfo = true
        }
        
        if (hasInfo) {
            document.add(com.itextpdf.layout.element.Paragraph("").setMarginTop(10f).setMarginBottom(10f))
        }
    }
    
    private fun addPriceChart(document: Document, prices: List<StampPrice>, font: PdfFont) {
        if (prices.isEmpty()) return
        
        val sortedPrices = prices.sortedBy { it.date }
        val table = Table(UnitValue.createPercentArray(floatArrayOf(40f, 30f, 30f)))
            .useAllAvailableWidth()
            .setMarginTop(10f)
            .setMarginBottom(10f)
        
        // Заголовки таблицы
        table.addHeaderCell(
            Cell().add(Paragraph("Дата").setFont(font).setBold())
                .setPadding(8f)
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
        )
        table.addHeaderCell(
            Cell().add(Paragraph("Цена").setFont(font).setBold())
                .setPadding(8f)
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
        )
        table.addHeaderCell(
            Cell().add(Paragraph("Валюта").setFont(font).setBold())
                .setPadding(8f)
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
        )
        
        // Данные
        sortedPrices.forEach { price ->
            val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                .format(Date(price.date))
            table.addCell(Cell().add(Paragraph(dateStr).setFont(font)).setPadding(5f))
            table.addCell(Cell().add(Paragraph(String.format("%.2f", price.price)).setFont(font)).setPadding(5f))
            table.addCell(Cell().add(Paragraph(price.currency).setFont(font)).setPadding(5f))
        }
        
        document.add(table)
    }
    
    private fun addCollectionPriceChart(document: Document, prices: List<CollectionPrice>, font: PdfFont) {
        if (prices.isEmpty()) return
        
        val sortedPrices = prices.sortedBy { it.date }
        val table = Table(UnitValue.createPercentArray(floatArrayOf(40f, 30f, 30f)))
            .useAllAvailableWidth()
            .setMarginTop(10f)
            .setMarginBottom(10f)
        
        // Заголовки таблицы
        table.addHeaderCell(
            Cell().add(Paragraph("Дата").setFont(font).setBold())
                .setPadding(8f)
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
        )
        table.addHeaderCell(
            Cell().add(Paragraph("Общая стоимость").setFont(font).setBold())
                .setPadding(8f)
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
        )
        table.addHeaderCell(
            Cell().add(Paragraph("Валюта").setFont(font).setBold())
                .setPadding(8f)
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
        )
        
        // Данные
        sortedPrices.forEach { price ->
            val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                .format(Date(price.date))
            table.addCell(Cell().add(Paragraph(dateStr).setFont(font)).setPadding(5f))
            table.addCell(Cell().add(Paragraph(String.format("%.2f", price.totalValue)).setFont(font)).setPadding(5f))
            table.addCell(Cell().add(Paragraph(price.currency).setFont(font)).setPadding(5f))
        }
        
        document.add(table)
    }
    
    private fun getDownloadsFile(fileName: String): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ используем MediaStore
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            File(downloadsDir, fileName)
        } else {
            // Для старых версий
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            File(downloadsDir, fileName)
        }
    }
}



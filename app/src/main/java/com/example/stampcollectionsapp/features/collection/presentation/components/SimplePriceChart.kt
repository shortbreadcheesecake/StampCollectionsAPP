package com.example.stampcollectionsapp.features.collection.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.stampcollectionsapp.features.collection.data.model.StampPrice
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SimplePriceChart(
    prices: List<StampPrice>,
    modifier: Modifier = Modifier
) {
    if (prices.isEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет данных о ценах")
            }
        }
        return
    }
    
    val sortedPrices = prices.sortedBy { it.date }
    val minPrice = sortedPrices.minOfOrNull { it.price } ?: 0.0
    val maxPrice = sortedPrices.maxOfOrNull { it.price } ?: 1.0
    val priceRange = maxPrice - minPrice
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "График изменения цены",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val width = size.width
                val height = size.height
                val padding = 40f
                
                val chartWidth = width - padding * 2
                val chartHeight = height - padding * 2
                
                // Рисуем оси
                drawLine(
                    start = Offset(padding, padding),
                    end = Offset(padding, height - padding),
                    color = Color.Gray,
                    strokeWidth = 2f
                )
                drawLine(
                    start = Offset(padding, height - padding),
                    end = Offset(width - padding, height - padding),
                    color = Color.Gray,
                    strokeWidth = 2f
                )
                
                // Рисуем график
                if (sortedPrices.size > 1) {
                    val path = Path()
                    val stepX = chartWidth / (sortedPrices.size - 1)
                    
                    sortedPrices.forEachIndexed { index, price ->
                        val x = padding + index * stepX
                        val normalizedPrice = if (priceRange > 0) {
                            ((price.price - minPrice) / priceRange).toFloat()
                        } else {
                            0.5f
                        }
                        val y = height - padding - (normalizedPrice * chartHeight)
                        
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    
                    drawPath(
                        path = path,
                        color = Color(0xFF6200EE),
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )
                    
                    // Рисуем точки
                    sortedPrices.forEachIndexed { index, price ->
                        val x = padding + index * stepX
                        val normalizedPrice = if (priceRange > 0) {
                            ((price.price - minPrice) / priceRange).toFloat()
                        } else {
                            0.5f
                        }
                        val y = height - padding - (normalizedPrice * chartHeight)
                        
                        drawCircle(
                            color = Color(0xFF6200EE),
                            radius = 5f,
                            center = Offset(x, y)
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Мин: ${String.format("%.2f", minPrice)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Макс: ${String.format("%.2f", maxPrice)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}


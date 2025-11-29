package com.example.stampcollectionsapp.features.collection.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.stampcollectionsapp.features.collection.data.model.StampPrice

// Используем SimplePriceChart вместо MPAndroidChart для совместимости с Compose
@Composable
fun PriceChart(
    prices: List<StampPrice>,
    modifier: Modifier = Modifier
) {
    SimplePriceChart(prices = prices, modifier = modifier)
}

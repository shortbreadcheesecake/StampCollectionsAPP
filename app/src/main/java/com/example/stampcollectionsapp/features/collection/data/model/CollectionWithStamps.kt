package com.example.stampcollectionsapp.features.collection.data.model

data class CollectionWithStamps(
    val collection: Collection,
    val stamps: List<StampWithDetails>
)

data class StampWithDetails(
    val stamp: Stamp,
    val collectionStamp: CollectionStamp
)


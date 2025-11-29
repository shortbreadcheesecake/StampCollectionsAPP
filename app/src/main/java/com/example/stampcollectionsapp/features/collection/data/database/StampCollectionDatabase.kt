package com.example.stampcollectionsapp.features.collection.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.stampcollectionsapp.features.collection.data.dao.CollectorDao
import com.example.stampcollectionsapp.features.collection.data.dao.CollectionDao
import com.example.stampcollectionsapp.features.collection.data.dao.CollectionStampDao
import com.example.stampcollectionsapp.features.collection.data.dao.StampDao
import com.example.stampcollectionsapp.features.collection.data.dao.StampPriceDao
import com.example.stampcollectionsapp.features.collection.data.model.Collector
import com.example.stampcollectionsapp.features.collection.data.model.Collection
import com.example.stampcollectionsapp.features.collection.data.model.CollectionStamp
import com.example.stampcollectionsapp.features.collection.data.model.Stamp
import com.example.stampcollectionsapp.features.collection.data.model.StampPrice

@Database(
    entities = [Collector::class, Collection::class, Stamp::class, CollectionStamp::class, StampPrice::class],
    version = 6,
    exportSchema = false
)
abstract class StampCollectionDatabase : RoomDatabase() {
    abstract fun collectorDao(): CollectorDao
    abstract fun collectionDao(): CollectionDao
    abstract fun stampDao(): StampDao
    abstract fun collectionStampDao(): CollectionStampDao
    abstract fun stampPriceDao(): StampPriceDao
}


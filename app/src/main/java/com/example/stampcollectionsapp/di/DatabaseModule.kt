package com.example.stampcollectionsapp.di

import android.content.Context
import androidx.room.Room
import com.example.stampcollectionsapp.features.collection.data.dao.CollectorDao
import com.example.stampcollectionsapp.features.collection.data.dao.CollectionDao
import com.example.stampcollectionsapp.features.collection.data.dao.CollectionStampDao
import com.example.stampcollectionsapp.features.collection.data.dao.StampDao
import com.example.stampcollectionsapp.features.collection.data.dao.StampPriceDao
import com.example.stampcollectionsapp.features.collection.data.database.StampCollectionDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StampCollectionDatabase {
        return Room.databaseBuilder(
            context,
            StampCollectionDatabase::class.java,
            "stamp_collection_database"
        )
        .fallbackToDestructiveMigration() // Временно для разработки - в продакшене нужна миграция
        .build()
    }
    
    @Provides
    fun provideCollectorDao(database: StampCollectionDatabase): CollectorDao {
        return database.collectorDao()
    }
    
    @Provides
    fun provideCollectionDao(database: StampCollectionDatabase): CollectionDao {
        return database.collectionDao()
    }
    
    @Provides
    fun provideStampDao(database: StampCollectionDatabase): StampDao {
        return database.stampDao()
    }
    
    @Provides
    fun provideCollectionStampDao(database: StampCollectionDatabase): CollectionStampDao {
        return database.collectionStampDao()
    }
    
    @Provides
    fun provideStampPriceDao(database: StampCollectionDatabase): StampPriceDao {
        return database.stampPriceDao()
    }
}


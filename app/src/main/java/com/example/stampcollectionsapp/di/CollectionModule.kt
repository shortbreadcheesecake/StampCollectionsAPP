package com.example.stampcollectionsapp.di

import com.example.stampcollectionsapp.features.collection.data.repository.CollectorRepositoryImpl
import com.example.stampcollectionsapp.features.collection.data.repository.CollectionRepositoryImpl
import com.example.stampcollectionsapp.features.collection.data.repository.CollectionStampRepositoryImpl
import com.example.stampcollectionsapp.features.collection.data.repository.ExportImportRepositoryImpl
import com.example.stampcollectionsapp.features.collection.data.repository.StampPriceRepositoryImpl
import com.example.stampcollectionsapp.features.collection.data.repository.StampRepositoryImpl
import com.example.stampcollectionsapp.features.collection.domain.repository.CollectorRepository
import com.example.stampcollectionsapp.features.collection.domain.repository.CollectionRepository
import com.example.stampcollectionsapp.features.collection.domain.repository.CollectionStampRepository
import com.example.stampcollectionsapp.features.collection.domain.repository.ExportImportRepository
import com.example.stampcollectionsapp.features.collection.domain.repository.StampPriceRepository
import com.example.stampcollectionsapp.features.collection.domain.repository.StampRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CollectionModule {
    
    @Binds
    @Singleton
    abstract fun bindCollectorRepository(
        collectorRepositoryImpl: CollectorRepositoryImpl
    ): CollectorRepository
    
    @Binds
    @Singleton
    abstract fun bindCollectionRepository(
        collectionRepositoryImpl: CollectionRepositoryImpl
    ): CollectionRepository
    
    @Binds
    @Singleton
    abstract fun bindStampRepository(
        stampRepositoryImpl: StampRepositoryImpl
    ): StampRepository
    
    @Binds
    @Singleton
    abstract fun bindCollectionStampRepository(
        collectionStampRepositoryImpl: CollectionStampRepositoryImpl
    ): CollectionStampRepository
    
    @Binds
    @Singleton
    abstract fun bindExportImportRepository(
        exportImportRepositoryImpl: ExportImportRepositoryImpl
    ): ExportImportRepository
    
    @Binds
    @Singleton
    abstract fun bindStampPriceRepository(
        stampPriceRepositoryImpl: StampPriceRepositoryImpl
    ): StampPriceRepository
}


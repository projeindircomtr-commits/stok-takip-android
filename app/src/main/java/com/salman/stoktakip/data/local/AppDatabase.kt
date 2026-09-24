package com.salman.stoktakip.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.salman.stoktakip.data.local.dao.*
import com.salman.stoktakip.data.local.entity.*

@Database(
    entities = [
        MalzemeCacheEntity::class,
        AracCacheEntity::class,
        KategoriCacheEntity::class,
        LokasyonCacheEntity::class,
        BekleyenIslemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun malzemeDao(): MalzemeDao
    abstract fun aracDao(): AracDao
    abstract fun kategoriDao(): KategoriDao
    abstract fun lokasyonDao(): LokasyonDao
    abstract fun bekleyenIslemDao(): BekleyenIslemDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stok_takip.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}

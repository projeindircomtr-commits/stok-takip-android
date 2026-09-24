package com.salman.stoktakip.data.local.dao

import androidx.room.*
import com.salman.stoktakip.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MalzemeDao {
    @Query("SELECT * FROM malzeme_cache ORDER BY id DESC")
    fun tumunuGozlemle(): Flow<List<MalzemeCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ekleVeyaGuncelle(liste: List<MalzemeCacheEntity>)

    @Query("DELETE FROM malzeme_cache")
    suspend fun hepsiniSil()

    @Query("DELETE FROM malzeme_cache WHERE id = :id")
    suspend fun idIleSil(id: Int)

    @Transaction
    suspend fun onbellekYenile(liste: List<MalzemeCacheEntity>) {
        hepsiniSil()
        ekleVeyaGuncelle(liste)
    }
}

@Dao
interface AracDao {
    @Query("SELECT * FROM arac_cache ORDER BY id DESC")
    fun tumunuGozlemle(): Flow<List<AracCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ekleVeyaGuncelle(liste: List<AracCacheEntity>)

    @Query("DELETE FROM arac_cache")
    suspend fun hepsiniSil()

    @Query("DELETE FROM arac_cache WHERE id = :id")
    suspend fun idIleSil(id: Int)

    @Transaction
    suspend fun onbellekYenile(liste: List<AracCacheEntity>) {
        hepsiniSil()
        ekleVeyaGuncelle(liste)
    }
}

@Dao
interface KategoriDao {
    @Query("SELECT * FROM kategori_cache ORDER BY ad ASC")
    fun tumunuGozlemle(): Flow<List<KategoriCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ekleVeyaGuncelle(liste: List<KategoriCacheEntity>)

    @Query("DELETE FROM kategori_cache")
    suspend fun hepsiniSil()

    @Transaction
    suspend fun onbellekYenile(liste: List<KategoriCacheEntity>) {
        hepsiniSil()
        ekleVeyaGuncelle(liste)
    }
}

@Dao
interface LokasyonDao {
    @Query("SELECT * FROM lokasyon_cache ORDER BY ad ASC")
    fun tumunuGozlemle(): Flow<List<LokasyonCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ekleVeyaGuncelle(liste: List<LokasyonCacheEntity>)

    @Query("DELETE FROM lokasyon_cache")
    suspend fun hepsiniSil()

    @Transaction
    suspend fun onbellekYenile(liste: List<LokasyonCacheEntity>) {
        hepsiniSil()
        ekleVeyaGuncelle(liste)
    }
}

@Dao
interface BekleyenIslemDao {
    @Query("SELECT * FROM bekleyen_islem ORDER BY olusturmaZamani ASC")
    suspend fun hepsi(): List<BekleyenIslemEntity>

    @Query("SELECT COUNT(*) FROM bekleyen_islem")
    fun sayiGozlemle(): Flow<Int>

    @Insert
    suspend fun ekle(kayit: BekleyenIslemEntity): Long

    @Delete
    suspend fun sil(kayit: BekleyenIslemEntity)

    @Query("UPDATE bekleyen_islem SET sonHata = :hata WHERE id = :id")
    suspend fun hataYaz(id: Long, hata: String)
}

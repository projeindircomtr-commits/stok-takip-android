package com.salman.stoktakip.data.remote

import com.salman.stoktakip.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("giris.php")
    suspend fun girisYap(@Body istek: GirisIstek): Response<ApiResponse<GirisCevap>>

    @POST("cikis.php")
    suspend fun cikisYap(): Response<ApiResponse<Any>>

    @GET("dashboard.php")
    suspend fun dashboard(): Response<ApiResponse<Dashboard>>

    @GET("kategoriler.php")
    suspend fun kategoriler(): Response<ApiResponse<List<Kategori>>>

    @POST("kategoriler.php")
    suspend fun kategoriEkle(@Body istek: AdIstek): Response<ApiResponse<Kategori>>

    @GET("lokasyonlar.php")
    suspend fun lokasyonlar(): Response<ApiResponse<List<Lokasyon>>>

    @POST("lokasyonlar.php")
    suspend fun lokasyonEkle(@Body istek: AdIstek): Response<ApiResponse<Lokasyon>>

    @GET("malzemeler.php")
    suspend fun malzemeler(
        @Query("ara") ara: String? = null,
        @Query("kategori_id") kategoriId: Int? = null,
        @Query("lokasyon_id") lokasyonId: Int? = null
    ): Response<ApiResponse<List<Malzeme>>>

    @POST("malzemeler.php")
    suspend fun malzemeEkle(@Body istek: MalzemeIstek): Response<ApiResponse<Map<String, Int>>>

    @PUT("malzemeler.php")
    suspend fun malzemeGuncelle(@Query("id") id: Int, @Body istek: MalzemeIstek): Response<ApiResponse<Any>>

    @DELETE("malzemeler.php")
    suspend fun malzemeSil(@Query("id") id: Int): Response<ApiResponse<Any>>

    @GET("araclar.php")
    suspend fun araclar(
        @Query("ara") ara: String? = null,
        @Query("kategori_id") kategoriId: Int? = null,
        @Query("lokasyon_id") lokasyonId: Int? = null
    ): Response<ApiResponse<List<Arac>>>

    @POST("araclar.php")
    suspend fun aracEkle(@Body istek: AracIstek): Response<ApiResponse<Map<String, Int>>>

    @PUT("araclar.php")
    suspend fun aracGuncelle(@Query("id") id: Int, @Body istek: AracIstek): Response<ApiResponse<Any>>

    @DELETE("araclar.php")
    suspend fun aracSil(@Query("id") id: Int): Response<ApiResponse<Any>>
}

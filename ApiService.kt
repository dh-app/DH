package org.darulhuda.udupi.data

import org.darulhuda.udupi.model.QpcModel
import retrofit2.http.GET

/**
 * 🌐 ApiService — Retrofit endpoints for Nabi-ur-Rahmah App.
 * Fetches all remote JSON content for QPC, Flyers, Books, Projects, and About/Contact sections.
 */
interface ApiService {

    // 🕌 Qur'an Printing Complex
    @GET("https://gist.githubusercontent.com/dh-app/2d6d536eb82f788b5f0ce1c8c32f3272/raw/qpc.json")
    suspend fun getQpc(): QpcModel

    // 🕊 Flyers Index
    @GET("https://gist.githubusercontent.com/dh-app/2fead87a1d2f0e35a4a66630dc625604/raw/index.json")
    suspend fun getFlyersIndex(): List<Map<String, String>>

    // 📚 Books / Digital Library
    @GET("https://gist.githubusercontent.com/dh-app/63bd5c71db15798164b286b247456856/raw/index.json")
    suspend fun getBooks(): List<Map<String, String>>

    // 🏗 Projects
    @GET("https://gist.githubusercontent.com/dh-app/badf3a5a39d86b96fa48d4b1671088b3/raw/index.json")
    suspend fun getProjects(): List<Map<String, String>>

    // ℹ About Darul Huda
    @GET("https://gist.githubusercontent.com/dh-app/0ff3f7c9643a37a996b875299fe7a37c/raw/about.json")
    suspend fun getAbout(): Map<String, String>

    // ☎ Contact Info
    @GET("https://gist.githubusercontent.com/dh-app/94df1491f37c8f4c633f2bf75ec574fa/raw/contact.json")
    suspend fun getContact(): Map<String, String>
}

package org.darulhuda.udupi.core.network

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://gist.githubusercontent.com/dh-app/"

    val api: org.darulhuda.udupi.data.ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(org.darulhuda.udupi.data.ApiService::class.java)
    }
}

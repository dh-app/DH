package org.darulhuda.udupi.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.darulhuda.udupi.core.network.RetrofitClient
import org.darulhuda.udupi.model.QpcModel
import java.io.IOException

/**
 * 🗂 Repository — Centralized data access layer for NabiUrRahmah App.
 * Handles API communication, caching, and safe coroutine wrapping.
 */
class Repository(private val api: ApiService = RetrofitClient.api) {

    /**
     * 🕌 Qur'an Printing Complex Data
     */
    suspend fun fetchQpc(): Result<QpcModel> = safeApiCall {
        api.getQpc()
    }

    /**
     * 🕊 Flyers List
     */
    suspend fun fetchFlyers(): Result<List<Map<String, String>>> = safeApiCall {
        api.getFlyersIndex()
    }

    /**
     * 📚 Books / Digital Library
     */
    suspend fun fetchBooks(): Result<List<Map<String, String>>> = safeApiCall {
        api.getBooks()
    }

    /**
     * 🏗 Projects
     */
    suspend fun fetchProjects(): Result<List<Map<String, String>>> = safeApiCall {
        api.getProjects()
    }

    /**
     * ℹ About Darul Huda
     */
    suspend fun fetchAbout(): Result<Map<String, String>> = safeApiCall {
        api.getAbout()
    }

    /**
     * ☎ Contact Information
     */
    suspend fun fetchContact(): Result<Map<String, String>> = safeApiCall {
        api.getContact()
    }

    /**
     * 🛡 Safe API call wrapper — catches exceptions and prevents UI crashes.
     */
    private suspend fun <T> safeApiCall(apiCall: suspend () -> T): Result<T> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(apiCall())
            } catch (e: IOException) {
                e.printStackTrace()
                Result.failure(e)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
}

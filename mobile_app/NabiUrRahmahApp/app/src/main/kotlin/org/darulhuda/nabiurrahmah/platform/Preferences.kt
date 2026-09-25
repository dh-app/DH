package org.darulhuda.nabiurrahmah.platform

import android.content.Context
import androidx.core.content.edit

/** Small per-device settings and reading positions. */
class Preferences(context: Context) {

    private val prefs = context.getSharedPreferences("nur", Context.MODE_PRIVATE)

    var playSalawatOnOpen: Boolean
        get() = prefs.getBoolean(KEY_SALAWAT, true)
        set(value) = prefs.edit { putBoolean(KEY_SALAWAT, value) }

    fun lastPage(bookKey: String): Int = prefs.getInt("page:$bookKey", 0)

    fun saveLastPage(bookKey: String, page: Int) = prefs.edit { putInt("page:$bookKey", page) }

    private companion object {
        const val KEY_SALAWAT = "salawat_on_open"
    }
}

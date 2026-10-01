package org.darulhuda.nabiurrahmah.platform

import android.app.Activity
import android.content.Context
import androidx.core.content.edit
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.ktx.launchReview
import kotlin.coroutines.cancellation.CancellationException

/**
 * Asks for a Play Store rating with Google's in-app review sheet, but only from
 * someone who clearly uses the app: several visits, several books read, videos
 * watched or flyers opened, and at least [MIN_DAYS_INSTALLED] days in. It asks
 * at most once every [DAYS_BETWEEN_ASKS] days, and only back on the home screen,
 * never in the middle of reading or watching. Google may still decide not to
 * show the sheet; nothing in the app depends on it.
 */
class ReviewPrompt(context: Context, private val clock: () -> Long = System::currentTimeMillis) {

    private val prefs = context.getSharedPreferences("nur_review", Context.MODE_PRIVATE)

    fun onAppOpened() = prefs.edit {
        if (!prefs.contains(KEY_FIRST_OPEN)) putLong(KEY_FIRST_OPEN, clock())
        putInt(KEY_OPENS, prefs.getInt(KEY_OPENS, 0) + 1)
    }

    /** A book, video or flyer was opened. */
    fun onEngaged() = prefs.edit { putInt(KEY_MOMENTS, prefs.getInt(KEY_MOMENTS, 0) + 1) }

    suspend fun askIfDue(activity: Activity) {
        val due = isDue(
            opens = prefs.getInt(KEY_OPENS, 0),
            moments = prefs.getInt(KEY_MOMENTS, 0),
            firstOpen = prefs.getLong(KEY_FIRST_OPEN, clock()),
            lastAsked = prefs.getLong(KEY_LAST_ASKED, 0),
            now = clock(),
        )
        if (!due) return
        prefs.edit { putLong(KEY_LAST_ASKED, clock()) }
        try {
            val manager = ReviewManagerFactory.create(activity)
            manager.launchReview(activity, manager.requestReview())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // No Play Store, offline, or quota reached: try again another time.
        }
    }

    companion object {
        const val MIN_OPENS = 4
        const val MIN_MOMENTS = 5
        const val MIN_DAYS_INSTALLED = 2
        const val DAYS_BETWEEN_ASKS = 120
        private const val DAY = 24L * 60 * 60 * 1000

        private const val KEY_FIRST_OPEN = "first_open"
        private const val KEY_OPENS = "opens"
        private const val KEY_MOMENTS = "moments"
        private const val KEY_LAST_ASKED = "last_asked"

        fun isDue(opens: Int, moments: Int, firstOpen: Long, lastAsked: Long, now: Long): Boolean =
            opens >= MIN_OPENS &&
                moments >= MIN_MOMENTS &&
                now - firstOpen >= MIN_DAYS_INSTALLED * DAY &&
                (lastAsked == 0L || now - lastAsked >= DAYS_BETWEEN_ASKS * DAY)
    }
}

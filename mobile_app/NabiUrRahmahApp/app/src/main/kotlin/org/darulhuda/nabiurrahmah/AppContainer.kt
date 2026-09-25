package org.darulhuda.nabiurrahmah

import android.content.Context
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.darulhuda.nabiurrahmah.data.CatalogJson
import org.darulhuda.nabiurrahmah.data.CatalogRepository
import org.darulhuda.nabiurrahmah.data.library.LibraryRepository
import org.darulhuda.nabiurrahmah.data.library.ShelfId
import org.darulhuda.nabiurrahmah.data.library.TextFetcher
import org.darulhuda.nabiurrahmah.data.library.bookSourcesFrom
import org.darulhuda.nabiurrahmah.data.model.About
import org.darulhuda.nabiurrahmah.data.source.FileCatalogStore
import org.darulhuda.nabiurrahmah.data.source.HttpWebsiteSource
import org.darulhuda.nabiurrahmah.data.youtube.YouTubePlaylistSource
import org.darulhuda.nabiurrahmah.platform.BookDownloads
import org.darulhuda.nabiurrahmah.platform.FlyerFiles
import org.darulhuda.nabiurrahmah.platform.GallerySaver
import org.darulhuda.nabiurrahmah.platform.PdfDocuments
import org.darulhuda.nabiurrahmah.platform.Preferences
import org.darulhuda.nabiurrahmah.platform.SalawatPlayer

/** Creates and holds the app's long-lived objects. */
class AppContainer(context: Context, private val appScope: CoroutineScope) {

    private val appContext = context.applicationContext

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // Holds website pages so unchanged ones are revalidated with a cheap 304.
            .cache(Cache(File(appContext.cacheDir, "http"), 30L * 1024 * 1024))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "NabiUrRahmah/${BuildConfig.VERSION_NAME} (Android)")
                        .build(),
                )
            }
            .build()
    }

    val catalogRepository: CatalogRepository by lazy {
        CatalogRepository(
            indexUrl = BuildConfig.SITE_URL.toHttpUrl(),
            website = HttpWebsiteSource(okHttpClient),
            store = FileCatalogStore(File(appContext.filesDir, "catalog.json")),
            playlists = YouTubePlaylistSource(okHttpClient),
            configuredPlaylistIds = BuildConfig.YOUTUBE_PLAYLISTS.split(',').map { it.trim() }.filter { it.isNotEmpty() },
        )
    }

    private val textFetcher = TextFetcher { url ->
        runInterruptible(Dispatchers.IO) {
            okHttpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        }
    }

    val libraryRepository: LibraryRepository by lazy {
        val language = Locale.getDefault().language.ifBlank { "en" }
        LibraryRepository(
            sources = mapOf(
                ShelfId.Biography to bookSourcesFrom(BuildConfig.SHELF_BIOGRAPHY, textFetcher, language),
                ShelfId.Testimonies to bookSourcesFrom(BuildConfig.SHELF_TESTIMONIES, textFetcher, language),
                ShelfId.Books to bookSourcesFrom(BuildConfig.SHELF_BOOKS, textFetcher, language),
            ),
            storeFor = { shelf -> FileCatalogStore(File(appContext.filesDir, "shelf-${shelf.key}.json")) },
        )
    }

    /** Contact details shown on the About screen. */
    val about: About by lazy {
        appContext.assets.open("about.json").bufferedReader().use { CatalogJson.decodeAbout(it.readText()) }
    }

    val preferences: Preferences by lazy { Preferences(appContext) }

    val salawatPlayer: SalawatPlayer by lazy { SalawatPlayer(appContext, preferences) }

    val pdfDocuments = PdfDocuments()

    val bookDownloads: BookDownloads by lazy { BookDownloads(appContext, okHttpClient, appScope) }

    val flyerFiles: FlyerFiles by lazy { FlyerFiles(appContext, okHttpClient) }

    val gallerySaver: GallerySaver by lazy { GallerySaver(appContext) }
}

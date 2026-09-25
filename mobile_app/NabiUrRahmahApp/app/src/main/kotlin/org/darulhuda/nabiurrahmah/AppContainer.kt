package org.darulhuda.nabiurrahmah

import android.content.Context
import java.io.File
import java.util.concurrent.TimeUnit
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.darulhuda.nabiurrahmah.data.CatalogJson
import org.darulhuda.nabiurrahmah.data.CatalogRepository
import org.darulhuda.nabiurrahmah.data.model.About
import org.darulhuda.nabiurrahmah.data.source.FileCatalogStore
import org.darulhuda.nabiurrahmah.data.source.HttpWebsiteSource
import org.darulhuda.nabiurrahmah.platform.FlyerFiles
import org.darulhuda.nabiurrahmah.platform.GallerySaver

/** Creates and holds the app's long-lived objects. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // Holds website pages so unchanged ones are revalidated with a cheap 304.
            .cache(Cache(File(appContext.cacheDir, "http"), 20L * 1024 * 1024))
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
        )
    }

    /** Contact details shown on the About screen. */
    val about: About by lazy {
        appContext.assets.open("about.json").bufferedReader().use { CatalogJson.decodeAbout(it.readText()) }
    }

    val flyerFiles: FlyerFiles by lazy { FlyerFiles(appContext, okHttpClient) }

    val gallerySaver: GallerySaver by lazy { GallerySaver(appContext) }
}

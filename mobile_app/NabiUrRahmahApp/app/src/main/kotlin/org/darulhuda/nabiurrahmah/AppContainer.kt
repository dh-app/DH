package org.darulhuda.nabiurrahmah

import android.content.Context
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.darulhuda.nabiurrahmah.data.CatalogParser
import org.darulhuda.nabiurrahmah.data.CatalogRepository
import org.darulhuda.nabiurrahmah.data.source.FileCatalogStore
import org.darulhuda.nabiurrahmah.data.source.HttpCatalogSource
import org.darulhuda.nabiurrahmah.platform.FlyerFiles
import org.darulhuda.nabiurrahmah.platform.GallerySaver

/** Creates and holds the app's long-lived objects. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
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

    private val catalogUrl = BuildConfig.CATALOG_URL.toHttpUrl()

    val catalogRepository: CatalogRepository by lazy {
        CatalogRepository(
            remote = HttpCatalogSource(okHttpClient, catalogUrl),
            store = FileCatalogStore(File(appContext.filesDir, "catalog.json")) {
                try {
                    appContext.assets.open("catalog.json").bufferedReader().use { it.readText() }
                } catch (e: IOException) {
                    null
                }
            },
            parser = CatalogParser(catalogUrl),
        )
    }

    val flyerFiles: FlyerFiles by lazy { FlyerFiles(appContext, okHttpClient) }

    val gallerySaver: GallerySaver by lazy { GallerySaver(appContext) }
}

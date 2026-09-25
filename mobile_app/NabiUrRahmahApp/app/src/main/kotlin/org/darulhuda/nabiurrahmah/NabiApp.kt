package org.darulhuda.nabiurrahmah

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.darulhuda.nabiurrahmah.platform.PdfPageDecoder

class NabiApp : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch { container.catalogRepository.load() }
    }

    /** Checks the website for new flyers if the last check is old; cheap to call often. */
    fun refreshIfStale() {
        appScope.launch { container.catalogRepository.refresh(force = false) }
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            // Coil keeps its own disk cache, so give it a client without OkHttp's cache.
            .okHttpClient { container.okHttpClient.newBuilder().cache(null).build() }
            .components { add(PdfPageDecoder.Factory()) }
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("images"))
                    .maxSizeBytes(512L * 1024 * 1024)
                    .build()
            }
            // Uploaded flyer files don't change in place, so a cached copy stays valid,
            // and flyers already seen keep working offline.
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
}

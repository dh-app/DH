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

class NabiApp : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch { container.catalogRepository.load() }
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            // Coil keeps its own disk cache, so give it a client without OkHttp's cache.
            .okHttpClient { container.okHttpClient.newBuilder().cache(null).build() }
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("images"))
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            // Flyer files never change in place (a new version gets a new name),
            // so a cached copy is always valid, even offline.
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
}

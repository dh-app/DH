package org.darulhuda.nabiurrahmah.builder

import java.io.File
import java.util.Properties
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.darulhuda.nabiurrahmah.data.CatalogError
import org.darulhuda.nabiurrahmah.data.CatalogJson
import org.darulhuda.nabiurrahmah.data.CatalogRepository
import org.darulhuda.nabiurrahmah.data.model.Catalog
import org.darulhuda.nabiurrahmah.data.model.Flyer
import org.darulhuda.nabiurrahmah.data.model.Language
import org.darulhuda.nabiurrahmah.data.model.Playlist
import org.darulhuda.nabiurrahmah.data.source.HttpWebsiteSource
import org.darulhuda.nabiurrahmah.data.source.WebsiteSource
import org.darulhuda.nabiurrahmah.data.youtube.YouTubePlaylistSource

private const val DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0 Safari/537.36"

/**
 * Builds library/catalog.json, the one file the app downloads:
 *  1. flyers from the Nabi ur Rahmah website (the Internet Archive's copy if the site is down),
 *     copied into library/flyers/_from-website with display copies and previews;
 *  2. flyers uploaded to library/flyers/<Language>/;
 *  3. the YouTube playlists, in full.
 * Whatever can't be read keeps its last published version, so a bad day never empties the app.
 *
 * Usage: run from the repository root.
 */
fun main(args: Array<String>) = runBlocking<Unit>(Dispatchers.Default) {
    val repoRoot = File(args.getOrNull(0) ?: ".").canonicalFile
    val properties = Properties().apply {
        File(repoRoot, "mobile_app/NabiUrRahmahApp/gradle.properties").inputStream().use(::load)
    }
    val layout = LibraryLayout(File(repoRoot, "library"), properties.getProperty("nur.publishedBase"))
    val siteUrls = properties.getProperty("nur.siteUrl").split(',').map { it.trim() }.filter { it.isNotEmpty() }
    val playlistIds = properties.getProperty("nur.youtubePlaylists", "").split(',').map { it.trim() }.filter { it.isNotEmpty() }

    val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain -> chain.proceed(chain.request().newBuilder().header("User-Agent", DESKTOP).build()) }
        .build()
    val json = Json { prettyPrint = true; encodeDefaults = false; explicitNulls = false }
    val previous = layout.catalog.takeIf { it.isFile }?.let { runCatching { CatalogJson.decodeCatalog(it.readText()) }.getOrNull() }
    val report = StringBuilder("## Catalogue build\n\n")

    // 1. The website, live first, then as archived.
    val http = HttpWebsiteSource(client)
    val attempts: List<Pair<String, WebsiteSource>> =
        siteUrls.map { "live $it" to http } + siteUrls.map { "archived $it" to WaybackWebsiteSource(http) }
    var website: Catalog? = null
    var playlists: List<Playlist> = emptyList()
    for ((index, attempt) in attempts.withIndex()) {
        val (label, source) = attempt
        val url = siteUrls[index % siteUrls.size]
        val repository = CatalogRepository(
            indexUrl = url.toHttpUrl(),
            website = source,
            store = MemoryStore(),
            playlists = YouTubePlaylistSource(client),
            configuredPlaylistIds = playlistIds,
        )
        val error = repository.refresh(force = true)
        val catalog = repository.state.value.catalog
        if (catalog != null && catalog.playlists.size > playlists.size) playlists = catalog.playlists
        val flyers = catalog?.flyerCount ?: 0
        println("website ($label): ${error ?: "ok"}, ${catalog?.languages?.size ?: 0} languages, $flyers flyers")
        if (catalog != null && flyers > 0 && (error == null || error == CatalogError.Partial)) {
            report.append("- Website: read from **$label** ($flyers flyers").append(if (error != null) ", some pages failed" else "").append(")\n")
            website = catalog
            break
        }
    }

    // 2. Copy the website's flyers into the repository.
    val websiteLanguages: List<Language> = if (website != null) {
        mirror(website.languages, layout, client).also { mirrored ->
            layout.websiteManifest.parentFile.mkdirs()
            layout.websiteManifest.writeText(json.encodeToString(Catalog.serializer(), Catalog(languages = mirrored)))
        }
    } else {
        report.append("- Website: **not reachable** (live or archived); keeping the last copy\n")
        layout.websiteManifest.takeIf { it.isFile }?.let { CatalogJson.decodeCatalog(it.readText()).languages }.orEmpty()
    }

    // 3. Uploaded flyers.
    val uploaded = readUploadedFlyers(layout)
    report.append("- Uploaded flyers: ${uploaded.sumOf { it.flyers.size }} in ${uploaded.size} languages\n")

    // 4. Videos: keep yesterday's playlists if YouTube can't be read today.
    if (playlists.isEmpty()) {
        playlists = previous?.playlists.orEmpty()
        report.append("- YouTube: **not readable**; keeping ${playlists.size} playlists from the last build\n")
    } else {
        report.append("- YouTube: ${playlists.size} playlists, ${playlists.sumOf { it.videos.size }} videos\n")
    }

    val catalog = Catalog(languages = merge(websiteLanguages, uploaded), playlists = playlists)
    report.append("\n| Language | Flyers |\n|---|---|\n")
    catalog.languages.forEach { report.append("| ${it.name} (${it.nativeName}) | ${it.flyers.size} |\n") }

    // Publishing nothing would hide what phones already have; fail the run so it gets noticed.
    if (catalog.languages.isEmpty() && catalog.playlists.isEmpty()) {
        report.append("\n**Nothing could be read, so catalog.json was left as it is.**\n")
        println(report)
        System.getenv("GITHUB_STEP_SUMMARY")?.let { File(it).appendText(report.toString()) }
        exitProcess(1)
    }

    val output = json.encodeToString(Catalog.serializer(), catalog) + "\n"
    if (layout.catalog.takeIf { it.isFile }?.readText() != output) {
        layout.catalog.writeText(output)
        report.append("\ncatalog.json updated.\n")
    } else {
        report.append("\nNo changes.\n")
    }
    println(report)
    System.getenv("GITHUB_STEP_SUMMARY")?.let { File(it).appendText(report.toString()) }
}

/** Downloads every flyer once, then publishes display copies and previews. */
private suspend fun mirror(languages: List<Language>, layout: LibraryLayout, client: OkHttpClient): List<Language> {
    val downloader = Downloader(client)
    val semaphore = Semaphore(6)
    return kotlinx.coroutines.coroutineScope {
        languages.map { language ->
            val dir = File(layout.mirror, language.code)
            val flyers = language.flyers.map { flyer ->
                async { semaphore.withPermit { mirrorFlyer(flyer, dir, layout, downloader) } }
            }.awaitAll()
            println("  ${language.code}: ${flyers.count { layout.isPublished(it.image) }}/${flyers.size} copied")
            language.copy(pageUrl = null, flyers = flyers)
        }
    }
}

private suspend fun mirrorFlyer(flyer: Flyer, dir: File, layout: LibraryLayout, downloader: Downloader): Flyer {
    val pdf = flyer.pdf
    if (pdf != null) {
        val pdfFile = File(dir, "${flyer.id}.pdf")
        if (!downloader.download(pdf, pdfFile)) return flyer
        val pdfUrl = layout.url(pdfFile)
        if (flyer.image == pdf) return flyer.copy(image = pdfUrl, pdf = pdfUrl, thumbnail = null)
        val cover = File(dir, "${flyer.id}-cover.${extension(flyer.image)}")
        return if (downloader.download(flyer.image, cover)) {
            publish(layout, cover, dir, flyer.title, flyer.id).copy(pdf = pdfUrl)
        } else {
            flyer.copy(pdf = pdfUrl)
        }
    }
    val original = File(dir, "${flyer.id}.${extension(flyer.image)}")
    if (!downloader.download(flyer.image, original)) {
        // Fall back to the preview if the full size was never online.
        val preview = flyer.thumbnail ?: return flyer
        if (!downloader.download(preview, original)) return flyer
    }
    return publish(layout, original, dir, flyer.title, flyer.id)
}

private fun extension(url: String): String =
    url.substringBefore('?').substringAfterLast('/').substringAfterLast('.', "jpg").lowercase()
        .takeIf { it in setOf("jpg", "jpeg", "png", "webp", "pdf") } ?: "jpg"

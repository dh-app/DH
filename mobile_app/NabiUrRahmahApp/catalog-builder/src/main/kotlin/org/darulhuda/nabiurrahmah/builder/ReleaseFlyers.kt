package org.darulhuda.nabiurrahmah.builder

import java.io.File
import java.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.darulhuda.nabiurrahmah.data.model.Flyer
import org.darulhuda.nabiurrahmah.data.model.Language
import org.darulhuda.nabiurrahmah.data.site.KnownLanguages

/**
 * Flyer files too big for GitHub's upload page (over 25 MB), attached instead to the
 * repository's release tagged [TAG], which takes files up to 2 GB and keeps them out
 * of the repository itself. Each file names its language, e.g. "Nabi ur Rahmah Urdu.pdf".
 *
 * Only the rendered pages are kept in the repository, under a name tied to the
 * attached file's id and date: a file is downloaded and rendered once, and again
 * only when it is replaced. Pages of files no longer attached are removed.
 */
class ReleaseFlyers(
    private val client: OkHttpClient,
    private val repository: String,
    private val token: String? = System.getenv("GITHUB_TOKEN"),
) {

    data class Asset(val id: Long, val name: String, val downloadUrl: String, val updatedAt: String)

    /** The attached files, or null when the release can't be read (offline, rate limit). */
    fun assets(): List<Asset>? {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$repository/releases/tags/$TAG")
            .header("Accept", "application/vnd.github+json")
            .apply { if (!token.isNullOrBlank()) header("Authorization", "Bearer $token") }
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                when {
                    response.code == 404 -> emptyList() // no release yet
                    !response.isSuccessful -> null
                    else -> parse(response.body?.string().orEmpty())
                }
            }
        } catch (e: IOException) {
            null
        }
    }

    fun read(layout: LibraryLayout, report: StringBuilder): List<Language> {
        val assets = assets()
        val dir = File(layout.derived, "_updated/_release")
        if (assets == null) {
            report.append("- Release `$TAG`: **not readable**; keeping its pages from the last build\n")
            return fromExisting(layout, dir)
        }
        val byLanguage = LinkedHashMap<String, MutableList<Flyer>>()
        val keep = HashSet<String>()
        var skipped = 0
        for (asset in assets) {
            val extension = asset.name.substringAfterLast('.', "").lowercase()
            val language = KnownLanguages.mentionedIn(asset.name.substringBeforeLast('.').replace(Regex("[._-]+"), " "))
            if (extension !in setOf("pdf", "jpg", "jpeg", "png") || language == null) {
                println("  ! release file '${asset.name}' skipped: not a PDF or image, or no language in its name")
                skipped++
                continue
            }
            val prefix = prefixFor(asset)
            keep += prefix
            val flyers = try {
                if (extension == "pdf") pdfFlyers(layout, asset, prefix, dir) else imageFlyer(layout, asset, prefix, extension, dir)
            } catch (e: Exception) {
                println("  ! release file '${asset.name}' failed: ${e.message}")
                skipped++
                emptyList()
            }
            println("  release ${asset.name}: ${flyers.size} flyers (${language.name})")
            byLanguage.getOrPut(language.code) { mutableListOf() } += flyers
        }
        // Pages of files that were replaced or removed.
        dir.listFiles().orEmpty().filter { file -> keep.none { file.name.startsWith(it) } }.forEach { it.delete() }
        report.append("- Release `$TAG`: ${assets.size - skipped} files, ${byLanguage.values.sumOf { it.size }} flyers")
            .append(if (skipped > 0) "; **$skipped skipped** (see the log)\n" else "\n")
        return languages(byLanguage)
    }

    private fun pdfFlyers(layout: LibraryLayout, asset: Asset, prefix: String, dir: File): List<Flyer> {
        val marker = File(dir, "$prefix.done")
        var pages = PdfPages.existing(prefix, dir)
        if (!marker.isFile || pages.isEmpty()) {
            val pdf = download(asset, File(CACHE, "$prefix.pdf"))
            try {
                pages = PdfPages.render(pdf, dir, prefix)
                marker.writeText(asset.name)
            } finally {
                pdf.delete()
            }
        }
        return pageFlyers(layout, pages, dir, baseId = "rel-${asset.id}", title = null)
    }

    private fun imageFlyer(layout: LibraryLayout, asset: Asset, prefix: String, extension: String, dir: File): List<Flyer> {
        val file = File(dir, "$prefix.$extension")
        if (!file.isFile) download(asset, file)
        return listOf(publish(layout, file, dir, title = titleFrom(asset.name), id = "rel-${asset.id}"))
    }

    /** When the release can't be read, its pages from the last build stay published. */
    private fun fromExisting(layout: LibraryLayout, dir: File): List<Language> {
        val byLanguage = LinkedHashMap<String, MutableList<Flyer>>()
        dir.listFiles { f -> f.name.endsWith(".done") }.orEmpty().sortedBy { it.name }.forEach { marker ->
            val prefix = marker.name.removeSuffix(".done")
            val language = KnownLanguages.mentionedIn(marker.readText().substringBeforeLast('.').replace(Regex("[._-]+"), " ")) ?: return@forEach
            val id = Regex("-a(\\d+)-").find(prefix)?.groupValues?.get(1) ?: prefix
            byLanguage.getOrPut(language.code) { mutableListOf() } +=
                pageFlyers(layout, PdfPages.existing(prefix, dir), dir, baseId = "rel-$id", title = null)
        }
        return languages(byLanguage)
    }

    private fun languages(byLanguage: Map<String, List<Flyer>>): List<Language> = byLanguage.mapNotNull { (code, flyers) ->
        val known = KnownLanguages.byCode(code) ?: return@mapNotNull null
        Language(code = code, name = known.name, nativeName = known.nativeName, rtl = known.rtl, flyers = flyers)
    }.filter { it.flyers.isNotEmpty() }

    private fun download(asset: Asset, target: File): File {
        target.parentFile.mkdirs()
        val partial = File(target.path + ".part")
        client.newCall(Request.Builder().url(asset.downloadUrl).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} downloading ${asset.name}")
            val body = response.body ?: throw IOException("Empty download of ${asset.name}")
            partial.outputStream().use { out -> body.byteStream().use { it.copyTo(out) } }
        }
        if (!partial.renameTo(target)) throw IOException("Could not store ${asset.name}")
        return target
    }

    companion object {
        /** The release (Releases → this tag) that large flyer files are attached to. */
        const val TAG = "flyers"

        /** Downloads are temporary and never committed. */
        private val CACHE = File(System.getProperty("java.io.tmpdir"), "nur-release-flyers")

        internal fun prefixFor(asset: Asset): String {
            val stem = asset.name.substringBeforeLast('.').lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(40)
            val version = asset.updatedAt.filter(Char::isDigit).takeLast(10)
            return "$stem-a${asset.id}-$version"
        }

        internal fun parse(body: String): List<Asset> {
            val release = Json.parseToJsonElement(body) as? JsonObject ?: return emptyList()
            return (release["assets"] as? JsonArray).orEmpty().mapNotNull { element ->
                val asset = element as? JsonObject ?: return@mapNotNull null
                Asset(
                    id = asset["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null,
                    name = asset["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null,
                    downloadUrl = asset["browser_download_url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null,
                    updatedAt = asset["updated_at"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                )
            }.sortedBy { it.name.lowercase() }
        }

        private fun JsonArray?.orEmpty(): List<kotlinx.serialization.json.JsonElement> = this ?: emptyList()
    }
}

package org.darulhuda.nabiurrahmah.builder

import java.io.File
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.darulhuda.nabiurrahmah.data.library.GitHubReleaseSource
import org.darulhuda.nabiurrahmah.data.library.LanguageNames
import org.darulhuda.nabiurrahmah.data.model.Flyer
import org.darulhuda.nabiurrahmah.data.model.Language
import org.darulhuda.nabiurrahmah.data.site.KnownLanguages
import org.darulhuda.nabiurrahmah.data.site.NabiSiteParser

/** Where published files live, and their public addresses. */
class LibraryLayout(val root: File, publicBase: String) {
    private val base: HttpUrl = publicBase.toHttpUrl()

    /** True for files published from this repository (as opposed to still pointing at the website). */
    fun isPublished(url: String): Boolean = url.startsWith(base.toString())

    val flyers = File(root, "flyers")
    val mirror = File(flyers, "_from-website")
    val derived = File(flyers, "_generated")
    val catalog = File(root, "catalog.json")
    val websiteManifest = File(mirror, "manifest.json")

    /** Public URL of a file under [root], each path segment encoded. */
    fun url(file: File): String {
        val relative = file.canonicalFile.relativeTo(root.canonicalFile).invariantSeparatorsPath
        return base.newBuilder().apply { relative.split('/').forEach { addPathSegment(it) } }.build().toString()
    }
}

private val FLYER_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "pdf")
private val CAMERA_NAMES = Regex("(?i)^(img|dsc|pxl|scan|screenshot|whatsapp image|\\d)")

/**
 * Flyers uploaded straight to GitHub: `library/flyers/<Language>/<file>`, the
 * folder named in English or the language's own script ("Hindi", "اردو").
 */
fun readUploadedFlyers(layout: LibraryLayout): List<Language> {
    val folders = layout.flyers.listFiles { f -> f.isDirectory && !f.name.startsWith("_") && !f.name.startsWith(".") }
        .orEmpty().sortedBy { it.name.lowercase() }
    return folders.mapNotNull { folder ->
        val known = KnownLanguages.match(folder.name, maxWords = 4) ?: KnownLanguages.mentionedIn(folder.name)
        val code = known?.code ?: LanguageNames.fromScript(folder.name) ?: return@mapNotNull null.also {
            println("  ! skipped folder '${folder.name}': not a recognised language name")
        }
        val files = folder.listFiles { f -> f.isFile && f.extension.lowercase() in FLYER_EXTENSIONS }.orEmpty().sortedBy { it.name.lowercase() }
        val flyers = files.map { file -> publish(layout, file, File(layout.derived, folder.name), title = titleFrom(file.name)) }
        Language(
            code = code,
            name = known?.name ?: LanguageNames.english(code),
            nativeName = known?.nativeName ?: LanguageNames.native(code),
            rtl = known?.rtl ?: LanguageNames.isRtl(code),
            flyers = flyers,
        )
    }.filter { it.flyers.isNotEmpty() }
}

/** A published flyer from a local file: display copy, preview and size. */
fun publish(layout: LibraryLayout, file: File, derivedDir: File, title: String?, id: String? = null): Flyer {
    val baseName = file.nameWithoutExtension
    val flyerId = id ?: "up-" + NabiSiteParser.canonicalKey(file.path).hashCode().toUInt().toString(36)
    if (file.extension.equals("pdf", ignoreCase = true)) {
        val url = layout.url(file)
        return Flyer(id = flyerId, image = url, pdf = url, title = title)
    }
    val processed = Images.process(file, derivedDir, baseName)
    return Flyer(
        id = flyerId,
        image = layout.url(processed.display),
        thumbnail = processed.thumbnail?.let(layout::url),
        title = title,
        width = processed.width,
        height = processed.height,
    )
}

internal fun titleFrom(fileName: String): String? {
    val (title, _) = GitHubReleaseSource.describe(fileName)
    return title.takeUnless { CAMERA_NAMES.containsMatchIn(it) || it.length < 3 }
}

/** Website languages first, in the site's order; uploads join their language or add a new one. */
fun merge(website: List<Language>, uploaded: List<Language>): List<Language> {
    val byCode = LinkedHashMap<String, Language>()
    website.forEach { byCode[it.code] = it }
    uploaded.forEach { upload ->
        val existing = byCode[upload.code]
        byCode[upload.code] = existing?.copy(flyers = (existing.flyers + upload.flyers).distinctBy { it.id }) ?: upload
    }
    return byCode.values.toList()
}

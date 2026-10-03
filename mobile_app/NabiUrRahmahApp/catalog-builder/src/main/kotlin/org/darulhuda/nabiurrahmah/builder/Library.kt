package org.darulhuda.nabiurrahmah.builder

import java.io.File
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.darulhuda.nabiurrahmah.data.library.GitHubReleaseSource
import org.darulhuda.nabiurrahmah.data.library.LanguageNames
import org.darulhuda.nabiurrahmah.data.model.Flyer
import org.darulhuda.nabiurrahmah.data.model.Language
import org.darulhuda.nabiurrahmah.data.site.KnownLanguage
import org.darulhuda.nabiurrahmah.data.site.KnownLanguages
import org.darulhuda.nabiurrahmah.data.site.NabiSiteParser

/** Where published files live, and their public addresses. */
class LibraryLayout(val root: File, publicBase: String) {
    private val base: HttpUrl = publicBase.toHttpUrl()

    /** True for files published from this repository (as opposed to still pointing at the website). */
    fun isPublished(url: String): Boolean = url.startsWith(base.toString())

    val flyers = File(root, "flyers")
    /** A second upload folder, for new and updated flyers. */
    val updatedFlyers = File(root, "Flyers_Updated")
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
 * Flyers uploaded straight to GitHub, in `library/flyers/` or `library/Flyers_Updated/`:
 * one folder per language, named in English or the language's own script ("Hindi",
 * "اردو"). A file placed directly in one of those folders counts too when its name
 * mentions the language ("Urdu flyers.pdf").
 */
fun readUploadedFlyers(layout: LibraryLayout): List<Language> {
    val byLanguage = LinkedHashMap<String, Pair<KnownLanguage?, MutableList<Flyer>>>()
    fun add(code: String, known: KnownLanguage?, flyers: List<Flyer>) {
        byLanguage.getOrPut(code) { known to mutableListOf() }.second += flyers
    }
    for (root in listOf(layout.flyers, layout.updatedFlyers)) {
        val entries = root.listFiles { f -> !f.name.startsWith("_") && !f.name.startsWith(".") }.orEmpty().sortedBy { it.name.lowercase() }
        val derivedRoot = if (root == layout.flyers) layout.derived else File(layout.derived, "_updated")
        for (folder in entries.filter { it.isDirectory }) {
            val known = KnownLanguages.match(folder.name, maxWords = 4) ?: KnownLanguages.mentionedIn(folder.name)
            val code = known?.code ?: LanguageNames.fromScript(folder.name)
            if (code == null) {
                println("  ! skipped folder '${folder.name}': not a recognised language name")
                continue
            }
            val files = folder.listFiles { f -> f.isFile && f.extension.lowercase() in FLYER_EXTENSIONS }.orEmpty().toList()
            add(code, known, flyersFrom(layout, inNumberOrder(files), File(derivedRoot, folder.name)))
        }
        for (file in inNumberOrder(entries.filter { it.isFile && it.extension.lowercase() in FLYER_EXTENSIONS })) {
            val known = KnownLanguages.mentionedIn(file.nameWithoutExtension.replace('_', ' ').replace('-', ' '))
            if (known == null) {
                println("  ! skipped '${file.name}': put it in a language folder, or name the language in the file name")
                continue
            }
            add(known.code, known, flyersFrom(layout, listOf(file), File(derivedRoot, "_loose")))
        }
    }
    return byLanguage.map { (code, entry) ->
        val (known, flyers) = entry
        Language(
            code = code,
            name = known?.name ?: LanguageNames.english(code),
            nativeName = known?.nativeName ?: LanguageNames.native(code),
            rtl = known?.rtl ?: LanguageNames.isRtl(code),
            flyers = flyers.distinctBy { it.id },
        )
    }.filter { it.flyers.isNotEmpty() }
}

private fun inNumberOrder(files: List<File>): List<File> =
    files.sortedWith(
        compareBy<File> { Regex("\\d+").find(it.nameWithoutExtension)?.value?.take(12)?.toLongOrNull() ?: Long.MAX_VALUE }
            .thenBy { it.name.lowercase() },
    )

private fun flyersFrom(layout: LibraryLayout, files: List<File>, derived: File): List<Flyer> = files.flatMap { file ->
    if (file.extension.equals("pdf", ignoreCase = true)) {
        pdfFlyers(layout, file, derived)
    } else {
        listOf(publish(layout, file, derived, title = titleFrom(file.name)))
    }
}

/**
 * One flyer per page of an uploaded PDF, so each page can be viewed, saved and
 * shared on its own. A PDF that can't be read is offered whole instead.
 */
private fun pdfFlyers(layout: LibraryLayout, pdf: File, derivedDir: File): List<Flyer> {
    val title = titleFrom(pdf.name)
    val pages = try {
        PdfPages.render(pdf, derivedDir)
    } catch (e: Exception) {
        println("  ! could not read ${pdf.name}: ${e.message}")
        return listOf(publish(layout, pdf, derivedDir, title))
    }
    return pageFlyers(layout, pages, derivedDir, baseId = uploadId(pdf), title = title)
}

/**
 * Flyers for rendered pages. A many-page PDF is a set of flyers, each with its
 * own content, so its pages carry no title; a one-page PDF keeps its name.
 */
internal fun pageFlyers(layout: LibraryLayout, pages: List<File>, derivedDir: File, baseId: String, title: String?): List<Flyer> =
    pages.mapIndexed { index, page ->
        publish(layout, page, derivedDir, title.takeIf { pages.size == 1 }, id = "$baseId-p${index + 1}")
    }

private fun uploadId(file: File): String =
    "up-" + NabiSiteParser.canonicalKey(file.path).hashCode().toUInt().toString(36)

/** A published flyer from a local file: display copy, preview and size. */
fun publish(layout: LibraryLayout, file: File, derivedDir: File, title: String?, id: String? = null): Flyer {
    val baseName = file.nameWithoutExtension
    val flyerId = id ?: uploadId(file)
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

/**
 * Flyers in their series order (1, 2, 3 … 97), read from the number in each file
 * name: `Nabi-ur-Rahma-Urdu_page-0001.jpg` is 1. The website's own order is kept
 * when most names carry no number; flyers without one go last.
 */
fun inSeriesOrder(flyers: List<Flyer>): List<Flyer> {
    val numbers = flyers.map { seriesNumber(it.image) }
    if (numbers.count { it != null } < flyers.size * 0.8) return flyers
    return flyers.zip(numbers).sortedWith(compareBy(nullsLast()) { it.second }).map { it.first }
}

private fun seriesNumber(url: String): Long? {
    val stem = url.substringBefore('?').substringAfterLast('/').substringBeforeLast('.')
        .replace(Regex("-\\d{2,5}x\\d{2,5}$"), "")
        .removeSuffix("-scaled")
    return Regex("\\d+").findAll(stem).lastOrNull()?.value?.take(12)?.toLongOrNull()
}

/**
 * A language uploaded to `Flyers_Updated` replaces the website's copies of that
 * language, so flyers don't appear twice. Its place in the list is kept.
 */
fun withoutReplaced(website: List<Language>, uploaded: List<Language>, layout: LibraryLayout): List<Language> {
    val replaced = replacedCodes(uploaded, layout)
    return website.map { if (it.code in replaced) it.copy(flyers = emptyList()) else it }
}

/** Languages with flyers from `Flyers_Updated` or the flyers release. */
fun replacedCodes(uploaded: List<Language>, layout: LibraryLayout): Set<String> {
    val updatedPrefix = layout.url(layout.updatedFlyers)
    val generatedPrefix = layout.url(File(layout.derived, "_updated"))
    return uploaded.filter { language ->
        language.flyers.any { it.image.startsWith(updatedPrefix) || it.image.startsWith(generatedPrefix) }
    }.map { it.code }.toSet()
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

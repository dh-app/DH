package org.darulhuda.nabiurrahmah.data.site

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.darulhuda.nabiurrahmah.data.model.Flyer
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/** A language found on the Nabi ur Rahmah page. */
data class SiteLanguage(
    val language: KnownLanguage,
    /** Page that lists this language's flyers, if it has its own page. */
    val pageUrl: String?,
    /** Flyers shown directly on the index page under this language. */
    val flyers: List<Flyer>,
)

data class SiteIndex(
    val languages: List<SiteLanguage>,
    /** Images on the index page that are not flyers (banners, decoration); ignored on language pages too. */
    val decorationKeys: Set<String>,
)

/**
 * Reads flyers from darulhudaudupi.org without any manual publishing step.
 *
 * The site is edited by people, not generated for this app, so the parser does
 * not depend on one exact layout. Walking the page in reading order, it understands:
 *  - links to a page per language ("Urdu", "ಕನ್ನಡ", /nabi-ur-rahmah-tamil/),
 *  - flyers placed under a language heading or tab on the page itself,
 *  - direct links to flyer files labelled with a language,
 * and for each flyer picks a small rendition for grids and a large one for the viewer.
 */
class NabiSiteParser {

    fun parseIndex(html: String, pageUrl: HttpUrl): SiteIndex {
        val root = contentRoot(Jsoup.parse(html, pageUrl.toString()))
        val builders = LinkedHashMap<String, LanguageBuilder>()
        val unattributed = LinkedHashMap<String, Flyer>()
        var current: KnownLanguage? = null

        fun builder(language: KnownLanguage) = builders.getOrPut(language.code) { LanguageBuilder(language) }

        walk(root) { element ->
            when {
                element.tagName() == "a" && element.hasAttr("href") -> {
                    val href = resolve(element.attr("href"), pageUrl)
                    val label = linkLabel(element)
                    val file = href?.let(::fileKind)
                    val language = KnownLanguages.match(label)
                        ?: href?.takeIf { file == null && isSameSite(it, pageUrl) }?.let { KnownLanguages.matchSlug(it.encodedPath) }
                    when {
                        href == null -> false
                        language != null && file != null -> {
                            flyerFrom(element, href, file)?.let { builder(language).add(it) }
                            true
                        }
                        language != null && isSameSite(href, pageUrl) && !isSamePage(href, pageUrl) -> {
                            builder(language).pageUrl = builder(language).pageUrl ?: href.withoutFragment()
                            true
                        }
                        file != null -> {
                            flyerFrom(element, href, file)?.let { flyer ->
                                val target = current
                                if (target != null) builder(target).add(flyer) else unattributed[flyer.id] = flyer
                            }
                            true
                        }
                        else -> false
                    }
                }
                element.hasAttr("aria-labelledby") -> {
                    // Tab panels (Elementor and others) name their language in a separate tab title.
                    val title = element.ownerDocument()?.getElementById(element.attr("aria-labelledby"))
                    title?.let { KnownLanguages.match(it.text()) }?.let { current = it; builder(it) }
                    false
                }
                element.isHeadingLike() -> {
                    KnownLanguages.match(element.text())?.let { current = it; builder(it) }
                    false
                }
                element.tagName() == "img" -> {
                    flyerFrom(element, null, null)?.let { flyer ->
                        val target = current
                        if (target != null) builder(target).add(flyer) else unattributed[flyer.id] = flyer
                    }
                    false
                }
                else -> false
            }
        }

        val languages = builders.values
            .filter { it.pageUrl != null || it.flyers.isNotEmpty() }
            .map { SiteLanguage(it.language, it.pageUrl, it.flyers.values.toList()) }

        return if (languages.isEmpty() && unattributed.isNotEmpty()) {
            // No language structure at all: still show what is there.
            SiteIndex(listOf(SiteLanguage(ALL_LANGUAGES, null, unattributed.values.toList())), emptySet())
        } else {
            SiteIndex(languages, unattributed.keys)
        }
    }

    /** All flyers on a language page, in reading order. */
    fun parseFlyers(html: String, pageUrl: HttpUrl, decorationKeys: Set<String> = emptySet()): List<Flyer> {
        val root = contentRoot(Jsoup.parse(html, pageUrl.toString()))
        val flyers = LinkedHashMap<String, Flyer>()
        walk(root) { element ->
            when {
                element.tagName() == "a" && element.hasAttr("href") -> {
                    val href = resolve(element.attr("href"), pageUrl)
                    val file = href?.let(::fileKind)
                    if (href != null && file != null) {
                        flyerFrom(element, href, file)?.let { flyers.putIfAbsent(it.id, it) }
                        true
                    } else {
                        false
                    }
                }
                element.tagName() == "img" -> {
                    flyerFrom(element, null, null)?.let { flyers.putIfAbsent(it.id, it) }
                    false
                }
                else -> false
            }
        }
        return flyers.values.filter { it.id !in decorationKeys }
    }

    // region Page structure

    private fun contentRoot(document: Document): Element {
        document.select(NOISE).remove()
        return document.selectFirst(CONTENT_ROOTS) ?: document.body()
    }

    /** Depth-first, reading order. [visit] returns true to skip the element's children. */
    private fun walk(element: Element, visit: (Element) -> Boolean) {
        for (child in element.children()) {
            if (!visit(child)) walk(child, visit)
        }
    }

    private fun Element.isHeadingLike(): Boolean =
        tagName() in HEADING_TAGS ||
            classNames().any { name -> HEADING_CLASS_HINTS.any { name.contains(it) } } ||
            attr("role") == "tab" ||
            // A paragraph that is only bold text works as a heading on many pages.
            (tagName() in setOf("p", "div") && children().size == 1 && children().first()!!.tagName() in setOf("strong", "b") &&
                ownText().isBlank())

    private fun linkLabel(anchor: Element): String =
        anchor.text().ifBlank {
            anchor.attr("title").ifBlank { anchor.selectFirst("img")?.attr("alt").orEmpty() }
        }

    // endregion

    // region Flyers

    private enum class FileKind { Image, Pdf, Drive }

    private fun fileKind(url: HttpUrl): FileKind? {
        if (driveFileId(url) != null) return FileKind.Drive
        return when (url.pathSegments.lastOrNull()?.substringAfterLast('.', "")?.lowercase()) {
            "pdf" -> FileKind.Pdf
            in IMAGE_EXTENSIONS -> FileKind.Image
            else -> null
        }
    }

    /** A flyer from a link to a file (with its preview image, if any) or from a lone image. */
    private fun flyerFrom(element: Element, href: HttpUrl?, kind: FileKind?): Flyer? {
        val img = if (element.tagName() == "img") element else element.selectFirst("img")
        val renditions = img?.takeUnless(::isDecoration)?.let(::renditions).orEmpty()
        if (href == null && renditions.isEmpty()) return null

        val large = renditions.maxByOrNull { it.width ?: 0 }?.url
        val small = renditions
            .filter { (it.width ?: Int.MAX_VALUE) >= THUMBNAIL_MIN_WIDTH }
            .minByOrNull { it.width ?: Int.MAX_VALUE }?.url
            ?: large

        val image: String
        var pdf: String? = null
        var key: String? = null
        when (kind) {
            FileKind.Image -> image = requireNotNull(href).toString()
            FileKind.Pdf -> {
                pdf = requireNotNull(href).toString()
                // With no preview image, the app renders the PDF's first page itself.
                image = large ?: pdf
            }
            FileKind.Drive -> {
                val id = requireNotNull(driveFileId(requireNotNull(href)))
                key = "https://drive.google.com/file/d/$id"
                image = large ?: "https://drive.google.com/thumbnail?id=$id&sz=w2000"
            }
            null -> image = large ?: return null
        }
        val thumbnail = small?.takeIf { it != image }

        val (width, height) = img?.let(::dimensions) ?: (null to null)
        return Flyer(
            id = flyerId(key ?: pdf ?: image),
            image = image,
            thumbnail = thumbnail,
            pdf = pdf,
            title = titleFor(element, img),
            width = width,
            height = height,
        )
    }

    private data class Rendition(val url: String, val width: Int?)

    /** Every URL an <img> offers, including lazy-loading attributes. */
    private fun renditions(img: Element): List<Rendition> {
        val base = img.baseUri().toHttpUrlOrNull()
        val fromSrcset = SRCSET_ATTRS.flatMap { attr ->
            img.attr(attr).split(',').mapNotNull { candidate ->
                val parts = candidate.trim().split(Regex("\\s+"))
                val url = parts.firstOrNull()?.let { resolve(it, base) }?.takeIf(::isImageUrl) ?: return@mapNotNull null
                Rendition(url.toString(), parts.getOrNull(1)?.removeSuffix("w")?.toIntOrNull())
            }
        }
        val fromSrc = SRC_ATTRS.mapNotNull { attr ->
            resolve(img.attr(attr), base)?.takeIf(::isImageUrl)?.let { Rendition(it.toString(), img.attr("width").toIntOrNull()) }
        }
        return (fromSrcset + fromSrc).distinctBy { it.url }
    }

    private fun isDecoration(img: Element): Boolean {
        val src = SRC_ATTRS.map(img::attr).firstOrNull { it.isNotBlank() && !it.startsWith("data:") }.orEmpty().lowercase()
        if (src.isEmpty() && SRCSET_ATTRS.all { img.attr(it).isBlank() }) return true
        if (src.endsWith(".svg") || src.endsWith(".gif")) return true
        if (DECORATION_HINTS.any { src.substringAfterLast('/').contains(it) }) return true
        val width = img.attr("width").toIntOrNull()
        val height = img.attr("height").toIntOrNull()
        return width != null && height != null && maxOf(width, height) < MIN_FLYER_SIZE
    }

    private fun dimensions(img: Element): Pair<Int?, Int?> {
        img.attr("data-orig-size").split(',').mapNotNull { it.trim().toIntOrNull() }.let {
            if (it.size == 2) return it[0] to it[1]
        }
        return img.attr("width").toIntOrNull() to img.attr("height").toIntOrNull()
    }

    private fun titleFor(element: Element, img: Element?): String? {
        val candidates = sequence {
            yield(element.closest("figure")?.selectFirst("figcaption")?.text())
            yield(img?.attr("title"))
            yield(img?.attr("alt"))
            if (element.tagName() == "a") yield(element.ownText())
        }
        return candidates
            .mapNotNull { it?.trim()?.replace(Regex("\\s+"), " ") }
            .firstOrNull { it.length in 3..120 && !looksLikeFileName(it) && KnownLanguages.match(it) == null }
    }

    private fun looksLikeFileName(text: String): Boolean =
        (!text.contains(' ') && (text.contains('-') || text.contains('_'))) ||
            FILE_NAME_PATTERNS.any { it.containsMatchIn(text) }

    /** Stable id: the same flyer keeps its id across refreshes and renditions. */
    private fun flyerId(url: String): String {
        val key = canonicalKey(url)
        val name = key.substringAfterLast('/').substringBeforeLast('.')
            .replace(Regex("[^a-z0-9]+"), "-").trim('-').take(40).ifEmpty { "flyer" }
        return "$name-${(key.hashCode().toLong() and 0xFFFFFFFFL).toString(36)}"
    }

    // endregion

    // region URLs

    private fun resolve(raw: String, base: HttpUrl?): HttpUrl? {
        val value = raw.trim()
        if (value.isEmpty() || value.startsWith("data:") || value.startsWith("javascript:") ||
            value.startsWith("mailto:") || value.startsWith("tel:")
        ) {
            return null
        }
        val url = (base?.resolve(value) ?: value.toHttpUrlOrNull()) ?: return null
        // Android blocks clear-text traffic; every host involved serves https.
        return if (url.scheme == "http") url.newBuilder().scheme("https").build() else url
    }

    private fun isImageUrl(url: HttpUrl): Boolean =
        url.pathSegments.lastOrNull()?.substringAfterLast('.', "")?.lowercase() in IMAGE_EXTENSIONS

    private fun isSameSite(url: HttpUrl, page: HttpUrl): Boolean =
        url.host.removePrefix("www.") == page.host.removePrefix("www.") &&
            SKIPPED_PATH_HINTS.none { url.encodedPath.contains(it) }

    private fun isSamePage(url: HttpUrl, page: HttpUrl): Boolean =
        url.encodedPath.trimEnd('/') == page.encodedPath.trimEnd('/')

    private fun HttpUrl.withoutFragment(): String = newBuilder().fragment(null).build().toString()

    private fun driveFileId(url: HttpUrl): String? {
        if (url.host != "drive.google.com" && url.host != "docs.google.com") return null
        val segments = url.pathSegments
        val index = segments.indexOf("d")
        return when {
            index >= 0 && segments.getOrNull(index - 1) == "file" -> segments.getOrNull(index + 1)
            segments.firstOrNull() in setOf("open", "uc") -> url.queryParameter("id")
            else -> null
        }?.takeIf { it.isNotBlank() }
    }

    // endregion

    private class LanguageBuilder(val language: KnownLanguage) {
        var pageUrl: String? = null
        val flyers = LinkedHashMap<String, Flyer>()
        fun add(flyer: Flyer) {
            flyers.putIfAbsent(flyer.id, flyer)
        }
    }

    companion object {
        val ALL_LANGUAGES = KnownLanguage("mul", "All flyers", "All flyers")

        /** Same flyer, any rendition: WordPress adds `-300x424` and `-scaled` to resized copies. */
        fun canonicalKey(url: String): String =
            url.substringBefore('?').substringBefore('#').lowercase()
                .replace(Regex("-\\d{2,5}x\\d{2,5}(?=\\.[a-z0-9]+$)"), "")
                .replace(Regex("-scaled(?=\\.[a-z0-9]+$)"), "")

        private const val THUMBNAIL_MIN_WIDTH = 480
        private const val MIN_FLYER_SIZE = 200

        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
        private val SRCSET_ATTRS = listOf("data-lazy-srcset", "data-srcset", "srcset")
        private val SRC_ATTRS = listOf("data-lazy-src", "data-src", "data-original", "data-orig-file", "src")
        private val HEADING_TAGS = setOf("h1", "h2", "h3", "h4", "h5", "h6", "summary", "button", "dt", "legend")
        private val HEADING_CLASS_HINTS = listOf("tab-title", "heading-title", "accordion-title", "toggle-title", "title")
        private val DECORATION_HINTS = listOf("logo", "icon", "favicon", "emoji", "avatar", "placeholder", "spinner", "loader", "banner-bg")
        private val SKIPPED_PATH_HINTS = listOf("/wp-admin", "/wp-login", "/feed", "/wp-json", "/cart", "/checkout")
        private val FILE_NAME_PATTERNS = listOf(
            Regex("(?i)^(img|dsc|pxl|screenshot|whatsapp image|scan)[ _-]?\\d"),
            Regex("(?i)\\.(jpe?g|png|webp|pdf)$"),
            Regex("^\\d[\\d _-]+$"),
        )

        private const val NOISE =
            "header, footer, nav, aside, script, style, template, form, iframe, " +
                "#masthead, #colophon, .site-header, .site-footer, .elementor-location-header, " +
                ".elementor-location-footer, .widget-area, .sidebar, .comments-area, .sharedaddy, " +
                ".jp-relatedposts, .screen-reader-text, .skip-link"

        private const val CONTENT_ROOTS =
            "article .entry-content, .entry-content, [data-elementor-type=wp-page], " +
                "[data-elementor-type=single-page], [data-elementor-type=single], main article, main, #main, #content"
    }
}

package org.darulhuda.nabiurrahmah.data.library

import java.util.Locale
import kotlinx.serialization.Serializable

/** One work, possibly published in several languages (editions). */
@Serializable
data class Book(
    val id: String,
    val editions: List<Edition>,
    /** Where the book comes from, credited wherever it is shown. Null for our own uploads. */
    val source: BookOrigin? = null,
) {
    val languages: List<String> get() = editions.map { it.language }.distinct()

    /** The edition to show first: the reader's language, then English, then the original. */
    fun preferredEdition(preferredLanguages: List<String>): Edition =
        preferredLanguages.firstNotNullOfOrNull { lang -> editions.firstOrNull { it.language == lang } }
            ?: editions.firstOrNull { it.language == "en" }
            ?: editions.first()
}

@Serializable
data class BookOrigin(val name: String, val url: String) {
    companion object {
        val IslamHouse = BookOrigin(name = "www.islamhouse.com", url = "https://www.islamhouse.com")
    }
}

@Serializable
data class Edition(
    val id: String,
    /** ISO 639 code, e.g. "en", "ur". */
    val language: String,
    val title: String,
    val author: String? = null,
    val description: String? = null,
    val files: List<BookFile>,
) {
    val pdf: BookFile? get() = files.firstOrNull { it.format == "pdf" } ?: files.firstOrNull()
}

@Serializable
data class BookFile(
    val url: String,
    val format: String = "pdf",
    /** As published, e.g. "2.6 MB". */
    val size: String? = null,
    val label: String? = null,
)

@Serializable
data class Shelf(
    val books: List<Book> = emptyList(),
    val fetchedAt: Long = 0,
)

/** Display names for any language code, in English and in the language itself. */
object LanguageNames {
    private val RTL = setOf("ar", "ur", "fa", "ps", "he", "ug", "sd", "ckb", "dv", "yi", "ks")

    fun english(code: String): String =
        Locale.forLanguageTag(code).getDisplayLanguage(Locale.ENGLISH).ifBlank { code.uppercase() }

    fun native(code: String): String {
        val locale = Locale.forLanguageTag(code)
        return locale.getDisplayLanguage(locale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            .ifBlank { english(code) }
    }

    fun isRtl(code: String): Boolean = code in RTL

    /**
     * The language a title is written in, judged by its script: Urdu is told apart
     * from Arabic by letters only Urdu uses (ی ے ں ٹ ڈ ڑ ہ گ چ پ ژ). Null for Latin text.
     */
    fun fromScript(text: String): String? {
        // Honorific ligatures such as ﷺ appear in titles of every language; they don't say which.
        val letters = text.filter { it.isLetter() && it.code !in 0xFDF0..0xFDFF }
        if (letters.isEmpty()) return null
        val dominant = letters.groupingBy { script(it) }.eachCount().maxByOrNull { it.value }?.key
        if (dominant == null || dominant == "latin") return null
        return if (dominant == "arabic") {
            if (letters.any { it in URDU_LETTERS }) "ur" else "ar"
        } else {
            dominant
        }
    }

    private const val URDU_LETTERS = "یےںٹڈڑہگچپژھۓ"

    private fun script(c: Char): String = when (c.code) {
        in 0x0600..0x06FF, in 0x0750..0x077F, in 0xFB50..0xFDFF, in 0xFE70..0xFEFF -> "arabic"
        in 0x0900..0x097F -> "hi"
        in 0x0980..0x09FF -> "bn"
        in 0x0A00..0x0A7F -> "pa"
        in 0x0A80..0x0AFF -> "gu"
        in 0x0B00..0x0B7F -> "or"
        in 0x0B80..0x0BFF -> "ta"
        in 0x0C00..0x0C7F -> "te"
        in 0x0C80..0x0CFF -> "kn"
        in 0x0D00..0x0D7F -> "ml"
        in 0x0400..0x04FF -> "ru"
        in 0x4E00..0x9FFF -> "zh"
        else -> "latin"
    }
}

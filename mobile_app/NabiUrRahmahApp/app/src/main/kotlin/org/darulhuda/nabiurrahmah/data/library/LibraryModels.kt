package org.darulhuda.nabiurrahmah.data.library

import java.util.Locale
import kotlinx.serialization.Serializable

/** One work, possibly published in several languages (editions). */
@Serializable
data class Book(
    val id: String,
    val editions: List<Edition>,
) {
    val languages: List<String> get() = editions.map { it.language }.distinct()

    /** The edition to show first: the reader's language, then English, then the original. */
    fun preferredEdition(preferredLanguages: List<String>): Edition =
        preferredLanguages.firstNotNullOfOrNull { lang -> editions.firstOrNull { it.language == lang } }
            ?: editions.firstOrNull { it.language == "en" }
            ?: editions.first()
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
}

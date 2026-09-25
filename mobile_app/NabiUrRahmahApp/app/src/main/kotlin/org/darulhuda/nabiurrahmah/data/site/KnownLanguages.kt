package org.darulhuda.nabiurrahmah.data.site

import java.text.Normalizer

/** A language the parser can recognise from link text, headings or URL slugs. */
data class KnownLanguage(
    val code: String,
    val name: String,
    val nativeName: String,
    val rtl: Boolean = false,
    /** Other spellings, lower case (English or native). */
    val aliases: List<String> = emptyList(),
)

/**
 * The languages Nabi ur Rahmah flyers are (or may be) published in. Recognition is
 * deliberately generous about spelling, and strict about context (see [match]).
 */
object KnownLanguages {

    val all: List<KnownLanguage> = listOf(
        KnownLanguage("en", "English", "English", aliases = listOf("eng")),
        KnownLanguage("ur", "Urdu", "اردو", rtl = true, aliases = listOf("اُردو")),
        KnownLanguage("hi", "Hindi", "हिन्दी", aliases = listOf("हिंदी")),
        KnownLanguage("ar", "Arabic", "العربية", rtl = true, aliases = listOf("عربي", "عربى", "arabi")),
        KnownLanguage("kn", "Kannada", "ಕನ್ನಡ", aliases = listOf("kanada")),
        KnownLanguage("ml", "Malayalam", "മലയാളം"),
        KnownLanguage("ta", "Tamil", "தமிழ்"),
        KnownLanguage("te", "Telugu", "తెలుగు"),
        KnownLanguage("tcy", "Tulu", "ತುಳು"),
        KnownLanguage("kok", "Konkani", "कोंकणी", aliases = listOf("konkni")),
        KnownLanguage("mr", "Marathi", "मराठी"),
        KnownLanguage("gu", "Gujarati", "ગુજરાતી", aliases = listOf("gujrati")),
        KnownLanguage("bn", "Bengali", "বাংলা", aliases = listOf("bangla")),
        KnownLanguage("pa", "Punjabi", "ਪੰਜਾਬੀ", aliases = listOf("panjabi")),
        KnownLanguage("or", "Odia", "ଓଡ଼ିଆ", aliases = listOf("oriya", "odiya")),
        KnownLanguage("as", "Assamese", "অসমীয়া"),
        KnownLanguage("ne", "Nepali", "नेपाली"),
        KnownLanguage("si", "Sinhala", "සිංහල", aliases = listOf("sinhalese")),
        KnownLanguage("fr", "French", "Français", aliases = listOf("francais")),
        KnownLanguage("es", "Spanish", "Español", aliases = listOf("espanol")),
        KnownLanguage("de", "German", "Deutsch"),
        KnownLanguage("tr", "Turkish", "Türkçe", aliases = listOf("turkce")),
        KnownLanguage("ru", "Russian", "Русский"),
        KnownLanguage("zh", "Chinese", "中文", aliases = listOf("mandarin")),
        KnownLanguage("ja", "Japanese", "日本語"),
        KnownLanguage("ko", "Korean", "한국어"),
        KnownLanguage("id", "Indonesian", "Bahasa Indonesia", aliases = listOf("indonesia")),
        KnownLanguage("ms", "Malay", "Bahasa Melayu", aliases = listOf("melayu")),
        KnownLanguage("sw", "Swahili", "Kiswahili"),
        KnownLanguage("fa", "Persian", "فارسی", rtl = true, aliases = listOf("farsi")),
        KnownLanguage("ps", "Pashto", "پښتو", rtl = true),
        KnownLanguage("sd", "Sindhi", "سنڌي", rtl = true),
        KnownLanguage("ks", "Kashmiri", "کٲشُر", rtl = true),
        KnownLanguage("pt", "Portuguese", "Português", aliases = listOf("portugues")),
        KnownLanguage("it", "Italian", "Italiano"),
        KnownLanguage("nl", "Dutch", "Nederlands"),
        KnownLanguage("th", "Thai", "ไทย"),
        KnownLanguage("vi", "Vietnamese", "Tiếng Việt"),
        KnownLanguage("tl", "Filipino", "Filipino", aliases = listOf("tagalog")),
        KnownLanguage("am", "Amharic", "አማርኛ"),
        KnownLanguage("so", "Somali", "Soomaali"),
        KnownLanguage("ha", "Hausa", "Hausa"),
    )

    private val byCode = all.associateBy { it.code }

    fun byCode(code: String): KnownLanguage? = byCode[code]

    /** Latin-script spellings (lower case, accents removed) → language. */
    private val latinWords: Map<String, KnownLanguage> = buildMap {
        for (language in all) {
            (listOf(language.name, language.nativeName) + language.aliases)
                .map(::fold)
                .filter { it.all { c -> c in 'a'..'z' || c == ' ' } }
                .forEach { put(it, language) }
        }
    }

    /** Non-Latin native names and aliases → language. */
    private val nativeNames: List<Pair<String, KnownLanguage>> =
        all.flatMap { language -> (listOf(language.nativeName) + language.aliases).map { it to language } }
            .filter { (name, _) -> name.any { it.code > 0x2FF } }

    /**
     * Finds the language a short piece of text names, e.g. "English", "Urdu Flyers",
     * "اردو", "Download in Kannada". Returns null for longer text so that sentences
     * that merely mention a language ("Arabic College admissions open") are ignored.
     */
    fun match(text: String, maxWords: Int = 3): KnownLanguage? {
        val words = fold(text).split(' ').filter { it.isNotEmpty() && it !in FILLER_WORDS }
        if (words.isEmpty() || words.size > maxWords) return null

        // Two-word names first ("bahasa indonesia"), then single words.
        for (size in 2 downTo 1) {
            words.windowed(size).forEach { window -> latinWords[window.joinToString(" ")]?.let { return it } }
        }
        return nativeNames.firstOrNull { (name, _) -> text.contains(name) }?.second
    }

    /** Recognises a language from a URL slug such as `/nabi-ur-rahmah-urdu/` or `/flyers/kannada`. */
    fun matchSlug(path: String): KnownLanguage? =
        path.lowercase()
            .split('/', '-', '_', '.')
            .filter { it.isNotEmpty() }
            .firstNotNullOfOrNull { latinWords[it] }

    /** Words that often surround a language name in labels and don't change its meaning. */
    private val FILLER_WORDS = setOf(
        "nabi", "ur", "al", "rahmah", "rehmah", "rahma", "rahmat", "rehmat", "prophet", "of", "mercy",
        "flyer", "flyers", "flier", "fliers", "pamphlet", "pamphlets", "leaflet", "leaflets", "brochure", "brochures",
        "poster", "posters", "download", "downloads", "read", "view", "click", "here", "open", "pdf", "pdfs",
        "in", "the", "for", "and", "language", "languages", "version", "edition", "translation", "series",
    )

    private fun fold(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
            .replace(Regex("[^a-z\\p{L} ]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}

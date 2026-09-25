package org.darulhuda.udupi.core.network

import org.jsoup.Jsoup
import org.darulhuda.udupi.core.model.Book
import org.darulhuda.udupi.core.model.Flyer
import org.darulhuda.udupi.core.model.LanguageLink
import org.darulhuda.udupi.core.model.QuranComplex

private const val NABI_URL = "https://darulhudaudupi.org/nabi-ur-rahmah/"
private const val BOOKS_URL = "https://darulhudaudupi.org/books-printing-distribution/"
private const val QURAN_URL = "https://darulhudaudupi.org/quran-printing-complex/"

/**
 * 🌐 Loads all supported Nabi ur Rahmah languages from website links.
 */
suspend fun loadNabiLanguages(): List<LanguageLink> {
  val html = fetchHtml(NABI_URL)
  val doc = Jsoup.parse(html)
  return doc.select("a[href]").mapNotNull { a ->
    val text = a.text().trim()
    val href = a.absUrl("href")
    if (text.matches(Regex("(?i).*english|arabic|urdu|hindi|malayalam|tamil|kannada|telugu|french|turkish|bangla|indonesian|swahili.*")))
      LanguageLink(text, guessCode(text), href)
    else null
  }.distinctBy { it.code }
    .sortedWith(compareBy({ if (it.code == "en") 0 else 1 }, { it.name.lowercase() }))
}

/**
 * Guesses a two-letter language code from a given name.
 */
private fun guessCode(name: String): String = when {
  name.contains("arab", true) -> "ar"
  name.contains("urdu", true) -> "ur"
  name.contains("hindi", true) -> "hi"
  name.contains("malayalam", true) -> "ml"
  name.contains("tamil", true) -> "ta"
  name.contains("kannada", true) -> "kn"
  name.contains("telugu", true) -> "te"
  name.contains("french", true) -> "fr"
  name.contains("turk", true) -> "tr"
  name.contains("bangla", true) || name.contains("beng", true) -> "bn"
  name.contains("indones", true) || name.contains("bahasa", true) -> "id"
  name.contains("swahili", true) -> "sw"
  name.contains("english", true) -> "en"
  else -> name.lowercase().take(2)
}

/**
 * 📄 Loads flyers for a given language link.
 */
suspend fun loadFlyersForLanguage(lang: LanguageLink): List<Flyer> {
  val html = fetchHtml(lang.url)
  val doc = Jsoup.parse(html)

  return doc.select("a[href]").mapNotNull { a ->
    val href = a.absUrl("href")
    val title = a.text().ifBlank { "Flyer" }

    if (href.endsWith(".pdf", true) || href.endsWith(".jpg", true) || href.endsWith(".png", true)) {
      Flyer(
        title = title,
        fileUrl = href,
        thumbnailUrl = null,
        language = lang.name
      )
    } else null
  }
}

/**
 * 📚 Loads book links from the “Books & Printing Distribution” page.
 */
suspend fun loadBooks(): List<Book> {
  val html = fetchHtml(BOOKS_URL)
  val doc = Jsoup.parse(html)

  return doc.select("a[href]").mapNotNull { a ->
    val href = a.absUrl("href")
    if (href.contains("/book", true) || href.endsWith(".pdf", true)) {
      Book(
        title = a.text().ifBlank { "Book" },
        detailUrl = href,
        languageCode = "en"
      )
    } else null
  }
}

/**
 * 📖 Loads content for the Qur’an Printing Complex page.
 */
suspend fun loadQuranComplex(): QuranComplex {
  val html = fetchHtml(QURAN_URL)
  val doc = Jsoup.parse(html)

  val title = doc.selectFirst("h1,h2")?.text()?.trim().orEmpty()
  val paragraphs = doc.select("p").map { it.text().trim() }.filter { it.isNotBlank() }
  val images = doc.select("img[src]").map { it.absUrl("src") }

  return QuranComplex(
    title = title,
    description = paragraphs.take(4).joinToString("\n\n"),
    images = images
  )
}

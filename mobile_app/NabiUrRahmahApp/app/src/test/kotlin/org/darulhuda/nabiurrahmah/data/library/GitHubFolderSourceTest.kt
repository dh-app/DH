package org.darulhuda.nabiurrahmah.data.library

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubFolderSourceTest {

    /** The notes exactly as uploaded to the repository (Windows line endings included). */
    private val authorTxt = "Book Name: Short Seerah of Prophet ﷺ \r\nAuthor: Shaikh Dr. Abu Omar Parvez Nakwa Madani\r\n\r\n\r\nBook Name:A Concise Biography of the Prophet ﷺ and His Special Traits \r\nAuthor: Shaykh Haytham ibn Muhammad Sarhān\r\n\r\n\r\n\r\n"
    private val authorsTxt = "Book Name: مختصر سیرت وشمائل نبی ﷺ\r\nAuthor:  فضيلة الشيخ بیشم بن محمد جمیل سرحان"

    private val raw = "https://raw.githubusercontent.com/dh-app/DH/main"

    private val listing = """[
        {"name":"Short Seerah of Prophet ﷺ.pdf","size":20041792,"type":"file","download_url":"$raw/Short%20Seerah%20of%20Prophet%20%EF%B7%BA.pdf"},
        {"name":"مختصر سیرت وشمائل نبی ﷺ.pdf","size":15873744,"type":"file","download_url":"$raw/urdu.pdf"},
        {"name":"author.txt","size":219,"type":"file","download_url":"$raw/author.txt"},
        {"name":"authors.txt","size":129,"type":"file","download_url":"$raw/authors.txt"},
        {"name":"strings.xml","size":1,"type":"file","download_url":"$raw/strings.xml"},
        {"name":"mobile_app","type":"dir","download_url":null}
    ]"""

    @Test
    fun `reads the uploaded seerah books with their authors`() = runTest {
        val source = GitHubFolderSource("dh-app/DH", "main", "") { url ->
            when {
                url.encodedPath.startsWith("/repos/dh-app/DH/contents") -> {
                    assertEquals("main", url.queryParameter("ref"))
                    listing
                }
                url.toString() == "$raw/author.txt" -> authorTxt
                url.toString() == "$raw/authors.txt" -> authorsTxt
                else -> null
            }
        }

        val books = source.books()

        assertEquals(2, books.size)
        val english = books[0].editions.single()
        assertEquals("Short Seerah of Prophet ﷺ", english.title)
        assertEquals("Shaikh Dr. Abu Omar Parvez Nakwa Madani", english.author)
        assertEquals("en", english.language)
        assertEquals("20.0 MB", english.pdf!!.size)

        val urdu = books[1].editions.single()
        assertEquals("مختصر سیرت وشمائل نبی ﷺ", urdu.title)
        assertEquals("ur", urdu.language)
        assertTrue(urdu.author!!.contains("سرحان"))
    }

    @Test
    fun `a missing folder is an empty shelf`() = runTest {
        assertTrue(GitHubFolderSource("dh-app/DH", "main", "library/none") { null }.books().isEmpty())
    }

    @Test
    fun `script tells urdu from arabic`() {
        assertEquals("ur", LanguageNames.fromScript("مختصر سیرت وشمائل نبی"))
        assertEquals("ar", LanguageNames.fromScript("الرحيق المختوم"))
        assertEquals("kn", LanguageNames.fromScript("ಪ್ರವಾದಿ"))
        assertNull(LanguageNames.fromScript("The Sealed Nectar"))
    }

    @Test
    fun `repo sources can be configured`() {
        val sources = bookSourcesFrom("repo:dh-app/DH@main, repo:dh-app/DH@main/library/seerah, repo:bad", { null })
        assertEquals(2, sources.size)
        assertTrue(sources.all { it is GitHubFolderSource })
    }
}

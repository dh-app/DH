package org.darulhuda.nabiurrahmah.data.library

import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleaseSourceTest {

    @Test
    fun `file names become titles and languages`() {
        assertEquals("The Sealed Nectar" to "ur", GitHubReleaseSource.describe("The Sealed Nectar - Urdu.pdf"))
        assertEquals("Ar-Raheeq Al-Makhtum" to "ur", GitHubReleaseSource.describe("Ar-Raheeq Al-Makhtum (Urdu).pdf"))
        assertEquals("The Sealed Nectar" to "en", GitHubReleaseSource.describe("The_Sealed_Nectar_English.pdf"))
        assertEquals("Seerah Notes" to "en", GitHubReleaseSource.describe("Seerah Notes.pdf"))
        assertEquals("Seerah" to "ml", GitHubReleaseSource.describe("Seerah [Malayalam].pdf"))
    }

    @Test
    fun `release pdfs are grouped into books by title`() = runTest {
        val release = """{"tag_name":"prophetic-biography","assets":[
            {"name":"The Sealed Nectar - English.pdf","size":2600000,"browser_download_url":"https://github.com/dh-app/DH/releases/download/prophetic-biography/en.pdf"},
            {"name":"The Sealed Nectar - Urdu.pdf","size":3100000,"browser_download_url":"https://github.com/dh-app/DH/releases/download/prophetic-biography/ur.pdf"},
            {"name":"Muhammad the Messenger.pdf","size":900000,"browser_download_url":"https://github.com/x/y.pdf"},
            {"name":"cover.jpg","size":1,"browser_download_url":"https://github.com/x/cover.jpg"}
        ]}"""
        var requested: HttpUrl? = null
        val books = GitHubReleaseSource("dh-app/DH", "prophetic-biography") { url -> requested = url; release }.books()

        assertEquals("https://api.github.com/repos/dh-app/DH/releases/tags/prophetic-biography", requested.toString())
        assertEquals(2, books.size)
        val nectar = books.first { it.editions.size == 2 }
        assertEquals(listOf("en", "ur"), nectar.languages)
        assertEquals("2.6 MB", nectar.editions.first().pdf!!.size)
        assertEquals("900 KB", books.first { it.editions.size == 1 }.editions.single().pdf!!.size)
    }

    @Test
    fun `no release yet is an empty shelf, not an error`() = runTest {
        assertTrue(GitHubReleaseSource("dh-app/DH", "missing") { null }.books().isEmpty())
    }

    @Test
    fun `github sources can be configured`() {
        val sources = bookSourcesFrom("github:dh-app/DH@prophetic-biography, github:bad", { null })
        assertEquals(1, sources.size)
        assertTrue(sources.single() is GitHubReleaseSource)
    }
}

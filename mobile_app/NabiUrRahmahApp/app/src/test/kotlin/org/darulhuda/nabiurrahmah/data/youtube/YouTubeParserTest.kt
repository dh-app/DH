package org.darulhuda.nabiurrahmah.data.youtube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YouTubeParserTest {

    private fun renderer(id: String, title: String, seconds: String? = "754", playable: Boolean = true) = """
        {"playlistVideoRenderer":{"videoId":"$id","title":{"runs":[{"text":"$title"}]},
          ${seconds?.let { "\"lengthSeconds\":\"$it\"," } ?: ""}"isPlayable":$playable}}
    """

    private fun playlistPage(vararg renderers: String) = """
        <html><head><meta property="og:title" content="Seerah &amp; Mercy"></head><body>
        <script>var ytInitialData = {"contents":{"items":[${renderers.joinToString(",")}]},
          "metadata":{"playlistMetadataRenderer":{"title":"Nabi ur Rahmah Series"}},
          "tricky":"a string with { braces } and \"quotes\""};</script>
        <script>var other = {"x": 1};</script></body></html>
    """

    @Test
    fun `reads videos, durations and title from the playlist page`() {
        val playlist = YouTubeParser.parsePlaylistPage(
            playlistPage(
                renderer("2Xd9-hfUxXw", "Episode 1"),
                renderer("SneY6d5-mD0", "Episode 2", seconds = null),
                renderer("aaaaaaaaaaa", "[Private video]"),
                renderer("bbbbbbbbbbb", "Blocked", playable = false),
                renderer("2Xd9-hfUxXw", "Episode 1 again"),
            ),
            "PLtest",
        )!!

        assertEquals("Nabi ur Rahmah Series", playlist.title)
        assertEquals(listOf("2Xd9-hfUxXw", "SneY6d5-mD0"), playlist.videos.map { it.id })
        assertEquals(754, playlist.videos[0].durationSeconds)
        assertNull(playlist.videos[1].durationSeconds)
        assertEquals("https://i.ytimg.com/vi/2Xd9-hfUxXw/mqdefault.jpg", playlist.videos[0].thumbnailUrl)
    }

    private fun shortItem(id: String, title: String, withOverlay: Boolean) = """
        {"richItemRenderer":{"content":{"shortsLockupViewModel":{"entityId":"E$id",
          "accessibilityText":"$title, 410 views - play Short",
          ${if (withOverlay) "\"overlayMetadata\":{\"primaryText\":{\"content\":\"$title\"}}," else ""}
          "onTap":{"innertubeCommand":{"commandMetadata":{"webCommandMetadata":{"url":"/shorts/$id"}},
            "reelWatchEndpoint":{"videoId":"$id","playlistId":"PLx",
              "thumbnail":{"thumbnails":[{"url":"https://i.ytimg.com/vi/$id/frame0.jpg","width":720,"height":1280}]}}}}}}}}
    """

    @Test
    fun `reads playlists of shorts`() {
        val html = """<script>var ytInitialData = {"contents":[${shortItem("2Xd9-hfUxXw", "Muslims are safe - Hadith Series", true)},
            ${shortItem("oCGaAsmyf7o", "When Allah intends - Hadith Series", false)}],
            "metadata":{"playlistMetadataRenderer":{"title":"Nabi-ur-Rahmah Series - Darul Huda Udupi"}}};</script>"""

        val playlist = YouTubeParser.parsePlaylistPage(html, "PLx")!!

        assertEquals("Nabi-ur-Rahmah Series - Darul Huda Udupi", playlist.title)
        assertEquals(listOf("Muslims are safe - Hadith Series", "When Allah intends - Hadith Series"), playlist.videos.map { it.title })
        assertEquals(true, playlist.videos.all { it.isShort })
        assertEquals("https://www.youtube.com/shorts/2Xd9-hfUxXw", playlist.videos[0].watchUrl("PLx"))
    }

    @Test
    fun `reads the escaped data pages served to phones`() {
        val json = """{"contents":[${shortItem("abc12345678", "Make things easy", true)}]}"""
        val escaped = json.map { c -> if (c == '{' || c == '}' || c == '"' || c == '[' || c == ']') "\\x%02x".format(c.code) else c.toString() }.joinToString("")
        val html = "<script>var ytInitialData = '$escaped';</script>"

        val playlist = YouTubeParser.parsePlaylistPage(html, "PLx")!!

        assertEquals("Make things easy", playlist.videos.single().title)
    }

    @Test
    fun `a page without playlist data is not a playlist`() {
        assertNull(YouTubeParser.parsePlaylistPage("<html>Before you continue to YouTube</html>", "PLtest"))
    }

    @Test
    fun `reads the rss feed`() {
        val playlist = YouTubeParser.parseFeed(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns:yt="http://www.youtube.com/xml/schemas/2015" xmlns="http://www.w3.org/2005/Atom">
              <title>Nabi ur Rahmah</title>
              <entry><yt:videoId>v1</yt:videoId><title>First &amp; best</title><link rel="alternate" href="https://www.youtube.com/shorts/v1"/></entry>
              <entry><yt:videoId>v2</yt:videoId><title>Second</title></entry>
            </feed>
            """.trimIndent(),
            "PLtest",
        )!!

        assertEquals("Nabi ur Rahmah", playlist.title)
        assertEquals(listOf("First & best", "Second"), playlist.videos.map { it.title })
        assertEquals(listOf(true, false), playlist.videos.map { it.isShort })
    }

    @Test
    fun `finds playlist ids in links and embeds`() {
        assertEquals(
            "PLcF_nL7kXdt2VWfAg3XkhXaOEjH5KYviq",
            YouTubeParser.playlistIdFrom("https://www.youtube.com/watch?v=2Xd9-hfUxXw&list=PLcF_nL7kXdt2VWfAg3XkhXaOEjH5KYviq"),
        )
        assertEquals(
            "PLcF_nL7kXdt0udlvCNfprkl5r0upR9iir",
            YouTubeParser.playlistIdFrom("//www.youtube.com/embed/videoseries?list=PLcF_nL7kXdt0udlvCNfprkl5r0upR9iir"),
        )
        assertNull("personal mixes are not playlists", YouTubeParser.playlistIdFrom("https://youtube.com/watch?v=x&list=RDx123456789"))
        assertNull(YouTubeParser.playlistIdFrom("https://example.com/?list=PLabcdefghijkl"))
    }
}

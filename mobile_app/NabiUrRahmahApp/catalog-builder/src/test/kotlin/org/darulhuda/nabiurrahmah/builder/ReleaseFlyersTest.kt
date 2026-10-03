package org.darulhuda.nabiurrahmah.builder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ReleaseFlyersTest {

    @Test
    fun `reads the attached files of the flyers release`() {
        val assets = ReleaseFlyers.parse(
            """{"tag_name":"flyers","assets":[
                {"id":22,"name":"Nabi.ur.Rahmah.Urdu.pdf","updated_at":"2026-10-03T10:00:00Z",
                 "browser_download_url":"https://github.com/dh-app/DH/releases/download/flyers/Nabi.ur.Rahmah.Urdu.pdf"},
                {"id":11,"name":"Nabi.ur.Rahmah.Assamese.pdf","updated_at":"2026-10-03T09:00:00Z",
                 "browser_download_url":"https://github.com/dh-app/DH/releases/download/flyers/Nabi.ur.Rahmah.Assamese.pdf"},
                {"name":"broken"}
            ]}""",
        )

        assertEquals(listOf("Nabi.ur.Rahmah.Assamese.pdf", "Nabi.ur.Rahmah.Urdu.pdf"), assets.map { it.name })
        assertEquals(11L, assets[0].id)
    }

    @Test
    fun `a replaced file gets a new name, so its pages are rendered again`() {
        val first = ReleaseFlyers.Asset(5, "Nabi ur Rahmah Urdu.pdf", "u", "2026-10-03T10:00:00Z")
        val replaced = first.copy(id = 6, updatedAt = "2026-11-01T10:00:00Z")

        assertEquals("nabi-ur-rahmah-urdu-a5-1003100000", ReleaseFlyers.prefixFor(first))
        assertNotEquals(ReleaseFlyers.prefixFor(first), ReleaseFlyers.prefixFor(replaced))
    }
}

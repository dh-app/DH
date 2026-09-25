package org.darulhuda.nabiurrahmah.data.site

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KnownLanguagesTest {

    @Test
    fun `recognises labels in English and native scripts`() {
        assertEquals("en", KnownLanguages.match("English")?.code)
        assertEquals("ur", KnownLanguages.match("  URDU ")?.code)
        assertEquals("ur", KnownLanguages.match("اردو")?.code)
        assertEquals("kn", KnownLanguages.match("ಕನ್ನಡ")?.code)
        assertEquals("hi", KnownLanguages.match("हिंदी")?.code)
        assertEquals("bn", KnownLanguages.match("Bangla")?.code)
        assertEquals("or", KnownLanguages.match("Oriya")?.code)
        assertEquals("fr", KnownLanguages.match("Français")?.code)
        assertEquals("id", KnownLanguages.match("Bahasa Indonesia")?.code)
        assertEquals("ms", KnownLanguages.match("Bahasa Melayu")?.code)
    }

    @Test
    fun `ignores filler words around the language`() {
        assertEquals("ta", KnownLanguages.match("Nabi ur Rahmah Flyers in Tamil")?.code)
        assertEquals("ml", KnownLanguages.match("Download Malayalam PDF")?.code)
        assertEquals("te", KnownLanguages.match("Telugu Version")?.code)
    }

    @Test
    fun `does not treat sentences or other words as languages`() {
        assertNull(KnownLanguages.match("Arabic College admissions are now open for all students"))
        assertNull(KnownLanguages.match("Contact us"))
        assertNull(KnownLanguages.match(""))
        assertNull(KnownLanguages.match("Malayalam and Tamil and Telugu and Kannada"))
    }

    @Test
    fun `reads language slugs from urls`() {
        assertEquals("ur", KnownLanguages.matchSlug("/nabi-ur-rahmah-urdu/")?.code)
        assertEquals("kn", KnownLanguages.matchSlug("/flyers/kannada")?.code)
        assertNull(KnownLanguages.matchSlug("/about-us/"))
    }
}

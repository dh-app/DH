package org.darulhuda.nabiurrahmah.platform

import org.darulhuda.nabiurrahmah.platform.ReviewPrompt.Companion.isDue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPromptTest {

    private val day = 24L * 60 * 60 * 1000
    private val now = 1_000 * day

    @Test
    fun `asks a regular reader once, then waits months`() {
        assertTrue(isDue(opens = 4, moments = 5, firstOpen = now - 3 * day, lastAsked = 0, now = now))
        assertFalse(isDue(opens = 9, moments = 20, firstOpen = now - 30 * day, lastAsked = now - 30 * day, now = now))
        assertTrue(isDue(opens = 9, moments = 20, firstOpen = now - 300 * day, lastAsked = now - 121 * day, now = now))
    }

    @Test
    fun `never asks someone who has barely used the app`() {
        assertFalse(isDue(opens = 1, moments = 50, firstOpen = now - 30 * day, lastAsked = 0, now = now))
        assertFalse(isDue(opens = 10, moments = 2, firstOpen = now - 30 * day, lastAsked = 0, now = now))
        assertFalse(isDue(opens = 10, moments = 10, firstOpen = now - day / 2, lastAsked = 0, now = now))
    }
}

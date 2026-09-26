package org.darulhuda.nabiurrahmah.builder

import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import org.darulhuda.nabiurrahmah.data.model.Flyer
import org.darulhuda.nabiurrahmah.data.model.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.junit.Test

class BuilderTest {

    private val root = Files.createTempDirectory("library").toFile()
    private val layout = LibraryLayout(root, "https://raw.githubusercontent.com/dh-app/DH/main/library/")

    private fun image(file: File, width: Int, height: Int) {
        file.parentFile.mkdirs()
        ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "jpg", file)
    }

    @Test
    fun `large images get a display copy and every image a preview`() {
        val big = File(root, "big.jpg").also { image(it, 3600, 5100) }
        val small = File(root, "small.jpg").also { image(it, 1200, 1700) }

        val bigOut = Images.process(big, File(root, "out"), "big")
        val smallOut = Images.process(small, File(root, "out"), "small")

        val display = ImageIO.read(bigOut.display)
        assertEquals(Images.MAX_SIDE, maxOf(display.width, display.height))
        assertEquals(3600 to 5100, bigOut.width to bigOut.height)
        assertEquals(small, smallOut.display)
        assertEquals(Images.THUMB_WIDTH, ImageIO.read(smallOut.thumbnail).width)
    }

    @Test
    fun `uploaded folders become languages, named in any script`() {
        image(File(root, "flyers/Hindi/Seerah 1.jpg"), 800, 1131)
        image(File(root, "flyers/Hindi/IMG_2031.jpg"), 800, 1131)
        image(File(root, "flyers/اردو/Mercy.jpg"), 800, 1131)
        File(root, "flyers/Urdu Books").mkdirs()
        File(root, "flyers/_generated").mkdirs()

        val languages = readUploadedFlyers(layout)

        assertEquals(listOf("hi", "ur"), languages.map { it.code })
        val hindi = languages[0]
        assertEquals(listOf("IMG_2031", "Seerah 1").size, hindi.flyers.size)
        assertEquals(setOf(null, "Seerah 1"), hindi.flyers.map { it.title }.toSet())
        val first = hindi.flyers.first { it.title == "Seerah 1" }
        assertTrue(first.image, first.image.startsWith("https://raw.githubusercontent.com/dh-app/DH/main/library/flyers/Hindi/Seerah%201.jpg"))
        assertNotNull(first.thumbnail)
        assertEquals(800f / 1131f, first.aspectRatio!!, 0.001f)
        assertEquals("اردو", languages[1].nativeName)
    }

    @Test
    fun `uploads join the website's language or add their own`() {
        val website = listOf(Language("en", "English", flyers = listOf(Flyer("w1", "w1.jpg"))), Language("ur", "Urdu"))
        val uploads = listOf(Language("ur", "Urdu", flyers = listOf(Flyer("u1", "u1.jpg"))), Language("ta", "Tamil", flyers = listOf(Flyer("t1", "t1.jpg"))))

        val merged = merge(website, uploads)

        assertEquals(listOf("en", "ur", "ta"), merged.map { it.code })
        assertEquals(listOf("u1"), merged[1].flyers.map { it.id })
    }

    @Test
    fun `an uploaded PDF becomes one sharp flyer per page, rendered once`() {
        val pdf = File(root, "flyers/Kannada/Nabi ur Rahmah Kannada.pdf").apply { parentFile.mkdirs() }
        PDDocument().use { document ->
            repeat(3) {
                val page = PDPage(PDRectangle.A4)
                document.addPage(page)
                PDPageContentStream(document, page).use { content ->
                    content.addRect(50f, 50f, 200f, 300f)
                    content.fill()
                }
            }
            document.save(pdf)
        }

        val kannada = readUploadedFlyers(layout).single()
        val again = readUploadedFlyers(layout).single()

        assertEquals("kn", kannada.code)
        assertEquals(3, kannada.flyers.size)
        assertEquals(listOf("Nabi ur Rahmah · 1", "Nabi ur Rahmah · 2", "Nabi ur Rahmah · 3"), kannada.flyers.map { it.title })
        val first = kannada.flyers.first()
        assertNull(first.pdf)
        assertTrue(first.image, first.image.contains("/flyers/_generated/Kannada/"))
        assertEquals(Images.MAX_SIDE, maxOf(first.width!!, first.height!!))
        assertEquals(PDRectangle.A4.width / PDRectangle.A4.height, first.aspectRatio!!, 0.002f)
        assertEquals(kannada, again)
        assertEquals(kannada.flyers.map { it.id }.toSet().size, 3)
    }

    @Test
    fun `camera file names don't become titles`() {
        assertNull(titleFrom("IMG_2031.jpg"))
        assertNull(titleFrom("WhatsApp Image 2024-01-01.jpeg"))
        assertEquals("Mercy to the Worlds", titleFrom("Mercy to the Worlds - Urdu.jpg"))
    }
}

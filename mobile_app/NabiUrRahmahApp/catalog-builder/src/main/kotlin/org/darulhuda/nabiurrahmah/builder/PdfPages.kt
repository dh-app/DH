package org.darulhuda.nabiurrahmah.builder

import java.io.File
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.min
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer

/**
 * Turns an uploaded PDF into one image per page, so a whole language's flyers
 * can be added as a single file. Pages are rendered sharp enough to read when
 * zoomed ([DPI], at most [Images.MAX_SIDE] pixels), and only once: the file
 * names carry a fingerprint of the PDF, so replacing it renders it again.
 */
object PdfPages {
    const val DPI = 220f

    /** Sharp at 2400 px; a little more compression than photos need keeps 21 languages of pages light. */
    private const val QUALITY = 0.85f

    /** Pages already rendered under [prefix], in page order. */
    fun existing(prefix: String, outputDir: File): List<File> =
        outputDir.listFiles { f -> f.name.startsWith("$prefix-p") && f.name.endsWith(".jpg") && !f.name.endsWith("-thumb.jpg") }
            .orEmpty().sortedBy { it.name }

    fun render(pdf: File, outputDir: File, prefix: String = "${pdf.nameWithoutExtension}-${fingerprint(pdf)}"): List<File> {
        outputDir.mkdirs()
        return Loader.loadPDF(pdf).use { document ->
            val renderer = PDFRenderer(document)
            (0 until document.numberOfPages).map { index ->
                File(outputDir, "$prefix-p%03d.jpg".format(index + 1)).also { page ->
                    if (!page.isFile) {
                        val box = document.getPage(index).cropBox
                        val longestPoints = max(box.width, box.height).coerceAtLeast(1f)
                        val scale = min(DPI / 72f, Images.MAX_SIDE / longestPoints)
                        Images.writeJpeg(renderer.renderImage(index, scale, ImageType.RGB), page, QUALITY)
                    }
                }
            }
        }
    }

    private fun fingerprint(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1 shl 16)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().take(4).joinToString("") { "%02x".format(it) }
    }
}

package org.darulhuda.nabiurrahmah.builder

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.math.max
import kotlin.math.roundToInt

/** A flyer image as published: the size to show, a small copy for grids, and its dimensions. */
data class ProcessedImage(
    val display: File,
    val thumbnail: File?,
    val width: Int?,
    val height: Int?,
)

/**
 * Prepares flyer images for phones: keeps the original when it is already a
 * sensible size, otherwise a high-quality copy at most [MAX_SIDE] pixels, plus a
 * [THUMB_WIDTH]-pixel preview so grids load instantly. Files are only written once.
 */
object Images {
    const val MAX_SIDE = 2400
    const val THUMB_WIDTH = 540
    private const val MAX_BYTES = 1_800_000L

    fun process(original: File, outputDir: File, baseName: String): ProcessedImage {
        val image = try {
            ImageIO.read(original)
        } catch (e: Exception) {
            null
        } ?: return ProcessedImage(original, null, null, null) // e.g. WebP: published as is

        outputDir.mkdirs()
        val longest = max(image.width, image.height)
        val display = if (longest > MAX_SIDE || original.length() > MAX_BYTES) {
            File(outputDir, "$baseName-display.jpg").also { if (!it.isFile) writeJpeg(scale(image, MAX_SIDE.toDouble() / longest), it, 0.9f) }
        } else {
            original
        }
        val thumbnail = File(outputDir, "$baseName-thumb.jpg").also {
            if (!it.isFile) writeJpeg(scale(image, (THUMB_WIDTH.toDouble() / image.width).coerceAtMost(1.0)), it, 0.82f)
        }
        return ProcessedImage(display, thumbnail, image.width, image.height)
    }

    /** Smooth multi-step downscaling: sharp text without jagged edges. */
    private fun scale(source: BufferedImage, factor: Double): BufferedImage {
        if (factor >= 1.0) return opaque(source)
        var current = opaque(source)
        val targetW = (source.width * factor).roundToInt().coerceAtLeast(1)
        val targetH = (source.height * factor).roundToInt().coerceAtLeast(1)
        var w = current.width
        var h = current.height
        do {
            w = max(targetW, w / 2)
            h = max(targetH, h / 2)
            val next = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
            val g = next.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g.drawImage(current, 0, 0, w, h, null)
            g.dispose()
            current = next
        } while (w != targetW || h != targetH)
        return current
    }

    /** JPEG has no transparency; flatten onto white like the printed flyer. */
    private fun opaque(source: BufferedImage): BufferedImage {
        if (source.type == BufferedImage.TYPE_INT_RGB) return source
        val out = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        g.color = java.awt.Color.WHITE
        g.fillRect(0, 0, out.width, out.height)
        g.drawImage(source, 0, 0, null)
        g.dispose()
        return out
    }

    internal fun writeJpeg(image: BufferedImage, file: File, quality: Float) {
        val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
        val params = writer.defaultWriteParam.apply {
            compressionMode = ImageWriteParam.MODE_EXPLICIT
            compressionQuality = quality
        }
        val tmp = File(file.path + ".tmp")
        ImageIO.createImageOutputStream(tmp).use { out ->
            writer.output = out
            writer.write(null, IIOImage(image, null, null), params)
        }
        writer.dispose()
        tmp.renameTo(file)
    }
}

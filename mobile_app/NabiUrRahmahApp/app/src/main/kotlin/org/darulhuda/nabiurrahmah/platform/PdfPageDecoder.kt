package org.darulhuda.nabiurrahmah.platform

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.graphics.drawable.BitmapDrawable
import android.os.ParcelFileDescriptor
import coil.ImageLoader
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.decode.ImageSource
import coil.fetch.SourceResult
import coil.request.Options
import coil.size.pxOrElse
import kotlinx.coroutines.runInterruptible
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Lets Coil show PDF flyers: renders the first page at the size it is displayed,
 * so a PDF uploaded without a preview image still gets a sharp thumbnail.
 */
class PdfPageDecoder(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult = runInterruptible {
        val file = source.file().toFile()
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(descriptor)
        try {
            val page = renderer.openPage(0)
            try {
                val maxWidth = options.size.width.pxOrElse { MAX_SIDE }.coerceAtMost(MAX_SIDE)
                val maxHeight = options.size.height.pxOrElse { MAX_SIDE }.coerceAtMost(MAX_SIDE)
                val scale = min(maxWidth.toFloat() / page.width, maxHeight.toFloat() / page.height)
                val width = (page.width * scale).roundToInt().coerceAtLeast(1)
                val height = (page.height * scale).roundToInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                DecodeResult(BitmapDrawable(options.context.resources, bitmap), isSampled = true)
            } finally {
                page.close()
            }
        } finally {
            renderer.close()
            descriptor.close()
        }
    }

    class Factory : Decoder.Factory {
        override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder? {
            // Coil derives the type from the URL when the server sends a generic one.
            return if (result.mimeType == MIME_PDF) PdfPageDecoder(result.source, options) else null
        }
    }

    private companion object {
        /** Large enough to zoom into a page, small enough to stay well inside memory limits. */
        const val MAX_SIDE = 2400
    }
}

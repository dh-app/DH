package org.darulhuda.nabiurrahmah.platform

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.request.Options
import coil.size.pxOrElse
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Width / height of each page of a PDF. */
typealias PageRatios = List<Float>

/**
 * Keeps a few PDFs open and renders their pages on demand. [PdfRenderer] allows one
 * page open at a time per document, so each document has its own lock.
 */
class PdfDocuments {

    private class Document(val descriptor: ParcelFileDescriptor, val renderer: PdfRenderer) {
        val lock = Mutex()
    }

    private val open = LinkedHashMap<String, Document>(8, 0.75f, true)
    private val registry = Mutex()

    suspend fun pageRatios(file: File): PageRatios = withDocument(file) { renderer ->
        (0 until renderer.pageCount).map { index ->
            val page = renderer.openPage(index)
            try {
                page.width.toFloat() / page.height.coerceAtLeast(1)
            } finally {
                page.close()
            }
        }
    }

    /** Renders page [index] [width] pixels wide, on white, sharp enough for text. */
    suspend fun render(file: File, index: Int, width: Int): Bitmap = withDocument(file) { renderer ->
        val page = renderer.openPage(index.coerceIn(0, renderer.pageCount - 1))
        try {
            val scale = width.toFloat() / page.width.coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, (page.height * scale).roundToInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } finally {
            page.close()
        }
    }

    private suspend fun <T> withDocument(file: File, block: (PdfRenderer) -> T): T {
        val document = registry.withLock {
            open[file.path] ?: openDocument(file).also { opened ->
                open[file.path] = opened
                evictIdle()
            }
        }
        return document.lock.withLock { withContext(Dispatchers.IO) { block(document.renderer) } }
    }

    private fun openDocument(file: File): Document {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return try {
            Document(descriptor, PdfRenderer(descriptor))
        } catch (e: Exception) {
            descriptor.close()
            throw e
        }
    }

    /** Closes the least recently used documents that aren't rendering right now. */
    private fun evictIdle() {
        val iterator = open.entries.iterator()
        while (open.size > MAX_OPEN && iterator.hasNext()) {
            val document = iterator.next().value
            if (document.lock.tryLock()) {
                try {
                    document.renderer.close()
                    document.descriptor.close()
                } finally {
                    document.lock.unlock()
                }
                iterator.remove()
            }
        }
    }

    private companion object {
        const val MAX_OPEN = 3
    }
}

/** A page of a local PDF, loadable with Coil like any image. */
data class PdfPage(val path: String, val index: Int)

class PdfPageFetcher(
    private val page: PdfPage,
    private val options: Options,
    private val documents: PdfDocuments,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val width = options.size.width.pxOrElse { DEFAULT_WIDTH }.coerceIn(MIN_WIDTH, MAX_WIDTH)
        val bitmap = documents.render(File(page.path), page.index, width)
        return DrawableResult(BitmapDrawable(options.context.resources, bitmap), isSampled = true, dataSource = DataSource.DISK)
    }

    class Factory(private val documents: PdfDocuments) : Fetcher.Factory<PdfPage> {
        override fun create(data: PdfPage, options: Options, imageLoader: ImageLoader): Fetcher =
            PdfPageFetcher(data, options, documents)
    }

    /** Includes the size, so a zoomed-in page isn't served from a small cached copy. */
    class PageKeyer : Keyer<PdfPage> {
        override fun key(data: PdfPage, options: Options): String =
            "pdf:${data.path}#${data.index}@${options.size.width.pxOrElse { DEFAULT_WIDTH }}"
    }

    private companion object {
        const val DEFAULT_WIDTH = 2000
        const val MIN_WIDTH = 200
        const val MAX_WIDTH = 3000
    }
}

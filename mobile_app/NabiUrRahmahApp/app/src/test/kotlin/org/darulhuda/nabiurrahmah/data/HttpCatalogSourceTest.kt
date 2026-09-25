package org.darulhuda.nabiurrahmah.data

import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.darulhuda.nabiurrahmah.data.source.HttpCatalogSource
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HttpCatalogSourceTest {

    private val server = MockWebServer()

    @Before fun setUp() = server.start()

    @After fun tearDown() = server.shutdown()

    @Test
    fun `returns the body on success`() = runTest {
        server.enqueue(MockResponse().setBody("""{"languages":[]}"""))
        val source = HttpCatalogSource(OkHttpClient(), server.url("/catalog.json"))

        assertEquals("""{"languages":[]}""", source.fetch())
        assertEquals("/catalog.json", server.takeRequest().path)
    }

    @Test(expected = IOException::class)
    fun `throws on http errors`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        HttpCatalogSource(OkHttpClient(), server.url("/catalog.json")).fetch()
    }
}

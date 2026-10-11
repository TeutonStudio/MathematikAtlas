package de.TeutonStudio.MathematikAtlas.speicher

import java.net.InetAddress
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlin.test.*

class SichererKartenAbrufGrenzenTest {
    private val öffentlich = object : Dns { override fun lookup(hostname: String) = listOf(InetAddress.getByName("8.8.8.8")) }
    private fun client(antwort: (okhttp3.Interceptor.Chain) -> Response) =
        OkHttpClient.Builder().addInterceptor { antwort(it) }.build()
    private fun antwort(chain: okhttp3.Interceptor.Chain, code: Int = 200, body: ByteArray = byteArrayOf(), location: String? = null): Response =
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("Test")
            .body(body.toResponseBody()).apply { if (location != null) header("Location", location) }.build()

    @Test fun `vier Weiterleitungen sind erlaubt die fuenfte wird abgewiesen`() {
        for (anzahl in listOf(4, 5)) {
            var aufrufe = 0
            val transport = client { chain ->
                assertTrue(chain.call().timeout().timeoutNanos() in 1..60_000_000_000L)
                val n = aufrufe++
                if (n < anzahl) antwort(chain, 302, location = "/schritt${n + 1}") else antwort(chain, body = "karte".toByteArray())
            }
            if (anzahl == 4) assertEquals("karte", SichererKartenAbruf.lade("https://example.org/start", transport, öffentlich).decodeToString())
            else assertFailsWith<IllegalStateException> { SichererKartenAbruf.lade("https://example.org/start", transport, öffentlich) }
            assertEquals(5, aufrufe)
        }
    }

    @Test fun `Redirects zu HTTP und privaten Zielen werden vor dem Abruf abgewiesen`() {
        for (ziel in listOf("http://example.org/karte", "https://127.0.0.1/karte", "https://private.example/karte")) {
            var aufrufe = 0
            val dns = SichererKartenAbruf.öffentlichesDns { host ->
                listOf(InetAddress.getByName(if (host == "example.org") "8.8.8.8" else "127.0.0.1"))
            }
            val transport = client { chain -> aufrufe++; antwort(chain, 302, location = ziel) }
            assertFailsWith<IllegalArgumentException> { SichererKartenAbruf.lade("https://example.org/start", transport, dns) }
            assertEquals(1, aufrufe)
        }
    }

    @Test fun `16 MiB sind erlaubt ein weiteres Byte ist auch bei unbekannter Laenge unzulaessig`() {
        val grenze = SichererKartenAbruf.MAXIMALE_GRÖSSE
        for (groesse in listOf(grenze, grenze + 1)) for (bekannteLaenge in listOf(true, false)) {
            val transport = client { chain ->
                val daten = ByteArray(groesse) { 42 }
                val body = if (bekannteLaenge) daten.toResponseBody() else object : okhttp3.ResponseBody() {
                    override fun contentType(): okhttp3.MediaType? = null
                    override fun contentLength(): Long = -1
                    override fun source(): okio.BufferedSource = okio.Buffer().write(daten)
                }
                antwort(chain).newBuilder().body(body).build()
            }
            if (groesse == grenze) assertEquals(grenze, SichererKartenAbruf.lade("https://example.org/karte", transport, öffentlich).size)
            else assertFailsWith<IllegalArgumentException> { SichererKartenAbruf.lade("https://example.org/karte", transport, öffentlich) }
        }
    }
}

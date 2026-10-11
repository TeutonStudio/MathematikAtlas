package de.TeutonStudio.MathematikAtlas.speicher

import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.Proxy
import java.net.URI
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request

/** HTTPS-Transport mit kontrolliertem DNS; TLS und Hostheader verbleiben beim Originalhost. */
internal object SichererKartenAbruf {
    const val MAXIMALE_GRÖSSE = 16 * 1024 * 1024
    private const val MAXIMALE_WEITERLEITUNGEN = 4
    private const val ZEITLIMIT_MILLIS = 10_000L

    internal fun öffentlichesDns(auflösen: (String) -> List<InetAddress> = { Dns.SYSTEM.lookup(it) }): Dns =
        object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
            val adressen = auflösen(hostname)
            require(adressen.isNotEmpty() && adressen.all(::istÖffentlich)) {
                "Lokale und private Netzwerkziele sind beim Kartenimport nicht erlaubt."
            }
            return adressen
            }
        }

    private val dns = öffentlichesDns()
    private val client = OkHttpClient.Builder()
        .dns(dns)
        .proxy(Proxy.NO_PROXY)
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(ZEITLIMIT_MILLIS, TimeUnit.MILLISECONDS)
        .readTimeout(ZEITLIMIT_MILLIS, TimeUnit.MILLISECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    fun lade(adresse: String, transport: Call.Factory = client, dnsPrüfung: Dns = dns): ByteArray {
        var uri = prüfeHttpsAdresse(adresse)
        val deadline = System.nanoTime() + 60_000_000_000L
        repeat(MAXIMALE_WEITERLEITUNGEN + 1) { weiterleitungen ->
            check(System.nanoTime() < deadline) { "Zeitlimit beim Kartenimport überschritten." }
            // Auch numerische Hosts prüfen: OkHttp benötigt für IP-Literale keine DNS-Auflösung.
            dnsPrüfung.lookup(uri.host.trim('[', ']'))
            val request = Request.Builder().url(uri.toASCIIString())
                .header("Accept", "application/json, application/zip, application/octet-stream").build()
            val call = transport.newCall(request)
            // Das Gesamtbudget gilt auch für einen langsamen letzten Redirect/Abruf.
            val verbleibend = deadline - System.nanoTime()
            check(verbleibend > 0) { "Zeitlimit beim Kartenimport überschritten." }
            call.timeout().timeout(verbleibend, TimeUnit.NANOSECONDS)
            call.execute().use { response ->
                if (response.code in setOf(301, 302, 303, 307, 308)) {
                    check(weiterleitungen < MAXIMALE_WEITERLEITUNGEN) { "Zu viele Weiterleitungen beim Kartenimport." }
                    val ziel = response.header("Location") ?: error("Die Weiterleitung enthält kein Ziel.")
                    uri = prüfeHttpsAdresse(uri.resolve(ziel).toString())
                    return@repeat
                }
                check(response.code == 200) { "Der Kartenabruf ist mit HTTP-Status ${response.code} fehlgeschlagen." }
                val body = response.body ?: error("Der Kartenabruf enthält keine Daten.")
                require(body.contentLength() <= MAXIMALE_GRÖSSE) { "Die Importdatei ist größer als 16 MiB." }
                return body.byteStream().use { eingabe ->
                    val ausgabe = ByteArrayOutputStream()
                    val puffer = ByteArray(8192)
                    while (true) {
                        check(System.nanoTime() < deadline) { "Zeitlimit beim Kartenimport überschritten." }
                        val n = eingabe.read(puffer)
                        if (n < 0) break
                        require(ausgabe.size().toLong() + n <= MAXIMALE_GRÖSSE) { "Die Importdatei ist größer als 16 MiB." }
                        ausgabe.write(puffer, 0, n)
                    }
                    ausgabe.toByteArray()
                }
            }
        }
        error("Der Kartenabruf konnte nicht abgeschlossen werden.")
    }

    internal fun istÖffentlich(adresse: InetAddress): Boolean {
        val bytes = adresse.address
        if (adresse.isAnyLocalAddress || adresse.isLoopbackAddress || adresse.isLinkLocalAddress ||
            adresse.isSiteLocalAddress || adresse.isMulticastAddress) return false
        if (bytes.size == 16 && (bytes[0].toInt() and 0xfe) == 0xfc) return false
        if (bytes.size == 4) {
            val a = bytes[0].toInt() and 255; val b = bytes[1].toInt() and 255
            if (a == 0 || a >= 224 || (a == 100 && b in 64..127) || (a == 198 && b in 18..19)) return false
        }
        return true
    }

    internal fun prüfeHttpsAdresse(adresse: String): URI {
        val uri = URI(adresse.trim())
        require(uri.scheme.equals("https", ignoreCase = true)) { "Nur HTTPS-Adressen können importiert werden." }
        require(!uri.host.isNullOrBlank()) { "Die HTTPS-Adresse enthält keinen gültigen Host." }
        require(uri.host.trim('[', ']').lowercase() !in setOf("localhost", "localhost.localdomain") &&
            !uri.host.lowercase().endsWith(".localhost") && !uri.host.lowercase().endsWith(".local")) {
            "Lokale Adressen können nicht importiert werden."
        }
        require(uri.userInfo == null) { "Anmeldedaten in Importadressen sind nicht erlaubt." }
        return uri
    }
}

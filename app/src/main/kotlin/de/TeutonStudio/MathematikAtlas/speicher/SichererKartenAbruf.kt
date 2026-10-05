package de.TeutonStudio.MathematikAtlas.speicher

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import javax.net.ssl.HttpsURLConnection

internal object SichererKartenAbruf {
    const val MAXIMALE_GRÖSSE = 16 * 1024 * 1024
    private const val MAXIMALE_WEITERLEITUNGEN = 4
    private const val ZEITLIMIT_MILLIS = 10_000

    fun lade(adresse: String): ByteArray {
        var uri = prüfeHttpsAdresse(adresse)
        repeat(MAXIMALE_WEITERLEITUNGEN + 1) { weiterleitungen ->
            val verbindung = uri.toURL().openConnection() as? HttpsURLConnection
                ?: error("Die Adresse verwendet keine sichere HTTPS-Verbindung.")
            verbindung.instanceFollowRedirects = false
            verbindung.connectTimeout = ZEITLIMIT_MILLIS
            verbindung.readTimeout = ZEITLIMIT_MILLIS
            verbindung.setRequestProperty("Accept", "application/json, application/zip, application/octet-stream")
            try {
                val status = verbindung.responseCode
                if (status in 300..399) {
                    check(weiterleitungen < MAXIMALE_WEITERLEITUNGEN) {
                        "Zu viele Weiterleitungen beim Kartenimport."
                    }
                    val ziel = verbindung.getHeaderField("Location")
                        ?: error("Die Weiterleitung enthält kein Ziel.")
                    uri = prüfeHttpsAdresse(uri.resolve(ziel).toString())
                    return@repeat
                }
                check(status == HttpURLConnection.HTTP_OK) {
                    "Der Kartenabruf ist mit HTTP-Status $status fehlgeschlagen."
                }
                val angekündigt = verbindung.contentLengthLong
                require(angekündigt < 0 || angekündigt <= MAXIMALE_GRÖSSE) {
                    "Die Importdatei ist größer als 16 MiB."
                }
                return verbindung.inputStream.use { eingabe ->
                    val ausgabe = ByteArrayOutputStream()
                    val puffer = ByteArray(8192)
                    var gesamt = 0
                    while (true) {
                        val gelesen = eingabe.read(puffer)
                        if (gelesen < 0) break
                        gesamt += gelesen
                        require(gesamt <= MAXIMALE_GRÖSSE) { "Die Importdatei ist größer als 16 MiB." }
                        ausgabe.write(puffer, 0, gelesen)
                    }
                    ausgabe.toByteArray()
                }
            } finally {
                verbindung.disconnect()
            }
        }
        error("Der Kartenabruf konnte nicht abgeschlossen werden.")
    }

    internal fun prüfeHttpsAdresse(adresse: String): URI {
        val uri = URI(adresse.trim())
        require(uri.scheme.equals("https", ignoreCase = true)) { "Nur HTTPS-Adressen können importiert werden." }
        require(!uri.host.isNullOrBlank()) { "Die HTTPS-Adresse enthält keinen gültigen Host." }
        require(uri.userInfo == null) { "Anmeldedaten in Importadressen sind nicht erlaubt." }
        return uri
    }
}

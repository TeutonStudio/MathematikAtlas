package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

const val METHODEN_QUELLE_ART = "mathematik.methode"
const val METHODEN_QUELLE_KNOTEN = "methodenQuelle.knoten"
const val METHODEN_KOMPONENTEN_ID = "methodenKomponenteId"

/** Eine Methodenquelle benötigt eine feste Signatur auch für einen Mehrfacheingang. */
fun methodenQuellArgumente(quelle: KnotenDaten): KnotenDaten {
    if (quelle.anschlüsse.none { it.richtung == AnschlussRichtung.Eingang && it.belegung == AnschlussBelegung.Mehrfach }) return quelle
    val anzahl = quelle.parameter["festeEingänge"]?.let {
        it.toIntOrNull()?.takeIf { n -> n in 2..128 } ?: error("Die Anzahl muss zwischen 2 und 128 liegen.")
    } ?: 2
    return quelle.copy(anschlüsse = quelle.anschlüsse.flatMap { port ->
        if (port.richtung != AnschlussRichtung.Eingang || port.belegung != AnschlussBelegung.Mehrfach) listOf(port)
        else (0 until anzahl).map { index ->
            port.copy(
                id = if (index == 0) port.id else AnschlussId("${port.id.wert}:methodenArgument:$index"),
                name = if (index == 0) port.name else "${port.name}.argument${index + 1}",
                semantischeId = if (index == 0) port.semantischeId else "${port.semantischeId ?: port.id.wert}:argument:$index",
                reihenfolge = port.reihenfolge + index, belegung = AnschlussBelegung.Einzeln,
                kannSichErweitern = true, dynamischErzeugt = false, nächsteBeitragsReihenfolge = 0,
            )
        }
    })
}

/** Builds the ordinary public card boundary around one configured operation. */
fun methodenQuellKarte(quelle: KnotenDaten, besitzer: KnotenId): KartenDaten =
    baueMethodenQuellKarte(methodenQuellArgumente(quelle), besitzer)

private fun baueMethodenQuellKarte(quelle: KnotenDaten, besitzer: KnotenId): KartenDaten {
    require(quelle.art != METHODEN_QUELLE_ART && quelle.kartenVerweis == null) { "Diese Quelle wird über den Kartenmodus bereitgestellt." }
    val basis = "${besitzer.wert}:methodenQuelle"
    val knoten = quelle.copy(id = KnotenId("$basis:operation"))
    val grenzen = quelle.anschlüsse.filter { it.richtung != AnschlussRichtung.Neutral }.sortedWith(compareBy({ it.richtung }, { it.reihenfolge }))
    val rand = grenzen.mapIndexed { index, port ->
        val eingang = port.richtung == AnschlussRichtung.Eingang
        val symbolischeArt = if (eingang && port.art.wert in setOf("atlas.wert", "mathematik.atlasWert", "mathematik.objekt")) {
            listOf("mathematik.zahl", "mathematik.aussage", "mathematik.menge", "mathematik.tupel")
                .map(::AnschlussArtId).firstOrNull { it in port.zulässigeArten } ?: port.art
        } else port.art
        KnotenDaten(id = KnotenId("$basis:${port.richtung}:${port.id.wert}"),
            art = if (eingang) "mathematik.kartenEingang" else "mathematik.kartenAusgang",
            name = port.name,
            parameter = mapOf("name" to port.name, KARTEN_SCHNITTSTELLEN_POSITION to index.toString(), METHODEN_KOMPONENTEN_ID to (port.semantischeId ?: port.id.wert)),
            anschlüsse = listOf(port.copy(id = AnschlussId("$basis:rand:${port.id.wert}"), name = "wert", art = symbolischeArt, richtung = if (eingang) AnschlussRichtung.Ausgang else AnschlussRichtung.Eingang,
                belegung = AnschlussBelegung.Einzeln, nächsteBeitragsReihenfolge = 0, kannSichErweitern = false, dynamischErzeugt = false)))
    }
    val verbindungen = grenzen.zip(rand).map { (port, grenze) ->
        val innen = AnschlussVerweis(knoten.id, port.id)
        val außen = AnschlussVerweis(grenze.id, grenze.anschlüsse.single().id)
        if (port.richtung == AnschlussRichtung.Eingang) VerbindungDaten(von = außen, zu = innen) else VerbindungDaten(von = innen, zu = außen)
    }
    require(rand.any { it.art == "mathematik.kartenAusgang" }) { "Die Quelle besitzt keinen öffentlichen Ausgang." }
    return KartenDaten(id = KartenId("$basis:karte"), name = quelle.name, knoten = listOf(knoten) + rand, verbindungen = verbindungen)
}

const val METHODEN_QUELLE_ID = "methodenQuelle.id"

/** Gespeicherte Quellen werden vor jedem Dispatch validiert; unbekannte Auswahl ist keine Identität. */
fun validiereMethodenQuellenKonfiguration(knoten: KnotenDaten) {
    if (knoten.art != METHODEN_QUELLE_ART) return
    val rohId = knoten.eigenschaften[METHODEN_QUELLE_ID]
    require(rohId == null || rohId is KnotenEigenschaft.Text) { "Ungültige Methodenquellen-ID." }
    val id = (rohId as? KnotenEigenschaft.Text)?.wert
    val quelle = knoten.eigenschaften[METHODEN_QUELLE_KNOTEN]
    when {
        quelle != null -> {
            require(knoten.kartenVerweis == null) { "Methodenquelle besitzt widersprüchliche Knoten- und Kartenquellen." }
            require(quelle is KnotenEigenschaft.Text) { "Ungültige gespeicherte Knotenquelle." }
            val karte = KartenDatenJson.lese(quelle.wert)
            require(karte.knoten.size == 1 && karte.verbindungen.isEmpty()) { "Knotenquelle benötigt genau einen konfigurierten Knoten." }
            val source = karte.knoten.single()
            require(source.art != METHODEN_QUELLE_ART && source.kartenVerweis == null) { "Ungültige rekursive Knotenquelle." }
            require(id != null && (id.startsWith("knoten:${source.art}:") || id.startsWith("operator:"))) { "Unbekannte oder widersprüchliche Knotenquellen-ID." }
        }
        knoten.kartenVerweis != null -> require(id == "karte:${knoten.kartenVerweis}") { "Unbekannte oder widersprüchliche Kartenquellen-ID." }
        else -> require(id == null || id == "identitaet") { "Unbekannte Methodenquelle '$id'." }
    }
    val eingänge = knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }
    require(if (quelle == null && knoten.kartenVerweis == null) eingänge.size == 1 && eingänge.single().name == "menge" && eingänge.single().art.wert == "mathematik.menge" else eingänge.isEmpty()) { "Die Anschlüsse passen nicht zur Methodenquelle." }
    require(knoten.anschlüsse.count { it.richtung == AnschlussRichtung.Ausgang && it.name == "methode" && it.art.wert == "mathematik.methode" } == 1) { "Der Methodenausgang fehlt." }
}

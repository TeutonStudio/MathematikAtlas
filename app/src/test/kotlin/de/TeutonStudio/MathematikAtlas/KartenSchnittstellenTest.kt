package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KartenSchnittstellenTest {
    @Test
    fun `öffentliche Anschlüsse sind pro Richtung anhand ihres Namens eindeutig`() {
        val objekt = AnschlussArtId("objekt")
        val zahl = AnschlussArtId("zahl")
        val karte = KartenDaten(
            name = "Test",
            knoten = listOf(
                schnittstelle("mathematik.kartenEingang", "Eingang A", "wert", objekt, AnschlussRichtung.Ausgang),
                schnittstelle("mathematik.kartenEingang", "Eingang A doppelt", "wert", zahl, AnschlussRichtung.Ausgang),
                schnittstelle("mathematik.kartenEingang", "Eingang B", "index", zahl, AnschlussRichtung.Ausgang),
                schnittstelle("mathematik.kartenAusgang", "Ausgang A", "wert", zahl, AnschlussRichtung.Eingang),
                schnittstelle("mathematik.kartenAusgang", "Ausgang A doppelt", "wert", objekt, AnschlussRichtung.Eingang),
            ),
        )

        val eingänge = öffentlicheKartenAnschlüsse(karte, "mathematik.kartenEingang", AnschlussRichtung.Eingang, AnschlussKante.Links)
        val ausgänge = öffentlicheKartenAnschlüsse(karte, "mathematik.kartenAusgang", AnschlussRichtung.Ausgang, AnschlussKante.Rechts)

        assertEquals(listOf("wert", "index"), eingänge.map { it.name })
        assertEquals(listOf(objekt, zahl), eingänge.map { it.art })
        assertEquals(listOf("wert"), ausgänge.map { it.name })
        assertEquals(listOf(zahl), ausgänge.map { it.art })
    }

    @Test
    fun `Migration entfernt den alten Zielmengen-Anschluss und seine Kanten`() {
        val objekt = AnschlussArtId("objekt")
        val quelle = schnittstelle("test.quelle", "Quelle", "wert", objekt, AnschlussRichtung.Ausgang)
        val ausgang = KnotenDaten(
            art = "mathematik.kartenAusgang",
            name = "Ausgang",
            anschlüsse = listOf(
                AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = objekt),
                AnschlussDaten(name = "zielmenge", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = objekt),
            ),
        )
        val zielmenge = ausgang.anschlüsse.first { it.name == "zielmenge" }
        val karte = KartenDaten(
            name = "Alt",
            knoten = listOf(quelle, ausgang),
            verbindungen = listOf(VerbindungDaten(
                von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single().id),
                zu = AnschlussVerweis(ausgang.id, zielmenge.id),
            )),
        )

        val migriert = migriereKartenAusgangZuEinzelanschluss(karte)

        assertEquals(listOf("wert"), migriert.knoten.first { it.id == ausgang.id }.anschlüsse.map { it.name })
        assertEquals(emptyList(), migriert.verbindungen)
    }

    @Test
    fun `Migration normalisiert gespeicherten Extremwert mit dynamischen Eingängen`() {
        val maximum = MathematikKnotenVorlagen.Maximum.erzeuge(GraphPunkt.Zero)
        val dritterEingang = maximum.anschlüsse.first { it.name == "a" }.copy(id = neueAnschlussId(), name = "input3", reihenfolge = 2)
        val alt = maximum.copy(
            anschlüsse = maximum.anschlüsse + dritterEingang,
            parameter = maximum.parameter + mapOf("festeEingänge" to "2", "operatorAnzeige" to "name"),
        )

        val migriert = migriereAssoziativeKnoten(KartenDaten(name = "Alt", knoten = listOf(alt))).knoten.single()

        assertEquals("maximum", migriert.parameter.getValue("modus"))
        assertEquals("name", migriert.parameter.getValue("operatorAnzeige"))
        assertEquals(2, migriert.anschlüsse.count { it.richtung == AnschlussRichtung.Eingang })
        assertTrue(migriert.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.all { it.kannSichErweitern })
    }

    private fun schnittstelle(
        art: String,
        name: String,
        öffentlicherName: String,
        anschlussArt: AnschlussArtId,
        richtung: AnschlussRichtung,
    ) = KnotenDaten(
        art = art,
        name = name,
        parameter = mapOf("name" to öffentlicherName),
        anschlüsse = listOf(
            AnschlussDaten(
                name = "wert",
                richtung = richtung,
                kante = if (richtung == AnschlussRichtung.Eingang) AnschlussKante.Links else AnschlussKante.Rechts,
                art = anschlussArt,
            ),
        ),
    )
}

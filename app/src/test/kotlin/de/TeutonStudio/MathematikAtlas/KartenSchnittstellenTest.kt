package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.MATRIX_EINZEL_EINGABEN
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.WertebereichKonfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KartenSchnittstellenTest {
    @Test
    fun `Migration normalisiert Term zu Methode und Parameter auf abgeleitete Zielmengen`() {
        val variable = KnotenDaten(
            art = "mathematik.variable", name = "x", parameter = mapOf("name" to "x"),
            anschlüsse = listOf(
                AnschlussDaten(name = "wertevorrat", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Menge.id),
                AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Zahl.id),
            ),
        )
        val allgemeinerParameter = KnotenDaten(
            art = "mathematik.allgemeinerParameter", name = "a", parameter = mapOf("name" to "a", "werteVorrat" to "C"),
        )
        val methode = KnotenDaten(
            art = "mathematik.termZuMethode", name = "f",
            anschlüsse = listOf(
                AnschlussDaten(name = "term", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id),
                AnschlussDaten(name = "argument1", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id),
                AnschlussDaten(name = "zielmenge", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Menge.id),
                AnschlussDaten(name = "methode", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.ZahlFunktion.id),
            ),
        )
        val migriert = migriereTermZuMethodeUndVariablen(KartenDaten(name = "Alt", knoten = listOf(variable, allgemeinerParameter, methode)))
        val neueVariable = migriert.knoten.first { it.id == variable.id }
        val neuerAllgemeinerParameter = migriert.knoten.first { it.id == allgemeinerParameter.id }
        val neueMethode = migriert.knoten.first { it.id == methode.id }

        assertEquals(listOf("wert"), neueVariable.anschlüsse.map { it.name })
        assertEquals("R", neueVariable.parameter["werteVorrat"])
        assertEquals(listOf("term", "methode"), neueMethode.anschlüsse.map { it.name })
        assertEquals(MathematikAnschlussArten.Objekt.id, neueMethode.anschlüsse.first { it.name == "term" }.art)
        assertEquals(MathematikAnschlussArten.Funktion.id, neueMethode.anschlüsse.first { it.name == "methode" }.art)
        assertEquals(null, neueMethode.parameter["zielmenge"])
        assertEquals(null, neuerAllgemeinerParameter.parameter["werteVorrat"])
        assertEquals(
            WertebereichKonfiguration.Zahl("C").zuEigenschaft(),
            neuerAllgemeinerParameter.eigenschaften[WertebereichKonfiguration.EIGENSCHAFT],
        )
    }

    @Test
    fun `Migration erweitert den Abbild Methodenanschluss und bewahrt seine ID`() {
        val abbild = MathematikKnotenVorlagen.Abbild.erzeuge(GraphPunkt.Zero)
        val alteMethode = abbild.anschlüsse.first { it.name == "methode" }
        val alt = abbild.copy(anschlüsse = abbild.anschlüsse.map { anschluss ->
            if (anschluss.id == alteMethode.id) anschluss.copy(art = MathematikAnschlussArten.ZahlFunktion.id) else anschluss
        })
        val quelle = KnotenDaten(
            art = "test.methode", name = "Quelle",
            anschlüsse = listOf(AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.ZahlFunktion.id)),
        )
        val verbindung = VerbindungDaten(
            von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single().id),
            zu = AnschlussVerweis(alt.id, alteMethode.id),
        )

        val migriert = migriereAbbildZuAllgemeinerMethode(KartenDaten(name = "Alt", knoten = listOf(quelle, alt), verbindungen = listOf(verbindung)))
        val neueMethode = migriert.knoten.first { it.id == alt.id }.anschlüsse.first { it.name == "methode" }

        assertEquals(alteMethode.id, neueMethode.id)
        assertEquals(MathematikAnschlussArten.Funktion.id, neueMethode.art)
        assertEquals(listOf(verbindung), migriert.verbindungen)
    }

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

    @Test
    fun `Migration ersetzt alte Matrixzeilen durch einzelne Eingänge und entfernt deren Kanten`() {
        val quelle = schnittstelle("test.quelle", "Quelle", "wert", AnschlussArtId("mathematik.vektor.zeile"), AnschlussRichtung.Ausgang)
        val alteMatrix = KnotenDaten(
            art = "mathematik.matrix", name = "Matrix",
            anschlüsse = listOf(
                AnschlussDaten(name = "zeile1", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = AnschlussArtId("mathematik.vektor.zeile")),
                AnschlussDaten(name = "matrix", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = AnschlussArtId("mathematik.matrix")),
            ),
        )
        val karte = KartenDaten(
            name = "Alt", knoten = listOf(quelle, alteMatrix),
            verbindungen = listOf(VerbindungDaten(von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single().id), zu = AnschlussVerweis(alteMatrix.id, alteMatrix.anschlüsse.first { it.name == "zeile1" }.id))),
        )

        val migriert = migriereMatrixKnoten(karte)
        val matrix = migriert.knoten.first { it.id == alteMatrix.id }

        assertEquals(MATRIX_EINZEL_EINGABEN, matrix.parameter["erzeugungsArt"])
        assertEquals(listOf("eintrag_0_0", "eintrag_0_1", "eintrag_1_0", "eintrag_1_1", "matrix"), matrix.anschlüsse.map { it.name })
        assertTrue(migriert.verbindungen.isEmpty())
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

package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDatenJson
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.KompositionsAusgangsModus
import de.TeutonStudio.MathematikRechenSystem.kern.KompositionsBereichsModus
import de.TeutonStudio.MathematikRechenSystem.kern.KompositionsEingangsModus
import kotlin.test.Test
import kotlin.test.assertEquals

class IterierteSelbstkompositionKartenJsonTest {
    @Test
    fun `Knoten roundtrippt Ordnung Modi Budget und Anschluss IDs`() {
        val basis = IterierteSelbstkompositionKnotenVorlagen.Selbstkomposition
            .erzeuge(GraphPunkt.Zero)
        val knoten = basis.copy(
            parameter = basis.parameter + mapOf(
                SELBSTKOMPOSITION_ORDNUNG_PARAMETER to "7",
                SELBSTKOMPOSITION_EINGANGSMODUS_PARAMETER to KompositionsEingangsModus.GEPACKTES_TUPEL.name,
                SELBSTKOMPOSITION_AUSGANGSMODUS_PARAMETER to KompositionsAusgangsModus.ENTPACKT.name,
                SELBSTKOMPOSITION_BEREICHSMODUS_PARAMETER to KompositionsBereichsModus.VOLLSTAENDIGER_URSPRUNGSBEREICH.name,
                SELBSTKOMPOSITION_AUSWERTUNGSBUDGET_PARAMETER to "20",
            ),
        )
        val gelesen = KartenJson.lese(
            KartenJson.schreibe(KartenDaten(name = "Komposition", knoten = listOf(knoten))),
        ).knoten.single()

        assertEquals(SELBSTKOMPOSITION_KNOTEN_ART, gelesen.art)
        assertEquals("7", gelesen.parameter[SELBSTKOMPOSITION_ORDNUNG_PARAMETER])
        assertEquals(
            KompositionsEingangsModus.GEPACKTES_TUPEL.name,
            gelesen.parameter[SELBSTKOMPOSITION_EINGANGSMODUS_PARAMETER],
        )
        assertEquals(
            KompositionsAusgangsModus.ENTPACKT.name,
            gelesen.parameter[SELBSTKOMPOSITION_AUSGANGSMODUS_PARAMETER],
        )
        assertEquals(
            KompositionsBereichsModus.VOLLSTAENDIGER_URSPRUNGSBEREICH.name,
            gelesen.parameter[SELBSTKOMPOSITION_BEREICHSMODUS_PARAMETER],
        )
        assertEquals("20", gelesen.parameter[SELBSTKOMPOSITION_AUSWERTUNGSBUDGET_PARAMETER])
        assertEquals(knoten.anschlüsse.map { it.id }, gelesen.anschlüsse.map { it.id })
    }

    @Test
    fun `historischer Selbstkompositionsknoten wird beim Lesen migriert`() {
        val alt = KnotenDaten(
            art = "mathematik.selbstkompositionIteriert",
            name = "Alt",
            position = GraphPunkt.Zero,
            anschlüsse = IterierteSelbstkompositionKnotenVorlagen.Selbstkomposition.anschlüsse,
            parameter = mapOf("ordnung" to "3"),
        )
        val roh = KartenDatenJson.schreibe(KartenDaten(name = "Alt", knoten = listOf(alt)))

        val gelesen = KartenJson.lese(roh).knoten.single()

        assertEquals(SELBSTKOMPOSITION_KNOTEN_ART, gelesen.art)
        assertEquals("3", gelesen.parameter[SELBSTKOMPOSITION_ORDNUNG_PARAMETER])
        assertEquals(
            KompositionsEingangsModus.GETRENNTE_ARGUMENTE.name,
            gelesen.parameter[SELBSTKOMPOSITION_EINGANGSMODUS_PARAMETER],
        )
        assertEquals(alt.anschlüsse.map { it.id }, gelesen.anschlüsse.map { it.id })
    }
}

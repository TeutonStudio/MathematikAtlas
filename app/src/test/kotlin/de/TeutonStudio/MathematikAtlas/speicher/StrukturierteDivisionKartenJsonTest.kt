package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_DIVISIONSSEITE
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_DIVISIONSSEITE_FEHLT
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_OPERATOR
import de.TeutonStudio.MathematikKnoten.ZahlenRechnerKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.konfiguriereDivisionsSeite
import de.TeutonStudio.MathematikRechenSystem.kern.DivisionsSeite
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StrukturierteDivisionKartenJsonTest {
    @Test
    fun `Divisionsseite und Anschluss IDs bleiben im Karten JSON erhalten`() {
        val division = ZahlenRechnerKnotenVorlagen.alle.single {
            it.standardParameter[ZAHLENRECHNER_OPERATOR] ==
                UniversellerZahlenOperator.DIVISION.stabileId
        }.erzeuge(GraphPunkt.Zero)
        val links = konfiguriereDivisionsSeite(division, DivisionsSeite.LINKS)
        val karte = KartenDaten(name = "Division", knoten = listOf(links))

        val text = KartenJson.schreibe(karte)
        val gelesen = KartenJson.lese(text).knoten.single()

        assertTrue(text.contains("\"$ZAHLENRECHNER_DIVISIONSSEITE\""))
        assertEquals("links", gelesen.parameter[ZAHLENRECHNER_DIVISIONSSEITE])
        assertEquals("false", gelesen.parameter[ZAHLENRECHNER_DIVISIONSSEITE_FEHLT])
        assertEquals(links.anschlüsse.map { it.id }, gelesen.anschlüsse.map { it.id })
        assertEquals("Division (links)", gelesen.name)
    }
}

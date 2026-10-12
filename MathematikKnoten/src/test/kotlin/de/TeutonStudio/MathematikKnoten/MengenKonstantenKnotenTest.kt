package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class MengenKonstantenKnotenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    @Test fun `alle Quellen ueberstehen den echten Codec und liefern die definierte Menge`() {
        MengenKnotenAuswahl.entries.forEach { auswahl ->
            val knoten = MengenKnotenVorlagen.vorlage(auswahl).erzeuge(GraphPunkt.Zero)
            val karte = KartenDaten(name = "Quellen", knoten = listOf(knoten))
            val geladen = MathematikKartenCodec.lese(KartenDatenJson.schreibe(karte)).knoten.single()
            assertEquals(knoten.id, geladen.id)
            assertEquals(knoten.anschlüsse, geladen.anschlüsse)
            assertEquals(auswahl.stabileId, geladen.parameter[MENGEN_KNOTEN_AUSWAHL])
            val ergebnis = register.finde(geladen.art)!!.auswerten(KnotenAuswertungsKontext(geladen, emptyMap(), RechenKontext()))
            assertNull(ergebnis.fehler)
            assertEquals(auswahl.operator.auswerten(), ergebnis.ausgaben.getValue("menge").objekt)
        }
    }

    @Test fun `unbekannte Quellen bleiben erhalten und liefern keinen Ersatzwert`() {
        val knoten = MengenKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero).copy(parameter = mapOf(MENGEN_KNOTEN_AUSWAHL to "future"))
        val geladen = MathematikKartenCodec.lese(KartenDatenJson.schreibe(KartenDaten(name = "Unbekannt", knoten = listOf(knoten)))).knoten.single()
        assertEquals("future", geladen.parameter[MENGEN_KNOTEN_AUSWAHL])
        val ergebnis = register.finde(geladen.art)!!.auswerten(KnotenAuswertungsKontext(geladen, emptyMap(), RechenKontext()))
        assertTrue(ergebnis.ausgaben.isEmpty())
        assertContains(ergebnis.fehler.orEmpty(), "future")
    }

    @Test fun `historische Mengenkennungen werden auf dieselben Quellen migriert`() {
        val alt = KnotenDaten(art = "mathematik.primzahlen", name = "Primzahlen", anschlüsse = MengenKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero).anschlüsse)
        val geladen = MathematikKartenCodec.lade(KartenDatenJson.schreibe(KartenDaten(name = "Alt", knoten = listOf(alt)))).migriereKonsolidierteKnoten().knoten.single()
        assertEquals(MENGEN_KNOTEN_ART, geladen.art)
        assertEquals("P", geladen.parameter[MENGEN_KNOTEN_AUSWAHL])
        assertEquals(alt.anschlüsse.single().id, geladen.anschlüsse.single().id)
    }
}

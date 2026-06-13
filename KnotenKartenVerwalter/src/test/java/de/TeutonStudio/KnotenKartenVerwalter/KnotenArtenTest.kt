package de.TeutonStudio.KnotenKartenVerwalter

import com.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import com.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.AusgabeKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.EingabeKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.KnotenArten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.RechenKnoten
import com.TeutonStudio.KnotenKartenVerwalter.daten.ZahlenTyp
import com.TeutonStudio.KnotenKartenVerwalter.daten.Zahlenraum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KnotenArtenTest {
    @Test
    fun eingabeKnotenHatNurAusgaenge() {
        val knoten = KnotenArten.Standard.erstelle(
            KnotenDaten(id = "eingabe", name = "Eingabe", art = EingabeKnoten.KNOTEN_ART),
        )

        assertTrue(knoten.eingänge.isEmpty())
        assertEquals(1, knoten.ausgänge.size)
        assertTrue(knoten.erhalteAnschlüsseGeordnet(AnschlussRichtung.Eingang).isEmpty())
        assertEquals(1, knoten.erhalteAnschlüsseGeordnet(AnschlussRichtung.Ausgang).size)
    }

    @Test
    fun ausgabeKnotenHatNurEingaenge() {
        val knoten = KnotenArten.Standard.erstelle(
            KnotenDaten(id = "ausgabe", name = "Ausgabe", art = AusgabeKnoten.KNOTEN_ART),
        )

        assertEquals(1, knoten.eingänge.size)
        assertTrue(knoten.ausgänge.isEmpty())
        assertEquals(1, knoten.erhalteAnschlüsseGeordnet(AnschlussRichtung.Eingang).size)
        assertTrue(knoten.erhalteAnschlüsseGeordnet(AnschlussRichtung.Ausgang).isEmpty())
    }

    @Test
    fun rechenKnotenTraegtZahlenTypAnAnschluessen() {
        val zahlenTyp = ZahlenTyp(Zahlenraum.Rational)
        val knoten = KnotenArten.Standard.erstelle(
            KnotenDaten(
                id = "plus",
                name = "Plus",
                art = RechenKnoten.KNOTEN_ART,
                data = mapOf("zahlenTyp" to zahlenTyp),
            ),
        )

        assertEquals(2, knoten.eingänge.size)
        assertEquals(1, knoten.ausgänge.size)
        assertEquals(zahlenTyp, knoten.erhalteAnschlüsseGeordnet(AnschlussRichtung.Eingang).first().zahlenTyp)
        assertEquals(zahlenTyp, knoten.erhalteAnschlüsseGeordnet(AnschlussRichtung.Ausgang).first().zahlenTyp)
    }
}

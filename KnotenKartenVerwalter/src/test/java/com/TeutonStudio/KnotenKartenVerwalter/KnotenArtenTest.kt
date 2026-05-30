package com.TeutonStudio.KnotenKartenVerwalter

import com.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import com.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AusgabeKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.EingabeKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenArten
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
}

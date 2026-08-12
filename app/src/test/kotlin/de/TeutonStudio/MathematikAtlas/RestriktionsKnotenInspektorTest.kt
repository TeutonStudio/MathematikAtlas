package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikKnoten.RestriktionsKnotenVorlagen
import kotlin.test.Test
import kotlin.test.assertEquals

class RestriktionsKnotenInspektorTest {
    @Test
    fun `Umsortierung tauscht nur semantische Reihenfolge und behaelt Anschluss IDs`() {
        val grund = RestriktionsKnotenVorlagen.Bereichsanpassung.erzeuge(GraphPunkt.Zero)
        val eins = ergänzung("ergänzung.0", 2)
        val zwei = ergänzung("ergänzung.1", 3)
        val knoten = grund.copy(anschlüsse = grund.anschlüsse + eins + zwei)

        val getauscht = tauscheErgänzungsReihenfolge(knoten, eins, zwei)
        val gelesenEins = getauscht.anschlüsse.single { it.id == eins.id }
        val gelesenZwei = getauscht.anschlüsse.single { it.id == zwei.id }

        assertEquals(3, gelesenEins.reihenfolge)
        assertEquals(2, gelesenZwei.reihenfolge)
        assertEquals(eins.id, gelesenEins.id)
        assertEquals(zwei.id, gelesenZwei.id)
    }

    private fun ergänzung(name: String, reihenfolge: Int) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Methode.id,
        reihenfolge = reihenfolge,
        dynamischErzeugt = true,
    )
}

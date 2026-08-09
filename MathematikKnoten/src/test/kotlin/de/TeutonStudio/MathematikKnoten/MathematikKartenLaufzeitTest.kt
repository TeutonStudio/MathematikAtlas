package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MathematikKartenLaufzeitTest {
    @Test
    fun `Laufzeit stellt kanonischen Katalog und Gesamtauswerter gemeinsam bereit`() {
        val laufzeit = MathematikKartenLaufzeit()
        val zahl = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero)
        val ergebnis = laufzeit.auswerten(KartenDaten(name = "Laufzeit", knoten = listOf(zahl)))

        assertTrue(laufzeit.vorlagen.any { it.art == zahl.art })
        assertNotNull(ergebnis.knoten[zahl.id]?.ausgaben?.get("wert"))
    }

    @Test
    fun `Cache kann über die gemeinsame Laufzeit gezielt und vollständig verworfen werden`() {
        val laufzeit = MathematikKartenLaufzeit()
        val zahl = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero)
        val karte = KartenDaten(name = "Cache", knoten = listOf(zahl))

        assertNotNull(laufzeit.auswerten(karte).knoten[zahl.id])
        laufzeit.verwerfeCache(zahl.id)
        assertNotNull(laufzeit.auswerten(karte).knoten[zahl.id])
        laufzeit.leereCache()
        assertNotNull(laufzeit.auswerten(karte).knoten[zahl.id])
    }
}

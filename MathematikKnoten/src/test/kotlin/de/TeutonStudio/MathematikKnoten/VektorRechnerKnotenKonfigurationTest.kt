package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechner
import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechnerOperator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VektorRechnerKnotenKonfigurationTest {
    @Test
    fun `Vektorfeldintegral hat nur Vektorfeld und Menge als sichtbare Eingaenge`() {
        val anschluesse = vektorRechnerAnschluesse(VektorRechnerOperator.VEKTORFELD_INTEGRIEREN)
        val eingaenge = anschluesse.filter { it.richtung == AnschlussRichtung.Eingang }

        assertEquals(listOf("vektorfeld", "menge"), eingaenge.sortedBy { it.reihenfolge }.map { it.name })
        assertEquals(1, anschluesse.count { it.richtung == AnschlussRichtung.Ausgang })
    }

    @Test
    fun `Zerlegen bietet vor bekannter Struktur die endliche Indexmethode an`() {
        val anschluesse = vektorRechnerAnschluesse(VektorRechnerOperator.ZERLEGEN)

        assertEquals(2, anschluesse.size)
        assertEquals("struktur", anschluesse.single { it.richtung == AnschlussRichtung.Eingang }.name)
        assertEquals("methode", anschluesse.single { it.richtung == AnschlussRichtung.Ausgang }.name)
        assertEquals(MathematikAnschlussArten.Methode.id, anschluesse.single { it.richtung == AnschlussRichtung.Ausgang }.art)
    }

    @Test
    fun `Legacy Tupelaufloeser wird mit Anschluss IDs in Zerlegen migriert`() {
        val eingangId = AnschlussId("legacy-in")
        val ausgangId = AnschlussId("legacy-out")
        val alt = KnotenDaten(
            art = TUPEL_AUFLÖSEN_ART,
            name = "Tupel auflösen",
            anschlüsse = listOf(
                AnschlussDaten(
                    id = eingangId,
                    name = "tupel",
                    richtung = AnschlussRichtung.Eingang,
                    kante = AnschlussKante.Links,
                    art = MathematikAnschlussArten.Tupel.id,
                ),
                AnschlussDaten(
                    id = ausgangId,
                    name = "element-1",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = MathematikAnschlussArten.Zahl.id,
                ),
            ),
        )

        val migriert = KartenDaten(name = "Legacy", knoten = listOf(alt))
            .migriereLegacyVektorStrukturKnoten()
            .knoten.single()

        assertEquals(VektorRechner.KNOTEN_ART, migriert.art)
        assertEquals(VektorRechnerOperator.ZERLEGEN.stabileId, migriert.parameter[VEKTOR_RECHNER_OPERATOR])
        assertTrue(migriert.anschlüsse.any { it.id == eingangId && it.name == "struktur" })
        assertTrue(migriert.anschlüsse.any { it.id == ausgangId && it.name == "element1" })
    }
}

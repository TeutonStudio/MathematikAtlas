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
    fun `Zerlegen erfindet vor bekannter Struktur keine Ausgaenge`() {
        val anschluesse = vektorRechnerAnschluesse(VektorRechnerOperator.ZERLEGEN)

        assertEquals(1, anschluesse.size)
        assertEquals(AnschlussRichtung.Eingang, anschluesse.single().richtung)
        assertEquals("struktur", anschluesse.single().name)
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

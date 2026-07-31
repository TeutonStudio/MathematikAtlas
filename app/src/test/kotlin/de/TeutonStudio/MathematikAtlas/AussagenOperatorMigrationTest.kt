package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.MathematikKnoten.AUSSAGEN_LOGIK_SEMANTIK
import de.TeutonStudio.MathematikKnoten.AUSSAGEN_LOGIK_XOR
import de.TeutonStudio.MathematikKnoten.AussagenLogikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AussagenOperatorMigrationTest {
    @Test
    fun `historische Adjunktion wird verlustfrei zur Konjunktion`() {
        val alt = MathematikKnotenVorlagen.Adjunktion.erzeuge(GraphPunkt(12f, 34f))
        val karte = KartenDaten(name = "Alt", knoten = listOf(alt))

        val migriert = migriereAussagenOperatoren(karte)
        val knoten = migriert.knoten.single()

        assertEquals("mathematik.konjunktion", knoten.art)
        assertEquals("Konjunktion", knoten.name)
        assertEquals(alt.id, knoten.id)
        assertEquals(alt.anschlüsse.map { it.id }, knoten.anschlüsse.map { it.id })
        assertTrue(knoten.anschlüsse.filter { it.name in setOf("a", "b") }.all { it.kannSichErweitern })
        assertEquals(migriert, migriereAussagenOperatoren(migriert))
    }

    @Test
    fun `historische iterierte Adjunktion wird zur iterierten Konjunktion`() {
        val alt = MathematikKnotenVorlagen.IterierteAdjunktion.erzeuge(GraphPunkt.Zero)
        val migriert = migriereAussagenOperatoren(KartenDaten(name = "Alt", knoten = listOf(alt))).knoten.single()

        assertEquals(MathematikKnotenVorlagen.ITERIERTE_AUSSAGENVERKNÜPFUNG_ART, migriert.art)
        assertEquals("konjunktion", migriert.parameter["operator"])
        assertEquals("Iterierte Konjunktion", migriert.name)
    }

    @Test
    fun `neue XOR Adjunktion wird nicht umgedeutet`() {
        val neu = AussagenLogikKnotenVorlagen.Adjunktion.erzeuge(GraphPunkt.Zero)
        val migriert = migriereAussagenOperatoren(KartenDaten(name = "Neu", knoten = listOf(neu))).knoten.single()

        assertEquals("mathematik.adjunktion", migriert.art)
        assertEquals(AUSSAGEN_LOGIK_XOR, migriert.parameter[AUSSAGEN_LOGIK_SEMANTIK])
    }
}

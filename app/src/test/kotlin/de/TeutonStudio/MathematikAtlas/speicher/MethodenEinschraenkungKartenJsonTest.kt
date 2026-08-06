package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDatenJson
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKnoten.METHODEN_EINSCHRAENKUNG_KNOTEN_ART
import de.TeutonStudio.MathematikKnoten.MethodenEinschraenkungKnotenVorlagen
import kotlin.test.Test
import kotlin.test.assertEquals

class MethodenEinschraenkungKartenJsonTest {
    @Test
    fun `Restriktionsknoten behaelt Art und Anschluss IDs`() {
        val knoten = MethodenEinschraenkungKnotenVorlagen.Einschraenkung.erzeuge(GraphPunkt.Zero)
        val gelesen = KartenJson.lese(
            KartenJson.schreibe(KartenDaten(name = "Restriktion", knoten = listOf(knoten))),
        ).knoten.single()

        assertEquals(METHODEN_EINSCHRAENKUNG_KNOTEN_ART, gelesen.art)
        assertEquals(knoten.anschlüsse.map { it.id }, gelesen.anschlüsse.map { it.id })
    }

    @Test
    fun `historische Restriktionsart wird beim Lesen migriert`() {
        val alt = KnotenDaten(
            art = "mathematik.methodenEinschraenkung",
            name = "Alt",
            position = GraphPunkt.Zero,
            anschlüsse = MethodenEinschraenkungKnotenVorlagen.Einschraenkung.anschlüsse,
        )
        val roh = KartenDatenJson.schreibe(KartenDaten(name = "Alt", knoten = listOf(alt)))

        val gelesen = KartenJson.lese(roh).knoten.single()

        assertEquals(METHODEN_EINSCHRAENKUNG_KNOTEN_ART, gelesen.art)
        assertEquals(alt.anschlüsse.map { it.id }, gelesen.anschlüsse.map { it.id })
    }
}

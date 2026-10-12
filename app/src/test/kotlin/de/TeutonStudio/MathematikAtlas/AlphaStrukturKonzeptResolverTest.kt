package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*
import kotlin.test.*

class AlphaStrukturKonzeptResolverTest {
    private fun text(k: KonzeptDefinition)=k.reiter.flatMap { it.karte.knoten }.joinToString("\n") { it.parameter["regel"].orEmpty() }
    @Test fun `Definitionsresolver liefert fuer jedes gespeicherte Uebersetzungsziel eine passende Karte`() {
        val basis=ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero)
        val ids=mutableSetOf<KonzeptId>()
        ÜBERSETZER_ZIELE.forEach { ziel ->
            val knoten=konfiguriereÜbersetzer(basis,ziel)
            val konzept=assertNotNull(dynamischesKonzeptFürKnoten(knoten))
            ids+=konzept.id
            assertTrue(konzept.name.contains(ziel))
            assertEquals(1,konzept.reiter.count { it.rolle==KonzeptReiterRolle.Definition })
            assertTrue(konzept.reiter.single().karte.knoten.isNotEmpty())
            assertTrue(text(konzept).contains("ohne Konjugation"))
            if(ziel=="methode") {
                assertTrue(text(konzept).contains("{()}"));assertTrue(text(konzept).contains("Außenquellen"))
                assertTrue(text(konzept).contains("Annahmen"));assertTrue(text(konzept).contains("Einertupel"))
            } else assertTrue(text(konzept).contains("symbolische"))
        }
        assertEquals(ÜBERSETZER_ZIELE.size,ids.size)
        assertEquals(dynamischesKonzeptFürKnoten(konfiguriereÜbersetzer(basis,"spalte"))!!.id,dynamischesKonzeptFürKnoten(basis)!!.id)
    }
    @Test fun `Abflachendefinition im echten Resolver erklaert DFS Reihenfolge Leerheit und heterogene Blaetter`() {
        val k=ÜbersetzungsKnotenVorlagen.Abflachen.erzeuge(GraphPunkt.Zero)
        val konzept=assertNotNull(dynamischesKonzeptFürKnoten(k))
        assertEquals(setOf(TUPEL_ABFLACHEN_ART),konzept.knotenArten)
        val regeln=text(konzept)
        listOf("DFS","links nach rechts","F(())=()","kein Tupel","Mengen","Aussagen","weder sortiert noch dedupliziert","Annahmen","Variablenquellen").forEach { assertTrue(regeln.contains(it),it) }
        assertEquals(konzept.reiter.single().karte,MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(konzept.reiter.single().karte)))
    }
}

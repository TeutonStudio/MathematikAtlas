package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntervallMigrationTest {
    @Test
    fun alteIntervalleErhaltenStabileAnschluesse() {
        val halb = MathematikKnotenVorlagen.HalbesIntervall.erzeuge(GraphPunkt.Zero)
            .copy(
                name = "Halbes Intervall (Obergrenze)",
                parameter = mapOf("richtung" to "abwaerts"),
            )
        val voll = MathematikKnotenVorlagen.ReellesIntervall.erzeuge(GraphPunkt.Zero)
        val alte = listOf(halb, voll).map { knoten ->
            knoten.copy(
                anschlüsse = knoten.anschlüsse.filterNot { it.name == "grundmenge" },
                parameter = knoten.parameter - "standardMenge",
            )
        }
        val karte = KartenDaten(name = "Alte Intervalle", knoten = alte)
        val erste = migriereAssoziativeKnoten(karte)
        val zweite = migriereAssoziativeKnoten(erste)
        assertEquals(erste, zweite)
        alte.zip(erste.knoten).forEach { (vorher, nachher) ->
            assertEquals(vorher.id, nachher.id)
            assertEquals(vorher.anschlüsse, nachher.anschlüsse.filterNot { it.name == "grundmenge" })
            assertEquals(1, nachher.anschlüsse.count { it.name == "grundmenge" })
            assertEquals("R", nachher.parameter["standardMenge"])
            assertEquals(
                MathematikAnschlussArten.Menge.id,
                nachher.anschlüsse.single { it.name == "grundmenge" }.art,
            )
        }
        assertEquals("abwaerts", erste.knoten.first().parameter["richtung"])
    }
}

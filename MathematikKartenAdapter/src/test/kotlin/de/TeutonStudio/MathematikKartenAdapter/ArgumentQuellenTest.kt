package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArgumentQuellenTest {
    @Test
    fun `argumentquellen bleiben geordnet und werden semantisch dedupliziert`() {
        val zweite = VariablenQuelle(
            knotenId = KnotenId("b"),
            name = "B",
            werteVorrat = WahrheitsMenge,
            reihenfolge = 1,
            argumentArt = ArgumentQuellenArt.Aussage,
            aussage = AussagenParameter("B"),
        )
        val erste = VariablenQuelle(
            knotenId = KnotenId("a"),
            name = "x",
            werteVorrat = ReelleZahlen,
            reihenfolge = 0,
        )

        assertEquals(listOf(erste, zweite), listOf(zweite, erste, erste).geordnetEindeutig())
        assertEquals(
            listOf(
                PrädikatsArgument.Wert("x", ReelleZahlen, "a:x"),
                PrädikatsArgument.AussageWert("B", "B", "b:B"),
            ),
            BedingterWert(AllgemeinerParameter("wert"), variablenQuellen = listOf(zweite, erste, erste))
                .prädikatsArgumente(),
        )
    }

    @Test
    fun `doppelte offene praedikatsnamen werden als kartenfehler gemeldet`() {
        fun prädikat(ausdruck: Aussage) = Methode(
            name = "P",
            parameter = listOf(AussagenParameter("A")),
            ausgaben = mapOf("aussage" to ausdruck),
            zielMengen = mapOf("aussage" to WahrheitsMenge),
            werteVorräte = mapOf("A" to WahrheitsMenge),
        )
        val erstes = prädikat(AussagenParameter("A"))
        val zweites = prädikat(Negation(AussagenParameter("A")))
        val ergebnis = KartenAuswertungsErgebnis(
            knoten = mapOf(
                KnotenId("eins") to KnotenAuswertungsErgebnis(mapOf("methode" to BedingterWert(erstes))),
                KnotenId("zwei") to KnotenAuswertungsErgebnis(mapOf("methode" to BedingterWert(zweites))),
            ),
            emptyList(),
        )

        assertTrue(ergebnis.fehler.single().contains("Prädikatsname 'P'"))
    }
}

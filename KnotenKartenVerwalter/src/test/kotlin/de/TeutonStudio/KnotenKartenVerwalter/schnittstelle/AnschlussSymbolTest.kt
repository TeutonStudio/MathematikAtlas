package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.TypSystem.AnschlussVertrag
import de.TeutonStudio.TypSystem.TypAusdruck
import de.TeutonStudio.TypSystem.TypId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AnschlussSymbolTest {
    private val zahl = TypAusdruck.Atom(TypId("mathematik.zahl"))
    private val aussage = TypAusdruck.Atom(TypId("mathematik.aussage"))
    private val menge = TypAusdruck.Atom(TypId("mathematik.menge"))

    @Test
    fun `Methodenanschluss trennt Argument- und Ausgabefarben`() {
        val argument = tupel(zahl, aussage)
        val methode = TypAusdruck.Parameterisiert(
            TypId("mathematik.methode"),
            listOf(argument, menge),
        )
        val plan = assertIs<AnschlussSymbolPlan.Methode>(anschlussSymbolPlan(anschluss(methode)))

        assertEquals(listOf("mathematik.zahl", "mathematik.aussage"), plan.argumentFarben)
        assertEquals(listOf("mathematik.menge"), plan.ausgabeFarben)
    }

    @Test
    fun `Methode ohne Signatur fällt auf Tupel beider Seiten zurück`() {
        val plan = assertIs<AnschlussSymbolPlan.Methode>(
            anschlussSymbolPlan(
                AnschlussDaten(
                    name = "methode",
                    kante = AnschlussKante.Links,
                    art = AnschlussArtId("mathematik.methode"),
                ),
            ),
        )

        assertEquals(listOf("mathematik.tupel"), plan.argumentFarben)
        assertEquals(listOf("mathematik.tupel"), plan.ausgabeFarben)
    }

    @Test
    fun `Endliches Tupel erhält einen Ring je Element in Reihenfolge`() {
        val plan = assertIs<AnschlussSymbolPlan.Tupel>(
            anschlussSymbolPlan(anschluss(tupel(zahl, aussage, menge))),
        )

        assertEquals(3, plan.ringe.size)
        assertEquals(listOf("mathematik.zahl"), plan.ringe[0].farben)
        assertEquals(listOf("mathematik.aussage"), plan.ringe[1].farben)
        assertEquals(listOf("mathematik.menge"), plan.ringe[2].farben)
        assertTrue(plan.ringe.none { it.gepunktet })
    }

    @Test
    fun `Unendliches Tupel zeigt drei Komponenten und zwei Fortsetzungsringe`() {
        val unendlich = TypAusdruck.Parameterisiert(
            TypId("typ.tupel.unendlich"),
            listOf(zahl),
        )
        val plan = assertIs<AnschlussSymbolPlan.Tupel>(anschlussSymbolPlan(anschluss(unendlich)))

        assertEquals(5, plan.ringe.size)
        assertTrue(plan.ringe.take(3).none { it.gepunktet })
        assertTrue(plan.ringe.takeLast(2).all { it.gepunktet })
        assertTrue(plan.ringe.all { it.farben == listOf("mathematik.zahl") })
    }

    @Test
    fun `Typunterarten verwenden die sichtbare Oberartfarbe`() {
        assertEquals("mathematik.zahl", normalisiereFarbId("mathematik.zahl.reell"))
        assertEquals("mathematik.vektor", normalisiereFarbId("mathematik.vektor.spalte"))
        assertEquals("mathematik.methode", normalisiereFarbId("mathematik.funktion.zahl"))
        assertEquals("mathematik.tupel", normalisiereFarbId("typ.tupel"))
        assertFalse(normalisiereFarbId("mathematik.menge").isBlank())
    }

    private fun tupel(vararg komponenten: TypAusdruck): TypAusdruck =
        TypAusdruck.Parameterisiert(TypId("typ.tupel"), komponenten.toList())

    private fun anschluss(typ: TypAusdruck): AnschlussDaten = AnschlussDaten(
        name = "wert",
        kante = AnschlussKante.Links,
        art = AnschlussArtId("mathematik.objekt"),
        vertrag = AnschlussVertrag(typ = typ),
    )
}

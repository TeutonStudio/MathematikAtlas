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
        val anschluss = anschluss(methode)
        val plan = assertIs<AnschlussSymbolPlan.Methode>(anschlussSymbolPlan(anschluss))

        assertEquals(listOf("mathematik.zahl", "mathematik.aussage"), plan.argumentFarben)
        assertEquals(listOf("mathematik.menge"), plan.ausgabeFarben)
        assertEquals(
            listOf(AnschlussArtId("mathematik.methode")),
            sichtbareAnschlussArtIds(anschluss),
        )
    }

    @Test
    fun `Methodenhalbkreise verwenden dreifachen bisherigen Abstand`() {
        assertEquals(3f, METHODEN_HALBKREIS_ABSTAND_FAKTOR)
    }

    @Test
    fun `Methode ohne Signatur fällt auf Tupel beider Seiten zurück`() {
        val anschluss = AnschlussDaten(
            name = "methode",
            kante = AnschlussKante.Links,
            art = AnschlussArtId("mathematik.methode"),
        )
        val plan = assertIs<AnschlussSymbolPlan.Methode>(anschlussSymbolPlan(anschluss))

        assertEquals(listOf("mathematik.tupel"), plan.argumentFarben)
        assertEquals(listOf("mathematik.tupel"), plan.ausgabeFarben)
        assertEquals(
            listOf(AnschlussArtId("mathematik.methode")),
            sichtbareAnschlussArtIds(anschluss),
        )
    }

    @Test
    fun `Endliches Tupel erhält einen Ring je Element in Reihenfolge`() {
        val anschluss = anschluss(tupel(zahl, aussage, menge))
        val plan = assertIs<AnschlussSymbolPlan.Tupel>(anschlussSymbolPlan(anschluss))

        assertEquals(3, plan.ringe.size)
        assertEquals(listOf("mathematik.zahl"), plan.ringe[0].farben)
        assertEquals(listOf("mathematik.aussage"), plan.ringe[1].farben)
        assertEquals(listOf("mathematik.menge"), plan.ringe[2].farben)
        assertTrue(plan.ringe.none { it.gepunktet })
        assertEquals(
            listOf(AnschlussArtId("mathematik.tupel")),
            sichtbareAnschlussArtIds(anschluss),
        )
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
    fun `Standardanschluss bewahrt ODER Signatur für Legende`() {
        val gemischt = AnschlussDaten(
            name = "wert",
            kante = AnschlussKante.Links,
            art = AnschlussArtId("mathematik.objekt"),
            zulässigeArten = setOf(
                AnschlussArtId("mathematik.zahl"),
                AnschlussArtId("mathematik.menge"),
            ),
        )

        assertEquals(
            listOf(
                AnschlussArtId("mathematik.menge"),
                AnschlussArtId("mathematik.zahl"),
            ),
            sichtbareAnschlussArtIds(gemischt),
        )
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

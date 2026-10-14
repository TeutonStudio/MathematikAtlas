package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class IntervallGrundmengeTest {
    private val auswerter = StandardMathematikAuswerter.erzeugeRegister()
    private fun z(n: Long) = RationaleZahl.von(n)

    private fun berechne(knoten: KnotenDaten, eingänge: Map<String, BedingterWert>): MengenAusdruck =
        auswerter.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(knoten, eingänge, RechenKontext()),
        ).ausgaben.getValue("menge").objekt as MengenAusdruck

    @Test fun halbesIntervallHatNurEineVorlage() {
        val vorlagen = MathematikKnotenVorlagen.alle.filter { it.art == "mathematik.halbesIntervall" }
        assertEquals(1, vorlagen.size)
        val knoten = vorlagen.single().erzeuge(GraphPunkt.Zero)
        assertEquals("aufwaerts", knoten.parameter["richtung"])
        assertEquals("R", knoten.parameter["standardMenge"])
        assertEquals(listOf("grenze", "offen", "grundmenge"),
            knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }
                .sortedBy { it.reihenfolge }.map { it.name })
    }

    @Test fun bestehendeRichtungswerteBleibenGueltig() {
        val alt = MathematikKnotenVorlagen.HalbesIntervall.erzeuge(GraphPunkt.Zero)
            .copy(parameter = mapOf("richtung" to "abwaerts"))
        val menge = assertIs<DefinierteMenge>(
            berechne(alt, mapOf("grenze" to BedingterWert(z(3)))))
        assertEquals(ReelleZahlen, menge.variablen.single().grundMenge)
        val vergleich = assertIs<Vergleich>(menge.bedingung)
        assertEquals(VergleichsArt.KleinerGleich, vergleich.art)
        assertEquals(Variable("x"), vergleich.links)
        assertEquals(z(3), vergleich.rechts)
    }

    @Test fun standardmengeUndMengeneingang() {
        val knoten = MathematikKnotenVorlagen.HalbesIntervall.erzeuge(GraphPunkt.Zero)
            .copy(parameter = mapOf("richtung" to "aufwaerts", "standardMenge" to "Z"))
        val grenze = mapOf("grenze" to BedingterWert(z(2)))
        assertTrue(GanzeZahlen in assertIs<Schnitt>(berechne(knoten, grenze)).mengen)
        val verbunden = assertIs<Schnitt>(
            berechne(knoten, grenze + ("grundmenge" to BedingterWert(RationaleZahlen))))
        assertTrue(RationaleZahlen in verbunden.mengen)
        assertTrue(GanzeZahlen !in verbunden.mengen)
    }

    @Test fun reellesIntervallUebernimmtBeideGrundmengen() {
        val knoten = MathematikKnotenVorlagen.ReellesIntervall.erzeuge(GraphPunkt.Zero)
        val grenzen = mapOf("links" to BedingterWert(z(1)), "rechts" to BedingterWert(z(3)))
        assertIs<ReellesIntervall>(berechne(knoten, grenzen))
        val standard = assertIs<Schnitt>(berechne(
            knoten.copy(parameter = mapOf("standardMenge" to "N")), grenzen))
        assertTrue(NatürlicheZahlen in standard.mengen)
        val verbunden = assertIs<Schnitt>(
            berechne(knoten, grenzen + ("grundmenge" to BedingterWert(GanzeZahlen))))
        assertTrue(GanzeZahlen in verbunden.mengen)
    }

    @Test fun unpassendeTypenWerdenAbgelehnt() {
        val knoten = MathematikKnotenVorlagen.HalbesIntervall.erzeuge(GraphPunkt.Zero)
        val grenze = mapOf("grenze" to BedingterWert(z(0)))
        assertFailsWith<IllegalStateException> {
            berechne(knoten, grenze + ("grundmenge" to BedingterWert(z(1))))
        }
        assertFailsWith<IllegalArgumentException> {
            berechne(knoten.copy(parameter = mapOf("standardMenge" to "C")), grenze)
        }
    }
}

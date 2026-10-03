package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OrbitKnotenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    @Test
    fun `Orbitvorlagen besitzen stabile Anschlüsse und registrierte Auswerter`() {
        val orbit = MathematikKnotenVorlagen.Orbit
        val beschränktheit = MathematikKnotenVorlagen.OrbitBeschraenktheit

        assertEquals("mathematik.orbit", orbit.art)
        assertEquals("mathematik.orbitBeschraenktheit", beschränktheit.art)
        assertEquals(listOf("schritt", "start"), orbit.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.map { it.name })
        assertEquals(listOf("orbit"), orbit.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }.map { it.name })
        assertEquals(listOf("orbit", "parameterraum"), beschränktheit.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.map { it.name })
        assertTrue(orbit in MathematikKnotenVorlagen.alle)
        assertTrue(beschränktheit in MathematikKnotenVorlagen.alle)
        assertNotNull(register.finde(orbit.art))
        assertNotNull(register.finde(beschränktheit.art))
    }

    @Test
    fun `Auswerter reichen die generische Orbitfamilie in die Beschränktheitsmenge`() {
        val z = Variable("z")
        val c = Variable("c")
        val schritt = Methode(
            name = "f",
            parameter = listOf(z, c),
            vorschrift = addition(Potenz(z, RationaleZahl.von(2)), c),
            zielMenge = KomplexeZahlen,
            werteVorräte = mapOf("z" to KomplexeZahlen, "c" to KomplexeZahlen),
        )
        val orbitKnoten = MathematikKnotenVorlagen.Orbit.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf("zustandsArgument" to "c"),
        )
        val orbitErgebnis = register.finde(orbitKnoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                orbitKnoten,
                mapOf("schritt" to BedingterWert(schritt), "start" to BedingterWert(RationaleZahl.Null)),
                RechenKontext(),
            ),
        )
        val orbit = assertIs<OrbitFamilie>(orbitErgebnis.ausgaben.getValue("orbit").objekt)
        assertEquals(listOf("c", "z"), orbit.schritt.parameter.map { it.name })

        val mengenKnoten = MathematikKnotenVorlagen.OrbitBeschraenktheit.erzeuge(GraphPunkt.Zero)
        val mengenErgebnis = register.finde(mengenKnoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                mengenKnoten,
                mapOf("orbit" to BedingterWert(orbit), "parameterraum" to BedingterWert(KomplexeZahlen)),
                RechenKontext(),
            ),
        )

        val menge = assertIs<OrbitBeschraenktheitsMenge>(mengenErgebnis.ausgaben.getValue("menge").objekt)
        assertEquals(orbit, menge.orbit)
        assertEquals(KomplexeZahlen, menge.parameterRaum)
    }

    @Test
    fun `Orbit Auswerter melden fehlende Eingänge und ungültige Methodensignaturen`() {
        val knoten = MathematikKnotenVorlagen.Orbit.erzeuge(GraphPunkt.Zero)
        val auswerter = register.finde(knoten.art)!!

        assertFailsWith<IllegalStateException> {
            auswerter.auswerten(KnotenAuswertungsKontext(knoten, emptyMap(), RechenKontext()))
        }

        val x = Variable("x")
        val einstellig = Methode(
            name = "g",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )
        assertFailsWith<IllegalArgumentException> {
            auswerter.auswerten(
                KnotenAuswertungsKontext(
                    knoten,
                    mapOf(
                        "schritt" to BedingterWert(einstellig),
                        "start" to BedingterWert(RationaleZahl.Null),
                    ),
                    RechenKontext(),
                ),
            )
        }

        val beschränktheit = MathematikKnotenVorlagen.OrbitBeschraenktheit.erzeuge(GraphPunkt.Zero)
        assertFailsWith<IllegalStateException> {
            register.finde(beschränktheit.art)!!.auswerten(
                KnotenAuswertungsKontext(beschränktheit, emptyMap(), RechenKontext()),
            )
        }
    }

    @Test
    fun `Orbit Auswerter lehnen nachweisbar unvereinbare Definitionsbereiche ab`() {
        val z = Variable("z")
        val p = Variable("p")
        val natürlicherZustand = Methode(
            name = "natürlicherZustand",
            parameter = listOf(z, p),
            vorschrift = z,
            zielMenge = NatürlicheZahlen,
            werteVorräte = mapOf("z" to NatürlicheZahlen, "p" to ReelleZahlen),
        )
        val orbitKnoten = MathematikKnotenVorlagen.Orbit.erzeuge(GraphPunkt.Zero)
        assertFailsWith<IllegalArgumentException> {
            register.finde(orbitKnoten.art)!!.auswerten(
                KnotenAuswertungsKontext(
                    orbitKnoten,
                    mapOf(
                        "schritt" to BedingterWert(natürlicherZustand),
                        "start" to BedingterWert(RationaleZahl.Null),
                    ),
                    RechenKontext(),
                ),
            )
        }

        val natürlicherParameter = Methode(
            name = "natürlicherParameter",
            parameter = listOf(z, p),
            vorschrift = z,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("z" to ReelleZahlen, "p" to NatürlicheZahlen),
        )
        val orbit = OrbitFamilie(natürlicherParameter, RationaleZahl.Eins)
        val beschränktheit = MathematikKnotenVorlagen.OrbitBeschraenktheit.erzeuge(GraphPunkt.Zero)
        assertFailsWith<IllegalArgumentException> {
            register.finde(beschränktheit.art)!!.auswerten(
                KnotenAuswertungsKontext(
                    beschränktheit,
                    mapOf(
                        "orbit" to BedingterWert(orbit),
                        "parameterraum" to BedingterWert(ReelleZahlen),
                    ),
                    RechenKontext(),
                ),
            )
        }
    }

    @Test
    fun `Auswahl des Zustandsarguments permutiert den effektiven Methodenbereich`() {
        val p = Variable("p")
        val z = Variable("z")
        val methode = Methode(
            name = "geordnet",
            parameter = listOf(p, z),
            vorschrift = z,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("p" to ReelleZahlen, "z" to ReelleZahlen),
            effektiverWerteVorrat = Tupelraum(listOf(NatürlicheZahlen, ReelleZahlen)),
        )
        val knoten = MathematikKnotenVorlagen.Orbit.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf("zustandsArgument" to "z"),
        )
        val ergebnis = register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "schritt" to BedingterWert(methode),
                    "start" to BedingterWert(RationaleZahl.Eins),
                ),
                RechenKontext(),
            ),
        )
        val orbit = assertIs<OrbitFamilie>(ergebnis.ausgaben.getValue("orbit").objekt)
        val schritt = orbit.schritt.alsMathematischeMethode("Test")

        assertEquals(listOf("z", "p"), schritt.parameter.map { it.name })
        assertEquals(Tupelraum(listOf(ReelleZahlen, NatürlicheZahlen)), schritt.effektiverWerteVorrat)
        assertIs<OrbitEntscheidung.Unbekannt>(
            entscheideOrbitBeschraenktheit(
                OrbitBeschraenktheitsMenge(orbit, ReelleZahlen),
                RationaleZahl.von(1, 2),
            ),
        )
    }
}

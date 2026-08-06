package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AlgebraischePotenzKnotenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    private fun kontext(
        knoten: KnotenDaten,
        eingaben: Map<String, BedingterWert>,
    ) = KnotenAuswertungsKontext(
        knoten = knoten,
        eingänge = eingaben,
        rechenKontext = RechenKontext(),
    )

    @Test
    fun `Vorlagenkatalog enthaelt genau einen strukturellen Potenzknoten`() {
        val vorlagen = alleMathematikKnotenVorlagen().filter { it.art == ALGEBRAISCHE_POTENZ_KNOTEN_ART }

        assertEquals(1, vorlagen.size)
        assertEquals(
            setOf("basis", "ordnung", "struktur"),
            vorlagen.single().anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }
                .map { it.name }.toSet(),
        )
        assertEquals("wert", vorlagen.single().anschlüsse.single {
            it.richtung == AnschlussRichtung.Ausgang
        }.name)
    }

    @Test
    fun `Zahlpotenz wird automatisch ausgewertet`() {
        val knoten = AlgebraischePotenzKnotenVorlagen.Potenz.erzeuge(GraphPunkt.Zero)
        val ergebnis = assertNotNull(register.finde(ALGEBRAISCHE_POTENZ_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "basis" to BedingterWert(RationaleZahl.von(2)),
                    "ordnung" to BedingterWert(RationaleZahl.von(10)),
                ),
            ),
        )

        assertEquals(RationaleZahl.von(1024), ergebnis.ausgaben.getValue("wert").objekt)
        assertTrue(ergebnis.warnungen.any { it.contains("potenz.zahlbereich") })
    }

    @Test
    fun `Matrixpotenz liefert Matrix im selben Raum`() {
        val matrix = Matrix(
            listOf(
                listOf(RationaleZahl.von(2), RationaleZahl.Null),
                listOf(RationaleZahl.Null, RationaleZahl.von(3)),
            ),
        )
        val knoten = AlgebraischePotenzKnotenVorlagen.Potenz.erzeuge(GraphPunkt.Zero)
        val ergebnis = assertNotNull(register.finde(ALGEBRAISCHE_POTENZ_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "basis" to BedingterWert(matrix),
                    "ordnung" to BedingterWert(RationaleZahl.von(2)),
                ),
            ),
        )

        assertIs<Matrix>(ergebnis.ausgaben.getValue("wert").objekt)
        assertIs<Matrizenraum>(ergebnis.ausgaben.getValue("wert").zielMenge)
    }

    @Test
    fun `Methode wird punktweise potenziert und bleibt Methode`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = addition(x, RationaleZahl.Eins),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )
        val knoten = AlgebraischePotenzKnotenVorlagen.Potenz.erzeuge(GraphPunkt.Zero)
        val ergebnis = assertNotNull(register.finde(ALGEBRAISCHE_POTENZ_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "basis" to BedingterWert(methode),
                    "ordnung" to BedingterWert(RationaleZahl.von(2)),
                ),
            ),
        )
        val ausgabe = assertIs<Methode>(ergebnis.ausgaben.getValue("wert").objekt)

        assertEquals(methode.werteVorräte, ausgabe.werteVorräte)
        assertEquals(methode.zielMenge, ausgabe.zielMenge)
        assertEquals("{f}^{2}", ausgabe.name)
    }

    @Test
    fun `symbolische Ordnung bleibt bedingt in N null`() {
        val n = Variable("n")
        val knoten = AlgebraischePotenzKnotenVorlagen.Potenz.erzeuge(GraphPunkt.Zero)
        val ergebnis = assertNotNull(register.finde(ALGEBRAISCHE_POTENZ_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "basis" to BedingterWert(RationaleZahl.von(2)),
                    "ordnung" to BedingterWert(n),
                ),
            ),
        )

        assertIs<AlgebraischePotenz>(ergebnis.ausgaben.getValue("wert").objekt)
        assertTrue(ergebnis.ausgaben.getValue("wert").annahmen.any {
            it.zuLatex().contains("mathbb N_0")
        })
        assertTrue(ergebnis.warnungen.any { it.contains("BEDINGT") })
    }

    @Test
    fun `Tupel ohne Struktur erzeugt eindeutige Diagnose`() {
        val knoten = AlgebraischePotenzKnotenVorlagen.Potenz.erzeuge(GraphPunkt.Zero)
        val ergebnis = assertNotNull(register.finde(ALGEBRAISCHE_POTENZ_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "basis" to BedingterWert(
                        Tupel(listOf(RationaleZahl.Eins, RationaleZahl.von(2))),
                    ),
                    "ordnung" to BedingterWert(RationaleZahl.von(2)),
                ),
            ),
        )

        assertTrue(ergebnis.ausgaben.isEmpty())
        assertTrue(ergebnis.fehler.orEmpty().contains("potenzstruktur_nicht_eindeutig"))
    }

    @Test
    fun `expliziter Strukturmodus verlangt Strukturvertrag`() {
        val basis = AlgebraischePotenzKnotenVorlagen.Potenz.erzeuge(GraphPunkt.Zero)
        val knoten = basis.copy(
            parameter = basis.parameter +
                (POTENZ_STRUKTUR_MODUS_PARAMETER to PotenzStrukturModus.EXPLIZIT.name),
        )
        val ergebnis = assertNotNull(register.finde(ALGEBRAISCHE_POTENZ_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "basis" to BedingterWert(RationaleZahl.von(2)),
                    "ordnung" to BedingterWert(RationaleZahl.von(2)),
                ),
            ),
        )

        assertTrue(ergebnis.fehler.orEmpty().contains("explizite Strukturmodus"))
    }

    @Test
    fun `historischer Potenzknoten migriert idempotent`() {
        val alt = KnotenDaten(
            art = "mathematik.potenz",
            name = "Alt",
            position = GraphPunkt.Zero,
            anschlüsse = AlgebraischePotenzKnotenVorlagen.Potenz.anschlüsse,
            parameter = mapOf("exponent" to "5"),
        )
        val karte = KartenDaten(name = "Alt", knoten = listOf(alt))

        val migriert = karte.migriereAlgebraischePotenzKnoten()

        assertEquals(ALGEBRAISCHE_POTENZ_KNOTEN_ART, migriert.knoten.single().art)
        assertEquals("5", migriert.knoten.single().parameter[POTENZ_ORDNUNG_PARAMETER])
        assertEquals(alt.anschlüsse.map { it.id }, migriert.knoten.single().anschlüsse.map { it.id })
        assertEquals(migriert, migriert.migriereAlgebraischePotenzKnoten())
    }
}

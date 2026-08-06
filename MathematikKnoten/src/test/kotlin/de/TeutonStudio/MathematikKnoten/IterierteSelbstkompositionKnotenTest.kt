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

class IterierteSelbstkompositionKnotenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()
    private val x = Variable("x")

    private fun kontext(
        knoten: KnotenDaten,
        eingaben: Map<String, BedingterWert>,
    ) = KnotenAuswertungsKontext(
        knoten = knoten,
        eingänge = eingaben,
        rechenKontext = RechenKontext(),
    )

    private fun verschiebung(): Methode = Methode(
        name = "f",
        parameter = listOf(x),
        vorschrift = addition(x, RationaleZahl.Eins),
        zielMenge = ReelleZahlen,
        werteVorräte = mapOf("x" to ReelleZahlen),
    )

    @Test
    fun `Vorlagenkatalog enthaelt genau einen Selbstkompositionsknoten`() {
        val vorlagen = alleMathematikKnotenVorlagen().filter {
            it.art == SELBSTKOMPOSITION_KNOTEN_ART
        }

        assertEquals(1, vorlagen.size)
        assertEquals(
            setOf("methode", "ordnung"),
            vorlagen.single().anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }
                .map { it.name }.toSet(),
        )
        assertEquals("methode", vorlagen.single().anschlüsse.single {
            it.richtung == AnschlussRichtung.Ausgang
        }.name)
    }

    @Test
    fun `zweite Komposition wird als Methode ausgewertet`() {
        val knoten = IterierteSelbstkompositionKnotenVorlagen.Selbstkomposition
            .erzeuge(GraphPunkt.Zero)
        val ergebnis = assertNotNull(register.finde(SELBSTKOMPOSITION_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "methode" to BedingterWert(verschiebung()),
                    "ordnung" to BedingterWert(RationaleZahl.von(2)),
                ),
            ),
        )
        val methode = assertIs<Methode>(ergebnis.ausgaben.getValue("methode").objekt)

        assertEquals(addition(x, RationaleZahl.von(2)), methode.vorschrift)
        assertEquals(ReelleZahlen, methode.werteVorräte.getValue("x"))
        assertTrue(ergebnis.warnungen.any { it.contains("TOTAL_GUELTIG") })
    }

    @Test
    fun `mehrstellige Methode bleibt mehrstellig`() {
        val y = Variable("y")
        val vertauschung = Methode(
            name = "s",
            parameter = listOf(x, y),
            vorschrift = Tupel(listOf(y, x)),
            zielMenge = Tupelraum(listOf(ReelleZahlen, ReelleZahlen)),
            werteVorräte = mapOf("x" to ReelleZahlen, "y" to ReelleZahlen),
        )
        val knoten = IterierteSelbstkompositionKnotenVorlagen.Selbstkomposition
            .erzeuge(GraphPunkt.Zero)
        val ergebnis = assertNotNull(register.finde(SELBSTKOMPOSITION_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "methode" to BedingterWert(vertauschung),
                    "ordnung" to BedingterWert(RationaleZahl.von(2)),
                ),
            ),
        )
        val methode = assertIs<Methode>(ergebnis.ausgaben.getValue("methode").objekt)

        assertEquals(2, methode.parameter.size)
        assertEquals(Tupel(listOf(x, y)), methode.vorschrift)
    }

    @Test
    fun `gepackter Nullfall erzeugt einstellige Identitaet`() {
        val y = Variable("y")
        val methode = Methode(
            name = "g",
            parameter = listOf(x, y),
            vorschrift = Tupel(listOf(x, y)),
            zielMenge = Tupelraum(listOf(ReelleZahlen, ReelleZahlen)),
            werteVorräte = mapOf("x" to ReelleZahlen, "y" to ReelleZahlen),
        )
        val basis = IterierteSelbstkompositionKnotenVorlagen.Selbstkomposition
            .erzeuge(GraphPunkt.Zero)
        val knoten = basis.copy(
            parameter = basis.parameter + mapOf(
                SELBSTKOMPOSITION_EINGANGSMODUS_PARAMETER to KompositionsEingangsModus.GEPACKTES_TUPEL.name,
                SELBSTKOMPOSITION_AUSGANGSMODUS_PARAMETER to KompositionsAusgangsModus.GEPACKT.name,
            ),
        )
        val ergebnis = assertNotNull(register.finde(SELBSTKOMPOSITION_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "methode" to BedingterWert(methode),
                    "ordnung" to BedingterWert(RationaleZahl.Null),
                ),
            ),
        )
        val identitaet = assertIs<Methode>(ergebnis.ausgaben.getValue("methode").objekt)

        assertEquals(1, identitaet.parameter.size)
        assertEquals(aeussererMethodenWertevorrat(methode), identitaet.zielMenge)
    }

    @Test
    fun `symbolische Ordnung bleibt bedingte strukturierte Methode`() {
        val n = Variable("n")
        val knoten = IterierteSelbstkompositionKnotenVorlagen.Selbstkomposition
            .erzeuge(GraphPunkt.Zero)
        val ergebnis = assertNotNull(register.finde(SELBSTKOMPOSITION_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "methode" to BedingterWert(verschiebung()),
                    "ordnung" to BedingterWert(n),
                ),
            ),
        )
        val methode = assertIs<Methode>(ergebnis.ausgaben.getValue("methode").objekt)

        assertIs<IterierteSelbstkomposition>(methode.vorschrift)
        assertTrue(ergebnis.ausgaben.getValue("methode").annahmen.any {
            it.zuLatex().contains("mathbb N_0")
        })
    }

    @Test
    fun `inkompatible Komponentenzahl erzeugt transparenten Fehler`() {
        val y = Variable("y")
        val methode = Methode(
            name = "g",
            parameter = listOf(x, y),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen, "y" to ReelleZahlen),
        )
        val knoten = IterierteSelbstkompositionKnotenVorlagen.Selbstkomposition
            .erzeuge(GraphPunkt.Zero)
        val ergebnis = assertNotNull(register.finde(SELBSTKOMPOSITION_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "methode" to BedingterWert(methode),
                    "ordnung" to BedingterWert(RationaleZahl.von(2)),
                ),
            ),
        )

        assertTrue(ergebnis.ausgaben.isEmpty())
        assertTrue(ergebnis.fehler.orEmpty().contains("MATHEMATISCH_UNMOEGLICH"))
    }

    @Test
    fun `historischer Knoten migriert idempotent`() {
        val alt = KnotenDaten(
            art = "mathematik.selbstkompositionIteriert",
            name = "Alt",
            position = GraphPunkt.Zero,
            anschlüsse = IterierteSelbstkompositionKnotenVorlagen.Selbstkomposition.anschlüsse,
            parameter = mapOf("ordnung" to "5"),
        )
        val karte = KartenDaten(name = "Alt", knoten = listOf(alt))

        val migriert = karte.migriereIterierteSelbstkompositionKnoten()

        assertEquals(SELBSTKOMPOSITION_KNOTEN_ART, migriert.knoten.single().art)
        assertEquals("5", migriert.knoten.single().parameter[SELBSTKOMPOSITION_ORDNUNG_PARAMETER])
        assertEquals(alt.anschlüsse.map { it.id }, migriert.knoten.single().anschlüsse.map { it.id })
        assertEquals(migriert, migriert.migriereIterierteSelbstkompositionKnoten())
    }
}

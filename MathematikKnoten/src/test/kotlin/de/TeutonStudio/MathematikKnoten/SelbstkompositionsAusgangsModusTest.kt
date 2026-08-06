package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class SelbstkompositionsAusgangsModusTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    private fun werteAus(
        methode: Methode,
        modus: KompositionsAusgangsModus,
    ): Methode {
        val basis = IterierteSelbstkompositionKnotenVorlagen.Selbstkomposition
            .erzeuge(GraphPunkt.Zero)
        val knoten = basis.copy(
            parameter = basis.parameter + mapOf(
                SELBSTKOMPOSITION_AUSGANGSMODUS_PARAMETER to modus.name,
                SELBSTKOMPOSITION_ORDNUNG_PARAMETER to "1",
            ),
        )
        val ergebnis = assertNotNull(register.finde(SELBSTKOMPOSITION_KNOTEN_ART)).auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf("methode" to BedingterWert(methode)),
                rechenKontext = RechenKontext(),
            ),
        )
        return assertIs(ergebnis.ausgaben.getValue("methode").objekt)
    }

    @Test
    fun `Tupelausgang kann einleistig entpackt werden`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = Tupel(listOf(x, RationaleZahl.Eins)),
            zielMenge = Tupelraum(listOf(ReelleZahlen, ReelleZahlen)),
            werteVorräte = mapOf("x" to ReelleZahlen),
        )

        val entpackt = werteAus(methode, KompositionsAusgangsModus.ENTPACKT)

        assertEquals(2, entpackt.ausgabeNamen.size)
        assertEquals(Tupelraum(listOf(ReelleZahlen, ReelleZahlen)), entpackt.zielMenge)
    }

    @Test
    fun `Zeilenvektor kann als Tupelausgang dargestellt werden`() {
        val x = Variable("x")
        val methode = Methode(
            name = "z",
            parameter = listOf(x),
            vorschrift = ZeilenVektor(listOf(x, RationaleZahl.Eins)),
            zielMenge = Vektorraum(VektorOrientierung.Zeile, 2, ReelleZahlen),
            werteVorräte = mapOf("x" to ReelleZahlen),
        )

        val entpackt = werteAus(methode, KompositionsAusgangsModus.ENTPACKT)

        assertIs<Tupel>(entpackt.vorschrift)
        assertEquals(2, entpackt.ausgabeNamen.size)
        assertEquals(Tupelraum(listOf(ReelleZahlen, ReelleZahlen)), entpackt.zielMenge)
    }

    @Test
    fun `mehrere Ausgaenge koennen wieder gepackt werden`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = Tupel(listOf(x, RationaleZahl.Eins)),
            zielMenge = Tupelraum(listOf(ReelleZahlen, ReelleZahlen)),
            werteVorräte = mapOf("x" to ReelleZahlen),
            ausgabeNamen = listOf("links", "rechts"),
        )

        val gepackt = werteAus(methode, KompositionsAusgangsModus.GEPACKT)

        assertEquals(listOf("wert"), gepackt.ausgabeNamen)
        assertIs<Tupel>(gepackt.vorschrift)
    }

    @Test
    fun `verschachteltes Tupel wird beim Entpacken nicht rekursiv abgeflacht`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = Tupel(listOf(Tupel(listOf(x, RationaleZahl.Eins)), x)),
            zielMenge = Tupelraum(
                listOf(
                    Tupelraum(listOf(ReelleZahlen, ReelleZahlen)),
                    ReelleZahlen,
                ),
            ),
            werteVorräte = mapOf("x" to ReelleZahlen),
        )

        val entpackt = werteAus(methode, KompositionsAusgangsModus.ENTPACKT)
        val tupel = assertIs<Tupel>(entpackt.vorschrift)

        assertEquals(2, entpackt.ausgabeNamen.size)
        assertIs<Tupel>(tupel.elemente.first())
    }
}

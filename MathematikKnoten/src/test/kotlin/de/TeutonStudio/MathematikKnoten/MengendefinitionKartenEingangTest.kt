package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.PrädikatsMenge
import kotlin.test.*

class MengendefinitionKartenEingangTest {
    @Test
    fun `Vereinigung akzeptiert Mengen aus öffentlichen Karten-Eingängen`() {
        val a = kartenEingang("A")
        val b = kartenEingang("B")
        val paar = erzeugeMengendefinitionsPaar(GraphPunkt.Zero)
        val elementA = elementKnoten()
        val elementB = elementKnoten()
        val disjunktion = disjunktionKnoten()

        val karte = KartenDaten(
            name = "Vereinigung",
            knoten = listOf(a, b, paar.konstruktor, elementA, elementB, disjunktion, paar.definator),
            verbindungen = listOf(
                verbinde(a, "wert", elementA, "rechts"),
                verbinde(b, "wert", elementB, "rechts"),
                verbinde(paar.konstruktor, "element", elementA, "links"),
                verbinde(paar.konstruktor, "element", elementB, "links"),
                verbinde(elementA, "aussage", disjunktion, "a"),
                verbinde(elementB, "aussage", disjunktion, "b"),
                verbinde(disjunktion, "aussage", paar.definator, "aussage"),
            ),
        )

        val ergebnis = KartenAuswerter(StandardMathematikAuswerter.erzeugeRegister()).auswerten(karte)

        assertTrue(ergebnis.fehler.isEmpty(), ergebnis.fehler.joinToString("\n"))
        val menge = ergebnis.knoten.getValue(paar.definator.id).ausgaben.getValue("menge")
        assertIs<PrädikatsMenge>(menge.objekt)
        assertEquals(
            "M=\\left\\{x\\mid x \\in A \\lor x \\in B\\right\\}",
            menge.anzeigeLatex(),
        )
    }

    private fun kartenEingang(name: String) = KnotenDaten(
        art = KARTEN_EINGANG_ART,
        name = "Karten-Eingang",
        parameter = mapOf("name" to name),
        anschlüsse = listOf(
            AnschlussDaten(
                name = "wert",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Menge.id,
            ),
        ),
    )

    private fun elementKnoten() = KnotenDaten(
        art = "mathematik.element",
        name = "Element",
        anschlüsse = listOf(
            AnschlussDaten(
                name = "links",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Objekt.id,
                reihenfolge = 0,
            ),
            AnschlussDaten(
                name = "rechts",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Menge.id,
                reihenfolge = 1,
            ),
            AnschlussDaten(
                name = "aussage",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Aussage.id,
            ),
        ),
    )

    private fun disjunktionKnoten() = KnotenDaten(
        art = "mathematik.disjunktion",
        name = "Disjunktion",
        anschlüsse = listOf(
            AnschlussDaten(
                name = "a",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Aussage.id,
                reihenfolge = 0,
            ),
            AnschlussDaten(
                name = "b",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Aussage.id,
                reihenfolge = 1,
            ),
            AnschlussDaten(
                name = "aussage",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Aussage.id,
            ),
        ),
    )

    private fun verbinde(
        von: KnotenDaten,
        vonName: String,
        zu: KnotenDaten,
        zuName: String,
    ) = VerbindungDaten(
        von = AnschlussVerweis(von.id, von.anschlüsse.first { it.name == vonName }.id),
        zu = AnschlussVerweis(zu.id, zu.anschlüsse.first { it.name == zuName }.id),
    )
}

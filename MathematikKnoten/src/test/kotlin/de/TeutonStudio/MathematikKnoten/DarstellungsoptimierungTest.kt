package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswerter
import de.TeutonStudio.MathematikKartenAdapter.anzeigeLatex
import kotlin.test.*

class DarstellungsoptimierungTest {
    @Test fun `Aliase ändern nur die Darstellung und bleiben je Graphpfad getrennt`() {
        val zahl = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero).copy(
            id = KnotenId("zahl"),
            parameter = mapOf("wert" to "2"),
        )
        val aliasU = MathematikKnotenVorlagen.Darstellungsoptimierung.erzeuge(GraphPunkt(240f, 0f)).copy(
            id = KnotenId("alias-u"),
            parameter = mapOf("latex" to "u"),
        )
        val aliasV = MathematikKnotenVorlagen.Darstellungsoptimierung.erzeuge(GraphPunkt(240f, 180f)).copy(
            id = KnotenId("alias-v"),
            parameter = mapOf("latex" to "v"),
        )
        val addition = MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt(520f, 90f)).copy(id = KnotenId("addition"))
        val karte = KartenDaten(
            name = "Aliase",
            knoten = listOf(zahl, aliasU, aliasV, addition),
            verbindungen = listOf(
                VerbindungDaten(von = ausgang(zahl), zu = eingang(aliasU, "wert")),
                VerbindungDaten(von = ausgang(zahl), zu = eingang(aliasV, "wert")),
                VerbindungDaten(von = ausgang(aliasU), zu = eingang(addition, "a")),
                VerbindungDaten(von = ausgang(aliasV), zu = eingang(addition, "b")),
            ),
        )

        val ergebnis = KartenAuswerter(StandardMathematikAuswerter.erzeugeRegister()).auswerten(karte)
        assertTrue(ergebnis.fehler.isEmpty(), ergebnis.fehler.joinToString())
        val zahlWert = ergebnis.knoten.getValue(zahl.id).ausgaben.getValue("wert")
        val uWert = ergebnis.knoten.getValue(aliasU.id).ausgaben.getValue("wert")
        val vWert = ergebnis.knoten.getValue(aliasV.id).ausgaben.getValue("wert")
        val summe = ergebnis.knoten.getValue(addition.id).ausgaben.getValue("wert")

        assertSame(zahlWert.objekt, uWert.objekt)
        assertSame(zahlWert.objekt, vWert.objekt)
        assertEquals("u", uWert.anzeigeLatex())
        assertEquals("v", vWert.anzeigeLatex())
        assertEquals("u + v", summe.anzeigeLatex())
    }

    private fun ausgang(knoten: KnotenDaten) = AnschlussVerweis(
        knoten.id,
        knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }.id,
    )

    private fun eingang(knoten: KnotenDaten, name: String) = AnschlussVerweis(
        knoten.id,
        knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang && it.name == name }.id,
    )
}

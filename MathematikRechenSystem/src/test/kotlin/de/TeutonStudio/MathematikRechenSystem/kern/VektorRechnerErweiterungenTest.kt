package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VektorRechnerErweiterungenTest {
    private fun quelle(vararg werte: Long): VektorQuelle {
        val vektor = SpaltenVektor(werte.map(RationaleZahl::von))
        return VektorQuelle.Vektor(
            wert = vektor,
            vertrag = KartesischerKoordinatenVertrag(
                dimension = werte.size,
                zahlbereich = FundamentalerZahlbereich.RATIONAL,
                basisId = "standard",
                koordinatensystemId = "kartesisch",
                standardBasis = true,
            ),
        )
    }

    @Test
    fun `Distanz verwendet austauschbare registrierbare Metrikdefinition`() {
        var übergebeneDifferenzen: List<ZahlAusdruck>? = null
        val testMetrik = VektorMetrikDefinition("test.metrik", "Test") { differenzen ->
            übergebeneDifferenzen = differenzen
            RationaleZahl.von(42)
        }

        val ergebnis = VektorRechner.erzeuge(
            VektorRechnerAnfrage(
                operator = VektorRechnerOperator.DISTANZ,
                vektoren = listOf(quelle(1, 2), quelle(4, 6)),
                metrik = testMetrik,
            ),
        )

        val zahl = assertIs<VektorRechnerErgebnis.ZahlWert>(ergebnis)
        assertEquals(RationaleZahl.von(42), zahl.wert)
        assertEquals(2, übergebeneDifferenzen?.size)
    }

    @Test
    fun `Winkel zu Achse ist einsbasiert und lehnt Achse ausserhalb der Dimension ab`() {
        val ergebnis = VektorRechner.erzeuge(
            VektorRechnerAnfrage(
                operator = VektorRechnerOperator.WINKEL_ZU_ACHSE,
                vektoren = listOf(quelle(1, 2, 3)),
                achse = 4,
            ),
        )

        val fehler = assertIs<VektorRechnerErgebnis.Ungueltig>(ergebnis)
        assertEquals("achse_ausserhalb_dimension", fehler.code)
        assertTrue(fehler.nachricht.contains("1-basiert"))
    }

    @Test
    fun `Winkel zu Achse ist fuer Nullvektor undefiniert`() {
        val ergebnis = VektorRechner.erzeuge(
            VektorRechnerAnfrage(
                operator = VektorRechnerOperator.WINKEL_ZU_ACHSE,
                vektoren = listOf(quelle(0, 0)),
                achse = 1,
            ),
        )

        assertEquals("winkel_nullvektor", assertIs<VektorRechnerErgebnis.Ungueltig>(ergebnis).code)
    }

    @Test
    fun `Vektorfeldintegral erfordert explizites Mass`() {
        val x = Variable("x")
        val feld = Methode(
            name = "F",
            parameter = listOf(x),
            vorschrift = Tupel(listOf(x, x)),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )

        val ergebnis = VektorRechner.erzeuge(
            VektorRechnerAnfrage(
                operator = VektorRechnerOperator.VEKTORFELD_INTEGRIEREN,
                methode = feld,
                menge = ReelleZahlen,
                mass = null,
            ),
        )

        assertEquals("integrationsmass_fehlt", assertIs<VektorRechnerErgebnis.Ungueltig>(ergebnis).code)
    }

    @Test
    fun `Zusammenfuehren akzeptiert Elemente und Tupel geordnet`() {
        val ergebnis = VektorRechner.erzeuge(
            VektorRechnerAnfrage(
                operator = VektorRechnerOperator.ZUSAMMENFUEHREN,
                objekte = listOf(
                    RationaleZahl.Eins,
                    Tupel(listOf(RationaleZahl.von(2), RationaleZahl.von(3))),
                ),
                strukturAusgabe = VektorStrukturAusgabe.TUPEL,
            ),
        )

        val wert = assertIs<VektorRechnerErgebnis.VektorWert>(ergebnis).wert
        assertEquals(
            Tupel(listOf(RationaleZahl.Eins, RationaleZahl.von(2), RationaleZahl.von(3))),
            wert,
        )
    }
}

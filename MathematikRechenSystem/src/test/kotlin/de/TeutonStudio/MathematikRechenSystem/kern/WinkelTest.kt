package de.TeutonStudio.MathematikRechenSystem.kern

import de.TeutonStudio.TypSystem.TypPrüfung
import kotlin.test.*

class WinkelTest {
    private fun z(n: Long) = RationaleZahl.von(n)
    private fun wert(z: ZahlAusdruck) = ((DomaenenAuswerter.wert(z) as DomaenenErgebnis.Wert).wert as DomaenenWert.Reell).wert.toDouble()
    @Test fun `Grad normalisiert rationale Klassen exakt und bleibt von R getrennt`() {
        assertEquals(GradWinkel(z(0)), GradWinkel(z(360)))
        assertEquals(Wahrheitswert.Wahr, entscheideGleichheit(GradWinkel(Addition.roh(listOf(z(180), z(180)))), GradWinkel(z(0))).wahrheitswert)
        assertFails { GradWinkel(KomplexeZahl(z(1), z(1))) }
        assertFails { WinkelAusBogenmass(KomplexeZahl(z(1), z(1))) }
        assertEquals(GradWinkel(z(270)), GradWinkel(z(-90)))
        assertEquals(GradWinkel(RationaleZahl.von(1, 2)), GradWinkel(RationaleZahl.von(721, 2)))
        assertEquals(Wahrheitswert.Wahr, ElementBeziehung(GradWinkel(z(90)), Gradmenge).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Lüge, ElementBeziehung(GradWinkel(z(90)), ReelleZahlen).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Lüge, ElementBeziehung(z(90), Gradmenge).entscheide().wahrheitswert)
        assertFalse(MathematischeTypen.istAtomUntertyp(MathematischeTypen.Winkel, MathematischeTypen.Reell))
        assertEquals(Gradmenge, inferiereZielmenge(GradWinkel(z(90))))
    }
    @Test fun `Pi Vielfache und beide Hauptvertreter behandeln die Schnittstelle exakt`() {
        assertEquals(GradWinkel(z(90)), winkelAusBogenmass(Division(Pi, z(2))))
        assertEquals(GradWinkel(z(180)), winkelAusBogenmass(negation(Pi)))
        assertEquals(GradWinkel(z(0)), winkelAusBogenmass(multiplikation(z(2), Pi)))
        val w = GradWinkel(z(180))
        assertEquals(Math.PI, wert(winkelBogenmass(w)), 1e-14)
        assertEquals(-Math.PI, wert(winkelBogenmass(w, WinkelVertreter.SYMMETRISCH)), 1e-14)
        assertEquals(0.0, wert(winkelBogenmass(GradWinkel(z(360)))), 0.0)
        assertEquals(1.0, wert(SinusGrad(GradWinkel(z(90)))), 1e-14)
    }
    @Test fun `symbolische Winkel lassen sich substituieren und numerisch auswerten`() {
        val x = Variable("x")
        val q = winkelAusBogenmass(x)
        val sinus = SinusGrad(q)
        assertEquals(setOf(x), sinus.freieMethodenParameter())
        val ersetzt = ersetze(sinus, mapOf("x" to Division(Pi, z(2))))
        assertEquals(1.0, wert(ersetzt), 1e-14)
        assertIs<DomaenenErgebnis.Unentscheidbar>(DomaenenAuswerter.wert(sinus))
        assertIs<DomaenenErgebnis.Unentscheidbar>(DomaenenAuswerter.wert(WinkelBogenmass(WinkelParameter("w"))))
    }
    @Test fun `kleine Bogenmasse verlieren beim Sinus und Hauptvertreter keine Stellen`() {
        for (x in listOf(1e-20, -1e-20)) {
            val w = WinkelAusBogenmass(rationaleKoordinate(x))
            assertEquals(x, wert(SinusGrad(w)), 1e-35)
            assertEquals(x, wert(WinkelBogenmass(w, WinkelVertreter.SYMMETRISCH)), 1e-35)
        }
        assertEquals(1e-20, wert(WinkelBogenmass(WinkelAusBogenmass(rationaleKoordinate(1e-20)))), 1e-35)
    }
    @Test fun `Methodenkomposition durch Grad ist total und substitutionsfaehig`() {
        val x = Variable("x"); val w = WinkelParameter("w")
        val q = MathematischeMethode("q", listOf(x), winkelAusBogenmass(x), Gradmenge, mapOf("x" to ReelleZahlen))
        val sin = MathematischeMethode("sinGrad", listOf(w), SinusGrad(w), ReelleZahlen, mapOf("w" to Gradmenge))
        assertTrue(prüfeKompositionsKette(listOf(sin, q)).istGültig)
        val zusammengesetzt = komponiere(listOf(sin, q)) as MathematischeMethode
        assertEquals(setOf(x), zusammengesetzt.vorschrift.freieMethodenParameter())
        assertEquals(1.0, wert(ersetze(zusammengesetzt.vorschrift, mapOf("x" to Division(Pi, z(2)))) as ZahlAusdruck), 1e-14)
    }
    @Test fun `heterogene Komponentenobermengen bilden die Vereinigung und behalten Produktvertrag`() {
        val raum = Tupelraum(listOf(ReelleZahlen, WahrheitsMenge, Gradmenge))
        assertEquals(vereinige(raum.komponenten), raum.gemeinsameGrundmenge())
        assertTrue(raum.potenzLatex().endsWith("^{3}"))
        assertTrue(raum.zuLatex().contains("\\times"))
        assertEquals("\\{()\\}", Tupelraum(emptyList()).potenzLatex())
        val ziel = strukturRaumÜbersetzung(Tupelraum(listOf(ReelleZahlen, RationaleZahlen)), ZeilenVektor(listOf(z(1), z(2)))) as Vektorraum
        assertEquals(ReelleZahlen, ziel.skalarMenge)
        assertEquals(VektorOrientierung.Zeile, ziel.orientierung)
    }
}

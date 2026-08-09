package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.AussageStatus
import de.TeutonStudio.MathematikRechenSystem.kern.DefinierteMenge
import de.TeutonStudio.MathematikRechenSystem.kern.EigenschaftsAussage
import de.TeutonStudio.MathematikRechenSystem.kern.EndlicheMenge
import de.TeutonStudio.MathematikRechenSystem.kern.LeereMenge
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.Potenz
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import de.TeutonStudio.MathematikRechenSystem.kern.ReelleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.Variable
import de.TeutonStudio.MathematikRechenSystem.kern.addition
import de.TeutonStudio.MathematikRechenSystem.kern.multiplikation
import de.TeutonStudio.MathematikRechenSystem.kern.negation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ExakteEigenschaftsAuswertungTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()
    private val x = Variable("x")

    @Test
    fun `x Quadrat ist streng konvex und nicht konkav`() {
        val quadrat = methode(Potenz(x, RationaleZahl.von(2)))

        assertEquals(AussageStatus.BEWIESEN, prüfeGlobal(quadrat, "konvex", streng = true).aussageStatus)
        assertEquals(AussageStatus.WIDERLEGT, prüfeGlobal(quadrat, "konkav", streng = false).aussageStatus)
    }

    @Test
    fun `minus x Quadrat ist streng konkav`() {
        val negativQuadratisch = methode(negation(Potenz(x, RationaleZahl.von(2))))

        assertEquals(AussageStatus.BEWIESEN, prüfeGlobal(negativQuadratisch, "konkav", streng = true).aussageStatus)
        assertEquals(AussageStatus.WIDERLEGT, prüfeGlobal(negativQuadratisch, "konvex", streng = false).aussageStatus)
    }

    @Test
    fun `affine Methode ist nicht streng konvex und nicht streng konkav`() {
        val affin = methode(addition(multiplikation(RationaleZahl.von(3), x), RationaleZahl.von(2)))

        assertEquals(AussageStatus.BEWIESEN, prüfeGlobal(affin, "konvex", streng = false).aussageStatus)
        assertEquals(AussageStatus.BEWIESEN, prüfeGlobal(affin, "konkav", streng = false).aussageStatus)
        assertEquals(AussageStatus.WIDERLEGT, prüfeGlobal(affin, "konvex", streng = true).aussageStatus)
        assertEquals(AussageStatus.WIDERLEGT, prüfeGlobal(affin, "konkav", streng = true).aussageStatus)
    }

    @Test
    fun `x hoch drei ist global weder konvex noch konkav`() {
        val kubisch = methode(Potenz(x, RationaleZahl.von(3)))

        assertEquals(AussageStatus.WIDERLEGT, prüfeGlobal(kubisch, "konvex", streng = false).aussageStatus)
        assertEquals(AussageStatus.WIDERLEGT, prüfeGlobal(kubisch, "konkav", streng = false).aussageStatus)
    }

    @Test
    fun `x hoch drei liefert exakte Wendestelle und Sattelstelle`() {
        val kubisch = methode(Potenz(x, RationaleZahl.von(3)))

        val wende = prüfeStellen(kubisch, MathematischeEigenschaftRegister.Wendestelle.id)
        val sattel = prüfeStellen(kubisch, MathematischeEigenschaftRegister.Sattelpunkt.id)

        assertEquals(EndlicheMenge(setOf(RationaleZahl.Null)), wende)
        assertEquals(EndlicheMenge(setOf(RationaleZahl.Null)), sattel)
    }

    @Test
    fun `Konvexitaetsbereich von x Quadrat wird zur ganzen Grundmenge normalisiert`() {
        val quadrat = methode(Potenz(x, RationaleZahl.von(2)))

        assertEquals(
            ReelleZahlen,
            prüfeStellen(quadrat, MathematischeEigenschaftRegister.Konvexitaetsbereich.id, streng = false),
        )
        assertEquals(
            ReelleZahlen,
            prüfeStellen(quadrat, MathematischeEigenschaftRegister.Konvexitaetsbereich.id, streng = true),
        )
        assertEquals(
            LeereMenge,
            prüfeStellen(quadrat, MathematischeEigenschaftRegister.Konkavitaetsbereich.id, streng = false),
        )
        assertEquals(
            LeereMenge,
            prüfeStellen(quadrat, MathematischeEigenschaftRegister.Konkavitaetsbereich.id, streng = true),
        )
    }

    @Test
    fun `affine Krümmungsbereiche unterscheiden streng und nicht streng exakt`() {
        val affin = methode(addition(multiplikation(RationaleZahl.von(3), x), RationaleZahl.von(2)))

        assertEquals(
            ReelleZahlen,
            prüfeStellen(affin, MathematischeEigenschaftRegister.Konvexitaetsbereich.id, streng = false),
        )
        assertEquals(
            ReelleZahlen,
            prüfeStellen(affin, MathematischeEigenschaftRegister.Konkavitaetsbereich.id, streng = false),
        )
        assertEquals(
            LeereMenge,
            prüfeStellen(affin, MathematischeEigenschaftRegister.Konvexitaetsbereich.id, streng = true),
        )
        assertEquals(
            LeereMenge,
            prüfeStellen(affin, MathematischeEigenschaftRegister.Konkavitaetsbereich.id, streng = true),
        )
    }

    @Test
    fun `Konvexitaetsbereich von x hoch drei bleibt echte definierte Menge`() {
        val kubisch = methode(Potenz(x, RationaleZahl.von(3)))

        val menge = assertIs<DefinierteMenge>(
            prüfeStellen(kubisch, MathematischeEigenschaftRegister.Konvexitaetsbereich.id),
        )

        assertTrue(menge.bedingung.zuLatex().contains("6"))
        assertTrue(menge.bedingung.zuLatex().contains("\\ge"))
    }

    private fun prüfeGlobal(methode: Methode, eigenschaft: String, streng: Boolean): EigenschaftsAussage {
        val knoten = MathematischeEigenschaftKnotenVorlagen.MethodenEigenschaft.erzeuge(GraphPunkt.Zero).copy(
            parameter = MathematischeEigenschaftKnotenVorlagen.MethodenEigenschaft.standardParameter + mapOf(
                EIGENSCHAFT_PARAMETER to eigenschaft,
                EIGENSCHAFT_STRENGE_PARAMETER to if (streng) "streng" else "nicht-streng",
            ),
        )
        val ergebnis = register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf("methode" to BedingterWert(methode)),
                rechenKontext = RechenKontext(),
            ),
        )
        return assertIs(ergebnis.ausgaben.getValue("aussage").objekt)
    }

    private fun prüfeStellen(
        methode: Methode,
        eigenschaft: String,
        streng: Boolean = false,
    ) = run {
        val knoten = MathematischeEigenschaftKnotenVorlagen.AnalysisEigenschaft.erzeuge(GraphPunkt.Zero).copy(
            parameter = MathematischeEigenschaftKnotenVorlagen.AnalysisEigenschaft.standardParameter + mapOf(
                EIGENSCHAFT_PARAMETER to eigenschaft,
                EIGENSCHAFT_STRENGE_PARAMETER to if (streng) "streng" else "nicht-streng",
            ),
        )
        register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf("methode" to BedingterWert(methode)),
                rechenKontext = RechenKontext(),
            ),
        ).ausgaben.getValue("stellen").objekt
    }

    private fun methode(vorschrift: de.TeutonStudio.MathematikRechenSystem.kern.ZahlAusdruck) = Methode(
        name = "f",
        parameter = listOf(x),
        vorschrift = vorschrift,
        zielMenge = ReelleZahlen,
        werteVorräte = mapOf(x.name to ReelleZahlen),
    )
}
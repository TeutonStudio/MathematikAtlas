package de.TeutonStudio.MathematikRechenSystem.kern

import de.TeutonStudio.TypSystem.TypAusdruck
import de.TeutonStudio.TypSystem.TypPrüfung
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AngulusTest {
    @Test
    fun `Angulus konvertiert Grad und Gon exakt symbolisch nach Radian`() {
        val dimensionen = listOf("x", "y")
        val grad = Angulus(RationaleZahl.von(180), AngulusEinheit.GRAD, dimensionen)
        val gon = Angulus(RationaleZahl.von(200), AngulusEinheit.GON, dimensionen)

        val gradRad = grad.inEinheit(AngulusEinheit.RADIAN)
        val gonRad = gon.inEinheit(AngulusEinheit.RADIAN)

        assertEquals(AngulusEinheit.RADIAN, gradRad.einheit)
        assertEquals(AngulusEinheit.RADIAN, gonRad.einheit)
        assertEquals(dimensionen, gradRad.dimensionen)
        assertEquals(dimensionen, gonRad.dimensionen)
        assertTrue("\\pi" in gradRad.zuLatex())
        assertTrue("\\pi" in gonRad.zuLatex())
    }

    @Test
    fun `Angulus ist eigenes mathematisches Objekt und keine Zahl`() {
        val typ = MathematischeTypen.angulusTyp(AngulusEinheit.GRAD, listOf("x", "y"))
        assertIs<TypPrüfung.Inkompatibel>(
            MathematischeTypen.typSystem.prüfe(typ, TypAusdruck.Atom(MathematischeTypen.Zahl)),
        )
        assertIs<TypPrüfung.Kompatibel>(
            MathematischeTypen.typSystem.prüfe(typ, TypAusdruck.Atom(MathematischeTypen.Objekt)),
        )
    }

    @Test
    fun `Tupel werden aus ihren Komponenten kartesisch oder polar klassifiziert`() {
        val kartesisch = Tupel(listOf(RationaleZahl.Eins, RationaleZahl.von(2), RationaleZahl.von(3)))
        val polar = Tupel(
            listOf(
                RationaleZahl.von(2),
                Angulus(RationaleZahl.von(90), AngulusEinheit.GRAD, listOf("x", "y")),
            ),
        )
        val allgemein = Tupel(listOf(RationaleZahl.Eins, BenannteMenge("M")))

        assertEquals(TupelKoordinatenArt.KARTESISCH, kartesisch.koordinatenArt())
        assertEquals(TupelKoordinatenArt.POLAR, polar.koordinatenArt())
        assertEquals(TupelKoordinatenArt.ALLGEMEIN, allgemein.koordinatenArt())
        assertEquals(MathematischeTypen.KartesischesTupel, (kartesisch.koordinatenTypAusdruck() as TypAusdruck.Parameterisiert).konstruktor)
        assertEquals(MathematischeTypen.PolarTupel, (polar.koordinatenTypAusdruck() as TypAusdruck.Parameterisiert).konstruktor)
    }

    @Test
    fun `Angulus Methode kann eine Zieleinheit erzwingen`() {
        val x = Variable("x")
        val basis = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )
        val winkelMethode: Methode = AngulusTransformierteMethode(basis, AngulusMethodenOperation.ArcSinus)
        val gradMethode = winkelMethode.erzwingeAngulusEinheit(AngulusEinheit.GRAD)

        val ergebnis = gradMethode.wendeKanonischAn(mapOf("x" to RationaleZahl.Null))
        val winkel = assertIs<Angulus>(ergebnis)
        assertEquals(AngulusEinheit.GRAD, winkel.einheit)
        assertEquals(AngulusEinheit.GRAD, assertIs<AngulusRaum>(gradMethode.methodenSignatur().zielMenge).einheit)
    }

    @Test
    fun `Polarform kann Radiusmethode mit festem Angulus heben`() {
        val x = Variable("x")
        val radius = Methode(
            name = "r",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )
        val polar = PolarKomplexMethode(
            radiusQuelle = radius,
            winkelQuelle = Angulus(RationaleZahl.von(90), AngulusEinheit.GRAD),
        )

        assertEquals(KomplexeZahlen, polar.signatur.zielMenge)
        assertIs<KomplexeZahl>(polar.wendeKanonischAn(mapOf("x" to RationaleZahl.von(2))))
    }
}

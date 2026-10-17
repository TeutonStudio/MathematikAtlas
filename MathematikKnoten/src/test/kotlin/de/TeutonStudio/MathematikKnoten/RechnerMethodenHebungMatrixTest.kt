package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class RechnerMethodenHebungMatrixTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    @Test fun `Norm einer strukturierten reellen Variablen verwendet den vorhandenen Quellraum`() {
        val quelle=de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten(art="mathematik.variable",name="Variable",parameter=mapOf("name" to "v"))
        val wert=variablenGrundraumWert(quelle,Vektorraum(VektorOrientierung.Spalte,2,ReelleZahlen))
        val k=vektorRechnerVorlage(VektorRechnerOperator.NORM).erzeuge(GraphPunkt.Zero)
        val r=register.finde(VektorRechner.KNOTEN_ART)!!.auswerten(KnotenAuswertungsKontext(k,mapOf("vektor" to wert),RechenKontext()))
        assertNull(r.fehler)
        assertIs<ZahlAusdruck>(r.ausgaben.getValue("norm").objekt)
        assertEquals(wert.variablenQuellen,r.ausgaben.getValue("norm").variablenQuellen)
    }

    @Test
    fun `Matrixaddition aus Methode und Konstante liefert Matrixmethode`() {
        val x = Variable("x")
        val matrixMenge = BenannteMenge("R11", "\\mathbb R^{1\\times 1}")
        val f = Methode(
            name = "A",
            parameter = listOf(x),
            vorschrift = Matrix(listOf(listOf(x))),
            zielMenge = matrixMenge,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )
        val konstant = Matrix(listOf(listOf(RationaleZahl.Eins)))
        val knoten = normalisiereRechnerMethodenAnschluesse(
            konfiguriereStrukturRechner(
                StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero),
                StrukturRechnerKnotenFamilie.MATRIX,
                "matrix.addition",
            ),
        )

        val ergebnis = register.finde(MatrixRechner.KNOTEN_ART)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf(
                    "a" to BedingterWert(f),
                    "b" to BedingterWert(konstant),
                ),
                rechenKontext = RechenKontext(),
            ),
        )

        val methode = assertIs<Methode>(ergebnis.ausgaben.getValue("wert").objekt)
        assertEquals(listOf("x"), methode.parameter.map { it.name })
        val matrix = assertIs<Matrix>(methode.vorschrift)
        assertEquals(1, matrix.zeilen.size)
        assertEquals(1, matrix.zeilen.single().size)
        assertTrue(matrix.zuLatex().contains("x"))
        assertTrue(matrix.zuLatex().contains("1"))
    }
    @Test fun `Hebung bindet getrennte Quellen nach IDs und behaelt Voraussetzungen`() {
        val x=Variable("x",bindungsId="quelle-f")
        val y=Variable("x",bindungsId="quelle-g")
        val raum=BenannteMenge("R11","R11")
        val f=MathematischeMethode("f",listOf(x),Matrix(listOf(listOf(x))),raum,mapOf("x" to ReelleZahlen),
            argumentIdentitäten=mapOf("x" to "basis-id"),voraussetzungen=setOf(Ungleichheit(x,RationaleZahl.Null)))
        val g=MathematischeMethode("g",listOf(y),Matrix(listOf(listOf(y))),raum,mapOf("x" to ReelleZahlen))
        val k=konfiguriereStrukturRechner(StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero),StrukturRechnerKnotenFamilie.MATRIX,"matrix.addition")
        val r=register.finde(MatrixRechner.KNOTEN_ART)!!.auswerten(KnotenAuswertungsKontext(k,mapOf("a" to BedingterWert(f),"b" to BedingterWert(g)),RechenKontext()))
        val m=assertIs<MathematischeMethode>(r.ausgaben.getValue("wert").objekt)
        assertEquals(listOf("basis-id"),m.signatur.argumente.map {it.id})
        assertEquals(Tupel(listOf(Matrix(listOf(listOf(RationaleZahl.von(4)))))),
            m.wendeMitIdentitätenAn(mapOf("basis-id" to RationaleZahl.von(2))).ergebnis)
        assertFails {m.wendeMitIdentitätenAn(mapOf("basis-id" to RationaleZahl.Null))}
        val h=Variable("h")
        assertTrue(Ungleichheit(h,RationaleZahl.Null) in m.wendeMitIdentitätenAn(mapOf("basis-id" to h)).bedingungen)
    }
    @Test fun `mehrere logische Ergebnisse werden nicht als ein Wertoperand gehoben`() {
        val matrix=Matrix(listOf(listOf(RationaleZahl.Eins)))
        val m=MathematischeMethode("multi",emptyList(),Tupel(listOf(matrix,matrix)),
            Tupelraum(listOf(BenannteMenge("M","M"),BenannteMenge("M","M"))),ausgabeNamen=listOf("a","b"))
        val k=konfiguriereStrukturRechner(StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero),StrukturRechnerKnotenFamilie.MATRIX,"matrix.addition")
        assertTrue(assertFails {register.finde(MatrixRechner.KNOTEN_ART)!!.auswerten(KnotenAuswertungsKontext(k,mapOf("a" to BedingterWert(m),"b" to BedingterWert(matrix)),RechenKontext()))}.message.orEmpty().contains("genau ein logisches"))
    }

    @Test fun `extrahierte gehobene Normalisierung behaelt ihre eigene Nichtnullbedingung`() {
        val t=Variable("t",bindungsId="qt")
        val f=MathematischeMethode("f",listOf(t),SpaltenVektor(listOf(t)),Vektorraum(VektorOrientierung.Spalte,1,ReelleZahlen),mapOf("t" to ReelleZahlen))
        val k=vektorRechnerVorlage(VektorRechnerOperator.NORMALISIERUNG).erzeuge(GraphPunkt.Zero)
        val r=register.finde(VektorRechner.KNOTEN_ART)!!.auswerten(KnotenAuswertungsKontext(k,mapOf("vektor" to BedingterWert(f)),RechenKontext()))
        val m=assertIs<MathematischeMethode>(r.ausgaben.getValue("vektor").objekt)
        assertTrue(m.voraussetzungen.isNotEmpty())
        assertFails {m.wendeMitIdentitätenAn(mapOf("qt" to RationaleZahl.Null))}
        val h=Variable("h")
        assertTrue(m.wendeMitIdentitätenAn(mapOf("qt" to h)).bedingungen.any {h in it.enthalteneVariablen()})
    }
    @Test fun `Hebung schneidet effektive und komponentenweise Definitionsraeume gemeinsam`() {
        val x=Variable("x",bindungsId="qx");val y=Variable("y",bindungsId="qy")
        val raum=BenannteMenge("R11","R11")
        val positiv=ReellesIntervall(RationaleZahl.Null,true,RationaleZahl.von(10),false)
        val f=MathematischeMethode("f",listOf(x),Matrix(listOf(listOf(x))),raum,mapOf("x" to ReelleZahlen),effektiverWerteVorrat=Tupelraum(listOf(ReelleZahlen)))
        val g=MathematischeMethode("g",listOf(y),Matrix(listOf(listOf(y))),raum,mapOf("y" to positiv))
        val k=konfiguriereStrukturRechner(StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero),StrukturRechnerKnotenFamilie.MATRIX,"matrix.addition")
        val r=register.finde(MatrixRechner.KNOTEN_ART)!!.auswerten(KnotenAuswertungsKontext(k,mapOf("a" to BedingterWert(f),"b" to BedingterWert(g)),RechenKontext()))
        val m=assertIs<MathematischeMethode>(r.ausgaben.getValue("wert").objekt)
        assertFails {m.wendeMitIdentitätenAn(mapOf("qx" to RationaleZahl.von(-1)))}
        assertEquals(Tupel(listOf(Matrix(listOf(listOf(RationaleZahl.von(4)))))),m.wendeMitIdentitätenAn(mapOf("qx" to RationaleZahl.von(2))).ergebnis)
    }

}

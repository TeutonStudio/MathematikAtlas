package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class StrukturKonsolidierungKonfigurationTest {
    @Test fun `Diagonalprojektion benennt Verbindungsverlust vorab und bleibt ein Undo-Schritt`() {
        val laufzeit=MathematikKartenLaufzeit()
        val basis=KartenDaten(name="D",knoten=listOf(MatrixdiagonaleKnotenVorlagen.Matrixdiagonale.erzeuge(GraphPunkt.Zero))).migriereStrukturKonsolidierung().knoten.single()
        val empfänger=MathematikKnotenVorlagen.TupelZuSpalte.erzeuge(GraphPunkt.Zero)
        val verbindung=VerbindungDaten(von=AnschlussVerweis(basis.id,basis.anschlüsse.last().id),zu=AnschlussVerweis(empfänger.id,empfänger.anschlüsse.first().id))
        val alt=KartenDaten(name="Projektion",knoten=listOf(basis,empfänger),verbindungen=listOf(verbindung))
        val kandidat=konfiguriereMatrixRechnerDiagonalAusgabe(basis,"zeile")
        assertEquals(basis.anschlüsse.map {it.id},kandidat.anschlüsse.map {it.id})
        val plan=planeKonfiguration(alt,kandidat,laufzeit.graphPrüfung,laufzeit::auswerten)
        assertEquals(listOf(verbindung.id),plan.auswirkung.entfallendeVerbindungen.map {it.id})
        val editor=KartenEditorZustand(alt,laufzeit.graphPrüfung)
        editor.führeAus(KartenAktion.KartenInhaltErsetzen(plan.karte))
        assertEquals("zeile",editor.karte.knoten.first().parameter[MATRIXDIAGONALE_AUSGABE])
        editor.rückgängig();assertEquals(alt,editor.karte)
        editor.wiederholen();assertEquals(plan.karte.knoten,editor.karte.knoten)
        assertEquals(basis.anschlüsse.map {it.id},konfiguriereMatrixRechnerDiagonalAusgabe(kandidat,"tupel").anschlüsse.map {it.id})
        assertTrue(hatGemeinsameAusgangsEinstellungen(kandidat))
        assertFalse(hatGemeinsameAusgangsEinstellungen(konfiguriereStrukturRechner(kandidat,StrukturRechnerKnotenFamilie.MATRIX,"matrix.diagonalmatrixErzeugen")))
    }
    @Test fun `Formeloperatoren erben keine inaktiven Diagonal oder Kreuzproduktmodi`() {
        val matrix=Matrix(listOf(listOf(RationaleZahl.Eins,RationaleZahl.von(2)),listOf(RationaleZahl.von(3),RationaleZahl.von(4))))
        val basis=KartenDaten(name="D",knoten=listOf(MatrixdiagonaleKnotenVorlagen.Matrixdiagonale.erzeuge(GraphPunkt.Zero))).migriereStrukturKonsolidierung().knoten.single()
        val zeile=konfiguriereMatrixRechnerDiagonalAusgabe(basis,"zeile")
        val op=FormelAusdruck.Operation("diag","matrix.hauptdiagonale",listOf(FormelArgument("matrix",0,FormelAusdruck.Literal("m",matrix,FormelTyp.MATRIX))),FormelTyp.TUPEL)
        val formel=konfiguriereStrukturRechnerFormel(zeile,StrukturRechnerKnotenFamilie.MATRIX,op)
        val register=GesamterMathematikAuswerter.erzeugeRegister()
        val r=register.findeNachMigration(formel.art).auswerten(KnotenAuswertungsKontext(formel,emptyMap(),RechenKontext()))
        assertNull(r.fehler);assertIs<Tupel>(r.ausgaben.getValue("wert").objekt)
        val a=SpaltenVektor(listOf(RationaleZahl.Eins,RationaleZahl.Null,RationaleZahl.Null))
        val b=SpaltenVektor(listOf(RationaleZahl.Null,RationaleZahl.Eins,RationaleZahl.Null))
        val kreuz=konfiguriereKreuzproduktRechnerOrientierung(vektorRechnerVorlage(VektorRechnerOperator.KREUZPRODUKT).erzeuge(GraphPunkt.Zero),"zeile")
        val ast=FormelAusdruck.Operation("kreuz","vektor.kreuzprodukt",listOf(FormelArgument("links",0,FormelAusdruck.Literal("a",a,FormelTyp.VEKTOR)),FormelArgument("rechts",1,FormelAusdruck.Literal("b",b,FormelTyp.VEKTOR))),FormelTyp.VEKTOR)
        val k=konfiguriereStrukturRechnerFormel(kreuz,StrukturRechnerKnotenFamilie.VEKTOR,ast)
        val aus=register.findeNachMigration(k.art).auswerten(KnotenAuswertungsKontext(k,emptyMap(),RechenKontext()))
        assertNull(aus.fehler);assertEquals(kreuzprodukt(a,b),aus.ausgaben.getValue("wert").objekt)
    }
    @Test fun `Basisvektor verwendet die Graphposition und lehnt defekte Eingaben ab`() {
        val basis=VektorOrientierungsV2300Vorlagen.EinheitsVektor.erzeuge(GraphPunkt.Zero)
        val register=GesamterMathematikAuswerter.erzeugeRegister()
        fun aus(k:KnotenDaten,e:Map<String,BedingterWert>)=register.findeNachMigration(k.art).auswerten(KnotenAuswertungsKontext(k,e,RechenKontext()))
        val graph=mapOf("dimension" to BedingterWert(RationaleZahl.von(3)),"position" to BedingterWert(RationaleZahl.Eins))
        val erwartet=SpaltenVektor(listOf(RationaleZahl.Null,RationaleZahl.Eins,RationaleZahl.Null))
        assertEquals(erwartet,aus(basis,graph).ausgaben.getValue("vektor").objekt)
        for(e in listOf(graph-"position",graph-"dimension",graph+("dimension" to BedingterWert(Variable("d"))),graph+("position" to BedingterWert(RationaleZahl.von(3)))))
            assertFails {aus(basis,e)}
        for(p in listOf("dimension" to "kaputt","index" to "kaputt",VEKTOR_ORIENTIERUNG_PARAMETER to "kaputt"))
            assertFails {aus(basis.copy(parameter=basis.parameter+p),emptyMap())}
    }
    @Test fun `gesicherte historische Definitionskarte migriert ohne Referenzverlust`() {
        val text=javaClass.getResource("/konsolidierung-m2/matrixdiagonale-v7.json")!!.readText()
        assertEquals("763ee69d0e1eb2dce3bc7d504a7eb735fc6d1e9ea5ef14923324751a9f9fe16f",java.security.MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") {"%02x".format(it)})
        val alt=KartenDatenJson.lese(text);val neu=MathematikKartenCodec.lade(text)
        val original=alt.knoten.single {it.art==MATRIXDIAGONALE_ART}
        val k=neu.knoten.single {it.id==original.id}
        assertEquals(MatrixRechner.KNOTEN_ART,k.art)
        assertEquals(original.anschlüsse.map {it.id},k.anschlüsse.map {it.id})
        assertEquals(alt.verbindungen,neu.verbindungen)
        assertEquals(neu,MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(neu)))
        val matrix=Matrix(listOf(listOf(RationaleZahl.Eins,RationaleZahl.von(2)),listOf(RationaleZahl.von(3),RationaleZahl.von(4))))
        val register=GesamterMathematikAuswerter.erzeugeRegister()
        fun wert(knoten:KnotenDaten,rolle:String)=register.findeNachMigration(knoten.art).auswerten(KnotenAuswertungsKontext(knoten,mapOf("matrix" to BedingterWert(matrix)),RechenKontext())).ausgaben.getValue(rolle)
        assertEquals(wert(original,"diagonale").objekt,wert(k,"wert").objekt)
    }
}

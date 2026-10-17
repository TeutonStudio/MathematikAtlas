package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class MatrixProduktVariadikTest {
    private val register=GesamterMathematikAuswerter.erzeugeRegister()
    private fun matrix(n: Long)=Matrix(listOf(listOf(RationaleZahl.von(n))))
    private fun knoten(anzahl: Int, kanonisch: Boolean): KnotenDaten {
        if (kanonisch) return konfiguriereStrukturRechner(
            StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero).let { it.copy(parameter=it.parameter+("festeEingänge" to anzahl.toString())) },
            StrukturRechnerKnotenFamilie.MATRIX,"matrix.produkt")
        val basis=MathematikKnotenVorlagen.MatrixProdukt.erzeuge(GraphPunkt.Zero)
        val eingänge=basis.anschlüsse.filter { it.richtung==AnschlussRichtung.Eingang }
        return basis.copy(parameter=basis.parameter+("festeEingänge" to anzahl.toString()),anschlüsse=
            List(anzahl) { i -> eingänge[minOf(i,1)].copy(id=AnschlussId("f$i"),name=if(i<2) eingänge[i].name else "input${i+1}",reihenfolge=i) }+
                basis.anschlüsse.filter { it.richtung==AnschlussRichtung.Ausgang })
    }
    private fun diagnosetest(block: () -> KnotenAuswertungsErgebnis) {
        val ergebnis=runCatching(block)
        if (ergebnis.isSuccess) {
            assertNotNull(ergebnis.getOrThrow().fehler)
            assertTrue(ergebnis.getOrThrow().ausgaben.isEmpty())
        } else assertIs<IllegalArgumentException>(ergebnis.exceptionOrNull())
    }
    private fun auswerten(k: KnotenDaten, werte: Map<String,BedingterWert>) = register.findeNachMigration(k.art).auswerten(KnotenAuswertungsKontext(k,werte,RechenKontext()))

    @Test fun `beide Pfade rechnen sechs geordnete Faktoren ohne Obergrenze`() {
        listOf(false,true).forEach { kanonisch ->
            val k=knoten(6,kanonisch)
            val werte=k.anschlüsse.filter { it.richtung==AnschlussRichtung.Eingang }.associate { it.name to BedingterWert(matrix(it.reihenfolge+1L)) }
            assertEquals(matrix(720),auswerten(k,werte).ausgaben.values.single().objekt)
        }
    }
    @Test fun `fehlende feste und innere Faktoren werden nicht zusammengeschoben`() {
        listOf(false,true).forEach { kanonisch ->
            val k=knoten(3,kanonisch)
            val ports=k.anschlüsse.filter { it.richtung==AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }
            diagnosetest { auswerten(k,mapOf(ports[0].name to BedingterWert(matrix(2)),ports[2].name to BedingterWert(matrix(4)))) }
            diagnosetest { auswerten(k,mapOf(ports[0].name to BedingterWert(matrix(2)),ports[1].name to BedingterWert(matrix(3)))) }
        }
    }
    @Test fun `nur unverbundene dynamische Schlussreserve darf fehlen`() {
        val k=knoten(2,false)
        val reserve=k.anschlüsse.first().copy(id=AnschlussId("reserve"),name="input3",reihenfolge=2,dynamischErzeugt=true)
        val neu=k.copy(anschlüsse=k.anschlüsse+reserve)
        assertEquals(matrix(6),auswerten(neu,mapOf("a" to BedingterWert(matrix(2)),"b" to BedingterWert(matrix(3)))).ausgaben.values.single().objekt)
        diagnosetest { auswerten(neu,mapOf("a" to BedingterWert(matrix(2)),"input3" to BedingterWert(matrix(4)))) }
    }
}

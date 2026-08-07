package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDatenJson
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.DifferentialAusgabeForm
import de.TeutonStudio.MathematikRechenSystem.kern.DifferentialBegriff
import de.TeutonStudio.MathematikRechenSystem.kern.DifferentialOperator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DifferentialKartenJsonTest {
    @Test fun `Methodenmodus roundtrippt Ordnung Operator Begriff und Handles`() { val k=DifferentialKnotenVorlagen.Differential.erzeuge(GraphPunkt.Zero).copy(parameter=DifferentialKnotenVorlagen.Differential.standardParameter+mapOf(DIFFERENTIAL_AUSGABEFORM_PARAMETER to DifferentialAusgabeForm.METHODE.name,DIFFERENTIAL_ORDNUNG_PARAMETER to "4",DIFFERENTIAL_OPERATOR_PARAMETER to DifferentialOperator.Partiell(1).operatorId,DIFFERENTIAL_ARGUMENT_INDEX_PARAMETER to "2",DIFFERENTIAL_BEGRIFF_PARAMETER to DifferentialBegriff.REELL_FRECHET.name)); val g=KartenJson.lese(KartenJson.schreibe(KartenDaten(name="Differential",knoten=listOf(k)))).knoten.single(); assertEquals(DIFFERENTIAL_KNOTEN_ART,g.art); assertEquals("4",g.parameter[DIFFERENTIAL_ORDNUNG_PARAMETER]); assertEquals(DifferentialOperator.Partiell(1).operatorId,g.parameter[DIFFERENTIAL_OPERATOR_PARAMETER]); assertEquals("2",g.parameter[DIFFERENTIAL_ARGUMENT_INDEX_PARAMETER]); assertEquals(k.anschlüsse.map{it.id},g.anschlüsse.map{it.id}); assertTrue(g.anschlüsse.any{it.name=="ordnung"}) }
    @Test fun `Termmodus roundtrippt Quellen ID und termbezogene Handles`() { val b=DifferentialKnotenVorlagen.Differential.erzeuge(GraphPunkt.Zero); val k=konfiguriereDifferentialKnoten(b,DifferentialAusgabeForm.TERM).copy(parameter=b.parameter+mapOf(DIFFERENTIAL_AUSGABEFORM_PARAMETER to DifferentialAusgabeForm.TERM.name,DIFFERENTIAL_QUELLEN_ID_PARAMETER to "quelle.x")); val g=KartenJson.lese(KartenJson.schreibe(KartenDaten(name="Term",knoten=listOf(k)))).knoten.single(); assertEquals(DifferentialAusgabeForm.TERM.name,g.parameter[DIFFERENTIAL_AUSGABEFORM_PARAMETER]); assertEquals("quelle.x",g.parameter[DIFFERENTIAL_QUELLEN_ID_PARAMETER]); assertEquals(setOf("term","nach"),g.anschlüsse.map{it.name}.toSet()); assertTrue(g.anschlüsse.none{it.name=="ordnung"||it.name=="methode"}) }
    @Test fun `historische Ableitung wird beim Lesen strukturiert migriert`() { val a=KnotenDaten(art="mathematik.ableitung",name="Ableitung alt",position=GraphPunkt.Zero,parameter=mapOf("ordnung" to "2")); val g=KartenJson.lese(KartenDatenJson.schreibe(KartenDaten(name="Alt",knoten=listOf(a)))).knoten.single(); assertEquals(DIFFERENTIAL_KNOTEN_ART,g.art); assertEquals(DifferentialAusgabeForm.METHODE.name,g.parameter[DIFFERENTIAL_AUSGABEFORM_PARAMETER]); assertEquals("2",g.parameter[DIFFERENTIAL_ORDNUNG_PARAMETER]); assertTrue(g.anschlüsse.any{it.name=="methode"}) }
}

package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikAtlas.speicher.KartenJson
import kotlin.test.*

class GruppenAuswertungTest {
    @Test fun `Außenwerte und Ergebnisse bleiben beim Falten Öffnen und Container Roundtrip gleich`() {
        val laufzeit=MathematikKartenLaufzeit()
        val a=MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt(20f,120f)).copy(parameter=mapOf("wert" to "3"))
        val b=MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt(20f,280f)).copy(parameter=mapOf("wert" to "2"))
        val potenz=MathematikKnotenVorlagen.Potenz.erzeuge(GraphPunkt(320f,150f))
        fun edge(k: KnotenDaten,name: String)=VerbindungDaten(
            von=AnschlussVerweis(k.id,k.anschlüsse.single().id),
            zu=AnschlussVerweis(potenz.id,potenz.anschlüsse.single{it.name==name}.id))
        val gruppe=VisuelleKnotenGruppeDaten(titel="Potenz",knotenIds=setOf(potenz.id),position=GraphPunkt(280f,80f),größe=GraphGröße(400f,300f))
        val karte=KartenDaten(name="Außenwerte",knoten=listOf(a,b,potenz),verbindungen=listOf(edge(a,"basis"),edge(b,"exponent")),visuelleGruppen=listOf(gruppe))
        val editor=KartenEditorZustand(MatlasKartenContainer.lese(MatlasKartenContainer.schreibe(karte,"gruppen-test")),laufzeit.graphPrüfung)
        val vorher=laufzeit.auswerten(editor.karte).knoten.getValue(potenz.id).ausgaben
        assertTrue(vorher.isNotEmpty())
        editor.führeAus(KartenAktion.VisuelleGruppeFaltungÄndern(gruppe.id,true))
        editor.öffneGruppe(gruppe.id)
        assertEquals(vorher,laufzeit.auswerten(editor.karte).knoten.getValue(potenz.id).ausgaben)
        val json=KartenJson.lese(KartenJson.schreibe(editor.karte))
        assertEquals(editor.karte.visuelleGruppen,json.visuelleGruppen)
        val container=MatlasKartenContainer.lese(MatlasKartenContainer.schreibe(editor.karte,"gruppen-test"))
        assertEquals(editor.karte.visuelleGruppen,container.visuelleGruppen)
        assertEquals(editor.karte.verbindungen,container.verbindungen)
        assertEquals(vorher,laufzeit.auswerten(container).knoten.getValue(potenz.id).ausgaben)
        editor.rückgängig()
        assertFalse(editor.karte.visuelleGruppen.single().eingeklappt)
        assertEquals(0L,editor.auswertungsRevision)
    }
}

package de.TeutonStudio.MathematikKnoten.visualisierung

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import kotlin.test.*

class VisualisierungsPersistenzTest {
    @Test fun neueAnsichtUeberlebtGemeinsamenCodecUndDuplizierenMitNeuenIds() {
        val c=VisualisierungsKonfiguration(dimension=RaumDimension.R3,achsenSichtbar=false,rasterSichtbar=false,
            kamera=KameraZustand(24.0,-32.0,0.0,2.0,-1.0,0.0,1.8),festeSchnitte=mapOf("t" to 2.0))
        val n=MathematikKnotenVorlagen.Visualisierung.erzeuge(GraphPunkt.Zero).copy(eigenschaften=c.inEigenschaften(mapOf("erweiterung" to KnotenEigenschaft.Text("bleibt"))))
        val geladen=MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(KartenDaten(name="Visualisierung",knoten=listOf(n))))
        assertEquals(c,VisualisierungsKonfiguration.aus(geladen.knoten.single().eigenschaften))
        assertEquals(n.id,geladen.knoten.single().id);assertEquals(n.anschlüsse,geladen.knoten.single().anschlüsse)
        val editor=KartenEditorZustand(geladen,MathematikKartenLaufzeit().graphPrüfung)
        editor.wähleKnoten(n.id);editor.dupliziereAuswahl()
        val kopie=editor.karte.knoten.single{it.id!=n.id}
        assertEquals(n.eigenschaften,kopie.eigenschaften)
        assertTrue(kopie.anschlüsse.none{neu->n.anschlüsse.any{it.id==neu.id}})
        editor.rückgängig();assertEquals(listOf(n),editor.karte.knoten)
    }
    @Test fun historischeKameraWirdBeimLadenGenauEinmalNormalisiert() {
        val c=VisualisierungsKonfiguration(kamera=KameraZustand(0.0,0.0,0.0,2.0,3.0,0.0,2.0))
        val n=MathematikKnotenVorlagen.Visualisierung.erzeuge(GraphPunkt.Zero).copy(eigenschaften=c.zuEigenschaften()-"ansichtVersion")
        val geladen=MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(KartenDaten(name="Alt",knoten=listOf(n))))
        val neu=VisualisierungsKonfiguration.aus(geladen.knoten.single().eigenschaften)
        assertEquals(7.0,neu.kamera.translationX)
        val normalisiert=geladen.copy(knoten=listOf(geladen.knoten.single().copy(eigenschaften=neu.inEigenschaften(n.eigenschaften))))
        val erneut=MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(normalisiert))
        assertEquals(neu,VisualisierungsKonfiguration.aus(erneut.knoten.single().eigenschaften))
    }
}

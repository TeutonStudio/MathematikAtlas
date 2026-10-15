package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenKartenEditor
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import org.junit.Rule
import org.junit.Test
import kotlin.test.*

class KnotenVerbindungsDragDesktopTest {
    @get:Rule val compose=createComposeRule()
    @Test fun knotenAufKanteZiehenUndBewusstHerausziehenFunktioniertBeiVerschiedenemZoom() {
        val art=AnschlussArtId("test")
        fun port(name:String,ein:Boolean)=AnschlussDaten(name=name,richtung=if(ein) AnschlussRichtung.Eingang else AnschlussRichtung.Ausgang,
            kante=if(ein) AnschlussKante.Links else AnschlussKante.Rechts,art=art)
        val a=KnotenDaten(art="test",name="A",position=GraphPunkt(10f,150f),anschlüsse=listOf(port("out",false)))
        val b=KnotenDaten(art="test",name="B",position=GraphPunkt(500f,150f),anschlüsse=listOf(port("in",true)))
        val c=KnotenDaten(art="test",name="C",position=GraphPunkt(250f,340f),anschlüsse=listOf(port("in",true),port("out",false)))
        val verbindung=VerbindungDaten(von=AnschlussVerweis(a.id,a.anschlüsse.single().id),zu=AnschlussVerweis(b.id,b.anschlüsse.single().id))
        val basis=KartenDaten(name="Drag",knoten=listOf(a,b,c),verbindungen=listOf(verbindung))
        val editor=KartenEditorZustand(basis,GraphPrüfung(AnschlussArtRegister(listOf(AnschlussArt(art,"Test")))))
        compose.setContent { MaterialTheme { CompositionLocalProvider(LocalDensity provides Density(1f,1f)) {
            KnotenKartenEditor(editor,Modifier.size(1200.dp,800.dp),zeigeKnotenInspektor=false)
        } } }
        for(zoom in listOf(0.5f,1.5f)) {
            compose.runOnIdle { editor.ersetzeKarte(basis.copy(ansicht=AnsichtsFenster(zoom=zoom))) }
            compose.waitForIdle()
            val karte=editor.darstellungsKarte
            val start=karte.knoten.single { it.id==a.id }
            val ende=karte.knoten.single { it.id==b.id }
            val gezogen=karte.knoten.single { it.id==c.id }
            val von=start.position+start.anschlussPositionRelativ(start.anschlüsse.single())
            val zu=ende.position+ende.anschlussPositionRelativ(ende.anschlüsse.single())
            val ziel=GraphPunkt((von.x+zu.x)/2f-gezogen.größe.breite/2f,(von.y+zu.y)/2f-gezogen.größe.höhe/2f)
            val delta=ziel-gezogen.position
            compose.onNodeWithContentDescription("Knoten C, 2 Anschlüsse").performTouchInput {
                down(Offset(40f,20f));moveBy(Offset(delta.x*zoom,delta.y*zoom),delayMillis=300);up()
            }
            compose.waitForIdle()
            compose.runOnIdle { assertEquals(2,editor.karte.verbindungen.size,"Einfügen bei Zoom $zoom") }
            val eingefügt=editor.karte
            if (zoom == 0.5f) {
                compose.onNodeWithText("Aus Verbindung herausziehen").performClick()
                compose.onNodeWithContentDescription("Knoten C, 2 Anschlüsse").performTouchInput {
                    down(Offset(40f,20f));moveBy(Offset(0f,160f*zoom),delayMillis=300);up()
                }
            } else {
                compose.onNodeWithContentDescription("Knoten C, 2 Anschlüsse")
                    .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
                compose.onNodeWithContentDescription("Knoten C, 2 Anschlüsse").performKeyInput { keyDown(Key.AltLeft) }
                compose.onNodeWithContentDescription("Knoten C, 2 Anschlüsse").performMouseInput {
                    moveTo(Offset(40f,20f));press();moveBy(Offset(0f,160f*zoom),delayMillis=300);release()
                }
                compose.onNodeWithContentDescription("Knoten C, 2 Anschlüsse").performKeyInput { keyUp(Key.AltLeft) }
            }
            compose.waitForIdle()
            compose.runOnIdle {
                assertEquals(1,editor.karte.verbindungen.size,"Herausziehen bei Zoom $zoom")
                assertEquals(verbindung.von,editor.karte.verbindungen.single().von)
                assertEquals(verbindung.zu,editor.karte.verbindungen.single().zu)
                editor.rückgängig();assertEquals(eingefügt,editor.karte)
                editor.rückgängig();assertEquals(listOf(verbindung),editor.karte.verbindungen)
            }
        }
    }
}

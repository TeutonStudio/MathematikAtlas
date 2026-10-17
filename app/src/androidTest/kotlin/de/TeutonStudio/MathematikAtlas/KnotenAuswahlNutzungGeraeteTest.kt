package de.TeutonStudio.MathematikAtlas

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikAtlas.knotenauswahl.*
import de.TeutonStudio.MathematikAtlas.speicher.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Echte Dialoginteraktionen; getrennt von den deterministischen Verwalter-/Speichertests. */
class KnotenAuswahlNutzungGeraeteTest {
    @get:Rule val compose=createComposeRule()
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var atlas: AtlasZustand
    @Before fun vorbereiten() {
        KnotenWählerModusSpeicher(context).speichere(KnotenWählerModus.Standard)
        compose.runOnIdle { atlas=AtlasZustand(context) }
        atlas.knotenNutzung.löschen()
        atlas.knotenNutzung.einstellungen(KnotenNutzungsEinstellungen())
        runBlocking { assertTrue(atlas.knotenNutzung.sichern().await()) }
        compose.runOnIdle { atlas.öffneKnotenAuswahl(GraphPunkt.Zero) }
        compose.setContent { MaterialTheme { atlas.knotenAuswahlPosition?.let { KnotenAuswahlDialog(atlas,it) } } }
    }
    @After fun aufräumen() {
        if(::atlas.isInitialized) {
            compose.runOnIdle { atlas.beendeBerechnungen() }
            runBlocking { atlas.knotenNutzung.sichern().await() }
        }
    }
    private fun daten(): KnotenNutzungsDaten {
        compose.waitForIdle()
        runBlocking { assertTrue(atlas.knotenNutzung.sichern().await()) }
        return KnotenNutzungsSpeicher(AndroidKnotenNutzungsDatei(context)).laden().also { assertFalse(it.geschützt) }.daten
    }
    @Test fun schnelleAuswahlZaehltSammelaktionEinmalUndUndoNichtErneut() {
        compose.onNodeWithText("Knoten suchen").performTextInput("Spaltenvektor erzeugen")
        // Der Suchtext ist zugleich der Eintragsname: das editierbare Suchfeld ausschließen.
        val eintrag=hasText("Spaltenvektor erzeugen",substring=false) and !hasSetTextAction()
        // Die Bildschirmtastatur kann die Zeile aus dem komponierten LazyColumn-Bereich verdrängen.
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(eintrag)
        compose.onNode(eintrag).performClick()
        val d=daten();val s=d.sitzungen.single()
        assertEquals(SitzungsAbschluss.AUSWAHL,s.abschluss)
        assertEquals("Spaltenvektor erzeugen",s.suchschritte.last().zustand.originaltext)
        assertEquals(s.suchschritte.last().id,s.auswahlen.single().suchschritt)
        assertEquals(2,s.auswahlen.single().erzeugt.size)
        assertEquals(1L,d.statistik.eintraege.values.single().anzahl)
        compose.runOnIdle { atlas.editor.rückgängig();atlas.editor.wiederholen() }
        assertEquals(d.statistik,daten().statistik)
    }
    @Test fun resetUndAbbrechenErhaltenAuchNochNichtBeruhigteSuche() {
        compose.onNodeWithText("Knoten suchen").performTextInput("keinTreffer-ellipsoid-xyz")
        // Die IME kann den Suchkopf im LazyColumn-Bereich verdrängen.
        compose.onNodeWithContentDescription("Suche zurücksetzen")
            .performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("", atlas.suchText) }
        compose.onNodeWithText("Schließen",substring=false).performClick()
        val d=daten();val s=d.sitzungen.single()
        assertEquals(SitzungsAbschluss.ABBRUCH,s.abschluss)
        assertTrue(s.suchschritte.any { it.zustand.originaltext=="keinTreffer-ellipsoid-xyz" && it.zustand.einfügbareTreffer==0 })
        assertEquals(SuchtextHerkunft.ZURUECKGESETZT,s.suchschritte.last().zustand.herkunft)
        assertTrue(d.statistik.eintraege.isEmpty())
    }
    @Test fun allgemeineEinfuegungZaehltAuchBeiOffenerStandardlisteNicht() {
        compose.runOnIdle {
            val vorlage=atlas.standardErgebnis().zeilen.first { it.eintrag?.vorlage!=null }.eintrag!!.vorlage!!
            atlas.fügeKnotenEin(vorlage,GraphPunkt.Zero)
        }
        val d=daten()
        assertTrue(d.statistik.eintraege.isEmpty())
        assertTrue(d.sitzungen.single().auswahlen.isEmpty())
        assertEquals(SitzungsAbschluss.ABBRUCH,d.sitzungen.single().abschluss)
    }
    @Test fun moduswechselSichertLetztenTextOhneAuswahl() {
        compose.onNodeWithText("Knoten suchen").performTextInput("noch-nicht-beruhigt-xyz")
        compose.onNodeWithText("Konzeptbibliothek",substring=false).performScrollTo().performClick()
        val d=daten();val s=d.sitzungen.single()
        assertEquals(SitzungsAbschluss.MODUSWECHSEL,s.abschluss)
        assertEquals("noch-nicht-beruhigt-xyz",s.suchschritte.last().zustand.originaltext)
        assertTrue(d.statistik.eintraege.isEmpty())
    }
    @Test fun tabwechselUndUnterbrechungErhaltenKontextUndUebernommenenText() {
        compose.runOnIdle { atlas.wähleStandardTab("Tupel") }
        compose.onNodeWithText("Knoten suchen").performTextInput("Tupel")
        compose.runOnIdle { atlas.unterbrecheKnotenNutzung() }
        val alt=daten().sitzungen.single()
        assertEquals(SitzungsAbschluss.UNTERBROCHEN,alt.abschluss)
        assertEquals("Tupel",alt.suchschritte.last().zustand.gewünschterTab)
        compose.runOnIdle { atlas.setzeKnotenNutzungFort() }
        compose.waitUntil { atlas.standardSitzung!=null }
        compose.onNodeWithText("Schließen",substring=false).performClick()
        val neu=daten().sitzungen.last()
        assertNotEquals(alt.id,neu.id)
        assertEquals(SuchtextHerkunft.UEBERNOMMEN,neu.suchschritte.first().zustand.herkunft)
        assertEquals("Tupel",neu.suchschritte.first().zustand.originaltext)
    }
}

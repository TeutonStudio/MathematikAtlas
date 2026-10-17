package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikKnoten.visualisierung.ui.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import org.junit.Assert.*
import androidx.compose.ui.semantics.SemanticsProperties
import org.junit.Rule
import org.junit.Test

class VisualisierungsBedienungTest {
    @get:Rule val compose=createComposeRule()
    private fun knoten()=MathematikKnotenVorlagen.Visualisierung.erzeuge(GraphPunkt(80f,60f)).copy(größe=GraphGröße(600f,420f))
    private fun auswertung(n:Long=2)=KnotenAuswertungsErgebnis(emptyMap(),eingänge=mapOf("menge" to BedingterWert(EndlicheMenge(setOf(RationaleZahl.von(n))))))
    private fun editor(n:KnotenDaten)=KartenEditorZustand(KartenDaten(name="Visualisierungstest",knoten=listOf(n)),MathematikKartenLaufzeit().graphPrüfung)
    @Test fun ziehenAufDerVorschauVerschiebtDenKnoten() {
        val n=knoten();val e=editor(n);val r=VisualisierungsKnotenRenderer{auswertung()}
        compose.setContent {MaterialTheme { KnotenKartenEditor(e,Modifier.size(1100.dp,800.dp),rendererFür={r},zeigeKnotenInspektor=false) }}
        compose.onNodeWithTag("visualisierung-vorschau").performTouchInput { swipe(center,center+Offset(100f,50f)) }
        compose.runOnIdle { assertTrue(e.karte.knoten.single().position.x>n.position.x);assertEquals(n.eigenschaften,e.karte.knoten.single().eigenschaften) }
    }
    @Test fun dialogGesteBleibtEntwurfUndUebernahmeIstEinUndoSchritt() {
        val n=knoten().copy(eigenschaften=knoten().eigenschaften+("fremd" to KnotenEigenschaft.Text("bewahren")));val e=editor(n)
        var offen by mutableStateOf(true)
        compose.setContent {MaterialTheme { if(offen) VisualisierungsDialog(e.karte.knoten.firstOrNull(),auswertung(),{e.führeAus(KartenAktion.KnotenEigenschaftenErsetzen(n.id,it))},{offen=false}) }}
        compose.onNodeWithTag("visualisierung-plot").performTouchInput { swipe(center,center+Offset(90f,30f)) }
        compose.runOnIdle {assertEquals(n,e.karte.knoten.single())}
        compose.onNodeWithText("Übernehmen").performClick()
        compose.runOnIdle {
            assertNotEquals(n.eigenschaften,e.karte.knoten.single().eigenschaften)
            assertEquals(KnotenEigenschaft.Text("bewahren"),e.karte.knoten.single().eigenschaften["fremd"])
            assertEquals(n.anschlüsse,e.karte.knoten.single().anschlüsse)
            e.rückgängig();assertEquals(n,e.karte.knoten.single());e.wiederholen();assertNotEquals(n.eigenschaften,e.karte.knoten.single().eigenschaften)
        }
    }
    @Test fun abbrechenVerwirftDieAnsichtNachBestaetigung() {
        val n=knoten();var anzahl=0;var offen by mutableStateOf(true)
        compose.setContent { MaterialTheme {if(offen) VisualisierungsDialog(n,auswertung(),{anzahl++},{offen=false})} }
        compose.onNodeWithText("+").performClick()
        compose.onNodeWithText("Abbrechen").performClick()
        compose.onNodeWithText("Änderungen verwerfen?").assertIsDisplayed()
        compose.onNodeWithText("Verwerfen").performClick()
        compose.runOnIdle { assertEquals(0,anzahl);assertFalse(offen) }
    }
    @Test fun externeAenderungUndLoeschenSperrenUebernahme() {
        var n by mutableStateOf<KnotenDaten?>(knoten())
        compose.setContent {MaterialTheme {VisualisierungsDialog(n,auswertung(),{},{})}}
        compose.onNodeWithText("+").performClick()
        compose.runOnIdle {n=n!!.copy(eigenschaften=n!!.eigenschaften+VisualisierungsKonfiguration().copy(rasterSichtbar=false).zuEigenschaften())}
        compose.onNodeWithText("Übernehmen").assertIsNotEnabled()
        compose.onNodeWithText("Neu laden").performClick()
        compose.onNodeWithText("Übernehmen").assertIsEnabled()
        compose.runOnIdle {n=null}
        compose.onNodeWithText("Der Knoten wurde gelöscht.").assertIsDisplayed()
        compose.onNodeWithText("Übernehmen").assertIsNotEnabled()
    }
    @Test fun ungueltigerTextBleibtBeimSchliessenDerSchmalenEinstellungenErhalten() {
        val n=knoten();var übernommen=0
        compose.setContent { MaterialTheme {CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,2f)) {VisualisierungsDialog(n,auswertung(),{übernommen++},{},modifier=Modifier.width(480.dp))} } }
        compose.onNodeWithText("Einstellungen öffnen").performClick()
        compose.onNodeWithText("X-Bereich").performScrollTo().performTextReplacement("kaputt")
        compose.onNodeWithText("Übernehmen").assertIsNotEnabled()
        compose.onNodeWithText("Einstellungen schließen").performClick()
        compose.onNodeWithText("Übernehmen").assertIsNotEnabled()
        compose.onNodeWithText("Einstellungen öffnen").performClick()
        compose.onNodeWithText("kaputt").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Abbrechen").performClick()
        compose.onNodeWithText("Änderungen verwerfen?").assertIsDisplayed()
        compose.runOnIdle {assertEquals(0,übernommen)}
    }
    @Test fun mausradAendertDenEntwurfUndNichtDenKnoten() {
        val n=knoten();var übernommen:Map<String,KnotenEigenschaft>?=null
        compose.setContent {MaterialTheme {VisualisierungsDialog(n,auswertung(),{übernommen=it},{})}}
        compose.onNodeWithTag("visualisierung-plot").performMouseInput {moveTo(center);scroll(-2f)}
        compose.onNodeWithText("Übernehmen").performClick()
        compose.runOnIdle {assertNotNull(übernommen);assertTrue(VisualisierungsKonfiguration.aus(übernommen!!).kamera.zoom>1)}
    }
    @Test fun neuerEingangWirdAuchMitVorhandenemErgebnisGezeichnet() {
        val n=knoten();var input by mutableStateOf(2L);val r=VisualisierungsKnotenRenderer{auswertung(input)}
        compose.setContent {MaterialTheme {Box(Modifier.size(650.dp,500.dp)) {r.Inhalt(n,false,object:KnotenRendererAktionen {override fun eigenschaftenErsetzen(e:Map<String,KnotenEigenschaft>) {}})}}}
        compose.waitUntil(10_000) {compose.onAllNodesWithText("Berechne Darstellung …").fetchSemanticsNodes().isEmpty()}
        compose.waitForIdle()
        fun bild():List<Float> {val p=compose.onNodeWithTag("visualisierung-vorschau").captureToImage().toPixelMap();return List(p.width*p.height){p[it%p.width,it/p.width].blue}}
        val vorher=bild()
        compose.runOnIdle {input=-5L}
        compose.waitUntil(10_000) {compose.onAllNodesWithText("Berechne Darstellung …").fetchSemanticsNodes().isEmpty()}
        compose.waitForIdle();assertNotEquals(vorher,bild())
    }
    @Test fun graphInhaltBleibtAuchBeiWeitAusserhalbLiegenderGeometrieImRechteck() {
        val x=Variable("x")
        val menge=Abbild(ReellesIntervall(RationaleZahl.von(-30),false,RationaleZahl.von(30),false),Methode(name="f",parameter=listOf(x),vorschrift=x,zielMenge=ReelleZahlen,werteVorräte=mapOf("x" to ReelleZahlen)))
        val e=KnotenAuswertungsErgebnis(emptyMap(),eingänge=mapOf("menge" to BedingterWert(menge)))
        val n=knoten().copy(eigenschaften=VisualisierungsKonfiguration(dimensionsModus=DimensionsModus.Manuell).zuEigenschaften())
        compose.setContent {MaterialTheme {Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).background(Color.Magenta).testTag("clip-root")) {
            Box(Modifier.size(300.dp).align(androidx.compose.ui.Alignment.Center)) { VisualisierungsDetailansicht(n,e,{},Modifier.fillMaxSize()) }
        }}}
        compose.waitUntil(10_000){compose.onAllNodesWithText("Berechne",substring=true).fetchSemanticsNodes().isEmpty() && compose.onAllNodesWithText("Verfeinere",substring=true).fetchSemanticsNodes().isEmpty()}
        val p=compose.onNodeWithTag("clip-root").captureToImage().toPixelMap()
        for(i in 1..9) {
            assertEquals(Color.Magenta.toArgb(),p[p.width*i/10,p.height/50].toArgb())
            assertEquals(Color.Magenta.toArgb(),p[p.width*i/10,p.height*49/50].toArgb())
            assertEquals(Color.Magenta.toArgb(),p[p.width/50,p.height*i/10].toArgb())
            assertEquals(Color.Magenta.toArgb(),p[p.width*49/50,p.height*i/10].toArgb())
        }
    }
    @Test fun alleRaeumeBleibenBeiGrosserSchriftUndNiedrigemFensterBedienbar() {
        var dimension by mutableStateOf(RaumDimension.R1);var dunkel by mutableStateOf(false)
        val n=knoten()
        compose.setContent {MaterialTheme(colorScheme=if(dunkel)darkColorScheme()else lightColorScheme()) {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,2f)) {
                val objekt:MathematischesObjekt=when(dimension){RaumDimension.R1->RationaleZahl.Eins;RaumDimension.C->KomplexeZahl(RationaleZahl.Eins,RationaleZahl.Eins);else->Tupel(List(if(dimension==RaumDimension.R3)3 else 2){RationaleZahl.Eins})}
                val e=KnotenAuswertungsErgebnis(emptyMap(),eingänge=mapOf("menge" to BedingterWert(EndlicheMenge(setOf(objekt)))))
                key(dimension,dunkel){VisualisierungsDialog(n.copy(eigenschaften=VisualisierungsKonfiguration(dimension=dimension,dimensionsModus=DimensionsModus.Manuell).zuEigenschaften()),e,{},{},modifier=Modifier.width(480.dp).height(500.dp))}
            }
        }}
        for(d in RaumDimension.entries) for(theme in listOf(false,true)) {
            compose.runOnIdle {dimension=d;dunkel=theme}
            compose.onNodeWithTag("visualisierung-plot").assertExists()
            compose.onNodeWithText("Übernehmen").assertIsDisplayed()
            compose.onNodeWithText("Abbrechen").assertIsDisplayed()
            compose.onNodeWithText("Ansicht").assertIsDisplayed().performClick()
            compose.onNodeWithText("Standardansicht").assertIsDisplayed().performClick()
        }
    }


    @Test fun flaechenWerdenOhneRasterpunkteUndMitMehrerenEbenenGezeichnet() {
        val x=Variable("x");val y=Variable("y")
        val fläche=DefinierteMenge(listOf(GebundeneMengenVariable(x,ReelleZahlen),GebundeneMengenVariable(y,ReelleZahlen)),Vergleich(y,VergleichsArt.Kleiner,RationaleZahl.Null))
        val punkt=EndlicheMenge(setOf(Tupel(listOf(RationaleZahl.von(4),RationaleZahl.von(4)))))
        var muster by mutableStateOf(FüllArt.Vollfläche)
        val e=KnotenAuswertungsErgebnis(emptyMap(),eingänge=mapOf("menge" to BedingterWert(fläche),"menge2" to BedingterWert(punkt)))
        compose.setContent {MaterialTheme {
            val c=VisualisierungsKonfiguration(achsenSichtbar=false,rasterSichtbar=false,ebenen=mapOf("menge" to EbenenStil(name="Gebiet",farbe=0xFF2563EB,füllArt=muster),"menge2" to EbenenStil(name="Punkt",farbe=0xFFDC2626)))
            VisualisierungsDetailansicht(knoten().copy(eigenschaften=c.zuEigenschaften()),e,{},Modifier.size(600.dp,400.dp))
        }}
        fun fertig() {compose.waitUntil(15_000) {compose.onAllNodesWithText("Berechne",substring=true).fetchSemanticsNodes().isEmpty() && compose.onAllNodesWithText("Verfeinere",substring=true).fetchSemanticsNodes().isEmpty()};compose.waitForIdle()}
        fertig()
        compose.onNodeWithText("● Gebiet",substring=true).assertExists()
        compose.onNodeWithText("● Punkt",substring=true).assertExists()
        val voll=compose.onNodeWithTag("visualisierung-plot").captureToImage().toPixelMap()
        val px=voll.width/3;val py=voll.height*3/4
        assertEquals(voll[px,py].toArgb(),voll[px+3,py+3].toArgb())
        val blau=voll[px,py];assertTrue(blau.blue>blau.red)
        val bilder=mutableSetOf<List<Int>>()
        for(art in FüllArt.entries) {
            compose.runOnIdle {muster=art};fertig()
            val p=compose.onNodeWithTag("visualisierung-plot").captureToImage().toPixelMap()
            val image=java.awt.image.BufferedImage(p.width,p.height,java.awt.image.BufferedImage.TYPE_INT_ARGB)
            for(j in 0 until p.height) for(i in 0 until p.width) image.setRGB(i,j,p[i,j].toArgb())
            javax.imageio.ImageIO.write(image,"png",java.io.File("/tmp/visual-muster-$art.png"))
            println("Muster $art: ${p[px,py].toArgb()}")
            bilder += (p.height*2/3 until p.height*4/5).flatMap { j -> (p.width/4 until p.width/2).map { i -> p[i,j].toArgb() } }
        }
        assertEquals(FüllArt.entries.size,bilder.size)
    }

    @Test fun mengenraeumeVorzeichenUndKlammernInBeidenThemes() {
        var dunkel by mutableStateOf(false)
        val formeln = listOf(
            "\\lbrace x\\rbrace", "\\left\\lbrace\\frac{a}{b}\\right\\rbrace",
            "\\bigl\\lbrace x\\bigr\\rbrace", "\\Set{x\\in\\mathbb{R}}",
            "A^{\\times\\limits_k n(k)}", "A^{2\\times3\\times4}",
            addition(Variable("a"),multiplikation(RationaleZahl.von(-1),addition(Variable("b"),Variable("c")))).zuLatex(),
        )
        compose.setContent { MaterialTheme(colorScheme = if(dunkel) androidx.compose.material3.darkColorScheme() else androidx.compose.material3.lightColorScheme()) {
            androidx.compose.material3.Surface { Column(Modifier.width(600.dp).testTag("mengen-vorzeichen")) {
                formeln.forEach { LatexText(it, style=androidx.compose.ui.text.TextStyle(fontSize=24.sp)) }
            } }
        } }
        for(theme in listOf(false,true)) {
            compose.runOnIdle { dunkel=theme }; compose.waitForIdle()
            val p=compose.onNodeWithTag("mengen-vorzeichen").captureToImage().toPixelMap()
            val rot=(0 until p.height).sumOf {y -> (0 until p.width).count {x -> val c=p[x,y];c.red>.6f && c.green<.35f && c.blue<.35f}}
            val bild=java.awt.image.BufferedImage(p.width,p.height,java.awt.image.BufferedImage.TYPE_INT_ARGB)
            for(y in 0 until p.height) for(x in 0 until p.width) bild.setRGB(x,y,p[x,y].toArgb())
            javax.imageio.ImageIO.write(bild,"png",java.io.File("/tmp/atlas-mengen-vorzeichen-${if(theme) "dunkel" else "hell"}.png"))
            assertEquals(0,rot)
        }
    }

    @Test fun mengenLatexEnthaeltKeineRotenFehlerglyphen() {
        compose.setContent {MaterialTheme {Box(Modifier.background(Color.White).testTag("latex-mengen")) {
            LatexText("\\mathbb R \\cup \\{x\\mid x>0\\}",style=androidx.compose.ui.text.TextStyle(color=Color.Black,fontSize=24.sp))
        }}}
        val p=compose.onNodeWithTag("latex-mengen").captureToImage().toPixelMap()
        val rot=(0 until p.height).sumOf {y -> (0 until p.width).count {x -> val c=p[x,y];c.red>.6f && c.green<.35f && c.blue<.35f}}
        assertEquals(0,rot)
    }

    @Test fun tiefenreihenfolgeMehrererFlaechenIstUnabhaengigVonDerEingangsreihenfolge() {
        var tauschen by mutableStateOf(false)
        val x=Variable("x");val y=Variable("y")
        fun fläche(z:Long)=Abbild(KartesischesProdukt(listOf(ReellesIntervall(RationaleZahl.von(-4),false,RationaleZahl.von(4),false),ReellesIntervall(RationaleZahl.von(-4),false,RationaleZahl.von(4),false))),
            Methode(name="z$z",parameter=listOf(x,y),vorschrift=RationaleZahl.von(z),zielMenge=ReelleZahlen,werteVorräte=mapOf("x" to ReelleZahlen,"y" to ReelleZahlen)))
        val vorne=fläche(3);val hinten=fläche(-3)
        compose.setContent {MaterialTheme {
            val farben=if(tauschen)listOf(0xFFDC2626L,0xFF2563EBL)else listOf(0xFF2563EBL,0xFFDC2626L)
            val c=VisualisierungsKonfiguration(dimension=RaumDimension.R3,dimensionsModus=DimensionsModus.Manuell,
                methodenModus=MethodenDarstellungsModus.Funktionsgraph,achsenSichtbar=false,rasterSichtbar=false,
                ebenen=mapOf("menge" to EbenenStil(farbe=farben[0]),"menge2" to EbenenStil(farbe=farben[1])))
            val e=KnotenAuswertungsErgebnis(emptyMap(),eingänge=mapOf("menge" to BedingterWert(if(tauschen)vorne else hinten),"menge2" to BedingterWert(if(tauschen)hinten else vorne)))
            VisualisierungsDetailansicht(knoten().copy(eigenschaften=c.zuEigenschaften()),e,{},Modifier.size(600.dp,400.dp))
        }}
        fun bild():List<Int> {
            compose.waitUntil(20_000) {compose.onAllNodesWithText("Berechne",substring=true).fetchSemanticsNodes().isEmpty() && compose.onAllNodesWithText("Verfeinere",substring=true).fetchSemanticsNodes().isEmpty()};compose.waitForIdle()
            val p=compose.onNodeWithTag("visualisierung-plot").captureToImage().toPixelMap()
            return (p.height*2/5 until p.height*3/5).flatMap {j -> (p.width*2/5 until p.width*3/5).map {i ->p[i,j].toArgb()}}
        }
        val vorher=bild();compose.runOnIdle {tauschen=true};assertEquals(vorher,bild())
    }

    @Test fun einzelneEbeneAusblendenBleibtKonsistentUndAbsturzfrei() {
        var sichtbar by mutableStateOf(true)
        val punkt=EndlicheMenge(setOf(Tupel(listOf(RationaleZahl.von(2),RationaleZahl.von(2)))))
        val e=KnotenAuswertungsErgebnis(emptyMap(),eingänge=mapOf("menge" to BedingterWert(punkt),"menge2" to BedingterWert(punkt)))
        compose.setContent {MaterialTheme {
            val c=VisualisierungsKonfiguration(ebenen=mapOf("menge" to EbenenStil(sichtbar=sichtbar,name="Erste"),"menge2" to EbenenStil(name="Zweite")))
            VisualisierungsDetailansicht(knoten().copy(eigenschaften=c.zuEigenschaften()),e,{},Modifier.size(600.dp,400.dp))
        }}
        compose.waitForIdle()
        repeat(3) {
            compose.runOnIdle {sichtbar=false};compose.waitForIdle()
            compose.onNodeWithText("● Erste",substring=true).assertDoesNotExist()
            compose.onNodeWithText("● Zweite",substring=true).assertExists()
            compose.runOnIdle {sichtbar=true};compose.waitForIdle()
            compose.onNodeWithText("● Erste",substring=true).assertExists()
        }
    }

    @Test fun identischeMengeBleibtBeimWechselDerSichtbarenEbeneGezeichnet() {
        var erste by mutableStateOf(true)
        val punkt=EndlicheMenge(setOf(Tupel(listOf(RationaleZahl.von(2),RationaleZahl.von(2)))))
        val e=KnotenAuswertungsErgebnis(emptyMap(),eingänge=mapOf("menge" to BedingterWert(punkt),"menge2" to BedingterWert(punkt)))
        compose.setContent {MaterialTheme {
            val c=VisualisierungsKonfiguration(achsenSichtbar=false,rasterSichtbar=false,
                ebenen=mapOf("menge" to EbenenStil(sichtbar=erste,farbe=0xFF2563EB),"menge2" to EbenenStil(sichtbar=!erste,farbe=0xFFDC2626)))
            VisualisierungsDetailansicht(knoten().copy(eigenschaften=c.zuEigenschaften()),e,{},Modifier.size(600.dp,400.dp))
        }}
        fun bild():Color {
            compose.waitUntil(15_000) {compose.onAllNodesWithText("Berechne",substring=true).fetchSemanticsNodes().isEmpty() && compose.onAllNodesWithText("Verfeinere",substring=true).fetchSemanticsNodes().isEmpty()};compose.waitForIdle()
            val p=compose.onNodeWithTag("visualisierung-plot").captureToImage().toPixelMap()
            val schritt=minOf(p.width,p.height)/10
            return p[p.width/2+schritt,p.height/2-schritt]
        }
        val blau=bild();assertTrue(blau.blue>blau.red)
        compose.runOnIdle {erste=false}
        val rot=bild();assertTrue(rot.red>rot.blue)
    }

    @Test fun tupelfilterFuelltNurDasEnthalteneGebiet() {
        val p=TypisiertesElement("p",MathematikAnschlussArten.Tupel.id.wert)
        val basis=KartesischesProdukt(listOf(ReelleZahlen,ReelleZahlen))
        val a=strukturKomponente(p,RationaleZahl.Eins,numerisch=true) as ZahlAusdruck
        val b=strukturKomponente(p,RationaleZahl.von(2),numerisch=true) as ZahlAusdruck
        val filter=GefilterteMenge(basis,MathematischeMethode(name="P",parameter=listOf(p),
            vorschrift=Vergleich(a,VergleichsArt.Kleiner,multiplikation(b,b)),zielMenge=WahrheitsMenge,werteVorräte=mapOf("p" to basis)))
        val c=VisualisierungsKonfiguration(achsenSichtbar=false,rasterSichtbar=false,
            dimensionsModus=DimensionsModus.Manuell,dimension=RaumDimension.R2,
            bereiche=AchsenBereiche(ZahlenBereich(-2.0,2.0),ZahlenBereich(-2.0,2.0),null),
            ebenen=mapOf("menge" to EbenenStil(farbe=0xFF2563EB)))
        compose.setContent { MaterialTheme(colorScheme=lightColorScheme()) {
            VisualisierungsDetailansicht(knoten().copy(eigenschaften=c.zuEigenschaften()),
                KnotenAuswertungsErgebnis(emptyMap(),eingänge=mapOf("menge" to BedingterWert(filter))),{},Modifier.size(600.dp,400.dp))
        }}
        compose.waitUntil(15_000) {compose.onAllNodesWithText("Berechne",substring=true).fetchSemanticsNodes().isEmpty() && compose.onAllNodesWithText("Verfeinere",substring=true).fetchSemanticsNodes().isEmpty()}
        compose.waitForIdle()
        val bild=compose.onNodeWithTag("visualisierung-plot").captureToImage().toPixelMap()
        val image=java.awt.image.BufferedImage(bild.width,bild.height,java.awt.image.BufferedImage.TYPE_INT_ARGB)
        for(j in 0 until bild.height) for(i in 0 until bild.width) image.setRGB(i,j,bild[i,j].toArgb())
        javax.imageio.ImageIO.write(image,"png",java.io.File("/tmp/filter-v3-desktop.png"))
        val t=VisualisierungsTransformation(c,bild.width.toDouble(),bild.height.toDouble())
        val innen=t.bild(-1.0,.3);val außen=t.bild(1.0,.3)
        val enthalten=bild[innen.x.toInt(),innen.y.toInt()];val ausgeschlossen=bild[außen.x.toInt(),außen.y.toInt()]
        assertTrue(enthalten.blue-enthalten.red>.1f)
        assertTrue(kotlin.math.abs(ausgeschlossen.blue-ausgeschlossen.red)<.1f)
        // Ausgeschlossene Bereiche bleiben auch zwischen den früheren Schraffurlinien leer.
        val farben=(0..8).map {bild[außen.x.toInt()+it,außen.y.toInt()+it].toArgb()}.toSet()
        assertEquals(1,farben.size)
        compose.onNodeWithText("Ansicht").performClick()
        compose.onNodeWithText("Sichtbarer Weltbereich:",substring=true).assertExists()
        compose.onNodeWithText("20 % Reserve",substring=true).assertExists()
    }

    @Test fun offeneGesteErhaeltAlleDetailsMehrererEbenen() {
        val x=Variable("x");val y=Variable("y")
        val graph=Methode(name="f",parameter=listOf(x),vorschrift=Sinus(multiplikation(RationaleZahl.von(9),x)),zielMenge=ReelleZahlen,
            werteVorräte=mapOf("x" to ReellesIntervall(RationaleZahl.von(-2),false,RationaleZahl.von(2),false))).graphMenge()
        val gebiet=DefinierteMenge(listOf(GebundeneMengenVariable(x,ReelleZahlen),GebundeneMengenVariable(y,ReelleZahlen)),
            Vergleich(y,VergleichsArt.Kleiner,multiplikation(x,x)))
        val e=KnotenAuswertungsErgebnis(emptyMap(),eingänge=mapOf("menge" to BedingterWert(graph),"menge2" to BedingterWert(gebiet)))
        val c=VisualisierungsKonfiguration(dimension=RaumDimension.R2,dimensionsModus=DimensionsModus.Manuell,
            bereiche=AchsenBereiche(ZahlenBereich(-2.0,2.0),ZahlenBereich(-2.0,2.0),null),achsenSichtbar=false,rasterSichtbar=false)
        var daten by mutableStateOf(knoten().copy(eigenschaften=c.zuEigenschaften()))
        compose.setContent {MaterialTheme {VisualisierungsDetailansicht(daten,e,{daten=daten.copy(eigenschaften=it)},Modifier.size(600.dp,400.dp))}}
        warteAufNavigationsDetail()
        val node=compose.onNodeWithTag("visualisierung-plot")
        val vorher=node.captureToImage().toPixelMap()
        node.performTouchInput {down(center)}
        compose.waitForIdle()
        val berührt=node.captureToImage().toPixelMap()
        speichereNavigationBild(vorher,"vorher")
        speichereNavigationBild(berührt,"beruehrt")
        assertTrue("Berühren darf Details nicht ändern",ausschnitt(vorher)==ausschnitt(berührt))
        node.performTouchInput {moveTo(center+Offset(12f,0f))}
        compose.waitForIdle()
        val verschoben=node.captureToImage().toPixelMap()
        var fehler=0.0;var anzahl=0
        for(j in vorher.height/4 until vorher.height*3/4) for(i in vorher.width/4 until vorher.width*3/4) {
            val a=vorher[i,j];val b=verschoben[i+12,j]
            fehler+=kotlin.math.abs(a.red-b.red)+kotlin.math.abs(a.green-b.green)+kotlin.math.abs(a.blue-b.blue);anzahl++
        }
        assertTrue("Pan muss vorhandene Detailgeometrie transformieren: ${fehler/anzahl}",fehler/anzahl<.01)
        node.performTouchInput {up()}
        warteAufNavigationsDetail()
        node.performTouchInput {down(0,center-Offset(70f,0f));down(1,center+Offset(70f,0f))}
        val vorPinch=node.captureToImage().toPixelMap()
        node.performTouchInput {moveTo(0,center-Offset(95f,0f));moveTo(1,center+Offset(95f,0f))}
        compose.waitForIdle()
        val gezoomt=node.captureToImage().toPixelMap()
        assertTrue("Pinch muss die Kamera ändern",ausschnitt(vorPinch)!=ausschnitt(gezoomt))
        node.performTouchInput {up(0);up(1)}
        warteAufNavigationsDetail()
        val vorMausrad=VisualisierungsKonfiguration.aus(daten.eigenschaften).kamera
        node.performMouseInput {moveTo(center);scroll(-1f)}
        warteAufNavigationsDetail()
        assertTrue(VisualisierungsKonfiguration.aus(daten.eigenschaften).kamera.zoom>vorMausrad.zoom)
        val vorTastatur=VisualisierungsKonfiguration.aus(daten.eigenschaften).kamera
        node.performKeyInput {pressKey(androidx.compose.ui.input.key.Key.DirectionRight);pressKey(androidx.compose.ui.input.key.Key.Plus)}
        warteAufNavigationsDetail()
        assertTrue(VisualisierungsKonfiguration.aus(daten.eigenschaften).kamera.zoom>vorTastatur.zoom)
        assertTrue(VisualisierungsKonfiguration.aus(daten.eigenschaften).kamera.translationX!=vorTastatur.translationX)
        compose.onNodeWithText("+").performClick()
        warteAufNavigationsDetail()
        assertTrue(ausschnitt(node.captureToImage().toPixelMap()).distinct().size>10)
    }

    @Test fun beruehrenEiner3DFlaecheAendertWederGeometrieNochKanten() {
        val x=Variable("x");val y=Variable("y")
        val intervall=ReellesIntervall(RationaleZahl.von(-2),false,RationaleZahl.von(2),false)
        val fläche=Methode(name="f",parameter=listOf(x,y),vorschrift=multiplikation(x,y),zielMenge=ReelleZahlen,
            werteVorräte=mapOf("x" to intervall,"y" to intervall)).graphMenge()
        val e=KnotenAuswertungsErgebnis(emptyMap(),eingänge=mapOf("menge" to BedingterWert(fläche)))
        val c=VisualisierungsKonfiguration(dimension=RaumDimension.R3,dimensionsModus=DimensionsModus.Manuell,
            bereiche=AchsenBereiche(ZahlenBereich(-3.0,3.0),ZahlenBereich(-3.0,3.0),ZahlenBereich(-3.0,3.0)),
            kamera=KameraZustand(25.0,-35.0,0.0,0.0,0.0,0.0,1.0),achsenSichtbar=false,rasterSichtbar=false)
        var daten by mutableStateOf(knoten().copy(eigenschaften=c.zuEigenschaften()))
        compose.setContent {MaterialTheme {VisualisierungsDetailansicht(daten,e,{daten=daten.copy(eigenschaften=it)},Modifier.size(600.dp,400.dp))}}
        warteAufNavigationsDetail()
        val node=compose.onNodeWithTag("visualisierung-plot")
        val vorher=ausschnitt(node.captureToImage().toPixelMap())
        assertTrue(vorher.distinct().size>10)
        node.performTouchInput {down(center)}
        compose.waitForIdle()
        assertTrue("Berühren darf weder 3D-Geometrie noch Kanten ändern",vorher==ausschnitt(node.captureToImage().toPixelMap()))
        node.performTouchInput {moveTo(center+Offset(20f,10f));up()}
        warteAufNavigationsDetail()
        assertTrue("Die Fläche muss nach Rotation anders erscheinen",vorher!=ausschnitt(node.captureToImage().toPixelMap()))
    }

    private fun warteAufNavigationsDetail() {
        compose.waitUntil(20_000) {compose.onAllNodesWithText("Berechne",substring=true).fetchSemanticsNodes().isEmpty() && compose.onAllNodesWithText("Verfeinere",substring=true).fetchSemanticsNodes().isEmpty()}
        compose.waitUntil(20_000) {
            compose.onNodeWithTag("visualisierung-plot").fetchSemanticsNode().config.getOrElse(SemanticsProperties.StateDescription) { "" } == "Darstellung fertig"
        }
        compose.waitForIdle()
        compose.onNodeWithTag("visualisierung-plot").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"Darstellung fertig"))
    }
    private fun ausschnitt(p:androidx.compose.ui.graphics.PixelMap):List<Int> =
        (p.height/4 until p.height*3/4).flatMap {j -> (p.width/4 until p.width*3/4).map {i -> p[i,j].toArgb()}}
    private fun speichereNavigationBild(p:androidx.compose.ui.graphics.PixelMap,name:String) {
        val image=java.awt.image.BufferedImage(p.width,p.height,java.awt.image.BufferedImage.TYPE_INT_ARGB)
        for(j in 0 until p.height) for(i in 0 until p.width) image.setRGB(i,j,p[i,j].toArgb())
        javax.imageio.ImageIO.write(image,"png",java.io.File("/tmp/navigation-$name.png"))
    }
}

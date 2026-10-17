package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.InspektorSichtbarkeit
import de.TeutonStudio.MathematikKnoten.*
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/** Produktiver Android-Inspector; ausgeführte Geräteabnahme getrennt dokumentieren. */
class InspektorBereicheGeraeteTest {
    @get:Rule val compose = createComposeRule()

    @Test fun uebersetzerKonfiguriertAusgangUndBehaeltKlappzustand() {
        val basis = ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero)
        lateinit var atlas: AtlasZustand
        compose.setContent {
            val context = LocalContext.current
            atlas = remember { AtlasZustand(IsolierterInspektorTestContext(context)).also {
                it.editor.ersetzeKarte(KartenDaten(name = "Inspectortest", knoten = listOf(basis)))
                it.editor.wähleKnoten(basis.id)
                it.aktualisiereAuswertung()
                InspektorSichtbarkeit.öffnen()
            } }
            MaterialTheme { Row(Modifier.fillMaxSize()) {
                Spacer(Modifier.weight(1f))
                Inspektor(atlas, Modifier)
            } }
        }
        compose.onNodeWithText("Übersetzungsziel").assertDoesNotExist()
        compose.onNodeWithText("Standarddaten").performClick()
        compose.onNodeWithText("Ausgangsdaten").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Gemeinsame Einstellungen").performScrollTo().performClick()
        compose.onNodeWithText("Polarkoordinaten").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("polar", übersetzerZiel(atlas.editor.karte.knoten.single()))
            assertEquals(basis.anschlüsse.map { it.id }, atlas.editor.karte.knoten.single().anschlüsse.map { it.id })
            atlas.editor.rückgängig(); atlas.aktualisiereAuswertung()
            assertEquals(übersetzerZiel(basis), übersetzerZiel(atlas.editor.karte.knoten.single()))
        }
        compose.onNodeWithText("Ausgangsdaten").performScrollTo().performClick()
        compose.onNodeWithText("Übersetzungsziel").assertDoesNotExist()
        compose.runOnIdle { atlas.beendeBerechnungen() }
    }

    @Test fun diagonalprojektionWechseltImGemeinsamenAusgangsbereichMitUndo() {
        val basis=KartenDaten(name="D",knoten=listOf(MatrixdiagonaleKnotenVorlagen.Matrixdiagonale.erzeuge(GraphPunkt.Zero)))
            .migriereStrukturKonsolidierung().knoten.single()
        lateinit var atlas: AtlasZustand
        compose.setContent {
            val context=LocalContext.current
            atlas=remember { AtlasZustand(IsolierterInspektorTestContext(context)).also {
                it.editor.ersetzeKarte(KartenDaten(name="Diagonalprojektion",knoten=listOf(basis)))
                it.editor.wähleKnoten(basis.id);it.aktualisiereAuswertung();InspektorSichtbarkeit.öffnen()
            } }
            MaterialTheme { Row(Modifier.fillMaxSize()) { Spacer(Modifier.weight(1f));Inspektor(atlas,Modifier) } }
        }
        compose.onNodeWithText("Standarddaten").performClick()
        compose.onNodeWithText("Ausgangsdaten").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Gemeinsame Einstellungen").performScrollTo().performClick()
        compose.onNodeWithText("Zeilenvektor").performScrollTo().performClick()
        compose.runOnIdle {
            val neu=atlas.editor.karte.knoten.single()
            assertEquals("zeile",neu.parameter[MATRIXDIAGONALE_AUSGABE])
            assertEquals(basis.anschlüsse.map {it.id},neu.anschlüsse.map {it.id})
            atlas.editor.rückgängig();atlas.aktualisiereAuswertung()
            assertEquals(basis,atlas.editor.karte.knoten.single());atlas.beendeBerechnungen()
        }
    }

    @Test fun abflachtiefeWechseltImUebersetzerMitEigenemUndoSchritt() {
        val basis=ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero)
        lateinit var atlas:AtlasZustand
        compose.setContent {
            val context=LocalContext.current
            atlas=remember {AtlasZustand(IsolierterInspektorTestContext(context)).also {
                it.editor.ersetzeKarte(KartenDaten(name="Abflachen",knoten=listOf(basis)))
                it.editor.wähleKnoten(basis.id);it.aktualisiereAuswertung();InspektorSichtbarkeit.öffnen()
            }}
            MaterialTheme {Row(Modifier.fillMaxSize()) {Spacer(Modifier.weight(1f));Inspektor(atlas,Modifier)}}
        }
        compose.onNodeWithText("Standarddaten").performClick()
        compose.onNodeWithText("Gemeinsame Einstellungen").performScrollTo().performClick()
        compose.onNodeWithText("Tupel abflachen").performScrollTo().performClick()
        compose.runOnIdle {assertEquals("abflachen",übersetzerZiel(atlas.editor.karte.knoten.single()))}
        compose.onNodeWithText("Eine Ebene: Tupel und Vektoren").performScrollTo().performClick()
        compose.runOnIdle {
            val k=atlas.editor.karte.knoten.single()
            assertEquals("1",k.parameter["abflachen.ebenen"])
            assertEquals(basis.anschlüsse.map {it.id},k.anschlüsse.map {it.id})
            atlas.editor.rückgängig();atlas.aktualisiereAuswertung()
            assertEquals("abflachen",übersetzerZiel(atlas.editor.karte.knoten.single()))
            assertNull(atlas.editor.karte.knoten.single().parameter["abflachen.ebenen"])
            atlas.editor.rückgängig();atlas.aktualisiereAuswertung()
            assertEquals(basis,atlas.editor.karte.knoten.single());atlas.beendeBerechnungen()
        }
    }

    @Test fun mengeneingabeUndElementanzahlWechselnJeweilsMitEigenemUndoSchritt() {
        val basis=MathematikKnotenVorlagen.EndlicheMenge.erzeuge(GraphPunkt.Zero)
        lateinit var atlas:AtlasZustand
        compose.setContent {
            val context=LocalContext.current
            atlas=remember {AtlasZustand(IsolierterInspektorTestContext(context)).also {
                it.editor.ersetzeKarte(KartenDaten(name="Graphmenge",knoten=listOf(basis)))
                it.editor.wähleKnoten(basis.id);it.aktualisiereAuswertung();InspektorSichtbarkeit.öffnen()
            }}
            MaterialTheme {Row(Modifier.fillMaxSize()) {Spacer(Modifier.weight(1f));Inspektor(atlas,Modifier)}}
        }
        compose.onNodeWithText("Standarddaten").performClick()
        compose.onNodeWithText("Gemeinsame Einstellungen").performScrollTo().performClick()
        compose.onNodeWithText("Graph-Elemente").performScrollTo().performClick()
        compose.runOnIdle {assertEquals(ENDLICHE_MENGE_GRAPH,endlicheMengeEingabe(atlas.editor.karte.knoten.single()))}
        compose.onNodeWithText("Graph-Elemente (0 bis 64)").performScrollTo().performTextReplacement("2")
        compose.onNodeWithText("Elementanzahl übernehmen").performScrollTo().performClick()
        compose.runOnIdle {
            val k=atlas.editor.karte.knoten.single()
            assertEquals(2,endlicheMengeGraphAnzahl(k))
            assertEquals(basis.anschlüsse.single().id,k.anschlüsse.single {it.name=="menge"}.id)
            assertEquals(basis.parameter[ENDLICHE_MENGE_KONFIGURATION_PARAMETER],k.parameter[ENDLICHE_MENGE_KONFIGURATION_PARAMETER])
            atlas.editor.rückgängig();atlas.aktualisiereAuswertung()
            assertEquals(1,endlicheMengeGraphAnzahl(atlas.editor.karte.knoten.single()))
            atlas.editor.rückgängig();atlas.aktualisiereAuswertung()
            assertEquals(basis,atlas.editor.karte.knoten.single());atlas.beendeBerechnungen()
        }
    }

    @Test fun allgemeinerParameterBereichUndDimensionSindValidiertUndEinzelnRueckgaengig() {
        val basis=MathematikKnotenVorlagen.AllgemeinerParameter.erzeuge(GraphPunkt.Zero)
        lateinit var atlas:AtlasZustand
        compose.setContent {
            val context=LocalContext.current
            atlas=remember {AtlasZustand(IsolierterInspektorTestContext(context)).also {
                it.editor.ersetzeKarte(KartenDaten(name="Parameter",knoten=listOf(basis)))
                it.editor.wähleKnoten(basis.id);it.aktualisiereAuswertung();InspektorSichtbarkeit.öffnen()
            }}
            MaterialTheme {Row(Modifier.fillMaxSize()) {Spacer(Modifier.weight(1f));Inspektor(atlas,Modifier)}}
        }
        compose.onNodeWithText("Standarddaten").performClick()
        compose.onNodeWithText("Matrix").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(WertebereichKonfiguration.Matrix(),WertebereichKonfiguration.vonEigenschaft(atlas.editor.karte.knoten.single().eigenschaften[WertebereichKonfiguration.EIGENSCHAFT]))
            assertEquals(basis.anschlüsse.map {it.id},atlas.editor.karte.knoten.single().anschlüsse.map {it.id})
        }
        compose.onNodeWithText("Zeilen").performScrollTo().performTextReplacement("0")
        compose.runOnIdle {assertEquals(2,(WertebereichKonfiguration.vonEigenschaft(atlas.editor.karte.knoten.single().eigenschaften[WertebereichKonfiguration.EIGENSCHAFT]) as WertebereichKonfiguration.Matrix).zeilen)}
        compose.onNodeWithText("Zeilen").performTextReplacement("3")
        compose.runOnIdle {
            assertEquals(3,(WertebereichKonfiguration.vonEigenschaft(atlas.editor.karte.knoten.single().eigenschaften[WertebereichKonfiguration.EIGENSCHAFT]) as WertebereichKonfiguration.Matrix).zeilen)
            atlas.editor.rückgängig();atlas.aktualisiereAuswertung()
            assertEquals(2,(WertebereichKonfiguration.vonEigenschaft(atlas.editor.karte.knoten.single().eigenschaften[WertebereichKonfiguration.EIGENSCHAFT]) as WertebereichKonfiguration.Matrix).zeilen)
            atlas.editor.rückgängig();atlas.aktualisiereAuswertung()
            assertEquals(basis,atlas.editor.karte.knoten.single());atlas.beendeBerechnungen()
        }
    }

    @Test fun fehlerhafterParameterBereichBleibtBisZurExplizitenAuswahlErhalten() {
        val basis=MathematikKnotenVorlagen.AllgemeinerParameter.erzeuge(GraphPunkt.Zero).let {it.copy(
            eigenschaften=it.eigenschaften+(WertebereichKonfiguration.EIGENSCHAFT to KnotenEigenschaft.Text("defekt")))}
        lateinit var atlas:AtlasZustand
        compose.setContent {
            val context=LocalContext.current
            atlas=remember {AtlasZustand(IsolierterInspektorTestContext(context)).also {
                it.editor.ersetzeKarte(KartenDaten(name="Defekter Parameter",knoten=listOf(basis)))
                it.editor.wähleKnoten(basis.id);it.aktualisiereAuswertung();InspektorSichtbarkeit.öffnen()
            }}
            MaterialTheme {Row(Modifier.fillMaxSize()) {Spacer(Modifier.weight(1f));Inspektor(atlas,Modifier)}}
        }
        compose.onNodeWithText("Standarddaten").performClick()
        compose.onNodeWithText("Wähle den gewünschten Bereich ausdrücklich neu. Die gespeicherte Konfiguration bleibt bis dahin erhalten.").performScrollTo().assertIsDisplayed()
        compose.runOnIdle {assertEquals(basis,atlas.editor.karte.knoten.single())}
        compose.onNodeWithText("Tupel").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(WertebereichKonfiguration.Tupel(),WertebereichKonfiguration.vonEigenschaft(atlas.editor.karte.knoten.single().eigenschaften[WertebereichKonfiguration.EIGENSCHAFT]))
            atlas.editor.rückgängig();atlas.aktualisiereAuswertung()
            assertEquals(basis,atlas.editor.karte.knoten.single());atlas.beendeBerechnungen()
        }
    }

    @Test fun relationswechselAendertParameterUndStandardnamenInEinemUndoSchritt() {
        val basis = MathematikKnotenVorlagen.Kleiner.erzeuge(GraphPunkt.Zero)
        lateinit var atlas: AtlasZustand
        compose.setContent {
            val context = LocalContext.current
            atlas = remember { AtlasZustand(IsolierterInspektorTestContext(context)).also {
                it.editor.ersetzeKarte(KartenDaten(name = "Relationswechsel", knoten = listOf(basis)))
                it.editor.wähleKnoten(basis.id); it.aktualisiereAuswertung(); InspektorSichtbarkeit.öffnen()
            } }
            MaterialTheme { Row(Modifier.fillMaxSize()) {
                Spacer(Modifier.weight(1f)); Inspektor(atlas, Modifier)
            } }
        }
        compose.onNodeWithText("Standarddaten").performClick()
        compose.onNodeWithText("≥").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("größerGleich", atlas.editor.karte.knoten.single().parameter["relation"])
            assertEquals("Größer oder gleich", atlas.editor.karte.knoten.single().name)
            atlas.editor.rückgängig(); atlas.aktualisiereAuswertung()
            assertEquals(basis, atlas.editor.karte.knoten.single())
            atlas.beendeBerechnungen()
        }
    }
}

private class IsolierterInspektorTestContext(context: android.content.Context) : android.content.ContextWrapper(context) {
    private val basis = java.io.File(context.cacheDir, "inspektortest-${java.util.UUID.randomUUID()}").apply { mkdirs() }
    override fun getFilesDir() = java.io.File(basis, "files").apply { mkdirs() }
    override fun getNoBackupFilesDir() = java.io.File(basis, "no-backup").apply { mkdirs() }
}

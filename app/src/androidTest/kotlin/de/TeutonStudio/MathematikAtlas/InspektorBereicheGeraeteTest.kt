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

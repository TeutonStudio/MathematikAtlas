package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import org.junit.Test
import org.junit.Rule
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*

@OptIn(ExperimentalTestApi::class)
class UrbildInspektorGeraeteTest {
    @get:Rule val compose = createComposeRule()
    @Test fun urbildEingabeformIstNurInGemeinsamenEingangsdatenBedienbarUndAtomarRueckgaengig() = with(compose) {
        val k = konfiguriereMengenRechner(MengenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero),MengenRechnerOperator.URBILD)
        val laufzeit = MathematikKartenLaufzeit()
        val editor = KartenEditorZustand(KartenDaten(name="Urbild",knoten=listOf(k)),laufzeit.graphPrüfung)
        val aktuell = mutableStateOf(k)
        val dunkel = mutableStateOf(false)
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,1.35f)) {
                MaterialTheme(colorScheme=if(dunkel.value) darkColorScheme() else lightColorScheme()) {
                    Surface { Column(Modifier.width(310.dp).verticalScroll(rememberScrollState())) {
                        KnotenInspektorBereiche(editor.karte,aktuell.value,remember { InspektorKlappZustand() },
                            standarddaten={ Text("Urbild") },knotendaten={ Text("Operator Urbild") },
                            eingangsEinstellungen={ InspektorRechnerEingangsEinstellungen(aktuell.value) { kandidat ->
                                val plan=planeKonfiguration(editor.karte,kandidat,laufzeit.graphPrüfung,laufzeit::auswerten)
                                editor.führeAus(KartenAktion.KartenInhaltErsetzen(plan.karte))
                                aktuell.value=editor.karte.knoten.single()
                            } }) { Text("Anschluss ${it.name}") }
                    } }
                }
            }
        }
        onNodeWithText("Zielmenge").assertDoesNotExist()
        onNodeWithText("Gemeinsame Einstellungen").performScrollTo().performClick()
        onNodeWithText("Einzelwert").assertIsSelected()
        onNodeWithText("Zielmenge").performScrollTo().performClick()
        onNodeWithText("Zielmenge").assertIsSelected()
        runOnIdle {
            assertEquals(URBILD_ZIELMENGE,urbildEingabeform(aktuell.value))
            assertEquals(k.anschlüsse.map {it.id},aktuell.value.anschlüsse.map {it.id})
            editor.rückgängig(); aktuell.value=editor.karte.knoten.single()
        }
        onNodeWithText("Einzelwert").assertIsSelected()
        runOnIdle { editor.wiederholen(); aktuell.value=editor.karte.knoten.single(); dunkel.value=true }
        onNodeWithText("Zielmenge").assertIsSelected()
        val bild=onRoot().captureToImage().asAndroidBitmap()
        File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"urbild-inspektor.png")
            .outputStream().use { bild.compress(Bitmap.CompressFormat.PNG,100,it) }
        onNodeWithText("Einzelwert").performScrollTo().performClick()
        onNodeWithText("Einzelwert").assertIsSelected()
        Unit
    }
}

package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.awt.ComposeWindow
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.*
import java.awt.GraphicsEnvironment
import java.util.concurrent.ConcurrentHashMap
import javax.swing.SwingUtilities
import org.junit.Assume.assumeFalse
import org.junit.Test
import kotlin.test.assertTrue

/** Echte Compose-Fenster: prüft Überlauf und Scrollbesitz statt Abstandskonstanten. */
class AtlasDialogLayoutTest {
    @Test
    fun langeTexteUndScrollendeListeLassenAktionenErreichbar() {
        assumeFalse("Benötigt eine grafische Desktop-Sitzung", GraphicsEnvironment.isHeadless())
        for ((breite, höhe) in listOf(390 to 800, 800 to 390, 1100 to 760)) {
        for (schrift in listOf(1f, 2f)) for (dunkel in listOf(false, true)) {
            val bounds = ConcurrentHashMap<String, Rect>()
            val nachUnten = mutableStateOf(false)
            lateinit var window: ComposeWindow
            SwingUtilities.invokeAndWait {
                window = ComposeWindow().apply {
                    setSize(breite, höhe)
                    setLocationRelativeTo(null)
                    setContent {
                        MaterialTheme(colorScheme = if (dunkel) darkColorScheme() else lightColorScheme()) {
                            val density = LocalDensity.current
                            CompositionLocalProvider(LocalDensity provides Density(density.density, schrift)) {
                                AtlasDialog(onDismissRequest = {}, modifier = Modifier.width(breite.dp).height(höhe.dp)) {
                                    Column(Modifier.fillMaxSize().onGloballyPositioned { bounds["dialog"] = it.boundsInWindow() }) {
                                        AtlasDialogKopf(
                                            titel = { Text("Ein sehr langer Dialogtitel mit mehreren Wörtern", Modifier.onGloballyPositioned { bounds["titel"] = it.boundsInWindow() }) },
                                            untertitel = { Text("Eine ausführliche Beschreibung, die auch bei großer Schrift vollständig erreichbar bleibt.") },
                                        )
                                        val liste = rememberLazyListState()
                                        LaunchedEffect(nachUnten.value) { if (nachUnten.value) liste.scrollToItem(79) }
                                        LazyColumn(Modifier.weight(1f).fillMaxWidth().onGloballyPositioned { bounds["liste"] = it.boundsInWindow() }, state = liste) {
                                            items(80) { index ->
                                                AtlasDialogEintrag {
                                                    Text("Eintrag $index", Modifier.onGloballyPositioned { if (index == 79) bounds["letzter"] = it.boundsInWindow() })
                                                }
                                            }
                                        }
                                        AtlasDialogAktionen {
                                            TextButton(onClick = {}, modifier = Modifier.onGloballyPositioned { bounds["abbrechen"] = it.boundsInWindow() }) { Text("Abbrechen") }
                                            Button(onClick = {}, modifier = Modifier.onGloballyPositioned { bounds["bestätigen"] = it.boundsInWindow() }) { Text("Auswahl übernehmen") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    isVisible = true
                }
            }
            try {
                warte { listOf("dialog", "titel", "liste", "abbrechen", "bestätigen").all(bounds::containsKey) }
                Thread.sleep(250) // Native Dialogfenster nach initialer Positionierung stabilisieren.
                val dialog = bounds.getValue("dialog")
                val liste = bounds.getValue("liste")
                val aktion = bounds.getValue("bestätigen")
                assertTrue(liste.height > 0, "Inhalt braucht sichtbaren Restplatz")
                assertTrue(aktion.top >= liste.bottom - 1, "Aktionen dürfen die Liste nicht überlagern")
                assertTrue(aktion.bottom <= dialog.bottom + 1, "Bestätigung muss innerhalb des Dialogs liegen")
                assertTrue(bounds.getValue("titel").right <= dialog.right + 1, "Titel darf horizontal nicht überlaufen")
                SwingUtilities.invokeAndWait { nachUnten.value = true }
                warte("$breite × $höhe / $schrift / $dunkel", bounds) { bounds["letzter"]?.let { it.top >= bounds.getValue("liste").top - 1 && it.bottom <= bounds.getValue("liste").bottom + 1 } == true }
                assertTrue(kotlin.math.abs((bounds.getValue("bestätigen").top - bounds.getValue("dialog").top) - (aktion.top - dialog.top)) <= 1f, "Scrollen darf den Footer nicht verschieben: $bounds")
            } finally {
                SwingUtilities.invokeAndWait { window.dispose() }
            }
        }
    }

    }

    @Test
    fun aussenklickRespektiertPropertyUndInnenklickBleibtOffen() {
        assumeFalse("Benötigt eine grafische Desktop-Sitzung", GraphicsEnvironment.isHeadless())
        for (außenSchließt in listOf(false, true)) {
            val geschlossen = java.util.concurrent.atomic.AtomicInteger()
            val bounds = ConcurrentHashMap<String, Rect>()
            lateinit var host: ComposeWindow
            SwingUtilities.invokeAndWait {
                host = ComposeWindow().apply {
                    setSize(800, 600)
                    setContent {
                        MaterialTheme {
                            AtlasDialog(
                                onDismissRequest = { geschlossen.incrementAndGet() },
                                größe = AtlasDialogGröße.Kompakt,
                                properties = androidx.compose.ui.window.DialogProperties(
                                    usePlatformDefaultWidth = false, dismissOnClickOutside = außenSchließt,
                                ),
                            ) {
                                Box(Modifier.fillMaxWidth().height(180.dp).onGloballyPositioned { bounds["innen"] = it.boundsInWindow() }) {
                                    Text("Innenfläche")
                                }
                            }
                        }
                    }
                    isVisible = true
                }
            }
            try {
                warte { bounds.containsKey("innen") }
                Thread.sleep(250)
                fun klicke(x: Int, y: Int) {
                    SwingUtilities.invokeAndWait {
                        val popup = java.awt.Window.getWindows().firstOrNull { it.isShowing && it !== host } ?: host
                        val pane = (popup as javax.swing.RootPaneContainer).contentPane
                        val target = SwingUtilities.getDeepestComponentAt(pane, x, y)
                        val point = SwingUtilities.convertPoint(pane, x, y, target)
                        val now = System.currentTimeMillis()
                        target.dispatchEvent(java.awt.event.MouseEvent(target, java.awt.event.MouseEvent.MOUSE_PRESSED,
                            now, java.awt.event.InputEvent.BUTTON1_DOWN_MASK, point.x, point.y, 1, false, java.awt.event.MouseEvent.BUTTON1))
                        target.dispatchEvent(java.awt.event.MouseEvent(target, java.awt.event.MouseEvent.MOUSE_RELEASED,
                            now + 1, 0, point.x, point.y, 1, false, java.awt.event.MouseEvent.BUTTON1))
                    }
                }
                val innen = bounds.getValue("innen")
                klicke(innen.center.x.toInt(), innen.center.y.toInt())
                Thread.sleep(150)
                assertTrue(geschlossen.get() == 0, "Innenklick darf den Dialog nicht schließen")
                klicke(4, 4)
                if (außenSchließt) warte { geschlossen.get() == 1 }
                else {
                    Thread.sleep(150)
                    assertTrue(geschlossen.get() == 0, "Außenklick muss deaktivierbar bleiben")
                }
            } finally {
                SwingUtilities.invokeAndWait { host.dispose() }
            }
        }
    }

    private fun warte(fall: String = "Start", bounds: Map<String, Rect> = emptyMap(), bedingung: () -> Boolean) {
        val ende = System.nanoTime() + 10_000_000_000L
        while (!bedingung() && System.nanoTime() < ende) Thread.sleep(30)
        assertTrue(bedingung(), "Compose-Layout wurde nicht rechtzeitig sichtbar: $fall / $bounds")
    }
}

package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.ui.input.key.*
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.logik.*

class AtlasTastaturAusführer(
    private val befehle: AtlasBefehlsAusführer,
    private val kontext: () -> BefehlsKontext,
) {
    fun verarbeite(event: KeyEvent): Boolean {
        val schritt = if (event.isShiftPressed) 10f else 1f
        val delta = when (event.key) {
            Key.DirectionLeft -> GraphPunkt(-schritt, 0f)
            Key.DirectionRight -> GraphPunkt(schritt, 0f)
            Key.DirectionUp -> GraphPunkt(0f, -schritt)
            Key.DirectionDown -> GraphPunkt(0f, schritt)
            else -> null
        }
        if (delta != null && !event.isAltPressed && !event.isCtrlPressed && !event.isMetaPressed) {
            return when (event.type) {
                KeyEventType.KeyDown -> befehle.verschiebeAuswahlWiederholbar(delta, kontext())
                KeyEventType.KeyUp -> { befehle.beendeWiederholbareAktion(); true }
                else -> false
            }
        }
        if (event.type != KeyEventType.KeyDown) return false
        val befehl = event.alsAtlasBefehl() ?: return false
        return befehle.führeAus(befehl, kontext())
    }
}

private fun KeyEvent.alsAtlasBefehl(): AtlasBefehl? {
    val primär = isCtrlPressed || isMetaPressed
    return when {
        primär && key == Key.S -> AtlasBefehl.Speichern
        primär && key == Key.Z && isShiftPressed -> AtlasBefehl.Wiederholen
        primär && key == Key.Y -> AtlasBefehl.Wiederholen
        primär && key == Key.Z -> AtlasBefehl.Rückgängig
        primär && key == Key.A -> AtlasBefehl.AllesAuswählen
        primär && key == Key.C -> AtlasBefehl.AuswahlKopieren
        primär && key == Key.X -> AtlasBefehl.AuswahlAusschneiden
        primär && key == Key.V -> AtlasBefehl.AuswahlEinfügen()
        primär && key == Key.D -> AtlasBefehl.AuswahlDuplizieren
        primär && key == Key.G && isShiftPressed -> AtlasBefehl.GruppierungAufheben
        primär && key == Key.G -> AtlasBefehl.AuswahlGruppieren
        primär && key == Key.F -> AtlasBefehl.SucheÖffnen
        key == Key.Delete -> AtlasBefehl.AuswahlLöschen
        key == Key.Escape -> AtlasBefehl.InteraktionAbbrechen
        key == Key.N -> AtlasBefehl.KnotenAuswahlÖffnen
        key == Key.MoveHome -> AtlasBefehl.InhaltEinpassen
        key == Key.F -> AtlasBefehl.AuswahlZentrieren
        key == Key.Equals || key == Key.NumPadAdd -> AtlasBefehl.ZoomÄndern(1.1f)
        key == Key.Minus || key == Key.NumPadSubtract -> AtlasBefehl.ZoomÄndern(1f / 1.1f)
        key == Key.Zero || key == Key.NumPad0 -> AtlasBefehl.ZoomSetzen(1f)
        key == Key.F2 -> AtlasBefehl.Umbenennen
        else -> null
    }
}

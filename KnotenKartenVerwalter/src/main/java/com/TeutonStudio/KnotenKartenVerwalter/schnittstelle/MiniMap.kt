package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import com.TeutonStudio.KnotenKartenVerwalter.daten.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KarteZustand

/**
 * Ziel-Datei fuer die Minimap.
 *
 * Die aktuelle Minimap-Logik liegt noch in `Uebersicht.kt`. Diese Datei markiert den vorgesehenen
 * neuen Ort, damit die Hauptkarte, die Minimap und die Kontrollleiste klar getrennt werden koennen.
 */
@Composable
internal fun KartenMiniMap(
    daten: KarteDaten,
    zustand: KarteZustand,
    flaeche: IntSize,
    onAnsichtAendern: (KarteZustand) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Delegation an die bestehende Uebersicht bleibt auskommentiert, bis die alte Datei umbenannt
    // oder inhaltlich bereinigt ist. Die Parameter beschreiben bereits die spaetere Schnittstelle.
    @Suppress("UNUSED_VARIABLE")
    val geplanteSchnittstelle = listOf(daten, zustand, flaeche, onAnsichtAendern, modifier)
}

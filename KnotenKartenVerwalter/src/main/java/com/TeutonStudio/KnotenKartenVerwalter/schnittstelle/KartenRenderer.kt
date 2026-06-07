package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import com.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KnotenDaten

/**
 * Renderer-Registry fuer spaetere benutzerdefinierte Knoten- und Verbindungstypen.
 *
 * React Flow erlaubt Renderer pro `type`. Diese Datei bereitet dieselbe Struktur fuer Compose vor,
 * ohne die bestehende Default-Darstellung schon umzubauen.
 */
typealias KnotenRenderer = @Composable (
    knoten: KnotenDaten,
    modifierKnoten: Modifier,
    modifierAnschluss: (AnschlussRichtung, Int) -> Modifier,
) -> Unit

/** Zeichnet eine Verbindung innerhalb eines Canvas-Layers. */
typealias VerbindungRenderer = DrawScope.(
    verbindung: VerbindungDaten,
    geometrie: VerbindungGeometrie,
) -> Unit

/**
 * Sammlung aller registrierten Renderer.
 *
 * Fehlt ein Typ in einer Map, soll die spaetere Implementierung auf den Default-Renderer
 * zurueckfallen.
 */
data class KartenRenderer(
    /** Renderer fuer `KnotenDaten.typ`. */
    val knotenRenderer: Map<String, KnotenRenderer> = emptyMap(),

    /** Renderer fuer `VerbindungDaten.typ`. */
    val verbindungRenderer: Map<String, VerbindungRenderer> = emptyMap(),
)

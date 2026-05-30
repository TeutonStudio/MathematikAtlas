package com.TeutonStudio.KnotenKartenVerwalter.daten

import androidx.compose.ui.geometry.Offset
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenArten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.VerbindungArten

/**
 * Persistierter Viewport einer Karte.
 *
 * `x` und `y` beschreiben die Verschiebung des Weltkoordinatensystems in
 * Bildschirmkoordinaten. `zoom` beschreibt den Skalierungsfaktor zwischen Welt-
 * und Bildschirmkoordinaten.
 */
open class AnsichtsfensterDaten(
    val verschiebung: Offset = Offset.Zero,
    val zoom: Float = 1f,
)

/**
 * Laufzeit-Zustand der Kartenansicht.
 *
 * Anders als [AnsichtsfensterDaten] enthält dieser Zustand zusätzlich UI-Flags
 * für Hilfselemente. Die Verschiebung liegt in Bildschirmkoordinaten, während
 * Knotenpositionen weiterhin in Weltkoordinaten gespeichert werden.
 */
open class KarteZustand(
    val verschiebung: Offset = Offset.Zero,
    val zoom: Float = 1f,
    val zeigeÜbersicht: Boolean = false,
    val zeigeKontrollLeiste: Boolean = false,
) {
    constructor(
        zustand: KarteZustand,
        verschiebung: Offset? = null,
        zoom: Float? = null,
        zeigeÜbersicht: Boolean? = null,
        zeigeKontrollLeiste: Boolean? = null,
    ): this(
        verschiebung ?: zustand.verschiebung,
        zoom ?: zustand.zoom,
        zeigeÜbersicht ?: zustand.zeigeÜbersicht,
        zeigeKontrollLeiste ?: zustand.zeigeKontrollLeiste,
    )

    fun copy(
        verschiebung: Offset = this.verschiebung,
        zoom: Float = this.zoom,
        zeigeÜbersicht: Boolean = this.zeigeÜbersicht,
        zeigeKontrollLeiste: Boolean = this.zeigeKontrollLeiste,
    ): KarteZustand = KarteZustand(verschiebung, zoom, zeigeÜbersicht, zeigeKontrollLeiste)
}

/**
 * Vollständiger fachlicher Zustand einer Knotenkarte.
 *
 * Die Klasse ist bewusst immutable gehalten: Änderungen an Knoten, Verbindungen
 * oder dem Viewport werden durch `copy(...)` erzeugt und vom aufrufenden Code
 * kontrolliert.
 */
open class KarteDaten(
    override val id: String,
    val name: String,
    val größe: Offset? = null, // TODO größe des Graphs
    val knoten: List<KnotenDaten> = emptyList(),
    val verbindungen: List<VerbindungDaten> = emptyList(),
    val initialKnoten: List<KnotenDaten> = emptyList(),
    val initialVerbindungen: List<VerbindungDaten> = emptyList(),
    val artenKnoten: List<KnotenArten> = emptyList(),
    val artenVerbindungen: List<VerbindungArten> = emptyList(),
    val ansichtsfenster: AnsichtsfensterDaten = AnsichtsfensterDaten(),
): GraphDaten {
    constructor(
        daten: KarteDaten,
        id: String? = null,
        name: String? = null,
        größe: Offset? = null,
        knoten: List<KnotenDaten>? = null,
        verbindungen: List<VerbindungDaten>? = null,
        initialKnoten: List<KnotenDaten>? = null,
        initialVerbindungen: List<VerbindungDaten>? = null,
        artenKnoten: List<KnotenArten>? = null,
        artenVerbindungen: List<VerbindungArten>? = null,
        ansichtsfenster: AnsichtsfensterDaten? = null,
    ): this(
        id ?: daten.id,
        name ?: daten.name,
        größe ?: daten.größe,
        knoten ?: daten.knoten,
        verbindungen ?: daten.verbindungen,
        initialKnoten ?: daten.initialKnoten,
        initialVerbindungen ?: daten.initialVerbindungen,
        artenKnoten ?: daten.artenKnoten,
        artenVerbindungen ?: daten.artenVerbindungen,
        ansichtsfenster ?: daten.ansichtsfenster,
    )

    fun copy(
        id: String = this.id,
        name: String = this.name,
        größe: Offset? = this.größe,
        knoten: List<KnotenDaten> = this.knoten,
        verbindungen: List<VerbindungDaten> = this.verbindungen,
        initialKnoten: List<KnotenDaten> = this.initialKnoten,
        initialVerbindungen: List<VerbindungDaten> = this.initialVerbindungen,
        artenKnoten: List<KnotenArten> = this.artenKnoten,
        artenVerbindungen: List<VerbindungArten> = this.artenVerbindungen,
        ansichtsfenster: AnsichtsfensterDaten = this.ansichtsfenster,
    ): KarteDaten = KarteDaten(
        id = id,
        name = name,
        größe = größe,
        knoten = knoten,
        verbindungen = verbindungen,
        initialKnoten = initialKnoten,
        initialVerbindungen = initialVerbindungen,
        artenKnoten = artenKnoten,
        artenVerbindungen = artenVerbindungen,
        ansichtsfenster = ansichtsfenster,
    )
}

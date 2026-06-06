package com.TeutonStudio.KnotenKartenVerwalter.daten.fix

import androidx.compose.ui.geometry.Offset
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KartenCacheDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.VerbindungArten

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
    val auswahl: AuswahlDaten = AuswahlDaten(),
) {
    constructor(
        zustand: KarteZustand,
        verschiebung: Offset? = null,
        zoom: Float? = null,
        zeigeÜbersicht: Boolean? = null,
        zeigeKontrollLeiste: Boolean? = null,
        auswahl: AuswahlDaten? = null,
    ): this(
        verschiebung ?: zustand.verschiebung,
        zoom ?: zustand.zoom,
        zeigeÜbersicht ?: zustand.zeigeÜbersicht,
        zeigeKontrollLeiste ?: zustand.zeigeKontrollLeiste,
        auswahl ?: zustand.auswahl,
    )

    fun copy(
        verschiebung: Offset = this.verschiebung,
        zoom: Float = this.zoom,
        zeigeÜbersicht: Boolean = this.zeigeÜbersicht,
        zeigeKontrollLeiste: Boolean = this.zeigeKontrollLeiste,
        auswahl: AuswahlDaten = this.auswahl,
    ): KarteZustand = KarteZustand(verschiebung, zoom, zeigeÜbersicht, zeigeKontrollLeiste, auswahl)
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
    override val klasse: String?,
    public val name: String,
    public val größe: Offset? = null, // TODO größe des Graphs
    public val knoten: List<KnotenDaten> = emptyList(),
    public val verbindungen: List<VerbindungDaten> = emptyList(),
    public val initialKnoten: List<KnotenDaten> = emptyList(),
    public val initialVerbindungen: List<VerbindungDaten> = emptyList(),
//    public val artenKnoten: List<KnotenArten> = emptyList(),
    public val artenVerbindungen: List<VerbindungArten> = emptyList(),
    public val ansichtsfenster: AnsichtsfensterDaten = AnsichtsfensterDaten(),
    public val cache: KartenCacheDaten = KartenCacheDaten(),
): GraphDaten {
    constructor(
        daten: KarteDaten,
        id: String? = null,
        klasse: String? = null,
        name: String? = null,
        größe: Offset? = null,
        knoten: List<KnotenDaten>? = null,
        verbindungen: List<VerbindungDaten>? = null,
        initialKnoten: List<KnotenDaten>? = null,
        initialVerbindungen: List<VerbindungDaten>? = null,
//        artenKnoten: List<KnotenArten>? = null,
        artenVerbindungen: List<VerbindungArten>? = null,
        ansichtsfenster: AnsichtsfensterDaten? = null,
        cache: KartenCacheDaten? = null,
    ): this(
        id ?: daten.id,
        klasse ?: daten.klasse,
        name ?: daten.name,
        größe ?: daten.größe,
        knoten ?: daten.knoten,
        verbindungen ?: daten.verbindungen,
        initialKnoten ?: daten.initialKnoten,
        initialVerbindungen ?: daten.initialVerbindungen,
//        artenKnoten ?: daten.artenKnoten,
        artenVerbindungen ?: daten.artenVerbindungen,
        ansichtsfenster ?: daten.ansichtsfenster,
        cache ?: daten.cache,
    )

    fun copy(
        id: String = this.id,
        klasse: String? = this.klasse,
        name: String = this.name,
        größe: Offset? = this.größe,
        knoten: List<KnotenDaten> = this.knoten,
        verbindungen: List<VerbindungDaten> = this.verbindungen,
        initialKnoten: List<KnotenDaten> = this.initialKnoten,
        initialVerbindungen: List<VerbindungDaten> = this.initialVerbindungen,
//        artenKnoten: List<KnotenArten> = this.artenKnoten,
        artenVerbindungen: List<VerbindungArten> = this.artenVerbindungen,
        ansichtsfenster: AnsichtsfensterDaten = this.ansichtsfenster,
        cache: KartenCacheDaten = this.cache,
    ): KarteDaten = KarteDaten(
        id = id,
        klasse = klasse,
        name = name,
        größe = größe,
        knoten = knoten,
        verbindungen = verbindungen,
        initialKnoten = initialKnoten,
        initialVerbindungen = initialVerbindungen,
//        artenKnoten = artenKnoten,
        artenVerbindungen = artenVerbindungen,
        ansichtsfenster = ansichtsfenster,
        cache = cache,
    )
}
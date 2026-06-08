package com.TeutonStudio.KnotenKartenVerwalter.daten.fix

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import com.TeutonStudio.KnotenKartenVerwalter.AnsichtsfensterDaten
import com.TeutonStudio.KnotenKartenVerwalter.KartenArt
import com.TeutonStudio.KnotenKartenVerwalter.Rechteck
import com.TeutonStudio.KnotenKartenVerwalter.StandardAnsicht
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisKarte

/**
 * Persistierter Viewport einer Karte.
 *
 * `x` und `y` beschreiben die Verschiebung des Weltkoordinatensystems in
 * Bildschirmkoordinaten. `zoom` beschreibt den Skalierungsfaktor zwischen Welt-
 * und Bildschirmkoordinaten.
 */
/*open class AnsichtsfensterDaten(
    val verschiebung: Offset = Offset.Zero,
    val zoom: Float = 1f,
)*/

/**
 * Laufzeit-Zustand der Kartenansicht.
 *
 * Anders als [AnsichtsfensterDaten] enthält dieser Zustand zusätzlich UI-Flags
 * für Hilfselemente. Die Verschiebung liegt in Bildschirmkoordinaten, während
 * Knotenpositionen weiterhin in Weltkoordinaten gespeichert werden.
 */
open class KarteZustand(
    ansicht: AnsichtsfensterDaten = StandardAnsicht(),
    val zeigeÜbersicht: Boolean = false,
    val zeigeKontrollLeiste: Boolean = false,
    val auswahl: AuswahlDaten = AuswahlDaten(),
) {
    var ansicht by mutableStateOf(ansicht)

    constructor(
        zustand: KarteZustand,
        ansicht: AnsichtsfensterDaten,
        zeigeÜbersicht: Boolean? = null,
        zeigeKontrollLeiste: Boolean? = null,
        auswahl: AuswahlDaten? = null,
    ): this(
        ansicht ?: zustand.ansicht,
        zeigeÜbersicht ?: zustand.zeigeÜbersicht,
        zeigeKontrollLeiste ?: zustand.zeigeKontrollLeiste,
        auswahl ?: zustand.auswahl,
    )

/*    fun copy(
        ansicht: AnsichtsfensterDaten = this.ansicht,
        zeigeÜbersicht: Boolean = this.zeigeÜbersicht,
        zeigeKontrollLeiste: Boolean = this.zeigeKontrollLeiste,
        auswahl: AuswahlDaten = this.auswahl,
    ): KarteZustand = KarteZustand(ansicht, zeigeÜbersicht, zeigeKontrollLeiste, auswahl)*/
}

open class KartenCacheDaten() {

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
    override val klasse: KartenArt? = BasisKarte.KARTEN_ART,
    public val name: String,
    public val größe: Rechteck? = null, // TODO größe des Graphs
    public val knoten: List<KnotenDaten> = emptyList(), // TODO evtl mutable
    public val verbindungen: List<VerbindungDaten> = emptyList(), // TODO evtl mutable
    public val initialKnoten: List<KnotenDaten> = emptyList(),
    public val initialVerbindungen: List<VerbindungDaten> = emptyList(),
//    public val artenKnoten: List<KnotenArten> = emptyList(),
//    public val artenVerbindungen: List<VerbindungArten> = emptyList(),
    public val ansicht: AnsichtsfensterDaten = StandardAnsicht(),
    public val cache: KartenCacheDaten = KartenCacheDaten(),
): GraphDaten {
    constructor(
        daten: KarteDaten,
        id: String? = null,
        klasse: String? = null,
        name: String? = null,
        größe: Rechteck? = null,
        knoten: List<KnotenDaten>? = null,
        verbindungen: List<VerbindungDaten>? = null,
        initialKnoten: List<KnotenDaten>? = null,
        initialVerbindungen: List<VerbindungDaten>? = null,
//        artenKnoten: List<KnotenArten>? = null,
//        artenVerbindungen: List<VerbindungArten>? = null,
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
//        artenVerbindungen ?: daten.artenVerbindungen,
        ansichtsfenster ?: daten.ansicht,
        cache ?: daten.cache,
    )

    fun copy(
        id: String = this.id,
        klasse: String? = this.klasse,
        name: String = this.name,
        größe: Rechteck? = this.größe,
        knoten: List<KnotenDaten> = this.knoten,
        verbindungen: List<VerbindungDaten> = this.verbindungen,
        initialKnoten: List<KnotenDaten> = this.initialKnoten,
        initialVerbindungen: List<VerbindungDaten> = this.initialVerbindungen,
//        artenKnoten: List<KnotenArten> = this.artenKnoten,
//        artenVerbindungen: List<VerbindungArten> = this.artenVerbindungen,
        ansichtsfenster: AnsichtsfensterDaten = this.ansicht,
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
//        artenVerbindungen = artenVerbindungen,
        ansicht = ansichtsfenster,
        cache = cache,
    )
}
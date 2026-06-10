package com.TeutonStudio.KnotenKartenVerwalter.daten.fix

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import com.TeutonStudio.KnotenKartenVerwalter.AnsichtsfensterDaten
import com.TeutonStudio.KnotenKartenVerwalter.KartenArt
import com.TeutonStudio.KnotenKartenVerwalter.Rechteck
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
//    ansicht: AnsichtsfensterDaten = StandardAnsicht(),
    val zeigeÜbersicht: Boolean = false,
    val zeigeKontrollLeiste: Boolean = false,
    var auswahl: AuswahlDaten = AuswahlDaten(),
) {
    var zoom by mutableFloatStateOf(1f)
    var pos by mutableStateOf(Offset.Zero)
//    var ansicht by mutableStateOf(ansicht)

    constructor(
        zustand: KarteZustand,
//        ansicht: AnsichtsfensterDaten,
        zeigeÜbersicht: Boolean? = null,
        zeigeKontrollLeiste: Boolean? = null,
        auswahl: AuswahlDaten? = null,
    ): this(
//        ansicht ?: zustand.ansicht,
        zeigeÜbersicht ?: zustand.zeigeÜbersicht,
        zeigeKontrollLeiste ?: zustand.zeigeKontrollLeiste,
        auswahl ?: zustand.auswahl,
    )
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
    public val name: String,
    public val größe: Rechteck? = null, // TODO größe des Graphs
    public val initialKnoten: List<KnotenDaten> = emptyList(),
    public val initialVerbindungen: List<VerbindungDaten> = emptyList(),
): GraphDaten {
    override val klasse: KartenArt? = BasisKarte.KARTEN_ART

    public val cache: KartenCacheDaten = KartenCacheDaten()
    public val knoten: MutableList<KnotenDaten> = initialKnoten.toMutableList()
    public val verbindungen: MutableList<VerbindungDaten> = initialVerbindungen.toMutableList()
//    public var ansicht: AnsichtsfensterDaten = StandardAnsicht()

    constructor(
        daten: KarteDaten,
        id: String? = null,
        name: String? = null,
        größe: Rechteck? = null,
        initialKnoten: List<KnotenDaten>? = null,
        initialVerbindungen: List<VerbindungDaten>? = null,
        ansichtsfenster: AnsichtsfensterDaten? = null,
        cache: KartenCacheDaten? = null,
    ): this(
        id ?: daten.id,
        name ?: daten.name,
        größe ?: daten.größe,
        initialKnoten ?: daten.initialKnoten,
        initialVerbindungen ?: daten.initialVerbindungen,
    ) {
//        this.ansicht = ansichtsfenster ?: daten.ansicht
//        this.cache = cache ?: daten.cache // TODO

    }

}
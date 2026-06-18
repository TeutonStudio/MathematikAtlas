package de.TeutonStudio.KnotenKartenVerwalter.daten.knoten

import androidx.compose.runtime.toMutableStateList
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.RichtungsAnschlussDaten

/** Datenvertrag für Knoten, deren Anschlüsse aus Kanten- und Richtungsangaben erzeugt werden. */
interface KnotenRichtung<D : RichtungsAnschlussDaten>: AnschlüsseDaten<D> {
    val anschlussIdx: MutableMap<String, Int>

    val anschlussLabel: MutableMap<AnschlussKante, Pair<String, Int>>
    override val anschlüsse get() = erhalteAnschlüsse()

    /** Erzeugt einen Anschlussdatensatz für die angegebene Kante und Beschriftung. */
    public fun erzeugeAnschluss(id: String, kante: AnschlussKante, label: String,): D
    /** Passt einen erzeugten Anschlussdatensatz nachträglich an. */
    public fun anschlussKorrektur(a: D) {}

    /** Erzeugt die stabile Anschluss-ID innerhalb eines Knotens. */
    public fun erzeugeAnschlussId(knotenId: String, idx: Int): String

    private fun erhalteAnschlüsse() = anschlussLabel.map { (kante,labelIdx) ->
        erzeugeAnschlussId(id, labelIdx.second).let {
            anschlussIdx[it] = labelIdx.second
            erzeugeAnschluss(
                id = it,
                kante = kante,
                label = labelIdx.first,
            ).apply { anschlussKorrektur(this) }
        }
    }.toMutableStateList()

}

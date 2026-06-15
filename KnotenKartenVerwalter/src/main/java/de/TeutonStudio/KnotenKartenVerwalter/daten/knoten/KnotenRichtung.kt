package de.TeutonStudio.KnotenKartenVerwalter.daten.knoten

import androidx.compose.runtime.toMutableStateList
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.RichtungsAnschlussDaten

interface KnotenRichtung<D : RichtungsAnschlussDaten>: AnschlüsseDaten<D> {
    val anschlussIdx: MutableMap<String, Int>

    val anschlussLabel: MutableMap<AnschlussKante, Pair<String, Int>>
    override val anschlüsse get() = erhalteAnschlüsse()

    public fun erzeugeAnschluss(
        id: String,
        kante: AnschlussKante,
        label: String,
    ): D
    public fun anschlussKorrektur(a: D) {}

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

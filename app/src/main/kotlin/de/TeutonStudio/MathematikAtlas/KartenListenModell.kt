package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.MathematikAtlas.speicher.KartenOrdnung
import de.TeutonStudio.MathematikAtlas.speicher.formatiereOrdnerPfad

internal sealed interface KartenListenEintrag {
    val tiefe: Int
    val schlüssel: String

    data class Ordner(
        val pfad: List<String>,
        override val tiefe: Int,
    ) : KartenListenEintrag {
        override val schlüssel: String = "ordner:${formatiereOrdnerPfad(pfad)}"
    }

    data class Karte(
        val karte: KartenDaten,
        override val tiefe: Int,
    ) : KartenListenEintrag {
        override val schlüssel: String = "karte:${karte.id.wert}"
    }
}

internal fun kartenListenEinträge(
    karten: List<KartenDaten>,
    ordnung: KartenOrdnung,
    eingeklappteOrdner: Set<List<String>> = emptySet(),
): List<KartenListenEintrag> = buildList {
    fun fügeEbeneHinzu(eltern: List<String>, tiefe: Int) {
        ordnung.ordner.asSequence()
            .filter { it.size == eltern.size + 1 && it.take(eltern.size) == eltern }
            .sortedBy { it.last().lowercase() }
            .forEach { pfad ->
                add(KartenListenEintrag.Ordner(pfad, tiefe))
                if (pfad !in eingeklappteOrdner) {
                    fügeEbeneHinzu(pfad, tiefe + 1)
                }
            }

        karten.asSequence()
            .filter { ordnung.ordnerFür(it.id) == eltern }
            .sortedBy { it.name.lowercase() }
            .forEach { add(KartenListenEintrag.Karte(it, tiefe)) }
    }

    fügeEbeneHinzu(emptyList(), 0)
}

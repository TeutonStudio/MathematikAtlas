package com.TeutonStudio.KnotenKartenVerwalter.daten

import androidx.compose.ui.geometry.Offset
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KnotenDaten

/**
 * Schnittstelle fuer zukuenftige automatische Layout-Algorithmen.
 *
 * Das Modul bindet bewusst keine konkrete Layout-Bibliothek fest ein. Eine App kann spaeter einen
 * eigenen Algorithmus bereitstellen und nur die neuen Knotenpositionen zurueckgeben.
 */
fun interface KartenLayoutAlgorithmus {
    /**
     * Berechnet eine neue Karte aus einer vorhandenen Karte.
     *
     * Implementierungen sollten IDs, Verbindungen und fachliche Daten erhalten und nur die Position
     * der Knoten aendern, sofern keine andere Aenderung ausdruecklich gewuenscht ist.
     */
    fun berechneLayout(karte: KarteDaten): KarteDaten
}

data class StandardKartenLayout(
    val start: Offset = Offset(80f, 80f),
    val spaltenAbstand: Float = 280f,
    val zeilenAbstand: Float = 160f,
) : KartenLayoutAlgorithmus {
    override fun berechneLayout(karte: KarteDaten): KarteDaten {
        if (karte.knoten.isEmpty()) return karte

        val knotenIds = karte.knoten.mapTo(mutableSetOf()) { it.id }
        val originalIndex = karte.knoten.mapIndexed { index, knoten -> knoten.id to index }.toMap()
        val eingehend = knotenIds.associateWith { 0 }.toMutableMap()
        val ausgehend = knotenIds.associateWith { mutableListOf<String>() }

        karte.verbindungen.forEach { verbindung ->
            if (verbindung.quellKnotenId in knotenIds && verbindung.zielKnotenId in knotenIds) {
                ausgehend.getValue(verbindung.quellKnotenId).add(verbindung.zielKnotenId)
                eingehend[verbindung.zielKnotenId] = eingehend.getValue(verbindung.zielKnotenId) + 1
            }
        }

        val tiefe = knotenIds.associateWith { 0 }.toMutableMap()
        val warteschlange = ArrayDeque(
            karte.knoten
                .filter { eingehend.getValue(it.id) == 0 }
                .sortedBy { originalIndex.getValue(it.id) }
                .map { it.id },
        )

        val besucht = mutableSetOf<String>()
        while (warteschlange.isNotEmpty()) {
            val knotenId = warteschlange.removeFirst()
            if (!besucht.add(knotenId)) continue

            ausgehend.getValue(knotenId).forEach { zielId ->
                tiefe[zielId] = maxOf(tiefe.getValue(zielId), tiefe.getValue(knotenId) + 1)
                eingehend[zielId] = eingehend.getValue(zielId) - 1
                if (eingehend.getValue(zielId) == 0) {
                    warteschlange.addLast(zielId)
                }
            }
        }

        val nichtBesuchte = karte.knoten
            .filterNot { it.id in besucht }
            .sortedBy { originalIndex.getValue(it.id) }
        val startTiefeFuerRest = tiefe.values.maxOrNull().orEmptyZero() + 1
        nichtBesuchte.forEachIndexed { index, knoten ->
            tiefe[knoten.id] = startTiefeFuerRest + index
        }

        val zeilenProSpalte = mutableMapOf<Int, Int>()
        val neueKnoten = karte.knoten
            .sortedWith(compareBy<KnotenDaten> { tiefe.getValue(it.id) }.thenBy { originalIndex.getValue(it.id) })
            .map { knoten ->
                val spalte = tiefe.getValue(knoten.id)
                val zeile = zeilenProSpalte.getOrDefault(spalte, 0)
                zeilenProSpalte[spalte] = zeile + 1
                knoten.copy(
                    position = Offset(
                        x = start.x + spalte * spaltenAbstand,
                        y = start.y + zeile * zeilenAbstand,
                    ),
                )
            }
            .associateBy { it.id }

        return karte.copy(knoten = karte.knoten.map { neueKnoten.getValue(it.id) })
    }
}

private fun Int?.orEmptyZero(): Int = this ?: 0

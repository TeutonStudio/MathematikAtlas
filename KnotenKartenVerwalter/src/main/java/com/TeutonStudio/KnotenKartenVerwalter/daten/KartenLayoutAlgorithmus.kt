package com.TeutonStudio.KnotenKartenVerwalter.daten

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

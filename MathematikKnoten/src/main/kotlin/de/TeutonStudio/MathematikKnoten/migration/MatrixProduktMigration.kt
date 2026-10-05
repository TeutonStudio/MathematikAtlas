package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten

/**
 * Aktualisiert persistierte Matrixprodukt-Knoten auf den gemeinsamen Faktorvertrag.
 *
 * Vor der Unterstützung orientierter Vektoren waren beide Eingänge fest als Matrix
 * gespeichert. Die Migration übernimmt den heutigen Eingangsvertrag, bewahrt aber
 * die vorhandenen Anschluss-IDs und damit alle Verbindungen.
 */
internal fun KartenDaten.migriereMatrixProduktAnschlüsse(): KartenDaten {
    if (knoten.none { it.art == "mathematik.matrixProdukt" }) return this

    val standardEingänge = MathematikKnotenVorlagen.MatrixProdukt
        .erzeuge(GraphPunkt.Zero)
        .anschlüsse
        .filter { it.richtung == AnschlussRichtung.Eingang }
        .associateBy { it.name }
    var verändert = false
    val neueKnoten = knoten.map knoten@{ knoten ->
        if (knoten.art != "mathematik.matrixProdukt") return@knoten knoten
        val neueAnschlüsse = knoten.anschlüsse.map anschluss@{ anschluss ->
            val standard = standardEingänge[anschluss.name]
                ?.takeIf { anschluss.richtung == AnschlussRichtung.Eingang }
                ?: return@anschluss anschluss
            standard.copy(id = anschluss.id).also {
                if (it != anschluss) verändert = true
            }
        }
        if (neueAnschlüsse == knoten.anschlüsse) knoten else knoten.copy(anschlüsse = neueAnschlüsse)
    }
    return if (verändert) copy(knoten = neueKnoten) else this
}

package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.graphMenge
import de.TeutonStudio.MathematikRechenSystem.kern.graphRaum

const val METHODEN_GRAPH_KNOTEN_ART = "mathematik.methodenGraph"

object MethodenGraphKnotenVorlagen {
    val Graph = KnotenVorlage(
        art = METHODEN_GRAPH_KNOTEN_ART,
        name = "Graph",
        kategorie = "Analysis: Funktionen",
        beschreibung = "Erzeugt den Funktionsgraphen einer Methode f: W→Z als Menge Graph(f) = {(x,f(x)) | x∈W} ⊆ W×Z. Suchbegriffe: Funktionsgraph, Graph einer Funktion, Graph einer Methode, Γ_f.",
        standardGröße = GraphGröße(260f, 120f),
        anschlüsse = listOf(
            AnschlussDaten(
                name = "methode",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Methode.id,
            ),
            AnschlussDaten(
                name = "graph",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Menge.id,
            ),
        ),
    )

    val alle = listOf(Graph)
}

internal fun MathematikAuswerterRegister.registriereMethodenGraphKnoten() {
    registriere(METHODEN_GRAPH_KNOTEN_ART) { kontext ->
        val methode = kontext.eingänge["methode"]?.objekt as? Methode
            ?: error("Für den Graphen fehlt die Methode.")

        // Die Signatur wird bewusst vor der Graphkonstruktion vollständig geprüft.
        // So entstehen keine scheinbar gültigen Graphmengen mit geratenem Definitionsraum.
        runCatching { methode.graphRaum() }.getOrElse { ursache ->
            error(
                "Der Graph von '${methode.name}' kann nicht gebildet werden: " +
                    (ursache.message ?: "Die Methodensignatur ist unvollständig."),
            )
        }

        val graph = methode.graphMenge()
        val annahmen = kontext.eingänge.values.flatMap { it.annahmen }.toSet()

        KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "graph" to BedingterWert(
                    objekt = graph,
                    annahmen = annahmen,
                    latexDarstellung = graph.zuLatex(),
                ),
            ),
        )
    }
}

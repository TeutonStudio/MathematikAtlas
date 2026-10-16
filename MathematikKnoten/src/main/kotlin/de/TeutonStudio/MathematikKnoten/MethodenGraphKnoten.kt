package de.TeutonStudio.MathematikKnoten

import androidx.compose.runtime.Composable
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Row
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
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
        val eingang = kontext.eingänge[if (graphTermEingang(kontext.knoten)) "term" else "methode"]
            ?: error("Für den Graphen fehlt der Eingang.")
        val methode = (if (graphTermEingang(kontext.knoten)) termAlsMethode(kontext, eingang).objekt else eingang.objekt) as? Methode
            ?: error("Für den Graphen fehlt eine mathematische Methode.")

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

const val GRAPH_EINGANG_MODUS = "graph.eingang"
fun graphTermEingang(knoten: KnotenDaten) = knoten.parameter[GRAPH_EINGANG_MODUS] == "term"
fun konfiguriereGraphEingang(knoten: KnotenDaten, term: Boolean): KnotenDaten {
    require(knoten.art == METHODEN_GRAPH_KNOTEN_ART)
    return knoten.copy(parameter = knoten.parameter + (GRAPH_EINGANG_MODUS to if (term) "term" else "methode"),
        anschlüsse = knoten.anschlüsse.map {
            if (it.richtung == AnschlussRichtung.Eingang) it.copy(name = if (term) "term" else "methode",
                art = if (term) MathematikAnschlussArten.Objekt.id else MathematikAnschlussArten.Methode.id,
                zulässigeArten = if (term) setOf(MathematikAnschlussArten.Zahl.id, MathematikAnschlussArten.Aussage.id, MathematikAnschlussArten.Menge.id, MathematikAnschlussArten.Tupel.id, MathematikAnschlussArten.Vektor.id, MathematikAnschlussArten.Matrix.id, MathematikAnschlussArten.Winkel.id) else emptySet(),
                vertrag = de.TeutonStudio.TypSystem.AnschlussVertrag()) else it
        })
}
@Composable
fun GraphEingangsEinstellungen(knoten: KnotenDaten, ändern: (KnotenDaten) -> Unit) {
    if (knoten.art != METHODEN_GRAPH_KNOTEN_ART) return
    Text("Eingang", style = MaterialTheme.typography.titleSmall)
    Row {
        FilterChip(!graphTermEingang(knoten), { ändern(konfiguriereGraphEingang(knoten, false)) }, label = { Text("Methode") })
        FilterChip(graphTermEingang(knoten), { ändern(konfiguriereGraphEingang(knoten, true)) }, label = { Text("Term") })
    }
}

/** Fügt in der vorhandenen Definition dieselbe Übersetzung wie im produktiven Termmodus ein. */
fun graphDefinitionsKarte(knoten: KnotenDaten, karte: de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten): de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten {
    if (knoten.art != METHODEN_GRAPH_KNOTEN_ART || !graphTermEingang(knoten)) return karte
    val quelle = karte.knoten.firstOrNull { it.id.wert == "graph-funktion" } ?: return karte
    val ausgang = quelle.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }
    val übersetzerBasis = konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(
        de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt(quelle.position.x + 300f, quelle.position.y)), "methode")
        .copy(id = de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId("graph-term-übersetzer"), name = "Term → Methode")
    val übersetzer = übersetzerBasis.copy(anschlüsse = übersetzerBasis.anschlüsse.map { it.copy(id = de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussId("graph-term-übersetzer-${it.name}")) })
    val eingangRef = de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussVerweis(übersetzer.id, übersetzer.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang }.id)
    val ausgangRef = de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussVerweis(übersetzer.id, übersetzer.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }.id)
    return karte.copy(knoten = karte.knoten.map {
        if (it.id == quelle.id) it.copy(parameter = it.parameter + ("name" to "term"), anschlüsse = it.anschlüsse.map { a -> a.copy(art = MathematikAnschlussArten.Objekt.id) })
        else it.copy(position = it.position + de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt(400f, 0f))
    } + übersetzer, verbindungen = karte.verbindungen.map {
        if (it.von.knotenId == quelle.id && it.von.anschlussId == ausgang.id) it.copy(von = ausgangRef) else it
    } + de.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten(
        id = de.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungsId("graph-term-v1"),
        von = de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussVerweis(quelle.id, ausgang.id), zu = eingangRef))
}

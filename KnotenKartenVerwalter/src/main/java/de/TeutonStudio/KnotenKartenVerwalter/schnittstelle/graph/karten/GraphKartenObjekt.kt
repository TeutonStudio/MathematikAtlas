package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import de.TeutonStudio.KnotenKartenVerwalter.AuswahlÄndern
import de.TeutonStudio.KnotenKartenVerwalter.KartenAktualisierung
import de.TeutonStudio.KnotenKartenVerwalter.VerbindungErstellen
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteZustand
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjekt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.Anschluss
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.Verbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.VerbindungFabrik

/**
 * Vertrag für das Kartenobjekt, das Knoten, Anschlüsse und Verbindungen der [KarteDaten] verwaltet.
 * [Karte] und [BasisKarte] sind die vorgesehenen Erweiterungspunkte für konkrete Kartenimplementierungen.
 *
 * Die Karte hält den interaktiven [KarteZustand], erzeugt Graphobjekte über Fabriken und koordiniert Auswahl,
 * Kontextzustand sowie Verbindungserstellung zwischen Anschlüssen.
 */
internal interface GraphKartenObjekt<K: KarteDaten>: GraphDatenObjekt<K> {
    abstract val zustand: KarteZustand
    abstract val knotenFabrik: KnotenFabrik
    abstract val verbindungFabrik: VerbindungFabrik
    abstract val pseudoVerbindung: MutableState<Verbindung?>
    abstract val aktualisierung: KartenAktualisierung
    abstract val onVerbindungErstellen: VerbindungErstellen
//    abstract val onKontextAktion: KontextAktionAusführen
    abstract val onAuswahlÄndern: AuswahlÄndern

    public fun verschiebeKnoten(id: String, um: Offset): Boolean
    public fun vernichteKnoten(knoten: Knoten): Boolean
    public fun dupliziereKnoten(knoten: Knoten): Boolean

    public fun planeVerbindung(a: Anschluss<out AnschlussDaten>)
    public fun definiereVerbindung(mann: Anschluss<out AnschlussDaten>, weib: Anschluss<out AnschlussDaten>): Boolean
    public fun vernichteVerbindung(verbindung: Verbindung): Boolean

    /** Erstellt den Modifier-Vertrag der Kartenfläche mit Größenmessung, Clipping und Eingabegesten. */
    @Composable override fun Modifier.modifier(): Modifier = fillMaxSize().onSizeChanged { zustand.dimension = it }.clipToBounds().transform().tapping()

}

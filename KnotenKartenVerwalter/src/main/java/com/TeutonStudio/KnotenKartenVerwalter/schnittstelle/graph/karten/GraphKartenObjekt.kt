package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import com.TeutonStudio.KnotenKartenVerwalter.AuswahlÄndern
import com.TeutonStudio.KnotenKartenVerwalter.KartenAktualisierung
import com.TeutonStudio.KnotenKartenVerwalter.VerbindungErstellen
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjekt
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.Anschluss
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenFabrik
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.Verbindung
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.VerbindungFabrik

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

    public fun definiereVerbindung(mann: Anschluss<out AnschlussDaten>, weib: Anschluss<out AnschlussDaten>): Boolean
    public fun vernichteVerbindung(verbindung: Verbindung): Boolean

    @Composable override fun Modifier.modifier(): Modifier = fillMaxSize().onSizeChanged { zustand.dimension = it }.clipToBounds().transform().tapping()

}
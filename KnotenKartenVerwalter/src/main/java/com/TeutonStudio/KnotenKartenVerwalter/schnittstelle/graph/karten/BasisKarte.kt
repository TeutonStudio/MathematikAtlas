package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.TeutonStudio.KnotenKartenVerwalter.AuswahlÄndern
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import com.TeutonStudio.KnotenKartenVerwalter.KartenAktualisierung
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.KontextAktionAusführen
import com.TeutonStudio.KnotenKartenVerwalter.VerbindungErstellen
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.pos
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.BasisKnotenFabrik
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenArt
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenFabrik
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.BasisVerbindungFabrik
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.Verbindung
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.VerbindungFabrik

open class BasisKarte(
    _graph: Graph,
    override val daten: KarteDaten,
    override val zustand: KarteZustand,
    override val aktualisierung: KartenAktualisierung,
    override val onVerbindungErstellen: VerbindungErstellen,
    override val onKontextAktion: KontextAktionAusführen,
    override val onAuswahlÄndern: AuswahlÄndern,
): Karte(_graph) {
    override val knotenFabrik: KnotenFabrik = BasisKnotenFabrik
    override val verbindungFabrik: VerbindungFabrik = BasisVerbindungFabrik

    override val pseudoVerbindung = mutableStateOf<Verbindung?>(null)

    private fun pos(arg: Map.Entry<AnschlussDaten, KnotenDaten>): KartenPosition = (arg.value to arg.key).pos()

    public companion object {
        public const val KARTEN_ART: KnotenArt = "default"
    }
}
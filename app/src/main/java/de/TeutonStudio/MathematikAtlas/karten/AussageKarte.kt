package de.TeutonStudio.MathematikAtlas.karten

import de.TeutonStudio.KnotenKartenVerwalter.AuswahlÄndern
import de.TeutonStudio.KnotenKartenVerwalter.KartenAktualisierung
import de.TeutonStudio.KnotenKartenVerwalter.VerbindungErstellen
import de.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteZustand
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.AnschlussKnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.BasisKarte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.BasisKartenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.KartenArt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.KartenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.KartenKonstruktor
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.BasisKnotenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.VerbindungFabrik
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.AussageKnotenFabrik
import de.TeutonStudio.MathematikAtlas.knoten.MatheKnotenFabrik

class AussageKarteDaten(
    id: String,
    name: String,
    initialKnoten: List<AnschlussKnotenDaten> = emptyList(),
    initialVerbindungen: List<VerbindungDaten> = emptyList(),
) : KarteDaten(
    id = id,
    name = name,
    initialKnoten = initialKnoten,
    initialVerbindungen = initialVerbindungen,
) {
    override var klasse: KartenArt? = AussageKarte.KARTEN_ART
}

class AussageKarte(
    graph: Graph,
    daten: KarteDaten,
    zustand: KarteZustand,
    aktualisierung: KartenAktualisierung,
    onVerbindungErstellen: VerbindungErstellen,
    onAuswahlÄndern: AuswahlÄndern,
) : BasisKarte(
    graph = graph,
    daten = daten,
    zustand = zustand,
    aktualisierung = aktualisierung,
    onVerbindungErstellen = onVerbindungErstellen,
    onAuswahlÄndern = onAuswahlÄndern,
) {
    override val knotenFabrik: KnotenFabrik = BasisKnotenFabrik + AussageKnotenFabrik

    companion object {
        const val KARTEN_ART: KartenArt = "aussage-karte"
    }
}

@Suppress("UNCHECKED_CAST")
val MatheKartenFabrik: KartenFabrik =
    BasisKartenFabrik + mapOf(
        AussageKarte.KARTEN_ART to
                (::AussageKarte as KartenKonstruktor)
    )
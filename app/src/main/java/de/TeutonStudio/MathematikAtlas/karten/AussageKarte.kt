package de.TeutonStudio.MathematikAtlas.karten

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKarte
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKnoten
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Richtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.vordefiniert.BasisDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Auswahl
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKarte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Kontext
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Zustand
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindete
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.veränderung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.wählte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.BasisKartenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.BasisKnotenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.BasisVerbindungFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KartenArt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KartenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KartenKonstruktor
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KnotenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.VerbindungFabrik
import de.TeutonStudio.MathematikAtlas.anschlüsse.AussageObjektAnschluss
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.AussageAuswerten
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.AussageWert
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.AussageKnotenFabrik
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.OperatorDaten

class AussageKarte(
    override val graph: Graph,
    override val daten: AussageKarte.AussageKarteDaten,
    veränderung: veränderung,
    verbindete: verbindete,
    wählte: wählte,
): GraphDatenObjektKarte<AussageKarte.AussageKarteDaten> {
    override val knotenFabrik: KnotenFabrik = BasisKnotenFabrik + AussageKnotenFabrik
    override val verbindungFabrik: VerbindungFabrik = BasisVerbindungFabrik

    override val ctx = Kontext()
    override val auswahl = Auswahl()
    override val zustand: Zustand = Zustand()
    override val pseudoVerbindung = mutableStateOf<GraphDatenObjektVerbindung<*>?>(null)
    override fun definiereVerbindung(
        mann: GraphDatenObjektAnschluss<*>,
        weib: GraphDatenObjektAnschluss<*>
    ) {
        super.definiereVerbindung(mann, weib)
        val ids = GraphDatenVerbindung.IDEhe(mann, weib)
        listOf(mann, weib)
            .filter { (it.daten as? GraphDatenAnschluss.gerichteteGDA)?.richtung == Richtung.Eingang }
            .forEach { eingang ->
                daten.verbindungen.removeAll { verbindung ->
                    verbindung.ids.enthält(eingang.daten)
                }
            }

        daten.verbindungen.add(
            BasisDatenVerbindung(
                id = "${ids.knotenIdMann}-${ids.anschlussIdMann}-${ids.knotenIdWeib}-${ids.anschlussIdWeib}",
                ids = ids,
            )
        )
        daten.aktualisierePullCaches()
    }

    override val layoutCoordinates = mutableStateOf<LayoutCoordinates?>(null)

    class AussageKarteDaten(
        override val id: String,
        override val name: String,
        initialKnoten: List<GraphDatenKnoten> = emptyList(),
        initialVerbindungen: List<GraphDatenVerbindung> = emptyList(),
    ): GraphDatenKarte{
        override val knoten = mutableStateListOf<GraphDatenKnoten>()
        override val verbindungen = mutableStateListOf<GraphDatenVerbindung>()
        init {
            knoten.addAll(initialKnoten)
            verbindungen.addAll(initialVerbindungen)
            aktualisierePullCaches()
        }

        override var breite = 0f
        override var tiefe = 0f
        override var klasse: KartenArt? = AussageKarte.KARTEN_ART

        fun entferneVerbindungenMitAnschlüssen(anschlussIds: Collection<String>) {
            if (anschlussIds.isEmpty()) return

            verbindungen.removeAll {
                it.ids.anschlussIdMann in anschlussIds || it.ids.anschlussIdWeib in anschlussIds
            }
            aktualisierePullCaches()
        }

        fun aktualisierePullCaches() {
            knoten
                .flatMap { it.anschlüsse }
                .filterIsInstance<GraphDatenAnschluss.auswertbarerGDA>()
                .filter { it.istEingang }
                .forEach { it.setzeAussageWert(AussageWert.UNBEKANNT) }

            repeat(knoten.size + verbindungen.size + 1) {
                var geändert = false

                verbindungen.forEach { verbindung ->
                    val mann = knoten
                        .find { it.id == verbindung.ids.knotenIdMann }
                        ?.anschlüsse
                        ?.find { it.id == verbindung.ids.anschlussIdMann }
                    val weib = knoten
                        .find { it.id == verbindung.ids.knotenIdWeib }
                        ?.anschlüsse
                        ?.find { it.id == verbindung.ids.anschlussIdWeib }

                    geändert = übertrageAussageCache(mann, weib) || geändert
                    geändert = übertrageAussageCache(weib, mann) || geändert
                }

                knoten.filterIsInstance<OperatorDaten>().forEach { operator ->
                    val vorher = operator.ausgangsWerte()
                    operator.aktualisiereCache()
                    geändert = operator.ausgangsWerte() != vorher || geändert
                }

                if (!geändert) {
                    synchronisiereAuswertenUnbekannte()
                    return
                }
            }

            synchronisiereAuswertenUnbekannte()
        }

        private fun übertrageAussageCache(
            von: GraphDatenAnschluss?,
            nach: GraphDatenAnschluss?,
        ): Boolean {
            val ausgang = von as? GraphDatenAnschluss.auswertbarerGDA
                ?: return false
            val eingang = nach as? GraphDatenAnschluss.auswertbarerGDA
                ?: return false

            if (!ausgang.istAusgang || !eingang.istEingang) return false

            val alterWert = eingang.aussageWert()
            val neuerWert = ausgang.aussageWert()
            if (alterWert == neuerWert) return false

            eingang.setzeAussageWert(neuerWert)
            return true
        }

        private fun synchronisiereAuswertenUnbekannte() {
            knoten.filterIsInstance<AussageAuswerten>().forEach { auswerten ->
                val hauptEingang = auswerten.hauptEingang() ?: return@forEach
                val unbekannte = unbekannteEingängeVor(auswerten.id, hauptEingang.id)
                    .distinctBy { it.id }
                auswerten.aktualisiereUnbekannteEingänge(unbekannte.size)
            }
        }

        private fun unbekannteEingängeVor(
            knotenId: String,
            anschlussId: String,
            besucht: Set<Pair<String, String>> = emptySet(),
        ): List<GraphDatenAnschluss> {
            val schlüssel = knotenId to anschlussId
            if (schlüssel in besucht) return emptyList()

            val knoten = knoten.find { it.id == knotenId } ?: return emptyList()
            val anschluss = knoten.anschlüsse.find { it.id == anschlussId } ?: return emptyList()
            val auswertbarerAnschluss = anschluss as? GraphDatenAnschluss.auswertbarerGDA

            if (auswertbarerAnschluss?.istEingang == true) {
                val quelle = verbindungen
                    .asSequence()
                    .mapNotNull { verbindung -> verbindung.andereSeiteVon(knotenId, anschlussId) }
                    .firstOrNull { (quellKnotenId, quellAnschlussId) ->
                        val quellKnoten = this.knoten.find { it.id == quellKnotenId }
                        val quellAnschluss = quellKnoten
                            ?.anschlüsse
                            ?.find { it.id == quellAnschlussId }
                        quellAnschluss is GraphDatenAnschluss.auswertbarerGDA &&
                                quellAnschluss.istAusgang
                    }

                return if (quelle == null) {
                    if (auswertbarerAnschluss.aussageWert() == AussageWert.UNBEKANNT) listOf(anschluss)
                    else emptyList()
                } else {
                    unbekannteEingängeVor(quelle.first, quelle.second, besucht + schlüssel)
                }
            }

            if (auswertbarerAnschluss?.istAusgang == true && knoten is OperatorDaten) {
                return knoten.anschlüsse
                    .filterIsInstance<GraphDatenAnschluss.auswertbarerGDA>()
                    .filter { it.istEingang }
                    .flatMap { unbekannteEingängeVor(knoten.id, it.id, besucht + schlüssel) }
            }

            return emptyList()
        }
    }

    @Composable
    override fun BoxScope.KontextFenster(pos: IntOffset) {
        Column() {
            Text("KontextFenster der AussageKarte")
        }
    }

    @Composable
    override fun BoxScope.Inspektor() {}

    companion object {
        const val KARTEN_ART: KartenArt = "aussage-karte"
    }
}

@Suppress("UNCHECKED_CAST")
val MatheKartenFabrik: KartenFabrik = BasisKartenFabrik + mapOf(
        AussageKarte.KARTEN_ART to (::AussageKarte as KartenKonstruktor)
    )

private fun GraphDatenAnschluss.auswertbarerGDA.aussageWert(): AussageWert =
    (cache as? AussageObjektAnschluss.AussageAnschlussDaten.CacheDaten)?.wert
        ?: AussageWert.UNENTSCHEIDBAR

private fun GraphDatenAnschluss.auswertbarerGDA.setzeAussageWert(wert: AussageWert) {
    cache = AussageObjektAnschluss.AussageAnschlussDaten.CacheDaten(wert)
}

private fun OperatorDaten.ausgangsWerte(): List<AussageWert> =
    anschlüsse
        .filterIsInstance<GraphDatenAnschluss.auswertbarerGDA>()
        .filter { it.istAusgang }
        .map { it.aussageWert() }

private fun GraphDatenVerbindung.andereSeiteVon(
    knotenId: String,
    anschlussId: String,
): Pair<String, String>? =
    when {
        ids.knotenIdWeib == knotenId && ids.anschlussIdWeib == anschlussId ->
            ids.knotenIdMann to ids.anschlussIdMann

        ids.knotenIdMann == knotenId && ids.anschlussIdMann == anschlussId ->
            ids.knotenIdWeib to ids.anschlussIdWeib

        else -> null
    }

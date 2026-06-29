package de.TeutonStudio.MathematikAtlas.karten

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
import de.TeutonStudio.MathematikAtlas.anschlüsse.MengenObjektAnschluss
import de.TeutonStudio.MathematikAtlas.anschlüsse.ZahlenObjektAnschluss
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.AussageKnotenFabrik
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.AussageWert
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.OperatorDaten
import de.TeutonStudio.MathematikAtlas.knoten.MengenKnoten.MengenKnotenFabrik
import de.TeutonStudio.MathematikAtlas.knoten.MengenKnoten.MengenOperatorDaten
import de.TeutonStudio.MathematikAtlas.knoten.MengenKnoten.MengenRelationDaten
import de.TeutonStudio.MathematikAtlas.knoten.ZahlenKnoten.ZahlenKnotenFabrik
import de.TeutonStudio.MathematikAtlas.knoten.ZahlenKnoten.ZahlenOperatorDaten
import de.TeutonStudio.MathematikAtlas.knoten.ZahlenKnoten.ZahlenRelationDaten

class MatheKarte(
    override val graph: Graph,
    override val daten: MatheKarteDaten,
    veränderung: veränderung,
    verbindete: verbindete,
    wählte: wählte,
) : GraphDatenObjektKarte<MatheKarte.MatheKarteDaten> {
    override val knotenFabrik: KnotenFabrik =
        BasisKnotenFabrik + AussageKnotenFabrik + ZahlenKnotenFabrik + MengenKnotenFabrik
    override val verbindungFabrik: VerbindungFabrik = BasisVerbindungFabrik

    override val kontextData: List<@Composable (() -> Unit)> = listOf(
        { Text(daten.name,Modifier.clickable { TODO("Kein duplizieren implementiert") }) },
        { Text("Duplizieren",Modifier.clickable { TODO("Kein duplizieren implementiert") }) },
        { Text("Vernichten",Modifier.clickable { TODO("Kein vernichten implementiert") }) },
    )


    override val ctx = Kontext()
    override val auswahl = Auswahl()
    override val zustand = Zustand()
    override val pseudoVerbindung = mutableStateOf<GraphDatenObjektVerbindung<*>?>(null)
    override val layoutCoordinates = mutableStateOf<LayoutCoordinates?>(null)

    override fun definiereVerbindung(
        mann: GraphDatenObjektAnschluss<*>,
        weib: GraphDatenObjektAnschluss<*>,
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

    @Composable
    override fun BoxScope.KontextFenster(pos: IntOffset) {
        Card(Modifier.offset { pos }.padding(4.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("KontextFenster der MatheKarte")
            }
        }
    }

    class MatheKarteDaten(
        override val id: String,
        override val name: String,
        initialKnoten: List<GraphDatenKnoten> = emptyList(),
        initialVerbindungen: List<GraphDatenVerbindung> = emptyList(),
    ) : GraphDatenKarte {
        override val knoten = mutableStateListOf<GraphDatenKnoten>()
        override val verbindungen = mutableStateListOf<GraphDatenVerbindung>()
        override var breite = 0f
        override var tiefe = 0f
        override var klasse: KartenArt? = KARTEN_ART

        init {
            knoten.addAll(initialKnoten)
            verbindungen.addAll(initialVerbindungen)
            aktualisierePullCaches()
        }

        fun aktualisierePullCaches() {
            knoten
                .flatMap { it.anschlüsse }
                .filterIsInstance<GraphDatenAnschluss.auswertbarerGDA>()
                .filter { it.istEingang }
                .forEach { it.setzeUnbekannt() }

            repeat(knoten.size + verbindungen.size + 1) {
                var geändert = false

                verbindungen.forEach { verbindung ->
                    val mann = anschluss(verbindung.ids.knotenIdMann, verbindung.ids.anschlussIdMann)
                    val weib = anschluss(verbindung.ids.knotenIdWeib, verbindung.ids.anschlussIdWeib)
                    geändert = übertrageCache(mann, weib) || geändert
                    geändert = übertrageCache(weib, mann) || geändert
                }

                geändert = aktualisiereKnotenCaches() || geändert
                if (!geändert) return
            }
        }

        private fun aktualisiereKnotenCaches(): Boolean {
            var geändert = false
            knoten.filterIsInstance<OperatorDaten>().forEach {
                val vorher = it.ausgangsCacheSignatur()
                it.aktualisiereCache()
                geändert = it.ausgangsCacheSignatur() != vorher || geändert
            }
            knoten.filterIsInstance<ZahlenOperatorDaten>().forEach {
                val vorher = it.ausgangsCacheSignatur()
                it.aktualisiereCache()
                geändert = it.ausgangsCacheSignatur() != vorher || geändert
            }
            knoten.filterIsInstance<MengenOperatorDaten>().forEach {
                val vorher = it.ausgangsCacheSignatur()
                it.aktualisiereCache()
                geändert = it.ausgangsCacheSignatur() != vorher || geändert
            }
            knoten.filterIsInstance<ZahlenRelationDaten>().forEach {
                val vorher = it.ausgangsCacheSignatur()
                it.aktualisiereCache()
                geändert = it.ausgangsCacheSignatur() != vorher || geändert
            }
            knoten.filterIsInstance<MengenRelationDaten>().forEach {
                val vorher = it.ausgangsCacheSignatur()
                it.aktualisiereCache()
                geändert = it.ausgangsCacheSignatur() != vorher || geändert
            }
            return geändert
        }

        private fun anschluss(knotenId: String, anschlussId: String): GraphDatenAnschluss? =
            knoten.find { it.id == knotenId }?.anschlüsse?.find { it.id == anschlussId }

        private fun übertrageCache(
            von: GraphDatenAnschluss?,
            nach: GraphDatenAnschluss?,
        ): Boolean {
            val ausgang = von as? GraphDatenAnschluss.auswertbarerGDA ?: return false
            val eingang = nach as? GraphDatenAnschluss.auswertbarerGDA ?: return false
            if (!ausgang.istAusgang || !eingang.istEingang) return false

            val vorher = eingang.cacheSignatur()
            eingang.cache = ausgang.cache
            return eingang.cacheSignatur() != vorher
        }
    }

    companion object {
        const val KARTEN_ART: KartenArt = "mathe-karte"
    }
}

@Suppress("UNCHECKED_CAST")
val MatheKartenFabrik: KartenFabrik = BasisKartenFabrik + mapOf(
    AussageKarte.KARTEN_ART to (::AussageKarte as KartenKonstruktor),
    MatheKarte.KARTEN_ART to (::MatheKarte as KartenKonstruktor),
)

fun GraphDatenAnschluss.auswertbarerGDA.cacheSignatur(): String =
    when (val cache = cache) {
        is AussageObjektAnschluss.AussageAnschlussDaten.CacheDaten -> cache.wert.name
        is ZahlenObjektAnschluss.ZahlenAnschlussDaten.CacheDaten -> cache.latex
        is MengenObjektAnschluss.MengenAnschlussDaten.CacheDaten -> cache.latex
        else -> cache.toString()
    }

fun GraphDatenAnschluss.auswertbarerGDA.setzeUnbekannt() {
    cache = when (this) {
        is AussageObjektAnschluss.AussageAnschlussDaten ->
            AussageObjektAnschluss.AussageAnschlussDaten.CacheDaten(AussageWert.UNBEKANNT)
        is ZahlenObjektAnschluss.ZahlenAnschlussDaten ->
            ZahlenObjektAnschluss.ZahlenAnschlussDaten.CacheDaten()
        is MengenObjektAnschluss.MengenAnschlussDaten ->
            MengenObjektAnschluss.MengenAnschlussDaten.CacheDaten()
        else -> cache
    }
}

fun GraphDatenKnoten.ausgangsCacheSignatur(): List<String> =
    anschlüsse
        .filterIsInstance<GraphDatenAnschluss.auswertbarerGDA>()
        .filter { it.istAusgang }
        .map { it.cacheSignatur() }

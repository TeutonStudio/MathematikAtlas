package de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.fremderAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKnoten
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Kante
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Richtung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisObjektKontext
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKarte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektInspektor
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.auswahl
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.info
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.AnschlussFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KnotenArt
import de.TeutonStudio.MathematikAtlas.anschlüsse.AussageObjektAnschluss
import de.TeutonStudio.MathematikAtlas.anschlüsse.AussageObjektAnschluss.AussageAnschlussDaten
import de.TeutonStudio.MathematikAtlas.anschlüsse.AussageObjektAusgang
import de.TeutonStudio.MathematikAtlas.anschlüsse.AussageObjektEingang.AussageEingang
import de.TeutonStudio.MathematikAtlas.anschlüsse.AussageObjektAusgang.AussageAusgang
import de.TeutonStudio.MathematikAtlas.anschlüsse.AussageObjektEingang
import de.TeutonStudio.MathematikAtlas.anschlüsse.MatheAnschlussFabrik
import de.TeutonStudio.MathematikAtlas.karten.AussageKarte

typealias OperatorDaten = operator.AussageOperatorDatenBasis

class operator(
    override val graph: Graph,
    override val daten: OperatorDaten,
    override val besitzer: GraphDatenObjektKarte<*>,
): GraphDatenObjektKnoten<OperatorDaten>, GraphDatenObjektInspektor<OperatorDaten> {
    override val layoutCoordinates = mutableStateOf<LayoutCoordinates?>(null)

    @Composable
    override fun BoxScope.Darstellung() {
        Card(Modifier) {
            Column {
                Text(daten.name)
                LaTeXFormelText(
                    formel = besitzer.daten.latexFormelFuer(daten),
                    karte = besitzer.daten,
                )
            }
        }
    }

    @Composable
    override fun BoxScope.KontextFenster(pos: IntOffset) {
        StandardKontextFenster(pos)
    }

    @Composable
    override fun BoxScope.Inspektor() {
        Composable()
    }

    @Composable
    override fun Inhalt() {
        BasisObjektKontext(
            auswahl(
                name = "LaTeX",
                wert = if (daten.latexRekursiv()) "rekursiv" else "implizit",
                optionen = listOf("rekursiv", "implizit"),
            ) { auswahl ->
                daten.setzeLatexRekursiv(auswahl == "rekursiv")
            },
            info("Formel", besitzer.daten.latexFormelFuer(daten)),
            auswahl(
                name = "Operator",
                wert = verknüpfung.anzeige,
                optionen = AussagenVerknüpfung.entries.map { it.anzeige },
            ) { auswahl ->
                AussagenVerknüpfung.entries
                    .firstOrNull { it.anzeige == auswahl }
                    ?.let(::setzeVerknüpfung)
            },
            info("Stelligkeit", verknüpfung.stelligkeit),
        ).Inhalt()
    }

    class AussageOperatorDatenBasis(
        override val id: String,
        override val name: String = "operator Knoten",
    ): GraphDatenKnoten, GraphDatenKnoten.auswertbarerGDK {
        override var klasse: KnotenArt? = operator.KNOTEN_ART
//        override val anschlussCache: SnapshotStateMap<String, PullSystemDaten.PullDaten<*>> = erhalteCache().value
/*        override fun baueCache(
            ausgang: AussageAnschlussDaten,
            eingänge: List<AussageAnschlussDaten>
        ): PullSystemDaten.PullDaten<*> {
            eingänge.map { it.cache }
            TODO("Not yet implemented")
        }*/
        override var beweglich: Boolean = true
        override val anschlüsse = mutableStateListOf<GraphDatenAnschluss>()
        override val anschlussIdx = mutableStateMapOf<String, Int>()
        override val data = mutableStateMapOf<String,Any>()
        override var position: GraphPosition by mutableStateOf(GraphPosition.Zero)
        override var breite: Float by mutableFloatStateOf(0f)
        override var tiefe: Float by mutableFloatStateOf(0f)

        val istAssoziativ: Boolean
            get() = aussagenVerknüpfung().istAssoziativ

        val istKommutativ: Boolean
            get() = aussagenVerknüpfung().istKommutativ

//        private fun eingangIdx() = anschlüsse.filterIsInstance<GraphDatenAnschluss.gerichteteGDA>().filter { it.richtung == Richtung.Eingang }.maxBy { anschlussIdx[it.id] ?: 0 }.id.split("-").last().toInt()
        private fun eingangId(idx: Int) = listOf(id,"eingang",idx).joinToString("-")
        private fun pseudoEingangId(idx: Int) = listOf(id, PSEUDO_EINGANG_MARKER, idx).joinToString("-")
        public fun eingang(idx: Int) = AussageAnschlussDaten(
            eingangId(idx),
            Kante.Links,
            Richtung.Eingang
        ).apply {
            label = "Aussage ${idx + 1}"
            klasse = AussageObjektEingang.ANSCHLUSS_ART
        }

        private fun pseudoEingang(idx: Int) = AussageAnschlussDaten(
            pseudoEingangId(idx),
            Kante.Links,
            Richtung.Eingang,
        ).apply {
            label = "Aussage ${idx + 1}"
            klasse = AussageObjektEingang.ANSCHLUSS_ART
            cache = AussageObjektAnschluss.AussageAnschlussDaten.CacheDaten(AussageWert.UNBEKANNT)
        }

        init {
            val ausgang = AussageAnschlussDaten("$id-ausgang-0", Kante.Rechts, Richtung.Ausgang)
                .apply {
                    label = "Ergebnis"
                    klasse = AussageObjektAusgang.ANSCHLUSS_ART
                }
            val anschlussListe = listOf(
                eingang(0).apply { anschlussIdx[this.id] = 0 },
                eingang(1).apply { anschlussIdx[this.id] = 1 },
                ausgang,
            )

            anschlüsse.addAll(anschlussListe)
            anschlussIdx[ausgang.id] = 0
            data[operator.OPERATOR_SCHLÜSSEL] = operator.AussagenVerknüpfung.UND.name
            synchronisiereEingänge()
        }

        fun aussagenVerknüpfung(): AussagenVerknüpfung =
            data[operator.OPERATOR_SCHLÜSSEL]
                ?.toString()
                ?.let { gespeicherterWert ->
                    AussagenVerknüpfung.entries.firstOrNull {
                        it.name == gespeicherterWert
                    }
                }
                ?: AussagenVerknüpfung.UND

        fun setzeAussagenVerknüpfung(verknüpfung: AussagenVerknüpfung): List<String> {
            data[operator.OPERATOR_SCHLÜSSEL] = verknüpfung.name
            val entfernteAnschlussIds = synchronisiereEingänge()
            aktualisiereCache()
            return entfernteAnschlussIds
        }

        private fun synchronisiereEingänge(): List<String> {
            val verknüpfung = aussagenVerknüpfung()
            val minimaleStelligkeit = verknüpfung.stelligkeit
            val eingänge = eingänge()
            val entfernte = eingänge
                .drop(minimaleStelligkeit)
                .map { it.id }

            if (entfernte.isNotEmpty()) {
                anschlüsse.removeAll { it.id in entfernte }
                entfernte.forEach { anschlussIdx.remove(it) }
            }

            val vorhandeneReguläreIndizes = eingänge()
                .filter { !it.id.contains(PSEUDO_EINGANG_MARKER) }
                .mapNotNull { anschlussIdx[it.id] }
                .toSet()

            repeat(minimaleStelligkeit) { index ->
                if (index !in vorhandeneReguläreIndizes) {
                    val anschluss = eingang(index)
                    anschlüsse.add(anschluss)
                    anschlussIdx[anschluss.id] = index
                }
            }

            return entfernte
        }

        private fun eingänge(): List<GraphDatenAnschluss.auswertbarerGDA> =
            anschlüsse
                .filterIsInstance<GraphDatenAnschluss.auswertbarerGDA>()
                .filter { it.istEingang }
                .sortedBy { anschlussIdx[it.id] ?: Int.MAX_VALUE }

        fun aktualisiereCache() {
            val eingänge = eingänge()
            val ausgänge = anschlüsse
                .filterIsInstance<GraphDatenAnschluss.auswertbarerGDA>()
                .filter { it.istAusgang }
            val werte = eingänge.map {
                (it.cache as? AussageObjektAnschluss.AussageAnschlussDaten.CacheDaten)?.wert
                    ?: AussageWert.UNENTSCHEIDBAR
            }
            val neuerWert = when (aussagenVerknüpfung()) {
                AussagenVerknüpfung.UND -> when {
                    werte.any { it == AussageWert.LUEGE } -> AussageWert.LUEGE
                    werte.any { it == AussageWert.UNBEKANNT } -> AussageWert.UNBEKANNT
                    werte.all { it == AussageWert.WAHR } -> AussageWert.WAHR
                    else -> AussageWert.UNENTSCHEIDBAR
                }

                AussagenVerknüpfung.ODER -> when {
                    werte.any { it == AussageWert.WAHR } -> AussageWert.WAHR
                    werte.any { it == AussageWert.UNBEKANNT } -> AussageWert.UNBEKANNT
                    werte.all { it == AussageWert.LUEGE } -> AussageWert.LUEGE
                    else -> AussageWert.UNENTSCHEIDBAR
                }

                AussagenVerknüpfung.IMPLIKATION -> when {
                    werte.size < 2 -> AussageWert.UNBEKANNT
                    werte[0] == AussageWert.LUEGE -> AussageWert.WAHR
                    werte[1] == AussageWert.WAHR -> AussageWert.WAHR
                    werte[0] == AussageWert.WAHR && werte[1] == AussageWert.LUEGE -> AussageWert.LUEGE
                    werte.any { it == AussageWert.UNBEKANNT } -> AussageWert.UNBEKANNT
                    else -> AussageWert.UNENTSCHEIDBAR
                }

                AussagenVerknüpfung.KONTRAJUNKTION -> when {
                    werte.size < 2 -> AussageWert.UNBEKANNT
                    werte.any { it == AussageWert.UNBEKANNT } -> AussageWert.UNBEKANNT
                    werte[0] == AussageWert.UNENTSCHEIDBAR || werte[1] == AussageWert.UNENTSCHEIDBAR -> AussageWert.UNENTSCHEIDBAR
                    werte[0] != werte[1] -> AussageWert.WAHR
                    else -> AussageWert.LUEGE
                }

                AussagenVerknüpfung.NEGATION -> when (werte.firstOrNull()) {
                    AussageWert.WAHR -> AussageWert.LUEGE
                    AussageWert.LUEGE -> AussageWert.WAHR
                    AussageWert.UNBEKANNT, null -> AussageWert.UNBEKANNT
                    else -> AussageWert.UNENTSCHEIDBAR
                }
            }

            ausgänge.forEach {
                it.cache = AussageObjektAnschluss.AussageAnschlussDaten.CacheDaten(neuerWert)
            }
        }

        override fun wurdeVerbunden(von: String, mit: fremderAnschluss) {
            super<GraphDatenKnoten.auswertbarerGDK>.wurdeVerbunden(von, mit)
            aktualisiereCache()
        }

        fun planePseudoEingang() {
            if (!istAssoziativ) return
            if (anschlüsse.any { it.id.contains(PSEUDO_EINGANG_MARKER) }) return

            val nächsteIdx = anschlüsse
                .filterIsInstance<GraphDatenAnschluss.auswertbarerGDA>()
                .filter { it.istEingang }
                .mapNotNull { anschlussIdx[it.id] }
                .maxOrNull()
                ?.plus(1)
                ?: 0
            val anschluss = pseudoEingang(nächsteIdx)
            anschlüsse.add(anschluss)
            anschlussIdx[anschluss.id] = nächsteIdx
        }

        fun entferneUnverbundenePseudoEingänge(verbindungen: Iterable<GraphDatenVerbindung>) {
            val verbundeneAnschlussIds = verbindungen.flatMap {
                listOf(it.ids.anschlussIdMann, it.ids.anschlussIdWeib)
            }.toSet()
            val unverbundenePseudoEingänge = anschlüsse.filter {
                it.id.contains(PSEUDO_EINGANG_MARKER) && it.id !in verbundeneAnschlussIds
            }

            unverbundenePseudoEingänge.forEach {
                anschlussIdx.remove(it.id)
            }
            anschlüsse.removeAll(unverbundenePseudoEingänge.toSet())
        }
    }

    enum class AussagenVerknüpfung(
        val anzeige: String,
        val stelligkeit: Int,
        val istAssoziativ: Boolean,
        val istKommutativ: Boolean,
    ) {
        UND("UND", 2, true, true),
        ODER("ODER", 2, true, true),
        IMPLIKATION("Implikation", 2, false, false),
        KONTRAJUNKTION("Kontrajunktion", 2, false, true),
        NEGATION("Negation", 1, false, false),
    }

    override val anschlussFabrik: AnschlussFabrik
        get() = MatheAnschlussFabrik

    override fun definiereVerbindung() {
        TODO("Not yet implemented")
    }

    override fun planeVerbindung(
        vonAnschluss: GraphDatenObjektAnschluss<*>,
        vonKnoten: GraphDatenObjektKnoten<*>,
    ) {
        if (vonKnoten.daten.id == daten.id) return
        if (vonAnschluss.istAusgang) daten.planePseudoEingang()
    }

    override fun verwerfeGeplanteVerbindung() {
        daten.entferneUnverbundenePseudoEingänge(besitzer.daten.verbindungen)
        (besitzer.daten as? AussageKarte.AussageKarteDaten)?.aktualisierePullCaches()
    }

/*    override val cacheAnschlüsse:
            SnapshotStateMap<String, PullErgebnis<Aussage>> =
        mutableStateMapOf()*/

//    override val wertKlasse = Aussage::class

    private var verknüpfung by mutableStateOf(
        daten.data[OPERATOR_SCHLÜSSEL]
            ?.toString()
            ?.let { gespeicherterWert ->
                AussagenVerknüpfung.entries.firstOrNull {
                    it.name == gespeicherterWert
                }
            }
            ?: AussagenVerknüpfung.UND
    )

    private fun setzeVerknüpfung(neueVerknüpfung: AussagenVerknüpfung) {
        verknüpfung = neueVerknüpfung
        val entfernteAnschlussIds = daten.setzeAussagenVerknüpfung(neueVerknüpfung)
        val aussageKarte = besitzer.daten as? AussageKarte.AussageKarteDaten
        aussageKarte?.entferneVerbindungenMitAnschlüssen(entfernteAnschlussIds)
        aussageKarte?.aktualisierePullCaches()
    }
/*
    public fun berechne(
        ausgangId: String,
        eingänge: Map<String, PullErgebnis<Aussage>>,
    ): PullErgebnis<Aussage> {
        if (eingänge.isEmpty()) {
            return PullErgebnis.Fehler(
                "Aussagenoperator ${daten.id} besitzt keine Eingänge"
            )
        }

        val eingangsFehler = eingänge.values
            .filterIsInstance<PullErgebnis.Fehler>()
            .firstOrNull()

        if (eingangsFehler != null) {
            return eingangsFehler
        }

        val aussagen = eingänge.values.mapNotNull {
            (it as? PullErgebnis.Wert<Aussage>)?.wert
        }

        if (aussagen.size != eingänge.size) {
            return PullErgebnis.Fehler(
                "Nicht alle Eingänge von ${daten.id} liefern Aussagen"
            )
        }

        return try {
            *//*
             * Das Prädikat wird sofort ausgewertet.
             *
             * Dadurch liefert dieser Knoten immer Wahr oder Lüge und
             * verschachtelte Operatoren laufen noch nicht in die
             * unvollständige Prädikat-Auswertung des Rechensystems.
             *//*
            val ergebnis = when (verknüpfung) {
                AussagenVerknüpfung.UND ->
                    konjunktion(
                        *aussagen.toTypedArray()
                    ).auswertung()

                AussagenVerknüpfung.ODER ->
                    disjunktion(
                        *aussagen.toTypedArray()
                    ).auswertung()
            }

            PullErgebnis.Wert(ergebnis)
        } catch (fehler: Throwable) {
            PullErgebnis.Fehler(
                meldung = "Aussagenoperator ${daten.id} konnte nicht " +
                        "ausgewertet werden: ${fehler.message}",
                ursache = fehler,
            )
        }
    }*/

    @Composable
    public fun Textzeile() {
        var geöffnet by remember { mutableStateOf(false) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { geöffnet = true },
            ) {
                Text(verknüpfung.anzeige)
            }
            DropdownMenu(
                expanded = geöffnet,
                onDismissRequest = { geöffnet = false },
            ) {
                AussagenVerknüpfung.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.anzeige) },
                        onClick = {
                            geöffnet = false
                            setzeVerknüpfung(option)
                        },
                    )
                }
            }
        }
    }

    public companion object {
        const val KNOTEN_ART: KnotenArt = "operatorAussage"
        const val OPERATOR_SCHLÜSSEL = "aussagen-operator"
        const val PSEUDO_EINGANG_MARKER = "pseudo-eingang"
    }
}

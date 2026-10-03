package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*

object BeispielKarten {
    fun alle(): List<KartenDaten> {
        return historische() + mandelbrotKarte()
    }

    /** Unveränderter Fünfersatz für die einmalige Legacy-Migration. */
    internal fun historische(): List<KartenDaten> {
        val doppeln = doppelnKarte()
        return listOf(doppeln, rechnenKarte(doppeln), aussageKarte(), mengenKarte(), verbindungsKarte())
    }

    private fun mandelbrotKarte(): KartenDaten {
        val zustand = MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt(70f, 70f)).copy(
            name = "Zustand z",
            parameter = mapOf("name" to "z", "werteVorrat" to "C"),
        )
        val parameter = MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt(70f, 230f)).copy(
            name = "Parameter c",
            parameter = mapOf("name" to "c", "werteVorrat" to "C"),
        )
        val zwei = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt(70f, 390f)).copy(
            name = "Exponent 2",
            parameter = mapOf("wert" to "2"),
        )
        val quadrat = MathematikKnotenVorlagen.Potenz.erzeuge(GraphPunkt(370f, 100f)).copy(name = "z²")
        val addiereParameter = MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt(670f, 170f)).copy(name = "z² + c")
        val methode = MathematikKnotenVorlagen.TermZuMethode.erzeuge(GraphPunkt(980f, 180f)).copy(
            name = "Iterationsmethode",
            parameter = mapOf("name" to "f", "argumentReihenfolge" to "z,c"),
        )
        val nullStart = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt(980f, 380f)).copy(
            name = "Start 0",
            parameter = mapOf("wert" to "0"),
        )
        val orbit = MathematikKnotenVorlagen.Orbit.erzeuge(GraphPunkt(1280f, 210f)).copy(
            parameter = mapOf("zustandsArgument" to "z"),
        )
        val komplexe = MathematikKnotenVorlagen.KomplexeZahlen.erzeuge(GraphPunkt(1280f, 410f)).copy(name = "Parameterraum ℂ")
        val beschränkt = MathematikKnotenVorlagen.OrbitBeschraenktheit.erzeuge(GraphPunkt(1590f, 250f))
        val ansicht = VisualisierungsKonfiguration(
            dimension = RaumDimension.C,
            achsen = AchsenZuordnung("re", "im", null),
            bereiche = AchsenBereiche(
                ZahlenBereich(-2.2, 0.8),
                ZahlenBereich(-1.5, 1.5),
                null,
            ),
            sampling = SamplingKonfiguration(96, 22, 0.02, maximaleOrbitSchritte = 256),
        )
        val visualisierung = MathematikKnotenVorlagen.Visualisierung.erzeuge(GraphPunkt(1920f, 170f)).copy(
            name = "Mandelbrot-Menge",
            eigenschaften = ansicht.zuEigenschaften(),
        )
        return KarteBauer("Mandelbrot-Menge als Orbitfamilie")
            .knoten(zustand, parameter, zwei, quadrat, addiereParameter, methode, nullStart, orbit, komplexe, beschränkt, visualisierung)
            .verbinde(zustand, "wert", quadrat, "basis")
            .verbinde(zwei, "wert", quadrat, "exponent")
            .verbinde(quadrat, "wert", addiereParameter, "a")
            .verbinde(parameter, "wert", addiereParameter, "b")
            .verbinde(addiereParameter, "wert", methode, "term")
            .verbinde(methode, "methode", orbit, "schritt")
            .verbinde(nullStart, "wert", orbit, "start")
            .verbinde(orbit, "orbit", beschränkt, "orbit")
            .verbinde(komplexe, "menge", beschränkt, "parameterraum")
            .verbinde(beschränkt, "menge", visualisierung, "menge")
            .baue()
    }

    private fun rechnenKarte(doppeln: KartenDaten): KartenDaten {
        val zwei = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt(80f, 100f)).copy(name = "Zwei", parameter = mapOf("wert" to "2"))
        val drei = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt(80f, 260f)).copy(name = "Drei", parameter = mapOf("wert" to "3"))
        val plus = MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt(390f, 160f))
        val auswerten = MathematikKnotenVorlagen.Auswerten.erzeuge(GraphPunkt(730f, 170f))
        val gruppe = gruppenKnoten(doppeln, GraphPunkt(1050f, 170f))
        return KarteBauer("Rechnen").knoten(zwei, drei, plus, auswerten, gruppe)
            .verbinde(zwei, "wert", plus, "a").verbinde(drei, "wert", plus, "b")
            .verbinde(plus, "wert", auswerten, "term").verbinde(auswerten, "term", gruppe, "x").baue()
    }

    private fun doppelnKarte(): KartenDaten {
        val ein = MathematikKnotenVorlagen.KartenEingang.erzeuge(GraphPunkt(80f, 150f)).copy(name = "x", parameter = mapOf("name" to "x"))
        val plus = MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt(390f, 130f))
        val aus = MathematikKnotenVorlagen.KartenAusgang.erzeuge(GraphPunkt(730f, 150f)).copy(name = "doppelt", parameter = mapOf("name" to "doppelt"))
        return KarteBauer("Doppeln").knoten(ein, plus, aus).verbinde(ein, "wert", plus, "a").verbinde(ein, "wert", plus, "b").verbinde(plus, "wert", aus, "wert").baue()
    }

    private fun aussageKarte(): KartenDaten {
        val a = MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt(90f, 100f)).copy(parameter = mapOf("name" to "x"))
        val b = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt(90f, 260f)).copy(parameter = mapOf("wert" to "0"))
        val gleich = MathematikKnotenVorlagen.Gleichheit.erzeuge(GraphPunkt(420f, 170f))
        val aus = MathematikKnotenVorlagen.Auswerten.erzeuge(GraphPunkt(760f, 170f))
        return KarteBauer("Aussage").knoten(a,b,gleich,aus).verbinde(a,"wert",gleich,"links").verbinde(b,"wert",gleich,"rechts").verbinde(gleich,"aussage",aus,"term").baue()
    }

    private fun mengenKarte(): KartenDaten {
        val a = MathematikKnotenVorlagen.EndlicheMenge.erzeuge(GraphPunkt(80f, 100f)).copy(parameter = mapOf("elemente" to "1,2,3"))
        val b = MathematikKnotenVorlagen.EndlicheMenge.erzeuge(GraphPunkt(80f, 270f)).copy(parameter = mapOf("elemente" to "3,4,5"))
        val union = MathematikKnotenVorlagen.Vereinigung.erzeuge(GraphPunkt(430f, 180f))
        return KarteBauer("Mengen").knoten(a,b,union).verbinde(a,"menge",union,"a").verbinde(b,"menge",union,"b").baue()
    }

    private fun verbindungsKarte(): KartenDaten {
        val zahl = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt(80f, 100f))
        val menge = MathematikKnotenVorlagen.EndlicheMenge.erzeuge(GraphPunkt(80f, 260f))
        val gleich = MathematikKnotenVorlagen.Gleichheit.erzeuge(GraphPunkt(450f, 170f))
        return KarteBauer("Zahl und Menge verbinden").knoten(zahl,menge,gleich).verbinde(zahl,"wert",gleich,"links").verbinde(menge,"menge",gleich,"rechts").baue()
    }

    private fun gruppenKnoten(karte: KartenDaten, position: GraphPunkt): KnotenDaten {
        val eingänge = karte.knoten.filter { it.art == "mathematik.kartenEingang" }.mapIndexed { i, k -> AnschlussDaten(
            name = k.parameter["name"] ?: k.name, richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links,
            art = k.anschlüsse.first { it.name == "wert" && it.richtung == AnschlussRichtung.Ausgang }.art, reihenfolge = i,
        ) }
        val ausgänge = karte.knoten.filter { it.art == "mathematik.kartenAusgang" }.mapIndexed { i, k -> AnschlussDaten(
            name = k.parameter["name"] ?: k.name, richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts,
            art = k.anschlüsse.first { it.name == "wert" && it.richtung == AnschlussRichtung.Eingang }.art, reihenfolge = i,
        ) }
        return KnotenDaten(art = "mathematik.gruppe", name = karte.name, position = position, anschlüsse = eingänge + ausgänge, kartenVerweis = KartenVerweis(karte.id, karte.version))
    }

    private class KarteBauer(name: String) {
        private var karte = KartenDaten(name = name)
        fun knoten(vararg k: KnotenDaten) = apply { karte = karte.copy(knoten = karte.knoten + k) }
        fun verbinde(von: KnotenDaten, vonName: String, zu: KnotenDaten, zuName: String) = apply {
            val a = findeAnschluss(von, vonName, AnschlussRichtung.Ausgang)
            val b = findeAnschluss(zu, zuName, AnschlussRichtung.Eingang)
            karte = karte.copy(verbindungen = karte.verbindungen + VerbindungDaten(von = AnschlussVerweis(von.id,a.id), zu = AnschlussVerweis(zu.id,b.id)))
        }
        fun baue() = karte

        private fun findeAnschluss(
            knoten: KnotenDaten,
            name: String,
            richtung: AnschlussRichtung,
        ): AnschlussDaten = requireNotNull(
            knoten.anschlüsse.firstOrNull { it.name == name && it.richtung == richtung },
        ) {
            val vorhanden = knoten.anschlüsse.joinToString { "${it.name}:${it.richtung}" }
            "Knoten '${knoten.name}' (${knoten.art}) besitzt keinen $richtung-Anschluss '$name'. Vorhanden: $vorhanden"
        }
    }
}

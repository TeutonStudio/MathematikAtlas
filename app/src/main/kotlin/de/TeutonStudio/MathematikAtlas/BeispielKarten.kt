package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen

object BeispielKarten {
    fun alle(): List<KartenDaten> {
        val doppeln = doppelnKarte()
        return listOf(doppeln, rechnenKarte(doppeln), aussageKarte(), mengenKarte(), verbindungsKarte())
    }

    private fun rechnenKarte(doppeln: KartenDaten): KartenDaten {
        val zwei = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt(80f, 100f)).copy(name = "Zwei", parameter = mapOf("wert" to "2"))
        val drei = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt(80f, 260f)).copy(name = "Drei", parameter = mapOf("wert" to "3"))
        val plus = MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt(390f, 160f))
        val auswerten = MathematikKnotenVorlagen.Auswerten.erzeuge(GraphPunkt(730f, 170f))
        val gruppe = gruppenKnoten(doppeln, GraphPunkt(1050f, 170f))
        return KarteBauer("Rechnen").knoten(zwei, drei, plus, auswerten, gruppe)
            .verbinde(zwei, "wert", plus, "a").verbinde(drei, "wert", plus, "b")
            .verbinde(plus, "wert", auswerten, "objekt").verbinde(auswerten, "wert", gruppe, "x").baue()
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
        return KarteBauer("Aussage").knoten(a,b,gleich,aus).verbinde(a,"wert",gleich,"links").verbinde(b,"wert",gleich,"rechts").verbinde(gleich,"aussage",aus,"objekt").baue()
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
            art = k.anschlüsse.first { it.name == "wert" }.art, reihenfolge = i,
        ) }
        val ausgänge = karte.knoten.filter { it.art == "mathematik.kartenAusgang" }.mapIndexed { i, k -> AnschlussDaten(
            name = k.parameter["name"] ?: k.name, richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts,
            art = k.anschlüsse.first { it.name == "wert" }.art, reihenfolge = i,
        ) }
        return KnotenDaten(art = "mathematik.gruppe", name = karte.name, position = position, anschlüsse = eingänge + ausgänge, kartenVerweis = KartenVerweis(karte.id, karte.version))
    }

    private class KarteBauer(name: String) {
        private var karte = KartenDaten(name = name)
        fun knoten(vararg k: KnotenDaten) = apply { karte = karte.copy(knoten = karte.knoten + k) }
        fun verbinde(von: KnotenDaten, vonName: String, zu: KnotenDaten, zuName: String) = apply {
            val a = von.anschlüsse.first { it.name == vonName }; val b = zu.anschlüsse.first { it.name == zuName }
            karte = karte.copy(verbindungen = karte.verbindungen + VerbindungDaten(von = AnschlussVerweis(von.id,a.id), zu = AnschlussVerweis(zu.id,b.id)))
        }
        fun baue() = karte
    }
}

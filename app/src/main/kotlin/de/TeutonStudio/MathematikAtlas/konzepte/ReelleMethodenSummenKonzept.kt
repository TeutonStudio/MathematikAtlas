package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.ErweiterteMathematikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen

/** Definitionskarten für Unter- und Obersummen reeller Methoden. */
object ReelleMethodenSummenKonzept {
    val definition: KonzeptDefinition by lazy {
        val unten = karte("untersumme")
        val oben = karte("obersumme")
        KonzeptDefinition(
            id = KonzeptId("reelle-methodensumme"),
            name = "Reelle Methodenober- und -untersumme",
            beschreibung = "Für f:A→B mit A,B⊆ℝ wird jedes Teilintervall durch sein Minimum beziehungsweise Maximum angenähert und mit seiner Breite multipliziert.",
            pfad = listOf("Analysis", "Riemann-Summen"),
            tags = setOf("Untersumme", "Obersumme", "Partition", "Minimum", "Maximum", "Riemann"),
            knotenArten = setOf("mathematik.reelleMethodenSumme"),
            reiter = listOf(
                KonzeptReiter("definition", "Untersumme", KonzeptReiterRolle.Definition, unten),
                KonzeptReiter("obersumme", "Obersumme", KonzeptReiterRolle.Spezialfall, oben),
            ),
        )
    }

    private fun karte(art: String): KartenDaten {
        val methode = schnittstelle("summen-$art-methode", "f:A→B", MathematikAnschlussArten.Funktion.id, true, 30f, 60f)
        val partitionen = zahl("summen-$art-n", "4", 30f, 220f)
        val minimum = zahl("summen-$art-min", "0", 30f, 370f)
        val maximum = zahl("summen-$art-max", "1", 30f, 520f)
        val summe = knoten(ErweiterteMathematikKnotenVorlagen.ReelleMethodenSumme, "summen-$art-knoten", 390f, 210f).copy(
            parameter = mapOf("summenArt" to art, "bereichsArt" to "grenzen"),
        )
        return KartenDaten(
            id = KartenId("konzept-reelle-methodensumme-$art"),
            name = if (art == "untersumme") "Untersumme über Teilintervall-Minima" else "Obersumme über Teilintervall-Maxima",
            knoten = listOf(methode, partitionen, minimum, maximum, summe),
            verbindungen = listOf(
                verbindung("summen-$art-1", methode, "wert", summe, "methode"),
                verbindung("summen-$art-2", partitionen, "wert", summe, "partitionen"),
                verbindung("summen-$art-3", minimum, "wert", summe, "minimum"),
                verbindung("summen-$art-4", maximum, "wert", summe, "maximum"),
            ),
        )
    }

    private fun zahl(id: String, wert: String, x: Float, y: Float) =
        knoten(MathematikKnotenVorlagen.Zahl, id, x, y).copy(parameter = mapOf("wert" to wert))

    private fun schnittstelle(id: String, name: String, art: AnschlussArtId, ausgang: Boolean, x: Float, y: Float): KnotenDaten {
        val basis = knoten(if (ausgang) MathematikKnotenVorlagen.KartenEingang else MathematikKnotenVorlagen.KartenAusgang, id, x, y)
        return basis.copy(
            parameter = mapOf("name" to name),
            anschlüsse = listOf(AnschlussDaten(
                id = AnschlussId("$id-wert"), name = "wert",
                richtung = if (ausgang) AnschlussRichtung.Ausgang else AnschlussRichtung.Eingang,
                kante = if (ausgang) AnschlussKante.Rechts else AnschlussKante.Links,
                art = art,
            )),
        )
    }

    private fun knoten(vorlage: KnotenVorlage, id: String, x: Float, y: Float): KnotenDaten {
        val k = vorlage.erzeuge(GraphPunkt(x, y))
        return k.copy(id = KnotenId(id), anschlüsse = k.anschlüsse.map { it.copy(id = AnschlussId("$id-${it.name}-${it.reihenfolge}")) })
    }

    private fun verbindung(id: String, von: KnotenDaten, vonName: String, zu: KnotenDaten, zuName: String) = VerbindungDaten(
        id = VerbindungsId(id),
        von = AnschlussVerweis(von.id, von.anschlüsse.first { it.name == vonName }.id),
        zu = AnschlussVerweis(zu.id, zu.anschlüsse.first { it.name == zuName }.id),
    )
}

package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.Aussage
import de.TeutonStudio.MathematikRechenSystem.kern.Disjunktion as DisjunktionAussage
import de.TeutonStudio.MathematikRechenSystem.kern.Implikation as ImplikationAussage
import de.TeutonStudio.MathematikRechenSystem.kern.Konjunktion as KonjunktionAussage
import de.TeutonStudio.MathematikRechenSystem.kern.Negation as NegationsAussage
import de.TeutonStudio.MathematikRechenSystem.kern.UnentscheidbareAussage
import de.TeutonStudio.MathematikRechenSystem.kern.WahrheitsKonstante
import de.TeutonStudio.MathematikRechenSystem.kern.Wahrheitswert
import de.TeutonStudio.MathematikRechenSystem.kern.adjunktion
import de.TeutonStudio.MathematikRechenSystem.kern.Äquivalenz as ÄquivalenzAussage
import java.math.BigInteger

const val AUSSAGEN_LOGIK_SEMANTIK = "logikSemantik"
const val AUSSAGEN_LOGIK_XOR = "xor"

/** Korrigierte Vorlagen der Aussagenlogik, ohne den historischen Katalog zu duplizieren. */
object AussagenLogikKnotenVorlagen {
    private fun eingang(name: String, reihe: Int = 0, erweiterbar: Boolean = false) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Aussage.id,
        reihenfolge = reihe,
        kannSichErweitern = erweiterbar,
    )

    private fun ausgang() = AnschlussDaten(
        name = "aussage",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = MathematikAnschlussArten.Aussage.id,
    )

    val Negation = KnotenVorlage(
        art = "mathematik.negation",
        name = "Negation",
        kategorie = "Aussagen: Aussagenlogik",
        beschreibung = "Kehrt den Wahrheitswert einer Aussage mit ¬ um.",
        standardGröße = GraphGröße(210f, 100f),
        anschlüsse = listOf(eingang("aussage"), ausgang()),
    )

    val Adjunktion = KnotenVorlage(
        art = "mathematik.adjunktion",
        name = "Adjunktion",
        kategorie = "Aussagen: Aussagenlogik",
        beschreibung = "Bildet das ausschließende Oder: (a ∨ b) ∧ ¬(a ∧ b).",
        standardGröße = GraphGröße(240f, 115f),
        anschlüsse = listOf(eingang("a", 0, true), eingang("b", 1, true), ausgang()),
        standardParameter = mapOf(
            "festeEingänge" to "2",
            "operatorAnzeige" to "wert",
            AUSSAGEN_LOGIK_SEMANTIK to AUSSAGEN_LOGIK_XOR,
        ),
    )

    val IterierteAdjunktion = KnotenVorlage(
        art = MathematikKnotenVorlagen.ITERIERTE_AUSSAGENVERKNÜPFUNG_ART,
        name = "Iterierte Adjunktion",
        kategorie = "Operatoren",
        beschreibung = "Verknüpft die Aussagenwerte einer Methode über einer Indexmenge als iteriertes ausschließendes Oder.",
        standardGröße = GraphGröße(290f, 120f),
        anschlüsse = listOf(
            AnschlussDaten(
                name = "methode",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.AussageMethode.id,
            ),
            AnschlussDaten(
                name = "indexmenge",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Menge.id,
                reihenfolge = 1,
            ),
            ausgang(),
        ),
        standardParameter = mapOf(
            "operator" to "adjunktion",
            AUSSAGEN_LOGIK_SEMANTIK to AUSSAGEN_LOGIK_XOR,
        ),
    )

    val alle = listOf(Negation, Adjunktion, IterierteAdjunktion)
}

private fun vorlagenSchlüssel(vorlage: KnotenVorlage): Pair<String, String> =
    vorlage.art to (vorlage.standardParameter["operator"] ?: vorlage.name)

/**
 * Ersetzt korrigierte Varianten, entfernt historische Zahlrechnerarten aus dem
 * Erstellen-Dialog und hängt additive Knotendomänen an.
 */
fun alleMathematikKnotenVorlagen(): List<KnotenVorlage> {
    val ersatz = AussagenLogikKnotenVorlagen.alle.associateBy(::vorlagenSchlüssel)
    val vorhandeneSchlüssel = MathematikKnotenVorlagen.alle.mapTo(mutableSetOf(), ::vorlagenSchlüssel)
    val basis = MathematikKnotenVorlagen.alle
        .filterNot { it.art in historischeZahlenRechnerArten }
        .map { vorlage -> ersatz[vorlagenSchlüssel(vorlage)] ?: vorlage } +
        AussagenLogikKnotenVorlagen.alle.filter { vorlagenSchlüssel(it) !in vorhandeneSchlüssel }
    return (
        basis +
            ZahlenRechnerKnotenVorlagen.alle +
            FaltungsKnotenVorlagen.alle +
            MatrixdiagonaleKnotenVorlagen.alle
        )
        .distinctBy(::vorlagenSchlüssel)
}

enum class AussagenOperatorArt(
    val knotenArt: String,
    val titel: String,
    val minimum: Int,
    val maximum: Int?,
    val operatorLatex: String,
) {
    Negation("mathematik.negation", "Negation", 1, 1, "\\neg"),
    Konjunktion("mathematik.konjunktion", "Konjunktion", 2, null, "\\land"),
    Disjunktion("mathematik.disjunktion", "Disjunktion", 2, null, "\\lor"),
    Adjunktion("mathematik.adjunktion", "Adjunktion", 2, null, "\\stackrel{\\bullet}{\\lor}"),
    Implikation("mathematik.implikation", "Implikation", 2, 2, "\\Rightarrow"),
    Äquivalenz("mathematik.äquivalenz", "Äquivalenz", 2, 2, "\\Leftrightarrow"),
    ;

    fun erzeuge(aussagen: List<Aussage>): Aussage {
        require(aussagen.size >= minimum && (maximum == null || aussagen.size <= maximum)) {
            "$titel benötigt ${maximum?.let { "genau $it" } ?: "mindestens $minimum"} Aussageeingänge."
        }
        return when (this) {
            Negation -> NegationsAussage(aussagen.single())
            Konjunktion -> KonjunktionAussage(aussagen)
            Disjunktion -> DisjunktionAussage(aussagen)
            Adjunktion -> adjunktion(aussagen)
            Implikation -> ImplikationAussage(aussagen[0], aussagen[1])
            Äquivalenz -> ÄquivalenzAussage(aussagen[0], aussagen[1])
        }
    }

    fun ergebnisLatex(anzahl: Int): String {
        val namen = (1..anzahl).map { "A_{$it}" }
        return if (this == Negation) "\\neg ${namen.single()}" else namen.joinToString(" $operatorLatex ")
    }

    companion object {
        fun für(knoten: KnotenDaten): AussagenOperatorArt? = entries.firstOrNull { it.knotenArt == knoten.art }
    }
}

data class WahrheitstabellenZeile(
    val index: BigInteger,
    val eingänge: List<Boolean>,
    val ergebnis: Boolean,
)

/** Berechnet Zeilen adressierbar, ohne bei 2^n Kombinationen eine vollständige Liste anzulegen. */
class Wahrheitstabelle(
    val art: AussagenOperatorArt,
    val anzahlEingänge: Int,
) {
    init {
        require(anzahlEingänge >= art.minimum)
        require(art.maximum == null || anzahlEingänge <= art.maximum)
    }

    val zeilenAnzahl: BigInteger = BigInteger.ONE.shiftLeft(anzahlEingänge)

    fun zeile(index: BigInteger): WahrheitstabellenZeile {
        require(index.signum() >= 0 && index < zeilenAnzahl)
        val eingänge = List(anzahlEingänge) { position ->
            !index.testBit(anzahlEingänge - position - 1)
        }
        val aussage = art.erzeuge(eingänge.map(::WahrheitsKonstante))
        val ergebnis = aussage.entscheide().wahrheitswert == Wahrheitswert.Wahr
        return WahrheitstabellenZeile(index, eingänge, ergebnis)
    }
}

internal fun MathematikAuswerterRegister.registriereAussagenLogikKnoten() {
    registriere("mathematik.negation") { kontext ->
        val aussage = kontext.eingänge["aussage"]?.objekt as? Aussage ?: error("Aussage fehlt.")
        KnotenAuswertungsErgebnis(mapOf("aussage" to BedingterWert(NegationsAussage(aussage), kontext.annahmen())))
    }
    registriere("mathematik.adjunktion") { kontext ->
        val aussagen = kontext.operatorEingänge { _, index -> UnentscheidbareAussage("A_$index", "unverbunden") }
            .map { it.objekt as? Aussage ?: error("Aussageneingang ist ungültig.") }
        require(aussagen.size >= 2) { "Adjunktion benötigt mindestens zwei Aussageeingänge." }
        KnotenAuswertungsErgebnis(mapOf("aussage" to BedingterWert(adjunktion(aussagen), kontext.annahmen())))
    }
}

private fun KnotenAuswertungsKontext.annahmen() = eingänge.values.flatMap { it.annahmen }.toSet()

package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.MengenAusdruck
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.Tensorartig
import de.TeutonStudio.MathematikRechenSystem.kern.Tupel
import de.TeutonStudio.MathematikRechenSystem.kern.ZahlAusdruck
import de.TeutonStudio.MathematikRechenSystem.kern.addition
import de.TeutonStudio.MathematikRechenSystem.kern.iterierteSumme
import de.TeutonStudio.MathematikRechenSystem.kern.spur

const val SPUR_ART = "mathematik.spur"
const val ITERIERTE_SUMME_TUPEL_MODUS = "tupel"

object SpurKnotenVorlagen {
    val Spur = KnotenVorlage(
        art = SPUR_ART,
        name = "Spur",
        kategorie = "Matrizen",
        beschreibung = "Summiert die Hauptdiagonale einer quadratischen Matrix.",
        standardGröße = GraphGröße(230f, 105f),
        anschlüsse = listOf(
            AnschlussDaten(
                name = "matrix",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Matrix.id,
            ),
            AnschlussDaten(
                name = "spur",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Zahl.id,
            ),
        ),
    )

    /** Alternative Eingabeform des vorhandenen Summenknotens für kartesische Zahlentupel. */
    val IterierteSummeTupel = KnotenVorlage(
        art = MathematikKnotenVorlagen.IterierteSumme.art,
        name = "Iterierte Summe (Tupel)",
        kategorie = "Operatoren",
        beschreibung = "Summiert die Komponenten eines kartesischen Zahlentupels in ihrer Reihenfolge.",
        standardGröße = GraphGröße(260f, 105f),
        anschlüsse = listOf(
            AnschlussDaten(
                name = "tupel",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Tupel.id,
            ),
            AnschlussDaten(
                name = "wert",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Zahl.id,
            ),
        ),
        standardParameter = mapOf("eingabeModus" to ITERIERTE_SUMME_TUPEL_MODUS),
    )

    val alle = listOf(Spur, IterierteSummeTupel)
}

/** Registriert die Spur und erweitert die vorhandene iterierte Summe rückwärtskompatibel um Tupel. */
internal fun MathematikAuswerterRegister.registriereSpurUndTupelsumme() {
    registriere(SPUR_ART) { kontext ->
        val eingang = kontext.eingänge["matrix"] ?: error("Matrixeingang fehlt.")
        val matrix = eingang.objekt as? Tensorartig ?: error("Der Eingang ist keine Matrix.")
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf("spur" to BedingterWert(spur(matrix), eingang.annahmen)),
            eingänge = kontext.eingänge,
        )
    }

    registriere(MathematikKnotenVorlagen.IterierteSumme.art) { kontext ->
        val tupelWert = kontext.eingänge["tupel"]
        val ergebnis = if (tupelWert != null) {
            val tupel = tupelWert.objekt as? Tupel ?: error("Der Tupel-Eingang enthält kein Tupel.")
            addition(tupel.elemente.mapIndexed { index, element ->
                element as? ZahlAusdruck
                    ?: error("Tupelkomponente ${index + 1} ist keine Zahl.")
            })
        } else {
            val methode = kontext.eingänge["methode"]?.objekt as? Methode ?: error("Zahlfunktion fehlt.")
            val indexMenge = kontext.eingänge["indexmenge"]?.objekt as? MengenAusdruck ?: error("Indexmenge fehlt.")
            iterierteSumme(methode, indexMenge)
        }
        val annahmen = kontext.eingänge.values.flatMap { it.annahmen }.toSet()
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf("wert" to BedingterWert(ergebnis, annahmen)),
            eingänge = kontext.eingänge,
        )
    }
}

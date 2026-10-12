package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val ÜBERSETZER_ART = "atlas.uebersetzer"
const val TUPEL_ABFLACHEN_ART = "mathematik.tupelAbflachen"
const val ÜBERSETZER_ZIEL = "uebersetzer.ziel"
val ÜBERSETZER_ZIELE = listOf("tupel", "zeile", "spalte", "methode")

fun übersetzerZielTitel(ziel: String): String = when (ziel) {
    "tupel" -> "Tupel"
    "zeile" -> "Zeilenvektor"
    "spalte" -> "Spaltenvektor"
    "methode" -> "Term → Methode"
    else -> ziel
}

fun übersetzerZiel(knoten: KnotenDaten): String =
    (knoten.eigenschaften[ÜBERSETZER_ZIEL] as? KnotenEigenschaft.Text)?.wert ?: "spalte"

fun konfiguriereÜbersetzer(knoten: KnotenDaten, ziel: String): KnotenDaten {
    require(knoten.art == ÜBERSETZER_ART && ziel in ÜBERSETZER_ZIELE) { "Ungültiges Übersetzungsziel '$ziel'." }
    val ausgangArt = when (ziel) {
        "tupel" -> MathematikAnschlussArten.Tupel.id
        "zeile" -> MathematikAnschlussArten.ZeilenVektor.id
        "spalte" -> MathematikAnschlussArten.SpaltenVektor.id
        else -> MathematikAnschlussArten.Methode.id
    }
    return knoten.copy(eigenschaften = knoten.eigenschaften + (ÜBERSETZER_ZIEL to KnotenEigenschaft.Text(ziel)),
        anschlüsse = knoten.anschlüsse.map { p -> if (p.richtung == AnschlussRichtung.Ausgang)
            p.copy(art = ausgangArt, kannSichErweitern = false, dynamischErzeugt = false)
            else p.copy(art = MathematikAnschlussArten.Objekt.id,
                zulässigeArten = when (ziel) {
                    "tupel" -> setOf(MathematikAnschlussArten.Tupel.id, MathematikAnschlussArten.ZeilenVektor.id, MathematikAnschlussArten.SpaltenVektor.id)
                    "zeile" -> setOf(MathematikAnschlussArten.Tupel.id, MathematikAnschlussArten.ZeilenVektor.id, MathematikAnschlussArten.SpaltenVektor.id)
                    "spalte" -> setOf(MathematikAnschlussArten.Tupel.id, MathematikAnschlussArten.ZeilenVektor.id, MathematikAnschlussArten.SpaltenVektor.id)
                    else -> emptySet()
                }, kannSichErweitern = false, dynamischErzeugt = false) })
}


const val RICHTUNG_WINKEL_ART = "mathematik.richtungWinkel"
const val RICHTUNG_WINKEL_VORZEICHEN = "richtung.vorzeichen"
const val RICHTUNG_WINKEL_LISTE = "richtung.winkelGrad"

/** Eine Richtung in R^n; n = Anzahl der Halbkreiswinkel + 1. */
val RichtungWinkelVorlage = KnotenVorlage(
    RICHTUNG_WINKEL_ART, "Richtung (n-dimensional)", "Vektoren",
    "R¹: ±1; jede zusätzliche Dimension fügt einen Halbkreiswinkel von 0° bis unter 180° hinzu.",
    GraphGröße(250f, 120f),
    listOf(AnschlussDaten(name = "richtung", richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.SpaltenVektor.id)),
    standardParameter = mapOf(RICHTUNG_WINKEL_VORZEICHEN to "1", RICHTUNG_WINKEL_LISTE to ""),
)

object ÜbersetzungsKnotenVorlagen {
    val Übersetzer = KnotenVorlage(ÜBERSETZER_ART, "Übersetzer", "Strukturen",
        "Übersetzt Tupel, Zeilen, Spalten oder einen Term in eine Methode.", GraphGröße(220f, 96f),
        listOf(AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Objekt.id, zulässigeArten = setOf(MathematikAnschlussArten.Tupel.id, MathematikAnschlussArten.ZeilenVektor.id, MathematikAnschlussArten.SpaltenVektor.id)),
            AnschlussDaten(name = "ergebnis", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.SpaltenVektor.id)),
        standardEigenschaften = mapOf(ÜBERSETZER_ZIEL to KnotenEigenschaft.Text("spalte"))).alsAdapterVorlage()
    val Abflachen = KnotenVorlage(TUPEL_ABFLACHEN_ART, "Tupel abflachen", "Strukturen",
        "Öffnet verschachtelte Tupel rekursiv von links nach rechts; andere Elemente bleiben erhalten.", GraphGröße(240f, 110f),
        listOf(AnschlussDaten(name = "tupel", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Tupel.id),
            AnschlussDaten(name = "tupel", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Tupel.id)))
    val alle = listOf(Übersetzer, Abflachen, RichtungWinkelVorlage)
}

/** Gemeinsame Methodenbildung für den historischen Adapter und den Übersetzer. */
internal fun termAlsMethode(k: KnotenAuswertungsKontext, termWert: BedingterWert): BedingterWert {
    val term = termWert.mathematischesObjekt("Term der Methodenbildung")
    require(term !is Methode) { "Eine Methode ist kein Term für die Methodenbildung." }
    val freie = term.freieMethodenParameter().associateBy { it.name }
    val quellen = termWert.variablenQuellen.filter { it.name in freie }.groupBy { it.name }
    val fehlende = freie.keys - quellen.keys
    require(fehlende.isEmpty()) { "Für die freien Parameter ${fehlende.sorted().joinToString(", ")} fehlt eine Parameter- oder Außenquelle." }
    val vorräte = quellen.mapValues { (name, qs) ->
        require(qs.map { it.werteVorrat }.distinct().size == 1 && qs.map { it.alsMethodenParameter }.distinct().size == 1) {
            "Die gleichnamigen Quellen für '$name' widersprechen einander."
        }
        qs.first().werteVorrat
    }
    val bindbare = quellen.filterValues { it.first().alsMethodenParameter }.keys
    val gespeichert = k.knoten.parameter["argumentReihenfolge"].orEmpty().split(',').map(String::trim)
        .filter { it in bindbare }.distinct()
    val namen = gespeichert + (bindbare - gespeichert.toSet()).sorted()
    val ziel = inferiereZielmenge(term, vorräte, termWert.annahmen)
    val ausgabeNamen = if (term is Tupel) term.elemente.indices.map { "element${it + 1}" } else listOf("wert")
    val methode = MathematischeMethode(name = k.knoten.parameter["name"] ?: "f", parameter = namen.map { name ->
        if (quellen.getValue(name).first().lösungsRolle == LösungsQuellenRolle.Parameter)
            AllgemeinerParameter(name) else freie.getValue(name)
    },
        vorschrift = term, zielMenge = ziel, werteVorräte = namen.associateWith { vorräte.getValue(it) }, ausgabeNamen = ausgabeNamen)
    return termWert.copy(objekt = methode, zielMenge = null, werteVorrat = null, latexDarstellung = null,
        reelleVariablen = termWert.reelleVariablen.filterKeys { it !in namen },
        variablenQuellen = termWert.variablenQuellen.filterNot { it.name in namen && it.alsMethodenParameter })
}

fun MathematikAuswerterRegister.registriereÜbersetzungsKnoten() {
    registriere(RICHTUNG_WINKEL_ART) { k ->
        val vorzeichen = k.knoten.parameter[RICHTUNG_WINKEL_VORZEICHEN]?.toIntOrNull()
            ?: error("Richtungsvorzeichen muss -1 oder +1 sein.")
        val roh = k.knoten.parameter[RICHTUNG_WINKEL_LISTE].orEmpty()
        val winkel = if (roh.isBlank()) emptyList() else roh.split(';').map { eintrag ->
            eintrag.trim().toDoubleOrNull() ?: error("Ungültiger Halbkreiswinkel: '$eintrag'.")
        }
        KnotenAuswertungsErgebnis(mapOf("richtung" to BedingterWert(richtungAlsSpaltenVektor(vorzeichen, winkel))))
    }
    registriere(ÜBERSETZER_ART) { k ->
        val wert = k.eingänge["wert"] ?: error("Der zu übersetzende Wert fehlt.")
        val ziel = übersetzerZiel(k.knoten)
        require(ziel in ÜBERSETZER_ZIELE) { "Unbekanntes Übersetzungsziel '$ziel'." }
        val ergebnis = if (ziel == "methode") termAlsMethode(k, wert)
            else übersetzterWert(wert, übersetzeStruktur(wert.mathematischesObjekt("Übersetzer"), ziel))
        KnotenAuswertungsErgebnis(mapOf("ergebnis" to ergebnis))
    }
    registriere(TUPEL_ABFLACHEN_ART) { k ->
        val wert = k.eingänge["tupel"] ?: error("Tupel fehlt.")
        val tupel = wert.objekt as? Tupel ?: error("Abflachen benötigt ein Tupel.")
        KnotenAuswertungsErgebnis(mapOf("tupel" to übersetzterWert(wert, tupelAbflachen(tupel))))
    }
}

private fun übersetzterWert(quelle: BedingterWert, ergebnis: MathematischesObjekt): BedingterWert = quelle.copy(
    objekt = ergebnis, latexDarstellung = null, werteVorrat = null, elementArt = null,
    zielMenge = inferiereZielmenge(ergebnis, quelle.variablenQuellen.associate { it.name to it.werteVorrat }, quelle.annahmen),
)

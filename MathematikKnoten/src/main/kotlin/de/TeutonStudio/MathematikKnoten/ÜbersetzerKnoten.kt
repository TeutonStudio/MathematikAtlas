package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val ÜBERSETZER_ART = "atlas.uebersetzer"
const val TUPEL_ABFLACHEN_ART = "mathematik.tupelAbflachen"
const val ÜBERSETZER_ZIEL = "uebersetzer.ziel"
val ÜBERSETZER_ZIELE = listOf("tupel", "zeile", "spalte", "methode")

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
                    "tupel" -> setOf(MathematikAnschlussArten.ZeilenVektor.id, MathematikAnschlussArten.SpaltenVektor.id)
                    "zeile" -> setOf(MathematikAnschlussArten.Tupel.id, MathematikAnschlussArten.SpaltenVektor.id)
                    "spalte" -> setOf(MathematikAnschlussArten.Tupel.id, MathematikAnschlussArten.ZeilenVektor.id)
                    else -> emptySet()
                }, kannSichErweitern = false, dynamischErzeugt = false) })
}

object ÜbersetzungsKnotenVorlagen {
    val Übersetzer = KnotenVorlage(ÜBERSETZER_ART, "Übersetzer", "Strukturen",
        "Übersetzt Tupel, Zeilen, Spalten oder einen Term in eine Methode.", GraphGröße(220f, 96f),
        listOf(AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Objekt.id, zulässigeArten = setOf(MathematikAnschlussArten.Tupel.id, MathematikAnschlussArten.ZeilenVektor.id)),
            AnschlussDaten(name = "ergebnis", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.SpaltenVektor.id)),
        standardEigenschaften = mapOf(ÜBERSETZER_ZIEL to KnotenEigenschaft.Text("spalte"))).alsAdapterVorlage()
    val Abflachen = KnotenVorlage(TUPEL_ABFLACHEN_ART, "Tupel abflachen", "Strukturen",
        "Öffnet verschachtelte Tupel rekursiv von links nach rechts; andere Elemente bleiben erhalten.", GraphGröße(240f, 110f),
        listOf(AnschlussDaten(name = "tupel", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Tupel.id),
            AnschlussDaten(name = "tupel", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Tupel.id)))
    val alle = listOf(Übersetzer, Abflachen)
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
    val methode = MathematischeMethode(name = k.knoten.parameter["name"] ?: "f", parameter = namen.map { freie.getValue(it) },
        vorschrift = term, zielMenge = ziel, werteVorräte = namen.associateWith { vorräte.getValue(it) }, ausgabeNamen = ausgabeNamen)
    return termWert.copy(objekt = methode, zielMenge = null, werteVorrat = null, latexDarstellung = null,
        reelleVariablen = termWert.reelleVariablen.filterKeys { it !in namen },
        variablenQuellen = termWert.variablenQuellen.filterNot { it.name in namen && it.alsMethodenParameter })
}

fun MathematikAuswerterRegister.registriereÜbersetzungsKnoten() {
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

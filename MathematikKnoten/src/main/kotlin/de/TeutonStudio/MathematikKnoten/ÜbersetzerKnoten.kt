package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val ÜBERSETZER_ART = "atlas.uebersetzer"
const val TUPEL_ABFLACHEN_ART = "mathematik.tupelAbflachen"
const val ÜBERSETZER_ZIEL = "uebersetzer.ziel"
const val ÜBERSETZER_POLAR_GETRENNT = "uebersetzer.polarGetrennt"
fun übersetzerPolarGetrennt(knoten: KnotenDaten) = (knoten.eigenschaften[ÜBERSETZER_POLAR_GETRENNT] as? KnotenEigenschaft.Text)?.wert == "true"

fun konfiguriereÜbersetzerPolarAnschlüsse(knoten: KnotenDaten, getrennt: Boolean): KnotenDaten {
    require(knoten.art == ÜBERSETZER_ART) { "Nur Übersetzer unterstützen die Polaraufteilung." }
    val aktualisiert = knoten.copy(eigenschaften = knoten.eigenschaften + (ÜBERSETZER_POLAR_GETRENNT to KnotenEigenschaft.Text(getrennt.toString())))
    return konfiguriereÜbersetzer(aktualisiert, übersetzerZiel(aktualisiert))
}
val ÜBERSETZER_ZIELE = listOf("tupel", "zeile", "spalte", "methode", "polar", "kartesisch")

fun übersetzerZielTitel(ziel: String): String = when (ziel) {
    "tupel" -> "Tupel"
    "zeile" -> "Zeilenvektor"
    "spalte" -> "Spaltenvektor"
    "methode" -> "Term → Methode"
    "polar" -> "Kartesisch → Polar"
    "kartesisch" -> "Polar → Kartesisch"
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
        "polar", "kartesisch" -> MathematikAnschlussArten.Tupel.id
        else -> MathematikAnschlussArten.Methode.id
    }
    val getrennt = übersetzerPolarGetrennt(knoten)
    val spezial = when {
        ziel == "kartesisch" && getrennt -> listOf(
            AnschlussDaten(name = "radius", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id),
            AnschlussDaten(name = "winkel", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Tupel.id),
            knoten.anschlüsse.firstOrNull { it.name == "ergebnis" }?.copy(art = MathematikAnschlussArten.Tupel.id)
                ?: AnschlussDaten(name = "ergebnis", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Tupel.id),
        )
        ziel == "polar" && getrennt -> listOf(
            knoten.anschlüsse.firstOrNull { it.name == "wert" }?.copy(art = MathematikAnschlussArten.Objekt.id)
                ?: AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Objekt.id),
            AnschlussDaten(name = "radius", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Zahl.id),
            AnschlussDaten(name = "winkel", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Tupel.id),
        )
        else -> null
    }
    val grund = if (spezial != null) spezial else listOf(
        knoten.anschlüsse.firstOrNull { it.name == "wert" } ?: AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Objekt.id),
        knoten.anschlüsse.firstOrNull { it.name == "ergebnis" } ?: AnschlussDaten(name = "ergebnis", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = ausgangArt),
    )
    return knoten.copy(eigenschaften = knoten.eigenschaften + (ÜBERSETZER_ZIEL to KnotenEigenschaft.Text(ziel)),
        anschlüsse = grund.map { p -> if (spezial != null) {
            p.copy(kannSichErweitern = false, dynamischErzeugt = false)
        } else if (p.richtung == AnschlussRichtung.Ausgang)
            p.copy(art = ausgangArt, kannSichErweitern = false, dynamischErzeugt = false)
            else p.copy(art = MathematikAnschlussArten.Objekt.id,
                zulässigeArten = when (ziel) {
                    "tupel" -> setOf(MathematikAnschlussArten.Tupel.id, MathematikAnschlussArten.ZeilenVektor.id, MathematikAnschlussArten.SpaltenVektor.id)
                    "zeile" -> setOf(MathematikAnschlussArten.Tupel.id, MathematikAnschlussArten.ZeilenVektor.id, MathematikAnschlussArten.SpaltenVektor.id)
                    "spalte" -> setOf(MathematikAnschlussArten.Tupel.id, MathematikAnschlussArten.ZeilenVektor.id, MathematikAnschlussArten.SpaltenVektor.id)
                    "polar" -> setOf(MathematikAnschlussArten.Tupel.id, MathematikAnschlussArten.ZeilenVektor.id, MathematikAnschlussArten.SpaltenVektor.id)
                    "kartesisch" -> setOf(MathematikAnschlussArten.Tupel.id)
                    else -> emptySet()
                }, kannSichErweitern = false, dynamischErzeugt = false) })
}


const val RICHTUNG_WINKEL_ART = "mathematik.richtungWinkel"
const val RICHTUNG_WINKEL_VORZEICHEN = "richtung.vorzeichen"
const val RICHTUNG_WINKEL_LISTE = "richtung.winkelGrad"
const val RICHTUNG_WINKEL_ANZAHL = "richtung.n"

/** n natürliche Halbkreiswinkel definieren eine Richtung in R^(n+1). */
val RichtungWinkelVorlage = KnotenVorlage(
    RICHTUNG_WINKEL_ART, "Richtung (n-dimensional)", "Vektoren",
    "n ∈ ℕ₀ legt n Halbkreiswinkel und damit eine Richtung in Rⁿ⁺¹ fest.",
    GraphGröße(250f, 120f),
    listOf(AnschlussDaten(name = "n", richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id),
        AnschlussDaten(name = "richtung", richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.SpaltenVektor.id)),
    standardParameter = mapOf(RICHTUNG_WINKEL_VORZEICHEN to "1", RICHTUNG_WINKEL_LISTE to "", RICHTUNG_WINKEL_ANZAHL to "0"),
)

object ÜbersetzungsKnotenVorlagen {
    val Übersetzer = KnotenVorlage(ÜBERSETZER_ART, "Übersetzer", "Strukturen",
        "Übersetzt kartesische Koordinaten in Polarkoordinaten und zurück; zusätzlich Tupel, Vektoren und Methoden.", GraphGröße(220f, 96f),
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
        val n = k.eingänge["n"]?.let { quelle ->
            val zahl = quelle.objekt as? RationaleZahl
                ?: error("n muss eine konkrete natürliche Zahl sein.")
            require(zahl.nenner == java.math.BigInteger.ONE && zahl.zähler.signum() >= 0) {
                "n muss eine natürliche ganze Zahl einschließlich 0 sein."
            }
            try { zahl.zähler.intValueExact() } catch (_: ArithmeticException) {
                error("n überschreitet den unterstützten Dimensionsbereich.")
            }
        } ?: k.knoten.parameter[RICHTUNG_WINKEL_ANZAHL]?.toIntOrNull()
            ?: error("n muss eine natürliche ganze Zahl sein.")
        require(n in 0..31) { "n muss zwischen 0 und 31 liegen (Dimension 1 bis 32)." }
        require(winkel.size <= n) { "Für n = $n sind höchstens $n Halbkreiswinkel zulässig." }
        // Nicht angegebene Winkel entsprechen einer Rotation um 0 Grad.
        val vollständig = winkel + List(n - winkel.size) { 0.0 }
        KnotenAuswertungsErgebnis(mapOf("richtung" to BedingterWert(richtungAlsSpaltenVektor(vorzeichen, vollständig))))
    }
    registriere(ÜBERSETZER_ART) { k ->
        val ziel = übersetzerZiel(k.knoten)
        val getrennt = übersetzerPolarGetrennt(k.knoten)
        val wert = if (ziel == "kartesisch" && getrennt) {
            val radius = k.eingänge["radius"] ?: error("Radius fehlt.")
            val winkel = k.eingänge["winkel"] ?: error("Winkeltupel fehlt.")
            val tupel = winkel.mathematischesObjekt("Polarwinkel") as? Tupel ?: error("Richtung muss ein Winkeltupel sein.")
            übersetzterWert(radius, Tupel(listOf(radius.mathematischesObjekt("Radius"), tupel))).copy(
                annahmen = radius.annahmen + winkel.annahmen,
                variablenQuellen = radius.variablenQuellen + winkel.variablenQuellen,
            )
        } else k.eingänge["wert"] ?: error("Der zu übersetzende Wert fehlt.")
        require(ziel in ÜBERSETZER_ZIELE) { "Unbekanntes Übersetzungsziel '$ziel'." }
        val ergebnis = if (ziel == "methode") termAlsMethode(k, wert) else {
            val objekt = wert.mathematischesObjekt("Übersetzer")
            val umgewandelt = when (ziel) {
                "polar" -> {
                    val tupel = when (objekt) {
                        is Tupel -> objekt
                        is OrientierterVektor -> Tupel(objekt.werte)
                        else -> error("Kartesisch → Polar benötigt ein Tupel oder einen Vektor.")
                    }
                    Polarkoordinaten.polar(tupel)
                }
                "kartesisch" -> Polarkoordinaten.kartesisch(
                    objekt as? Tupel ?: error("Polar → Kartesisch benötigt ein Zweiertupel aus Radius und Winkeltupel."),
                )
                else -> übersetzeStruktur(objekt, ziel)
            }
            übersetzterWert(wert, umgewandelt)
        }
        if (ziel == "polar" && getrennt) {
            val polar = ergebnis.objekt as? Tupel ?: error("Polar muss ein Zweiertupel sein.")
            KnotenAuswertungsErgebnis(mapOf(
                "radius" to übersetzterWert(ergebnis, polar.elemente[0]),
                "winkel" to übersetzterWert(ergebnis, polar.elemente[1]),
            ))
        } else KnotenAuswertungsErgebnis(mapOf("ergebnis" to ergebnis))
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

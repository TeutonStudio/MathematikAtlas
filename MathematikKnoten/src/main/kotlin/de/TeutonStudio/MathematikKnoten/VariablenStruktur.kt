package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val VARIABLE_STRUKTUR = "variable.struktur"
const val VARIABLE_DIMENSION = "variable.dimension"
const val VARIABLE_STRUKTUR_ZAHL = "zahl"
const val VARIABLE_STRUKTUR_TUPEL = "tupel"
const val VARIABLE_STRUKTUR_WINKEL = "winkeltupel"
const val VARIABLE_STRUKTUR_ZEILE = "zeile"
const val VARIABLE_STRUKTUR_SPALTE = "spalte"

fun variablenStruktur(k: KnotenDaten): String =
    k.parameter[VARIABLE_STRUKTUR]?.takeIf { it in setOf("zahl", "tupel", "winkeltupel", "zeile", "spalte") } ?: "zahl"

fun variablenDimension(k: KnotenDaten): Int =
    (k.parameter[VARIABLE_DIMENSION]?.toIntOrNull() ?: 2).also {
        require(it in 0..64 && (variablenStruktur(k) == VARIABLE_STRUKTUR_WINKEL || it > 0)) {
            "Winkeltupel erlauben 0 bis 64 Winkel; andere Strukturen benötigen 1 bis 64 Komponenten."
        }
    }

fun konfiguriereVariablenStruktur(k: KnotenDaten, art: String): KnotenDaten {
    require(k.art == "mathematik.variable" && art in setOf("zahl", "tupel", "winkeltupel", "zeile", "spalte"))
    val anschlussArt = when (art) {
        "tupel", "winkeltupel" -> MathematikAnschlussArten.Tupel.id
        "zeile" -> MathematikAnschlussArten.ZeilenVektor.id
        "spalte" -> MathematikAnschlussArten.SpaltenVektor.id
        else -> MathematikAnschlussArten.Zahl.id
    }
    return k.copy(parameter = k.parameter + (VARIABLE_STRUKTUR to art),
        anschlüsse = k.anschlüsse.map { if (it.richtung == AnschlussRichtung.Ausgang) it.copy(art = anschlussArt) else it })
}

internal fun variablenStrukturWert(k: KnotenDaten, grundmenge: MengenAusdruck): BedingterWert {
    val name = k.parameter["name"]?.trim().orEmpty().ifBlank { "x" }
    val art = variablenStruktur(k)
    val basis = if (art == VARIABLE_STRUKTUR_WINKEL) grundmenge else variablenWertevorrat(k, grundmenge)
    if (art == "zahl") return BedingterWert(Variable(name), werteVorrat = basis,
        variablenQuellen = listOf(VariablenQuelle(k.id, name, basis, lösungsRolle = LösungsQuellenRolle.Variable)))
    val n = variablenDimension(k)
    // n ist die Anzahl der Winkel; das Vorzeichen ist eine zusätzliche Komponente.
    val anzahl = if (art == VARIABLE_STRUKTUR_WINKEL) n + 1 else n
    val variablen = List(anzahl) { i -> Variable("${name}_${i+1}", "${name}_{${i+1}}") }
    val bereiche = List(anzahl) { i ->
        if (art != VARIABLE_STRUKTUR_WINKEL) basis
        else if (i == 0) EndlicheMenge(setOf(RationaleZahl.von(-1), RationaleZahl.Eins))
        else ReellesIntervall(RationaleZahl.Null, false, RationaleZahl.von(180), true)
    }
    val objekt: MathematischesObjekt = when (art) {
        "zeile" -> ZeilenVektor(variablen)
        "spalte" -> SpaltenVektor(variablen)
        else -> Tupel(variablen)
    }
    val wertevorrat: MengenAusdruck = when (art) {
        "zeile" -> Vektorraum(VektorOrientierung.Zeile, n, basis)
        "spalte" -> Vektorraum(VektorOrientierung.Spalte, n, basis)
        else -> Tupelraum(bereiche)
    }
    return BedingterWert(objekt, werteVorrat = wertevorrat,
        variablenQuellen = variablen.mapIndexed { i, variable ->
            VariablenQuelle(k.id, variable.name, bereiche[i],
                bindungsId = "${k.id.wert}:struktur", bindungsName = "komponente-${i+1}",
                reihenfolge = i, lösungsRolle = LösungsQuellenRolle.Variable)
        })
}

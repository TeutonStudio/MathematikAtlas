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
        require(if(variablenStruktur(k)==VARIABLE_STRUKTUR_WINKEL) it in 0..MAXIMALE_RICHTUNGS_WINKEL else it in 1..64) {
            "Winkeltupel erlauben 0 bis $MAXIMALE_RICHTUNGS_WINKEL Winkel; andere Strukturen benötigen 1 bis 64 Komponenten."
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

/** Ein Grundraumelement besitzt genau eine Quellenidentität, auch nach Projektionen. */
internal fun variablenGrundraumWert(k: KnotenDaten, menge: MengenAusdruck): BedingterWert {
    val grundraum = kanonischerRichtungsraum(menge)
    val name = k.parameter["name"]?.trim().orEmpty().ifBlank { "x" }
    val id = "${k.id.wert}:grundraum-element"
    val intern = "__variable:${k.id.wert}:$name"
    val typ = grundraum.elementTypAusdruck()
    val art = variablenAnschlussArt(typ)
    fun komponentenRäume(raum: MengenAusdruck): List<MengenAusdruck>? {
        raum.kartesischeFaktorenOderNull()?.let { return it }
        if (raum !is EndlicheMenge || raum.elemente.isEmpty() || raum.elemente.any { it !is Tupel }) return null
        val tupel = raum.elemente.filterIsInstance<Tupel>()
        val n = tupel.first().elemente.size
        if (tupel.any { it.elemente.size != n }) return null
        return List(n) { i -> EndlicheMenge(tupel.map { it.elemente[i] }.toSet()) }
    }
    val form = when (grundraum) {
        is Vektorraum -> listOf(RationaleZahl.von(grundraum.dimension.toLong()))
        is Matrizenraum -> listOf(RationaleZahl.von(grundraum.zeilen.toLong()), RationaleZahl.von(grundraum.spalten.toLong()))
        is Tensorraum -> grundraum.dimensionen
        else -> komponentenRäume(grundraum)?.let { listOf(RationaleZahl.von(it.size.toLong())) }
    }
    val parameter = TypisiertesElement(intern, art.wert, latex = name, strukturForm = form?.takeUnless { achsen -> achsen.any { it == RationaleZahl.Null } })
    fun element(raum: MengenAusdruck, quelle: MathematischesObjekt): MathematischesObjekt {
        val faktoren = komponentenRäume(raum)
        if (faktoren != null) return Tupel(faktoren.mapIndexed { i, faktor ->
            val index = RationaleZahl.von((i + 1).toLong())
            val numerisch = variablenAnschlussArt(faktor.elementTypAusdruck()) == MathematikAnschlussArten.Zahl.id ||
                faktor == Gradmenge || faktor is EndlicheMenge && faktor.elemente.all { it is ZahlAusdruck }
            val zugriff = strukturKomponente(quelle, index, numerisch)
            element(faktor, zugriff)
        })
        return when (raum) {
            is Vektorraum -> {
                val werte = List(raum.dimension) { SymbolischeZahlKomponente(quelle, RationaleZahl.von((it + 1).toLong())) }
                if (raum.orientierung == VektorOrientierung.Zeile) ZeilenVektor(werte) else SpaltenVektor(werte)
            }
            is Matrizenraum -> Matrix(List(raum.zeilen) { i -> List(raum.spalten) { j ->
                SymbolischeZahlKomponente(quelle, RationaleZahl.von((i + 1).toLong()), listOf(RationaleZahl.von((j + 1).toLong())))
            } })
            else -> quelle
        }
    }
    // Numerische Skalare bleiben Zahlterme; alle strukturierten Zugriffe teilen den Wurzelparameter.
    val skalar = art == MathematikAnschlussArten.Zahl.id
    val objekt = when {
        skalar -> Variable(name, bindungsId = id)
        grundraum == Gradmenge -> WinkelParameter(name, bindungsId = id)
        else -> element(grundraum, parameter)
    }
    return BedingterWert(objekt, werteVorrat = grundraum,
        komponentenMengen = komponentenRäume(grundraum).orEmpty(),
        variablenQuellen = listOf(VariablenQuelle(k.id, if (skalar || grundraum == Gradmenge) name else intern, grundraum, bindungsId = id,
            lösungsRolle = LösungsQuellenRolle.Variable,
            strukturBindung = StrukturQuellenBindung(id, name, grundraum))))
}

internal fun variablenAnschlussArt(typ: de.TeutonStudio.TypSystem.TypAusdruck): AnschlussArtId {
    val id = when (typ) {
        is de.TeutonStudio.TypSystem.TypAusdruck.Atom -> typ.id
        is de.TeutonStudio.TypSystem.TypAusdruck.Parameterisiert -> typ.konstruktor
        else -> null
    }
    return when {
        id == MathematischeTypen.Tupel -> MathematikAnschlussArten.Tupel.id
        id != null && MathematischeTypen.istAtomUntertyp(id, MathematischeTypen.Zahl) -> MathematikAnschlussArten.Zahl.id
        id != null -> AnschlussArtId(id.wert)
        else -> MathematikAnschlussArten.Objekt.id
    }
}

/** Nur für das Lesen historischer Konfigurationen; nicht für die aktive Auswertung. */
internal fun historischerVariablenGrundraum(k: KnotenDaten, basis: MengenAusdruck): MengenAusdruck {
    val menge = variablenWertevorrat(k, basis)
    return when (variablenStruktur(k)) {
        "winkeltupel" -> winkelraum(variablenDimension(k))
        "tupel" -> Tupelraum(List(variablenDimension(k)) { menge })
        "zeile" -> Vektorraum(VektorOrientierung.Zeile, variablenDimension(k), menge)
        "spalte" -> Vektorraum(VektorOrientierung.Spalte, variablenDimension(k), menge)
        else -> {
            val n = k.parameter[MATRIX_VARIABLE_DIMENSION]?.toIntOrNull() ?: 0
            require(n in 0..8) { "Ungültige historische Matrixdimension." }
            if (n == 0) menge else Matrizenraum(n, n, menge)
        }
    }
}

internal fun variablenStrukturWert(k: KnotenDaten, grundmenge: MengenAusdruck, grundraumVerbunden: Boolean = false): BedingterWert =
    variablenGrundraumWert(k, if (grundraumVerbunden) grundmenge else historischerVariablenGrundraum(k, grundmenge))

/** Ergänzt den Grundraumanschluss, ohne bestehende Ausgangsreferenzen zu verändern. */
fun KartenDaten.migriereVariablenGrundraum(): KartenDaten = copy(knoten = knoten.map { k ->
    if (k.art != "mathematik.variable" || k.anschlüsse.any { it.richtung == AnschlussRichtung.Eingang && it.name == "grundraum" }) k
    else k.copy(anschlüsse = k.anschlüsse + AnschlussDaten(
        id = AnschlussId("${k.id.wert}:grundraum"), name = "grundraum",
        richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Menge.id,
    ))
})

package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.Abbildungsmenge
import de.TeutonStudio.MathematikRechenSystem.kern.GaußscheGanzeZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.GaußschePrimzahlen
import de.TeutonStudio.MathematikRechenSystem.kern.LeereMenge as LeereMengeWert
import de.TeutonStudio.MathematikRechenSystem.kern.Matrizenraum
import de.TeutonStudio.MathematikRechenSystem.kern.MengenAusdruck
import de.TeutonStudio.MathematikRechenSystem.kern.ModuloZahlenraum
import de.TeutonStudio.MathematikRechenSystem.kern.Potenzmenge
import de.TeutonStudio.MathematikRechenSystem.kern.Primzahlen
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.Tensorraum
import de.TeutonStudio.MathematikRechenSystem.kern.Tupel
import de.TeutonStudio.MathematikRechenSystem.kern.symmetrischeDifferenz

/** Vorlagen und Auswerter der Mengen- und Koordinatenräume. */
object MengenraumKnotenVorlagen {
    private fun eingang(name: String, reihe: Int = 0) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Menge.id,
        reihenfolge = reihe,
    )

    private fun tupelEingang(name: String, reihe: Int = 0) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Tupel.id,
        reihenfolge = reihe,
    )

    private fun ausgang(name: String = "menge") = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = MathematikAnschlussArten.Menge.id,
    )

    val LeereMenge = KnotenVorlage(
        "mathematik.leereMenge",
        "Leere Menge",
        "Mengen",
        "Die eindeutig bestimmte Menge ohne Elemente.",
        GraphGröße(190f, 90f),
        listOf(ausgang()),
    )

    val GaußscheZahlen = KnotenVorlage(
        "mathematik.gaussZahlen",
        "Gaußsche ganze Zahlen",
        "Mengen",
        "Der Ring der komplexen ganzen Zahlen a + bi mit a,b ∈ ℤ.",
        GraphGröße(245f, 96f),
        listOf(ausgang()),
    )

    val Primzahlen = KnotenVorlage(
        "mathematik.primzahlen",
        "Primzahlen",
        "Mengen",
        "Die Menge der positiven Primzahlen in ℕ.",
        GraphGröße(210f, 92f),
        listOf(ausgang()),
    )

    val GaußschePrimzahlen = KnotenVorlage(
        "mathematik.gaussPrimzahlen",
        "Gaußsche Primzahlen",
        "Mengen",
        "Die Primelemente von ℤ[i], auch komplexe Primzahlen genannt.",
        GraphGröße(245f, 96f),
        listOf(ausgang()),
    )

    val Potenzmenge = KnotenVorlage(
        "mathematik.potenzmenge",
        "Potenzmenge",
        "Mengen",
        "Bildet die Menge aller Teilmengen einer Grundmenge.",
        GraphGröße(225f, 108f),
        listOf(eingang("grundmenge"), ausgang()),
    )

    val Abbildungsmenge = KnotenVorlage(
        "mathematik.abbildungsmenge",
        "Abbildungsmenge",
        "Mengen",
        "Bildet A^B, die Menge aller Abbildungen von B nach A.",
        GraphGröße(245f, 120f),
        listOf(eingang("zielmenge", 0), eingang("definitionsmenge", 1), ausgang()),
    )

    val Vektorraum = KnotenVorlage(
        "mathematik.vektorraum",
        "Vektorraum Aⁿ",
        "Mengen",
        "Erzeugt den n-dimensionalen Koordinatenraum über einer Grundmenge.",
        GraphGröße(245f, 110f),
        listOf(eingang("grundmenge"), ausgang()),
        mapOf("dimension" to "3"),
    )

    val Matrizenraum = KnotenVorlage(
        "mathematik.matrizenraum",
        "Matrizenraum Aⁿˣᵐ",
        "Mengen",
        "Erzeugt den Raum aller n×m-Matrizen über einer Grundmenge.",
        GraphGröße(255f, 115f),
        listOf(eingang("grundmenge"), ausgang()),
        mapOf("zeilen" to "2", "spalten" to "2"),
    )

    val Tensorraum = KnotenVorlage(
        "mathematik.tensorraum",
        "Tensorraum",
        "Mengen",
        "Erzeugt A^{n×m×k×…} aus einem Tupel positiver natürlicher Dimensionen.",
        GraphGröße(270f, 125f),
        listOf(eingang("grundmenge", 0), tupelEingang("dimensionen", 1), ausgang()),
    )

    val ModuloZahlenraum = KnotenVorlage(
        "mathematik.moduloZahlenraum",
        "Modulo-Zahlenraum",
        "Mengen",
        "Der Restklassenring ℤ/nℤ für n ≥ 2.",
        GraphGröße(235f, 105f),
        listOf(ausgang()),
        mapOf("modul" to "2"),
    )

    val SymmetrischeDifferenz = KnotenVorlage(
        "mathematik.symmetrischeDifferenz",
        "Symmetrische Differenz",
        "Mengen",
        "Enthält genau die Elemente, die in genau einer der beiden Mengen liegen.",
        GraphGröße(250f, 112f),
        listOf(eingang("links", 0), eingang("rechts", 1), ausgang()),
    )

    /** Nur ein Mengenknoten ist erzeugbar; historische Konstanten bleiben ausschließlich für Migration und alte Karten registriert. */
    val alle = listOf(
        MengenKnotenVorlagen.standard,
        Potenzmenge,
        Abbildungsmenge,
        Vektorraum,
        Matrizenraum,
        Tensorraum,
        ModuloZahlenraum,
        SymmetrischeDifferenz,
    )
}

internal fun MathematikAuswerterRegister.registriereMengenraumKnoten() {
    registriere("mathematik.leereMenge") {
        mengenraumErgebnis(LeereMengeWert)
    }
    registriere("mathematik.gaussZahlen") {
        mengenraumErgebnis(GaußscheGanzeZahlen)
    }
    registriere("mathematik.primzahlen") {
        mengenraumErgebnis(Primzahlen)
    }
    registriere("mathematik.gaussPrimzahlen") {
        mengenraumErgebnis(GaußschePrimzahlen)
    }
    registriere("mathematik.potenzmenge") { k ->
        mengenraumErgebnis(Potenzmenge(k.mengenraumEingabe("grundmenge")), k)
    }
    registriere("mathematik.abbildungsmenge") { k ->
        mengenraumErgebnis(
            Abbildungsmenge(
                zielMenge = k.mengenraumEingabe("zielmenge"),
                definitionsMenge = k.mengenraumEingabe("definitionsmenge"),
            ),
            k,
        )
    }
    registriere("mathematik.vektorraum") { k ->
        mengenraumErgebnis(
            Tensorraum(k.mengenraumEingabe("grundmenge"), listOf(k.mengenraumPositiveGanzzahl("dimension"))),
            k,
        )
    }
    registriere("mathematik.matrizenraum") { k ->
        mengenraumErgebnis(
            Matrizenraum(
                zeilen = k.mengenraumPositiveGanzzahl("zeilen"),
                spalten = k.mengenraumPositiveGanzzahl("spalten"),
                skalarMenge = k.mengenraumEingabe("grundmenge"),
            ),
            k,
        )
    }
    registriere(TENSORRAUM_LEGACY_DIMENSIONEN_ART) { k ->
        val dimensionen = k.knoten.parameter["werte"].orEmpty()
            .split(',')
            .map(String::trim)
            .filter(String::isNotBlank)
            .map { it.toLongOrNull() ?: error("Legacy-Tensorraumdimension '$it' ist keine ganze Zahl.") }
        require(dimensionen.isNotEmpty() && dimensionen.all { it > 0 }) {
            "Legacy-Tensorraumdimensionen müssen positive ganze Zahlen sein."
        }
        KnotenAuswertungsErgebnis(
            mapOf("tupel" to BedingterWert(objekt = Tupel(dimensionen.map(RationaleZahl::von)))),
        )
    }
    registriere("mathematik.tensorraum") { k ->
        mengenraumErgebnis(Tensorraum(k.mengenraumEingabe("grundmenge"), k.mengenraumDimensionen()), k)
    }
    registriere("mathematik.moduloZahlenraum") { k ->
        mengenraumErgebnis(ModuloZahlenraum(k.mengenraumPositiveGanzzahl("modul", minimum = 2)))
    }
    registriere("mathematik.symmetrischeDifferenz") { k ->
        mengenraumErgebnis(
            symmetrischeDifferenz(
                k.mengenraumEingabe("links"),
                k.mengenraumEingabe("rechts"),
            ),
            k,
        )
    }
}

private fun mengenraumErgebnis(
    menge: MengenAusdruck,
    kontext: KnotenAuswertungsKontext? = null,
) = KnotenAuswertungsErgebnis(
    mapOf(
        "menge" to BedingterWert(
            objekt = menge,
            annahmen = kontext?.eingänge?.values?.flatMap { it.annahmen }?.toSet().orEmpty(),
        ),
    ),
)

private fun KnotenAuswertungsKontext.mengenraumEingabe(name: String): MengenAusdruck =
    eingänge[name]?.objekt as? MengenAusdruck ?: error("Mengeneingang $name fehlt.")

private fun KnotenAuswertungsKontext.mengenraumPositiveGanzzahl(name: String, minimum: Int = 1): Int {
    val wert = knoten.parameter[name]?.trim()?.toIntOrNull()
        ?: error("Parameter $name muss eine ganze Zahl sein.")
    require(wert >= minimum) { "Parameter $name muss mindestens $minimum sein." }
    return wert
}

private fun KnotenAuswertungsKontext.mengenraumDimensionen(): List<Int> {
    val tupel = eingänge["dimensionen"]?.objekt as? Tupel
    if (tupel != null) {
        require(tupel.elemente.isNotEmpty()) { "Ein Tensorraum benötigt mindestens eine Dimension." }
        return tupel.elemente.mapIndexed { index, element ->
            val zahl = element as? RationaleZahl
                ?: error("Tensorraumdimension ${index + 1} ist keine konkrete natürliche Zahl.")
            require(
                zahl.nenner == java.math.BigInteger.ONE &&
                    zahl.zähler.signum() > 0 &&
                    zahl.zähler.bitLength() < 31,
            ) { "Tensorraumdimension ${index + 1} muss eine positive natürliche Zahl sein." }
            zahl.zähler.toInt()
        }
    }

    // Rückwärtskompatibilität alter Karten. Neue Vorlagen schreiben diesen Parameter nicht mehr.
    val historisch = knoten.parameter["dimensionen"]
        ?: error("Der Tupel-Eingang 'dimensionen' fehlt.")
    val dimensionen = historisch
        .split(',')
        .map(String::trim)
        .filter(String::isNotBlank)
        .map { it.toIntOrNull() ?: error("Tensorraumdimension '$it' ist keine ganze Zahl.") }
    require(dimensionen.isNotEmpty()) { "Ein Tensorraum benötigt mindestens eine Dimension." }
    require(dimensionen.all { it > 0 }) { "Alle Tensorraumdimensionen müssen positiv sein." }
    return dimensionen
}

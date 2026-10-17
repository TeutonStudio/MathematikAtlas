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
import de.TeutonStudio.MathematikRechenSystem.kern.*

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
        "Tensorträgermenge erster Stufe",
        "Mengen",
        "Erzeugt die Trägermenge von Tensoren erster Stufe über einer Grundmenge; legt keine Addition, Skalarmultiplikation oder Vektorraumaxiome fest.",
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

    /** Historische Vorlage bleibt für alte Karten ladefähig, wird aber nicht mehr im Erstellen-Katalog angeboten. */
    val SymmetrischeDifferenz = KnotenVorlage(
        "mathematik.symmetrischeDifferenz",
        "Symmetrische Differenz",
        "Mengen",
        "Enthält genau die Elemente, die in genau einer der beiden Mengen liegen.",
        GraphGröße(250f, 112f),
        listOf(eingang("links", 0), eingang("rechts", 1), ausgang()),
    )

    /** Nur kanonische Mengenknoten sind erzeugbar; historische Einzeloperatoren bleiben ausschließlich ladefähig. */
    val alle = listOf(
        MengenKnotenVorlagen.standard,
        Potenzmenge,
        Abbildungsmenge,
        Vektorraum,
        Matrizenraum,
        Tensorraum,
        ModuloZahlenraum,
    )
}

internal fun MathematikAuswerterRegister.registriereMengenraumKnoten() {






    registriere("mathematik.vektorraum") { k ->
        mengenraumErgebnis(
            Tensorraum(
                k.mengenraumEingabe("grundmenge"),
                listOf(RationaleZahl.von(k.mengenraumPositiveGanzzahl("dimension").toLong())),
            ),
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
        val dimensionen = k.mengenraumDimensionen()
        mengenraumErgebnis(
            Tensorraum(k.mengenraumEingabe("grundmenge"), dimensionen.werte),
            k,
            dimensionen.bedingungen,
        )
    }
    registriere("mathematik.moduloZahlenraum") { k ->
        mengenraumErgebnis(ModuloZahlenraum(k.mengenraumPositiveGanzzahl("modul", minimum = 2)), k)
    }

}

private fun mengenraumErgebnis(
    menge: MengenAusdruck,
    kontext: KnotenAuswertungsKontext? = null,
    zusätzlicheAnnahmen: Set<Aussage> = emptySet(),
) = KnotenAuswertungsErgebnis(
    mapOf(
        "menge" to BedingterWert(
            objekt = menge,
            annahmen = kontext?.eingänge?.values?.flatMap { it.annahmen }?.toSet().orEmpty() + zusätzlicheAnnahmen,
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

private data class TensorraumDimensionen(
    val werte: List<ZahlAusdruck>,
    val bedingungen: Set<Aussage> = emptySet(),
)

private fun KnotenAuswertungsKontext.mengenraumDimensionen(): TensorraumDimensionen {
    val eingang = eingänge["dimensionen"]
    val tupel = eingang?.objekt as? Tupel
    if (tupel != null) {
        require(tupel.elemente.isNotEmpty()) { "Ein Tensorraum benötigt mindestens eine Dimension." }
        val bedingungen = linkedSetOf<Aussage>()
        val werte = tupel.elemente.mapIndexed { index, element ->
            when (element) {
                is RationaleZahl -> {
                    require(element.nenner == java.math.BigInteger.ONE && element.zähler.signum() > 0) {
                        "Tensorraumdimension ${index + 1} muss eine positive natürliche Zahl sein."
                    }
                    element
                }
                is Variable -> {
                    val werteVorräte = eingang.variablenQuellen
                        .filter { it.name == element.name }
                        .map { it.werteVorrat }
                        .distinct()
                    require(werteVorräte.size == 1 && werteVorräte.single() == NatürlicheZahlen) {
                        "Tensorraumdimension ${index + 1} '${element.name}' benötigt einen nachgewiesenen Wertevorrat in ℕ."
                    }
                    val positiv = Vergleich(element, VergleichsArt.Größer, RationaleZahl.Null)
                    if (positiv !in eingang.annahmen) bedingungen += positiv
                    element
                }
                is ZahlAusdruck -> error(
                    "Tensorraumdimension ${index + 1} ist symbolisch, besitzt aber keinen nachgewiesenen natürlichen Dimensionsvertrag.",
                )
                else -> error("Tensorraumdimension ${index + 1} ist keine Zahl.")
            }
        }
        return TensorraumDimensionen(werte, bedingungen)
    }

    // Rückwärtskompatibilität alter Karten. Neue Vorlagen schreiben diesen Parameter nicht mehr.
    val historisch = knoten.parameter["dimensionen"]
        ?: error("Der Tupel-Eingang 'dimensionen' fehlt.")
    val dimensionen = historisch
        .split(',')
        .map(String::trim)
        .filter(String::isNotBlank)
        .map { it.toLongOrNull() ?: error("Tensorraumdimension '$it' ist keine ganze Zahl.") }
    require(dimensionen.isNotEmpty()) { "Ein Tensorraum benötigt mindestens eine Dimension." }
    require(dimensionen.all { it > 0 }) { "Alle Tensorraumdimensionen müssen positiv sein." }
    return TensorraumDimensionen(dimensionen.map(RationaleZahl::von))
}

/** Gemeinsame Auswertung der vordefinierten Matrizenmengen und des Mengenoperators. */
internal fun matrizenMengenErgebnis(kontext: KnotenAuswertungsKontext, grundmenge: MengenAusdruck): KnotenAuswertungsErgebnis {
    require(kontext.eingänge.values.flatMap { it.annahmen }.none {
        it.entscheide(kontext.rechenKontext).wahrheitswert == Wahrheitswert.Lüge
    }) { "Eine Voraussetzung der Matrizenmenge ist widerlegt." }
    val bedingungen = linkedSetOf<Aussage>()
    val dimensionen = listOf("n", "m").map { name ->
        val eingang = kontext.eingänge[name] ?: error("Der Dimensionseingang '$name' fehlt.")
        when (val zahl = eingang.objekt) {
            is RationaleZahl -> {
                require(zahl.nenner == java.math.BigInteger.ONE && zahl.zähler.signum() > 0) {
                    "Die Dimension '$name' muss eine positive natürliche Zahl sein."
                }
                zahl
            }
            is Variable -> {
                val bereiche = eingang.variablenQuellen.filter { quelle ->
                    if (zahl.bindungsId != null) (quelle.bindungsId ?: quelle.knotenId.wert) == zahl.bindungsId
                    else quelle.name == zahl.name
                }
                require(bereiche.map { it.identität }.distinct().size == 1 && bereiche.all {
                    it.werteVorrat in setOf(NatürlicheZahlen, NichtnegativeGanzeZahlenSemantik.menge)
                }) {
                    "Die Dimension '$name' benötigt eine eindeutig gebundene Quelle mit Wertevorrat ℕ."
                }
                val positiv = Vergleich(zahl, VergleichsArt.Größer, RationaleZahl.Null)
                require((eingang.annahmen + positiv).none { it.entscheide(kontext.rechenKontext).wahrheitswert == Wahrheitswert.Lüge }) {
                    "Die Voraussetzungen der Dimension '$name' sind widerlegt."
                }
                if (positiv !in eingang.annahmen) bedingungen += positiv
                zahl
            }
            else -> error("Die Dimension '$name' muss eine positive natürliche Zahl oder eine gebundene natürliche Variable sein.")
        }
    }
    return KnotenAuswertungsErgebnis(
        mapOf("menge" to kontext.rechnerErgebnisWert(Tensorraum(grundmenge, dimensionen, matrixForm = true), bedingungen)),
        eingänge = kontext.eingänge,
    )
}


/** Dimensionsverträge für die kanonischen Mengenquellen und Raumoperatoren. */
internal fun mengenDimensionsErgebnis(
    kontext: KnotenAuswertungsKontext,
    grundmenge: MengenAusdruck,
    tensor: Boolean,
): KnotenAuswertungsErgebnis {
    val eingang = kontext.eingänge["dimension"] ?: error("Der Dimensionseingang fehlt.")
    val annahmen = kontext.eingänge.values.flatMap { it.annahmen }.toSet()
    require(annahmen.none { it.entscheide(kontext.rechenKontext).wahrheitswert == Wahrheitswert.Lüge }) {
        "Eine Voraussetzung des Raumes ist widerlegt."
    }
    val elemente = if (tensor) (eingang.objekt as? Tupel)?.elemente
        ?: error("Tensorraum erwartet ein Zahlentupel als Dimension.") else listOf(eingang.objekt)
    require(elemente.isNotEmpty()) { "Ein Tensorraum benötigt mindestens eine Dimension." }
    val bedingungen = linkedSetOf<Aussage>()
    val dimensionen = elemente.mapIndexed { index, element ->
        val zahl = (element as? ZahlAusdruck)?.let { vereinfache(it, kontext.rechenKontext) }
            ?: error("Dimension ${index + 1} ist keine Zahl.")
        when (zahl) {
            is RationaleZahl -> require(zahl.nenner == java.math.BigInteger.ONE &&
                if (tensor) zahl.zähler.signum() > 0 else zahl.zähler.signum() >= 0) {
                "Dimension ${index + 1} muss ${if (tensor) "positiv" else "nichtnegativ"} und ganzzahlig sein."
            }
            is Variable -> {
                val quellen = eingang.variablenQuellen.filter { q ->
                    if (zahl.bindungsId != null) (q.bindungsId ?: q.knotenId.wert) == zahl.bindungsId else q.name == zahl.name
                }
                require(quellen.map { it.identität }.distinct().size == 1 && quellen.all {
                    it.werteVorrat in setOf(NatürlicheZahlen, NichtnegativeGanzeZahlenSemantik.menge)
                }) { "Die symbolische Dimension benötigt eine eindeutig gebundene natürliche Quelle." }
                val voraussetzung = Vergleich(zahl, if (tensor) VergleichsArt.Größer else VergleichsArt.GrößerGleich, RationaleZahl.Null)
                val gegenteil = Vergleich(zahl, if (tensor) VergleichsArt.KleinerGleich else VergleichsArt.Kleiner, RationaleZahl.Null)
                require(Negation(voraussetzung) !in annahmen && gegenteil !in annahmen && voraussetzung.entscheide(kontext.rechenKontext).wahrheitswert != Wahrheitswert.Lüge) { "Die Dimensionsvoraussetzung ist widerlegt." }
                if (voraussetzung !in annahmen) bedingungen += voraussetzung
            }
            else -> error("Die symbolische Dimension besitzt keinen nachgewiesenen natürlichen Dimensionsvertrag.")
        }
        zahl
    }
    val raum = if (tensor) Tensorraum(grundmenge, dimensionen, matrixForm = kontext.knoten.parameter["raum.matrixHistorisch"] == "true") else kartesischePotenz(grundmenge, dimensionen.single())
    return KnotenAuswertungsErgebnis(mapOf("menge" to kontext.rechnerErgebnisWert(raum, bedingungen)), eingänge = kontext.eingänge)
}

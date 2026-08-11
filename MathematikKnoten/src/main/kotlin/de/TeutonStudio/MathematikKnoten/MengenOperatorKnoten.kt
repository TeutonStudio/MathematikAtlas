package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val MENGENRECHNER_OPERATOR_PARAMETER = "operator"
const val MENGENRELATION_OPERATOR_PARAMETER = "operator"
const val MENGEN_MASS_KNOTEN_ART = "mathematik.mass"
const val MENGEN_MASS_MODUS_PARAMETER = "massModus"
const val MENGEN_MASS_SYMBOL_PARAMETER = "massSymbol"
const val MENGEN_NORM_KNOTEN_ART = "mathematik.mengenNorm"

private val sichtbareMengenRechnerOperatoren = listOf(
    MengenRechnerOperator.SCHNITT,
    MengenRechnerOperator.VEREINIGUNG,
    MengenRechnerOperator.DIFFERENZ,
    MengenRechnerOperator.KARTESISCHES_PRODUKT,
    MengenRechnerOperator.ITERIERTES_KARTESISCHES_PRODUKT,
    MengenRechnerOperator.SYMMETRISCHE_DIFFERENZ,
    MengenRechnerOperator.ITERIERTE_VEREINIGUNG,
    MengenRechnerOperator.ITERIERTER_SCHNITT,
    MengenRechnerOperator.POTENZMENGE,
    MengenRechnerOperator.ABBILDUNGSMENGE,
    MengenRechnerOperator.FOLGENMENGE,
    MengenRechnerOperator.HALBFOLGENMENGE,
    MengenRechnerOperator.KLASSIFIZIERTE_MENGE,
)

fun sichtbareMengenRechnerOperatoren(): List<MengenRechnerOperator> = sichtbareMengenRechnerOperatoren

fun MengenRechnerOperator.titel(): String = when (this) {
    MengenRechnerOperator.SCHNITT -> "Schnitt"
    MengenRechnerOperator.VEREINIGUNG -> "Vereinigung"
    MengenRechnerOperator.DIFFERENZ -> "Differenz"
    MengenRechnerOperator.KARTESISCHES_PRODUKT -> "Kartesisches Produkt"
    MengenRechnerOperator.ITERIERTES_KARTESISCHES_PRODUKT -> "Iteriertes kartesisches Produkt"
    MengenRechnerOperator.SYMMETRISCHE_DIFFERENZ -> "Symmetrische Differenz"
    MengenRechnerOperator.ITERIERTE_VEREINIGUNG -> "Iterierte Vereinigung"
    MengenRechnerOperator.ITERIERTER_SCHNITT -> "Iterierter Schnitt"
    MengenRechnerOperator.KOMPLEMENT -> "Komplement"
    MengenRechnerOperator.POTENZMENGE -> "Potenzmenge"
    MengenRechnerOperator.ABBILDUNGSMENGE -> "Abbildungsmenge"
    MengenRechnerOperator.FOLGENMENGE -> "Folgenmenge"
    MengenRechnerOperator.HALBFOLGENMENGE -> "Halbfolgenmenge"
    MengenRechnerOperator.KLASSIFIZIERTE_MENGE -> "Klassifizierte Menge"
    MengenRechnerOperator.BILD -> "Bild"
    MengenRechnerOperator.URBILD -> "Urbild"
}

fun MengenRechnerOperator.vorschauLatex(): String = when (this) {
    MengenRechnerOperator.SCHNITT -> "A\\cap B"
    MengenRechnerOperator.VEREINIGUNG -> "A\\cup B"
    MengenRechnerOperator.DIFFERENZ -> "A\\setminus B"
    MengenRechnerOperator.KARTESISCHES_PRODUKT -> "A\\times B"
    MengenRechnerOperator.ITERIERTES_KARTESISCHES_PRODUKT ->
        "\\mathop{\\Large\\times}\\limits_{i\\in I}A(i)"
    MengenRechnerOperator.SYMMETRISCHE_DIFFERENZ -> "A\\triangle B"
    MengenRechnerOperator.ITERIERTE_VEREINIGUNG -> "\\bigcup\\limits_{i\\in I}A(i)"
    MengenRechnerOperator.ITERIERTER_SCHNITT -> "\\bigcap\\limits_{i\\in I}A(i)"
    MengenRechnerOperator.KOMPLEMENT -> "U\\setminus A"
    MengenRechnerOperator.POTENZMENGE -> "\\mathcal{P}(M)"
    MengenRechnerOperator.ABBILDUNGSMENGE -> "M^A"
    MengenRechnerOperator.FOLGENMENGE -> "M^{\\mathbb Z}"
    MengenRechnerOperator.HALBFOLGENMENGE -> "M^{\\mathbb N_0}"
    MengenRechnerOperator.KLASSIFIZIERTE_MENGE -> "M\\div r"
    MengenRechnerOperator.BILD -> "f(A)"
    MengenRechnerOperator.URBILD -> "f^{-1}(A)"
}

fun MengenRechnerOperator.definitionLatex(): String = when (this) {
    MengenRechnerOperator.SCHNITT -> "A\\cap B=\\{x\\mid x\\in A\\land x\\in B\\}"
    MengenRechnerOperator.VEREINIGUNG -> "A\\cup B=\\{x\\mid x\\in A\\lor x\\in B\\}"
    MengenRechnerOperator.DIFFERENZ -> "A\\setminus B=\\{x\\in A\\mid x\\notin B\\}"
    MengenRechnerOperator.KARTESISCHES_PRODUKT -> "A\\times B=\\{(a,b)\\mid a\\in A, b\\in B\\}"
    MengenRechnerOperator.ITERIERTES_KARTESISCHES_PRODUKT ->
        "\\mathop{\\Large\\times}\\limits_{i\\in I}A(i)"
    MengenRechnerOperator.SYMMETRISCHE_DIFFERENZ ->
        "A\\triangle B=(A\\setminus B)\\cup(B\\setminus A)"
    MengenRechnerOperator.ITERIERTE_VEREINIGUNG ->
        "\\bigcup\\limits_{i\\in I}A(i)=\\{x\\mid\\exists i\\in I:x\\in A(i)\\}"
    MengenRechnerOperator.ITERIERTER_SCHNITT ->
        "\\bigcap\\limits_{i\\in I}A(i)=\\{x\\mid\\forall i\\in I:x\\in A(i)\\}"
    MengenRechnerOperator.POTENZMENGE -> "\\mathcal{P}(M)=\\{A\\mid A\\subseteq M\\}"
    MengenRechnerOperator.ABBILDUNGSMENGE -> "M^A:=\\{f\\mid f:A\\to M\\}"
    MengenRechnerOperator.FOLGENMENGE -> "M^{\\mathbb Z}:=\\{f\\mid f:\\mathbb Z\\to M\\}"
    MengenRechnerOperator.HALBFOLGENMENGE -> "M^{\\mathbb N_0}:=\\{f\\mid f:\\mathbb N_0\\to M\\}"
    MengenRechnerOperator.KLASSIFIZIERTE_MENGE ->
        "M\\div r:=\\{[x]_r\\mid x\\in M\\},\\quad [x]_r=\\{y\\in M\\mid r(x,y)\\}"
    else -> vorschauLatex()
}

object MengenRechnerKnotenVorlagen {
    val standard = KnotenVorlage(
        art = MengenRechner.KNOTEN_ART,
        name = MengenRechnerOperator.VEREINIGUNG.titel(),
        kategorie = "Mengen: Rechnung",
        beschreibung = "Konsolidierter Mengenrechner mit umschaltbaren Mengenoperatoren.",
        standardGröße = GraphGröße(270f, 145f),
        anschlüsse = mengenRechnerAnschluesse(MengenRechnerOperator.VEREINIGUNG, 2),
        standardParameter = mapOf(
            MENGENRECHNER_OPERATOR_PARAMETER to MengenRechnerOperator.VEREINIGUNG.stabileId,
            "festeEingänge" to "2",
            "operatorAnzeige" to "wert",
        ),
    )
}

object MengenRelationsKnotenVorlagen {
    val standard = KnotenVorlage(
        art = MengenRelationRechner.KNOTEN_ART,
        name = MengenRelationsOperator.TEILMENGE.titel,
        kategorie = "Mengen: Relationen",
        beschreibung = "Konsolidierter Mengenrelationsknoten mit umschaltbarer Relation.",
        standardGröße = GraphGröße(270f, 135f),
        anschlüsse = mengenRelationsAnschluesse(MengenRelationsOperator.TEILMENGE),
        standardParameter = mapOf(
            MENGENRELATION_OPERATOR_PARAMETER to MengenRelationsOperator.TEILMENGE.stabileId,
        ),
    )
}

object MengenMassKnotenVorlagen {
    val Mass = KnotenVorlage(
        art = MENGEN_MASS_KNOTEN_ART,
        name = "Maß",
        kategorie = "Mengen: Maßtheorie",
        beschreibung = "Erzeugt einen expliziten Maßvertrag für Integral- und Mengenmaßknoten.",
        standardGröße = GraphGröße(220f, 105f),
        anschlüsse = listOf(
            ausgang("maß", MathematikAnschlussArten.Mass.id),
        ),
        standardParameter = mapOf(
            MENGEN_MASS_MODUS_PARAMETER to IntegralMassModus.ALLGEMEIN.name,
            MENGEN_MASS_SYMBOL_PARAMETER to "\\mu",
        ),
    )

    val MengenNorm = KnotenVorlage(
        art = MENGEN_NORM_KNOTEN_ART,
        name = "Norm einer Menge",
        kategorie = "Mengen: Maßtheorie",
        beschreibung = "Berechnet für eine messbare Menge A die maßinduzierte Größe ∫_A 1 dμ = μ(A).",
        standardGröße = GraphGröße(270f, 130f),
        anschlüsse = listOf(
            eingang("menge", MathematikAnschlussArten.Menge.id, 0),
            eingang("maß", MathematikAnschlussArten.Mass.id, 1),
            ausgang("wert", MathematikAnschlussArten.Zahl.id),
        ),
    )

    val alle = listOf(Mass, MengenNorm)
}

fun konfiguriereMengenRechner(
    knoten: KnotenDaten,
    operator: MengenRechnerOperator,
    festeEingänge: Int = knoten.parameter["festeEingänge"]?.toIntOrNull()?.coerceAtLeast(2) ?: 2,
): KnotenDaten {
    require(knoten.art == MengenRechner.KNOTEN_ART)
    val gewuenscht = mengenRechnerAnschluesse(operator, festeEingänge)
    val anschluesse = erhalteAnschlussIds(knoten.anschlüsse, gewuenscht)
    val bisherigerTitel = MengenRechnerOperator.vonIdOderNull(knoten.parameter[MENGENRECHNER_OPERATOR_PARAMETER])?.titel()
    val automatischerName = bisherigerTitel != null && knoten.name == bisherigerTitel ||
        sichtbareMengenRechnerOperatoren.any { it.titel() == knoten.name }
    return normalisiereRechnerMethodenAnschluesse(
        knoten.copy(
            name = if (automatischerName) operator.titel() else knoten.name,
            anschlüsse = anschluesse,
            parameter = knoten.parameter + mapOf(
                MENGENRECHNER_OPERATOR_PARAMETER to operator.stabileId,
                "festeEingänge" to festeEingänge.toString(),
            ),
        ),
    )
}

fun konfiguriereMengenRelation(
    knoten: KnotenDaten,
    operator: MengenRelationsOperator,
): KnotenDaten {
    require(knoten.art == MengenRelationRechner.KNOTEN_ART)
    val anschluesse = erhalteAnschlussIds(knoten.anschlüsse, mengenRelationsAnschluesse(operator))
    val bisherigerTitel = MengenRelationsOperator.vonIdOderNull(knoten.parameter[MENGENRELATION_OPERATOR_PARAMETER])?.titel
    val automatischerName = bisherigerTitel != null && knoten.name == bisherigerTitel ||
        MengenRelationsOperator.entries.any { it.titel == knoten.name }
    return knoten.copy(
        name = if (automatischerName) operator.titel else knoten.name,
        anschlüsse = anschluesse,
        parameter = knoten.parameter + (MENGENRELATION_OPERATOR_PARAMETER to operator.stabileId),
    )
}

internal fun MathematikAuswerterRegister.registriereMengenOperatorKnoten() {
    registriere(MengenRechner.KNOTEN_ART) { kontext -> werteMengenRechnerAus(kontext) }
    registriere(MengenRelationRechner.KNOTEN_ART) { kontext -> werteMengenRelationAus(kontext) }
    registriere(MENGEN_MASS_KNOTEN_ART) { kontext ->
        val mass = when (IntegralMassModus.entries.firstOrNull {
            it.name == kontext.knoten.parameter[MENGEN_MASS_MODUS_PARAMETER]
        } ?: IntegralMassModus.ALLGEMEIN) {
            IntegralMassModus.AUTO,
            IntegralMassModus.ALLGEMEIN,
            -> IntegralMass.Allgemein(
                kontext.knoten.parameter[MENGEN_MASS_SYMBOL_PARAMETER].orEmpty().ifBlank { "\\mu" },
            )
            IntegralMassModus.STANDARD_REELL -> IntegralMass.StandardReell
            IntegralMassModus.ZAEHLMASS -> IntegralMass.Zaehlmass
            IntegralMassModus.NICHTSTANDARD -> IntegralMass.NichtstandardZellgewicht()
        }
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf("maß" to BedingterWert(mass)),
        )
    }
    registriere(MENGEN_NORM_KNOTEN_ART) { kontext ->
        val mengeWert = kontext.eingänge["menge"] ?: error("Die Menge fehlt.")
        val massWert = kontext.eingänge["maß"] ?: error("Das Maß fehlt.")
        val menge = mengeWert.objekt as? MengenAusdruck ?: error("Der Eingang 'menge' erwartet eine Menge.")
        val mass = massWert.objekt as? IntegralMass ?: error("Der Eingang 'maß' erwartet einen Maßvertrag.")
        val ergebnis = normEinerMenge(menge, mass)
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "wert" to BedingterWert(
                    objekt = ergebnis.wert,
                    annahmen = mengeWert.annahmen + massWert.annahmen + ergebnis.voraussetzungen,
                    zielMenge = ReelleZahlen,
                    latexDarstellung = ergebnis.wert.zuLatex(),
                ),
            ),
            schritte = ergebnis.schritte,
            warnungen = buildList {
                add("Definition: \\lVert A\\rVert_{\\mu}=\\int_A 1\\,\\mathrm d\\mu=\\mu(A)")
                add("Integralstatus: ${ergebnis.status.name}")
                add("Maß: ${mass.zuLatex()}")
                if (ergebnis.voraussetzungen.isNotEmpty()) {
                    add("Messbarkeit ist nicht vollständig nachgewiesen; das Ergebnis bleibt unter den angegebenen Voraussetzungen gültig.")
                }
            },
            eingänge = kontext.eingänge,
        )
    }
}

private fun werteMengenRechnerAus(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
    val operator = MengenRechnerOperator.vonIdOderNull(
        kontext.knoten.parameter[MENGENRECHNER_OPERATOR_PARAMETER],
    ) ?: error("Unbekannter Mengenrechner-Operator.")
    val annahmen = kontext.eingänge.values.flatMap { it.annahmen }.toSet()
    val objekt: MengenAusdruck
    val warnungen = mutableListOf<String>()

    if (operator == MengenRechnerOperator.KLASSIFIZIERTE_MENGE) {
        val menge = kontext.eingänge["menge"]?.objekt as? MengenAusdruck
            ?: error("Die klassifizierte Menge benötigt eine Grundmenge M.")
        val relation = kontext.eingänge["relation"]?.objekt as? Methode
            ?: error("Die klassifizierte Menge benötigt eine zweistellige Relation als Prädikat.")
        require(relation.istPrädikat() && relation.argumentAnzahl == 2) {
            "Die Klassifikationsrelation muss ein zweistelliges Prädikat sein."
        }
        val relationName = relation.name.ifBlank { "r" }
        objekt = BenannteMenge(
            name = "klassifizierteMenge:${menge.zuLatex()}:$relationName",
            latex = "${menge.zuLatex()}\\div $relationName",
        )
        warnungen += "Klassifikation setzt voraus, dass $relationName auf ${menge.zuLatex()} reflexiv, symmetrisch und transitiv ist."
        warnungen += "Definition: ${menge.zuLatex()}\\div $relationName=\\{[x]_$relationName\\mid x\\in${menge.zuLatex()}\\}"
    } else if (operator in setOf(
            MengenRechnerOperator.ITERIERTES_KARTESISCHES_PRODUKT,
            MengenRechnerOperator.ITERIERTE_VEREINIGUNG,
            MengenRechnerOperator.ITERIERTER_SCHNITT,
        )
    ) {
        val methode = kontext.eingänge["methode"]?.objekt as? Methode
            ?: error("Der iterierte Mengenoperator benötigt eine Mengenmethode.")
        val indexMenge = kontext.eingänge["indexmenge"]?.objekt as? MengenAusdruck
            ?: error("Der iterierte Mengenoperator benötigt eine Indexmenge.")
        objekt = when (operator) {
            MengenRechnerOperator.ITERIERTES_KARTESISCHES_PRODUKT -> iteriertesKartesischesProdukt(methode, indexMenge)
            MengenRechnerOperator.ITERIERTE_VEREINIGUNG -> iterierteVereinigung(methode, indexMenge)
            MengenRechnerOperator.ITERIERTER_SCHNITT -> iterierterSchnitt(methode, indexMenge)
            else -> error("Nicht iterierter Operator im Iterationszweig.")
        }
    } else {
        val eingangsAnschluesse = kontext.knoten.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Eingang }
            .sortedBy { it.reihenfolge }
        val eingaben = eingangsAnschluesse.mapIndexed { index, anschluss ->
            val wert = kontext.eingänge[anschluss.name]
                ?: error("Mengeneingang ${index + 1} ist nicht verbunden.")
            val menge = wert.objekt as? MengenAusdruck
                ?: error("Eingang '${anschluss.name}' erwartet eine Menge.")
            val rolle = when {
                operator == MengenRechnerOperator.DIFFERENZ && index == 0 -> "grundmenge"
                operator == MengenRechnerOperator.DIFFERENZ && index == 1 -> "abzug"
                operator == MengenRechnerOperator.SYMMETRISCHE_DIFFERENZ && index == 0 -> "links"
                operator == MengenRechnerOperator.SYMMETRISCHE_DIFFERENZ && index == 1 -> "rechts"
                else -> anschluss.name
            }
            MengenRechnerEingabe(rolle, menge)
        }
        when (val ergebnis = MengenRechner.erzeuge(operator, eingaben)) {
            is MengenRechnerErgebnis.Wert -> {
                objekt = ergebnis.menge
                warnungen += ergebnis.bedingungen
            }
            is MengenRechnerErgebnis.Bedingt -> {
                objekt = BenannteMenge("Bedingte Mengenoperation", ergebnis.latex)
                warnungen += ergebnis.bedingungen
            }
            is MengenRechnerErgebnis.Ungueltig -> error(ergebnis.nachricht)
        }
    }

    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf(
            "menge" to BedingterWert(
                objekt = objekt,
                annahmen = annahmen,
                reelleVariablen = reelleVariablen(kontext.eingänge.values),
                variablenQuellen = kontext.eingänge.values.flatMap { it.variablenQuellen }.geordnetEindeutig(),
            ),
        ),
        warnungen = warnungen,
        eingänge = kontext.eingänge,
    )
}

private fun werteMengenRelationAus(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
    val operator = MengenRelationsOperator.vonIdOderNull(
        kontext.knoten.parameter[MENGENRELATION_OPERATOR_PARAMETER],
    ) ?: error("Unbekannter Mengenrelations-Operator.")
    val (links, rechts) = if (operator == MengenRelationsOperator.ELEMENT) {
        (kontext.eingänge["element"]?.objekt ?: error("Das Element fehlt.")) to
            (kontext.eingänge["menge"]?.objekt ?: error("Die Menge fehlt."))
    } else {
        (kontext.eingänge["links"]?.objekt ?: error("Die linke Menge fehlt.")) to
            (kontext.eingänge["rechts"]?.objekt ?: error("Die rechte Menge fehlt."))
    }
    val aussage = MengenRelationRechner.erzeuge(operator, links, rechts)
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf(
            "aussage" to BedingterWert(
                objekt = aussage,
                annahmen = kontext.eingänge.values.flatMap { it.annahmen }.toSet(),
                reelleVariablen = reelleVariablen(kontext.eingänge.values),
                variablenQuellen = kontext.eingänge.values.flatMap { it.variablenQuellen }.geordnetEindeutig(),
            ),
        ),
        eingänge = kontext.eingänge,
    )
}

fun KartenDaten.migriereMengenOperatorKnoten(): KartenDaten = copy(
    knoten = knoten.map { alt ->
        MengenRechnerMigration.alteKnotenArten[alt.art]?.let { operator ->
            val basis = alt.copy(
                art = MengenRechner.KNOTEN_ART,
                parameter = MengenRechnerKnotenVorlagen.standard.standardParameter + alt.parameter +
                    (MENGENRECHNER_OPERATOR_PARAMETER to operator.stabileId),
            )
            return@map konfiguriereMengenRechner(
                knoten = basis,
                operator = operator,
                festeEingänge = alt.anschlüsse.count { it.richtung == AnschlussRichtung.Eingang }.coerceAtLeast(2),
            )
        }
        MengenRelationsMigration.alteKnotenArten[alt.art]?.let { operator ->
            val basis = alt.copy(
                art = MengenRelationRechner.KNOTEN_ART,
                parameter = MengenRelationsKnotenVorlagen.standard.standardParameter + alt.parameter +
                    (MENGENRELATION_OPERATOR_PARAMETER to operator.stabileId),
            )
            return@map konfiguriereMengenRelation(basis, operator)
        }
        alt
    },
)

private fun mengenRechnerAnschluesse(
    operator: MengenRechnerOperator,
    festeEingänge: Int,
): List<AnschlussDaten> = when (operator) {
    MengenRechnerOperator.VEREINIGUNG,
    MengenRechnerOperator.SCHNITT,
    MengenRechnerOperator.KARTESISCHES_PRODUKT,
    -> List(festeEingänge.coerceAtLeast(2)) { index ->
        eingang(
            name = ('a'.code + index).toChar().toString(),
            art = MathematikAnschlussArten.Menge.id,
            reihenfolge = index,
            erweiterbar = true,
        )
    } + ausgang("menge", MathematikAnschlussArten.Menge.id)

    MengenRechnerOperator.DIFFERENZ,
    MengenRechnerOperator.SYMMETRISCHE_DIFFERENZ,
    -> listOf(
        eingang("links", MathematikAnschlussArten.Menge.id, 0),
        eingang("rechts", MathematikAnschlussArten.Menge.id, 1),
        ausgang("menge", MathematikAnschlussArten.Menge.id),
    )

    MengenRechnerOperator.ITERIERTES_KARTESISCHES_PRODUKT,
    MengenRechnerOperator.ITERIERTE_VEREINIGUNG,
    MengenRechnerOperator.ITERIERTER_SCHNITT,
    -> listOf(
        eingang("methode", MathematikAnschlussArten.Methode.id, 0),
        eingang("indexmenge", MathematikAnschlussArten.Menge.id, 1),
        ausgang("menge", MathematikAnschlussArten.Menge.id),
    )

    MengenRechnerOperator.ABBILDUNGSMENGE -> listOf(
        eingang("zielmenge", MathematikAnschlussArten.Menge.id, 0),
        eingang("argumentmenge", MathematikAnschlussArten.Menge.id, 1),
        ausgang("menge", MathematikAnschlussArten.Menge.id),
    )

    MengenRechnerOperator.KLASSIFIZIERTE_MENGE -> listOf(
        eingang("menge", MathematikAnschlussArten.Menge.id, 0),
        eingang("relation", MathematikAnschlussArten.Methode.id, 1),
        ausgang("menge", MathematikAnschlussArten.Menge.id),
    )

    else -> listOf(
        eingang("a", MathematikAnschlussArten.Menge.id, 0),
        ausgang("menge", MathematikAnschlussArten.Menge.id),
    )
}

private fun mengenRelationsAnschluesse(operator: MengenRelationsOperator): List<AnschlussDaten> =
    if (operator == MengenRelationsOperator.ELEMENT) {
        listOf(
            eingang("element", MathematikAnschlussArten.Objekt.id, 0),
            eingang("menge", MathematikAnschlussArten.Menge.id, 1),
            ausgang("aussage", MathematikAnschlussArten.Aussage.id),
        )
    } else {
        listOf(
            eingang("links", MathematikAnschlussArten.Menge.id, 0),
            eingang("rechts", MathematikAnschlussArten.Menge.id, 1),
            ausgang("aussage", MathematikAnschlussArten.Aussage.id),
        )
    }

private fun eingang(
    name: String,
    art: AnschlussArtId,
    reihenfolge: Int,
    erweiterbar: Boolean = false,
): AnschlussDaten = AnschlussDaten(
    name = name,
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = art,
    reihenfolge = reihenfolge,
    kannSichErweitern = erweiterbar,
)

private fun ausgang(name: String, art: AnschlussArtId): AnschlussDaten = AnschlussDaten(
    name = name,
    richtung = AnschlussRichtung.Ausgang,
    kante = AnschlussKante.Rechts,
    art = art,
    reihenfolge = 0,
)

private fun erhalteAnschlussIds(
    bisher: List<AnschlussDaten>,
    gewuenscht: List<AnschlussDaten>,
): List<AnschlussDaten> {
    val bisherEingaenge = bisher.filter { it.richtung == AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }
    val bisherAusgaenge = bisher.filter { it.richtung == AnschlussRichtung.Ausgang }.sortedBy { it.reihenfolge }
    return gewuenscht.mapIndexed { index, neu ->
        val gleichnamig = bisher.firstOrNull { it.richtung == neu.richtung && it.name == neu.name }
        val positionsAlt = when (neu.richtung) {
            AnschlussRichtung.Eingang -> bisherEingaenge.getOrNull(index)
            AnschlussRichtung.Ausgang -> bisherAusgaenge.getOrNull(index)
            AnschlussRichtung.Neutral -> null
        }
        val alt = gleichnamig ?: positionsAlt
        if (alt == null) neu else neu.copy(id = alt.id)
    }
}

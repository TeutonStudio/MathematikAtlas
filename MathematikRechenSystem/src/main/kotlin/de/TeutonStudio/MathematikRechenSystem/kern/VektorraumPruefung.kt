package de.TeutonStudio.MathematikRechenSystem.kern

internal data class MethodenVertrag(
    val argumentMengen: List<MengenAusdruck>,
    val zielMenge: MengenAusdruck,
)

internal fun Methode.vertragOderNull(): MethodenVertrag? = runCatching {
    val signatur = methodenSignatur()
    MethodenVertrag(signatur.argumente.map { it.werteVorrat }, signatur.zielMenge)
}.getOrNull()

internal fun statusAus(pruefungen: List<BegriffsAxiomPruefung>): NachweisStatus {
    val status = pruefungen.map { it.status }
    return when {
        status.any { it == NachweisStatus.Widerlegt } -> NachweisStatus.Widerlegt
        status.any { it == NachweisStatus.Unvollstaendig } -> NachweisStatus.Unvollstaendig
        status.all { it == NachweisStatus.Nachgewiesen } -> NachweisStatus.Nachgewiesen
        status.any { it is NachweisStatus.Bedingt } -> NachweisStatus.Bedingt(
            status.filterIsInstance<NachweisStatus.Bedingt>().flatMap { it.bedingungen },
        )
        else -> NachweisStatus.Unentscheidbar
    }
}

internal val bekannteKoerper = setOf(RationaleZahlen, ReelleZahlen, KomplexeZahlen)

internal fun istBekannterKoerper(menge: MengenAusdruck): Boolean = menge in bekannteKoerper

internal fun MengeOderRaumBezeichnung(menge: MengenAusdruck): String = menge.zuLatex()

internal fun kanonischeSkalarAddition(methode: Methode, menge: MengenAusdruck): Boolean {
    val parameter = methode.parameter.filterIsInstance<Variable>()
    if (parameter.size != 2) return false
    val vertrag = methode.vertragOderNull() ?: return false
    if (vertrag.argumentMengen != listOf(menge, menge) || vertrag.zielMenge != menge) return false
    val erwartet = addition(parameter)
    return methode.vorschrift is ZahlAusdruck &&
        vereinfache(methode.vorschrift as ZahlAusdruck) == vereinfache(erwartet)
}

internal fun kanonischeSkalarMultiplikation(methode: Methode, menge: MengenAusdruck): Boolean {
    val parameter = methode.parameter.filterIsInstance<Variable>()
    if (parameter.size != 2) return false
    val vertrag = methode.vertragOderNull() ?: return false
    if (vertrag.argumentMengen != listOf(menge, menge) || vertrag.zielMenge != menge) return false
    val erwartet = multiplikation(parameter)
    return methode.vorschrift is ZahlAusdruck &&
        vereinfache(methode.vorschrift as ZahlAusdruck) == vereinfache(erwartet)
}

internal fun Methode.wendeBinärAn(
    links: MathematischesObjekt,
    rechts: MathematischesObjekt,
): MathematischesObjekt = wendeAn(listOf(links, rechts))

internal fun mengeEnthaelt(menge: MengenAusdruck, objekt: MathematischesObjekt): Boolean = when (menge) {
    is EndlicheMenge -> objekt in menge.elemente
    NatürlicheZahlen -> objekt is RationaleZahl && objekt.nenner == java.math.BigInteger.ONE &&
        objekt.zähler.signum() >= 0
    GanzeZahlen -> objekt is RationaleZahl && objekt.nenner == java.math.BigInteger.ONE
    RationaleZahlen, ReelleZahlen, KomplexeZahlen -> objekt is ZahlAusdruck
    is Vektorraum -> when (objekt) {
        is SpaltenVektor -> menge.orientierung == VektorOrientierung.Spalte &&
            objekt.werte.size == menge.dimension
        is ZeilenVektor -> menge.orientierung == VektorOrientierung.Zeile &&
            objekt.werte.size == menge.dimension
        else -> false
    }
    is Matrizenraum -> objekt is Matrix &&
        objekt.zeilenAnzahl == menge.zeilen &&
        objekt.spaltenAnzahl == menge.spalten
    else -> false
}

internal fun endlicheElementeOderNull(menge: MengenAusdruck): List<MathematischesObjekt>? =
    (menge as? EndlicheMenge)?.elemente?.sortedBy { it.zuLatex() }

internal fun zahlenZeugen(menge: MengenAusdruck): List<MathematischesObjekt> = when (menge) {
    NatürlicheZahlen -> listOf(0L, 1L, 2L).map { RationaleZahl.von(it) }
    GanzeZahlen, RationaleZahlen, ReelleZahlen, KomplexeZahlen ->
        listOf(-1L, 0L, 1L, 2L).map { RationaleZahl.von(it) }
    else -> emptyList()
}

private fun pruefeEndlicheAdditiveGruppe(
    traeger: EndlicheMenge,
    addition: Methode,
): List<BegriffsAxiomPruefung> {
    val elemente = traeger.elemente.sortedBy { it.zuLatex() }
    fun bild(a: MathematischesObjekt, b: MathematischesObjekt) = addition.wendeBinärAn(a, b)

    fun widerlegung(
        id: String,
        name: String,
        begruendung: String,
        werte: Map<String, MathematischesObjekt>,
    ) = BegriffsAxiomPruefung(id, name, NachweisStatus.Widerlegt, begruendung, werte)

    for (u in elemente) for (v in elemente) {
        val summe = runCatching { bild(u, v) }.getOrElse {
            return listOf(widerlegung(
                "abschluss",
                "Abgeschlossenheit",
                "Die Addition konnte für konkrete Trägerelemente nicht ausgewertet werden.",
                mapOf("u" to u, "v" to v),
            ))
        }
        if (summe !in traeger.elemente) {
            return listOf(widerlegung(
                "abschluss",
                "Abgeschlossenheit",
                "u + v liegt nicht in der Trägermenge.",
                mapOf("u" to u, "v" to v, "u+v" to summe),
            ))
        }
        if (bild(u, v) != bild(v, u)) {
            return listOf(widerlegung(
                "kommutativ",
                "Kommutativität",
                "u + v und v + u sind verschieden.",
                mapOf("u" to u, "v" to v),
            ))
        }
    }

    for (u in elemente) for (v in elemente) for (w in elemente) {
        if (bild(bild(u, v), w) != bild(u, bild(v, w))) {
            return listOf(widerlegung(
                "assoziativ",
                "Assoziativität",
                "(u + v) + w und u + (v + w) sind verschieden.",
                mapOf("u" to u, "v" to v, "w" to w),
            ))
        }
    }

    val nullElement = elemente.firstOrNull { kandidat ->
        elemente.all { u -> bild(kandidat, u) == u && bild(u, kandidat) == u }
    } ?: return listOf(
        BegriffsAxiomPruefung(
            "nullvektor",
            "Nullvektor",
            NachweisStatus.Widerlegt,
            "Es existiert kein beidseitig neutrales Element der Addition.",
        ),
    )

    for (u in elemente) {
        val inverse = elemente.firstOrNull { v -> bild(u, v) == nullElement && bild(v, u) == nullElement }
        if (inverse == null) {
            return listOf(widerlegung(
                "inverses",
                "Additives Inverses",
                "Für u existiert kein additives Inverses.",
                mapOf("u" to u),
            ))
        }
    }

    return listOf(
        BegriffsAxiomPruefung("abschluss", "Abgeschlossenheit", NachweisStatus.Nachgewiesen, "Endlich vollständig geprüft."),
        BegriffsAxiomPruefung("assoziativ", "Assoziativität", NachweisStatus.Nachgewiesen, "Endlich vollständig geprüft."),
        BegriffsAxiomPruefung("kommutativ", "Kommutativität", NachweisStatus.Nachgewiesen, "Endlich vollständig geprüft."),
        BegriffsAxiomPruefung("nullvektor", "Nullvektor", NachweisStatus.Nachgewiesen, "Neutrales Element ${nullElement.zuLatex()} gefunden."),
        BegriffsAxiomPruefung("inverses", "Additives Inverses", NachweisStatus.Nachgewiesen, "Endlich vollständig geprüft."),
    )
}

fun pruefeVektorraum(
    traegerMenge: MengenAusdruck,
    addition: Methode,
    skalareMultiplikation: Methode,
): BegriffsAussage {
    val diagnosen = mutableListOf<String>()
    val addVertrag = addition.vertragOderNull()
    val skalarVertrag = skalareMultiplikation.vertragOderNull()
    val skalarKoerper = skalarVertrag?.argumentMengen?.firstOrNull()

    val signaturFehler = buildList {
        if (addVertrag == null) add("Die Additionsmethode besitzt keine vollständige Signatur.")
        else {
            if (addVertrag.argumentMengen != listOf(traegerMenge, traegerMenge)) {
                add("Addition muss V × V als Definitionsmenge besitzen.")
            }
            if (addVertrag.zielMenge != traegerMenge) add("Addition muss nach V abbilden.")
        }
        if (skalarVertrag == null) add("Die skalare Multiplikation besitzt keine vollständige Signatur.")
        else {
            if (skalarVertrag.argumentMengen.size != 2 ||
                skalarVertrag.argumentMengen.getOrNull(1) != traegerMenge
            ) {
                add("Skalare Multiplikation muss K × V als Definitionsmenge besitzen.")
            }
            if (skalarVertrag.zielMenge != traegerMenge) {
                add("Skalare Multiplikation muss nach V abbilden.")
            }
        }
    }

    if (signaturFehler.isNotEmpty() || skalarKoerper == null) {
        diagnosen += signaturFehler
        val pruefungen = listOf(
            BegriffsAxiomPruefung(
                "signatur",
                "Operationssignaturen",
                NachweisStatus.Widerlegt,
                signaturFehler.joinToString(" "),
            ),
        )
        return BegriffsAussage(
            BegriffsPruefung(
                VEKTORRAUM_BEGRIFF_ID,
                "Vektorraum",
                pruefungen,
                NachweisStatus.Widerlegt,
                diagnosen,
            ),
            "\\operatorname{Vektorraum}\\left(${traegerMenge.zuLatex()}\\right)",
        )
    }

    val pruefungen = mutableListOf<BegriffsAxiomPruefung>()
    pruefungen += BegriffsAxiomPruefung(
        "koerper",
        "Skalarkörper",
        if (istBekannterKoerper(skalarKoerper)) NachweisStatus.Nachgewiesen else NachweisStatus.Unentscheidbar,
        if (istBekannterKoerper(skalarKoerper)) {
            "${MengeOderRaumBezeichnung(skalarKoerper)} ist als Körper registriert."
        } else {
            "Für die Skalarmenge ist keine Körperstruktur registriert."
        },
    )

    val endlicherTraeger = traegerMenge as? EndlicheMenge
    if (endlicherTraeger != null && endlicherTraeger.elemente.size <= 128) {
        pruefungen += pruefeEndlicheAdditiveGruppe(endlicherTraeger, addition)
    } else if (
        traegerMenge == skalarKoerper &&
        istBekannterKoerper(skalarKoerper) &&
        kanonischeSkalarAddition(addition, skalarKoerper)
    ) {
        pruefungen += listOf(
            BegriffsAxiomPruefung("abschluss", "Abgeschlossenheit", NachweisStatus.Nachgewiesen, "Folgt aus der registrierten Körperaddition."),
            BegriffsAxiomPruefung("assoziativ", "Assoziativität", NachweisStatus.Nachgewiesen, "Folgt aus der registrierten Körperaddition."),
            BegriffsAxiomPruefung("kommutativ", "Kommutativität", NachweisStatus.Nachgewiesen, "Folgt aus der registrierten Körperaddition."),
            BegriffsAxiomPruefung("nullvektor", "Nullvektor", NachweisStatus.Nachgewiesen, "Die skalare Null ist neutral."),
            BegriffsAxiomPruefung("inverses", "Additives Inverses", NachweisStatus.Nachgewiesen, "Das additive Inverse stammt aus dem Körper."),
        )
    } else {
        pruefungen += listOf(
            "abschluss" to "Abgeschlossenheit",
            "assoziativ" to "Assoziativität",
            "kommutativ" to "Kommutativität",
            "nullvektor" to "Nullvektor",
            "inverses" to "Additives Inverses",
        ).map { (id, name) ->
            BegriffsAxiomPruefung(id, name, NachweisStatus.Unentscheidbar, "Für diese Struktur liegt kein vollständiger Nachweis vor.")
        }
    }

    val kanonischeSkalarmultiplikation =
        traegerMenge == skalarKoerper &&
            istBekannterKoerper(skalarKoerper) &&
            kanonischeSkalarMultiplikation(skalareMultiplikation, skalarKoerper)

    pruefungen += listOf(
        "distributivVektor" to "Distributivität über Vektoraddition",
        "distributivSkalar" to "Distributivität über Skalaraddition",
        "skalarAssoziativ" to "Assoziativität der Skalarmultiplikation",
        "skalarEins" to "Einselement des Körpers",
    ).map { (id, name) ->
        BegriffsAxiomPruefung(
            id,
            name,
            if (kanonischeSkalarmultiplikation) NachweisStatus.Nachgewiesen else NachweisStatus.Unentscheidbar,
            if (kanonischeSkalarmultiplikation) {
                "Folgt aus der registrierten Körpermultiplikation."
            } else {
                "Die allgemeine Skalarmultiplikation benötigt einen externen Nachweis oder eine endliche vollständige Prüfung."
            },
        )
    }

    val status = statusAus(pruefungen)
    if (status == NachweisStatus.Unentscheidbar) {
        diagnosen += "Die Signaturen passen, aber mindestens ein Vektorraumaxiom ist im aktuellen System nicht entscheidbar."
    }
    val zeugnis = if (status == NachweisStatus.Nachgewiesen) {
        VektorraumZeugnis(traegerMenge, addition, skalareMultiplikation, skalarKoerper)
    } else null

    return BegriffsAussage(
        BegriffsPruefung(
            VEKTORRAUM_BEGRIFF_ID,
            "Vektorraum",
            pruefungen,
            status,
            diagnosen,
            zeugnis,
            spezifikation = VEKTORRAUM_SPEZIFIKATION,
            kandidat = BegriffsKandidat(
                spezifikationId = VEKTORRAUM_BEGRIFF_ID,
                belegung = mapOf(
                    "menge" to traegerMenge,
                    "addition" to addition,
                    "skalareMultiplikation" to skalareMultiplikation,
                ),
            ),
        ),
        "${traegerMenge.zuLatex()}\\ \\operatorname{ist\\ ein\\ Vektorraum\\ ueber}\\ ${skalarKoerper.zuLatex()}",
    )
}


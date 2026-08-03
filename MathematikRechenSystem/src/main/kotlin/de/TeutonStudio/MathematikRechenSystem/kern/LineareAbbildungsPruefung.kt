package de.TeutonStudio.MathematikRechenSystem.kern

private fun istNullObjekt(objekt: MathematischesObjekt): Boolean = when (objekt) {
    is RationaleZahl -> objekt.istNull()
    is SpaltenVektor -> objekt.werte.all(::istNullObjekt)
    is ZeilenVektor -> objekt.werte.all(::istNullObjekt)
    is Matrix -> objekt.zeilen.flatten().all(::istNullObjekt)
    is Tupel -> objekt.elemente.all(::istNullObjekt)
    else -> false
}

private fun istIdentitaet(methode: Methode): Boolean =
    methode.parameter.size == 1 && methode.vorschrift == methode.parameter.single()

private data class LinearitaetsWiderlegung(
    val axiomId: String,
    val axiomName: String,
    val begruendung: String,
    val werte: Map<String, MathematischesObjekt>,
)

private fun sucheLinearitaetsWiderlegung(
    quelle: VektorraumZeugnis,
    ziel: VektorraumZeugnis,
    methode: Methode,
): LinearitaetsWiderlegung? {
    val vektoren = endlicheElementeOderNull(quelle.traegerMenge)
        ?: zahlenZeugen(quelle.traegerMenge)
    val skalare = endlicheElementeOderNull(quelle.skalarKoerper)
        ?: zahlenZeugen(quelle.skalarKoerper)
    if (vektoren.isEmpty() || skalare.isEmpty()) return null

    for (u in vektoren) for (v in vektoren) {
        val links = runCatching {
            methode.wendeAn(listOf(quelle.addition.wendeBinärAn(u, v)))
        }.getOrNull() ?: continue
        val rechts = runCatching {
            ziel.addition.wendeBinärAn(methode.wendeAn(listOf(u)), methode.wendeAn(listOf(v)))
        }.getOrNull() ?: continue
        if (links != rechts) {
            return LinearitaetsWiderlegung(
                "additiv",
                "Additivität",
                "f(u+v) ist nicht gleich f(u)+f(v).",
                mapOf("u" to u, "v" to v, "f(u+v)" to links, "f(u)+f(v)" to rechts),
            )
        }
    }

    for (a in skalare) for (u in vektoren) {
        val links = runCatching {
            methode.wendeAn(listOf(quelle.skalareMultiplikation.wendeBinärAn(a, u)))
        }.getOrNull() ?: continue
        val rechts = runCatching {
            ziel.skalareMultiplikation.wendeBinärAn(a, methode.wendeAn(listOf(u)))
        }.getOrNull() ?: continue
        if (links != rechts) {
            return LinearitaetsWiderlegung(
                "homogen",
                "Homogenität",
                "f(a·u) ist nicht gleich a·f(u).",
                mapOf("a" to a, "u" to u, "f(a·u)" to links, "a·f(u)" to rechts),
            )
        }
    }
    return null
}

fun pruefeLineareAbbildung(
    definitionsRaum: BegriffsAussage,
    zielRaum: BegriffsAussage,
    abbildung: Methode,
): BegriffsAussage {
    val quelle = definitionsRaum.pruefung.zeugnis as? VektorraumZeugnis
    val ziel = zielRaum.pruefung.zeugnis as? VektorraumZeugnis
    val diagnosen = mutableListOf<String>()

    if (quelle == null || ziel == null) {
        diagnosen += "Definitions- und Zielraum benötigen nachgewiesene Vektorraumzeugnisse."
        val pruefung = BegriffsAxiomPruefung(
            "raeume",
            "Vektorräume",
            NachweisStatus.Unvollstaendig,
            diagnosen.single(),
        )
        return BegriffsAussage(
            BegriffsPruefung(
                LINEARE_ABBILDUNG_BEGRIFF_ID,
                "Lineare Abbildung",
                listOf(pruefung),
                NachweisStatus.Unvollstaendig,
                diagnosen,
            ),
            "\\operatorname{Linear}\\left(${abbildung.name}\\right)",
        )
    }

    val vertrag = abbildung.vertragOderNull()
    val signaturGueltig = vertrag?.argumentMengen == listOf(quelle.traegerMenge) &&
        vertrag.zielMenge == ziel.traegerMenge
    val gleicherKoerper = quelle.skalarKoerper == ziel.skalarKoerper
    val pruefungen = mutableListOf(
        BegriffsAxiomPruefung(
            "signatur",
            "Abbildungssignatur",
            if (signaturGueltig) NachweisStatus.Nachgewiesen else NachweisStatus.Widerlegt,
            if (signaturGueltig) "Die Methode bildet V nach W ab." else "Die Methode muss genau V nach W abbilden.",
        ),
        BegriffsAxiomPruefung(
            "koerper",
            "Gemeinsamer Skalarkörper",
            if (gleicherKoerper) NachweisStatus.Nachgewiesen else NachweisStatus.Widerlegt,
            if (gleicherKoerper) "Definitions- und Zielraum verwenden denselben Skalarkörper."
            else "Definitions- und Zielraum verwenden verschiedene Skalarkörper.",
        ),
    )

    if (!signaturGueltig || !gleicherKoerper) {
        val status = statusAus(pruefungen)
        return BegriffsAussage(
            BegriffsPruefung(
                LINEARE_ABBILDUNG_BEGRIFF_ID,
                "Lineare Abbildung",
                pruefungen,
                status,
                diagnosen,
            ),
            "${abbildung.name}\\notin\\operatorname{Hom}\\left(${quelle.traegerMenge.zuLatex()},${ziel.traegerMenge.zuLatex()}\\right)",
        )
    }

    val widerlegung = sucheLinearitaetsWiderlegung(quelle, ziel, abbildung)
    if (widerlegung != null) {
        pruefungen += BegriffsAxiomPruefung(
            widerlegung.axiomId,
            widerlegung.axiomName,
            NachweisStatus.Widerlegt,
            widerlegung.begruendung,
            widerlegung.werte,
        )
        val anderesAxiom = if (widerlegung.axiomId == "additiv") "homogen" to "Homogenität"
        else "additiv" to "Additivität"
        pruefungen += BegriffsAxiomPruefung(
            anderesAxiom.first,
            anderesAxiom.second,
            NachweisStatus.Unentscheidbar,
            "Nach der ersten Widerlegung wurde keine Vollprüfung erzwungen.",
        )
    } else {
        val eindeutigNachgewiesen = when {
            istIdentitaet(abbildung) && quelle.traegerMenge == ziel.traegerMenge -> true
            istNullObjekt(abbildung.vorschrift) -> true
            else -> {
                val quelleEndlich = endlicheElementeOderNull(quelle.traegerMenge)
                val skalareEndlich = endlicheElementeOderNull(quelle.skalarKoerper)
                quelleEndlich != null && skalareEndlich != null
            }
        }
        val status = if (eindeutigNachgewiesen) NachweisStatus.Nachgewiesen else NachweisStatus.Unentscheidbar
        val begruendung = when {
            istIdentitaet(abbildung) -> "Die Identität erfüllt beide Linearitätsaxiome."
            istNullObjekt(abbildung.vorschrift) -> "Die Nullabbildung erfüllt beide Linearitätsaxiome."
            eindeutigNachgewiesen -> "Die endlichen Wertebereiche wurden vollständig geprüft."
            else -> "Kein Gegenbeispiel gefunden; ein allgemeiner symbolischer Beweis fehlt."
        }
        pruefungen += BegriffsAxiomPruefung("additiv", "Additivität", status, begruendung)
        pruefungen += BegriffsAxiomPruefung("homogen", "Homogenität", status, begruendung)
    }

    val status = statusAus(pruefungen)
    val zeugnis = if (status == NachweisStatus.Nachgewiesen) {
        LineareAbbildungsZeugnis(quelle, ziel, abbildung)
    } else null
    if (status == NachweisStatus.Unentscheidbar) {
        diagnosen += "Die Methode ist typkorrekt, aber ihre Linearität ist im aktuellen System nicht vollständig beweisbar."
    }

    return BegriffsAussage(
        BegriffsPruefung(
            LINEARE_ABBILDUNG_BEGRIFF_ID,
            "Lineare Abbildung",
            pruefungen,
            status,
            diagnosen,
            zeugnis,
            spezifikation = LINEARE_ABBILDUNG_SPEZIFIKATION,
            kandidat = BegriffsKandidat(
                spezifikationId = LINEARE_ABBILDUNG_BEGRIFF_ID,
                belegung = mapOf(
                    "definitionsraum" to definitionsRaum,
                    "zielraum" to zielRaum,
                    "methode" to abbildung,
                ),
            ),
        ),
        "${abbildung.name}\\in\\operatorname{Hom}_{${quelle.skalarKoerper.zuLatex()}}" +
            "\\left(${quelle.traegerMenge.zuLatex()},${ziel.traegerMenge.zuLatex()}\\right)",
    )
}

package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val POTENZ_STRUKTUR_KNOTEN_ART = "mathematik.potenzStruktur"
const val POTENZ_STRUKTUR_BEZEICHNUNG_PARAMETER = "potenzStruktur.id"
const val POTENZ_STRUKTUR_OPERATOR_PARAMETER = "potenzStruktur.operatorId"

object PotenzStrukturKnotenVorlagen {
    val Struktur = KnotenVorlage(
        art = POTENZ_STRUKTUR_KNOTEN_ART,
        name = "Potenzstruktur",
        kategorie = "Algebra: Strukturen",
        beschreibung = "Bündelt Träger, binäre innere Multiplikation, Assoziativität und optionales neutrales Element zu einem ausführbaren Potenzzeugnis.",
        standardGröße = GraphGröße(315f, 190f),
        anschlüsse = listOf(
            strukturEingang("traeger", MathematikAnschlussArten.Menge.id, 0),
            strukturEingang("multiplikation", MathematikAnschlussArten.Methode.id, 1),
            strukturEingang("abgeschlossen", MathematikAnschlussArten.Aussage.id, 2),
            strukturEingang("assoziativ", MathematikAnschlussArten.Aussage.id, 3),
            strukturEingang("neutral", MathematikAnschlussArten.Objekt.id, 4),
            strukturEingang("neutralitaet", MathematikAnschlussArten.Aussage.id, 5),
            AnschlussDaten(
                name = "struktur",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Objekt.id,
            ),
        ),
        standardParameter = mapOf(
            POTENZ_STRUKTUR_BEZEICHNUNG_PARAMETER to "eigene.potenzstruktur",
            POTENZ_STRUKTUR_OPERATOR_PARAMETER to "eigene.multiplikation",
        ),
    )

    val alle = listOf(Struktur)
}

internal fun MathematikAuswerterRegister.registrierePotenzStrukturKnoten() {
    registriere(POTENZ_STRUKTUR_KNOTEN_ART) { kontext ->
        kontext.wertePotenzStrukturAus()
    }
}

private fun KnotenAuswertungsKontext.wertePotenzStrukturAus(): KnotenAuswertungsErgebnis {
    val traeger = eingänge["traeger"]?.objekt as? MengenAusdruck
        ?: return fehlerErgebnis("Die Potenzstruktur benötigt eine Trägermenge.")
    val multiplikation = eingänge["multiplikation"]?.objekt as? Methode
        ?: return fehlerErgebnis("Die Potenzstruktur benötigt eine binäre Multiplikationsmethode.")
    if (multiplikation.parameter.size != 2) {
        return fehlerErgebnis("Die Multiplikationsmethode muss genau zwei formale Argumente besitzen.")
    }

    val signaturPruefung = pruefeInnereMultiplikation(
        methode = multiplikation,
        traeger = traeger,
        kontext = rechenKontext,
    )
    signaturPruefung.fehler?.let { return fehlerErgebnis(it) }

    val abgeschlossenAussage = eingänge["abgeschlossen"]?.objekt as? Aussage
    val assoziativAussage = eingänge["assoziativ"]?.objekt as? Aussage
    val neutral = eingänge["neutral"]?.objekt
    val neutralitaetsAussage = eingänge["neutralitaet"]?.objekt as? Aussage
    if (neutral == null && neutralitaetsAussage != null) {
        return fehlerErgebnis("Ein Neutralitätsnachweis ohne verbundenes neutrales Element ist unvollständig.")
    }

    val neutralPruefung = pruefeNeutralesElement(
        neutral = neutral,
        traeger = traeger,
        kontext = rechenKontext,
    )
    neutralPruefung.fehler?.let { return fehlerErgebnis(it) }

    val struktur = PotenzStruktur(
        id = knoten.parameter[POTENZ_STRUKTUR_BEZEICHNUNG_PARAMETER]
            .orEmpty().ifBlank { "eigene.potenzstruktur" },
        traeger = PotenzTraeger.Explizit(traeger),
        multiplikationsOperatorId = knoten.parameter[POTENZ_STRUKTUR_OPERATOR_PARAMETER]
            .orEmpty().ifBlank { "eigene.multiplikation" },
        abgeschlossenheit = statusAusAussage(abgeschlossenAussage)
            .mitZusaetzlichenBedingungen(signaturPruefung.voraussetzungen),
        assoziativitaet = statusAusAussage(assoziativAussage),
        neutralesElement = neutral,
        neutralitaet = if (neutral == null) {
            NachweisStatus.Unvollstaendig
        } else {
            statusAusAussage(neutralitaetsAussage)
                .mitZusaetzlichenBedingungen(neutralPruefung.voraussetzungen)
        },
        multiplikationsMethode = multiplikation,
    )
    val annahmen = gemeinsameAnnahmen() +
        signaturPruefung.voraussetzungen +
        neutralPruefung.voraussetzungen +
        struktur.abgeschlossenheit.bedingungenOderLeer() +
        struktur.assoziativitaet.bedingungenOderLeer() +
        struktur.neutralitaet.bedingungenOderLeer()
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf(
            "struktur" to BedingterWert(
                objekt = struktur,
                annahmen = annahmen,
            ),
        ),
        warnungen = buildList {
            add("Träger: ${traeger.zuLatex()}")
            add("Multiplikation: ${multiplikation.name}")
            add("Abgeschlossenheit: ${struktur.abgeschlossenheit.statusName()}")
            add("Assoziativität: ${struktur.assoziativitaet.statusName()}")
            add("Neutralität: ${struktur.neutralitaet.statusName()}")
            if (signaturPruefung.voraussetzungen.isNotEmpty()) {
                add("Die innere Signatur bleibt unter ${signaturPruefung.voraussetzungen.size} Voraussetzung(en) gültig.")
            }
            if (neutralPruefung.voraussetzungen.isNotEmpty()) {
                add("Die Trägerzugehörigkeit des neutralen Elements bleibt offen.")
            }
        },
        eingänge = eingänge,
    )
}

private data class StrukturVertragsPruefung(
    val voraussetzungen: Set<Aussage> = emptySet(),
    val fehler: String? = null,
)

private fun pruefeInnereMultiplikation(
    methode: Methode,
    traeger: MengenAusdruck,
    kontext: RechenKontext,
): StrukturVertragsPruefung {
    val voraussetzungen = linkedSetOf<Aussage>()
    methode.parameter.forEach { parameter ->
        val bereich = methode.werteVorräte[parameter.name]
            ?: return StrukturVertragsPruefung(
                fehler = "Für das Argument '${parameter.name}' fehlt der Wertevorrat.",
            )
        if (bereich != traeger) {
            val beziehung = TeilmengenBeziehung(traeger, bereich)
            when (val pruefung = pruefeVertragsAussage(
                aussage = beziehung,
                kontext = kontext,
                widerlegtNachricht = "Der Träger ${traeger.zuLatex()} liegt nicht im Wertevorrat ${bereich.zuLatex()} des Arguments '${parameter.name}'.",
            )) {
                is StrukturVertragsPruefung -> {
                    pruefung.fehler?.let { return pruefung }
                    voraussetzungen += pruefung.voraussetzungen
                }
            }
        }
    }

    if (methode.zielMenge != traeger) {
        val beziehung = TeilmengenBeziehung(methode.zielMenge, traeger)
        val pruefung = pruefeVertragsAussage(
            aussage = beziehung,
            kontext = kontext,
            widerlegtNachricht = "Die Zielmenge ${methode.zielMenge.zuLatex()} der Multiplikation liegt nicht im Träger ${traeger.zuLatex()}.",
        )
        pruefung.fehler?.let { return pruefung }
        voraussetzungen += pruefung.voraussetzungen
    }
    return StrukturVertragsPruefung(voraussetzungen)
}

private fun pruefeNeutralesElement(
    neutral: MathematischesObjekt?,
    traeger: MengenAusdruck,
    kontext: RechenKontext,
): StrukturVertragsPruefung {
    if (neutral == null) return StrukturVertragsPruefung()
    return pruefeVertragsAussage(
        aussage = ElementBeziehung(neutral, traeger),
        kontext = kontext,
        widerlegtNachricht = "Das angegebene neutrale Element ${neutral.zuLatex()} liegt nicht im Träger ${traeger.zuLatex()}.",
    )
}

private fun pruefeVertragsAussage(
    aussage: Aussage,
    kontext: RechenKontext,
    widerlegtNachricht: String,
): StrukturVertragsPruefung = when (aussage.entscheide(kontext).wahrheitswert) {
    Wahrheitswert.Wahr -> StrukturVertragsPruefung()
    Wahrheitswert.Lüge -> StrukturVertragsPruefung(fehler = widerlegtNachricht)
    null -> StrukturVertragsPruefung(voraussetzungen = setOf(aussage))
}

private fun KnotenAuswertungsKontext.statusAusAussage(aussage: Aussage?): NachweisStatus {
    if (aussage == null) return NachweisStatus.Unvollstaendig
    return when (aussage.entscheide(rechenKontext).wahrheitswert) {
        Wahrheitswert.Wahr -> NachweisStatus.Nachgewiesen
        Wahrheitswert.Lüge -> NachweisStatus.Widerlegt
        null -> NachweisStatus.Bedingt(listOf(aussage))
    }
}

private fun NachweisStatus.mitZusaetzlichenBedingungen(
    bedingungen: Set<Aussage>,
): NachweisStatus = when {
    bedingungen.isEmpty() -> this
    this == NachweisStatus.Widerlegt -> this
    this is NachweisStatus.Bedingt -> NachweisStatus.Bedingt(
        (this.bedingungen + bedingungen).distinct(),
    )
    this == NachweisStatus.Nachgewiesen -> NachweisStatus.Bedingt(bedingungen.toList())
    else -> NachweisStatus.Bedingt(bedingungen.toList())
}

private fun NachweisStatus.bedingungenOderLeer(): Set<Aussage> = when (this) {
    is NachweisStatus.Bedingt -> bedingungen.toSet()
    else -> emptySet()
}

private fun NachweisStatus.statusName(): String = when (this) {
    NachweisStatus.Nachgewiesen -> "nachgewiesen"
    NachweisStatus.Widerlegt -> "widerlegt"
    is NachweisStatus.Bedingt -> "bedingt"
    NachweisStatus.Unvollstaendig -> "unvollständig"
    NachweisStatus.Unentscheidbar -> "unentscheidbar"
}

private fun KnotenAuswertungsKontext.gemeinsameAnnahmen(): Set<Aussage> =
    eingänge.values.flatMap { it.annahmen }.toSet()

private fun KnotenAuswertungsKontext.fehlerErgebnis(nachricht: String): KnotenAuswertungsErgebnis =
    KnotenAuswertungsErgebnis(
        ausgaben = emptyMap(),
        fehler = nachricht,
        eingänge = eingänge,
    )

private fun strukturEingang(
    name: String,
    art: AnschlussArtId,
    reihenfolge: Int,
): AnschlussDaten = AnschlussDaten(
    name = name,
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = art,
    reihenfolge = reihenfolge,
)

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

    val signaturPruefung = pruefeInnereMultiplikation(multiplikation, traeger)
    if (signaturPruefung.fehler != null) {
        return fehlerErgebnis(signaturPruefung.fehler)
    }
    val abgeschlossenAussage = eingänge["abgeschlossen"]?.objekt as? Aussage
    val assoziativAussage = eingänge["assoziativ"]?.objekt as? Aussage
    val neutral = eingänge["neutral"]?.objekt
    val neutralitaetsAussage = eingänge["neutralitaet"]?.objekt as? Aussage
    if (neutral == null && neutralitaetsAussage != null) {
        return fehlerErgebnis("Ein Neutralitätsnachweis ohne verbundenes neutrales Element ist unvollständig.")
    }

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
        },
        multiplikationsMethode = multiplikation,
    )
    val annahmen = gemeinsameAnnahmen() + signaturPruefung.voraussetzungen +
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
        },
        eingänge = eingänge,
    )
}

private data class InnereMultiplikationsPruefung(
    val voraussetzungen: Set<Aussage> = emptySet(),
    val fehler: String? = null,
)

private fun pruefeInnereMultiplikation(
    methode: Methode,
    traeger: MengenAusdruck,
): InnereMultiplikationsPruefung {
    val voraussetzungen = linkedSetOf<Aussage>()
    methode.parameter.forEach { parameter ->
        val bereich = methode.werteVorräte[parameter.name]
            ?: return InnereMultiplikationsPruefung(
                fehler = "Für das Argument '${parameter.name}' fehlt der Wertevorrat.",
            )
        when {
            bereich == traeger -> Unit
            else -> voraussetzungen += TeilmengenBeziehung(traeger, bereich)
        }
    }
    when {
        methode.zielMenge == traeger -> Unit
        else -> voraussetzungen += TeilmengenBeziehung(methode.zielMenge, traeger)
    }
    return InnereMultiplikationsPruefung(voraussetzungen)
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

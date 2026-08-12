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

const val BEGRIFF_SKALARPRODUKT_KNOTEN_ART = "mathematik.begriff.skalarprodukt"
const val SKALARPRODUKT_DEFINITION_PARAMETER = "skalarprodukt.definition"
const val SKALARPRODUKT_DEFINITION_STANDARD = "standard"
const val SKALARPRODUKT_DEFINITION_ZERTIFIZIERT = "zertifiziert"
const val SKALARPRODUKT_DEFINITION_EINGANG = "definition"
const val SKALARPRODUKT_ZAHLBEREICH_PARAMETER = "skalarprodukt.zahlbereich"
const val SKALARPRODUKT_LINEARITAET_PARAMETER = "skalarprodukt.linearitaet"
const val SKALARPRODUKT_ZERTIFIKAT_VERSION_PARAMETER = "skalarprodukt.zertifikatVersion"
const val SKALARPRODUKT_NACHWEIS_LINEARITAET = "skalarprodukt.nachweis.linearitaet"
const val SKALARPRODUKT_NACHWEIS_SYMMETRIE = "skalarprodukt.nachweis.symmetrie"
const val SKALARPRODUKT_NACHWEIS_POSITIV = "skalarprodukt.nachweis.positiv"

object SkalarproduktKnotenVorlagen {
    val Begriff = KnotenVorlage(
        art = BEGRIFF_SKALARPRODUKT_KNOTEN_ART,
        name = "Skalarprodukt überprüfen",
        kategorie = "Lineare Algebra: Begriffe",
        beschreibung = "Zertifiziert eine zweistellige Methode als Skalarprodukt. Fehlende Nachweisreferenzen bleiben ausdrücklich unvollständig.",
        standardGröße = GraphGröße(315f, 125f),
        anschlüsse = listOf(
            AnschlussDaten(
                name = "methode",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Methode.id,
            ),
            AnschlussDaten(
                name = "aussage",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Aussage.id,
            ),
        ),
        standardParameter = mapOf(
            SKALARPRODUKT_ZAHLBEREICH_PARAMETER to FundamentalerZahlbereich.REELL.id,
            SKALARPRODUKT_LINEARITAET_PARAMETER to SkalarproduktLinearitaet.RECHTSLINEAR.name,
            SKALARPRODUKT_ZERTIFIKAT_VERSION_PARAMETER to SKALARPRODUKT_ZERTIFIKAT_VERSION.toString(),
            SKALARPRODUKT_NACHWEIS_LINEARITAET to "",
            SKALARPRODUKT_NACHWEIS_SYMMETRIE to "",
            SKALARPRODUKT_NACHWEIS_POSITIV to "",
        ),
    )

    val alle = listOf(Begriff)
}

internal fun MathematikAuswerterRegister.registriereSkalarproduktErweiterungen() {
    val bisherigerVektorRechner = requireNotNull(finde(VektorRechner.KNOTEN_ART)) {
        "Der konsolidierte Vektorrechner muss vor der Skalarprodukt-Erweiterung registriert sein."
    }

    registriere(BEGRIFF_SKALARPRODUKT_KNOTEN_ART) { kontext ->
        val methode = kontext.eingänge["methode"]?.objekt as? Methode
            ?: error("Die zu zertifizierende Methode fehlt.")
        val zahlbereich = zahlbereichAusId(
            kontext.knoten.parameter[SKALARPRODUKT_ZAHLBEREICH_PARAMETER],
        )
        val linearitaet = skalarproduktLinearitaet(
            kontext.knoten.parameter[SKALARPRODUKT_LINEARITAET_PARAMETER],
        )
        val referenzen = SkalarproduktNachweisReferenzen(
            linearitaet = kontext.knoten.parameter[SKALARPRODUKT_NACHWEIS_LINEARITAET].orEmpty(),
            konjugierteSymmetrie = kontext.knoten.parameter[SKALARPRODUKT_NACHWEIS_SYMMETRIE].orEmpty(),
            positivDefinit = kontext.knoten.parameter[SKALARPRODUKT_NACHWEIS_POSITIV].orEmpty(),
        )
        val version = kontext.knoten.parameter[SKALARPRODUKT_ZERTIFIKAT_VERSION_PARAMETER]
            ?.toIntOrNull()
            ?: SKALARPRODUKT_ZERTIFIKAT_VERSION
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "aussage" to BedingterWert(
                    objekt = pruefeSkalarprodukt(
                        methode = methode,
                        zahlbereich = zahlbereich,
                        linearitaet = linearitaet,
                        referenzen = referenzen,
                        zertifikatVersion = version,
                    ),
                    annahmen = kontext.gemeinsameSkalarproduktAnnahmen(),
                    zielMenge = WahrheitsMenge,
                ),
            ),
            eingänge = kontext.eingänge,
        )
    }

    registriere(VektorRechner.KNOTEN_ART) { kontext ->
        val operator = VektorRechnerOperator.entries.firstOrNull {
            it.stabileId == kontext.knoten.parameter[VEKTOR_RECHNER_OPERATOR]
        } ?: VektorRechnerOperator.SKALARPRODUKT
        if (operator != VektorRechnerOperator.SKALARPRODUKT) {
            return@registriere bisherigerVektorRechner.auswerten(kontext)
        }
        kontext.werteSkalarproduktAus()
    }
}

private fun KnotenAuswertungsKontext.werteSkalarproduktAus(): KnotenAuswertungsErgebnis {
    val links = mathematischerEingang("links", "Linker Skalarproduktoperand")
    val rechts = mathematischerEingang("rechts", "Rechter Skalarproduktoperand")
    val linkeAnsicht = when (val ergebnis = links.numerischeKomponentenAnsicht()) {
        is StrukturPruefung.Gueltig -> ergebnis.wert
        is StrukturPruefung.Bedingt -> error(ergebnis.bedingungen.joinToString())
        is StrukturPruefung.Ungueltig -> error(ergebnis.grund)
        is StrukturPruefung.Unentscheidbar -> error(ergebnis.grund)
    }
    val rechteAnsicht = when (val ergebnis = rechts.numerischeKomponentenAnsicht()) {
        is StrukturPruefung.Gueltig -> ergebnis.wert
        is StrukturPruefung.Bedingt -> error(ergebnis.bedingungen.joinToString())
        is StrukturPruefung.Ungueltig -> error(ergebnis.grund)
        is StrukturPruefung.Unentscheidbar -> error(ergebnis.grund)
    }
    require(linkeAnsicht.laenge == rechteAnsicht.laenge) {
        "Das Skalarprodukt benötigt gleich lange Komponentenfolgen."
    }

    val zahlMenge = maximaleZahlenGrundmenge(
        listOf(linkeAnsicht.zahlBereich, rechteAnsicht.zahlBereich),
    )
    val deklarierterZahlbereich = zahlbereichAusId(
        knoten.parameter[SKALARPRODUKT_ZAHLBEREICH_PARAMETER],
    )
    val inferierterZahlbereich = zahlMenge.skalarproduktZahlbereichOderNull()
        ?: error("Der gemeinsame Skalarkörper ist nicht als fundamentaler Zahlbereich registriert.")
    val zahlbereich = if (deklarierterZahlbereich == FundamentalerZahlbereich.QUATERNION) {
        FundamentalerZahlbereich.QUATERNION
    } else {
        inferierterZahlbereich
    }
    val konfigurierteLinearitaet = skalarproduktLinearitaet(
        knoten.parameter[SKALARPRODUKT_LINEARITAET_PARAMETER],
    )
    val linearitaet = if (zahlbereich == FundamentalerZahlbereich.QUATERNION) {
        konfigurierteLinearitaet
    } else {
        SkalarproduktLinearitaet.RECHTSLINEAR
    }
    val definition = knoten.parameter[SKALARPRODUKT_DEFINITION_PARAMETER]
        ?: SKALARPRODUKT_DEFINITION_STANDARD

    val (wert, darstellung) = if (definition == SKALARPRODUKT_DEFINITION_ZERTIFIZIERT) {
        val zertifikatsMethode = eingänge[SKALARPRODUKT_DEFINITION_EINGANG]?.objekt as? Methode
            ?: error("Die ausgewählte Zertifikatskarte liefert keine Methode.")
        require(zertifikatsMethode.parameter.isEmpty()) {
            "Eine Skalarprodukt-Zertifikatskarte darf keine öffentlichen Eingänge besitzen."
        }
        val aussage = zertifikatsMethode.wendeAn(emptyList()) as? BegriffsAussage
            ?: error("Die Zertifikatskarte muss eine Skalarprodukt-Aussage ausgeben.")
        skalarproduktZertifikatFehler(aussage, zahlbereich, linearitaet)?.let(::error)
        val zeugnis = aussage.pruefung.zeugnis as SkalarproduktZeugnis
        val ausgabe = zeugnis.methode.wendeAn(listOf(links, rechts)) as? ZahlAusdruck
            ?: error("Die zertifizierte Skalarproduktmethode liefert keinen Zahlterm.")
        ausgabe to "${zeugnis.methode.name}\\left(${links.zuLatex()},${rechts.zuLatex()}\\right)"
    } else {
        val konjugiert = zahlbereich in setOf(
            FundamentalerZahlbereich.KOMPLEX,
            FundamentalerZahlbereich.QUATERNION,
        )
        val ergebnis = standardSkalarprodukt(
            links = links,
            rechts = rechts,
            spezifikation = SkalarproduktSpezifikation(
                linearitaet = linearitaet,
                konjugiert = konjugiert,
            ),
        )
        val ausgabe = when (ergebnis) {
            is StrukturPruefung.Gueltig -> ergebnis.wert
            is StrukturPruefung.Bedingt -> ergebnis.wert ?: error(ergebnis.bedingungen.joinToString())
            is StrukturPruefung.Ungueltig -> error(ergebnis.grund)
            is StrukturPruefung.Unentscheidbar -> error(ergebnis.grund)
        }
        ausgabe to SkalarproduktFalkSchema(
            dimension = linkeAnsicht.laenge,
            linearitaet = linearitaet,
            konjugiert = konjugiert,
        ).zuLatex()
    }

    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf(
            VEKTOR_RECHNER_AUSGANG to BedingterWert(
                objekt = wert,
                annahmen = gemeinsameSkalarproduktAnnahmen(),
                zielMenge = zahlbereich.alsMenge(),
                latexDarstellung = darstellung,
            ),
        ),
        eingänge = eingänge,
    )
}

private fun skalarproduktLinearitaet(wert: String?): SkalarproduktLinearitaet =
    SkalarproduktLinearitaet.entries.firstOrNull { it.name == wert }
        ?: SkalarproduktLinearitaet.RECHTSLINEAR

private fun zahlbereichAusId(wert: String?): FundamentalerZahlbereich =
    FundamentalerZahlbereich.entries.firstOrNull { it.id == wert || it.name == wert }
        ?: FundamentalerZahlbereich.REELL

private fun KnotenAuswertungsKontext.gemeinsameSkalarproduktAnnahmen(): Set<Aussage> =
    eingänge.values.flatMap { it.annahmen }.toSet()

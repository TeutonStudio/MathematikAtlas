package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val INTEGRAL_KNOTEN_ART = "mathematik.integral"
const val INTEGRAL_AUSGABEFORM_PARAMETER = "integral.ausgabeForm"
const val INTEGRAL_METHODEN_DARSTELLUNG_PARAMETER = "integral.methodenDarstellung"
const val INTEGRAL_MASS_MODUS_PARAMETER = "integral.massModus"
const val INTEGRAL_MASS_SYMBOL_PARAMETER = "integral.massSymbol"
const val INTEGRAL_QUELLEN_IDS_PARAMETER = "integral.quellenIds"
const val INTEGRAL_MIGRATIONSFEHLER_PARAMETER = "integral.migrationsFehler"

private const val HISTORISCHES_METHODEN_INTEGRAL = "mathematik.integralMethode"
private const val HISTORISCHES_TERM_INTEGRAL = "mathematik.integralTerm"

enum class IntegralMassModus {
    AUTO,
    STANDARD_REELL,
    ZAEHLMASS,
    ALLGEMEIN,
    NICHTSTANDARD,
}

object IntegralKnotenVorlagen {
    val Integral = KnotenVorlage(
        art = INTEGRAL_KNOTEN_ART,
        name = "Integral",
        kategorie = "Analysis: Integralrechnung",
        beschreibung = "Ein gemeinsamer Integralknoten für Methoden- und Termform mit explizitem Maßvertrag und strukturierter Variablenbindung.",
        standardGröße = GraphGröße(295f, 145f),
        anschlüsse = integralAnschluesse(IntegralAusgabeform.METHODE),
        standardParameter = mapOf(
            INTEGRAL_AUSGABEFORM_PARAMETER to IntegralAusgabeform.METHODE.name,
            INTEGRAL_METHODEN_DARSTELLUNG_PARAMETER to IntegralMethodenDarstellung.KURZ.name,
            INTEGRAL_MASS_MODUS_PARAMETER to IntegralMassModus.AUTO.name,
            INTEGRAL_MASS_SYMBOL_PARAMETER to "\\mu",
            INTEGRAL_QUELLEN_IDS_PARAMETER to "integral.variable",
        ),
    )

    val alle = listOf(Integral)
}

data class IntegralModusWechselDiagnose(
    val ausgabeform: IntegralAusgabeform,
    val erhalteneAnschlussIds: Set<AnschlussId>,
    val entfernteAnschlussIds: Set<AnschlussId>,
    val verbundeneEntfernteAnschlussIds: Set<AnschlussId>,
) {
    val trenntVerbindungen: Boolean get() = verbundeneEntfernteAnschlussIds.isNotEmpty()
}

fun aktuelleIntegralAusgabeform(knoten: KnotenDaten): IntegralAusgabeform =
    IntegralAusgabeform.entries.firstOrNull {
        it.name == knoten.parameter[INTEGRAL_AUSGABEFORM_PARAMETER]
    } ?: IntegralAusgabeform.METHODE

fun konfiguriereIntegralKnoten(
    knoten: KnotenDaten,
    ausgabeform: IntegralAusgabeform,
): KnotenDaten {
    require(knoten.art == INTEGRAL_KNOTEN_ART)
    val gewuenscht = integralAnschluesse(ausgabeform)
    val bestehend = knoten.anschlüsse.associateBy { it.richtung to it.name }
    val anschluesse = gewuenscht.map { neu ->
        bestehend[neu.richtung to neu.name]?.let { alt -> neu.copy(id = alt.id) } ?: neu
    }
    return knoten.copy(
        name = when (ausgabeform) {
            IntegralAusgabeform.METHODE -> "Integral einer Methode"
            IntegralAusgabeform.TERM -> "Integral eines Terms"
        },
        anschlüsse = anschluesse,
        parameter = knoten.parameter + (INTEGRAL_AUSGABEFORM_PARAMETER to ausgabeform.name),
    )
}

fun diagnostiziereIntegralModusWechsel(
    knoten: KnotenDaten,
    ausgabeform: IntegralAusgabeform,
    verbundeneAnschlussIds: Set<AnschlussId>,
): IntegralModusWechselDiagnose {
    val probe = konfiguriereIntegralKnoten(knoten, ausgabeform)
    val alt = knoten.anschlüsse.mapTo(linkedSetOf()) { it.id }
    val neu = probe.anschlüsse.mapTo(linkedSetOf()) { it.id }
    val entfernt = alt - neu
    return IntegralModusWechselDiagnose(
        ausgabeform = ausgabeform,
        erhalteneAnschlussIds = alt.intersect(neu),
        entfernteAnschlussIds = entfernt,
        verbundeneEntfernteAnschlussIds = entfernt.intersect(verbundeneAnschlussIds),
    )
}

internal fun MathematikAuswerterRegister.registriereIntegralKnoten() {
    registriere(INTEGRAL_KNOTEN_ART) { kontext -> kontext.werteIntegralKnotenAus() }
}

private fun KnotenAuswertungsKontext.werteIntegralKnotenAus(): KnotenAuswertungsErgebnis {
    knoten.parameter[INTEGRAL_MIGRATIONSFEHLER_PARAMETER]?.let { return fehlerErgebnis(it) }
    val menge = eingänge["menge"]?.objekt as? MengenAusdruck
        ?: return fehlerErgebnis("Der Integrationsbereich 'menge' fehlt.")
    val bereich = IntegralBereich(menge.integralKomponenten())
    val mass = bestimmeIntegralMass(bereich) ?: return fehlerErgebnis(
        "Das Maß fehlt oder ist für den Integrationsbereich nicht eindeutig ableitbar.",
    )
    val integral = runCatching {
        when (aktuelleIntegralAusgabeform(knoten)) {
            IntegralAusgabeform.METHODE -> erzeugeMethodenIntegral(bereich, mass)
            IntegralAusgabeform.TERM -> erzeugeTermIntegral(bereich, mass)
        }
    }.getOrElse { fehler ->
        return fehlerErgebnis(fehler.message ?: "Der Integralvertrag ist ungültig.")
    }
    val ergebnis = werteIntegralAus(integral)
    val annahmen = gemeinsameAnnahmen() + ergebnis.voraussetzungen
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf(
            "wert" to BedingterWert(
                objekt = ergebnis.wert,
                annahmen = annahmen,
                zielMenge = if (ergebnis.wert is ZahlAusdruck) ReelleZahlen else null,
            ),
        ),
        schritte = ergebnis.schritte,
        warnungen = buildList {
            add("Status: ${ergebnis.status.name}")
            add("Maß: ${mass.zuLatex()}")
            add("Regel: ${ergebnis.regel}")
            if (integral.freieVariablen.isNotEmpty()) {
                add("Freie Parameter: ${integral.freieVariablen.joinToString { it.name }}")
            }
        },
        eingänge = eingänge,
    )
}

private fun KnotenAuswertungsKontext.erzeugeMethodenIntegral(
    bereich: IntegralBereich,
    mass: IntegralMass,
): StrukturiertesIntegral {
    val methode = eingänge["methode"]?.objekt as? Methode
        ?: error("Die Methodenform benötigt eine Methode.")
    val darstellung = IntegralMethodenDarstellung.entries.firstOrNull {
        it.name == knoten.parameter[INTEGRAL_METHODEN_DARSTELLUNG_PARAMETER]
    } ?: IntegralMethodenDarstellung.KURZ
    return methodenIntegral(
        methode = methode,
        bereich = bereich,
        kurz = darstellung == IntegralMethodenDarstellung.KURZ,
        mass = mass,
        vertrag = if (mass == IntegralMass.StandardReell) standardRiemannVertrag(bereich) else null,
    )
}

private fun KnotenAuswertungsKontext.erzeugeTermIntegral(
    bereich: IntegralBereich,
    mass: IntegralMass,
): StrukturiertesIntegral {
    val term = eingänge["term"]?.objekt
        ?: error("Die Termform benötigt einen Integranden.")
    val variablen = when (val objekt = eingänge["variable"]?.objekt) {
        is Variable -> listOf(objekt)
        is Tupel -> objekt.elemente.map { element ->
            element as? Variable ?: error("Das Variablentupel darf nur Variablen enthalten.")
        }
        null -> error("Die Termform benötigt eine gebundene Variable oder ein Variablentupel.")
        else -> error("Der Eingang 'variable' muss eine Variable oder ein Tupel von Variablen sein.")
    }
    if (variablen.size != bereich.dimension) {
        error("Der Bereich besitzt Dimension ${bereich.dimension}, erhalten wurden ${variablen.size} Bindungen.")
    }
    val konfigurierteIds = knoten.parameter[INTEGRAL_QUELLEN_IDS_PARAMETER]
        .orEmpty()
        .split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)
    val bindungen = variablen.mapIndexed { index, variable ->
        IntegralBindung(
            variable = variable,
            quellenId = konfigurierteIds.getOrNull(index) ?: variable.name,
        )
    }
    return termIntegral(
        term = term,
        bereiche = bereich.komponenten,
        bindungen = bindungen,
        mass = mass,
        vertrag = if (mass == IntegralMass.StandardReell) standardRiemannVertrag(bereich) else null,
    )
}

private fun KnotenAuswertungsKontext.bestimmeIntegralMass(
    bereich: IntegralBereich,
): IntegralMass? {
    val verbunden = eingänge["mass"]?.objekt
    if (verbunden != null) return verbunden as? IntegralMass
    val modus = IntegralMassModus.entries.firstOrNull {
        it.name == knoten.parameter[INTEGRAL_MASS_MODUS_PARAMETER]
    } ?: IntegralMassModus.AUTO
    return when (modus) {
        IntegralMassModus.AUTO -> leiteIntegralMassOderNull(bereich)
        IntegralMassModus.STANDARD_REELL -> IntegralMass.StandardReell
        IntegralMassModus.ZAEHLMASS -> IntegralMass.Zaehlmass
        IntegralMassModus.ALLGEMEIN -> IntegralMass.Allgemein(
            knoten.parameter[INTEGRAL_MASS_SYMBOL_PARAMETER].orEmpty().ifBlank { "\\mu" },
        )
        IntegralMassModus.NICHTSTANDARD -> IntegralMass.NichtstandardZellgewicht()
    }
}

private fun MengenAusdruck.integralKomponenten(): List<MengenAusdruck> = when (this) {
    is KartesischesProdukt -> mengen
    is Tupelraum -> komponenten
    else -> listOf(this)
}

private fun KnotenAuswertungsKontext.gemeinsameAnnahmen(): Set<Aussage> =
    eingänge.values.flatMap { it.annahmen }.toSet()

private fun KnotenAuswertungsKontext.fehlerErgebnis(nachricht: String): KnotenAuswertungsErgebnis =
    KnotenAuswertungsErgebnis(
        ausgaben = emptyMap(),
        fehler = nachricht,
        eingänge = eingänge,
    )

fun KartenDaten.migriereIntegralKnoten(): KartenDaten = copy(
    knoten = knoten.map { knoten ->
        when (knoten.art) {
            INTEGRAL_KNOTEN_ART -> knoten.normalisiereIntegralParameter()
            HISTORISCHES_METHODEN_INTEGRAL -> knoten.copy(
                art = INTEGRAL_KNOTEN_ART,
                parameter = knoten.parameter + mapOf(
                    INTEGRAL_AUSGABEFORM_PARAMETER to IntegralAusgabeform.METHODE.name,
                    INTEGRAL_METHODEN_DARSTELLUNG_PARAMETER to (
                        if (knoten.parameter["kurz"] == "false") {
                            IntegralMethodenDarstellung.VOLLSTAENDIG.name
                        } else {
                            IntegralMethodenDarstellung.KURZ.name
                        }
                    ),
                    INTEGRAL_MASS_MODUS_PARAMETER to IntegralMassModus.AUTO.name,
                    INTEGRAL_MASS_SYMBOL_PARAMETER to "\\mu",
                    INTEGRAL_QUELLEN_IDS_PARAMETER to "integral.variable",
                ),
            ).normalisiereIntegralParameter()
            HISTORISCHES_TERM_INTEGRAL -> knoten.copy(
                art = INTEGRAL_KNOTEN_ART,
                parameter = knoten.parameter + mapOf(
                    INTEGRAL_AUSGABEFORM_PARAMETER to IntegralAusgabeform.TERM.name,
                    INTEGRAL_METHODEN_DARSTELLUNG_PARAMETER to IntegralMethodenDarstellung.KURZ.name,
                    INTEGRAL_MASS_MODUS_PARAMETER to IntegralMassModus.AUTO.name,
                    INTEGRAL_MASS_SYMBOL_PARAMETER to "\\mu",
                    INTEGRAL_QUELLEN_IDS_PARAMETER to (
                        knoten.parameter["quellenId"] ?: "integral.variable"
                    ),
                ),
            ).normalisiereIntegralParameter()
            else -> knoten
        }
    },
)

private fun KnotenDaten.normalisiereIntegralParameter(): KnotenDaten {
    val form = aktuelleIntegralAusgabeform(this)
    val mitStandard = copy(
        parameter = IntegralKnotenVorlagen.Integral.standardParameter + parameter,
    )
    return konfiguriereIntegralKnoten(mitStandard, form)
}

private fun integralAnschluesse(form: IntegralAusgabeform): List<AnschlussDaten> = when (form) {
    IntegralAusgabeform.METHODE -> listOf(
        integralEingang("menge", MathematikAnschlussArten.Menge.id, 0),
        integralEingang("methode", MathematikAnschlussArten.Methode.id, 1),
        integralEingang("mass", MathematikAnschlussArten.Objekt.id, 2),
        integralAusgang(),
    )
    IntegralAusgabeform.TERM -> listOf(
        integralEingang("variable", MathematikAnschlussArten.Objekt.id, 0),
        integralEingang("menge", MathematikAnschlussArten.Menge.id, 1),
        integralEingang("term", MathematikAnschlussArten.Objekt.id, 2),
        integralEingang("mass", MathematikAnschlussArten.Objekt.id, 3),
        integralAusgang(),
    )
}

private fun integralEingang(
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

private fun integralAusgang(): AnschlussDaten = AnschlussDaten(
    name = "wert",
    richtung = AnschlussRichtung.Ausgang,
    kante = AnschlussKante.Rechts,
    art = MathematikAnschlussArten.Objekt.id,
)

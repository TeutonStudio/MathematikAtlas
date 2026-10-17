package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

/**
 * Gemeinsamer Vertrag der wertartigen Rechnerknoten:
 * Ein gewöhnlicher mathematischer Werteingang T darf alternativ eine mathematische
 * Methode W -> T tragen. Allgemeine Script-/Engine-Methoden werden hier ausdrücklich
 * nicht punktweise in mathematische Rechneroperationen gehoben.
 */
private val methodenHebbareRechnerArten: Set<KnotenArtId> = setOf(
    ZAHLENRECHNER_ART,
    MengenRechner.KNOTEN_ART,
    AussagenSatzRechner.KNOTEN_ART,
    VektorRechner.KNOTEN_ART,
    MatrixRechner.KNOTEN_ART,
    TensorRechner.KNOTEN_ART,
)

private val methodenHebbareWertArten: Set<AnschlussArtId> = setOf(
    MathematikAnschlussArten.Zahl.id,
    MathematikAnschlussArten.Aussage.id,
    MathematikAnschlussArten.Menge.id,
    MathematikAnschlussArten.Tupel.id,
    MathematikAnschlussArten.Vektor.id,
    MathematikAnschlussArten.SpaltenVektor.id,
    MathematikAnschlussArten.ZeilenVektor.id,
    MathematikAnschlussArten.Matrix.id,
    MathematikAnschlussArten.Tensor.id,
)

fun normalisiereRechnerMethodenAnschluesse(knoten: KnotenDaten): KnotenDaten {
    if (istUrbildKnoten(knoten) || RECHNER_MIGRATION_DIAGNOSE in knoten.parameter || knoten.art !in methodenHebbareRechnerArten) return knoten

    if (knoten.anschlüsse.any {
            it.richtung == AnschlussRichtung.Ausgang && it.art == MathematikAnschlussArten.Methode.id
        }
    ) return knoten

    if (
        knoten.art == VektorRechner.KNOTEN_ART &&
        knoten.parameter[VEKTOR_RECHNER_OPERATOR] == VektorRechnerOperator.VEKTORFELD_INTEGRIEREN.stabileId
    ) return knoten

    val hebbareEingaenge = mutableListOf<String>()
    val anschluesse = knoten.anschlüsse.map { anschluss ->
        if (anschluss.richtung != AnschlussRichtung.Eingang) return@map anschluss
        if(knoten.art==ZAHLENRECHNER_ART) {
            val rollen=RechnerOperatorDefinitionen.finde(OperatorFamilie.ZAHL,knoten.parameter[ZAHLENRECHNER_OPERATOR])?.signaturen.orEmpty().flatMap {it.eingänge}
            if(rollen.none {it.methodenfähig && (it.name==anschluss.name || it.variadisch)}) return@map anschluss
        }
        if (anschluss.art == MathematikAnschlussArten.Methode.id) return@map anschluss

        val deklarierteWertArten = buildSet {
            if (anschluss.zulässigeArten.isEmpty() && anschluss.art in methodenHebbareWertArten) add(anschluss.art)
            addAll(anschluss.zulässigeArten.filter { it in methodenHebbareWertArten })
        }
        if (deklarierteWertArten.isEmpty()) return@map anschluss

        hebbareEingaenge += anschluss.name
        anschluss.copy(
            art = MathematikAnschlussArten.AtlasWert.id,
            zulässigeArten = deklarierteWertArten + anschluss.zulässigeArten + MathematikAnschlussArten.Methode.id,
        )
    }.map { anschluss ->
        if (
            anschluss.richtung != AnschlussRichtung.Ausgang ||
            hebbareEingaenge.isEmpty() ||
            anschluss.art == MathematikAnschlussArten.Methode.id
        ) return@map anschluss

        anschluss.copy(
            artPriorisiertEingänge = anschluss.artPriorisiertEingänge ?: AnschlussArtPriorisierung(
                eingänge = hebbareEingaenge,
                prioritäten = listOf(MathematikAnschlussArten.Methode.id),
            ),
        )
    }

    return if (anschluesse == knoten.anschlüsse) knoten else knoten.copy(anschlüsse = anschluesse)
}

fun methodenfaehigeRechnerVorlage(vorlage: KnotenVorlage): KnotenVorlage {
    if (vorlage.art !in methodenHebbareRechnerArten) return vorlage
    val probe = KnotenDaten(
        art = vorlage.art,
        name = vorlage.name,
        anschlüsse = vorlage.anschlüsse,
        parameter = vorlage.standardParameter,
    )
    return vorlage.copy(anschlüsse = normalisiereRechnerMethodenAnschluesse(probe).anschlüsse)
}

fun KartenDaten.migriereRechnerMethodenAnschluesse(): KartenDaten {
    val normalisiert = knoten.map(::normalisiereRechnerMethodenAnschluesse)
    return if (normalisiert == knoten) this else copy(knoten = normalisiert)
}

private data class AllgemeineMethodenHebung(
    val parameter: List<MethodenParameter>,
    val werteVorraete: Map<String, MengenAusdruck>,
    val effektiverWerteVorrat: MengenAusdruck?,
    val operanden: Map<String, MathematischesObjekt>,
    val argumentIdentitäten: Map<String, String>,
    val voraussetzungen: Set<Aussage>,
    val umbenennungen: Map<MathematischeMethode, Map<String, MethodenParameter>>,
) {
    fun methode(
        name: String,
        vorschrift: MathematischesObjekt,
        zielMenge: MengenAusdruck,
        bedingungen: Set<Aussage>,
    ): MathematischeMethode = MathematischeMethode(
        name = name,
        parameter = parameter,
        vorschrift = vorschrift,
        zielMenge = zielMenge,
        werteVorräte = werteVorraete,
        effektiverWerteVorrat = effektiverWerteVorrat,
        argumentIdentitäten = argumentIdentitäten,
        voraussetzungen = voraussetzungen + bedingungen,
    )
}

private fun bereiteAllgemeineMethodenHebungVor(
    operanden: Map<String, MathematischesObjekt>,
): AllgemeineMethodenHebung {
    val methoden = operanden.values.filterIsInstance<MathematischeMethode>()
    require(methoden.isNotEmpty())
    require(methoden.all { it.signatur.ergebnisse.size == 1 }) {
        "Ein punktweise gehobener Wertoperand benötigt genau ein logisches Methodenergebnis."
    }
    val basis = methoden.first()
    val stelligkeit = basis.signatur.argumente.size
    require(methoden.all { it.signatur.argumente.size == stelligkeit }) {
        "Punktweise verknüpfte mathematische Methoden müssen dieselbe Argumentanzahl besitzen."
    }

    val basisParameter = basis.parameter
    val umbenennungen = methoden.associateWith { methode ->
        methode.parameter.mapIndexed { index, parameter ->
            (parameter.bindungsId ?: parameter.name) to basisParameter[index]
        }.toMap()
    }

    val werteVorraete = basisParameter.mapIndexed { index, parameter ->
        val mengen = methoden.map { methode ->
            val menge = methode.mathematischeSignatur.argumente[index].definitionsMenge
            val bindungen = umbenennungen.getValue(methode)
            (ersetze(menge, bindungen) as? MengenAusdruck) ?: menge
        }
        parameter.name to schneide(mengen)
    }.toMap(LinkedHashMap())

    val effektive = methoden.map { methode ->
        ersetze(methode.mathematischeSignatur.definitionsRaum,umbenennungen.getValue(methode)) as MengenAusdruck
    }
    val effektiverWerteVorrat = schneide(effektive)

    val angeglicheneOperanden = operanden.mapValues { (_, objekt) ->
        if (objekt !is MathematischeMethode) objekt else {
            val bindungen = umbenennungen.getValue(objekt)
            ersetze(objekt.einzigeAusgabe().second, bindungen)
        }
    }

    return AllgemeineMethodenHebung(
        parameter = basisParameter,
        werteVorraete = werteVorraete,
        effektiverWerteVorrat = effektiverWerteVorrat,
        operanden = angeglicheneOperanden,
        argumentIdentitäten = basis.argumentIdentitäten,
        voraussetzungen = methoden.flatMap { methode ->
            methode.voraussetzungen.map { ersetze(it, umbenennungen.getValue(methode)) }
        }.toSet(),
        umbenennungen = umbenennungen,
    )
}

internal fun mitRechnerMethodenHebung(basis: de.TeutonStudio.MathematikKartenAdapter.MathematikKnotenAuswerter): de.TeutonStudio.MathematikKartenAdapter.MathematikKnotenAuswerter =
    de.TeutonStudio.MathematikKartenAdapter.MathematikKnotenAuswerter { kontext ->
            if (istUrbildKnoten(kontext.knoten)) return@MathematikKnotenAuswerter basis.auswerten(kontext)
            val hebbareNamen = kontext.knoten.anschlüsse
                .filter { anschluss ->
                    anschluss.richtung == AnschlussRichtung.Eingang &&
                        anschluss.art != MathematikAnschlussArten.Methode.id &&
                        MathematikAnschlussArten.Methode.id in anschluss.zulässigeArten
                }
                .mapTo(linkedSetOf()) { it.name }
            val roheOperanden = kontext.eingänge
                .filterKeys { it in hebbareNamen }
                .mapValues { it.value.objekt }
            if (roheOperanden.values.none { it is Methode }) {
                return@MathematikKnotenAuswerter basis.auswerten(kontext)
            }
            val methodenOperanden = roheOperanden.mapValues { (name, objekt) ->
                if (objekt is Methode) runCatching { objekt.alsMathematischeMethode("punktweise Methodenhebung") }
                    .getOrElse { error("Die Methode '${objekt.name}' besitzt keine symbolische mathematische Vorschrift für eine punktweise Methodenhebung: ${it.message}") }
                else objekt as? MathematischesObjekt
                    ?: error("Der mathematische Rechneroperand '$name' ist kein MathematischesObjekt.")
            }

            val hebung = bereiteAllgemeineMethodenHebungVor(methodenOperanden)
            val angeglicheneEingaenge = kontext.eingänge.mapValues { (name, wert) ->
                hebung.operanden[name]?.let { objekt ->
                    wert.copy(
                        objekt = objekt,
                        zielMenge = null,
                        werteVorrat = (methodenOperanden[name] as? MathematischeMethode)?.einzigeZielMenge ?: wert.werteVorrat,
                        annahmen = (methodenOperanden[name] as? MathematischeMethode)?.let { methode ->
                            wert.annahmen.map { ersetze(it, hebung.umbenennungen.getValue(methode)) }.toSet()
                        } ?: wert.annahmen,
                        reelleVariablen = wert.reelleVariablen + (hebung.werteVorraete + hebung.parameter.associate {
                            (it.bindungsId ?: it.name) to hebung.werteVorraete.getValue(it.name)
                        }).filterValues { nachgewieseneZahlenGrundmenge(it)?.fundamentalerZahlbereichOderNull()?.let { b -> b <= FundamentalerZahlbereich.REELL } == true },
                        variablenQuellen = wert.variablenQuellen + hebung.parameter.map { p ->
                            VariablenQuelle(kontext.knoten.id,p.name,hebung.werteVorraete.getValue(p.name),
                                alsMethodenParameter=false,bindungsId=p.bindungsId ?: hebung.argumentIdentitäten.getValue(p.name))
                        },
                        latexDarstellung = null,
                    )
                } ?: wert
            }
            val konkret = basis.auswerten(kontext.copy(eingänge = angeglicheneEingaenge))
            if (konkret.fehler != null) return@MathematikKnotenAuswerter konkret.copy(eingänge = kontext.eingänge)

            val annahmen = angeglicheneEingaenge.values.flatMap { it.annahmen }.toSet()
            val quellWerte = kontext.eingänge.values
            val ausgaben = konkret.ausgaben.mapValues { (ausgang, wert) ->
                require(wert.objekt !is Methode) {
                    "Der Ausgang '$ausgang' liefert bereits eine Methode und kann nicht zusätzlich punktweise gehoben werden."
                }
                val mathematischerWert = wert.objekt as? MathematischesObjekt
                    ?: error("Der mathematische Rechnerausgang '$ausgang' ist kein MathematischesObjekt.")
                val ziel = when (mathematischerWert) {
                    is Aussage -> WahrheitsMenge
                    else -> wert.zielMenge ?: inferiereZielmenge(
                        mathematischerWert,
                        hebung.werteVorraete + hebung.parameter.associate {
                            (it.bindungsId ?: it.name) to hebung.werteVorraete.getValue(it.name)
                        },
                        annahmen + wert.annahmen,
                    )
                }
                val methode = hebung.methode(
                    name = punktweiserRechnerMethodenName(kontext, ausgang),
                    vorschrift = mathematischerWert,
                    zielMenge = ziel,
                    bedingungen = annahmen + wert.annahmen,
                )
                BedingterWert(
                    objekt = methode,
                    annahmen = annahmen + wert.annahmen,
                    zielMenge = ziel,
                    werteVorrat = methode.mathematischeSignatur.definitionsRaum,
                    reelleVariablen = reelleVariablen(quellWerte),
                    variablenQuellen = quellWerte.flatMap { it.variablenQuellen }.geordnetEindeutig(),
                    übersetzungen = ÜbersetzungsMetadaten(herkunft = (quellWerte.flatMap { it.übersetzungen.alsHerkunft().herkunft } +
                        methodenOperanden.values.filterIsInstance<MathematischeMethode>().flatMap { it.übersetzungen.alsHerkunft().herkunft }).distinct()),
                )
            }
            konkret.copy(
                ausgaben = ausgaben,
                eingänge = kontext.eingänge,
                warnungen = konkret.warnungen + "Punktweise Methodenhebung: Wertausgang wurde zu mathematischer Methode/Prädikat angehoben.",
            )
}

fun MathematikAuswerterRegister.registriereRechnerMethodenHebung() {
    methodenHebbareRechnerArten.forEach { art -> finde(art)?.let { basis -> registriere(art, mitRechnerMethodenHebung(basis)) } }
}

private fun punktweiserRechnerMethodenName(
    kontext: KnotenAuswertungsKontext,
    ausgang: String,
): String {
    val methoden = kontext.eingänge.values.mapNotNull { it.objekt as? Methode }
    val argument = methoden.joinToString(",") { it.name }.ifBlank { "f" }
    return "${kontext.knoten.name}[$ausgang]($argument)"
}

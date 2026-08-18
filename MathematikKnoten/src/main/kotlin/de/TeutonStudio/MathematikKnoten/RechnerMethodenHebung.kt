package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

/**
 * Gemeinsamer Vertrag der wertartigen Rechnerknoten:
 * Ein gewöhnlicher Werteingang T darf alternativ eine Methode W -> T tragen.
 * Sobald mindestens ein solcher Operand eine Methode ist, wird die konkrete
 * Rechneroperation punktweise über die gemeinsame Methodensignatur gehoben.
 *
 * Echte Methodenoperanden eines Operators besitzen bereits die Anschlussart
 * [MathematikAnschlussArten.Methode] und werden ausdrücklich nicht erneut gehoben.
 */
private val methodenHebbareRechnerArten: Set<KnotenArtId> = setOf(
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

/**
 * Macht die statische Knotenschnittstelle methodenfähig. Die Funktion ist
 * idempotent und kann deshalb auf Vorlagen, geladene Karten und Inspector-
 * Kandidaten gleichermaßen angewandt werden.
 */
fun normalisiereRechnerMethodenAnschluesse(knoten: KnotenDaten): KnotenDaten {
    if (knoten.art !in methodenHebbareRechnerArten) return knoten

    // Operatoren, die selbst Methoden ausgeben, werden nicht zu Methoden höherer
    // Ordnung gehoben. Das bestehende Methodenmodell ist absichtlich erster Ordnung.
    if (knoten.anschlüsse.any {
            it.richtung == AnschlussRichtung.Ausgang && it.art == MathematikAnschlussArten.Methode.id
        }
    ) return knoten

    // Das Vektorfeldintegral konsumiert eine Methode als mathematischen Operanden;
    // seine Integrationsmenge ist kein punktweise zu hebender Werteparameter.
    if (
        knoten.art == VektorRechner.KNOTEN_ART &&
        knoten.parameter[VEKTOR_RECHNER_OPERATOR] == VektorRechnerOperator.VEKTORFELD_INTEGRIEREN.stabileId
    ) return knoten

    val hebbareEingaenge = mutableListOf<String>()
    val anschluesse = knoten.anschlüsse.map { anschluss ->
        if (anschluss.richtung != AnschlussRichtung.Eingang) return@map anschluss
        if (anschluss.art == MathematikAnschlussArten.Methode.id) return@map anschluss

        val deklarierteWertArten = buildSet {
            if (anschluss.art in methodenHebbareWertArten) add(anschluss.art)
            addAll(anschluss.zulässigeArten.filter { it in methodenHebbareWertArten })
        }
        if (deklarierteWertArten.isEmpty()) return@map anschluss

        hebbareEingaenge += anschluss.name
        anschluss.copy(
            art = MathematikAnschlussArten.Objekt.id,
            zulässigeArten = deklarierteWertArten + anschluss.zulässigeArten + MathematikAnschlussArten.Methode.id,
        )
    }.map { anschluss ->
        if (
            anschluss.richtung != AnschlussRichtung.Ausgang ||
            hebbareEingaenge.isEmpty() ||
            anschluss.art == MathematikAnschlussArten.Methode.id
        ) return@map anschluss

        anschluss.copy(
            artPriorisiertEingänge = AnschlussArtPriorisierung(
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
) {
    fun methode(
        name: String,
        vorschrift: MathematischesObjekt,
        zielMenge: MengenAusdruck,
    ): Methode = Methode(
        name = name,
        parameter = parameter,
        vorschrift = vorschrift,
        zielMenge = zielMenge,
        werteVorräte = werteVorraete,
        effektiverWerteVorrat = effektiverWerteVorrat,
    )
}

private fun bereiteAllgemeineMethodenHebungVor(
    operanden: Map<String, AtlasWert>,
): AllgemeineMethodenHebung {
    val methoden = operanden.values.filterIsInstance<Methode>()
    require(methoden.isNotEmpty())
    val basis = methoden.first()
    val stelligkeit = basis.argumentAnzahl
    require(methoden.all { it.argumentAnzahl == stelligkeit }) {
        "Punktweise verknüpfte Methoden müssen dieselbe Argumentanzahl besitzen."
    }

    // Punktweise Rechnerhebung ist eine mathematische Capability. Neutrale Script-
    // oder Engine-Methoden dürfen hier nicht über eine erfundene Vorschrift laufen.
    val mathematischeMethoden = methoden.associateWith { methode ->
        methode.alsMathematischeMethode("Punktweise Rechnerhebung von '${methode.name}'")
    }
    val signaturen = methoden.map(Methode::methodenSignatur)
    val basisParameter = mathematischeMethoden.getValue(basis).parameter
    val umbenennungen = methoden.associateWith { methode ->
        mathematischeMethoden.getValue(methode).parameter.mapIndexed { index, parameter ->
            parameter.name to basisParameter[index]
        }.toMap()
    }

    val werteVorraete = basisParameter.mapIndexed { index, parameter ->
        val mengen = signaturen.mapIndexed { methodenIndex, signatur ->
            val menge = signatur.argumente[index].werteVorrat
            val bindungen = umbenennungen.getValue(methoden[methodenIndex])
            @Suppress("UNCHECKED_CAST")
            (ersetze(menge, bindungen) as? MengenAusdruck) ?: menge
        }
        parameter.name to schneide(mengen)
    }.toMap(LinkedHashMap())

    val effektive = methoden.mapNotNull { methode ->
        mathematischeMethoden.getValue(methode).effektiverWerteVorrat?.let { menge ->
            val bindungen = umbenennungen.getValue(methode)
            (ersetze(menge, bindungen) as? MengenAusdruck) ?: menge
        }
    }
    val effektiverWerteVorrat = when {
        effektive.isNotEmpty() -> schneide(effektive)
        stelligkeit == 0 -> null
        else -> Tupelraum(basisParameter.map { werteVorraete.getValue(it.name) })
    }

    val angeglicheneOperanden: Map<String, MathematischesObjekt> = operanden.mapValues { (name, objekt) ->
        if (objekt !is Methode) {
            objekt.alsMathematischesObjekt("Punktweiser Rechneroperand '$name'")
        } else {
            val bindungen = umbenennungen.getValue(objekt)
            ersetze(mathematischeMethoden.getValue(objekt).vorschrift, bindungen)
        }
    }

    return AllgemeineMethodenHebung(
        parameter = basisParameter,
        werteVorraete = werteVorraete,
        effektiverWerteVorrat = effektiverWerteVorrat,
        operanden = angeglicheneOperanden,
    )
}

/**
 * Letzte Rechner-Verfeinerung. Sie umschließt die bereits vollständig
 * registrierten Fachauswerter und verändert daher deren eigentliche Mathematik
 * nicht, sondern nur den Übergang Wert <-> Methode.
 */
fun MathematikAuswerterRegister.registriereRechnerMethodenHebung() {
    methodenHebbareRechnerArten.forEach { art ->
        val basis = finde(art) ?: return@forEach
        registriere(art) { kontext ->
            val hebbareNamen = kontext.knoten.anschlüsse
                .filter { anschluss ->
                    anschluss.richtung == AnschlussRichtung.Eingang &&
                        anschluss.art != MathematikAnschlussArten.Methode.id &&
                        MathematikAnschlussArten.Methode.id in anschluss.zulässigeArten
                }
                .mapTo(linkedSetOf()) { it.name }
            val methodenOperanden: Map<String, AtlasWert> = kontext.eingänge
                .filterKeys { it in hebbareNamen }
                .mapValues { it.value.objekt }
            if (methodenOperanden.values.none { it is Methode }) {
                return@registriere basis.auswerten(kontext)
            }

            val hebung = bereiteAllgemeineMethodenHebungVor(methodenOperanden)
            val angeglicheneEingaenge = kontext.eingänge.mapValues { (name, wert) ->
                hebung.operanden[name]?.let { objekt ->
                    wert.copy(
                        objekt = objekt,
                        zielMenge = null,
                        werteVorrat = (wert.objekt as? Methode)?.zielMenge ?: wert.werteVorrat,
                        latexDarstellung = null,
                    )
                } ?: wert
            }
            val konkret = basis.auswerten(kontext.copy(eingänge = angeglicheneEingaenge))
            if (konkret.fehler != null) return@registriere konkret.copy(eingänge = kontext.eingänge)

            val annahmen = kontext.eingänge.values.flatMap { it.annahmen }.toSet()
            val quellWerte = kontext.eingänge.values
            val ausgaben = konkret.ausgaben.mapValues { (ausgang, wert) ->
                require(wert.objekt !is Methode) {
                    "Der Ausgang '$ausgang' liefert bereits eine Methode und kann nicht zusätzlich punktweise gehoben werden."
                }
                val mathematischerWert = wert.mathematischesObjekt("Punktweiser Rechnerausgang '$ausgang'")
                val ziel = when (mathematischerWert) {
                    is Aussage -> WahrheitsMenge
                    else -> wert.zielMenge ?: inferiereZielmenge(
                        mathematischerWert,
                        hebung.werteVorraete,
                        annahmen + wert.annahmen,
                    )
                }
                val methode = hebung.methode(
                    name = punktweiserRechnerMethodenName(kontext, ausgang),
                    vorschrift = mathematischerWert,
                    zielMenge = ziel,
                )
                BedingterWert(
                    objekt = methode,
                    annahmen = annahmen + wert.annahmen,
                    zielMenge = ziel,
                    werteVorrat = methode.methodenSignatur().werteVorrat,
                    reelleVariablen = reelleVariablen(quellWerte),
                    variablenQuellen = quellWerte.flatMap { it.variablenQuellen }.geordnetEindeutig(),
                )
            }
            konkret.copy(
                ausgaben = ausgaben,
                eingänge = kontext.eingänge,
                warnungen = konkret.warnungen + "Punktweise Methodenhebung: Wertausgang wurde zu Methode/Prädikat angehoben.",
            )
        }
    }
}

private fun punktweiserRechnerMethodenName(
    kontext: KnotenAuswertungsKontext,
    ausgang: String,
): String {
    val methoden = kontext.eingänge.values.mapNotNull { it.objekt as? Methode }
    val argument = methoden.joinToString(",") { it.name }.ifBlank { "f" }
    return "${kontext.knoten.name}[$ausgang]($argument)"
}

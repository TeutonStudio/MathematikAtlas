package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val HYPER_WERT_KNOTEN_ART = "mathematik.hyper.wert"
const val HYPER_ERWEITERUNG_KNOTEN_ART = "mathematik.hyper.erweiterung"
const val HYPER_PRAEDIKAT_KNOTEN_ART = "mathematik.hyper.praedikat"
const val HYPER_TRANSFER_KNOTEN_ART = "mathematik.hyper.transfer"
const val HYPER_ENDLICH_KNOTEN_ART = "mathematik.hyper.endlicheStruktur"
const val HYPER_LIMES_KNOTEN_ART = "mathematik.hyper.limes"

const val HYPER_MODELL_ID_PARAMETER = "hyperModelId"
const val HYPER_WERT_NAME_PARAMETER = "hyperWert.name"
const val HYPER_GROESSENKLASSE_PARAMETER = "hyperWert.groessenKlasse"
const val HYPER_STANDARDTEIL_PARAMETER = "hyperWert.standardteil"
const val HYPER_ERWEITERUNGSART_PARAMETER = "hyperErweiterungsArt"
const val HYPER_PRAEDIKAT_PARAMETER = "hyperPraedikat"
const val HYPER_BEWEISSTATUS_PARAMETER = "hyperBeweisstatus"

private val kanonischeHyperParameter = mapOf(
    HYPER_MODELL_ID_PARAMETER to KanonischesHyperModell.modell.id.wert,
)

object HyperAnalysisKnotenVorlagen {
    private fun eingang(
        name: String,
        art: de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId,
        reihenfolge: Int = 0,
    ) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = art,
        reihenfolge = reihenfolge,
    )

    private fun ausgang(
        name: String,
        art: de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId,
    ) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = art,
    )

    val HyperWert = KnotenVorlage(
        art = HYPER_WERT_KNOTEN_ART,
        name = "Hyperreeller Wert",
        kategorie = "Analysis: Nichtstandardanalysis",
        beschreibung = "Erzeugt einen symbolischen hyperreellen Wert im kanonischen Hypermodell. Die Größenklasse bleibt explizite Konfiguration.",
        standardGröße = GraphGröße(265f, 120f),
        anschlüsse = listOf(ausgang("wert", MathematikAnschlussArten.Zahl.id)),
        standardParameter = kanonischeHyperParameter + mapOf(
            HYPER_WERT_NAME_PARAMETER to "h",
            HYPER_GROESSENKLASSE_PARAMETER to HyperGroessenKlasse.NICHT_ENTSCHEIDBAR.name,
            HYPER_STANDARDTEIL_PARAMETER to "",
            HYPER_BEWEISSTATUS_PARAMETER to NachweisStatus.Unentscheidbar::class.simpleName.orEmpty(),
        ),
    )

    val HyperErweiterung = KnotenVorlage(
        art = HYPER_ERWEITERUNG_KNOTEN_ART,
        name = "Hypererweiterung",
        kategorie = "Analysis: Nichtstandardanalysis",
        beschreibung = "Erweitert ein registriertes Objekt symbolisch. Ein freier Ultrafilter wird referenziert, nicht materialisiert.",
        standardGröße = GraphGröße(270f, 115f),
        anschlüsse = listOf(
            eingang("grundobjekt", MathematikAnschlussArten.Objekt.id),
            ausgang("hyperobjekt", MathematikAnschlussArten.Objekt.id),
        ),
        standardParameter = kanonischeHyperParameter + mapOf(
            HYPER_ERWEITERUNGSART_PARAMETER to HyperErweiterungsArt.WERT.name,
        ),
    )

    val ExternesPraedikat = KnotenVorlage(
        art = HYPER_PRAEDIKAT_KNOTEN_ART,
        name = "Externes Hyperprädikat",
        kategorie = "Analysis: Nichtstandardanalysis",
        beschreibung = "Markiert Standardheit, Endlichkeit, Unendlichkeit oder Infinitesimalität als externen Begriff.",
        standardGröße = GraphGröße(280f, 115f),
        anschlüsse = listOf(
            eingang("argument", MathematikAnschlussArten.Objekt.id),
            ausgang("aussage", MathematikAnschlussArten.Aussage.id),
        ),
        standardParameter = kanonischeHyperParameter + mapOf(
            HYPER_PRAEDIKAT_PARAMETER to ExternesHyperPraedikat.INFINITESIMAL.name,
        ),
    )

    val Transfer = KnotenVorlage(
        art = HYPER_TRANSFER_KNOTEN_ART,
        name = "Transfer",
        kategorie = "Analysis: Nichtstandardanalysis",
        beschreibung = "Überträgt interne erststufige Aussagen und liefert bei externen Begriffen eine transparente Diagnose.",
        standardGröße = GraphGröße(260f, 110f),
        anschlüsse = listOf(
            eingang("aussage", MathematikAnschlussArten.Aussage.id),
            ausgang("aussage", MathematikAnschlussArten.Aussage.id),
        ),
        standardParameter = kanonischeHyperParameter,
    )

    val HyperendlicheStruktur = KnotenVorlage(
        art = HYPER_ENDLICH_KNOTEN_ART,
        name = "Hyperendliche Struktur",
        kategorie = "Analysis: Nichtstandardanalysis",
        beschreibung = "Erzeugt eine symbolische hyperendliche Struktur. Ein optionales Sichtfenster ist nur Vorschau, kein Gesamtbeweis.",
        standardGröße = GraphGröße(285f, 135f),
        anschlüsse = listOf(
            eingang("struktur", MathematikAnschlussArten.Objekt.id, 0),
            eingang("hyperIndex", MathematikAnschlussArten.Zahl.id, 1),
            eingang("sichtfenster", MathematikAnschlussArten.Objekt.id, 2),
            ausgang("struktur", MathematikAnschlussArten.Objekt.id),
        ),
        standardParameter = kanonischeHyperParameter,
    )

    val HyperLimes = KnotenVorlage(
        art = HYPER_LIMES_KNOTEN_ART,
        name = "Hyper-Limes",
        kategorie = "Analysis: Nichtstandardanalysis",
        beschreibung = "Bildet einen hyperreellen Wert auf die erweiterten reellen Werte ℝ∪{−∞,+∞} ab.",
        standardGröße = GraphGröße(250f, 105f),
        anschlüsse = listOf(
            eingang("hyperwert", MathematikAnschlussArten.Zahl.id),
            ausgang("wert", MathematikAnschlussArten.Objekt.id),
        ),
        standardParameter = kanonischeHyperParameter,
    )

    val alle = listOf(
        HyperWert,
        HyperErweiterung,
        ExternesPraedikat,
        Transfer,
        HyperendlicheStruktur,
        HyperLimes,
    )
}

internal fun MathematikAuswerterRegister.registriereHyperAnalysisKnoten() {
    registriere(HYPER_WERT_KNOTEN_ART) { kontext -> kontext.werteHyperWertAus() }
    registriere(HYPER_ERWEITERUNG_KNOTEN_ART) { kontext -> kontext.werteHyperErweiterungAus() }
    registriere(HYPER_PRAEDIKAT_KNOTEN_ART) { kontext -> kontext.werteHyperPraedikatAus() }
    registriere(HYPER_TRANSFER_KNOTEN_ART) { kontext -> kontext.werteTransferAus() }
    registriere(HYPER_ENDLICH_KNOTEN_ART) { kontext -> kontext.werteHyperendlicheStrukturAus() }
    registriere(HYPER_LIMES_KNOTEN_ART) { kontext -> kontext.werteHyperLimesAus() }
}

private fun KnotenAuswertungsKontext.werteHyperWertAus(): KnotenAuswertungsErgebnis {
    val klasse = HyperGroessenKlasse.entries.firstOrNull {
        it.name == knoten.parameter[HYPER_GROESSENKLASSE_PARAMETER]
    } ?: HyperGroessenKlasse.NICHT_ENTSCHEIDBAR
    val standardteilText = knoten.parameter[HYPER_STANDARDTEIL_PARAMETER].orEmpty().trim()
    val standardteil = if (standardteilText.isBlank()) {
        null
    } else {
        runCatching { RationaleZahl.parse(standardteilText) }.getOrElse { fehler ->
            return fehlerErgebnis("Ungültiger Standardteil: ${fehler.message}")
        }
    }
    if (standardteil != null && klasse != HyperGroessenKlasse.ENDLICH) {
        return fehlerErgebnis("Ein Standardteil darf nur für einen als endlich klassifizierten Hyperwert gesetzt werden.")
    }
    val wert = SymbolischerHyperReellerWert(
        name = knoten.parameter[HYPER_WERT_NAME_PARAMETER].orEmpty().ifBlank { "h" },
        groessenKlasse = klasse,
        standardteil = standardteil,
        modellId = aktuelleHyperModellId(),
        voraussetzungen = gemeinsameAnnahmen(),
    )
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf("wert" to BedingterWert(wert, gemeinsameAnnahmen())),
        eingänge = eingänge,
    )
}

private fun KnotenAuswertungsKontext.werteHyperErweiterungAus(): KnotenAuswertungsErgebnis {
    val grundobjekt = eingänge["grundobjekt"]?.objekt
        ?: return fehlerErgebnis("Das Grundobjekt der Hypererweiterung fehlt.")
    val art = HyperErweiterungsArt.entries.firstOrNull {
        it.name == knoten.parameter[HYPER_ERWEITERUNGSART_PARAMETER]
    } ?: HyperErweiterungsArt.WERT
    val erweiterung = runCatching {
        SymbolischeHyperErweiterung(
            grundobjekt = grundobjekt,
            art = art,
            modellId = aktuelleHyperModellId(),
            voraussetzungen = gemeinsameAnnahmen(),
        )
    }.getOrElse { fehler -> return fehlerErgebnis(fehler.message ?: "Ungültige Hypererweiterung.") }
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf("hyperobjekt" to BedingterWert(erweiterung, gemeinsameAnnahmen())),
        eingänge = eingänge,
    )
}

private fun KnotenAuswertungsKontext.werteHyperPraedikatAus(): KnotenAuswertungsErgebnis {
    val argument = eingänge["argument"]?.objekt
        ?: return fehlerErgebnis("Das Argument des externen Hyperprädikats fehlt.")
    val art = ExternesHyperPraedikat.entries.firstOrNull {
        it.name == knoten.parameter[HYPER_PRAEDIKAT_PARAMETER]
    } ?: ExternesHyperPraedikat.INFINITESIMAL
    val aussage = externesHyperPraedikat(art, argument)
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf(
            "aussage" to BedingterWert(
                objekt = aussage,
                annahmen = gemeinsameAnnahmen(),
                zielMenge = WahrheitsMenge,
            ),
        ),
        eingänge = eingänge,
    )
}

private fun KnotenAuswertungsKontext.werteTransferAus(): KnotenAuswertungsErgebnis {
    val aussage = eingänge["aussage"]?.objekt as? Aussage
        ?: return fehlerErgebnis("Der Transfer benötigt eine Aussage.")
    val ergebnis = TransferUebersetzer.uebertrage(aussage)
    val ausgabe = ergebnis.uebertragen ?: UnentscheidbareAussage(
        bezeichnung = "Transfer(${aussage.zuLatex()})",
        system = when (ergebnis.status) {
            TransferStatus.EXTERNE_BESTANDTEILE ->
                "Externe Bestandteile: ${ergebnis.externeBestandteile.joinToString { it.sichtbarerName }}"
            TransferStatus.NICHT_REGISTRIERTE_SYMBOLE ->
                "Nicht registrierte Symbole: ${ergebnis.nichtRegistrierteSymbole.joinToString()}"
            TransferStatus.UEBERTRAGEN -> "Transferdiagnose"
        },
    )
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf(
            "aussage" to BedingterWert(
                objekt = ausgabe,
                annahmen = gemeinsameAnnahmen() + ergebnis.voraussetzungen,
                zielMenge = WahrheitsMenge,
            ),
        ),
        warnungen = when (ergebnis.status) {
            TransferStatus.UEBERTRAGEN -> emptyList()
            TransferStatus.EXTERNE_BESTANDTEILE -> listOf(
                "Transfer abgelehnt: ${ergebnis.externeBestandteile.joinToString { it.sichtbarerName }} ist extern.",
            )
            TransferStatus.NICHT_REGISTRIERTE_SYMBOLE -> listOf(
                "Transfer abgelehnt: nicht registrierte Symbole ${ergebnis.nichtRegistrierteSymbole.joinToString()}.",
            )
        },
        eingänge = eingänge,
    )
}

private fun KnotenAuswertungsKontext.werteHyperendlicheStrukturAus(): KnotenAuswertungsErgebnis {
    val struktur = eingänge["struktur"]?.objekt
        ?: return fehlerErgebnis("Die zu erweiternde Struktur fehlt.")
    val hyperIndex = eingänge["hyperIndex"]?.objekt as? ZahlAusdruck
        ?: return fehlerErgebnis("Der Hyperindex muss ein Zahlterm sein.")
    val sichtfenster = when (val objekt = eingänge["sichtfenster"]?.objekt) {
        null -> emptyList()
        is Tupel -> objekt.elemente
        else -> listOf(objekt)
    }
    val ausgabe = SymbolischeHyperendlicheStruktur(
        grundstruktur = struktur,
        hyperIndex = hyperIndex,
        modellId = aktuelleHyperModellId(),
        sichtfenster = sichtfenster,
    )
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf("struktur" to BedingterWert(ausgabe, gemeinsameAnnahmen())),
        warnungen = if (sichtfenster.isEmpty()) emptyList() else listOf(
            "Das Sichtfenster zeigt ${sichtfenster.size} Elemente und beweist keine Aussage über das Gesamtobjekt.",
        ),
        eingänge = eingänge,
    )
}

private fun KnotenAuswertungsKontext.werteHyperLimesAus(): KnotenAuswertungsErgebnis {
    val wert = eingänge["hyperwert"]?.objekt as? SymbolischerHyperReellerWert
        ?: return fehlerErgebnis("Der direkte Hyper-Limes benötigt einen symbolischen hyperreellen Wert.")
    return when (val ergebnis = werteHyperLimes(wert)) {
        is HyperLimesErgebnis.Wert -> KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "wert" to BedingterWert(
                    objekt = ergebnis.wert,
                    annahmen = gemeinsameAnnahmen(),
                    zielMenge = BenannteMenge("erweiterteReelle", "\\overline{\\mathbb R}"),
                ),
            ),
            eingänge = eingänge,
        )
        is HyperLimesErgebnis.Bedingt -> KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "wert" to BedingterWert(
                    objekt = ergebnis.ausdruck,
                    annahmen = gemeinsameAnnahmen() + ergebnis.voraussetzungen,
                    zielMenge = BenannteMenge("erweiterteReelle", "\\overline{\\mathbb R}"),
                ),
            ),
            warnungen = listOf("Der Hyper-Limes bleibt bis zur Größenklassifikation bedingt."),
            eingänge = eingänge,
        )
    }
}

private fun KnotenAuswertungsKontext.aktuelleHyperModellId(): HyperModellId = HyperModellId(
    knoten.parameter[HYPER_MODELL_ID_PARAMETER]
        ?.takeIf(String::isNotBlank)
        ?: KanonischesHyperModell.modell.id.wert,
)

private fun KnotenAuswertungsKontext.gemeinsameAnnahmen(): Set<Aussage> =
    eingänge.values.flatMap { it.annahmen }.toSet()

private fun KnotenAuswertungsKontext.fehlerErgebnis(nachricht: String): KnotenAuswertungsErgebnis =
    KnotenAuswertungsErgebnis(
        ausgaben = emptyMap(),
        fehler = nachricht,
        eingänge = eingänge,
    )

private val historischeHyperWertArten = setOf(
    "mathematik.hyperzahl",
    "mathematik.hyperReelleZahl",
    "mathematik.nichtstandard.hyperwert",
)

fun KartenDaten.migriereHyperAnalysisKnoten(): KartenDaten = copy(
    knoten = knoten.map(KnotenDaten::migriereHyperAnalysisKnoten),
)

private fun KnotenDaten.migriereHyperAnalysisKnoten(): KnotenDaten {
    val zielArt = if (art in historischeHyperWertArten) HYPER_WERT_KNOTEN_ART else art
    if (zielArt !in HyperAnalysisKnotenVorlagen.alle.map { it.art }.toSet()) return this
    val standard = kanonischeHyperParameter + when (zielArt) {
        HYPER_WERT_KNOTEN_ART -> mapOf(
            HYPER_WERT_NAME_PARAMETER to (parameter[HYPER_WERT_NAME_PARAMETER] ?: parameter["name"] ?: "h"),
            HYPER_GROESSENKLASSE_PARAMETER to (
                parameter[HYPER_GROESSENKLASSE_PARAMETER]
                    ?: HyperGroessenKlasse.NICHT_ENTSCHEIDBAR.name
            ),
            HYPER_STANDARDTEIL_PARAMETER to (parameter[HYPER_STANDARDTEIL_PARAMETER] ?: ""),
        )
        HYPER_ERWEITERUNG_KNOTEN_ART -> mapOf(
            HYPER_ERWEITERUNGSART_PARAMETER to (
                parameter[HYPER_ERWEITERUNGSART_PARAMETER]
                    ?: HyperErweiterungsArt.WERT.name
            ),
        )
        HYPER_PRAEDIKAT_KNOTEN_ART -> mapOf(
            HYPER_PRAEDIKAT_PARAMETER to (
                parameter[HYPER_PRAEDIKAT_PARAMETER]
                    ?: ExternesHyperPraedikat.INFINITESIMAL.name
            ),
        )
        else -> emptyMap()
    }
    return copy(
        art = zielArt,
        parameter = standard + parameter,
    )
}

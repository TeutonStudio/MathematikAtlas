package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import java.text.Normalizer
import java.util.Locale

internal enum class RechnerOperatorAuswahlArt {
    OPERATOR,
    FORMEL,
    UNBEKANNT,
}

internal enum class RechnerOperatorBestätigungsAktion {
    KEINE,
    FORMEL_BAUEN,
    KNOTEN_ERSETZEN,
}

internal data class RechnerOperatorAuswahlEintrag(
    val id: String,
    val titel: String,
    val symbolLatex: String,
    val kategorie: String,
    val beschreibung: String,
    val suchbegriffe: Set<String> = emptySet(),
    val status: String? = null,
    val kandidat: KnotenDaten? = null,
    val art: RechnerOperatorAuswahlArt = RechnerOperatorAuswahlArt.OPERATOR,
) {
    init {
        require(id.isNotBlank())
        require(titel.isNotBlank())
        require(kategorie.isNotBlank())
        require(art != RechnerOperatorAuswahlArt.OPERATOR || kandidat != null) {
            "Ein Operator benötigt einen konfigurierten Kandidatenknoten."
        }
    }

    val eingänge: List<AnschlussDaten>
        get() = kandidat?.anschlüsse
            .orEmpty()
            .filter { it.richtung == AnschlussRichtung.Eingang }
            .sortedBy { it.reihenfolge }

    val ausgänge: List<AnschlussDaten>
        get() = kandidat?.anschlüsse
            .orEmpty()
            .filter { it.richtung == AnschlussRichtung.Ausgang }
            .sortedBy { it.reihenfolge }

    internal fun entspricht(suchtext: String): Boolean {
        val gesucht = normalisiereOperatorSuche(suchtext)
        if (gesucht.isBlank()) return true
        return suchfelder().any { feld -> gesucht in normalisiereOperatorSuche(feld) }
    }

    private fun suchfelder(): Sequence<String> = sequence {
        yield(id)
        yield(titel)
        yield(symbolLatex)
        yield(kategorie)
        yield(beschreibung)
        status?.let { yield(it) }
        yieldAll(suchbegriffe)
        eingänge.forEach { anschluss ->
            yield(anschluss.name)
            yield(anschluss.art.wert)
        }
        ausgänge.forEach { anschluss ->
            yield(anschluss.name)
            yield(anschluss.art.wert)
        }
    }
}

internal fun filtereRechnerOperatoren(
    einträge: List<RechnerOperatorAuswahlEintrag>,
    suchtext: String,
    kategorie: String?,
): List<RechnerOperatorAuswahlEintrag> = einträge.filter { eintrag ->
    (kategorie == null || eintrag.kategorie == kategorie) && eintrag.entspricht(suchtext)
}

internal fun bestätigungsAktionFür(
    eintrag: RechnerOperatorAuswahlEintrag?,
    aktuelleId: String?,
): RechnerOperatorBestätigungsAktion = when {
    eintrag == null || eintrag.art == RechnerOperatorAuswahlArt.UNBEKANNT ->
        RechnerOperatorBestätigungsAktion.KEINE
    eintrag.art == RechnerOperatorAuswahlArt.FORMEL && eintrag.kandidat == null ->
        RechnerOperatorBestätigungsAktion.FORMEL_BAUEN
    eintrag.art == RechnerOperatorAuswahlArt.FORMEL ->
        RechnerOperatorBestätigungsAktion.KNOTEN_ERSETZEN
    eintrag.id == aktuelleId -> RechnerOperatorBestätigungsAktion.KEINE
    else -> RechnerOperatorBestätigungsAktion.KNOTEN_ERSETZEN
}

private fun normalisiereOperatorSuche(text: String): String = Normalizer
    .normalize(text.trim().lowercase(Locale.GERMAN), Normalizer.Form.NFD)
    .replace("ß", "ss")
    .replace(Regex("\\p{Mn}+"), "")

/** Auswahl, Suche und Informationsfelder projizieren denselben deklarativen Vertrag. */
internal fun rechnerOperatorEinträge(knoten: KnotenDaten, familie: de.TeutonStudio.MathematikKnoten.RechnerFamilie): List<RechnerOperatorAuswahlEintrag> {
    val registry = de.TeutonStudio.MathematikKnoten.RechnerOperatorRegistry
    val gespeichert = knoten.parameter[familie.operatorParameter]
    return buildList {
        if (gespeichert != null && registry.finde(familie, gespeichert) is de.TeutonStudio.MathematikKnoten.RechnerOperatorAuflösung.Unbekannt) add(
            RechnerOperatorAuswahlEintrag(gespeichert, "Unbekannter gespeicherter Operator", "?", "Nicht verfügbar",
                "Die gespeicherte Operator-ID bleibt erhalten.", art = RechnerOperatorAuswahlArt.UNBEKANNT))
        registry.fürFamilie(familie).filterNot { it.operatorId == "zahl.komplexerRadius" }.forEach { d ->
            val formel = d.operatorId.endsWith(".formel")
            val kandidat = if (formel) null else if (d.operatorId == "zahl.differential") {
                de.TeutonStudio.MathematikKnoten.konfiguriereZahlenRechnerDifferential(knoten,
                    de.TeutonStudio.MathematikKnoten.ZahlenRechnerDifferentialErgebnisArt.ABLEITUNGSFUNKTION)
            } else registry.konfiguriere(knoten, d.operatorId)
            add(RechnerOperatorAuswahlEintrag(d.operatorId, if (d.operatorId == "zahl.betrag") "0-Distanz" else d.titel,
                d.symbolLatex, d.kategorie, d.definitionsLatex, d.aliases,
                status = d.unterstützung.name, kandidat = kandidat,
                art = if (formel) RechnerOperatorAuswahlArt.FORMEL else RechnerOperatorAuswahlArt.OPERATOR))
        }
    }
}

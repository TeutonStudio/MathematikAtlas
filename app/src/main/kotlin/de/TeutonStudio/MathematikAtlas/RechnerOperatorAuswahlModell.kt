package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKnoten.normalisiereAngulusZahlenRechner
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
    var symbolLatex: String,
    val kategorie: String,
    val beschreibung: String,
    val suchbegriffe: Set<String> = emptySet(),
    val status: String? = null,
    var kandidat: KnotenDaten? = null,
    val art: RechnerOperatorAuswahlArt = RechnerOperatorAuswahlArt.OPERATOR,
) {
    init {
        require(id.isNotBlank())
        require(titel.isNotBlank())
        require(kategorie.isNotBlank())
        require(art != RechnerOperatorAuswahlArt.OPERATOR || kandidat != null) {
            "Ein Operator benötigt einen konfigurierten Kandidatenknoten."
        }
        symbolLatex = normalisiereOperatorVorschau(kategorie, symbolLatex)
        kandidat = kandidat?.let(::normalisiereAngulusZahlenRechner)
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

/**
 * Im Operatorwahldialog sind A, B, M, I usw. keine mathematischen Konstanten,
 * sondern Platzhalter für spätere Anschlusswerte. Wie beim Zahlenrechner werden
 * sie deshalb als \dots dargestellt. Definitionskarten bleiben unverändert.
 */
private fun normalisiereOperatorVorschau(kategorie: String, latex: String): String {
    if ("Mengenoperator" !in kategorie) return latex
    return when (latex.replace(" ", "")) {
        "A\\capB" -> "(\\dots)\\cap(\\dots)"
        "A\\cupB" -> "(\\dots)\\cup(\\dots)"
        "A\\setminusB" -> "(\\dots)\\setminus(\\dots)"
        "A\\timesB" -> "(\\dots)\\times(\\dots)"
        "U\\setminusA" -> "(\\dots)\\setminus(\\dots)"
        "\\mathcal{P}(M)" -> "\\mathcal{P}(\\dots)"
        "M^A" -> "(\\dots)^{(\\dots)}"
        "M^{\\mathbbZ}", "M^{\\mathbb{Z}}" -> "(\\dots)^{\\mathbb{Z}}"
        "M^{\\mathbbN_0}", "M^{\\mathbb{N}_0}" -> "(\\dots)^{\\mathbb{N}_0}"
        "M\\divr" -> "(\\dots)\\div(\\dots)"
        "f(A)" -> "(\\dots)(\\dots)"
        "f^{-1}(A)" -> "(\\dots)^{-1}(\\dots)"
        "\\mathop{\\Large\\times}\\limits_{i\\inI}A(i)" ->
            "\\mathop{\\Large\\times}\\limits_{i\\in\\dots}(\\dots)(i)"
        "\\bigcup\\limits_{i\\inI}A(i)" -> "\\bigcup\\limits_{i\\in\\dots}(\\dots)(i)"
        "\\bigcap\\limits_{i\\inI}A(i)" -> "\\bigcap\\limits_{i\\in\\dots}(\\dots)(i)"
        else -> latex
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

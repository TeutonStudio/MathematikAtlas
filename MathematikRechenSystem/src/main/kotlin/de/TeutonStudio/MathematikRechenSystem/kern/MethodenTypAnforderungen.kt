package de.TeutonStudio.MathematikRechenSystem.kern

import de.TeutonStudio.TypSystem.TypAusdruck
import de.TeutonStudio.TypSystem.TypPrüfung
import de.TeutonStudio.TypSystem.TypSystem

/**
 * Semantische Ergebnisanforderung ohne Rückgriff auf UI-Anschluss-IDs.
 *
 * Der historische [MethodenAnforderung.ErgebnisArt] bleibt für bestehende Knoten
 * quellkompatibel. Neue Operatoren sollen diese Funktion verwenden.
 */
fun ergebnisTypAnforderung(
    erwarteterTyp: TypAusdruck,
    typSystem: TypSystem = MathematischeTypen.typSystem,
): MethodenAnforderung = MethodenAnforderung { methode ->
    val signatur = (methode as? SignaturtragendeMethode)?.signatur
        ?: return@MethodenAnforderung "Die Methode '${methode.name}' besitzt keine semantische Signatur."
    val ergebnisTyp = signatur.zielMenge.elementTypAusdruck()
    when (val prüfung = typSystem.prüfe(ergebnisTyp, erwarteterTyp)) {
        TypPrüfung.Kompatibel -> null
        is TypPrüfung.Unbestimmt ->
            "Der Ergebnistyp der Methode '${methode.name}' konnte nicht hinreichend bestimmt werden: ${prüfung.grund}"
        is TypPrüfung.Inkompatibel ->
            "Der Ergebnistyp der Methode '${methode.name}' ist nicht kompatibel: ${prüfung.grund}"
    }
}

/**
 * Vollständige Signaturanforderung. Sie ist insbesondere für ScriptMethod- und
 * Adapterknoten gedacht, bei denen nicht nur die grobe Methodenart relevant ist.
 */
fun methodenTypAnforderung(
    erwarteterTyp: TypAusdruck,
    typSystem: TypSystem = MathematischeTypen.typSystem,
): MethodenAnforderung = MethodenAnforderung { methode ->
    when (val prüfung = typSystem.prüfe(methode.methodenTypAusdruck(), erwarteterTyp)) {
        TypPrüfung.Kompatibel -> null
        is TypPrüfung.Unbestimmt ->
            "Der Methodentyp '${methode.name}' konnte nicht hinreichend bestimmt werden: ${prüfung.grund}"
        is TypPrüfung.Inkompatibel ->
            "Der Methodentyp '${methode.name}' ist nicht kompatibel: ${prüfung.grund}"
    }
}

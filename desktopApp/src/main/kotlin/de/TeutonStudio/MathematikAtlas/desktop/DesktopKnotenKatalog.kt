package de.TeutonStudio.MathematikAtlas.desktop

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage

internal fun desktopKnotenVorlagen(): List<KnotenVorlage> =
    de.TeutonStudio.MathematikKnoten.katalog.KanonischerMathematikKnotenKatalog.alle()

/** Derselbe Operatorsuchbestand wie Android; Treffer sind sofort konfiguriert. */
internal fun desktopKnotenSuchVorlagen(text: String, basis: List<KnotenVorlage> = desktopKnotenVorlagen()): List<KnotenVorlage> {
    val vorlagen = basis.filter { text.isBlank() || it.name.contains(text, true) || it.kategorie.contains(text, true) }
    val operatoren = if (text.isBlank()) emptyList() else
        de.TeutonStudio.MathematikKnoten.katalog.OperatorKnotenSuchindex.suche(text, basis)
    return (vorlagen + operatoren).distinctBy(de.TeutonStudio.MathematikKnoten.katalog.KanonischerMathematikKnotenKatalog::variantenKennung)
}

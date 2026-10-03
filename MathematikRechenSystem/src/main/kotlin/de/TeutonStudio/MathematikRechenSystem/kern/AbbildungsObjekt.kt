package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Wertorientierter mathematischer Abbildungsvertrag ohne Methoden-, Aufruf- oder
 * Effektsemantik. Ein Abbildungsobjekt ordnet Indizes konkrete Werte zu.
 */
interface AbbildungsObjekt : MathematischesObjekt {
    val indexMenge: MengenAusdruck
    fun wertAn(index: MathematischesObjekt): MathematischesObjekt
}

/**
 * Effiziente Capability für endlich indexierte Abbildungsobjekte.
 *
 * [wertAn] verwendet den mathematischen Index des Objekts. [wertAnPosition] ist
 * dagegen ausdrücklich der nullbasierte technische Zugriff auf die materialisierte
 * Reihenfolge und darf nicht als mathematische Indexkonvention nach außen dringen.
 */
interface EndlichIndexiertesObjekt : AbbildungsObjekt {
    val anzahl: Int
    fun wertAnPosition(position: Int): MathematischesObjekt
}

/** Kanonische geordnete Indexmenge I_n = {1,...,n}. */
fun endlicheGeordneteIndexMenge(anzahl: Int): MengenAusdruck {
    require(anzahl >= 0) { "Eine endliche Indexmenge kann keine negative Länge besitzen." }
    if (anzahl == 0) return LeereMenge
    return EndlicheMenge(
        (1..anzahl).map { RationaleZahl.von(it.toLong()) }.toSet(),
    )
}

/** Wandelt einen mathematischen 1-basierten Index in eine nullbasierte Position um. */
internal fun mathematischeIndexPosition(index: MathematischesObjekt, anzahl: Int): Int {
    val zahl = index as? RationaleZahl
        ?: throw IllegalArgumentException("Ein endlicher Tupelindex muss eine natürliche Zahl sein.")
    require(zahl.nenner == java.math.BigInteger.ONE) {
        "Ein endlicher Tupelindex muss ganzzahlig sein."
    }
    val mathematischerIndex = runCatching { zahl.zähler.intValueExact() }
        .getOrElse { throw IndexOutOfBoundsException("Der Tupelindex ${zahl.zuLatex()} liegt außerhalb des darstellbaren Bereichs.") }
    require(mathematischerIndex in 1..anzahl) {
        "Der mathematische Tupelindex $mathematischerIndex liegt außerhalb von 1..$anzahl."
    }
    return mathematischerIndex - 1
}

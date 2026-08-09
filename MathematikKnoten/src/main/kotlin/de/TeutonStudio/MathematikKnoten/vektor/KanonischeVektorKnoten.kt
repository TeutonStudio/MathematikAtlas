package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister

/**
 * Kanonische Fassade für den konsolidierten Vektorkonstruktor.
 *
 * Die interne v2.30-Implementierung bleibt vorerst erhalten, damit dieser reine
 * Architekturrefactor keine Persistenz- oder Verhaltensänderung einschleppt.
 * Neue Produktpfade verwenden ausschließlich diese versionsfreie Fassade.
 */
object VektorKonstruktorVorlagen {
    val standard: KnotenVorlage
        get() = VektorKonstruktorV2300Vorlagen.standard
}

/** Kanonische Fassade für die parametrierten Orientierungsoperationen. */
object VektorOrientierungsVorlagen {
    val alle: List<KnotenVorlage>
        get() = VektorOrientierungsV2300Vorlagen.alle
}

fun MathematikAuswerterRegister.registriereVektorKonstruktor() {
    registriereVektorKonstruktorV2300()
}

fun MathematikAuswerterRegister.registriereVektorOrientierungsKnoten() {
    registriereVektorOrientierungsKnotenV2300()
}

fun MathematikAuswerterRegister.registrierePolynomMultinomVertrag() {
    registrierePolynomMultinomVertragV2300()
}

package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikRechenSystem.kern.MathematischeArgumentKomponente
import de.TeutonStudio.MathematikRechenSystem.kern.MathematischesObjekt
import de.TeutonStudio.MathematikRechenSystem.kern.MengenAusdruck
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.MethodenParameter
import de.TeutonStudio.MathematikRechenSystem.kern.SymbolischMathematischeMethode

/**
 * Eng begrenzte Migrationsprojektionen für den mathematischen Knoten-Layer.
 *
 * Sie halten die historische Knotenimplementierung während G0.5 quellkompatibel,
 * ohne die allgemeine [Methode] wieder mit Mengen-, Vorschrifts- oder Bindungssemantik
 * zu belasten. Neue Knoten sollen direkt auf [SymbolischMathematischeMethode] bzw.
 * die mathematische Signatur verengen.
 */
private fun Methode.symbolisch(operation: String): SymbolischMathematischeMethode =
    this as? SymbolischMathematischeMethode
        ?: error("Die Methode '$name' unterstützt $operation nicht als symbolische mathematische Methode.")

internal val Methode.parameter: List<MethodenParameter>
    get() = symbolisch("Parameterzugriff").parameter

internal val Methode.vorschrift: MathematischesObjekt
    get() = symbolisch("Vorschriftszugriff").vorschrift

internal val Methode.zielMenge: MengenAusdruck
    get() = symbolisch("Zielmengenzugriff").zielMenge

internal val Methode.werteVorräte: Map<String, MengenAusdruck>
    get() = symbolisch("Definitionsmengenzugriff").werteVorräte

internal val Methode.ausgabeNamen: List<String>
    get() = symbolisch("Ausgabezugriff").ausgabeNamen

internal val Methode.effektiverWerteVorrat: MengenAusdruck?
    get() = symbolisch("effektiven Definitionsraum").effektiverWerteVorrat

internal val MathematischeArgumentKomponente.werteVorrat: MengenAusdruck
    get() = definitionsMenge

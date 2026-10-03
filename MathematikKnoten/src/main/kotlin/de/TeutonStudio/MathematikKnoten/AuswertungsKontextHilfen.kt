package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.AtlasWert
import de.TeutonStudio.MathematikRechenSystem.kern.Aussage
import de.TeutonStudio.MathematikRechenSystem.kern.MathematischeMethode
import de.TeutonStudio.MathematikRechenSystem.kern.MathematischesObjekt
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.alsMathematischeMethode

/**
 * Sammelt die an allen verbundenen Eingängen bereits nachgewiesenen Annahmen.
 *
 * Die Hilfe ist modulweit sichtbar, damit Auswerter ohne eigene dateilokale
 * Kopie denselben Laufzeitkontext weiterreichen können. Der optionale Marker
 * hält die Signatur absichtlich von älteren dateiprivaten annahmen()-Hilfen
 * getrennt: Dort gewinnt der exakte parameterlose Overload, während neue
 * Auswerter diesen gemeinsamen Fallback weiterhin parameterlos aufrufen.
 */
internal fun KnotenAuswertungsKontext.annahmen(
    @Suppress("UNUSED_PARAMETER") gemeinsamerFallback: Unit = Unit,
): Set<Aussage> = eingänge.values.flatMap { it.annahmen }.toSet()

/**
 * Explizite Mathematikgrenze im Knotenmodul.
 *
 * Der Kartenkanal transportiert absichtlich beliebige [AtlasWert]e. Ein
 * Mathematikknoten muss daher an seiner fachlichen Grenze ausdrücklich verlangen,
 * dass ein Eingang tatsächlich ein [MathematischesObjekt] ist. Dadurch bleiben
 * Script-, Grafik- und spätere Enginewerte neutral, ohne mathematische Operationen
 * mit wiederholten unbenannten Casts zu überziehen.
 */
internal fun AtlasWert.alsMathematischesObjekt(rolle: String): MathematischesObjekt =
    this as? MathematischesObjekt
        ?: error("$rolle benötigt einen mathematischen Atlaswert, erhielt aber ${this::class.simpleName ?: "AtlasWert"}.")

internal fun BedingterWert.mathematischesObjekt(rolle: String): MathematischesObjekt =
    objekt.alsMathematischesObjekt(rolle)

internal fun KnotenAuswertungsKontext.mathematischerEingang(
    name: String,
    rolle: String = "Eingang '$name'",
): MathematischesObjekt =
    eingänge[name]?.mathematischesObjekt(rolle) ?: error("$rolle fehlt.")

internal fun Iterable<BedingterWert>.mathematischeObjekte(rolle: String): List<MathematischesObjekt> =
    mapIndexed { index, wert -> wert.mathematischesObjekt("$rolle ${index + 1}") }

internal fun Map<String, BedingterWert>.mathematischeObjekte(rolle: String): Map<String, MathematischesObjekt> =
    mapValues { (name, wert) -> wert.mathematischesObjekt("$rolle '$name'") }

/** Neutrale Methodenwerte werden erst an mathematischen Methodenknoten fachlich eingeengt. */
internal fun AtlasWert.alsMethode(rolle: String): Methode =
    this as? Methode
        ?: error("$rolle benötigt eine Methode, erhielt aber ${this::class.simpleName ?: "AtlasWert"}.")

internal fun AtlasWert.alsMathematischeMethode(rolle: String): MathematischeMethode =
    alsMethode(rolle).alsMathematischeMethode(rolle)

internal fun KnotenAuswertungsKontext.mathematischeMethode(
    name: String = "methode",
    rolle: String = "Eingang '$name'",
): MathematischeMethode =
    eingänge[name]?.objekt?.alsMathematischeMethode(rolle) ?: error("$rolle fehlt.")

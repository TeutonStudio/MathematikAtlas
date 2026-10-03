package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikRechenSystem.kern.AtlasWert
import de.TeutonStudio.MathematikRechenSystem.kern.MathematischesObjekt

/**
 * Explizite Mathematikgrenze des Knoten-Layers.
 *
 * Der allgemeine Kartenkanal bleibt absichtlich [AtlasWert]. Nur Knoten, deren
 * Fachvertrag mathematische Werte verlangt, verengen hier. So erhalten spätere
 * Script-/Godot-Werte keinen künstlichen MathematischesObjekt-Vertrag.
 */
internal fun AtlasWert.alsMathematischesObjekt(kontext: String): MathematischesObjekt =
    this as? MathematischesObjekt
        ?: error("$kontext benötigt einen mathematischen Wert, erhalten: ${this::class.simpleName ?: "AtlasWert"}.")

internal fun AtlasWert?.alsMathematischesObjektOderNull(): MathematischesObjekt? =
    this as? MathematischesObjekt

internal fun BedingterWert.mathematischesObjekt(kontext: String): MathematischesObjekt =
    objekt.alsMathematischesObjekt(kontext)

internal fun Map<String, BedingterWert>.mathematischeObjekte(kontext: String): Map<String, MathematischesObjekt> =
    mapValues { (name, wert) -> wert.objekt.alsMathematischesObjekt("$kontext ('$name')") }

internal fun Iterable<BedingterWert>.mathematischeObjekte(kontext: String): List<MathematischesObjekt> =
    mapIndexed { index, wert -> wert.objekt.alsMathematischesObjekt("$kontext (${index + 1})") }

internal fun Iterable<AtlasWert>.alsMathematischeObjekte(kontext: String): List<MathematischesObjekt> =
    mapIndexed { index, wert -> wert.alsMathematischesObjekt("$kontext (${index + 1})") }

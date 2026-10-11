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
internal fun AtlasWert?.alsMathematischesObjektOderNull(): MathematischesObjekt? =
    this as? MathematischesObjekt

internal fun Iterable<AtlasWert>.alsMathematischeObjekte(kontext: String): List<MathematischesObjekt> =
    mapIndexed { index, wert -> wert.alsMathematischesObjekt("$kontext (${index + 1})") }

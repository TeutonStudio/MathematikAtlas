package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.MathematikRechenSystem.kern.MathematischesObjekt

/**
 * Explizite Grenze vom domänenneutralen Kartenwertkanal in mathematische Adapterlogik.
 *
 * Generische Knoten dürfen diese Funktion nicht benötigen. Mathematische Knoten
 * erhalten dagegen eine fachlich benannte Diagnose, wenn versehentlich ein Script-,
 * Grafik- oder anderer nichtmathematischer Atlaswert angeschlossen wird.
 */
internal fun BedingterWert.mathematischesObjekt(rolle: String): MathematischesObjekt =
    objekt as? MathematischesObjekt
        ?: error("$rolle benötigt einen mathematischen Atlaswert, erhielt aber ${objekt::class.simpleName ?: "AtlasWert"}.")

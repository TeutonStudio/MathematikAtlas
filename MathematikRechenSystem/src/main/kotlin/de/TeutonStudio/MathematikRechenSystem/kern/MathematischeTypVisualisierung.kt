package de.TeutonStudio.MathematikRechenSystem.kern

import de.TeutonStudio.TypSystem.TypAusdruck
import de.TeutonStudio.TypSystem.TypId
import de.TeutonStudio.TypSystem.TypVisualDescriptor
import de.TeutonStudio.TypSystem.zuVisualDescriptor

/**
 * Kanonische Kurznotationen für die Orchestrator-artigen Typminigrafiken des Atlas.
 * Die Zuordnung bleibt im Mathematikkern; konkrete Farben und Zeichenoperationen
 * gehören in die jeweilige UI.
 */
fun mathematischerTypKurzname(id: TypId): String = when (id) {
    MathematischeTypen.Objekt -> "Obj"
    MathematischeTypen.Zahl -> "Zahl"
    MathematischeTypen.Aussage -> "Aussage"
    MathematischeTypen.Menge -> "Menge"
    MathematischeTypen.Mass -> "μ"
    MathematischeTypen.Vektor -> "Vektor"
    MathematischeTypen.SpaltenVektor -> "V↓"
    MathematischeTypen.ZeilenVektor -> "V→"
    MathematischeTypen.Matrix -> "Matrix"
    MathematischeTypen.Tensor -> "Tensor"
    MathematischeTypen.Tupel -> "Tupel"
    MathematischeTypen.UnendlichesTupel -> "Tupel∞"
    MathematischeTypen.Methode -> "ƒ"
    MathematischeTypen.Natuerlich -> "ℕ⁺"
    MathematischeTypen.NatuerlichMitNull -> "ℕ₀"
    MathematischeTypen.Ganz -> "ℤ"
    MathematischeTypen.Rational -> "ℚ"
    MathematischeTypen.Reell -> "ℝ"
    MathematischeTypen.Komplex -> "ℂ"
    MathematischeTypen.Quaternion -> "ℍ"
    else -> id.wert.substringAfterLast('.')
}

fun TypAusdruck.mathematischeTypVisualisierung(): TypVisualDescriptor =
    zuVisualDescriptor(::mathematischerTypKurzname)

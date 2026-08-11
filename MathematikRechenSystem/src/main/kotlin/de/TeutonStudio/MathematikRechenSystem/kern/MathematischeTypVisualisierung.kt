package de.TeutonStudio.MathematikRechenSystem.kern

import de.TeutonStudio.TypSystem.*

/**
 * Mathematische Beschriftung der neutralen G0.2-Typgrafiken.
 *
 * Die Struktur der Grafik bleibt UI-neutral. Compose bzw. später der Godot-Editor
 * müssen nur [TypMiniGrafik] zeichnen und kennen keine mathematischen Typregeln.
 */
object MathematischeTypVisualisierung : TypVisualResolver {
    override fun beschreibe(typ: TypAusdruck): TypVisualDescriptor = when (typ) {
        TypAusdruck.Beliebig -> TypVisualDescriptor("*", "Beliebiger mathematischer Typ", TypMiniGrafik.Symbol("*"))
        TypAusdruck.Unbekannt -> TypVisualDescriptor("?", "Noch nicht bestimmter mathematischer Typ", TypMiniGrafik.Unbekannt())
        is TypAusdruck.Literal -> TypVisualDescriptor(typ.wert, grafik = TypMiniGrafik.Symbol(typ.wert))
        is TypAusdruck.Variable -> TypVisualDescriptor(typ.id.wert, grafik = TypMiniGrafik.Symbol(typ.id.wert))
        is TypAusdruck.Atom -> atom(typ.id)
        is TypAusdruck.Vereinigung -> {
            val kinder = typ.alternativen.map(::beschreibe)
            TypVisualDescriptor(
                kurzLabel = kinder.joinToString(" ∨ ") { it.kurzLabel },
                tooltip = kinder.joinToString(" oder ") { it.tooltip },
                grafik = TypMiniGrafik.Vereinigung(kinder.map { it.grafik }),
            )
        }
        is TypAusdruck.Parameterisiert -> parameterisiert(typ)
    }

    private fun atom(id: TypId): TypVisualDescriptor {
        val (label, tooltip) = when (id) {
            MathematischeTypen.Natuerlich -> "ℕ" to "Natürliche Zahl"
            MathematischeTypen.NatuerlichMitNull -> "ℕ₀" to "Natürliche Zahl einschließlich 0"
            MathematischeTypen.Ganz -> "ℤ" to "Ganze Zahl"
            MathematischeTypen.Rational -> "ℚ" to "Rationale Zahl"
            MathematischeTypen.Reell -> "ℝ" to "Reelle Zahl"
            MathematischeTypen.Komplex -> "ℂ" to "Komplexe Zahl"
            MathematischeTypen.Quaternion -> "ℍ" to "Quaternion"
            MathematischeTypen.Zahl -> "Zahl" to "Zahl"
            MathematischeTypen.Aussage -> "A" to "Aussage"
            MathematischeTypen.Menge -> "M" to "Menge"
            MathematischeTypen.Mass -> "μ" to "Maß"
            MathematischeTypen.Vektor -> "v" to "Vektor"
            MathematischeTypen.SpaltenVektor -> "v↓" to "Spaltenvektor"
            MathematischeTypen.ZeilenVektor -> "v→" to "Zeilenvektor"
            MathematischeTypen.Matrix -> "Aᵢⱼ" to "Matrix"
            MathematischeTypen.Tensor -> "T" to "Tensor"
            MathematischeTypen.Tupel -> "(·)" to "Tupel"
            MathematischeTypen.Methode -> "f" to "Methode"
            MathematischeTypen.Objekt -> "◇" to "Mathematisches Objekt"
            else -> id.wert.substringAfterLast('.') to id.wert
        }
        return TypVisualDescriptor(label, tooltip, TypMiniGrafik.Symbol(label))
    }

    private fun parameterisiert(typ: TypAusdruck.Parameterisiert): TypVisualDescriptor {
        val kinder = typ.argumente.map(::beschreibe)
        return when (typ.konstruktor) {
            MathematischeTypen.Tupel -> {
                val label = kinder.joinToString(prefix = "(", postfix = ")") { it.kurzLabel }
                TypVisualDescriptor(label, "Tupel $label", TypMiniGrafik.Tupel(kinder.map { it.grafik }))
            }
            MathematischeTypen.Methode -> {
                val quelle = kinder.getOrNull(0) ?: beschreibe(TypAusdruck.Unbekannt)
                val ziel = kinder.getOrNull(1) ?: beschreibe(TypAusdruck.Unbekannt)
                val label = "${quelle.kurzLabel} → ${ziel.kurzLabel}"
                TypVisualDescriptor(label, "Methode $label", TypMiniGrafik.Pfeil(quelle.grafik, ziel.grafik))
            }
            MathematischeTypen.SpaltenVektor,
            MathematischeTypen.ZeilenVektor -> {
                val skalar = kinder.getOrNull(0)?.kurzLabel ?: "?"
                val dimension = kinder.getOrNull(1)?.kurzLabel ?: "?"
                val label = if (typ.konstruktor == MathematischeTypen.SpaltenVektor) {
                    "$skalar^{$dimension×1}"
                } else {
                    "$skalar^{1×$dimension}"
                }
                TypVisualDescriptor(
                    label,
                    if (typ.konstruktor == MathematischeTypen.SpaltenVektor) "Spaltenvektor $label" else "Zeilenvektor $label",
                    TypMiniGrafik.Parameter(atom(typ.konstruktor).kurzLabel, kinder.map { it.grafik }),
                )
            }
            MathematischeTypen.Matrix -> {
                val skalar = kinder.getOrNull(0)?.kurzLabel ?: "?"
                val zeilen = kinder.getOrNull(1)?.kurzLabel ?: "?"
                val spalten = kinder.getOrNull(2)?.kurzLabel ?: "?"
                val label = "$skalar^{$zeilen×$spalten}"
                TypVisualDescriptor(label, "Matrix $label", TypMiniGrafik.Parameter("Matrix", kinder.map { it.grafik }))
            }
            MathematischeTypen.Tensor -> {
                val label = "T<${kinder.joinToString(",") { it.kurzLabel }}>"
                TypVisualDescriptor(label, "Tensor $label", TypMiniGrafik.Parameter("T", kinder.map { it.grafik }))
            }
            else -> StandardTypVisualResolver.beschreibe(typ)
        }
    }
}

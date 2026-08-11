package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.TypAusdruck
import de.TeutonStudio.KnotenKartenVerwalter.daten.TypId
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.StandardTypVisualAuflöser
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.TypVisualAuflöser
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.TypVisualDescriptor

/** Mathematische Kurznotation für die G0.2-Handle-Minigrafiken. */
object MathematikTypVisualAuflöser : TypVisualAuflöser {
    override fun beschreibe(typ: TypAusdruck): TypVisualDescriptor = when (typ) {
        TypAusdruck.Beliebig -> TypVisualDescriptor("*", "Beliebiger mathematischer Typ")
        TypAusdruck.Unbekannt -> TypVisualDescriptor("?", "Mathematischer Typ noch unbekannt")
        is TypAusdruck.Variable -> TypVisualDescriptor("${'$'}${typ.id.wert}", "Typvariable ${typ.id.wert}")
        is TypAusdruck.Atom -> atom(typ.id)
        is TypAusdruck.Vereinigung -> {
            val alternativen = typ.alternativen.map(::beschreibe)
            TypVisualDescriptor(
                kurzText = alternativen.joinToString("∨") { it.kurzText },
                tooltipText = alternativen.joinToString(" oder ") { it.tooltipText },
                alternativen = alternativen,
            )
        }
        is TypAusdruck.Parameterisiert -> parameterisiert(typ)
    }

    private fun parameterisiert(typ: TypAusdruck.Parameterisiert): TypVisualDescriptor {
        val argumente = typ.argumente.map(::beschreibe)
        return when (typ.konstruktor) {
            MathematikTypen.Methode -> {
                val definitionsTyp = argumente.getOrNull(0)?.kurzText ?: "?"
                val zielTyp = argumente.getOrNull(1)?.kurzText ?: "?"
                TypVisualDescriptor("$definitionsTyp→$zielTyp", "Methode $definitionsTyp → $zielTyp")
            }
            MathematikTypen.Tupel -> {
                val text = argumente.joinToString(prefix = "(", postfix = ")") { it.kurzText }
                TypVisualDescriptor(text, "Tupel $text")
            }
            MathematikTypen.Menge -> {
                val element = argumente.singleOrNull()?.kurzText ?: "?"
                TypVisualDescriptor("{$element}", "Menge mit Elementtyp $element")
            }
            MathematikTypen.SpaltenVektor -> {
                val skalar = argumente.getOrNull(0)?.kurzText ?: "?"
                val dimension = argumente.getOrNull(1)?.kurzText ?: "?"
                TypVisualDescriptor("$skalar^$dimension×1", "Spaltenvektor $skalar^{$dimension×1}")
            }
            MathematikTypen.ZeilenVektor -> {
                val skalar = argumente.getOrNull(0)?.kurzText ?: "?"
                val dimension = argumente.getOrNull(1)?.kurzText ?: "?"
                TypVisualDescriptor("$skalar^1×$dimension", "Zeilenvektor $skalar^{1×$dimension}")
            }
            MathematikTypen.Matrix -> {
                val skalar = argumente.getOrNull(0)?.kurzText ?: "?"
                val zeilen = argumente.getOrNull(1)?.kurzText ?: "?"
                val spalten = argumente.getOrNull(2)?.kurzText ?: "?"
                TypVisualDescriptor("$skalar^$zeilen×$spalten", "Matrix $skalar^{$zeilen×$spalten}")
            }
            MathematikTypen.Tensor -> {
                val element = argumente.firstOrNull()?.kurzText ?: "?"
                val dimensionen = argumente.drop(1).joinToString("×") { it.kurzText }
                val text = if (dimensionen.isBlank()) "T[$element]" else "$element^$dimensionen"
                TypVisualDescriptor(text, "Tensor $text")
            }
            MathematikTypen.Folge -> {
                val element = argumente.singleOrNull()?.kurzText ?: "?"
                TypVisualDescriptor("$element^ℕ", "Folge mit Elementtyp $element")
            }
            else -> StandardTypVisualAuflöser.beschreibe(typ)
        }
    }

    private fun atom(id: TypId): TypVisualDescriptor = when (id) {
        MathematikTypen.N -> TypVisualDescriptor("ℕ", "Natürliche Zahlen")
        MathematikTypen.N0 -> TypVisualDescriptor("ℕ₀", "Nichtnegative ganze Zahlen")
        MathematikTypen.Z -> TypVisualDescriptor("ℤ", "Ganze Zahlen")
        MathematikTypen.Q -> TypVisualDescriptor("ℚ", "Rationale Zahlen")
        MathematikTypen.R -> TypVisualDescriptor("ℝ", "Reelle Zahlen")
        MathematikTypen.C -> TypVisualDescriptor("ℂ", "Komplexe Zahlen")
        MathematikTypen.H -> TypVisualDescriptor("ℍ", "Quaternionen")
        MathematikTypen.Zahl -> TypVisualDescriptor("#", "Zahl")
        MathematikTypen.Aussage -> TypVisualDescriptor("⊨", "Aussage")
        MathematikTypen.Menge -> TypVisualDescriptor("{}", "Menge")
        MathematikTypen.Mass -> TypVisualDescriptor("μ", "Maß")
        MathematikTypen.Tupel -> TypVisualDescriptor("(·)", "Tupel")
        MathematikTypen.LeeresTupel -> TypVisualDescriptor("()", "Leeres Tupel")
        MathematikTypen.Vektor -> TypVisualDescriptor("v", "Vektor")
        MathematikTypen.SpaltenVektor -> TypVisualDescriptor("v↓", "Spaltenvektor")
        MathematikTypen.ZeilenVektor -> TypVisualDescriptor("v→", "Zeilenvektor")
        MathematikTypen.Matrix -> TypVisualDescriptor("▦", "Matrix")
        MathematikTypen.Tensor -> TypVisualDescriptor("T", "Tensor")
        MathematikTypen.Methode -> TypVisualDescriptor("ƒ", "Methode")
        MathematikTypen.Folge -> TypVisualDescriptor("aₙ", "Folge")
        MathematikTypen.SymbolischeDimension -> TypVisualDescriptor("n", "Symbolische Dimension")
        else -> if (id.wert.startsWith("mathematik.dimension.")) {
            val dimension = id.wert.substringAfterLast('.')
            TypVisualDescriptor(dimension, "Dimension $dimension")
        } else {
            StandardTypVisualAuflöser.beschreibe(TypAusdruck.Atom(id))
        }
    }
}

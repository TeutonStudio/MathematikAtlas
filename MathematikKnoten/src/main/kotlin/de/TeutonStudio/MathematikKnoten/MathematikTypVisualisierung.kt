package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

/**
 * Mathematische Notation für die Orchestrator-artigen Minigrafiken an Ports.
 * Die Segmentschlüssel bleiben stabile semantische IDs; die UI darf daraus
 * Farben, Muster oder spätere SVG-Glyphen ableiten.
 */
object MathematikTypVisualResolver : TypVisualResolver {
    override fun beschreibe(typ: TypAusdruck): TypVisualDescriptor = when (typ) {
        TypAusdruck.Beliebig -> TypVisualDescriptor("*", "Beliebiger mathematischer Typ")
        TypAusdruck.Unbekannt -> TypVisualDescriptor("?", "Mathematischer Typ noch unbekannt")
        is TypAusdruck.Atom -> atom(typ.id)
        is TypAusdruck.Variable -> TypVisualDescriptor(typ.id.wert, "Typvariable ${typ.id.wert}")
        is TypAusdruck.Vereinigung -> vereinigung(typ)
        is TypAusdruck.Parameterisiert -> parameterisiert(typ)
    }

    private fun atom(id: TypId): TypVisualDescriptor {
        val text = when (id) {
            MathematikTypen.NatürlicheZahl -> "ℕ"
            MathematikTypen.GanzeZahl -> "ℤ"
            MathematikTypen.RationaleZahl -> "ℚ"
            MathematikTypen.ReelleZahl -> "ℝ"
            MathematikTypen.KomplexeZahl -> "ℂ"
            MathematikTypen.Zahl -> "Zahl"
            MathematikTypen.Aussage -> "𝔹"
            MathematikTypen.Menge -> "M"
            MathematikTypen.Mass -> "μ"
            MathematikTypen.Vektor -> "v"
            MathematikTypen.SpaltenVektor -> "v↓"
            MathematikTypen.ZeilenVektor -> "v→"
            MathematikTypen.Matrix -> "A"
            MathematikTypen.Tensor -> "T"
            MathematikTypen.Methode -> "ƒ"
            MathematikTypen.Objekt -> "Obj"
            else -> id.wert.substringAfterLast('.')
        }
        return TypVisualDescriptor(
            kurztext = text,
            tooltip = id.wert,
            segmente = listOf(TypVisualSegment(id.wert, text)),
        )
    }

    private fun parameterisiert(typ: TypAusdruck.Parameterisiert): TypVisualDescriptor = when (typ.konstruktor) {
        TypKernIds.Tupel -> {
            val komponenten = typ.argumente.map(::beschreibe)
            TypVisualDescriptor(
                kurztext = komponenten.joinToString(prefix = "(", postfix = ")") { it.kurztext },
                tooltip = komponenten.joinToString(prefix = "Tupel<", postfix = ">") { it.tooltip },
                segmente = listOf(TypVisualSegment(TypKernIds.Tupel.wert, "( )")),
            )
        }
        MathematikTypen.Methode -> {
            val argument = typ.argumente.getOrNull(0)?.let(::beschreibe)
                ?: TypVisualDescriptor("?", "Unbekannter Wertevorrat")
            val ziel = typ.argumente.getOrNull(1)?.let(::beschreibe)
                ?: TypVisualDescriptor("?", "Unbekannte Zielmenge")
            TypVisualDescriptor(
                kurztext = "${argument.kurztext}→${ziel.kurztext}",
                tooltip = "Methode ${argument.tooltip} → ${ziel.tooltip}",
                segmente = listOf(TypVisualSegment(MathematikTypen.Methode.wert, "ƒ")),
            )
        }
        else -> StandardTypVisualResolver.beschreibe(typ)
    }

    private fun vereinigung(typ: TypAusdruck.Vereinigung): TypVisualDescriptor {
        val teile = typ.alternativen.map(::beschreibe)
        return TypVisualDescriptor(
            kurztext = teile.joinToString("∨") { it.kurztext },
            tooltip = teile.joinToString(" oder ") { it.tooltip },
            segmente = teile.flatMap { descriptor ->
                descriptor.segmente.ifEmpty {
                    listOf(TypVisualSegment(descriptor.kurztext, descriptor.kurztext))
                }
            }.distinctBy(TypVisualSegment::schlüssel),
        )
    }
}

fun AnschlussDaten.mathematischeTypVisualisierung(
    effektiverTyp: TypAusdruck = vertrag.typ,
): TypVisualDescriptor = MathematikTypVisualResolver.beschreibe(effektiverTyp)

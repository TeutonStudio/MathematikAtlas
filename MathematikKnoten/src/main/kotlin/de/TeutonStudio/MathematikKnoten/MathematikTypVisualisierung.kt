package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

/** Mathematische Notation für kompakte, Orchestrator-artige Typgrafiken an Ports. */
object MathematikTypVisualResolver : TypVisualResolver {
    override fun beschreibe(typ: TypAusdruck): TypVisualDescriptor = when (typ) {
        TypAusdruck.Beliebig -> TypVisualDescriptor("*", "Beliebiger mathematischer Typ")
        TypAusdruck.Unbekannt -> TypVisualDescriptor("?", "Mathematischer Typ noch unbekannt")
        is TypAusdruck.Atom -> atom(typ.id)
        is TypAusdruck.Literal -> TypVisualDescriptor(typ.wert, "Typkonstante ${typ.wert}")
        is TypAusdruck.Variable -> TypVisualDescriptor(typ.id.wert, "Typvariable ${typ.id.wert}")
        is TypAusdruck.Vereinigung -> vereinigung(typ)
        is TypAusdruck.Parameterisiert -> parameterisiert(typ)
    }

    private fun atom(id: TypId): TypVisualDescriptor {
        val text = when (id) {
            MathematikTypen.NatürlicheZahl -> "ℕ"
            MathematikTypen.NichtnegativeGanzeZahl -> "ℕ₀"
            MathematikTypen.GanzeZahl -> "ℤ"
            MathematikTypen.RationaleZahl -> "ℚ"
            MathematikTypen.ReelleZahl -> "ℝ"
            MathematikTypen.KomplexeZahl -> "ℂ"
            MathematikTypen.QuaternionZahl -> "ℍ"
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
        return TypVisualDescriptor(text, id.wert, listOf(TypVisualSegment(id.wert, text)))
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
        MathematikTypen.Menge -> {
            val element = typ.argumente.firstOrNull()?.let(::beschreibe) ?: TypVisualDescriptor("?", "Unbekannter Elementtyp")
            TypVisualDescriptor(
                kurztext = "M⟨${element.kurztext}⟩",
                tooltip = "Menge mit Elementtyp ${element.tooltip}",
                segmente = listOf(TypVisualSegment(MathematikTypen.Menge.wert, "M")),
            )
        }
        MathematikTypen.SpaltenVektor, MathematikTypen.ZeilenVektor -> {
            val element = typ.argumente.getOrNull(0)?.let(::beschreibe) ?: TypVisualDescriptor("?", "Unbekannter Skalartyp")
            val dimension = (typ.argumente.getOrNull(1) as? TypAusdruck.Literal)?.wert ?: "?"
            val text = if (typ.konstruktor == MathematikTypen.SpaltenVektor) "${element.kurztext}^{$dimension×1}"
            else "${element.kurztext}^{1×$dimension}"
            TypVisualDescriptor(
                kurztext = text,
                tooltip = if (typ.konstruktor == MathematikTypen.SpaltenVektor) "Spaltenvektor $text" else "Zeilenvektor $text",
                segmente = listOf(TypVisualSegment(typ.konstruktor.wert, if (typ.konstruktor == MathematikTypen.SpaltenVektor) "v↓" else "v→")),
            )
        }
        MathematikTypen.Matrix -> {
            val element = typ.argumente.getOrNull(0)?.let(::beschreibe) ?: TypVisualDescriptor("?", "Unbekannter Skalartyp")
            val zeilen = (typ.argumente.getOrNull(1) as? TypAusdruck.Literal)?.wert ?: "?"
            val spalten = (typ.argumente.getOrNull(2) as? TypAusdruck.Literal)?.wert ?: "?"
            val text = "${element.kurztext}^{$zeilen×$spalten}"
            TypVisualDescriptor(text, "Matrix $text", listOf(TypVisualSegment(MathematikTypen.Matrix.wert, "A")))
        }
        MathematikTypen.Tensor -> {
            val element = typ.argumente.getOrNull(0)?.let(::beschreibe) ?: TypVisualDescriptor("?", "Unbekannter Elementtyp")
            val form = (typ.argumente.getOrNull(1) as? TypAusdruck.Literal)?.wert ?: "?"
            val text = "${element.kurztext}^{$form}"
            TypVisualDescriptor(text, "Tensor $text", listOf(TypVisualSegment(MathematikTypen.Tensor.wert, "T")))
        }
        MathematikTypen.Methode -> {
            val argument = typ.argumente.getOrNull(0)?.let(::beschreibe) ?: TypVisualDescriptor("?", "Unbekannter Wertevorrat")
            val ziel = typ.argumente.getOrNull(1)?.let(::beschreibe) ?: TypVisualDescriptor("?", "Unbekannte Zielmenge")
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
                descriptor.segmente.ifEmpty { listOf(TypVisualSegment(descriptor.kurztext, descriptor.kurztext)) }
            }.distinctBy(TypVisualSegment::schlüssel),
        )
    }
}

fun AnschlussDaten.mathematischeTypVisualisierung(
    effektiverTyp: TypAusdruck = vertrag.typ,
): TypVisualDescriptor = MathematikTypVisualResolver.beschreibe(effektiverTyp)

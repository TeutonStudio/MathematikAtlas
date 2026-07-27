package de.TeutonStudio.MathematikRechenSystem.kern

data class EndlicheMenge(val elemente: Set<MathematischesObjekt>) : MengenAusdruck {
    override fun zuLatex() = elemente.joinToString(prefix = "\\{", postfix = "\\}") { it.zuLatex() }
}

data object LeereMenge : MengenAusdruck { override fun zuLatex() = "\\varnothing" }

data class BenannteMenge(val name: String, val latex: String = name) : MengenAusdruck { override fun zuLatex() = latex }

data class Vereinigung(val mengen: List<MengenAusdruck>) : MengenAusdruck {
    override fun zuLatex() = mengen.joinToString(" \\cup ") { it.zuLatex() }
}

data class Schnitt(val mengen: List<MengenAusdruck>) : MengenAusdruck {
    override fun zuLatex() = mengen.joinToString(" \\cap ") { it.zuLatex() }
}

data class MengenDifferenz(val links: MengenAusdruck, val rechts: MengenAusdruck) : MengenAusdruck {
    override fun zuLatex() = "${links.zuLatex()} \\setminus ${rechts.zuLatex()}"
}

data class ElementBeziehung(val element: MathematischesObjekt, val menge: MengenAusdruck) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis = when (menge) {
        is EndlicheMenge -> if (element in menge.elemente) AussageErgebnis(Wahrheitswert.Wahr, EntscheidungsStatus.Bewiesen) else AussageErgebnis(Wahrheitswert.Falsch, EntscheidungsStatus.Widerlegt)
        LeereMenge -> AussageErgebnis(Wahrheitswert.Falsch, EntscheidungsStatus.Widerlegt)
        else -> AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
    }
    override fun zuLatex() = "${element.zuLatex()} \\in ${menge.zuLatex()}"
}

data class TeilmengenBeziehung(val links: MengenAusdruck, val rechts: MengenAusdruck) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis {
        if (links is EndlicheMenge && rechts is EndlicheMenge) {
            val wahr = rechts.elemente.containsAll(links.elemente)
            return AussageErgebnis(if (wahr) Wahrheitswert.Wahr else Wahrheitswert.Falsch, if (wahr) EntscheidungsStatus.Bewiesen else EntscheidungsStatus.Widerlegt)
        }
        return AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
    }
    override fun zuLatex() = "${links.zuLatex()} \\subseteq ${rechts.zuLatex()}"
}

val NatürlicheZahlen = BenannteMenge("Natürliche Zahlen", "\\mathbb{N}")
val GanzeZahlen = BenannteMenge("Ganze Zahlen", "\\mathbb{Z}")
val RationaleZahlen = BenannteMenge("Rationale Zahlen", "\\mathbb{Q}")
val ReelleZahlen = BenannteMenge("Reelle Zahlen", "\\mathbb{R}")

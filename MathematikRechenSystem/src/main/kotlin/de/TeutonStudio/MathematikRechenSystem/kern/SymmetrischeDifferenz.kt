package de.TeutonStudio.MathematikRechenSystem.kern

/** Symbolische symmetrische Differenz zweier Mengen. */
data class SymmetrischeDifferenz(
    val links: MengenAusdruck,
    val rechts: MengenAusdruck,
) : MengenAusdruck {
    override fun zuLatex(): String = "${links.zuLatex()} \\triangle ${rechts.zuLatex()}"
}

/**
 * Bildet die symmetrische Differenz und normalisiert elementare Identitäten sowie
 * vollständig bekannte endliche Mengen. Symbolische Operanden werden kanonisch
 * geordnet, damit die Kommutativität auch strukturell sichtbar bleibt.
 */
fun symmetrischeDifferenz(
    links: MengenAusdruck,
    rechts: MengenAusdruck,
): MengenAusdruck = when {
    links == rechts -> LeereMenge
    links == LeereMenge -> rechts
    rechts == LeereMenge -> links
    links is EndlicheMenge && rechts is EndlicheMenge -> {
        val elemente = (links.elemente - rechts.elemente) + (rechts.elemente - links.elemente)
        if (elemente.isEmpty()) LeereMenge else EndlicheMenge(elemente)
    }
    else -> {
        val (erster, zweiter) = listOf(links, rechts).sortedBy(::strukturellerSchlüssel)
        SymmetrischeDifferenz(erster, zweiter)
    }
}

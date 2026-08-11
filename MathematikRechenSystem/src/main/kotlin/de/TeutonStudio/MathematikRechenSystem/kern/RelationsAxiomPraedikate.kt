package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Verbindet die kompakten Relationsmetadaten mit den tatsächlich verwendbaren
 * Axiom-Prädikaten. Damit sind Chip-Klassifikation und Prädikatseite keine zwei
 * voneinander unabhängigen Axiomnamensräume.
 */
val RelationsAxiom.praedikatAxiomId: String
    get() = when (this) {
        RelationsAxiom.REFLEXIV -> "axiom.relation.reflexiv"
        RelationsAxiom.IRREFLEXIV -> "axiom.relation.irreflexiv"
        RelationsAxiom.SYMMETRISCH -> "axiom.relation.symmetrisch"
        RelationsAxiom.ANTISYMMETRISCH -> "axiom.relation.antisymmetrisch"
        RelationsAxiom.ASYMMETRISCH -> "axiom.relation.asymmetrisch"
        RelationsAxiom.TRANSITIV -> "axiom.relation.transitiv"
        RelationsAxiom.TOTAL -> "axiom.relation.total"
    }

fun RelationsAxiom.praedikatDefinition(): AxiomOperatorDefinition =
    checkNotNull(AxiomOperatoren.vonIdOderNull(praedikatAxiomId)) {
        "Für das Relationsaxiom $name fehlt das Axiom-Prädikat $praedikatAxiomId."
    }

fun RelationsStruktur.axiomPraedikate(): List<AxiomOperatorDefinition> =
    nachweise.map { it.axiom.praedikatDefinition() }.distinctBy { it.stabileId }

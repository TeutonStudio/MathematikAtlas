package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Kanonische mathematische Definition einer registrierten Relation.
 *
 * [symbolLatex][RelationsOperatorDefinition.symbolLatex] bleibt die kompakte
 * Schreibweise des Operators. Diese Struktur beschreibt dagegen die Formel,
 * die in Definitionskarten und Dokumentation tatsächlich als Definition
 * angezeigt wird.
 */
data class RelationsDefinitionsFormel(
    val latex: String,
    val vorausgesetzteAxiomIds: Set<String> = emptySet(),
    val hinweis: String = "",
) {
    init {
        require(latex.isNotBlank())
        require(vorausgesetzteAxiomIds.none(String::isBlank))
    }
}

/**
 * Vollständiges Definitionsregister für [RelationsOperatoren].
 *
 * Absichtlich ohne Fallback auf das Relationssymbol: Eine neue Relation soll
 * nicht unbemerkt eine Definitionskarte erhalten, die lediglich `R(a,b)`
 * wiederholt. Fehlende mathematische Semantik fällt dadurch sofort in Tests
 * und Entwicklung auf.
 */
object RelationsDefinitionsFormeln {
    private fun formel(
        latex: String,
        vararg vorausgesetzteAxiome: String,
        hinweis: String = "",
    ) = RelationsDefinitionsFormel(
        latex = latex,
        vorausgesetzteAxiomIds = vorausgesetzteAxiome.toSet(),
        hinweis = hinweis,
    )

    private val nachId: Map<String, RelationsDefinitionsFormel> = mapOf(
        "relation.gleichheit" to formel(
            "a=b\\Longleftrightarrow\\forall M\\left(\\operatorname{Menge}(M)\\Rightarrow\\left(a\\in M\\Leftrightarrow b\\in M\\right)\\right)",
            "axiom.zf.paarmenge",
            hinweis = "Menge(M) ist die Typbedingung, dass M eine Menge ist. Die Rückrichtung benutzt die durch das Paarmengenaxiom verfügbare Einzelmenge {a}.",
        ),
        "relation.ungleichheit" to formel(
            "a\\neq b\\Longleftrightarrow\\exists M\\left(\\operatorname{Menge}(M)\\land\\neg\\left(a\\in M\\Leftrightarrow b\\in M\\right)\\right)",
            "axiom.zf.paarmenge",
            hinweis = "Die Ungleichheit ist die Negation der Gleichheit; äquivalent existiert eine Menge, die genau eines der beiden Objekte enthält.",
        ),
        "relation.kleiner" to formel(
            latex = "a<b\\Longleftrightarrow\\exists c\\in\\mathbb{R}\\setminus\\{0\\}:\\;a+c^2=b",
            hinweis = "Die Definition verwendet die positive Quadratzahl c² als strikt positiven Abstand.",
        ),
        "relation.groesser" to formel(
            latex = "a>b\\Longleftrightarrow b<a",
        ),
        "relation.kleinerGleich" to formel(
            latex = "a\\le b\\Longleftrightarrow(a<b)\\lor(a=b)",
        ),
        "relation.groesserGleich" to formel(
            latex = "a\\ge b\\Longleftrightarrow(a>b)\\lor(a=b)",
        ),

        MengenRelationsOperator.ELEMENT.stabileId to formel(
            latex = "x\\in A",
            hinweis = "Die Elementbeziehung ist die primitive Mengenrelation; sie wird nicht zirkulär durch eine andere Mengenrelation definiert.",
        ),
        MengenRelationsOperator.TEIL_ODER_GLEICHMENGE.stabileId to formel(
            latex = "A\\subseteq B\\Longleftrightarrow\\forall x\\left(x\\in A\\Rightarrow x\\in B\\right)",
        ),
        MengenRelationsOperator.TEILMENGE.stabileId to formel(
            latex = "A\\subset B\\Longleftrightarrow(A\\subseteq B)\\land(A\\neq B)",
        ),
        MengenRelationsOperator.UEBER_ODER_GLEICHMENGE.stabileId to formel(
            latex = "A\\supseteq B\\Longleftrightarrow B\\subseteq A",
        ),
        MengenRelationsOperator.UEBERMENGE.stabileId to formel(
            latex = "A\\supset B\\Longleftrightarrow B\\subset A",
        ),
        MengenRelationsOperator.DISJUNKT.stabileId to formel(
            latex = "A\\cap B=\\varnothing\\Longleftrightarrow\\forall x\\;\\neg\\left(x\\in A\\land x\\in B\\right)",
        ),

        "geometrie.relation.inzidenz" to formel(
            latex = "P\\mathrel{\\mathbf I}G\\Longleftrightarrow P\\in\\operatorname{Punktmenge}(G)",
        ),
        "geometrie.relation.zwischenlage" to formel(
            latex = "\\operatorname{Zwischen}(A,B,C)\\Longleftrightarrow\\operatorname{kollinear}(A,B,C)\\land\\langle A-B,C-B\\rangle\\le0",
        ),
        "geometrie.relation.kollinear" to formel(
            latex = "\\operatorname{kollinear}(A,B,C)\\Longleftrightarrow\\exists g:\\;A\\mathrel{\\mathbf I}g\\land B\\mathrel{\\mathbf I}g\\land C\\mathrel{\\mathbf I}g",
        ),
        "geometrie.relation.parallel" to formel(
            latex = "g\\parallel h\\Longleftrightarrow g\\not\\equiv_G h\\land\\exists\\lambda\\in\\mathbb{R}\\setminus\\{0\\}:\\;\\vec d_g=\\lambda\\vec d_h",
        ),
        "geometrie.relation.orthogonal" to formel(
            latex = "g\\perp h\\Longleftrightarrow\\langle\\vec d_g,\\vec d_h\\rangle=0",
        ),
        "geometrie.relation.gleichheit" to formel(
            latex = "A\\equiv_G B\\Longleftrightarrow\\forall P\\left(P\\mathrel{\\mathbf I}A\\Leftrightarrow P\\mathrel{\\mathbf I}B\\right)",
            hinweis = "Geometrische Gleichheit meint Gleichheit der dargestellten Punktmengen, nicht Kotlin-Objektidentität.",
        ),
        "geometrie.relation.streckenkongruenz" to formel(
            latex = "s_1\\cong s_2\\Longleftrightarrow\\ell(s_1)=\\ell(s_2)",
        ),
        "geometrie.relation.winkelkongruenz" to formel(
            latex = "\\alpha\\cong\\beta\\Longleftrightarrow|\\alpha|=|\\beta|",
        ),
    )

    fun für(definition: RelationsOperatorDefinition): RelationsDefinitionsFormel =
        checkNotNull(nachId[definition.stabileId]) {
            "Für die Relation '${definition.stabileId}' fehlt eine kanonische Definitionsformel."
        }

    fun fehlendeDefinitionen(): Set<String> =
        RelationsOperatoren.alle.mapTo(mutableSetOf()) { it.stabileId } - nachId.keys
}

val RelationsOperatorDefinition.definitionsFormel: RelationsDefinitionsFormel
    get() = RelationsDefinitionsFormeln.für(this)

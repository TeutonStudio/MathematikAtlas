package de.TeutonStudio.MathematikRechenSystem.kern

enum class MengenRelationsOperator(
    val stabileId: String,
    val titel: String,
    val symbolLatex: String,
) {
    UEBERMENGE("menge.relation.uebermenge", "Echte Übermenge", "A\\supset B"),
    TEILMENGE("menge.relation.teilmenge", "Echte Teilmenge", "A\\subset B"),
    UEBER_ODER_GLEICHMENGE("menge.relation.ueberOderGleichmenge", "Übermenge", "A\\supseteq B"),
    TEIL_ODER_GLEICHMENGE("menge.relation.teilOderGleichmenge", "Teilmenge", "A\\subseteq B"),
    ELEMENT("menge.relation.element", "Element", "x\\in A"),
    DISJUNKT("menge.relation.disjunkt", "Disjunkt", "A\\cap B=\\varnothing"),
    ;

    companion object {
        fun vonIdOderNull(id: String?): MengenRelationsOperator? = entries.firstOrNull { operator ->
            id == operator.stabileId || id.equals(operator.name, ignoreCase = true)
        }
    }
}

object MengenRelationRechner {
    const val KNOTEN_ART = "mathematik.mengenrelation"

    fun erzeuge(
        operator: MengenRelationsOperator,
        links: MathematischesObjekt,
        rechts: MathematischesObjekt,
    ): Aussage = when (operator) {
        MengenRelationsOperator.ELEMENT -> ElementBeziehung(
            element = links,
            menge = rechts as? MengenAusdruck
                ?: error("Die Elementrelation benötigt rechts eine Menge."),
        )
        MengenRelationsOperator.TEILMENGE -> EchteTeilmengeBeziehung(
            links = links.alsMenge("links"),
            rechts = rechts.alsMenge("rechts"),
        )
        MengenRelationsOperator.UEBERMENGE -> ObermengenBeziehung(
            links = links.alsMenge("links"),
            rechts = rechts.alsMenge("rechts"),
            echt = true,
        )
        MengenRelationsOperator.TEIL_ODER_GLEICHMENGE -> TeilmengenBeziehung(
            links = links.alsMenge("links"),
            rechts = rechts.alsMenge("rechts"),
        )
        MengenRelationsOperator.UEBER_ODER_GLEICHMENGE -> ObermengenBeziehung(
            links = links.alsMenge("links"),
            rechts = rechts.alsMenge("rechts"),
        )
        MengenRelationsOperator.DISJUNKT -> Disjunktheit(
            links = links.alsMenge("links"),
            rechts = rechts.alsMenge("rechts"),
        )
    }

    private fun MathematischesObjekt.alsMenge(rolle: String): MengenAusdruck =
        this as? MengenAusdruck ?: error("Die Rolle '$rolle' benötigt eine Menge.")
}

object MengenRelationsMigration {
    val alteKnotenArten: Map<String, MengenRelationsOperator> = mapOf(
        "mathematik.übermenge" to MengenRelationsOperator.UEBERMENGE,
        "mathematik.teilmenge" to MengenRelationsOperator.TEILMENGE,
        "mathematik.überOderGleichmenge" to MengenRelationsOperator.UEBER_ODER_GLEICHMENGE,
        "mathematik.teilOderGleichmenge" to MengenRelationsOperator.TEIL_ODER_GLEICHMENGE,
        "mathematik.element" to MengenRelationsOperator.ELEMENT,
        "mathematik.disjunkt" to MengenRelationsOperator.DISJUNKT,
    )
}

package de.TeutonStudio.MathematikRechenSystem.kern

@JvmInline
value class VerzweigungsQuellenId(val wert: String) {
    init { require(wert.isNotBlank()) }
}

enum class VorzeichenReihenfolge(val latex: String) {
    PLUS_MINUS("\\pm"),
    MINUS_PLUS("\\mp"),
}

data class VorzeichenVerzweigung(
    val reihenfolge: VorzeichenReihenfolge,
    val operand: ZahlAusdruck,
    val quellenId: VerzweigungsQuellenId,
) : MathematischesObjekt {
    val operatorId: String = when (reihenfolge) {
        VorzeichenReihenfolge.PLUS_MINUS -> "algebra.vorzeichen.plusMinus"
        VorzeichenReihenfolge.MINUS_PLUS -> "algebra.vorzeichen.minusPlus"
    }

    override fun zuLatex(): String = "${reihenfolge.latex}\\,${operand.zuLatex()}"

    fun entfalte(): VerzweigungsTupel {
        val werte = when (reihenfolge) {
            VorzeichenReihenfolge.PLUS_MINUS -> listOf(operand, negation(operand))
            VorzeichenReihenfolge.MINUS_PLUS -> listOf(negation(operand), operand)
        }
        return VerzweigungsTupel(
            zweige = werte.mapIndexed { index, wert ->
                VerzweigungsZweig(wert, linkedMapOf(quellenId to index))
            },
            quellenReihenfolge = listOf(quellenId),
        )
    }
}

data class VerzweigungsZweig(
    val wert: MathematischesObjekt,
    val belegung: Map<VerzweigungsQuellenId, Int>,
) {
    init { require(belegung.values.all { it >= 0 }) }
}

data class VerzweigungsTupel(
    val zweige: List<VerzweigungsZweig>,
    val quellenReihenfolge: List<VerzweigungsQuellenId>,
) : MathematischesObjekt {
    init {
        require(zweige.isNotEmpty())
        require(quellenReihenfolge.distinct().size == quellenReihenfolge.size)
        require(zweige.all { zweig -> zweig.belegung.keys.all { it in quellenReihenfolge } })
    }

    val komponenten: List<MathematischesObjekt> get() = zweige.map(VerzweigungsZweig::wert)

    override fun zuLatex(): String = komponenten.joinToString(
        prefix = "\\left(",
        separator = ",",
        postfix = "\\right)",
    ) { it.zuLatex() }

    fun alsLoesungsMenge(): EndlicheMenge = EndlicheMenge(komponenten.toSet())
}

fun VerzweigungsTupel.wendeKomponentenweiseAn(
    operation: (MathematischesObjekt) -> MathematischesObjekt,
): VerzweigungsTupel = copy(
    zweige = zweige.map { zweig -> zweig.copy(wert = operation(zweig.wert)) },
)

fun kombiniereVerzweigungen(
    links: VerzweigungsTupel,
    rechts: VerzweigungsTupel,
    operation: (MathematischesObjekt, MathematischesObjekt) -> MathematischesObjekt,
): VerzweigungsTupel {
    val gemeinsameQuellen = links.quellenReihenfolge.toSet().intersect(rechts.quellenReihenfolge.toSet())
    val quellen = links.quellenReihenfolge + rechts.quellenReihenfolge.filterNot { it in links.quellenReihenfolge }
    val zweige = buildList {
        for (linkerZweig in links.zweige) {
            for (rechterZweig in rechts.zweige) {
                val kompatibel = gemeinsameQuellen.all { quelle ->
                    linkerZweig.belegung[quelle] == rechterZweig.belegung[quelle]
                }
                if (!kompatibel) continue
                add(
                    VerzweigungsZweig(
                        wert = operation(linkerZweig.wert, rechterZweig.wert),
                        belegung = linkerZweig.belegung + rechterZweig.belegung,
                    ),
                )
            }
        }
    }
    require(zweige.isNotEmpty()) { "Die gekoppelten Verzweigungsquellen besitzen keine kompatiblen Zweige." }
    return VerzweigungsTupel(zweige, quellen)
}

fun multipliziereVerzweigungen(
    links: VerzweigungsTupel,
    rechts: VerzweigungsTupel,
): VerzweigungsTupel = kombiniereVerzweigungen(links, rechts) { a, b ->
    require(a is ZahlAusdruck && b is ZahlAusdruck)
    multiplikation(a, b)
}

enum class QuadratischerLoesungsSchrittArt {
    VORZEICHEN_SUBSTITUTION,
    HAUPTWURZEL,
    RUECKSUBSTITUTION,
}

data class QuadratischerLoesungsSchritt(
    val art: QuadratischerLoesungsSchrittArt,
    val latex: String,
)

data class QuadratischeVorzeichenLoesung(
    val variable: Variable,
    val rechteSeite: ZahlAusdruck,
    val hilfsVariable: Variable,
    val quellenId: VerzweigungsQuellenId,
    val schritte: List<QuadratischerLoesungsSchritt>,
    val geordnetesErgebnis: VerzweigungsTupel,
    val bedingungen: List<Aussage>,
) {
    fun alsLoesungsMenge(): EndlicheMenge = geordnetesErgebnis.alsLoesungsMenge()
}

fun loeseQuadratischePotenzMitVorzeichenSubstitution(
    variable: Variable,
    rechteSeite: ZahlAusdruck,
    quellenId: VerzweigungsQuellenId = VerzweigungsQuellenId("${variable.name}.quadratisch"),
    ueberReellenZahlen: Boolean = true,
): QuadratischeVorzeichenLoesung {
    val phi = Variable("\\varphi")
    val hauptwurzel = Wurzel(rechteSeite)
    val substitution = VorzeichenVerzweigung(
        VorzeichenReihenfolge.PLUS_MINUS,
        phi,
        quellenId,
    )
    val ruecksubstitution = VorzeichenVerzweigung(
        VorzeichenReihenfolge.PLUS_MINUS,
        hauptwurzel,
        quellenId,
    )
    val bedingungen = if (ueberReellenZahlen) {
        listOf(Vergleich(RationaleZahl.Null, VergleichsArt.KleinerGleich, rechteSeite))
    } else {
        emptyList()
    }
    return QuadratischeVorzeichenLoesung(
        variable = variable,
        rechteSeite = rechteSeite,
        hilfsVariable = phi,
        quellenId = quellenId,
        schritte = listOf(
            QuadratischerLoesungsSchritt(
                QuadratischerLoesungsSchrittArt.VORZEICHEN_SUBSTITUTION,
                "${variable.zuLatex()}=${substitution.zuLatex()}",
            ),
            QuadratischerLoesungsSchritt(
                QuadratischerLoesungsSchrittArt.HAUPTWURZEL,
                "${phi.zuLatex()}=${hauptwurzel.zuLatex()}",
            ),
            QuadratischerLoesungsSchritt(
                QuadratischerLoesungsSchrittArt.RUECKSUBSTITUTION,
                "${variable.zuLatex()}=${ruecksubstitution.zuLatex()}",
            ),
        ),
        geordnetesErgebnis = ruecksubstitution.entfalte(),
        bedingungen = bedingungen,
    )
}

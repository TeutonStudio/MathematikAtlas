package de.TeutonStudio.MathematikRechenSystem.kern

/** Projektterminologie: unendliches, mit N ab 1 indiziertes Tupel. */
fun interface UnnatuerlichesTupel<T> {
    fun komponente(index: Long): T
}

/** Echte Folge im Mathematik Atlas: eine Abbildung Z -> X. */
fun interface ZFolge<T> {
    fun wert(index: Long): T
}

enum class FolgenRichtung(val schritt: Long, val latex: String) {
    POSITIV(1L, "+\\infty"),
    NEGATIV(-1L, "-\\infty"),
}

enum class FolgenAnalyseRichtung {
    POSITIV,
    NEGATIV,
    BEIDSEITIG,
}

/**
 * Nicht materialisierte Ansicht A_(s,d)(f)_n = f(s+d(n-1)).
 */
data class GerichteteFolgenAnsicht<T>(
    val folge: ZFolge<T>,
    val startIndex: Long,
    val richtung: FolgenRichtung,
) : UnnatuerlichesTupel<T> {
    override fun komponente(index: Long): T {
        require(index >= 1L) { "Ein unnatürliches Tupel ist ab Index 1 definiert." }
        val abstand = Math.multiplyExact(richtung.schritt, index - 1L)
        return folge.wert(Math.addExact(startIndex, abstand))
    }
}

interface NullElementVertrag<T> {
    val traegerId: String
    val traeger: MengenAusdruck
    fun nullElement(): T
}

/**
 * E_(s,d)(u)(z): betroffener Ast aus u, Gegenseite aus dem ausgezeichneten
 * Nullelement. Die Konstruktion bleibt eine Funktion und erzeugt keine Liste.
 */
data class EingebetteteUnnatuerlicheFolge<T>(
    val tupel: UnnatuerlichesTupel<T>,
    val startIndex: Long,
    val richtung: FolgenRichtung,
    val nullVertrag: NullElementVertrag<T>,
) : ZFolge<T> {
    override fun wert(index: Long): T {
        val gerichteterAbstand = Math.multiplyExact(richtung.schritt, Math.subtractExact(index, startIndex))
        return if (gerichteterAbstand >= 0L) {
            tupel.komponente(Math.addExact(1L, gerichteterAbstand))
        } else {
            nullVertrag.nullElement()
        }
    }

    fun aktiverAst(): GerichteteFolgenAnsicht<T> =
        GerichteteFolgenAnsicht(this, startIndex, richtung)
}

object StandardNullElementVertraege {
    val rationaleZahlen = object : NullElementVertrag<ZahlAusdruck> {
        override val traegerId: String = "zahlbereich.Q"
        override val traeger: MengenAusdruck = RationaleZahlen
        override fun nullElement(): ZahlAusdruck = RationaleZahl.Null
    }

    fun vektorraum(
        dimension: Int,
        orientierung: VektorOrientierung,
        skalarMenge: MengenAusdruck,
    ): NullElementVertrag<OrientierterVektor> = object : NullElementVertrag<OrientierterVektor> {
        override val traegerId: String = "vektorraum.${orientierung.name}.$dimension.${skalarMenge.zuLatex()}"
        override val traeger: MengenAusdruck = Vektorraum(orientierung, dimension, skalarMenge)
        override fun nullElement(): OrientierterVektor {
            val werte = List<ZahlAusdruck>(dimension) { RationaleZahl.Null }
            return if (orientierung == VektorOrientierung.Zeile) ZeilenVektor(werte) else SpaltenVektor(werte)
        }
    }

    fun matrizenraum(
        zeilen: Int,
        spalten: Int,
        skalarMenge: MengenAusdruck,
    ): NullElementVertrag<Matrix> = object : NullElementVertrag<Matrix> {
        override val traegerId: String = "matrizenraum.$zeilen.$spalten.${skalarMenge.zuLatex()}"
        override val traeger: MengenAusdruck = Matrizenraum(zeilen, spalten, skalarMenge)
        override fun nullElement(): Matrix = Matrix(
            List(zeilen) { List<ZahlAusdruck>(spalten) { RationaleZahl.Null } },
        )
    }
}

enum class FolgenAnalyseOperator(val stabileId: String, val ergebnisTyp: FormelTyp) {
    NACH_OBEN_BESCHRAENKT("folge.nachObenBeschraenkt", FormelTyp.AUSSAGE),
    NACH_UNTEN_BESCHRAENKT("folge.nachUntenBeschraenkt", FormelTyp.AUSSAGE),
    BESCHRAENKT("folge.beschraenkt", FormelTyp.AUSSAGE),
    MONOTON_STEIGEND("folge.monotonSteigend", FormelTyp.AUSSAGE),
    MONOTON_FALLEND("folge.monotonFallend", FormelTyp.AUSSAGE),
    STRENG_MONOTON_STEIGEND("folge.strengMonotonSteigend", FormelTyp.AUSSAGE),
    STRENG_MONOTON_FALLEND("folge.strengMonotonFallend", FormelTyp.AUSSAGE),
    SCHLIESSLICH_KONSTANT("folge.schliesslichKonstant", FormelTyp.AUSSAGE),
    SCHLIESSLICH_POSITIV("folge.schliesslichPositiv", FormelTyp.AUSSAGE),
    SCHLIESSLICH_NEGATIV("folge.schliesslichNegativ", FormelTyp.AUSSAGE),
    KONVERGENT("folge.konvergent", FormelTyp.AUSSAGE),
    LIMES("folge.limes", FormelTyp.OBJEKT),
    NULL_LIMES("folge.nullLimes", FormelTyp.AUSSAGE),
    DIVERGENZART("folge.divergenzart", FormelTyp.OBJEKT),
    CAUCHY("folge.cauchy", FormelTyp.AUSSAGE),
    TEILFOLGE("folge.teilfolge", FormelTyp.OBJEKT),
    HAEUFUNGSWERT("folge.haeufungswert", FormelTyp.AUSSAGE),
    HAEUFUNGSWERTE("folge.haeufungswerte", FormelTyp.MENGE),
    LIMSUP("folge.limsup", FormelTyp.OBJEKT),
    LIMINF("folge.liminf", FormelTyp.OBJEKT),
}

enum class DivergenzArt {
    KONVERGIERT_ENDLICH,
    POSITIV_UNENDLICH,
    NEGATIV_UNENDLICH,
    ENDLICH_OSZILLIEREND,
    UNBESCHRAENKT_OSZILLIEREND,
    KEIN_EINDEUTIGER_LIMES,
    UNENTSCHEIDBAR,
    NICHT_ANWENDBAR,
}

sealed interface FolgenAnalyseStatus {
    data object Nachgewiesen : FolgenAnalyseStatus
    data object Widerlegt : FolgenAnalyseStatus
    data class Bedingt(val bedingungen: List<String>) : FolgenAnalyseStatus
    data class Unentscheidbar(val grund: String) : FolgenAnalyseStatus
    data class NichtAnwendbar(val grund: String) : FolgenAnalyseStatus
}

data class FolgenAnalyseDefinition(
    val operator: FolgenAnalyseOperator,
    val richtung: FolgenAnalyseRichtung,
    val startIndex: Long,
    val nichtstandardLatex: String,
    val klassischLatex: String,
    val voraussetzungen: List<String>,
)

/** Gemeinsamer Definitionskatalog; Auswerter dürfen darauf aufbauen, aber nicht abweichend definieren. */
object FolgenAnalyseKatalog {
    fun definition(
        operator: FolgenAnalyseOperator,
        richtung: FolgenAnalyseRichtung,
        startIndex: Long,
        folgenName: String = "f",
        kandidatenName: String = "L",
    ): FolgenAnalyseDefinition {
        val ast = astName(richtung, folgenName, startIndex)
        val unendlich = "H\\in{}^*\\mathbb N\\setminus\\mathbb N"
        val nichtstandard = when (operator) {
            FolgenAnalyseOperator.NACH_OBEN_BESCHRAENKT ->
                "\\exists M\\in K_{\\mathrm{standard}}\\;\\forall $unendlich:\\;{}^*${ast}_H\\le M"
            FolgenAnalyseOperator.NACH_UNTEN_BESCHRAENKT ->
                "\\exists m\\in K_{\\mathrm{standard}}\\;\\forall $unendlich:\\;m\\le{}^*${ast}_H"
            FolgenAnalyseOperator.BESCHRAENKT ->
                "\\exists M\\in\\mathbb R_{\\mathrm{standard},\\ge0}\\;\\forall $unendlich:\\;\\lVert{}^*${ast}_H\\rVert\\le M"
            FolgenAnalyseOperator.MONOTON_STEIGEND -> monotonie(ast, streng = false, steigend = true)
            FolgenAnalyseOperator.MONOTON_FALLEND -> monotonie(ast, streng = false, steigend = false)
            FolgenAnalyseOperator.STRENG_MONOTON_STEIGEND -> monotonie(ast, streng = true, steigend = true)
            FolgenAnalyseOperator.STRENG_MONOTON_FALLEND -> monotonie(ast, streng = true, steigend = false)
            FolgenAnalyseOperator.SCHLIESSLICH_KONSTANT ->
                "\\forall H,J\\in{}^*\\mathbb N\\setminus\\mathbb N:\\;{}^*${ast}_H={}^*${ast}_J"
            FolgenAnalyseOperator.SCHLIESSLICH_POSITIV ->
                "\\forall $unendlich:\\;{}^*${ast}_H>0"
            FolgenAnalyseOperator.SCHLIESSLICH_NEGATIV ->
                "\\forall $unendlich:\\;{}^*${ast}_H<0"
            FolgenAnalyseOperator.KONVERGENT,
            FolgenAnalyseOperator.LIMES,
            -> "\\forall $unendlich:\\;{}^*${ast}_H\\approx $kandidatenName"
            FolgenAnalyseOperator.NULL_LIMES ->
                "\\forall $unendlich:\\;{}^*${ast}_H\\approx0"
            FolgenAnalyseOperator.CAUCHY ->
                "\\forall H,J\\in{}^*\\mathbb N\\setminus\\mathbb N:\\;{}^*${ast}_H\\approx{}^*${ast}_J"
            FolgenAnalyseOperator.HAEUFUNGSWERT ->
                "\\exists H\\in{}^*\\mathbb N\\setminus\\mathbb N:\\;{}^*${ast}_H\\approx $kandidatenName"
            FolgenAnalyseOperator.LIMSUP -> "\\limsup_{n\\to\\infty}${ast}_n"
            FolgenAnalyseOperator.LIMINF -> "\\liminf_{n\\to\\infty}${ast}_n"
            FolgenAnalyseOperator.HAEUFUNGSWERTE ->
                "\\{L\\mid\\exists H\\in{}^*\\mathbb N\\setminus\\mathbb N:\\;{}^*${ast}_H\\approx L\\}"
            FolgenAnalyseOperator.DIVERGENZART -> "\\operatorname{DivArt}($ast)"
            FolgenAnalyseOperator.TEILFOLGE -> "(${ast}_{k_n})_{n\\in\\mathbb N}"
        }
        val klassisch = klassischeDefinition(operator, ast, kandidatenName)
        val voraussetzungen = buildList {
            if (operator in geordneteOperatoren) add("Der Zielbereich benötigt eine Ordnung.")
            if (operator in metrischeOperatoren) add("Der Zielbereich benötigt eine Metrik oder Norm.")
            if (richtung == FolgenAnalyseRichtung.BEIDSEITIG) {
                add("Positive und negative gerichtete Ansicht werden getrennt geprüft und ausdrücklich kombiniert.")
                if (operator in setOf(FolgenAnalyseOperator.KONVERGENT, FolgenAnalyseOperator.LIMES)) {
                    add("Beide Richtungen müssen denselben endlichen Limes besitzen.")
                }
            }
        }
        return FolgenAnalyseDefinition(operator, richtung, startIndex, nichtstandard, klassisch, voraussetzungen)
    }

    private val geordneteOperatoren = setOf(
        FolgenAnalyseOperator.NACH_OBEN_BESCHRAENKT,
        FolgenAnalyseOperator.NACH_UNTEN_BESCHRAENKT,
        FolgenAnalyseOperator.MONOTON_STEIGEND,
        FolgenAnalyseOperator.MONOTON_FALLEND,
        FolgenAnalyseOperator.STRENG_MONOTON_STEIGEND,
        FolgenAnalyseOperator.STRENG_MONOTON_FALLEND,
        FolgenAnalyseOperator.SCHLIESSLICH_POSITIV,
        FolgenAnalyseOperator.SCHLIESSLICH_NEGATIV,
        FolgenAnalyseOperator.LIMSUP,
        FolgenAnalyseOperator.LIMINF,
    )

    private val metrischeOperatoren = setOf(
        FolgenAnalyseOperator.BESCHRAENKT,
        FolgenAnalyseOperator.KONVERGENT,
        FolgenAnalyseOperator.LIMES,
        FolgenAnalyseOperator.NULL_LIMES,
        FolgenAnalyseOperator.CAUCHY,
        FolgenAnalyseOperator.HAEUFUNGSWERT,
        FolgenAnalyseOperator.HAEUFUNGSWERTE,
    )

    private fun astName(richtung: FolgenAnalyseRichtung, name: String, start: Long): String = when (richtung) {
        FolgenAnalyseRichtung.POSITIV -> "A_{$start,+1}($name)"
        FolgenAnalyseRichtung.NEGATIV -> "A_{$start,-1}($name)"
        FolgenAnalyseRichtung.BEIDSEITIG -> "A_{$start,\\pm1}($name)"
    }

    private fun monotonie(ast: String, streng: Boolean, steigend: Boolean): String {
        val relation = when {
            steigend && streng -> "<"
            steigend -> "\\le"
            streng -> ">"
            else -> "\\ge"
        }
        return "\\forall H<J\\in{}^*\\mathbb N:\\;{}^*${ast}_H$relation{}^*${ast}_J"
    }

    private fun klassischeDefinition(
        operator: FolgenAnalyseOperator,
        ast: String,
        kandidat: String,
    ): String = when (operator) {
        FolgenAnalyseOperator.KONVERGENT,
        FolgenAnalyseOperator.LIMES,
        -> "\\forall\\varepsilon>0\\;\\exists N\\;\\forall n\\ge N:\\;d(${ast}_n,$kandidat)<\\varepsilon"
        FolgenAnalyseOperator.CAUCHY ->
            "\\forall\\varepsilon>0\\;\\exists N\\;\\forall m,n\\ge N:\\;d(${ast}_m,${ast}_n)<\\varepsilon"
        FolgenAnalyseOperator.NULL_LIMES ->
            "\\forall\\varepsilon>0\\;\\exists N\\;\\forall n\\ge N:\\;\\lVert${ast}_n\\rVert<\\varepsilon"
        else -> "\\operatorname{klassisch}_{${operator.stabileId}}($ast)"
    }
}

data class BeidseitigesFolgenErgebnis<T>(
    val positiv: T,
    val negativ: T,
    val kombiniert: FolgenAnalyseStatus,
)

object BeidseitigeFolgenAnalyse {
    fun kombiniereAussagen(
        positiv: FolgenAnalyseStatus,
        negativ: FolgenAnalyseStatus,
    ): FolgenAnalyseStatus = when {
        positiv is FolgenAnalyseStatus.Widerlegt || negativ is FolgenAnalyseStatus.Widerlegt ->
            FolgenAnalyseStatus.Widerlegt
        positiv is FolgenAnalyseStatus.Nachgewiesen && negativ is FolgenAnalyseStatus.Nachgewiesen ->
            FolgenAnalyseStatus.Nachgewiesen
        positiv is FolgenAnalyseStatus.NichtAnwendbar -> positiv
        negativ is FolgenAnalyseStatus.NichtAnwendbar -> negativ
        else -> FolgenAnalyseStatus.Bedingt(
            listOf("Positive und negative Richtung müssen jeweils nachgewiesen werden."),
        )
    }

    fun kombiniereLimes(
        positiv: MathematischesObjekt?,
        negativ: MathematischesObjekt?,
    ): FolgenAnalyseStatus = when {
        positiv == null || negativ == null -> FolgenAnalyseStatus.Unentscheidbar("Mindestens ein gerichteter Limes fehlt.")
        positiv == negativ -> FolgenAnalyseStatus.Nachgewiesen
        else -> FolgenAnalyseStatus.Widerlegt
    }
}

object FolgenAnalyseKnotenVertrag {
    const val KNOTEN_ART = "mathematik.folgenanalyse"
    const val EINBETTUNGS_KNOTEN_ART = "mathematik.unnatuerlichesTupelZuFolge"
}

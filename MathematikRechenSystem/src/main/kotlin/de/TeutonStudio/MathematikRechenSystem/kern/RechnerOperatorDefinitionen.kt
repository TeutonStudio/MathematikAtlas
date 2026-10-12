package de.TeutonStudio.MathematikRechenSystem.kern

/** Mathematische Metadaten: keine Karten, Anschluss-IDs oder Konfiguratoraufrufe. */
enum class OperatorFamilie { MATRIX, VEKTOR, ZAHL, TENSOR }
enum class OperatorUnterstützung { KONKRET, SYMBOLISCH, REGISTRIERT }
data class OperatorParameter(val id: String, val erforderlich: Boolean = false, val standardwert: String? = null)
data class OperatorRolle(
    val name: String,
    val typ: FormelTyp,
    val aliases: Set<String> = emptySet(),
    val zulässigeTypen: Set<FormelTyp> = emptySet(),
    val methodenfähig: Boolean = false,
    val variadisch: Boolean = false,
)
data class OperatorSignatur(val id: String, val eingänge: List<OperatorRolle>, val ausgänge: List<OperatorRolle>)
data class RechnerOperatorDefinition(
    val familie: OperatorFamilie,
    val id: String,
    val titel: String,
    val symbolLatex: String,
    val definitionsLatex: String,
    val signaturen: List<OperatorSignatur>,
    val formelArgumente: List<StrukturRechnerEingang>,
    val formelErgebnisTyp: FormelTyp,
    val formelFamilie: OperatorFamilie? = null,
    val aliases: Set<String> = emptySet(),
    val parameter: List<OperatorParameter> = emptyList(),
    val unterstützung: OperatorUnterstützung = OperatorUnterstützung.KONKRET,
    val kategorie: String = familie.name.lowercase().replaceFirstChar(Char::uppercase),
) {
    /** Assoziativität ist unabhängig von Kommutativität und Anschlussidentität. */
    val assoziativ: Boolean get() = istAssoziativerRechnerOperator(id)

    init {
        require(signaturen.isNotEmpty())
        signaturen.forEach { signatur ->
            require(signatur.ausgänge.isNotEmpty())
            require(signatur.eingänge.map { it.name }.distinct().size == signatur.eingänge.size)
            require(signatur.ausgänge.map { it.name }.distinct().size == signatur.ausgänge.size)
        }
    }
}

object RechnerOperatorDefinitionen {
    /** Mengenquellen teilen den Signaturvertrag, aber keinen numerischen Dispatcher. */
    val mengenQuellen: List<MengenKonstante> = MengenKonstante.entries

    private fun rolle(name: String, typ: FormelTyp, variadisch: Boolean = false, methodenfähig: Boolean = false, aliases: Set<String> = emptySet()) =
        OperatorRolle(name, typ, aliases, methodenfähig = methodenfähig, variadisch = variadisch)
    private fun ausgang(typ: FormelTyp, name: String = "wert") = rolle(name, typ)
    private fun signatur(eingänge: List<OperatorRolle>, typ: FormelTyp = FormelTyp.ZAHL, name: String = "wert", id: String = "standard") =
        OperatorSignatur(id, eingänge, listOf(ausgang(typ, name)))

    private val matrix = StrukturRechnerOperatoren.fuer(StrukturRechnerKnotenFamilie.MATRIX).map { d ->
        val zerlegen = d.id == MatrixRechnerOperator.ZERLEGEN.stabileId
        RechnerOperatorDefinition(OperatorFamilie.MATRIX, d.id, d.titel, d.symbolLatex, d.definitionsLatex,
            listOf(signatur(d.eingänge.map { rolle(it.name, it.typ, variadisch = istAssoziativerRechnerOperator(d.id), methodenfähig = true,
                aliases = if (d.id == MatrixRechnerOperator.MATRIXPRODUKT.stabileId) when (it.name) { "links" -> setOf("a"); "rechts" -> setOf("b"); else -> emptySet() } else emptySet())
                .let { r -> if (d.id == MatrixRechnerOperator.MATRIXPRODUKT.stabileId) r.copy(zulässigeTypen = setOf(FormelTyp.MATRIX, FormelTyp.VEKTOR)) else r } },
                if (zerlegen) FormelTyp.METHODE else d.ergebnisTyp, if (zerlegen) "methode" else "wert")),
            d.eingänge, d.ergebnisTyp, formelFamilie = OperatorFamilie.MATRIX)
    }

    private val vektor = VektorRechnerOperator.entries.map { op ->
        fun v(name: String, variadisch: Boolean = false, aliases: Set<String> = emptySet()) =
            rolle(name, FormelTyp.VEKTOR, variadisch, methodenfähig = true, aliases = aliases + when (name) {
                "vektor", "struktur", "vektor.1", "links" -> setOf("vektor", "struktur", "vektor.1", "links", "a") - name
                "vektor.2", "rechts" -> setOf("vektor.2", "rechts", "b") - name
                else -> emptySet()
            }).copy(zulässigeTypen = setOf(FormelTyp.VEKTOR, FormelTyp.TUPEL))
        val eingänge = when (op) {
            VektorRechnerOperator.ADDITION -> listOf(v("vektor.1", true, aliases = setOf("links", "a")), v("vektor.2", true, setOf("rechts", "b")))
            VektorRechnerOperator.HADAMARD_PRODUKT -> listOf(v("links", true), v("rechts", true))
            VektorRechnerOperator.SUBTRAKTION, VektorRechnerOperator.KREUZPRODUKT,
            VektorRechnerOperator.PROJEKTION, VektorRechnerOperator.SKALARPRODUKT, VektorRechnerOperator.WINKEL, VektorRechnerOperator.DISTANZ ->
                listOf(v("links", aliases = setOf("a", "vektor.1")), v("rechts", aliases = setOf("b", "vektor.2")))
            VektorRechnerOperator.SKALARMULTIPLIKATION -> listOf(v("vektor"), rolle("skalar", FormelTyp.ZAHL, methodenfähig = true))
            VektorRechnerOperator.VEKTORFELD_INTEGRIEREN -> listOf(rolle("vektorfeld", FormelTyp.METHODE), rolle("menge", FormelTyp.MENGE))
            VektorRechnerOperator.ZERLEGEN -> listOf(v("struktur"))
            VektorRechnerOperator.ZUSAMMENFUEHREN -> listOf("element.1", "element.2").mapIndexed { i, n ->
                rolle(n, FormelTyp.OBJEKT, variadisch = true).copy(zulässigeTypen = setOf(FormelTyp.VEKTOR, FormelTyp.TUPEL, FormelTyp.ZAHL, FormelTyp.OBJEKT)) }
            else -> listOf(v("vektor"))
        }
        val (typ, name) = when (op) {
            VektorRechnerOperator.SKALARPRODUKT -> FormelTyp.ZAHL to "skalar"
            VektorRechnerOperator.WINKEL, VektorRechnerOperator.WINKEL_ZU_ACHSE -> FormelTyp.ZAHL to "winkel"
            VektorRechnerOperator.DISTANZ -> FormelTyp.ZAHL to "distanz"
            VektorRechnerOperator.NORM -> FormelTyp.ZAHL to "norm"
            VektorRechnerOperator.ZERLEGEN -> FormelTyp.METHODE to "methode"
            VektorRechnerOperator.ZUSAMMENFUEHREN -> FormelTyp.OBJEKT to "struktur"
            else -> FormelTyp.VEKTOR to "vektor"
        }
        val struktur = StrukturRechnerOperatoren.fuer(StrukturRechnerKnotenFamilie.VEKTOR).firstOrNull { it.id == op.stabileId }
        RechnerOperatorDefinition(OperatorFamilie.VEKTOR, op.stabileId, op.titel, struktur?.symbolLatex ?: op.titel,
            struktur?.definitionsLatex ?: "\\operatorname{${op.titel}}", listOf(signatur(eingänge, typ, name)),
            struktur?.eingänge ?: eingänge.map { StrukturRechnerEingang(it.name, it.typ) }, struktur?.ergebnisTyp ?: typ,
            formelFamilie = if (struktur != null) OperatorFamilie.VEKTOR else null,
            aliases = setOf(op.name) + when (op) {
                VektorRechnerOperator.SKALARPRODUKT -> setOf("dot product", "inneres produkt")
                VektorRechnerOperator.KREUZPRODUKT -> setOf("cross product")
                VektorRechnerOperator.DISTANZ -> setOf("abstand", "metrik")
                VektorRechnerOperator.VEKTORFELD_INTEGRIEREN -> setOf("vektorfeld integral", "feld integrieren")
                else -> emptySet()
            }, parameter = listOf(OperatorParameter("metrik", standardwert = "metrik.l2"), OperatorParameter("achse", standardwert = "1")))
    }

    private val zahlen = UniversellerZahlenOperator.entries.map { op ->
        val variadisch = op in setOf(UniversellerZahlenOperator.ADDITION, UniversellerZahlenOperator.MULTIPLIKATION, UniversellerZahlenOperator.MINIMUM, UniversellerZahlenOperator.MAXIMUM)
        val komplex = op in setOf(UniversellerZahlenOperator.KOMPLEX_AUS_POLAR, UniversellerZahlenOperator.KOMPLEX_AUS_KARTESISCH)
        val binär = op in setOf(UniversellerZahlenOperator.SUBTRAKTION, UniversellerZahlenOperator.POTENZ, UniversellerZahlenOperator.WURZEL, UniversellerZahlenOperator.LOGARITHMUS, UniversellerZahlenOperator.MODULO)
        val methodisch = op in setOf(UniversellerZahlenOperator.ITERIERTE_SUMME, UniversellerZahlenOperator.ITERIERTES_PRODUKT)
        fun zahl(name: String) = rolle(name, FormelTyp.ZAHL, variadisch, methodenfähig = op.hebungsArt == ZahlenOperatorHebungsArt.PUNKTWEISE)
        val rollen = when {
            methodisch -> listOf(rolle("methode", FormelTyp.METHODE), rolle("indexmenge", FormelTyp.MENGE))
            op == UniversellerZahlenOperator.DIVISION -> listOf(zahl("a"), zahl("b"), rolle("c", FormelTyp.ZAHL))
            variadisch || komplex || binär -> listOf(zahl("a"), zahl("b"))
            else -> listOf(zahl("a"))
        }
        val signaturen = listOf(signatur(rollen)) + if (op == UniversellerZahlenOperator.DIVISION) listOf(signatur(rollen.take(2), id = "ohneNullersatz")) else if (komplex) listOf(signatur(listOf(rolle("tupel", FormelTyp.TUPEL)), id = "tupel")) else emptyList()
        RechnerOperatorDefinition(OperatorFamilie.ZAHL, op.stabileId, op.titel, op.symbolLatex, op.symbolLatex,
            signaturen, op.formelRollen().map { StrukturRechnerEingang(it, if (it == "methode") FormelTyp.METHODE else if (it == "indexmenge") FormelTyp.MENGE else FormelTyp.ZAHL) },
            FormelTyp.ZAHL, aliases = setOf(op.name) + when (op) {
                UniversellerZahlenOperator.SINUS -> setOf("sin")
                UniversellerZahlenOperator.COSINUS -> setOf("cos")
                UniversellerZahlenOperator.INTEGRAL -> setOf("integrieren", "integration")
                UniversellerZahlenOperator.DIFFERENTIAL -> setOf("ableiten", "differenzieren")
                UniversellerZahlenOperator.QUADRATWURZEL -> setOf("sqrt", "wurzel")
                else -> emptySet()
            }, parameter = listOf(OperatorParameter("festeEingänge", standardwert = "2")))
    } + ZusätzlicherZahlenOperator.entries.map { op ->
        val eingänge = when (op) {
            ZusätzlicherZahlenOperator.POLYNOM -> listOf(rolle("koeffizienten", FormelTyp.OBJEKT).copy(zulässigeTypen = setOf(FormelTyp.TUPEL, FormelTyp.VEKTOR)), rolle("argument", FormelTyp.ZAHL, methodenfähig = true))
            ZusätzlicherZahlenOperator.STAMMFUNKTION -> listOf(rolle("methode", FormelTyp.METHODE), rolle("startwert", FormelTyp.ZAHL))
            ZusätzlicherZahlenOperator.ARITHMETISCHES_MITTEL, ZusätzlicherZahlenOperator.GEOMETRISCHES_MITTEL ->
                listOf(rolle("a", FormelTyp.ZAHL, true, true), rolle("b", FormelTyp.ZAHL, true, true))
            else -> listOf(rolle("a", FormelTyp.ZAHL, methodenfähig = true))
        }
        val methode = op == ZusätzlicherZahlenOperator.STAMMFUNKTION
        val typ = if (methode) FormelTyp.METHODE else FormelTyp.ZAHL
        RechnerOperatorDefinition(OperatorFamilie.ZAHL, op.stabileId, op.titel, op.symbolLatex, op.symbolLatex,
            listOf(signatur(eingänge, typ, if (op == ZusätzlicherZahlenOperator.STAMMFUNKTION) "stammfunktion" else "wert")),
            eingänge.map { StrukturRechnerEingang(it.name, it.typ) }, typ, aliases = setOf(op.name))
    }

    private val tensor = StandardTensorOperationen.registry.alle().map { d ->
        val eingänge = d.eingangsRollen.map { r -> rolle(r.wert, if (r.wert == "skalar") FormelTyp.ZAHL else FormelTyp.OBJEKT, variadisch = istAssoziativerRechnerOperator(d.id.wert),
            aliases = when {
                r.wert == "skalar" && d.id.wert == TensorRechnerOperator.SKALARMULTIPLIKATION.stabileId -> setOf("links")
                r.wert == "tensor" && d.id.wert == TensorRechnerOperator.SKALARMULTIPLIKATION.stabileId -> setOf("rechts")
                r.wert == "tensor" -> setOf("links")
                else -> emptySet()
            }) }
        val achsen = d.minimaleAchsenAnzahl > 0 && d.id.wert != TensorRechnerOperator.INDEXAUSWERTUNG.stabileId
        val ausgänge = d.ausgangsRollen.map { r -> rolle(r.wert, when (r.wert) { "wert", "rang" -> FormelTyp.ZAHL; "methode" -> FormelTyp.METHODE; else -> FormelTyp.OBJEKT }, aliases = if (r.wert == "ergebnis") setOf("wert") else emptySet()) }
        val signaturen = listOf(OperatorSignatur("standard", eingänge + if (achsen) listOf(rolle("achsen", FormelTyp.TUPEL)) else emptyList(), ausgänge)) +
            if (achsen) listOf(OperatorSignatur("einzelachsen", eingänge + List(d.minimaleAchsenAnzahl.coerceAtLeast(1)) { i -> rolle("achse.${i + 1}", FormelTyp.ZAHL, d.maximaleAchsenAnzahl == null) }, ausgänge)) else emptyList()
        val struktur = StrukturRechnerOperatoren.fuer(StrukturRechnerKnotenFamilie.TENSOR).firstOrNull { it.id == d.id.wert }
        RechnerOperatorDefinition(OperatorFamilie.TENSOR, d.id.wert, d.titel, struktur?.symbolLatex ?: "\\operatorname{${d.titel}}",
            struktur?.definitionsLatex ?: "\\operatorname{${d.titel}}", signaturen,
            struktur?.eingänge ?: eingänge.map { StrukturRechnerEingang(it.name, it.typ) }, struktur?.ergebnisTyp ?: ausgänge.first().typ,
            formelFamilie = if (struktur != null && TensorRechnerOperator.entries.any { it.stabileId == d.id.wert }) OperatorFamilie.TENSOR else null,
            aliases = when (d.id.wert) {
                TensorRechnerOperator.TENSORPRODUKT.stabileId -> setOf("tensor produkt", "⊗")
                TensorRechnerOperator.KONTRAKTION.stabileId -> setOf("kontrahieren")
                else -> emptySet()
            },
            parameter = d.parameter.map { OperatorParameter(it.id, it.erforderlich, it.standardWert) },
            unterstützung = when (d.unterstuetzungsStatus) {
                TensorUnterstuetzungsStatus.KONKRET_IMPLEMENTIERT -> OperatorUnterstützung.KONKRET
                TensorUnterstuetzungsStatus.SYMBOLISCH_IMPLEMENTIERT -> OperatorUnterstützung.SYMBOLISCH
                TensorUnterstuetzungsStatus.REGISTRIERT -> OperatorUnterstützung.REGISTRIERT
            })
    }

    val alle: List<RechnerOperatorDefinition> = matrix + vektor + zahlen + tensor + OperatorFamilie.entries.map { familie ->
        val id = "${when (familie) { OperatorFamilie.ZAHL -> "zahl"; OperatorFamilie.VEKTOR -> "vektor"; OperatorFamilie.MATRIX -> "matrix"; OperatorFamilie.TENSOR -> "tensor" }}.formel"
        RechnerOperatorDefinition(familie, id, "Eigene Formel", "f(\\ldots)", "f(\\ldots)",
            listOf(signatur(emptyList(), FormelTyp.OBJEKT, id = "formel")), emptyList(), FormelTyp.OBJEKT,
            aliases = setOf("CAS", "Formelbauer"), unterstützung = OperatorUnterstützung.SYMBOLISCH)
    }
    private val nachId = alle.associateBy { it.familie to it.id }.also { require(it.size == alle.size) }
    fun finde(familie: OperatorFamilie, id: String?) = nachId[familie to id]
    fun fürFamilie(familie: OperatorFamilie) = alle.filter { it.familie == familie }
}
fun UniversellerZahlenOperator.formelRollen(): List<String> = when (this) {
        UniversellerZahlenOperator.ADDITION,
        UniversellerZahlenOperator.SUBTRAKTION,
        UniversellerZahlenOperator.MULTIPLIKATION,
        UniversellerZahlenOperator.MINIMUM,
        UniversellerZahlenOperator.MAXIMUM,
        -> listOf("a", "b")

        UniversellerZahlenOperator.DIVISION -> listOf("zaehler", "nenner")
        UniversellerZahlenOperator.POTENZ -> listOf("basis", "exponent")
        UniversellerZahlenOperator.WURZEL -> listOf("radikand")
        UniversellerZahlenOperator.LOGARITHMUS -> listOf("basis", "argument")
        UniversellerZahlenOperator.ITERIERTE_SUMME,
        UniversellerZahlenOperator.ITERIERTES_PRODUKT,
        -> listOf("methode", "indexmenge")

        UniversellerZahlenOperator.KOMPLEX_AUS_POLAR -> listOf("radius", "winkel")
        UniversellerZahlenOperator.KOMPLEX_AUS_KARTESISCH -> listOf("realteil", "imaginaerteil")
        UniversellerZahlenOperator.MODULO -> listOf("dividend", "modul")
        else -> listOf("argument")
    }

/** Nur geschlossene, assoziative Operandenrollen; keine Skalarmultiplikation oder Achsenlisten. */
fun istAssoziativerRechnerOperator(id: String): Boolean = id in setOf(
    "zahl.addition", "zahl.multiplikation", "zahl.minimum", "zahl.maximum",
    "matrix.addition", "matrix.produkt", "matrix.hadamard",
    "vektor.addition", "vektor.hadamard",
    "tensor.addition", "tensor.hadamard", "tensor.tensorprodukt",
    "aussage.konjunktion", "aussage.disjunktion", "aussage.adjunktion", "aussage.aequivalenz",
    "menge.vereinigung", "menge.schnitt", "menge.symmetrischeDifferenz",
)

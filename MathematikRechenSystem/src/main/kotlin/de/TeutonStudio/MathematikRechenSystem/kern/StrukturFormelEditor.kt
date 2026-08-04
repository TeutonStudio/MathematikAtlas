package de.TeutonStudio.MathematikRechenSystem.kern

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.Base64

/** Rechnerfamilien, die denselben typisierten CAS-Ausdrucksbaum verwenden. */
enum class StrukturFormelFamilie(
    val wurzelErwartung: FormelTyp,
    val erlaubteVariablenTypen: List<FormelTyp>,
) {
    AUSSAGESATZ(FormelTyp.AUSSAGE, listOf(FormelTyp.AUSSAGE, FormelTyp.MENGE)),
    VEKTOR(FormelTyp.OBJEKT, listOf(FormelTyp.VEKTOR, FormelTyp.ZAHL)),
    MATRIX(FormelTyp.OBJEKT, listOf(FormelTyp.MATRIX, FormelTyp.VEKTOR, FormelTyp.ZAHL)),
    TENSOR(FormelTyp.OBJEKT, listOf(FormelTyp.TENSOR, FormelTyp.ZAHL)),
}

enum class StrukturFormelKategorie {
    LOGIK,
    VEKTOR,
    MATRIX,
    TENSOR,
    SKALAR,
}

data class StrukturFormelArgumentTyp(
    val rolle: String,
    val typ: FormelTyp,
)

data class StrukturFormelTaste(
    val id: String,
    val beschriftung: String,
    val kategorie: StrukturFormelKategorie,
    val operatorId: String,
    val ergebnisTyp: FormelTyp,
    val argumente: List<StrukturFormelArgumentTyp>,
) {
    init {
        require(id.isNotBlank())
        require(operatorId.isNotBlank())
        require(argumente.isNotEmpty())
    }
}

/** UI-neutrale, typisierte Tastaturen für die vier Strukturrechner. */
object StrukturFormelTastatur {
    private fun argument(rolle: String, typ: FormelTyp) = StrukturFormelArgumentTyp(rolle, typ)

    private fun taste(
        id: String,
        beschriftung: String,
        kategorie: StrukturFormelKategorie,
        operatorId: String,
        ergebnisTyp: FormelTyp,
        vararg argumente: StrukturFormelArgumentTyp,
    ) = StrukturFormelTaste(id, beschriftung, kategorie, operatorId, ergebnisTyp, argumente.toList())

    private val skalar = listOf(
        taste("zahl-plus", "+", StrukturFormelKategorie.SKALAR, "zahl.addition", FormelTyp.ZAHL,
            argument("a", FormelTyp.ZAHL), argument("b", FormelTyp.ZAHL)),
        taste("zahl-minus", "−", StrukturFormelKategorie.SKALAR, "zahl.subtraktion", FormelTyp.ZAHL,
            argument("a", FormelTyp.ZAHL), argument("b", FormelTyp.ZAHL)),
        taste("zahl-mal", "×", StrukturFormelKategorie.SKALAR, "zahl.multiplikation", FormelTyp.ZAHL,
            argument("a", FormelTyp.ZAHL), argument("b", FormelTyp.ZAHL)),
        taste("zahl-geteilt", "÷", StrukturFormelKategorie.SKALAR, "zahl.division", FormelTyp.ZAHL,
            argument("zaehler", FormelTyp.ZAHL), argument("nenner", FormelTyp.ZAHL)),
        taste("zahl-potenz", "xʸ", StrukturFormelKategorie.SKALAR, "zahl.potenz", FormelTyp.ZAHL,
            argument("basis", FormelTyp.ZAHL), argument("exponent", FormelTyp.ZAHL)),
        taste("zahl-wurzel", "√", StrukturFormelKategorie.SKALAR, "zahl.wurzel", FormelTyp.ZAHL,
            argument("radikand", FormelTyp.ZAHL)),
    )

    private val aussage = listOf(
        taste("aussage-negation", "¬", StrukturFormelKategorie.LOGIK, "aussage.negation", FormelTyp.AUSSAGE,
            argument("aussage", FormelTyp.AUSSAGE)),
        taste("aussage-konjunktion", "∧", StrukturFormelKategorie.LOGIK, "aussage.konjunktion", FormelTyp.AUSSAGE,
            argument("a", FormelTyp.AUSSAGE), argument("b", FormelTyp.AUSSAGE)),
        taste("aussage-disjunktion", "∨", StrukturFormelKategorie.LOGIK, "aussage.disjunktion", FormelTyp.AUSSAGE,
            argument("a", FormelTyp.AUSSAGE), argument("b", FormelTyp.AUSSAGE)),
        taste("aussage-adjunktion", "⊻", StrukturFormelKategorie.LOGIK, "aussage.adjunktion", FormelTyp.AUSSAGE,
            argument("a", FormelTyp.AUSSAGE), argument("b", FormelTyp.AUSSAGE)),
        taste("aussage-implikation", "⇒", StrukturFormelKategorie.LOGIK, "aussage.implikation", FormelTyp.AUSSAGE,
            argument("voraussetzung", FormelTyp.AUSSAGE), argument("folgerung", FormelTyp.AUSSAGE)),
        taste("aussage-aequivalenz", "⇔", StrukturFormelKategorie.LOGIK, "aussage.aequivalenz", FormelTyp.AUSSAGE,
            argument("links", FormelTyp.AUSSAGE), argument("rechts", FormelTyp.AUSSAGE)),
    )

    private val vektor = listOf(
        taste("vektor-plus", "+", StrukturFormelKategorie.VEKTOR, "vektor.addition", FormelTyp.VEKTOR,
            argument("a", FormelTyp.VEKTOR), argument("b", FormelTyp.VEKTOR)),
        taste("vektor-minus", "−", StrukturFormelKategorie.VEKTOR, "vektor.subtraktion", FormelTyp.VEKTOR,
            argument("links", FormelTyp.VEKTOR), argument("rechts", FormelTyp.VEKTOR)),
        taste("vektor-skalar", "s·v", StrukturFormelKategorie.VEKTOR, "vektor.skalarmultiplikation", FormelTyp.VEKTOR,
            argument("skalar", FormelTyp.ZAHL), argument("vektor", FormelTyp.VEKTOR)),
        taste("vektor-negation", "−v", StrukturFormelKategorie.VEKTOR, "vektor.negation", FormelTyp.VEKTOR,
            argument("vektor", FormelTyp.VEKTOR)),
        taste("vektor-skalarprodukt", "⟨u,v⟩", StrukturFormelKategorie.VEKTOR, "vektor.skalarprodukt", FormelTyp.ZAHL,
            argument("links", FormelTyp.VEKTOR), argument("rechts", FormelTyp.VEKTOR)),
        taste("vektor-kreuzprodukt", "u×v", StrukturFormelKategorie.VEKTOR, "vektor.kreuzprodukt", FormelTyp.VEKTOR,
            argument("links", FormelTyp.VEKTOR), argument("rechts", FormelTyp.VEKTOR)),
        taste("vektor-norm", "‖v‖", StrukturFormelKategorie.VEKTOR, "vektor.norm", FormelTyp.ZAHL,
            argument("vektor", FormelTyp.VEKTOR)),
        taste("vektor-normalisierung", "v/‖v‖", StrukturFormelKategorie.VEKTOR, "vektor.normalisierung", FormelTyp.VEKTOR,
            argument("vektor", FormelTyp.VEKTOR)),
        taste("vektor-hadamard", "u∘v", StrukturFormelKategorie.VEKTOR, "vektor.hadamard", FormelTyp.VEKTOR,
            argument("links", FormelTyp.VEKTOR), argument("rechts", FormelTyp.VEKTOR)),
        taste("vektor-projektion", "proj", StrukturFormelKategorie.VEKTOR, "vektor.projektion", FormelTyp.VEKTOR,
            argument("vektor", FormelTyp.VEKTOR), argument("ziel", FormelTyp.VEKTOR)),
        taste("vektor-winkel", "∠", StrukturFormelKategorie.VEKTOR, "vektor.winkel", FormelTyp.ZAHL,
            argument("links", FormelTyp.VEKTOR), argument("rechts", FormelTyp.VEKTOR)),
    )

    private val matrix = listOf(
        taste("matrix-plus", "+", StrukturFormelKategorie.MATRIX, "matrix.addition", FormelTyp.MATRIX,
            argument("a", FormelTyp.MATRIX), argument("b", FormelTyp.MATRIX)),
        taste("matrix-minus", "−", StrukturFormelKategorie.MATRIX, "matrix.subtraktion", FormelTyp.MATRIX,
            argument("links", FormelTyp.MATRIX), argument("rechts", FormelTyp.MATRIX)),
        taste("matrix-skalar", "s·A", StrukturFormelKategorie.MATRIX, "matrix.skalarmultiplikation", FormelTyp.MATRIX,
            argument("skalar", FormelTyp.ZAHL), argument("matrix", FormelTyp.MATRIX)),
        taste("matrix-produkt", "AB", StrukturFormelKategorie.MATRIX, "matrix.produkt", FormelTyp.MATRIX,
            argument("links", FormelTyp.MATRIX), argument("rechts", FormelTyp.MATRIX)),
        taste("matrix-hadamard", "A∘B", StrukturFormelKategorie.MATRIX, "matrix.hadamard", FormelTyp.MATRIX,
            argument("links", FormelTyp.MATRIX), argument("rechts", FormelTyp.MATRIX)),
        taste("matrix-vektor", "Av", StrukturFormelKategorie.MATRIX, "matrix.vektorProdukt", FormelTyp.VEKTOR,
            argument("matrix", FormelTyp.MATRIX), argument("vektor", FormelTyp.VEKTOR)),
        taste("matrix-transponieren", "Aᵀ", StrukturFormelKategorie.MATRIX, "matrix.transponieren", FormelTyp.MATRIX,
            argument("matrix", FormelTyp.MATRIX)),
        taste("matrix-inverse", "A⁻¹", StrukturFormelKategorie.MATRIX, "matrix.inverse", FormelTyp.MATRIX,
            argument("matrix", FormelTyp.MATRIX)),
        taste("matrix-potenz", "Aⁿ", StrukturFormelKategorie.MATRIX, "matrix.potenz", FormelTyp.MATRIX,
            argument("matrix", FormelTyp.MATRIX), argument("exponent", FormelTyp.ZAHL)),
        taste("matrix-determinante", "det", StrukturFormelKategorie.MATRIX, "matrix.determinante", FormelTyp.ZAHL,
            argument("matrix", FormelTyp.MATRIX)),
        taste("matrix-spur", "tr", StrukturFormelKategorie.MATRIX, "matrix.spur", FormelTyp.ZAHL,
            argument("matrix", FormelTyp.MATRIX)),
        taste("matrix-rang", "rang", StrukturFormelKategorie.MATRIX, "matrix.rang", FormelTyp.ZAHL,
            argument("matrix", FormelTyp.MATRIX)),
        taste("matrix-hauptdiagonale", "diag", StrukturFormelKategorie.MATRIX, "matrix.hauptdiagonale", FormelTyp.TUPEL,
            argument("matrix", FormelTyp.MATRIX)),
        taste("matrix-nebendiagonale", "anti-diag", StrukturFormelKategorie.MATRIX, "matrix.nebendiagonale", FormelTyp.TUPEL,
            argument("matrix", FormelTyp.MATRIX)),
    )

    private val tensor = listOf(
        taste("tensor-plus", "+", StrukturFormelKategorie.TENSOR, "tensor.addition", FormelTyp.TENSOR,
            argument("a", FormelTyp.TENSOR), argument("b", FormelTyp.TENSOR)),
        taste("tensor-minus", "−", StrukturFormelKategorie.TENSOR, "tensor.subtraktion", FormelTyp.TENSOR,
            argument("links", FormelTyp.TENSOR), argument("rechts", FormelTyp.TENSOR)),
        taste("tensor-skalar", "s·T", StrukturFormelKategorie.TENSOR, "tensor.skalarmultiplikation", FormelTyp.TENSOR,
            argument("skalar", FormelTyp.ZAHL), argument("tensor", FormelTyp.TENSOR)),
        taste("tensor-hadamard", "A∘B", StrukturFormelKategorie.TENSOR, "tensor.hadamard", FormelTyp.TENSOR,
            argument("links", FormelTyp.TENSOR), argument("rechts", FormelTyp.TENSOR)),
        taste("tensor-produkt", "A⊗B", StrukturFormelKategorie.TENSOR, "tensor.tensorprodukt", FormelTyp.TENSOR,
            argument("links", FormelTyp.TENSOR), argument("rechts", FormelTyp.TENSOR)),
        taste("tensor-kontraktion", "kontr", StrukturFormelKategorie.TENSOR, "tensor.kontraktion", FormelTyp.TENSOR,
            argument("tensor", FormelTyp.TENSOR)),
        taste("tensor-permutation", "perm", StrukturFormelKategorie.TENSOR, "tensor.achsenpermutation", FormelTyp.TENSOR,
            argument("tensor", FormelTyp.TENSOR)),
        taste("tensor-transponieren", "Tᵀ", StrukturFormelKategorie.TENSOR, "tensor.transponieren", FormelTyp.TENSOR,
            argument("tensor", FormelTyp.TENSOR)),
        taste("tensor-schnitt", "Achse", StrukturFormelKategorie.TENSOR, "tensor.achsenschnitt", FormelTyp.TENSOR,
            argument("tensor", FormelTyp.TENSOR)),
        taste("tensor-index", "Tᵢ", StrukturFormelKategorie.TENSOR, "tensor.indexauswertung", FormelTyp.ZAHL,
            argument("tensor", FormelTyp.TENSOR)),
        taste("tensor-norm", "‖T‖", StrukturFormelKategorie.TENSOR, "tensor.norm", FormelTyp.ZAHL,
            argument("tensor", FormelTyp.TENSOR)),
    )

    fun fuer(familie: StrukturFormelFamilie): List<StrukturFormelTaste> = when (familie) {
        StrukturFormelFamilie.AUSSAGESATZ -> aussage
        StrukturFormelFamilie.VEKTOR -> vektor + skalar
        StrukturFormelFamilie.MATRIX -> matrix + vektor + skalar
        StrukturFormelFamilie.TENSOR -> tensor + skalar
    }
}

/** Transaktionaler, typisierter Bearbeitungszustand für Strukturformeln. */
class StrukturFormelEditorZustand(
    start: FormelAusdruck = strukturFormelPlatzhalter("wurzel", FormelTyp.OBJEKT, "Ausdruck"),
) {
    private val rueckgaengig = ArrayDeque<FormelAusdruck>()
    private val wiederholen = ArrayDeque<FormelAusdruck>()
    private var idZaehler = 0

    var wurzel: FormelAusdruck = start
        private set
    var auswahlId: String = start.ersterStrukturPlatzhalter()?.id ?: start.id
        private set

    val kannRueckgaengig: Boolean get() = rueckgaengig.isNotEmpty()
    val kannWiederholen: Boolean get() = wiederholen.isNotEmpty()
    val offenePlatzhalter: List<FormelAusdruck.Platzhalter> get() = wurzel.strukturPlatzhalter()
    val auswahlTyp: FormelTyp? get() = wurzel.findeStrukturAusdruck(auswahlId)?.typ

    fun kannDruecken(taste: StrukturFormelTaste): Boolean =
        auswahlTyp?.let { typKompatibel(it, taste.ergebnisTyp) } == true

    fun kannEinsetzen(typ: FormelTyp): Boolean =
        auswahlTyp?.let { typKompatibel(it, typ) } == true

    fun waehle(ausdrucksId: String): Boolean {
        if (wurzel.findeStrukturAusdruck(ausdrucksId) == null) return false
        auswahlId = ausdrucksId
        return true
    }

    fun naechsterPlatzhalter(richtung: Int = 1): String? {
        val ids = offenePlatzhalter.map { it.id }
        if (ids.isEmpty()) return null
        val aktuell = ids.indexOf(auswahlId)
        val index = if (aktuell < 0) 0 else Math.floorMod(aktuell + richtung, ids.size)
        auswahlId = ids[index]
        return auswahlId
    }

    fun druecke(taste: StrukturFormelTaste): Boolean {
        val ziel = wurzel.findeStrukturAusdruck(auswahlId) ?: return false
        if (!typKompatibel(ziel.typ, taste.ergebnisTyp)) return false
        val neu = FormelAusdruck.Operation(
            id = neueId("operation"),
            operatorId = taste.operatorId,
            argumente = taste.argumente.mapIndexed { index, argument ->
                FormelArgument(
                    argument.rolle,
                    index,
                    strukturFormelPlatzhalter(
                        id = neueId("platzhalter"),
                        rolle = argument.rolle,
                        typ = argument.typ,
                        beschriftung = argument.rolle,
                    ),
                )
            },
            typ = taste.ergebnisTyp,
        )
        return ersetzeAuswahl(neu)
    }

    fun setzeVariable(name: String, typ: FormelTyp): Boolean {
        val bereinigt = name.trim()
        if (bereinigt.isBlank()) return false
        val ziel = wurzel.findeStrukturAusdruck(auswahlId) ?: return false
        if (!typKompatibel(ziel.typ, typ)) return false
        return ersetzeAuswahl(
            FormelAusdruck.Variable(neueId("variable"), bereinigt, bereinigt, typ),
        )
    }

    fun setzeZahl(text: String): Boolean {
        val ziel = wurzel.findeStrukturAusdruck(auswahlId) ?: return false
        if (!typKompatibel(ziel.typ, FormelTyp.ZAHL)) return false
        val import = FormelLatexCodec.importiere(text)
        val ausdruck = (import as? FormelLatexImportErgebnis.Erfolg)?.ausdruck as? FormelAusdruck.Literal
            ?: return false
        return ersetzeAuswahl(ausdruck.copy(id = neueId("zahl"), typ = FormelTyp.ZAHL))
    }

    fun loescheAuswahl(): Boolean {
        val ziel = wurzel.findeStrukturAusdruck(auswahlId) ?: return false
        return ersetzeAuswahl(
            strukturFormelPlatzhalter(
                id = neueId("platzhalter"),
                rolle = "argument",
                typ = ziel.typ,
                beschriftung = "argument",
            ),
        )
    }

    fun rueckgaengig(): Boolean {
        val vorher = rueckgaengig.removeLastOrNull() ?: return false
        wiederholen.add(wurzel)
        wurzel = vorher
        auswahlId = wurzel.ersterStrukturPlatzhalter()?.id ?: wurzel.id
        return true
    }

    fun wiederholen(): Boolean {
        val nachher = wiederholen.removeLastOrNull() ?: return false
        rueckgaengig.add(wurzel)
        wurzel = nachher
        auswahlId = wurzel.ersterStrukturPlatzhalter()?.id ?: wurzel.id
        return true
    }

    private fun ersetzeAuswahl(neu: FormelAusdruck): Boolean {
        val ersetzt = wurzel.ersetzeStrukturAusdruck(auswahlId, neu) ?: return false
        if (ersetzt == wurzel) return false
        rueckgaengig.add(wurzel)
        wiederholen.clear()
        wurzel = ersetzt
        auswahlId = neu.ersterStrukturPlatzhalter()?.id ?: neu.id
        return true
    }

    private fun neueId(art: String): String = "struktur-editor-$art-${++idZaehler}"
}

fun strukturFormelPlatzhalter(
    rolle: String,
    typ: FormelTyp,
    beschriftung: String = rolle,
): FormelAusdruck.Platzhalter = strukturFormelPlatzhalter(
    id = "struktur-platzhalter-${rolle.hashCode().toUInt()}-${System.nanoTime().toUInt()}",
    rolle = rolle,
    typ = typ,
    beschriftung = beschriftung,
)

private fun strukturFormelPlatzhalter(
    id: String,
    rolle: String,
    typ: FormelTyp,
    beschriftung: String,
): FormelAusdruck.Platzhalter = FormelAusdruck.Platzhalter(
    id = id,
    rollenId = rolle,
    beschriftung = beschriftung,
    typ = typ,
)

private fun typKompatibel(erwartet: FormelTyp, tatsaechlich: FormelTyp): Boolean =
    erwartet == FormelTyp.OBJEKT || erwartet == tatsaechlich

private fun FormelAusdruck.findeStrukturAusdruck(id: String): FormelAusdruck? = when {
    this.id == id -> this
    this is FormelAusdruck.Operation -> argumente.firstNotNullOfOrNull { it.ausdruck.findeStrukturAusdruck(id) }
    else -> null
}

private fun FormelAusdruck.ersetzeStrukturAusdruck(id: String, neu: FormelAusdruck): FormelAusdruck? {
    if (this.id == id) return neu
    if (this !is FormelAusdruck.Operation) return null
    var gefunden = false
    val neueArgumente = argumente.map { argument ->
        val ersetzt = argument.ausdruck.ersetzeStrukturAusdruck(id, neu)
        if (ersetzt == null) argument else {
            gefunden = true
            argument.copy(ausdruck = ersetzt)
        }
    }
    return if (gefunden) copy(argumente = neueArgumente) else null
}

private fun FormelAusdruck.strukturPlatzhalter(): List<FormelAusdruck.Platzhalter> = buildList {
    fun besuche(ausdruck: FormelAusdruck) {
        when (ausdruck) {
            is FormelAusdruck.Platzhalter -> add(ausdruck)
            is FormelAusdruck.Operation -> ausdruck.argumente.sortedBy { it.position }.forEach { besuche(it.ausdruck) }
            else -> Unit
        }
    }
    besuche(this@strukturPlatzhalter)
}

private fun FormelAusdruck.ersterStrukturPlatzhalter(): FormelAusdruck.Platzhalter? = strukturPlatzhalter().firstOrNull()

data class StrukturFormelVariable(val name: String, val typ: FormelTyp)

fun strukturFormelVariablen(wurzel: FormelAusdruck): List<StrukturFormelVariable> {
    val variablen = linkedMapOf<String, FormelTyp>()
    fun besuche(ausdruck: FormelAusdruck) {
        when (ausdruck) {
            is FormelAusdruck.Variable -> {
                val vorhanden = variablen[ausdruck.name]
                require(vorhanden == null || vorhanden == ausdruck.typ) {
                    "Die Variable ${ausdruck.name} wird mit mehreren Typen verwendet."
                }
                variablen.putIfAbsent(ausdruck.name, ausdruck.typ)
            }
            is FormelAusdruck.Operation -> ausdruck.argumente.sortedBy { it.position }.forEach { besuche(it.ausdruck) }
            else -> Unit
        }
    }
    besuche(wurzel)
    return variablen.map { (name, typ) -> StrukturFormelVariable(name, typ) }
}

/** Stabile binäre Persistenz des Ausdrucksbaums, Base64-kodiert für Knotenparameter. */
object StrukturFormelCodec {
    private const val VERSION = 1

    fun kodieren(wurzel: FormelAusdruck): String {
        val bytes = ByteArrayOutputStream().use { ausgabe ->
            DataOutputStream(ausgabe).use { daten ->
                daten.writeInt(VERSION)
                schreibe(daten, wurzel)
            }
            ausgabe.toByteArray()
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun dekodieren(kodiert: String): FormelAusdruck {
        val bytes = Base64.getUrlDecoder().decode(kodiert)
        return DataInputStream(ByteArrayInputStream(bytes)).use { daten ->
            require(daten.readInt() == VERSION) { "Nicht unterstützte Strukturformel-Version." }
            lies(daten)
        }
    }

    private fun schreibe(daten: DataOutputStream, ausdruck: FormelAusdruck) {
        when (ausdruck) {
            is FormelAusdruck.Literal -> {
                daten.writeByte(0)
                kopf(daten, ausdruck)
                daten.writeUTF(ausdruck.wert.zuLatex())
            }
            is FormelAusdruck.Variable -> {
                daten.writeByte(1)
                kopf(daten, ausdruck)
                daten.writeUTF(ausdruck.name)
                daten.writeUTF(ausdruck.latex)
            }
            is FormelAusdruck.Platzhalter -> {
                daten.writeByte(2)
                kopf(daten, ausdruck)
                daten.writeUTF(ausdruck.rollenId)
                daten.writeUTF(ausdruck.beschriftung)
            }
            is FormelAusdruck.Operation -> {
                daten.writeByte(3)
                kopf(daten, ausdruck)
                daten.writeUTF(ausdruck.operatorId)
                daten.writeBoolean(ausdruck.explizitGruppiert)
                val argumente = ausdruck.argumente.sortedBy { it.position }
                daten.writeInt(argumente.size)
                argumente.forEach { argument ->
                    daten.writeUTF(argument.rollenId)
                    daten.writeInt(argument.position)
                    schreibe(daten, argument.ausdruck)
                }
            }
        }
    }

    private fun kopf(daten: DataOutputStream, ausdruck: FormelAusdruck) {
        daten.writeUTF(ausdruck.id)
        daten.writeUTF(ausdruck.typ.name)
    }

    private fun lies(daten: DataInputStream): FormelAusdruck {
        val art = daten.readByte().toInt()
        val id = daten.readUTF()
        val typ = FormelTyp.valueOf(daten.readUTF())
        return when (art) {
            0 -> {
                val latex = daten.readUTF()
                val import = FormelLatexCodec.importiere(latex)
                val literal = (import as? FormelLatexImportErgebnis.Erfolg)?.ausdruck as? FormelAusdruck.Literal
                    ?: error("Literal '$latex' kann nicht wiederhergestellt werden.")
                literal.copy(id = id, typ = typ)
            }
            1 -> FormelAusdruck.Variable(id, daten.readUTF(), daten.readUTF(), typ)
            2 -> FormelAusdruck.Platzhalter(id, daten.readUTF(), daten.readUTF(), typ)
            3 -> {
                val operatorId = daten.readUTF()
                val gruppiert = daten.readBoolean()
                val argumente = List(daten.readInt()) {
                    val rolle = daten.readUTF()
                    val position = daten.readInt()
                    FormelArgument(rolle, position, lies(daten))
                }
                FormelAusdruck.Operation(id, operatorId, argumente, typ, explizitGruppiert = gruppiert)
            }
            else -> error("Unbekannte Ausdrucksart $art.")
        }
    }
}

/** Lesbare Projektion der fachlichen Operatoren, unabhängig von ihrer Persistenz. */
object StrukturFormelDarstellung {
    fun latex(wurzel: FormelAusdruck, quantorVariable: String = "x"): String = render(wurzel, quantorVariable)

    private fun render(ausdruck: FormelAusdruck, quantorVariable: String): String = when (ausdruck) {
        is FormelAusdruck.Literal -> ausdruck.wert.zuLatex()
        is FormelAusdruck.Variable -> ausdruck.latex
        is FormelAusdruck.Platzhalter -> "\\square_{${ausdruck.beschriftung}}"
        is FormelAusdruck.Operation -> renderOperation(ausdruck, quantorVariable)
    }

    private fun renderOperation(operation: FormelAusdruck.Operation, variable: String): String {
        val argumente = operation.argumente.sortedBy { it.position }.map { it.ausdruck }
        fun argument(index: Int): String = argumente.getOrNull(index)?.let { teil ->
            val text = render(teil, variable)
            if (teil is FormelAusdruck.Operation) "\\left($text\\right)" else text
        } ?: "\\square"
        fun binaer(zeichen: String): String = "${argument(0)} $zeichen ${argument(1)}"
        fun funktion(name: String): String = "$name\\left(${argument(0)}\\right)"

        return when (operation.operatorId) {
            "zahl.addition", "vektor.addition", "matrix.addition", "tensor.addition" -> binaer("+")
            "zahl.subtraktion", "vektor.subtraktion", "matrix.subtraktion", "tensor.subtraktion" -> binaer("-")
            "zahl.multiplikation" -> binaer("\\cdot")
            "zahl.division" -> "\\frac{${argument(0)}}{${argument(1)}}"
            "zahl.potenz" -> "${argument(0)}^{${argument(1)}}"
            "zahl.wurzel" -> "\\sqrt{${argument(0)}}"

            "aussage.negation" -> "\\neg ${argument(0)}"
            "aussage.konjunktion" -> binaer("\\land")
            "aussage.disjunktion" -> binaer("\\lor")
            "aussage.adjunktion" -> binaer("\\mathbin{\\dot\\lor}")
            "aussage.implikation" -> binaer("\\Rightarrow")
            "aussage.aequivalenz" -> binaer("\\Leftrightarrow")
            "aussage.allquantor" -> "\\forall $variable\\in ${argument(0)}:\\;${argument(1)}"
            "aussage.existenzquantor" -> "\\exists $variable\\in ${argument(0)}:\\;${argument(1)}"
            "aussage.eindeutigerExistenzquantor" -> "\\exists! $variable\\in ${argument(0)}:\\;${argument(1)}"

            "vektor.skalarmultiplikation", "matrix.skalarmultiplikation", "tensor.skalarmultiplikation" -> binaer("\\cdot")
            "vektor.negation" -> "-${argument(0)}"
            "vektor.skalarprodukt" -> "\\left\\langle ${argument(0)},${argument(1)}\\right\\rangle"
            "vektor.kreuzprodukt" -> binaer("\\times")
            "vektor.norm", "tensor.norm" -> "\\left\\|${argument(0)}\\right\\|"
            "vektor.normalisierung" -> "\\frac{${argument(0)}}{\\left\\|${argument(0)}\\right\\|}"
            "vektor.hadamard", "matrix.hadamard", "tensor.hadamard" -> binaer("\\circ")
            "vektor.projektion" -> "\\operatorname{proj}_{${argument(1)}}\\left(${argument(0)}\\right)"
            "vektor.winkel" -> "\\angle\\left(${argument(0)},${argument(1)}\\right)"

            "matrix.produkt", "matrix.vektorProdukt" -> binaer("\\,")
            "matrix.transponieren" -> "${argument(0)}^{\\mathsf T}"
            "matrix.inverse" -> "${argument(0)}^{-1}"
            "matrix.potenz" -> "${argument(0)}^{${argument(1)}}"
            "matrix.determinante" -> funktion("\\det")
            "matrix.spur" -> funktion("\\operatorname{tr}")
            "matrix.rang" -> funktion("\\operatorname{rang}")
            "matrix.hauptdiagonale" -> funktion("\\operatorname{diag}")
            "matrix.nebendiagonale" -> funktion("\\operatorname{antidiag}")

            "tensor.tensorprodukt" -> binaer("\\otimes")
            "tensor.kontraktion" -> funktion("\\operatorname{kontr}")
            "tensor.achsenpermutation" -> funktion("\\operatorname{perm}")
            "tensor.transponieren" -> "${argument(0)}^{\\mathsf T}"
            "tensor.achsenschnitt" -> funktion("\\operatorname{achsenschnitt}")
            "tensor.indexauswertung" -> funktion("\\operatorname{index}")
            else -> "\\operatorname{${operation.operatorId}}\\left(${argumente.indices.joinToString(",") { argument(it) }}\\right)"
        }
    }
}

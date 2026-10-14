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

    private fun ausQuelle(familie: OperatorFamilie, kategorie: StrukturFormelKategorie) =
        RechnerOperatorDefinitionen.fürFamilie(familie).filter { it.formelFamilie == familie && it.formelArgumente.isNotEmpty() }.map { d ->
            StrukturFormelTaste(d.id, d.symbolLatex, kategorie, d.id, d.formelErgebnisTyp,
                d.formelArgumente.map { StrukturFormelArgumentTyp(it.name, it.typ) })
        }
    private val vektor = ausQuelle(OperatorFamilie.VEKTOR, StrukturFormelKategorie.VEKTOR)
    private val matrix = ausQuelle(OperatorFamilie.MATRIX, StrukturFormelKategorie.MATRIX)
    private val tensor = ausQuelle(OperatorFamilie.TENSOR, StrukturFormelKategorie.TENSOR)

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
    fun kodieren(formel: GebundeneFormel): String = FormelKontextCodec.kodieren(formel)
    fun dekodierenGebunden(kodiert: String): GebundeneFormel = FormelKontextCodec.dekodieren(kodiert)

    private const val VERSION = 3

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
        require(kodiert.length <= 32_000_000) { "Strukturformel überschreitet die Größenbegrenzung." }
        val bytes = Base64.getUrlDecoder().decode(kodiert)
        return DataInputStream(ByteArrayInputStream(bytes)).use { daten ->
            val version = daten.readInt()
            require(version == VERSION) { "Nicht unterstützte Strukturformel-Version $version; erwartet wird Version $VERSION." }
            lies(daten, version).also { require(daten.available() == 0) { "Unerwartete Strukturformel-Restdaten." } }
        }
    }

    private fun schreibe(daten: DataOutputStream, ausdruck: FormelAusdruck, tiefe: Int = 0) {
        require(tiefe <= 128) { "Strukturformel ist zu tief verschachtelt." }
        when (ausdruck) {
            is FormelAusdruck.Literal -> {
                daten.writeByte(0)
                kopf(daten, ausdruck)
                schreibeText(daten, MathematischerWertCodec.kodieren(ausdruck.wert))
                ÜbersetzungsCodec.metadaten(daten, ausdruck.übersetzungen)
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
                daten.writeInt(ausdruck.bedingungen.size)
                ausdruck.bedingungen.forEach { schreibeText(daten, MathematischerWertCodec.kodieren(it)) }
                val argumente = ausdruck.argumente.sortedBy { it.position }
                daten.writeInt(argumente.size)
                argumente.forEach { argument ->
                    daten.writeUTF(argument.rollenId)
                    daten.writeInt(argument.position)
                    schreibe(daten, argument.ausdruck, tiefe + 1)
                }
            }
        }
    }

    private fun kopf(daten: DataOutputStream, ausdruck: FormelAusdruck) {
        daten.writeUTF(ausdruck.id)
        daten.writeUTF(ausdruck.typ.name)
    }

    private fun schreibeText(daten: DataOutputStream, text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        require(bytes.size <= 16_000_000)
        daten.writeInt(bytes.size); daten.write(bytes)
    }
    private fun liesText(daten: DataInputStream): String {
        val n = daten.readInt()
        require(n in 0..16_000_000 && n <= daten.available()) { "Ungültige Strukturformel-Textlänge." }
        return ByteArray(n).also(daten::readFully).toString(Charsets.UTF_8)
    }

    private fun anzahl(daten: DataInputStream): Int = daten.readInt().also {
        require(it in 0..100_000) { "Ungültige Strukturformel-Komponentenanzahl." }
    }

    private fun lies(daten: DataInputStream, tiefe: Int = 0): FormelAusdruck {
        require(tiefe <= 128) { "Strukturformel ist zu tief verschachtelt." }
        val art = daten.readByte().toInt()
        val id = daten.readUTF()
        val typ = FormelTyp.valueOf(daten.readUTF())
        return when (art) {
            0 -> {
                val wert = MathematischerWertCodec.dekodieren(liesText(daten))
                val metadaten = ÜbersetzungsCodec.metadaten(daten)
                require(metadaten.aktuell == null || metadaten.aktuell.ziel == ÜbersetzungsStruktur.von(wert)) { "Literal und Zeugnisstruktur widersprechen sich." }
                FormelAusdruck.Literal(id, wert, typ, metadaten)
            }
            1 -> FormelAusdruck.Variable(id, daten.readUTF(), daten.readUTF(), typ)
            2 -> FormelAusdruck.Platzhalter(id, daten.readUTF(), daten.readUTF(), typ)
            3 -> {
                val operatorId = daten.readUTF()
                val gruppiert = daten.readBoolean()
                val bedingungen = List(anzahl(daten)) {
                    MathematischerWertCodec.dekodieren(liesText(daten)) as Aussage
                }
                val argumente = List(anzahl(daten)) {
                    val rolle = daten.readUTF()
                    val position = daten.readInt()
                    FormelArgument(rolle, position, lies(daten, tiefe + 1))
                }
                FormelAusdruck.Operation(id, operatorId, argumente, typ, bedingungen = bedingungen, explizitGruppiert = gruppiert)
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

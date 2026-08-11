package de.TeutonStudio.MathematikRechenSystem.kern

import de.TeutonStudio.TypSystem.TypAusdruck
import de.TeutonStudio.TypSystem.TypTragend
import java.math.BigDecimal
import kotlin.math.abs

/** Rechteckiger SVG-Zeichenbereich. */
data class SvgViewport(
    val minX: Double = 0.0,
    val minY: Double = 0.0,
    val breite: Double = 1000.0,
    val höhe: Double = 1000.0,
) {
    init {
        require(breite > 0.0) { "Die SVG-ViewBox benötigt eine positive Breite." }
        require(höhe > 0.0) { "Die SVG-ViewBox benötigt eine positive Höhe." }
    }
}

/**
 * Mathematischer Koordinatenraum, der zentral in den SVG-Zeichenraum abgebildet wird.
 * Mathematisch wächst y nach oben; SVG wächst y nach unten.
 */
data class SvgKoordinatenraum(
    val xMin: Double = -10.0,
    val xMax: Double = 10.0,
    val yMin: Double = -10.0,
    val yMax: Double = 10.0,
) {
    init {
        require(xMax > xMin) { "Der SVG-Koordinatenraum benötigt xMax > xMin." }
        require(yMax > yMin) { "Der SVG-Koordinatenraum benötigt yMax > yMin." }
    }

    fun bildeAb(punkt: SvgPunkt, viewport: SvgViewport): SvgPunkt {
        val x = viewport.minX + (punkt.x - xMin) / (xMax - xMin) * viewport.breite
        val y = viewport.minY + (yMax - punkt.y) / (yMax - yMin) * viewport.höhe
        return SvgPunkt(x, y)
    }
}

data class SvgPunkt(val x: Double, val y: Double)

/** Wiederverwendbarer Stilwert für SVG-Ergänzungsknoten. */
data class SvgStil(
    val füllung: String = "none",
    val kontur: String = "currentColor",
    val konturBreite: Double = 1.5,
    val deckkraft: Double = 1.0,
    val strichMuster: List<Double> = emptyList(),
    val linienEnde: String = "round",
    val linienVerbindung: String = "round",
) : DarstellungsWert, TypTragend {
    init {
        require(konturBreite >= 0.0) { "Die Konturbreite darf nicht negativ sein." }
        require(deckkraft in 0.0..1.0) { "Die Deckkraft muss zwischen 0 und 1 liegen." }
        require(strichMuster.all { it >= 0.0 }) { "Strichmusterwerte dürfen nicht negativ sein." }
    }

    override val typAusdruck: TypAusdruck
        get() = TypAusdruck.Atom(MathematischeTypen.SvgStil)

    override fun zuLatex(): String = "\\operatorname{SVG\\text{-}Stil}"
}

sealed interface SvgTransformation {
    data class Verschieben(val x: Double, val y: Double) : SvgTransformation
    data class Skalieren(val x: Double, val y: Double = x) : SvgTransformation
    data class Drehen(val grad: Double, val um: SvgPunkt? = null) : SvgTransformation
    data class Matrix(
        val a: Double,
        val b: Double,
        val c: Double,
        val d: Double,
        val e: Double,
        val f: Double,
    ) : SvgTransformation
}

sealed interface SvgPfadSegment {
    data class Bewegen(val ziel: SvgPunkt) : SvgPfadSegment
    data class Linie(val ziel: SvgPunkt) : SvgPfadSegment
    data class Kubisch(
        val kontrolle1: SvgPunkt,
        val kontrolle2: SvgPunkt,
        val ziel: SvgPunkt,
    ) : SvgPfadSegment
    data class Quadratisch(val kontrolle: SvgPunkt, val ziel: SvgPunkt) : SvgPfadSegment
    data class Bogen(
        val radiusX: Double,
        val radiusY: Double,
        val rotation: Double,
        val großerBogen: Boolean,
        val imUhrzeigersinn: Boolean,
        val ziel: SvgPunkt,
    ) : SvgPfadSegment
    data object Schließen : SvgPfadSegment
}

sealed interface SvgElement {
    val id: String
    val stil: SvgStil?
    val transformationen: List<SvgTransformation>
}

data class SvgGruppe(
    override val id: String,
    val kinder: List<SvgElement>,
    override val stil: SvgStil? = null,
    override val transformationen: List<SvgTransformation> = emptyList(),
) : SvgElement

data class SvgPfad(
    override val id: String,
    val segmente: List<SvgPfadSegment>,
    override val stil: SvgStil? = null,
    override val transformationen: List<SvgTransformation> = emptyList(),
) : SvgElement

data class SvgLinie(
    override val id: String,
    val start: SvgPunkt,
    val ende: SvgPunkt,
    override val stil: SvgStil? = null,
    override val transformationen: List<SvgTransformation> = emptyList(),
) : SvgElement

data class SvgRechteck(
    override val id: String,
    val position: SvgPunkt,
    val breite: Double,
    val höhe: Double,
    val radiusX: Double = 0.0,
    val radiusY: Double = 0.0,
    override val stil: SvgStil? = null,
    override val transformationen: List<SvgTransformation> = emptyList(),
) : SvgElement

data class SvgKreis(
    override val id: String,
    val mittelpunkt: SvgPunkt,
    val radius: Double,
    override val stil: SvgStil? = null,
    override val transformationen: List<SvgTransformation> = emptyList(),
) : SvgElement

data class SvgEllipse(
    override val id: String,
    val mittelpunkt: SvgPunkt,
    val radiusX: Double,
    val radiusY: Double,
    override val stil: SvgStil? = null,
    override val transformationen: List<SvgTransformation> = emptyList(),
) : SvgElement

data class SvgPolygon(
    override val id: String,
    val punkte: List<SvgPunkt>,
    override val stil: SvgStil? = null,
    override val transformationen: List<SvgTransformation> = emptyList(),
) : SvgElement

data class SvgLinienzug(
    override val id: String,
    val punkte: List<SvgPunkt>,
    override val stil: SvgStil? = null,
    override val transformationen: List<SvgTransformation> = emptyList(),
) : SvgElement

data class SvgText(
    override val id: String,
    val position: SvgPunkt,
    val inhalt: String,
    /** Kennzeichnet mathematischen LaTeX-Inhalt. Der Serializer bewahrt ihn verlustfrei als Metadatum. */
    val mathematikLatex: Boolean = false,
    override val stil: SvgStil? = null,
    override val transformationen: List<SvgTransformation> = emptyList(),
) : SvgElement

/** Definitionen werden getrennt vom sichtbaren Elementbaum geführt. */
sealed interface SvgDefinition {
    val id: String
}

data class SvgSymbolDefinition(
    override val id: String,
    val elemente: List<SvgElement>,
) : SvgDefinition

/**
 * Unveränderlicher SVG-Dokumentwert. Jeder SVG-Knoten reicht diesen vollständigen AST weiter
 * und ergänzt oder verändert genau den eigenen Verarbeitungsschritt.
 */
data class SvgGrafik(
    val viewport: SvgViewport = SvgViewport(),
    val koordinatenraum: SvgKoordinatenraum = SvgKoordinatenraum(),
    val elemente: List<SvgElement> = emptyList(),
    val definitionen: List<SvgDefinition> = emptyList(),
    val breite: String = "100%",
    val höhe: String = "100%",
    val preserveAspectRatio: String = "xMidYMid meet",
) : Grafik, TypTragend {
    override val typAusdruck: TypAusdruck
        get() = TypAusdruck.Atom(MathematischeTypen.SvgGrafik)

    override fun zuLatex(): String = "\\operatorname{SVG}_{${elemente.size}}"

    fun mitElement(element: SvgElement): SvgGrafik = copy(elemente = elemente + element)

    fun mitElementen(neu: List<SvgElement>): SvgGrafik = copy(elemente = elemente + neu)

    fun gruppiere(id: String, stil: SvgStil? = null): SvgGrafik =
        copy(elemente = listOf(SvgGruppe(id = id, kinder = elemente, stil = stil)))

    fun kombiniere(andere: SvgGrafik): SvgGrafik {
        val belegteIds = elementIds().toMutableSet()
        val neueElemente = andere.elemente.map { element ->
            element.mitEindeutigerId(belegteIds).also { belegteIds += it.id }
        }
        val belegteDefinitionen = definitionen.mapTo(mutableSetOf()) { it.id }
        val neueDefinitionen = andere.definitionen.map { definition ->
            if (definition.id !in belegteDefinitionen) definition
            else when (definition) {
                is SvgSymbolDefinition -> definition.copy(id = eindeutigeId(definition.id, belegteDefinitionen))
            }
        }
        return copy(
            elemente = elemente + neueElemente,
            definitionen = definitionen + neueDefinitionen,
        )
    }

    fun zuSvg(): String = SvgSerializer.serialisiere(this)

    private fun elementIds(): Set<String> = buildSet {
        fun sammle(element: SvgElement) {
            add(element.id)
            if (element is SvgGruppe) element.kinder.forEach(::sammle)
        }
        elemente.forEach(::sammle)
    }

    companion object {
        fun standard(): SvgGrafik = SvgGrafik()
    }
}

private fun SvgElement.mitEindeutigerId(belegt: Set<String>): SvgElement {
    if (id !in belegt) return this
    val neueId = eindeutigeId(id, belegt)
    return when (this) {
        is SvgGruppe -> copy(id = neueId)
        is SvgPfad -> copy(id = neueId)
        is SvgLinie -> copy(id = neueId)
        is SvgRechteck -> copy(id = neueId)
        is SvgKreis -> copy(id = neueId)
        is SvgEllipse -> copy(id = neueId)
        is SvgPolygon -> copy(id = neueId)
        is SvgLinienzug -> copy(id = neueId)
        is SvgText -> copy(id = neueId)
    }
}

private fun eindeutigeId(basis: String, belegt: Set<String>): String {
    var index = 2
    var kandidat = "$basis-$index"
    while (kandidat in belegt) {
        index++
        kandidat = "$basis-$index"
    }
    return kandidat
}

object SvgSerializer {
    fun serialisiere(grafik: SvgGrafik): String = buildString {
        val viewport = grafik.viewport
        append("<svg xmlns=\"http://www.w3.org/2000/svg\"")
        append(" width=\"").append(xml(grafik.breite)).append("\"")
        append(" height=\"").append(xml(grafik.höhe)).append("\"")
        append(" viewBox=\"")
            .append(zahl(viewport.minX)).append(' ')
            .append(zahl(viewport.minY)).append(' ')
            .append(zahl(viewport.breite)).append(' ')
            .append(zahl(viewport.höhe)).append("\"")
        append(" preserveAspectRatio=\"").append(xml(grafik.preserveAspectRatio)).append("\">")
        if (grafik.definitionen.isNotEmpty()) {
            append("<defs>")
            grafik.definitionen.forEach { definition ->
                when (definition) {
                    is SvgSymbolDefinition -> {
                        append("<symbol id=\"").append(xmlId(definition.id)).append("\">")
                        definition.elemente.forEach { appendElement(it) }
                        append("</symbol>")
                    }
                }
            }
            append("</defs>")
        }
        grafik.elemente.forEach { appendElement(it) }
        append("</svg>")
    }

    private fun StringBuilder.appendElement(element: SvgElement) {
        when (element) {
            is SvgGruppe -> {
                append("<g")
                appendGemeinsam(element)
                append('>')
                element.kinder.forEach { appendElement(it) }
                append("</g>")
            }
            is SvgPfad -> {
                append("<path")
                appendGemeinsam(element)
                append(" d=\"").append(xml(pfad(element.segmente))).append("\"/>")
            }
            is SvgLinie -> {
                append("<line")
                appendGemeinsam(element)
                attribut("x1", element.start.x)
                attribut("y1", element.start.y)
                attribut("x2", element.ende.x)
                attribut("y2", element.ende.y)
                append("/>")
            }
            is SvgRechteck -> {
                append("<rect")
                appendGemeinsam(element)
                attribut("x", element.position.x)
                attribut("y", element.position.y)
                attribut("width", element.breite)
                attribut("height", element.höhe)
                if (element.radiusX > 0.0) attribut("rx", element.radiusX)
                if (element.radiusY > 0.0) attribut("ry", element.radiusY)
                append("/>")
            }
            is SvgKreis -> {
                append("<circle")
                appendGemeinsam(element)
                attribut("cx", element.mittelpunkt.x)
                attribut("cy", element.mittelpunkt.y)
                attribut("r", element.radius)
                append("/>")
            }
            is SvgEllipse -> {
                append("<ellipse")
                appendGemeinsam(element)
                attribut("cx", element.mittelpunkt.x)
                attribut("cy", element.mittelpunkt.y)
                attribut("rx", element.radiusX)
                attribut("ry", element.radiusY)
                append("/>")
            }
            is SvgPolygon -> {
                append("<polygon")
                appendGemeinsam(element)
                append(" points=\"").append(punkte(element.punkte)).append("\"/>")
            }
            is SvgLinienzug -> {
                append("<polyline")
                appendGemeinsam(element)
                append(" points=\"").append(punkte(element.punkte)).append("\"/>")
            }
            is SvgText -> {
                append("<text")
                appendGemeinsam(element)
                attribut("x", element.position.x)
                attribut("y", element.position.y)
                if (element.mathematikLatex) {
                    append(" data-mathematik-latex=\"").append(xml(element.inhalt)).append("\"")
                }
                append('>').append(xml(element.inhalt)).append("</text>")
            }
        }
    }

    private fun StringBuilder.appendGemeinsam(element: SvgElement) {
        append(" id=\"").append(xmlId(element.id)).append("\"")
        element.stil?.let(::appendStil)
        if (element.transformationen.isNotEmpty()) {
            append(" transform=\"")
            append(element.transformationen.joinToString(" ", transform = ::transformation))
            append('"')
        }
    }

    private fun StringBuilder.appendStil(stil: SvgStil) {
        append(" fill=\"").append(xml(stil.füllung)).append("\"")
        append(" stroke=\"").append(xml(stil.kontur)).append("\"")
        append(" stroke-width=\"").append(zahl(stil.konturBreite)).append("\"")
        append(" opacity=\"").append(zahl(stil.deckkraft)).append("\"")
        append(" stroke-linecap=\"").append(xml(stil.linienEnde)).append("\"")
        append(" stroke-linejoin=\"").append(xml(stil.linienVerbindung)).append("\"")
        if (stil.strichMuster.isNotEmpty()) {
            append(" stroke-dasharray=\"")
                .append(stil.strichMuster.joinToString(",", transform = ::zahl))
                .append("\"")
        }
    }

    private fun StringBuilder.attribut(name: String, wert: Double) {
        append(' ').append(name).append("=\"").append(zahl(wert)).append("\"")
    }

    private fun punkte(punkte: List<SvgPunkt>): String =
        punkte.joinToString(" ") { "${zahl(it.x)},${zahl(it.y)}" }

    private fun pfad(segmente: List<SvgPfadSegment>): String = segmente.joinToString(" ") { segment ->
        when (segment) {
            is SvgPfadSegment.Bewegen -> "M ${punkt(segment.ziel)}"
            is SvgPfadSegment.Linie -> "L ${punkt(segment.ziel)}"
            is SvgPfadSegment.Kubisch -> "C ${punkt(segment.kontrolle1)} ${punkt(segment.kontrolle2)} ${punkt(segment.ziel)}"
            is SvgPfadSegment.Quadratisch -> "Q ${punkt(segment.kontrolle)} ${punkt(segment.ziel)}"
            is SvgPfadSegment.Bogen -> "A ${zahl(segment.radiusX)} ${zahl(segment.radiusY)} ${zahl(segment.rotation)} ${if (segment.großerBogen) 1 else 0} ${if (segment.imUhrzeigersinn) 1 else 0} ${punkt(segment.ziel)}"
            SvgPfadSegment.Schließen -> "Z"
        }
    }

    private fun punkt(punkt: SvgPunkt): String = "${zahl(punkt.x)} ${zahl(punkt.y)}"

    private fun transformation(transformation: SvgTransformation): String = when (transformation) {
        is SvgTransformation.Verschieben -> "translate(${zahl(transformation.x)} ${zahl(transformation.y)})"
        is SvgTransformation.Skalieren -> "scale(${zahl(transformation.x)} ${zahl(transformation.y)})"
        is SvgTransformation.Drehen -> transformation.um?.let {
            "rotate(${zahl(transformation.grad)} ${zahl(it.x)} ${zahl(it.y)})"
        } ?: "rotate(${zahl(transformation.grad)})"
        is SvgTransformation.Matrix -> "matrix(${zahl(transformation.a)} ${zahl(transformation.b)} ${zahl(transformation.c)} ${zahl(transformation.d)} ${zahl(transformation.e)} ${zahl(transformation.f)})"
    }

    private fun zahl(wert: Double): String {
        require(wert.isFinite()) { "SVG kann keine nichtendliche Koordinate serialisieren." }
        if (abs(wert) < 1e-12) return "0"
        return BigDecimal.valueOf(wert).stripTrailingZeros().toPlainString()
    }

    private fun xmlId(wert: String): String = xml(wert.ifBlank { "element" })

    private fun xml(wert: String): String = buildString(wert.length) {
        wert.forEach { zeichen ->
            when (zeichen) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(zeichen)
            }
        }
    }
}

package de.TeutonStudio.MathematikRechenSystem.kern

import de.TeutonStudio.TypSystem.TypAusdruck
import de.TeutonStudio.TypSystem.TypTragend
import java.math.BigDecimal
import kotlin.math.abs

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
    val attribute: Map<String, String>
}

data class SvgGruppe(
    override val id: String,
    val kinder: List<SvgElement>,
    override val stil: SvgStil? = null,
    override val transformationen: List<SvgTransformation> = emptyList(),
    override val attribute: Map<String, String> = emptyMap(),
) : SvgElement

data class SvgPfad(
    override val id: String,
    val segmente: List<SvgPfadSegment>,
    override val stil: SvgStil? = null,
    override val transformationen: List<SvgTransformation> = emptyList(),
    override val attribute: Map<String, String> = emptyMap(),
) : SvgElement

data class SvgLinie(
    override val id: String,
    val start: SvgPunkt,
    val ende: SvgPunkt,
    override val stil: SvgStil? = null,
    override val transformationen: List<SvgTransformation> = emptyList(),
    override val attribute: Map<String, String> = emptyMap(),
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
    override val attribute: Map<String, String> = emptyMap(),
) : SvgElement

data class SvgKreis(
    override val id: String,
    val mittelpunkt: SvgPunkt,
    val radius: Double,
    override val stil: SvgStil? = null,
    override val transformationen: List<SvgTransformation> = emptyList(),
    override val attribute: Map<String, String> = emptyMap(),
) : SvgElement

data class SvgEllipse(
    override val id: String,
    val mittelpunkt: SvgPunkt,
    val radiusX: Double,
    val radiusY: Double,
    override val stil: SvgStil? = null,
    override val transformationen: List<SvgTransformation> = emptyList(),
    override val attribute: Map<String, String> = emptyMap(),
) : SvgElement

data class SvgPolygon(
    override val id: String,
    val punkte: List<SvgPunkt>,
    override val stil: SvgStil? = null,
    override val transformationen: List<SvgTransformation> = emptyList(),
    override val attribute: Map<String, String> = emptyMap(),
) : SvgElement

data class SvgLinienzug(
    override val id: String,
    val punkte: List<SvgPunkt>,
    override val stil: SvgStil? = null,
    override val transformationen: List<SvgTransformation> = emptyList(),
    override val attribute: Map<String, String> = emptyMap(),
) : SvgElement

data class SvgText(
    override val id: String,
    val position: SvgPunkt,
    val inhalt: String,
    val mathematikLatex: Boolean = false,
    override val stil: SvgStil? = null,
    override val transformationen: List<SvgTransformation> = emptyList(),
    override val attribute: Map<String, String> = emptyMap(),
) : SvgElement

data class SvgVerwendung(
    override val id: String,
    val referenzId: String,
    val position: SvgPunkt = SvgPunkt(0.0, 0.0),
    val breite: Double? = null,
    val höhe: Double? = null,
    override val stil: SvgStil? = null,
    override val transformationen: List<SvgTransformation> = emptyList(),
    override val attribute: Map<String, String> = emptyMap(),
) : SvgElement

data class SvgFarbStopp(
    val offset: Double,
    val farbe: String,
    val deckkraft: Double = 1.0,
) {
    init {
        require(offset in 0.0..1.0) { "Ein SVG-Farbstopp benötigt einen Offset zwischen 0 und 1." }
        require(deckkraft in 0.0..1.0) { "Die Deckkraft eines SVG-Farbstopps muss zwischen 0 und 1 liegen." }
    }
}

enum class SvgFilterPrimitivArt(val tagName: String) {
    Blend("feBlend"),
    ColorMatrix("feColorMatrix"),
    ComponentTransfer("feComponentTransfer"),
    Composite("feComposite"),
    ConvolveMatrix("feConvolveMatrix"),
    DiffuseLighting("feDiffuseLighting"),
    DisplacementMap("feDisplacementMap"),
    DropShadow("feDropShadow"),
    Flood("feFlood"),
    GaussianBlur("feGaussianBlur"),
    Image("feImage"),
    Merge("feMerge"),
    Morphology("feMorphology"),
    Offset("feOffset"),
    SpecularLighting("feSpecularLighting"),
    Tile("feTile"),
    Turbulence("feTurbulence"),
}

data class SvgFilterPrimitiv(
    val art: SvgFilterPrimitivArt,
    val attribute: Map<String, String> = emptyMap(),
)

sealed interface SvgDefinition {
    val id: String
}

data class SvgSymbolDefinition(
    override val id: String,
    val elemente: List<SvgElement>,
) : SvgDefinition

data class SvgMarkerDefinition(
    override val id: String,
    val elemente: List<SvgElement>,
    val refX: Double = 0.0,
    val refY: Double = 0.0,
    val markerBreite: Double = 3.0,
    val markerHöhe: Double = 3.0,
    val orientierung: String = "auto",
) : SvgDefinition

data class SvgClipPfadDefinition(
    override val id: String,
    val elemente: List<SvgElement>,
) : SvgDefinition

data class SvgMaskenDefinition(
    override val id: String,
    val elemente: List<SvgElement>,
) : SvgDefinition

data class SvgLinearerVerlaufDefinition(
    override val id: String,
    val start: SvgPunkt = SvgPunkt(0.0, 0.0),
    val ende: SvgPunkt = SvgPunkt(1.0, 0.0),
    val stopps: List<SvgFarbStopp>,
) : SvgDefinition

data class SvgRadialerVerlaufDefinition(
    override val id: String,
    val mittelpunkt: SvgPunkt = SvgPunkt(0.5, 0.5),
    val radius: Double = 0.5,
    val stopps: List<SvgFarbStopp>,
) : SvgDefinition

data class SvgPatternDefinition(
    override val id: String,
    val breite: Double,
    val höhe: Double,
    val elemente: List<SvgElement>,
) : SvgDefinition

data class SvgFilterDefinition(
    override val id: String,
    val primitive: List<SvgFilterPrimitiv>,
) : SvgDefinition

data class SvgGrafik(
    val viewport: SvgViewport = SvgViewport(),
    val koordinatenraum: SvgKoordinatenraum = SvgKoordinatenraum(),
    val elemente: List<SvgElement> = emptyList(),
    val definitionen: List<SvgDefinition> = emptyList(),
    val breite: String = "100%",
    val höhe: String = "100%",
    val preserveAspectRatio: String = "xMidYMid meet",
    val titel: String? = null,
    val beschreibung: String? = null,
    val metadaten: String? = null,
) : Grafik, TypTragend {
    override val typAusdruck: TypAusdruck
        get() = TypAusdruck.Atom(MathematischeTypen.SvgGrafik)

    override fun zuLatex(): String = "\\operatorname{SVG}_{${elemente.size}}"

    fun mitElement(element: SvgElement): SvgGrafik = copy(elemente = elemente + element)
    fun mitElementen(neu: List<SvgElement>): SvgGrafik = copy(elemente = elemente + neu)

    fun mitDefinition(definition: SvgDefinition): SvgGrafik = copy(
        definitionen = definitionen.filterNot { it.id == definition.id } + definition,
    )

    fun gruppiere(id: String, stil: SvgStil? = null): SvgGrafik =
        copy(elemente = listOf(SvgGruppe(id = id, kinder = elemente, stil = stil)))

    fun gruppeAuflösen(zielId: String? = null): SvgGrafik =
        copy(elemente = elemente.flacheGruppe(zielId?.takeIf { it.isNotBlank() }))

    fun findeElement(id: String): SvgElement? = elemente.firstNotNullOfOrNull { it.finde(id) }

    fun elementAuswahl(zielId: String?): List<SvgElement> {
        val id = zielId?.trim().orEmpty()
        return if (id.isBlank()) elemente else listOfNotNull(findeElement(id))
    }

    fun transformiere(zielId: String?, transformation: SvgTransformation): SvgGrafik =
        ändereElemente(zielId) { it.mitTransformation(transformation) }

    fun stilisiere(zielId: String?, stil: SvgStil): SvgGrafik =
        ändereElemente(zielId) { it.mitStil(stil) }

    fun setzeAttribut(zielId: String?, name: String, wert: String?): SvgGrafik =
        ändereElemente(zielId) { element ->
            val neu = element.attribute.toMutableMap()
            if (wert == null) neu.remove(name) else neu[name] = wert
            element.mitAttributen(neu)
        }

    fun entferneElement(zielId: String): SvgGrafik = copy(elemente = elemente.entferneRekursiv(zielId))

    fun dupliziereElement(zielId: String, neueId: String): SvgGrafik {
        val belegt = elementIds()
        val gewünschteId = neueId.ifBlank { "$zielId-kopie" }
        val kopieId = if (gewünschteId in belegt) eindeutigeId(gewünschteId, belegt) else gewünschteId
        return copy(elemente = elemente.dupliziereRekursiv(zielId, kopieId))
    }

    fun ordneElement(zielId: String, delta: Int, ganz: Boolean = false): SvgGrafik =
        copy(elemente = elemente.ordneRekursiv(zielId, delta, ganz))

    fun kombiniere(andere: SvgGrafik): SvgGrafik {
        val belegteDefinitionen = definitionen.mapTo(mutableSetOf()) { it.id }
        val mapping = linkedMapOf<String, String>()
        andere.definitionen.forEach { definition ->
            val neu = if (definition.id in belegteDefinitionen) eindeutigeId(definition.id, belegteDefinitionen) else definition.id
            mapping[definition.id] = neu
            belegteDefinitionen += neu
        }

        val neueDefinitionen = andere.definitionen.map { definition ->
            definition.mitId(mapping.getValue(definition.id)).umschreibeReferenzen(mapping)
        }

        val belegteIds = elementIds().toMutableSet()
        val neueElemente = andere.elemente.map { element ->
            val umgeschrieben = element.umschreibeReferenzen(mapping)
            umgeschrieben.mitEindeutigerId(belegteIds).also { belegteIds += it.id }
        }

        return copy(
            elemente = elemente + neueElemente,
            definitionen = definitionen + neueDefinitionen,
        )
    }

    fun zuSvg(): String = SvgSerializer.serialisiere(this)

    fun elementIds(): Set<String> = buildSet {
        fun sammle(element: SvgElement) {
            add(element.id)
            if (element is SvgGruppe) element.kinder.forEach(::sammle)
        }
        elemente.forEach(::sammle)
    }

    private fun ändereElemente(zielId: String?, änderung: (SvgElement) -> SvgElement): SvgGrafik {
        val id = zielId?.trim().orEmpty()
        return if (id.isBlank()) {
            copy(elemente = elemente.map(änderung))
        } else {
            copy(elemente = elemente.map { it.mappeRekursiv(id, änderung) })
        }
    }

    companion object {
        fun standard(): SvgGrafik = SvgGrafik()
    }
}

private fun SvgElement.finde(id: String): SvgElement? = when {
    this.id == id -> this
    this is SvgGruppe -> kinder.firstNotNullOfOrNull { it.finde(id) }
    else -> null
}

private fun SvgElement.mappeRekursiv(id: String, änderung: (SvgElement) -> SvgElement): SvgElement {
    if (this.id == id) return änderung(this)
    return if (this is SvgGruppe) copy(kinder = kinder.map { it.mappeRekursiv(id, änderung) }) else this
}

private fun List<SvgElement>.entferneRekursiv(id: String): List<SvgElement> = mapNotNull { element ->
    when {
        element.id == id -> null
        element is SvgGruppe -> element.copy(kinder = element.kinder.entferneRekursiv(id))
        else -> element
    }
}

private fun List<SvgElement>.dupliziereRekursiv(id: String, neueId: String): List<SvgElement> = buildList {
    for (element in this@dupliziereRekursiv) {
        add(element)
        if (element.id == id) {
            add(element.mitId(neueId))
        } else if (element is SvgGruppe) {
            removeAt(lastIndex)
            add(element.copy(kinder = element.kinder.dupliziereRekursiv(id, neueId)))
        }
    }
}

private fun List<SvgElement>.ordneRekursiv(id: String, delta: Int, ganz: Boolean): List<SvgElement> {
    val index = indexOfFirst { it.id == id }
    if (index >= 0) {
        val mutable = toMutableList()
        val element = mutable.removeAt(index)
        val ziel = when {
            ganz && delta < 0 -> 0
            ganz && delta > 0 -> mutable.size
            else -> (index + delta).coerceIn(0, mutable.size)
        }
        mutable.add(ziel, element)
        return mutable
    }
    return map { element ->
        if (element is SvgGruppe) element.copy(kinder = element.kinder.ordneRekursiv(id, delta, ganz)) else element
    }
}

private fun List<SvgElement>.flacheGruppe(zielId: String?): List<SvgElement> = buildList {
    for (element in this@flacheGruppe) {
        when {
            element is SvgGruppe && (zielId == null || element.id == zielId) -> addAll(element.kinder)
            element is SvgGruppe -> add(element.copy(kinder = element.kinder.flacheGruppe(zielId)))
            else -> add(element)
        }
    }
}

private fun SvgElement.mitStil(neu: SvgStil?): SvgElement = when (this) {
    is SvgGruppe -> copy(stil = neu)
    is SvgPfad -> copy(stil = neu)
    is SvgLinie -> copy(stil = neu)
    is SvgRechteck -> copy(stil = neu)
    is SvgKreis -> copy(stil = neu)
    is SvgEllipse -> copy(stil = neu)
    is SvgPolygon -> copy(stil = neu)
    is SvgLinienzug -> copy(stil = neu)
    is SvgText -> copy(stil = neu)
    is SvgVerwendung -> copy(stil = neu)
}

private fun SvgElement.mitTransformation(neu: SvgTransformation): SvgElement = when (this) {
    is SvgGruppe -> copy(transformationen = transformationen + neu)
    is SvgPfad -> copy(transformationen = transformationen + neu)
    is SvgLinie -> copy(transformationen = transformationen + neu)
    is SvgRechteck -> copy(transformationen = transformationen + neu)
    is SvgKreis -> copy(transformationen = transformationen + neu)
    is SvgEllipse -> copy(transformationen = transformationen + neu)
    is SvgPolygon -> copy(transformationen = transformationen + neu)
    is SvgLinienzug -> copy(transformationen = transformationen + neu)
    is SvgText -> copy(transformationen = transformationen + neu)
    is SvgVerwendung -> copy(transformationen = transformationen + neu)
}

private fun SvgElement.mitAttributen(neu: Map<String, String>): SvgElement = when (this) {
    is SvgGruppe -> copy(attribute = neu)
    is SvgPfad -> copy(attribute = neu)
    is SvgLinie -> copy(attribute = neu)
    is SvgRechteck -> copy(attribute = neu)
    is SvgKreis -> copy(attribute = neu)
    is SvgEllipse -> copy(attribute = neu)
    is SvgPolygon -> copy(attribute = neu)
    is SvgLinienzug -> copy(attribute = neu)
    is SvgText -> copy(attribute = neu)
    is SvgVerwendung -> copy(attribute = neu)
}

private fun SvgElement.mitId(neueId: String): SvgElement = when (this) {
    is SvgGruppe -> copy(id = neueId)
    is SvgPfad -> copy(id = neueId)
    is SvgLinie -> copy(id = neueId)
    is SvgRechteck -> copy(id = neueId)
    is SvgKreis -> copy(id = neueId)
    is SvgEllipse -> copy(id = neueId)
    is SvgPolygon -> copy(id = neueId)
    is SvgLinienzug -> copy(id = neueId)
    is SvgText -> copy(id = neueId)
    is SvgVerwendung -> copy(id = neueId)
}

private fun SvgElement.mitEindeutigerId(belegt: Set<String>): SvgElement =
    if (id !in belegt) this else mitId(eindeutigeId(id, belegt))

private fun SvgElement.umschreibeReferenzen(mapping: Map<String, String>): SvgElement {
    val neueAttribute = attribute.mapValues { (_, wert) -> referenzWert(wert, mapping) }
    return when (this) {
        is SvgGruppe -> copy(
            kinder = kinder.map { it.umschreibeReferenzen(mapping) },
            attribute = neueAttribute,
        )
        is SvgPfad -> copy(attribute = neueAttribute)
        is SvgLinie -> copy(attribute = neueAttribute)
        is SvgRechteck -> copy(attribute = neueAttribute)
        is SvgKreis -> copy(attribute = neueAttribute)
        is SvgEllipse -> copy(attribute = neueAttribute)
        is SvgPolygon -> copy(attribute = neueAttribute)
        is SvgLinienzug -> copy(attribute = neueAttribute)
        is SvgText -> copy(attribute = neueAttribute)
        is SvgVerwendung -> copy(
            referenzId = mapping[referenzId] ?: referenzId,
            attribute = neueAttribute,
        )
    }
}

private fun SvgDefinition.mitId(neueId: String): SvgDefinition = when (this) {
    is SvgSymbolDefinition -> copy(id = neueId)
    is SvgMarkerDefinition -> copy(id = neueId)
    is SvgClipPfadDefinition -> copy(id = neueId)
    is SvgMaskenDefinition -> copy(id = neueId)
    is SvgLinearerVerlaufDefinition -> copy(id = neueId)
    is SvgRadialerVerlaufDefinition -> copy(id = neueId)
    is SvgPatternDefinition -> copy(id = neueId)
    is SvgFilterDefinition -> copy(id = neueId)
}

private fun SvgDefinition.umschreibeReferenzen(mapping: Map<String, String>): SvgDefinition = when (this) {
    is SvgSymbolDefinition -> copy(elemente = elemente.map { it.umschreibeReferenzen(mapping) })
    is SvgMarkerDefinition -> copy(elemente = elemente.map { it.umschreibeReferenzen(mapping) })
    is SvgClipPfadDefinition -> copy(elemente = elemente.map { it.umschreibeReferenzen(mapping) })
    is SvgMaskenDefinition -> copy(elemente = elemente.map { it.umschreibeReferenzen(mapping) })
    is SvgLinearerVerlaufDefinition -> this
    is SvgRadialerVerlaufDefinition -> this
    is SvgPatternDefinition -> copy(elemente = elemente.map { it.umschreibeReferenzen(mapping) })
    is SvgFilterDefinition -> copy(
        primitive = primitive.map { primitiv ->
            primitiv.copy(attribute = primitiv.attribute.mapValues { (_, wert) -> referenzWert(wert, mapping) })
        },
    )
}

private fun referenzWert(wert: String, mapping: Map<String, String>): String {
    var ergebnis = wert
    mapping.forEach { (alt, neu) ->
        ergebnis = ergebnis.replace("url(#$alt)", "url(#$neu)")
        if (ergebnis == "#$alt") ergebnis = "#$neu"
    }
    return ergebnis
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

        grafik.titel?.takeIf { it.isNotBlank() }?.let { append("<title>").append(xml(it)).append("</title>") }
        grafik.beschreibung?.takeIf { it.isNotBlank() }?.let { append("<desc>").append(xml(it)).append("</desc>") }
        grafik.metadaten?.takeIf { it.isNotBlank() }?.let { append("<metadata>").append(xml(it)).append("</metadata>") }

        if (grafik.definitionen.isNotEmpty()) {
            append("<defs>")
            grafik.definitionen.forEach { appendDefinition(it) }
            append("</defs>")
        }
        grafik.elemente.forEach { appendElement(it) }
        append("</svg>")
    }

    private fun StringBuilder.appendDefinition(definition: SvgDefinition) {
        when (definition) {
            is SvgSymbolDefinition -> {
                append("<symbol id=\"").append(xmlId(definition.id)).append("\">")
                definition.elemente.forEach { appendElement(it) }
                append("</symbol>")
            }
            is SvgMarkerDefinition -> {
                append("<marker id=\"").append(xmlId(definition.id)).append("\"")
                attribut("refX", definition.refX)
                attribut("refY", definition.refY)
                attribut("markerWidth", definition.markerBreite)
                attribut("markerHeight", definition.markerHöhe)
                append(" orient=\"").append(xml(definition.orientierung)).append("\">")
                definition.elemente.forEach { appendElement(it) }
                append("</marker>")
            }
            is SvgClipPfadDefinition -> {
                append("<clipPath id=\"").append(xmlId(definition.id)).append("\">")
                definition.elemente.forEach { appendElement(it) }
                append("</clipPath>")
            }
            is SvgMaskenDefinition -> {
                append("<mask id=\"").append(xmlId(definition.id)).append("\">")
                definition.elemente.forEach { appendElement(it) }
                append("</mask>")
            }
            is SvgLinearerVerlaufDefinition -> {
                append("<linearGradient id=\"").append(xmlId(definition.id)).append("\"")
                attribut("x1", definition.start.x)
                attribut("y1", definition.start.y)
                attribut("x2", definition.ende.x)
                attribut("y2", definition.ende.y)
                append('>')
                definition.stopps.forEach { appendStopp(it) }
                append("</linearGradient>")
            }
            is SvgRadialerVerlaufDefinition -> {
                append("<radialGradient id=\"").append(xmlId(definition.id)).append("\"")
                attribut("cx", definition.mittelpunkt.x)
                attribut("cy", definition.mittelpunkt.y)
                attribut("r", definition.radius)
                append('>')
                definition.stopps.forEach { appendStopp(it) }
                append("</radialGradient>")
            }
            is SvgPatternDefinition -> {
                append("<pattern id=\"").append(xmlId(definition.id)).append("\" patternUnits=\"userSpaceOnUse\"")
                attribut("width", definition.breite)
                attribut("height", definition.höhe)
                append('>')
                definition.elemente.forEach { appendElement(it) }
                append("</pattern>")
            }
            is SvgFilterDefinition -> {
                append("<filter id=\"").append(xmlId(definition.id)).append("\">")
                definition.primitive.forEach { primitiv ->
                    append('<').append(primitiv.art.tagName)
                    appendAttribute(primitiv.attribute)
                    append("/>")
                }
                append("</filter>")
            }
        }
    }

    private fun StringBuilder.appendStopp(stopp: SvgFarbStopp) {
        append("<stop offset=\"").append(zahl(stopp.offset * 100.0)).append("%\"")
        append(" stop-color=\"").append(xml(stopp.farbe)).append("\"")
        append(" stop-opacity=\"").append(zahl(stopp.deckkraft)).append("\"/>")
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
            is SvgVerwendung -> {
                append("<use")
                appendGemeinsam(element)
                append(" href=\"#").append(xmlId(element.referenzId)).append("\"")
                attribut("x", element.position.x)
                attribut("y", element.position.y)
                element.breite?.let { attribut("width", it) }
                element.höhe?.let { attribut("height", it) }
                append("/>")
            }
        }
    }

    private fun StringBuilder.appendGemeinsam(element: SvgElement) {
        append(" id=\"").append(xmlId(element.id)).append("\"")
        element.stil?.let { appendStil(it) }
        if (element.transformationen.isNotEmpty()) {
            append(" transform=\"")
            append(element.transformationen.joinToString(" ", transform = ::transformation))
            append('"')
        }
        appendAttribute(element.attribute)
    }

    private fun StringBuilder.appendAttribute(attribute: Map<String, String>) {
        attribute.toSortedMap().forEach { (name, wert) ->
            require(ATTRIBUT_NAME.matches(name)) { "Ungültiger SVG-Attributname '$name'." }
            append(' ').append(name).append("=\"").append(xml(wert)).append("\"")
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

    private val ATTRIBUT_NAME = Regex("[A-Za-z_][A-Za-z0-9_.:-]*")
}

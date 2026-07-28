package de.TeutonStudio.MathematikRechenSystem.kern

/** Kennzeichnet das für einen geometrischen Raum verwendete Axiomsystem. */
enum class GeometrischesAxiomSystem { HilbertEuklidisch }

/** Eigenständiger euklidischer Raum; geometrische Objekte verschiedener Räume werden nicht implizit vermischt. */
data class EuklidischerRaum(
    val id: String,
    val dimension: Int,
    val axiomSystem: GeometrischesAxiomSystem = GeometrischesAxiomSystem.HilbertEuklidisch,
) : MathematischesObjekt {
    init {
        require(id.isNotBlank()) { "Ein geometrischer Raum benötigt eine Kennung." }
        require(dimension > 0) { "Die Dimension eines geometrischen Raums muss positiv sein." }
    }

    override fun zuLatex(): String = "${id}_{\\mathbb{E}^{${dimension}}}"
}

/** Koordinatensystem als analytische Realisierung eines geometrischen Raums. */
data class GeometrischesKoordinatensystem(
    val raum: EuklidischerRaum,
    val name: String = "K",
) : MathematischesObjekt {
    init { require(name.isNotBlank()) }
    override fun zuLatex(): String = "${name}:${raum.zuLatex()}"
}

/** Punkt als geometrisches Objekt. Das Tupel ist nur seine Darstellung in einem Koordinatensystem. */
data class GeometriePunkt(
    val name: String,
    override val raum: EuklidischerRaum,
    val koordinaten: Tupel? = null,
) : GeometrischerAusdruck {
    init {
        require(name.isNotBlank()) { "Ein Punkt benötigt einen Namen." }
        koordinaten?.let {
            require(it.elemente.size == raum.dimension) { "Die Tupeldimension muss zur Raumdimension passen." }
            require(it.elemente.all { element -> element is ZahlAusdruck }) { "Punktkoordinaten müssen Zahlen sein." }
        }
    }

    override fun zuLatex(): String = koordinaten?.let { "${name}=${it.zuLatex()}" } ?: name
}

data class GeometrieGerade(
    val a: GeometriePunkt,
    val b: GeometriePunkt,
) : GeometrischerAusdruck {
    override val raum: EuklidischerRaum = gemeinsamerRaum(a, b)
    override fun zuLatex(): String = "\\overleftrightarrow{${a.name}${b.name}}"
}

data class GeometrieEbene(
    val a: GeometriePunkt,
    val b: GeometriePunkt,
    val c: GeometriePunkt,
) : GeometrischerAusdruck {
    override val raum: EuklidischerRaum = gemeinsamerRaum(a, b, c).also {
        require(it.dimension >= 3) { "Eine Ebene als Grundobjekt benötigt mindestens einen dreidimensionalen Raum." }
    }
    override fun zuLatex(): String = "\\operatorname{Ebene}(${a.name},${b.name},${c.name})"
}

data class GeometrieStrecke(
    val anfang: GeometriePunkt,
    val ende: GeometriePunkt,
) : GeometrischerAusdruck {
    override val raum: EuklidischerRaum = gemeinsamerRaum(anfang, ende)
    override fun zuLatex(): String = "\\overline{${anfang.name}${ende.name}}"
}

data class GeometrieStrahl(
    val ursprung: GeometriePunkt,
    val richtungsPunkt: GeometriePunkt,
) : GeometrischerAusdruck {
    override val raum: EuklidischerRaum = gemeinsamerRaum(ursprung, richtungsPunkt)
    override fun zuLatex(): String = "\\overrightarrow{${ursprung.name}${richtungsPunkt.name}}"
}

data class GeometrieWinkel(
    val a: GeometriePunkt,
    val scheitel: GeometriePunkt,
    val c: GeometriePunkt,
    val orientiert: Boolean = false,
) : GeometrischerAusdruck {
    override val raum: EuklidischerRaum = gemeinsamerRaum(a, scheitel, c)
    override fun zuLatex(): String = if (orientiert) "\\measuredangle ${a.name}${scheitel.name}${c.name}" else "\\angle ${a.name}${scheitel.name}${c.name}"
}

data class GeometrieKreislinie(
    val mittelpunkt: GeometriePunkt,
    val randpunkt: GeometriePunkt,
) : GeometrischerAusdruck {
    override val raum: EuklidischerRaum = gemeinsamerRaum(mittelpunkt, randpunkt).also {
        require(it.dimension >= 2) { "Eine Kreislinie benötigt mindestens zwei Dimensionen." }
    }
    override fun zuLatex(): String = "\\mathcal{K}(${mittelpunkt.name};${randpunkt.name})"
}

data class GeometriePolygon(
    val ecken: List<GeometriePunkt>,
) : GeometrischerAusdruck {
    init { require(ecken.size >= 3) { "Ein Polygon benötigt mindestens drei Ecken." } }
    override val raum: EuklidischerRaum = gemeinsamerRaum(*ecken.toTypedArray())
    override fun zuLatex(): String = ecken.joinToString(prefix = "\\operatorname{Polygon}(", postfix = ")") { it.name }
}

data class GeometrieGruppe(
    val objekte: List<GeometrischerAusdruck>,
) : GeometrischerAusdruck {
    init { require(objekte.isNotEmpty()) { "Eine Geometriegruppe darf nicht leer sein." } }
    override val raum: EuklidischerRaum = gemeinsamerRaum(*objekte.toTypedArray())
    override fun zuLatex(): String = objekte.joinToString(prefix = "\\langle ", postfix = " \\rangle") { it.zuLatex() }
}

/** Allgemeines Bildobjekt, wenn eine Transformation den konkreten geometrischen Typ nicht erhält. */
data class TransformiertesGeometrieObjekt(
    val original: GeometrischerAusdruck,
    val struktur: GeometrieStruktur,
    override val raum: EuklidischerRaum,
) : GeometrischerAusdruck {
    override fun zuLatex(): String = "T\\left(${original.zuLatex()}\\right)"
}

enum class StrukturDarstellungsArt { Exakt, Symbolisch, PolygonalApproximation }

data class OrientierterZellVerweis(
    val zellId: String,
    val orientierung: Int = 1,
) {
    init { require(orientierung == -1 || orientierung == 1) { "Eine Zellorientierung muss -1 oder 1 sein." } }
}

data class GeometrischeZelle(
    val id: String,
    val dimension: Int,
    val rand: List<OrientierterZellVerweis> = emptyList(),
    val geometrie: GeometrischerAusdruck? = null,
) {
    init {
        require(id.isNotBlank())
        require(dimension >= 0)
    }
}

data class ZellStufe(
    val dimension: Int,
    val zellen: List<GeometrischeZelle>,
) {
    init {
        require(dimension >= 0)
        require(zellen.all { it.dimension == dimension }) { "Alle Zellen einer Stufe müssen dieselbe Dimension besitzen." }
    }
}

/** Zelluläre Struktur eines geometrischen Objekts, getrennt von dessen intrinsischer Bedeutung und von Rendernetzen. */
data class GeometrieStruktur(
    val raum: EuklidischerRaum,
    val stufen: List<ZellStufe>,
    val darstellungsArt: StrukturDarstellungsArt = StrukturDarstellungsArt.Exakt,
) : MathematischesObjekt {
    init {
        require(stufen.map { it.dimension } == stufen.map { it.dimension }.distinct().sorted()) {
            "Zellstufen müssen eindeutig und aufsteigend geordnet sein."
        }
        val alleZellen = stufen.flatMap { it.zellen }
        require(alleZellen.map { it.id }.distinct().size == alleZellen.size) { "Zell-IDs müssen eindeutig sein." }
        val nachId = alleZellen.associateBy { it.id }
        alleZellen.forEach { zelle ->
            zelle.geometrie?.let { require(it.raum == raum) { "Zellgeometrien müssen im Strukturraum liegen." } }
            zelle.rand.forEach { ref ->
                val randZelle = requireNotNull(nachId[ref.zellId]) { "Unbekannte Randzelle ${ref.zellId}." }
                require(randZelle.dimension == zelle.dimension - 1) { "Der Rand einer k-Zelle muss aus (k-1)-Zellen bestehen." }
            }
        }
    }

    val dimension: Int get() = stufen.maxOfOrNull { it.dimension } ?: 0
    override fun zuLatex(): String = "\\mathcal{C}_{${dimension}}(${raum.id})"
}

/** Intrinsische Trägermenge geometrischer Punkte; das geometrische Objekt selbst bleibt ausdrücklich keine Menge. */
data class GeometrischeTrägermenge(
    val objekt: GeometrischerAusdruck,
) : MengenAusdruck {
    override fun zuLatex(): String = "\\operatorname{Träger}\\left(${objekt.zuLatex()}\\right)"
}

/** Analytisches Koordinatenbild als Menge von Zahlentupeln. */
data class KoordinatenBild(
    val objekt: GeometrischerAusdruck,
    val koordinatensystem: GeometrischesKoordinatensystem,
) : MengenAusdruck {
    init { require(objekt.raum == koordinatensystem.raum) { "Objekt und Koordinatensystem müssen zum selben Raum gehören." } }
    override fun zuLatex(): String = "\\kappa_{${koordinatensystem.name}}\\left(${objekt.zuLatex()}\\right)"
}

fun gemeinsamerRaum(vararg objekte: GeometrischerAusdruck): EuklidischerRaum {
    require(objekte.isNotEmpty())
    val raum = objekte.first().raum
    require(objekte.all { it.raum == raum }) { "Geometrische Objekte müssen im selben Raum liegen." }
    return raum
}

fun gemeinsamerRaum(vararg punkte: GeometriePunkt): EuklidischerRaum = gemeinsamerRaum(*punkte.map { it as GeometrischerAusdruck }.toTypedArray())

fun GeometriePunkt.zahlKoordinaten(): List<ZahlAusdruck>? = koordinaten?.elemente?.map { it as? ZahlAusdruck ?: return null }

fun GeometriePunkt.dezimalKoordinaten(): List<Double>? = zahlKoordinaten()?.map { zahl ->
    val vereinfacht = vereinfache(zahl)
    (vereinfacht as? RationaleZahl)?.zuDezimal()?.toDouble() ?: return null
}

fun GeometrischerAusdruck.objektDimension(): Int = when (this) {
    is GeometriePunkt -> 0
    is GeometrieGerade, is GeometrieStrecke, is GeometrieStrahl, is GeometrieKreislinie -> 1
    is GeometrieEbene, is GeometriePolygon -> 2
    is GeometrieGruppe -> objekte.maxOf { it.objektDimension() }
    is TransformiertesGeometrieObjekt -> struktur.dimension
    else -> raum.dimension
}

fun strukturVon(objekt: GeometrischerAusdruck): GeometrieStruktur = when (objekt) {
    is GeometriePunkt -> GeometrieStruktur(objekt.raum, listOf(ZellStufe(0, listOf(GeometrischeZelle("p", 0, geometrie = objekt)))))
    is GeometrieStrecke -> strukturAusStrecke(objekt)
    is GeometriePolygon -> strukturAusPolygon(objekt)
    is GeometrieGruppe -> strukturAusGruppe(objekt)
    is TransformiertesGeometrieObjekt -> objekt.struktur
    else -> GeometrieStruktur(
        objekt.raum,
        listOf(ZellStufe(objekt.objektDimension(), listOf(GeometrischeZelle("z0", objekt.objektDimension(), geometrie = objekt)))),
        StrukturDarstellungsArt.Symbolisch,
    )
}

private fun strukturAusStrecke(strecke: GeometrieStrecke): GeometrieStruktur {
    val a = GeometrischeZelle("p0", 0, geometrie = strecke.anfang)
    val b = GeometrischeZelle("p1", 0, geometrie = strecke.ende)
    val kante = GeometrischeZelle("e0", 1, listOf(OrientierterZellVerweis(a.id), OrientierterZellVerweis(b.id, -1)), strecke)
    return GeometrieStruktur(strecke.raum, listOf(ZellStufe(0, listOf(a, b)), ZellStufe(1, listOf(kante))))
}

private fun strukturAusPolygon(polygon: GeometriePolygon): GeometrieStruktur {
    val punkte = polygon.ecken.mapIndexed { index, punkt -> GeometrischeZelle("p$index", 0, geometrie = punkt) }
    val kanten = polygon.ecken.indices.map { index ->
        val nächster = (index + 1) % polygon.ecken.size
        GeometrischeZelle(
            "e$index",
            1,
            listOf(OrientierterZellVerweis("p$index"), OrientierterZellVerweis("p$nächster", -1)),
            GeometrieStrecke(polygon.ecken[index], polygon.ecken[nächster]),
        )
    }
    val fläche = GeometrischeZelle("f0", 2, kanten.map { OrientierterZellVerweis(it.id) }, polygon)
    return GeometrieStruktur(polygon.raum, listOf(ZellStufe(0, punkte), ZellStufe(1, kanten), ZellStufe(2, listOf(fläche))))
}

private fun strukturAusGruppe(gruppe: GeometrieGruppe): GeometrieStruktur {
    val teilStrukturen = gruppe.objekte.mapIndexed { index, objekt -> index to strukturVon(objekt) }
    val stufen = teilStrukturen.flatMap { (index, struktur) ->
        struktur.stufen.map { stufe ->
            ZellStufe(stufe.dimension, stufe.zellen.map { zelle ->
                val präfix = "g${index}_"
                zelle.copy(
                    id = präfix + zelle.id,
                    rand = zelle.rand.map { it.copy(zellId = präfix + it.zellId) },
                )
            })
        }
    }.groupBy { it.dimension }.toSortedMap().map { (dimension, gleicheStufe) ->
        ZellStufe(dimension, gleicheStufe.flatMap { it.zellen })
    }
    return GeometrieStruktur(gruppe.raum, stufen, if (teilStrukturen.all { it.second.darstellungsArt == StrukturDarstellungsArt.Exakt }) StrukturDarstellungsArt.Exakt else StrukturDarstellungsArt.Symbolisch)
}

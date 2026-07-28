package de.TeutonStudio.MathematikRechenSystem.kern

interface GeometrischeTransformation : MathematischesObjekt {
    val quellRaum: EuklidischerRaum
    val zielRaum: EuklidischerRaum
    fun transformiere(koordinaten: Tupel): Tupel
}

data class LineareGeometrieTransformation(
    val matrix: Matrix,
    override val quellRaum: EuklidischerRaum,
    override val zielRaum: EuklidischerRaum,
) : GeometrischeTransformation {
    init { prüfeDimensionen(matrix, quellRaum, zielRaum) }
    override fun transformiere(koordinaten: Tupel) = transformierePunkt(koordinaten, matrix)
    override fun zuLatex() = "T(x)=${matrix.zuLatex()}x"
}

data class AffineGeometrieTransformation(
    val matrix: Matrix,
    val verschiebung: Tupel,
    override val quellRaum: EuklidischerRaum,
    override val zielRaum: EuklidischerRaum,
) : GeometrischeTransformation {
    init {
        prüfeDimensionen(matrix, quellRaum, zielRaum)
        require(verschiebung.elemente.size == zielRaum.dimension)
        verschiebung.alsSpaltenVektor()
    }
    override fun transformiere(koordinaten: Tupel): Tupel {
        val linear = (matrix * koordinaten.alsSpaltenVektor()).werte
        val translation = verschiebung.alsSpaltenVektor().werte
        return Tupel(linear.zip(translation) { a, b -> vereinfache(addition(a, b)) })
    }
    override fun zuLatex() = "T(x)=${matrix.zuLatex()}x+${verschiebung.zuLatex()}"
}

private fun prüfeDimensionen(matrix: Matrix, quelle: EuklidischerRaum, ziel: EuklidischerRaum) {
    require(matrix.spaltenAnzahl == quelle.dimension) { "Die Matrixspalten müssen zur Quelldimension passen." }
    require(matrix.zeilenAnzahl == ziel.dimension) { "Die Matrixzeilen müssen zur Zieldimension passen." }
}

fun transformierePunkt(koordinaten: Tupel, matrix: Matrix): Tupel =
    (matrix * koordinaten.alsSpaltenVektor()).alsTupel()

fun transformierePunkt(punkt: GeometriePunkt, transformation: GeometrischeTransformation): GeometriePunkt {
    require(punkt.raum == transformation.quellRaum)
    val koordinaten = punkt.koordinaten ?: error("Ein freier Punkt besitzt keine transformierbaren Koordinaten.")
    return GeometriePunkt("${punkt.name}'", transformation.zielRaum, transformation.transformiere(koordinaten))
}

fun transformiereGeometrie(objekt: GeometrischerAusdruck, transformation: GeometrischeTransformation): GeometrischerAusdruck {
    require(objekt.raum == transformation.quellRaum)
    fun p(wert: GeometriePunkt) = transformierePunkt(wert, transformation)
    return when (objekt) {
        is GeometriePunkt -> p(objekt)
        is GeometrieGerade -> GeometrieGerade(p(objekt.a), p(objekt.b))
        is GeometrieEbene -> GeometrieEbene(p(objekt.a), p(objekt.b), p(objekt.c))
        is GeometrieStrecke -> GeometrieStrecke(p(objekt.anfang), p(objekt.ende))
        is GeometrieStrahl -> GeometrieStrahl(p(objekt.ursprung), p(objekt.richtungsPunkt))
        is GeometrieWinkel -> GeometrieWinkel(p(objekt.a), p(objekt.scheitel), p(objekt.c), objekt.orientiert)
        is GeometriePolygon -> GeometriePolygon(objekt.ecken.map(::p))
        is GeometrieGruppe -> GeometrieGruppe(objekt.objekte.map { transformiereGeometrie(it, transformation) })
        else -> TransformiertesGeometrieObjekt(
            objekt,
            transformiereStruktur(strukturVon(objekt), transformation),
            transformation.zielRaum,
        )
    }
}

fun transformiereStruktur(struktur: GeometrieStruktur, transformation: GeometrischeTransformation): GeometrieStruktur {
    require(struktur.raum == transformation.quellRaum)
    val stufen = struktur.stufen.map { stufe ->
        ZellStufe(stufe.dimension, stufe.zellen.map { zelle ->
            zelle.copy(geometrie = (zelle.geometrie as? GeometriePunkt)?.let { transformierePunkt(it, transformation) })
        })
    }
    return GeometrieStruktur(transformation.zielRaum, stufen, struktur.darstellungsArt)
}

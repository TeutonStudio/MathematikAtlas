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
    init {
        require(matrix.spaltenAnzahl == quellRaum.dimension) { "Die Matrixspalten müssen zur Quelldimension passen." }
        require(matrix.zeilenAnzahl == zielRaum.dimension) { "Die Matrixzeilen müssen zur Zieldimension passen." }
    }

    override fun transformiere(koordinaten: Tupel): Tupel = transformierePunkt(koordinaten, matrix)
    override fun zuLatex(): String = "T(x)=${matrix.zuLatex()}x"
}

data class AffineGeometrieTransformation(
    val matrix: Matrix,
    val verschiebung: Tupel,
    override val quellRaum: EuklidischerRaum,
    override val zielRaum: EuklidischerRaum,
) : GeometrischeTransformation {
    init {
        require(matrix.spaltenAnzahl == quellRaum.dimension) { "Die Matrixspalten müssen zur Quelldimension passen." }
        require(matrix.zeilenAnzahl == zielRaum.dimension) { "Die Matrixzeilen müssen zur Zieldimension passen." }
        require(verschiebung.elemente.size == zielRaum.dimension) { "Die Verschiebung muss im Zielraum liegen." }
        verschiebung.alsSpaltenVektor()
    }

    override fun transformiere(koordinaten: Tupel): Tupel {
        val linear = (matrix * koordinaten.alsSpaltenVektor()).werte
        val translation = verschiebung.alsSpaltenVektor().werte
        return Tupel(linear.zip(translation) { a, b -> vereinfache(addition(a, b)) })
    }

    override fun zuLatex(): String = "T(x)=${matrix.zuLatex()}x+${verschiebung.zuLatex()}"
}

fun transformierePunkt(koordinaten: Tupel, matrix: Matrix): Tupel = (matrix * koordinaten.alsSpaltenVektor()).alsTupel()

fun transformierePunkt(punkt: GeometriePunkt, transformation: GeometrischeTransformation): GeometriePunkt {
    require(punkt.raum == transformation.quellRaum) { "Der Punkt liegt nicht im Quellraum der Transformation." }
    val koordinaten = punkt.koordinaten ?: error("Ein freier Punkt besitzt keine transformierbaren Koordinaten.")
    return GeometriePunkt("${punkt.name}'", transformation.zielRaum, transformation.transformiere(koordinaten))
}

fun transformiereGeometrie(
    objekt: GeometrischerAusdruck,
    transformation: GeometrischeTransformation,
): GeometrischerAusdruck {
    require(objekt.raum == transformation.quellRaum) { "Das Objekt liegt nicht im Quellraum der Transformation." }
    return when (objekt) {
        is GeometriePunkt -> transformierePunkt(objekt, transformation)
        is GeometrieGerade -> GeometrieGerade(transformierePunkt(objekt.a, transformation), transformierePunkt(objekt.b, transformation))
        is GeometrieEbene -> GeometrieEbene(
            transformierePunkt(objekt.a, transformation),
            transformierePunkt(objekt.b, transformation),
            transformierePunkt(objekt.c, transformation),
        )
        is GeometrieStrecke -> GeometrieStrecke(transformierePunkt(objekt.anfang, transformation), transformierePunkt(objekt.ende, transformation))
        is GeometrieStrahl -> GeometrieStrahl(transformierePunkt(objekt.ursprung, transformation), transformierePunkt(objekt.richtungsPunkt, transformation))
        is GeometrieWinkel -> GeometrieWinkel(
            transformierePunkt(objekt.a, transformation),
            transformierePunkt(objekt.scheitel, transformation),
            transformierePunkt(objekt.c, transformation),
            objekt.orientiert,
        )
        is GeometriePolygon -> GeometriePolygon(objekt.ecken.map { transformierePunkt(it, transformation) })
        is GeometrieGruppe -> GeometrieGruppe(objekt.objekte.map { transformiereGeometrie(it, transformation) })
        is GeometrieKreislinie -> {
            val struktur = transformiereStruktur(strukturVon(objekt), transformation)
            TransformiertesGeometrieObjekt(objekt, struktur, transformation.zielRaum)
        }
        is TransformiertesGeometrieObjekt -> TransformiertesGeometrieObjekt(
            objekt,
            transformiereStruktur(objekt.struktur, transformation),
            transformation.zielRaum,
        )
        else -> TransformiertesGeometrieObjekt(
            objekt,
            transformiereStruktur(strukturVon(objekt), transformation),
            transformation.zielRaum,
        )
    }
}

fun transformiereStruktur(
    struktur: GeometrieStruktur,
    transformation: GeometrischeTransformation,
): GeometrieStruktur {
    require(struktur.raum == transformation.quellRaum)
    val stufen = struktur.stufen.map { stufe ->
        ZellStufe(stufe.dimension, stufe.zellen.map { zelle ->
            val geometrie = when (val teil = zelle.geometrie) {
                null -> null
                is GeometriePunkt -> transformierePunkt(teil, transformation)
                else -> runCatching { transformiereGeometrie(teil, transformation) }.getOrNull()
            }
            zelle.copy(geometrie = geometrie)
        })
    }
    return GeometrieStruktur(transformation.zielRaum, stufen, struktur.darstellungsArt)
}

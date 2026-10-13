package de.TeutonStudio.MathematikRechenSystem.kern

/** Explizite Realisierung einer strukturierten Koordinatenmenge im euklidischen Raum. */
data class GeometrischeMengenRealisierung(val menge: MengenAusdruck, override val raum: EuklidischerRaum) : GeometrischerAusdruck {
    override fun zuLatex() = "\\kappa^{-1}\\left(${menge.zuLatex()}\\right)"
}

/** Translation bewahrt die vollständige Menge, auch bei symbolischer Geometrie. */
data class VerschobeneKoordinatenMenge(val original: MengenAusdruck, val verschiebung: List<ZahlAusdruck>) : MengenAusdruck {
    override fun zuLatex() = "${Tupel(verschiebung).zuLatex()}+${original.zuLatex()}"
}

fun geometrieKoordinatenMenge(objekt: GeometrischerAusdruck, system: GeometrischesKoordinatensystem = GeometrischesKoordinatensystem(objekt.raum)): MengenAusdruck {
    require(system.raum == objekt.raum) { "Objekt und Koordinatensystem gehören zu verschiedenen Räumen." }
    geometrieBekannteKoordinaten(objekt).forEach { require(ElementBeziehung(it, ReelleZahlen).entscheide().wahrheitswert != Wahrheitswert.Lüge && it !is KomplexeZahl) { "Geometrische Koordinaten müssen reell sein." } }
    return when (objekt) {
        is GeometriePunkt -> objekt.koordinaten?.let { EndlicheMenge(setOf(it)) } ?: KoordinatenBild(objekt, system)
        is GeometrieGruppe -> Vereinigung(objekt.objekte.map { geometrieKoordinatenMenge(it, system) })
        is GeometrischeMengenRealisierung -> objekt.menge
        is GeometrieGerade -> geometrischerSpann(listOf(objekt.a, objekt.b)) ?: KoordinatenBild(objekt, system)
        is GeometrieEbene -> geometrischerSpann(listOf(objekt.a, objekt.b, objekt.c)) ?: KoordinatenBild(objekt, system)
        else -> KoordinatenBild(objekt, system)
    }
}

private fun geometrischerSpann(punkte: List<GeometriePunkt>): UnterraumMenge? {
    val c = punkte.first().zahlKoordinaten() ?: return null
    val a = punkte.drop(1).map { p -> p.zahlKoordinaten()?.zip(c) { x, y -> vereinfache(subtraktion(x, y)) } ?: return null }
    return UnterraumMenge(a, c, c.size)
}

fun geometrieMittelpunkt(objekt: GeometrischerAusdruck): Tupel = when (objekt) {
    is GeometriePunkt -> objekt.koordinaten ?: error("Punktkoordinaten sind unbekannt.")
    is GeometrieKreislinie -> objekt.mittelpunkt.koordinaten ?: error("Mittelpunktkoordinaten sind unbekannt.")
    is GeometrieStrecke -> {
        val a = objekt.anfang.zahlKoordinaten() ?: error("Streckenkoordinaten sind unbekannt.")
        val b = objekt.ende.zahlKoordinaten() ?: error("Streckenkoordinaten sind unbekannt.")
        Tupel(a.zip(b) { x, y -> vereinfache(Division(addition(x, y), RationaleZahl.von(2))) })
    }
    is GeometriePolygon -> polygonSchwerpunkt(objekt)
    is GeometrieDreieck -> polygonSchwerpunkt(objekt.polygon)
    else -> error("Dieses Objekt besitzt keinen eindeutig definierten Mittelpunkt.")
}

private fun polygonSchwerpunkt(polygon: GeometriePolygon): Tupel {
    val p = polygon.ecken.map { it.zahlKoordinaten()?.map { x -> vereinfache(x) as? RationaleZahl ?: error("Polygongeometrie ist nicht exakt entscheidbar.") } ?: error("Polygonkoordinaten fehlen.") }
    require(p.distinct().size == p.size) { "Wiederholte Polygonecken sind nicht zulässig." }
    val richtungen = p.drop(1).map { row -> row.zip(p.first()) { x, y -> vereinfache(subtraktion(x,y)) } }
    require(UnterraumMenge(richtungen, p.first(), polygon.raum.dimension).rangOderNull() == 2) { "Polygon muss eben und nicht entartet sein." }
    val achsen = (0 until polygon.raum.dimension).firstNotNullOfOrNull { x -> (x+1 until polygon.raum.dimension).firstNotNullOfOrNull { y ->
        val minorNichtnull = richtungen.indices.any { i -> (i+1 until richtungen.size).any { j -> vereinfache(subtraktion(multiplikation(richtungen[i][x],richtungen[j][y]),multiplikation(richtungen[i][y],richtungen[j][x]))) != RationaleZahl.Null } }
        if (minorNichtnull) x to y else null
    } } ?: error("Polygon besitzt keine Flächenprojektion.")
    val relativ = p.map { row -> row.zip(p.first()) { x,y -> vereinfache(subtraktion(x,y)) as RationaleZahl } }
    val proj = relativ.map { listOf(it[achsen.first],it[achsen.second]) }
    fun kreuz(a: List<RationaleZahl>, b: List<RationaleZahl>, c: List<RationaleZahl>): RationaleZahl = vereinfache(subtraktion(
        multiplikation(subtraktion(b[0],a[0]),subtraktion(c[1],a[1])), multiplikation(subtraktion(b[1],a[1]),subtraktion(c[0],a[0])))) as RationaleZahl
    for (i in p.indices) for (j in i+1 until p.size) {
        if (j == i+1 || i == 0 && j == p.lastIndex) continue
        val a=proj[i];val b=proj[(i+1)%p.size];val c=proj[j];val d=proj[(j+1)%p.size]
        val x=kreuz(a,b,c); val y=kreuz(a,b,d);val z=kreuz(c,d,a);val w=kreuz(c,d,b)
        fun aufSegment(a: List<RationaleZahl>, b: List<RationaleZahl>, x: List<RationaleZahl>) = (0..1).all { axis -> x[axis] >= minOf(a[axis],b[axis]) && x[axis] <= maxOf(a[axis],b[axis]) }
        val kreuzt = x.zähler.signum()*y.zähler.signum() < 0 && z.zähler.signum()*w.zähler.signum() < 0 ||
            x == RationaleZahl.Null && aufSegment(a,b,c) || y == RationaleZahl.Null && aufSegment(a,b,d) ||
            z == RationaleZahl.Null && aufSegment(c,d,a) || w == RationaleZahl.Null && aufSegment(c,d,b)
        require(!kreuzt) { "Polygon ist nicht einfach." }
    }
    val quer = p.indices.map { i -> val a=proj[i];val b=proj[(i+1)%p.size]; vereinfache(subtraktion(multiplikation(a[0],b[1]),multiplikation(b[0],a[1]))) as RationaleZahl }
    val flaeche = vereinfache(addition(quer)) as RationaleZahl
    require(flaeche != RationaleZahl.Null) { "Entartetes Polygon besitzt keinen Flächenschwerpunkt." }
    return Tupel(List(polygon.raum.dimension) { axis -> vereinfache(addition(p.first()[axis], Division(addition(relativ.indices.map { i -> multiplikation(addition(relativ[i][axis],relativ[(i+1)%p.size][axis]),quer[i]) }),multiplikation(RationaleZahl.von(3),flaeche)))) })
}

fun koordinatenMengenMitgliedschaft(element: MathematischesObjekt, bild: KoordinatenBild, kontext: RechenKontext): AussageErgebnis {
    val t = element as? Tupel ?: return AussageErgebnis(Wahrheitswert.Lüge, EntscheidungsStatus.Widerlegt)
    if (t.elemente.size != bild.objekt.raum.dimension || t.elemente.any { it !is ZahlAusdruck }) return AussageErgebnis(Wahrheitswert.Lüge, EntscheidungsStatus.Widerlegt)
    val reell = t.elemente.map { ElementBeziehung(it, ReelleZahlen).entscheide(kontext).wahrheitswert }
    if (reell.any { it == Wahrheitswert.Lüge }) return AussageErgebnis(Wahrheitswert.Lüge, EntscheidungsStatus.Widerlegt)
    if (reell.any { it == null }) return AussageErgebnis(null, EntscheidungsStatus.Unbekannt, "Reellheit der Koordinaten ist nicht nachgewiesen.")
    if (bild.objekt is GeometrieKreislinie && bild.objekt.raum.dimension > 2) return AussageErgebnis(null, EntscheidungsStatus.Unbekannt, "Die definierende Kreisebene fehlt.")
    return GeometrischeInzidenz(GeometriePunkt("x", bild.objekt.raum, t), bild.objekt).entscheide(kontext)
}

fun geometrieBekannteKoordinaten(objekt: GeometrischerAusdruck): List<ZahlAusdruck> = when (objekt) {
    is GeometriePunkt -> objekt.zahlKoordinaten().orEmpty()
    is GeometrieGerade -> listOf(objekt.a,objekt.b).flatMap { it.zahlKoordinaten().orEmpty() }
    is GeometrieEbene -> listOf(objekt.a,objekt.b,objekt.c).flatMap { it.zahlKoordinaten().orEmpty() }
    is GeometrieStrecke -> listOf(objekt.anfang,objekt.ende).flatMap { it.zahlKoordinaten().orEmpty() }
    is GeometrieStrahl -> listOf(objekt.ursprung,objekt.richtungsPunkt).flatMap { it.zahlKoordinaten().orEmpty() }
    is GeometrieKreislinie -> listOf(objekt.mittelpunkt,objekt.randpunkt).flatMap { it.zahlKoordinaten().orEmpty() }
    is GeometriePolygon -> objekt.ecken.flatMap { it.zahlKoordinaten().orEmpty() }
    is GeometrieDreieck -> geometrieBekannteKoordinaten(objekt.polygon)
    is GeometrieWinkel -> listOf(objekt.a,objekt.scheitel,objekt.c).flatMap { it.zahlKoordinaten().orEmpty() }
    is GeometrieGruppe -> objekt.objekte.flatMap(::geometrieBekannteKoordinaten)
    is TransformiertesGeometrieObjekt -> geometrieBekannteKoordinaten(objekt.original)
    else -> emptyList()
}

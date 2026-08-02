package de.TeutonStudio.MathematikRechenSystem.kern

/** Geometrische Koinzidenz bleibt von der strukturellen Kotlin-Gleichheit getrennt. */
data class GeometrischeGleichheit(
    val links: GeometrischerAusdruck,
    val rechts: GeometrischerAusdruck,
) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis {
        if (links.raum != rechts.raum) return falsch("Die Objekte liegen in verschiedenen Räumen.")
        return geometrischGleich(links, rechts)?.let(::ergebnis) ?: unbekannt()
    }

    override fun zuLatex(): String = "${links.zuLatex()} \\equiv_G ${rechts.zuLatex()}"
}

data class GeometrischeInzidenz(
    val punkt: GeometriePunkt,
    val objekt: GeometrischerAusdruck,
) : Aussage {
    init { require(punkt.raum == objekt.raum) { "Punkt und Objekt müssen im selben Raum liegen." } }

    override fun entscheide(kontext: RechenKontext): AussageErgebnis = when (objekt) {
        is GeometriePunkt -> geometrischGleich(punkt, objekt)?.let(::ergebnis) ?: unbekannt()
        is GeometrieGerade -> kollinear(objekt.a, punkt, objekt.b)?.let(::ergebnis) ?: unbekannt()
        is GeometrieStrecke -> {
            val endpoint = geometrischGleich(punkt, objekt.anfang) == true || geometrischGleich(punkt, objekt.ende) == true
            if (endpoint) wahr("Der Punkt ist Endpunkt der Strecke.")
            else zwischen(objekt.anfang, punkt, objekt.ende)?.let(::ergebnis) ?: unbekannt()
        }
        is GeometrieStrahl -> strahlEnthält(objekt, punkt)?.let(::ergebnis) ?: unbekannt()
        is GeometrieKreislinie -> {
            val radius = abstandQuadrat(objekt.mittelpunkt, objekt.randpunkt)
            val distanz = abstandQuadrat(objekt.mittelpunkt, punkt)
            if (radius != null && distanz != null) ergebnis(radius == distanz) else unbekannt()
        }
        is GeometriePolygon -> if (objekt.ecken.any { geometrischGleich(it, punkt) == true }) wahr("Der Punkt ist eine Polygonecke.") else unbekannt()
        is GeometrieDreieck -> GeometrischeInzidenz(punkt, objekt.polygon).entscheide(kontext)
        is GeometrieGruppe -> {
            val werte = objekt.objekte.map { GeometrischeInzidenz(punkt, it).entscheide(kontext).wahrheitswert }
            when {
                Wahrheitswert.Wahr in werte -> wahr()
                werte.all { it == Wahrheitswert.Lüge } -> falsch()
                else -> unbekannt()
            }
        }
        is TransformiertesGeometrieObjekt -> unbekannt("Die Inzidenz des allgemeinen Transformationsbilds ist symbolisch.")
        else -> unbekannt()
    }

    override fun zuLatex(): String = "${punkt.zuLatex()} \\mathrel{\\mathbf{I}} ${objekt.zuLatex()}"
}

data class Zwischenlage(
    val a: GeometriePunkt,
    val b: GeometriePunkt,
    val c: GeometriePunkt,
) : Aussage {
    init { gemeinsamerRaum(a, b, c) }
    override fun entscheide(kontext: RechenKontext): AussageErgebnis = zwischen(a, b, c)?.let(::ergebnis) ?: unbekannt()
    override fun zuLatex(): String = "\\operatorname{Zwischen}(${a.name},${b.name},${c.name})"
}

data class Kollinearität(
    val punkte: List<GeometriePunkt>,
) : Aussage {
    init {
        require(punkte.size >= 2)
        gemeinsamerRaum(*punkte.toTypedArray())
    }

    override fun entscheide(kontext: RechenKontext): AussageErgebnis {
        if (punkte.size == 2) return wahr("Zwei Punkte liegen immer auf mindestens einer gemeinsamen Geraden.")
        val a = punkte[0]
        val b = punkte[1]
        val werte = punkte.drop(2).map { kollinear(a, b, it) }
        return when {
            werte.any { it == false } -> falsch()
            werte.all { it == true } -> wahr()
            else -> unbekannt()
        }
    }

    override fun zuLatex(): String = punkte.joinToString(prefix = "\\operatorname{kollinear}(", postfix = ")") { it.name }
}

data class GeometrischeParallelität(
    val links: GeometrieGerade,
    val rechts: GeometrieGerade,
    val identischeGeradenZulassen: Boolean = false,
) : Aussage {
    init { require(links.raum == rechts.raum) }

    override fun entscheide(kontext: RechenKontext): AussageErgebnis {
        val identisch = geometrischGleich(links, rechts)
        if (identisch == true) return ergebnis(identischeGeradenZulassen)
        return parallel(links, rechts)?.let(::ergebnis) ?: unbekannt()
    }

    override fun zuLatex(): String = "${links.zuLatex()} \\parallel ${rechts.zuLatex()}"
}

data class GeometrischeOrthogonalität(
    val links: GeometrieGerade,
    val rechts: GeometrieGerade,
) : Aussage {
    init { require(links.raum == rechts.raum) }
    override fun entscheide(kontext: RechenKontext): AussageErgebnis = orthogonal(links, rechts)?.let(::ergebnis) ?: unbekannt()
    override fun zuLatex(): String = "${links.zuLatex()} \\perp ${rechts.zuLatex()}"
}

data class StreckenKongruenz(
    val links: GeometrieStrecke,
    val rechts: GeometrieStrecke,
) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis {
        if (links.raum != rechts.raum) return falsch()
        val a = abstandQuadrat(links.anfang, links.ende)
        val b = abstandQuadrat(rechts.anfang, rechts.ende)
        return if (a != null && b != null) ergebnis(a == b) else if (links == rechts) wahr() else unbekannt()
    }

    override fun zuLatex(): String = "${links.zuLatex()} \\cong ${rechts.zuLatex()}"
}

data class WinkelKongruenz(
    val links: GeometrieWinkel,
    val rechts: GeometrieWinkel,
) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis = if (links == rechts) wahr() else unbekannt()
    override fun zuLatex(): String = "${links.zuLatex()} \\cong ${rechts.zuLatex()}"
}

fun geometrischGleich(links: GeometrischerAusdruck, rechts: GeometrischerAusdruck): Boolean? {
    if (links.raum != rechts.raum) return false
    if (links == rechts) return true
    return when {
        links is GeometriePunkt && rechts is GeometriePunkt -> {
            val l = rationaleKoordinaten(links)
            val r = rationaleKoordinaten(rechts)
            if (l != null && r != null) l == r else null
        }
        links is GeometrieGerade && rechts is GeometrieGerade ->
            gleichePunktPaare(links.a, links.b, rechts.a, rechts.b) ||
                (kollinear(links.a, links.b, rechts.a) == true && kollinear(links.a, links.b, rechts.b) == true)
        links is GeometrieStrecke && rechts is GeometrieStrecke -> gleichePunktPaare(links.anfang, links.ende, rechts.anfang, rechts.ende)
        links is GeometrieStrahl && rechts is GeometrieStrahl ->
            geometrischGleich(links.ursprung, rechts.ursprung) == true &&
                strahlEnthält(links, rechts.richtungsPunkt) == true && strahlEnthält(rechts, links.richtungsPunkt) == true
        links is GeometrieKreislinie && rechts is GeometrieKreislinie -> {
            val gleicheMitte = geometrischGleich(links.mittelpunkt, rechts.mittelpunkt)
            val l = abstandQuadrat(links.mittelpunkt, links.randpunkt)
            val r = abstandQuadrat(rechts.mittelpunkt, rechts.randpunkt)
            if (gleicheMitte == false) false else if (gleicheMitte == true && l != null && r != null) l == r else null
        }
        links is GeometriePolygon && rechts is GeometriePolygon -> links.ecken == rechts.ecken
        links is GeometrieDreieck && rechts is GeometrieDreieck -> geometrischGleich(links.polygon, rechts.polygon)
        else -> null
    }
}

private fun gleichePunktPaare(a: GeometriePunkt, b: GeometriePunkt, c: GeometriePunkt, d: GeometriePunkt): Boolean =
    geometrischGleich(a, c) == true && geometrischGleich(b, d) == true || geometrischGleich(a, d) == true && geometrischGleich(b, c) == true

private fun rationaleKoordinaten(punkt: GeometriePunkt): List<RationaleZahl>? = punkt.zahlKoordinaten()?.map { wert ->
    vereinfache(wert) as? RationaleZahl ?: return null
}

private fun richtung(von: GeometriePunkt, nach: GeometriePunkt): List<RationaleZahl>? {
    val a = rationaleKoordinaten(von) ?: return null
    val b = rationaleKoordinaten(nach) ?: return null
    return a.zip(b) { x, y -> y - x }
}

private fun skalarprodukt(a: List<RationaleZahl>, b: List<RationaleZahl>): RationaleZahl =
    a.zip(b) { x, y -> x * y }.fold(RationaleZahl.Null, RationaleZahl::plus)

private fun abstandQuadrat(a: GeometriePunkt, b: GeometriePunkt): RationaleZahl? {
    val d = richtung(a, b) ?: return null
    return skalarprodukt(d, d)
}

private fun kollinear(a: GeometriePunkt, b: GeometriePunkt, c: GeometriePunkt): Boolean? {
    val u = richtung(a, b) ?: return null
    val v = richtung(a, c) ?: return null
    return when (u.size) {
        1 -> true
        2 -> u[0] * v[1] - u[1] * v[0] == RationaleZahl.Null
        3 -> listOf(
            u[1] * v[2] - u[2] * v[1],
            u[2] * v[0] - u[0] * v[2],
            u[0] * v[1] - u[1] * v[0],
        ).all(RationaleZahl::istNull)
        else -> null
    }
}

private fun zwischen(a: GeometriePunkt, b: GeometriePunkt, c: GeometriePunkt): Boolean? {
    if (kollinear(a, b, c) != true) return false
    val ba = richtung(b, a) ?: return null
    val bc = richtung(b, c) ?: return null
    return skalarprodukt(ba, bc) <= RationaleZahl.Null
}

private fun strahlEnthält(strahl: GeometrieStrahl, punkt: GeometriePunkt): Boolean? {
    if (geometrischGleich(strahl.ursprung, punkt) == true) return true
    if (kollinear(strahl.ursprung, strahl.richtungsPunkt, punkt) != true) return false
    val richtung = richtung(strahl.ursprung, strahl.richtungsPunkt) ?: return null
    val zumPunkt = richtung(strahl.ursprung, punkt) ?: return null
    return skalarprodukt(richtung, zumPunkt) >= RationaleZahl.Null
}

private fun parallel(a: GeometrieGerade, b: GeometrieGerade): Boolean? {
    val u = richtung(a.a, a.b) ?: return null
    val v = richtung(b.a, b.b) ?: return null
    return when (u.size) {
        1 -> true
        2 -> u[0] * v[1] - u[1] * v[0] == RationaleZahl.Null
        3 -> listOf(
            u[1] * v[2] - u[2] * v[1],
            u[2] * v[0] - u[0] * v[2],
            u[0] * v[1] - u[1] * v[0],
        ).all(RationaleZahl::istNull)
        else -> null
    }
}

private fun orthogonal(a: GeometrieGerade, b: GeometrieGerade): Boolean? {
    val u = richtung(a.a, a.b) ?: return null
    val v = richtung(b.a, b.b) ?: return null
    return skalarprodukt(u, v).istNull()
}

private fun ergebnis(wahr: Boolean): AussageErgebnis = if (wahr) wahr() else falsch()
private fun wahr(begründung: String = "") = AussageErgebnis(Wahrheitswert.Wahr, EntscheidungsStatus.Bewiesen, begründung)
private fun falsch(begründung: String = "") = AussageErgebnis(Wahrheitswert.Lüge, EntscheidungsStatus.Widerlegt, begründung)
private fun unbekannt(begründung: String = "") = AussageErgebnis(null, EntscheidungsStatus.Unbekannt, begründung)

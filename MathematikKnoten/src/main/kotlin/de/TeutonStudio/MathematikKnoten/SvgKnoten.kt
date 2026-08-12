package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.TypSystem.AnschlussVertrag
import de.TeutonStudio.TypSystem.TypAusdruck
import kotlin.math.abs

const val SVG_KNOTEN_ART = "grafik.svg.knoten"
const val SVG_STIL_KNOTEN_ART = "grafik.svg.stil"
const val SVG_OPERATOR_PARAMETER = "svgOperator"

data class SvgOperatorDefinition(
    val id: String,
    val titel: String,
    val symbolLatex: String,
    val kategorie: String,
    val beschreibung: String,
    val anschlüsse: List<AnschlussDaten>,
    val standardParameter: Map<String, String> = emptyMap(),
)

object SvgOperatoren {
    val Dokument = SvgOperatorDefinition(
        id = "dokument",
        titel = "SVG-Dokument",
        symbolLatex = "\\mathrm{SVG}",
        kategorie = "Dokument",
        beschreibung = "Erzeugt ohne verbundenen SVG-Eingang ein neues Dokument oder ändert ViewBox und mathematischen Koordinatenraum eines bestehenden SVG.",
        anschlüsse = svgRahmen(),
        standardParameter = mapOf(
            "viewBoxMinX" to "0",
            "viewBoxMinY" to "0",
            "viewBoxBreite" to "1000",
            "viewBoxHöhe" to "1000",
            "xMin" to "-10",
            "xMax" to "10",
            "yMin" to "-10",
            "yMax" to "10",
            "breite" to "100%",
            "höhe" to "100%",
            "preserveAspectRatio" to "xMidYMid meet",
        ),
    )
    val Linie = elementOperator(
        id = "linie",
        titel = "Linie",
        symbol = "\\overline{AB}",
        kategorie = "Grundformen",
        beschreibung = "Ergänzt eine Linie zwischen zwei mathematischen Koordinaten.",
        zahlen = listOf("x1", "y1", "x2", "y2"),
    )
    val Rechteck = elementOperator(
        id = "rechteck",
        titel = "Rechteck",
        symbol = "\\square",
        kategorie = "Grundformen",
        beschreibung = "Ergänzt ein Rechteck im mathematischen Koordinatenraum.",
        zahlen = listOf("x", "y", "breite", "höhe"),
        standardParameter = mapOf("radiusX" to "0", "radiusY" to "0"),
    )
    val Kreis = elementOperator(
        id = "kreis",
        titel = "Kreis",
        symbol = "\\bigcirc",
        kategorie = "Grundformen",
        beschreibung = "Ergänzt einen mathematischen Kreis; bei anisotroper Abbildung wird er korrekt als SVG-Ellipse dargestellt.",
        zahlen = listOf("x", "y", "radius"),
    )
    val Ellipse = elementOperator(
        id = "ellipse",
        titel = "Ellipse",
        symbol = "\\bigcirc",
        kategorie = "Grundformen",
        beschreibung = "Ergänzt eine Ellipse aus Mittelpunkt und zwei Radien.",
        zahlen = listOf("x", "y", "radiusX", "radiusY"),
    )
    val Polygon = elementOperator(
        id = "polygon",
        titel = "Polygon",
        symbol = "\\operatorname{Polygon}",
        kategorie = "Pfade",
        beschreibung = "Ergänzt ein geschlossenes Polygon aus einer geordneten Punktliste.",
        standardParameter = mapOf("punkte" to "-1,-1 1,-1 0,1"),
    )
    val Linienzug = elementOperator(
        id = "linienzug",
        titel = "Linienzug",
        symbol = "\\operatorname{Polyline}",
        kategorie = "Pfade",
        beschreibung = "Ergänzt einen offenen Linienzug aus einer geordneten Punktliste.",
        standardParameter = mapOf("punkte" to "-1,0 0,1 1,0"),
    )
    val Pfad = elementOperator(
        id = "pfad",
        titel = "Pfad",
        symbol = "\\operatorname{Path}",
        kategorie = "Pfade",
        beschreibung = "Ergänzt einen strukturiert geparsten SVG-Pfad. Unterstützt M, L, C, Q, A und Z in mathematischen Koordinaten.",
        standardParameter = mapOf("pfad" to "M -1 0 L 1 0"),
    )
    val Text = elementOperator(
        id = "text",
        titel = "Text / Mathematik",
        symbol = "\\mathrm{Text}",
        kategorie = "Beschriftung",
        beschreibung = "Ergänzt Text oder bewahrt eine mathematische LaTeX-Beschriftung verlustfrei im SVG-AST.",
        zahlen = listOf("x", "y"),
        standardParameter = mapOf("text" to "x", "mathematikLatex" to "true"),
    )
    val Gruppe = elementOperator(
        id = "gruppe",
        titel = "Gruppieren",
        symbol = "\\{\\cdots\\}",
        kategorie = "Struktur",
        beschreibung = "Fasst alle bisher sichtbaren Elemente des eingehenden SVG zu einer Gruppe zusammen.",
    )
    val Kombinieren = SvgOperatorDefinition(
        id = "kombinieren",
        titel = "SVG kombinieren",
        symbolLatex = "\\mathrm{SVG}_1\\cup\\mathrm{SVG}_2",
        kategorie = "Struktur",
        beschreibung = "Führt zwei vollständige SVG-ASTs zusammen. Der erste Eingang bestimmt ViewBox und Koordinatenraum.",
        anschlüsse = listOf(
            svgEingang("svg", 0),
            svgEingang("zweitesSvg", 1),
            svgAusgang(),
        ),
    )

    val alle = listOf(Dokument, Linie, Rechteck, Kreis, Ellipse, Polygon, Linienzug, Pfad, Text, Gruppe, Kombinieren)

    fun finde(id: String?): SvgOperatorDefinition = alle.firstOrNull { it.id == id } ?: Dokument

    private fun elementOperator(
        id: String,
        titel: String,
        symbol: String,
        kategorie: String,
        beschreibung: String,
        zahlen: List<String> = emptyList(),
        standardParameter: Map<String, String> = emptyMap(),
    ) = SvgOperatorDefinition(
        id = id,
        titel = titel,
        symbolLatex = symbol,
        kategorie = kategorie,
        beschreibung = beschreibung,
        anschlüsse = buildList {
            add(svgEingang("svg", 0))
            add(stilEingang(1))
            zahlen.forEachIndexed { index, name -> add(zahlEingang(name, index + 2)) }
            add(svgAusgang())
        },
        standardParameter = standardParameter,
    )

    private fun svgRahmen(): List<AnschlussDaten> = listOf(svgEingang("svg", 0), svgAusgang())
}

object SvgKnotenVorlagen {
    val Svg = KnotenVorlage(
        art = SVG_KNOTEN_ART,
        name = "SVG",
        kategorie = "Grafik: SVG",
        beschreibung = "Einheitlicher SVG-Knoten, der einen vollständigen SVG-AST übernimmt oder bei unbelegtem Eingang neu beginnt und ihn um den ausgewählten Operator erweitert.",
        standardGröße = GraphGröße(300f, 150f),
        anschlüsse = SvgOperatoren.Dokument.anschlüsse,
        standardParameter = SvgOperatoren.Dokument.standardParameter + (SVG_OPERATOR_PARAMETER to SvgOperatoren.Dokument.id),
    )

    val Stil = KnotenVorlage(
        art = SVG_STIL_KNOTEN_ART,
        name = "SVG-Stil",
        kategorie = "Grafik: SVG",
        beschreibung = "Wiederverwendbarer Stil für SVG-Ergänzungsknoten.",
        standardGröße = GraphGröße(240f, 115f),
        anschlüsse = listOf(
            AnschlussDaten(
                name = "stil",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.SvgStil.id,
                vertrag = AnschlussVertrag(TypAusdruck.Atom(MathematischeTypen.SvgStil)),
            ),
        ),
        standardParameter = mapOf(
            "füllung" to "none",
            "kontur" to "currentColor",
            "konturBreite" to "1.5",
            "deckkraft" to "1",
            "strichMuster" to "",
            "linienEnde" to "round",
            "linienVerbindung" to "round",
        ),
    )

    val alle = listOf(Svg, Stil)
}

fun konfiguriereSvgKnoten(knoten: KnotenDaten, operatorId: String): KnotenDaten {
    val operator = SvgOperatoren.finde(operatorId)
    val bisherNachRolle = knoten.anschlüsse.associateBy { it.name to it.richtung }
    val neueAnschlüsse = operator.anschlüsse.map { ziel ->
        bisherNachRolle[ziel.name to ziel.richtung]?.let { alt -> ziel.copy(id = alt.id) } ?: ziel
    }
    val gemeinsameParameter = knoten.parameter.filterKeys { it != SVG_OPERATOR_PARAMETER }
    return knoten.copy(
        name = operator.titel,
        anschlüsse = neueAnschlüsse,
        parameter = gemeinsameParameter + operator.standardParameter + (SVG_OPERATOR_PARAMETER to operator.id),
    )
}

fun MathematikAuswerterRegister.registriereSvgKnoten() {
    registriere(SVG_STIL_KNOTEN_ART) { kontext ->
        val parameter = kontext.knoten.parameter
        val stil = SvgStil(
            füllung = parameter["füllung"].orEmpty().ifBlank { "none" },
            kontur = parameter["kontur"].orEmpty().ifBlank { "currentColor" },
            konturBreite = parameter.double("konturBreite", 1.5),
            deckkraft = parameter.double("deckkraft", 1.0),
            strichMuster = parameter["strichMuster"].orEmpty()
                .split(',', ' ', ';')
                .mapNotNull { it.trim().takeIf(String::isNotEmpty)?.toDoubleOrNull() },
            linienEnde = parameter["linienEnde"].orEmpty().ifBlank { "round" },
            linienVerbindung = parameter["linienVerbindung"].orEmpty().ifBlank { "round" },
        )
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf("stil" to BedingterWert(stil)),
            eingänge = kontext.eingänge,
        )
    }

    registriere(SVG_KNOTEN_ART) { kontext ->
        val operator = SvgOperatoren.finde(kontext.knoten.parameter[SVG_OPERATOR_PARAMETER])
        val erster = kontext.eingänge["svg"]?.objekt as? SvgGrafik
        val zweiter = kontext.eingänge["zweitesSvg"]?.objekt as? SvgGrafik
        val basis = erster ?: if (operator == SvgOperatoren.Kombinieren && zweiter != null) zweiter else SvgGrafik.standard()
        val stil = kontext.eingänge["stil"]?.objekt as? SvgStil ?: SvgStil()
        val id = "svg-${kontext.knoten.id.wert}-${operator.id}".replace(Regex("[^A-Za-z0-9_.:-]"), "-")
        val parameter = kontext.knoten.parameter

        val grafik = when (operator) {
            SvgOperatoren.Dokument -> basis.copy(
                viewport = SvgViewport(
                    minX = parameter.double("viewBoxMinX", basis.viewport.minX),
                    minY = parameter.double("viewBoxMinY", basis.viewport.minY),
                    breite = parameter.double("viewBoxBreite", basis.viewport.breite),
                    höhe = parameter.double("viewBoxHöhe", basis.viewport.höhe),
                ),
                koordinatenraum = SvgKoordinatenraum(
                    xMin = parameter.double("xMin", basis.koordinatenraum.xMin),
                    xMax = parameter.double("xMax", basis.koordinatenraum.xMax),
                    yMin = parameter.double("yMin", basis.koordinatenraum.yMin),
                    yMax = parameter.double("yMax", basis.koordinatenraum.yMax),
                ),
                breite = parameter["breite"].orEmpty().ifBlank { basis.breite },
                höhe = parameter["höhe"].orEmpty().ifBlank { basis.höhe },
                preserveAspectRatio = parameter["preserveAspectRatio"].orEmpty().ifBlank { basis.preserveAspectRatio },
            )
            SvgOperatoren.Linie -> basis.mitElement(
                SvgLinie(
                    id = id,
                    start = basis.mappe(kontext.punkt("x1", "y1")),
                    ende = basis.mappe(kontext.punkt("x2", "y2")),
                    stil = stil,
                ),
            )
            SvgOperatoren.Rechteck -> {
                val x = kontext.zahl("x")
                val y = kontext.zahl("y")
                val breite = kontext.zahl("breite")
                val höhe = kontext.zahl("höhe")
                val obenLinks = basis.mappe(SvgPunkt(x, y + höhe))
                basis.mitElement(
                    SvgRechteck(
                        id = id,
                        position = obenLinks,
                        breite = abs(basis.skaliereX(breite)),
                        höhe = abs(basis.skaliereY(höhe)),
                        radiusX = abs(basis.skaliereX(parameter.double("radiusX", 0.0))),
                        radiusY = abs(basis.skaliereY(parameter.double("radiusY", 0.0))),
                        stil = stil,
                    ),
                )
            }
            SvgOperatoren.Kreis -> {
                val mittelpunkt = basis.mappe(kontext.punkt("x", "y"))
                val radius = abs(kontext.zahl("radius"))
                val rx = abs(basis.skaliereX(radius))
                val ry = abs(basis.skaliereY(radius))
                val element: SvgElement = if (abs(rx - ry) < 1e-9) {
                    SvgKreis(id, mittelpunkt, rx, stil)
                } else {
                    SvgEllipse(id, mittelpunkt, rx, ry, stil)
                }
                basis.mitElement(element)
            }
            SvgOperatoren.Ellipse -> basis.mitElement(
                SvgEllipse(
                    id = id,
                    mittelpunkt = basis.mappe(kontext.punkt("x", "y")),
                    radiusX = abs(basis.skaliereX(kontext.zahl("radiusX"))),
                    radiusY = abs(basis.skaliereY(kontext.zahl("radiusY"))),
                    stil = stil,
                ),
            )
            SvgOperatoren.Polygon -> basis.mitElement(
                SvgPolygon(id, parsePunkte(parameter["punkte"]).map(basis::mappe), stil),
            )
            SvgOperatoren.Linienzug -> basis.mitElement(
                SvgLinienzug(id, parsePunkte(parameter["punkte"]).map(basis::mappe), stil),
            )
            SvgOperatoren.Pfad -> basis.mitElement(
                SvgPfad(id, parsePfad(parameter["pfad"].orEmpty(), basis), stil),
            )
            SvgOperatoren.Text -> basis.mitElement(
                SvgText(
                    id = id,
                    position = basis.mappe(kontext.punkt("x", "y")),
                    inhalt = parameter["text"].orEmpty(),
                    mathematikLatex = parameter["mathematikLatex"]?.toBooleanStrictOrNull() ?: true,
                    stil = stil,
                ),
            )
            SvgOperatoren.Gruppe -> basis.gruppiere(id, stil)
            SvgOperatoren.Kombinieren -> when {
                erster != null && zweiter != null -> erster.kombiniere(zweiter)
                erster != null -> erster
                zweiter != null -> zweiter
                else -> SvgGrafik.standard()
            }
            else -> basis
        }

        val warnungen = if (
            operator == SvgOperatoren.Kombinieren &&
            erster != null && zweiter != null &&
            (erster.viewport != zweiter.viewport || erster.koordinatenraum != zweiter.koordinatenraum)
        ) {
            listOf("Die kombinierten SVGs besitzen unterschiedliche Dokumenträume; der erste SVG-Eingang bestimmt ViewBox und Koordinatenraum des Ergebnisses.")
        } else emptyList()

        KnotenAuswertungsErgebnis(
            ausgaben = mapOf("svg" to BedingterWert(grafik)),
            eingänge = kontext.eingänge,
            warnungen = warnungen,
        )
    }
}

private fun svgEingang(name: String, reihenfolge: Int) = AnschlussDaten(
    name = name,
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = MathematikAnschlussArten.SvgGrafik.id,
    reihenfolge = reihenfolge,
    vertrag = AnschlussVertrag(TypAusdruck.Atom(MathematischeTypen.SvgGrafik)),
)

private fun svgAusgang() = AnschlussDaten(
    name = "svg",
    richtung = AnschlussRichtung.Ausgang,
    kante = AnschlussKante.Rechts,
    art = MathematikAnschlussArten.SvgGrafik.id,
    vertrag = AnschlussVertrag(TypAusdruck.Atom(MathematischeTypen.SvgGrafik)),
)

private fun stilEingang(reihenfolge: Int) = AnschlussDaten(
    name = "stil",
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = MathematikAnschlussArten.SvgStil.id,
    reihenfolge = reihenfolge,
    vertrag = AnschlussVertrag(TypAusdruck.Atom(MathematischeTypen.SvgStil)),
)

private fun zahlEingang(name: String, reihenfolge: Int) = AnschlussDaten(
    name = name,
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = MathematikAnschlussArten.Zahl.id,
    reihenfolge = reihenfolge,
    vertrag = AnschlussVertrag(TypAusdruck.Atom(MathematischeTypen.Zahl)),
)

private fun KnotenAuswertungsKontext.zahl(name: String): Double {
    val objekt = eingänge[name]?.objekt ?: error("SVG-Operator benötigt den Zahleneingang '$name'.")
    return when (objekt) {
        is RationaleZahl -> objekt.zuDezimal(rechenKontext.dezimalstellen).toDouble()
        else -> error("SVG-Operator benötigt an '$name' derzeit eine auswertbare rationale Zahl.")
    }
}

private fun KnotenAuswertungsKontext.punkt(x: String, y: String) = SvgPunkt(zahl(x), zahl(y))

private fun SvgGrafik.mappe(punkt: SvgPunkt): SvgPunkt = koordinatenraum.bildeAb(punkt, viewport)
private fun SvgGrafik.skaliereX(länge: Double): Double = länge / (koordinatenraum.xMax - koordinatenraum.xMin) * viewport.breite
private fun SvgGrafik.skaliereY(länge: Double): Double = länge / (koordinatenraum.yMax - koordinatenraum.yMin) * viewport.höhe

private fun Map<String, String>.double(name: String, fallback: Double): Double =
    get(name)?.trim()?.toDoubleOrNull() ?: fallback

private fun parsePunkte(roh: String?): List<SvgPunkt> {
    val text = roh.orEmpty().trim()
    require(text.isNotBlank()) { "Eine SVG-Punktliste darf nicht leer sein." }
    return text.split(Regex("\\s+")).mapIndexed { index, paar ->
        val teile = paar.split(',')
        require(teile.size == 2) { "Punkt ${index + 1} muss als x,y angegeben werden." }
        SvgPunkt(
            teile[0].toDoubleOrNull() ?: error("Ungültige x-Koordinate in Punkt ${index + 1}."),
            teile[1].toDoubleOrNull() ?: error("Ungültige y-Koordinate in Punkt ${index + 1}."),
        )
    }
}

/** Kleiner, absichtlich strukturierter Pfadparser statt Weiterreichen roher XML-Fragmente. */
private fun parsePfad(roh: String, grafik: SvgGrafik): List<SvgPfadSegment> {
    val tokens = Regex("[MLCQAZ]|[-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][-+]?\\d+)?", RegexOption.IGNORE_CASE)
        .findAll(roh)
        .map { it.value }
        .toList()
    require(tokens.isNotEmpty()) { "Ein SVG-Pfad benötigt mindestens ein Segment." }
    var index = 0
    val segmente = mutableListOf<SvgPfadSegment>()

    fun zahl(): Double {
        require(index < tokens.size) { "Unvollständiges SVG-Pfadsegment." }
        return tokens[index++].toDoubleOrNull() ?: error("Erwartete Zahl im SVG-Pfad.")
    }
    fun punkt(): SvgPunkt = grafik.mappe(SvgPunkt(zahl(), zahl()))

    while (index < tokens.size) {
        val befehl = tokens[index++]
        require(befehl.length == 1 && befehl[0].isLetter()) { "SVG-Pfad erwartet einen Segmentbefehl." }
        when (befehl.uppercase()) {
            "M" -> segmente += SvgPfadSegment.Bewegen(punkt())
            "L" -> segmente += SvgPfadSegment.Linie(punkt())
            "C" -> segmente += SvgPfadSegment.Kubisch(punkt(), punkt(), punkt())
            "Q" -> segmente += SvgPfadSegment.Quadratisch(punkt(), punkt())
            "A" -> {
                val rx = abs(grafik.skaliereX(zahl()))
                val ry = abs(grafik.skaliereY(zahl()))
                val rotation = zahl()
                val groß = zahl() != 0.0
                val uhrzeigersinn = zahl() != 0.0
                segmente += SvgPfadSegment.Bogen(rx, ry, rotation, groß, uhrzeigersinn, punkt())
            }
            "Z" -> segmente += SvgPfadSegment.Schließen
            else -> error("Nicht unterstützter SVG-Pfadbefehl '$befehl'. Unterstützt werden M, L, C, Q, A und Z.")
        }
    }
    return segmente
}

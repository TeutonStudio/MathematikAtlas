package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.TypSystem.AnschlussVertrag
import de.TeutonStudio.TypSystem.TypAusdruck
import kotlin.math.abs

object SvgErweiterteOperatoren {
    val Titel = einfach(
        "titel", "Titel", "\\mathrm{title}", "Metadaten",
        "Setzt den strukturierten SVG-Dokumenttitel.",
        mapOf("text" to "Mathematische Grafik"),
    )
    val Beschreibung = einfach(
        "beschreibung", "Beschreibung", "\\mathrm{desc}", "Metadaten",
        "Setzt die strukturierte SVG-Beschreibung für Barrierefreiheit und Export.",
        mapOf("text" to ""),
    )
    val Metadaten = einfach(
        "metadaten", "Metadaten", "\\mathrm{meta}", "Metadaten",
        "Setzt Dokumentmetadaten als escaped Textinhalt im SVG-AST.",
        mapOf("text" to ""),
    )

    val Mathematik = SvgOperatorDefinition(
        id = "mathematik",
        titel = "Mathematik / LaTeX",
        symbolLatex = "x^2",
        kategorie = "Text",
        beschreibung = "Ergänzt eine mathematische Beschriftung und bewahrt ihren LaTeX-Inhalt strukturiert.",
        anschlüsse = svgStilZahlen("x", "y"),
        standardParameter = mapOf("text" to "x"),
    )

    val GruppeAuflösen = zielOperator(
        "gruppeAufloesen", "Gruppe auflösen", "\\operatorname{ungroup}", "Struktur",
        "Löst die gewählte Gruppe auf; ohne Ziel-ID werden alle obersten Gruppen aufgelöst.",
    )
    val Duplizieren = zielOperator(
        "duplizieren", "Element duplizieren", "\\operatorname{copy}", "Struktur",
        "Dupliziert ein Element anhand seiner stabilen ID.",
        mapOf("neueId" to "kopie"),
    )
    val Entfernen = zielOperator(
        "entfernen", "Element entfernen", "\\operatorname{remove}", "Struktur",
        "Entfernt ein Element rekursiv anhand seiner stabilen ID.",
    )
    val Ersetzen = SvgOperatorDefinition(
        id = "ersetzen",
        titel = "Element ersetzen",
        symbolLatex = "\\operatorname{replace}",
        kategorie = "Struktur",
        beschreibung = "Ersetzt ein Ziel-Element durch die sichtbaren Elemente eines zweiten SVG-ASTs.",
        anschlüsse = listOf(svgEin("svg", 0), svgEin("zweitesSvg", 1), svgAus()),
        standardParameter = mapOf("zielId" to ""),
    )
    val NachVorne = zielOperator("nachVorne", "Nach vorne", "\\uparrow", "Struktur", "Verschiebt das Element eine Zeichenebene nach vorne.")
    val NachHinten = zielOperator("nachHinten", "Nach hinten", "\\downarrow", "Struktur", "Verschiebt das Element eine Zeichenebene nach hinten.")
    val GanzNachVorne = zielOperator("ganzNachVorne", "Ganz nach vorne", "\\Uparrow", "Struktur", "Verschiebt das Element an das Ende seiner Zeichenebene.")
    val GanzNachHinten = zielOperator("ganzNachHinten", "Ganz nach hinten", "\\Downarrow", "Struktur", "Verschiebt das Element an den Anfang seiner Zeichenebene.")

    val Verschieben = SvgOperatorDefinition(
        "verschieben", "Verschieben", "T", "Transformation",
        "Hängt eine SVG-Translation an ausgewählte Elemente; ohne Ziel-ID an alle obersten Elemente.",
        svgZahlen("x", "y"),
        mapOf("zielId" to ""),
    )
    val Skalieren = SvgOperatorDefinition(
        "skalieren", "Skalieren", "S", "Transformation",
        "Hängt eine SVG-Skalierung an ausgewählte Elemente.",
        svgZahlen("x", "y"),
        mapOf("zielId" to ""),
    )
    val Drehen = SvgOperatorDefinition(
        "drehen", "Drehen", "R", "Transformation",
        "Hängt eine Rotation an ausgewählte Elemente; ein optionales Zentrum wird im Inspector gesetzt.",
        svgZahlen("grad"),
        mapOf("zielId" to "", "mitZentrum" to "false", "zentrumX" to "0", "zentrumY" to "0"),
    )
    val Matrix = SvgOperatorDefinition(
        "matrix", "Transformationsmatrix", "\\begin{pmatrix}a&c&e\\\\b&d&f\end{pmatrix}", "Transformation",
        "Hängt eine affine SVG-Matrixtransformation an ausgewählte Elemente.",
        svgZahlen("a", "b", "c", "d", "e", "f"),
        mapOf("zielId" to ""),
    )

    val Stilisieren = SvgOperatorDefinition(
        id = "stilisieren",
        titel = "Stilisieren",
        symbolLatex = "\\mathrm{style}",
        kategorie = "Stil",
        beschreibung = "Ersetzt den Stil ausgewählter Elemente; ohne Ziel-ID werden alle obersten Elemente stilisiert.",
        anschlüsse = listOf(svgEin("svg", 0), stilEin(1), svgAus()),
        standardParameter = mapOf("zielId" to ""),
    )

    val SymbolDefinieren = zielOperator(
        "symbolDefinieren", "Symbol definieren", "\\mathrm{symbol}", "Definitionen",
        "Übernimmt die Zielauswahl als strukturierte <symbol>-Definition.",
        mapOf("definitionId" to "symbol"),
    )
    val SymbolVerwenden = SvgOperatorDefinition(
        id = "symbolVerwenden",
        titel = "Symbol verwenden",
        symbolLatex = "\\mathrm{use}",
        kategorie = "Wiederverwendung",
        beschreibung = "Ergänzt eine strukturierte <use>-Referenz auf ein vorhandenes Symbol.",
        anschlüsse = svgStilZahlen("x", "y"),
        standardParameter = mapOf("definitionId" to "symbol", "breite" to "", "höhe" to ""),
    )

    val MarkerDefinieren = zielOperator(
        "markerDefinieren", "Marker definieren", "\\blacktriangleright", "Marker",
        "Übernimmt die Zielauswahl als Markerdefinition.",
        mapOf(
            "definitionId" to "marker", "refX" to "0", "refY" to "0",
            "markerBreite" to "3", "markerHöhe" to "3", "orientierung" to "auto",
        ),
    )
    val MarkerStart = referenzOperator("markerStart", "Marker am Start", "marker-start", "Marker")
    val MarkerMitte = referenzOperator("markerMitte", "Marker in der Mitte", "marker-mid", "Marker")
    val MarkerEnde = referenzOperator("markerEnde", "Marker am Ende", "marker-end", "Marker")

    val ClipDefinieren = zielOperator(
        "clipDefinieren", "ClipPath definieren", "\\mathrm{clipPath}", "Clipping & Masken",
        "Übernimmt die Zielauswahl als ClipPath-Definition.",
        mapOf("definitionId" to "clip"),
    )
    val ClipAnwenden = referenzOperator("clipAnwenden", "ClipPath anwenden", "clip-path", "Clipping & Masken")
    val MaskeDefinieren = zielOperator(
        "maskeDefinieren", "Maske definieren", "\\mathrm{mask}", "Clipping & Masken",
        "Übernimmt die Zielauswahl als Maskendefinition.",
        mapOf("definitionId" to "maske"),
    )
    val MaskeAnwenden = referenzOperator("maskeAnwenden", "Maske anwenden", "mask", "Clipping & Masken")

    val LinearerVerlauf = einfach(
        "linearerVerlauf", "Linearen Verlauf definieren", "\\mathrm{linearGradient}", "Verläufe",
        "Erzeugt eine strukturierte lineare Verlaufdefinition aus Farbstopps.",
        mapOf(
            "definitionId" to "verlauf", "x1" to "0", "y1" to "0", "x2" to "1", "y2" to "0",
            "stopps" to "0:#000000;1:#ffffff",
        ),
    )
    val RadialerVerlauf = einfach(
        "radialerVerlauf", "Radialen Verlauf definieren", "\\mathrm{radialGradient}", "Verläufe",
        "Erzeugt eine strukturierte radiale Verlaufdefinition aus Farbstopps.",
        mapOf(
            "definitionId" to "verlauf", "cx" to "0.5", "cy" to "0.5", "radius" to "0.5",
            "stopps" to "0:#000000;1:#ffffff",
        ),
    )
    val VerlaufFüllung = referenzOperator("verlaufFuellung", "Verlauf als Füllung", "fill", "Verläufe", stilAttribut = "fill")
    val VerlaufKontur = referenzOperator("verlaufKontur", "Verlauf als Kontur", "stroke", "Verläufe", stilAttribut = "stroke")

    val PatternDefinieren = zielOperator(
        "patternDefinieren", "Pattern definieren", "\\mathrm{pattern}", "Pattern",
        "Übernimmt die Zielauswahl als wiederholbare Patterndefinition.",
        mapOf("definitionId" to "pattern", "breite" to "100", "höhe" to "100"),
    )
    val PatternFüllung = referenzOperator("patternFuellung", "Pattern als Füllung", "fill", "Pattern", stilAttribut = "fill")
    val PatternKontur = referenzOperator("patternKontur", "Pattern als Kontur", "stroke", "Pattern", stilAttribut = "stroke")

    private data class FilterOperator(
        val definition: SvgOperatorDefinition,
        val art: SvgFilterPrimitivArt,
    )

    private val filterOperatoren = SvgFilterPrimitivArt.entries.map { art ->
        val id = "filter${art.name}"
        val titel = when (art) {
            SvgFilterPrimitivArt.Blend -> "Blend"
            SvgFilterPrimitivArt.ColorMatrix -> "ColorMatrix"
            SvgFilterPrimitivArt.ComponentTransfer -> "ComponentTransfer"
            SvgFilterPrimitivArt.Composite -> "Composite"
            SvgFilterPrimitivArt.ConvolveMatrix -> "ConvolveMatrix"
            SvgFilterPrimitivArt.DiffuseLighting -> "DiffuseLighting"
            SvgFilterPrimitivArt.DisplacementMap -> "DisplacementMap"
            SvgFilterPrimitivArt.DropShadow -> "DropShadow"
            SvgFilterPrimitivArt.Flood -> "Flood"
            SvgFilterPrimitivArt.GaussianBlur -> "GaussianBlur"
            SvgFilterPrimitivArt.Image -> "Image"
            SvgFilterPrimitivArt.Merge -> "Merge"
            SvgFilterPrimitivArt.Morphology -> "Morphology"
            SvgFilterPrimitivArt.Offset -> "Offset"
            SvgFilterPrimitivArt.SpecularLighting -> "SpecularLighting"
            SvgFilterPrimitivArt.Tile -> "Tile"
            SvgFilterPrimitivArt.Turbulence -> "Turbulence"
        }
        FilterOperator(
            definition = einfach(
                id, titel, "\\mathrm{${titel}}", "Filter",
                "Ergänzt das Filterprimitiv ${art.tagName} zu einer strukturierten Filterdefinition.",
                mapOf("definitionId" to "filter", "attribute" to standardFilterAttribute(art)),
            ),
            art = art,
        )
    }

    val FilterAnwenden = referenzOperator("filterAnwenden", "Filter anwenden", "filter", "Filter")

    val alle: List<SvgOperatorDefinition> = buildList {
        addAll(listOf(Titel, Beschreibung, Metadaten, Mathematik))
        addAll(listOf(GruppeAuflösen, Duplizieren, Entfernen, Ersetzen, NachVorne, NachHinten, GanzNachVorne, GanzNachHinten))
        addAll(listOf(Verschieben, Skalieren, Drehen, Matrix, Stilisieren))
        addAll(listOf(SymbolDefinieren, SymbolVerwenden))
        addAll(listOf(MarkerDefinieren, MarkerStart, MarkerMitte, MarkerEnde))
        addAll(listOf(ClipDefinieren, ClipAnwenden, MaskeDefinieren, MaskeAnwenden))
        addAll(listOf(LinearerVerlauf, RadialerVerlauf, VerlaufFüllung, VerlaufKontur))
        addAll(listOf(PatternDefinieren, PatternFüllung, PatternKontur))
        addAll(filterOperatoren.map { it.definition })
        add(FilterAnwenden)
    }

    fun filterArt(operatorId: String): SvgFilterPrimitivArt? =
        filterOperatoren.firstOrNull { it.definition.id == operatorId }?.art

    fun istErweitert(operator: SvgOperatorDefinition): Boolean = operator in alle

    private fun einfach(
        id: String,
        titel: String,
        symbol: String,
        kategorie: String,
        beschreibung: String,
        parameter: Map<String, String> = emptyMap(),
    ) = SvgOperatorDefinition(id, titel, symbol, kategorie, beschreibung, svgRahmen(), parameter)

    private fun zielOperator(
        id: String,
        titel: String,
        symbol: String,
        kategorie: String,
        beschreibung: String,
        parameter: Map<String, String> = emptyMap(),
    ) = einfach(id, titel, symbol, kategorie, beschreibung, mapOf("zielId" to "") + parameter)

    private fun referenzOperator(
        id: String,
        titel: String,
        attribut: String,
        kategorie: String,
        stilAttribut: String? = null,
    ) = einfach(
        id = id,
        titel = titel,
        symbol = "\\mathrm{$titel}",
        kategorie = kategorie,
        beschreibung = "Wendet die Definition über das SVG-Attribut $attribut auf die Zielauswahl an.",
        parameter = mapOf(
            "zielId" to "",
            "definitionId" to when (kategorie) {
                "Marker" -> "marker"
                "Clipping & Masken" -> if (attribut == "mask") "maske" else "clip"
                "Pattern" -> "pattern"
                "Filter" -> "filter"
                else -> "verlauf"
            },
            "referenzAttribut" to attribut,
        ) + (stilAttribut?.let { mapOf("stilAttribut" to it) } ?: emptyMap()),
    )

    private fun svgRahmen() = listOf(svgEin("svg", 0), svgAus())
    private fun svgZahlen(vararg namen: String) = buildList {
        add(svgEin("svg", 0))
        namen.forEachIndexed { index, name -> add(zahlEin(name, index + 1)) }
        add(svgAus())
    }
    private fun svgStilZahlen(vararg namen: String) = buildList {
        add(svgEin("svg", 0))
        add(stilEin(1))
        namen.forEachIndexed { index, name -> add(zahlEin(name, index + 2)) }
        add(svgAus())
    }

    private fun standardFilterAttribute(art: SvgFilterPrimitivArt): String = when (art) {
        SvgFilterPrimitivArt.GaussianBlur -> "stdDeviation=4"
        SvgFilterPrimitivArt.DropShadow -> "dx=2;dy=2;stdDeviation=2;flood-opacity=0.5"
        SvgFilterPrimitivArt.Offset -> "dx=2;dy=2"
        SvgFilterPrimitivArt.Morphology -> "operator=dilate;radius=1"
        SvgFilterPrimitivArt.Turbulence -> "baseFrequency=0.05;numOctaves=2"
        SvgFilterPrimitivArt.Blend -> "mode=multiply"
        SvgFilterPrimitivArt.Flood -> "flood-color=currentColor;flood-opacity=1"
        else -> ""
    }
}

data class SvgErweitertesAuswertungsErgebnis(
    val grafik: SvgGrafik,
    val warnungen: List<String> = emptyList(),
)

object SvgErweiterteOperatorAuswertung {
    fun auswerten(
        operator: SvgOperatorDefinition,
        kontext: KnotenAuswertungsKontext,
        basis: SvgGrafik,
        stil: SvgStil,
    ): SvgErweitertesAuswertungsErgebnis {
        val p = kontext.knoten.parameter
        val zielId = p["zielId"]?.trim().orEmpty().ifBlank { null }
        val definitionId = p["definitionId"].orEmpty().ifBlank { "definition" }
        val elementId = "svg-${kontext.knoten.id.wert}-${operator.id}".replace(Regex("[^A-Za-z0-9_.:-]"), "-")

        fun zielFehlt(): SvgErweitertesAuswertungsErgebnis = SvgErweitertesAuswertungsErgebnis(
            basis,
            listOf("Der SVG-Operator '${operator.titel}' benötigt eine vorhandene Ziel-ID."),
        )

        val grafik = when (operator) {
            SvgErweiterteOperatoren.Titel -> basis.copy(titel = p["text"].orEmpty())
            SvgErweiterteOperatoren.Beschreibung -> basis.copy(beschreibung = p["text"].orEmpty())
            SvgErweiterteOperatoren.Metadaten -> basis.copy(metadaten = p["text"].orEmpty())
            SvgErweiterteOperatoren.Mathematik -> basis.mitElement(
                SvgText(
                    id = elementId,
                    position = basis.mappeSvg(kontext.punktSvg("x", "y")),
                    inhalt = p["text"].orEmpty(),
                    mathematikLatex = true,
                    stil = stil,
                ),
            )
            SvgErweiterteOperatoren.GruppeAuflösen -> basis.gruppeAuflösen(zielId)
            SvgErweiterteOperatoren.Duplizieren -> {
                if (zielId == null || basis.findeElement(zielId) == null) return zielFehlt()
                basis.dupliziereElement(zielId, p["neueId"].orEmpty())
            }
            SvgErweiterteOperatoren.Entfernen -> {
                if (zielId == null || basis.findeElement(zielId) == null) return zielFehlt()
                basis.entferneElement(zielId)
            }
            SvgErweiterteOperatoren.Ersetzen -> {
                if (zielId == null || basis.findeElement(zielId) == null) return zielFehlt()
                val ersatz = kontext.eingänge["zweitesSvg"]?.objekt as? SvgGrafik
                    ?: return SvgErweitertesAuswertungsErgebnis(basis, listOf("Zum Ersetzen fehlt der zweite SVG-Eingang."))
                basis.entferneElement(zielId).mitElementen(ersatz.elemente)
            }
            SvgErweiterteOperatoren.NachVorne -> zielId?.let { basis.ordneElement(it, +1) } ?: return zielFehlt()
            SvgErweiterteOperatoren.NachHinten -> zielId?.let { basis.ordneElement(it, -1) } ?: return zielFehlt()
            SvgErweiterteOperatoren.GanzNachVorne -> zielId?.let { basis.ordneElement(it, +1, ganz = true) } ?: return zielFehlt()
            SvgErweiterteOperatoren.GanzNachHinten -> zielId?.let { basis.ordneElement(it, -1, ganz = true) } ?: return zielFehlt()
            SvgErweiterteOperatoren.Verschieben -> basis.transformiere(
                zielId,
                SvgTransformation.Verschieben(kontext.zahlSvg("x"), kontext.zahlSvg("y")),
            )
            SvgErweiterteOperatoren.Skalieren -> basis.transformiere(
                zielId,
                SvgTransformation.Skalieren(kontext.zahlSvg("x"), kontext.zahlSvg("y")),
            )
            SvgErweiterteOperatoren.Drehen -> {
                val um = if (p["mitZentrum"]?.toBooleanStrictOrNull() == true) {
                    basis.mappeSvg(SvgPunkt(p.doubleSvg("zentrumX", 0.0), p.doubleSvg("zentrumY", 0.0)))
                } else null
                basis.transformiere(zielId, SvgTransformation.Drehen(kontext.zahlSvg("grad"), um))
            }
            SvgErweiterteOperatoren.Matrix -> basis.transformiere(
                zielId,
                SvgTransformation.Matrix(
                    kontext.zahlSvg("a"), kontext.zahlSvg("b"), kontext.zahlSvg("c"),
                    kontext.zahlSvg("d"), kontext.zahlSvg("e"), kontext.zahlSvg("f"),
                ),
            )
            SvgErweiterteOperatoren.Stilisieren -> basis.stilisiere(zielId, stil)
            SvgErweiterteOperatoren.SymbolDefinieren -> {
                val auswahl = basis.elementAuswahl(zielId)
                if (auswahl.isEmpty()) return zielFehlt()
                basis.mitDefinition(SvgSymbolDefinition(definitionId, auswahl))
            }
            SvgErweiterteOperatoren.SymbolVerwenden -> {
                if (basis.definitionen.none { it.id == definitionId }) {
                    return SvgErweitertesAuswertungsErgebnis(basis, listOf("Das SVG enthält keine Definition '$definitionId'."))
                }
                basis.mitElement(
                    SvgVerwendung(
                        id = elementId,
                        referenzId = definitionId,
                        position = basis.mappeSvg(kontext.punktSvg("x", "y")),
                        breite = p["breite"]?.toDoubleOrNull(),
                        höhe = p["höhe"]?.toDoubleOrNull(),
                        stil = stil,
                    ),
                )
            }
            SvgErweiterteOperatoren.MarkerDefinieren -> {
                val auswahl = basis.elementAuswahl(zielId)
                if (auswahl.isEmpty()) return zielFehlt()
                basis.mitDefinition(
                    SvgMarkerDefinition(
                        id = definitionId,
                        elemente = auswahl,
                        refX = p.doubleSvg("refX", 0.0),
                        refY = p.doubleSvg("refY", 0.0),
                        markerBreite = abs(p.doubleSvg("markerBreite", 3.0)),
                        markerHöhe = abs(p.doubleSvg("markerHöhe", 3.0)),
                        orientierung = p["orientierung"].orEmpty().ifBlank { "auto" },
                    ),
                )
            }
            SvgErweiterteOperatoren.ClipDefinieren -> {
                val auswahl = basis.elementAuswahl(zielId)
                if (auswahl.isEmpty()) return zielFehlt()
                basis.mitDefinition(SvgClipPfadDefinition(definitionId, auswahl))
            }
            SvgErweiterteOperatoren.MaskeDefinieren -> {
                val auswahl = basis.elementAuswahl(zielId)
                if (auswahl.isEmpty()) return zielFehlt()
                basis.mitDefinition(SvgMaskenDefinition(definitionId, auswahl))
            }
            SvgErweiterteOperatoren.LinearerVerlauf -> basis.mitDefinition(
                SvgLinearerVerlaufDefinition(
                    id = definitionId,
                    start = SvgPunkt(p.doubleSvg("x1", 0.0), p.doubleSvg("y1", 0.0)),
                    ende = SvgPunkt(p.doubleSvg("x2", 1.0), p.doubleSvg("y2", 0.0)),
                    stopps = parseStopps(p["stopps"]),
                ),
            )
            SvgErweiterteOperatoren.RadialerVerlauf -> basis.mitDefinition(
                SvgRadialerVerlaufDefinition(
                    id = definitionId,
                    mittelpunkt = SvgPunkt(p.doubleSvg("cx", 0.5), p.doubleSvg("cy", 0.5)),
                    radius = abs(p.doubleSvg("radius", 0.5)),
                    stopps = parseStopps(p["stopps"]),
                ),
            )
            SvgErweiterteOperatoren.PatternDefinieren -> {
                val auswahl = basis.elementAuswahl(zielId)
                if (auswahl.isEmpty()) return zielFehlt()
                basis.mitDefinition(
                    SvgPatternDefinition(
                        definitionId,
                        abs(p.doubleSvg("breite", 100.0)),
                        abs(p.doubleSvg("höhe", 100.0)),
                        auswahl,
                    ),
                )
            }
            SvgErweiterteOperatoren.MarkerStart,
            SvgErweiterteOperatoren.MarkerMitte,
            SvgErweiterteOperatoren.MarkerEnde,
            SvgErweiterteOperatoren.ClipAnwenden,
            SvgErweiterteOperatoren.MaskeAnwenden,
            SvgErweiterteOperatoren.FilterAnwenden -> basis.setzeAttribut(
                zielId,
                p["referenzAttribut"].orEmpty(),
                "url(#$definitionId)",
            )
            SvgErweiterteOperatoren.VerlaufFüllung,
            SvgErweiterteOperatoren.PatternFüllung -> basis.stilisierePaint(zielId, definitionId, füllung = true)
            SvgErweiterteOperatoren.VerlaufKontur,
            SvgErweiterteOperatoren.PatternKontur -> basis.stilisierePaint(zielId, definitionId, füllung = false)
            else -> {
                val filterArt = SvgErweiterteOperatoren.filterArt(operator.id)
                if (filterArt != null) {
                    val neu = SvgFilterPrimitiv(filterArt, parseAttribute(p["attribute"]))
                    val vorhanden = basis.definitionen.filterIsInstance<SvgFilterDefinition>().firstOrNull { it.id == definitionId }
                    basis.mitDefinition(
                        SvgFilterDefinition(
                            id = definitionId,
                            primitive = vorhanden?.primitive.orEmpty() + neu,
                        ),
                    )
                } else basis
            }
        }
        return SvgErweitertesAuswertungsErgebnis(grafik)
    }
}

object SvgOperatorAblauf {
    fun für(operator: SvgOperatorDefinition): String = when (operator.id) {
        "dokument" -> "SVG übernehmen oder Standard-SVG erzeugen → Dokumentraum aktualisieren → SVG ausgeben"
        "kombinieren" -> "SVG A + SVG B → IDs und Definitionen abgleichen → ASTs vereinigen → SVG ausgeben"
        "gruppe" -> "SVG übernehmen → sichtbare Elemente in Gruppe kapseln → SVG ausgeben"
        "gruppeAufloesen" -> "SVG übernehmen → Zielgruppe finden → Kinder an ihre Stelle heben → SVG ausgeben"
        "duplizieren" -> "SVG übernehmen → Element-ID suchen → unveränderliche Kopie mit neuer ID einsetzen → SVG ausgeben"
        "entfernen" -> "SVG übernehmen → Element-ID rekursiv suchen → Element entfernen → SVG ausgeben"
        "ersetzen" -> "SVG + zweites SVG → Ziel entfernen → Ersatzelemente übernehmen → SVG ausgeben"
        "verschieben", "skalieren", "drehen", "matrix" -> "SVG übernehmen → Ziel-ID oder gesamte oberste Ebene wählen → Transformation an AST-Elemente anhängen → SVG ausgeben"
        "stilisieren" -> "SVG + Stil → Ziel-ID oder gesamte oberste Ebene wählen → Stil am AST ersetzen → SVG ausgeben"
        "symbolDefinieren", "markerDefinieren", "clipDefinieren", "maskeDefinieren", "patternDefinieren" -> "SVG übernehmen → Zielauswahl bestimmen → strukturierte Definition unter <defs> ergänzen → SVG ausgeben"
        "symbolVerwenden" -> "SVG übernehmen → Symbolreferenz prüfen → <use> mit Position ergänzen → SVG ausgeben"
        "linearerVerlauf", "radialerVerlauf" -> "SVG übernehmen → Farbstopps parsen → Verlauf unter <defs> ergänzen → SVG ausgeben"
        "markerStart", "markerMitte", "markerEnde", "clipAnwenden", "maskeAnwenden", "filterAnwenden" -> "SVG übernehmen → Zielauswahl bestimmen → typisierte Definitionsreferenz als SVG-Attribut setzen → SVG ausgeben"
        "verlaufFuellung", "verlaufKontur", "patternFuellung", "patternKontur" -> "SVG übernehmen → Zielauswahl bestimmen → Paint-Referenz url(#…) in den Stil eintragen → SVG ausgeben"
        else -> if (SvgErweiterteOperatoren.filterArt(operator.id) != null) {
            "SVG übernehmen → Filterattribute parsen → Filterprimitiv strukturiert an Definition anhängen → SVG ausgeben"
        } else {
            "SVG übernehmen oder Standard-SVG erzeugen → Operator anwenden → vollständigen neuen SVG-AST ausgeben"
        }
    }
}

private fun svgEin(name: String, reihenfolge: Int) = AnschlussDaten(
    name = name,
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = MathematikAnschlussArten.SvgGrafik.id,
    reihenfolge = reihenfolge,
    vertrag = AnschlussVertrag(TypAusdruck.Atom(MathematischeTypen.SvgGrafik)),
)

private fun svgAus() = AnschlussDaten(
    name = "svg",
    richtung = AnschlussRichtung.Ausgang,
    kante = AnschlussKante.Rechts,
    art = MathematikAnschlussArten.SvgGrafik.id,
    vertrag = AnschlussVertrag(TypAusdruck.Atom(MathematischeTypen.SvgGrafik)),
)

private fun stilEin(reihenfolge: Int) = AnschlussDaten(
    name = "stil",
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = MathematikAnschlussArten.SvgStil.id,
    reihenfolge = reihenfolge,
    vertrag = AnschlussVertrag(TypAusdruck.Atom(MathematischeTypen.SvgStil)),
)

private fun zahlEin(name: String, reihenfolge: Int) = AnschlussDaten(
    name = name,
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = MathematikAnschlussArten.Zahl.id,
    reihenfolge = reihenfolge,
    vertrag = AnschlussVertrag(TypAusdruck.Atom(MathematischeTypen.Zahl)),
)

private fun KnotenAuswertungsKontext.zahlSvg(name: String): Double {
    val objekt = eingänge[name]?.objekt ?: error("SVG-Operator benötigt den Zahleneingang '$name'.")
    return when (objekt) {
        is RationaleZahl -> objekt.zuDezimal(rechenKontext.dezimalstellen).toDouble()
        else -> error("SVG-Operator benötigt an '$name' eine auswertbare rationale Zahl.")
    }
}

private fun KnotenAuswertungsKontext.punktSvg(x: String, y: String) = SvgPunkt(zahlSvg(x), zahlSvg(y))

private fun SvgGrafik.mappeSvg(punkt: SvgPunkt): SvgPunkt = koordinatenraum.bildeAb(punkt, viewport)

private fun Map<String, String>.doubleSvg(name: String, fallback: Double): Double =
    get(name)?.trim()?.toDoubleOrNull() ?: fallback

private fun parseStopps(roh: String?): List<SvgFarbStopp> {
    val stopps = roh.orEmpty().split(';').mapNotNull { eintrag ->
        val teile = eintrag.trim().split(':')
        if (teile.size < 2) return@mapNotNull null
        val offset = teile[0].toDoubleOrNull() ?: return@mapNotNull null
        val farbe = teile[1].ifBlank { return@mapNotNull null }
        val deckkraft = teile.getOrNull(2)?.toDoubleOrNull() ?: 1.0
        SvgFarbStopp(offset.coerceIn(0.0, 1.0), farbe, deckkraft.coerceIn(0.0, 1.0))
    }
    require(stopps.isNotEmpty()) { "Ein SVG-Verlauf benötigt mindestens einen Farbstopp, z. B. 0:#000000;1:#ffffff." }
    return stopps
}

private fun parseAttribute(roh: String?): Map<String, String> = buildMap {
    roh.orEmpty().split(';').forEach { eintrag ->
        val index = eintrag.indexOf('=')
        if (index <= 0) return@forEach
        val name = eintrag.substring(0, index).trim()
        val wert = eintrag.substring(index + 1).trim()
        if (name.isNotBlank() && wert.isNotBlank()) put(name, wert)
    }
}

private fun SvgGrafik.stilisierePaint(zielId: String?, definitionId: String, füllung: Boolean): SvgGrafik {
    val url = "url(#$definitionId)"
    val basisStil = SvgStil()
    val id = zielId?.takeIf { it.isNotBlank() }
    fun stilFür(element: SvgElement): SvgStil {
        val alt = element.stil ?: basisStil
        return if (füllung) alt.copy(füllung = url) else alt.copy(kontur = url)
    }
    return if (id == null) {
        var ergebnis = this
        elemente.forEach { element -> ergebnis = ergebnis.stilisiere(element.id, stilFür(element)) }
        ergebnis
    } else {
        val element = findeElement(id) ?: return this
        stilisiere(id, stilFür(element))
    }
}

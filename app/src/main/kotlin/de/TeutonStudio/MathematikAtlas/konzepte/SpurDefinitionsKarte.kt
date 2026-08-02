package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussVerweis
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnsichtsFenster
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungsId
import de.TeutonStudio.MathematikKnoten.MATRIXDIAGONALE_ART_PARAMETER
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.MatrixdiagonaleKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.SpurKnotenVorlagen
import de.TeutonStudio.MathematikRechenSystem.kern.MatrixDiagonalArt

/** Selbstbezugsfreie Definition: Matrix → Hauptdiagonale → iterierte Tupelsumme → Zahl. */
internal fun spurDefinitionsKarte(vorlage: KnotenVorlage, variantenIndex: Int): KartenDaten {
    val prefix = "spur-definition-$variantenIndex"
    val matrix = knoten(
        vorlage = MathematikKnotenVorlagen.KartenEingang,
        id = "$prefix-matrix",
        x = 30f,
        y = 120f,
        parameter = mapOf("name" to "Quadratische Matrix A"),
        anschlussArt = MathematikAnschlussArten.Matrix.id,
    )
    val diagonale = knoten(
        vorlage = MatrixdiagonaleKnotenVorlagen.Matrixdiagonale,
        id = "$prefix-diagonale",
        x = 360f,
        y = 120f,
        parameter = mapOf(
            MATRIXDIAGONALE_ART_PARAMETER to MatrixDiagonalArt.HAUPTDIAGONALE.parameterWert,
        ),
    )
    val summe = knoten(
        vorlage = SpurKnotenVorlagen.IterierteSummeTupel,
        id = "$prefix-summe",
        x = 700f,
        y = 120f,
    )
    val ausgang = knoten(
        vorlage = MathematikKnotenVorlagen.KartenAusgang,
        id = "$prefix-ausgang",
        x = 1040f,
        y = 120f,
        parameter = mapOf("name" to "Spur von A"),
        anschlussArt = MathematikAnschlussArten.Zahl.id,
    )

    return KartenDaten(
        id = KartenId(prefix),
        name = "Definition von ${vorlage.name}",
        knoten = listOf(matrix, diagonale, summe, ausgang),
        verbindungen = listOf(
            verbinde("$prefix-v1", matrix, "wert", diagonale, "matrix"),
            verbinde("$prefix-v2", diagonale, "diagonale", summe, "tupel"),
            verbinde("$prefix-v3", summe, "wert", ausgang, "wert"),
        ),
        ansicht = AnsichtsFenster(zoom = .75f),
    )
}

private fun knoten(
    vorlage: KnotenVorlage,
    id: String,
    x: Float,
    y: Float,
    parameter: Map<String, String> = emptyMap(),
    anschlussArt: AnschlussArtId? = null,
): KnotenDaten {
    val erzeugt = vorlage.erzeuge(GraphPunkt(x, y))
    return erzeugt.copy(
        id = KnotenId(id),
        anschlüsse = erzeugt.anschlüsse.map { anschluss ->
            anschluss.copy(
                id = AnschlussId("$id-${anschluss.richtung.name.lowercase()}-${anschluss.name}"),
                art = anschlussArt ?: anschluss.art,
            )
        },
        parameter = erzeugt.parameter + parameter,
    )
}

private fun verbinde(
    id: String,
    von: KnotenDaten,
    vonName: String,
    zu: KnotenDaten,
    zuName: String,
): VerbindungDaten = VerbindungDaten(
    id = VerbindungsId(id),
    von = AnschlussVerweis(
        von.id,
        von.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang && it.name == vonName }.id,
    ),
    zu = AnschlussVerweis(
        zu.id,
        zu.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang && it.name == zuName }.id,
    ),
)

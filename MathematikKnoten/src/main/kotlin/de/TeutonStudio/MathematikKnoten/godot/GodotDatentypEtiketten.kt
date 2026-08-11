package de.TeutonStudio.MathematikKnoten.godot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.TeutonStudio.MathematikRechenSystem.kern.MathematischesObjekt
import de.TeutonStudio.MathematikRechenSystem.kern.Matrix
import de.TeutonStudio.MathematikRechenSystem.kern.OrientierterVektor
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.Tupel
import de.TeutonStudio.MathematikRechenSystem.kern.ZahlAusdruck
import java.math.BigInteger

/**
 * Godot-Builtin-Datentypen mit eigener kompakter Editor-Glyphe.
 *
 * Orchestrator bezieht diese Bilder für seine Datenpins über Godots
 * `get_class_icon(...)`. Die Pfadgeometrien hier sind aus den entsprechenden
 * `editor/icons/*.svg`-Dateien des Godot-Repositories übernommen und werden als
 * eigenständiges Atlas-Etikett gerendert. Die Zuordnung zum Knoten erfolgt allein
 * über die mathematische Struktur seiner Ausgabe, nicht über einen Godot-Typ im
 * Mathematikkern.
 */
enum class GodotDatentyp(
    val anzeigeName: String,
    val mathematischeStruktur: String,
    internal val ebenen: List<GodotGlyphEbene>,
) {
    Vector2(
        "Vector2",
        "2-dimensionales numerisches Tupel oder orientierter 2-Vektor",
        listOf(
            ebene(0xFFAC73F1, "M12 2v2h1a1 1 0 0 1 0 2 2 2 0 0 0-2 2v2h5V8h-3a3 3 0 0 0 0-6zM1 4v6h2a3 3 0 0 0 3-3V4H4v3a1 1 0 0 1-1 1V4zm9 6V8H9a1 1 0 0 1 0-2h1V4H9a3 3 0 0 0 0 6z"),
            ebene(0x66FFFFFF, "M12 2v2h1a1 1 0 0 1 0 2 2 2 0 0 0-2 2v2h5V8h-3a3 3 0 0 0 0-6z"),
        ),
    ),
    Vector2i(
        "Vector2i",
        "ganzzahliges 2-dimensionales Tupel oder orientierter 2-Vektor",
        listOf(
            ebene(0xFFAC73F1, "M8 2v2h1a1 1 0 0 1 0 2 2 2 0 0 0-2 2v2h5V8H9a3 3 0 0 0 0-6zM1 4v6h2a3 3 0 0 0 3-3V4H4v3a1 1 0 0 1-1 1V4z"),
            ebene(0x66FFFFFF, "M8 2v2h1a1 1 0 0 1 0 2 2 2 0 0 0-2 2v2h5V8H9a3 3 0 0 0 0-6z"),
            ebene(0xFF5ABBEF, "M13 2v2h2V2zm0 4v4h2V6z"),
        ),
    ),
    Rect2(
        "Rect2",
        "geordnetes Paar aus zwei 2-dimensionalen numerischen Tupeln/Vektoren",
        listOf(ebene(0xFFF1738F, "M13 2v2h-1a3 3 0 0 0-2.5 1.346A3 3 0 1 0 7 10h1V8H7a1 1 0 0 1-1-1h3a3 3 0 0 0 3 3h1V8h-1a1 1 0 0 1 0-2h1v1a3 3 0 0 0 3 3V8a1 1 0 0 1-1-1V6h1V4h-1V2zM3 4a3 3 0 0 0-3 3v3h2V7a1 1 0 0 1 1-1h1V4z")),
    ),
    Rect2i(
        "Rect2i",
        "geordnetes Paar aus zwei ganzzahligen 2-dimensionalen Tupeln/Vektoren",
        listOf(
            ebene(0xFFF1738F, "M9 2v2H8a3 3 0 0 0 0 6h1V8H8a1 1 0 0 1 0-2h1v1a3 3 0 0 0 3 3V8a1 1 0 0 1-1-1V6h1V4h-1V2zM4 4a3 3 0 0 0-3 3v3h2V7a1 1 0 0 1 1-1h1V4z"),
            ebene(0xFF5ABBEF, "M13 2v2h2V2zm0 4v4h2V6z"),
        ),
    ),
    Vector3(
        "Vector3",
        "3-dimensionales numerisches Tupel oder orientierter 3-Vektor",
        listOf(
            ebene(0xFFDE66F0, "M11 2v2h2a1 1 0 0 1-1 1v2a1 1 0 0 1 0 2h-1v2h1a3 3 0 0 0 2.232-5A3 3 0 0 0 15 4V2zM1 4v6h2a3 3 0 0 0 3-3V4H4v3a1 1 0 0 1-1 1V4zm9 6V8H9a1 1 0 0 1 0-2h1V4H9a3 3 0 0 0 0 6z"),
            ebene(0x66FFFFFF, "M11 2v2h2a1 1 0 0 1-1 1v2a1 1 0 0 1 0 2h-1v2h1a3 3 0 0 0 2.232-5A3 3 0 0 0 15 4V2z"),
        ),
    ),
    Vector3i(
        "Vector3i",
        "ganzzahliges 3-dimensionales Tupel oder orientierter 3-Vektor",
        listOf(
            ebene(0xFFDE66F0, "M8 2v2h2a1 1 0 0 1-1 1v2a1 1 0 0 1 0 2H8v2h1a3 3 0 0 0 2.232-5A3 3 0 0 0 12 4V2zM1 4v6h2a3 3 0 0 0 3-3V4H4v3a1 1 0 0 1-1 1V4z"),
            ebene(0x66FFFFFF, "M8 2v2h2a1 1 0 0 1-1 1v2a1 1 0 0 1 0 2H8v2h1a3 3 0 0 0 2.232-5A3 3 0 0 0 12 4V2z"),
            ebene(0xFF5ABBEF, "M13 2v2h2V2zm0 4v4h2V6z"),
        ),
    ),
    Transform2D(
        "Transform2D",
        "2×3-Matrix oder Tupel aus drei 2-Vektoren",
        listOf(
            ebene(0xFFB9EC41, "M0 2v2h2v6h2V4h3a1 1 0 0 1 0 2 2 2 0 0 0-2 2v2h7a4 4 0 0 0 0-8h-2v6H7a3 3 0 0 0 0-6zm12 2a2 2 0 0 1 0 4z"),
            ebene(0x66FFFFFF, "M6.5 2v2H7a1 1 0 0 1 0 2 2 2 0 0 0-2 2v2h5V8H7a3 3 0 0 0 0-6h-.5z"),
        ),
    ),
    Vector4(
        "Vector4",
        "4-dimensionales numerisches Tupel oder orientierter 4-Vektor",
        listOf(
            ebene(0xFFF066BD, "M11 3v5h3v3h2V2h-2v4h-1V3zM1 4v6h2a3 3 0 0 0 3-3V4H4v3a1 1 0 0 1-1 1V4zm9 6V8H9a1 1 0 0 1 0-2h1V4H9a3 3 0 0 0 0 6z"),
            ebene(0x66FFFFFF, "M11 3v5h3v3h2V2h-2v4h-1V3z"),
        ),
    ),
    Vector4i(
        "Vector4i",
        "ganzzahliges 4-dimensionales Tupel oder orientierter 4-Vektor",
        listOf(
            ebene(0xFFF066BD, "M7 3v5h3v3h2V2h-2v4H9V3zM1 4v6h2a3 3 0 0 0 3-3V4H4v3a1 1 0 0 1-1 1V4z"),
            ebene(0x66FFFFFF, "M7 3v5h3v3h2V2h-2v4H9V3z"),
            ebene(0xFF5ABBEF, "M13 2v2h2V2zm0 4v4h2V6z"),
        ),
    ),
    Plane(
        "Plane",
        "4-dimensionaler Zahlenvektor (Normalenkomponenten und Abstand)",
        listOf(ebene(0xFFF74949, "M1 2v8h2V8a3 3 0 0 0 0-6zm6 0v5a3 3 0 0 0 3 3V8a1 1 0 0 1-1-1V2zM3 4a1 1 0 0 1 0 2zm8 0v6h2V6a1 1 0 0 1 1 1v3h2V7a3 3 0 0 0-3-3z")),
    ),
    Quaternion(
        "Quaternion",
        "4-dimensionaler Zahlenvektor beziehungsweise Quaternion-Komponenten",
        listOf(
            ebene(0xFFEC418E, "M3 3a3 3 0 0 0 0 6v2h2V8.236A3 3 0 0 0 7 9h2v-.764A3 3 0 0 0 11 9h2V6a3 3 0 0 0 3 3V7a1 1 0 0 1-1-1V5h1V3h-1V1h-2v2h-2a3 3 0 0 0-2 .764V3H7v4a1 1 0 0 1-1-1V3zm0 4a1 1 0 0 1 0-2zm8 0a1 1 0 0 1 0-2z"),
            ebene(0x66FFFFFF, "M4 3v3a3 3 0 0 0 3 3h2V3H7v4a1 1 0 0 1-1-1V3z"),
        ),
    ),
    AABB(
        "AABB",
        "geordnetes Paar aus zwei 3-dimensionalen Zahlenvektoren",
        listOf(
            ebene(0xFFEE5677, "M5 1a3 3 0 0 0-2.223 5.012A3 3 0 0 0 3 12h2V7h2V1H5zm6 0v5.174A3 3 0 0 0 10 6V4H8v8h2a3 3 0 0 0 3-3 3 3 0 0 0 0-6V1h-2zM5 3v2a1 1 0 0 1 0-2zm8 2a1 1 0 0 1 0 2zM3 8v2a1 1 0 0 1 0-2zm7 0a1 1 0 0 1 0 2z"),
            ebene(0x3CFFFFFF, "M8 4v8h2a3 3 0 0 0 0-6V4zM3 6a3 3 0 0 0 0 6h2V6zm0 2v2a1 1 0 0 1 0-2zm7 0a1 1 0 0 1 0 2z"),
        ),
    ),
    Basis(
        "Basis",
        "3×3-Matrix oder Tupel aus drei 3-Vektoren",
        listOf(
            ebene(0xFFE1EC41, "M0 2v8h2a3 3 0 0 0 0-6V2zm10 0v2h2V2zM7 4a2 2 0 0 0 0 4H5v2h2a2 2 0 0 0 0-4h2V4zm7 0a2 2 0 0 0 0 4h-2V6h-2v4h4a2 2 0 0 0 0-4h2V4zM2 6a1 1 0 0 1 1 1 1 1 0 0 1-1 1z"),
            ebene(0x66FFFFFF, "M10 2v2h2V2zm0 4v4h2V6z"),
        ),
    ),
    Transform3D(
        "Transform3D",
        "3×4-Matrix oder Tupel aus vier 3-Vektoren",
        listOf(
            ebene(0xFFF68F45, "M4 4h3.349a1 1.267 0 0 1-1 1.267v1a.866.866 0 0 1 0 1.732h-1v2h1a2.81 2.81 0 0 0 2.232-4.734A1.32 1.32 0 0 0 9.345 4V2H0v2h2v6h2zm6 6h2a4 4 0 0 0 0-8h-2zm2-6a2 2 0 0 1 0 4z"),
            ebene(0x66FFFFFF, "M5.5 4h1.849a1 1.267 0 0 1-1 1.267v1a.866.866 0 0 1 0 1.732h-1v2h1a2.81 2.81 0 0 0 2.232-4.734A1.32 1.32 0 0 0 9.345 4V2H5.5z"),
        ),
    ),
    Projection(
        "Projection",
        "4×4-Matrix oder Tupel aus vier 4-Vektoren",
        listOf(
            ebene(0xFF44BD44, "M0 2v8h2V8a3 3 0 0 0 2-.779V10h2V7a1 1 0 0 1 1-1h1V4H7a3 3 0 0 0-2.02.795A3 3 0 0 0 2 2zm8 4v2a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-1a2 2 0 0 0-2 2zm6-4v2h2V2zM2 4a1 1 0 0 1 0 2zm8 2h1v2h-1zm4 0v3a1 1 0 0 1-1 1h-1v2h1a3 3 0 0 0 3-3V6z"),
            ebene(0x66FFFFFF, "M7 4a3 3 0 0 0-3 3v3h2V7a1 1 0 0 1 1-1h1V4z"),
        ),
    ),
    Color(
        "Color",
        "4-dimensionales Zahlentupel für RGBA-Komponenten",
        listOf(
            ebene(0xFFFF5F5F, "M4 4a3 3 0 0 0 0 6h1V8H4a1 1 0 0 1 0-2h1V4z"),
            ebene(0xFF5FFF97, "M6 2v5a3 3 0 0 0 3 3h1V8H9a1 1 0 0 1-1-1V2z"),
            ebene(0xFF5FB2FF, "M14 4a3 3 0 0 0-3 3v3h2V7a1 1 0 0 1 1-1h1V4z"),
        ),
    ),
}

data class GodotDatentypEtikett(
    val typ: GodotDatentyp,
    val begründung: String = typ.mathematischeStruktur,
)

/**
 * Leitet alle strukturell kompatiblen Godot-Builtin-Typen aus einem mathematischen
 * Objekt ab. Mehrdeutigkeit ist beabsichtigt: Ein 4-Tupel ist beispielsweise
 * gleichzeitig eine mögliche Repräsentation von Vector4, Plane, Quaternion und Color.
 */
fun godotDatentypEtikettenFür(objekt: MathematischesObjekt): List<GodotDatentypEtikett> {
    val typen = linkedSetOf<GodotDatentyp>()

    objekt.flacheZahlKomponenten()?.let { komponenten ->
        when (komponenten.size) {
            2 -> {
                typen += GodotDatentyp.Vector2
                if (komponenten.sindGanzzahlig()) typen += GodotDatentyp.Vector2i
            }
            3 -> {
                typen += GodotDatentyp.Vector3
                if (komponenten.sindGanzzahlig()) typen += GodotDatentyp.Vector3i
            }
            4 -> {
                typen += GodotDatentyp.Vector4
                if (komponenten.sindGanzzahlig()) typen += GodotDatentyp.Vector4i
                typen += GodotDatentyp.Plane
                typen += GodotDatentyp.Quaternion
                typen += GodotDatentyp.Color
            }
        }
    }

    objekt.zahlKomponentenBlöcke(anzahl = 2, dimension = 2)?.let { blöcke ->
        typen += GodotDatentyp.Rect2
        if (blöcke.flatten().sindGanzzahlig()) typen += GodotDatentyp.Rect2i
    }
    if (objekt.zahlKomponentenBlöcke(anzahl = 2, dimension = 3) != null) {
        typen += GodotDatentyp.AABB
    }
    if (objekt.zahlKomponentenBlöcke(anzahl = 3, dimension = 2) != null) {
        typen += GodotDatentyp.Transform2D
    }
    if (objekt.zahlKomponentenBlöcke(anzahl = 3, dimension = 3) != null) {
        typen += GodotDatentyp.Basis
    }
    if (objekt.zahlKomponentenBlöcke(anzahl = 4, dimension = 3) != null) {
        typen += GodotDatentyp.Transform3D
    }
    if (objekt.zahlKomponentenBlöcke(anzahl = 4, dimension = 4) != null) {
        typen += GodotDatentyp.Projection
    }

    if (objekt is Matrix) {
        when (objekt.zeilenAnzahl to objekt.spaltenAnzahl) {
            2 to 3 -> typen += GodotDatentyp.Transform2D
            3 to 3 -> typen += GodotDatentyp.Basis
            3 to 4 -> typen += GodotDatentyp.Transform3D
            4 to 4 -> typen += GodotDatentyp.Projection
        }
    }

    return typen.map(::GodotDatentypEtikett)
}

@Composable
fun GodotDatentypEtiketten(
    etiketten: List<GodotDatentypEtikett>,
    modifier: Modifier = Modifier,
) {
    if (etiketten.isEmpty()) return
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        etiketten.distinctBy { it.typ }.chunked(3).forEach { zeile ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                zeile.forEach(::GodotDatentypMarke)
            }
        }
    }
}

@Composable
private fun GodotDatentypMarke(etikett: GodotDatentypEtikett) {
    Surface(
        modifier = Modifier.semantics {
            contentDescription = "Godot-Datentyp ${etikett.typ.anzeigeName}: ${etikett.begründung}"
        },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GodotDatentypSymbol(
                typ = etikett.typ,
                modifier = Modifier.size(width = 20.dp, height = 15.dp),
            )
            Text(etikett.typ.anzeigeName, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun GodotDatentypSymbol(
    typ: GodotDatentyp,
    modifier: Modifier = Modifier,
) {
    val pfade = remember(typ) {
        typ.ebenen.map { ebene ->
            GezeichneterGodotPfad(
                pfad = PathParser().parsePathString(ebene.pfad).toPath(),
                farbe = ebene.farbe,
            )
        }
    }
    Canvas(modifier) {
        val faktor = minOf(size.width / GODOT_GLYPH_BREITE, size.height / GODOT_GLYPH_HÖHE)
        val breite = GODOT_GLYPH_BREITE * faktor
        val höhe = GODOT_GLYPH_HÖHE * faktor
        withTransform({
            translate((size.width - breite) / 2f, (size.height - höhe) / 2f)
            scale(faktor, faktor, pivot = Offset.Zero)
        }) {
            pfade.forEach { drawPath(it.pfad, it.farbe) }
        }
    }
}

private fun MathematischesObjekt.flacheZahlKomponenten(): List<ZahlAusdruck>? = when (this) {
    is OrientierterVektor -> werte
    is Tupel -> elemente.mapNotNull { it as? ZahlAusdruck }.takeIf { it.size == elemente.size }
    else -> null
}

private fun MathematischesObjekt.zahlKomponentenBlöcke(
    anzahl: Int,
    dimension: Int,
): List<List<ZahlAusdruck>>? {
    val tupel = this as? Tupel ?: return null
    if (tupel.elemente.size != anzahl) return null
    val blöcke = tupel.elemente.map { element -> element.flacheZahlKomponenten() ?: return null }
    return blöcke.takeIf { it.all { block -> block.size == dimension } }
}

private fun List<ZahlAusdruck>.sindGanzzahlig(): Boolean =
    all { zahl -> zahl is RationaleZahl && zahl.nenner == BigInteger.ONE }

internal data class GodotGlyphEbene(val farbe: Color, val pfad: String)
private data class GezeichneterGodotPfad(val pfad: Path, val farbe: Color)

private fun ebene(argb: Long, pfad: String) = GodotGlyphEbene(Color(argb), pfad)

private const val GODOT_GLYPH_BREITE = 16f
private const val GODOT_GLYPH_HÖHE = 12f

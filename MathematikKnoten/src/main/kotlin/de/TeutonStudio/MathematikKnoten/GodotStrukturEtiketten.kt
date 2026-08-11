package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenInteraktionsModus
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRenderer
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRendererAktionen
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.math.BigInteger

/**
 * Godot-Variant-Strukturtypen, für die Orchestrator einen Struct-Pin verwendet.
 *
 * Orchestrator bezieht die sichtbaren Glyphen für diese Pins aus Godots
 * `EditorIcons`. Die unten gespeicherten Pfade sind exakt aus diesen Godot-SVGs
 * übernommen; siehe THIRD_PARTY_NOTICES.md.
 */
enum class GodotStrukturTyp(val godotName: String, internal val icon: GodotEditorIcon) {
    VECTOR2("Vector2", GodotEditorIcon.Vector2),
    VECTOR2I("Vector2i", GodotEditorIcon.Vector2i),
    VECTOR3("Vector3", GodotEditorIcon.Vector3),
    VECTOR3I("Vector3i", GodotEditorIcon.Vector3i),
    VECTOR4("Vector4", GodotEditorIcon.Vector4),
    VECTOR4I("Vector4i", GodotEditorIcon.Vector4i),
    RECT2("Rect2", GodotEditorIcon.Rect2),
    RECT2I("Rect2i", GodotEditorIcon.Rect2i),
    TRANSFORM2D("Transform2D", GodotEditorIcon.Transform2D),
    TRANSFORM3D("Transform3D", GodotEditorIcon.Transform3D),
    PLANE("Plane", GodotEditorIcon.Plane),
    QUATERNION("Quaternion", GodotEditorIcon.Quaternion),
    PROJECTION("Projection", GodotEditorIcon.Projection),
    AABB("AABB", GodotEditorIcon.Aabb),
    BASIS("Basis", GodotEditorIcon.Basis),
}

data class GodotStrukturEtikett(
    val typ: GodotStrukturTyp,
    val ausgangName: String,
)

/**
 * Leitet genau eine primäre Godot-Strukturrepräsentation aus einem Atlas-Objekt ab.
 *
 * Die Zuordnung ist strukturell und konservativ. Insbesondere werden Tupel sowie
 * Zeilen-/Spaltenvektoren gemeinsam als Godot-Vektoren erkannt. Semantisch
 * mehrdeutige Vierertupel bleiben standardmäßig Vector4; Quaternion wird nur mit
 * einem entsprechenden semantischen Hinweis gewählt.
 */
fun MathematischesObjekt.godotStrukturTyp(
    semantikHinweis: String = "",
): GodotStrukturTyp? {
    val hinweis = semantikHinweis.lowercase()

    if (("quaternion" in hinweis || "quat" in hinweis) && istNumerischerVektor(4)) {
        return GodotStrukturTyp.QUATERNION
    }
    if (("plane" in hinweis || "ebene" in hinweis) && istPlaneStruktur()) {
        return GodotStrukturTyp.PLANE
    }

    if (istTransform3DStruktur()) return GodotStrukturTyp.TRANSFORM3D
    if (istTransform2DStruktur()) return GodotStrukturTyp.TRANSFORM2D
    if (istAabbStruktur()) return GodotStrukturTyp.AABB
    if (istRect2Struktur()) {
        return if (alleSkalareGanzzahlig()) GodotStrukturTyp.RECT2I else GodotStrukturTyp.RECT2
    }
    if (istPlaneStruktur()) return GodotStrukturTyp.PLANE

    if (this is Matrix) {
        return when {
            zeilenAnzahl == 4 && spaltenAnzahl == 4 -> GodotStrukturTyp.PROJECTION
            zeilenAnzahl == 3 && spaltenAnzahl == 3 -> GodotStrukturTyp.BASIS
            else -> null
        }
    }

    val komponenten = numerischeVektorKomponenten() ?: return null
    val ganzzahlig = komponenten.all(ZahlAusdruck::istExaktGanzzahlig)
    return when (komponenten.size) {
        2 -> if (ganzzahlig) GodotStrukturTyp.VECTOR2I else GodotStrukturTyp.VECTOR2
        3 -> if (ganzzahlig) GodotStrukturTyp.VECTOR3I else GodotStrukturTyp.VECTOR3
        4 -> if (ganzzahlig) GodotStrukturTyp.VECTOR4I else GodotStrukturTyp.VECTOR4
        else -> null
    }
}

/** Liefert die aus den tatsächlichen Knotenausgaben abgeleiteten Etiketten. */
fun KnotenAuswertungsErgebnis.godotStrukturEtiketten(knoten: KnotenDaten): List<GodotStrukturEtikett> =
    ausgaben.mapNotNull { (ausgangName, wert) ->
        val hinweis = "${knoten.art} ${knoten.name} $ausgangName"
        wert.objekt.godotStrukturTyp(hinweis)?.let { typ -> GodotStrukturEtikett(typ, ausgangName) }
    }.distinctBy { it.typ to it.ausgangName }

/**
 * Dekoriert beliebige Knotendarstellungen mit Godot-Typetiketten, ohne Knotenmodell,
 * Größe, Anschlüsse oder Persistenz zu verändern.
 */
fun KnotenRenderer.mitGodotStrukturEtiketten(
    ergebnisFür: (KnotenDaten) -> KnotenAuswertungsErgebnis?,
): KnotenRenderer {
    val basis = this
    return object : KnotenRenderer {
        override val interaktionsModus: KnotenInteraktionsModus get() = basis.interaktionsModus

        @Composable
        override fun Inhalt(knoten: KnotenDaten, ausgewählt: Boolean, aktionen: KnotenRendererAktionen) {
            Box(Modifier.fillMaxSize()) {
                basis.Inhalt(knoten, ausgewählt, aktionen)
                val etiketten = ergebnisFür(knoten)?.godotStrukturEtiketten(knoten).orEmpty()
                if (etiketten.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 5.dp, end = 44.dp)
                            .zIndex(8f),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        etiketten.forEach(GodotStrukturEtikett::Darstellung)
                    }
                }
            }
        }

        @Composable
        override fun Fußzeile(knoten: KnotenDaten, ausgewählt: Boolean) {
            basis.Fußzeile(knoten, ausgewählt)
        }
    }
}

@Composable
private fun GodotStrukturEtikett.Darstellung() {
    Surface(
        shape = RoundedCornerShape(5.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = .96f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp,
        modifier = Modifier.semantics {
            contentDescription = "Godot-Typ ${typ.godotName}, Ausgang $ausgangName"
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            GodotEditorIcon(typ.icon, Modifier.size(16.dp, 12.dp))
            Text(typ.godotName, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

private fun MathematischesObjekt.numerischeVektorKomponenten(): List<ZahlAusdruck>? = when (this) {
    is OrientierterVektor -> werte
    is Tupel -> elemente.map { it as? ZahlAusdruck ?: return null }
    is Matrix -> when {
        zeilenAnzahl == 1 -> zeilen.single()
        spaltenAnzahl == 1 -> zeilen.map { it.single() }
        else -> null
    }
    else -> null
}

private fun MathematischesObjekt.istNumerischerVektor(dimension: Int): Boolean =
    numerischeVektorKomponenten()?.size == dimension

private fun ZahlAusdruck.istExaktGanzzahlig(): Boolean =
    this is RationaleZahl && nenner == BigInteger.ONE

private fun MathematischesObjekt.alleSkalareGanzzahlig(): Boolean = when (this) {
    is ZahlAusdruck -> istExaktGanzzahlig()
    is OrientierterVektor -> werte.all(ZahlAusdruck::istExaktGanzzahlig)
    is Tupel -> elemente.all(MathematischesObjekt::alleSkalareGanzzahlig)
    is Matrix -> zeilen.flatten().all(ZahlAusdruck::istExaktGanzzahlig)
    else -> false
}

private fun MathematischesObjekt.istRect2Struktur(): Boolean =
    this is Tupel && elemente.size == 2 && elemente.all { it.istNumerischerVektor(2) }

private fun MathematischesObjekt.istAabbStruktur(): Boolean =
    this is Tupel && elemente.size == 2 && elemente.all { it.istNumerischerVektor(3) }

private fun MathematischesObjekt.istPlaneStruktur(): Boolean =
    this is Tupel && elemente.size == 2 && elemente[0].istNumerischerVektor(3) && elemente[1] is ZahlAusdruck

private fun MathematischesObjekt.istBasisStruktur(): Boolean = when (this) {
    is Matrix -> zeilenAnzahl == 3 && spaltenAnzahl == 3
    is Tupel -> elemente.size == 3 && elemente.all { it.istNumerischerVektor(3) }
    else -> false
}

private fun MathematischesObjekt.istTransform2DStruktur(): Boolean = when (this) {
    is Matrix -> zeilenAnzahl == 3 && spaltenAnzahl == 3 && letzteZeileIst(0, 0, 1)
    is Tupel -> elemente.size == 3 && elemente.all { it.istNumerischerVektor(2) }
    else -> false
}

private fun MathematischesObjekt.istTransform3DStruktur(): Boolean = when (this) {
    is Matrix -> zeilenAnzahl == 4 && spaltenAnzahl == 4 && letzteZeileIst(0, 0, 0, 1)
    is Tupel -> elemente.size == 2 && elemente[0].istBasisStruktur() && elemente[1].istNumerischerVektor(3)
    else -> false
}

private fun Matrix.letzteZeileIst(vararg werte: Long): Boolean {
    if (spaltenAnzahl != werte.size) return false
    return zeilen.last().zip(werte.asList()).all { (wert, erwartet) ->
        wert is RationaleZahl && wert == RationaleZahl.von(erwartet)
    }
}

/** Exakt übernommene Godot-EditorIcons der von Orchestrator verwendeten Struct-Typen. */
internal enum class GodotEditorIcon(
    val breite: Float,
    val höhe: Float,
    val pfade: List<GodotIconPfad>,
) {
    Vector2(16f, 12f, listOf(
        pfad(0xAC73F1, "M12 2v2h1a1 1 0 0 1 0 2 2 2 0 0 0-2 2v2h5V8h-3a3 3 0 0 0 0-6zM1 4v6h2a3 3 0 0 0 3-3V4H4v3a1 1 0 0 1-1 1V4zm9 6V8H9a1 1 0 0 1 0-2h1V4H9a3 3 0 0 0 0 6z"),
        pfad(0xFFFFFF, "M12 2v2h1a1 1 0 0 1 0 2 2 2 0 0 0-2 2v2h5V8h-3a3 3 0 0 0 0-6z", .4f),
    )),
    Vector2i(16f, 12f, listOf(
        pfad(0xAC73F1, "M8 2v2h1a1 1 0 0 1 0 2 2 2 0 0 0-2 2v2h5V8H9a3 3 0 0 0 0-6zM1 4v6h2a3 3 0 0 0 3-3V4H4v3a1 1 0 0 1-1 1V4z"),
        pfad(0xFFFFFF, "M8 2v2h1a1 1 0 0 1 0 2 2 2 0 0 0-2 2v2h5V8H9a3 3 0 0 0 0-6z", .4f),
        pfad(0x5ABBEF, "M13 2v2h2V2zm0 4v4h2V6z"),
    )),
    Vector3(16f, 12f, listOf(
        pfad(0xDE66F0, "M11 2v2h2a1 1 0 0 1-1 1v2a1 1 0 0 1 0 2h-1v2h1a3 3 0 0 0 2.232-5A3 3 0 0 0 15 4V2zM1 4v6h2a3 3 0 0 0 3-3V4H4v3a1 1 0 0 1-1 1V4zm9 6V8H9a1 1 0 0 1 0-2h1V4H9a3 3 0 0 0 0 6z"),
        pfad(0xFFFFFF, "M11 2v2h2a1 1 0 0 1-1 1v2a1 1 0 0 1 0 2h-1v2h1a3 3 0 0 0 2.232-5A3 3 0 0 0 15 4V2z", .4f),
    )),
    Vector3i(16f, 12f, listOf(
        pfad(0xDE66F0, "M8 2v2h2a1 1 0 0 1-1 1v2a1 1 0 0 1 0 2H8v2h1a3 3 0 0 0 2.232-5A3 3 0 0 0 12 4V2zM1 4v6h2a3 3 0 0 0 3-3V4H4v3a1 1 0 0 1-1 1V4z"),
        pfad(0xFFFFFF, "M8 2v2h2a1 1 0 0 1-1 1v2a1 1 0 0 1 0 2H8v2h1a3 3 0 0 0 2.232-5A3 3 0 0 0 12 4V2z", .4f),
        pfad(0x5ABBEF, "M13 2v2h2V2zm0 4v4h2V6z"),
    )),
    Vector4(16f, 12f, listOf(
        pfad(0xF066BD, "M11 3v5h3v3h2V2h-2v4h-1V3zM1 4v6h2a3 3 0 0 0 3-3V4H4v3a1 1 0 0 1-1 1V4zm9 6V8H9a1 1 0 0 1 0-2h1V4H9a3 3 0 0 0 0 6z"),
        pfad(0xFFFFFF, "M11 3v5h3v3h2V2h-2v4h-1V3z", .4f),
    )),
    Vector4i(16f, 12f, listOf(
        pfad(0xF066BD, "M7 3v5h3v3h2V2h-2v4H9V3zM1 4v6h2a3 3 0 0 0 3-3V4H4v3a1 1 0 0 1-1 1V4z"),
        pfad(0xFFFFFF, "M7 3v5h3v3h2V2h-2v4H9V3z", .4f),
        pfad(0x5ABBEF, "M13 2v2h2V2zm0 4v4h2V6z"),
    )),
    Rect2(16f, 12f, listOf(
        pfad(0xF1738F, "M13 2v2h-1a3 3 0 0 0-2.5 1.346A3 3 0 1 0 7 10h1V8H7a1 1 0 0 1-1-1h3a3 3 0 0 0 3 3h1V8h-1a1 1 0 0 1 0-2h1v1a3 3 0 0 0 3 3V8a1 1 0 0 1-1-1V6h1V4h-1V2zM3 4a3 3 0 0 0-3 3v3h2V7a1 1 0 0 1 1-1h1V4z"),
    )),
    Rect2i(16f, 12f, listOf(
        pfad(0xF1738F, "M9 2v2H8a3 3 0 0 0 0 6h1V8H8a1 1 0 0 1 0-2h1v1a3 3 0 0 0 3 3V8a1 1 0 0 1-1-1V6h1V4h-1V2zM4 4a3 3 0 0 0-3 3v3h2V7a1 1 0 0 1 1-1h1V4z"),
        pfad(0x5ABBEF, "M13 2v2h2V2zm0 4v4h2V6z"),
    )),
    Transform2D(16f, 12f, listOf(
        pfad(0xB9EC41, "M0 2v2h2v6h2V4h3a1 1 0 0 1 0 2 2 2 0 0 0-2 2v2h7a4 4 0 0 0 0-8h-2v6H7a3 3 0 0 0 0-6zm12 2a2 2 0 0 1 0 4z"),
        pfad(0xFFFFFF, "M6.5 2v2H7a1 1 0 0 1 0 2 2 2 0 0 0-2 2v2h5V8H7a3 3 0 0 0 0-6h-.5z", .4f),
    )),
    Transform3D(16f, 12f, listOf(
        pfad(0xF68F45, "M4 4h3.349a1 1.267 0 0 1-1 1.267v1a.866.866 0 0 1 0 1.732h-1v2h1a2.81 2.81 0 0 0 2.232-4.734A1.32 1.32 0 0 0 9.345 4V2H0v2h2v6h2zm6 6h2a4 4 0 0 0 0-8h-2zm2-6a2 2 0 0 1 0 4z"),
        pfad(0xFFFFFF, "M5.5 4h1.849a1 1.267 0 0 1-1 1.267v1a.866.866 0 0 1 0 1.732h-1v2h1a2.81 2.81 0 0 0 2.232-4.734A1.32 1.32 0 0 0 9.345 4V2H5.5z", .4f),
    )),
    Plane(16f, 12f, listOf(
        pfad(0xF74949, "M1 2v8h2V8a3 3 0 0 0 0-6zm6 0v5a3 3 0 0 0 3 3V8a1 1 0 0 1-1-1V2zM3 4a1 1 0 0 1 0 2zm8 0v6h2V6a1 1 0 0 1 1 1v3h2V7a3 3 0 0 0-3-3z"),
    )),
    Quaternion(16f, 12f, listOf(
        pfad(0xEC418E, "M3 3a3 3 0 0 0 0 6v2h2V8.236A3 3 0 0 0 7 9h2v-.764A3 3 0 0 0 11 9h2V6a3 3 0 0 0 3 3V7a1 1 0 0 1-1-1V5h1V3h-1V1h-2v2h-2a3 3 0 0 0-2 .764V3H7v4a1 1 0 0 1-1-1V3zm0 4a1 1 0 0 1 0-2zm8 0a1 1 0 0 1 0-2z"),
        pfad(0xFFFFFF, "M4 3v3a3 3 0 0 0 3 3h2V3H7v4a1 1 0 0 1-1-1V3z", .4f),
    )),
    Projection(16f, 12f, listOf(
        pfad(0x44BD44, "M0 2v8h2V8a3 3 0 0 0 2-.779V10h2V7a1 1 0 0 1 1-1h1V4H7a3 3 0 0 0-2.02.795A3 3 0 0 0 2 2zm8 4v2a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-1a2 2 0 0 0-2 2zm6-4v2h2V2zM2 4a1 1 0 0 1 0 2zm8 2h1v2h-1zm4 0v3a1 1 0 0 1-1 1h-1v2h1a3 3 0 0 0 3-3V6z"),
        pfad(0xFFFFFF, "M7 4a3 3 0 0 0-3 3v3h2V7a1 1 0 0 1 1-1h1V4z", .4f),
    )),
    Aabb(16f, 12f, listOf(
        pfad(0xEE5677, "M5 1a3 3 0 0 0-2.223 5.012A3 3 0 0 0 3 12h2V7h2V1H5zm6 0v5.174A3 3 0 0 0 10 6V4H8v8h2a3 3 0 0 0 3-3 3 3 0 0 0 0-6V1h-2zM5 3v2a1 1 0 0 1 0-2zm8 2a1 1 0 0 1 0 2zM3 8v2a1 1 0 0 1 0-2zm7 0a1 1 0 0 1 0 2z"),
        pfad(0xFFFFFF, "M8 4v8h2a3 3 0 0 0 0-6V4zM3 6a3 3 0 0 0 0 6h2V6zm0 2v2a1 1 0 0 1 0-2zm7 0a1 1 0 0 1 0 2z", .235f),
    )),
    Basis(16f, 12f, listOf(
        pfad(0xE1EC41, "M0 2v8h2a3 3 0 0 0 0-6V2zm10 0v2h2V2zM7 4a2 2 0 0 0 0 4H5v2h2a2 2 0 0 0 0-4h2V4zm7 0a2 2 0 0 0 0 4h-2V6h-2v4h4a2 2 0 0 0 0-4h2V4zM2 6a1 1 0 0 1 1 1 1 1 0 0 1-1 1z"),
        pfad(0xFFFFFF, "M10 2v2h2V2zm0 4v4h2V6z", .4f),
    )),
}

internal data class GodotIconPfad(
    val farbe: Color,
    val daten: String,
    val alpha: Float = 1f,
)

private fun pfad(rgb: Long, daten: String, alpha: Float = 1f) = GodotIconPfad(
    farbe = Color(0xFF000000L or rgb),
    daten = daten,
    alpha = alpha,
)

@Composable
internal fun GodotEditorIcon(icon: GodotEditorIcon, modifier: Modifier = Modifier) {
    val pfade = remember(icon) {
        icon.pfade.map { definition ->
            definition to PathParser().parsePathString(definition.daten).toPath()
        }
    }
    Canvas(modifier = modifier) {
        val faktor = minOf(size.width / icon.breite, size.height / icon.höhe)
        val versatzX = (size.width - icon.breite * faktor) / 2f
        val versatzY = (size.height - icon.höhe * faktor) / 2f
        withTransform({
            translate(versatzX, versatzY)
            scale(faktor, faktor)
        }) {
            pfade.forEach { (definition, path) ->
                drawPath(path, definition.farbe.copy(alpha = definition.alpha))
            }
        }
    }
}
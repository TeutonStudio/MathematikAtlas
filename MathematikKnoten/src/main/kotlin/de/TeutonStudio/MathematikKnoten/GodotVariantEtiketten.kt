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
import androidx.compose.ui.geometry.Offset
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
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.istNachweisbarReell
import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.math.BigInteger

/**
 * Vollständiger endlicher Godot-Variant-Typkatalog, wie er von Orchestrator für
 * Nicht-Ausführungs-Pins an Godots `EditorIcons` weitergereicht wird.
 *
 * `Variant` entspricht dabei dem sichtbaren NIL-as-Variant-Fall. Reine Execution-
 * Pins besitzen in Orchestrator kein Typicon. Konkrete Object-Unterklassen liegen
 * außerhalb dieses endlichen Katalogs und werden später über die Godot-ClassDB
 * aufgelöst.
 */
enum class GodotVariantTyp(
    val godotName: String,
    internal val icon: GodotIconGrafik,
) {
    VARIANT("Variant", GodotIcons.Variant),
    BOOL("bool", GodotIcons.Bool),
    INT("int", GodotIcons.Int),
    FLOAT("float", GodotIcons.Float),
    STRING("String", GodotIcons.String),

    VECTOR2("Vector2", GodotIcons.Vector2),
    VECTOR2I("Vector2i", GodotIcons.Vector2i),
    RECT2("Rect2", GodotIcons.Rect2),
    RECT2I("Rect2i", GodotIcons.Rect2i),
    VECTOR3("Vector3", GodotIcons.Vector3),
    VECTOR3I("Vector3i", GodotIcons.Vector3i),
    TRANSFORM2D("Transform2D", GodotIcons.Transform2D),
    VECTOR4("Vector4", GodotIcons.Vector4),
    VECTOR4I("Vector4i", GodotIcons.Vector4i),
    PLANE("Plane", GodotIcons.Plane),
    QUATERNION("Quaternion", GodotIcons.Quaternion),
    AABB("AABB", GodotIcons.Aabb),
    BASIS("Basis", GodotIcons.Basis),
    TRANSFORM3D("Transform3D", GodotIcons.Transform3D),
    PROJECTION("Projection", GodotIcons.Projection),

    COLOR("Color", GodotIcons.Color),
    STRING_NAME("StringName", GodotIcons.StringName),
    NODE_PATH("NodePath", GodotIcons.NodePath),
    RID("RID", GodotIcons.Rid),
    OBJECT("Object", GodotIcons.Object),
    CALLABLE("Callable", GodotIcons.Callable),
    SIGNAL("Signal", GodotIcons.Signal),
    DICTIONARY("Dictionary", GodotIcons.Dictionary),
    ARRAY("Array", GodotIcons.Array),

    PACKED_BYTE_ARRAY("PackedByteArray", GodotIcons.PackedByteArray),
    PACKED_INT32_ARRAY("PackedInt32Array", GodotIcons.PackedInt32Array),
    PACKED_INT64_ARRAY("PackedInt64Array", GodotIcons.PackedInt64Array),
    PACKED_FLOAT32_ARRAY("PackedFloat32Array", GodotIcons.PackedFloat32Array),
    PACKED_FLOAT64_ARRAY("PackedFloat64Array", GodotIcons.PackedFloat64Array),
    PACKED_STRING_ARRAY("PackedStringArray", GodotIcons.PackedStringArray),
    PACKED_VECTOR2_ARRAY("PackedVector2Array", GodotIcons.PackedVector2Array),
    PACKED_VECTOR3_ARRAY("PackedVector3Array", GodotIcons.PackedVector3Array),
    PACKED_COLOR_ARRAY("PackedColorArray", GodotIcons.PackedColorArray),
    PACKED_VECTOR4_ARRAY("PackedVector4Array", GodotIcons.PackedVector4Array),
}

/**
 * Ein sichtbares Godot-Typetikett. Bei Array/Dictionary entsprechen [elementTyp],
 * [schlüsselTyp] und [wertTyp] Orchestrators typisierter Icon-Komposition.
 */
data class GodotVariantEtikett(
    val typ: GodotVariantTyp,
    val ausgangName: String,
    val elementTyp: GodotVariantTyp? = null,
    val schlüsselTyp: GodotVariantTyp? = null,
    val wertTyp: GodotVariantTyp? = null,
)

private data class GodotTypAbleitung(
    val typ: GodotVariantTyp,
    val elementTyp: GodotVariantTyp? = null,
)

/**
 * Mathematische Strukturkompatibilität mit Godots eingebauten Math-Variants.
 *
 * Ein numerisches n-Tupel, ein Zeilen-/Spaltenvektor und eine 1×n-/n×1-Matrix
 * sind für n=2,3,4 dieselbe Godot-Vektorstruktur. Ganzzahlige konkrete Komponenten
 * wählen die i-Variante. Semantisch mehrdeutige Vierertupel bleiben Vector4;
 * Color und Quaternion benötigen einen expliziten semantischen Hinweis.
 */
fun MathematischesObjekt.godotStrukturVariantTyp(
    semantikHinweis: String = "",
): GodotVariantTyp? {
    val hinweis = semantikHinweis.lowercase()

    if (("color" in hinweis || "farbe" in hinweis || "rgba" in hinweis) && istNumerischerVektor(4)) {
        return GodotVariantTyp.COLOR
    }
    if (("quaternion" in hinweis || "quat" in hinweis) && istNumerischerVektor(4)) {
        return GodotVariantTyp.QUATERNION
    }
    if (("plane" in hinweis || "ebene" in hinweis) && istPlaneStruktur()) {
        return GodotVariantTyp.PLANE
    }

    if (istTransform3DStruktur()) return GodotVariantTyp.TRANSFORM3D
    if (istTransform2DStruktur()) return GodotVariantTyp.TRANSFORM2D
    if (istAabbStruktur()) return GodotVariantTyp.AABB
    if (istRect2Struktur()) {
        return if (alleSkalareGanzzahlig()) GodotVariantTyp.RECT2I else GodotVariantTyp.RECT2
    }
    if (istPlaneStruktur()) return GodotVariantTyp.PLANE

    if (this is Matrix) {
        when {
            zeilenAnzahl == 4 && spaltenAnzahl == 4 -> return GodotVariantTyp.PROJECTION
            zeilenAnzahl == 3 && spaltenAnzahl == 3 -> return GodotVariantTyp.BASIS
        }
    }

    val komponenten = numerischeVektorKomponenten() ?: return null
    val ganzzahlig = komponenten.all { it.istExaktGanzzahlig() }
    return when (komponenten.size) {
        2 -> if (ganzzahlig) GodotVariantTyp.VECTOR2I else GodotVariantTyp.VECTOR2
        3 -> if (ganzzahlig) GodotVariantTyp.VECTOR3I else GodotVariantTyp.VECTOR3
        4 -> if (ganzzahlig) GodotVariantTyp.VECTOR4I else GodotVariantTyp.VECTOR4
        else -> null
    }
}

/**
 * Leitet aus einem tatsächlichen Atlas-Ausgang genau einen primären Godot-Variant-
 * Typ ab. Die Regel ist absichtlich strenger als bloße Serialisierbarkeit.
 */
private fun BedingterWert.godotTypAbleitung(semantikHinweis: String): GodotTypAbleitung? {
    objekt.godotStrukturVariantTyp(semantikHinweis)?.let { return GodotTypAbleitung(it) }

    when (val wert = objekt) {
        is Aussage -> return GodotTypAbleitung(GodotVariantTyp.BOOL)
        is RationaleZahl -> return GodotTypAbleitung(
            if (wert.istGodotInt()) GodotVariantTyp.INT else GodotVariantTyp.FLOAT,
        )
        is ZahlAusdruck -> if (wert !is KomplexeZahl && istNachweisbarReell()) {
            return GodotTypAbleitung(GodotVariantTyp.FLOAT)
        }
        is Tupel -> return wert.godotArrayAbleitung(semantikHinweis)
    }
    return null
}

/**
 * Längere, nicht bereits als Vector2/3/4 klassifizierte Tupel können als Godot-
 * Array oder als passendes PackedArray dargestellt werden, wenn alle Elemente eine
 * eindeutige homogene Godot-Repräsentation besitzen.
 */
private fun Tupel.godotArrayAbleitung(semantikHinweis: String): GodotTypAbleitung? {
    if (elemente.isEmpty()) return GodotTypAbleitung(GodotVariantTyp.ARRAY)

    val elementTypen = elemente.map { element ->
        element.godotElementTyp(semantikHinweis) ?: return GodotTypAbleitung(GodotVariantTyp.ARRAY)
    }
    val einheitlich = elementTypen.distinct().singleOrNull()
        ?: return GodotTypAbleitung(GodotVariantTyp.ARRAY)

    val packed = when (einheitlich) {
        GodotVariantTyp.INT -> GodotVariantTyp.PACKED_INT64_ARRAY
        GodotVariantTyp.FLOAT -> GodotVariantTyp.PACKED_FLOAT64_ARRAY
        GodotVariantTyp.VECTOR2 -> GodotVariantTyp.PACKED_VECTOR2_ARRAY
        GodotVariantTyp.VECTOR3 -> GodotVariantTyp.PACKED_VECTOR3_ARRAY
        GodotVariantTyp.VECTOR4 -> GodotVariantTyp.PACKED_VECTOR4_ARRAY
        GodotVariantTyp.COLOR -> GodotVariantTyp.PACKED_COLOR_ARRAY
        else -> null
    }
    return packed?.let { typ -> GodotTypAbleitung(typ) }
        ?: GodotTypAbleitung(GodotVariantTyp.ARRAY, elementTyp = einheitlich)
}

private fun MathematischesObjekt.godotElementTyp(semantikHinweis: String): GodotVariantTyp? {
    godotStrukturVariantTyp(semantikHinweis)?.let { return it }
    return when (this) {
        is Aussage -> GodotVariantTyp.BOOL
        is RationaleZahl -> if (istGodotInt()) GodotVariantTyp.INT else GodotVariantTyp.FLOAT
        else -> null
    }
}

/** Liefert die aus den tatsächlichen Knotenausgaben abgeleiteten Etiketten. */
fun KnotenAuswertungsErgebnis.godotVariantEtiketten(knoten: KnotenDaten): List<GodotVariantEtikett> =
    ausgaben.mapNotNull { (ausgangName, wert) ->
        val hinweis = "${knoten.art} ${knoten.name} $ausgangName"
        wert.godotTypAbleitung(hinweis)?.let { ableitung ->
            GodotVariantEtikett(
                typ = ableitung.typ,
                ausgangName = ausgangName,
                elementTyp = ableitung.elementTyp,
            )
        }
    }.distinctBy { etikett ->
        listOf(
            etikett.typ.godotName,
            etikett.ausgangName,
            etikett.elementTyp?.godotName,
            etikett.schlüsselTyp?.godotName,
            etikett.wertTyp?.godotName,
        )
    }

/**
 * Dekoriert einen Renderer mit Godot-Typetiketten, ohne Knotenmodell, Größe,
 * Anschlüsse oder Persistenz zu verändern.
 */
fun KnotenRenderer.mitGodotVariantEtiketten(
    ergebnisFür: (KnotenDaten) -> KnotenAuswertungsErgebnis?,
): KnotenRenderer {
    val basis = this
    return object : KnotenRenderer {
        override val interaktionsModus: KnotenInteraktionsModus get() = basis.interaktionsModus

        @Composable
        override fun Inhalt(knoten: KnotenDaten, ausgewählt: Boolean, aktionen: KnotenRendererAktionen) {
            Box(Modifier.fillMaxSize()) {
                basis.Inhalt(knoten, ausgewählt, aktionen)
                val etiketten = ergebnisFür(knoten)?.godotVariantEtiketten(knoten).orEmpty()
                if (etiketten.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 5.dp, end = 44.dp)
                            .zIndex(8f),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        for (etikett in etiketten) etikett.Darstellung()
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
private fun GodotVariantEtikett.Darstellung() {
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
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            GodotIcon(typ.icon, Modifier.size(16.dp, typ.icon.anzeigeHöheDp.dp))
            when (typ) {
                GodotVariantTyp.ARRAY -> elementTyp?.let { element ->
                    Text("[", style = MaterialTheme.typography.labelSmall)
                    GodotIcon(element.icon, Modifier.size(16.dp, element.icon.anzeigeHöheDp.dp))
                    Text("]", style = MaterialTheme.typography.labelSmall)
                }
                GodotVariantTyp.DICTIONARY -> if (schlüsselTyp != null && wertTyp != null) {
                    Text("[", style = MaterialTheme.typography.labelSmall)
                    GodotIcon(schlüsselTyp.icon, Modifier.size(16.dp, schlüsselTyp.icon.anzeigeHöheDp.dp))
                    Spacer(Modifier.width(3.dp))
                    GodotIcon(wertTyp.icon, Modifier.size(16.dp, wertTyp.icon.anzeigeHöheDp.dp))
                    Text("]", style = MaterialTheme.typography.labelSmall)
                }
                else -> Unit
            }
            Text(typ.godotName, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

private fun MathematischesObjekt.numerischeVektorKomponenten(): List<ZahlAusdruck>? = when (this) {
    is OrientierterVektor -> werte
    is Tupel -> elemente.map { element -> element as? ZahlAusdruck ?: return null }
    is Matrix -> when {
        zeilenAnzahl == 1 -> zeilen.single()
        spaltenAnzahl == 1 -> zeilen.map { zeile -> zeile.single() }
        else -> null
    }
    else -> null
}

private fun MathematischesObjekt.istNumerischerVektor(dimension: Int): Boolean =
    numerischeVektorKomponenten()?.size == dimension

private fun ZahlAusdruck.istExaktGanzzahlig(): Boolean =
    this is RationaleZahl && nenner == BigInteger.ONE

private fun RationaleZahl.istGodotInt(): Boolean =
    nenner == BigInteger.ONE && zähler.bitLength() <= 63

private fun MathematischesObjekt.alleSkalareGanzzahlig(): Boolean = when (this) {
    is ZahlAusdruck -> istExaktGanzzahlig()
    is OrientierterVektor -> werte.all { wert -> wert.istExaktGanzzahlig() }
    is Tupel -> elemente.all { element -> element.alleSkalareGanzzahlig() }
    is Matrix -> zeilen.flatten().all { wert -> wert.istExaktGanzzahlig() }
    else -> false
}

private fun MathematischesObjekt.istRect2Struktur(): Boolean =
    this is Tupel && elemente.size == 2 && elemente.all { element -> element.istNumerischerVektor(2) }

private fun MathematischesObjekt.istAabbStruktur(): Boolean =
    this is Tupel && elemente.size == 2 && elemente.all { element -> element.istNumerischerVektor(3) }

private fun MathematischesObjekt.istPlaneStruktur(): Boolean =
    this is Tupel && elemente.size == 2 && elemente[0].istNumerischerVektor(3) && elemente[1] is ZahlAusdruck

private fun MathematischesObjekt.istBasisStruktur(): Boolean = when (this) {
    is Matrix -> zeilenAnzahl == 3 && spaltenAnzahl == 3
    is Tupel -> elemente.size == 3 && elemente.all { element -> element.istNumerischerVektor(3) }
    else -> false
}

private fun MathematischesObjekt.istTransform2DStruktur(): Boolean = when (this) {
    is Matrix -> zeilenAnzahl == 3 && spaltenAnzahl == 3 && letzteZeileIst(0, 0, 1)
    is Tupel -> elemente.size == 3 && elemente.all { element -> element.istNumerischerVektor(2) }
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

internal data class GodotIconGrafik(
    val breite: Float,
    val höhe: Float,
    val pfade: List<GodotIconPfad>,
) {
    val anzeigeHöheDp: Float get() = 16f * höhe / breite
}

internal data class GodotIconPfad(
    val farbe: Color,
    val daten: String,
    val alpha: Float = 1f,
)

private fun grafik(
    breite: Float = 16f,
    höhe: Float = 12f,
    vararg pfade: GodotIconPfad,
) = GodotIconGrafik(breite, höhe, pfade.toList())

/** Helfer für aus SVG-Einpfaddefinitionen direkt übernommene Arrays. */
private fun grafik(
    pfade: Array<GodotIconPfad>,
    breite: Float = 16f,
    höhe: Float = 12f,
) = GodotIconGrafik(breite, höhe, pfade.toList())

private fun pfad(rgb: Long, daten: String, alpha: Float = 1f) = GodotIconPfad(
    farbe = Color(0xFF000000L or rgb),
    daten = daten,
    alpha = alpha,
)

/** Exakt aus Godot `editor/icons` extrahierte SVG-Pfade, Stand siehe THIRD_PARTY_NOTICES.md. */
private object GodotIcons {
    val Variant = grafik(pfade = arrayOf(pfad(0x41ECAD, "M3 3a3 3 0 0 0 0 6h2V3zm3 0v6h2V5a1 1 0 0 1 1 1v3h2V6a3 3 0 0 0-3-3zm5 2a3 3 0 0 0 3 3 1 1 0 0 1-1 1h-1v2h1a3 3 0 0 0 3-3V3h-2v3a1 1 0 0 1-1-1V3h-2zM3 7a1 1 0 0 1 0-2z")))
    val Bool = grafik(höhe = 16f, pfade = arrayOf(pfad(0x6F91F0, "M0 4v8h2a3 3 0 0 0 2.5-1.346 3 3 0 0 0 4.5.578 3 3 0 0 0 4.5-.578A3 3 0 0 0 16 12v-2a1 1 0 0 1-1-1V4h-2v2.77a3 3 0 0 0-4 0 3 3 0 0 0-4.5.578A3 3 0 0 0 2 6V4zm2 4a1 1 0 0 1 0 2zm5 0a1 1 0 0 1 0 2 1 1 0 0 1 0-2zm4 0a1 1 0 0 1 0 2 1 1 0 0 1 0-2z")))
    val Int = grafik(pfade = arrayOf(pfad(0x5ABBEF, "m1 2v2h2v-2zm11 0v5a3 3 0 0 0 3 3h1v-2h-1a1 1 0 0 1-1-1v-1h2v-2h-2v-2zm-8 2v6h2v-4h1a1 1 0 0 1 1 1v3h2v-3a3 3 0 0 0-3-3h-1zm-3 2v4h2v-4z")))
    val Float = grafik(höhe = 16f, pfade = arrayOf(pfad(0x35D4F4, "M3 4a3 3 0 0 0-3 3v5h2v-2h2V8H2V7a1 1 0 0 1 1-1h1V4zm3 0v5a3 3 0 0 0 3 3h1v-2H9a1 1 0 0 1-1-1V4zm6 0v5a3 3 0 0 0 3 3h1v-2h-1a1 1 0 0 1-1-1V8h2V6h-2V4z")))
    val String = grafik(pfade = arrayOf(pfad(0x4593EC, "M7 2H4a2.5 2.5 0 0 0 0 5 .5.5 0 0 1 0 1H1v2h3a2.5 2.5 0 0 0 0-5 .5.5 0 0 1 0-1h3zm1 0v5a3 3 0 0 0 3 3V8a1 1 0 0 1-1-1V6h1V4h-1V2zm4 8h2V7a1 1 0 0 1 1-1V4a3 3 0 0 0-3 3z")))

    val Vector2 = grafik(
        pfad(0xAC73F1, "M12 2v2h1a1 1 0 0 1 0 2 2 2 0 0 0-2 2v2h5V8h-3a3 3 0 0 0 0-6zM1 4v6h2a3 3 0 0 0 3-3V4H4v3a1 1 0 0 1-1 1V4zm9 6V8H9a1 1 0 0 1 0-2h1V4H9a3 3 0 0 0 0 6z"),
        pfad(0xFFFFFF, "M12 2v2h1a1 1 0 0 1 0 2 2 2 0 0 0-2 2v2h5V8h-3a3 3 0 0 0 0-6z", .4f),
    )
    val Vector2i = grafik(
        pfad(0xAC73F1, "M8 2v2h1a1 1 0 0 1 0 2 2 2 0 0 0-2 2v2h5V8H9a3 3 0 0 0 0-6zM1 4v6h2a3 3 0 0 0 3-3V4H4v3a1 1 0 0 1-1 1V4z"),
        pfad(0xFFFFFF, "M8 2v2h1a1 1 0 0 1 0 2 2 2 0 0 0-2 2v2h5V8H9a3 3 0 0 0 0-6z", .4f),
        pfad(0x5ABBEF, "M13 2v2h2V2zm0 4v4h2V6z"),
    )
    val Rect2 = grafik(pfade = arrayOf(pfad(0xF1738F, "M13 2v2h-1a3 3 0 0 0-2.5 1.346A3 3 0 1 0 7 10h1V8H7a1 1 0 0 1-1-1h3a3 3 0 0 0 3 3h1V8h-1a1 1 0 0 1 0-2h1v1a3 3 0 0 0 3 3V8a1 1 0 0 1-1-1V6h1V4h-1V2zM3 4a3 3 0 0 0-3 3v3h2V7a1 1 0 0 1 1-1h1V4z")))
    val Rect2i = grafik(
        pfad(0xF1738F, "M9 2v2H8a3 3 0 0 0 0 6h1V8H8a1 1 0 0 1 0-2h1v1a3 3 0 0 0 3 3V8a1 1 0 0 1-1-1V6h1V4h-1V2zM4 4a3 3 0 0 0-3 3v3h2V7a1 1 0 0 1 1-1h1V4z"),
        pfad(0x5ABBEF, "M13 2v2h2V2zm0 4v4h2V6z"),
    )
    val Vector3 = grafik(
        pfad(0xDE66F0, "M11 2v2h2a1 1 0 0 1-1 1v2a1 1 0 0 1 0 2h-1v2h1a3 3 0 0 0 2.232-5A3 3 0 0 0 15 4V2zM1 4v6h2a3 3 0 0 0 3-3V4H4v3a1 1 0 0 1-1 1V4zm9 6V8H9a1 1 0 0 1 0-2h1V4H9a3 3 0 0 0 0 6z"),
        pfad(0xFFFFFF, "M11 2v2h2a1 1 0 0 1-1 1v2a1 1 0 0 1 0 2h-1v2h1a3 3 0 0 0 2.232-5A3 3 0 0 0 15 4V2z", .4f),
    )
    val Vector3i = grafik(
        pfad(0xDE66F0, "M8 2v2h2a1 1 0 0 1-1 1v2a1 1 0 0 1 0 2H8v2h1a3 3 0 0 0 2.232-5A3 3 0 0 0 12 4V2zM1 4v6h2a3 3 0 0 0 3-3V4H4v3a1 1 0 0 1-1 1V4z"),
        pfad(0xFFFFFF, "M8 2v2h2a1 1 0 0 1-1 1v2a1 1 0 0 1 0 2H8v2h1a3 3 0 0 0 2.232-5A3 3 0 0 0 12 4V2z", .4f),
        pfad(0x5ABBEF, "M13 2v2h2V2zm0 4v4h2V6z"),
    )
    val Transform2D = grafik(
        pfad(0xB9EC41, "M0 2v2h2v6h2V4h3a1 1 0 0 1 0 2 2 2 0 0 0-2 2v2h7a4 4 0 0 0 0-8h-2v6H7a3 3 0 0 0 0-6zm12 2a2 2 0 0 1 0 4z"),
        pfad(0xFFFFFF, "M6.5 2v2H7a1 1 0 0 1 0 2 2 2 0 0 0-2 2v2h5V8H7a3 3 0 0 0 0-6h-.5z", .4f),
    )
    val Vector4 = grafik(
        pfad(0xF066BD, "M11 3v5h3v3h2V2h-2v4h-1V3zM1 4v6h2a3 3 0 0 0 3-3V4H4v3a1 1 0 0 1-1 1V4zm9 6V8H9a1 1 0 0 1 0-2h1V4H9a3 3 0 0 0 0 6z"),
        pfad(0xFFFFFF, "M11 3v5h3v3h2V2h-2v4h-1V3z", .4f),
    )
    val Vector4i = grafik(
        pfad(0xF066BD, "M7 3v5h3v3h2V2h-2v4H9V3zM1 4v6h2a3 3 0 0 0 3-3V4H4v3a1 1 0 0 1-1 1V4z"),
        pfad(0xFFFFFF, "M7 3v5h3v3h2V2h-2v4H9V3z", .4f),
        pfad(0x5ABBEF, "M13 2v2h2V2zm0 4v4h2V6z"),
    )
    val Plane = grafik(pfade = arrayOf(pfad(0xF74949, "M1 2v8h2V8a3 3 0 0 0 0-6zm6 0v5a3 3 0 0 0 3 3V8a1 1 0 0 1-1-1V2zM3 4a1 1 0 0 1 0 2zm8 0v6h2V6a1 1 0 0 1 1 1v3h2V7a3 3 0 0 0-3-3z")))
    val Quaternion = grafik(
        pfad(0xEC418E, "M3 3a3 3 0 0 0 0 6v2h2V8.236A3 3 0 0 0 7 9h2v-.764A3 3 0 0 0 11 9h2V6a3 3 0 0 0 3 3V7a1 1 0 0 1-1-1V5h1V3h-1V1h-2v2h-2a3 3 0 0 0-2 .764V3H7v4a1 1 0 0 1-1-1V3zm0 4a1 1 0 0 1 0-2zm8 0a1 1 0 0 1 0-2z"),
        pfad(0xFFFFFF, "M4 3v3a3 3 0 0 0 3 3h2V3H7v4a1 1 0 0 1-1-1V3z", .4f),
    )
    val Aabb = grafik(
        pfad(0xEE5677, "M5 1a3 3 0 0 0-2.223 5.012A3 3 0 0 0 3 12h2V7h2V1H5zm6 0v5.174A3 3 0 0 0 10 6V4H8v8h2a3 3 0 0 0 3-3 3 3 0 0 0 0-6V1h-2zM5 3v2a1 1 0 0 1 0-2zm8 2a1 1 0 0 1 0 2zM3 8v2a1 1 0 0 1 0-2zm7 0a1 1 0 0 1 0 2z"),
        pfad(0xFFFFFF, "M8 4v8h2a3 3 0 0 0 0-6V4zM3 6a3 3 0 0 0 0 6h2V6zm0 2v2a1 1 0 0 1 0-2zm7 0a1 1 0 0 1 0 2z", .235f),
    )
    val Basis = grafik(
        pfad(0xE1EC41, "M0 2v8h2a3 3 0 0 0 0-6V2zm10 0v2h2V2zM7 4a2 2 0 0 0 0 4H5v2h2a2 2 0 0 0 0-4h2V4zm7 0a2 2 0 0 0 0 4h-2V6h-2v4h4a2 2 0 0 0 0-4h2V4zM2 6a1 1 0 0 1 1 1 1 1 0 0 1-1 1z"),
        pfad(0xFFFFFF, "M10 2v2h2V2zm0 4v4h2V6z", .4f),
    )
    val Transform3D = grafik(
        pfad(0xF68F45, "M4 4h3.349a1 1.267 0 0 1-1 1.267v1a.866.866 0 0 1 0 1.732h-1v2h1a2.81 2.81 0 0 0 2.232-4.734A1.32 1.32 0 0 0 9.345 4V2H0v2h2v6h2zm6 6h2a4 4 0 0 0 0-8h-2zm2-6a2 2 0 0 1 0 4z"),
        pfad(0xFFFFFF, "M5.5 4h1.849a1 1.267 0 0 1-1 1.267v1a.866.866 0 0 1 0 1.732h-1v2h1a2.81 2.81 0 0 0 2.232-4.734A1.32 1.32 0 0 0 9.345 4V2H5.5z", .4f),
    )
    val Projection = grafik(
        pfad(0x44BD44, "M0 2v8h2V8a3 3 0 0 0 2-.779V10h2V7a1 1 0 0 1 1-1h1V4H7a3 3 0 0 0-2.02.795A3 3 0 0 0 2 2zm8 4v2a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-1a2 2 0 0 0-2 2zm6-4v2h2V2zM2 4a1 1 0 0 1 0 2zm8 2h1v2h-1zm4 0v3a1 1 0 0 1-1 1h-1v2h1a3 3 0 0 0 3-3V6z"),
        pfad(0xFFFFFF, "M7 4a3 3 0 0 0-3 3v3h2V7a1 1 0 0 1 1-1h1V4z", .4f),
    )

    val Color = grafik(
        pfad(0xFF5F5F, "M4 4a3 3 0 0 0 0 6h1V8H4a1 1 0 0 1 0-2h1V4z"),
        pfad(0x5FFF97, "M6 2v5a3 3 0 0 0 3 3h1V8H9a1 1 0 0 1-1-1V2z"),
        pfad(0x5FB2FF, "M14 4a3 3 0 0 0-3 3v3h2V7a1 1 0 0 1 1-1h1V4z"),
    )
    val StringName = grafik(
        pfad(0x4593EC, "M3 2a2.5 2.5 0 0 0 0 5 .5.5 0 0 1 0 1H0v2h3a2.5 2.5 0 0 0 0-5 .5.5 0 0 1 0-1h3v3a3 3 0 0 0 3 3h3V6a2 2 0 0 1 2 2v2h2V8a4 4 0 0 0-4-4H8V2zm7 4v2H9a1 1 0 0 1-1-1V6z"),
        pfad(0xFFFFFF, "M6 2v5a3 3 0 0 0 3 3h1V8H9a1 1 0 0 1-1-1V6h2V4H8V2z", .4f),
    )
    val NodePath = grafik(pfade = arrayOf(pfad(0x417AEC, "M0 2v8h2V8a3 3 0 0 0 0-6zm6 0v5a3 3 0 0 0 3 3h1V8H9a1 1 0 0 1-1-1V6h2V4H8V2zm5 0v8h2V6a1 1 0 0 1 1 1v3h2V7a3 3 0 0 0-3-3V2zM2 4a1 1 0 0 1 0 2z")))
    val Rid = grafik(pfade = arrayOf(pfad(0x41EC80, "M7 2v2h2V2zm7 0v2h-1a3 3 0 0 0 0 6h3V2zM4 4a3 3 0 0 0-3 3v3h2V7a1 1 0 0 1 1-1h1V4zm3 2v4h2V6zm6 0h1v2h-1a1 1 0 0 1 0-2z")))
    val Object = grafik(höhe = 16f, pfade = arrayOf(pfad(0xE0E0E0, "M1.553 4.104A1 1 0 0 0 1 5v6a1 1 0 0 0 .553.895l6 3a1 1 0 0 0 .894 0l6-3A1 1 0 0 0 15 11V5a1 1 0 0 0-.553-.894l-6-3a1 1 0 0 0-.894 0zm6.447-1 3.764 1.882L8 6.868 4.236 4.986zm-5 3.5 4 2v3.766l-4-2z")))
    val Callable = grafik(höhe = 16f, pfade = arrayOf(pfad(0xE0E0E0, "m12 1c-2 2-4 4-7 4h-4v5h4c3 0 5 2 7 4zm1 4v5c2.59-.016 2.59-4.985 0-5zm-11 6v4h2l1-4z")))
    val Signal = grafik(höhe = 16f, pfade = arrayOf(pfad(0xFF5F5F, "m1 3v10h2 4v-2h-4v-6h4v-2h-4zm9 1v3h-5v2h5v3l2.5-2 2.5-2-2.5-2z")))
    val Dictionary = grafik(pfade = arrayOf(pfad(0x54ED9E, "M3 2v2a3 3 0 0 0 0 6h2V2zm3 0v2h2V2zm7 0v5a3 3 0 0 0 3 3V8a1 1 0 0 1-1-1V6h1V4h-1V2zm-2 2a3 3 0 0 0 0 6h1V8h-1a1 1 0 0 1 0-2h1V4zM8 7V6H6v4h2zM3 6v2a1 1 0 0 1 0-2z")))
    val Array = grafik(pfade = arrayOf(pfad(0xE0E0E0, "M4 4a3 3 0 0 0 0 6h2V4zm6 0a3 3 0 0 0-3 3v3h2V7a1 1 0 0 1 1-1h1V4zm4 0a3 3 0 0 0-3 3v3h2V7a1 1 0 0 1 1-1h1V4zM4 6v2a1 1 0 0 1 0-2z")))

    val PackedByteArray = grafik(
        pfad(0xE0E0E0, "M0 0v12h4v-2H2V2h2V0zm12 0v2h2v8h-2v2h4V0z"),
        pfad(0x5FFF97, "M5 3a3 3 0 0 0-3 3v3h2V6a1 1 0 0 1 1-1h1v4h2a3 3 0 0 0 1-.176V9h2a3 3 0 0 0 3-3V3h-2v3a1 1 0 0 1-1 1V3H9v3a1 1 0 0 1-1 1V3z"),
        pfad(0xFFFFFF, "M6 9V3h2v4a1 1 0 0 0 1-1V3h2v4a1 1 0 0 0 1-1V3h2v3a3 3 0 0 1-3 3H9v-.176A3 3 0 0 1 8 9z", .4f),
    )
    val PackedInt32Array = packedIntGrafik()
    val PackedInt64Array = packedIntGrafik()
    val PackedFloat32Array = packedFloatGrafik()
    val PackedFloat64Array = packedFloatGrafik()
    val PackedStringArray = grafik(
        pfad(0xE0E0E0, "M0 0v12h4v-2H2V2h2V0zm12 0v2h2v8h-2v2h4V0z"),
        pfad(0x4593EC, "M5 2a2.5 2.5 0 0 0 0 5 .5.5 0 0 1 0 1H2v2h3a2.5 2.5 0 0 0 0-5 .5.5 0 0 1 0-1h3v3a3 3 0 0 0 3 3h2V7a1 1 0 0 1 1-1V4a3 3 0 0 0-3 3v1a1 1 0 0 1-1-1V6h1V4h-1V2z"),
        pfad(0xFFFFFF, "M8 2v5a3 3 0 0 0 3 3V8a1 1 0 0 1-1-1V6h1V4h-1V2z", .4f),
    )
    val PackedVector2Array = grafik(
        pfad(0xE0E0E0, "M0 0v12h4v-2H2V2h2V0zm12 0v2h2v8h-2v2h4V0z"),
        pfad(0xAC73F1, "M9 2v2h1a1 1 0 0 1 0 2 2 2 0 0 0-2 2v2h5V8h-3a3 3 0 0 0 0-6zM3 3v6h2a3 3 0 0 0 3-3V3H6v3a1 1 0 0 1-1 1V3z"),
        pfad(0xFFFFFF, "M9 2v2h1a1 1 0 0 1 0 2 2 2 0 0 0-2 2v2h5V8h-3a3 3 0 0 0 0-6z", .4f),
    )
    val PackedVector3Array = grafik(
        pfad(0xE0E0E0, "M0 0v12h4v-2H2V2h2V0zm12 0v2h2v8h-2v2h4V0z"),
        pfad(0xDE66F0, "M8 1v2h2a1 1 0 0 1-1 1v2a1 1 0 0 1 0 2H8v2h1a3 3 0 0 0 2.232-5A3 3 0 0 0 12 3V1zm0 2H6v3a1 1 0 0 1-1 1V3H3v6h2a3 3 0 0 0 3-3z"),
        pfad(0xFFFFFF, "M8 1v2h2a1 1 0 0 1-1 1v2a1 1 0 0 1 0 2H8v2h1a3 3 0 0 0 2.232-5A3 3 0 0 0 12 3V1z", .4f),
    )
    val PackedColorArray = grafik(
        pfad(0xE0E0E0, "M0 0v12h4v-2H2V2h2V0zm12 0v2h2v8h-2v2h4V0z"),
        pfad(0xFF4545, "M6 3.5a3 3 0 0 0 0 6h1v-2H6a1 1 0 0 1 0-2h1v-2z"),
        pfad(0x80FF45, "M7 1.5v5a3 3 0 0 0 3 3v-2a1 1 0 0 1-1-1v-5z"),
        pfad(0x45D7FF, "M13 3.5a3 3 0 0 0-3 3v3h2v-3a1 1 0 0 1 1-1z"),
    )
    val PackedVector4Array = grafik(
        pfad(0xE0E0E0, "m0 0v12h4v-2h-2v-8h2v-2zm12 0v2h2v8h-2v2h4v-12z"),
        pfad(0xAC73F1, "M8 3h2v2h1v-3h2v8h-2v-3h-3zM3 3v6h2a3 3 0 003-3v-3h-2v3a1 1 0 01-1 1v-4z"),
        pfad(0xFFFFFF, "M8 3h2v2h1v-3h2v8h-2v-3h-3z", .392f),
    )

    private fun packedIntGrafik() = grafik(
        pfad(0xE0E0E0, "M0 0v12h4v-2H2V2h2V0zm12 0v2h2v8h-2v2h4V0z"),
        pfad(0x5ABBEF, "M3 2v2h2V2zm2 2v2H3v4h4V6a1 1 0 0 1 1 1v3h2V7a3 3 0 0 0-3-3zm5 3a3 3 0 0 0 3 3V8a1 1 0 0 1-1-1V6h1V4h-1V2h-2z"),
        pfad(0xFFFFFF, "M5 4v6h2V6a1 1 0 0 1 1 1v3h2V7a3 3 0 0 0-3-3z", .4f),
    )

    private fun packedFloatGrafik() = grafik(
        pfad(0xE0E0E0, "M0 0v12h4v-2H2V2h2V0zm12 0v2h2v8h-2v2h4V0z"),
        pfad(0x35D4F4, "M6 2a3 3 0 0 0-3 3v5h2V8h1V6H5V5a1 1 0 0 1 1-1zm1 0v5a3 3 0 0 0 3 3V8a1 1 0 0 1-1-1V2zm3 0v5a3 3 0 0 0 3 3V8a1 1 0 0 1-1-1V6h1V4h-1V2z"),
        pfad(0xFFFFFF, "M7 2v5a3 3 0 0 0 3 3V8a1 1 0 0 1-1-1V2z", .4f),
    )
}

@Composable
internal fun GodotIcon(icon: GodotIconGrafik, modifier: Modifier = Modifier) {
    val pfade = remember(icon) {
        icon.pfade.map { definition -> definition to PathParser().parsePathString(definition.daten).toPath() }
    }
    Canvas(modifier = modifier) {
        val faktor = minOf(size.width / icon.breite, size.height / icon.höhe)
        val versatzX = (size.width - icon.breite * faktor) / 2f
        val versatzY = (size.height - icon.höhe * faktor) / 2f
        withTransform({
            translate(versatzX, versatzY)
            scale(faktor, faktor, pivot = Offset.Zero)
        }) {
            for ((definition, path) in pfade) {
                drawPath(path, definition.farbe.copy(alpha = definition.alpha))
            }
        }
    }
}
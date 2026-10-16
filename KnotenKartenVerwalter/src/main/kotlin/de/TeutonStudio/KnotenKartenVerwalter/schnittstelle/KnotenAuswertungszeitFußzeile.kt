package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenMeldung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenMeldungsArt

/**
 * Blender-artige Laufzeitanzeige in einem eigenen unteren Rahmen.
 *
 * Der Editor hängt die Zeile außerhalb des Knotenkörpers an.
 * Auswahl, Größenänderung und Anschlüsse bleiben auf den Körper begrenzt.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun KnotenAuswertungszeitFußzeile(dauerNanos: Long?, ausgewählt: Boolean = false) {
    val dauer = dauerNanos ?: return
    val formatiert = formatiereAuswertungsDauerNanos(dauer)
    val form = RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)
    val rahmenFarbe = MaterialTheme.colorScheme.outlineVariant
    val rahmenBreite = 1.dp
    Surface(
        modifier = Modifier.fillMaxWidth().drawWithContent {
            drawContent()
            // Die Knotenunterkante bildet bereits den oberen Abschluss.
            val breite = rahmenBreite.toPx()
            val inset = breite / 2
            val rechts = (size.width - inset).coerceAtLeast(inset)
            val unten = (size.height - inset).coerceAtLeast(inset)
            val radius = (minOf(4.dp.toPx(), size.width / 2, size.height / 2) - inset).coerceAtLeast(0f)
            val rahmen = Path().apply {
                moveTo(inset, 0f)
                lineTo(inset, unten - radius)
                if (radius > 0f) arcTo(Rect(inset, unten - 2 * radius, inset + 2 * radius, unten), 180f, -90f, false)
                lineTo(rechts - radius, unten)
                if (radius > 0f) arcTo(Rect(rechts - 2 * radius, unten - 2 * radius, rechts, unten), 90f, -90f, false)
                lineTo(rechts, 0f)
            }
            drawPath(rahmen, rahmenFarbe, style = Stroke(breite))
        },
        shape = form,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = "Δzeit  $formatiert",
                modifier = Modifier.semantics {
                    contentDescription = "Letzte Auswertungsdauer $formatiert"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
            )
        }
    }
}

/** Alle Diagnosen vollständig, nach oben wachsend; der Körper bildet die Unterkante. */
@Composable
internal fun KnotenMeldungsKopfzeile(meldungen: List<KnotenMeldung>) {
    val rahmenFarbe = MaterialTheme.colorScheme.outlineVariant
    Surface(
        modifier = Modifier.fillMaxWidth().drawWithContent {
            drawContent()
            val inset = 0.5.dp.toPx()
            val rechts = (size.width - inset).coerceAtLeast(inset)
            val radius = (minOf(4.dp.toPx(), size.width / 2, size.height / 2) - inset).coerceAtLeast(0f)
            val rahmen = Path().apply {
                moveTo(inset, size.height)
                lineTo(inset, inset + radius)
                if (radius > 0f) arcTo(Rect(inset, inset, inset + 2 * radius, inset + 2 * radius), 180f, 90f, false)
                lineTo(rechts - radius, inset)
                if (radius > 0f) arcTo(Rect(rechts - 2 * radius, inset, rechts, inset + 2 * radius), 270f, 90f, false)
                lineTo(rechts, size.height)
            }
            drawPath(rahmen, rahmenFarbe, style = Stroke(1.dp.toPx()))
        },
        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
    ) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            meldungen.forEach { meldung ->
                Text(
                    text = meldung.text,
                    style = MaterialTheme.typography.labelSmall,
                    color = when (meldung.art) {
                        KnotenMeldungsArt.Fehler -> MaterialTheme.colorScheme.error
                        KnotenMeldungsArt.Warnung -> MaterialTheme.colorScheme.tertiary
                        KnotenMeldungsArt.Hinweis -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.fillMaxWidth().semantics {
                        contentDescription = "${meldung.art}: ${meldung.text}"
                    },
                )
            }
        }
    }
}

fun formatiereAuswertungsDauerNanos(dauerNanos: Long): String {
    val dauer = dauerNanos.coerceAtLeast(0L)
    return when {
        dauer < 1_000L -> "<1 µs"
        dauer < 1_000_000L -> String.format(Locale.ROOT, "%.0f µs", dauer / 1_000.0).replace('.', ',')
        dauer < 1_000_000_000L -> String.format(Locale.ROOT, "%.1f ms", dauer / 1_000_000.0).replace('.', ',')
        dauer >= 1_000_000_000_000_000L -> String.format(Locale.ROOT, "%.2e s", dauer / 1_000_000_000.0).replace('.', ',')
        else -> String.format(Locale.ROOT, "%.2f s", dauer / 1_000_000_000.0).replace('.', ',')
    }
}

/** X-Abstände der Übergänge von den unteren Rundungen zur geraden Unterkante. */
internal fun unterkantenEinzüge(outline: Outline): Pair<Float, Float> = when (outline) {
    is Outline.Rounded -> outline.roundRect.bottomLeftCornerRadius.x to outline.roundRect.bottomRightCornerRadius.x
    else -> 0f to 0f
}

/** Gemeinsame (unskalierte) Rechteckgeometrie der Knoten-Zeitfußzeile. */
internal data class ZeitfußzeilenGeometrie(
    val hauptHöhe: Dp,
    val links: Dp,
    val rechts: Dp,
    val breite: Dp,
)

/** Die Oberkante der außen angefügten Fußzeile liegt auf der Körperunterkante. */
internal fun berechneZeitfußzeilenGeometrie(
    breite: Dp,
    körperHöhe: Dp,
    hauptForm: Shape,
    dichte: Density,
    richtung: LayoutDirection,
): ZeitfußzeilenGeometrie = with(dichte) {
    val hauptHöhe = körperHöhe.coerceAtLeast(0.dp)
    val (links, rechts) = berechneAnhangEinzüge(breite, hauptHöhe, hauptForm, this, richtung, oben = false)
    ZeitfußzeilenGeometrie(hauptHöhe, links, rechts, (breite - links - rechts).coerceAtLeast(0.dp))
}

/** An den Tangenten ansetzen; auch kreisförmige Adapter behalten ein lesbares Feld. */
internal fun berechneAnhangEinzüge(
    breite: Dp, körperHöhe: Dp, form: Shape, dichte: Density, richtung: LayoutDirection, oben: Boolean,
): Pair<Dp, Dp> = with(dichte) {
    val outline = form.createOutline(Size(breite.toPx(), körperHöhe.toPx()), richtung, this)
    val (linksPx, rechtsPx) = if (oben && outline is Outline.Rounded)
        outline.roundRect.topLeftCornerRadius.x to outline.roundRect.topRightCornerRadius.x
    else unterkantenEinzüge(outline)
    val summe = linksPx + rechtsPx
    val faktor = if (summe > 0f) ((breite - minOf(48.dp, breite)).toPx() / summe).coerceIn(0f, 1f) else 1f
    (linksPx * faktor).toDp() to (rechtsPx * faktor).toDp()
}

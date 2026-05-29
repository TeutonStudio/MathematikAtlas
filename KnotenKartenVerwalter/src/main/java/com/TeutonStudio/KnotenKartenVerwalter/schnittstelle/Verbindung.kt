package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten

private fun VerbindungDaten.zuPfad(start: Offset, ende: Offset): DrawScope.() -> Unit = { VerbindungPfad(this@zuPfad, start, ende) }

@Composable
public fun VerbindungDaten.zuComposable(start: Offset, ende: Offset, modifier: Modifier = Modifier) = VerbindungUmgebung(modifier,this.zuPfad(start,ende))

@Composable
public fun List<VerbindungDaten>.zuComposable(start: (VerbindungDaten) -> Offset?, ende: (VerbindungDaten) -> Offset?, modifier: Modifier = Modifier) = VerbindungUmgebung(modifier,this.map {
    val s = start(it)
    val e = ende(it)
    if (s != null && e != null) it.zuPfad(s,e) else null }.filterNotNull())

@Composable
public fun List<Triple<VerbindungDaten, Offset, Offset>>.zuComposable(modifier: Modifier = Modifier) = VerbindungUmgebung(modifier,this.map { it.first.zuPfad(it.second,it.third) })

/*@Composable
private fun Verbindung(
    daten: VerbindungDaten,
    start: Offset,
    ende: Offset,
    modifier: Modifier = Modifier,
) { VerbindungUmgebung(modifier,daten.zuPfad(start,ende))
    val farbe = if (daten.ausgewaehlt) Color(0xFF2563EB) else Color(0xFF475569)

    Canvas(modifier = modifier) {
        val kontrollAbstand = kotlin.math.max(48f, kotlin.math.abs(ende.x - start.x) / 2f)
        val pfad = Path().apply {
            moveTo(start.x, start.y)
            cubicTo(
                start.x + kontrollAbstand,
                start.y,
                ende.x - kontrollAbstand,
                ende.y,
                ende.x,
                ende.y,
            )
        }
        drawPath(
            path = pfad,
            color = farbe,
            style = Stroke(width = 3f, cap = StrokeCap.Round),
        )
    }
}*/

@Composable
private fun VerbindungUmgebung(
    modifier: Modifier = Modifier,
    vararg inhalt: DrawScope.() -> Unit,
) { Canvas(modifier = modifier) { inhalt.forEach { it() } } }

@Composable
private fun VerbindungUmgebung(
    modifier: Modifier = Modifier,
    inhalt: List<DrawScope.() -> Unit>,
) { Canvas(modifier = modifier) { inhalt.forEach { it() } } }

private fun DrawScope.VerbindungPfad(
    daten: VerbindungDaten,
    start: Offset,
    ende: Offset,
) {
    val farbe = if (daten.ausgewaehlt) Color(0xFF2563EB) else Color(0xFF475569)

    val kontrollAbstand = kotlin.math.max(48f, kotlin.math.abs(ende.x - start.x) / 2f)
    val pfad = Path().apply {
        moveTo(start.x, start.y)
        cubicTo(
            start.x + kontrollAbstand,
            start.y,
            ende.x - kontrollAbstand,
            ende.y,
            ende.x,
            ende.y,
        )
    }
    drawPath(
        path = pfad,
        color = farbe,
        style = Stroke(width = 3f, cap = StrokeCap.Round),
    )
}
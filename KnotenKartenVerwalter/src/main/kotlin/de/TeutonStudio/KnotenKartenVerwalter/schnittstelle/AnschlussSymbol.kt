package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.TypSystem.TypAusdruck
import de.TeutonStudio.TypSystem.TypId
import kotlin.math.max

private const val TUPPEL_TYP = "typ.tupel"
private const val UNENDLICHES_TUPEL_TYP = "typ.tupel.unendlich"
private const val LEGACY_TUPEL_ART = "mathematik.tupel"
private const val METHODEN_TYP = "mathematik.methode"
private const val LEGACY_METHODEN_PREFIX = "mathematik.funktion"

internal sealed interface AnschlussSymbolPlan {
    data object Standard : AnschlussSymbolPlan

    data class Tupel(
        val ringe: List<TupelRingPlan>,
    ) : AnschlussSymbolPlan

    data class Methode(
        val argumentFarben: List<String>,
        val ausgabeFarben: List<String>,
    ) : AnschlussSymbolPlan
}

internal data class TupelRingPlan(
    val farben: List<String>,
    val gepunktet: Boolean = false,
)

/**
 * Leitet die Form des Anschlusses ausschließlich aus dem semantischen G0.2-Typ ab.
 * Die alte Anschlussart bleibt nur als Migrations-Fallback erhalten.
 */
internal fun anschlussSymbolPlan(anschluss: AnschlussDaten): AnschlussSymbolPlan {
    val typ = anschluss.vertrag.typ
    val methode = typ.alsMethodenSignatur()
    if (methode != null || anschluss.istLegacyMethode()) {
        val (argument, ausgabe) = methode ?: (generischerTupelTyp() to generischerTupelTyp())
        return AnschlussSymbolPlan.Methode(
            argumentFarben = methodenSeitenFarben(argument),
            ausgabeFarben = methodenSeitenFarben(ausgabe),
        )
    }

    val tupel = typ.alsTupelTyp()
    if (tupel != null) {
        return AnschlussSymbolPlan.Tupel(tupelRingPlan(tupel.first, tupel.second))
    }
    if (anschluss.art.wert == LEGACY_TUPEL_ART) {
        return AnschlussSymbolPlan.Tupel(tupelRingPlan(emptyList(), unendlich = false))
    }
    return AnschlussSymbolPlan.Standard
}

private fun AnschlussDaten.istLegacyMethode(): Boolean =
    art.wert == METHODEN_TYP || art.wert.startsWith(LEGACY_METHODEN_PREFIX)

private fun TypAusdruck.alsMethodenSignatur(): Pair<TypAusdruck, TypAusdruck>? =
    (this as? TypAusdruck.Parameterisiert)
        ?.takeIf { it.konstruktor.wert == METHODEN_TYP && it.argumente.size >= 2 }
        ?.let { it.argumente[0] to it.argumente[1] }

/** Liefert Komponenten und die Information, ob die Folge unendlich fortgesetzt wird. */
private fun TypAusdruck.alsTupelTyp(): Pair<List<TypAusdruck>, Boolean>? =
    (this as? TypAusdruck.Parameterisiert)?.let { typ ->
        when (typ.konstruktor.wert) {
            TUPPEL_TYP -> typ.argumente to false
            UNENDLICHES_TUPEL_TYP -> typ.argumente to true
            else -> null
        }
    }

private fun generischerTupelTyp(): TypAusdruck =
    TypAusdruck.Parameterisiert(TypId(TUPPEL_TYP), emptyList())

/**
 * Endliche Tupel erhalten einen Ring je Komponente. Ein unendliches Tupel zeigt
 * drei repräsentative Komponenten und danach zwei gepunktete Fortsetzungsringe.
 */
internal fun tupelRingPlan(
    komponenten: List<TypAusdruck>,
    unendlich: Boolean,
): List<TupelRingPlan> {
    val sichereKomponenten = komponenten.ifEmpty { listOf(TypAusdruck.Unbekannt) }
    if (!unendlich) {
        return sichereKomponenten.map { typ -> TupelRingPlan(farbIdsFürTyp(typ)) }
    }

    val sichtbare = List(3) { index ->
        TupelRingPlan(farbIdsFürTyp(sichereKomponenten[index % sichereKomponenten.size]))
    }
    val fortsetzung = List(2) { index ->
        TupelRingPlan(
            farben = farbIdsFürTyp(sichereKomponenten[(index + 3) % sichereKomponenten.size]),
            gepunktet = true,
        )
    }
    return sichtbare + fortsetzung
}

private fun methodenSeitenFarben(typ: TypAusdruck): List<String> {
    val tupel = typ.alsTupelTyp()
    if (tupel != null) {
        val komponenten = tupel.first
        return if (komponenten.isEmpty()) listOf(LEGACY_TUPEL_ART)
        else komponenten.flatMap(::farbIdsFürTyp).distinct()
    }
    return farbIdsFürTyp(typ).distinct()
}

/**
 * Übersetzt semantische Typen auf die vorhandene Farbpalette. Untertypen teilen
 * dabei bewusst die Farbe ihrer sichtbaren Oberart.
 */
internal fun farbIdsFürTyp(typ: TypAusdruck): List<String> = when (typ) {
    is TypAusdruck.Vereinigung -> typ.alternativen.flatMap(::farbIdsFürTyp).distinct()
    is TypAusdruck.Atom -> listOf(normalisiereFarbId(typ.id.wert))
    is TypAusdruck.Parameterisiert -> listOf(
        when (typ.konstruktor.wert) {
            TUPPEL_TYP, UNENDLICHES_TUPEL_TYP -> LEGACY_TUPEL_ART
            METHODEN_TYP -> METHODEN_TYP
            else -> normalisiereFarbId(typ.konstruktor.wert)
        },
    )
    TypAusdruck.Beliebig, TypAusdruck.Unbekannt,
    is TypAusdruck.Variable, is TypAusdruck.Literal,
    -> listOf("mathematik.objekt")
}

internal fun normalisiereFarbId(id: String): String = when {
    id.startsWith("mathematik.zahl.") -> "mathematik.zahl"
    id.startsWith("mathematik.vektor.") -> "mathematik.vektor"
    id == TUPPEL_TYP || id == UNENDLICHES_TUPEL_TYP -> LEGACY_TUPEL_ART
    id.startsWith(LEGACY_METHODEN_PREFIX) -> METHODEN_TYP
    else -> id
}

@Composable
internal fun AnschlussSymbol(
    anschluss: AnschlussDaten,
    fallbackFarben: List<Color>,
    größe: Float,
    zoom: Float,
    aktiviert: Boolean,
    farbeFürAnschluss: @Composable (AnschlussDaten) -> Color,
) {
    val plan = anschlussSymbolPlan(anschluss)
    val fallback = fallbackFarben.ifEmpty { listOf(MaterialTheme.colorScheme.primary) }
    val farbeFürId: @Composable (String) -> Color = { id ->
        farbeFürAnschluss(
            anschluss.copy(
                art = AnschlussArtId(normalisiereFarbId(id)),
                zulässigeArten = emptySet(),
            ),
        ).let { if (aktiviert) it else it.copy(alpha = .2f) }
    }

    val standardFarben = fallback.map { if (aktiviert) it else it.copy(alpha = .2f) }
    val ringFarben = mutableListOf<List<Color>>()
    if (plan is AnschlussSymbolPlan.Tupel) {
        for (ring in plan.ringe) {
            val aufgelöst = mutableListOf<Color>()
            for (id in ring.farben) aufgelöst += farbeFürId(id)
            ringFarben += aufgelöst
        }
    }
    val argumentFarben = mutableListOf<Color>()
    val ausgabeFarben = mutableListOf<Color>()
    if (plan is AnschlussSymbolPlan.Methode) {
        for (id in plan.argumentFarben) argumentFarben += farbeFürId(id)
        for (id in plan.ausgabeFarben) ausgabeFarben += farbeFürId(id)
    }
    val rahmen = MaterialTheme.colorScheme.surface
    val pfeil = MaterialTheme.colorScheme.onSurface

    Box(Modifier.size(größe.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().clip(CircleShape)) {
            when (plan) {
                AnschlussSymbolPlan.Standard -> zeichneGestreifteFläche(standardFarben)

                is AnschlussSymbolPlan.Tupel -> {
                    drawCircle(rahmen)
                    val ringe = plan.ringe
                    if (ringe.isNotEmpty()) {
                        val maxRadius = size.minDimension / 2f - max(1f, size.minDimension * .04f)
                        val schritt = maxRadius / ringe.size.coerceAtLeast(1)
                        val strich = (schritt * .72f).coerceAtLeast(.75f)
                        ringe.forEachIndexed { index, ring ->
                            val radius = schritt * (index + 1)
                            val farben = ringFarben.getOrElse(index) { standardFarben }.ifEmpty { standardFarben }
                            val pathEffect = if (ring.gepunktet) {
                                PathEffect.dashPathEffect(floatArrayOf(strich * .75f, strich * 1.05f))
                            } else null
                            if (farben.size == 1) {
                                drawCircle(
                                    color = farben.single(),
                                    radius = radius,
                                    style = Stroke(width = strich, pathEffect = pathEffect, cap = StrokeCap.Round),
                                )
                            } else {
                                val winkel = 360f / farben.size
                                farben.forEachIndexed { farbIndex, farbe ->
                                    drawArc(
                                        color = farbe,
                                        startAngle = -90f + farbIndex * winkel,
                                        sweepAngle = winkel + .5f,
                                        useCenter = false,
                                        topLeft = Offset(center.x - radius, center.y - radius),
                                        size = Size(radius * 2f, radius * 2f),
                                        style = Stroke(width = strich, pathEffect = pathEffect, cap = StrokeCap.Round),
                                    )
                                }
                            }
                        }
                    }
                }

                is AnschlussSymbolPlan.Methode -> {
                    clipRect(right = size.width / 2f) {
                        zeichneGestreifteFläche(argumentFarben.ifEmpty { standardFarben })
                    }
                    clipRect(left = size.width / 2f) {
                        zeichneGestreifteFläche(ausgabeFarben.ifEmpty { standardFarben })
                    }
                    val trennBreite = (size.minDimension * .08f).coerceAtLeast(1f)
                    drawLine(rahmen, Offset(center.x, 0f), Offset(center.x, size.height), trennBreite)

                    val start = Offset(size.width * .28f, center.y)
                    val ende = Offset(size.width * .72f, center.y)
                    val pfeilBreite = (size.minDimension * .09f).coerceAtLeast(1f)
                    val hinterlegung = pfeilBreite * 2.4f
                    drawLine(rahmen, start, ende, hinterlegung, cap = StrokeCap.Round)
                    drawLine(pfeil, start, ende, pfeilBreite, cap = StrokeCap.Round)
                    val spitzeX = size.width * .60f
                    val spitzeY = size.height * .36f
                    val untenY = size.height * .64f
                    drawLine(rahmen, Offset(spitzeX, spitzeY), ende, hinterlegung, cap = StrokeCap.Round)
                    drawLine(rahmen, Offset(spitzeX, untenY), ende, hinterlegung, cap = StrokeCap.Round)
                    drawLine(pfeil, Offset(spitzeX, spitzeY), ende, pfeilBreite, cap = StrokeCap.Round)
                    drawLine(pfeil, Offset(spitzeX, untenY), ende, pfeilBreite, cap = StrokeCap.Round)
                }
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = rahmen,
                radius = size.minDimension / 2f - .5f,
                style = Stroke(width = (2f / zoom).dp.toPx().coerceAtMost(size.minDimension * .22f)),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.zeichneGestreifteFläche(farben: List<Color>) {
    val sichereFarben = farben.ifEmpty { listOf(Color.Gray) }
    if (sichereFarben.size == 1) {
        drawRect(sichereFarben.single())
        return
    }
    val streifenBreite = (size.minDimension / sichereFarben.size.coerceAtLeast(2)).coerceAtLeast(2f)
    rotate(-45f) {
        var x = -size.width * 1.5f
        var index = 0
        while (x < size.width * 2.5f) {
            drawRect(
                color = sichereFarben[index % sichereFarben.size],
                topLeft = Offset(x, -size.height),
                size = Size(streifenBreite, size.height * 3f),
            )
            x += streifenBreite
            index += 1
        }
    }
}

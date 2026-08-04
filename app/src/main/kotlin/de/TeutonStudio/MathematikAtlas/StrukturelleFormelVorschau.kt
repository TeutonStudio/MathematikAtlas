package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.localBoundingBoxOf
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import de.TeutonStudio.MathematikKnoten.LatexText
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.math.hypot

/** Tatsächliche Compose-Geometrie eines semantischen Ausdrucksziels. */
internal data class CursorZielGeometrie(
    val ausdrucksId: String,
    val bereich: Rect,
    val prioritaet: Int,
)

@Composable
internal fun StrukturelleFormelVorschau(
    wurzel: FormelAusdruck,
    cursor: FormelCursor,
    cursorSetzen: (String, CursorPosition) -> Unit,
    navigieren: (FormelCursorRichtung) -> Unit,
    rücklöschen: () -> Unit,
    vorwärtsLöschen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fokus = remember { FocusRequester() }
    var wurzelKoordinaten by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val bereiche = remember(wurzel) { mutableStateMapOf<String, Rect>() }
    val aktuellerCursor by rememberUpdatedState(cursor)
    val aktuelleWurzel by rememberUpdatedState(wurzel)

    fun registriere(id: String, koordinaten: LayoutCoordinates) {
        val root = wurzelKoordinaten ?: return
        val neu = root.localBoundingBoxOf(koordinaten, clipBounds = false)
        if (bereiche[id] != neu) bereiche[id] = neu
    }

    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 82.dp)
            .horizontalScroll(rememberScrollState())
            .focusRequester(fokus)
            .focusable()
            .onPreviewKeyEvent { ereignis ->
                if (ereignis.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (ereignis.key) {
                    Key.DirectionLeft -> navigieren(FormelCursorRichtung.Links)
                    Key.DirectionRight -> navigieren(FormelCursorRichtung.Rechts)
                    Key.DirectionUp -> navigieren(FormelCursorRichtung.Oben)
                    Key.DirectionDown -> navigieren(FormelCursorRichtung.Unten)
                    Key.Backspace -> rücklöschen()
                    Key.Delete -> vorwärtsLöschen()
                    else -> return@onPreviewKeyEvent false
                }
                true
            }
            .onGloballyPositioned { wurzelKoordinaten = it }
            .pointerInput(wurzel) {
                detectTapGestures { position ->
                    fokus.requestFocus()
                    val dichte = 12.dp.toPx()
                    val treffer = bereiche
                        .map { (id, bereich) ->
                            CursorZielGeometrie(
                                ausdrucksId = id,
                                bereich = bereich,
                                prioritaet = aktuelleWurzel.tiefeVon(id),
                            )
                        }
                        .filter { ziel -> ziel.bereich.erweitert(dichte).enthält(position) }
                        .sortedWith(
                            compareByDescending<CursorZielGeometrie> { it.prioritaet }
                                .thenBy { it.bereich.width * it.bereich.height },
                        )
                        .firstOrNull()
                    if (treffer == null) {
                        val root = bereiche[aktuelleWurzel.id]
                        cursorSetzen(
                            aktuelleWurzel.id,
                            if (root == null || position.x < root.center.x) {
                                CursorPosition.VorAusdruck
                            } else {
                                CursorPosition.NachAusdruck
                            },
                        )
                        return@detectTapGestures
                    }
                    val ausdruck = aktuelleWurzel.findeAnzeigeAusdruck(treffer.ausdrucksId)
                    val positionImAusdruck = when (ausdruck) {
                        is FormelAusdruck.Platzhalter -> CursorPosition.InPlatzhalter(ausdruck.rollenId)
                        else -> if (position.x < treffer.bereich.center.x) {
                            CursorPosition.VorAusdruck
                        } else {
                            CursorPosition.NachAusdruck
                        }
                    }
                    cursorSetzen(treffer.ausdrucksId, positionImAusdruck)
                }
            }
            .semantics { contentDescription = "Strukturierte Formel mit Cursor" },
        contentAlignment = Alignment.CenterStart,
    ) {
        FormelTeil(
            ausdruck = wurzel,
            registriere = ::registriere,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        )
        val cursorBereich = cursorBereich(cursor, wurzel, bereiche)
        if (cursorBereich != null) {
            val cursorFarbe = MaterialTheme.colorScheme.primary
            Canvas(Modifier.matchParentSize()) {
                drawLine(
                    color = cursorFarbe,
                    start = Offset(cursorBereich.left, cursorBereich.top),
                    end = Offset(cursorBereich.left, cursorBereich.bottom),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
internal fun FormelCursorTasten(
    kannBewegen: (FormelCursorRichtung) -> Boolean,
    bewegen: (FormelCursorRichtung) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        CursorTaste("↑", "Cursor nach oben", kannBewegen(FormelCursorRichtung.Oben)) {
            bewegen(FormelCursorRichtung.Oben)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CursorTaste("←", "Cursor nach links", kannBewegen(FormelCursorRichtung.Links)) {
                bewegen(FormelCursorRichtung.Links)
            }
            CursorTaste("↓", "Cursor nach unten", kannBewegen(FormelCursorRichtung.Unten)) {
                bewegen(FormelCursorRichtung.Unten)
            }
            CursorTaste("→", "Cursor nach rechts", kannBewegen(FormelCursorRichtung.Rechts)) {
                bewegen(FormelCursorRichtung.Rechts)
            }
        }
    }
}

@Composable
private fun CursorTaste(
    symbol: String,
    beschreibung: String,
    aktiv: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = aktiv,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = beschreibung },
    ) { Text(symbol, style = MaterialTheme.typography.titleLarge) }
}

@Composable
private fun FormelTeil(
    ausdruck: FormelAusdruck,
    registriere: (String, LayoutCoordinates) -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineSmall,
) {
    Box(
        modifier.onGloballyPositioned { registriere(ausdruck.id, it) },
        contentAlignment = Alignment.Center,
    ) {
        when (ausdruck) {
            is FormelAusdruck.Literal -> LatexText(ausdruck.wert.zuLatex(), style = style)
            is FormelAusdruck.Variable -> LatexText(ausdruck.latex, style = style)
            is FormelAusdruck.Platzhalter -> Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline,
                ),
            ) {
                Text(
                    "□ ${ausdruck.beschriftung}",
                    Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            is FormelAusdruck.Operation -> FormelOperation(
                operation = ausdruck,
                registriere = registriere,
                style = style,
            )
        }
    }
}

@Composable
private fun FormelOperation(
    operation: FormelAusdruck.Operation,
    registriere: (String, LayoutCoordinates) -> Unit,
    style: TextStyle,
) {
    val argumente = operation.argumente.sortedBy { it.position }.map { it.ausdruck }
    when (val name = operation.operatorId.substringAfterLast('.')) {
        "division" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FormelTeil(argumente.getOrNull(0) ?: return@Column, registriere, style = style)
            HorizontalDivider(Modifier.widthIn(min = 44.dp), thickness = 1.5.dp)
            FormelTeil(argumente.getOrNull(1) ?: return@Column, registriere, style = style)
        }
        "potenz" -> Row(verticalAlignment = Alignment.CenterVertically) {
            FormelTeil(argumente.getOrNull(0) ?: return@Row, registriere, style = style)
            FormelTeil(
                argumente.getOrNull(1) ?: return@Row,
                registriere,
                modifier = Modifier.offset(y = (-10).dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        "logarithmus" -> Row(verticalAlignment = Alignment.CenterVertically) {
            LatexText("\\log", style = style)
            FormelTeil(
                argumente.getOrNull(0) ?: return@Row,
                registriere,
                modifier = Modifier.offset(y = 8.dp),
                style = MaterialTheme.typography.labelMedium,
            )
            Text("(", style = style)
            FormelTeil(argumente.getOrNull(1) ?: return@Row, registriere, style = style)
            Text(")", style = style)
        }
        "addition", "subtraktion", "multiplikation" -> {
            val symbol = when (name) {
                "addition" -> "+"
                "subtraktion" -> "−"
                else -> "·"
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                argumente.forEachIndexed { index, argument ->
                    if (index > 0) Text(symbol, style = style)
                    FormelTeil(argument, registriere, style = style)
                }
            }
        }
        "wurzel" -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text("√", style = style)
            FormelTeil(
                argumente.firstOrNull() ?: return@Row,
                registriere,
                modifier = Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface,
                ).padding(horizontal = 4.dp),
                style = style,
            )
        }
        "betrag" -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text("|", style = style)
            FormelTeil(argumente.firstOrNull() ?: return@Row, registriere, style = style)
            Text("|", style = style)
        }
        else -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(formelFunktionsName(name), style = style)
            Text("(", style = style)
            argumente.forEachIndexed { index, argument ->
                if (index > 0) Text(",", style = style)
                FormelTeil(argument, registriere, style = style)
            }
            Text(")", style = style)
        }
    }
}

private fun formelFunktionsName(name: String): String = when (name) {
    "minimum" -> "min"
    "maximum" -> "max"
    else -> name
}

private fun cursorBereich(
    cursor: FormelCursor,
    wurzel: FormelAusdruck,
    bereiche: Map<String, Rect>,
): Rect? {
    val basis = bereiche[cursor.ausdrucksId] ?: return null
    val x = when (val position = cursor.position) {
        CursorPosition.VorAusdruck -> basis.left
        CursorPosition.NachAusdruck -> basis.right
        is CursorPosition.InPlatzhalter -> basis.left + 4f
        is CursorPosition.ZwischenArgumenten -> {
            val operation = wurzel.findeAnzeigeAusdruck(cursor.ausdrucksId) as? FormelAusdruck.Operation
                ?: return basis
            val argumente = operation.argumente.sortedBy { it.position }
            val links = argumente.getOrNull(position.index - 1)?.ausdruck?.id?.let(bereiche::get)
            val rechts = argumente.getOrNull(position.index)?.ausdruck?.id?.let(bereiche::get)
            when {
                links != null && rechts != null -> (links.right + rechts.left) / 2f
                rechts != null -> rechts.left
                links != null -> links.right
                else -> basis.center.x
            }
        }
    }
    return Rect(x, basis.top, x + 1f, basis.bottom)
}

private fun FormelAusdruck.tiefeVon(id: String): Int {
    fun suche(ausdruck: FormelAusdruck, tiefe: Int): Int? {
        if (ausdruck.id == id) return tiefe
        if (ausdruck is FormelAusdruck.Operation) {
            ausdruck.argumente.forEach { argument ->
                suche(argument.ausdruck, tiefe + 1)?.let { return it }
            }
        }
        return null
    }
    return suche(this, 0) ?: -1
}

private fun FormelAusdruck.findeAnzeigeAusdruck(id: String): FormelAusdruck? = when {
    this.id == id -> this
    this is FormelAusdruck.Operation -> argumente.firstNotNullOfOrNull {
        it.ausdruck.findeAnzeigeAusdruck(id)
    }
    else -> null
}

private fun Rect.erweitert(puffer: Float): Rect = Rect(
    left = left - puffer,
    top = top - puffer,
    right = right + puffer,
    bottom = bottom + puffer,
)

private fun Rect.enthält(punkt: Offset): Boolean =
    punkt.x in left..right && punkt.y in top..bottom

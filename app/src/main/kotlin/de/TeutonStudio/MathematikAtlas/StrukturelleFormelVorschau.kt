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
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import de.TeutonStudio.MathematikKnoten.LatexText
import de.TeutonStudio.MathematikRechenSystem.kern.*

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
    val scroll = rememberScrollState()
    val bereiche = remember(wurzel) { mutableStateMapOf<String, Rect>() }
    var wurzelUrsprung by remember { mutableStateOf(Offset.Zero) }
    var viewportBreite by remember { mutableIntStateOf(0) }
    val aktuelleWurzel by rememberUpdatedState(wurzel)
    val cursorAbsolut = cursorBereich(cursor, wurzel, bereiche)
    val cursorLokal = cursorAbsolut?.verschoben(Offset(-wurzelUrsprung.x, -wurzelUrsprung.y))

    LaunchedEffect(cursor, cursorLokal, viewportBreite) {
        val bereich = cursorLokal ?: return@LaunchedEffect
        if (viewportBreite <= 0) return@LaunchedEffect
        val rand = 24f
        val ziel = when {
            bereich.left < rand -> scroll.value + bereich.left - rand
            bereich.right > viewportBreite - rand -> scroll.value + bereich.right - (viewportBreite - rand)
            else -> null
        }
        if (ziel != null) scroll.scrollTo(ziel.toInt().coerceIn(0, scroll.maxValue))
    }

    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 82.dp)
            .onSizeChanged { viewportBreite = it.width }
            .horizontalScroll(scroll)
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
            .onGloballyPositioned { wurzelUrsprung = it.positionInRoot() }
            .pointerInput(wurzel) {
                detectTapGestures { lokal ->
                    fokus.requestFocus()
                    val absolut = lokal + wurzelUrsprung
                    val zwischen = findeArgumentLücke(aktuelleWurzel, absolut, bereiche)
                    if (zwischen != null) {
                        cursorSetzen(zwischen.first, CursorPosition.ZwischenArgumenten(zwischen.second))
                        return@detectTapGestures
                    }
                    val puffer = 12.dp.toPx()
                    val treffer = bereiche
                        .map { (id, bereich) ->
                            CursorZielGeometrie(id, bereich, aktuelleWurzel.tiefeVon(id))
                        }
                        .filter { it.bereich.erweitert(puffer).enthält(absolut) }
                        .sortedWith(
                            compareByDescending<CursorZielGeometrie> { it.prioritaet }
                                .thenBy { it.bereich.width * it.bereich.height },
                        )
                        .firstOrNull()
                    if (treffer == null) {
                        val root = bereiche[aktuelleWurzel.id]
                        cursorSetzen(
                            aktuelleWurzel.id,
                            if (root == null || absolut.x < root.center.x) {
                                CursorPosition.VorAusdruck
                            } else {
                                CursorPosition.NachAusdruck
                            },
                        )
                        return@detectTapGestures
                    }
                    val ausdruck = aktuelleWurzel.findeAnzeigeAusdruck(treffer.ausdrucksId)
                    val position = when (ausdruck) {
                        is FormelAusdruck.Platzhalter -> CursorPosition.InPlatzhalter(ausdruck.rollenId)
                        else -> if (absolut.x < treffer.bereich.center.x) {
                            CursorPosition.VorAusdruck
                        } else {
                            CursorPosition.NachAusdruck
                        }
                    }
                    cursorSetzen(treffer.ausdrucksId, position)
                }
            }
            .semantics { contentDescription = "Strukturierte Formel mit Cursor" },
        contentAlignment = Alignment.CenterStart,
    ) {
        FormelTeil(
            ausdruck = wurzel,
            registriere = { id, koordinaten ->
                val neu = koordinaten.boundsInRoot()
                if (bereiche[id] != neu) bereiche[id] = neu
            },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        )
        if (cursorLokal != null) {
            val cursorFarbe = MaterialTheme.colorScheme.primary
            Canvas(Modifier.matchParentSize()) {
                drawLine(
                    color = cursorFarbe,
                    start = Offset(cursorLokal.left, cursorLokal.top),
                    end = Offset(cursorLokal.left, cursorLokal.bottom),
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
        modifier = Modifier.size(48.dp).semantics { contentDescription = beschreibung },
    ) { Text(symbol, style = MaterialTheme.typography.titleLarge) }
}

@Composable
private fun FormelTeil(
    ausdruck: FormelAusdruck,
    registriere: (String, androidx.compose.ui.layout.LayoutCoordinates) -> Unit,
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
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Text(
                    "□ ${ausdruck.beschriftung}",
                    Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            is FormelAusdruck.Operation -> FormelOperation(ausdruck, registriere, style)
        }
    }
}

@Composable
private fun FormelOperation(
    operation: FormelAusdruck.Operation,
    registriere: (String, androidx.compose.ui.layout.LayoutCoordinates) -> Unit,
    style: TextStyle,
) {
    val argumente = operation.argumente.sortedBy { it.position }.map { it.ausdruck }
    when (val name = operation.operatorId.substringAfterLast('.')) {
        "division" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            argumente.getOrNull(0)?.let { FormelTeil(it, registriere, style = style) }
            HorizontalDivider(Modifier.widthIn(min = 44.dp), thickness = 1.5.dp)
            argumente.getOrNull(1)?.let { FormelTeil(it, registriere, style = style) }
        }
        "potenz" -> Row(verticalAlignment = Alignment.CenterVertically) {
            argumente.getOrNull(0)?.let { FormelTeil(it, registriere, style = style) }
            argumente.getOrNull(1)?.let {
                FormelTeil(
                    it,
                    registriere,
                    modifier = Modifier.offset(y = (-10).dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        "logarithmus" -> Row(verticalAlignment = Alignment.CenterVertically) {
            LatexText("\\log", style = style)
            argumente.getOrNull(0)?.let {
                FormelTeil(
                    it,
                    registriere,
                    modifier = Modifier.offset(y = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Text("(", style = style)
            argumente.getOrNull(1)?.let { FormelTeil(it, registriere, style = style) }
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
            argumente.firstOrNull()?.let {
                FormelTeil(
                    it,
                    registriere,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.onSurface)
                        .padding(horizontal = 4.dp),
                    style = style,
                )
            }
        }
        "betrag" -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text("|", style = style)
            argumente.firstOrNull()?.let { FormelTeil(it, registriere, style = style) }
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

private fun findeArgumentLücke(
    wurzel: FormelAusdruck,
    position: Offset,
    bereiche: Map<String, Rect>,
): Pair<String, Int>? {
    val operationen = buildList<FormelAusdruck.Operation> {
        fun besuche(ausdruck: FormelAusdruck) {
            if (ausdruck is FormelAusdruck.Operation) {
                add(ausdruck)
                ausdruck.argumente.forEach { besuche(it.ausdruck) }
            }
        }
        besuche(wurzel)
    }.sortedByDescending { wurzel.tiefeVon(it.id) }

    operationen.forEach { operation ->
        val argumente = operation.argumente.sortedBy { it.position }
        for (index in 1 until argumente.size) {
            val links = bereiche[argumente[index - 1].ausdruck.id] ?: continue
            val rechts = bereiche[argumente[index].ausdruck.id] ?: continue
            val lücke = Rect(
                left = minOf(links.right, rechts.left) - 8f,
                top = minOf(links.top, rechts.top) - 8f,
                right = maxOf(links.right, rechts.left) + 8f,
                bottom = maxOf(links.bottom, rechts.bottom) + 8f,
            )
            if (lücke.enthält(position)) return operation.id to index
        }
    }
    return null
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

private fun Rect.verschoben(delta: Offset): Rect = Rect(
    left = left + delta.x,
    top = top + delta.y,
    right = right + delta.x,
    bottom = bottom + delta.y,
)

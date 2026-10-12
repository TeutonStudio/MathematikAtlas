package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Fester Dialogvertrag, unabhängig von App-Dichte und mathematischem Inhalt. */
object AtlasDialogAbstände {
    val fensterrand = 16.dp
    val horizontal = 24.dp
    val kopfOben = 24.dp
    val kopfUnten = 16.dp
    val untertitel = 8.dp
    val abschnitt = 24.dp
    val beschreibung = 8.dp
    val felder = 12.dp
    val eintragHorizontal = 16.dp
    val eintragVertikal = 12.dp
    val eintragBeschreibung = 4.dp
    val aktionenVertikal = 16.dp
    val aktionen = 8.dp
    val inhalt = PaddingValues(horizontal = horizontal, vertical = 16.dp)
}

private val LocalDialogRandBereichHöhe = staticCompositionLocalOf { Dp.Infinity }

enum class AtlasDialogGröße { Kompakt, Arbeitsdialog }

/**
 * Gemeinsame Fenstergrenzen und Oberfläche. Der Inhalt verwaltet seine Scrollbereiche selbst.
 * Keine zusätzliche Scrollhülle um Lazy-Listen, Grids oder Zeichenflächen.
 */
@Composable
fun AtlasDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    größe: AtlasDialogGröße = AtlasDialogGröße.Arbeitsdialog,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        BoxWithConstraints(
            Modifier.fillMaxSize().pointerInput(onDismissRequest, properties.dismissOnClickOutside) {
                detectTapGestures { if (properties.dismissOnClickOutside) onDismissRequest() }
            }.windowInsetsPadding(WindowInsets.safeDrawing).imePadding()
                .padding(AtlasDialogAbstände.fensterrand),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            val breite = if (größe == AtlasDialogGröße.Kompakt) 560.dp else 1180.dp
            Surface(
                modifier = modifier.widthIn(max = minOf(maxWidth, breite)).fillMaxWidth()
                    .heightIn(max = maxHeight).pointerInput(Unit) { detectTapGestures { } },
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
            ) {
                CompositionLocalProvider(LocalDialogRandBereichHöhe provides (maxHeight * .4f)) { content() }
            }
        }
    }
}

/** Titelaktionen stehen unter dem Text, damit auch lange Titel ihre volle Breite erhalten. */
@Composable
fun AtlasDialogKopf(
    titel: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    untertitel: (@Composable () -> Unit)? = null,
    aktionen: (@Composable FlowRowScope.() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().heightIn(max = LocalDialogRandBereichHöhe.current)
        .verticalScroll(rememberScrollState()).padding(
        start = AtlasDialogAbstände.horizontal, end = AtlasDialogAbstände.horizontal,
        top = AtlasDialogAbstände.kopfOben, bottom = AtlasDialogAbstände.kopfUnten,
    )) {
        ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
            Box(Modifier.semantics { heading() }) { titel() }
        }
        if (untertitel != null) {
            Spacer(Modifier.height(AtlasDialogAbstände.untertitel))
            ProvideTextStyle(MaterialTheme.typography.bodyMedium) { untertitel() }
        }
        if (aktionen != null) {
            Spacer(Modifier.height(AtlasDialogAbstände.aktionen))
            AtlasDialogAktionsZeile(content = aktionen)
        }
    }
}

@Composable
fun AtlasDialogAbschnitt(
    titel: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AtlasDialogAbstände.beschreibung)) {
        Text(titel, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Column(verticalArrangement = Arrangement.spacedBy(AtlasDialogAbstände.felder), content = content)
    }
}

@Composable
fun AtlasDialogEintrag(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth().heightIn(min = 48.dp).padding(
            horizontal = AtlasDialogAbstände.eintragHorizontal, vertical = AtlasDialogAbstände.eintragVertikal,
        ),
        verticalArrangement = Arrangement.spacedBy(AtlasDialogAbstände.eintragBeschreibung),
        content = content,
    )
}

@Composable
private fun AtlasDialogAktionsZeile(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AtlasDialogAbstände.aktionen, androidx.compose.ui.Alignment.End),
        verticalArrangement = Arrangement.spacedBy(AtlasDialogAbstände.aktionen), content = content,
    )
}

@Composable
fun AtlasDialogAktionen(modifier: Modifier = Modifier, content: @Composable FlowRowScope.() -> Unit) {
    Box(modifier.fillMaxWidth().heightIn(max = LocalDialogRandBereichHöhe.current)
        .verticalScroll(rememberScrollState()).padding(
        horizontal = AtlasDialogAbstände.horizontal, vertical = AtlasDialogAbstände.aktionenVertikal,
    )) { AtlasDialogAktionsZeile(content) }
}

/** Kopf und Aktionen bleiben außerhalb des begrenzten Inhaltsbereichs. */
@Composable
fun AtlasDialog(
    onDismissRequest: () -> Unit,
    titel: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    untertitel: (@Composable () -> Unit)? = null,
    aktionen: (@Composable FlowRowScope.() -> Unit)? = null,
    größe: AtlasDialogGröße = AtlasDialogGröße.Arbeitsdialog,
    inhaltScrolltSelbst: Boolean = false,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable () -> Unit,
) {
    AtlasDialog(onDismissRequest, modifier, größe, properties) {
        Column(if (größe == AtlasDialogGröße.Arbeitsdialog) Modifier.fillMaxSize() else Modifier.fillMaxWidth()) {
            AtlasDialogKopf(titel = titel, untertitel = untertitel)
            val scroll = if (inhaltScrolltSelbst) Modifier else Modifier.verticalScroll(rememberScrollState())
            Box(Modifier.weight(1f, fill = größe == AtlasDialogGröße.Arbeitsdialog)
                .fillMaxWidth().then(scroll).padding(AtlasDialogAbstände.inhalt)) {
                ProvideTextStyle(MaterialTheme.typography.bodyMedium) { content() }
            }
            if (aktionen != null) AtlasDialogAktionen(content = aktionen)
        }
    }
}

/** Kleine Meldungen und Formulare; eigene Listen müssen [inhaltScrolltSelbst] setzen. */
@Composable
fun AtlasBestätigungsDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    untertitel: (@Composable () -> Unit)? = null,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    inhaltScrolltSelbst: Boolean = false,
) {
    AtlasDialog(onDismissRequest, modifier, AtlasDialogGröße.Kompakt, properties) {
        Column(Modifier.fillMaxWidth()) {
            if (title != null) AtlasDialogKopf(titel = title, untertitel = untertitel)
            if (text != null) {
                val scroll = if (inhaltScrolltSelbst) Modifier else Modifier.verticalScroll(rememberScrollState())
                Box(Modifier.weight(1f, fill = false).fillMaxWidth().then(scroll).padding(AtlasDialogAbstände.inhalt)) {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) { text() }
                }
            }
            AtlasDialogAktionen {
                dismissButton?.invoke()
                confirmButton()
            }
        }
    }
}

/** Einheitlicher Listeneintrag mit optionalen Symbolen und umbrechenden Nebenaktionen. */
@Composable
fun AtlasDialogListenEintrag(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    AtlasDialogEintrag(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AtlasDialogAbstände.eintragHorizontal)) {
            leadingContent?.invoke()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AtlasDialogAbstände.eintragBeschreibung)) {
                ProvideTextStyle(MaterialTheme.typography.bodyLarge) { headlineContent() }
                if (supportingContent != null) {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) { supportingContent() }
                }
            }
        }
        if (trailingContent != null) AtlasDialogAktionsZeile { trailingContent() }
    }
}

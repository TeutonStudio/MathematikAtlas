package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import kotlin.math.roundToInt

private class LetzteGruppenDarstellung(var projektion: GruppenProjektion, var faltung: List<Pair<VisuelleGruppenId, Boolean>>, var übergang: GruppenÜbergang? = null)

internal class GruppenÜbergang(
    val vorher: GruppenProjektion?,
    val fortschritt: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    private val startPositionen: Map<AnschlussVerweis,GraphPunkt> = emptyMap(),
    val startRahmen: Map<VisuelleGruppenId,Pair<GraphPunkt,GraphGröße>> = vorher?.let(::gruppenRahmen).orEmpty(),
) {
    val fertig: Boolean get() = fortschritt.value >= 1f
    val vorherigeKnotenIds = vorher?.karte?.knoten.orEmpty().mapTo(mutableSetOf()) { it.id }
    val altePositionen = vorher?.karte?.knoten.orEmpty().flatMap { k -> k.anschlüsse.map { a ->
        AnschlussVerweis(k.id,a.id) to (k.position + k.anschlussPositionRelativ(a))
    } }.toMap()
    fun position(original: AnschlussVerweis): GraphPunkt? = startPositionen[original] ?: vorher?.sichtbar(original)?.let(altePositionen::get)
    fun aktuellePositionen(ziel: GruppenProjektion): Map<AnschlussVerweis,GraphPunkt> = ziel.karte.knoten.flatMap { k ->
        k.anschlüsse.map { a ->
            val ref = ziel.original(AnschlussVerweis(k.id,a.id))
            val zu = k.position + k.anschlussPositionRelativ(a)
            val von = position(ref) ?: zu
            ref to GraphPunkt(von.x+(zu.x-von.x)*fortschritt.value,von.y+(zu.y-von.y)*fortschritt.value)
        }
    }.toMap()
    fun aktuelleRahmen(ziel: GruppenProjektion): Map<VisuelleGruppenId,Pair<GraphPunkt,GraphGröße>> = gruppenRahmen(ziel).mapValues { (id,zu) ->
        val von = startRahmen[id] ?: zu
        val t = fortschritt.value
        GraphPunkt(von.first.x+(zu.first.x-von.first.x)*t,von.first.y+(zu.first.y-von.first.y)*t) to
            GraphGröße(von.second.breite+(zu.second.breite-von.second.breite)*t,von.second.höhe+(zu.second.höhe-von.second.höhe)*t)
    }
}

@Composable
internal fun erinnereGruppenÜbergang(editor: KartenEditorZustand): GruppenÜbergang {
    val projektion = editor.projektion
    val faltung = editor.karte.visuelleGruppen.map { it.id to it.eingeklappt }
    val letzte = remember(editor.karte.id, editor.geöffneteGruppe) { LetzteGruppenDarstellung(projektion, faltung) }
    val übergang = remember(projektion) {
        val geändert = letzte.faltung != faltung
        val alt = if (geändert) letzte.projektion else null
        if (geändert) letzte.faltung = faltung
        val läuft = letzte.übergang?.takeIf { !it.fertig }
        GruppenÜbergang(alt, Animatable(if (alt == null) 1f else 0f),
            läuft?.aktuellePositionen(letzte.projektion).orEmpty(),
            läuft?.aktuelleRahmen(letzte.projektion) ?: alt?.let(::gruppenRahmen).orEmpty())
            .also { letzte.übergang = it }
    }
    SideEffect { letzte.projektion = projektion }
    LaunchedEffect(übergang) {
        if (übergang.vorher != null) {
            editor.beginneGruppenÜbergang(übergang)
            try { übergang.fortschritt.animateTo(1f, tween(200)) }
            finally { editor.beendeGruppenÜbergang(übergang) }
        }
    }
    return übergang
}

/** Eine gemeinsame Ebene für ausblendende Inhalte; nach 200 ms nicht mehr komponiert. */
@Composable
internal fun GruppenÜbergangsInhalt(
    übergang: GruppenÜbergang,
    ziel: KartenDaten,
    bereich: androidx.compose.ui.geometry.Rect?,
    rendererFür: (KnotenDaten) -> KnotenRenderer,
) {
    val fertig by remember(übergang) { derivedStateOf { übergang.fertig } }
    val alt = übergang.vorher ?: return
    if (fertig) return
    val dichte = LocalDensity.current.density
    val ansicht = ziel.ansicht
    val alteKnoten = remember(übergang, ziel.knoten, bereich) {
        val ids = ziel.knoten.mapTo(mutableSetOf()) { it.id }
        val sichtbar = bereich?.let { renderAuswahlFür(alt.karte, it).knoten } ?: alt.karte.knoten
        sichtbar.filter { it.id !in ids }
    }
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - übergang.fortschritt.value }
        .clearAndSetSemantics { }
        .pointerInput(übergang) {
            awaitPointerEventScope { while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() } }
        }) {
        alteKnoten.forEach { k ->
            key(k.id) {
                val renderer = if (k.art in setOf(GRUPPEN_BLOCK_ART,GRUPPEN_EINGANG_ART,GRUPPEN_AUSGANG_ART)) GruppenKnotenRenderer else rendererFür(k)
                Card(Modifier.offset {
                    IntOffset((ansicht.verschiebung.x + k.position.x * dichte * ansicht.zoom).roundToInt(),
                        (ansicht.verschiebung.y + k.position.y * dichte * ansicht.zoom).roundToInt())
                }.graphicsLayer { scaleX = ansicht.zoom; scaleY = ansicht.zoom; transformOrigin = TransformOrigin(0f,0f) }
                    .size(k.größe.breite.dp,k.größe.höhe.dp)) {
                    renderer.Inhalt(k,false,object : KnotenRendererAktionen {
                        override fun eigenschaftenErsetzen(eigenschaften: Map<String,KnotenEigenschaft>) {}
                    })
                }
            }
        }
    }
}

@Composable
internal fun GruppenÜbergangsRahmen(übergang: GruppenÜbergang, ziel: GruppenProjektion, ansicht: AnsichtsFenster) {
    val fertig by remember(übergang) { derivedStateOf { übergang.fertig } }
    val alt = übergang.vorher ?: return
    if (fertig) return
    val paare = remember(alt,ziel) {
        val von = übergang.startRahmen;val zu = gruppenRahmen(ziel)
        von.mapNotNull { (id,rect) -> zu[id]?.takeIf { it != rect }?.let { rect to it } }
    }
    val farbe = MaterialTheme.colorScheme.tertiary
    Canvas(Modifier.fillMaxSize()) {
        val t = übergang.fortschritt.value
        val faktor = density * ansicht.zoom
        paare.forEach { (von,zu) ->
            val x = von.first.x + (zu.first.x-von.first.x)*t
            val y = von.first.y + (zu.first.y-von.first.y)*t
            val b = von.second.breite + (zu.second.breite-von.second.breite)*t
            val h = von.second.höhe + (zu.second.höhe-von.second.höhe)*t
            drawRoundRect(farbe.copy(alpha=.65f*(1f-t)), Offset(ansicht.verschiebung.x+x*faktor,ansicht.verschiebung.y+y*faktor),
                Size(b*faktor,h*faktor),CornerRadius(14.dp.toPx()),style=Stroke(2.dp.toPx()))
        }
    }
}

private fun gruppenRahmen(p: GruppenProjektion) = buildMap<VisuelleGruppenId,Pair<GraphPunkt,GraphGröße>> {
        p.karte.visuelleGruppen.forEach { put(it.id,it.position to it.größe) }
        val knoten = p.karte.knoten.associateBy { it.id }
        p.blöcke.forEach { (id,gid) -> knoten[id]?.let { put(gid,it.position to it.größe) } }
    }

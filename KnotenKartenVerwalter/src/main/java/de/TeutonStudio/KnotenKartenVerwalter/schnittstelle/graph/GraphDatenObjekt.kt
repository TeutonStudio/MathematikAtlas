package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung
import java.util.Dictionary

interface GraphDatenObjekt<D: GraphDaten>: GraphObjekt {
    public val daten: D

    public val layoutCoordinates: MutableState<LayoutCoordinates?>
    public val objektModifier @Composable get() = Modifier.modiInputEvent()

    public open fun beiKlick(klickPos: Offset)
    public open fun beiHalten(klickPos: Offset)
    public open fun beiTransform(centroid: Offset, zoomDelta: Float, panDelta: Offset, rotationChange: Float)

    @Composable public open fun Modifier.modiInputEvent() = vorher().position().tapping().transform()
    @Composable public open fun Modifier.vorher() = zIndex(1f)

    @Composable public fun Modifier.transform() = transformable(rememberTransformableState(::beiTransform))
    @Composable public fun Modifier.tapping() = pointerInput(daten.id) { detectTapGestures(onTap = ::beiKlick,onLongPress = ::beiHalten) }
    @Composable public fun Modifier.position() = onGloballyPositioned { layoutCoordinates.value = it }


    @Composable public open fun BoxScope.Darstellung()
    @Composable public fun ComposableDarstellung() = Box(objektModifier) { Darstellung() }

    public val istSelektiert get() = derivedStateOf { graph.karte.auswahl.enthält(this) }

    public fun erhalteAnschluss(knotenId: String,anschlussId: String) = graph.karte.knoten.find { it.daten.id == knotenId }!!.anschlüsse.find { it.daten.id == anschlussId }
    public fun erhalteAnschlussMann(id: GraphDatenVerbindung.IDEhe) = erhalteAnschluss(id.knotenIdMann,id.anschlussIdMann)
    public fun erhalteAnschlussWeib(id: GraphDatenVerbindung.IDEhe) = erhalteAnschluss(id.knotenIdWeib,id.anschlussIdWeib)

    public fun kontextPosition(klickPos: Offset): IntOffset {
        val objektKoordinaten = layoutCoordinates.value ?: return klickPos.round()
        val kartenKoordinaten = graph.karte.layoutCoordinates.value ?: return klickPos.round()
        return kartenKoordinaten.localPositionOf(objektKoordinaten, klickPos).round()
    }

//    public fun erstelleVerbindung(von: GraphDatenObjektAnschluss<*>, zu: GraphDatenObjektAnschluss<*>) = Unit

    public companion object {
        @Composable public fun Iterable<GraphDatenObjekt<*>>.Composable(/*modifier: Modifier = Modifier*/) = forEach { it.ComposableDarstellung() }

    }

    public interface Vergrößerbar<D> where D : GraphDaten.bewegbareGD, D : GraphDaten.orthogoneGD {

        public val daten: D
        public val vergrößerbarZoom: Float
        public val vergrößerbarLayoutCoordinates: MutableState<LayoutCoordinates?>?
            get() = null
        public val vergrößerbarFarbe: Color get() = Color(0xFF2563EB)
        public val minimaleBreite: Float get() = 48f
        public val minimaleTiefe: Float get() = 32f
        public val horizontalVergrößerbar: Boolean get() = true
        public val vertikalVergrößerbar: Boolean get() = true

        public fun vergrößereBereich(bereich: VergrößerBereich, klickPosImKnoten: Offset) {
            vergrößereBereich(
                bereich = bereich,
                startPosition = daten.position,
                startBreite = daten.breite,
                startTiefe = daten.tiefe,
                klickPosImKnoten = klickPosImKnoten,
            )
        }

        public fun vergrößereBereich(
            bereich: VergrößerBereich,
            startPosition: Offset,
            startBreite: Float,
            startTiefe: Float,
            klickPosImKnoten: Offset,
        ) {
            val pos = startPosition + klickPosImKnoten
            val kanten = startPosition + Offset(startBreite,startTiefe)

            var neueLinks = startPosition.x
            var neueOben = startPosition.y
            var neueRechts = kanten.x
            var neueUnten = kanten.y

            if (horizontalVergrößerbar) { when {
                bereich.links -> neueLinks = pos.x.coerceAtMost(kanten.x - minimaleBreite)
                bereich.rechts -> neueRechts = pos.x.coerceAtLeast(startPosition.x + minimaleBreite)
            } }

            if (vertikalVergrößerbar) { when {
                bereich.oben -> neueOben = pos.y.coerceAtMost(kanten.y - minimaleTiefe)
                bereich.unten -> neueUnten = pos.y.coerceAtLeast(startPosition.y + minimaleTiefe)
            } }

            daten.position = Offset(neueLinks, neueOben)
            daten.breite = neueRechts - neueLinks
            daten.tiefe = neueUnten - neueOben
        }

        /**
         * Überträgt die in Weltkoordinaten gespeicherte Größe
         * auf das Compose-Layout.
         */
        @Composable public fun Modifier.vergrößerbareGröße(): Modifier = daten.dimModi(this,LocalDensity.current,Offset(minimaleBreite,minimaleTiefe))

        /**
         * Zeichnet die sichtbaren Griffe zur Größenänderung.
         */
        @Composable public fun BoxScope.VergrößerBereiche() {
            VergrößerBereich.entries.filter(::istBereichSichtbar).forEach { bereich ->
                var griffKoordinaten by remember(bereich) { mutableStateOf<LayoutCoordinates?>(null) }
                val istEcke = bereich.istEcke
                val länge = if (istEcke) 14.dp else 26.dp
                val dicke = if (istEcke) 14.dp else 8.dp

                val größenModifier = if (bereich.istVertikal) { Modifier.width(dicke).height(länge) } else { Modifier.width(länge).height(dicke) }

                Box(modifier = Modifier.align(bereich.ausrichtung).offset { bereich.offset.round() }
                    .then(größenModifier)
                    .onGloballyPositioned { griffKoordinaten = it }
                    .background(color = vergrößerbarFarbe, shape = CircleShape)
                    .zIndex(4f).pointerInput(bereich) {
                        awaitEachGesture {
                            val start = awaitFirstDown(requireUnconsumed = false)
                            start.consume()

                            val startPosition = daten.position
                            val startBreite = daten.breite
                            val startTiefe = daten.tiefe
                            val zoom = vergrößerbarZoom.coerceAtLeast(0.0001f)
                            val knotenUrsprungImRoot = vergrößerbarLayoutCoordinates?.value?.localToRoot(Offset.Zero)

                            val griffUrsprungImKnoten = bereich.griffUrsprung(
                                knotenBreite = daten.breite,
                                knotenTiefe = daten.tiefe,
                                griffBreite = size.width.toFloat(),
                                griffHöhe = size.height.toFloat(),
                            )
                            fun klickPositionImStartKnoten(klickPosImGriff: Offset): Offset {
                                val klickPosImRoot = griffKoordinaten?.localToRoot(klickPosImGriff)
                                return if (knotenUrsprungImRoot != null && klickPosImRoot != null) {
                                    (klickPosImRoot - knotenUrsprungImRoot) / zoom
                                } else { griffUrsprungImKnoten + klickPosImGriff }
                            }

                            vergrößereBereich(
                                bereich = bereich,
                                startPosition = startPosition,
                                startBreite = startBreite,
                                startTiefe = startTiefe,
                                klickPosImKnoten = klickPositionImStartKnoten(start.position),
                            )

                            drag(start.id) { change ->
                                change.consume()
                                vergrößereBereich(
                                    bereich = bereich,
                                    startPosition = startPosition,
                                    startBreite = startBreite,
                                    startTiefe = startTiefe,
                                    klickPosImKnoten = klickPositionImStartKnoten(change.position),
                                )
                            }
                        }
                    },
                )
            }
        }

        /**
         * Ecken werden nur benötigt, wenn beide Achsen veränderbar sind.
         * Für einzelne Achsen genügen die jeweiligen Seitengriffe.
         */
        public fun istBereichSichtbar(bereich: VergrößerBereich): Boolean = when {
            bereich.istEcke -> horizontalVergrößerbar && vertikalVergrößerbar
            bereich.links || bereich.rechts -> horizontalVergrößerbar
            bereich.oben || bereich.unten -> vertikalVergrößerbar

            else -> false
        }

        public enum class VergrößerBereich(
            public val links: Boolean,
            public val rechts: Boolean,
            public val oben: Boolean,
            public val unten: Boolean,
            internal val ausrichtung: Alignment,
            internal val offset: Offset,
        ) {
            Links(
                links = true,
                rechts = false,
                oben = false,
                unten = false,
                ausrichtung = Alignment.CenterStart,
                offset = Offset(-6f, 0f),
            ),

            Rechts(
                links = false,
                rechts = true,
                oben = false,
                unten = false,
                ausrichtung = Alignment.CenterEnd,
                offset = Offset(6f, 0f),
            ),

            Oben(
                links = false,
                rechts = false,
                oben = true,
                unten = false,
                ausrichtung = Alignment.TopCenter,
                offset = Offset(0f, -6f),
            ),

            Unten(
                links = false,
                rechts = false,
                oben = false,
                unten = true,
                ausrichtung = Alignment.BottomCenter,
                offset = Offset(0f, 6f),
            ),

            LinksOben(
                links = true,
                rechts = false,
                oben = true,
                unten = false,
                ausrichtung = Alignment.TopStart,
                offset = Offset(-6f, -6f),
            ),

            RechtsOben(
                links = false,
                rechts = true,
                oben = true,
                unten = false,
                ausrichtung = Alignment.TopEnd,
                offset = Offset(6f, -6f),
            ),

            LinksUnten(
                links = true,
                rechts = false,
                oben = false,
                unten = true,
                ausrichtung = Alignment.BottomStart,
                offset = Offset(-6f, 6f),
            ),

            RechtsUnten(
                links = false,
                rechts = true,
                oben = false,
                unten = true,
                ausrichtung = Alignment.BottomEnd,
                offset = Offset(6f, 6f),
            );

            public val istEcke: Boolean get() = (links || rechts) && (oben || unten)

            /**
             * Gibt an, ob der Griff als vertikaler Balken dargestellt wird.
             * Bei Eckgriffen sind Länge und Dicke identisch.
             */
            internal val istVertikal: Boolean get() = links || rechts

            internal fun griffUrsprung(
                knotenBreite: Float,
                knotenTiefe: Float,
                griffBreite: Float,
                griffHöhe: Float,
            ): Offset {
                val x = when {
                    links -> -griffBreite / 2f
                    rechts -> knotenBreite - griffBreite / 2f
                    else -> (knotenBreite - griffBreite) / 2f
                }
                val y = when {
                    oben -> -griffHöhe / 2f
                    unten -> knotenTiefe - griffHöhe / 2f
                    else -> (knotenTiefe - griffHöhe) / 2f
                }
                return Offset(x, y)
            }
        }
    }

    public interface Inspektor<D: GraphDaten>: GraphDatenObjekt<D>/*, GraphDatenObjektInspektorBasis<D>*/ {
//        override val istSelektiert get() = derivedStateOf { graph.karte.auswahl.enthält(this) }
        public val inpsektorData: List<@Composable () -> Unit>

        @Composable public fun ComposableInspektor() = Box(Modifier.zIndex(20f)) { Inspektor() }
        @Composable public open fun BoxScope.Inspektor() {
            Card(Modifier.padding(15.dp)) {
                inpsektorData.forEach { it() }
            }
        }
    }

    public interface Kontext<D: GraphDaten>: GraphDatenObjekt<D> {
        public val öffneKontext get() = derivedStateOf { graph.karte.ctx.objektDatenId == daten.id }
        public val kontextData: List<@Composable () -> Unit>

        @Composable public fun ComposableKontext() = Box(Modifier.zIndex(30f)) { KontextFenster() }
        @Composable public open fun BoxScope.KontextFenster(pos: IntOffset = graph.karte.ctx.pos) {
            Card(Modifier.offset { pos }.padding(15.dp)) {
                kontextData.forEach { it() }
            }
        }
    }
}

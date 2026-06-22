package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Kante

//typealias GraphObjekt = GraphDatenObjekt<out GraphDaten>

interface GraphDatenObjekt<D: GraphDaten>: GraphObjekt {
    public val daten: D
//    public val graph: Graph
//    public fun registriere() = also { graph.inhalt.add(it) }

    public val layoutCoordinates: MutableState<LayoutCoordinates?>
    public val objektModifier @Composable get() = Modifier.modiInputEvent()

    public open fun beiKlick(klickPos: Offset)
    public open fun beiHalten(klickPos: Offset)
    public open fun beiTransform(centroid: Offset, zoomDelta: Float, panDelta: Offset, rotationChange: Float)

    @Composable public open fun Modifier.modiInputEvent() = vorher().position().transform().tapping()
    @Composable public open fun Modifier.vorher() = zIndex(1f)

    @Composable public fun Modifier.transform() = transformable(rememberTransformableState(::beiTransform))
    @Composable public fun Modifier.tapping() = pointerInput(daten.id) { detectTapGestures(onTap = ::beiKlick,onLongPress = ::beiHalten) }
    @Composable public fun Modifier.position() = onGloballyPositioned { layoutCoordinates.value = it }


    @Composable public open fun BoxScope.Darstellung()
    @Composable public open fun BoxScope.KontextFenster(pos: IntOffset = graph.karte.ctx.pos)
    @Composable public open fun BoxScope.Inspektor()

    @Composable public fun ComposableStandard() = Box(objektModifier) { Darstellung() }
    @Composable public fun ComposableKontext() = Box() { KontextFenster() }
    @Composable public fun ComposableInspektor() = Box() { Inspektor() }

    public val öffneKontext get() = derivedStateOf { graph.karte.ctx.objektDatenId == daten.id }
    public val istSelektiert get() = derivedStateOf { graph.karte.auswahl.enthält(this) }

    public fun erhalteAnschluss(knotenId: String,anschlussId: String) = graph.karte.knoten.find { it.daten.id == knotenId }!!.anschlüsse.find { it.daten.id == anschlussId }
    public fun erhalteAnschlussMann(id: GraphDatenVerbindung.IDEhe) = erhalteAnschluss(id.knotenIdMann,id.anschlussIdMann)
    public fun erhalteAnschlussWeib(id: GraphDatenVerbindung.IDEhe) = erhalteAnschluss(id.knotenIdWeib,id.anschlussIdWeib)

//    public fun BildschirmPosition.zuGraph(zustand: Zustand = graph.karte.zustand): KartenPosition = zustand.zuKarte(this)
//    public fun KartenPosition.zuBild(zustand: Zustand = graph.karte.zustand): BildschirmPosition = zustand.zuBild(this)
//    public fun BildschirmPosition.zuDelta(zustand: Zustand = graph.karte.zustand): KartenPosition = this.toOffset() / zustand.erhalteZoom()
//    public fun BildschirmPosition.zuKnoten(knoten: GraphDatenObjekt<GraphDaten.bewegbareGD>, zustand: Zustand = graph.karte.zustand): KnotenPosition = this.toOffset() - knoten.daten.position

    public fun erstelleVerbindung(von: GraphDatenObjektAnschluss<*>, zu: GraphDatenObjektAnschluss<*>) = Unit

    public companion object {
        @Composable public fun Iterable<GraphDatenObjekt<*>>.Composable(/*modifier: Modifier = Modifier*/) = forEach { it.ComposableStandard() }

    }

    public interface Vergrößerbar<D> where D : GraphDaten.bewegbareGD, D : GraphDaten.orthogoneGD {

        public val daten: D

        /**
         * Aktueller Darstellungszoom. Dient dazu, Bildschirmbewegungen
         * in Weltkoordinaten umzurechnen.
         */
        public val vergrößerbarZoom: Float

        public val vergrößerbarFarbe: Color
            get() = Color(0xFF2563EB)

        public val minimaleBreite: Float
            get() = 48f

        public val minimaleTiefe: Float
            get() = 32f

        public val horizontalVergrößerbar: Boolean
            get() = true

        public val vertikalVergrößerbar: Boolean
            get() = true

        /**
         * Verändert die Größe anhand des gezogenen Bereichs.
         *
         * Bei der linken und oberen Kante wird zusätzlich die Position
         * verschoben, damit die gegenüberliegende Kante stehen bleibt.
         */
        public fun vergrößereBereich(
            bereich: VergrößerBereich,
            delta: Offset,
        ) {
            val zoom = vergrößerbarZoom.coerceAtLeast(0.0001f)
            val weltDelta = delta / zoom

            var neuePosition = daten.position
            var neueBreite = daten.breite
            var neueTiefe = daten.tiefe

            if (horizontalVergrößerbar) {
                when {
                    bereich.links -> {
                        val alteBreite = neueBreite
                        neueBreite =
                            (alteBreite - weltDelta.x)
                                .coerceAtLeast(minimaleBreite)

                        val tatsächlicheÄnderung = alteBreite - neueBreite

                        neuePosition += Offset(
                            x = tatsächlicheÄnderung,
                            y = 0f,
                        )
                    }

                    bereich.rechts -> {
                        neueBreite =
                            (neueBreite + weltDelta.x)
                                .coerceAtLeast(minimaleBreite)
                    }
                }
            }

            if (vertikalVergrößerbar) {
                when {
                    bereich.oben -> {
                        val alteTiefe = neueTiefe
                        neueTiefe =
                            (alteTiefe - weltDelta.y)
                                .coerceAtLeast(minimaleTiefe)

                        val tatsächlicheÄnderung = alteTiefe - neueTiefe

                        neuePosition += Offset(
                            x = 0f,
                            y = tatsächlicheÄnderung,
                        )
                    }

                    bereich.unten -> {
                        neueTiefe =
                            (neueTiefe + weltDelta.y)
                                .coerceAtLeast(minimaleTiefe)
                    }
                }
            }

            daten.position = neuePosition
            daten.breite = neueBreite
            daten.tiefe = neueTiefe
        }

        /**
         * Überträgt die in Weltkoordinaten gespeicherte Größe
         * auf das Compose-Layout.
         */
        @Composable
        public fun Modifier.vergrößerbareGröße(): Modifier {
            if (daten.breite <= 0f || daten.tiefe <= 0f) {
                return this
            }

            val dichte = LocalDensity.current

            val breite = with(dichte) {
                daten.breite.toDp()
            }

            val tiefe = with(dichte) {
                daten.tiefe.toDp()
            }

            return this
                .width(breite)
                .height(tiefe)
        }

        /**
         * Zeichnet die sichtbaren Griffe zur Größenänderung.
         */
        @Composable
        public fun BoxScope.VergrößerBereiche() {
            VergrößerBereich.entries
                .filter(::istBereichSichtbar)
                .forEach { bereich ->
                    val istEcke = bereich.istEcke
                    val länge = if (istEcke) 14.dp else 26.dp
                    val dicke = if (istEcke) 14.dp else 8.dp

                    val größenModifier =
                        if (bereich.istVertikal) {
                            Modifier
                                .width(dicke)
                                .height(länge)
                        } else {
                            Modifier
                                .width(länge)
                                .height(dicke)
                        }

                    Box(
                        modifier = Modifier
                            .align(bereich.ausrichtung)
                            .offset {
                                bereich.offset.round()
                            }
                            .then(größenModifier)
                            .background(
                                color = vergrößerbarFarbe,
                                shape = CircleShape,
                            )
                            .zIndex(4f)
                            .pointerInput(
                                bereich,
                                vergrößerbarZoom,
                            ) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    vergrößereBereich(bereich, dragAmount)
                                }
                            },
                    )
                }
        }

        /**
         * Ecken werden nur benötigt, wenn beide Achsen veränderbar sind.
         * Für einzelne Achsen genügen die jeweiligen Seitengriffe.
         */
        public fun istBereichSichtbar(
            bereich: VergrößerBereich,
        ): Boolean = when {
            bereich.istEcke ->
                horizontalVergrößerbar && vertikalVergrößerbar

            bereich.links || bereich.rechts ->
                horizontalVergrößerbar

            bereich.oben || bereich.unten ->
                vertikalVergrößerbar

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

            public val istEcke: Boolean
                get() = (links || rechts) && (oben || unten)

            /**
             * Gibt an, ob der Griff als vertikaler Balken dargestellt wird.
             * Bei Eckgriffen sind Länge und Dicke identisch.
             */
            internal val istVertikal: Boolean
                get() = links || rechts
        }
    }

    public interface Inspektor<D: GraphDaten>: GraphDatenObjektInspektorBasis<D>
}

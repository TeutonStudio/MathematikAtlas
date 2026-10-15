package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import kotlin.math.abs
import kotlin.math.exp

/** Ein Finger dreht, zwei Finger verschieben/zoomen; expliziter Pan-Modus für Maus und Tablet. */
@Composable
internal fun Modifier.geometryNavigation(kamera: GeometryKamera,verschieben: Boolean,aendern: (GeometryKamera) -> Unit): Modifier {
    val aktuell by rememberUpdatedState(kamera)
    val panModus by rememberUpdatedState(verschieben)
    val uebernehmen by rememberUpdatedState(aendern)
    return this.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed=false)
            var stand=aktuell
            var aktiv=false;var summe=Offset.Zero;var zoomSumme=1f
            do {
                val ereignis=awaitPointerEvent()
                val pan=ereignis.calculatePan();val zoom=ereignis.calculateZoom()
                summe+=pan;zoomSumme*=zoom
                val beginn=!aktiv && (summe.getDistance()>viewConfiguration.touchSlop || abs(1-zoomSumme)*ereignis.calculateCentroidSize(useCurrent=false)>viewConfiguration.touchSlop)
                if(beginn) aktiv=true
                if(aktiv && (pan!=Offset.Zero || zoom!=1f || beginn)) {
                    val bewegung=if(beginn) summe else pan
                    val faktor=if(beginn) zoomSumme else zoom
                    val seite=minOf(size.width,size.height).coerceAtLeast(1).toDouble()
                    if(panModus || ereignis.keyboardModifiers.isShiftPressed || ereignis.changes.count { it.pressed }>1) {
                        val anker=ereignis.calculateCentroid(useCurrent=false)
                        if(faktor.isFinite() && faktor>0 && anker.x.isFinite() && anker.y.isFinite())
                            stand=stand.zoomen(faktor.toDouble(),(anker.x-size.width/2.0)/seite,(anker.y-size.height/2.0)/seite)
                        stand=stand.verschieben(bewegung.x/seite,bewegung.y/seite)
                    } else stand=stand.drehen(bewegung.x*0.01,bewegung.y*0.01)
                    uebernehmen(stand)
                    ereignis.changes.forEach { if(it.positionChanged()) it.consume() }
                }
            } while(ereignis.changes.any { it.pressed })
        }
    }.pointerInput(Unit) {
        awaitPointerEventScope {
            while(true) {
                val event=awaitPointerEvent()
                if(event.type==PointerEventType.Scroll) {
                    val change=event.changes.firstOrNull() ?: continue
                    val dy=change.scrollDelta.y
                    if(dy.isFinite() && dy!=0f) {
                        val seite=minOf(size.width,size.height).coerceAtLeast(1).toDouble()
                        uebernehmen(aktuell.zoomen(exp((-dy*0.15).coerceIn(-3.0,3.0)),(change.position.x-size.width/2.0)/seite,(change.position.y-size.height/2.0)/seite))
                        event.changes.forEach { it.consume() }
                    }
                }
            }
        }
    }
}

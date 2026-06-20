package de.TeutonStudio.KnotenKartenVerwalter

import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphPosition
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Auswahl
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektVerbindung

//import de.TeutonStudio.KnotenKartenVerwalter.daten.auswahl.AuswahlDaten
//import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten

/* Skalarprodukt */
//public operator fun Offset.times(other: Offset): Float = x * other.x + y * other.y

public fun printLogCat(vararg arg: Any?) = arg.forEach { println(it) }


/* Geometrie */

/**
 * Position in der Karte
 */
//typealias KartenPosition = Offset
/**
 * Position im Knoten
 */
//typealias KnotenPosition = Offset
/**
 * Position auf dem Bildschirm
 */
//typealias BildschirmPosition = IntOffset
/**
 * Dimensionen eines Rechteck
 */
//typealias Rechteck = Rect

//public operator fun Rechteck.times(other: Float): Offset = diagonale() * other
//public fun Rechteck.diagonale(): Offset = Offset(width,height)



/* Anschluss */


//public fun RectF.overlaps(other: RectF) = left <= other.right && right >= other.left && top <= other.bottom && bottom >= other.top

/* Verbindung */

/* Knoten */

/**
 * Callback fuer eine geaenderte Knotenposition in Weltkoordinaten.
 */
//typealias KartenAktualisierung = (knotenId: String, position: GraphPosition) -> Unit

/**
 * Callback, wenn durch Anschluss-Drag eine neue Verbindung entstanden ist.
 */
//typealias VerbindungErstellen = (verbindung: GraphDatenObjektVerbindung<*>) -> Unit

/**
 * Callback fuer kontrollierte Auswahl von Knoten und Verbindungen.
 */
//typealias AuswahlÄndern = (auswahl: Auswahl) -> Unit

/* Modifier */

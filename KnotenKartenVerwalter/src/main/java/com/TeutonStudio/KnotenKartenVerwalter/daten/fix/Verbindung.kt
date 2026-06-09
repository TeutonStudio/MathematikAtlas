package com.TeutonStudio.KnotenKartenVerwalter.daten.fix

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.TeutonStudio.KnotenKartenVerwalter.VerbindungArt
import com.TeutonStudio.KnotenKartenVerwalter.erhalteKnotenIds
import com.TeutonStudio.KnotenKartenVerwalter.hatGleichenAnschluss
import com.TeutonStudio.KnotenKartenVerwalter.idReferenz
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisEingang
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisVerbindung

/**
 * Fachliche Beschreibung einer Verbindung zwischen zwei Anschlüssen.
 *
 * Quelle und Ziel werden über Knoten-ID und Anschluss-ID referenziert. Dadurch
 * bleiben Verbindungen stabil, auch wenn Knoten visuell verschoben oder
 * Anschlüsse neu gerendert werden.
 */
open class VerbindungDaten(
    override val id: String,
    open val ids: idReferenz,
    open val label: String? = null,
    open val fehler: String? = null,
): GraphDaten {
    override val klasse: VerbindungArt = BasisVerbindung.VERBINDUNG_ART
//    open var ausgewaehlt by mutableStateOf(false)

    // TODO Konstruktor
}

/**
 * Fügt eine Verbindung hinzu und ersetzt dabei eine vorhandene Verbindung auf
 * demselben Ziel-Eingang. Ausgänge bleiben damit automatisch mehrfach nutzbar.
 */
public fun List<VerbindungDaten>.mitErsetztemEingang(verbindung: VerbindungDaten): List<VerbindungDaten> =
    filterNot { it.id == verbindung.id || it.ids.hatGleichenAnschluss(verbindung.ids) } + verbindung

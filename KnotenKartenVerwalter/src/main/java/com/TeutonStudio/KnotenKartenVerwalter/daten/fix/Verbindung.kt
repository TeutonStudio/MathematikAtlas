package com.TeutonStudio.KnotenKartenVerwalter.daten.fix

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
    val ids: idReferenz,
    val label: String? = null,
    val art: String = "default",
    var ausgewaehlt: Boolean = false,
    val fehler: String? = null,
): GraphDaten {
    override val klasse: VerbindungArt = BasisVerbindung.VERBINDUNG_ART

    // TODO Konstruktor
}

/**
 * Fügt eine Verbindung hinzu und ersetzt dabei eine vorhandene Verbindung auf
 * demselben Ziel-Eingang. Ausgänge bleiben damit automatisch mehrfach nutzbar.
 */
public fun List<VerbindungDaten>.mitErsetztemEingang(verbindung: VerbindungDaten): List<VerbindungDaten> =
    filterNot { it.id == verbindung.id || it.ids.hatGleichenAnschluss(verbindung.ids) } + verbindung

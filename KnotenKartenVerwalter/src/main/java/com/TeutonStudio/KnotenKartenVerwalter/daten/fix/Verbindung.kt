package com.TeutonStudio.KnotenKartenVerwalter.daten.fix

import com.TeutonStudio.KnotenKartenVerwalter.VerbindungArt
import com.TeutonStudio.KnotenKartenVerwalter.erhalteKnotenIds
import com.TeutonStudio.KnotenKartenVerwalter.hatGleichenAnschluss
import com.TeutonStudio.KnotenKartenVerwalter.idReferenz
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisEingang

/**
 * Fachliche Beschreibung einer Verbindung zwischen zwei Anschlüssen.
 *
 * Quelle und Ziel werden über Knoten-ID und Anschluss-ID referenziert. Dadurch
 * bleiben Verbindungen stabil, auch wenn Knoten visuell verschoben oder
 * Anschlüsse neu gerendert werden.
 */
open class VerbindungDaten(
    override val id: String,
    override val klasse: VerbindungArt = BasisEingang.ANSCHLUSS_ART,
    val ids: idReferenz,
    val label: String? = null,
    val art: String = "default",
    val ausgewaehlt: Boolean = false,
    val fehler: String? = null,
): GraphDaten {
    fun copy(
        id: String = this.id,
        klasse: VerbindungArt = this.klasse,
        ids: idReferenz = this.ids,
        label: String? = this.label,
        art: String = this.art,
        ausgewaehlt: Boolean = this.ausgewaehlt,
        fehler: String? = this.fehler,
    ): VerbindungDaten = VerbindungDaten(
        id = id,
        klasse = klasse,
        ids = ids,
        label = label,
        art = art,
        ausgewaehlt = ausgewaehlt,
        fehler = fehler,
    )
}

/**
 * Fügt eine Verbindung hinzu und ersetzt dabei eine vorhandene Verbindung auf
 * demselben Ziel-Eingang. Ausgänge bleiben damit automatisch mehrfach nutzbar.
 */
public fun List<VerbindungDaten>.mitErsetztemEingang(verbindung: VerbindungDaten): List<VerbindungDaten> =
    filterNot { it.id == verbindung.id || it.ids.hatGleichenAnschluss(verbindung.ids) } + verbindung

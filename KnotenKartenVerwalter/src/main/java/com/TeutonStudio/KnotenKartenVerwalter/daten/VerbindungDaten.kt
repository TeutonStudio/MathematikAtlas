package com.TeutonStudio.KnotenKartenVerwalter.daten




/**
 * Fachliche Beschreibung einer Verbindung zwischen zwei Anschlüssen.
 *
 * Quelle und Ziel werden über Knoten-ID und Anschluss-ID referenziert. Dadurch
 * bleiben Verbindungen stabil, auch wenn Knoten visuell verschoben oder
 * Anschlüsse neu gerendert werden.
 */
open class VerbindungDaten(
    override val id: String,
    val quellKnotenId: String,
    val quellAnschlussId: String,
    val zielKnotenId: String,
    val zielAnschlussId: String,
    val label: String? = null,
    val art: String = "default",
    val ausgewaehlt: Boolean = false,
    val zahlenTyp: ZahlenTyp? = null,
    val fehler: String? = null,
): GraphDaten {
    fun copy(
        id: String = this.id,
        quellKnotenId: String = this.quellKnotenId,
        quellAnschlussId: String = this.quellAnschlussId,
        zielKnotenId: String = this.zielKnotenId,
        zielAnschlussId: String = this.zielAnschlussId,
        label: String? = this.label,
        art: String = this.art,
        ausgewaehlt: Boolean = this.ausgewaehlt,
        zahlenTyp: ZahlenTyp? = this.zahlenTyp,
        fehler: String? = this.fehler,
    ): VerbindungDaten = VerbindungDaten(
        id = id,
        quellKnotenId = quellKnotenId,
        quellAnschlussId = quellAnschlussId,
        zielKnotenId = zielKnotenId,
        zielAnschlussId = zielAnschlussId,
        label = label,
        art = art,
        ausgewaehlt = ausgewaehlt,
        zahlenTyp = zahlenTyp,
        fehler = fehler,
    )
}

/**
 * Fügt eine Verbindung hinzu und ersetzt dabei eine vorhandene Verbindung auf
 * demselben Ziel-Eingang. Ausgänge bleiben damit automatisch mehrfach nutzbar.
 */
public fun List<VerbindungDaten>.mitErsetztemEingang(verbindung: VerbindungDaten): List<VerbindungDaten> =
    filterNot {
        it.id == verbindung.id ||
            (
                it.zielKnotenId == verbindung.zielKnotenId &&
                    it.zielAnschlussId == verbindung.zielAnschlussId
                )
    } + verbindung

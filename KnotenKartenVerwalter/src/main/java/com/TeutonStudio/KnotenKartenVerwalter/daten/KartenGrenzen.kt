package com.TeutonStudio.KnotenKartenVerwalter.daten

/**
 * Rechteckiger Weltbereich, der Knoten, Verbindungen oder die gesamte Karte umfassen kann.
 *
 * Diese Datei ist als zukuenftiger Ort fuer reine Bounds- und FitView-Funktionen vorgesehen. Reine
 * Funktionen sind hier besser aufgehoben als in Composables, weil sie einfacher getestet werden
 * koennen.
 */
data class KartenGrenzenDaten(
    /** Linke Grenze in Weltkoordinaten. */
    val links: Float,

    /** Obere Grenze in Weltkoordinaten. */
    val oben: Float,

    /** Rechte Grenze in Weltkoordinaten. */
    val rechts: Float,

    /** Untere Grenze in Weltkoordinaten. */
    val unten: Float,
) {
    /** Breite des Grenzrechtecks in Weltkoordinaten. */
    val breite: Float
        get() = rechts - links

    /** Hoehe des Grenzrechtecks in Weltkoordinaten. */
    val hoehe: Float
        get() = unten - oben
}

/**
 * Liefert die Grenzen einer Knotenliste oder `null`, wenn keine Knoten vorhanden sind.
 *
 * Die konkrete Hauptkarte besitzt derzeit noch eigene interne Bounds-Funktionen. Diese Funktion ist
 * der vorbereitete, testbare Zielort fuer diese Logik.
 */
fun List<KnotenDaten>.zuKartenGrenzenDaten(padding: Float = 0f): KartenGrenzenDaten? {
    if (isEmpty()) return null
    val erster = first()
    var links = erster.position.x
    var oben = erster.position.y
    var rechts = erster.position.x + erster.fläche.x
    var unten = erster.position.y + erster.fläche.y

    drop(1).forEach { knoten ->
        links = minOf(links, knoten.position.x)
        oben = minOf(oben, knoten.position.y)
        rechts = maxOf(rechts, knoten.position.x + knoten.fläche.x)
        unten = maxOf(unten, knoten.position.y + knoten.fläche.y)
    }

    return KartenGrenzenDaten(
        links = links - padding,
        oben = oben - padding,
        rechts = rechts + padding,
        unten = unten + padding,
    )
}

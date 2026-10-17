package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.TypSystem.TypAusdruck
import de.TeutonStudio.MathematikRechenSystem.kern.MathematischeTypen

/** Typvorschau für viele sichtbare Ports, ohne den gesamten Graph pro Port zu erweitern. */
class ÜbersetzerVerbindungsPlaner(private val prüfung: GraphPrüfung) {
    private val möglichkeiten = linkedMapOf<Pair<AnschlussDaten, AnschlussDaten>, Boolean>()

    fun vorschläge(karte: KartenDaten, von: AnschlussVerweis, zu: AnschlussVerweis): List<VerbindungsVorschlag> =
        planeÜbersetzerVerbindungen(karte, von, zu, prüfung)

    fun möglich(karte: KartenDaten, erster: AnschlussVerweis, zweiter: AnschlussVerweis): Boolean {
        val (von, zu) = prüfung.normalisiere(karte, erster, zweiter) ?: return false
        if (von.knotenId == zu.knotenId) return false
        val nachfolger = karte.verbindungen.groupBy { it.von.knotenId }
        val offen = ArrayDeque<KnotenId>().apply { add(zu.knotenId) }
        val besucht = mutableSetOf<KnotenId>()
        while (offen.isNotEmpty()) {
            val id = offen.removeFirst()
            if (id == von.knotenId) return false
            if (besucht.add(id)) nachfolger[id].orEmpty().forEach { offen.add(it.zu.knotenId) }
        }
        fun port(ref: AnschlussVerweis, eingang: Boolean): AnschlussDaten? {
            val basis = effektiverMathematikAnschluss(karte, ref) ?: karte.findeAnschluss(ref) ?: return null
            return AnschlussDaten(
                id = AnschlussId(if (eingang) "ziel" else "quelle"), name = "wert",
                richtung = if (eingang) AnschlussRichtung.Eingang else AnschlussRichtung.Ausgang,
                kante = if (eingang) AnschlussKante.Links else AnschlussKante.Rechts,
                art = prüfung.effektiveArt(karte, ref), zulässigeArten = basis.zulässigeArten,
                vertrag = basis.vertrag.copy(typ = prüfung.effektiverTyp(karte, ref)),
            )
        }
        val a = port(von, false) ?: return false
        val b = port(zu, true) ?: return false
        val schlüssel = a to b
        möglichkeiten[schlüssel]?.let { return it }
        val quelle = KnotenDaten(id = KnotenId("vorschau.quelle"), art = "vorschau", name = "Quelle", anschlüsse = listOf(a))
        val ziel = KnotenDaten(id = KnotenId("vorschau.ziel"), art = "vorschau", name = "Ziel", anschlüsse = listOf(b))
        val probe = KartenDaten(name = "Typvorschau", knoten = listOf(quelle, ziel))
        val ausgang = AnschlussVerweis(quelle.id, a.id)
        val eingang = AnschlussVerweis(ziel.id, b.id)
        // Direkt typverträglich kann die echte Kante trotzdem einen abhängigen
        // Ausgang ändern. Dort kann eine explizite Übersetzung den alten Typ erhalten.
        val möglich = prüfung.prüfe(probe, ausgang, eingang) is VerbindungsPrüfung.Erlaubt ||
            vorschläge(probe, ausgang, eingang).isNotEmpty()
        if (möglichkeiten.size >= 128) möglichkeiten.remove(möglichkeiten.keys.first())
        möglichkeiten[schlüssel] = möglich
        return möglich
    }
}

/** Alle kürzesten einfachen Wege durch die vorhandenen, konfigurierten Übersetzer. */
fun planeÜbersetzerVerbindungen(
    karte: KartenDaten,
    erster: AnschlussVerweis,
    zweiter: AnschlussVerweis,
    prüfung: GraphPrüfung,
): List<VerbindungsVorschlag> {
    if (prüfung.prüfe(karte, erster, zweiter) is VerbindungsPrüfung.Erlaubt) return emptyList()
    val (von, zu) = prüfung.normalisiere(karte, erster, zweiter) ?: return emptyList()
    if (von.knotenId == zu.knotenId) return emptyList()
    // Die automatisch angebotenen Tupel-/Vektorübersetzungen ändern keine
    // Komponentenanzahl. Ein gröberer Geometrie-Zwischentyp darf einen bekannten
    // Widerspruch nicht in einen scheinbar kompatiblen unbekannten Typ verwandeln.
    fun komponenten(typ: TypAusdruck): Int? {
        val struktur = typ as? TypAusdruck.Parameterisiert ?: return null
        return when (struktur.konstruktor) {
            MathematischeTypen.Tupel -> struktur.argumente.size
            MathematischeTypen.SpaltenVektor, MathematischeTypen.ZeilenVektor ->
                (struktur.argumente.getOrNull(1) as? TypAusdruck.Literal)?.wert?.toIntOrNull()
            else -> null
        }
    }
    val quellAnzahl = komponenten(prüfung.effektiverTyp(karte, von))
    val zielAnzahl = komponenten(prüfung.effektiverTyp(karte, zu))
    if (quellAnzahl != null && zielAnzahl != null && quellAnzahl != zielAnzahl) return emptyList()
    val quelle = karte.knoten.firstOrNull { it.id == von.knotenId } ?: return emptyList()
    val ziel = karte.knoten.firstOrNull { it.id == zu.knotenId } ?: return emptyList()
    // Den bisher gültigen Eingang bis zur letzten Kante erhalten. Sonst wären
    // abhängige Ausgänge während des Planens vorübergehend untypisiert.
    val basis = karte
    data class Zustand(val karte: KartenDaten, val ausgang: AnschlussVerweis, val ziele: List<String>, val besucht: Set<AnschlussArtId>)
    fun typ(k: KartenDaten, ref: AnschlussVerweis) = prüfung.effektiveArt(k, ref) to prüfung.effektiverTyp(k, ref)
    val tiefen = mutableMapOf(typ(basis, von) to 0)
    var ebene = listOf(Zustand(basis, von, emptyList(), setOf(prüfung.effektiveArt(basis, von))))
    // Identität/Koordinaten-Konventionswechsel und zeugnisabhängige Rückführung sind
    // keine impliziten Anschlusskonvertierungen. Die bestehenden Ziele liefern die Ports.
    val ziele = ÜBERSETZER_ZIELE.filterNot { it in setOf("rueckfuehrung", "polar", "kartesisch", "objektpunkt", "abflachen") }
    repeat(ziele.size) {
        val nächste = mutableListOf<Zustand>()
        val fertig = mutableListOf<VerbindungsVorschlag>()
        for (stand in ebene) for (konversion in ziele) {
            if (konversion in stand.ziele) continue
            val position = GraphPunkt(
                quelle.position.x + (stand.ziele.size + 1) * 260f,
                (quelle.position.y + ziel.position.y) / 2f,
            )
            val knoten = konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(position), konversion)
            val eingang = knoten.anschlüsse.firstOrNull { it.name == "wert" && it.richtung == AnschlussRichtung.Eingang } ?: continue
            val ausgang = knoten.anschlüsse.singleOrNull { it.richtung == AnschlussRichtung.Ausgang } ?: continue
            val probe = stand.karte.copy(knoten = stand.karte.knoten + knoten)
            val verbunden = verbindeGeprüft(probe, stand.ausgang, AnschlussVerweis(knoten.id, eingang.id), prüfung) ?: continue
            val ref = AnschlussVerweis(knoten.id, ausgang.id)
            val zustand = typ(verbunden, ref)
            // Nicht über eine gröber typisierte Zwischenform in dieselbe Art
            // zurückkehren: Das könnte bekannte Dimensionsfehler nur verdecken.
            if (zustand.first in stand.besucht) continue
            val weg = stand.ziele + konversion
            if ((tiefen[zustand] ?: Int.MAX_VALUE) < weg.size) continue
            tiefen[zustand] = weg.size
            val vollständig = verbindeGeprüft(verbunden, ref, zu, prüfung)
            if (vollständig != null && istGültigeVerdrahtung(vollständig, prüfung)) {
                fertig += VerbindungsVorschlag(weg.joinToString(" → ", transform = ::übersetzerZielTitel), vollständig)
            } else nächste += Zustand(verbunden, ref, weg, stand.besucht + zustand.first)
        }
        if (fertig.isNotEmpty()) return fertig.distinctBy { it.titel }
        if (nächste.isEmpty()) return emptyList()
        ebene = nächste
    }
    return emptyList()
}

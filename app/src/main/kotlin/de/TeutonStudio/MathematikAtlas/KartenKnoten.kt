package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikKnoten.mathematikTypSystem

internal enum class KartenKnotenZustand { Schnittstelle, Methode }

internal data class KartenKnotenKompatibilität(
    val karte: KartenDaten,
    val kompatibel: Boolean,
    val grund: String? = null,
)

internal fun KnotenDaten.kartenKnotenZustand(): KartenKnotenZustand =
    if (art.startsWith("methode.")) KartenKnotenZustand.Methode else KartenKnotenZustand.Schnittstelle

internal fun AtlasZustand.kartenVorlage(
    karte: KartenDaten,
    zustand: KartenKnotenZustand = KartenKnotenZustand.Schnittstelle,
): KnotenVorlage {
    val anschlüsse = when (zustand) {
        KartenKnotenZustand.Schnittstelle ->
            öffentlicheKartenAnschlüsse(karte, "mathematik.kartenEingang", AnschlussRichtung.Eingang, AnschlussKante.Links) +
                öffentlicheKartenAnschlüsse(karte, "mathematik.kartenAusgang", AnschlussRichtung.Ausgang, AnschlussKante.Rechts)
        KartenKnotenZustand.Methode -> listOf(
            AnschlussDaten(
                name = "methode",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Methode.id,
            ),
        )
    }
    return KnotenVorlage(
        art = if (zustand == KartenKnotenZustand.Methode) "methode.${karte.id.wert}" else "gruppe.${karte.id.wert}",
        name = karte.name,
        kategorie = "Gespeicherte Karten",
        beschreibung = if (zustand == KartenKnotenZustand.Methode) {
            "Karte als Methode, fest auf Version ${karte.version} verwiesen."
        } else {
            "Wiederverwendbare Karte, fest auf Version ${karte.version} verwiesen."
        },
        standardGröße = GraphGröße(240f, maxOf(100f, 54f + anschlüsse.groupBy { it.richtung }.values.maxOfOrNull { it.size }.orEmpty() * 28f)),
        anschlüsse = anschlüsse,
        kartenVerweis = KartenVerweis(karte.id, karte.version),
    )
}

private fun Int?.orEmpty(): Int = this ?: 0

internal fun AtlasZustand.fügeKartenKnotenEin(karte: KartenDaten, position: GraphPunkt): KnotenDaten? {
    if (!karteIstAlsKnotenZulässig(karte)) return null
    val knoten = kartenVorlage(karte).erzeuge(position)
    editor.führeAus(KartenAktion.KnotenEinfügen(knoten))
    editor.wähleKnoten(knoten.id)
    return knoten
}

internal fun AtlasZustand.kartenVersionen(id: KartenId): List<KartenDaten> {
    val höchste = (karten.firstOrNull { it.id == id } ?: speicher.ladeAktuell(id))?.version ?: return emptyList()
    return (1..höchste).mapNotNull { version -> speicher.lade(KartenVerweis(id, version)) }.sortedByDescending { it.version }
}

internal fun AtlasZustand.kartenKandidaten(knoten: KnotenDaten): List<KartenKnotenKompatibilität> =
    karten.asSequence()
        .filter { !it.archiviert && it.id != editor.karte.id }
        .map { karte ->
            val versionen = kartenVersionen(karte.id)
            val beste = versionen.firstOrNull { prüfeKartenKandidat(knoten, it).kompatibel }
            if (beste != null) KartenKnotenKompatibilität(karte, true)
            else prüfeKartenKandidat(knoten, versionen.firstOrNull() ?: karte)
        }
        .sortedBy { it.karte.name.lowercase() }
        .toList()

internal fun AtlasZustand.prüfeKartenKandidat(
    knoten: KnotenDaten,
    karte: KartenDaten,
): KartenKnotenKompatibilität {
    if (!karteIstAlsKnotenZulässig(karte)) {
        return KartenKnotenKompatibilität(karte, false, "Die Karte würde einen rekursiven Kartenverweis erzeugen.")
    }
    schnittstellenFehler(karte, knoten.kartenKnotenZustand())?.let {
        return KartenKnotenKompatibilität(karte, false, it)
    }
    val kandidat = knotenFürKarte(knoten, karte, knoten.kartenKnotenZustand(), anschlussIdsErhalten = true)
    val incident = editor.karte.verbindungen.filter { it.von.knotenId == knoten.id || it.zu.knotenId == knoten.id }
    val kandidatIds = kandidat.anschlüsse.mapTo(mutableSetOf()) { it.id }
    incident.firstOrNull { verbindung ->
        (verbindung.von.knotenId == knoten.id && verbindung.von.anschlussId !in kandidatIds) ||
            (verbindung.zu.knotenId == knoten.id && verbindung.zu.anschlussId !in kandidatIds)
    }?.let { return KartenKnotenKompatibilität(karte, false, "Mindestens ein verbundener Anschluss fehlt in dieser Karte.") }

    val prüfung = GraphPrüfung(anschlussArten, mathematikTypSystem(anschlussArten))
    var probe = editor.karte.copy(
        knoten = editor.karte.knoten.map { if (it.id == knoten.id) kandidat else it },
        verbindungen = editor.karte.verbindungen.filterNot { it in incident },
    )
    incident.forEach { verbindung ->
        when (val ergebnis = prüfung.prüfe(probe, verbindung.von, verbindung.zu)) {
            VerbindungsPrüfung.Erlaubt -> probe = probe.copy(verbindungen = probe.verbindungen + verbindung)
            is VerbindungsPrüfung.Abgelehnt -> return KartenKnotenKompatibilität(karte, false, ergebnis.grund)
        }
    }
    return KartenKnotenKompatibilität(karte, true)
}

internal fun AtlasZustand.setzeKartenKnotenKarte(knoten: KnotenDaten, karte: KartenDaten): Boolean {
    if (!prüfeKartenKandidat(knoten, karte).kompatibel) return false
    editor.führeAus(KartenAktion.KnotenErsetzen(
        knotenFürKarte(knoten, karte, knoten.kartenKnotenZustand(), anschlussIdsErhalten = true),
    ))
    return true
}

internal fun AtlasZustand.setzeKartenKnotenZustand(knoten: KnotenDaten, zustand: KartenKnotenZustand): Boolean {
    val karte = knoten.kartenVerweis?.let(speicher::lade) ?: return false
    schnittstellenFehler(karte, zustand)?.let { return false }
    editor.führeAus(KartenAktion.KnotenErsetzen(
        knotenFürKarte(knoten, karte, zustand, anschlussIdsErhalten = false),
    ))
    return true
}

private fun AtlasZustand.knotenFürKarte(
    alt: KnotenDaten,
    karte: KartenDaten,
    zustand: KartenKnotenZustand,
    anschlussIdsErhalten: Boolean,
): KnotenDaten {
    val neu = kartenVorlage(karte, zustand).erzeuge(alt.position)
    val ids = if (anschlussIdsErhalten) alt.anschlüsse.associateBy({ it.richtung to it.name }, { it.id }) else emptyMap()
    return neu.copy(
        id = alt.id,
        name = alt.name,
        position = alt.position,
        parameter = alt.parameter,
        eigenschaften = alt.eigenschaften,
        eingangsKartenVerweise = alt.eingangsKartenVerweise,
        anschlüsse = neu.anschlüsse.map { anschluss ->
            ids[anschluss.richtung to anschluss.name]?.let { anschluss.copy(id = it) } ?: anschluss
        },
    )
}

private fun AtlasZustand.karteIstAlsKnotenZulässig(karte: KartenDaten): Boolean =
    karte.id != editor.karte.id && !karte.archiviert && !referenziertKarte(karte, editor.karte.id, mutableSetOf())

private fun AtlasZustand.referenziertKarte(
    karte: KartenDaten,
    gesuchteId: KartenId,
    besucht: MutableSet<KartenVerweis>,
): Boolean {
    val refs = karte.knoten.flatMap(KnotenDaten::alleKartenVerweise)
    if (refs.any { it.kartenId == gesuchteId }) return true
    return refs.any { ref ->
        if (!besucht.add(ref)) false
        else speicher.lade(ref)?.let { referenziertKarte(it, gesuchteId, besucht) } == true
    }
}

private fun schnittstellenFehler(karte: KartenDaten, zustand: KartenKnotenZustand): String? {
    fun namen(art: String) = karte.knoten.filter { it.art == art }.map(::öffentlicherKartenName)
    val eingänge = namen("mathematik.kartenEingang")
    val ausgänge = namen("mathematik.kartenAusgang")
    if (eingänge.distinct().size != eingänge.size) return "Öffentliche Karten-Eingänge benötigen eindeutige Namen."
    if (ausgänge.distinct().size != ausgänge.size) return "Öffentliche Karten-Ausgänge benötigen eindeutige Namen."
    if (zustand == KartenKnotenZustand.Methode && ausgänge.isEmpty()) return "Eine Kartenmethode benötigt mindestens einen Ausgang."
    return null
}

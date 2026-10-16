package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.KnotenKartenVerwalter.logik.findeAnschluss
import de.TeutonStudio.TypSystem.TypAusdruck

/** Abgeleitete, rein lesende Anschlussinformation; enthält keinen Editorzustand. */
data class AnschlussInfo(
    val knotenName: String,
    val name: String,
    val richtung: AnschlussRichtung,
    val arten: List<AnschlussArt>,
    val typ: String,
    val belegung: AnschlussBelegung,
    val verbindungen: List<AnschlussInfoVerbindung>,
    val anforderungen: List<String> = emptyList(),
    val eingabeHinweis: String? = null,
)

data class AnschlussInfoVerbindung(val id: VerbindungsId, val beschreibung: String)

/** Der neutrale Standard funktioniert auch ohne fachlichen Provider. */
fun anschlussBasisInfo(
    karte: KartenDaten,
    ref: AnschlussVerweis,
    register: AnschlussArtRegister = AnschlussArtRegister(),
    aufgelöst: AnschlussDaten? = karte.findeAnschluss(ref),
    typName: (String) -> String = { register.finde(AnschlussArtId(it))?.name ?: it },
): AnschlussInfo? {
    val knoten = karte.knoten.firstOrNull { it.id == ref.knotenId } ?: return null
    val port = aufgelöst ?: return null
    val arten = (port.zulässigeArten.ifEmpty { setOf(port.art) }).sortedBy { it.wert }.map { id ->
        register.finde(id) ?: AnschlussArt(id, id.wert, beschreibung = "Keine Beschreibung für diese Anschlussart vorhanden.")
    }
    val verbindungen = karte.verbindungen.filter { it.von == ref || it.zu == ref }
        .sortedBy { it.eingangsReihenfolge }.map { v ->
            val quelle = v.zu == ref
            val andere = if (quelle) v.von else v.zu
            val andererKnoten = karte.knoten.firstOrNull { it.id == andere.knotenId }
            val andererPort = karte.findeAnschluss(andere)
            AnschlussInfoVerbindung(v.id,
                "${if (quelle) "Quelle" else "Ziel"}: ${andererKnoten?.name ?: "Fehlender Knoten"} · ${andererPort?.name ?: "Fehlender Anschluss"}")
        }
    return AnschlussInfo(knoten.name, port.name, port.richtung, arten,
        anschlussTypText(port.vertrag.typ, typName), port.belegung, verbindungen,
        port.vertrag.anforderungen.map { a ->
            a.id + if (a.parameter.isEmpty()) "" else a.parameter.entries.sortedBy { it.key }
                .joinToString(prefix = " (", postfix = ")") { "${it.key}: ${it.value}" }
        })
}

/** Strukturelle Darstellung ohne fachliche Typkonventionen im Karteneditor. */
internal fun anschlussTypText(typ: TypAusdruck, name: (String) -> String): String = when (typ) {
    TypAusdruck.Unbekannt -> "Noch nicht bestimmt"
    TypAusdruck.Beliebig -> "Beliebiger Typ"
    is TypAusdruck.Atom -> name(typ.id.wert)
    is TypAusdruck.Parameterisiert -> name(typ.konstruktor.wert) +
        typ.argumente.joinToString(prefix = "<", postfix = ">") { anschlussTypText(it, name) }
    is TypAusdruck.Vereinigung -> typ.alternativen.joinToString(" oder ") { anschlussTypText(it, name) }
    is TypAusdruck.Variable -> "Typvariable ${typ.id.wert}"
    is TypAusdruck.Literal -> typ.wert
}

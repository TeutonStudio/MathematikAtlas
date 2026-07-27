package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

class KartenAuswerter(
    private val register: MathematikAuswerterRegister,
    private val kartenQuelle: KartenQuelle = KartenQuelle { null },
) {
    private data class CacheEintrag(val signatur: Int, val ergebnis: KnotenAuswertungsErgebnis)
    private val cache = mutableMapOf<KnotenId, CacheEintrag>()

    fun leereCache() = cache.clear()

    fun auswerten(
        karte: KartenDaten,
        vorgegebeneAusgaben: Map<KnotenId, Map<String, BedingterWert>> = emptyMap(),
    ): KartenAuswertungsErgebnis = auswertenIntern(karte, vorgegebeneAusgaben, setOf(KartenVerweis(karte.id, karte.version)))

    private fun auswertenIntern(
        karte: KartenDaten,
        vorgegebeneAusgaben: Map<KnotenId, Map<String, BedingterWert>>,
        kartenPfad: Set<KartenVerweis>,
    ): KartenAuswertungsErgebnis {
        val ergebnisse = linkedMapOf<KnotenId, KnotenAuswertungsErgebnis>()
        val fehler = mutableListOf<String>()
        val eingehend = karte.verbindungen.groupBy { it.zu.knotenId }
        val nachfolger = karte.verbindungen.groupBy { it.von.knotenId }
        val grad = karte.knoten.associate { knoten -> knoten.id to eingehend[knoten.id].orEmpty().map { it.von.knotenId }.distinct().size }.toMutableMap()
        val offen = ArrayDeque<KnotenId>()
        grad.filterValues { it == 0 }.keys.forEach { offen.add(it) }

        while (offen.isNotEmpty()) {
            val id = offen.removeFirst()
            val knoten = karte.knoten.firstOrNull { it.id == id } ?: continue
            val fest = vorgegebeneAusgaben[id]
            val ergebnis = if (fest != null) {
                KnotenAuswertungsErgebnis(fest)
            } else if (knoten.kartenVerweis != null) {
                werteGruppenKnotenAus(knoten, eingehend[id].orEmpty(), karte, ergebnisse, kartenPfad)
            } else {
                werteKnotenAus(knoten, eingehend[id].orEmpty(), karte, ergebnisse)
            }
            ergebnisse[id] = ergebnis
            ergebnis.fehler?.let { fehler += "${knoten.name}: $it" }
            nachfolger[id].orEmpty().map { it.zu.knotenId }.distinct().forEach { ziel ->
                grad[ziel] = (grad[ziel] ?: 1) - 1
                if (grad[ziel] == 0) offen.add(ziel)
            }
        }

        val nichtAusgewertet = karte.knoten.map { it.id }.filterNot { it in ergebnisse }
        if (nichtAusgewertet.isNotEmpty()) fehler += "${nichtAusgewertet.size} Knoten konnten wegen einer zyklischen oder unvollständigen Abhängigkeit nicht ausgewertet werden."
        return KartenAuswertungsErgebnis(ergebnisse, fehler)
    }

    private fun werteKnotenAus(
        knoten: KnotenDaten,
        verbindungen: List<VerbindungDaten>,
        karte: KartenDaten,
        ergebnisse: Map<KnotenId, KnotenAuswertungsErgebnis>,
    ): KnotenAuswertungsErgebnis {
        val eingänge = sammleEingänge(knoten, verbindungen, karte, ergebnisse)
        val annahmen = eingänge.values.flatMap { it.annahmen }.toSet()
        val signatur = 31 * knoten.hashCode() + eingänge.hashCode()
        cache[knoten.id]?.takeIf { it.signatur == signatur }?.let { return it.ergebnis }
        val auswerter = register.finde(knoten.art)
        val ergebnis = (if (auswerter == null) {
            KnotenAuswertungsErgebnis(emptyMap(), fehler = "Kein Auswerter für ${knoten.art} registriert.")
        } else runCatching {
            auswerter.auswerten(KnotenAuswertungsKontext(knoten, eingänge, RechenKontext(annahmen)))
        }.getOrElse { KnotenAuswertungsErgebnis(emptyMap(), fehler = it.message ?: it::class.simpleName.orEmpty()) }
        ).copy(eingänge = eingänge)
        cache[knoten.id] = CacheEintrag(signatur, ergebnis)
        return ergebnis
    }

    private fun werteGruppenKnotenAus(
        knoten: KnotenDaten,
        verbindungen: List<VerbindungDaten>,
        karte: KartenDaten,
        ergebnisse: Map<KnotenId, KnotenAuswertungsErgebnis>,
        kartenPfad: Set<KartenVerweis>,
    ): KnotenAuswertungsErgebnis {
        val verweis = knoten.kartenVerweis!!
        if (verweis in kartenPfad) return KnotenAuswertungsErgebnis(emptyMap(), fehler = "Zyklischer Kartenverweis erkannt.")
        val intern = kartenQuelle.lade(verweis) ?: return KnotenAuswertungsErgebnis(emptyMap(), fehler = "Referenzierte Karte fehlt.")
        val außen = sammleEingänge(knoten, verbindungen, karte, ergebnisse)
        val interneEingänge = intern.knoten.filter { it.art == "mathematik.kartenEingang" }
        val vorgaben = mutableMapOf<KnotenId, Map<String, BedingterWert>>()
        val freie = mutableListOf<Variable>()
        interneEingänge.forEach { eingang ->
            val name = eingang.parameter["name"] ?: eingang.name
            val wert = außen[name] ?: BedingterWert(Variable(name)).also { freie += it.objekt as Variable }
            vorgaben[eingang.id] = mapOf("wert" to wert)
        }
        val internErgebnis = auswertenIntern(intern, vorgaben, kartenPfad + verweis)
        if (internErgebnis.fehler.isNotEmpty()) return KnotenAuswertungsErgebnis(emptyMap(), fehler = internErgebnis.fehler.joinToString())
        val ausgänge = intern.knoten.filter { it.art == "mathematik.kartenAusgang" }
        val werte = ausgänge.mapNotNull { ausgang ->
            val name = ausgang.parameter["name"] ?: ausgang.name
            internErgebnis.knoten[ausgang.id]?.ausgaben?.get("wert")?.let { name to it }
        }.toMap()
        if (freie.isEmpty()) return KnotenAuswertungsErgebnis(werte)
        val zahlAusgaben = werte.mapValues { (_, wert) -> wert.objekt as? ZahlAusdruck ?: return KnotenAuswertungsErgebnis(werte) }
        val funktion = Funktion(knoten.name, freie.distinctBy { it.name }, zahlAusgaben)
        return KnotenAuswertungsErgebnis(mapOf("funktion" to BedingterWert(funktion)))
    }

    private fun sammleEingänge(
        knoten: KnotenDaten,
        verbindungen: List<VerbindungDaten>,
        karte: KartenDaten,
        ergebnisse: Map<KnotenId, KnotenAuswertungsErgebnis>,
    ): Map<String, BedingterWert> = buildMap {
        verbindungen.forEach { verbindung ->
            val zielAnschluss = knoten.anschlüsse.firstOrNull { it.id == verbindung.zu.anschlussId } ?: return@forEach
            val quellKnoten = karte.knoten.firstOrNull { it.id == verbindung.von.knotenId } ?: return@forEach
            val quellAnschluss = quellKnoten.anschlüsse.firstOrNull { it.id == verbindung.von.anschlussId } ?: return@forEach
            ergebnisse[quellKnoten.id]?.ausgaben?.get(quellAnschluss.name)?.let { put(zielAnschluss.name, it) }
        }
    }
}

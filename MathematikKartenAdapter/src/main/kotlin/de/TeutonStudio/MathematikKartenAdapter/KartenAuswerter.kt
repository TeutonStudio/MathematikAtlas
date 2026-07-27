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
        val offen = java.util.PriorityQueue<KnotenId>(compareBy { it.wert })
        grad.filterValues { it == 0 }.keys.forEach { offen.add(it) }
        val topologischeReihenfolge = mutableMapOf<KnotenId, Int>()

        while (offen.isNotEmpty()) {
            val id = offen.remove()
            topologischeReihenfolge[id] = topologischeReihenfolge.size
            val knoten = karte.knoten.firstOrNull { it.id == id } ?: continue
            val fest = vorgegebeneAusgaben[id]
            val ergebnis = if (fest != null) {
                KnotenAuswertungsErgebnis(fest)
            } else if (knoten.kartenVerweis != null) {
                werteGruppenKnotenAus(knoten, eingehend[id].orEmpty(), karte, ergebnisse, kartenPfad)
            } else {
                werteKnotenAus(knoten, eingehend[id].orEmpty(), karte, ergebnisse, topologischeReihenfolge)
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
        topologischeReihenfolge: Map<KnotenId, Int>,
    ): KnotenAuswertungsErgebnis {
        val eingänge = sammleEingänge(knoten, verbindungen, karte, ergebnisse)
        val annahmen = eingänge.values.flatMap { it.annahmen }.toSet()
        val signatur = 31 * knoten.hashCode() + eingänge.hashCode()
        cache[knoten.id]?.takeIf { it.signatur == signatur }?.let { return it.ergebnis }
        val auswerter = register.finde(knoten.art)
        val ergebnis = (if (auswerter == null) {
            KnotenAuswertungsErgebnis(emptyMap(), fehler = "Kein Auswerter für ${knoten.art} registriert.")
        } else runCatching {
            auswerter.auswerten(KnotenAuswertungsKontext(knoten, eingänge, RechenKontext(annahmen), topologischeReihenfolge))
        }.getOrElse { KnotenAuswertungsErgebnis(emptyMap(), fehler = it.message ?: it::class.simpleName.orEmpty()) }
        ).mitVariablenQuellenAusEingängen(eingänge, knoten.art).copy(eingänge = eingänge)
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
            val name = öffentlicherKartenName(eingang)
            val wert = außen[name] ?: BedingterWert(Variable(name)).also { freie += it.objekt as Variable }
            vorgaben[eingang.id] = mapOf("wert" to wert)
        }
        val internErgebnis = auswertenIntern(intern, vorgaben, kartenPfad + verweis)
        if (internErgebnis.fehler.isNotEmpty()) return KnotenAuswertungsErgebnis(emptyMap(), fehler = internErgebnis.fehler.joinToString())
        val ausgänge = intern.knoten.filter { it.art == "mathematik.kartenAusgang" }.distinctBy(::öffentlicherKartenName)
        val werte = ausgänge.mapNotNull { ausgang ->
            val name = öffentlicherKartenName(ausgang)
            internErgebnis.knoten[ausgang.id]?.ausgaben?.get("wert")?.let { name to it }
        }.toMap()
        if (!knoten.art.startsWith("methode.")) return KnotenAuswertungsErgebnis(werte)
        if (interneEingänge.size != 1) return KnotenAuswertungsErgebnis(emptyMap(), fehler = "Eine Methode benötigt genau einen öffentlichen Karten-Eingang.")
        if (ausgänge.size != 1 || werte.size != 1) return KnotenAuswertungsErgebnis(emptyMap(), fehler = "Eine Methode benötigt genau einen öffentlichen Karten-Ausgang mit Wert.")
        val zielMengen = werte.mapValues { (name, wert) -> wert.zielMenge ?: return KnotenAuswertungsErgebnis(emptyMap(), fehler = "Für die Methodenausgabe '$name' fehlt die Zielmenge.") }
        val funktion = Funktion(knoten.name, freie.distinctBy { it.name }, werte.mapValues { it.value.objekt }, zielMengen)
        if (funktion.einzigeAusgabe().second is MengenAusdruck) funktion.prüfeAlsIterationsMethode(erwartetMengenwert = true)
        return KnotenAuswertungsErgebnis(mapOf("methode" to BedingterWert(funktion)))
    }

    private fun öffentlicherKartenName(knoten: KnotenDaten): String =
        knoten.parameter["name"]?.trim()?.takeIf(String::isNotEmpty) ?: knoten.name

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

    private fun KnotenAuswertungsErgebnis.mitVariablenQuellenAusEingängen(
        eingänge: Map<String, BedingterWert>,
        art: KnotenArtId,
    ): KnotenAuswertungsErgebnis {
        if (art == "mathematik.termZuMethode") return this
        val quellen = eingänge.values.flatMap { it.variablenQuellen }
        if (quellen.isEmpty()) return this
        return copy(ausgaben = ausgaben.mapValues { (_, ausgabe) ->
            ausgabe.copy(variablenQuellen = (ausgabe.variablenQuellen + quellen)
                .distinctBy { quelle -> Pair(Triple(quelle.knotenId, quelle.name, quelle.werteVorrat), quelle.alsMethodenParameter) })
        })
    }
}

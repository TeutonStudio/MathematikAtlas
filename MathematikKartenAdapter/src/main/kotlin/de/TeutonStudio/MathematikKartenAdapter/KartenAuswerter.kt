package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

class KartenAuswerter(
    private val register: MathematikAuswerterRegister,
    private val kartenQuelle: KartenQuelle = KartenQuelle { null },
    private val nichtAuswertbareKnotenArten: Set<KnotenArtId> = emptySet(),
    private val nanoZeit: () -> Long = { System.nanoTime() },
    private val normalisiereKarte: (KartenDaten) -> KartenDaten = { it },
) {
    private data class CacheSignatur(
        val knoten: KnotenDaten,
        val eingänge: Map<String, BedingterWert>,
        val plan: GraphAuswertungsPlan,
    )
    private data class CacheEintrag(val signatur: CacheSignatur, val ergebnis: KnotenAuswertungsErgebnis)
    private val cache = mutableMapOf<KnotenId, CacheEintrag>()

    // Unterkarten wechseln während einer Auswertung: ein einzelner letzter Plan würde thrashen.
    private val pläne = java.util.LinkedHashMap<KartenVerweis, GraphAuswertungsPlan>(16, 0.75f, true)

    fun leereCache() {
        cache.clear()
        pläne.clear()
    }

    private fun planFür(karte: KartenDaten): GraphAuswertungsPlan {
        val verweis = KartenVerweis(karte.id, karte.version)
        val bisher = pläne[verweis]
        if (bisher != null && bisher.passtZu(karte)) return bisher
        if (bisher != null) verwerfePlan(verweis)
        if (pläne.size >= 16) verwerfePlan(pläne.keys.first())
        return GraphAuswertungsPlan(karte).also { pläne[verweis] = it }
    }

    private fun verwerfePlan(verweis: KartenVerweis) {
        val plan = pläne.remove(verweis) ?: return
        cache.entries.removeAll { it.value.signatur.plan === plan }
    }

    private fun speichereErgebnis(id: KnotenId, signatur: CacheSignatur, ergebnis: KnotenAuswertungsErgebnis) {
        // Tiefe Unterkarten können den äußeren Plan während der Berechnung verdrängen.
        // Ein danach abgelegtes Ergebnis dürfte diesen Plan nicht wieder festhalten.
        if (signatur.plan in pläne.values) cache[id] = CacheEintrag(signatur, ergebnis)
    }

    /** Verwirft ausschließlich das zwischengespeicherte Ergebnis des angegebenen Knotens. */
    fun verwerfeCache(knotenId: KnotenId) {
        cache.remove(knotenId)
    }

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
        val plan = planFür(karte)
        val index = GraphAuswertungsIndex(karte)
        val topologischeReihenfolge = mutableMapOf<KnotenId, Int>()

        for (id in plan.reihenfolge) {
            topologischeReihenfolge[id] = topologischeReihenfolge.size
            val knoten = index.knoten.getValue(id)
            val konfigurationsFehler = runCatching { validiereMethodenQuellenKonfiguration(knoten) }.exceptionOrNull()
            val fest = vorgegebeneAusgaben[id]
            val sperre = register.auswertungssperre(knoten)
            val ergebnis = if (sperre != null) {
                KnotenAuswertungsErgebnis(emptyMap(), fehler = sperre)
            } else if (konfigurationsFehler != null) {
                KnotenAuswertungsErgebnis(emptyMap(), fehler = konfigurationsFehler.message)
            } else if (fest != null) {
                KnotenAuswertungsErgebnis(fest)
            } else if (knoten.kartenVerweis != null) {
                werteGruppenKnotenAus(knoten, plan.eingehend[id].orEmpty(), index, plan, ergebnisse, kartenPfad)
            } else {
                werteKnotenAus(knoten, plan.eingehend[id].orEmpty(), index, plan, ergebnisse, topologischeReihenfolge, kartenPfad)
            }
            ergebnisse[id] = ergebnis
            ergebnis.fehler?.let { fehler += "${knoten.name}: $it" }
        }

        val nichtAusgewertet = karte.knoten.map { it.id }.filterNot { it in ergebnisse }
        if (nichtAusgewertet.isNotEmpty()) fehler += "${nichtAusgewertet.size} Knoten konnten wegen einer zyklischen oder unvollständigen Abhängigkeit nicht ausgewertet werden."
        return KartenAuswertungsErgebnis(ergebnisse, fehler)
    }

    private fun werteKnotenAus(
        knoten: KnotenDaten,
        verbindungen: List<VerbindungDaten>,
        index: GraphAuswertungsIndex,
        plan: GraphAuswertungsPlan,
        ergebnisse: Map<KnotenId, KnotenAuswertungsErgebnis>,
        topologischeReihenfolge: Map<KnotenId, Int>,
        kartenPfad: Set<KartenVerweis>,
    ): KnotenAuswertungsErgebnis {
        val startNanos = nanoZeit()
        fun fehler(text: String) = KnotenAuswertungsErgebnis(
            emptyMap(),
            fehler = text,
            auswertungsDauerNanos = dauerSeit(startNanos),
        )

        val verbundeneEingänge = runCatching {
            sammleEingänge(knoten, verbindungen, index, ergebnisse)
        }.getOrElse {
            return fehler(it.message ?: it::class.simpleName.orEmpty())
        }
        val mitKartenFallback = knoten.eingangsKartenVerweise.entries.fold(verbundeneEingänge) { aktuell, (name, verweis) ->
            if (name in aktuell) return@fold aktuell
            val fallbackId = name.substringAfter("@karte:", "")
            if (fallbackId.isNotEmpty() && knoten.parameter.containsKey("join.lokal.$fallbackId")) return@fold aktuell
            if (fallbackId.isNotEmpty() && verbindungen.any { it.eingangsFallback == name }) return@fold aktuell
            if ("@" !in name && aktuell.keys.any { it.startsWith(name + "@") }) return@fold aktuell
            val anschluss = knoten.anschlüsse.firstOrNull {
                it.richtung == AnschlussRichtung.Eingang && it.name == name.substringBefore("@")
            } ?: return fehler("Der Karten-Fallback verweist auf den unbekannten Eingang '$name'.")
            val fallback = werteReferenzierteKarteAus(
                verweis = verweis,
                außen = emptyMap(),
                kartenPfad = kartenPfad,
                alsMethode = true,
            )
            fallback.fehler?.let { fallbackFehler ->
                return fehler("Karten-Fallback für '$name': $fallbackFehler")
            }
            val wert = fallback.ausgaben["methode"]
                ?: return fehler("Der Karten-Fallback für '$name' liefert keine Methode.")
            if (!(setOf(anschluss.art) + anschluss.zulässigeArten).any { anschlussAkzeptiertMethode(it, wert.objekt) }) {
                return fehler("Die ausgewählte Kartenmethode ist für den Eingang '$name' nicht kompatibel.")
            }
            aktuell + ((if (anschluss.belegung == AnschlussBelegung.Mehrfach && "@" !in name) name + "@karte:standard" else name) to wert)
        }
        val identitaeten = runCatching {
            register.optionaleMethodenEingänge(knoten).filterNot { it in mitKartenFallback }.associateWith { name ->
                val port = knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang && it.name == name }
                val art = port.art.wert
                val typ = port.vertrag.typ
                val signatur = typ as? de.TeutonStudio.TypSystem.TypAusdruck.Parameterisiert
                val endoTyp = if (signatur != null) {
                    require(signatur.konstruktor == MathematischeTypen.Methode && signatur.argumente.size == 2) { "Der optionale Methodeneingang besitzt keinen Methodenvertrag." }
                    val argumente = signatur.argumente[0] as? de.TeutonStudio.TypSystem.TypAusdruck.Parameterisiert
                    val ergebnisse = signatur.argumente[1] as? de.TeutonStudio.TypSystem.TypAusdruck.Parameterisiert
                    require(argumente?.konstruktor == MathematischeTypen.Tupel && ergebnisse?.konstruktor == MathematischeTypen.Tupel && argumente.argumente.size == 1 && ergebnisse.argumente.size == 1) { "Identität benötigt genau ein Argument und ein Ergebnis im optionalen Methodenvertrag." }
                    val a = argumente.argumente.single(); val b = ergebnisse.argumente.single()
                    require(a == b || a is de.TeutonStudio.TypSystem.TypAusdruck.Variable || b is de.TeutonStudio.TypSystem.TypAusdruck.Variable || a == de.TeutonStudio.TypSystem.TypAusdruck.Unbekannt || b == de.TeutonStudio.TypSystem.TypAusdruck.Unbekannt) { "Identität passt nicht zum nicht-endomorphen Methodenvertrag." }
                    if (a is de.TeutonStudio.TypSystem.TypAusdruck.Variable || a == de.TeutonStudio.TypSystem.TypAusdruck.Unbekannt) b else a
                } else null
                val methode: Methode = if (endoTyp != null) TypisierteIdentitaetsMethode(endoTyp) else when (art) {
                    "mathematik.funktion.zahl" -> TypisierteIdentitaetsMethode(de.TeutonStudio.TypSystem.TypAusdruck.Atom(MathematischeTypen.Zahl))
                    "mathematik.funktion.aussage" -> TypisierteIdentitaetsMethode(de.TeutonStudio.TypSystem.TypAusdruck.Atom(MathematischeTypen.Aussage))
                    "mathematik.methode", "mathematik.funktion", "atlas.wert", "mathematik.atlasWert", "mathematik.objekt" -> IdentitaetsMethode
                    else -> error("Für den optionalen Methodeneingang '$name' muss eine zur spezialisierten Signatur passende Methode angegeben werden.")
                }
                BedingterWert(methode)
            }
        }.getOrElse { return fehler(it.message ?: "Ungültiger optionaler Methodenvertrag.") }
        val ungeordneteEingänge = mitKartenFallback + identitaeten
        val portReihenfolge = knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.associate { it.name to it.reihenfolge }
        fun beitragsPosition(name: String): Int {
            val id = name.substringAfter("@", "")
            return if (id.startsWith("lokal:")) knoten.parameter["join.position." + id.removePrefix("lokal:")]?.toIntOrNull() ?: Int.MAX_VALUE
            else verbindungen.firstOrNull { it.id.wert == id }?.eingangsReihenfolge ?: knoten.parameter["join.position.$id"]?.toIntOrNull() ?: portReihenfolge[name] ?: Int.MAX_VALUE
        }
        val eingänge = ungeordneteEingänge.entries.sortedBy { beitragsPosition(it.key) }
            .associateTo(linkedMapOf()) { it.key to it.value }
        val annahmen = eingänge.values.flatMap { it.annahmen }.toSet()
        val signatur = CacheSignatur(knoten, eingänge, plan)
        cache[knoten.id]?.takeIf { it.signatur == signatur }?.let { return it.ergebnis }

        val basis = when {
            knoten.art == METHODEN_QUELLE_ART && knoten.eigenschaften[METHODEN_QUELLE_KNOTEN] != null -> runCatching {
                val text = (knoten.eigenschaften[METHODEN_QUELLE_KNOTEN] as? KnotenEigenschaft.Text)?.wert ?: error("Ungültige Knotenquelle.")
                val quelle = KartenDatenJson.lese(text).knoten.single()
                exportiereKarteAlsMethode(normalisiereKarte(methodenQuellKarte(quelle, knoten.id)), quelle.name, kartenPfad)
            }.getOrElse { KnotenAuswertungsErgebnis(emptyMap(), fehler = it.message) }

            knoten.art in nichtAuswertbareKnotenArten -> KnotenAuswertungsErgebnis(emptyMap())

            knoten.art == DARSTELLUNGSOPTIMIERUNG -> runCatching {
                val wert = eingänge["wert"] ?: error("Ein Wert muss verbunden sein.")
                val latex = knoten.parameter["latex"].orEmpty().trim()
                KnotenAuswertungsErgebnis(
                    mapOf("wert" to wert.copy(latexDarstellung = latex.ifBlank { wert.latexDarstellung })),
                )
            }.getOrElse { KnotenAuswertungsErgebnis(emptyMap(), fehler = it.message ?: it::class.simpleName.orEmpty()) }

            knoten.art == MULTIPLIKATION && knoten.anschlüsse.none { it.belegung == AnschlussBelegung.Mehrfach } -> werteMultiplikationAus(knoten, eingänge, annahmen)

            knoten.art == KONJUGIERTE && eingänge["zahl"]?.istNachweisbarReell() == true ->
                KnotenAuswertungsErgebnis(mapOf("wert" to eingänge.getValue("zahl")))

            else -> {
                val auswerter = register.finde(knoten.art)
                if (auswerter == null) {
                    KnotenAuswertungsErgebnis(emptyMap(), fehler = "Kein Auswerter für ${knoten.art} registriert.")
                } else runCatching {
                    auswerter.auswerten(KnotenAuswertungsKontext(knoten, eingänge, RechenKontext(annahmen), topologischeReihenfolge,
                        mehrfachEingänge = knoten.anschlüsse.filter { it.belegung == AnschlussBelegung.Mehrfach }.associate { port ->
                            port.name to eingänge.filterKeys { it.startsWith(port.name + "@") }.values.toList()
                        }))
                }.getOrElse { KnotenAuswertungsErgebnis(emptyMap(), fehler = it.message ?: it::class.simpleName.orEmpty()) }
            }
        }

        val durchgereicht = if (knoten.art in setOf(DARSTELLUNGSOPTIMIERUNG, KARTEN_EINGANG_ART, "mathematik.kartenAusgang", "konzept.eingang"))
            basis.copy(bezeugteAusgaben = basis.ausgaben.keys) else basis
        val ergebnis = durchgereicht
            .führeÜbersetzungsHerkunftFort(eingänge.values)
            .mitVariablenQuellenAusEingängen(eingänge, knoten.art)
            .mitLatexDarstellungen(knoten, eingänge)
            .copy(
                eingänge = eingänge,
                auswertungsDauerNanos = dauerSeit(startNanos),
            )
        speichereErgebnis(knoten.id, signatur, ergebnis)
        return ergebnis
    }

    private fun anschlussAkzeptiertMethode(art: AnschlussArtId, objekt: AtlasWert): Boolean {
        val methode = objekt as? Methode ?: return false
        if (art.wert == "mathematik.methode" || art.wert == "mathematik.funktion") return true
        val symbolisch = methode as? SymbolischMathematischeMethode ?: return false
        val ausgabeName = symbolisch.ausgabeNamen.singleOrNull() ?: return false
        val ausgabe = symbolisch.vorschriftFür(ausgabeName)
        return when (art.wert) {
            "mathematik.funktion.zahl" -> ausgabe is ZahlAusdruck
            "mathematik.funktion.aussage" -> methode.istPrädikat()
            "mathematik.funktion.menge" -> ausgabe is MengenAusdruck
            "mathematik.funktion.vektor.spalte" -> ausgabe is SpaltenVektor
            "mathematik.funktion.vektor.zeile" -> ausgabe is ZeilenVektor
            else -> false
        }
    }

    private fun werteMultiplikationAus(
        knoten: KnotenDaten,
        eingänge: Map<String, BedingterWert>,
        annahmen: Set<Aussage>,
    ): KnotenAuswertungsErgebnis = runCatching {
        val werte = knoten.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Eingang }
            .sortedBy { it.reihenfolge }
            .mapNotNull { eingänge[it.name] }
        val zahlen = werte.map { it.objekt as? ZahlAusdruck ?: error("Multiplikation benötigt Zahlen.") }
        require(zahlen.size >= 2) { "Mindestens zwei Faktoren müssen verbunden sein oder einen Standardwert besitzen." }
        KnotenAuswertungsErgebnis(mapOf(
            "wert" to BedingterWert(
                objekt = multiplikation(zahlen),
                annahmen = annahmen,
                reelleVariablen = reelleVariablen(werte),
            ),
        ))
    }.getOrElse { KnotenAuswertungsErgebnis(emptyMap(), fehler = it.message ?: it::class.simpleName.orEmpty()) }

    private fun werteGruppenKnotenAus(
        knoten: KnotenDaten,
        verbindungen: List<VerbindungDaten>,
        index: GraphAuswertungsIndex,
        plan: GraphAuswertungsPlan,
        ergebnisse: Map<KnotenId, KnotenAuswertungsErgebnis>,
        kartenPfad: Set<KartenVerweis>,
    ): KnotenAuswertungsErgebnis {
        val startNanos = nanoZeit()
        val außen = runCatching {
            sammleEingänge(knoten, verbindungen, index, ergebnisse)
        }.getOrElse {
            return KnotenAuswertungsErgebnis(
                emptyMap(),
                fehler = it.message ?: it::class.simpleName.orEmpty(),
                auswertungsDauerNanos = dauerSeit(startNanos),
            )
        }
        val signatur = CacheSignatur(knoten, außen, plan)
        cache[knoten.id]?.takeIf { it.signatur == signatur }?.let { return it.ergebnis }

        val ergebnis = werteReferenzierteKarteAus(
            verweis = knoten.kartenVerweis!!,
            außen = außen,
            kartenPfad = kartenPfad,
            alsMethode = knoten.art.startsWith("methode.") || knoten.art == METHODEN_QUELLE_ART,
            methodenName = knoten.name,
        ).copy(
            eingänge = außen,
            auswertungsDauerNanos = dauerSeit(startNanos),
        )
        speichereErgebnis(knoten.id, signatur, ergebnis)
        return ergebnis
    }

    /** Isolated evaluator prevents transient source IDs from sharing the live graph cache. */
    fun exportiereKarteAlsMethode(karte: KartenDaten, name: String = karte.name): KnotenAuswertungsErgebnis =
        exportiereKarteAlsMethode(karte, name, emptySet())

    private fun exportiereKarteAlsMethode(karte: KartenDaten, name: String, kartenPfad: Set<KartenVerweis>): KnotenAuswertungsErgebnis {
        val verweis = KartenVerweis(karte.id, karte.version)
        val isoliert = KartenAuswerter(register, KartenQuelle { if (it == verweis) karte else kartenQuelle.lade(it) }, nichtAuswertbareKnotenArten, nanoZeit)
        return isoliert.werteReferenzierteKarteAus(verweis, emptyMap(), kartenPfad, true, name)
    }

    private fun werteReferenzierteKarteAus(
        verweis: KartenVerweis,
        außen: Map<String, BedingterWert>,
        kartenPfad: Set<KartenVerweis>,
        alsMethode: Boolean,
        methodenName: String? = null,
    ): KnotenAuswertungsErgebnis {
        if (verweis in kartenPfad) return KnotenAuswertungsErgebnis(emptyMap(), fehler = "Zyklischer Kartenverweis erkannt.")
        val intern = kartenQuelle.lade(verweis) ?: return KnotenAuswertungsErgebnis(emptyMap(), fehler = "Referenzierte Karte fehlt.")
        val sortierung = kartenGrenzSortierung()
        val interneEingänge = intern.knoten.filter { it.art == "mathematik.kartenEingang" }.sortedWith(sortierung)
        val eingangsNamen = interneEingänge.map(::öffentlicherKartenName)
        if (eingangsNamen.distinct().size != eingangsNamen.size) {
            return KnotenAuswertungsErgebnis(emptyMap(), fehler = "Öffentliche Karten-Eingänge benötigen eindeutige Namen.")
        }
        val vorgaben = mutableMapOf<KnotenId, Map<String, BedingterWert>>()
        val freie = mutableListOf<MethodenParameter>()
        val werteVorräte = linkedMapOf<String, MengenAusdruck>()
        interneEingänge.forEach { eingang ->
            val name = öffentlicherKartenName(eingang)
            val ausgangsArt = eingang.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Ausgang }?.art
                ?: AnschlussArtId("mathematik.objekt")
            val wert = außen[name] ?: symbolischerEingangswert(ausgangsArt, name, eingang.id).also { symbolisch ->
                if (!alsMethode) return@also
                val parameter = symbolisch.objekt as? MethodenParameter
                    ?: return KnotenAuswertungsErgebnis(emptyMap(), fehler = "Karteneingang '$name' ist kein Methodenparameter.")
                freie += parameter
                symbolisch.werteVorrat?.let { werteVorräte[name] = it }
            }
            vorgaben[eingang.id] = mapOf("wert" to wert)
        }
        val internErgebnis = auswertenIntern(intern, vorgaben, kartenPfad + verweis)
        if (internErgebnis.fehler.isNotEmpty()) return KnotenAuswertungsErgebnis(emptyMap(), fehler = internErgebnis.fehler.joinToString())
        val ausgänge = intern.knoten.filter { it.art == "mathematik.kartenAusgang" }.sortedWith(sortierung)
        val ausgangsNamen = ausgänge.map(::öffentlicherKartenName)
        if (ausgangsNamen.distinct().size != ausgangsNamen.size) {
            return KnotenAuswertungsErgebnis(emptyMap(), fehler = "Öffentliche Karten-Ausgänge benötigen eindeutige Namen.")
        }
        val werte = ausgänge.mapNotNull { ausgang ->
            val name = öffentlicherKartenName(ausgang)
            internErgebnis.knoten[ausgang.id]?.ausgaben?.get("wert")?.let { name to it }
        }.toMap(LinkedHashMap())
        if (!alsMethode) return KnotenAuswertungsErgebnis(werte)
        if (werte.size != ausgänge.size) return KnotenAuswertungsErgebnis(emptyMap(), fehler = "Nicht alle öffentlichen Kartenausgänge liefern einen Wert.")

        val geordneteWerte = ausgangsNamen.map { name ->
            name to requireNotNull(werte[name]) { "Für den Methodenausgang '$name' fehlt ein Wert." }
        }
        val mathematischeAusgaben = linkedMapOf<String, MathematischesObjekt>()
        val zielMengen = linkedMapOf<String, MengenAusdruck>()
        geordneteWerte.forEach { (name, wert) ->
            val objekt = wert.objekt as? MathematischesObjekt
                ?: return KnotenAuswertungsErgebnis(
                    emptyMap(),
                    fehler = "Die mathematische Kartenausgabe '$name' liefert keinen mathematischen Wert.",
                )
            val grenze = ausgänge.single { öffentlicherKartenName(it) == name }
            val ziel = wert.zielMenge ?: if (METHODEN_KOMPONENTEN_ID in grenze.parameter) {
                runCatching { inferiereZielmenge(objekt, werteVorräte, wert.annahmen) }.getOrNull()
            } else null
            if (ziel == null) return KnotenAuswertungsErgebnis(
                    emptyMap(),
                    fehler = "Für die Methodenausgabe '$name' fehlt die Zielmenge.",
                )
            mathematischeAusgaben[name] = objekt
            zielMengen[name] = ziel
        }
        @Suppress("DEPRECATION")
        val methode = MathematischeMethode(
            name = methodenName?.trim().orEmpty().ifBlank { intern.name },
            parameter = freie.distinctBy { it.name },
            ausgaben = mathematischeAusgaben,
            zielMengen = zielMengen,
            werteVorräte = werteVorräte,
        )
        val herkunft = ÜbersetzungsMetadaten(herkunft = geordneteWerte.flatMap { it.second.übersetzungen.alsHerkunft().herkunft }.distinct())
        val argumentIds = interneEingänge.associate { eingang ->
            öffentlicherKartenName(eingang) to (eingang.parameter[METHODEN_KOMPONENTEN_ID] ?: eingang.id.wert)
        }
        val ergebnisIds = ausgänge.associate { ausgang ->
            öffentlicherKartenName(ausgang) to (ausgang.parameter[METHODEN_KOMPONENTEN_ID] ?: ausgang.id.wert)
        }
        val übertragen = methode.copy(argumentIdentitäten = argumentIds.filterKeys { key -> freie.any { it.name == key } }, ergebnisIdentitäten = ergebnisIds, voraussetzungen = geordneteWerte.flatMap { it.second.annahmen }.toSet(), übersetzungen = herkunft)
        return KnotenAuswertungsErgebnis(mapOf("methode" to BedingterWert(übertragen, latexDarstellung = methode.name, übersetzungen = herkunft)))
    }

    private fun öffentlicherKartenName(knoten: KnotenDaten): String =
        knoten.parameter["name"]?.trim()?.takeIf(String::isNotEmpty) ?: knoten.name

    private fun sammleEingänge(
        knoten: KnotenDaten,
        verbindungen: List<VerbindungDaten>,
        index: GraphAuswertungsIndex,
        ergebnisse: Map<KnotenId, KnotenAuswertungsErgebnis>,
    ): Map<String, BedingterWert> = buildMap {
        val gebundeneIds = verbindungen.map { it.zu.anschlussId }.toSet()
        knoten.anschlüsse.filter { it.belegung == AnschlussBelegung.Mehrfach }.forEach { port ->
            knoten.parameter.filterKeys { it.startsWith("join.lokal.") }.entries.sortedBy { (key, _) -> knoten.parameter["join.position." + key.removePrefix("join.lokal.")]?.toIntOrNull() ?: Int.MAX_VALUE }.forEach { (key, text) ->
                val objekt: MathematischesObjekt = if (port.art == AUSSAGE_ART || AUSSAGE_ART in port.zulässigeArten) {
                    WahrheitsKonstante(text.toBooleanStrictOrNull() ?: error("Lokaler Operand muss true oder false sein."))
                } else RationaleZahl.parse(text)
                put(port.name + "@lokal:" + key.removePrefix("join.lokal."), BedingterWert(objekt))
            }
        }
        verbindungen.sortedBy { it.eingangsReihenfolge }.forEach { verbindung ->
            val zielAnschluss = index.anschluss(verbindung.zu)
                ?: error("Eine Verbindung verweist auf einen fehlenden Eingang.")
            val quellKnoten = index.knoten[verbindung.von.knotenId]
                ?: error("Die Quelle für '${zielAnschluss.name}' fehlt.")
            val quellAnschluss = index.anschluss(verbindung.von)
                ?: error("Der Quellanschluss für '${zielAnschluss.name}' fehlt.")
            val quelle = ergebnisse[quellKnoten.id]
            val wert = quelle?.ausgaben?.get(quellAnschluss.name)
                ?: error("Verbundener Eingang '${zielAnschluss.name}': ${quelle?.fehler ?: "Die Quelle liefert keinen Wert."}")
            put(if (zielAnschluss.belegung == AnschlussBelegung.Mehrfach) zielAnschluss.name + "@" + verbindung.id.wert else zielAnschluss.name, wert)
        }
        knoten.anschlüsse.filter {
            it.belegung != AnschlussBelegung.Mehrfach && it.richtung == AnschlussRichtung.Eingang && (it.art == AUSSAGE_ART || AUSSAGE_ART in it.zulässigeArten) && it.id !in gebundeneIds
        }.forEach { anschluss ->
            val text = knoten.parameter[standardwertSchlüssel(anschluss.name)]
            if (text == null && (anschluss.dynamischErzeugt || anschluss.name in knoten.eingangsKartenVerweise)) return@forEach
            val wert = text?.toBooleanStrictOrNull() ?: if (text == null) false else
                error("Standardwert für '${anschluss.name}' muss true oder false sein.")
            put(anschluss.name, BedingterWert(WahrheitsKonstante(wert)))
        }
        knoten.anschlüsse
            .filter {
                it.belegung != AnschlussBelegung.Mehrfach && it.richtung == AnschlussRichtung.Eingang &&
                    (it.art == ZAHL_ART || ZAHL_ART in it.zulässigeArten || knoten.art == "mathematik.tupel" && it.art.wert == "mathematik.objekt") &&
                    it.name !in this && it.id !in gebundeneIds
            }
            .forEach { anschluss ->
                val text = knoten.parameter[standardwertSchlüssel(anschluss.name)]?.trim().orEmpty()
                if (text.isBlank()) return@forEach
                val zahl = runCatching { RationaleZahl.parse(text) }.getOrElse {
                    error("Standardwert für '${anschluss.name}' ist keine gültige ganze oder rationale Zahl: '$text'.")
                }
                put(
                    anschluss.name,
                    BedingterWert(
                        objekt = zahl,
                        latexDarstellung = zahl.zuStrukturLatex(),
                    ),
                )
            }
    }

    private fun KnotenAuswertungsErgebnis.mitVariablenQuellenAusEingängen(
        eingänge: Map<String, BedingterWert>,
        art: KnotenArtId,
    ): KnotenAuswertungsErgebnis {
        val quellen = eingänge.values.flatMap { it.variablenQuellen }
        if (quellen.isEmpty()) return this
        return copy(ausgaben = ausgaben.mapValues { (_, ausgabe) ->
            val gebundeneNamen = if (art == "mathematik.termZuMethode" || art == "atlas.uebersetzer")
                (ausgabe.objekt as? Methode)?.parameter?.map { it.name }?.toSet().orEmpty() else emptySet()
            val gebundeneIds = if (art == "mathematik.termZuMethode" || art == "atlas.uebersetzer")
                (ausgabe.objekt as? MathematischeMethode)?.argumentIdentitäten?.values.orEmpty() else emptyList()
            ausgabe.copy(variablenQuellen = (ausgabe.variablenQuellen + quellen.filterNot {
                it.alsMethodenParameter && (it.name in gebundeneNamen || it.bindungsId in gebundeneIds || it.strukturBindung?.id in gebundeneIds)
            })
                .distinctBy { quelle -> Pair(Triple(quelle.knotenId, quelle.name, quelle.werteVorrat), quelle.alsMethodenParameter) })
        })
    }

    private fun KnotenAuswertungsErgebnis.mitLatexDarstellungen(
        knoten: KnotenDaten,
        eingänge: Map<String, BedingterWert>,
    ): KnotenAuswertungsErgebnis {
        if (knoten.art == DARSTELLUNGSOPTIMIERUNG || eingänge.values.none { it.latexDarstellung != null }) return this

        fun wert(name: String): String = eingänge[name]?.anzeigeLatex() ?: "?"
        fun geordnet(): List<String> = knoten.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Eingang }
            .sortedBy { it.reihenfolge }
            .mapNotNull { eingänge[it.name]?.anzeigeLatex() }
        fun binär(zeichen: String) = "${wert("links")} $zeichen ${wert("rechts")}"
        fun aUndB(zeichen: String) = "${wert("a")} $zeichen ${wert("b")}"

        val darstellungen: Map<String, String> = when (knoten.art) {
            "mathematik.addition" -> mapOf("wert" to geordnet().joinToString(" + "))
            "mathematik.multiplikation" -> mapOf("wert" to geordnet().joinToString(" \\cdot "))
            "mathematik.division" -> mapOf(
                "wert" to if (eingänge["fallsNennerNull"] == null) {
                    "\\frac{${wert("dividend")}}{${wert("divisor")}}"
                } else {
                    """\begin{cases}${wert("fallsNennerNull")},&${wert("divisor")}=0\\\frac{${wert("dividend")}}{${wert("divisor")}},&${wert("divisor")}\ne0\end{cases}"""
                },
            )
            "mathematik.potenz" -> mapOf("wert" to "\\left(${wert("basis")}\\right)^{${wert("exponent")}}")
            "mathematik.kehrwert" -> mapOf("wert" to "\\left(${wert("zahl")}\\right)^{-1}")
            "mathematik.wurzel" -> mapOf("wert" to "\\sqrt{${wert("radikand")}}")
            "mathematik.logarithmus" -> mapOf("wert" to "\\log_{${wert("basis")}}\\left(${wert("argument")}\\right)")
            "mathematik.ableiten" -> mapOf("wert" to "\\frac{d}{d${knoten.parameter["variable"] ?: "x"}}\\left(${wert("term")}\\right)")
            "mathematik.integrieren" -> mapOf("wert" to "\\int ${wert("term")}\\,d${knoten.parameter["variable"] ?: "x"}")
            "mathematik.gleichheit" -> mapOf("aussage" to binär("="))
            "mathematik.ordnungsrelation" -> mapOf(
                "aussage" to binär(when (knoten.parameter["relation"]) {
                    "kleiner" -> "<"
                    "kleinerGleich" -> "\\le"
                    "größer" -> ">"
                    "größerGleich" -> "\\ge"
                    else -> error("Unbekannte Ordnungsrelation.")
                }),
            )
            "mathematik.kleiner" -> mapOf("aussage" to binär("<"))
            "mathematik.größer" -> mapOf("aussage" to binär(">"))
            "mathematik.kleinerGleich" -> mapOf("aussage" to binär("\\le"))
            "mathematik.größerGleich" -> mapOf("aussage" to binär("\\ge"))
            "mathematik.element" -> mapOf("aussage" to binär("\\in"))
            "mathematik.teilmenge" -> mapOf("aussage" to binär("\\subset"))
            "mathematik.übermenge" -> mapOf("aussage" to binär("\\supset"))
            "mathematik.teilOderGleichmenge" -> mapOf("aussage" to binär("\\subseteq"))
            "mathematik.überOderGleichmenge" -> mapOf("aussage" to binär("\\supseteq"))
            "mathematik.disjunkt" -> mapOf("aussage" to "${wert("links")} \\cap ${wert("rechts")} = \\varnothing")
            "mathematik.vereinigung" -> mapOf("menge" to geordnet().joinToString(" \\cup "))
            "mathematik.schnitt" -> mapOf("menge" to geordnet().joinToString(" \\cap "))
            "mathematik.kartesischesProdukt" -> mapOf("menge" to geordnet().joinToString(" \\times "))
            "mathematik.differenz" -> mapOf("menge" to binär("\\setminus"))
            "mathematik.einzelmenge" -> mapOf("menge" to "\\left\\{${wert("element")}\\right\\}")
            "mathematik.tupel" -> mapOf("tupel" to geordnet().joinToString(prefix = "\\left(", postfix = "\\right)"))
            "mathematik.vektor" -> mapOf("vektor" to geordnet().joinToString(separator = " \\\\ ", prefix = "\\begin{pmatrix}", postfix = "\\end{pmatrix}"))
            "mathematik.zeilenVektor" -> mapOf("vektor" to geordnet().joinToString(prefix = "\\left(", postfix = "\\right)"))
            "mathematik.konjunktion" -> mapOf("aussage" to geordnet().joinToString(" \\land "))
            "mathematik.adjunktion" -> mapOf("aussage" to geordnet().joinToString(" \\stackrel{\\bullet}{\\lor} "))
            "mathematik.disjunktion" -> mapOf("aussage" to geordnet().joinToString(" \\lor "))
            "mathematik.implikation" -> mapOf("aussage" to aUndB("\\implies"))
            "mathematik.äquivalenz" -> mapOf("aussage" to aUndB("\\iff"))
            "mathematik.abbild" -> mapOf("menge" to "${wert("methode")}\\left[${wert("menge")}\\right]")
            "mathematik.komposition" -> mapOf("methode" to "${wert("außen")} \\circ ${wert("innen")}")
            "mathematik.iteration" -> mapOf("methode" to "\\left(${wert("methode")}\\right)^{${wert("exponent")}}")
            "mathematik.methodenDifferentieren", "mathematik.spaltenMethodeDifferentieren", "mathematik.zeilenMethodeDifferentieren" ->
                mapOf("methode" to "\\left(${wert("methode")}\\right)'")
            "mathematik.methodenIntegrieren", "mathematik.spaltenMethodeIntegrieren", "mathematik.zeilenMethodeIntegrieren" ->
                mapOf("methode" to "\\int ${wert("methode")}")
            "mathematik.fall" -> mapOf(
                "wert" to "\\begin{cases}${wert("wahr")},&${wert("aussage")}\\\\${wert("lüge")},&\\text{sonst}\\end{cases}",
            )
            else -> emptyMap()
        }

        if (darstellungen.isNotEmpty()) return copy(ausgaben = ausgaben.mapValues { (name, ausgabe) ->
            ausgabe.copy(latexDarstellung = darstellungen[name] ?: ausgabe.latexDarstellung)
        })

        if (eingänge.size == 1 && ausgaben.size == 1) {
            val eingang = eingänge.values.single()
            val ausgabe = ausgaben.values.single()
            if (eingang.objekt == ausgabe.objekt) {
                val name = ausgaben.keys.single()
                return copy(ausgaben = mapOf(name to ausgabe.copy(latexDarstellung = eingang.anzeigeLatex())))
            }
        }
        return this
    }

    private fun dauerSeit(startNanos: Long): Long = (nanoZeit() - startNanos).coerceAtLeast(0L)

    private fun standardwertSchlüssel(name: String) = "$STANDARDWERT_PREFIX$name"

    private companion object {
        const val DARSTELLUNGSOPTIMIERUNG = "mathematik.darstellungsoptimierung"
        const val MULTIPLIKATION = "mathematik.multiplikation"
        const val KONJUGIERTE = "mathematik.konjugierte"
        const val STANDARDWERT_PREFIX = "standardwert."
        val ZAHL_ART = AnschlussArtId("mathematik.zahl")
        val AUSSAGE_ART = AnschlussArtId("mathematik.aussage")
    }
}

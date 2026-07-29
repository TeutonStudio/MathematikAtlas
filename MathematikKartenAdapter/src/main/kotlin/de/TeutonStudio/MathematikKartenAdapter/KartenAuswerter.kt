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

        val basis = if (knoten.art == DARSTELLUNGSOPTIMIERUNG) {
            runCatching {
                val wert = eingänge["wert"] ?: error("Ein Wert muss verbunden sein.")
                val latex = knoten.parameter["latex"].orEmpty().trim()
                KnotenAuswertungsErgebnis(
                    mapOf("wert" to wert.copy(latexDarstellung = latex.ifBlank { wert.latexDarstellung })),
                )
            }.getOrElse { KnotenAuswertungsErgebnis(emptyMap(), fehler = it.message ?: it::class.simpleName.orEmpty()) }
        } else {
            val auswerter = register.finde(knoten.art)
            if (auswerter == null) {
                KnotenAuswertungsErgebnis(emptyMap(), fehler = "Kein Auswerter für ${knoten.art} registriert.")
            } else runCatching {
                auswerter.auswerten(KnotenAuswertungsKontext(knoten, eingänge, RechenKontext(annahmen), topologischeReihenfolge))
            }.getOrElse { KnotenAuswertungsErgebnis(emptyMap(), fehler = it.message ?: it::class.simpleName.orEmpty()) }
        }

        val ergebnis = basis
            .mitVariablenQuellenAusEingängen(eingänge, knoten.art)
            .mitLatexDarstellungen(knoten, eingänge)
            .copy(eingänge = eingänge)
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
            "mathematik.konjunktion", "mathematik.adjunktion" -> mapOf("aussage" to geordnet().joinToString(" \\land "))
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

    private companion object {
        const val DARSTELLUNGSOPTIMIERUNG = "mathematik.darstellungsoptimierung"
    }
}

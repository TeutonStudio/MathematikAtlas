package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

object StandardMathematikAuswerter {
    fun erzeugeRegister() = MathematikAuswerterRegister().apply {
        registriere("mathematik.zahl") { k ->
            KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(RationaleZahl.parse(k.knoten.parameter["wert"] ?: "0"))))
        }
        registriere("mathematik.variable") { k ->
            KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(Variable(k.knoten.parameter["name"] ?: "x"))))
        }
        registriere("mathematik.addition") { k ->
            val werte = k.operatorEingänge { anschluss, index ->
                Variable(unbekannteKennung(k.knoten, anschluss), unbekanntesOperatorLatex(k.knoten, index))
            }.map { it.objekt as? ZahlAusdruck ?: error("Zahleingang ${it} ist ungültig.") }
            require(werte.size >= 2) { "Mindestens zwei Summanden müssen verbunden sein." }
            KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(addition(werte), annahmen(k))))
        }
        registriere("mathematik.multiplikation") { k ->
            val werte = listOf("a", "b", "c").mapNotNull { k.eingänge[it]?.objekt as? ZahlAusdruck }
            require(werte.size >= 2) { "Mindestens zwei Faktoren müssen verbunden sein." }
            KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(multiplikation(werte), annahmen(k))))
        }
        registriere("mathematik.division") { k ->
            val dividend = k.zahl("dividend"); val divisor = k.zahl("divisor")
            KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(vereinfache(Division(dividend, divisor), k.rechenKontext), annahmen(k))))
        }
        registriere("mathematik.potenz") { k ->
            val basis = k.zahl("basis"); val exponent = k.zahl("exponent")
            KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(Potenz(basis, exponent), annahmen(k))))
        }
        registriere("mathematik.gleichheit") { k ->
            val links = k.eingänge["links"]?.objekt ?: error("Linke Seite fehlt.")
            val rechts = k.eingänge["rechts"]?.objekt ?: error("Rechte Seite fehlt.")
            KnotenAuswertungsErgebnis(mapOf("aussage" to BedingterWert(Gleichheit(links, rechts), annahmen(k))))
        }
        registriere("mathematik.gleichungLösen") { k ->
            val gleichung = k.eingänge["gleichung"]?.objekt as? Gleichheit ?: error("Eine Gleichheit muss verbunden sein.")
            val ergebnis = löseLinear(gleichung, Variable(k.knoten.parameter["variable"] ?: "x"))
            KnotenAuswertungsErgebnis(
                mapOf("lösungen" to BedingterWert(EndlicheMenge(ergebnis.lösungen.toSet()), annahmen(k))),
                ergebnis.schritte,
            )
        }
        registriere("mathematik.auswerten") { k ->
            val objekt = k.eingänge["objekt"]?.objekt ?: error("Objekt fehlt.")
            when (objekt) {
                is ZahlAusdruck -> {
                    val e = vereinfacheMitSchritten(objekt, k.rechenKontext)
                    KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(e.ergebnis, annahmen(k))), e.schritte)
                }
                is Aussage -> {
                    val ergebnis = objekt.entscheide(k.rechenKontext)
                    val auswertung = ergebnis.wahrheitswert?.let { WahrheitsKonstante(it == Wahrheitswert.Wahr) } ?: objekt
                    KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(auswertung, annahmen(k))), fehler = if (ergebnis.status is EntscheidungsStatus.NichtAuswertbar) "Aussage nicht auswertbar" else null)
                }
                else -> KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(objekt, annahmen(k))))
            }
        }
        registriere("mathematik.ableiten") { k ->
            val e = ableiten(k.zahl("term"), Variable(k.knoten.parameter["variable"] ?: "x"))
            KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(e.ergebnis, annahmen(k))), e.schritte)
        }
        registriere("mathematik.integrieren") { k ->
            val e = integrieren(k.zahl("term"), Variable(k.knoten.parameter["variable"] ?: "x"))
            KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(e.ergebnis, annahmen(k))), e.schritte)
        }
        registriere("mathematik.endlicheMenge") { k ->
            val elemente = (k.knoten.parameter["elemente"] ?: "").split(',').filter { it.isNotBlank() }.map { RationaleZahl.parse(it) }.toSet()
            KnotenAuswertungsErgebnis(mapOf("menge" to BedingterWert(EndlicheMenge(elemente))))
        }
        registriere("mathematik.vereinigung") { k ->
            val mengen = k.operatorEingänge { anschluss, index ->
                BenannteMenge(unbekannteKennung(k.knoten, anschluss), unbekanntesOperatorLatex(k.knoten, index))
            }.map { it.objekt as? MengenAusdruck ?: error("Mengeneingang ${it} ist ungültig.") }
            require(mengen.size >= 2) { "Mindestens zwei Mengen müssen verbunden sein." }
            val wert = if (mengen.all { it is EndlicheMenge }) {
                EndlicheMenge(mengen.filterIsInstance<EndlicheMenge>().flatMap { it.elemente }.toSet())
            } else {
                Vereinigung(mengen.flatMap { if (it is Vereinigung) it.mengen else listOf(it) })
            }
            KnotenAuswertungsErgebnis(mapOf("menge" to BedingterWert(wert, annahmen(k))))
        }
        registriere("mathematik.vektor") { k ->
            val werte = parseZahlen(k.knoten.parameter["werte"] ?: "")
            KnotenAuswertungsErgebnis(mapOf("vektor" to BedingterWert(Vektor(werte))))
        }
        registriere("mathematik.matrix") { k ->
            val zeilen = (k.knoten.parameter["werte"] ?: "").split(';').filter { it.isNotBlank() }.map(::parseZahlen)
            KnotenAuswertungsErgebnis(mapOf("matrix" to BedingterWert(Matrix(zeilen))))
        }
        registriere("mathematik.skalarprodukt") { k ->
            val a = k.eingänge["a"]?.objekt as? Vektor ?: error("Vektor a fehlt.")
            val b = k.eingänge["b"]?.objekt as? Vektor ?: error("Vektor b fehlt.")
            KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(vereinfache(a.skalarprodukt(b)), annahmen(k))))
        }
        registriere("mathematik.matrixInvertieren") { k ->
            val matrix = k.eingänge["matrix"]?.objekt as? Matrix ?: error("Matrix fehlt.")
            KnotenAuswertungsErgebnis(mapOf("inverse" to BedingterWert(matrix.inverseRational(), annahmen(k))))
        }
        registriere("mathematik.kartenEingang") { k ->
            val name = k.knoten.parameter["name"] ?: "x"
            KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(Variable(name))))
        }
        registriere("mathematik.kartenAusgang") { k ->
            val wert = k.eingänge["wert"] ?: error("Ausgabewert fehlt.")
            KnotenAuswertungsErgebnis(mapOf("wert" to wert))
        }
        registriere("mathematik.fall") { k ->
            val wert = k.eingänge["term"] ?: error("Term fehlt.")
            val bedingung = parseBedingung(k.knoten.parameter["bedingung"] ?: "x=0")
            KnotenAuswertungsErgebnis(mapOf(
                "fall" to wert.copy(annahmen = wert.annahmen + bedingung),
                "sonst" to wert.copy(annahmen = wert.annahmen + Negation(bedingung)),
            ))
        }
    }

    private fun KnotenAuswertungsKontext.zahl(name: String) = eingänge[name]?.objekt as? ZahlAusdruck ?: error("Zahleingang $name fehlt.")
    private fun annahmen(k: KnotenAuswertungsKontext) = k.eingänge.values.flatMap { it.annahmen }.toSet()
    private fun parseZahlen(text: String) = text.split(',').filter { it.isNotBlank() }.map { RationaleZahl.parse(it.trim()) }
    private fun parseBedingung(text: String): Aussage {
        val ungleich = text.split("!=")
        if (ungleich.size == 2) return Ungleichheit(parseTerm(ungleich[0]), parseTerm(ungleich[1]))
        val gleich = text.split('=')
        if (gleich.size == 2) return Gleichheit(parseTerm(gleich[0]), parseTerm(gleich[1]))
        return UnentscheidbareAussage(text, "Benutzerdefinierter Kontext")
    }
    private fun parseTerm(text: String): ZahlAusdruck = text.trim().toLongOrNull()?.let(RationaleZahl::von) ?: Variable(text.trim())
}

/** Formeln für assoziative Operatoren verwenden für fehlende Eingänge stabile, eindeutige Unbekannte. */
internal fun KnotenAuswertungsKontext.operatorEingänge(
    unbekannt: (de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten, Int) -> MathematischesObjekt,
): List<BedingterWert> = knoten.anschlüsse
    .filter { it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Eingang }
    .sortedBy { it.reihenfolge }
    .mapIndexed { index, anschluss -> eingänge[anschluss.name] ?: BedingterWert(unbekannt(anschluss, index + 1)) }

internal fun eingabeLatex(index: Int) = "\\mathrm{eingabe}_{${index}}"

internal fun unbekanntesOperatorLatex(
    knoten: de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten,
    index: Int,
) = "\\mathrm{${knoten.name}}_{${eingabeLatex(index)}}"

private fun unbekannteKennung(
    knoten: de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten,
    anschluss: de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten,
) = "unbekannt_${knoten.id.wert}_${anschluss.id.wert}"

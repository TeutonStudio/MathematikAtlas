package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

object StandardMathematikAuswerter {
    fun erzeugeRegister() = MathematikAuswerterRegister().apply {
        registriere("mathematik.zahl") { k ->
            KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(RationaleZahl.parse(k.knoten.parameter["wert"] ?: "0"))))
        }
        registriere("mathematik.variable") { k ->
            val wertevorrat = k.eingänge["wertevorrat"]?.objekt as? MengenAusdruck
            KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(Variable(k.knoten.parameter["name"] ?: "x"), werteVorrat = wertevorrat)))
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
            val nullFall = Gleichheit(divisor, RationaleZahl.Null)
            val definiert = Ungleichheit(divisor, RationaleZahl.Null)
            KnotenAuswertungsErgebnis(mapOf(
                "wert" to BedingterWert(vereinfache(Division(dividend, divisor), k.rechenKontext), annahmen(k) + definiert),
                "divisorNull" to BedingterWert(nullFall, annahmen(k)),
            ))
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
        registriere("mathematik.element") { k ->
            KnotenAuswertungsErgebnis(mapOf("aussage" to BedingterWert(ElementBeziehung(k.objekt("links"), k.menge("rechts")), annahmen(k))))
        }
        registriere("mathematik.kleiner") { k -> vergleich(k, VergleichsArt.Kleiner) }
        registriere("mathematik.größer") { k -> vergleich(k, VergleichsArt.Größer) }
        registriere("mathematik.kleinerGleich") { k -> vergleich(k, VergleichsArt.KleinerGleich) }
        registriere("mathematik.größerGleich") { k -> vergleich(k, VergleichsArt.GrößerGleich) }
        registriere("mathematik.teilmenge") { k -> mengenAussage(k) { a, b -> EchteTeilmengeBeziehung(a, b) } }
        registriere("mathematik.übermenge") { k -> mengenAussage(k) { a, b -> ObermengenBeziehung(a, b, echt = true) } }
        registriere("mathematik.teilOderGleichmenge") { k -> mengenAussage(k) { a, b -> TeilmengenBeziehung(a, b) } }
        registriere("mathematik.überOderGleichmenge") { k -> mengenAussage(k) { a, b -> ObermengenBeziehung(a, b) } }
        registriere("mathematik.disjunkt") { k -> mengenAussage(k) { a, b -> Disjunktheit(a, b) } }
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
        registriere("mathematik.wurzel") { k ->
            KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(wurzel(k.zahl("radikand"), k.rechenKontext), annahmen(k))))
        }
        registriere("mathematik.logarithmus") { k ->
            KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(Logarithmus(k.zahl("basis"), k.zahl("argument")), annahmen(k))))
        }
        registriere("mathematik.tupel") { k ->
            val werte = k.operatorEingänge { _, index -> Variable("tupel_$index") }.map { it.objekt as? ZahlAusdruck ?: error("Tupel benötigt Zahlen.") }
            KnotenAuswertungsErgebnis(mapOf("tupel" to BedingterWert(Tupel(werte), annahmen(k))))
        }
        registriere("mathematik.komplexAusTupel") { k ->
            val tupel = k.eingänge["tupel"]?.objekt as? Tupel ?: error("Zahlentupel fehlt.")
            val zahl = if (k.knoten.parameter["modus"] == "polar") komplexAusPolar(tupel) else komplexAusKartesisch(tupel)
            KnotenAuswertungsErgebnis(mapOf("zahl" to BedingterWert(zahl, annahmen(k))))
        }
        registriere("mathematik.konjugierte") { k -> KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(k.komplex("zahl").let(::konjugiere), annahmen(k)))) }
        registriere("mathematik.realteil") { k -> KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(k.komplex("zahl").realteil, annahmen(k)))) }
        registriere("mathematik.imaginärteil") { k -> KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(k.komplex("zahl").imaginärteil, annahmen(k)))) }
        registriere("mathematik.komplexerRadius") { k -> KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(komplexerBetrag(k.komplex("zahl")), annahmen(k)))) }
        registriere("mathematik.winkel") { k -> KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(Argument(k.komplex("zahl")), annahmen(k)))) }
        registriere("mathematik.endlicheMenge") { k ->
            val elemente = (k.knoten.parameter["elemente"] ?: "").split(',').filter { it.isNotBlank() }.map { RationaleZahl.parse(it) }.toSet()
            KnotenAuswertungsErgebnis(mapOf("menge" to BedingterWert(EndlicheMenge(elemente))))
        }
        registriere("mathematik.vereinigung") { k ->
            val mengen = k.operatorEingänge { anschluss, index ->
                BenannteMenge(unbekannteKennung(k.knoten, anschluss), unbekanntesOperatorLatex(k.knoten, index))
            }.map { it.objekt as? MengenAusdruck ?: error("Mengeneingang ${it} ist ungültig.") }
            require(mengen.size >= 2) { "Mindestens zwei Mengen müssen verbunden sein." }
            val wert = vereinige(mengen)
            KnotenAuswertungsErgebnis(mapOf("menge" to BedingterWert(wert, annahmen(k))))
        }
        registriere("mathematik.schnitt") { k ->
            val mengen = k.mengenOperatorEingänge()
            require(mengen.size >= 2) { "Mindestens zwei Mengen müssen verbunden sein." }
            KnotenAuswertungsErgebnis(mapOf("menge" to BedingterWert(schneide(mengen), annahmen(k))))
        }
        registriere("mathematik.differenz") { k ->
            KnotenAuswertungsErgebnis(mapOf("menge" to BedingterWert(mengenDifferenz(k.menge("links"), k.menge("rechts")), annahmen(k))))
        }
        registriere("mathematik.kartesischesProdukt") { k ->
            val mengen = k.mengenOperatorEingänge()
            require(mengen.size >= 2) { "Mindestens zwei Mengen müssen verbunden sein." }
            KnotenAuswertungsErgebnis(mapOf("menge" to BedingterWert(kartesischesProdukt(mengen), annahmen(k))))
        }
        registriere("mathematik.natürlicheZahlen") { KnotenAuswertungsErgebnis(mapOf("menge" to BedingterWert(NatürlicheZahlen))) }
        registriere("mathematik.ganzeZahlen") { KnotenAuswertungsErgebnis(mapOf("menge" to BedingterWert(GanzeZahlen))) }
        registriere("mathematik.rationaleZahlen") { KnotenAuswertungsErgebnis(mapOf("menge" to BedingterWert(RationaleZahlen))) }
        registriere("mathematik.reelleZahlen") { KnotenAuswertungsErgebnis(mapOf("menge" to BedingterWert(ReelleZahlen))) }
        registriere("mathematik.iterierteSumme") { k ->
            val methode = k.eingänge["methode"]?.objekt as? Funktion ?: error("Zahlfunktion fehlt.")
            val indexMenge = k.eingänge["indexmenge"]?.objekt as? MengenAusdruck ?: error("Indexmenge fehlt.")
            KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(iterierteSumme(methode, indexMenge), annahmen(k))))
        }
        registriere("mathematik.iteriertesProdukt") { k ->
            val methode = k.eingänge["methode"]?.objekt as? Funktion ?: error("Zahlfunktion fehlt.")
            val indexMenge = k.eingänge["indexmenge"]?.objekt as? MengenAusdruck ?: error("Indexmenge fehlt.")
            KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(iteriertesProdukt(methode, indexMenge), annahmen(k))))
        }
        registriere("mathematik.iterierteVereinigung") { k ->
            val methode = k.eingänge["methode"]?.objekt as? Funktion ?: error("Mengenfunktion fehlt.")
            val indexMenge = k.eingänge["indexmenge"]?.objekt as? MengenAusdruck ?: error("Indexmenge fehlt.")
            KnotenAuswertungsErgebnis(mapOf("menge" to BedingterWert(iterierteVereinigung(methode, indexMenge), annahmen(k))))
        }
        registriere("mathematik.iterierterSchnitt") { k ->
            val methode = k.eingänge["methode"]?.objekt as? Funktion ?: error("Mengenfunktion fehlt.")
            val indexMenge = k.eingänge["indexmenge"]?.objekt as? MengenAusdruck ?: error("Indexmenge fehlt.")
            KnotenAuswertungsErgebnis(mapOf("menge" to BedingterWert(iterierterSchnitt(methode, indexMenge), annahmen(k))))
        }
        registriere("mathematik.iteriertesKartesischesProdukt") { k ->
            val methode = k.eingänge["methode"]?.objekt as? Funktion ?: error("Mengenfunktion fehlt.")
            val indexMenge = k.eingänge["indexmenge"]?.objekt as? MengenAusdruck ?: error("Indexmenge fehlt.")
            KnotenAuswertungsErgebnis(mapOf("menge" to BedingterWert(iteriertesKartesischesProdukt(methode, indexMenge), annahmen(k))))
        }
        registriere("mathematik.abbild") { k ->
            val methode = k.eingänge["methode"]?.objekt as? Funktion ?: error("Methode fehlt.")
            KnotenAuswertungsErgebnis(mapOf("menge" to BedingterWert(bildeAb(k.menge("menge"), methode), annahmen(k))))
        }
        registriere("mathematik.termZuMethode") { k ->
            val term = k.zahl("term")
            val argumente = k.knoten.anschlüsse.filter { it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Eingang && it.name !in setOf("term", "zielmenge") }
                .sortedBy { it.reihenfolge }
                .map { anschluss -> k.eingänge[anschluss.name] ?: error("Das Methodenargument ${anschluss.name} fehlt.") }
            val variablen = argumente.map { it.objekt as? Variable ?: error("Methodenargumente müssen Variablen sein.") }
            require(variablen.map { it.name }.distinct().size == variablen.size) { "Methodenargumente müssen unterschiedliche Variablen sein." }
            val zielmenge = k.menge("zielmenge")
            val werteVorräte = argumente.mapNotNull { wert -> (wert.objekt as? Variable)?.let { variable -> wert.werteVorrat?.let { variable.name to it } } }.toMap()
            val funktion = Funktion(k.knoten.parameter["name"] ?: "f", variablen, mapOf("wert" to term), mapOf("wert" to zielmenge), werteVorräte)
            KnotenAuswertungsErgebnis(mapOf("methode" to BedingterWert(funktion, annahmen(k))))
        }
        registriere("mathematik.komposition") { k ->
            val außen = k.eingänge["außen"]?.objekt as? Funktion ?: error("Äußere Methode fehlt.")
            val innen = k.eingänge["innen"]?.objekt as? Funktion ?: error("Innere Methode fehlt.")
            KnotenAuswertungsErgebnis(mapOf("methode" to BedingterWert(komponiere(außen, innen), annahmen(k))))
        }
        registriere("mathematik.iteration") { k ->
            val methode = k.eingänge["methode"]?.objekt as? Funktion ?: error("Methode fehlt.")
            val exponent = vereinfache(k.zahl("exponent"), k.rechenKontext) as? RationaleZahl ?: error("Iterationsexponent muss ganzzahlig sein.")
            require(exponent.nenner == java.math.BigInteger.ONE && exponent.zähler.signum() >= 0 && exponent.zähler.bitLength() < 31) { "Iterationsexponent muss eine nichtnegative ganze Zahl sein." }
            KnotenAuswertungsErgebnis(mapOf("methode" to BedingterWert(iteriere(methode, exponent.zähler.toInt()), annahmen(k))))
        }
        registriere("mathematik.methodenDifferentieren") { k ->
            val methode = k.eingänge["methode"]?.objekt as? Funktion ?: error("Methode fehlt.")
            KnotenAuswertungsErgebnis(mapOf("methode" to BedingterWert(differenziereMethode(methode), annahmen(k))))
        }
        registriere("mathematik.methodenIntegrieren") { k ->
            val methode = k.eingänge["methode"]?.objekt as? Funktion ?: error("Methode fehlt.")
            KnotenAuswertungsErgebnis(mapOf("methode" to BedingterWert(integriereMethode(methode), annahmen(k))))
        }
        registriere("mathematik.spaltenMethodeDifferentieren") { k -> methodeAnalysis(k, ::differenziereMethode) }
        registriere("mathematik.zeilenMethodeDifferentieren") { k -> methodeAnalysis(k, ::differenziereMethode) }
        registriere("mathematik.spaltenMethodeIntegrieren") { k -> methodeAnalysis(k, ::integriereMethode) }
        registriere("mathematik.zeilenMethodeIntegrieren") { k -> methodeAnalysis(k, ::integriereMethode) }
        registriere("mathematik.vektor") { k ->
            KnotenAuswertungsErgebnis(mapOf("vektor" to BedingterWert(SpaltenVektor(k.zahlenOperatorEingänge()), annahmen(k))))
        }
        registriere("mathematik.zeilenVektor") { k ->
            KnotenAuswertungsErgebnis(mapOf("vektor" to BedingterWert(ZeilenVektor(k.zahlenOperatorEingänge()), annahmen(k))))
        }
        registriere("mathematik.tupelZuSpalte") { k ->
            val tupel = k.eingänge["tupel"]?.objekt as? Tupel ?: error("Tupel fehlt.")
            KnotenAuswertungsErgebnis(mapOf("vektor" to BedingterWert(SpaltenVektor(tupel.zahlen()), annahmen(k))))
        }
        registriere("mathematik.tupelZuZeile") { k ->
            val tupel = k.eingänge["tupel"]?.objekt as? Tupel ?: error("Tupel fehlt.")
            KnotenAuswertungsErgebnis(mapOf("vektor" to BedingterWert(ZeilenVektor(tupel.zahlen()), annahmen(k))))
        }
        registriere("mathematik.einheitsSpalte") { k -> KnotenAuswertungsErgebnis(mapOf("vektor" to BedingterWert(einheitsSpaltenVektor(k.parameterInt("dimension"), k.parameterInt("index"))))) }
        registriere("mathematik.einheitsZeile") { k -> KnotenAuswertungsErgebnis(mapOf("vektor" to BedingterWert(einheitsZeilenVektor(k.parameterInt("dimension"), k.parameterInt("index"))))) }
        registriere("mathematik.vektorRadiusSpalte") { k -> KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(k.spalte("vektor").radius(), annahmen(k)))) }
        registriere("mathematik.vektorRadiusZeile") { k -> KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(k.zeile("vektor").radius(), annahmen(k)))) }
        registriere("mathematik.matrix") { k ->
            val zeilen = k.operatorEingänge { _, _ -> error("Matrixzeile fehlt.") }.map { it.objekt as? ZeilenVektor ?: error("Matrix benötigt Zeilenvektoren.") }.map { it.werte }
            KnotenAuswertungsErgebnis(mapOf("matrix" to BedingterWert(Matrix(zeilen), annahmen(k))))
        }
        registriere("mathematik.skalarprodukt") { k ->
            val a = k.spalte("a"); val b = k.spalte("b")
            KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(vereinfache(a.skalarprodukt(b)), annahmen(k))))
        }
        registriere("mathematik.skalarproduktZeile") { k -> KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(vereinfache(k.zeile("a").skalarprodukt(k.zeile("b"))), annahmen(k)))) }
        registriere("mathematik.kreuzproduktSpalte") { k -> KnotenAuswertungsErgebnis(mapOf("vektor" to BedingterWert(kreuzprodukt(k.spalte("a"), k.spalte("b")), annahmen(k)))) }
        registriere("mathematik.kreuzproduktZeile") { k -> KnotenAuswertungsErgebnis(mapOf("vektor" to BedingterWert(kreuzprodukt(k.zeile("a"), k.zeile("b")), annahmen(k)))) }
        registriere("mathematik.transponiereSpalte") { k -> KnotenAuswertungsErgebnis(mapOf("vektor" to BedingterWert(k.spalte("vektor").transponiert(), annahmen(k)))) }
        registriere("mathematik.transponiereZeile") { k -> KnotenAuswertungsErgebnis(mapOf("vektor" to BedingterWert(k.zeile("vektor").transponiert(), annahmen(k)))) }
        registriere("mathematik.matrixProdukt") { k -> KnotenAuswertungsErgebnis(mapOf("matrix" to BedingterWert(k.matrix("a") * k.matrix("b"), annahmen(k)))) }
        registriere("mathematik.transponiereMatrix") { k -> KnotenAuswertungsErgebnis(mapOf("matrix" to BedingterWert(k.matrix("matrix").transponiert(), annahmen(k)))) }
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
            val zielMenge = k.eingänge["zielmenge"]?.objekt as? MengenAusdruck
            KnotenAuswertungsErgebnis(mapOf("wert" to wert.copy(zielMenge = zielMenge)))
        }
        registriere("mathematik.fall") { k ->
            val wert = k.eingänge["term"] ?: error("Term fehlt.")
            val bedingung = k.eingänge["aussage"]?.objekt as? Aussage ?: error("Aussage fehlt.")
            KnotenAuswertungsErgebnis(mapOf(
                "fall" to wert.copy(annahmen = wert.annahmen + bedingung),
                "sonst" to wert.copy(annahmen = wert.annahmen + Negation(bedingung)),
            ))
        }
    }

    private fun KnotenAuswertungsKontext.zahl(name: String) = eingänge[name]?.objekt as? ZahlAusdruck ?: error("Zahleingang $name fehlt.")
    private fun KnotenAuswertungsKontext.objekt(name: String) = eingänge[name]?.objekt ?: error("Eingang $name fehlt.")
    private fun KnotenAuswertungsKontext.menge(name: String) = eingänge[name]?.objekt as? MengenAusdruck ?: error("Mengeneingang $name fehlt.")
    private fun KnotenAuswertungsKontext.komplex(name: String) = eingänge[name]?.objekt as? KomplexeZahl ?: error("Komplexe Zahl $name fehlt.")
    private fun KnotenAuswertungsKontext.spalte(name: String) = eingänge[name]?.objekt as? SpaltenVektor ?: error("Spaltenvektor $name fehlt.")
    private fun KnotenAuswertungsKontext.zeile(name: String) = eingänge[name]?.objekt as? ZeilenVektor ?: error("Zeilenvektor $name fehlt.")
    private fun KnotenAuswertungsKontext.matrix(name: String) = eingänge[name]?.objekt as? Matrix ?: error("Matrix $name fehlt.")
    private fun KnotenAuswertungsKontext.parameterInt(name: String) = knoten.parameter[name]?.toIntOrNull()?.takeIf { it > 0 } ?: error("Parameter $name muss eine positive ganze Zahl sein.")
    private fun annahmen(k: KnotenAuswertungsKontext) = k.eingänge.values.flatMap { it.annahmen }.toSet()
    private fun parseZahlen(text: String) = text.split(',').filter { it.isNotBlank() }.map { RationaleZahl.parse(it.trim()) }
    private fun vergleich(k: KnotenAuswertungsKontext, art: VergleichsArt) = KnotenAuswertungsErgebnis(
        mapOf("aussage" to BedingterWert(Vergleich(k.zahl("links"), art, k.zahl("rechts")), annahmen(k))),
    )
    private fun mengenAussage(k: KnotenAuswertungsKontext, erzeuge: (MengenAusdruck, MengenAusdruck) -> Aussage) = KnotenAuswertungsErgebnis(
        mapOf("aussage" to BedingterWert(erzeuge(k.menge("links"), k.menge("rechts")), annahmen(k))),
    )
    private fun methodeAnalysis(k: KnotenAuswertungsKontext, operation: (Funktion) -> Funktion): KnotenAuswertungsErgebnis {
        val methode = k.eingänge["methode"]?.objekt as? Funktion ?: error("Methode fehlt.")
        return KnotenAuswertungsErgebnis(mapOf("methode" to BedingterWert(operation(methode), annahmen(k))))
    }
}

/** Formeln für assoziative Operatoren verwenden für fehlende Eingänge stabile, eindeutige Unbekannte. */
internal fun KnotenAuswertungsKontext.operatorEingänge(
    unbekannt: (de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten, Int) -> MathematischesObjekt,
): List<BedingterWert> = knoten.anschlüsse
    .filter { it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Eingang }
    .sortedBy { it.reihenfolge }
    .mapIndexed { index, anschluss -> eingänge[anschluss.name] ?: BedingterWert(unbekannt(anschluss, index + 1)) }

private fun KnotenAuswertungsKontext.mengenOperatorEingänge(): List<MengenAusdruck> = operatorEingänge { anschluss, index ->
    BenannteMenge(unbekannteKennung(knoten, anschluss), unbekanntesOperatorLatex(knoten, index))
}.map { it.objekt as? MengenAusdruck ?: error("Mengeneingang ist ungültig.") }

private fun KnotenAuswertungsKontext.zahlenOperatorEingänge(): List<ZahlAusdruck> = operatorEingänge { _, index -> Variable("vektor_$index") }
    .map { it.objekt as? ZahlAusdruck ?: error("Vektor benötigt Zahlen.") }
private fun Tupel.zahlen() = elemente.map { it as? ZahlAusdruck ?: error("Tupel benötigt Zahlen.") }

internal fun eingabeLatex(index: Int) = "\\mathrm{eingabe}_{${index}}"

internal fun unbekanntesOperatorLatex(
    knoten: de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten,
    index: Int,
) = "\\mathrm{${knoten.name}}_{${eingabeLatex(index)}}"

private fun unbekannteKennung(
    knoten: de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten,
    anschluss: de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten,
) = "unbekannt_${knoten.id.wert}_${anschluss.id.wert}"

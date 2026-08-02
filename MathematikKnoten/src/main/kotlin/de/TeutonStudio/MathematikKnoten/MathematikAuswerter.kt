package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

object StandardMathematikAuswerter {
    fun erzeugeRegister() = MathematikAuswerterRegister().apply {
        registriere("mathematik.zahl") { k ->
            KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(RationaleZahl.parse(k.knoten.parameter["wert"] ?: "0"))))
        }
        registriere("mathematik.variable") { k ->
            val name = k.knoten.parameter["name"]?.trim().orEmpty().ifBlank { "x" }
            val wertevorrat = grundmenge(k.knoten.parameter["werteVorrat"] ?: "R")
            KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(
                Variable(name),
                werteVorrat = wertevorrat,
                variablenQuellen = listOf(VariablenQuelle(k.knoten.id, name, wertevorrat)),
            )))
        }
        registriere("mathematik.allgemeinerParameter") { k ->
            val name = k.knoten.parameter["name"]?.trim().orEmpty().ifBlank { "a" }
            val wertevorrat = WertebereichKonfiguration.vonEigenschaft(
                k.knoten.eigenschaften[WertebereichKonfiguration.EIGENSCHAFT],
            ).zuMenge()
            KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(
                AllgemeinerParameter(name),
                werteVorrat = wertevorrat,
                variablenQuellen = listOf(VariablenQuelle(k.knoten.id, name, wertevorrat)),
            )))
        }
        registriere("mathematik.addition") { k ->
            val werte = k.operatorEingänge { anschluss, index ->
                Variable(unbekannteKennung(k.knoten, anschluss), unbekanntesOperatorLatex(k.knoten, index))
            }.map { it.objekt as? ZahlAusdruck ?: error("Zahleingang ${it} ist ungültig.") }
            require(werte.size >= 2) { "Mindestens zwei Summanden müssen verbunden sein." }
            KnotenAuswertungsErgebnis(mapOf("wert" to reellerZahlwert(addition(werte), k)))
        }
        registriere("mathematik.multiplikation") { k ->
            val werte = listOf("a", "b", "c").mapNotNull { k.eingänge[it]?.objekt as? ZahlAusdruck }
            require(werte.size >= 2) { "Mindestens zwei Faktoren müssen verbunden sein." }
            KnotenAuswertungsErgebnis(mapOf("wert" to reellerZahlwert(multiplikation(werte), k)))
        }
        registriere("mathematik.potenz") { k ->
            val basis = k.zahl("basis"); val exponent = k.zahl("exponent")
            KnotenAuswertungsErgebnis(mapOf("wert" to reellerZahlwert(Potenz(basis, exponent), k)))
        }
        registriere("mathematik.extremwert") { k ->
            val werte = k.operatorEingänge { anschluss, index ->
                Variable(unbekannteKennung(k.knoten, anschluss), unbekanntesOperatorLatex(k.knoten, index))
            }
            require(werte.size >= 2) { "Ein Extremwert benötigt mindestens zwei Eingänge." }
            require(werte.all { it.istNachweisbarReell() }) { "Maximum und Minimum sind nur für nachweisbar reelle Zahlen definiert." }
            val zahlen = werte.map { it.objekt as? ZahlAusdruck ?: error("Extremwert benötigt Zahlen.") }
            val wert = when (k.knoten.parameter["modus"]) {
                "maximum" -> maximum(zahlen)
                "minimum" -> minimum(zahlen)
                else -> error("Extremwertmodus muss 'maximum' oder 'minimum' sein.")
            }
            KnotenAuswertungsErgebnis(mapOf("wert" to reellerZahlwert(wert, k)))
        }
        registriere("mathematik.gleichheit") { k ->
            val links = k.eingänge["links"]?.objekt ?: error("Linke Seite fehlt.")
            val rechts = k.eingänge["rechts"]?.objekt ?: error("Rechte Seite fehlt.")
            KnotenAuswertungsErgebnis(mapOf("aussage" to BedingterWert(Gleichheit(links, rechts), annahmen(k))))
        }
        registriere("mathematik.wahr") { KnotenAuswertungsErgebnis(mapOf("aussage" to BedingterWert(WahrheitsKonstante(true)))) }
        registriere("mathematik.lüge") { KnotenAuswertungsErgebnis(mapOf("aussage" to BedingterWert(WahrheitsKonstante(false)))) }
        registriere("mathematik.element") { k ->
            KnotenAuswertungsErgebnis(mapOf("aussage" to BedingterWert(ElementBeziehung(k.objekt("links"), k.menge("rechts")), annahmen(k))))
        }
        registriere(MathematikKnotenVorlagen.ORDNUNGSRELATION_ART) { k ->
            vergleich(k, vergleichsArt(k.knoten.parameter["relation"]))
        }
        // Alte Schlüssel bleiben für importierte Karten bis zur Lade-Migration auswertbar.
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
                    KnotenAuswertungsErgebnis(mapOf("wert" to reellerZahlwert(e.ergebnis, k)), e.schritte)
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
            KnotenAuswertungsErgebnis(mapOf("wert" to reellerZahlwert(e.ergebnis, k)), e.schritte)
        }
        registriere("mathematik.integrieren") { k ->
            val e = integrieren(k.zahl("term"), Variable(k.knoten.parameter["variable"] ?: "x"))
            KnotenAuswertungsErgebnis(mapOf("wert" to reellerZahlwert(e.ergebnis, k)), e.schritte)
        }
        registriere("mathematik.wurzel") { k ->
            KnotenAuswertungsErgebnis(mapOf("wert" to reellerZahlwert(wurzel(k.zahl("radikand"), k.rechenKontext), k)))
        }
        registriere("mathematik.logarithmus") { k ->
            KnotenAuswertungsErgebnis(mapOf("wert" to reellerZahlwert(Logarithmus(k.zahl("basis"), k.zahl("argument")), k)))
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
        registriere("mathematik.endlicheMenge") { k -> EndlicheMengeAuswerter.auswerten(k) }
        registriere("mathematik.einzelmenge") { k ->
            val element = k.eingänge["element"] ?: error("Element fehlt.")
            KnotenAuswertungsErgebnis(mapOf("menge" to BedingterWert(
                objekt = EndlicheMenge(setOf(element.objekt)),
                annahmen = element.annahmen,
                reelleVariablen = element.reelleVariablen,
                variablenQuellen = element.variablenQuellen,
            )))
        }
        registriere("mathematik.mengenfilter") { k ->
            val methode = k.eingänge["methode"]?.objekt as? Funktion ?: error("Filtermethode fehlt.")
            val gefiltert = filtereMenge(k.menge("menge"), methode, k.rechenKontext)
            KnotenAuswertungsErgebnis(mapOf("menge" to BedingterWert(
                objekt = gefiltert,
                annahmen = annahmen(k),
                reelleVariablen = reelleVariablen(k.eingänge.values),
            )))
        }
        registriere("mathematik.reellesIntervall") { k ->
            val untereGrenze = k.zahl("untereGrenze")
            val obereGrenze = k.zahl("obereGrenze")
            require(k.eingänge.getValue("untereGrenze").istNachweisbarReell() && k.eingänge.getValue("obereGrenze").istNachweisbarReell()) {
                "Ein reelles Intervall benötigt zwei nachweisbar reelle Grenzen."
            }
            KnotenAuswertungsErgebnis(mapOf("menge" to BedingterWert(
                reellesIntervall(untereGrenze, obereGrenze, k.rechenKontext),
                annahmen(k),
                reelleVariablen = reelleVariablen(k.eingänge.values),
            )))
        }
        registriere("mathematik.lösungsmenge") { k ->
            val bedingung = k.aussage("bedingung")
            val freie = bedingung.freieVariablen().associateBy { it.name }
            val automatisch = k.knoten.parameter["automatisch"]?.toBooleanStrictOrNull() ?: true
            val namen = if (automatisch) freie.keys.sorted() else k.knoten.parameter["variablen"].orEmpty()
                .split(',').map(String::trim).filter(String::isNotBlank)
            require(namen.isNotEmpty()) { "Für die Lösungsmenge muss mindestens eine Variable angegeben werden." }
            require(namen.distinct().size == namen.size) { "Die Variablen der Lösungsmenge dürfen nicht doppelt vorkommen." }
            val unbekannt = namen.filterNot { it in freie }
            require(unbekannt.isEmpty()) { "Unbekannte Variablen in der Lösungsmenge: ${unbekannt.joinToString(", ")}." }
            val grundmengen = k.knoten.parameter["grundmengen"].orEmpty().split(',').map(String::trim).filter(String::isNotBlank)
            require(grundmengen.isEmpty() || grundmengen.size == 1 || grundmengen.size == namen.size) {
                "Es wird eine Grundmenge oder genau eine Grundmenge je Variable benötigt."
            }
            val mengen = if (grundmengen.isEmpty()) List(namen.size) { ReelleZahlen } else if (grundmengen.size == 1) List(namen.size) { grundmenge(grundmengen.single()) } else grundmengen.map(::grundmenge)
            val variablen = namen.mapIndexed { index, name -> GebundeneMengenVariable(freie.getValue(name), mengen[index]) }
            KnotenAuswertungsErgebnis(mapOf("menge" to BedingterWert(DefinierteMenge(variablen, bedingung), annahmen(k))))
        }
        registriere("mathematik.visualisierung") { k ->
            val menge = k.eingänge["menge"] ?: error("Für die Visualisierung muss eine Menge verbunden sein.")
            require(menge.objekt is MengenAusdruck) { "Der Visualisierungseingang enthält keine Menge." }
            KnotenAuswertungsErgebnis(mapOf("menge" to menge))
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
        registriere("mathematik.komplexeZahlen") { KnotenAuswertungsErgebnis(mapOf("menge" to BedingterWert(KomplexeZahlen))) }
        registriere("mathematik.mächtigkeit") { k -> KnotenAuswertungsErgebnis(mapOf("mächtigkeit" to BedingterWert(mächtigkeit(k.menge("menge")), annahmen(k)))) }
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
        registriere(MathematikKnotenVorlagen.ITERIERTE_AUSSAGENVERKNÜPFUNG_ART) { k ->
            val methode = k.eingänge["methode"]?.objekt as? Funktion ?: error("Aussagenmethode fehlt.")
            val indexMenge = k.eingänge["indexmenge"]?.objekt as? MengenAusdruck ?: error("Indexmenge fehlt.")
            val aussage = when (k.knoten.parameter["operator"]) {
                "konjunktion" -> iterierteKonjunktion(methode, indexMenge)
                "disjunktion" -> iterierteDisjunktion(methode, indexMenge)
                "adjunktion" -> iterierteAdjunktion(methode, indexMenge)
                else -> error("Unbekannte iterierte Aussagenverknüpfung.")
            }
            KnotenAuswertungsErgebnis(mapOf("aussage" to BedingterWert(aussage, annahmen(k))))
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
            val termWert = k.eingänge["term"] ?: error("Term fehlt.")
            val term = termWert.objekt
            val freieParameter = term.freieFunktionsParameter().associateBy { it.name }
            val quellenNachName = termWert.variablenQuellen
                .filter { it.name in freieParameter }
                .groupBy { it.name }
            val fehlende = freieParameter.keys.filterNot { it in quellenNachName }
            require(fehlende.isEmpty()) { "Für die Parameter ${fehlende.joinToString(", ")} fehlt ein verbundener Parameter- oder Karten-Eingang." }
            val werteVorräteNachName = quellenNachName.mapValues { (name, quellen) ->
                val mengen = quellen.map { it.werteVorrat }.distinct()
                require(mengen.size == 1) { "Die Variable '$name' besitzt widersprüchliche Wertevorräte." }
                mengen.single()
            }
            val methodenQuellen = quellenNachName.filterValues { quellen -> quellen.any { it.alsMethodenParameter } }
            val automatisch = methodenQuellen.entries.sortedWith(
                compareBy<Map.Entry<String, List<VariablenQuelle>>> { entry ->
                    entry.value.minOf { quelle -> k.topologischeReihenfolge[quelle.knotenId] ?: Int.MAX_VALUE }
                }.thenBy { entry -> entry.value.minOf { quelle -> quelle.knotenId.wert } },
            ).map { it.key }
            val gespeichert = k.knoten.parameter["argumentReihenfolge"].orEmpty()
                .split(',').map(String::trim).filter { it.isNotBlank() && it in methodenQuellen }.distinct()
            val namen = gespeichert + automatisch.filterNot { it in gespeichert }
            val parameter = namen.map { freieParameter.getValue(it) }
            val werteVorräte = namen.associateWith { werteVorräteNachName.getValue(it) }
            val zielmenge = inferiereZielmenge(term, werteVorräteNachName, termWert.annahmen)
            val funktion = Funktion(k.knoten.parameter["name"] ?: "f", parameter, mapOf("wert" to term), mapOf("wert" to zielmenge), werteVorräte)
            KnotenAuswertungsErgebnis(mapOf("methode" to BedingterWert(funktion, annahmen(k))))
        }
        registriere("mathematik.komposition") { k ->
            val anschlüsse = k.knoten.anschlüsse
                .filter { it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Eingang }
                .sortedBy { it.reihenfolge }
            require(anschlüsse.size >= 2) { "Eine Komposition benötigt mindestens zwei Methodeneingänge." }
            val methoden = anschlüsse.mapIndexed { index, anschluss ->
                k.eingänge[anschluss.name]?.objekt as? Funktion
                    ?: error("Methodeneingang ${index + 1} ist nicht verbunden oder enthält keine Methode.")
            }
            KnotenAuswertungsErgebnis(
                mapOf("methode" to BedingterWert(komponiere(methoden), annahmen(k))),
            )
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
        registriere("mathematik.vektorZuPolynom") { k ->
            val vektor = k.eingänge["vektor"]?.objekt as? OrientierterVektor ?: error("Vektoreingang fehlt.")
            val variablenName = (k.knoten.parameter["variable"] ?: "x").trim()
            require(variablenName.isNotEmpty()) { "Die Polynomvariable darf nicht leer sein." }
            KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(polynomAusKoeffizienten(vektor.werte, Variable(variablenName)), annahmen(k))))
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
            val höhe = k.parameterInt("höhe")
            val breite = k.parameterInt("breite")
            val matrix = if (k.knoten.parameter["erzeugungsArt"] == MATRIX_METHODE) {
                val methode = k.eingänge["methode"]?.objekt as? Funktion ?: error("Matrixmethode fehlt.")
                matrixAusMethode(methode, höhe, breite)
            } else {
                Matrix(List(höhe) { zeile -> List(breite) { spalte ->
                    k.eingänge[matrixEintragName(zeile, spalte)]?.objekt as? ZahlAusdruck
                        ?: error("Matrixeintrag ($zeile,$spalte) fehlt oder ist keine Zahl.")
                } })
            }
            KnotenAuswertungsErgebnis(mapOf("matrix" to BedingterWert(matrix, annahmen(k))))
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
            val name = k.knoten.parameter["name"]?.trim().orEmpty().ifBlank { "x" }
            val ausgangsArt = k.knoten.anschlüsse.firstOrNull {
                it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Ausgang
            }?.art ?: MathematikAnschlussArten.Objekt.id
            val parameter: FunktionsParameter = if (ausgangsArt == MathematikAnschlussArten.Zahl.id) Variable(name) else AllgemeinerParameter(name)
            val werteVorrat: MengenAusdruck = when (ausgangsArt) {
                MathematikAnschlussArten.Zahl.id -> ReelleZahlen
                MathematikAnschlussArten.Aussage.id -> EndlicheMenge(setOf(WahrheitsKonstante(true), WahrheitsKonstante(false)))
                MathematikAnschlussArten.Menge.id -> BenannteMenge("mengen_$name", "\\mathcal{P}(\\mathcal{U})")
                else -> BenannteMenge("werte_$name", "\\mathcal{W}_{${name}}")
            }
            KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(
                objekt = parameter,
                werteVorrat = werteVorrat,
                reelleVariablen = if (parameter is Variable) mapOf(name to werteVorrat) else emptyMap(),
                variablenQuellen = listOf(VariablenQuelle(k.knoten.id, name, werteVorrat, alsMethodenParameter = false)),
            )))
        }
        registriere("mathematik.kartenAusgang") { k ->
            val wert = k.eingänge["wert"] ?: error("Ausgabewert fehlt.")
            KnotenAuswertungsErgebnis(mapOf("wert" to wert))
        }
        registriere("mathematik.fall") { k ->
            val wahr = k.eingänge["wahr"] ?: error("Wahr-Eingang fehlt.")
            val lüge = k.eingänge["lüge"] ?: error("Lüge-Eingang fehlt.")
            val aussageWert = k.eingänge["aussage"] ?: error("Aussage-Eingang fehlt.")
            val aussage = aussageWert.objekt as? Aussage ?: error("Der Aussage-Eingang enthält keine Aussage.")
            val eingangsWerte = listOf(wahr, aussageWert, lüge)
            val gemeinsameAnnahmen = eingangsWerte.flatMap { it.annahmen }.toSet()
            val gemeinsameReelleVariablen = reelleVariablen(eingangsWerte)
            val gemeinsameQuellen = eingangsWerte.flatMap { it.variablenQuellen }
                .distinctBy { quelle -> Pair(Triple(quelle.knotenId, quelle.name, quelle.werteVorrat), quelle.alsMethodenParameter) }
            val kontext = k.rechenKontext.copy(annahmen = k.rechenKontext.annahmen + gemeinsameAnnahmen)
            val basis = when (aussage.entscheide(kontext).wahrheitswert) {
                Wahrheitswert.Wahr -> wahr
                Wahrheitswert.Lüge -> lüge
                null -> BedingterWert(FallAusdruck(wahr.objekt, aussage, lüge.objekt))
            }
            KnotenAuswertungsErgebnis(mapOf(
                "wert" to basis.copy(
                    annahmen = gemeinsameAnnahmen,
                    reelleVariablen = gemeinsameReelleVariablen,
                    variablenQuellen = gemeinsameQuellen,
                ),
            ))
        }
        registriere("mathematik.konjunktion") { k -> KnotenAuswertungsErgebnis(mapOf("aussage" to BedingterWert(Konjunktion(k.aussagenOperatorEingänge()), annahmen(k)))) }
        registriere("mathematik.disjunktion") { k -> KnotenAuswertungsErgebnis(mapOf("aussage" to BedingterWert(Disjunktion(k.aussagenOperatorEingänge()), annahmen(k)))) }
        registriere("mathematik.implikation") { k -> KnotenAuswertungsErgebnis(mapOf("aussage" to BedingterWert(Implikation(k.aussage("a"), k.aussage("b")), annahmen(k)))) }
        registriere("mathematik.äquivalenz") { k -> KnotenAuswertungsErgebnis(mapOf("aussage" to BedingterWert(Äquivalenz(k.aussage("a"), k.aussage("b")), annahmen(k)))) }
        registriere("mathematik.adjunktion") { k -> KnotenAuswertungsErgebnis(mapOf("aussage" to BedingterWert(Adjunktion(k.aussage("a"), k.aussage("b")), annahmen(k)))) }
        registriereDivisionUndKehrwert()
    }

    private fun KnotenAuswertungsKontext.zahl(name: String) = eingänge[name]?.objekt as? ZahlAusdruck ?: error("Zahleingang $name fehlt.")
    private fun KnotenAuswertungsKontext.objekt(name: String) = eingänge[name]?.objekt ?: error("Eingang $name fehlt.")
    private fun KnotenAuswertungsKontext.menge(name: String) = eingänge[name]?.objekt as? MengenAusdruck ?: error("Mengeneingang $name fehlt.")
    private fun KnotenAuswertungsKontext.komplex(name: String) = eingänge[name]?.objekt as? KomplexeZahl ?: error("Komplexe Zahl $name fehlt.")
    private fun KnotenAuswertungsKontext.spalte(name: String) = eingänge[name]?.objekt as? SpaltenVektor ?: error("Spaltenvektor $name fehlt.")
    private fun KnotenAuswertungsKontext.zeile(name: String) = eingänge[name]?.objekt as? ZeilenVektor ?: error("Zeilenvektor $name fehlt.")
    private fun KnotenAuswertungsKontext.matrix(name: String) = eingänge[name]?.objekt as? Matrix ?: error("Matrix $name fehlt.")
    private fun KnotenAuswertungsKontext.aussage(name: String) = eingänge[name]?.objekt as? Aussage ?: error("Aussage $name fehlt.")
    private fun KnotenAuswertungsKontext.parameterInt(name: String) = knoten.parameter[name]?.toIntOrNull()?.takeIf { it > 0 } ?: error("Parameter $name muss eine positive ganze Zahl sein.")
    private fun annahmen(k: KnotenAuswertungsKontext) = k.eingänge.values.flatMap { it.annahmen }.toSet()
    private fun reellerZahlwert(
        objekt: ZahlAusdruck,
        k: KnotenAuswertungsKontext,
        zusätzlicheAnnahmen: Set<Aussage> = emptySet(),
    ) = BedingterWert(objekt, annahmen(k) + zusätzlicheAnnahmen, reelleVariablen = reelleVariablen(k.eingänge.values))
    private fun parseZahlen(text: String) = text.split(',').filter { it.isNotBlank() }.map { RationaleZahl.parse(it.trim()) }
    private fun grundmenge(name: String): MengenAusdruck = when (name.trim().uppercase()) {
        "N", "ℕ" -> NatürlicheZahlen
        "Z", "ℤ" -> GanzeZahlen
        "Q", "ℚ" -> RationaleZahlen
        "R", "ℝ" -> ReelleZahlen
        "C", "ℂ" -> KomplexeZahlen
        else -> error("Unbekannte Grundmenge '$name'. Erlaubt sind N, Z, Q, R und C.")
    }
    private fun vergleichsArt(wert: String?): VergleichsArt = when (wert) {
        "kleiner" -> VergleichsArt.Kleiner
        "kleinerGleich" -> VergleichsArt.KleinerGleich
        "größer" -> VergleichsArt.Größer
        "größerGleich" -> VergleichsArt.GrößerGleich
        else -> error("Unbekannte Ordnungsrelation '$wert'.")
    }
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
private fun KnotenAuswertungsKontext.aussagenOperatorEingänge(): List<Aussage> = operatorEingänge { _, index -> UnentscheidbareAussage("A_$index", "unverbunden") }
    .map { it.objekt as? Aussage ?: error("Aussageneingang ist ungültig.") }

internal fun eingabeLatex(index: Int) = "\\mathrm{eingabe}_{${index}}"

internal fun unbekanntesOperatorLatex(
    knoten: de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten,
    index: Int,
) = "\\mathrm{${knoten.name}}_{${eingabeLatex(index)}}"

private fun unbekannteKennung(
    knoten: de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten,
    anschluss: de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten,
) = "unbekannt_${knoten.id.wert}_${anschluss.id.wert}"

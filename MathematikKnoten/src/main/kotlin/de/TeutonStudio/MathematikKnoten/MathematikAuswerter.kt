package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*


private fun relationsVariablen(
    eingang: BedingterWert,
    relation: Aussage,
    kontext: KnotenAuswertungsKontext,
): List<GebundeneMengenVariable> {
    val quellen = eingang.variablenQuellen.geordnetEindeutig()
    val relevant = quellen.filter { quelle -> relation.freieMethodenParameter().any { it.name == quelle.name } }
    require(relevant.groupBy { it.name }.values.none { gruppe ->
        gruppe.map { it.identität }.distinct().size > 1 || gruppe.map { it.lösungsRolle to it.werteVorrat }.distinct().size > 1
    }) { "Gleichnamige Symbolquellen sind mehrdeutig; bitte unterschiedliche Namen verwenden." }
    if (quellen.isNotEmpty()) return relevant.filter { quelle ->
        quelle.argumentArt == ArgumentQuellenArt.Wert &&
            (quelle.lösungsRolle == LösungsQuellenRolle.Variable ||
                quelle.lösungsRolle == LösungsQuellenRolle.Unbekannt && relation.enthältVariable(Variable(quelle.name)))
    }.map { GebundeneMengenVariable(Variable(it.name), it.werteVorrat) }
    val name = kontext.knoten.parameter["variable"].orEmpty().trim().ifBlank { "x" }
    val variable = Variable(name)
    return if (relation.enthältVariable(variable)) listOf(GebundeneMengenVariable(variable,
        eingang.reelleVariablen[name] ?: eingang.werteVorrat ?: ReelleZahlen)) else emptyList()
}

object StandardMathematikAuswerter {
    fun erzeugeRegister() = MathematikAuswerterRegister().apply {
        registriereLatexDarstellung()
        registriereVektorKonstruktor()
        registriereAuswertungssperre { knoten ->
            knoten.parameter[RECHNER_MIGRATION_DIAGNOSE]?.let { "Migrationsdiagnose: $it" }
        }
        RechnerFamilie.entries.forEach { familie ->
            registriereOptionaleMethodenEingänge(familie.knotenArt) { knoten ->
                val op = RechnerOperatorRegistry.finde(familie, knoten.parameter[familie.operatorParameter])
                if (op is RechnerOperatorAuflösung.Bekannt) op.vertrag.signaturen
                    .firstOrNull { signatur -> signatur.eingänge.map { it.name }.toSet() == knoten.anschlüsse.filter { it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Eingang }.map { it.name }.toSet() }
                    ?.eingänge?.filter { it.identitaetBeiFehlenderMethode }?.mapTo(mutableSetOf()) { it.name }.orEmpty() else emptySet()
            }
        }
        registriere(METHODEN_QUELLE_ART) { k ->
            val eingang = k.eingänge["menge"]
            val methode = if (eingang == null) IdentitaetsMethode else identitaetAuf(eingang.objekt as? MengenAusdruck ?: error("Identität erwartet eine Menge."))
            KnotenAuswertungsErgebnis(mapOf("methode" to BedingterWert(methode, annahmen = eingang?.annahmen.orEmpty(), latexDarstellung = "x\\mapsto x")))
        }
        registriere("mathematik.zahl") { k ->
            KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(RationaleZahl.parse(k.knoten.parameter["wert"] ?: "0"))))
        }
        registriere("mathematik.variable") { k ->
            val eingang = k.eingänge["grundraum"] ?: error("Grundraum fehlt. Verbinde eine Menge mit dem Grundraumeingang.")
            val grund = eingang.objekt as? MengenAusdruck ?: error("Der Grundraum muss eine Menge sein.")
            val wert = variablenGrundraumWert(k.knoten, grund)
            KnotenAuswertungsErgebnis(mapOf("wert" to wert.copy(annahmen = wert.annahmen + eingang.annahmen)))
        }
        registriere("mathematik.allgemeinerParameter") { k ->
            KnotenAuswertungsErgebnis(mapOf("wert" to allgemeinerParameterWert(k.knoten)))
        }





        registriere("mathematik.wahr") { KnotenAuswertungsErgebnis(mapOf("aussage" to aussagenKonstantenWert(true))) }
        registriere("mathematik.lüge") { KnotenAuswertungsErgebnis(mapOf("aussage" to aussagenKonstantenWert(false))) }

        // Alte Schlüssel bleiben für importierte Karten bis zur Lade-Migration auswertbar.









        registriere("mathematik.gleichungLösen") { k ->
            val eingang = (k.eingänge["relation"]
                ?: k.eingänge["gleichung"]
                ?: error("Relation fehlt.")).mitSkalarenLösungsUnbekannten()
            val relation = eingang.objekt as? Aussage ?: error("Auflösen benötigt ein Prädikat.")
            val variablen = relationsVariablen(eingang, relation, k)
            val auflösung = lösePrädikat(relation, variablen, k.rechenKontext.copy(
                annahmen = k.rechenKontext.annahmen + eingang.annahmen))
            val ausgangsName = k.knoten.anschlüsse.firstOrNull {
                it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Ausgang
            }?.name ?: "lösungsmenge"
            KnotenAuswertungsErgebnis(
                ausgaben = mapOf(
                    ausgangsName to BedingterWert(
                        objekt = auflösung.menge,
                        annahmen = eingang.annahmen,
                    ),
                ),
                schritte = auflösung.schritte,
                warnungen = auflösung.warnungen + if (eingang.variablenQuellen.isEmpty() && variablen.isNotEmpty())
                    listOf("Historische Eingabe ohne Quellenmetadaten: gespeicherte Variable verwendet.") else emptyList(),
                polynomProblem = polynomGleichungsProblem(relation, variablen)?.copy(bedingungen = (eingang.annahmen + k.rechenKontext.annahmen + definitionsBedingungen(relation)).toList()),
                polynomKonfiguration = polynomNumerikKonfiguration(k.knoten.parameter),
                lösungsVariablen = variablen,
                lösungsParameter = eingang.variablenQuellen.filter { it.lösungsRolle == LösungsQuellenRolle.Parameter },
            )
        }
        registriere("mathematik.auswerten") { k ->
            val eingang = k.eingänge["term"]
                ?: k.eingänge["objekt"]
                ?: error("Term fehlt.")
            val vereinfachung = vereinfacheTerm(
                eingang.mathematischesObjekt("Auszuwertender Term"),
                k.rechenKontext.copy(annahmen = k.rechenKontext.annahmen + eingang.annahmen),
            )
            val ausgangsName = k.knoten.anschlüsse.firstOrNull {
                it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Ausgang
            }?.name ?: "term"
            KnotenAuswertungsErgebnis(
                ausgaben = mapOf(
                    ausgangsName to eingang.copy(
                        objekt = vereinfachung.objekt,
                        annahmen = eingang.annahmen + k.rechenKontext.annahmen,
                        latexDarstellung = null,
                    ),
                ),
                polynomProblem = (vereinfachung.objekt as? Matrix)?.let(::eigenwertAnfrage),
                polynomKonfiguration = polynomNumerikKonfiguration(k.knoten.parameter),
                schritte = vereinfachung.schritte,
                fehler = vereinfachung.entscheidung?.takeIf { it.status == EntscheidungsStatus.NichtAuswertbar }?.begründung,
                entscheidung = vereinfachung.entscheidung ?: (vereinfachung.objekt as? Aussage)?.let { aussage ->
                    val e = aussage.entscheide(k.rechenKontext.copy(annahmen = k.rechenKontext.annahmen + eingang.annahmen))
                    e.copy(begründung = e.begründung.ifBlank {
                        when {
                            e.wahrheitswert != null -> "Prädikat durch exakte Vereinfachung entschieden."
                            aussage.freieMethodenParameter().isNotEmpty() -> "Vereinfachtes Prädikat mit freien Symbolen."
                            else -> "Mit den verfügbaren Verfahren nicht entscheidbar."
                        }
                    })
                },
            )
        }




        registriere("mathematik.tupel") { k ->
            val tupel = when (tupelKonfiguration(k.knoten).erzeugungsArt) {
                TUPEL_METHODE -> {
                    val dimension = k.positiveKonkreteGanzzahl("dimension")
                    val methode = k.eingänge["methode"]?.objekt as? Methode ?: error("Tupelmethode fehlt.")
                    tupelAusMethode(methode, dimension)
                }
                else -> {
                    val werte = k.operatorEingänge { _, index -> Variable("tupel_$index") }
                        .map { it.mathematischesObjekt("Tupelelement") }
                    Tupel(werte)
                }
            }
            val komponentenRaum = if (tupelKonfiguration(k.knoten).erzeugungsArt == TUPEL_METHODE) Tupelraum(List(tupel.elemente.size) { (k.eingänge["methode"]?.objekt as Methode).einzigeZielMenge }) else {
                val werte = k.operatorEingänge { _, index -> Variable("tupel_$index") }
                Tupelraum(werte.map { wert -> wert.zielMenge ?: wert.werteVorrat ?: runCatching {
                    inferiereZielmenge(wert.mathematischesObjekt("Tupelelement"), wert.variablenQuellen.associate { it.name to it.werteVorrat }, wert.annahmen)
                }.getOrElse { FehlendeObermenge("mathematik.objekt") } })
            }
            KnotenAuswertungsErgebnis(mapOf("tupel" to k.rechnerErgebnisWert(tupel, zielMenge = komponentenRaum).copy(komponentenMengen = (komponentenRaum as? Tupelraum)?.komponenten)))
        }






        registriere("mathematik.endlicheMenge") { k -> EndlicheMengeAuswerter.auswerten(k) }

        registriere("mathematik.mengenfilter") { k ->
            val methode = k.eingänge["methode"]?.objekt as? Methode ?: error("Filtermethode fehlt.")
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
            require(k.eingänge.values.all { it.objekt is MengenAusdruck }) { "Visualisierungseingänge müssen Mengen enthalten." }
            // Historische Durchleitung bleibt bis zur ausdrücklich bestätigten Umstellung lesbar.
            KnotenAuswertungsErgebnis(if (k.knoten.anschlüsse.any { it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Ausgang })
                k.eingänge["menge"]?.let { mapOf("menge" to it) }.orEmpty() else emptyMap(), eingänge = k.eingänge)
        }
        registriere("mathematik.orbit") { k ->
            val verbundenerSchritt = k.eingänge["schritt"]?.objekt as? Methode
                ?: error("Für eine Orbitfamilie muss eine Iterationsmethode verbunden sein.")
            val start = k.eingänge["start"]?.mathematischesObjekt("Orbitstart")
                ?: error("Für eine Orbitfamilie muss ein Startwert oder eine Startmethode verbunden sein.")
            val mathematisch = verbundenerSchritt.alsMathematischeMethode("eine Orbititeration")
            require(mathematisch.parameter.size == 2) {
                "Eine Orbititeration benötigt genau zwei Parameter: Zustand und Parameter."
            }
            val zustandsName = k.knoten.parameter["zustandsArgument"].orEmpty().ifBlank {
                mathematisch.parameter.first().name
            }
            val zustandsIndex = mathematisch.parameter.indexOfFirst { it.name == zustandsName }
            val zustand = mathematisch.parameter.getOrNull(zustandsIndex)
                ?: error("Das gewählte Zustandsargument '$zustandsName' gehört nicht zur Iterationsmethode.")
            val reihenfolge = listOf(zustandsIndex) + mathematisch.parameter.indices.filterNot { it == zustandsIndex }
            val effektiverWerteVorrat = mathematisch.effektiverWerteVorrat?.let { bereich ->
                if (zustandsIndex == 0) bereich else permutiereMethodenBereich(bereich, reihenfolge)
                    ?: error("Der effektive Methodenbereich kann für das gewählte Zustandsargument nicht sicher umgeordnet werden.")
            }
            val schritt = mathematisch.copy(
                parameter = reihenfolge.map(mathematisch.parameter::get),
                effektiverWerteVorrat = effektiverWerteVorrat,
            )
            val schrittSignatur = schritt.mathematischeMethodenSignatur()
            val zustandsRaum = schrittSignatur.argumente.first().werteVorrat
            require(prüfeTeilmenge(schrittSignatur.ergebnisse.single().zielMenge, zustandsRaum).wahrheitswert != Wahrheitswert.Lüge) {
                "Die Zielmenge der Iterationsmethode ist mit dem Wertevorrat des Zustandsarguments unvereinbar."
            }
            when (start) {
                is Methode -> require(
                    prüfeTeilmenge(start.mathematischeMethodenSignatur().ergebnisse.single().zielMenge, zustandsRaum).wahrheitswert != Wahrheitswert.Lüge,
                ) {
                    "Die Zielmenge der Startmethode ist mit dem Wertevorrat des Zustandsarguments unvereinbar."
                }
                else -> require(ElementBeziehung(start, zustandsRaum).entscheide().wahrheitswert != Wahrheitswert.Lüge) {
                    "Der Orbitstart liegt außerhalb des Wertevorrats des Zustandsarguments."
                }
            }
            KnotenAuswertungsErgebnis(
                mapOf(
                    "orbit" to BedingterWert(
                        objekt = OrbitFamilie(schritt, start),
                        annahmen = annahmen(k),
                        reelleVariablen = reelleVariablen(k.eingänge.values),
                    ),
                ),
            )
        }
        registriere("mathematik.orbitBeschraenktheit") { k ->
            val orbit = k.eingänge["orbit"]?.objekt as? OrbitFamilie
                ?: error("Für die Beschränktheitsmenge muss eine Orbitfamilie verbunden sein.")
            val parameterRaum = k.eingänge["parameterraum"]?.objekt as? MengenAusdruck
                ?: error("Für die Beschränktheitsmenge muss ein Parameterraum verbunden sein.")
            val schrittSignatur = orbit.schritt.mathematischeMethodenSignatur()
            val parameterWerteVorrat = schrittSignatur.argumente[1].werteVorrat
            require(prüfeTeilmenge(parameterRaum, parameterWerteVorrat).wahrheitswert != Wahrheitswert.Lüge) {
                "Der Parameterraum ist nicht mit dem Wertevorrat des Parameterarguments vereinbar."
            }
            (orbit.start as? Methode)?.mathematischeMethodenSignatur()?.let { startSignatur ->
                val startBereich = startSignatur.effektiverDefinitionsRaum
                    ?: startSignatur.argumente.single().werteVorrat
                require(prüfeTeilmenge(parameterRaum, startBereich).wahrheitswert != Wahrheitswert.Lüge) {
                    "Der Parameterraum ist nicht mit dem Wertevorrat der Startmethode vereinbar."
                }
            }
            KnotenAuswertungsErgebnis(
                mapOf(
                    "menge" to BedingterWert(
                        objekt = OrbitBeschraenktheitsMenge(orbit, parameterRaum),
                        annahmen = annahmen(k),
                        reelleVariablen = reelleVariablen(k.eingänge.values),
                    ),
                ),
            )
        }









        registriere("mathematik.mächtigkeit") { k -> KnotenAuswertungsErgebnis(mapOf("mächtigkeit" to BedingterWert(mächtigkeit(k.menge("menge")), annahmen(k)))) }


        registriere(MathematikKnotenVorlagen.ITERIERTE_AUSSAGENVERKNÜPFUNG_ART) { k ->
            val methode = k.eingänge["methode"]?.objekt as? Methode ?: error("Aussagenmethode fehlt.")
            val indexMenge = k.eingänge["indexmenge"]?.objekt as? MengenAusdruck ?: error("Indexmenge fehlt.")
            val aussage = when (k.knoten.parameter["operator"]) {
                "konjunktion" -> iterierteKonjunktion(methode, indexMenge)
                "disjunktion" -> iterierteDisjunktion(methode, indexMenge)
                "adjunktion" -> iterierteAdjunktion(methode, indexMenge)
                else -> error("Unbekannte iterierte Aussagenverknüpfung.")
            }
            KnotenAuswertungsErgebnis(mapOf("aussage" to BedingterWert(aussage, annahmen(k))))
        }



        registriere("mathematik.abbild") { k ->
            val methode = k.eingänge["methode"]?.objekt as? Methode ?: error("Methode fehlt.")
            val mathematisch = methode.alsMathematischeMethode("Bildmengenbildung")
            val definitionsMenge = if (
                k.knoten.parameter[BILDMENGE_ARGUMENT_MODUS] == BILDMENGE_MODUS_EINZELMENGEN
            ) {
                val mengen = mathematisch.parameter.indices.map { index ->
                    k.eingänge[bildmengeArgumentName(index)]?.objekt as? MengenAusdruck
                        ?: error("Menge für Methodenargument ${index + 1} fehlt.")
                }
                when (mengen.size) {
                    0 -> EndlicheMenge(setOf(Tupel(emptyList())))
                    1 -> mengen.single()
                    else -> kartesischesProdukt(mengen)
                }
            } else {
                k.menge("menge")
            }
            KnotenAuswertungsErgebnis(
                mapOf("menge" to BedingterWert(bildeAb(
                    if (mathematisch.parameter.size == 1 && mathematisch.werteVorräte[mathematisch.parameter.single().name]?.let(::istWinkelraum) == true)
                        kanonischerRichtungsraum(definitionsMenge) else definitionsMenge,
                    mathematisch,
                ), annahmen(k))),
            )
        }
        registriere("mathematik.termZuMethode") { k ->
            val term = k.eingänge["term"] ?: error("Term fehlt.")
            KnotenAuswertungsErgebnis(mapOf("methode" to termAlsMethode(k, term)))
        }
        registriere("mathematik.komposition") { k ->
            val anschlüsse = k.knoten.anschlüsse
                .filter { it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Eingang }
                .sortedBy { it.reihenfolge }
            require(anschlüsse.size >= 2) { "Eine Komposition benötigt mindestens zwei Methodeneingänge." }
            val methoden = anschlüsse.mapIndexed { index, anschluss ->
                k.eingänge[anschluss.name]?.objekt as? Methode
                    ?: error("Methodeneingang ${index + 1} ist nicht verbunden oder enthält keine Methode.")
            }
            KnotenAuswertungsErgebnis(
                mapOf("methode" to BedingterWert(komponiere(methoden), annahmen(k))),
            )
        }
        registriere("mathematik.iteration") { k ->
            val methode = k.eingänge["methode"]?.objekt as? Methode ?: error("Methode fehlt.")
            val exponent = vereinfache(k.zahl("exponent"), k.rechenKontext) as? RationaleZahl ?: error("Iterationsexponent muss ganzzahlig sein.")
            require(exponent.nenner == java.math.BigInteger.ONE && exponent.zähler.signum() >= 0 && exponent.zähler.bitLength() < 31) { "Iterationsexponent muss eine nichtnegative ganze Zahl sein." }
            KnotenAuswertungsErgebnis(mapOf("methode" to BedingterWert(iteriere(methode, exponent.zähler.toInt()), annahmen(k))))
        }
        registriere("mathematik.methodenDifferentieren") { k ->
            val methode = k.eingänge["methode"]?.objekt as? Methode ?: error("Methode fehlt.")
            KnotenAuswertungsErgebnis(mapOf("methode" to BedingterWert(differenziereMethode(methode), annahmen(k))))
        }
        registriere("mathematik.methodenIntegrieren") { k ->
            val methode = k.eingänge["methode"]?.objekt as? Methode ?: error("Methode fehlt.")
            KnotenAuswertungsErgebnis(mapOf("methode" to BedingterWert(integriereMethode(methode), annahmen(k))))
        }













        registriere("mathematik.matrix") { k ->
            val höhe = k.parameterInt("höhe")
            val breite = k.parameterInt("breite")
            val matrix = when (k.knoten.parameter["erzeugungsArt"] ?: MATRIX_EINZEL_EINGABEN) {
                MATRIX_METHODE -> {
                    val methode = k.eingänge["methode"]?.objekt as? Methode ?: error("Matrixmethode fehlt.")
                    matrixAusMethode(methode, höhe, breite)
                }
                MATRIX_ZEILEN -> Matrix(List(höhe) { zeile ->
                    val vektor = k.eingänge[matrixZeileName(zeile)]?.objekt as? ZeilenVektor
                        ?: error("Zeile ${zeile + 1} fehlt oder ist kein Zeilenvektor.")
                    require(vektor.werte.size == breite) {
                        "Zeile ${zeile + 1} muss $breite Elemente besitzen, hat aber ${vektor.werte.size}."
                    }
                    vektor.werte
                })
                MATRIX_SPALTEN -> {
                    val spalten = List(breite) { spalte ->
                        val vektor = k.eingänge[matrixSpalteName(spalte)]?.objekt as? SpaltenVektor
                            ?: error("Spalte ${spalte + 1} fehlt oder ist kein Spaltenvektor.")
                        require(vektor.werte.size == höhe) {
                            "Spalte ${spalte + 1} muss $höhe Elemente besitzen, hat aber ${vektor.werte.size}."
                        }
                        vektor.werte
                    }
                    Matrix(List(höhe) { zeile -> List(breite) { spalte -> spalten[spalte][zeile] } })
                }
                else -> Matrix(List(höhe) { zeile -> List(breite) { spalte ->
                    k.eingänge[matrixEintragName(zeile, spalte)]?.objekt as? ZahlAusdruck
                        ?: error("Matrixeintrag (${zeile + 1},${spalte + 1}) fehlt oder ist keine Zahl.")
                } })
            }
            KnotenAuswertungsErgebnis(mapOf("matrix" to BedingterWert(matrix, annahmen(k))))
        }









        registriere("mathematik.kartenEingang") { k ->
            val name = k.knoten.parameter["name"]?.trim().orEmpty().ifBlank { "x" }
            val ausgangsArt = k.knoten.anschlüsse.firstOrNull {
                it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Ausgang
            }?.art ?: MathematikAnschlussArten.Objekt.id
            val parameter: MethodenParameter = if (ausgangsArt == MathematikAnschlussArten.Zahl.id) Variable(name) else AllgemeinerParameter(name)
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
            val lüge = k.eingänge["lüge"] ?: error("Falsch-Eingang fehlt.")
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
                null -> BedingterWert(
                    FallAusdruck(
                        wahr.mathematischesObjekt("Wahr-Zweig der Fallunterscheidung"),
                        aussage,
                        lüge.mathematischesObjekt("Falsch-Zweig der Fallunterscheidung"),
                    ),
                )
            }
            val selektiv=k.knoten.parameter["fall.annahmenNurGewaehlterZweig"]=="true" && basis.objekt !is FallAusdruck
            KnotenAuswertungsErgebnis(mapOf(
                "wert" to basis.copy(
                    annahmen = if(selektiv) basis.annahmen+aussageWert.annahmen else gemeinsameAnnahmen,
                    reelleVariablen = gemeinsameReelleVariablen,
                    variablenQuellen = if(selektiv) (basis.variablenQuellen+aussageWert.variablenQuellen).distinct() else gemeinsameQuellen,
                ),
            ))
        }





    }

    private fun KnotenAuswertungsKontext.zahl(name: String) = eingänge[name]?.objekt as? ZahlAusdruck ?: error("Zahleingang $name fehlt.")
    private fun KnotenAuswertungsKontext.objekt(name: String) =
        eingänge[name]?.mathematischesObjekt("Eingang '$name'") ?: error("Eingang $name fehlt.")
    private fun KnotenAuswertungsKontext.menge(name: String) = eingänge[name]?.objekt as? MengenAusdruck ?: error("Mengeneingang $name fehlt.")
    private fun KnotenAuswertungsKontext.komplex(name: String) = eingänge[name]?.objekt as? KomplexeZahl ?: error("Komplexe Zahl $name fehlt.")
    private fun KnotenAuswertungsKontext.spalte(name: String) = eingänge[name]?.objekt as? SpaltenVektor ?: error("Spaltenvektor $name fehlt.")
    private fun KnotenAuswertungsKontext.zeile(name: String) = eingänge[name]?.objekt as? ZeilenVektor ?: error("Zeilenvektor $name fehlt.")
    private fun KnotenAuswertungsKontext.matrix(name: String) = eingänge[name]?.objekt as? Matrix ?: error("Matrix $name fehlt.")
    private fun KnotenAuswertungsKontext.aussage(name: String) = eingänge[name]?.objekt as? Aussage ?: error("Aussage $name fehlt.")
    private fun KnotenAuswertungsKontext.parameterInt(name: String) = knoten.parameter[name]?.toIntOrNull()?.takeIf { it > 0 } ?: error("Parameter $name muss eine positive ganze Zahl sein.")
    private fun KnotenAuswertungsKontext.positiveKonkreteGanzzahl(name: String): Int {
        val zahl = eingänge[name]?.objekt as? RationaleZahl
            ?: error("Der Eingang '$name' muss eine konkrete positive ganze Zahl sein.")
        require(zahl.nenner == java.math.BigInteger.ONE && zahl.zähler.signum() > 0) {
            "Der Eingang '$name' muss eine konkrete positive ganze Zahl sein."
        }
        return runCatching { zahl.zähler.intValueExact() }.getOrElse {
            error("Der Eingang '$name' überschreitet die unterstützte konkrete Tupeldimension.")
        }
    }
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


}

/** Formeln für assoziative Operatoren verwenden für fehlende Eingänge stabile, eindeutige Unbekannte. */
internal fun KnotenAuswertungsKontext.operatorEingänge(
    unbekannt: (de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten, Int) -> MathematischesObjekt,
): List<BedingterWert> = knoten.anschlüsse
    .filter { it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Eingang }
    .sortedBy { it.reihenfolge }
    .dropLastWhile { it.dynamischErzeugt && it.name !in eingänge }
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

private fun permutiereMethodenBereich(
    bereich: MengenAusdruck,
    reihenfolge: List<Int>,
): MengenAusdruck? = when (bereich) {
    is Tupelraum -> bereich.komponenten.takeIf { it.size == reihenfolge.size }
        ?.let { komponenten -> Tupelraum(reihenfolge.map(komponenten::get)) }
    is KartesischesProdukt -> bereich.mengen.takeIf { it.size == reihenfolge.size }
        ?.let { komponenten -> KartesischesProdukt(reihenfolge.map(komponenten::get)) }
    is EndlicheMenge -> {
        val tupel = bereich.elemente.map { it as? Tupel ?: return null }
        if (tupel.any { it.elemente.size != reihenfolge.size }) null
        else EndlicheMenge(tupel.map { wert -> Tupel(reihenfolge.map(wert.elemente::get)) }.toSet())
    }
    else -> null
}

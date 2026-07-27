package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.VisualisierungsKonfiguration

object MathematikKnotenVorlagen {
    private fun eingang(name: String, art: AnschlussArtId, reihe: Int = 0, erweiterbar: Boolean = false) = AnschlussDaten(name = name, richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = art, reihenfolge = reihe, kannSichErweitern = erweiterbar)
    private fun ausgang(name: String, art: AnschlussArtId, reihe: Int = 0) = AnschlussDaten(name = name, richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = art, reihenfolge = reihe)

    val Zahl = KnotenVorlage(
        "mathematik.zahl", "Zahl", "Rechnen", "Exakte ganze oder rationale Zahl.", GraphGröße(180f, 92f),
        listOf(ausgang("wert", MathematikAnschlussArten.Zahl.id)), mapOf("wert" to "2"),
    )
    val Variable = KnotenVorlage(
        "mathematik.variable", "Variable", "Rechnen", "Freie mathematische Variable mit optionalem Wertevorrat.", GraphGröße(190f, 102f),
        listOf(eingang("wertevorrat", MathematikAnschlussArten.Menge.id), ausgang("wert", MathematikAnschlussArten.Zahl.id)), mapOf("name" to "x"),
    )
    val Addition = KnotenVorlage(
        "mathematik.addition", "Addition", "Rechnen", "Addiert zwei oder mehr Zahlterme.", GraphGröße(220f, 130f),
        listOf(eingang("a", MathematikAnschlussArten.Zahl.id, 0, true), eingang("b", MathematikAnschlussArten.Zahl.id, 1, true), ausgang("wert", MathematikAnschlussArten.Zahl.id)),
        mapOf("festeEingänge" to "2", "operatorAnzeige" to "wert"),
    )
    val Maximum = extremwertVorlage("Maximum", "Maximum von zwei oder mehr nachweisbar reellen Zahlen.", "maximum")
    val Minimum = extremwertVorlage("Minimum", "Minimum von zwei oder mehr nachweisbar reellen Zahlen.", "minimum")
    val Multiplikation = KnotenVorlage(
        "mathematik.multiplikation", "Multiplikation", "Rechnen", "Multipliziert zwei oder mehr Zahlterme.", GraphGröße(220f, 130f),
        listOf(eingang("a", MathematikAnschlussArten.Zahl.id, 0), eingang("b", MathematikAnschlussArten.Zahl.id, 1), eingang("c", MathematikAnschlussArten.Zahl.id, 2), ausgang("wert", MathematikAnschlussArten.Zahl.id)),
    )
    val Division = KnotenVorlage(
        "mathematik.division", "Division", "Rechnen", "Teilt durch einen Zahlterm und führt den Fall Divisor = 0 als separaten Ausgang.", GraphGröße(235f, 126f),
        listOf(eingang("dividend", MathematikAnschlussArten.Zahl.id, 0), eingang("divisor", MathematikAnschlussArten.Zahl.id, 1), ausgang("wert", MathematikAnschlussArten.Zahl.id), ausgang("divisorNull", MathematikAnschlussArten.Aussage.id, 1)),
    )
    val Potenz = KnotenVorlage(
        "mathematik.potenz", "Potenz", "Rechnen", "Potenz aus Basis und Exponent.", GraphGröße(210f, 110f),
        listOf(eingang("basis", MathematikAnschlussArten.Zahl.id, 0), eingang("exponent", MathematikAnschlussArten.Zahl.id, 1), ausgang("wert", MathematikAnschlussArten.Zahl.id)),
    )
    val Gleichheit = KnotenVorlage(
        "mathematik.gleichheit", "Gleichheit", "Aussagen: Aussagenprädikate", "Vergleicht zwei mathematische Objekte.", GraphGröße(220f, 110f),
        listOf(eingang("links", MathematikAnschlussArten.Objekt.id, 0), eingang("rechts", MathematikAnschlussArten.Objekt.id, 1), ausgang("aussage", MathematikAnschlussArten.Aussage.id)),
    )
    val Wahr = KnotenVorlage("mathematik.wahr", "Wahr", "Aussagen: Aussagenlogik", "Die wahre Aussage ⊤.", GraphGröße(180f, 90f), listOf(ausgang("aussage", MathematikAnschlussArten.Aussage.id)))
    val Lüge = KnotenVorlage("mathematik.lüge", "Lüge", "Aussagen: Aussagenlogik", "Die falsche Aussage ⊥.", GraphGröße(180f, 90f), listOf(ausgang("aussage", MathematikAnschlussArten.Aussage.id)))
    val Element = aussagenVorlage("mathematik.element", "Element", "Prüft, ob ein Objekt Element einer Menge ist.", MathematikAnschlussArten.Objekt.id, MathematikAnschlussArten.Menge.id, "Aussagen: Mengenprädikate")
    val Kleiner = vergleichVorlage("mathematik.kleiner", "Kleiner", "<")
    val Größer = vergleichVorlage("mathematik.größer", "Größer", ">")
    val KleinerGleich = vergleichVorlage("mathematik.kleinerGleich", "Kleiner oder gleich", "≤")
    val GrößerGleich = vergleichVorlage("mathematik.größerGleich", "Größer oder gleich", "≥")
    val Teilmenge = mengenAussagenVorlage("mathematik.teilmenge", "Teilmenge", "Prüft die echte Teilmengenbeziehung.")
    val Übermenge = mengenAussagenVorlage("mathematik.übermenge", "Übermenge", "Prüft die echte Übermengenbeziehung.")
    val TeilOderGleichmenge = mengenAussagenVorlage("mathematik.teilOderGleichmenge", "Teil- oder Gleichmenge", "Prüft ⊆.")
    val ÜberOderGleichmenge = mengenAussagenVorlage("mathematik.überOderGleichmenge", "Über- oder Gleichmenge", "Prüft ⊇.")
    val Disjunkt = mengenAussagenVorlage("mathematik.disjunkt", "Disjunkt", "Prüft, ob zwei Mengen keinen gemeinsamen Wert besitzen.")
    val GleichungLösen = KnotenVorlage(
        "mathematik.gleichungLösen", "Lineare Gleichung lösen", "Algebra", "Löst eine lineare Gleichung nach der gewählten Variablen.", GraphGröße(240f, 115f),
        listOf(eingang("gleichung", MathematikAnschlussArten.Aussage.id), ausgang("lösungen", MathematikAnschlussArten.Menge.id)), mapOf("variable" to "x"),
    )
    val Auswerten = KnotenVorlage(
        "mathematik.auswerten", "Auswerten", "Steuerung", "Vereinfacht einen Term oder entscheidet eine Aussage.", GraphGröße(230f, 110f),
        listOf(eingang("objekt", MathematikAnschlussArten.Objekt.id), ausgang("wert", MathematikAnschlussArten.Objekt.id)),
    )
    val Ableiten = KnotenVorlage(
        "mathematik.ableiten", "Differentieren", "Analysis", "Differentiert einen Zahlterm symbolisch.", GraphGröße(220f, 110f),
        listOf(eingang("term", MathematikAnschlussArten.Zahl.id), ausgang("wert", MathematikAnschlussArten.Zahl.id)), mapOf("variable" to "x"),
    )
    val Integrieren = KnotenVorlage(
        "mathematik.integrieren", "Integrieren", "Analysis", "Bestimmt eine elementare Stammfunktion.", GraphGröße(220f, 110f),
        listOf(eingang("term", MathematikAnschlussArten.Zahl.id), ausgang("wert", MathematikAnschlussArten.Zahl.id)), mapOf("variable" to "x"),
    )
    val Wurzel = KnotenVorlage(
        "mathematik.wurzel", "Wurzel", "Rechnen", "Bildet genau die Hauptwurzel; für negative reelle Zahlen gegebenenfalls komplex.", GraphGröße(220f, 105f),
        listOf(eingang("radikand", MathematikAnschlussArten.Zahl.id), ausgang("wert", MathematikAnschlussArten.Zahl.id)),
    )
    val Logarithmus = KnotenVorlage(
        "mathematik.logarithmus", "Logarithmus", "Rechnen", "Logarithmus mit Basis; e, 2 und 10 werden als ln, lb und log dargestellt.", GraphGröße(230f, 112f),
        listOf(eingang("basis", MathematikAnschlussArten.Zahl.id, 0), eingang("argument", MathematikAnschlussArten.Zahl.id, 1), ausgang("wert", MathematikAnschlussArten.Zahl.id)),
    )
    val Tupel = KnotenVorlage(
        "mathematik.tupel", "Tupel", "Zahlen", "Geordnetes Tupel aus zwei oder mehr Zahlen.", GraphGröße(220f, 115f),
        listOf(eingang("a", MathematikAnschlussArten.Zahl.id, 0, true), eingang("b", MathematikAnschlussArten.Zahl.id, 1, true), ausgang("tupel", MathematikAnschlussArten.Tupel.id)),
        mapOf("festeEingänge" to "2", "operatorAnzeige" to "wert"),
    )
    val KomplexAusTupel = KnotenVorlage(
        "mathematik.komplexAusTupel", "Komplexe Zahl aus Tupel", "Zahlen", "Interpretiert ein Tupel als (x,y) oder (r,φ).", GraphGröße(250f, 110f),
        listOf(eingang("tupel", MathematikAnschlussArten.Tupel.id), ausgang("zahl", MathematikAnschlussArten.Zahl.id)), mapOf("modus" to "kartesisch"),
    )
    val Konjugierte = KnotenVorlage("mathematik.konjugierte", "Konjugierte", "Zahlen", "Bildet die komplex konjugierte Zahl.", GraphGröße(220f, 105f), listOf(eingang("zahl", MathematikAnschlussArten.Zahl.id), ausgang("wert", MathematikAnschlussArten.Zahl.id)))
    val Realteil = KnotenVorlage("mathematik.realteil", "Realteil", "Zahlen", "Liest den Realteil einer komplexen Zahl.", GraphGröße(210f, 105f), listOf(eingang("zahl", MathematikAnschlussArten.Zahl.id), ausgang("wert", MathematikAnschlussArten.Zahl.id)))
    val Imaginärteil = KnotenVorlage("mathematik.imaginärteil", "Imaginärteil", "Zahlen", "Liest den Imaginärteil einer komplexen Zahl.", GraphGröße(220f, 105f), listOf(eingang("zahl", MathematikAnschlussArten.Zahl.id), ausgang("wert", MathematikAnschlussArten.Zahl.id)))
    val KomplexerRadius = KnotenVorlage("mathematik.komplexerRadius", "Radius einer Zahl", "Zahlen", "Betrag einer komplexen Zahl.", GraphGröße(220f, 105f), listOf(eingang("zahl", MathematikAnschlussArten.Zahl.id), ausgang("wert", MathematikAnschlussArten.Zahl.id)))
    val Winkel = KnotenVorlage("mathematik.winkel", "Winkel einer Zahl", "Zahlen", "Hauptargument einer komplexen Zahl.", GraphGröße(220f, 105f), listOf(eingang("zahl", MathematikAnschlussArten.Zahl.id), ausgang("wert", MathematikAnschlussArten.Zahl.id)))
    val EndlicheMenge = KnotenVorlage(
        "mathematik.endlicheMenge", "Endliche Menge", "Mengen", "Endliche Menge aus einer kommagetrennten Liste.", GraphGröße(220f, 105f),
        listOf(ausgang("menge", MathematikAnschlussArten.Menge.id)), mapOf("elemente" to "1,2,3"),
    )
    val ReellesIntervall = KnotenVorlage(
        "mathematik.reellesIntervall", "Reelles Intervall", "Mengen", "Bildet das abgeschlossene reelle Intervall [a,b] aus zwei nachweisbar reellen Grenzen.", GraphGröße(270f, 115f),
        listOf(
            eingang("untereGrenze", MathematikAnschlussArten.Zahl.id, 0),
            eingang("obereGrenze", MathematikAnschlussArten.Zahl.id, 1),
            ausgang("menge", MathematikAnschlussArten.Menge.id),
        ),
    )
    val Lösungsmenge = KnotenVorlage(
        "mathematik.lösungsmenge", "Lösungsmenge", "Mengen", "Bildet eine symbolische Menge aller Variablenwerte, die eine Aussage erfüllen.", GraphGröße(270f, 135f),
        listOf(eingang("bedingung", MathematikAnschlussArten.Aussage.id), ausgang("menge", MathematikAnschlussArten.Menge.id)),
        mapOf("automatisch" to "true", "variablen" to "", "grundmengen" to "R"),
    )
    val Visualisierung = KnotenVorlage(
        "mathematik.visualisierung", "Visualisierung", "Visualisierung", "Stellt Mengen als numerische Approximation in R² oder R³ dar und reicht sie unverändert weiter.", GraphGröße(620f, 480f),
        listOf(eingang("menge", MathematikAnschlussArten.Menge.id), ausgang("menge", MathematikAnschlussArten.Menge.id)),
        standardEigenschaften = VisualisierungsKonfiguration().zuEigenschaften(),
    )
    val Vereinigung = KnotenVorlage(
        "mathematik.vereinigung", "Vereinigung", "Mengen", "Vereinigt zwei Mengen.", GraphGröße(220f, 110f),
        listOf(eingang("a", MathematikAnschlussArten.Menge.id, 0, true), eingang("b", MathematikAnschlussArten.Menge.id, 1, true), ausgang("menge", MathematikAnschlussArten.Menge.id)),
        mapOf("festeEingänge" to "2", "operatorAnzeige" to "wert"),
    )
    val Schnitt = mengenOperatorVorlage("mathematik.schnitt", "Schnitt", "Schneidet zwei oder mehr Mengen.", "\\cap")
    val Differenz = KnotenVorlage(
        "mathematik.differenz", "Differenz", "Mengen", "Entfernt die rechte Menge aus der linken.", GraphGröße(220f, 110f),
        listOf(eingang("links", MathematikAnschlussArten.Menge.id, 0), eingang("rechts", MathematikAnschlussArten.Menge.id, 1), ausgang("menge", MathematikAnschlussArten.Menge.id)),
    )
    val KartesischesProdukt = mengenOperatorVorlage("mathematik.kartesischesProdukt", "Kartesisches Produkt", "Bildet das kartesische Produkt von zwei oder mehr Mengen.", "\\times")
    val NatürlicheZahlen = KnotenVorlage(
        "mathematik.natürlicheZahlen", "Natürliche Zahlen", "Mengen", "Die Menge der natürlichen Zahlen.", GraphGröße(210f, 90f),
        listOf(ausgang("menge", MathematikAnschlussArten.Menge.id)),
    )
    val GanzeZahlen = KnotenVorlage(
        "mathematik.ganzeZahlen", "Ganze Zahlen", "Mengen", "Die Menge der ganzen Zahlen.", GraphGröße(210f, 90f),
        listOf(ausgang("menge", MathematikAnschlussArten.Menge.id)),
    )
    val RationaleZahlen = KnotenVorlage(
        "mathematik.rationaleZahlen", "Rationale Zahlen", "Mengen", "Die Menge der rationalen Zahlen.", GraphGröße(210f, 90f),
        listOf(ausgang("menge", MathematikAnschlussArten.Menge.id)),
    )
    val ReelleZahlen = KnotenVorlage(
        "mathematik.reelleZahlen", "Reelle Zahlen", "Mengen", "Die Menge der reellen Zahlen.", GraphGröße(210f, 90f),
        listOf(ausgang("menge", MathematikAnschlussArten.Menge.id)),
    )
    val KomplexeZahlen = KnotenVorlage("mathematik.komplexeZahlen", "Komplexe Zahlen", "Mengen", "Die Menge der komplexen Zahlen.", GraphGröße(210f, 90f), listOf(ausgang("menge", MathematikAnschlussArten.Menge.id)))
    val Mächtigkeit = KnotenVorlage("mathematik.mächtigkeit", "Mächtigkeit", "Mengen", "Bestimmt endlich, abzählbar unendlich oder überabzählbar.", GraphGröße(230f, 105f), listOf(eingang("menge", MathematikAnschlussArten.Menge.id), ausgang("mächtigkeit", MathematikAnschlussArten.Objekt.id)))
    val IterierteSumme = KnotenVorlage(
        "mathematik.iterierteSumme", "Iterierte Summe", "Operatoren", "Summiert die Werte einer Zahlfunktion über einer Indexmenge.", GraphGröße(250f, 120f),
        listOf(eingang("methode", MathematikAnschlussArten.ZahlFunktion.id), eingang("indexmenge", MathematikAnschlussArten.Menge.id, 1), ausgang("wert", MathematikAnschlussArten.Zahl.id)),
    )
    val IteriertesProdukt = KnotenVorlage(
        "mathematik.iteriertesProdukt", "Iteriertes Produkt", "Operatoren", "Multipliziert die Werte einer Zahlfunktion über einer Indexmenge.", GraphGröße(250f, 120f),
        listOf(eingang("methode", MathematikAnschlussArten.ZahlFunktion.id), eingang("indexmenge", MathematikAnschlussArten.Menge.id, 1), ausgang("wert", MathematikAnschlussArten.Zahl.id)),
    )
    val IterierteVereinigung = KnotenVorlage(
        "mathematik.iterierteVereinigung", "Iterierte Vereinigung", "Mengen", "Vereinigt die Mengenwerte einer Methode über einer Indexmenge.", GraphGröße(260f, 120f),
        listOf(eingang("methode", MathematikAnschlussArten.MengenFunktion.id), eingang("indexmenge", MathematikAnschlussArten.Menge.id, 1), ausgang("menge", MathematikAnschlussArten.Menge.id)),
    )
    val IterierterSchnitt = KnotenVorlage(
        "mathematik.iterierterSchnitt", "Iterierter Schnitt", "Mengen", "Schneidet Mengenwerte; die Grundmenge stammt aus der Zielmenge der Methode.", GraphGröße(260f, 120f),
        listOf(eingang("methode", MathematikAnschlussArten.MengenFunktion.id), eingang("indexmenge", MathematikAnschlussArten.Menge.id, 1), ausgang("menge", MathematikAnschlussArten.Menge.id)),
    )
    val IteriertesKartesischesProdukt = KnotenVorlage(
        "mathematik.iteriertesKartesischesProdukt", "Iteriertes kartesisches Produkt", "Mengen", "Bildet das kartesische Produkt der Mengenwerte einer Methode über einer Indexmenge.", GraphGröße(280f, 120f),
        listOf(eingang("methode", MathematikAnschlussArten.MengenFunktion.id), eingang("indexmenge", MathematikAnschlussArten.Menge.id, 1), ausgang("menge", MathematikAnschlussArten.Menge.id)),
    )
    val Abbild = KnotenVorlage(
        "mathematik.abbild", "Abbild", "Mengen", "Bildet eine Menge mit einer einwertigen Methode ab: f[M] = { f(x) : x ∈ M }.", GraphGröße(255f, 115f),
        listOf(eingang("menge", MathematikAnschlussArten.Menge.id, 0), eingang("methode", MathematikAnschlussArten.ZahlFunktion.id, 1), ausgang("menge", MathematikAnschlussArten.Menge.id)),
    )
    val TermZuMethode = KnotenVorlage(
        "mathematik.termZuMethode", "Term zu Methode", "Methoden", "Erzeugt aus einem Term, Variablen und einer Zielmenge eine Methode.", GraphGröße(265f, 135f),
        listOf(eingang("term", MathematikAnschlussArten.Zahl.id, 0), eingang("argument1", MathematikAnschlussArten.Zahl.id, 1, true), eingang("zielmenge", MathematikAnschlussArten.Menge.id, 2), ausgang("methode", MathematikAnschlussArten.ZahlFunktion.id)),
        mapOf("name" to "f"),
    )
    val Komposition = KnotenVorlage("mathematik.komposition", "Komposition", "Abbildungen", "Komponiert zwei einwertige skalare Methoden.", GraphGröße(245f, 110f), listOf(eingang("außen", MathematikAnschlussArten.ZahlFunktion.id, 0), eingang("innen", MathematikAnschlussArten.ZahlFunktion.id, 1), ausgang("methode", MathematikAnschlussArten.ZahlFunktion.id)))
    val Iteration = KnotenVorlage("mathematik.iteration", "Iteration", "Abbildungen", "Bildet die nichtnegative Iteration einer skalaren Endomorphismus-Methode.", GraphGröße(255f, 110f), listOf(eingang("methode", MathematikAnschlussArten.ZahlFunktion.id, 0), eingang("exponent", MathematikAnschlussArten.Zahl.id, 1), ausgang("methode", MathematikAnschlussArten.ZahlFunktion.id)))
    val MethodenDifferentieren = KnotenVorlage("mathematik.methodenDifferentieren", "Methode differentieren", "Abbildungen", "Differentiert eine skalare Methode bei differentialfähigem Wertevorrat.", GraphGröße(260f, 105f), listOf(eingang("methode", MathematikAnschlussArten.ZahlFunktion.id), ausgang("methode", MathematikAnschlussArten.ZahlFunktion.id)))
    val MethodenIntegrieren = KnotenVorlage("mathematik.methodenIntegrieren", "Methode integrieren", "Abbildungen", "Integriert eine skalare Methode bei integralfähigem Wertevorrat.", GraphGröße(255f, 105f), listOf(eingang("methode", MathematikAnschlussArten.ZahlFunktion.id), ausgang("methode", MathematikAnschlussArten.ZahlFunktion.id)))
    val SpaltenMethodeDifferentieren = methodenAnalysisVorlage("mathematik.spaltenMethodeDifferentieren", "Spaltenmethode differentieren", "Differentiert die Komponenten einer Spaltenvektormethode.", MathematikAnschlussArten.SpaltenVektorFunktion.id)
    val ZeilenMethodeDifferentieren = methodenAnalysisVorlage("mathematik.zeilenMethodeDifferentieren", "Zeilenmethode differentieren", "Differentiert die Komponenten einer Zeilenvektormethode.", MathematikAnschlussArten.ZeilenVektorFunktion.id)
    val SpaltenMethodeIntegrieren = methodenAnalysisVorlage("mathematik.spaltenMethodeIntegrieren", "Spaltenmethode integrieren", "Integriert die Komponenten einer Spaltenvektormethode.", MathematikAnschlussArten.SpaltenVektorFunktion.id)
    val ZeilenMethodeIntegrieren = methodenAnalysisVorlage("mathematik.zeilenMethodeIntegrieren", "Zeilenmethode integrieren", "Integriert die Komponenten einer Zeilenvektormethode.", MathematikAnschlussArten.ZeilenVektorFunktion.id)
    val Vektor = KnotenVorlage(
        "mathematik.vektor", "Spaltenvektor", "Vektoren", "Spaltenvektor aus dynamischen Zahl-Eingängen.", GraphGröße(220f, 115f),
        listOf(eingang("a", MathematikAnschlussArten.Zahl.id, 0, true), eingang("b", MathematikAnschlussArten.Zahl.id, 1, true), ausgang("vektor", MathematikAnschlussArten.SpaltenVektor.id)), mapOf("festeEingänge" to "2", "operatorAnzeige" to "wert"),
    )
    val ZeilenVektor = KnotenVorlage("mathematik.zeilenVektor", "Zeilenvektor", "Vektoren", "Zeilenvektor aus dynamischen Zahl-Eingängen.", GraphGröße(220f, 115f), listOf(eingang("a", MathematikAnschlussArten.Zahl.id, 0, true), eingang("b", MathematikAnschlussArten.Zahl.id, 1, true), ausgang("vektor", MathematikAnschlussArten.ZeilenVektor.id)), mapOf("festeEingänge" to "2", "operatorAnzeige" to "wert"))
    val VektorZuPolynom = KnotenVorlage(
        "mathematik.vektorZuPolynom", "Vektor zu Polynom", "Vektoren", "Liest einen Vektor als Koeffizienten c₀, …, cₙ eines Polynoms.", GraphGröße(240f, 110f),
        listOf(eingang("vektor", MathematikAnschlussArten.Vektor.id), ausgang("wert", MathematikAnschlussArten.Zahl.id)), mapOf("variable" to "x"),
    )
    val TupelZuSpalte = KnotenVorlage("mathematik.tupelZuSpalte", "Tupel zu Spalte", "Vektoren", "Erzeugt aus einem Zahlentupel einen Spaltenvektor.", GraphGröße(225f, 105f), listOf(eingang("tupel", MathematikAnschlussArten.Tupel.id), ausgang("vektor", MathematikAnschlussArten.SpaltenVektor.id)))
    val TupelZuZeile = KnotenVorlage("mathematik.tupelZuZeile", "Tupel zu Zeile", "Vektoren", "Erzeugt aus einem Zahlentupel einen Zeilenvektor.", GraphGröße(225f, 105f), listOf(eingang("tupel", MathematikAnschlussArten.Tupel.id), ausgang("vektor", MathematikAnschlussArten.ZeilenVektor.id)))
    val EinheitsSpalte = KnotenVorlage("mathematik.einheitsSpalte", "Einheitsvektor (Spalte)", "Vektoren", "Standardbasisvektor eᵢ als Spalte.", GraphGröße(225f, 105f), listOf(ausgang("vektor", MathematikAnschlussArten.SpaltenVektor.id)), mapOf("dimension" to "3", "index" to "1"))
    val EinheitsZeile = KnotenVorlage("mathematik.einheitsZeile", "Einheitsvektor (Zeile)", "Vektoren", "Standardbasisvektor eᵢ als Zeile.", GraphGröße(225f, 105f), listOf(ausgang("vektor", MathematikAnschlussArten.ZeilenVektor.id)), mapOf("dimension" to "3", "index" to "1"))
    val VektorRadiusSpalte = KnotenVorlage("mathematik.vektorRadiusSpalte", "Radius (Spalte)", "Vektoren", "Euklidische Norm eines Spaltenvektors.", GraphGröße(220f, 105f), listOf(eingang("vektor", MathematikAnschlussArten.SpaltenVektor.id), ausgang("wert", MathematikAnschlussArten.Zahl.id)))
    val VektorRadiusZeile = KnotenVorlage("mathematik.vektorRadiusZeile", "Radius (Zeile)", "Vektoren", "Euklidische Norm eines Zeilenvektors.", GraphGröße(220f, 105f), listOf(eingang("vektor", MathematikAnschlussArten.ZeilenVektor.id), ausgang("wert", MathematikAnschlussArten.Zahl.id)))
    val Matrix = KnotenVorlage(
        "mathematik.matrix", "Matrix", "Matrizen", "Matrix aus skalaren Einträgen oder einer zweistelligen Zahlmethode.", GraphGröße(250f, 115f),
        listOf(
            eingang(matrixEintragName(0, 0), MathematikAnschlussArten.Zahl.id, 0),
            eingang(matrixEintragName(0, 1), MathematikAnschlussArten.Zahl.id, 1),
            eingang(matrixEintragName(1, 0), MathematikAnschlussArten.Zahl.id, 2),
            eingang(matrixEintragName(1, 1), MathematikAnschlussArten.Zahl.id, 3),
            ausgang("matrix", MathematikAnschlussArten.Matrix.id),
        ),
        mapOf("erzeugungsArt" to MATRIX_EINZEL_EINGABEN, "höhe" to "2", "breite" to "2"),
    )
    val Skalarprodukt = KnotenVorlage(
        "mathematik.skalarprodukt", "Skalarprodukt (Spalten)", "Vektoren", "Berechnet das Skalarprodukt zweier Spaltenvektoren.", GraphGröße(230f, 110f),
        listOf(eingang("a", MathematikAnschlussArten.SpaltenVektor.id, 0), eingang("b", MathematikAnschlussArten.SpaltenVektor.id, 1), ausgang("wert", MathematikAnschlussArten.Zahl.id)),
    )
    val SkalarproduktZeile = KnotenVorlage("mathematik.skalarproduktZeile", "Skalarprodukt (Zeilen)", "Vektoren", "Berechnet das Skalarprodukt zweier Zeilenvektoren.", GraphGröße(230f, 110f), listOf(eingang("a", MathematikAnschlussArten.ZeilenVektor.id, 0), eingang("b", MathematikAnschlussArten.ZeilenVektor.id, 1), ausgang("wert", MathematikAnschlussArten.Zahl.id)))
    val KreuzproduktSpalte = KnotenVorlage("mathematik.kreuzproduktSpalte", "Kreuzprodukt (Spalten)", "Vektoren", "Kreuzprodukt reeller 3-Spaltenvektoren.", GraphGröße(235f, 110f), listOf(eingang("a", MathematikAnschlussArten.SpaltenVektor.id, 0), eingang("b", MathematikAnschlussArten.SpaltenVektor.id, 1), ausgang("vektor", MathematikAnschlussArten.SpaltenVektor.id)))
    val KreuzproduktZeile = KnotenVorlage("mathematik.kreuzproduktZeile", "Kreuzprodukt (Zeilen)", "Vektoren", "Kreuzprodukt reeller 3-Zeilenvektoren.", GraphGröße(235f, 110f), listOf(eingang("a", MathematikAnschlussArten.ZeilenVektor.id, 0), eingang("b", MathematikAnschlussArten.ZeilenVektor.id, 1), ausgang("vektor", MathematikAnschlussArten.ZeilenVektor.id)))
    val TransponiereSpalte = KnotenVorlage("mathematik.transponiereSpalte", "Transponiere Spalte", "Vektoren", "Wandelt eine Spalte in eine Zeile um.", GraphGröße(220f, 105f), listOf(eingang("vektor", MathematikAnschlussArten.SpaltenVektor.id), ausgang("vektor", MathematikAnschlussArten.ZeilenVektor.id)))
    val TransponiereZeile = KnotenVorlage("mathematik.transponiereZeile", "Transponiere Zeile", "Vektoren", "Wandelt eine Zeile in eine Spalte um.", GraphGröße(220f, 105f), listOf(eingang("vektor", MathematikAnschlussArten.ZeilenVektor.id), ausgang("vektor", MathematikAnschlussArten.SpaltenVektor.id)))
    val MatrixProdukt = KnotenVorlage("mathematik.matrixProdukt", "Matrixprodukt", "Rechnen", "Multipliziert zwei kompatible Matrizen.", GraphGröße(230f, 110f), listOf(eingang("a", MathematikAnschlussArten.Matrix.id, 0), eingang("b", MathematikAnschlussArten.Matrix.id, 1), ausgang("matrix", MathematikAnschlussArten.Matrix.id)))
    val TransponiereMatrix = KnotenVorlage("mathematik.transponiereMatrix", "Transponiere Matrix", "Rechnen", "Transponiert eine Matrix.", GraphGröße(230f, 105f), listOf(eingang("matrix", MathematikAnschlussArten.Matrix.id), ausgang("matrix", MathematikAnschlussArten.Matrix.id)))
    val MatrixInvertieren = KnotenVorlage(
        "mathematik.matrixInvertieren", "Matrix invertieren", "Rechnen", "Invertiert eine rationale quadratische Matrix exakt.", GraphGröße(245f, 110f),
        listOf(eingang("matrix", MathematikAnschlussArten.Matrix.id), ausgang("inverse", MathematikAnschlussArten.Matrix.id)),
    )
    val KartenEingang = KnotenVorlage(
        "mathematik.kartenEingang", "Karten-Eingang", "Gruppen", "Öffentlicher Parameter einer wiederverwendbaren Karte.", GraphGröße(210f, 100f),
        listOf(ausgang("wert", MathematikAnschlussArten.Objekt.id)), mapOf("name" to "x"),
    )
    val KartenAusgang = KnotenVorlage(
        "mathematik.kartenAusgang", "Karten-Ausgang", "Gruppen", "Öffentliche Ausgabe einer wiederverwendbaren Karte.", GraphGröße(210f, 100f),
        listOf(eingang("wert", MathematikAnschlussArten.Objekt.id)), mapOf("name" to "ergebnis"),
    )
    val Fall = KnotenVorlage(
        "mathematik.fall", "Fallunterscheidung", "Steuerung", "Verzweigt einen Ausdruck anhand einer Aussage und gibt die Annahmen weiter.", GraphGröße(250f, 140f),
        listOf(eingang("term", MathematikAnschlussArten.Objekt.id, 0), eingang("aussage", MathematikAnschlussArten.Aussage.id, 1), ausgang("fall", MathematikAnschlussArten.Objekt.id, 0), ausgang("sonst", MathematikAnschlussArten.Objekt.id, 1)),
    )
    val Konjunktion = aussagenOperatorVorlage("mathematik.konjunktion", "Konjunktion", "Verknüpft zwei oder mehr Aussagen mit ∧.")
    val Disjunktion = aussagenOperatorVorlage("mathematik.disjunktion", "Disjunktion", "Verknüpft zwei oder mehr Aussagen mit ∨.")
    val Implikation = KnotenVorlage("mathematik.implikation", "Implikation", "Aussage", "Bildet A ⇒ B.", GraphGröße(220f, 105f), listOf(eingang("a", MathematikAnschlussArten.Aussage.id, 0), eingang("b", MathematikAnschlussArten.Aussage.id, 1), ausgang("aussage", MathematikAnschlussArten.Aussage.id)))
    val Äquivalenz = KnotenVorlage("mathematik.äquivalenz", "Äquivalenz", "Aussage", "Bildet A ⇔ B.", GraphGröße(220f, 105f), listOf(eingang("a", MathematikAnschlussArten.Aussage.id, 0), eingang("b", MathematikAnschlussArten.Aussage.id, 1), ausgang("aussage", MathematikAnschlussArten.Aussage.id)))
    val Adjunktion = KnotenVorlage("mathematik.adjunktion", "Adjunktion", "Aussage", "Bildet die klassische UND-Verknüpfung A & B.", GraphGröße(220f, 105f), listOf(eingang("a", MathematikAnschlussArten.Aussage.id, 0), eingang("b", MathematikAnschlussArten.Aussage.id, 1), ausgang("aussage", MathematikAnschlussArten.Aussage.id)))

    val alle = listOf(Zahl, Variable, Addition, Maximum, Minimum, Multiplikation, Division, Potenz, Wurzel, Logarithmus, Tupel, KomplexAusTupel, Konjugierte, Realteil, Imaginärteil, KomplexerRadius, Winkel, Gleichheit, Wahr, Lüge, Element, Kleiner, Größer, KleinerGleich, GrößerGleich, Teilmenge, Übermenge, TeilOderGleichmenge, ÜberOderGleichmenge, Disjunkt, GleichungLösen, Auswerten, Ableiten, Integrieren, EndlicheMenge, ReellesIntervall, Lösungsmenge, Visualisierung, Vereinigung, Schnitt, Differenz, KartesischesProdukt, NatürlicheZahlen, GanzeZahlen, RationaleZahlen, ReelleZahlen, KomplexeZahlen, Mächtigkeit, IterierteSumme, IteriertesProdukt, IterierteVereinigung, IterierterSchnitt, IteriertesKartesischesProdukt, Abbild, TermZuMethode, Komposition, Iteration, MethodenDifferentieren, MethodenIntegrieren, SpaltenMethodeDifferentieren, ZeilenMethodeDifferentieren, SpaltenMethodeIntegrieren, ZeilenMethodeIntegrieren, Vektor, ZeilenVektor, VektorZuPolynom, TupelZuSpalte, TupelZuZeile, EinheitsSpalte, EinheitsZeile, VektorRadiusSpalte, VektorRadiusZeile, Matrix, Skalarprodukt, SkalarproduktZeile, KreuzproduktSpalte, KreuzproduktZeile, TransponiereSpalte, TransponiereZeile, MatrixProdukt, TransponiereMatrix, MatrixInvertieren, KartenEingang, KartenAusgang, Fall, Konjunktion, Disjunktion, Implikation, Äquivalenz, Adjunktion)

    private fun aussagenVorlage(art: String, name: String, beschreibung: String, links: AnschlussArtId, rechts: AnschlussArtId, kategorie: String = "Aussagen: Aussagenprädikate") = KnotenVorlage(
        art, name, kategorie, beschreibung, GraphGröße(220f, 110f),
        listOf(eingang("links", links, 0), eingang("rechts", rechts, 1), ausgang("aussage", MathematikAnschlussArten.Aussage.id)),
    )

    private fun vergleichVorlage(art: String, name: String, zeichen: String) = aussagenVorlage(art, name, "Vergleicht zwei Zahlterme mit $zeichen.", MathematikAnschlussArten.Zahl.id, MathematikAnschlussArten.Zahl.id, "Aussagen: Zahlenprädikate")
    private fun mengenAussagenVorlage(art: String, name: String, beschreibung: String) = aussagenVorlage(art, name, beschreibung, MathematikAnschlussArten.Menge.id, MathematikAnschlussArten.Menge.id, "Aussagen: Mengenprädikate")
    private fun mengenOperatorVorlage(art: String, name: String, beschreibung: String, zeichen: String) = KnotenVorlage(
        art, name, "Mengen", beschreibung, GraphGröße(230f, 120f),
        listOf(eingang("a", MathematikAnschlussArten.Menge.id, 0, true), eingang("b", MathematikAnschlussArten.Menge.id, 1, true), ausgang("menge", MathematikAnschlussArten.Menge.id)),
        mapOf("festeEingänge" to "2", "operatorAnzeige" to "wert"),
    )
    private fun extremwertVorlage(name: String, beschreibung: String, modus: String) = KnotenVorlage(
        "mathematik.extremwert", name, "Rechnen", beschreibung, GraphGröße(220f, 130f),
        listOf(eingang("a", MathematikAnschlussArten.Zahl.id, 0, true), eingang("b", MathematikAnschlussArten.Zahl.id, 1, true), ausgang("wert", MathematikAnschlussArten.Zahl.id)),
        mapOf("modus" to modus, "festeEingänge" to "2", "operatorAnzeige" to "wert"),
    )
    private fun methodenAnalysisVorlage(art: String, name: String, beschreibung: String, funktion: AnschlussArtId) = KnotenVorlage(art, name, "Abbildungen", beschreibung, GraphGröße(260f, 105f), listOf(eingang("methode", funktion), ausgang("methode", funktion)))
    private fun aussagenOperatorVorlage(art: String, name: String, beschreibung: String) = KnotenVorlage(art, name, "Aussage", beschreibung, GraphGröße(230f, 115f), listOf(eingang("a", MathematikAnschlussArten.Aussage.id, 0, true), eingang("b", MathematikAnschlussArten.Aussage.id, 1, true), ausgang("aussage", MathematikAnschlussArten.Aussage.id)), mapOf("festeEingänge" to "2", "operatorAnzeige" to "wert"))
}

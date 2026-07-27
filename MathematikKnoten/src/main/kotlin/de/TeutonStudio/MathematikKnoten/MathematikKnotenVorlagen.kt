package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

object MathematikKnotenVorlagen {
    private fun eingang(name: String, art: AnschlussArtId, reihe: Int = 0, erweiterbar: Boolean = false) = AnschlussDaten(name = name, richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = art, reihenfolge = reihe, kannSichErweitern = erweiterbar)
    private fun ausgang(name: String, art: AnschlussArtId, reihe: Int = 0) = AnschlussDaten(name = name, richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = art, reihenfolge = reihe)

    val Zahl = KnotenVorlage(
        "mathematik.zahl", "Zahl", "Rechnen", "Exakte ganze oder rationale Zahl.", GraphGröße(180f, 92f),
        listOf(ausgang("wert", MathematikAnschlussArten.Zahl.id)), mapOf("wert" to "2"),
    )
    val Variable = KnotenVorlage(
        "mathematik.variable", "Variable", "Rechnen", "Freie mathematische Variable.", GraphGröße(180f, 92f),
        listOf(ausgang("wert", MathematikAnschlussArten.Zahl.id)), mapOf("name" to "x"),
    )
    val Addition = KnotenVorlage(
        "mathematik.addition", "Addition", "Rechnen", "Addiert zwei oder mehr Zahlterme.", GraphGröße(220f, 130f),
        listOf(eingang("a", MathematikAnschlussArten.Zahl.id, 0, true), eingang("b", MathematikAnschlussArten.Zahl.id, 1, true), ausgang("wert", MathematikAnschlussArten.Zahl.id)),
        mapOf("festeEingänge" to "2", "operatorAnzeige" to "wert"),
    )
    val Multiplikation = KnotenVorlage(
        "mathematik.multiplikation", "Multiplikation", "Rechnen", "Multipliziert zwei oder mehr Zahlterme.", GraphGröße(220f, 130f),
        listOf(eingang("a", MathematikAnschlussArten.Zahl.id, 0), eingang("b", MathematikAnschlussArten.Zahl.id, 1), eingang("c", MathematikAnschlussArten.Zahl.id, 2), ausgang("wert", MathematikAnschlussArten.Zahl.id)),
    )
    val Division = KnotenVorlage(
        "mathematik.division", "Division", "Rechnen", "Teilt einen Zahlterm durch einen anderen.", GraphGröße(220f, 110f),
        listOf(eingang("dividend", MathematikAnschlussArten.Zahl.id, 0), eingang("divisor", MathematikAnschlussArten.Zahl.id, 1), ausgang("wert", MathematikAnschlussArten.Zahl.id)),
    )
    val Potenz = KnotenVorlage(
        "mathematik.potenz", "Potenz", "Rechnen", "Potenz aus Basis und Exponent.", GraphGröße(210f, 110f),
        listOf(eingang("basis", MathematikAnschlussArten.Zahl.id, 0), eingang("exponent", MathematikAnschlussArten.Zahl.id, 1), ausgang("wert", MathematikAnschlussArten.Zahl.id)),
    )
    val Gleichheit = KnotenVorlage(
        "mathematik.gleichheit", "Gleichheit", "Aussagen", "Vergleicht zwei mathematische Objekte.", GraphGröße(220f, 110f),
        listOf(eingang("links", MathematikAnschlussArten.Objekt.id, 0), eingang("rechts", MathematikAnschlussArten.Objekt.id, 1), ausgang("aussage", MathematikAnschlussArten.Aussage.id)),
    )
    val GleichungLösen = KnotenVorlage(
        "mathematik.gleichungLösen", "Gleichung lösen", "Algebra", "Löst eine lineare Gleichung nach der gewählten Variablen.", GraphGröße(240f, 115f),
        listOf(eingang("gleichung", MathematikAnschlussArten.Aussage.id), ausgang("lösungen", MathematikAnschlussArten.Menge.id)), mapOf("variable" to "x"),
    )
    val Auswerten = KnotenVorlage(
        "mathematik.auswerten", "Auswerten", "Steuerung", "Vereinfacht einen Term oder entscheidet eine Aussage.", GraphGröße(230f, 110f),
        listOf(eingang("objekt", MathematikAnschlussArten.Objekt.id), ausgang("wert", MathematikAnschlussArten.Objekt.id)),
    )
    val Ableiten = KnotenVorlage(
        "mathematik.ableiten", "Ableiten", "Analysis", "Leitet einen Zahlterm symbolisch ab.", GraphGröße(220f, 110f),
        listOf(eingang("term", MathematikAnschlussArten.Zahl.id), ausgang("wert", MathematikAnschlussArten.Zahl.id)), mapOf("variable" to "x"),
    )
    val Integrieren = KnotenVorlage(
        "mathematik.integrieren", "Integrieren", "Analysis", "Bestimmt eine elementare Stammfunktion.", GraphGröße(220f, 110f),
        listOf(eingang("term", MathematikAnschlussArten.Zahl.id), ausgang("wert", MathematikAnschlussArten.Zahl.id)), mapOf("variable" to "x"),
    )
    val EndlicheMenge = KnotenVorlage(
        "mathematik.endlicheMenge", "Endliche Menge", "Mengen", "Endliche Menge aus einer kommagetrennten Liste.", GraphGröße(220f, 105f),
        listOf(ausgang("menge", MathematikAnschlussArten.Menge.id)), mapOf("elemente" to "1,2,3"),
    )
    val Vereinigung = KnotenVorlage(
        "mathematik.vereinigung", "Vereinigung", "Mengen", "Vereinigt zwei Mengen.", GraphGröße(220f, 110f),
        listOf(eingang("a", MathematikAnschlussArten.Menge.id, 0, true), eingang("b", MathematikAnschlussArten.Menge.id, 1, true), ausgang("menge", MathematikAnschlussArten.Menge.id)),
        mapOf("festeEingänge" to "2", "operatorAnzeige" to "wert"),
    )
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
    val Vektor = KnotenVorlage(
        "mathematik.vektor", "Vektor", "Lineare Algebra", "Vektor aus exakten Zahlen.", GraphGröße(220f, 105f),
        listOf(ausgang("vektor", MathematikAnschlussArten.Vektor.id)), mapOf("werte" to "1,2,3"),
    )
    val Matrix = KnotenVorlage(
        "mathematik.matrix", "Matrix", "Lineare Algebra", "Matrix: Zeilen mit Semikolon, Werte mit Komma trennen.", GraphGröße(250f, 115f),
        listOf(ausgang("matrix", MathematikAnschlussArten.Matrix.id)), mapOf("werte" to "1,0;0,1"),
    )
    val Skalarprodukt = KnotenVorlage(
        "mathematik.skalarprodukt", "Skalarprodukt", "Lineare Algebra", "Berechnet das Skalarprodukt zweier Vektoren.", GraphGröße(230f, 110f),
        listOf(eingang("a", MathematikAnschlussArten.Vektor.id, 0), eingang("b", MathematikAnschlussArten.Vektor.id, 1), ausgang("wert", MathematikAnschlussArten.Zahl.id)),
    )
    val MatrixInvertieren = KnotenVorlage(
        "mathematik.matrixInvertieren", "Matrix invertieren", "Lineare Algebra", "Invertiert eine rationale quadratische Matrix exakt.", GraphGröße(245f, 110f),
        listOf(eingang("matrix", MathematikAnschlussArten.Matrix.id), ausgang("inverse", MathematikAnschlussArten.Matrix.id)),
    )
    val KartenEingang = KnotenVorlage(
        "mathematik.kartenEingang", "Karten-Eingang", "Gruppen", "Öffentlicher Parameter einer wiederverwendbaren Karte.", GraphGröße(210f, 100f),
        listOf(ausgang("wert", MathematikAnschlussArten.Objekt.id)), mapOf("name" to "x"),
    )
    val KartenAusgang = KnotenVorlage(
        "mathematik.kartenAusgang", "Karten-Ausgang", "Gruppen", "Öffentliche Ausgabe einer wiederverwendbaren Karte.", GraphGröße(210f, 100f),
        listOf(eingang("wert", MathematikAnschlussArten.Objekt.id), eingang("zielmenge", MathematikAnschlussArten.Menge.id, 1)), mapOf("name" to "ergebnis"),
    )
    val Fall = KnotenVorlage(
        "mathematik.fall", "Fallunterscheidung", "Steuerung", "Verzweigt einen Ausdruck mit weitergegebenen Annahmen.", GraphGröße(250f, 140f),
        listOf(eingang("term", MathematikAnschlussArten.Objekt.id), ausgang("fall", MathematikAnschlussArten.Objekt.id, 0), ausgang("sonst", MathematikAnschlussArten.Objekt.id, 1)),
        mapOf("modus" to "verzweigen", "bedingung" to "x=0"),
    )

    val alle = listOf(Zahl, Variable, Addition, Multiplikation, Division, Potenz, Gleichheit, GleichungLösen, Auswerten, Ableiten, Integrieren, EndlicheMenge, Vereinigung, NatürlicheZahlen, GanzeZahlen, RationaleZahlen, ReelleZahlen, IterierteSumme, IteriertesProdukt, IterierteVereinigung, IterierterSchnitt, Vektor, Matrix, Skalarprodukt, MatrixInvertieren, KartenEingang, KartenAusgang, Fall)
}

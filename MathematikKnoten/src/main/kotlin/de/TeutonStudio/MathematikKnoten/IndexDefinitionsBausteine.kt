package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

/** Adapter für reine Form-, Komponenten- und bestehende Gleichungssystemprimitive. */
internal fun indexDefinitionsBaustein(k: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis? {
    val modus = k.knoten.parameter["indexmethode.baustein"] ?: return null
    val wert = k.eingänge["wert"] ?: error("Der Bausteineingang fehlt.")
    val quelle = wert.objekt as? MathematischesObjekt ?: error("Ein mathematischer Bausteinwert fehlt.")
    val quellen = k.eingänge.values.flatMap { it.variablenQuellen }.distinctBy { it.identität }
    val annahmen = k.eingänge.values.flatMap { it.annahmen }.toSet()
    fun matrix() = matrixAusZweiachsigerStruktur(quelle)
    var neueQuelle: VariablenQuelle? = null
    val ergebnis: MathematischesObjekt = when {
        modus.startsWith("materialisieren:") -> strukturAusIndexMethode(quelle as? MathematischeMethode ?: error("Die Komponentenabbildung fehlt."), modus.substringAfter(':'), k.eingänge["vorbild"]?.objekt as? MathematischesObjekt)
        modus == "diagonalKomponenten" -> diagonaleKomponentenMethode(quelle)
        modus == "potenzQuotient" -> RationaleZahl.von((nichtnegativerGanzzahlExponent(quelle) / 2).toLong())
        modus == "potenzRest" -> RationaleZahl.von((nichtnegativerGanzzahlExponent(quelle) % 2).toLong())
        modus == "kommutativerVektor" -> {
            val werte = when (quelle) { is OrientierterVektor -> quelle.werte; is Tupel -> quelle.elemente.map { it as? ZahlAusdruck ?: error("Vektorkomponenten müssen Zahlen sein.") }; else -> error("Ein Vektor fehlt.") }
            val vorräte = quellen.associate { it.name to it.werteVorrat }
            require(werte.none { inferiereZahlenWertevorrat(it, vorräte, annahmen).fundamentalerZahlbereichOderNull() == FundamentalerZahlbereich.QUATERNION }) { "Vektorpotenz unterstützt reelle und komplexe Vektoren." }
            quelle
        }
        modus.startsWith("schnitte:") -> tensorZerlegeMethode(quelle, modus.substringAfter(':').toInt(), (quelle as? Tensorartig)?.tensorForm?.filterIndexed { i, _ -> i != modus.substringAfter(':').toInt() }?.map { RationaleZahl.von(it.toLong()) } ?: (quelle as? TypisiertesElement)?.strukturForm?.filterIndexed { i, _ -> i != modus.substringAfter(':').toInt() }).let { m ->
            var name = "i"; val belegt = quelle.enthalteneMethodenParameter().map { it.name }.toSet()
            while (name in belegt) name += "_"
            m.benenneIndexParameter(listOf(name))
        }
        modus == "polynomQuelle" -> matrix().also { a ->
            require(a.zeilenAnzahl == a.spaltenAnzahl && a.zeilenAnzahl <= 8) { "Die Gleichungssystemdefinition materialisiert Minimalpolynome bis Dimension 8; der direkte Rechner bleibt unverändert." }
            require(a.zeilen.flatten().all { vereinfache(it) is RationaleZahl }) { "Die exakte Minimalpolynomdefinition benötigt rationale Matrixeinträge." }
        }
        modus == "spaltenSammeln" -> spaltenAusStrukturen(quelle as? Tupel ?: error("Die Spaltensammlung benötigt ein Tupel."))
        modus == "vektorisieren" -> SpaltenVektor(matrix().zeilen.flatten())
        modus == "partikulaereLoesung" -> partikulaereLineareLoesung(matrix(), k.eingänge["rechts"]?.objekt as? SpaltenVektor ?: error("Die rechte Seite fehlt."))
        modus == "monomIndex" -> {
            val v = quelle as? OrientierterVektor ?: error("Die Koeffizienten fehlen.")
            val variable = k.eingänge["variable"]?.objekt as? ZahlAusdruck ?: error("Die Polynomvariable fehlt.")
            val frei = k.eingänge.values.flatMap { (it.objekt as? MathematischesObjekt)?.enthalteneMethodenParameter().orEmpty() }.map { it.name }
            var prefix = "__index"; while (frei.any { it.startsWith(prefix) }) prefix += "_"
            val i = Variable("$prefix::k", "k")
            MathematischeMethode("Monome", listOf(i), Potenz(variable, subtraktion(i, RationaleZahl.Eins)), RationaleZahlen,
                mapOf(i.name to EndlicheIndexMenge(RationaleZahl.von(v.werte.size.toLong()))))
        }
        modus == "alsMatrix" -> matrix()
        modus == "hermitescheProjektoren" -> konjugiertTransponierteProjektorKomponenten(quelle as? Tensorartig ?: error("Die Projektorfamilie fehlt."))
        modus == "orthogonaleProjektoren" -> orthogonaleProjektorBedingungen(quelle as? Tensorartig ?: error("Die Projektorfamilie fehlt."))
        modus == "einheitSpalten" -> identitaetsMatrix(matrix().spaltenAnzahl)
        modus == "nullMatrix" -> nullKomponenten(matrix())
        modus == "konjugiertTransponiert" -> konjugiertTransponierteKomponenten(matrix())
        modus == "unteresDreieck" -> untereDreiecksKomponenten(matrix())
        modus == "ausserdiagonal" -> ausserdiagonaleKomponenten(matrix())
        modus == "nichtnullSpalten" -> nichtverschwindendeSpaltenBedingungen(matrix())
        modus == "vektorfeldKomponenten" -> vektorfeldKomponentenMethoden(quelle as? Methode ?: error("Das Vektorfeld muss eine Methode sein."))
        modus == "jordanBedingungen" -> jordanFormBedingungen(matrix())
        modus == "nichtnegativeDiagonale" -> nichtnegativeDiagonaleBedingungen(matrix())
        modus.startsWith("kandidat:") -> {
            val a = matrix()
            require(maxOf(a.zeilenAnzahl, a.spaltenAnzahl) <= 7) { "Die ausführbaren Kandidatenbedingungen materialisieren Formen bis 7; es wird kein Faktorisierungssolver behauptet." }
            val m = a.zeilenAnzahl; val n = a.spaltenAnzahl; val r = minOf(m, n)
            val rolle = modus.substringAfter(':')
            val form = when (rolle) {
                "Q" -> listOf(m, r); "R" -> listOf(r, n); "U" -> listOf(m, m); "Sigma" -> listOf(m, n)
                "V" -> listOf(n, n); "lambdas" -> listOf(n); "Projektoren" -> listOf(n, n, n)
                "Lambda", "P", "J" -> { require(m == n) { "Diese Kandidatenbedingungen benötigen eine quadratische Matrix." }; listOf(n, n) }
                else -> error("Unbekannte Kandidatenrolle.")
            }
            val belegt = k.eingänge.values.flatMap { (it.objekt as? MathematischesObjekt)?.enthalteneMethodenParameter().orEmpty() }.map { it.name }.toSet()
            var name = rolle; while (name in belegt) name += "_"
            val typ = when (form.size) { 1 -> MathematikAnschlussArten.SpaltenVektor.id; 2 -> MathematikAnschlussArten.Matrix.id; else -> MathematikAnschlussArten.Tensor.id }
            val parameter = TypisiertesElement(name, typ.wert, strukturForm = form.map { RationaleZahl.von(it.toLong()) })
            val bereich = if (rolle == "Sigma") ReelleZahlen else KomplexeZahlen
            val domain = when (form.size) { 1 -> Vektorraum(VektorOrientierung.Spalte, form[0], bereich); 2 -> Matrizenraum(form[0], form[1], bereich); else -> Tensorraum(bereich, form.map { RationaleZahl.von(it.toLong()) }) }
            neueQuelle = VariablenQuelle(k.knoten.id, name, domain)
            strukturParameterKomponenten(parameter, form)
        }
        else -> error("Unbekannter Definitionsbaustein '$modus'.")
    }
    return KnotenAuswertungsErgebnis(mapOf("ergebnis" to BedingterWert(ergebnis, annahmen,
        variablenQuellen = quellen + listOfNotNull(neueQuelle))), eingänge = k.eingänge)
}

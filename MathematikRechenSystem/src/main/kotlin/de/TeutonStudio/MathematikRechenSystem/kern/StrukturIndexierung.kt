package de.TeutonStudio.MathematikRechenSystem.kern

/** Endlicher 1-basierter Indexbereich `{1,…,n}`; n=0 bezeichnet die leere Menge. */
private fun ZahlAusdruck.nichtnegativerIndexUmfang():Int? {
    val n=vereinfache(this) as? RationaleZahl ?: return null
    return n.takeIf { it.nenner==java.math.BigInteger.ONE && it.zähler.signum()>=0 && it.zähler.bitLength()<31 }?.zähler?.toInt()
}

data class EndlicheIndexMenge(val obergrenze: ZahlAusdruck) : MengenAusdruck {
    init {
        require(obergrenze !is RationaleZahl || obergrenze.nichtnegativerIndexUmfang() != null) {
            "Die obere Grenze einer endlichen Indexmenge muss nichtnegativ und ganzzahlig sein."
        }
    }

    override fun zuLatex(): String = "\\left\\{1,\\ldots,${obergrenze.zuLatex()}\\right\\}"
}

/** Symbolische Achsenlänge einer Quelle, deren konkrete Form noch nicht bekannt ist. */
data class StrukturAchsenLaenge(
    val quelle: MathematischesObjekt,
    /** Nullbasierte interne Achse. */
    val achse: Int,
) : ZahlAusdruck {
    init { require(achse >= 0) { "Die Strukturachse darf nicht negativ sein." } }
    override fun zuLatex(): String = "\\dim_{${achse + 1}}\\left(${quelle.zuLatex()}\\right)"
}

enum class StrukturZugriffsArt { Komponente, Zeile, Spalte, Schnitt }

/** Symbolischer allgemeiner Komponenten- oder Schnittzugriff. */
data class SymbolischerStrukturZugriff(
    val quelle: MathematischesObjekt,
    val index: ZahlAusdruck,
    val art: StrukturZugriffsArt,
    /** Nullbasierte interne Achse. */
    val achse: Int = 0,
    val ergebnisAnschlussArt: String,
    val ergebnisForm: List<ZahlAusdruck>? = null,
) : Ausdruck {
    init { require(achse >= 0) }
    override fun zuLatex(): String = when (art) {
        StrukturZugriffsArt.Komponente -> "${quelle.zuLatex()}_{${index.zuLatex()}}"
        StrukturZugriffsArt.Zeile -> "\\operatorname{Zeile}_{${index.zuLatex()}}\\left(${quelle.zuLatex()}\\right)"
        StrukturZugriffsArt.Spalte -> "\\operatorname{Spalte}_{${index.zuLatex()}}\\left(${quelle.zuLatex()}\\right)"
        StrukturZugriffsArt.Schnitt -> "\\operatorname{Schnitt}_{${achse + 1},${index.zuLatex()}}\\left(${quelle.zuLatex()}\\right)"
    }
}

/** Numerischer Sonderfall, damit symbolische Vektorkomponenten Zahlterme bleiben. */
data class SymbolischeZahlKomponente(
    val quelle: MathematischesObjekt,
    val index: ZahlAusdruck,
    val weitereIndizes: List<ZahlAusdruck> = emptyList(),
) : ZahlAusdruck {
    override fun zuLatex(): String = "${quelle.zuLatex()}_{${(listOf(index) + weitereIndizes).joinToString(",") { it.zuLatex() }}}"
}

/** Zielmenge einer strukturerhaltend typisierten Indexmethode. */
data class StrukturErgebnisMenge(
    val anschlussArt: String,
    val form: List<ZahlAusdruck>? = null,
    val bezeichnung: String,
) : MengenAusdruck {
    init { require(anschlussArt.isNotBlank() && bezeichnung.isNotBlank()) }
    override fun zuLatex(): String = bezeichnung
}

fun strukturAchsenLaenge(quelle: MathematischesObjekt, achse: Int): ZahlAusdruck {
    require(achse >= 0)
    val konkreteForm = when (quelle) {
        is Tensorartig -> quelle.tensorForm.map { RationaleZahl.von(it.toLong()) }
        is Tupel -> listOf(RationaleZahl.von(quelle.elemente.size.toLong()))
        is TypisiertesElement -> quelle.strukturForm
        else -> null
    }
    konkreteForm?.let { require(achse in it.indices) { "Die Struktur besitzt keine Achse ${achse + 1}." } }
    return konkreteForm?.get(achse) ?: StrukturAchsenLaenge(quelle, achse)
}

fun strukturKomponente(
    quelle: MathematischesObjekt,
    index: ZahlAusdruck,
    numerisch: Boolean,
): MathematischesObjekt {
    val konkret = index.konkreterEinsbasierterIndexOderNull()
    if (konkret != null) {
        val wert = when (quelle) {
            is Tupel -> quelle.elemente.getOrNull(konkret)
            is OrientierterVektor -> quelle.werte.getOrNull(konkret)
            else -> null
        }
        if (wert != null) return wert
        pruefeSymbolischeGrenze(quelle, 0, konkret)
    }
    return if (numerisch) SymbolischeZahlKomponente(quelle, index)
    else SymbolischerStrukturZugriff(quelle, index, StrukturZugriffsArt.Komponente, ergebnisAnschlussArt = "objekt")
}

fun strukturSchnitt(
    quelle: MathematischesObjekt,
    index: ZahlAusdruck,
    art: StrukturZugriffsArt,
    achse: Int,
    ergebnisAnschlussArt: String,
    ergebnisForm: List<ZahlAusdruck>? = null,
): MathematischesObjekt {
    require(art != StrukturZugriffsArt.Komponente)
    val konkret = index.konkreterEinsbasierterIndexOderNull()
    if (konkret != null) {
        when (quelle) {
            is Matrix -> {
                val schnitt = when (art) {
                StrukturZugriffsArt.Zeile -> ZeilenVektor(quelle.zeilen.getOrElse(konkret) { ausserhalb(konkret, quelle.zeilenAnzahl) })
                StrukturZugriffsArt.Spalte -> SpaltenVektor(quelle.zeilen.map { it.getOrElse(konkret) { ausserhalb(konkret, quelle.spaltenAnzahl) } })
                else -> tensorSchnitt(quelle, achse, konkret)
                }
                return if (ergebnisAnschlussArt == "mathematik.tupel" && schnitt is OrientierterVektor) Tupel(schnitt.werte) else schnitt
            }
            is Tensorartig -> return tensorSchnitt(quelle, achse, konkret)
            else -> pruefeSymbolischeGrenze(quelle, achse, konkret)
        }
    }
    return SymbolischerStrukturZugriff(quelle, index, art, achse, ergebnisAnschlussArt, ergebnisForm)
}

private fun tensorSchnitt(quelle: Tensorartig, achse: Int, index: Int): MathematischesObjekt {
    require(achse in quelle.tensorForm.indices) { "Die Struktur besitzt keine Achse ${achse + 1}." }
    require(index in 0 until quelle.tensorForm[achse]) {
        "Index ${index + 1} liegt außerhalb von {1,…,${quelle.tensorForm[achse]}}."
    }
    val restForm = quelle.tensorForm.filterIndexed { position, _ -> position != achse }
    if (restForm.isEmpty()) return quelle.tensorKomponente(listOf(index))
    val werte = restForm.indizesFolge().map { restIndizes ->
        val voll = restIndizes.toMutableList().also { it.add(achse, index) }
        quelle.tensorKomponente(voll)
    }
    return when (restForm.size) {
        1 -> Tensor(restForm, werte)
        2 -> Matrix(List(restForm[0]) { z -> List(restForm[1]) { s -> werte[z * restForm[1] + s] } })
        else -> Tensor(restForm, werte)
    }
}

private fun ZahlAusdruck.konkreterEinsbasierterIndexOderNull(): Int? {
    val rational = this as? RationaleZahl ?: return null
    require(rational.nenner == java.math.BigInteger.ONE && rational.zähler.signum() > 0 && rational.zähler.bitLength() < 31) {
        "Ein Strukturindex muss eine positive ganze Zahl sein."
    }
    return rational.zähler.toInt() - 1
}

private fun pruefeSymbolischeGrenze(quelle: MathematischesObjekt, achse: Int, index: Int) {
    val grenze = runCatching { strukturAchsenLaenge(quelle, achse).positiveGanzeZahlOderNull() }.getOrNull()
    if (grenze != null) require(index in 0 until grenze) {
        "Index ${index + 1} liegt außerhalb von {1,…,$grenze}."
    }
}

private fun ausserhalb(index: Int, grenze: Int): Nothing =
    throw IndexOutOfBoundsException("Index ${index + 1} liegt außerhalb von {1,…,$grenze}.")

/** Komponentenabruf mit einem 1-basierten Index je Strukturachse. */
data class IndexKomponente(val quelle: MathematischesObjekt, val indizes: List<ZahlAusdruck>) : Ausdruck {
    override fun zuLatex() = "${quelle.zuLatex()}_{${indizes.joinToString(",") { it.zuLatex() }}}"
}

fun indexKomponente(quelle: MathematischesObjekt, indizes: List<ZahlAusdruck>): MathematischesObjekt {
    if(quelle is TypisiertesElement && quelle.anschlussArt in setOf("mathematik.matrix","mathematik.tensor","mathematik.tupel","mathematik.vektor.zeile","mathematik.vektor.spalte")) {
        quelle.strukturForm?.let { form -> require(indizes.size==form.size) { "Die Indexanzahl passt nicht zur symbolischen Strukturform." } }
        return IndexKomponente(quelle,indizes)
    }
    val form = when (quelle) {
        is Tupel -> listOf(quelle.elemente.size)
        is OrientierterVektor -> listOf(quelle.werte.size)
        is Tensorartig -> quelle.tensorForm
        else -> error("Indexmethoden benötigen ein Tupel, einen Vektor, eine Matrix oder einen Tensor.")
    }
    require(indizes.size == form.size) { "Erwartet werden ${form.size} Indizes, erhalten: ${indizes.size}." }
    val konkret = indizes.mapIndexed { achse, index ->
        index.konkreterEinsbasierterIndexOderNull()?.also {
            require(it in 0 until form[achse]) { "Index ${it + 1} liegt außerhalb von {1,…,${form[achse]}}." }
        }
    }
    if (konkret.any { it == null }) return IndexKomponente(quelle, indizes)
    val positionen = konkret.filterNotNull()
    return when (quelle) {
        is Tupel -> quelle.elemente[positionen.single()]
        is OrientierterVektor -> quelle.werte[positionen.single()]
        is Tensorartig -> quelle.tensorKomponente(positionen)
        else -> error("Nicht unterstützte Struktur.")
    }
}

fun strukturIndexMethode(
    quelle: MathematischesObjekt,
    werteVorräte: Map<String, MengenAusdruck> = emptyMap(),
    annahmen: Set<Aussage> = emptySet(),
    formVorbild: MathematischesObjekt? = null,
): MathematischeMethode {
    if(quelle is TypisiertesElement && formVorbild==null) {
        val rang=when(quelle.anschlussArt) {
            "mathematik.vektor.spalte", "mathematik.vektor.zeile" -> 1
            "mathematik.matrix" -> 2
            "mathematik.tensor" -> quelle.strukturForm?.size ?: error("Der Tensorrang muss bekannt sein.")
            else -> error("Der Quelltyp besitzt keine Indexstruktur.")
        }
        val belegt=quelle.enthalteneMethodenParameter().map { it.name }.toMutableSet()
        val parameter=(0 until rang).map { achse ->
            var name="i${achse+1}";while(!belegt.add(name)) name+="_"
            Variable(name)
        }
        return MathematischeMethode("index",parameter,
            if(quelle.anschlussArt=="mathematik.tupel") IndexKomponente(quelle,parameter) else SymbolischeZahlKomponente(quelle,parameter.first(),parameter.drop(1)),
            Tupelraum(listOf(inferiereZahlenWertevorrat(SymbolischeZahlKomponente(quelle,parameter.first(),parameter.drop(1)),werteVorräte,annahmen))),parameter.mapIndexed { i,p -> p.name to EndlicheIndexMenge(strukturAchsenLaenge(quelle,i)) }.toMap())
    }
    val struktur = if(quelle is Tupel || quelle is OrientierterVektor || quelle is Tensorartig) quelle else formVorbild ?: quelle
    val form = when (struktur) {
        is Tupel -> listOf(struktur.elemente.size)
        is OrientierterVektor -> listOf(struktur.werte.size)
        is Tensorartig -> struktur.tensorForm
        else -> error("Indexmethoden benötigen ein Tupel, einen Vektor, eine Matrix oder einen Tensor.")
    }
    val belegt = quelle.enthalteneMethodenParameter().mapTo(mutableSetOf()) { it.name }
    val parameter = form.indices.map { achse ->
        val basis = "i${achse + 1}"
        var name = basis
        var suffix = 1
        while (!belegt.add(name)) name = "${basis}_${suffix++}"
        Variable(name)
    }
    val elemente = when (struktur) {
        is Tupel -> struktur.elemente
        is OrientierterVektor -> struktur.werte
        is Tensorartig -> struktur.tensorForm.indizesFolge().map { struktur.tensorKomponente(it) }
        else -> emptyList()
    }
    val numerisch = elemente.isNotEmpty() && elemente.all { it is ZahlAusdruck }
    val mengen = elemente.map { inferiereZielmenge(it, werteVorräte, annahmen) }
    val ziel = if (numerisch) maximaleZahlenGrundmenge(mengen) else vereinige(mengen)
    // Der Zugriff bleibt ein Ausdruck, auch wenn er später ein verschachteltes Tupel liefert.
    return MathematischeMethode("index", parameter,
        if (numerisch && parameter.isNotEmpty()) SymbolischeZahlKomponente(quelle, parameter.first(), parameter.drop(1)) else IndexKomponente(quelle, parameter),
        Tupelraum(listOf(ziel)), parameter.mapIndexed { i, p ->
            p.name to EndlicheIndexMenge(RationaleZahl.von(form[i].toLong()))
        }.toMap())
}

/** Ändert ausschließlich die Namen der Indexargumente, ohne Außenvariablen einzufangen. */
fun MathematischeMethode.benenneIndexParameter(namen: List<String>, anzeigen: List<String> = namen): MathematischeMethode {
    if (namen.isEmpty()) return this
    require(namen.size == parameter.size && namen.distinct().size == namen.size && namen.all { it.isNotBlank() }) {
        "Indexparameter benötigen je Achse einen eindeutigen Namen."
    }
    val aussen = vorschrift.enthalteneMethodenParameter().map { it.name }.toSet() - parameter.map { it.name }.toSet()
    require(namen.none { it in aussen }) { "Ein Indexname würde eine Außenvariable binden." }
    require(anzeigen.size==namen.size) { "Jeder Index benötigt einen Anzeigenamen." }
    val neu = namen.mapIndexed { i,name -> Variable(name,latex=anzeigen[i]) }
    val bindungen = parameter.map { it.name }.zip(neu).toMap()
    return copy(parameter = neu, vorschrift = ersetze(vorschrift, bindungen),
        werteVorräte = parameter.mapIndexed { i, p -> namen[i] to (ersetze(werteVorräte.getValue(p.name), bindungen) as MengenAusdruck) }.toMap())
}

/** Endliche Indexsumme über genau eine Achse; übrige Argumente bleiben frei. */
fun summiereIndexParameter(methode: MathematischeMethode, name: String, aggregation: String = "summe"): MathematischesObjekt {
    require(methode.parameter.any { it.name == name }) { "Unbekannter Summationsindex '$name'." }
    val bereich = methode.werteVorräte[name] as? EndlicheIndexMenge ?: error("Die Indexsumme benötigt einen endlichen Indexbereich.")
    val n = bereich.obergrenze.nichtnegativerIndexUmfang() ?: error("Die Indexgrenze muss konkret und nichtnegativ sein.")
    require(n <= 100_000) { "Die Indexsumme überschreitet das Materialisierungsbudget." }
    val term = methode.vorschriftFür(methode.ausgabeNamen.single()) as? ZahlAusdruck ?: error("Die Indexsumme benötigt Zahlkomponenten.")
    val terme = (1..n).map { ersetze(term, mapOf(name to RationaleZahl.von(it.toLong()))) }
    val summe = when (aggregation) {
        "summe" -> addition(terme)
        "produkt" -> multiplikation(terme)
        "maximum" -> { require(terme.isNotEmpty()) { "Maximum einer leeren Indexmenge ist nicht definiert." }; maximum(terme) }
        else -> error("Unbekannte Indexreduktion '$aggregation'.")
    }
    val rest = methode.parameter.filterNot { it.name == name }
    return if (rest.isEmpty()) vereinfache(summe) else MathematischeMethode(methode.name, rest, summe,
        methode.zielMenge, methode.werteVorräte - name)
}

/** Rekonstruiert eine endliche Struktur aus ihrer vollständigen Komponentenabbildung. */
fun strukturAusIndexMethode(methode: MathematischeMethode, art: String, vorbild: MathematischesObjekt? = null): MathematischesObjekt {
    val form = methode.parameter.map { p ->
        val bereich = methode.werteVorräte[p.name] as? EndlicheIndexMenge
            ?: error("Das Argument '${p.name}' besitzt keinen endlichen Indexbereich.")
        bereich.obergrenze.nichtnegativerIndexUmfang() ?: error("Die Struktur benötigt konkrete Achsenlängen.")
    }
    require(form.fold(1L) { a, n -> a * n }.let { it in 0..100_000 }) { "Die Struktur überschreitet das Materialisierungsbudget." }
    fun definiert(objekt: MathematischesObjekt): MathematischesObjekt {
        require(definitionsBedingungen(objekt).none { it.entscheide().wahrheitswert == Wahrheitswert.Lüge }) { "Eine Komponente ist nicht definiert (beispielsweise Division durch null)." }
        return objekt
    }
    if (form.isEmpty()) return definiert(methode.wendeAn(emptyList()))
    if (form == listOf(0) && (art == "tupel" || art == "vorbild" && vorbild is Tupel)) return Tupel(emptyList())
    val elemente = form.indizesFolge().map { indizes -> definiert(methode.wendeAn(indizes.map { RationaleZahl.von((it + 1).toLong()) })) }
    val ziel = if (art == "vorbild") when (vorbild) {
        is ZeilenVektor -> "zeile"
        is SpaltenVektor -> "spalte"
        is Matrix -> "matrix"
        is Tensor -> "tensor"
        else -> "tupel"
    } else art
    if (ziel == "tupel") {
        require(form.size == 1) { "Ein Tupel benötigt genau eine Indexachse." }
        return Tupel(elemente)
    }
    val zahlen = elemente.map { it as? ZahlAusdruck ?: error("Diese Struktur benötigt ausschließlich Zahlkomponenten.") }
    return when (ziel) {
        "zeile" -> { require(form.size == 1); ZeilenVektor(zahlen) }
        "spalte" -> { require(form.size == 1); SpaltenVektor(zahlen) }
        "matrix" -> { require(form.size == 2); Matrix(List(form[0]) { z -> List(form[1]) { s -> zahlen[z * form[1] + s] } }) }
        "tensor" -> Tensor(form, zahlen)
        else -> error("Unbekannte Indexstruktur '$ziel'.")
    }
}

/** Transformiert die Argumente einer Komponentenabbildung, ohne die Quellstruktur auszuwerten. */
fun MathematischeMethode.transformiereIndexAchsen(modus: String, achsen: List<Int>): MathematischeMethode {
    val namen = parameter.map { it.name }
    fun achse(i: Int): Int { require(i in 1..parameter.size) { "Achse $i liegt außerhalb von {1,…,${parameter.size}}." }; return i - 1 }
    return when (modus) {
        "permutation" -> {
            require(achsen.size == parameter.size && achsen.distinct().size == parameter.size) { "Eine Permutation muss jede Achse genau einmal enthalten." }
            copy(parameter = achsen.map { parameter[achse(it)] })
        }
        "diagonale" -> {
            require(achsen.size == 2 && achsen[0] != achsen[1]) { "Die Diagonale benötigt zwei verschiedene Achsen." }
            val a = achse(achsen[0]); val b = achse(achsen[1])
            require(werteVorräte.getValue(namen[a]) == werteVorräte.getValue(namen[b])) { "Kontraktionsachsen müssen gleich lang sein." }
            copy(parameter = parameter.filterIndexed { i, _ -> i != a && i != b } + parameter[a],
                vorschrift = ersetze(vorschrift, mapOf(namen[b] to parameter[a])), werteVorräte = werteVorräte - namen[b])
        }
        "hauptdiagonale", "gegendiagonale" -> {
            require(parameter.size==2) { "Die Gegendiagonale benötigt eine Matrix." }
            val n=(werteVorräte.getValue(namen[1]) as? EndlicheIndexMenge)?.obergrenze ?: error("Die Achse benötigt einen endlichen Indexbereich.")
            val m=(werteVorräte.getValue(namen[0]) as? EndlicheIndexMenge)?.obergrenze ?: error("Die Achse benötigt einen endlichen Indexbereich.")
            copy(parameter=listOf(parameter[0]),vorschrift=ersetze(vorschrift,mapOf(namen[1] to if(modus=="hauptdiagonale") Variable(namen[0]) else addition(n,RationaleZahl.Eins,negation(Variable(namen[0]))))),werteVorräte=mapOf(namen[0] to EndlicheIndexMenge(minimum(listOf(m,n)))))
        }
        "schnitt" -> {
            require(achsen.size == 2) { "Ein Schnitt benötigt Achse und Komponentenindex." }
            val a = achse(achsen[0]); val n = (werteVorräte.getValue(namen[a]) as? EndlicheIndexMenge)?.obergrenze?.positiveGanzeZahlOderNull()
                ?: error("Ein Schnitt benötigt eine endliche Achse.")
            require(achsen[1] in 1..n) { "Der Schnittindex liegt außerhalb von {1,…,$n}." }
            copy(parameter = parameter.filterIndexed { i, _ -> i != a },
                vorschrift = ersetze(vorschrift, mapOf(namen[a] to RationaleZahl.von(achsen[1].toLong()))), werteVorräte = werteVorräte - namen[a])
        }
        else -> error("Unbekannte Indexachsentransformation '$modus'.")
    }
}

fun Tupel.alsPositiveIndexListe(): List<Int> = elemente.map {
    val n = (it as? ZahlAusdruck)?.positiveGanzeZahlOderNull() ?: error("Indizes müssen positive ganze Zahlen sein.")
    require(n > 0) { "Indizes beginnen bei 1." }
    n
}

/** Endliche Komponenten-Auswahl entlang sämtlicher Permutationen, ohne Produkt oder Summe. */
fun permutationsKomponentenMethode(quelle: Matrix, minor: Boolean = false, vorzeichen: Boolean = false): MathematischeMethode {
    require(quelle.zeilenAnzahl == quelle.spaltenAnzahl) { "Permutationsauswahl benötigt eine quadratische Matrix." }
    val n = quelle.zeilenAnzahl
    require(quelle.zeilen.flatten().none { inferiereZahlenWertevorrat(it).fundamentalerZahlbereichOderNull() == FundamentalerZahlbereich.QUATERNION }) { "Die Permutationsdefinition benötigt einen kommutativen Zahlbereich." }
    require(!minor || n <= 6) { "Die ausführbare Kofaktorinverse wird bis Dimension 6 materialisiert." }
    val größe = if (minor) n - 1 else n
    require(größe <= 7) { "Die Permutationsauswahl überschreitet das Materialisierungsbudget (höchstens 7 Achsenpositionen)." }
    fun perm(rest: List<Int>): List<List<Int>> = if (rest.isEmpty()) listOf(emptyList()) else rest.flatMap { a -> perm(rest - a).map { listOf(a) + it } }
    val permutationen = perm((0 until größe).toList())
    if (vorzeichen) {
        val zeichen = permutationen.map { p ->
            val invers = p.indices.sumOf { i -> (i + 1 until p.size).count { j -> p[i] > p[j] } }
            RationaleZahl.von(if (invers % 2 == 0) 1L else -1L)
        }
        return strukturIndexMethode(Tupel(zeichen))
    }
    if (!minor) return strukturIndexMethode(Matrix(permutationen.map { p -> p.indices.map { k -> quelle.zeilen[k][p[k]] } }))
    val kAnzahl = größe.coerceAtLeast(1)
    require(n.toLong() * n * permutationen.size * kAnzahl <= 100_000) { "Die Minor-Komponentenauswahl überschreitet das Materialisierungsbudget." }
    val werte = (0 until n).flatMap { i -> (0 until n).flatMap { j -> permutationen.flatMap { p ->
        if (n == 1) listOf(RationaleZahl.Eins) else (0 until größe).map { k ->
            val zeile = if (k >= j) k + 1 else k
            val spalte = if (p[k] >= i) p[k] + 1 else p[k]
            quelle.zeilen[zeile][spalte]
        }
    } } }
    return strukturIndexMethode(Tensor(listOf(n,n,permutationen.size,kAnzahl),werte))
}

fun alternierendeAchsenMethode(quelle: Matrix): MathematischeMethode = strukturIndexMethode(Matrix(List(quelle.zeilenAnzahl) { i ->
    List(quelle.spaltenAnzahl) { j -> RationaleZahl.von(if ((i+j)%2==0) 1L else -1L) }
}))

/** Zeilenmajorer Komponentenindex auf eine neue Achsenform; kein Struktur-Rechneraufruf. */
fun umgeformteStrukturIndexMethode(quelle: MathematischesObjekt, form: List<Int>, werteVorräte: Map<String,MengenAusdruck> = emptyMap(), annahmen: Set<Aussage> = emptySet()): MathematischeMethode {
    require(form.isNotEmpty() && form.all { it>0 }) { "Die Zielform benötigt positive Achsenlängen." }
    val anzahl=form.fold(java.math.BigInteger.ONE) { a,n -> a*java.math.BigInteger.valueOf(n.toLong()) }
    require(anzahl<=java.math.BigInteger.valueOf(100_000)) { "Die Zielform überschreitet das Materialisierungsbudget." }
    val elemente=when(quelle) {
        is Tupel -> quelle.elemente.map { it as? ZahlAusdruck ?: error("Tensor-Komponenten müssen Zahlen sein.") }
        is OrientierterVektor -> quelle.werte
        is Tensorartig -> quelle.tensorForm.indizesFolge().map { quelle.tensorKomponente(it) }
        else -> error("Eine endliche numerische Komponentenstruktur wird benötigt.")
    }
    require(elemente.size==anzahl.toInt()) { "Die Zielform enthält ${anzahl} Plätze, die Quelle ${elemente.size} Komponenten." }
    val frei=quelle.enthalteneMethodenParameter().map { it.name }.toMutableSet()
    val parameter=form.indices.map { i -> var name="i${i+1}";while(!frei.add(name)) name+="_";Variable(name) }
    val linear=addition(listOf(RationaleZahl.Eins)+parameter.mapIndexed { i,p ->
        val schritt=form.drop(i+1).fold(1L) { a,n -> a*n }
        multiplikation(subtraktion(p,RationaleZahl.Eins),RationaleZahl.von(schritt))
    })
    val ziel=maximaleZahlenGrundmenge(elemente.map { inferiereZahlenWertevorrat(it,werteVorräte,annahmen) })
    return MathematischeMethode("index",parameter,SymbolischeZahlKomponente(Tupel(elemente),linear),Tupelraum(listOf(ziel)),parameter.mapIndexed { i,p -> p.name to EndlicheIndexMenge(RationaleZahl.von(form[i].toLong())) }.toMap())
}

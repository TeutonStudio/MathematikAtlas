package de.TeutonStudio.MathematikRechenSystem.kern

/** Strukturierte Auswahl aller quadratischen Minoren; kleinere Matrizen werden mit I ergänzt. */
private fun quadratischeMinorenFamilie(quelle: Matrix): Pair<List<Matrix>, List<Int>> {
    val n=minOf(quelle.zeilenAnzahl,quelle.spaltenAnzahl)
    require(n<=7) { "Die Minor-Familie überschreitet das Materialisierungsbudget." }
    val p=(1..n).fold(1L) { a,b -> a*b }
    fun anzahlKombinationen(umfang:Int,k:Int):java.math.BigInteger = (1..k).fold(java.math.BigInteger.ONE) { a,i -> a*java.math.BigInteger.valueOf((umfang-i+1).toLong())/java.math.BigInteger.valueOf(i.toLong()) }
    val anzahl=(1..n).fold(java.math.BigInteger.ONE) { a,d -> a+anzahlKombinationen(quelle.zeilenAnzahl,d)*anzahlKombinationen(quelle.spaltenAnzahl,d) }
    require(anzahl*java.math.BigInteger.valueOf(p*n)<=java.math.BigInteger.valueOf(100_000)) { "Die Minor-Komponentenauswahl überschreitet das Materialisierungsbudget." }
    fun kombinationen(umfang:Int,groesse:Int,start:Int=0):List<List<Int>> =
        if(groesse==0) listOf(emptyList()) else (start..umfang-groesse).flatMap { i -> kombinationen(umfang,groesse-1,i+1).map { listOf(i)+it } }
    val auswahlen=mutableListOf(Triple(emptyList<Int>(),emptyList<Int>(),0))
    for(d in 1..minOf(quelle.zeilenAnzahl,quelle.spaltenAnzahl)) for(r in kombinationen(quelle.zeilenAnzahl,d)) for(c in kombinationen(quelle.spaltenAnzahl,d)) auswahlen+=Triple(r,c,d)
    require(auswahlen.size.toLong()*p*n<=100_000) { "Die Minor-Komponentenauswahl überschreitet das Materialisierungsbudget." }
    return auswahlen.map { (r,c,d) -> Matrix(List(n) { i -> List(n) { j ->
        when { i<d && j<d -> quelle.zeilen[r[i]][c[j]]; i==j && i>=d -> RationaleZahl.Eins; else -> RationaleZahl.Null }
    } }) } to auswahlen.map { it.third }
}

/** Keine Determinantenberechnung: liefert ausschließlich Komponenten, Permutationszeichen oder Grade. */
fun minorenFamilienIndexMethode(quelle: Matrix, auswahl: String): MathematischeMethode {
    val (minoren,grade)=quadratischeMinorenFamilie(quelle)
    if(auswahl=="grade") return strukturIndexMethode(Tupel(grade.map { RationaleZahl.von(it.toLong()) }))
    if(auswahl=="vorzeichen") return permutationsKomponentenMethode(minoren.first(),vorzeichen=true)
    val n=minoren.first().zeilenAnzahl
    val p=(1..n).fold(1) { a,b -> a*b }
    val werte=minoren.flatMap { minor ->
        val methode=permutationsKomponentenMethode(minor)
        (1..p).flatMap { permutation -> (1..n).map { k -> methode.wendeAn(listOf(RationaleZahl.von(permutation.toLong()),RationaleZahl.von(k.toLong()))) as ZahlAusdruck } }
    }
    return strukturIndexMethode(Tensor(listOf(minoren.size,p,n),werte))
}

/** Formtreue Identifikation eines zweiachsigen Tensors mit einer rechteckigen Matrix. */
fun matrixAusZweiachsigerStruktur(quelle: MathematischesObjekt): Matrix {
    if(quelle is Matrix) return quelle
    val tensor=quelle as? Tensorartig ?: error("Eine Matrixstruktur mit zwei Achsen wird benötigt.")
    require(tensor.tensorForm.size==2) { "Eine Matrixstruktur benötigt genau zwei Achsen." }
    return Matrix(List(tensor.tensorForm[0]) { i -> List(tensor.tensorForm[1]) { j -> tensor.tensorKomponente(listOf(i,j)) } })
}

package de.TeutonStudio.MathematikRechenSystem.kern

/** Nur Tupel werden rekursiv geöffnet; alle anderen mathematischen Objekte sind Blätter. */
fun tupelAbflachen(tupel: Tupel): Tupel {
    val blätter = mutableListOf<MathematischesObjekt>()
    fun besuche(wert: MathematischesObjekt) {
        if (wert is Tupel) wert.elemente.forEach(::besuche) else blätter += wert
    }
    besuche(tupel)
    return Tupel(blätter)
}

fun übersetzeStruktur(wert: MathematischesObjekt, ziel: String): MathematischesObjekt = when (ziel) {
    "tupel" -> when (wert) {
        is OrientierterVektor -> Tupel(wert.werte)
        else -> error("Nach Tupel können nur Zeilen- oder Spaltenvektoren übersetzt werden.")
    }
    "zeile", "spalte" -> {
        val komponenten = when (wert) {
            is Tupel -> {
                require(wert.elemente.isNotEmpty()) { "Ein leeres Tupel kann nicht in einen Vektor übersetzt werden." }
                wert.elemente.mapIndexed { index, element -> element as? ZahlAusdruck
                    ?: error("Komponente ${index + 1} ist keine Zahl; verschachtelte Tupel, Mengen und Aussagen sind unzulässig.") }
            }
            is SpaltenVektor -> {
                require(ziel == "zeile") { "Die Quelle ist bereits eine Spalte." }
                wert.werte
            }
            is ZeilenVektor -> {
                require(ziel == "spalte") { "Die Quelle ist bereits eine Zeile." }
                wert.werte
            }
            else -> error("Nach $ziel können nur Tupel oder Vektoren der anderen Orientierung übersetzt werden.")
        }
        if (ziel == "spalte") SpaltenVektor(komponenten) else ZeilenVektor(komponenten)
    }
    else -> error("Unbekanntes Strukturübersetzungsziel '$ziel'.")
}

package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

/** Feste Faktoren und innere Positionen dürfen nie durch Weglassen zusammenrücken. */
fun matrixProduktFaktorWerte(kontext: KnotenAuswertungsKontext): LinkedHashMap<String, MathematischesObjekt> {
    val ports=kontext.knoten.anschlüsse.filter { it.richtung==AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }
    val feste=kontext.knoten.parameter["festeEingänge"]?.toIntOrNull()?.coerceAtLeast(2) ?: 2
    val letzterBelegt=ports.indexOfLast { it.name in kontext.eingänge }
    return linkedMapOf<String,MathematischesObjekt>().apply {
        ports.forEachIndexed { index,port ->
            val wert=kontext.eingänge[port.name]
            if (wert==null) {
                require(index>=feste && index>letzterBelegt && port.kannSichErweitern && port.dynamischErzeugt) { "Matrixfaktor ${index+1} ('${port.name}') fehlt oder ist nicht auswertbar." }
            } else put(port.name,wert.mathematischesObjekt("Matrixfaktor ${index+1} ('${port.name}')"))
        }
        require(size>=2) { "Matrixprodukt benötigt mindestens zwei Faktoren." }
    }
}

/** Gemeinsamer Sichtbarkeitstest für Android-Konzept und Desktop-Inspector, ohne Schreibweisenkopie. */
fun istMatrixProduktKnoten(knoten: KnotenDaten): Boolean {
    if (knoten.art == "mathematik.matrixProdukt") return true
    val familie=RechnerOperatorRegistry.familieFür(knoten.art) ?: return false
    return familie==RechnerFamilie.MATRIX && knoten.parameter[familie.operatorParameter]==MatrixRechnerOperator.MATRIXPRODUKT.stabileId
}

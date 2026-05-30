package com.TeutonStudio.MathematikAtlas.knoten

import com.TeutonStudio.KnotenKartenVerwalter.daten.ZahlenTyp
import com.TeutonStudio.KnotenKartenVerwalter.daten.Zahlenraum
import com.TeutonStudio.KnotenKartenVerwalter.daten.istKompatibelMit
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.RechenKnoten

// Latex-Anzeige, Inspektor-Auswahl von Operation.
// TODO: Fuer assoziative und kommutative Operationen dynamische Anschluesse
// bereitstellen, sobald das Modul variable Anschluesse pro Knoten erlaubt.
object RechnenAtlasKnoten {
    const val ART: String = RechenKnoten.KNOTEN_ART
    val operationen: List<String> = listOf("+", "-", "*", "/", "^")

    fun daten(operator: String = "+"): Map<String, Any> = matheDaten(
        art = ART,
        name = "Rechnung",
        daten = mapOf(
            "operator" to operator,
            "zahlenTyp" to ZahlenTyp(Zahlenraum.Reell),
        ),
    )
}

internal fun berechneOperator(inputs: List<MatheWert>, operator: String, typ: ZahlenTyp): MatheWert {
    if (inputs.size < 2) return MatheWert(operator, null, typ, "fehlende Eingabe")
    val links = inputs[0]
    val rechts = inputs[1]
    if (!links.typ.istKompatibelMit(typ) || !rechts.typ.istKompatibelMit(typ)) {
        return MatheWert(operator, null, typ, "inkompatibler Zahlenraum")
    }
    val ausdruck = "(${links.ausdruck} $operator ${rechts.ausdruck})"
    val wert = links.wert?.let { l ->
        rechts.wert?.let { r ->
            when (operator) {
                "+" -> l + r
                "-" -> l - r
                "*" -> l * r
                "/" -> if (r == 0.0) null else l / r
                "^" -> Math.pow(l, r)
                else -> null
            }
        }
    }
    return MatheWert(wert?.kompakt() ?: ausdruck, wert, typ.copy(ausdruck = ausdruck))
}

private fun Double.kompakt(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString()

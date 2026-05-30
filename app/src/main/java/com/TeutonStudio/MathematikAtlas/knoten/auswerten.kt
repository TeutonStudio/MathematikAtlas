package com.TeutonStudio.MathematikAtlas.knoten

import com.TeutonStudio.KnotenKartenVerwalter.daten.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.ZahlenTyp
import com.TeutonStudio.KnotenKartenVerwalter.daten.Zahlenraum
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.AuswertungsKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.FormelKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.FunktionKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.MathematikEingabeKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.RechenKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.UnbekannteKnoten

// Latex-Anzeige, Auswertung der verbundenen Knoten-Kette.
object AuswertenAtlasKnoten {
    const val ART: String = AuswertungsKnoten.KNOTEN_ART

    fun daten(): Map<String, Any> = matheDaten(ART, "Auswertung")
}

internal data class MatheWert(
    val ausdruck: String,
    val wert: Double?,
    val typ: ZahlenTyp,
    val fehler: String? = null,
)

fun KarteDaten.werteMathematikAus(karten: Map<String, KarteDaten>): KarteDaten {
    val auswertungsKarten = if (initialKnoten.isNotEmpty()) {
        karten + ("$id:initial" to KarteDaten(id = "$id:initial", name = "$name Funktion", knoten = initialKnoten, verbindungen = initialVerbindungen))
    } else {
        karten
    }
    val werte = mutableMapOf<String, MatheWert>()
    val eingehend = verbindungen.groupBy { it.zielKnotenId }
    val ausgehend = verbindungen.groupBy { it.quellKnotenId }
    val grad = knoten.associate { it.id to eingehend[it.id].orEmpty().size }.toMutableMap()
    val nachId = knoten.associateBy { it.id }
    val reihenfolge = mutableListOf<KnotenDaten>()
    val offen = ArrayDeque(knoten.filter { grad.getValue(it.id) == 0 }.map { it.id })

    while (offen.isNotEmpty()) {
        val id = offen.removeFirst()
        val aktueller = nachId[id] ?: continue
        reihenfolge += aktueller
        ausgehend[id].orEmpty().forEach { verbindung ->
            grad[verbindung.zielKnotenId] = grad.getValue(verbindung.zielKnotenId) - 1
            if (grad.getValue(verbindung.zielKnotenId) == 0) offen.addLast(verbindung.zielKnotenId)
        }
    }
    val zyklen = knoten.filterNot { kandidat -> reihenfolge.any { it.id == kandidat.id } }
    reihenfolge += zyklen

    val neueKnoten = reihenfolge.map { knoten ->
        val inputs = eingehend[knoten.id].orEmpty().mapNotNull { werte[it.quellKnotenId] }
        val wert = knoten.berechneMatheWert(inputs, auswertungsKarten, zyklen.any { it.id == knoten.id })
        werte[knoten.id] = wert
        val status = wert.fehler ?: wert.ausdruck
        knoten.copy(data = knoten.data + mapOf("status" to status, "kurzform" to status, "zahlenTyp" to wert.typ))
    }
    return copy(knoten = knoten.map { alt -> neueKnoten.firstOrNull { it.id == alt.id } ?: alt })
}

private fun KnotenDaten.berechneMatheWert(
    inputs: List<MatheWert>,
    karten: Map<String, KarteDaten>,
    istZyklus: Boolean,
): MatheWert {
    val typ = zahlenTyp()
    if (istZyklus) return MatheWert(name, null, typ, "zyklische Abhaengigkeit")
    val inputFehler = inputs.firstOrNull { it.fehler != null }?.fehler
    if (inputFehler != null) return MatheWert(name, null, typ, inputFehler)

    return when (art) {
        MathematikEingabeKnoten.KNOTEN_ART -> {
            val wertText = data["wert"]?.toString().orEmpty()
            MatheWert(wertText.ifBlank { name }, wertText.toDoubleOrNull(), typ.copy(wert = wertText.ifBlank { null }))
        }
        UnbekannteKnoten.KNOTEN_ART -> {
            val variable = data["variable"]?.toString()?.ifBlank { null } ?: name
            MatheWert(variable, null, typ.copy(anzeigename = variable))
        }
        RechenKnoten.KNOTEN_ART -> berechneOperator(inputs, data["operator"]?.toString() ?: "+", typ)
        FormelKnoten.KNOTEN_ART -> {
            val formel = data["formel"]?.toString()?.ifBlank { null } ?: inputs.joinToString(" ") { it.ausdruck }
            MatheWert(formel, inputs.firstOrNull()?.wert, typ.copy(ausdruck = formel))
        }
        AuswertungsKnoten.KNOTEN_ART -> inputs.firstOrNull()
            ?: MatheWert(name, null, typ, "fehlende Eingabe")
        FunktionKnoten.KNOTEN_ART -> {
            val referenz = data["kartenId"]?.toString()?.let { karten[it] }
            if (referenz == null) {
                MatheWert(name, null, typ, "referenzierte Karte fehlt")
            } else {
                val ausgabe = referenz.werteMathematikAus(karten).knoten.lastOrNull { it.art == FormelKnoten.KNOTEN_ART }
                val ausdruck = "${data["funktion"]?.toString()?.ifBlank { null } ?: referenz.name}(${inputs.joinToString(", ") { it.ausdruck }})"
                MatheWert(ausgabe?.data?.get("status")?.toString() ?: ausdruck, null, typ)
            }
        }
        LOESEN_KNOTEN_ART -> {
            val gleichung = inputs.joinToString(" = ") { it.ausdruck }.ifBlank { "Gleichungssystem" }
            MatheWert(gleichung, null, typ, if (inputs.size < 2) "mindestens zwei Kartenausdruecke noetig" else null)
        }
        else -> inputs.firstOrNull() ?: MatheWert(name, null, typ)
    }
}

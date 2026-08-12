package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val METHODEN_AUFRUF_ARGUMENTPROJEKTION = "methodenAufruf.argumentprojektion"
const val METHODEN_AUFRUF_ERGEBNISPROJEKTION = "methodenAufruf.ergebnisprojektion"
const val METHODEN_ARGUMENTPROJEKTION_SEPARIERT = "separiert"
const val METHODEN_ARGUMENTPROJEKTION_TUPEL = "tupel"
const val METHODEN_ERGEBNISPROJEKTION_DIREKT = "direkt"
const val METHODEN_ERGEBNISPROJEKTION_TUPEL = "tupel"

/**
 * Mathematischer Methodenaufruf mit rein graphischer Argument- und Ergebnisprojektion.
 *
 * Der allgemeine [Methode]-Vertrag ist absichtlich nicht ausführbar. Dieser Adapter
 * gehört zum MathematikKartenAdapter und akzeptiert deshalb für konkrete Methoden
 * ausschließlich die explizite [MathematischAuswertbareMethode]-Capability. Historische
 * symbolische Methodenplatzhalter bleiben als mathematische Ausdrücke auswertbar und
 * werden bei unbekannter Ergebnissignatur kanonisch als Tupelausgabe behandelt.
 */
internal object MethodenAufrufAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val methodenWert = kontext.eingänge["methode"] ?: error("Die Methode fehlt.")
        val methodenObjekt = methodenWert.objekt
        val konkreteMethode = methodenObjekt as? Methode
        val mathematischeMethode = konkreteMethode?.let { methode ->
            val auswertbar = methode as? MathematischAuswertbareMethode
                ?: error(
                    "Die Methode '${methode.name}' besitzt keine mathematische Auswertungs-Capability. " +
                        "Nichtmathematische Methoden benötigen ihre eigene Script-/Engine-Auswertung.",
                )
            auswertbar to methode.alsMathematischeMethode("mathematischen Methodenaufruf")
        }
        val argumentAnschlüsse = kontext.knoten.anschlüsse
            .filter { it.name != "methode" && it.name != "wert" }
            .sortedBy { it.reihenfolge }
        val argumentProjektion = kontext.knoten.parameter[METHODEN_AUFRUF_ARGUMENTPROJEKTION]
            ?: METHODEN_ARGUMENTPROJEKTION_SEPARIERT
        val ergebnisProjektion = kontext.knoten.parameter[METHODEN_AUFRUF_ERGEBNISPROJEKTION]
            ?: if (konkreteMethode == null) METHODEN_ERGEBNISPROJEKTION_TUPEL else METHODEN_ERGEBNISPROJEKTION_DIREKT

        val argumentWerte = if (mathematischeMethode != null) {
            val methode = mathematischeMethode.second
            when (argumentProjektion) {
                METHODEN_ARGUMENTPROJEKTION_TUPEL -> tupelArgumente(methode, argumentAnschlüsse, kontext)
                else -> separierteArgumente(methode, argumentAnschlüsse, kontext)
            }
        } else {
            symbolischeArgumente(argumentAnschlüsse, kontext, argumentProjektion)
        }
        val argumente = argumentWerte.mapIndexed { index, wert ->
            wert.objekt as? MathematischesObjekt
                ?: error(
                    "Das ${index + 1}. Argument des mathematischen Methodenaufrufs " +
                        "ist kein mathematischer Atlaswert.",
                )
        }

        val (roherWert, roheZielMenge, methodenReferenz) = if (mathematischeMethode != null) {
            val auswertbareMethode = mathematischeMethode.first
            val methode = mathematischeMethode.second
            val ausgabe = methode.einzigeAusgabe()
            val bindungen = methode.parameter
                .mapIndexed { index, parameter -> parameter.name to argumente[index] }
                .toMap()
            Triple(
                auswertbareMethode.wendeMathematischAn(bindungen),
                methode.zielMengeFür(ausgabe.first, bindungen),
                methode.name,
            )
        } else {
            val symbol = methodenObjekt as? MathematischesObjekt
                ?: error("Nichtmathematische Methoden benötigen eine eigene Script-/Engine-Auswertung.")
            val ergebnisArt = kontext.knoten.parameter[METHODEN_ANWENDUNG_ERGEBNIS_ART]
                ?.trim().orEmpty().ifBlank { "mathematik.objekt" }
            Triple(
                symbolischerMethodenAufrufWert(symbol, argumente, ergebnisArt),
                null,
                methodenWert.anzeigeLatex(),
            )
        }

        val (wert, zielMenge) = projiziereMethodenErgebnis(
            wert = roherWert,
            zielMenge = roheZielMenge,
            projektion = ergebnisProjektion,
        )
        val anwendungsLatex = "$methodenReferenz(${argumentWerte.joinToString(",") { it.anzeigeLatex() }})"
        val projiziertesLatex = if (
            ergebnisProjektion == METHODEN_ERGEBNISPROJEKTION_TUPEL && roherWert !is Tupel
        ) {
            "($anwendungsLatex)"
        } else {
            anwendungsLatex
        }
        return KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "wert" to BedingterWert(
                    objekt = wert,
                    annahmen = (listOf(methodenWert) + argumentWerte).flatMap { it.annahmen }.toSet(),
                    zielMenge = zielMenge,
                    reelleVariablen = reelleVariablen(argumentWerte),
                    variablenQuellen = (listOf(methodenWert) + argumentWerte).flatMap { it.variablenQuellen },
                    latexDarstellung = projiziertesLatex,
                ),
            ),
        )
    }

    private fun separierteArgumente(
        methode: MathematischeMethode,
        argumentAnschlüsse: List<AnschlussDaten>,
        kontext: KnotenAuswertungsKontext,
    ): List<BedingterWert> {
        val benötigteAnschlüsse = argumentAnschlüsse.take(methode.parameter.size)
        require(benötigteAnschlüsse.size == methode.parameter.size) {
            "Die Methode '${methode.name}' benötigt ${methode.parameter.size} Argumentanschlüsse, vorhanden sind ${argumentAnschlüsse.size}."
        }
        val fehlende = benötigteAnschlüsse.filter { kontext.eingänge[it.name] == null }
        require(fehlende.isEmpty()) {
            "Der Methode '${methode.name}' fehlen die Argumente ${fehlende.joinToString { "'${it.name}'" }}."
        }
        val überzählige = argumentAnschlüsse.drop(methode.parameter.size)
            .filter { kontext.eingänge[it.name] != null }
        require(überzählige.isEmpty()) {
            "Die Methode '${methode.name}' benötigt ${methode.parameter.size} Argumente; überzählig verbunden sind ${überzählige.joinToString { "'${it.name}'" }}."
        }
        return benötigteAnschlüsse.map { kontext.eingänge.getValue(it.name) }
    }

    private fun tupelArgumente(
        methode: MathematischeMethode,
        argumentAnschlüsse: List<AnschlussDaten>,
        kontext: KnotenAuswertungsKontext,
    ): List<BedingterWert> {
        if (methode.parameter.isEmpty()) {
            require(argumentAnschlüsse.none { kontext.eingänge[it.name] != null }) {
                "Eine nullstellige Methode besitzt auch im Tupelmodus keinen Argumentanschluss."
            }
            return emptyList()
        }
        require(argumentAnschlüsse.size == 1) {
            "Die Tupelprojektion der Methode '${methode.name}' benötigt genau einen Tupelanschluss."
        }
        val tupelWert = kontext.eingänge[argumentAnschlüsse.single().name]
            ?: error("Der Methode '${methode.name}' fehlt das Argumenttupel.")
        val tupel = tupelWert.objekt as? Tupel
            ?: error("Die Methode '${methode.name}' erwartet im Tupelmodus ein echtes Tupel; ein Skalar wird nicht automatisch verpackt.")
        require(tupel.elemente.size == methode.parameter.size) {
            "Die Methode '${methode.name}' besitzt ${methode.parameter.size} Argumente, das verbundene Tupel aber ${tupel.elemente.size} Elemente."
        }
        return tupel.elemente.mapIndexed { index, element ->
            val parameter = methode.parameter[index]
            tupelWert.copy(
                objekt = element,
                werteVorrat = methode.werteVorräte[parameter.name],
                latexDarstellung = null,
            )
        }
    }

    private fun symbolischeArgumente(
        argumentAnschlüsse: List<AnschlussDaten>,
        kontext: KnotenAuswertungsKontext,
        projektion: String,
    ): List<BedingterWert> {
        if (projektion == METHODEN_ARGUMENTPROJEKTION_TUPEL) {
            val verbundene = argumentAnschlüsse.filter { kontext.eingänge[it.name] != null }
            if (verbundene.isEmpty()) return emptyList()
            require(verbundene.size == 1) {
                "Ein symbolischer Methodenaufruf benötigt im Tupelmodus genau einen verbundenen Tupelanschluss."
            }
            val tupelWert = kontext.eingänge.getValue(verbundene.single().name)
            val tupel = tupelWert.objekt as? Tupel
                ?: error("Ein symbolischer Methodenaufruf erwartet im Tupelmodus ein echtes Tupel.")
            return tupel.elemente.map { element ->
                tupelWert.copy(objekt = element, latexDarstellung = null)
            }
        }

        val letztesVerbundenesArgument = argumentAnschlüsse.indexOfLast { kontext.eingänge[it.name] != null }
        if (letztesVerbundenesArgument < 0) return emptyList()
        val verwendeteAnschlüsse = argumentAnschlüsse.take(letztesVerbundenesArgument + 1)
        val fehlendes = verwendeteAnschlüsse.firstOrNull { kontext.eingänge[it.name] == null }
        require(fehlendes == null) {
            "Vor dem verbundenen Argument '${verwendeteAnschlüsse.last().name}' fehlt '${fehlendes?.name}'."
        }
        return verwendeteAnschlüsse.map { kontext.eingänge.getValue(it.name) }
    }
}

private fun symbolischerMethodenAufrufWert(
    methode: MathematischesObjekt,
    argumente: List<MathematischesObjekt>,
    ergebnisArt: String,
): MathematischesObjekt {
    val latex = "${methode.zuLatex()}(${argumente.joinToString(",") { it.zuLatex() }})"
    val kennung = latex.replace("\\", "_").replace(Regex("[^\\p{L}\\p{N}_]+"), "_")
    return when (ergebnisArt) {
        "mathematik.zahl" -> Variable(kennung)
        "mathematik.aussage" -> AussagenParameter(kennung, latex)
        "mathematik.menge" -> MengenParameter(kennung, latex)
        else -> TypisiertesElement(kennung, ergebnisArt, latex)
    }
}

internal fun projiziereMethodenErgebnis(
    wert: MathematischesObjekt,
    zielMenge: MengenAusdruck?,
    projektion: String,
): Pair<MathematischesObjekt, MengenAusdruck?> {
    if (projektion != METHODEN_ERGEBNISPROJEKTION_TUPEL) return wert to zielMenge
    val projizierterWert = if (wert is Tupel) wert else Tupel(listOf(wert))
    val projizierteZielMenge = when (zielMenge) {
        null -> null
        is Tupelraum -> zielMenge
        else -> Tupelraum(listOf(zielMenge))
    }
    return projizierterWert to projizierteZielMenge
}
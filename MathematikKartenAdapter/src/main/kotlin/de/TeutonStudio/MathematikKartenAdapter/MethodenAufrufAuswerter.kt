package de.TeutonStudio.MathematikKartenAdapter

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
 * gehört zum MathematikKartenAdapter und akzeptiert deshalb ausschließlich die
 * explizite [MathematischAuswertbareMethode]-Capability. Eine spätere Script- oder
 * Engine-Auswertung bekommt ihre eigene Laufzeit-Capability statt eines künstlichen
 * [MathematischesObjekt]-Aufrufs.
 */
internal object MethodenAufrufAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val methodenWert = kontext.eingänge["methode"] ?: error("Die Methode fehlt.")
        val methode = methodenWert.objekt as? Methode
            ?: error("Der Methodenanschluss enthält keinen Methodenwert.")
        val auswertbareMethode = methode as? MathematischAuswertbareMethode
            ?: error(
                "Die Methode '${methode.name}' besitzt keine mathematische Auswertungs-Capability. " +
                    "Nichtmathematische Methoden benötigen ihre eigene Script-/Engine-Auswertung.",
            )
        val mathematischeMethode = methode.alsMathematischeMethode("mathematischen Methodenaufruf")
        val argumentAnschlüsse = kontext.knoten.anschlüsse
            .filter { it.name != "methode" && it.name != "wert" }
            .sortedBy { it.reihenfolge }
        val argumentProjektion = kontext.knoten.parameter[METHODEN_AUFRUF_ARGUMENTPROJEKTION]
            ?: METHODEN_ARGUMENTPROJEKTION_SEPARIERT
        val ergebnisProjektion = kontext.knoten.parameter[METHODEN_AUFRUF_ERGEBNISPROJEKTION]
            ?: METHODEN_ERGEBNISPROJEKTION_DIREKT

        val argumentWerte = when (argumentProjektion) {
            METHODEN_ARGUMENTPROJEKTION_TUPEL -> tupelArgumente(mathematischeMethode, argumentAnschlüsse, kontext)
            else -> separierteArgumente(mathematischeMethode, argumentAnschlüsse, kontext)
        }
        val argumente = argumentWerte.mapIndexed { index, wert ->
            wert.objekt as? MathematischesObjekt
                ?: error(
                    "Das ${index + 1}. Argument der mathematischen Methode '${methode.name}' " +
                        "ist kein mathematischer Atlaswert.",
                )
        }
        val ausgabe = mathematischeMethode.einzigeAusgabe()
        val bindungen = mathematischeMethode.parameter
            .mapIndexed { index, parameter -> parameter.name to argumente[index] }
            .toMap()
        val roherWert = auswertbareMethode.wendeMathematischAn(bindungen)
        val roheZielMenge = mathematischeMethode.zielMengeFür(ausgabe.first, bindungen)
        val (wert, zielMenge) = projiziereMethodenErgebnis(
            wert = roherWert,
            zielMenge = roheZielMenge,
            projektion = ergebnisProjektion,
        )
        val anwendungsLatex = "${methode.name}(${argumentWerte.joinToString(",") { it.anzeigeLatex() }})"
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
        argumentAnschlüsse: List<de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten>,
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
        argumentAnschlüsse: List<de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten>,
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

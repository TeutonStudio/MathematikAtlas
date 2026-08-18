package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.MathematikRechenSystem.kern.*

const val METHODEN_AUFRUF_ARGUMENTPROJEKTION = "methodenAufruf.argumentprojektion"
const val METHODEN_AUFRUF_ERGEBNISPROJEKTION = "methodenAufruf.ergebnisprojektion"
const val METHODEN_ARGUMENTPROJEKTION_SEPARIERT = "separiert"
const val METHODEN_ARGUMENTPROJEKTION_TUPEL = "tupel"
const val METHODEN_ERGEBNISPROJEKTION_DIREKT = "direkt"
const val METHODEN_ERGEBNISPROJEKTION_TUPEL = "tupel"

/**
 * Methodenaufruf mit rein graphischer Argument- und Ergebnisprojektion.
 *
 * Dieser Mathematikadapter führt ausschließlich [MathematischAuswertbareMethode] aus.
 * Eine allgemeine Script-/Engine-Methode ist ein gültiger Methodenwert, wird hier aber
 * nicht durch mathematische Substitution oder künstliche Symbolwerte interpretiert.
 * Dafür kann später eine eigene Auswertungs-Capability registriert werden.
 */
internal object MethodenAufrufAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val methodenWert = kontext.eingänge["methode"] ?: error("Die Methode fehlt.")
        val methodenObjekt = methodenWert.objekt
        val methode = methodenObjekt as? Methode
        val auswertbareMethode = methode as? MathematischAuswertbareMethode
        val mathematischeMethode = methode?.let {
            runCatching { it.alsMathematischeMethode("mathematischen Methodenaufruf") }.getOrNull()
        }
        if (methode != null && auswertbareMethode == null) {
            error(
                "Die Methode '${methode.name}' besitzt keine in diesem Adapter registrierte Auswertungs-Capability. " +
                    "Allgemeine Methoden werden nicht automatisch mathematisch interpretiert.",
            )
        }

        val argumentAnschlüsse = kontext.knoten.anschlüsse
            .filter { it.name != "methode" && it.name != "wert" }
            .sortedBy { it.reihenfolge }
        val argumentProjektion = kontext.knoten.parameter[METHODEN_AUFRUF_ARGUMENTPROJEKTION]
            ?: METHODEN_ARGUMENTPROJEKTION_SEPARIERT
        val ergebnisProjektion = kontext.knoten.parameter[METHODEN_AUFRUF_ERGEBNISPROJEKTION]
            ?: if (auswertbareMethode != null) {
                METHODEN_ERGEBNISPROJEKTION_DIREKT
            } else {
                METHODEN_ERGEBNISPROJEKTION_TUPEL
            }

        val argumentWerte = if (mathematischeMethode != null) {
            when (argumentProjektion) {
                METHODEN_ARGUMENTPROJEKTION_TUPEL -> tupelArgumente(mathematischeMethode, argumentAnschlüsse, kontext)
                else -> separierteArgumente(mathematischeMethode, argumentAnschlüsse, kontext)
            }
        } else {
            freieProjektionsArgumente(argumentAnschlüsse, kontext)
        }

        val mathematischeArgumente = argumentWerte.map { wert ->
            wert.objekt as? MathematischesObjekt
                ?: error("Der Mathematikadapter kann das Argument '${wert.anzeigeLatex()}' nicht mathematisch auswerten.")
        }
        val ergebnisArt = kontext.knoten.parameter[METHODEN_ANWENDUNG_ERGEBNIS_ART]
            ?.trim().orEmpty().ifBlank { "mathematik.objekt" }
        val (roherWert, roheZielMenge) = if (
            mathematischeMethode != null && auswertbareMethode != null
        ) {
            val ausgabe = mathematischeMethode.einzigeAusgabe()
            val bindungen = mathematischeMethode.parameter
                .mapIndexed { index, parameter -> parameter.name to mathematischeArgumente[index] }
                .toMap()
            auswertbareMethode.wendeMathematischAn(bindungen) to mathematischeMethode.zielMengeFür(ausgabe.first, bindungen)
        } else {
            val symbolischeMethode = methodenObjekt as? MathematischesObjekt
                ?: error("Eine symbolische Methodenprojektion benötigt ein mathematisches Methodenobjekt.")
            symbolischerProjektionsAnwendungsWert(symbolischeMethode, mathematischeArgumente, ergebnisArt) to null
        }
        val (wert, zielMenge) = projiziereMethodenErgebnis(
            wert = roherWert,
            zielMenge = roheZielMenge,
            projektion = ergebnisProjektion,
        )
        val methodenReferenz = methode?.name ?: methodenWert.anzeigeLatex()
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

    private fun freieProjektionsArgumente(
        argumentAnschlüsse: List<de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten>,
        kontext: KnotenAuswertungsKontext,
    ): List<BedingterWert> {
        val letztesVerbundenesArgument = argumentAnschlüsse.indexOfLast { kontext.eingänge[it.name] != null }
        if (letztesVerbundenesArgument < 0) return emptyList()
        val verwendeteAnschlüsse = argumentAnschlüsse.take(letztesVerbundenesArgument + 1)
        val fehlendes = verwendeteAnschlüsse.firstOrNull { kontext.eingänge[it.name] == null }
        require(fehlendes == null) {
            "Vor dem verbundenen Argument '${verwendeteAnschlüsse.last().name}' fehlt '${fehlendes?.name}'."
        }
        return verwendeteAnschlüsse.map { kontext.eingänge.getValue(it.name) }
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
                werteVorrat = methode.mathematischeSignatur.argumente[index].definitionsMenge,
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

private fun symbolischerProjektionsAnwendungsWert(
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

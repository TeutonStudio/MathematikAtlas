package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.MathematikRechenSystem.kern.*

const val METHODEN_AUFRUF_ARGUMENTPROJEKTION = "methodenAufruf.argumentprojektion"
const val METHODEN_ARGUMENTPROJEKTION_SEPARIERT = "separiert"
const val METHODEN_ARGUMENTPROJEKTION_TUPEL = "tupel"

/**
 * Allgemeiner Methodenaufruf mit rein graphischer Argumentprojektion.
 *
 * Die Methode selbst bleibt unverändert. Im Tupelmodus wird ausschließlich an
 * dieser Adaptergrenze ein verbundenes Tupel positionsgetreu auf die kanonische
 * Parameterliste verteilt.
 */
internal object MethodenAufrufAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val methodenWert = kontext.eingänge["methode"] ?: error("Die Methode fehlt.")
        val methode = methodenWert.objekt
        val argumentAnschlüsse = kontext.knoten.anschlüsse
            .filter { it.name != "methode" && it.name != "wert" }
            .sortedBy { it.reihenfolge }
        val projektion = kontext.knoten.parameter[METHODEN_AUFRUF_ARGUMENTPROJEKTION]
            ?: METHODEN_ARGUMENTPROJEKTION_SEPARIERT

        val argumentWerte = if (methode is Methode) {
            when (projektion) {
                METHODEN_ARGUMENTPROJEKTION_TUPEL -> tupelArgumente(methode, argumentAnschlüsse, kontext)
                else -> separierteArgumente(methode, argumentAnschlüsse, kontext)
            }
        } else {
            val letztesVerbundenesArgument = argumentAnschlüsse.indexOfLast { kontext.eingänge[it.name] != null }
            if (letztesVerbundenesArgument < 0) emptyList() else {
                val verwendeteAnschlüsse = argumentAnschlüsse.take(letztesVerbundenesArgument + 1)
                val fehlendes = verwendeteAnschlüsse.firstOrNull { kontext.eingänge[it.name] == null }
                require(fehlendes == null) {
                    "Vor dem verbundenen Argument '${verwendeteAnschlüsse.last().name}' fehlt '${fehlendes?.name}'."
                }
                verwendeteAnschlüsse.map { kontext.eingänge.getValue(it.name) }
            }
        }

        val argumente = argumentWerte.map(BedingterWert::objekt)
        val ergebnisArt = kontext.knoten.parameter[METHODEN_ANWENDUNG_ERGEBNIS_ART]
            ?.trim().orEmpty().ifBlank { "mathematik.objekt" }
        val (wert, zielMenge) = if (methode is Methode) {
            val ausgabe = methode.einzigeAusgabe()
            val bindungen = methode.parameter.mapIndexed { index, parameter -> parameter.name to argumente[index] }.toMap()
            methode.wendeAn(bindungen) to methode.zielMengeFür(ausgabe.first, bindungen)
        } else {
            symbolischerProjektionsAnwendungsWert(methode, argumente, ergebnisArt) to null
        }
        val methodenReferenz = if (methode is Methode) methode.name else methodenWert.anzeigeLatex()
        val anwendungsLatex = "$methodenReferenz(${argumentWerte.joinToString(",") { it.anzeigeLatex() }})"
        return KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "wert" to BedingterWert(
                    objekt = wert,
                    annahmen = (listOf(methodenWert) + argumentWerte).flatMap { it.annahmen }.toSet(),
                    zielMenge = zielMenge,
                    reelleVariablen = reelleVariablen(argumentWerte),
                    variablenQuellen = (listOf(methodenWert) + argumentWerte).flatMap { it.variablenQuellen },
                    latexDarstellung = anwendungsLatex,
                ),
            ),
        )
    }

    private fun separierteArgumente(
        methode: Methode,
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
        methode: Methode,
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

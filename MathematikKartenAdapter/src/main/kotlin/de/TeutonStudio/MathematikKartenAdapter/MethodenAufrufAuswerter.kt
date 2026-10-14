package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.KnotenKartenVerwalter.daten.*

const val METHODEN_STRUKTUR_PACKUNG = "methodenAufruf.strukturPackung"
const val METHODEN_STRUKTUR_MIGRATIONSFEHLER = "methodenAufruf.strukturMigrationsfehler"

const val METHODEN_AUFRUF_PARTIELL = "methodenAufruf.partiell"
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
        kontext.knoten.parameter[METHODEN_STRUKTUR_MIGRATIONSFEHLER]?.let { error("Migrationsdiagnose: $it") }
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

        if(kontext.knoten.parameter[METHODEN_AUFRUF_PARTIELL]=="true") {
            val m=mathematischeMethode ?: error("Partielle Bindung benötigt eine mathematische Methodensignatur.")
            require(argumentProjektion==METHODEN_ARGUMENTPROJEKTION_SEPARIERT && METHODEN_STRUKTUR_PACKUNG !in kontext.knoten.eigenschaften) {"Partielle Bindung benötigt getrennte logische Argumentanschlüsse."}
            val ids=m.signatur.argumente.map {it.id}
            require(argumentAnschlüsse.mapIndexed {i,p -> p.semantischeId ?: ids.getOrNull(i)}.toSet()==ids.toSet()) {"Die Argumentidentitäten stimmen nicht mit der Signatur überein."}
            require(argumentAnschlüsse.size==ids.size) {"Die Argumentanschlüsse stimmen nicht mit der Signatur überein."}
            val bindungen=argumentAnschlüsse.mapIndexedNotNull {i,p -> kontext.eingänge[p.name]?.let { w ->
                val id=p.semantischeId ?: ids[i]
                require(id in ids) {"Unbekannte Argumentidentität."}
                id to (w.objekt as? MathematischesObjekt ?: error("Nichtmathematisches Argument."))
            }}.toMap()
            val gebunden=m.bindeIdentitäten(bindungen)
            val substitution=bindungen.mapKeys {(id,_) -> val p=m.parameter[m.signatur.argumente.indexOfFirst {it.id==id}];p.bindungsId ?: p.name}
            val annahmen=gebunden.voraussetzungen+methodenWert.annahmen.map {ersetze(it,substitution)}+kontext.eingänge.filterKeys {it!="methode"}.values.flatMap {it.annahmen}
            require(annahmen.none {it.entscheide(kontext.rechenKontext).wahrheitswert==Wahrheitswert.Lüge}) {"Die Bindung verletzt eine Definitionsbedingung."}
            return KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(gebunden,annahmen,variablenQuellen=kontext.eingänge.values.flatMap {it.variablenQuellen}.distinct(),übersetzungen=gebunden.übersetzungen)),eingänge=kontext.eingänge)
        }
        val argumentWerte = if (mathematischeMethode != null) {
            if (METHODEN_STRUKTUR_PACKUNG in kontext.knoten.eigenschaften) strukturierteArgumente(mathematischeMethode, kontext)
            else when (argumentProjektion) {
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
        val definitionsBedingungen = mutableSetOf<Aussage>()
        val (roherWert, roheZielMenge) = if (
            mathematischeMethode != null && auswertbareMethode != null
        ) {
            val bindungen = mathematischeMethode.parameter
                .mapIndexed { index, parameter -> (parameter.bindungsId ?: parameter.name) to mathematischeArgumente[index] }
                .toMap()
            mathematischeMethode.wendeMitBedingungenAn(Tupel(mathematischeArgumente), kontext.rechenKontext).also { definitionsBedingungen += it.bedingungen }.ergebnis to
                (ersetze(mathematischeMethode.zielRaum, bindungen) as MengenAusdruck)
        } else {
            val symbolischeMethode = methodenObjekt as? MathematischesObjekt
                ?: error("Eine symbolische Methodenprojektion benötigt ein mathematisches Methodenobjekt.")
            symbolischerProjektionsAnwendungsWert(symbolischeMethode, mathematischeArgumente, ergebnisArt) to null
        }
        val (wert, zielMenge) = projiziereMethodenErgebnis(
            wert = roherWert,
            zielMenge = roheZielMenge,
            projektion = ergebnisProjektion,
            kanonisch = mathematischeMethode != null,
        )
        val methodenReferenz = methode?.name ?: methodenWert.anzeigeLatex()
        val anwendungsLatex = "$methodenReferenz\\left(${argumentWerte.joinToString(",") { it.anzeigeLatex() }}\\right)"
        val projiziertesLatex = if (
            ergebnisProjektion == METHODEN_ERGEBNISPROJEKTION_TUPEL && roherWert !is Tupel
        ) {
            "\\left($anwendungsLatex\\right)"
        } else {
            anwendungsLatex
        }
        val annahmenBindungen = mathematischeMethode?.parameter?.mapIndexed { i, p -> (p.bindungsId ?: p.name) to mathematischeArgumente[i] }?.toMap().orEmpty()
        val angewandteAnnahmen = methodenWert.annahmen.map { ersetze(it, annahmenBindungen) as Aussage }.toSet() + argumentWerte.flatMap { it.annahmen } + definitionsBedingungen
        require(angewandteAnnahmen.none { it.entscheide(kontext.rechenKontext).wahrheitswert == Wahrheitswert.Lüge }) { "Die Argumente verletzen eine Definitionsbedingung der Methode." }
        return KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "wert" to BedingterWert(
                    objekt = wert,
                    annahmen = angewandteAnnahmen,
                    zielMenge = zielMenge,
                    reelleVariablen = methodenWert.reelleVariablen.filterKeys { it !in annahmenBindungen } + reelleVariablen(argumentWerte),
                    variablenQuellen = (listOf(methodenWert) + argumentWerte).flatMap { it.variablenQuellen },
                    latexDarstellung = projiziertesLatex,
                ),
            ),
        )
    }

    private fun strukturierteArgumente(methode: MathematischeMethode, kontext: KnotenAuswertungsKontext): List<BedingterWert> {
        val packung = kontext.knoten.eigenschaften.objekt(METHODEN_STRUKTUR_PACKUNG)?.felder ?: error("Die Argumentpackung fehlt.")
        val einträge = (packung["komponenten"] as? KnotenEigenschaft.Liste)?.werte ?: error("Komponentenzuordnung fehlt.")
        val parameter = methode.parameter.filter { istWinkelraum(methode.werteVorräte[it.name] ?: LeereMenge) }.singleOrNull()
            ?: error("Die gespeicherte Packung benötigt genau einen eindeutig erkennbaren Winkelraumparameter.")
        require((methode.werteVorräte[parameter.name] as Tupelraum).komponenten.size == einträge.size) { "Die historische Komponentenanzahl passt nicht mehr zum Winkelraum." }
        val verwendet = mutableSetOf<String>()
        val komponenten = einträge.map { eintrag ->
            val felder = (eintrag as? KnotenEigenschaft.Objekt)?.felder ?: error("Ungültiger Komponentenverweis.")
            val id = felder.text("id", ""); val name = felder.text("name", "")
            val ports = kontext.knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang && it.name != "methode" }
            val port = ports.singleOrNull { it.id.wert == id } ?: ports.singleOrNull { it.name == name }
                ?: error("Ein historischer Komponentenanschluss fehlt.")
            verwendet += port.name
            kontext.eingänge[port.name] ?: error("Winkelkomponente '${port.name}' fehlt.")
        }
        val skalare = packung.objekt("skalare")?.felder.orEmpty()
        val argumente = methode.parameter.map { p ->
            if (p.name == parameter.name) BedingterWert(Tupel(komponenten.map { (it.objekt as? MathematischesObjekt ?: error("Eine Winkelkomponente muss ein mathematisches Objekt sein.")) }),
                komponenten.flatMap { it.annahmen }.toSet(), variablenQuellen = komponenten.flatMap { it.variablenQuellen })
            else {
                val port = skalare.entries.singleOrNull { (it.value as? KnotenEigenschaft.Text)?.wert == p.name }?.key
                    ?: error("Der unabhängige Parameter '${p.name}' besitzt keine eindeutige historische Anschlusszuordnung.")
                verwendet += port
                kontext.eingänge[port] ?: error("Argument '$port' fehlt.")
            }
        }
        require(kontext.eingänge.keys.none { it != "methode" && it !in verwendet }) { "Überzähliger verbundener Anschluss außerhalb der gespeicherten Argumentpackung." }
        return argumente
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
    kanonisch: Boolean = false,
): Pair<MathematischesObjekt, MengenAusdruck?> {
    if (kanonisch) {
        val tupel = wert as? Tupel ?: error("Ein kanonisches Methodenergebnis muss ein Tupel sein.")
        if (projektion == METHODEN_ERGEBNISPROJEKTION_DIREKT && tupel.elemente.size == 1) {
            return tupel.elemente.single() to (zielMenge as? Tupelraum)?.komponenten?.singleOrNull()
        }
        return tupel to zielMenge
    }
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

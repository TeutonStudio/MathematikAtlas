package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.KnotenKartenVerwalter.daten.*

const val METHODEN_STRUKTUR_PACKUNG = "methodenAufruf.strukturPackung"
const val METHODEN_STRUKTUR_MIGRATIONSFEHLER = "methodenAufruf.strukturMigrationsfehler"

const val METHODEN_AUFRUF_PROJEKTION_FEST = "methodenAufruf.projektionFest"

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
        kontext.knoten.parameter["methodenAufruf.vertragsfehler"]?.let { error(it) }
        if (kontext.knoten.parameter[METHODEN_AUFRUF_PROJEKTION_FEST] == "true") kontext.knoten.parameter[METHODEN_ANWENDUNG_ERGEBNIS_ART]?.let { art ->
            requireNotNull(methode) { "Der gespeicherte Einzelresultatvertrag benötigt eine bekannte Methodensignatur." }
            MethodenAnforderung.ErgebnisArt(art).prüfe(methode)?.let { error(it) }
        }
        if (methode is AllgemeinAuswertbareMethode) return rufeAllgemeinAuf(methode, methodenWert, kontext)
        val auswertbareMethode = methode as? MathematischAuswertbareMethode
        val signatur = (methode as? SignaturtragendeMethode)?.signatur
            ?: error("Eine Methodenprojektion benötigt eine bekannte Methodensignatur.")
        val symbolisch = methode is MethodenVariable || methode is MethodenWertKomponente
        val mathematischeMethode = methode?.let {
            runCatching { it.alsMathematischeMethode("mathematischen Methodenaufruf") }.getOrNull()
        }
        if (auswertbareMethode == null && !symbolisch) {
            error(
                "Die Methode '${methode?.name}' besitzt keine in diesem Adapter registrierte Auswertungs-Capability. " +
                    "Allgemeine Methoden werden nicht automatisch mathematisch interpretiert.",
            )
        }

        val argumentAnschlüsse = kontext.knoten.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Eingang && it.name != "methode" }
            .sortedBy { it.reihenfolge }
        val argumentProjektion = kontext.knoten.parameter[METHODEN_AUFRUF_ARGUMENTPROJEKTION]
            ?: METHODEN_ARGUMENTPROJEKTION_SEPARIERT
        require(argumentProjektion in setOf(METHODEN_ARGUMENTPROJEKTION_SEPARIERT, METHODEN_ARGUMENTPROJEKTION_TUPEL)) { "Unbekannte Argumentprojektion '$argumentProjektion'." }
        val ergebnisProjektion = kontext.knoten.parameter[METHODEN_AUFRUF_ERGEBNISPROJEKTION]
            ?: if (signatur.ergebnisse.size == 1) METHODEN_ERGEBNISPROJEKTION_DIREKT else METHODEN_ERGEBNISPROJEKTION_TUPEL
        require(ergebnisProjektion in setOf(METHODEN_ERGEBNISPROJEKTION_DIREKT, METHODEN_ERGEBNISPROJEKTION_TUPEL)) { "Unbekannte Ergebnisprojektion '$ergebnisProjektion'." }
        if (ergebnisProjektion == METHODEN_ERGEBNISPROJEKTION_DIREKT) require(signatur.ergebnisse.size == 1) { "Die direkte Projektion benötigt genau eine Ergebniskomponente." }

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
        val argumentWerte = if (METHODEN_STRUKTUR_PACKUNG in kontext.knoten.eigenschaften) {
            strukturierteArgumente(mathematischeMethode ?: error("Die historische Argumentpackung benötigt eine symbolische mathematische Methode."), kontext)
        } else when (argumentProjektion) {
            METHODEN_ARGUMENTPROJEKTION_TUPEL -> tupelArgumente(signatur, argumentAnschlüsse, kontext)
            else -> separierteArgumente(signatur, argumentAnschlüsse, kontext)
        }
        val mathematischeArgumente = argumentWerte.map { wert ->
            wert.objekt as? MathematischesObjekt
                ?: error("Der Mathematikadapter kann das Argument '${wert.anzeigeLatex()}' nicht mathematisch auswerten.")
        }
        val idBindungen = signatur.argumente.mapIndexed { i, k -> k.id to mathematischeArgumente[i] }.toMap()
        val anwendung = if (auswertbareMethode != null) auswertbareMethode.wendeMitIdentitätenAn(idBindungen, kontext.rechenKontext)
            else symbolischerMethodenAufruf(methodenObjekt as MathematischesObjekt, signatur, Tupel(mathematischeArgumente), kontext.rechenKontext)
        val definitionsBedingungen = anwendung.bedingungen
        val roheBindungen = (methode as? MathematischeSignaturtragendeMethode)?.mathematischeSignatur?.argumente
            ?.mapIndexed { i, k -> (k.parameter.bindungsId ?: k.name) to mathematischeArgumente[i] }?.toMap().orEmpty()
        val roherWert = anwendung.ergebnis
        val roheZielMenge = (methode as? MathematischeSignaturtragendeMethode)?.mathematischeSignatur?.zielRaum
            ?.let { ersetze(it, roheBindungen) as MengenAusdruck }

        val (wert, zielMenge) = projiziereMethodenErgebnis(
            wert = roherWert,
            zielMenge = roheZielMenge,
            projektion = ergebnisProjektion,
            kanonisch = true,
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
        val annahmenBindungen = roheBindungen
        val angewandteAnnahmen = methodenWert.annahmen.map { ersetze(it, annahmenBindungen) as Aussage }.toSet() + argumentWerte.flatMap { it.annahmen } + definitionsBedingungen
        require(angewandteAnnahmen.none { it.entscheide(kontext.rechenKontext).wahrheitswert == Wahrheitswert.Lüge }) { "Die Argumente verletzen eine Definitionsbedingung der Methode." }
        if (mathematischeMethode?.argumentIdentitäten?.values?.singleOrNull() == IdentitaetsMethode.ARGUMENT &&
            mathematischeMethode.ergebnisIdentitäten.values.singleOrNull() == IdentitaetsMethode.ERGEBNIS &&
            mathematischeMethode.ergebnisTupel.elemente.singleOrNull() == mathematischeMethode.parameter.singleOrNull() &&
            ergebnisProjektion != METHODEN_ERGEBNISPROJEKTION_TUPEL) {
            return KnotenAuswertungsErgebnis(mapOf("wert" to argumentWerte.single().copy(
                objekt = wert, annahmen = angewandteAnnahmen, zielMenge = zielMenge)))
        }
        return KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "wert" to BedingterWert(
                    objekt = wert,
                    annahmen = angewandteAnnahmen,
                    zielMenge = zielMenge,
                    reelleVariablen = methodenWert.reelleVariablen.filterKeys { it !in annahmenBindungen } + reelleVariablen(argumentWerte),
                    variablenQuellen = (listOf(methodenWert) + argumentWerte).flatMap { it.variablenQuellen },
                    latexDarstellung = projiziertesLatex,
                    übersetzungen = ÜbersetzungsMetadaten(herkunft = ((listOf(methodenWert) + argumentWerte).flatMap { it.übersetzungen.alsHerkunft().herkunft } + mathematischeMethode?.übersetzungen?.alsHerkunft()?.herkunft.orEmpty()).distinct()),
                ),
            ),
        )
    }

    private fun rufeAllgemeinAuf(methode: AllgemeinAuswertbareMethode, methodenWert: BedingterWert, kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        require(kontext.knoten.parameter[METHODEN_AUFRUF_PARTIELL] != "true") { "Diese allgemeine Methode unterstützt keine partielle Bindung." }
        val ports = kontext.knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang && it.name != "methode" }.sortedBy { it.reihenfolge }
        val komponenten = methode.signatur.argumente
        val argumentProjektion = kontext.knoten.parameter[METHODEN_AUFRUF_ARGUMENTPROJEKTION] ?: METHODEN_ARGUMENTPROJEKTION_SEPARIERT
        require(argumentProjektion in setOf(METHODEN_ARGUMENTPROJEKTION_SEPARIERT, METHODEN_ARGUMENTPROJEKTION_TUPEL)) { "Unbekannte Argumentprojektion '$argumentProjektion'." }
        val tupelModus = argumentProjektion == METHODEN_ARGUMENTPROJEKTION_TUPEL
        val werte = if (tupelModus && komponenten.isEmpty()) {
            require(ports.none { it.name in kontext.eingänge }) { "Eine nullstellige Methode besitzt kein verbundenes Argument." }
            emptyMap()
        } else if (tupelModus) {
            require(ports.size == 1) { "Die Tupelprojektion benötigt genau einen Anschluss." }
            val wert = kontext.eingänge[ports.single().name] ?: error("Argumenttupel fehlt.")
            val tupel = wert.objekt as? Tupel ?: error("Ein echtes Argumenttupel wird benötigt.")
            require(tupel.elemente.size == komponenten.size) { "Argumentanzahl stimmt nicht mit der Signatur überein." }
            komponenten.mapIndexed { i, c -> c.id to projiziereArgumentKomponente(wert, tupel.elemente[i], i) }.toMap()
        } else {
            val zugeordnet = signaturPorts(methode.signatur, ports, kontext)
            komponenten.mapIndexed { i, c -> c.id to (kontext.eingänge[zugeordnet[i].name] ?: error("Argument '${c.name}' fehlt.")) }.toMap()
        }

        val annahmen = methodenWert.annahmen + werte.values.flatMap { it.annahmen }
        require(annahmen.none { it.entscheide(kontext.rechenKontext).wahrheitswert == Wahrheitswert.Lüge }) { "Die Argumente verletzen eine Definitionsbedingung der Methode." }
        val projektion = kontext.knoten.parameter[METHODEN_AUFRUF_ERGEBNISPROJEKTION]
        require(projektion == null || projektion in setOf(METHODEN_ERGEBNISPROJEKTION_DIREKT, METHODEN_ERGEBNISPROJEKTION_TUPEL)) { "Unbekannte Ergebnisprojektion '$projektion'." }
        if (projektion == METHODEN_ERGEBNISPROJEKTION_DIREKT) require(methode.signatur.ergebnisse.size == 1) { "Die direkte Projektion benötigt genau eine Ergebniskomponente." }
        val ergebnisse = methode.rufeAuf(werte.mapValues { it.value.objekt })
        require(ergebnisse.keys == methode.signatur.ergebnisse.map { it.id }.toSet()) { "Ergebnisidentitäten stimmen nicht mit der Signatur überein." }
        val geordnet = methode.signatur.ergebnisse.map { c ->
            val ergebnis = ergebnisse.getValue(c.id)
            val durchgereicht = ergebnis.durchgereichtesArgument?.let { id ->
                val wert = werte[id] ?: error("Die Methode verweist auf ein unbekanntes Argument.")
                require(wert.objekt == ergebnis.wert) { "Der durchgereichte Wert stimmt nicht mit seinem Argument überein." }
                wert
            }
            (durchgereicht ?: BedingterWert(
                ergebnis.wert,
                variablenQuellen = (listOf(methodenWert) + werte.values).flatMap { it.variablenQuellen }.distinct(),
                übersetzungen = ÜbersetzungsMetadaten(herkunft = (listOf(methodenWert) + werte.values)
                    .flatMap { it.übersetzungen.alsHerkunft().herkunft }.distinct()),
            )).copy(
                objekt = ergebnis.wert,
                annahmen = methodenWert.annahmen + werte.values.flatMap { it.annahmen },
            )
        }
        val direkt = projektion != METHODEN_ERGEBNISPROJEKTION_TUPEL && geordnet.size == 1
        val wert = if (direkt) geordnet.single() else BedingterWert(
            Tupel(geordnet.map { it.objekt as? MathematischesObjekt ?: error("Dieses Ergebnis kann nicht als mathematisches Tupel verpackt werden.") }),
            annahmen = geordnet.flatMap { it.annahmen }.toSet(),
            variablenQuellen = geordnet.flatMap { it.variablenQuellen },
            übersetzungen = ÜbersetzungsMetadaten(herkunft = geordnet.flatMap { it.übersetzungen.alsHerkunft().herkunft }.distinct()),
        )
        return KnotenAuswertungsErgebnis(mapOf("wert" to wert))
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

    private fun signaturPorts(signatur: MethodenSignatur, ports: List<AnschlussDaten>, kontext: KnotenAuswertungsKontext): List<AnschlussDaten> {
        require(ports.map { it.name }.distinct().size == ports.size && ports.map { it.id }.distinct().size == ports.size) { "Argumentanschlüsse sind nicht eindeutig." }
        val ids = signatur.argumente.map { it.id }
        val gespeicherteIds = ports.mapNotNull { it.semantischeId }
        require(gespeicherteIds.distinct().size == gespeicherteIds.size && gespeicherteIds.all { it in ids }) { "Argumentidentitäten stimmen nicht mit der Signatur überein." }
        val benutzt = mutableSetOf<AnschlussId>()
        val zugeordnet = signatur.argumente.mapIndexed { i, c ->
            val port = ports.singleOrNull { it.semantischeId == c.id }
                ?: ports.getOrNull(i)?.takeIf { it.semantischeId == null && it.id !in benutzt }
                ?: error("Argumentanschluss '${c.name}' fehlt oder ist nicht eindeutig zugeordnet.")
            require(benutzt.add(port.id)) { "Eine Argumentkomponente wurde mehrfach gebunden." }
            port
        }
        require(ports.filter { it.id !in benutzt }.none { it.name in kontext.eingänge }) { "Überzählige Argumente." }
        return zugeordnet
    }

    private fun separierteArgumente(signatur: MethodenSignatur, ports: List<AnschlussDaten>, kontext: KnotenAuswertungsKontext): List<BedingterWert> =
        signaturPorts(signatur, ports, kontext).map { kontext.eingänge[it.name] ?: error("Argument '${it.name}' fehlt.") }

    private fun tupelArgumente(signatur: MethodenSignatur, ports: List<AnschlussDaten>, kontext: KnotenAuswertungsKontext): List<BedingterWert> {
        if (signatur.argumente.isEmpty()) {
            require(ports.none { it.name in kontext.eingänge }) { "Eine nullstellige Methode besitzt kein verbundenes Argument." }
            return emptyList()
        }
        require(ports.size == 1) { "Die Tupelprojektion benötigt genau einen Anschluss." }
        val wert = kontext.eingänge[ports.single().name] ?: error("Das Argumenttupel fehlt.")
        val tupel = wert.objekt as? Tupel ?: error("Ein echtes Argumenttupel wird benötigt; ein Skalar wird nicht automatisch verpackt.")
        require(tupel.anzahl == signatur.argumente.size) { "Die Argumentanzahl stimmt nicht mit der Signatur überein." }
        return tupel.elemente.mapIndexed { i, element -> projiziereArgumentKomponente(wert, element, i) }
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

/** Eine Komponentenprojektion übernimmt keine Metadaten des gesamten Tupels. */
internal fun projiziereArgumentKomponente(wert: BedingterWert, element: MathematischesObjekt, index: Int): BedingterWert = wert.copy(
    objekt = element,
    zielMenge = (wert.zielMenge as? Tupelraum)?.komponenten?.getOrNull(index) ?: wert.komponentenMengen?.getOrNull(index),
    werteVorrat = (wert.werteVorrat as? Tupelraum)?.komponenten?.getOrNull(index),
    komponentenMengen = null,
    elementArt = null,
    latexDarstellung = null,
    gebundeneFormel = null,
    symbolischeMethode = element is MethodenVariable || element is MethodenWertKomponente,
    übersetzungen = wert.übersetzungen.alsHerkunft(),
)

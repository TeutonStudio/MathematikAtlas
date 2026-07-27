from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: erwartete genau 1 Fundstelle, gefunden {count}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


replace_once(
    "MathematikKartenAdapter/src/main/kotlin/de/TeutonStudio/MathematikKartenAdapter/Auswertung.kt",
    '''data class VariablenQuelle(
    val knotenId: KnotenId,
    val name: String,
    val werteVorrat: MengenAusdruck,
)
''',
    '''data class VariablenQuelle(
    val knotenId: KnotenId,
    val name: String,
    val werteVorrat: MengenAusdruck,
    /** Nur echte Parameterknoten werden in die Signatur einer mit „Term zu Methode“ erzeugten Methode übernommen. */
    val alsMethodenParameter: Boolean = true,
)
''',
)

replace_once(
    "MathematikKartenAdapter/src/main/kotlin/de/TeutonStudio/MathematikKartenAdapter/KartenAuswerter.kt",
    '''            ausgabe.copy(variablenQuellen = (ausgabe.variablenQuellen + quellen)
                .distinctBy { quelle -> Triple(quelle.knotenId, quelle.name, quelle.werteVorrat) })
''',
    '''            ausgabe.copy(variablenQuellen = (ausgabe.variablenQuellen + quellen)
                .distinctBy { quelle -> Pair(Triple(quelle.knotenId, quelle.name, quelle.werteVorrat), quelle.alsMethodenParameter) })
''',
)

replace_once(
    "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/MathematikAuswerter.kt",
    '''        registriere("mathematik.kartenEingang") { k ->
            val name = k.knoten.parameter["name"] ?: "x"
            KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(Variable(name))))
        }
''',
    '''        registriere("mathematik.kartenEingang") { k ->
            val name = k.knoten.parameter["name"]?.trim().orEmpty().ifBlank { "x" }
            val ausgangsArt = k.knoten.anschlüsse.firstOrNull {
                it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Ausgang
            }?.art ?: MathematikAnschlussArten.Objekt.id
            val parameter: FunktionsParameter = if (ausgangsArt == MathematikAnschlussArten.Zahl.id) Variable(name) else AllgemeinerParameter(name)
            val werteVorrat: MengenAusdruck = when (ausgangsArt) {
                MathematikAnschlussArten.Zahl.id -> ReelleZahlen
                MathematikAnschlussArten.Aussage.id -> EndlicheMenge(setOf(WahrheitsKonstante(true), WahrheitsKonstante(false)))
                MathematikAnschlussArten.Menge.id -> BenannteMenge("mengen_$name", "\\mathcal{P}(\\mathcal{U})")
                else -> BenannteMenge("werte_$name", "\\mathcal{W}_{${name}}")
            }
            KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(
                objekt = parameter,
                werteVorrat = werteVorrat,
                reelleVariablen = if (parameter is Variable) mapOf(name to werteVorrat) else emptyMap(),
                variablenQuellen = listOf(VariablenQuelle(k.knoten.id, name, werteVorrat, alsMethodenParameter = false)),
            )))
        }
''',
)

replace_once(
    "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/MathematikAuswerter.kt",
    '''        registriere("mathematik.termZuMethode") { k ->
            val termWert = k.eingänge["term"] ?: error("Term fehlt.")
            val term = termWert.objekt
            val freieParameter = term.freieFunktionsParameter().associateBy { it.name }
            val quellenNachName = termWert.variablenQuellen
                .filter { it.name in freieParameter }
                .groupBy { it.name }
            val fehlende = freieParameter.keys.filterNot { it in quellenNachName }
            require(fehlende.isEmpty()) { "Für die Parameter ${fehlende.joinToString(", ")} fehlt ein verbundener Parameterknoten." }
            val werteVorräteNachName = quellenNachName.mapValues { (name, quellen) ->
                val mengen = quellen.map { it.werteVorrat }.distinct()
                require(mengen.size == 1) { "Die Variable '$name' besitzt widersprüchliche Wertevorräte." }
                mengen.single()
            }
            val automatisch = quellenNachName.entries.sortedWith(
                compareBy<Map.Entry<String, List<VariablenQuelle>>> { entry ->
                    entry.value.minOf { quelle -> k.topologischeReihenfolge[quelle.knotenId] ?: Int.MAX_VALUE }
                }.thenBy { entry -> entry.value.minOf { quelle -> quelle.knotenId.wert } },
            ).map { it.key }
            val gespeichert = k.knoten.parameter["argumentReihenfolge"].orEmpty()
                .split(',').map(String::trim).filter { it.isNotBlank() && it in freieParameter }.distinct()
            val namen = gespeichert + automatisch.filterNot { it in gespeichert }
            val parameter = namen.map { freieParameter.getValue(it) }
            val werteVorräte = namen.associateWith { werteVorräteNachName.getValue(it) }
            val zielmenge = inferiereZielmenge(term, werteVorräte, termWert.annahmen)
            val funktion = Funktion(k.knoten.parameter["name"] ?: "f", parameter, mapOf("wert" to term), mapOf("wert" to zielmenge), werteVorräte)
            KnotenAuswertungsErgebnis(mapOf("methode" to BedingterWert(funktion, annahmen(k))))
        }
''',
    '''        registriere("mathematik.termZuMethode") { k ->
            val termWert = k.eingänge["term"] ?: error("Term fehlt.")
            val term = termWert.objekt
            val freieParameter = term.freieFunktionsParameter().associateBy { it.name }
            val quellenNachName = termWert.variablenQuellen
                .filter { it.name in freieParameter }
                .groupBy { it.name }
            val fehlende = freieParameter.keys.filterNot { it in quellenNachName }
            require(fehlende.isEmpty()) { "Für die Parameter ${fehlende.joinToString(", ")} fehlt ein verbundener Parameter- oder Karten-Eingang." }
            val werteVorräteNachName = quellenNachName.mapValues { (name, quellen) ->
                val mengen = quellen.map { it.werteVorrat }.distinct()
                require(mengen.size == 1) { "Die Variable '$name' besitzt widersprüchliche Wertevorräte." }
                mengen.single()
            }
            val methodenQuellen = quellenNachName.filterValues { quellen -> quellen.any { it.alsMethodenParameter } }
            val automatisch = methodenQuellen.entries.sortedWith(
                compareBy<Map.Entry<String, List<VariablenQuelle>>> { entry ->
                    entry.value.minOf { quelle -> k.topologischeReihenfolge[quelle.knotenId] ?: Int.MAX_VALUE }
                }.thenBy { entry -> entry.value.minOf { quelle -> quelle.knotenId.wert } },
            ).map { it.key }
            val gespeichert = k.knoten.parameter["argumentReihenfolge"].orEmpty()
                .split(',').map(String::trim).filter { it.isNotBlank() && it in methodenQuellen }.distinct()
            val namen = gespeichert + automatisch.filterNot { it in gespeichert }
            val parameter = namen.map { freieParameter.getValue(it) }
            val werteVorräte = namen.associateWith { werteVorräteNachName.getValue(it) }
            val zielmenge = inferiereZielmenge(term, werteVorräteNachName, termWert.annahmen)
            val funktion = Funktion(k.knoten.parameter["name"] ?: "f", parameter, mapOf("wert" to term), mapOf("wert" to zielmenge), werteVorräte)
            KnotenAuswertungsErgebnis(mapOf("methode" to BedingterWert(funktion, annahmen(k))))
        }
''',
)

replace_once(
    "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/MathematikAuswerter.kt",
    '''            val gemeinsameQuellen = eingangsWerte.flatMap { it.variablenQuellen }
                .distinctBy { quelle -> Triple(quelle.knotenId, quelle.name, quelle.werteVorrat) }
''',
    '''            val gemeinsameQuellen = eingangsWerte.flatMap { it.variablenQuellen }
                .distinctBy { quelle -> Pair(Triple(quelle.knotenId, quelle.name, quelle.werteVorrat), quelle.alsMethodenParameter) }
''',
)

replace_once(
    "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/MathematikAuswerter.kt",
    '''        registriere("mathematik.endlicheMenge") { k ->
            val elemente = (k.knoten.parameter["elemente"] ?: "").split(',').filter { it.isNotBlank() }.map { RationaleZahl.parse(it) }.toSet()
            KnotenAuswertungsErgebnis(mapOf("menge" to BedingterWert(EndlicheMenge(elemente))))
        }
        registriere("mathematik.reellesIntervall") { k ->
''',
    '''        registriere("mathematik.endlicheMenge") { k ->
            val elemente = (k.knoten.parameter["elemente"] ?: "").split(',').filter { it.isNotBlank() }.map { RationaleZahl.parse(it) }.toSet()
            KnotenAuswertungsErgebnis(mapOf("menge" to BedingterWert(EndlicheMenge(elemente))))
        }
        registriere("mathematik.einzelmenge") { k ->
            val element = k.eingänge["element"] ?: error("Element fehlt.")
            KnotenAuswertungsErgebnis(mapOf("menge" to BedingterWert(
                objekt = EndlicheMenge(setOf(element.objekt)),
                annahmen = element.annahmen,
                reelleVariablen = element.reelleVariablen,
                variablenQuellen = element.variablenQuellen,
            )))
        }
        registriere("mathematik.mengenfilter") { k ->
            val methode = k.eingänge["methode"]?.objekt as? Funktion ?: error("Filtermethode fehlt.")
            val gefiltert = filtereMenge(k.menge("menge"), methode, k.rechenKontext)
            KnotenAuswertungsErgebnis(mapOf("menge" to BedingterWert(
                objekt = gefiltert,
                annahmen = annahmen(k),
                reelleVariablen = reelleVariablen(k.eingänge.values),
            )))
        }
        registriere("mathematik.reellesIntervall") { k ->
''',
)

replace_once(
    "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/MathematikKnotenVorlagen.kt",
    '''    val EndlicheMenge = KnotenVorlage(
        "mathematik.endlicheMenge", "Endliche Menge", "Mengen", "Endliche Menge aus einer kommagetrennten Liste.", GraphGröße(220f, 105f),
        listOf(ausgang("menge", MathematikAnschlussArten.Menge.id)), mapOf("elemente" to "1,2,3"),
    )
    val ReellesIntervall = KnotenVorlage(
''',
    '''    val EndlicheMenge = KnotenVorlage(
        "mathematik.endlicheMenge", "Endliche Menge", "Mengen", "Endliche Menge aus einer kommagetrennten Liste.", GraphGröße(220f, 105f),
        listOf(ausgang("menge", MathematikAnschlussArten.Menge.id)), mapOf("elemente" to "1,2,3"),
    )
    val Einzelmenge = KnotenVorlage(
        "mathematik.einzelmenge", "Einzelmenge", "Mengen", "Bildet aus einem beliebigen mathematischen Objekt die Menge, die genau dieses Element enthält.", GraphGröße(225f, 105f),
        listOf(eingang("element", MathematikAnschlussArten.Objekt.id), ausgang("menge", MathematikAnschlussArten.Menge.id)),
    )
    val Mengenfilter = KnotenVorlage(
        "mathematik.mengenfilter", "Mengenfilter", "Mengen", "Behält genau die Elemente einer Menge, für die eine einstellige Methode eine wahre Aussage liefert.", GraphGröße(270f, 120f),
        listOf(eingang("menge", MathematikAnschlussArten.Menge.id, 0), eingang("methode", MathematikAnschlussArten.Funktion.id, 1), ausgang("menge", MathematikAnschlussArten.Menge.id)),
    )
    val ReellesIntervall = KnotenVorlage(
''',
)

replace_once(
    "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/MathematikKnotenVorlagen.kt",
    '''Ableiten, Integrieren, EndlicheMenge, ReellesIntervall, Lösungsmenge''',
    '''Ableiten, Integrieren, EndlicheMenge, Einzelmenge, Mengenfilter, ReellesIntervall, Lösungsmenge''',
)

replace_once(
    "MathematikRechenSystem/src/main/kotlin/de/TeutonStudio/MathematikRechenSystem/kern/Mengen.kt",
    '''sealed interface Mächtigkeit : MathematischesObjekt
''',
    '''/** Symbolischer Filter einer Menge durch eine einstellige aussagewertige Methode. */
data class GefilterteMenge(
    val menge: MengenAusdruck,
    val methode: Funktion,
) : MengenAusdruck {
    init {
        require(methode.parameter.size == 1) { "Eine Filtermethode benötigt genau einen Elementparameter." }
        require(methode.ausgaben.size == 1 && methode.ausgaben.values.single() is Aussage) {
            "Eine Filtermethode muss genau eine Aussage ausgeben."
        }
    }

    override fun zuLatex(): String {
        val parameter = methode.parameter.single()
        val bedingung = methode.ausgaben.values.single() as Aussage
        return "\\left\\{${parameter.zuLatex()}\\in${menge.zuLatex()}\\mid ${bedingung.zuLatex()}\\right\\}"
    }
}

/** Filtert endliche Mengen exakt und bewahrt andernfalls die symbolische Filterdefinition. */
fun filtereMenge(
    menge: MengenAusdruck,
    methode: Funktion,
    kontext: RechenKontext = RechenKontext(),
): MengenAusdruck {
    require(methode.parameter.size == 1) { "Eine Filtermethode benötigt genau einen Elementparameter." }
    val (ausgabeName, ausgabe) = methode.einzigeAusgabe()
    require(ausgabe is Aussage) { "Eine Filtermethode muss eine Aussage ausgeben." }
    if (menge == LeereMenge) return LeereMenge
    if (menge is EndlicheMenge) {
        val parameter = methode.parameter.single()
        val behalten = linkedSetOf<MathematischesObjekt>()
        for (element in menge.elemente.sortedBy(::strukturellerSchlüssel)) {
            val bedingung = methode.wendeAn(mapOf(parameter.name to element)).getValue(ausgabeName) as Aussage
            when (bedingung.entscheide(kontext).wahrheitswert) {
                Wahrheitswert.Wahr -> behalten += element
                Wahrheitswert.Falsch -> Unit
                null -> return GefilterteMenge(menge, methode)
            }
        }
        return if (behalten.isEmpty()) LeereMenge else EndlicheMenge(behalten)
    }
    return GefilterteMenge(menge, methode)
}

sealed interface Mächtigkeit : MathematischesObjekt
''',
)

replace_once(
    "MathematikRechenSystem/src/main/kotlin/de/TeutonStudio/MathematikRechenSystem/kern/Mengen.kt",
    '''        NatürlicheZahlen -> if (element is RationaleZahl) {
            val wahr = element.nenner == java.math.BigInteger.ONE && element.zähler.signum() >= 0
            AussageErgebnis(if (wahr) Wahrheitswert.Wahr else Wahrheitswert.Falsch, if (wahr) EntscheidungsStatus.Bewiesen else EntscheidungsStatus.Widerlegt)
        } else AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
        else -> AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
''',
    '''        NatürlicheZahlen -> if (element is RationaleZahl) {
            val wahr = element.nenner == java.math.BigInteger.ONE && element.zähler.signum() >= 0
            AussageErgebnis(if (wahr) Wahrheitswert.Wahr else Wahrheitswert.Falsch, if (wahr) EntscheidungsStatus.Bewiesen else EntscheidungsStatus.Widerlegt)
        } else AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
        is GefilterteMenge -> {
            val grundErgebnis = ElementBeziehung(element, menge.menge).entscheide(kontext)
            if (grundErgebnis.wahrheitswert == Wahrheitswert.Falsch) grundErgebnis else {
                val parameter = menge.methode.parameter.single()
                val bedingung = menge.methode.wendeAn(mapOf(parameter.name to element)).values.single() as Aussage
                val filterErgebnis = bedingung.entscheide(kontext)
                when {
                    filterErgebnis.wahrheitswert == Wahrheitswert.Falsch -> filterErgebnis
                    grundErgebnis.wahrheitswert == Wahrheitswert.Wahr && filterErgebnis.wahrheitswert == Wahrheitswert.Wahr ->
                        AussageErgebnis(Wahrheitswert.Wahr, EntscheidungsStatus.Bewiesen)
                    else -> AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
                }
            }
        }
        else -> AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
''',
)

replace_once(
    "MathematikRechenSystem/src/main/kotlin/de/TeutonStudio/MathematikRechenSystem/kern/Funktionen.kt",
    '''    is DefinierteMenge -> {
        val gebundeneNamen = objekt.variablen.map { it.variable.name }.toSet()
        val freieBindungen = bindungen - gebundeneNamen
        objekt.copy(
            variablen = objekt.variablen.map { it.copy(grundMenge = ersetze(it.grundMenge, freieBindungen) as MengenAusdruck) },
            bedingung = ersetze(objekt.bedingung, freieBindungen),
        )
    }
    is Gleichheit -> Gleichheit(ersetze(objekt.links, bindungen), ersetze(objekt.rechts, bindungen))
''',
    '''    is DefinierteMenge -> {
        val gebundeneNamen = objekt.variablen.map { it.variable.name }.toSet()
        val freieBindungen = bindungen - gebundeneNamen
        objekt.copy(
            variablen = objekt.variablen.map { it.copy(grundMenge = ersetze(it.grundMenge, freieBindungen) as MengenAusdruck) },
            bedingung = ersetze(objekt.bedingung, freieBindungen),
        )
    }
    is GefilterteMenge -> filtereMenge(
        ersetze(objekt.menge, bindungen) as MengenAusdruck,
        ersetze(objekt.methode, bindungen) as Funktion,
    )
    is Gleichheit -> Gleichheit(ersetze(objekt.links, bindungen), ersetze(objekt.rechts, bindungen))
''',
)

replace_once(
    "MathematikRechenSystem/src/main/kotlin/de/TeutonStudio/MathematikRechenSystem/kern/Funktionen.kt",
    '''    is Schnitt -> schneide(objekt.mengen, objekt.grundMenge)
    is FallAusdruck -> {
''',
    '''    is Schnitt -> schneide(objekt.mengen, objekt.grundMenge)
    is GefilterteMenge -> filtereMenge(objekt.menge, objekt.methode, kontext)
    is FallAusdruck -> {
''',
)

replace_once(
    "MathematikRechenSystem/src/main/kotlin/de/TeutonStudio/MathematikRechenSystem/kern/Funktionen.kt",
    '''    is DefinierteMenge -> {
        val gebundeneNamen = variablen.map { it.variable.name }.toSet()
        variablen.map { it.grundMenge }.enthalteneFunktionsParameter() +
            bedingung.enthalteneFunktionsParameter().filterNot { it.name in gebundeneNamen }
    }
    is Tupel -> elemente.enthalteneFunktionsParameter()
''',
    '''    is DefinierteMenge -> {
        val gebundeneNamen = variablen.map { it.variable.name }.toSet()
        variablen.map { it.grundMenge }.enthalteneFunktionsParameter() +
            bedingung.enthalteneFunktionsParameter().filterNot { it.name in gebundeneNamen }
    }
    is GefilterteMenge -> setOf(menge, methode).enthalteneFunktionsParameter()
    is Tupel -> elemente.enthalteneFunktionsParameter()
''',
)

replace_once(
    "MathematikRechenSystem/src/main/kotlin/de/TeutonStudio/MathematikRechenSystem/kern/Wertebereiche.kt",
    '''    is DefinierteMenge -> if (menge.variablen.size == 1) menge.variablen.single().grundMenge
        else Tupelraum(menge.variablen.map { it.grundMenge })
    is Abbild -> menge.methode.einzigeZielMenge
''',
    '''    is DefinierteMenge -> if (menge.variablen.size == 1) menge.variablen.single().grundMenge
        else Tupelraum(menge.variablen.map { it.grundMenge })
    is GefilterteMenge -> inferiereElementMenge(menge.menge, werteVorräte, annahmen)
    is Abbild -> menge.methode.einzigeZielMenge
''',
)

Path("MathematikKnoten/src/test/kotlin/de/TeutonStudio/MathematikKnoten/MengenKnotenTest.kt").write_text('''package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.VariablenQuelle
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class MengenKnotenTest {
    private val register = StandardMathematikAuswerter.erzeugeRegister()

    @Test
    fun `Einzelmenge nimmt beliebiges Objekt auf`() {
        val knoten = MathematikKnotenVorlagen.Einzelmenge.erzeuge(GraphPunkt.Zero)
        val aussage = WahrheitsKonstante(true)
        val ergebnis = register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(knoten, mapOf("element" to BedingterWert(aussage)), RechenKontext()),
        )

        assertEquals(EndlicheMenge(setOf(aussage)), ergebnis.ausgaben.getValue("menge").objekt)
    }

    @Test
    fun `Mengenfilter wertet endliche Menge exakt aus`() {
        val x = Variable("x")
        val wahrheitsmenge = EndlicheMenge(setOf(WahrheitsKonstante(true), WahrheitsKonstante(false)))
        val methode = Funktion(
            name = "positiv",
            parameter = listOf(x),
            ausgaben = mapOf("aussage" to Vergleich(x, VergleichsArt.Größer, RationaleZahl.Null)),
            zielMengen = mapOf("aussage" to wahrheitsmenge),
            werteVorräte = mapOf("x" to ReelleZahlen),
        )
        val menge = EndlicheMenge(setOf(RationaleZahl.von(-2), RationaleZahl.Null, RationaleZahl.von(3)))
        val knoten = MathematikKnotenVorlagen.Mengenfilter.erzeuge(GraphPunkt.Zero)
        val ergebnis = register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf("menge" to BedingterWert(menge), "methode" to BedingterWert(methode)),
                RechenKontext(),
            ),
        )

        assertEquals(EndlicheMenge(setOf(RationaleZahl.von(3))), ergebnis.ausgaben.getValue("menge").objekt)
    }

    @Test
    fun `Mengenfilter bewahrt unendliche Filter symbolisch`() {
        val x = Variable("x")
        val methode = Funktion(
            name = "positiv",
            parameter = listOf(x),
            ausgaben = mapOf("aussage" to Vergleich(x, VergleichsArt.Größer, RationaleZahl.Null)),
            zielMengen = mapOf("aussage" to EndlicheMenge(setOf(WahrheitsKonstante(true), WahrheitsKonstante(false)))),
            werteVorräte = mapOf("x" to ReelleZahlen),
        )

        assertIs<GefilterteMenge>(filtereMenge(ReelleZahlen, methode))
    }

    @Test
    fun `Karten-Eingänge werden nicht zu Parametern der inneren Methode`() {
        val kartenEingang = MathematikKnotenVorlagen.KartenEingang.erzeuge(GraphPunkt.Zero).let { knoten ->
            knoten.copy(
                parameter = knoten.parameter + ("name" to "min"),
                anschlüsse = knoten.anschlüsse.map { anschluss ->
                    if (anschluss.richtung == AnschlussRichtung.Ausgang) anschluss.copy(art = MathematikAnschlussArten.Zahl.id) else anschluss
                },
            )
        }
        val minWert = register.finde(kartenEingang.art)!!.auswerten(
            KnotenAuswertungsKontext(kartenEingang, emptyMap(), RechenKontext()),
        ).ausgaben.getValue("wert")
        assertFalse(minWert.variablenQuellen.single().alsMethodenParameter)

        val x = Variable("x")
        val termWert = BedingterWert(
            objekt = addition(listOf(x, minWert.objekt as ZahlAusdruck)),
            reelleVariablen = mapOf("x" to ReelleZahlen, "min" to ReelleZahlen),
            variablenQuellen = listOf(
                VariablenQuelle(KnotenId("x-quelle"), "x", ReelleZahlen),
                minWert.variablenQuellen.single(),
            ),
        )
        val termZuMethode = MathematikKnotenVorlagen.TermZuMethode.erzeuge(GraphPunkt.Zero)
        val methode = assertIs<Funktion>(register.finde(termZuMethode.art)!!.auswerten(
            KnotenAuswertungsKontext(
                termZuMethode,
                mapOf("term" to termWert),
                RechenKontext(),
                topologischeReihenfolge = mapOf(KnotenId("x-quelle") to 0, kartenEingang.id to 1),
            ),
        ).ausgaben.getValue("methode").objekt)

        assertEquals(listOf("x"), methode.parameter.map { it.name })
        assertEquals(setOf("x"), methode.werteVorräte.keys)
    }
}
''', encoding="utf-8")

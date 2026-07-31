package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.math.BigInteger
import de.TeutonStudio.MathematikKartenAdapter.*

internal const val KARTEN_TABELLEN_PREFIX = "kartenWahrheitstabelle"

internal data class KartenTabellenAnschluss(
    val name: String,
    val art: AnschlussArtId,
    val äußererAnschluss: AnschlussDaten,
    val innererKnoten: KnotenDaten,
)

internal data class KartenWahrheitstabellenQuelle(
    val verweis: KartenVerweis,
    val karte: KartenDaten,
    val eingänge: List<KartenTabellenAnschluss>,
    val aussageAusgänge: List<KartenTabellenAnschluss>,
    val weitereAusgänge: List<KartenTabellenAnschluss>,
)

internal fun ermittleKartenWahrheitstabellenQuelle(
    register: AnschlussArtRegister,
    knoten: KnotenDaten,
    karte: KartenDaten,
): KartenWahrheitstabellenQuelle? {
    val verweis = knoten.kartenVerweis ?: return null
    val interneEingänge = karte.knoten
        .filter { it.art == "mathematik.kartenEingang" }
        .distinctBy(::kartenTabellenÖffentlicherName)
        .associateBy(::kartenTabellenÖffentlicherName)
    val interneAusgänge = karte.knoten
        .filter { it.art == "mathematik.kartenAusgang" }
        .distinctBy(::kartenTabellenÖffentlicherName)
        .associateBy(::kartenTabellenÖffentlicherName)

    val eingänge = knoten.anschlüsse
        .filter { it.richtung == AnschlussRichtung.Eingang }
        .sortedBy { it.reihenfolge }
        .mapNotNull { anschluss ->
            interneEingänge[anschluss.name]?.let {
                KartenTabellenAnschluss(anschluss.name, anschluss.art, anschluss, it)
            }
        }
    val ausgänge = knoten.anschlüsse
        .filter { it.richtung == AnschlussRichtung.Ausgang }
        .sortedBy { it.reihenfolge }
        .mapNotNull { anschluss ->
            interneAusgänge[anschluss.name]?.let {
                KartenTabellenAnschluss(anschluss.name, anschluss.art, anschluss, it)
            }
        }
    val (aussagen, weitere) = ausgänge.partition {
        register.istUnterart(it.art, MathematikAnschlussArten.Aussage.id)
    }
    if (aussagen.isEmpty()) return null
    return KartenWahrheitstabellenQuelle(verweis, karte, eingänge, aussagen, weitere)
}

internal fun kartenWahrheitstabellenZeilenAnzahl(freieLogischeEingänge: Int): BigInteger =
    BigInteger.ONE.shiftLeft(freieLogischeEingänge.coerceAtLeast(0))

internal fun erzeugeTabellenPrädikat(
    name: String,
    definitionsMengen: List<MengenAusdruck>,
    argumente: List<MathematischesObjekt>,
    wert: Boolean,
): Funktion {
    require(definitionsMengen.isNotEmpty()) { "Ein Prädikat benötigt mindestens eine Definitionsmenge." }
    require(definitionsMengen.size == argumente.size) { "Für jede Definitionsmenge wird genau ein Argument benötigt." }
    val parameter: List<FunktionsParameter> = definitionsMengen.indices.map { AllgemeinerParameter("x${it + 1}") }
    val treffer = parameter.zip(argumente)
        .map { (parameterWert, argument) -> Gleichheit(parameterWert, argument) }
        .let { aussagen -> if (aussagen.size == 1) aussagen.single() else Konjunktion(aussagen) }
    val ausgabe = FallAusdruck(
        wahr = WahrheitsKonstante(wert),
        aussage = treffer,
        lüge = UnentscheidbareAussage("$name außerhalb der Tabellenbelegung", "Wahrheitstabelle"),
    ) as Aussage
    val wahrheitsMenge = EndlicheMenge(setOf(WahrheitsKonstante(true), WahrheitsKonstante(false)))
    return Funktion(
        name = name,
        parameter = parameter,
        ausgaben = mapOf("aussage" to ausgabe),
        zielMengen = mapOf("aussage" to wahrheitsMenge),
        werteVorräte = parameter.zip(definitionsMengen).associate { (variable, menge) -> variable.name to menge },
    )
}

internal fun kartenTabellenÖffentlicherName(knoten: KnotenDaten): String =
    knoten.parameter["name"]?.trim()?.takeIf(String::isNotEmpty) ?: knoten.name

internal fun kartenTabellenWertSchlüssel(feld: KartenTabellenAnschluss) =
    "$KARTEN_TABELLEN_PREFIX.eingang.${feld.äußererAnschluss.id.wert}"

internal fun kartenTabellenPrädikatMengenSchlüssel(feld: KartenTabellenAnschluss) =
    "$KARTEN_TABELLEN_PREFIX.praedikat.${feld.äußererAnschluss.id.wert}.mengen"

internal fun kartenTabellenPrädikatArgumentSchlüssel(feld: KartenTabellenAnschluss, index: Int) =
    "$KARTEN_TABELLEN_PREFIX.praedikat.${feld.äußererAnschluss.id.wert}.argument.$index"

internal fun standardWertFürKartenTabelle(art: AnschlussArtId): String = when (art) {
    MathematikAnschlussArten.Zahl.id -> "0"
    MathematikAnschlussArten.Menge.id -> "{}"
    else -> "x"
}

internal fun standardArgumentFürKartenTabelle(menge: MengenAusdruck): String = when (menge) {
    NatürlicheZahlen, GanzeZahlen, RationaleZahlen, ReelleZahlen, KomplexeZahlen -> "0"
    else -> "a"
}

internal fun parseKartenTabellenWert(
    text: String,
    art: AnschlussArtId,
    register: AnschlussArtRegister,
): MathematischesObjekt = when {
    register.istUnterart(art, MathematikAnschlussArten.Zahl.id) -> RationaleZahl.parse(text.trim())
    register.istUnterart(art, MathematikAnschlussArten.Menge.id) -> parseKartenTabellenMenge(text)
    art == MathematikAnschlussArten.Objekt.id -> parseAllgemeinenKartenTabellenWert(text)
    else -> error("Der Typ '${art.wert}' kann im Dialog noch nicht aus Text erzeugt werden und muss verbunden sein.")
}

internal fun parseAllgemeinenKartenTabellenWert(text: String): MathematischesObjekt {
    val bereinigt = text.trim()
    require(bereinigt.isNotEmpty()) { "Ein Wert darf nicht leer sein." }
    return runCatching { RationaleZahl.parse(bereinigt) }.getOrNull()
        ?: when (bereinigt.lowercase()) {
            "wahr", "true" -> WahrheitsKonstante(true)
            "lüge", "falsch", "false" -> WahrheitsKonstante(false)
            else -> if (bereinigt.startsWith("{") || bereinigt in setOf("N", "Z", "Q", "R", "C", "∅")) {
                parseKartenTabellenMenge(bereinigt)
            } else AllgemeinerParameter(bereinigt)
        }
}

internal fun parseKartenTabellenMenge(text: String): MengenAusdruck {
    val bereinigt = text.trim()
    return when (bereinigt) {
        "", "{}", "∅", "\\varnothing" -> LeereMenge
        "N", "\\mathbb{N}" -> NatürlicheZahlen
        "Z", "\\mathbb{Z}" -> GanzeZahlen
        "Q", "\\mathbb{Q}" -> RationaleZahlen
        "R", "\\mathbb{R}" -> ReelleZahlen
        "C", "\\mathbb{C}" -> KomplexeZahlen
        else -> if (bereinigt.startsWith("{") && bereinigt.endsWith("}")) {
            val innen = bereinigt.substring(1, bereinigt.length - 1).trim()
            if (innen.isEmpty()) LeereMenge
            else EndlicheMenge(innen.split(',').map(::parseAllgemeinenKartenTabellenWert).toSet())
        } else BenannteMenge(bereinigt, kartenTabellenLatexName(bereinigt))
    }
}

internal fun parseKartenTabellenMengenListe(text: String): List<MengenAusdruck> =
    text.split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .map(::parseKartenTabellenMenge)
        .ifEmpty { listOf(ReelleZahlen) }

internal fun kartenTabellenPrädikatSignatur(funktion: Funktion): String {
    val mengen = funktion.parameter.map { parameter ->
        funktion.werteVorräte[parameter.name] ?: BenannteMenge("?")
    }
    return "${kartenTabellenLatexName(funktion.name)}:${mengen.joinToString("\\times") { it.zuLatex() }}"
}

internal fun kartenTabellenLatexName(name: String): String =
    name.trim().ifEmpty { "P" }.replace(" ", "\\ ")

internal data class KartenTabellenZeile(
    val index: BigInteger,
    val eingänge: List<Boolean>,
    val ausgaben: List<KartenTabellenZelle>,
)

internal sealed interface KartenTabellenZelle {
    data class WahrheitswertZelle(val wert: Wahrheitswert?) : KartenTabellenZelle
    data class ObjektZelle(val latex: String) : KartenTabellenZelle
    data class FehlerZelle(val text: String) : KartenTabellenZelle
}

internal fun berechneKartenTabellenZeile(
    zustand: AtlasZustand,
    quelle: KartenWahrheitstabellenQuelle,
    evaluator: KartenAuswerter,
    index: BigInteger,
    freieLogischeEingänge: List<KartenTabellenAnschluss>,
    freieAussagen: List<KartenTabellenAnschluss>,
    freiePrädikate: List<KartenTabellenAnschluss>,
    verbundeneFelder: Set<KartenTabellenAnschluss>,
    verbundeneWerte: Map<KartenTabellenAnschluss, BedingterWert?>,
    weitereAnzeigen: Boolean,
    text: (String, String) -> String,
): KartenTabellenZeile {
    val belegung = freieLogischeEingänge.indices.map { position ->
        index.testBit(freieLogischeEingänge.lastIndex - position)
    }
    val belegungNachFeld = freieLogischeEingänge.zip(belegung).toMap()
    val vorgaben = linkedMapOf<KnotenId, Map<String, BedingterWert>>()
    val fehler = mutableListOf<String>()

    quelle.eingänge.forEach { feld ->
        val verbundenerWert = verbundeneWerte[feld]
        val wert = when {
            feld in verbundeneFelder && verbundenerWert != null -> verbundenerWert
            feld in verbundeneFelder -> {
                fehler += "${feld.name}: Der verbundene Wert ist nicht auswertbar."
                BedingterWert(AllgemeinerParameter(feld.name))
            }
            else -> runCatching {
                when {
                    feld in freieAussagen ->
                        BedingterWert(WahrheitsKonstante(belegungNachFeld.getValue(feld)))
                    feld in freiePrädikate -> {
                        val mengen = parseKartenTabellenMengenListe(
                            text(kartenTabellenPrädikatMengenSchlüssel(feld), "R"),
                        )
                        val argumente = mengen.mapIndexed { argumentIndex, menge ->
                            parseAllgemeinenKartenTabellenWert(
                                text(
                                    kartenTabellenPrädikatArgumentSchlüssel(feld, argumentIndex),
                                    standardArgumentFürKartenTabelle(menge),
                                ),
                            )
                        }
                        BedingterWert(
                            erzeugeTabellenPrädikat(
                                feld.name,
                                mengen,
                                argumente,
                                belegungNachFeld.getValue(feld),
                            ),
                        )
                    }
                    else -> BedingterWert(
                        parseKartenTabellenWert(
                            text(kartenTabellenWertSchlüssel(feld), standardWertFürKartenTabelle(feld.art)),
                            feld.art,
                            zustand.anschlussArten,
                        ),
                    )
                }
            }.getOrElse { ursache ->
                fehler += "${feld.name}: ${ursache.message ?: "ungültige Eingabe"}"
                BedingterWert(AllgemeinerParameter(feld.name))
            }
        }
        vorgaben[feld.innererKnoten.id] = mapOf("wert" to wert)
    }

    if (fehler.isNotEmpty()) {
        val ausgabeAnzahl = quelle.aussageAusgänge.size +
            if (weitereAnzeigen) quelle.weitereAusgänge.size else 0
        return KartenTabellenZeile(
            index,
            belegung,
            List(ausgabeAnzahl) { KartenTabellenZelle.FehlerZelle(fehler.joinToString()) },
        )
    }

    val ergebnis = evaluator.auswerten(quelle.karte, vorgaben)
    val sichtbareAusgänge = quelle.aussageAusgänge +
        if (weitereAnzeigen) quelle.weitereAusgänge else emptyList()
    val ausgaben = sichtbareAusgänge.map { ausgang ->
        val wert = ergebnis.knoten[ausgang.innererKnoten.id]?.ausgaben?.get("wert")
        when {
            wert == null -> KartenTabellenZelle.FehlerZelle(
                ergebnis.fehler.firstOrNull() ?: "Ausgang '${ausgang.name}' ist nicht auswertbar.",
            )
            zustand.anschlussArten.istUnterart(ausgang.art, MathematikAnschlussArten.Aussage.id) -> {
                val aussage = wert.objekt as? Aussage
                KartenTabellenZelle.WahrheitswertZelle(aussage?.entscheide()?.wahrheitswert)
            }
            else -> KartenTabellenZelle.ObjektZelle(wert.anzeigeLatex())
        }
    }
    return KartenTabellenZeile(index, belegung, ausgaben)
}

internal fun hatKartenTabellenVerbindung(
    zustand: AtlasZustand,
    knoten: KnotenDaten,
    anschluss: AnschlussDaten,
): Boolean = zustand.editor.karte.verbindungen.any {
    it.zu == AnschlussVerweis(knoten.id, anschluss.id)
}

internal fun verbundenerKartenTabellenWert(
    zustand: AtlasZustand,
    knoten: KnotenDaten,
    anschluss: AnschlussDaten,
): BedingterWert? {
    val verbindung = zustand.editor.karte.verbindungen.firstOrNull {
        it.zu == AnschlussVerweis(knoten.id, anschluss.id)
    } ?: return null
    val quellKnoten = zustand.editor.karte.knoten.firstOrNull {
        it.id == verbindung.von.knotenId
    } ?: return null
    val quellAnschluss = quellKnoten.anschlüsse.firstOrNull {
        it.id == verbindung.von.anschlussId
    } ?: return null
    return zustand.auswertung.knoten[quellKnoten.id]?.ausgaben?.get(quellAnschluss.name)
}

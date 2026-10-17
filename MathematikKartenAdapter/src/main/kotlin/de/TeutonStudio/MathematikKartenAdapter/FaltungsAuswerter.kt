package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val FALTUNGSKONSTRUKTOR_ART = "mathematik.faltungskonstruktor"
const val FALTUNGSDEFINATOR_ART = "mathematik.faltungsdefinator"
const val METHODEN_ANWENDUNG_ART = "mathematik.methodenAnwendung"
const val METHODEN_AUFRUF_ART = "mathematik.methodeAufrufen"
const val METHODEN_ZIELMENGE_ART = "mathematik.methodenZielmenge"
const val FALTUNG_PAAR = "faltung.paar"
const val FALTUNG_OPERATOR = "faltung.operator"
const val FALTUNG_INDEXNAME = "faltung.indexName"
const val FALTUNG_AKKUMULATORNAME = "faltung.akkumulatorName"
const val FALTUNG_ROLLE_INDEX = "index"
const val FALTUNG_ROLLE_AKKUMULATOR = "akkumulator"
const val METHODEN_ANWENDUNG_ERGEBNIS_ART = "methodenAnwendung.ergebnisArt"

internal object FaltungskonstruktorAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val paarId = kontext.knoten.parameter[FALTUNG_PAAR]
            ?.trim()?.takeIf(String::isNotEmpty)
            ?: error("Der Faltungskonstruktor gehört zu keinem Faltungspaar.")
        val indexMenge = kontext.eingänge["indexmenge"]?.objekt as? MengenAusdruck
            ?: error("Die Indexmenge fehlt.")
        val neutral = kontext.eingänge["neutral"] ?: error("Das neutrale Element fehlt.")
        val neutralesObjekt = neutral.objekt as? MathematischesObjekt
            ?: error("Eine mathematische Faltung benötigt ein mathematisches neutrales Element.")
        var indexName = kontext.knoten.parameter[FALTUNG_INDEXNAME]?.trim().orEmpty().ifBlank { "i" }
        var akkumulatorName = kontext.knoten.parameter[FALTUNG_AKKUMULATORNAME]?.trim().orEmpty().ifBlank { "a" }
        if(kontext.knoten.parameter["faltung.frischeNamen"]=="true") {
            val belegt=kontext.eingänge.values.flatMap { (it.objekt as? MathematischesObjekt)?.enthalteneMethodenParameter().orEmpty().map { p -> p.name } + it.variablenQuellen.map { q -> q.name } }.toMutableSet()
            fun frisch(basis:String):String { var name=basis;while(!belegt.add(name)) name+="_";return name }
            indexName=frisch("__faltungsIndex");akkumulatorName=frisch("__faltungsAkkumulator")
        }
        val akkumulatorArt = kontext.knoten.anschlüsse.firstOrNull { it.name == "akkumulator" }
            ?.art ?: AnschlussArtId("mathematik.objekt")
        val akkumulator = gebundenerParameter(akkumulatorName, akkumulatorArt)
        val akkumulatorVorrat = neutral.zielMenge ?: neutral.werteVorrat
            ?: BenannteMenge("werte_$akkumulatorName", "\\mathcal{W}_{${akkumulatorName}}")

        return KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "index" to BedingterWert(
                    objekt = Variable(indexName),
                    werteVorrat = indexMenge,
                    reelleVariablen = mapOf(indexName to indexMenge),
                    variablenQuellen = listOf(
                        VariablenQuelle(
                            knotenId = kontext.knoten.id,
                            name = indexName,
                            werteVorrat = indexMenge,
                            alsMethodenParameter = false,
                            bindungsId = paarId,
                            bindungsName = FALTUNG_ROLLE_INDEX,
                            gebundeneArt = AnschlussArtId("mathematik.zahl"),
                            bindungsWert = indexMenge,
                        ),
                    ),
                ),
                "akkumulator" to BedingterWert(
                    objekt = akkumulator,
                    zielMenge = neutral.zielMenge,
                    werteVorrat = akkumulatorVorrat,
                    variablenQuellen = listOf(
                        VariablenQuelle(
                            knotenId = kontext.knoten.id,
                            name = akkumulatorName,
                            werteVorrat = akkumulatorVorrat,
                            alsMethodenParameter = false,
                            bindungsId = paarId,
                            bindungsName = FALTUNG_ROLLE_AKKUMULATOR,
                            gebundeneArt = akkumulatorArt,
                            bindungsWert = neutralesObjekt,
                        ),
                    ),
                ),
            ),
        )
    }
}

internal object FaltungsdefinatorAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val paarId = kontext.knoten.parameter[FALTUNG_PAAR]
            ?.trim()?.takeIf(String::isNotEmpty)
            ?: error("Der Faltungsdefinator gehört zu keinem Faltungspaar.")
        val operator = kontext.knoten.parameter[FALTUNG_OPERATOR]?.trim().orEmpty()
        val nächster = kontext.eingänge["nächsterAkkumulator"]
            ?: error("Der nächste Akkumulatorwert fehlt.")
        val nächstesObjekt = nächster.objekt as? MathematischesObjekt
            ?: error("Der mathematische Faltungskörper muss ein MathematischesObjekt liefern.")
        val gebundeneQuellen = nächster.variablenQuellen
            .filter { it.bindungsId == paarId }
            .distinctBy { Triple(it.bindungsName, it.name, it.knotenId) }
        val indexQuelle = gebundeneQuellen.singleOrNull { it.bindungsName == FALTUNG_ROLLE_INDEX }
            ?: error("Der Faltungskörper muss den gebundenen Index verwenden.")
        val akkumulatorQuelle = gebundeneQuellen.singleOrNull { it.bindungsName == FALTUNG_ROLLE_AKKUMULATOR }
            ?: error("Der Faltungskörper muss den gebundenen Akkumulator verwenden.")
        val indexMenge = indexQuelle.bindungsWert as? MengenAusdruck
            ?: error("Die gebundene Indexmenge fehlt.")
        val neutral = akkumulatorQuelle.bindungsWert
            ?: error("Das gebundene neutrale Element fehlt.")
        val index = Variable(indexQuelle.name)
        val akkumulator = gebundenerParameter(
            akkumulatorQuelle.name,
            akkumulatorQuelle.gebundeneArt ?: AnschlussArtId("mathematik.objekt"),
        )
        val ergebnis: MathematischesObjekt = if (indexMenge is EndlicheMenge) {
            indexMenge.elemente.sortedBy { it.zuLatex() }.fold(neutral) { aktueller, indexWert ->
                ersetze(
                    nächstesObjekt,
                    mapOf(index.name to indexWert, akkumulator.name to aktueller),
                )
            }
        } else {
            val iterationsTerm = entferneAkkumulator(nächstesObjekt, akkumulator, operator)
            val zielMenge = nächster.zielMenge ?: akkumulatorQuelle.werteVorrat
            val methodenName = when (operator) {
                "konjunktion", "disjunktion", "adjunktion" -> "P"
                "vereinigung", "schnitt" -> "A"
                else -> "f"
            }
            val methode = Methode(
                name = methodenName,
                parameter = listOf(index),
                vorschrift = iterationsTerm,
                zielMenge = zielMenge,
                werteVorräte = mapOf(index.name to indexMenge),
            )
            when (operator) {
                "summe" -> iterierteSumme(methode, indexMenge)
                "produkt" -> iteriertesProdukt(methode, indexMenge)
                "konjunktion" -> iterierteKonjunktion(methode, indexMenge)
                "disjunktion" -> iterierteDisjunktion(methode, indexMenge)
                "adjunktion" -> iterierteAdjunktion(methode, indexMenge)
                "vereinigung" -> iterierteVereinigung(methode, indexMenge)
                "schnitt" -> iterierterSchnitt(methode, indexMenge)
                else -> error("Unbekannter Faltungsoperator '$operator'.")
            }
        }

        return KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "wert" to BedingterWert(
                    objekt = ergebnis,
                    annahmen = nächster.annahmen,
                    zielMenge = nächster.zielMenge,
                    werteVorrat = nächster.werteVorrat,
                    reelleVariablen = nächster.reelleVariablen - index.name,
                    variablenQuellen = nächster.variablenQuellen.filterNot { it.bindungsId == paarId },
                ),
            ),
        )
    }
}

/** Historische Einzelprojektion delegiert an den kanonischen Methodenaufruf. */
internal object MethodenAnwendungAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis =
        MethodenAufrufAuswerter.auswerten(kontext.copy(knoten = kontext.knoten.copy(parameter = kontext.knoten.parameter + mapOf(
            METHODEN_AUFRUF_ARGUMENTPROJEKTION to METHODEN_ARGUMENTPROJEKTION_SEPARIERT,
            METHODEN_AUFRUF_ERGEBNISPROJEKTION to METHODEN_ERGEBNISPROJEKTION_DIREKT,
        ))))
}

internal object MethodenZielmengeAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val methode = kontext.eingänge["methode"]?.objekt as? MathematischeSignaturtragendeMethode
            ?: error("Die Zielmenge benötigt eine mathematische Raum-/Mengensignatur.")
        val signatur = methode.mathematischeSignatur
        val zielMenge = if (signatur.ergebnisse.size == 1) {
            signatur.ergebnisse.single().zielMenge
        } else {
            signatur.zielRaum
        }
        return KnotenAuswertungsErgebnis(
            mapOf(
                "menge" to BedingterWert(
                    objekt = zielMenge,
                    variablenQuellen = kontext.eingänge.getValue("methode").variablenQuellen,
                ),
            ),
        )
    }
}

private fun gebundenerParameter(name: String, art: AnschlussArtId): MethodenParameter = when (art.wert) {
    "mathematik.zahl" -> Variable(name)
    "mathematik.aussage" -> AussagenParameter(name)
    "mathematik.menge" -> MengenParameter(name)
    else -> TypisiertesElement(name, art.wert)
}

private fun entferneAkkumulator(
    körper: MathematischesObjekt,
    akkumulator: MethodenParameter,
    operator: String,
): MathematischesObjekt = when (operator) {
    "summe" -> addition((körper as? Addition)?.summanden?.filterNot { it == akkumulator }
        ?: error("Der Summen-Faltungskörper muss eine Addition sein."))
    "produkt" -> multiplikation((körper as? Multiplikation)?.faktoren?.filterNot { it == akkumulator }
        ?: error("Der Produkt-Faltungskörper muss eine Multiplikation sein."))
    "konjunktion" -> Konjunktion((körper as? Konjunktion)?.aussagen?.filterNot { it == akkumulator }
        ?: error("Der Konjunktions-Faltungskörper muss eine Konjunktion sein."))
    "disjunktion" -> Disjunktion((körper as? Disjunktion)?.aussagen?.filterNot { it == akkumulator }
        ?: error("Der Disjunktions-Faltungskörper muss eine Disjunktion sein."))
    "adjunktion" -> {
        val xor = körper as? Adjunktion
            ?: error("Der Adjunktions-Faltungskörper muss eine Adjunktion sein.")
        when (akkumulator) {
            xor.links -> xor.rechts
            xor.rechts -> xor.links
            else -> error("Die Adjunktion verwendet den gebundenen Akkumulator nicht.")
        }
    }
    "vereinigung" -> vereinige((körper as? Vereinigung)?.mengen?.filterNot { it == akkumulator }
        ?: error("Der Vereinigungs-Faltungskörper muss eine Vereinigung sein."))
    "schnitt" -> {
        val schnitt = körper as? Schnitt ?: error("Der Schnitt-Faltungskörper muss ein Schnitt sein.")
        schneide(schnitt.mengen.filterNot { it == akkumulator }, schnitt.grundMenge)
    }
    else -> error("Unbekannter Faltungsoperator '$operator'.")
}

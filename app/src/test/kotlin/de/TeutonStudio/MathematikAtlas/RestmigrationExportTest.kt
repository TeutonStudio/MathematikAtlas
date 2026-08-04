package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikKnoten.enzyklopädie.MathematikEnzyklopädie
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensEintrag
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import java.io.File

/**
 * Temporärer, deterministischer Export der noch app-lokalen statischen
 * Konzeptkarten und der kanonischen Knoten-/Konzeptinventare. Das erzeugte
 * Artefakt wird anschließend als echte Modulquelle eingecheckt und dieser Test
 * wieder entfernt.
 */
class RestmigrationExportTest {
    @Test
    fun exportiereKanonischenRestbestand() {
        val arbeitsraum = File(System.getenv("GITHUB_WORKSPACE") ?: ".").canonicalFile
        val ausgabe = File(arbeitsraum, "build/restmigration-export").apply {
            deleteRecursively()
            mkdirs()
        }
        val kartenOrdner = File(ausgabe, "konzeptkarte").apply { mkdirs() }
        val wissen = MathematikEnzyklopädie.standard.alle
        val kartenNachVariante = linkedMapOf<String, MutableList<JSONObject>>()
        val exportierteDateien = linkedMapOf<String, String>()
        val exportFehler = mutableListOf<JSONObject>()

        wissen.filter { it.knotenVorlagen.isNotEmpty() }.forEach { eintrag ->
            eintrag.knotenVorlagen.forEachIndexed { index, vorlage ->
                val variantenId = vorlage.variantenId()
                runCatching {
                    val konzept = statischesKonzept(vorlage, index) ?: return@runCatching
                    val referenzen = exportiereKonzept(
                        wissensId = eintrag.id.wert,
                        variantenId = variantenId,
                        konzept = konzept,
                        ordner = kartenOrdner,
                        dateien = exportierteDateien,
                    )
                    kartenNachVariante.getOrPut(variantenId) { mutableListOf() }.addAll(referenzen)
                }.onFailure { fehler ->
                    exportFehler += JSONObject()
                        .put("wissensId", eintrag.id.wert)
                        .put("variantenId", variantenId)
                        .put("knotenArt", vorlage.art)
                        .put("vorlage", vorlage.name)
                        .put("fehlerTyp", fehler::class.qualifiedName)
                        .put("meldung", fehler.message ?: fehler.toString())
                }
            }
        }

        File(ausgabe, "wissen.json").writeText(
            JSONArray(wissen.map(::wissenZuJson)).toString(2),
        )
        File(ausgabe, "knotenarten.json").writeText(
            JSONArray(
                alleMathematikDefinitionsVorlagen()
                    .distinctBy { it.variantenId() }
                    .groupBy { it.art }
                    .toSortedMap()
                    .map { (art, varianten) ->
                        JSONObject()
                            .put("art", art)
                            .put("datei", "${klassenName(art)}Preview.kt")
                            .put("varianten", JSONArray(varianten.map(::vorlageZuJson)))
                    },
            ).toString(2),
        )
        File(ausgabe, "karten-zuordnung.json").writeText(
            JSONObject().apply {
                kartenNachVariante.toSortedMap().forEach { (variante, referenzen) ->
                    put(variante, JSONArray(referenzen.distinctBy { it.getString("id") }))
                }
            }.toString(2),
        )
        File(ausgabe, "karten-dateien.json").writeText(
            JSONObject(exportierteDateien.toSortedMap()).toString(2),
        )
        File(ausgabe, "export-fehler.json").writeText(
            JSONArray(exportFehler).toString(2),
        )
    }

    private fun statischesKonzept(vorlage: KnotenVorlage, index: Int): KonzeptDefinition? {
        val knoten = vorlage.deterministischerKnoten(index)
        if (vorlage.art == ZAHLENRECHNER_ART) {
            if (istZahlenRechnerFormel(knoten)) return null
            val operator = UniversellerZahlenOperator.vonId(
                vorlage.standardParameter[ZAHLENRECHNER_OPERATOR],
            )
            val basis = ZahlenRechnerDefinitionsKarten.konzept(listOf(vorlage))
            return basis.copy(reiter = basis.reiter + operatorHinweise(operator))
        }
        if (vorlage.art == MENGEN_KNOTEN_ART && vorlage.kartenVerweis == null) {
            return mengenKonzept(vorlage)
        }
        if (vorlage.art == MathematikKnotenVorlagen.MatrixProdukt.art) {
            return matrixProduktKonzept(vorlage)
        }
        val familie = StrukturRechnerKnotenFamilie.fuerKnotenArt(vorlage.art)
        if (familie != null) {
            val operatorId = vorlage.standardParameter[RECHNER_OPERATOR_PARAMETER]
            if (operatorId == familie.formelOperatorId) return null
            return strukturRechnerKonzept(knoten, familie)
        }
        return TestDefinitionsKarten.fürKnoten(knoten)
    }

    private fun mengenKonzept(vorlage: KnotenVorlage): KonzeptDefinition {
        val auswahl = MengenKnotenAuswahl.vonId(vorlage.standardParameter[MENGEN_KNOTEN_AUSWAHL])
        val prefix = "definition-menge-${slug(auswahl.stabileId)}"
        val karte = KartenDaten(
            id = KartenId(prefix),
            name = "Definition ${auswahl.titel}",
            knoten = listOf(
                KnotenDaten(
                    id = KnotenId("$prefix-regel"),
                    art = TestDefinitionsKarten.KONZEPT_REGEL_ART,
                    name = auswahl.menge().zuLatex(),
                    position = GraphPunkt(80f, 70f),
                    größe = GraphGröße(520f, 180f),
                    parameter = mapOf(
                        "regel" to auswahl.beschreibung,
                        "definition" to auswahl.menge().zuLatex(),
                        "knotenArt" to vorlage.art,
                    ),
                    anschlüsse = listOf(
                        AnschlussDaten(
                            id = AnschlussId("$prefix-ausgang"),
                            name = "menge",
                            richtung = AnschlussRichtung.Ausgang,
                            kante = AnschlussKante.Rechts,
                            art = MathematikAnschlussArten.Menge.id,
                        ),
                    ),
                ),
            ),
        )
        return KonzeptDefinition(
            id = KonzeptId("menge-${slug(auswahl.stabileId)}"),
            name = auswahl.titel,
            beschreibung = auswahl.beschreibung,
            pfad = listOf("Mengen", "Grundmengen"),
            tags = setOf("Menge", auswahl.titel, auswahl.stabileId),
            knotenArten = setOf(MENGEN_KNOTEN_ART),
            knotenParameter = mapOf(MENGEN_KNOTEN_AUSWAHL to auswahl.stabileId),
            reiter = listOf(
                KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, karte),
                KonzeptReiter(
                    "knotenvertrag",
                    "Knotenvertrag",
                    KonzeptReiterRolle.Äquivalenz,
                    TestDefinitionsKarten.definitionsKarte(vorlage, 1),
                ),
            ),
        )
    }

    private fun operatorHinweise(operator: UniversellerZahlenOperator): List<KonzeptReiter> = listOf(
        KonzeptReiter(
            id = "definitionsbereich",
            titel = "Definitionsbereich",
            rolle = KonzeptReiterRolle.Spezialfall,
            karte = hinweisKarte(
                "zahlenrechner-${operator.stabileId}-definitionsbereich",
                "Definitionsbereich ${operator.titel}",
                "Definiert, bedingt oder undefiniert",
                definitionsbereichText(operator),
            ),
        ),
        KonzeptReiter(
            id = "definitionsluecken",
            titel = "Definitionslücken",
            rolle = KonzeptReiterRolle.Spezialfall,
            karte = hinweisKarte(
                "zahlenrechner-${operator.stabileId}-luecken",
                "Definitionslücken ${operator.titel}",
                "Strukturierter Nichtwert",
                definitionsLueckenText(operator),
            ),
        ),
        KonzeptReiter(
            id = "aequivalente",
            titel = "Äquivalente",
            rolle = KonzeptReiterRolle.Äquivalenz,
            karte = hinweisKarte(
                "zahlenrechner-${operator.stabileId}-aequivalente",
                "Äquivalente Darstellungen ${operator.titel}",
                "Äquivalente Darstellung",
                aequivalenzText(operator),
            ),
        ),
    )

    private fun hinweisKarte(id: String, name: String, titel: String, text: String): KartenDaten =
        KartenDaten(
            id = KartenId(slug(id)),
            name = name,
            knoten = listOf(
                KnotenDaten(
                    id = KnotenId("${slug(id)}-regel"),
                    art = TestDefinitionsKarten.KONZEPT_REGEL_ART,
                    name = titel,
                    position = GraphPunkt(70f, 65f),
                    größe = GraphGröße(620f, 210f),
                    parameter = mapOf("regel" to text, "knotenArt" to ZAHLENRECHNER_ART),
                ),
            ),
        )

    private fun definitionsbereichText(operator: UniversellerZahlenOperator): String = when (operator) {
        UniversellerZahlenOperator.DIVISION, UniversellerZahlenOperator.KEHRWERT ->
            "Der Nenner beziehungsweise Operand muss von null verschieden sein. Ist dies nur als Annahme bekannt, bleibt die Auswertung bedingt definiert."
        UniversellerZahlenOperator.MINIMUM, UniversellerZahlenOperator.MAXIMUM ->
            "Alle Eingaben benötigen einen gemeinsamen geordneten Zahlbereich. Komplexe Zahlen und Quaternionen besitzen keine projektweit kanonische Ordnung."
        UniversellerZahlenOperator.QUADRATWURZEL, UniversellerZahlenOperator.WURZEL ->
            "Der Definitionsbereich folgt dem gewählten Zahlbereich und dem Wurzelexponenten. Reelle Hauptwerte dürfen nicht stillschweigend komplex fortgesetzt werden."
        else -> "Der Rechner wählt aus den Eingangsbereichen die engste nachweislich gültige Operatordefinition. Unbekannte Bereiche bleiben bedingt statt vorsorglich auf den größten Bereich erweitert zu werden."
    }

    private fun definitionsLueckenText(operator: UniversellerZahlenOperator): String = when (operator) {
        UniversellerZahlenOperator.DIVISION, UniversellerZahlenOperator.KEHRWERT ->
            "Bei einem nachweislich verschwindenden Nenner entsteht eine Definitionslücke mit Ursache „Division durch null“; sie ist kein Element eines Zahlbereichs."
        UniversellerZahlenOperator.MINIMUM, UniversellerZahlenOperator.MAXIMUM ->
            "Fehlt eine kompatible Ordnung, wird die Operation als nicht anwendbar diagnostiziert und nicht als falscher Zahlenwert ausgegeben."
        else -> "Fehlt eine gültige bereichsspezifische Definition, liefert der Rechner einen strukturierten Nichtwert mit Operator, Eingängen und Ursache."
    }

    private fun aequivalenzText(operator: UniversellerZahlenOperator): String = when (operator) {
        UniversellerZahlenOperator.SUBTRAKTION -> "a-b ist bei vorhandener additiver Inverser äquivalent zu a+(-b)."
        UniversellerZahlenOperator.DIVISION -> "a/b ist bei b ungleich null äquivalent zu a·b^{-1}; in nichtkommutativen Bereichen bleibt die Seitenwahl Teil der Definition."
        UniversellerZahlenOperator.POTENZ -> "Natürliche Potenzen können als iterierte Multiplikation dargestellt werden; andere Exponenten benötigen eine eigene Bereichsdefinition."
        UniversellerZahlenOperator.QUADRAT -> "a² ist die spezialisierte Potenz a^2 und äquivalent zu a·a."
        UniversellerZahlenOperator.KUBIK -> "a³ ist die spezialisierte Potenz a^3 und äquivalent zu a·a·a."
        else -> "Äquivalente Darstellungen verändern weder Eingangsbereiche noch offene Definitionsbedingungen."
    }

    private fun exportiereKonzept(
        wissensId: String,
        variantenId: String,
        konzept: KonzeptDefinition,
        ordner: File,
        dateien: MutableMap<String, String>,
    ): List<JSONObject> {
        val ergebnis = mutableListOf<JSONObject>()
        konzept.reiter.forEachIndexed { reiterIndex, reiter ->
            ergebnis += exportiereKarte(
                wissensId,
                variantenId,
                reiter,
                reiter.karte,
                ordner,
                dateien,
                primär = reiter.rolle == KonzeptReiterRolle.Definition && reiterIndex == 0,
                darstellungsGruppe = reiter.id.takeIf { reiter.darstellungsVarianten.isNotEmpty() },
                darstellung = null,
            )
            reiter.darstellungsVarianten.toSortedMap(compareBy { it.name }).forEach { (darstellung, karte) ->
                ergebnis += exportiereKarte(
                    wissensId,
                    variantenId,
                    reiter,
                    karte,
                    ordner,
                    dateien,
                    primär = false,
                    darstellungsGruppe = reiter.id,
                    darstellung = darstellung.name,
                )
            }
        }
        return ergebnis
    }

    private fun exportiereKarte(
        wissensId: String,
        variantenId: String,
        reiter: KonzeptReiter,
        karte: KartenDaten,
        ordner: File,
        dateien: MutableMap<String, String>,
        primär: Boolean,
        darstellungsGruppe: String?,
        darstellung: String?,
    ): JSONObject {
        val suffix = darstellung?.let { ".${it.lowercase()}" }.orEmpty()
        val variantenSchluessel = variantenId.hashCode().toUInt().toString(16)
        val referenzId = "$wissensId.$variantenSchluessel.${reiter.id}$suffix"
        val dateiSchluessel = referenzId.hashCode().toUInt().toString(16)
        val datei = "konzeptkarte-$dateiSchluessel-v1.json"
        val text = KartenDatenJson.schreibe(karte)
        val vorhanden = dateien.putIfAbsent(datei, text)
        require(vorhanden == null || vorhanden == text) { "Kollidierende Assetdatei $datei" }
        File(ordner, datei).writeText(text)
        return JSONObject()
            .put("id", referenzId)
            .put("datei", datei)
            .put("rolle", reiter.rolle.name)
            .put("titel", reiter.titel)
            .put("primaer", primär)
            .put("darstellungsGruppe", darstellungsGruppe ?: JSONObject.NULL)
            .put("darstellung", darstellung ?: JSONObject.NULL)
    }

    private fun wissenZuJson(eintrag: WissensEintrag): JSONObject = JSONObject()
        .put("id", eintrag.id.wert)
        .put("titel", eintrag.titel)
        .put("beschreibung", eintrag.kurzbeschreibung)
        .put("verfuegbarkeit", eintrag.verfügbarkeit.name)
        .put("reifegrad", eintrag.reifegrad.name)
        .put("fachPfade", JSONArray(eintrag.fachPfade.map { it.stabileId }.sorted()))
        .put("suchbegriffe", JSONArray(eintrag.suchbegriffe.sorted()))
        .put("aliase", JSONArray(eintrag.aliase.sorted()))
        .put("knotenArten", JSONArray(eintrag.knotenArten.sorted()))
        .put("varianten", JSONArray(eintrag.varianten.map { it.wert }.sorted()))
        .put("vorlagen", JSONArray(eintrag.knotenVorlagen.map(::vorlageZuJson)))
        .put("generatoren", JSONArray(eintrag.karten.map { it.id }))

    private fun vorlageZuJson(vorlage: KnotenVorlage): JSONObject = JSONObject()
        .put("variantenId", vorlage.variantenId())
        .put("art", vorlage.art)
        .put("name", vorlage.name)
        .put("kategorie", vorlage.kategorie)
        .put("beschreibung", vorlage.beschreibung)
        .put("parameter", JSONObject(vorlage.standardParameter.toSortedMap()))

    private fun KnotenVorlage.deterministischerKnoten(index: Int): KnotenDaten {
        val basis = erzeuge(GraphPunkt.Zero)
        val prefix = "export-${slug(variantenId())}-$index"
        return basis.copy(
            id = KnotenId(prefix),
            anschlüsse = basis.anschlüsse.mapIndexed { anschlussIndex, anschluss ->
                anschluss.copy(id = AnschlussId("$prefix-anschluss-$anschlussIndex"))
            },
        )
    }

    private fun KnotenVorlage.variantenId(): String = buildString {
        append(art)
        standardParameter.toSortedMap().forEach { (schlüssel, wert) ->
            append('|').append(schlüssel).append('=').append(wert)
        }
        append('|').append(name)
    }

    private fun klassenName(art: String): String = slug(art)
        .split('-')
        .filter(String::isNotBlank)
        .joinToString("") { teil -> teil.replaceFirstChar { it.uppercase() } }

    private fun slug(text: String): String = text.lowercase()
        .replace("ä", "ae")
        .replace("ö", "oe")
        .replace("ü", "ue")
        .replace("ß", "ss")
        .map { if (it.isLetterOrDigit()) it else '-' }
        .joinToString("")
        .replace(Regex("-+"), "-")
        .trim('-')
}

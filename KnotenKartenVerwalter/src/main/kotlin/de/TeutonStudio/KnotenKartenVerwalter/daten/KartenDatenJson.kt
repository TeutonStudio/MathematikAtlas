package de.TeutonStudio.KnotenKartenVerwalter.daten

import de.TeutonStudio.TypSystem.*
import org.json.JSONArray
import org.json.JSONObject

/** Reiner JSON-Codec für [KartenDaten], ohne App-, Datei- oder Migrationszuständigkeit. */
object KartenDatenJson {
    const val FORMAT_VERSION = 8

    fun schreibe(karte: KartenDaten): String = JSONObject().apply {
        put("formatVersion", FORMAT_VERSION)
        put("id", karte.id.wert)
        put("name", karte.name)
        put("version", karte.version)
        put("erstelltAm", karte.erstelltAm)
        put("archiviert", karte.archiviert)
        put("ansicht", JSONObject().apply {
            put("x", karte.ansicht.verschiebung.x)
            put("y", karte.ansicht.verschiebung.y)
            put("zoom", karte.ansicht.zoom)
        })
        put("knoten", JSONArray().apply { karte.knoten.forEach { put(knotenZuJson(it)) } })
        put("verbindungen", JSONArray().apply { karte.verbindungen.forEach { put(verbindungZuJson(it)) } })
        put("visuelleGruppen", JSONArray().apply { karte.visuelleGruppen.forEach { put(visuelleGruppeZuJson(it)) } })
    }.toString(2)

    fun lese(text: String): KartenDaten = lese(JSONObject(text))

    /**
     * Dekodiert einen bereits geparsten Objektbaum. Editoren und Importpipelines
     * können dadurch Syntax- und Schemaprüfung innerhalb einer Textrevision mit
     * genau einem vollständigen JSON-Parse durchführen.
     */
    fun lese(json: JSONObject): KartenDaten {
        val ansicht = json.optJSONObject("ansicht")
        return KartenDaten(
            id = KartenId(json.getString("id")),
            name = json.getString("name"),
            version = json.optInt("version", 1),
            erstelltAm = json.optLong("erstelltAm", 0L),
            knoten = json.optJSONArray("knoten").zuListe(::knotenVonJson),
            verbindungen = json.optJSONArray("verbindungen").zuListe(::verbindungVonJson),
            visuelleGruppen = json.optJSONArray("visuelleGruppen").zuListe(::visuelleGruppeVonJson),
            ansicht = AnsichtsFenster(
                GraphPunkt(
                    ansicht?.optDouble("x", 0.0)?.toFloat() ?: 0f,
                    ansicht?.optDouble("y", 0.0)?.toFloat() ?: 0f,
                ),
                ansicht?.optDouble("zoom", 1.0)?.toFloat() ?: 1f,
            ),
            archiviert = json.optBoolean("archiviert", false),
        ).bereinigteVisuelleGruppen()
    }

    fun formatVersion(text: String): Int = JSONObject(text).optInt("formatVersion", 1)

    private fun knotenZuJson(knoten: KnotenDaten) = JSONObject().apply {
        put("id", knoten.id.wert)
        put("art", knoten.art)
        put("name", knoten.name)
        put("position", JSONObject().put("x", knoten.position.x).put("y", knoten.position.y))
        put("größe", JSONObject().put("breite", knoten.größe.breite).put("höhe", knoten.größe.höhe))
        put("parameter", JSONObject(knoten.parameter))
        put("eigenschaften", JSONObject().apply {
            knoten.eigenschaften.forEach { (schlüssel, wert) -> put(schlüssel, eigenschaftZuJson(wert)) }
        })
        knoten.kartenVerweis?.let { put("kartenVerweis", it.zuJson()) }
        if (knoten.eingangsKartenVerweise.isNotEmpty()) {
            put("eingangsKartenVerweise", JSONObject().apply {
                knoten.eingangsKartenVerweise.toSortedMap().forEach { (name, verweis) -> put(name, verweis.zuJson()) }
            })
        }
        put("anschlüsse", JSONArray().apply {
            knoten.anschlüsse.forEach { anschluss ->
                put(JSONObject().apply {
                    put("id", anschluss.id.wert)
                    put("name", anschluss.name)
                    put("richtung", anschluss.richtung.name)
                    put("kante", anschluss.kante.name)
                    put("art", anschluss.art.wert)
                    put("reihenfolge", anschluss.reihenfolge)
                    put("kannSichErweitern", anschluss.kannSichErweitern)
                    put("dynamischErzeugt", anschluss.dynamischErzeugt)
                    anschluss.artFolgtEingang?.let { put("artFolgtEingang", it) }
                    if (anschluss.artVereinigtEingänge.isNotEmpty()) {
                        put("artVereinigtEingänge", JSONArray(anschluss.artVereinigtEingänge))
                    }
                    if (anschluss.zulässigeArten.isNotEmpty()) {
                        put("zulässigeArten", JSONArray(anschluss.zulässigeArten.map { it.wert }.sorted()))
                    }
                    anschluss.artAbbildungVonEingang?.let { regel ->
                        put("artAbbildungVonEingang", JSONObject().apply {
                            put("eingang", regel.eingang)
                            put("abbildung", JSONObject().apply {
                                regel.abbildung.entries.sortedBy { it.key.wert }.forEach { (von, zu) ->
                                    put(von.wert, zu.wert)
                                }
                            })
                        })
                    }
                    anschluss.artPriorisiertEingänge?.let { regel ->
                        put("artPriorisiertEingänge", JSONObject().apply {
                            put("eingänge", JSONArray(regel.eingänge))
                            put("prioritäten", JSONArray(regel.prioritäten.map { it.wert }))
                        })
                    }
                    if (
                        anschluss.vertrag.typ != TypAusdruck.Unbekannt ||
                        anschluss.vertrag.anforderungen.isNotEmpty()
                    ) {
                        put("vertrag", vertragZuJson(anschluss.vertrag))
                    }
                    anschluss.typInferenz?.let { put("typInferenz", typInferenzZuJson(it)) }
                })
            }
        })
    }

    private fun knotenVonJson(json: JSONObject): KnotenDaten {
        val position = json.getJSONObject("position")
        val größe = json.getJSONObject("größe")
        val verweis = json.optJSONObject("kartenVerweis")?.zuKartenVerweis()
        val eingangsVerweiseJson = json.optJSONObject("eingangsKartenVerweise") ?: JSONObject()
        val eingangsVerweise = eingangsVerweiseJson.keys().asSequence().mapNotNull { name ->
            eingangsVerweiseJson.optJSONObject(name)?.zuKartenVerweis()?.let { name to it }
        }.toMap()
        val parameterJson = json.optJSONObject("parameter") ?: JSONObject()
        val parameter = parameterJson.keys().asSequence().associateWith { parameterJson.optString(it) }
        val eigenschaftenJson = json.optJSONObject("eigenschaften") ?: JSONObject()
        val eigenschaften = eigenschaftenJson.keys().asSequence().mapNotNull { schlüssel ->
            eigenschaftVonJson(eigenschaftenJson.optJSONObject(schlüssel))?.let { schlüssel to it }
        }.toMap()
        return KnotenDaten(
            id = KnotenId(json.getString("id")),
            art = json.getString("art"),
            name = json.getString("name"),
            position = GraphPunkt(position.getDouble("x").toFloat(), position.getDouble("y").toFloat()),
            größe = GraphGröße(größe.getDouble("breite").toFloat(), größe.getDouble("höhe").toFloat()),
            anschlüsse = json.optJSONArray("anschlüsse").zuListe { anschluss ->
                AnschlussDaten(
                    id = AnschlussId(anschluss.getString("id")),
                    name = anschluss.getString("name"),
                    richtung = enumValueOf(anschluss.optString("richtung", "Neutral")),
                    kante = enumValueOf(anschluss.getString("kante")),
                    art = AnschlussArtId(anschluss.getString("art")),
                    reihenfolge = anschluss.optInt("reihenfolge", 0),
                    kannSichErweitern = anschluss.optBoolean("kannSichErweitern", false),
                    dynamischErzeugt = anschluss.optBoolean("dynamischErzeugt", false),
                    artFolgtEingang = anschluss.optString("artFolgtEingang").takeIf(String::isNotBlank),
                    artVereinigtEingänge = anschluss.optJSONArray("artVereinigtEingänge")?.let { namen ->
                        List(namen.length()) { index -> namen.getString(index) }
                    } ?: emptyList(),
                    zulässigeArten = anschluss.optJSONArray("zulässigeArten")?.let { arten ->
                        List(arten.length()) { index -> AnschlussArtId(arten.getString(index)) }.toSet()
                    } ?: emptySet(),
                    artAbbildungVonEingang = anschluss.optJSONObject("artAbbildungVonEingang")?.let { regel ->
                        val abbildung = regel.optJSONObject("abbildung") ?: JSONObject()
                        AnschlussArtAbbildung(
                            eingang = regel.getString("eingang"),
                            abbildung = abbildung.keys().asSequence().associate { von ->
                                AnschlussArtId(von) to AnschlussArtId(abbildung.getString(von))
                            },
                        )
                    },
                    artPriorisiertEingänge = anschluss.optJSONObject("artPriorisiertEingänge")?.let { regel ->
                        val eingänge = regel.optJSONArray("eingänge") ?: JSONArray()
                        val prioritäten = regel.optJSONArray("prioritäten") ?: JSONArray()
                        AnschlussArtPriorisierung(
                            eingänge = List(eingänge.length()) { index -> eingänge.getString(index) },
                            prioritäten = List(prioritäten.length()) { index ->
                                AnschlussArtId(prioritäten.getString(index))
                            },
                        )
                    },
                    vertrag = anschluss.optJSONObject("vertrag")?.let(::vertragVonJson) ?: AnschlussVertrag(),
                    typInferenz = anschluss.optJSONObject("typInferenz")?.let(::typInferenzVonJson),
                )
            },
            parameter = parameter,
            eigenschaften = eigenschaften,
            kartenVerweis = verweis,
            eingangsKartenVerweise = eingangsVerweise,
        )
    }

    private fun vertragZuJson(vertrag: AnschlussVertrag) = JSONObject().apply {
        put("typ", typZuJson(vertrag.typ))
        if (vertrag.anforderungen.isNotEmpty()) {
            put("anforderungen", JSONArray().apply {
                vertrag.anforderungen.forEach { anforderung ->
                    put(JSONObject().apply {
                        put("id", anforderung.id)
                        if (anforderung.parameter.isNotEmpty()) put("parameter", JSONObject(anforderung.parameter))
                    })
                }
            })
        }
    }

    private fun vertragVonJson(json: JSONObject): AnschlussVertrag = AnschlussVertrag(
        typ = json.optJSONObject("typ")?.let(::typVonJson) ?: TypAusdruck.Unbekannt,
        anforderungen = json.optJSONArray("anforderungen")?.zuListe { anforderung ->
            val parameter = anforderung.optJSONObject("parameter") ?: JSONObject()
            TypAnforderung(
                id = anforderung.getString("id"),
                parameter = parameter.keys().asSequence().associateWith { parameter.optString(it) },
            )
        } ?: emptyList(),
    )

    private fun typZuJson(typ: TypAusdruck): JSONObject = JSONObject().apply {
        when (typ) {
            TypAusdruck.Beliebig -> put("art", "beliebig")
            TypAusdruck.Unbekannt -> put("art", "unbekannt")
            is TypAusdruck.Atom -> {
                put("art", "atom")
                put("id", typ.id.wert)
            }
            is TypAusdruck.Parameterisiert -> {
                put("art", "parameterisiert")
                put("konstruktor", typ.konstruktor.wert)
                put("argumente", JSONArray().apply { typ.argumente.forEach { put(typZuJson(it)) } })
            }
            is TypAusdruck.Vereinigung -> {
                put("art", "vereinigung")
                put("alternativen", JSONArray().apply { typ.alternativen.forEach { put(typZuJson(it)) } })
            }
            is TypAusdruck.Variable -> {
                put("art", "variable")
                put("id", typ.id.wert)
            }
            is TypAusdruck.Literal -> {
                put("art", "literal")
                put("wert", typ.wert)
            }
        }
    }

    private fun typVonJson(json: JSONObject): TypAusdruck = when (json.optString("art")) {
        "beliebig" -> TypAusdruck.Beliebig
        "unbekannt" -> TypAusdruck.Unbekannt
        "atom" -> TypAusdruck.Atom(TypId(json.getString("id")))
        "parameterisiert" -> TypAusdruck.Parameterisiert(
            konstruktor = TypId(json.getString("konstruktor")),
            argumente = json.optJSONArray("argumente")?.zuListe(::typVonJson) ?: emptyList(),
        )
        "vereinigung" -> {
            val alternativen = json.optJSONArray("alternativen")?.zuListe(::typVonJson).orEmpty()
            if (alternativen.isEmpty()) TypAusdruck.Unbekannt else TypAusdruck.Vereinigung(alternativen)
        }
        "variable" -> TypAusdruck.Variable(TypVariablenId(json.getString("id")))
        "literal" -> TypAusdruck.Literal(json.getString("wert"))
        else -> TypAusdruck.Unbekannt
    }

    private fun typInferenzZuJson(regel: TypInferenzRegel): JSONObject = JSONObject().apply {
        when (regel) {
            is TypInferenzRegel.FolgtEingang -> {
                put("art", "folgtEingang")
                put("eingang", regel.eingang)
            }
            is TypInferenzRegel.GemeinsameOberart -> {
                put("art", "gemeinsameOberart")
                put("eingänge", JSONArray(regel.eingänge))
            }
            is TypInferenzRegel.Vereinigung -> {
                put("art", "vereinigung")
                put("eingänge", JSONArray(regel.eingänge))
            }
            is TypInferenzRegel.TupelAus -> {
                put("art", "tupelAus")
                put("eingänge", JSONArray(regel.eingänge))
            }
            is TypInferenzRegel.KomponenteVonTupel -> {
                put("art", "komponenteVonTupel")
                put("eingang", regel.eingang)
                put("index", regel.index)
            }
            is TypInferenzRegel.AbbildungVonEingang -> {
                put("art", "abbildungVonEingang")
                put("eingang", regel.eingang)
                put("abbildung", JSONArray().apply {
                    regel.abbildung.forEach { (von, zu) ->
                        put(JSONObject().put("von", typZuJson(von)).put("zu", typZuJson(zu)))
                    }
                })
            }
            is TypInferenzRegel.Priorisierung -> {
                put("art", "priorisierung")
                put("eingänge", JSONArray(regel.eingänge))
                put("prioritäten", JSONArray().apply { regel.prioritäten.forEach { put(typZuJson(it)) } })
            }
        }
    }

    private fun typInferenzVonJson(json: JSONObject): TypInferenzRegel? = when (json.optString("art")) {
        "folgtEingang" -> TypInferenzRegel.FolgtEingang(json.getString("eingang"))
        "gemeinsameOberart" -> TypInferenzRegel.GemeinsameOberart(json.stringListe("eingänge"))
        "vereinigung" -> TypInferenzRegel.Vereinigung(json.stringListe("eingänge"))
        "tupelAus" -> TypInferenzRegel.TupelAus(json.stringListe("eingänge"))
        "komponenteVonTupel" -> TypInferenzRegel.KomponenteVonTupel(
            json.getString("eingang"),
            json.getInt("index"),
        )
        "abbildungVonEingang" -> TypInferenzRegel.AbbildungVonEingang(
            eingang = json.getString("eingang"),
            abbildung = json.optJSONArray("abbildung")?.zuListe { eintrag ->
                typVonJson(eintrag.getJSONObject("von")) to typVonJson(eintrag.getJSONObject("zu"))
            }?.toMap() ?: emptyMap(),
        )
        "priorisierung" -> TypInferenzRegel.Priorisierung(
            eingänge = json.stringListe("eingänge"),
            prioritäten = json.optJSONArray("prioritäten")?.zuListe(::typVonJson) ?: emptyList(),
        )
        else -> null
    }

    private fun JSONObject.stringListe(name: String): List<String> =
        optJSONArray(name)?.let { array -> List(array.length()) { index -> array.getString(index) } } ?: emptyList()

    private fun KartenVerweis.zuJson() = JSONObject()
        .put("kartenId", kartenId.wert)
        .put("version", version)

    private fun JSONObject.zuKartenVerweis() =
        KartenVerweis(KartenId(getString("kartenId")), getInt("version"))

    private fun visuelleGruppeZuJson(gruppe: VisuelleKnotenGruppeDaten) = JSONObject().apply {
        put("id", gruppe.id.wert)
        put("titel", gruppe.titel)
        put("position", JSONObject().put("x", gruppe.position.x).put("y", gruppe.position.y))
        put("größe", JSONObject().put("breite", gruppe.größe.breite).put("höhe", gruppe.größe.höhe))
        put("knotenIds", JSONArray().apply { gruppe.knotenIds.map { it.wert }.sorted().forEach(::put) })
    }

    private fun visuelleGruppeVonJson(json: JSONObject): VisuelleKnotenGruppeDaten {
        val position = json.optJSONObject("position")
        val größe = json.optJSONObject("größe")
        return VisuelleKnotenGruppeDaten(
            id = VisuelleGruppenId(json.getString("id")),
            knotenIds = json.optJSONArray("knotenIds")?.let { ids ->
                List(ids.length()) { index -> KnotenId(ids.getString(index)) }.toSet()
            } ?: emptySet(),
            titel = json.optString("titel", VISUELLE_GRUPPE_STANDARD_TITEL),
            position = GraphPunkt(
                position?.optDouble("x", 0.0)?.toFloat() ?: 0f,
                position?.optDouble("y", 0.0)?.toFloat() ?: 0f,
            ),
            größe = GraphGröße(
                größe?.optDouble("breite", 0.0)?.toFloat() ?: 0f,
                größe?.optDouble("höhe", 0.0)?.toFloat() ?: 0f,
            ),
        )
    }

    private fun eigenschaftZuJson(wert: KnotenEigenschaft): JSONObject = JSONObject().apply {
        when (wert) {
            is KnotenEigenschaft.Text -> { put("typ", "text"); put("wert", wert.wert) }
            is KnotenEigenschaft.Ganzzahl -> { put("typ", "ganzzahl"); put("wert", wert.wert) }
            is KnotenEigenschaft.Dezimalzahl -> { put("typ", "dezimalzahl"); put("wert", wert.wert) }
            is KnotenEigenschaft.Wahrheitswert -> { put("typ", "wahrheitswert"); put("wert", wert.wert) }
            is KnotenEigenschaft.Farbe -> { put("typ", "farbe"); put("argb", wert.argb) }
            is KnotenEigenschaft.Liste -> {
                put("typ", "liste")
                put("werte", JSONArray().apply { wert.werte.forEach { put(eigenschaftZuJson(it)) } })
            }
            is KnotenEigenschaft.Objekt -> {
                put("typ", "objekt")
                put("felder", JSONObject().apply {
                    wert.felder.forEach { (schlüssel, feld) -> put(schlüssel, eigenschaftZuJson(feld)) }
                })
            }
        }
    }

    private fun eigenschaftVonJson(json: JSONObject?): KnotenEigenschaft? = when (json?.optString("typ")) {
        "text" -> KnotenEigenschaft.Text(json.optString("wert"))
        "ganzzahl" -> KnotenEigenschaft.Ganzzahl(json.optInt("wert"))
        "dezimalzahl" -> json.optDouble("wert", Double.NaN).takeIf { it.isFinite() }?.let(KnotenEigenschaft::Dezimalzahl)
        "wahrheitswert" -> KnotenEigenschaft.Wahrheitswert(json.optBoolean("wert"))
        "farbe" -> KnotenEigenschaft.Farbe(json.optLong("argb"))
        "liste" -> json.optJSONArray("werte")?.let { werte ->
            KnotenEigenschaft.Liste(List(werte.length()) { index ->
                eigenschaftVonJson(werte.optJSONObject(index)) ?: KnotenEigenschaft.Text("")
            })
        }
        "objekt" -> json.optJSONObject("felder")?.let { felder ->
            KnotenEigenschaft.Objekt(felder.keys().asSequence().mapNotNull { key ->
                eigenschaftVonJson(felder.optJSONObject(key))?.let { key to it }
            }.toMap())
        }
        else -> null
    }

    private fun verbindungZuJson(verbindung: VerbindungDaten) = JSONObject().apply {
        put("id", verbindung.id.wert)
        put("von", refZuJson(verbindung.von))
        put("zu", refZuJson(verbindung.zu))
    }

    private fun refZuJson(referenz: AnschlussVerweis) = JSONObject()
        .put("knotenId", referenz.knotenId.wert)
        .put("anschlussId", referenz.anschlussId.wert)

    private fun verbindungVonJson(json: JSONObject) = VerbindungDaten(
        id = VerbindungsId(json.getString("id")),
        von = refVonJson(json.getJSONObject("von")),
        zu = refVonJson(json.getJSONObject("zu")),
    )

    private fun refVonJson(json: JSONObject) = AnschlussVerweis(
        KnotenId(json.getString("knotenId")),
        AnschlussId(json.getString("anschlussId")),
    )

    private fun <T> JSONArray?.zuListe(wandler: (JSONObject) -> T): List<T> =
        if (this == null) emptyList() else List(length()) { wandler(getJSONObject(it)) }
}

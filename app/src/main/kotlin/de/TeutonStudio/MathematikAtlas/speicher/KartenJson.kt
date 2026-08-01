package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import org.json.JSONArray
import org.json.JSONObject

object KartenJson {
    fun schreibe(karte: KartenDaten): String = JSONObject().apply {
        put("formatVersion", 5)
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

    fun lese(text: String): KartenDaten {
        val json = JSONObject(text)
        val ansicht = json.optJSONObject("ansicht")
        return KartenDaten(
            id = KartenId(json.getString("id")),
            name = json.getString("name"),
            version = json.optInt("version", 1),
            erstelltAm = json.optLong("erstelltAm", System.currentTimeMillis()),
            knoten = json.optJSONArray("knoten").zuListe(::knotenVonJson),
            verbindungen = json.optJSONArray("verbindungen").zuListe(::verbindungVonJson),
            visuelleGruppen = json.optJSONArray("visuelleGruppen").zuListe(::visuelleGruppeVonJson),
            ansicht = AnsichtsFenster(
                GraphPunkt(ansicht?.optDouble("x", 0.0)?.toFloat() ?: 0f, ansicht?.optDouble("y", 0.0)?.toFloat() ?: 0f),
                ansicht?.optDouble("zoom", 1.0)?.toFloat() ?: 1f,
            ),
            archiviert = json.optBoolean("archiviert", false),
        ).bereinigteVisuelleGruppen()
    }

    private fun knotenZuJson(k: KnotenDaten) = JSONObject().apply {
        put("id", k.id.wert); put("art", k.art); put("name", k.name)
        put("position", JSONObject().put("x", k.position.x).put("y", k.position.y))
        put("größe", JSONObject().put("breite", k.größe.breite).put("höhe", k.größe.höhe))
        put("parameter", JSONObject(k.parameter))
        put("eigenschaften", JSONObject().apply {
            k.eigenschaften.forEach { (schlüssel, wert) -> put(schlüssel, eigenschaftZuJson(wert)) }
        })
        k.kartenVerweis?.let { put("kartenVerweis", it.zuJson()) }
        if (k.eingangsKartenVerweise.isNotEmpty()) {
            put("eingangsKartenVerweise", JSONObject().apply {
                k.eingangsKartenVerweise.toSortedMap().forEach { (name, verweis) -> put(name, verweis.zuJson()) }
            })
        }
        put("anschlüsse", JSONArray().apply { k.anschlüsse.forEach { a -> put(JSONObject().apply {
            put("id", a.id.wert); put("name", a.name); put("richtung", a.richtung.name); put("kante", a.kante.name)
            put("art", a.art.wert); put("reihenfolge", a.reihenfolge)
            put("kannSichErweitern", a.kannSichErweitern); put("dynamischErzeugt", a.dynamischErzeugt)
            a.artFolgtEingang?.let { put("artFolgtEingang", it) }
            if (a.artVereinigtEingänge.isNotEmpty()) put("artVereinigtEingänge", JSONArray(a.artVereinigtEingänge))
        }) } })
    }

    private fun knotenVonJson(j: JSONObject): KnotenDaten {
        val p = j.getJSONObject("position")
        val g = j.getJSONObject("größe")
        val verweis = j.optJSONObject("kartenVerweis")?.zuKartenVerweis()
        val eingangsVerweiseJson = j.optJSONObject("eingangsKartenVerweise") ?: JSONObject()
        val eingangsVerweise = eingangsVerweiseJson.keys().asSequence().mapNotNull { name ->
            eingangsVerweiseJson.optJSONObject(name)?.zuKartenVerweis()?.let { name to it }
        }.toMap()
        val parameterJson = j.optJSONObject("parameter") ?: JSONObject()
        val parameter = parameterJson.keys().asSequence().associateWith { parameterJson.optString(it) }
        val eigenschaftenJson = j.optJSONObject("eigenschaften") ?: JSONObject()
        val eigenschaften = eigenschaftenJson.keys().asSequence().mapNotNull { schlüssel ->
            eigenschaftVonJson(eigenschaftenJson.optJSONObject(schlüssel))?.let { schlüssel to it }
        }.toMap()
        return KnotenDaten(
            id = KnotenId(j.getString("id")), art = j.getString("art"), name = j.getString("name"),
            position = GraphPunkt(p.getDouble("x").toFloat(), p.getDouble("y").toFloat()),
            größe = GraphGröße(g.getDouble("breite").toFloat(), g.getDouble("höhe").toFloat()),
            anschlüsse = j.optJSONArray("anschlüsse").zuListe { a -> AnschlussDaten(
                id = AnschlussId(a.getString("id")), name = a.getString("name"),
                richtung = enumValueOf(a.optString("richtung", "Neutral")),
                kante = enumValueOf(a.getString("kante")), art = AnschlussArtId(a.getString("art")),
                reihenfolge = a.optInt("reihenfolge", 0),
                kannSichErweitern = a.optBoolean("kannSichErweitern", false),
                dynamischErzeugt = a.optBoolean("dynamischErzeugt", false),
                artFolgtEingang = a.optString("artFolgtEingang").takeIf(String::isNotBlank),
                artVereinigtEingänge = a.optJSONArray("artVereinigtEingänge")?.let { namen ->
                    List(namen.length()) { index -> namen.getString(index) }
                } ?: emptyList(),
            ) },
            parameter = parameter,
            eigenschaften = eigenschaften,
            kartenVerweis = verweis,
            eingangsKartenVerweise = eingangsVerweise,
        )
    }

    private fun KartenVerweis.zuJson() = JSONObject()
        .put("kartenId", kartenId.wert)
        .put("version", version)

    private fun JSONObject.zuKartenVerweis() =
        KartenVerweis(KartenId(getString("kartenId")), getInt("version"))

    private fun visuelleGruppeZuJson(gruppe: VisuelleKnotenGruppeDaten) = JSONObject().apply {
        put("id", gruppe.id.wert)
        put("knotenIds", JSONArray().apply { gruppe.knotenIds.forEach { put(it.wert) } })
    }

    private fun visuelleGruppeVonJson(json: JSONObject) = VisuelleKnotenGruppeDaten(
        id = VisuelleGruppenId(json.getString("id")),
        knotenIds = json.optJSONArray("knotenIds")?.let { ids ->
            List(ids.length()) { index -> KnotenId(ids.getString(index)) }.toSet()
        } ?: emptySet(),
    )

    private fun eigenschaftZuJson(wert: KnotenEigenschaft): JSONObject = JSONObject().apply {
        when (wert) {
            is KnotenEigenschaft.Text -> { put("typ", "text"); put("wert", wert.wert) }
            is KnotenEigenschaft.Ganzzahl -> { put("typ", "ganzzahl"); put("wert", wert.wert) }
            is KnotenEigenschaft.Dezimalzahl -> { put("typ", "dezimalzahl"); put("wert", wert.wert) }
            is KnotenEigenschaft.Wahrheitswert -> { put("typ", "wahrheitswert"); put("wert", wert.wert) }
            is KnotenEigenschaft.Farbe -> { put("typ", "farbe"); put("argb", wert.argb) }
            is KnotenEigenschaft.Liste -> { put("typ", "liste"); put("werte", JSONArray().apply { wert.werte.forEach { put(eigenschaftZuJson(it)) } }) }
            is KnotenEigenschaft.Objekt -> { put("typ", "objekt"); put("felder", JSONObject().apply { wert.felder.forEach { (k, v) -> put(k, eigenschaftZuJson(v)) } }) }
        }
    }

    /** Liest nur explizit typisierte Werte; unbekannte Daten werden sicher ausgelassen. */
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

    private fun verbindungZuJson(v: VerbindungDaten) = JSONObject().apply {
        put("id", v.id.wert)
        put("von", refZuJson(v.von)); put("zu", refZuJson(v.zu))
    }
    private fun refZuJson(r: AnschlussVerweis) = JSONObject().put("knotenId", r.knotenId.wert).put("anschlussId", r.anschlussId.wert)
    private fun verbindungVonJson(j: JSONObject) = VerbindungDaten(
        id = VerbindungsId(j.getString("id")), von = refVonJson(j.getJSONObject("von")), zu = refVonJson(j.getJSONObject("zu")),
    )
    private fun refVonJson(j: JSONObject) = AnschlussVerweis(KnotenId(j.getString("knotenId")), AnschlussId(j.getString("anschlussId")))

    private fun <T> JSONArray?.zuListe(wandler: (JSONObject) -> T): List<T> =
        if (this == null) emptyList() else List(length()) { wandler(getJSONObject(it)) }
}

package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import org.json.JSONArray
import org.json.JSONObject

object KartenJson {
    fun schreibe(karte: KartenDaten): String = JSONObject().apply {
        put("formatVersion", 1)
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
            ansicht = AnsichtsFenster(
                GraphPunkt(ansicht?.optDouble("x", 0.0)?.toFloat() ?: 0f, ansicht?.optDouble("y", 0.0)?.toFloat() ?: 0f),
                ansicht?.optDouble("zoom", 1.0)?.toFloat() ?: 1f,
            ),
            archiviert = json.optBoolean("archiviert", false),
        )
    }

    private fun knotenZuJson(k: KnotenDaten) = JSONObject().apply {
        put("id", k.id.wert); put("art", k.art); put("name", k.name)
        put("position", JSONObject().put("x", k.position.x).put("y", k.position.y))
        put("größe", JSONObject().put("breite", k.größe.breite).put("höhe", k.größe.höhe))
        put("parameter", JSONObject(k.parameter))
        k.kartenVerweis?.let { put("kartenVerweis", JSONObject().put("kartenId", it.kartenId.wert).put("version", it.version)) }
        put("anschlüsse", JSONArray().apply { k.anschlüsse.forEach { a -> put(JSONObject().apply {
            put("id", a.id.wert); put("name", a.name); put("richtung", a.richtung.name); put("kante", a.kante.name)
            put("art", a.art.wert); put("reihenfolge", a.reihenfolge)
            put("kannSichErweitern", a.kannSichErweitern); put("dynamischErzeugt", a.dynamischErzeugt)
        }) } })
    }

    private fun knotenVonJson(j: JSONObject): KnotenDaten {
        val p = j.getJSONObject("position")
        val g = j.getJSONObject("größe")
        val verweis = j.optJSONObject("kartenVerweis")?.let { KartenVerweis(KartenId(it.getString("kartenId")), it.getInt("version")) }
        val parameterJson = j.optJSONObject("parameter") ?: JSONObject()
        val parameter = parameterJson.keys().asSequence().associateWith { parameterJson.optString(it) }
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
            ) },
            parameter = parameter, kartenVerweis = verweis,
        )
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

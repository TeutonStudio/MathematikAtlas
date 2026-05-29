package com.TeutonStudio.MathematikAtlas

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.TeutonStudio.KnotenKartenVerwalter.daten.AnsichtsfensterDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.AusgangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.EingangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.FlächeDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.PositionDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KartenKontextAktion
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KartenTreffer
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.zuComposable
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

sealed class Screen(val route: String) {
    data object StartScreen : Screen(route = "start_screen")
}

@Composable
fun Navigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.StartScreen.route,
    ) {
        composable(Screen.StartScreen.route) {
            KnotenKartenTestAnwendung()
        }
    }
}

@Composable
private fun KnotenKartenTestAnwendung() {
    val context = LocalContext.current
    var gespeicherteKarten by remember { mutableStateOf(context.knotenKartenSpeicher().liste()) }
    var aktuelleKarte by remember { mutableStateOf(context.knotenKartenSpeicher().ladeErste() ?: beispielKarte("Testkarte")) }
    var status by remember { mutableStateOf("Bereit") }

    fun aktualisiereListe() {
        gespeicherteKarten = context.knotenKartenSpeicher().liste()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
            .padding(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        SeitenLeiste(
            karten = gespeicherteKarten,
            aktuelleKarte = aktuelleKarte,
            status = status,
            onNeueKarte = {
                aktuelleKarte = beispielKarte("Neue KnotenKarte")
                status = "Neue KnotenKarte erstellt"
            },
            onSpeichern = {
                context.knotenKartenSpeicher().speichere(aktuelleKarte)
                aktualisiereListe()
                status = "'${aktuelleKarte.name}' gespeichert"
            },
            onOeffnen = { eintrag ->
                aktuelleKarte = context.knotenKartenSpeicher().lade(eintrag.datei) ?: aktuelleKarte
                status = "'${eintrag.name}' geoeffnet"
            },
            onNameAendern = { name ->
                aktuelleKarte = aktuelleKarte.copy(name = name)
            },
            onKnotenHinzufuegen = {
                aktuelleKarte = aktuelleKarte.mitNeuemKnoten()
                status = "Knoten hinzugefuegt"
            },
        )

        Spacer(Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(8.dp)),
        ) {
            aktuelleKarte.zuComposable(
                modifier = Modifier.fillMaxSize(),
                zustand = KarteZustand(zeigeÜbersicht = true, zeigeKontrollLeiste = true),
                aktualisierung = { knotenId, position ->
                    aktuelleKarte = aktuelleKarte.copy(
                        knoten = aktuelleKarte.knoten.map { knoten ->
                            if (knoten.id == knotenId) knoten.copy(position = position) else knoten
                        },
                    )
                },
                onVerbindungErstellen = { verbindung ->
                    aktuelleKarte = aktuelleKarte.copy(
                        verbindungen = aktuelleKarte.verbindungen
                            .filterNot { it.id == verbindung.id } + verbindung,
                    )
                    status = "Verbindung erstellt"
                },
                onKontextAktion = { aktion ->
                    aktuelleKarte = aktuelleKarte.führeKontextAktionAus(aktion)
                    status = "Kontext: ${aktion.aktion}"
                },
            )
        }
    }
}

@Composable
private fun SeitenLeiste(
    karten: List<KartenEintrag>,
    aktuelleKarte: KarteDaten,
    status: String,
    onNeueKarte: () -> Unit,
    onSpeichern: () -> Unit,
    onOeffnen: (KartenEintrag) -> Unit,
    onNameAendern: (String) -> Unit,
    onKnotenHinzufuegen: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(8.dp))
            .padding(12.dp),
    ) {
        BasicText(
            text = "KnotenKarten Test",
            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827)),
        )
        Spacer(Modifier.height(12.dp))

        EingabeFeld(
            wert = aktuelleKarte.name,
            onWertAendern = onNameAendern,
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TestKnopf(text = "Neu", onClick = onNeueKarte, modifier = Modifier.weight(1f))
            TestKnopf(text = "Speichern", onClick = onSpeichern, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        TestKnopf(text = "Knoten erstellen", onClick = onKnotenHinzufuegen, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(16.dp))
        BasicText(
            text = "Gespeicherte Karten",
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151)),
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(karten, key = { it.datei }) { eintrag ->
                KartenZeile(
                    eintrag = eintrag,
                    ausgewaehlt = eintrag.id == aktuelleKarte.id,
                    onClick = { onOeffnen(eintrag) },
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        BasicText(
            text = status,
            style = TextStyle(fontSize = 13.sp, color = Color(0xFF4B5563)),
        )
    }
}

@Composable
private fun EingabeFeld(
    wert: String,
    onWertAendern: (String) -> Unit,
) {
    BasicTextField(
        value = wert,
        onValueChange = onWertAendern,
        textStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF111827)),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FAFB), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
    )
}

@Composable
private fun TestKnopf(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .background(Color(0xFF2563EB), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White),
        )
    }
}

@Composable
private fun KartenZeile(
    eintrag: KartenEintrag,
    ausgewaehlt: Boolean,
    onClick: () -> Unit,
) {
    val hintergrund = if (ausgewaehlt) Color(0xFFEFF6FF) else Color(0xFFF9FAFB)
    val rand = if (ausgewaehlt) Color(0xFF2563EB) else Color(0xFFE5E7EB)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(hintergrund, RoundedCornerShape(6.dp))
            .border(1.dp, rand, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        BasicText(
            text = eintrag.name,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827)),
        )
        BasicText(
            text = "${eintrag.knotenAnzahl} Knoten",
            style = TextStyle(fontSize = 12.sp, color = Color(0xFF6B7280)),
        )
    }
}

private data class KartenEintrag(
    val id: String,
    val name: String,
    val datei: String,
    val knotenAnzahl: Int,
)

private class KnotenKartenSpeicher(context: Context) {
    private val ordner = File(context.filesDir, "knotenkarten")

    fun liste(): List<KartenEintrag> {
        if (!ordner.exists()) return emptyList()
        return ordner.listFiles { file -> file.extension == "json" }
            .orEmpty()
            .mapNotNull { file ->
                runCatching {
                    val json = JSONObject(file.readText())
                    KartenEintrag(
                        id = json.getString("id"),
                        name = json.getString("name"),
                        datei = file.name,
                        knotenAnzahl = json.getJSONArray("knoten").length(),
                    )
                }.getOrNull()
            }
            .sortedBy { it.name.lowercase() }
    }

    fun ladeErste(): KarteDaten? = liste().firstOrNull()?.let { lade(it.datei) }

    fun lade(datei: String): KarteDaten? {
        val ziel = File(ordner, datei)
        if (!ziel.exists()) return null
        return runCatching { JSONObject(ziel.readText()).zuKarteDaten() }.getOrNull()
    }

    fun speichere(karte: KarteDaten) {
        if (!ordner.exists()) ordner.mkdirs()
        val ziel = File(ordner, "${karte.id}.json")
        ziel.writeText(karte.zuJson().toString(2))
    }
}

private fun Context.knotenKartenSpeicher() = KnotenKartenSpeicher(this)

private fun beispielKarte(name: String): KarteDaten {
    val id = UUID.randomUUID().toString()
    return KarteDaten(
        id = id,
        name = name,
        knoten = listOf(
            KnotenDaten(
                id = "definition",
                name = "Definition",
                position = PositionDaten(60f, 80f),
                ausgänge = listOf(AusgangDaten("weiter", "weiter")),
            ),
            KnotenDaten(
                id = "satz",
                name = "Satz",
                position = PositionDaten(360f, 260f),
                eingänge = listOf(EingangDaten("vorher", "vorher")),
            ),
        ),
        verbindungen = listOf(
            VerbindungDaten(
                id = "verbindung-1",
                quellKnotenId = "definition",
                quellAnschlussId = "weiter",
                zielKnotenId = "satz",
                zielAnschlussId = "vorher",
            ),
        ),
    )
}

private fun KarteDaten.mitNeuemKnoten(position: PositionDaten? = null): KarteDaten {
    val nummer = knoten.size + 1
    val id = "knoten-$nummer-${UUID.randomUUID()}"
    return copy(
        knoten = knoten + KnotenDaten(
            id = id,
            name = "Knoten $nummer",
            position = position ?: PositionDaten(
                waagrecht = 90f + nummer * 40f,
                senkrecht = 120f + nummer * 30f,
            ),
            fläche = FlächeDaten(180f, 96f),
            eingänge = listOf(EingangDaten("in", "in")),
            ausgänge = listOf(AusgangDaten("out", "out")),
        ),
    )
}

private fun KarteDaten.führeKontextAktionAus(aktion: KartenKontextAktion): KarteDaten = when (aktion.aktion) {
    "Knoten erstellen" -> mitNeuemKnoten(aktion.weltPosition)
    "Knoten loeschen" -> {
        val ziel = aktion.ziel as? KartenTreffer.Knoten ?: return this
        copy(
            knoten = knoten.filterNot { it.id == ziel.knotenId },
            verbindungen = verbindungen.filterNot {
                it.quellKnotenId == ziel.knotenId || it.zielKnotenId == ziel.knotenId
            },
        )
    }
    "Verbindung loeschen" -> {
        val ziel = aktion.ziel as? KartenTreffer.Verbindung ?: return this
        copy(verbindungen = verbindungen.filterNot { it.id == ziel.verbindungId })
    }
    else -> this
}

private fun KarteDaten.zuJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("ansichtsfenster", ansichtsfenster.zuJson())
    .put("knoten", JSONArray(knoten.map { it.zuJson() }))
    .put("verbindungen", JSONArray(verbindungen.map { it.zuJson() }))

private fun KnotenDaten.zuJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("position", JSONObject().put("waagrecht", position.waagrecht).put("senkrecht", position.senkrecht))
    .put("flaeche", JSONObject().put("waagrecht", fläche.waagrecht).put("senkrecht", fläche.senkrecht))
    .put("typ", typ)
    .put("beweglich", beweglich)
    .put("eingaenge", JSONArray(eingänge.map { JSONObject().put("id", it.id).put("label", it.label) }))
    .put("ausgaenge", JSONArray(ausgänge.map { JSONObject().put("id", it.id).put("label", it.label) }))

private fun VerbindungDaten.zuJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("quellKnotenId", quellKnotenId)
    .put("quellAnschlussId", quellAnschlussId)
    .put("zielKnotenId", zielKnotenId)
    .put("zielAnschlussId", zielAnschlussId)
    .put("label", label)
    .put("typ", typ)

private fun AnsichtsfensterDaten.zuJson(): JSONObject = JSONObject()
    .put("x", x)
    .put("y", y)
    .put("zoom", zoom)

private fun JSONObject.zuKarteDaten(): KarteDaten = KarteDaten(
    id = getString("id"),
    name = getString("name"),
    knoten = getJSONArray("knoten").asObjects().map { it.zuKnotenDaten() },
    verbindungen = getJSONArray("verbindungen").asObjects().map { it.zuVerbindungDaten() },
    ansichtsfenster = optJSONObject("ansichtsfenster")?.let {
        AnsichtsfensterDaten(
            x = it.optDouble("x", 0.0).toFloat(),
            y = it.optDouble("y", 0.0).toFloat(),
            zoom = it.optDouble("zoom", 1.0).toFloat(),
        )
    } ?: AnsichtsfensterDaten(),
)

private fun JSONObject.zuKnotenDaten(): KnotenDaten {
    val position = getJSONObject("position")
    val flaeche = getJSONObject("flaeche")
    return KnotenDaten(
        id = getString("id"),
        name = getString("name"),
        position = PositionDaten(
            waagrecht = position.getDouble("waagrecht").toFloat(),
            senkrecht = position.getDouble("senkrecht").toFloat(),
        ),
        fläche = FlächeDaten(
            waagrecht = flaeche.getDouble("waagrecht").toFloat(),
            senkrecht = flaeche.getDouble("senkrecht").toFloat(),
        ),
        typ = optString("typ", "default"),
        beweglich = optBoolean("beweglich", true),
        eingänge = optJSONArray("eingaenge").orEmptyObjects().map {
            EingangDaten(it.getString("id"), it.getString("label"))
        },
        ausgänge = optJSONArray("ausgaenge").orEmptyObjects().map {
            AusgangDaten(it.getString("id"), it.getString("label"))
        },
    )
}

private fun JSONObject.zuVerbindungDaten(): VerbindungDaten = VerbindungDaten(
    id = getString("id"),
    quellKnotenId = getString("quellKnotenId"),
    quellAnschlussId = getString("quellAnschlussId"),
    zielKnotenId = getString("zielKnotenId"),
    zielAnschlussId = getString("zielAnschlussId"),
    label = optString("label").takeIf { it.isNotBlank() && it != "null" },
    typ = optString("typ", "default"),
)

private fun JSONArray?.orEmptyObjects(): List<JSONObject> = this?.asObjects().orEmpty()

private fun JSONArray.asObjects(): List<JSONObject> = List(length()) { index -> getJSONObject(index) }

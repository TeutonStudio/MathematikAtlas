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
import androidx.compose.ui.geometry.Offset
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
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.AppKartenCommand
import com.TeutonStudio.KnotenKartenVerwalter.daten.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KartenCommand
import com.TeutonStudio.KnotenKartenVerwalter.daten.KartenCommandErgebnis
import com.TeutonStudio.KnotenKartenVerwalter.daten.KartenControllerZustand
import com.TeutonStudio.KnotenKartenVerwalter.daten.KartenCacheDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KartenLayoutAnwenden
import com.TeutonStudio.KnotenKartenVerwalter.daten.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.daten.KartenZwischenablage
import com.TeutonStudio.KnotenKartenVerwalter.daten.KnotenCacheEintrag
import com.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KnotenAendern
import com.TeutonStudio.KnotenKartenVerwalter.daten.KnotenErstellen
import com.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVerschieben
import com.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungErstellen
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlEinfuegen
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlLoeschen
import com.TeutonStudio.KnotenKartenVerwalter.daten.ZahlenTyp
import com.TeutonStudio.KnotenKartenVerwalter.daten.Zahlenraum
import com.TeutonStudio.KnotenKartenVerwalter.daten.istKompatibelMit
import com.TeutonStudio.KnotenKartenVerwalter.daten.dupliziereAuswahl
import com.TeutonStudio.KnotenKartenVerwalter.daten.kopiereAuswahl
import com.TeutonStudio.KnotenKartenVerwalter.daten.loescheAuswahl
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KartenKontextAktion
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KartenTreffer
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AusgabeKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.EingabeKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AuswertungsKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.FormelKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.FunktionKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.MathematikEingabeKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.RechenKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.UnbekannteKnoten
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
    val speicher = remember { context.knotenKartenSpeicher() }
    var gespeicherteKarten by remember { mutableStateOf(speicher.liste()) }
    var controller by remember {
        mutableStateOf(KartenControllerZustand(speicher.ladeErste() ?: beispielKarte("Mathematik Beispiel")))
    }
    var zwischenablage by remember { mutableStateOf(KartenZwischenablage()) }
    var status by remember { mutableStateOf("Bereit") }
    val aktuelleKarte = controller.karte
    val auswahl = controller.auswahl

    fun aktualisiereListe() {
        gespeicherteKarten = speicher.liste()
    }

    fun fuehreAus(command: KartenCommand) {
        controller = controller.fuehreAus(command)
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
            auswahl = auswahl,
            status = status,
            onNeueKarte = {
                controller = KartenControllerZustand(beispielKarte("Neue KnotenKarte"))
                status = "Neue KnotenKarte erstellt"
            },
            onBeispielKarte = {
                controller = KartenControllerZustand(beispielKarte("Mathematik Beispiel"))
                status = "Beispielkarte geladen"
            },
            onFunktionsBeispiel = {
                controller = KartenControllerZustand(funktionsBeispielKarte("Funktionskarte"))
                status = "Funktionsbeispiel geladen"
            },
            onSpeichern = {
                speicher.speichere(aktuelleKarte)
                aktualisiereListe()
                status = "'${aktuelleKarte.name}' gespeichert"
            },
            onOeffnen = { eintrag ->
                controller = KartenControllerZustand(speicher.lade(eintrag.datei) ?: aktuelleKarte)
                status = "'${eintrag.name}' geoeffnet"
            },
            onDuplizieren = {
                val duplikat = aktuelleKarte.duplizierteKarte()
                speicher.speichere(duplikat)
                controller = KartenControllerZustand(duplikat)
                aktualisiereListe()
                status = "'${aktuelleKarte.name}' dupliziert"
            },
            onLoeschen = {
                speicher.loesche(aktuelleKarte.id)
                aktualisiereListe()
                controller = KartenControllerZustand(speicher.ladeErste() ?: beispielKarte("Neue KnotenKarte"))
                status = "Karte geloescht"
            },
            onNameAendern = { name ->
                fuehreAus(AppKartenCommand("Karte umbenennen") { karte, aktuelleAuswahl ->
                    KartenCommandErgebnis(KarteDaten(karte, name = name), aktuelleAuswahl)
                })
            },
            onKnotenHinzufuegen = { art ->
                val knoten = aktuelleKarte.neuerKnoten(art = art)
                fuehreAus(KnotenErstellen(knoten))
                status = "Knoten '${knoten.name}' hinzugefuegt"
            },
            onAuswerten = {
                fuehreAus(AppKartenCommand("Mathematik auswerten") { karte, aktuelleAuswahl ->
                    KartenCommandErgebnis(
                        karte = karte.werteMathematikAus(speicher.alleKarten() + (karte.id to karte)),
                        auswahl = aktuelleAuswahl,
                    )
                })
                status = "Mathematik ausgewertet"
            },
            onKopieren = {
                zwischenablage = aktuelleKarte.kopiereAuswahl(auswahl)
                status = if (zwischenablage.istLeer) "Keine Auswahl zum Kopieren" else "Auswahl kopiert"
            },
            onEinfuegen = {
                if (zwischenablage.istLeer) {
                    status = "Zwischenablage ist leer"
                } else {
                    val ziel = Offset(120f + aktuelleKarte.knoten.size * 12f, 120f + aktuelleKarte.knoten.size * 12f)
                    fuehreAus(AuswahlEinfuegen(zwischenablage, ziel))
                    status = "Auswahl eingefuegt"
                }
            },
            onAllesAuswaehlen = {
                val neueAuswahl = AuswahlDaten(
                    knotenIds = aktuelleKarte.knoten.mapTo(mutableSetOf()) { it.id },
                    verbindungIds = aktuelleKarte.verbindungen.mapTo(mutableSetOf()) { it.id },
                )
                controller = controller.mitAuswahl(neueAuswahl)
                status = neueAuswahl.statusText()
            },
            onAuswahlLoeschen = {
                fuehreAus(AuswahlLoeschen(auswahl))
                status = "Auswahl geloescht"
            },
            onAuswahlDuplizieren = {
                fuehreAus(AppKartenCommand("Auswahl duplizieren") { karte, aktuelleAuswahl ->
                    val ergebnis = karte.dupliziereAuswahl(aktuelleAuswahl)
                    KartenCommandErgebnis(ergebnis.karte, ergebnis.auswahl, ausgefuehrt = !ergebnis.auswahl.istLeer)
                })
                status = if (auswahl.istLeer) "Keine Auswahl zum Duplizieren" else "Auswahl dupliziert"
            },
            onAuswahlLeeren = {
                controller = controller.mitAuswahl(AuswahlDaten())
                status = "Keine Auswahl"
            },
            onLayout = {
                fuehreAus(KartenLayoutAnwenden())
                status = "Layout angewendet"
            },
            onRueckgaengig = {
                controller = controller.rueckgaengig()
                status = "Rueckgaengig"
            },
            onWiederholen = {
                controller = controller.wiederholen()
                status = "Wiederholt"
            },
            onKnotenNameAendern = { knotenId, name ->
                aktuelleKarte.knoten.firstOrNull { it.id == knotenId }?.let { knoten ->
                    fuehreAus(KnotenAendern(knoten.copy(name = name)))
                    status = "Knoten bearbeitet"
                }
            },
            onKnotenDataAendern = { knotenId, key, wert ->
                aktuelleKarte.knoten.firstOrNull { it.id == knotenId }?.let { knoten ->
                    val neueDaten = knoten.data + (key to wert)
                    fuehreAus(KnotenAendern(knoten.copy(data = neueDaten.mitAktualisierterKurzform(knoten.art, knoten.name))))
                    status = "Knoten bearbeitet"
                }
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
                zustand = KarteZustand(
                    zeigeÜbersicht = true,
                    zeigeKontrollLeiste = true,
                    auswahl = auswahl,
                ),
                aktualisierung = { knotenId, position ->
                    fuehreAus(KnotenVerschieben(knotenId, position))
                },
                onVerbindungErstellen = { verbindung ->
                    fuehreAus(VerbindungErstellen(verbindung))
                    status = "Verbindung erstellt"
                },
                onKontextAktion = { aktion ->
                    fuehreAus(AppKartenCommand("Kontext: ${aktion.aktion}") { karte, aktuelleAuswahl ->
                        val ergebnis = karte.führeKontextAktionAus(aktion, aktuelleAuswahl)
                        KartenCommandErgebnis(ergebnis.karte, ergebnis.auswahl)
                    })
                    status = "Kontext: ${aktion.aktion}"
                },
                onAuswahlÄndern = { neueAuswahl ->
                    controller = controller.mitAuswahl(neueAuswahl)
                    status = neueAuswahl.statusText()
                },
            )
        }
    }
}

private fun AuswahlDaten.statusText(): String = when {
    istLeer -> "Keine Auswahl"
    knotenIds.size == 1 && verbindungIds.isEmpty() -> "Knoten ausgewaehlt"
    verbindungIds.size == 1 && knotenIds.isEmpty() -> "Verbindung ausgewaehlt"
    else -> "${knotenIds.size} Knoten, ${verbindungIds.size} Verbindungen ausgewaehlt"
}

@Composable
private fun SeitenLeiste(
    karten: List<KartenEintrag>,
    aktuelleKarte: KarteDaten,
    auswahl: AuswahlDaten,
    status: String,
    onNeueKarte: () -> Unit,
    onBeispielKarte: () -> Unit,
    onFunktionsBeispiel: () -> Unit,
    onSpeichern: () -> Unit,
    onOeffnen: (KartenEintrag) -> Unit,
    onDuplizieren: () -> Unit,
    onLoeschen: () -> Unit,
    onNameAendern: (String) -> Unit,
    onKnotenHinzufuegen: (String) -> Unit,
    onAuswerten: () -> Unit,
    onKopieren: () -> Unit,
    onEinfuegen: () -> Unit,
    onAllesAuswaehlen: () -> Unit,
    onAuswahlLoeschen: () -> Unit,
    onAuswahlDuplizieren: () -> Unit,
    onAuswahlLeeren: () -> Unit,
    onLayout: () -> Unit,
    onRueckgaengig: () -> Unit,
    onWiederholen: () -> Unit,
    onKnotenNameAendern: (String, String) -> Unit,
    onKnotenDataAendern: (String, String, Any) -> Unit,
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TestKnopf(text = "Beispiel", onClick = onBeispielKarte, modifier = Modifier.weight(1f))
            TestKnopf(text = "Funktion", onClick = onFunktionsBeispiel, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TestKnopf(text = "Karte kop.", onClick = onDuplizieren, modifier = Modifier.weight(1f))
            TestKnopf(text = "Karte loe.", onClick = onLoeschen, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        BasicText(
            text = "Mathematische Knoten",
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151)),
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TestKnopf(text = "Eingabe", onClick = { onKnotenHinzufuegen(MathematikEingabeKnoten.KNOTEN_ART) }, modifier = Modifier.weight(1f))
            TestKnopf(text = "Unbek.", onClick = { onKnotenHinzufuegen(UnbekannteKnoten.KNOTEN_ART) }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TestKnopf(text = "Rechen", onClick = { onKnotenHinzufuegen(RechenKnoten.KNOTEN_ART) }, modifier = Modifier.weight(1f))
            TestKnopf(text = "Formel", onClick = { onKnotenHinzufuegen(FormelKnoten.KNOTEN_ART) }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TestKnopf(text = "Auswert.", onClick = { onKnotenHinzufuegen(AuswertungsKnoten.KNOTEN_ART) }, modifier = Modifier.weight(1f))
            TestKnopf(text = "Funktion", onClick = { onKnotenHinzufuegen(FunktionKnoten.KNOTEN_ART) }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        TestKnopf(text = "LoesenKnoten", onClick = { onKnotenHinzufuegen(LOESEN_KNOTEN_ART) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        TestKnopf(text = "Auswerten", onClick = onAuswerten, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TestKnopf(text = "Kopieren", onClick = onKopieren, modifier = Modifier.weight(1f))
            TestKnopf(text = "Einfuegen", onClick = onEinfuegen, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TestKnopf(text = "Alle", onClick = onAllesAuswaehlen, modifier = Modifier.weight(1f))
            TestKnopf(text = "Leeren", onClick = onAuswahlLeeren, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        TestKnopf(text = "Layout", onClick = onLayout, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TestKnopf(text = "Undo", onClick = onRueckgaengig, modifier = Modifier.weight(1f))
            TestKnopf(text = "Redo", onClick = onWiederholen, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        AuswahlBearbeiter(
            karte = aktuelleKarte,
            auswahl = auswahl,
            onLoeschen = onAuswahlLoeschen,
            onDuplizieren = onAuswahlDuplizieren,
            onKnotenNameAendern = onKnotenNameAendern,
            onKnotenDataAendern = onKnotenDataAendern,
        )

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
private fun AuswahlBearbeiter(
    karte: KarteDaten,
    auswahl: AuswahlDaten,
    onLoeschen: () -> Unit,
    onDuplizieren: () -> Unit,
    onKnotenNameAendern: (String, String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FAFB), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(6.dp))
            .padding(10.dp),
    ) {
        BasicText(
            text = "Auswahl",
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151)),
        )
        Spacer(Modifier.height(8.dp))
        BasicText(
            text = auswahl.statusText(),
            style = TextStyle(fontSize = 13.sp, color = Color(0xFF4B5563)),
        )
        Spacer(Modifier.height(8.dp))

        val einzelnerKnoten = auswahl.knotenIds.singleOrNull()?.let { id ->
            karte.knoten.firstOrNull { it.id == id }
        }
        if (einzelnerKnoten != null && auswahl.verbindungIds.isEmpty()) {
            BasicText(
                text = "Name",
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B7280)),
            )
            Spacer(Modifier.height(4.dp))
            EingabeFeld(
                wert = einzelnerKnoten.name,
                onWertAendern = { onKnotenNameAendern(einzelnerKnoten.id, it) },
            )
            Spacer(Modifier.height(6.dp))
            BasicText(
                text = "Typ: ${einzelnerKnoten.art}",
                style = TextStyle(fontSize = 12.sp, color = Color(0xFF6B7280)),
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TestKnopf(text = "Duplizieren", onClick = onDuplizieren, modifier = Modifier.weight(1f))
            TestKnopf(text = "Loeschen", onClick = onLoeschen, modifier = Modifier.weight(1f))
        }
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
                position = Offset(60f, 80f),
                art = EingabeKnoten.KNOTEN_ART,
            ),
            KnotenDaten(
                id = "satz",
                name = "Satz",
                position = Offset(360f, 260f),
                art = AusgabeKnoten.KNOTEN_ART,
            ),
        ),
        verbindungen = listOf(
            VerbindungDaten(
                id = "verbindung-1",
                quellKnotenId = "definition",
                quellAnschlussId = "out",
                zielKnotenId = "satz",
                zielAnschlussId = "in",
            ),
        ),
    )
}

private fun KarteDaten.mitNeuemKnoten(position: Offset? = null): KarteDaten {
    return copy(knoten = knoten + neuerKnoten(position))
}

private fun KarteDaten.neuerKnoten(position: Offset? = null): KnotenDaten {
    val nummer = knoten.size + 1
    val id = "knoten-$nummer-${UUID.randomUUID()}"
    return KnotenDaten(
        id = id,
        name = "Knoten $nummer",
        position = position ?: Offset(
            x = 90f + nummer * 40f,
            y = 120f + nummer * 30f,
        ),
        fläche = Offset(180f, 96f),
    )
}

private data class KontextErgebnis(
    val karte: KarteDaten,
    val auswahl: AuswahlDaten,
)

private fun KarteDaten.führeKontextAktionAus(
    aktion: KartenKontextAktion,
    aktuelleAuswahl: AuswahlDaten,
): KontextErgebnis = when (aktion.aktion) {
    "Knoten erstellen" -> {
        val neueKarte = mitNeuemKnoten(aktion.weltPosition)
        KontextErgebnis(neueKarte, AuswahlDaten(knotenIds = setOf(neueKarte.knoten.last().id)))
    }
    "Knoten auswaehlen" -> {
        val ziel = aktion.ziel as? KartenTreffer.Knoten ?: return KontextErgebnis(this, aktuelleAuswahl)
        KontextErgebnis(this, AuswahlDaten(knotenIds = setOf(ziel.knotenId)))
    }
    "Verbindung auswaehlen" -> {
        val ziel = aktion.ziel as? KartenTreffer.Verbindung ?: return KontextErgebnis(this, aktuelleAuswahl)
        KontextErgebnis(this, AuswahlDaten(verbindungIds = setOf(ziel.verbindungId)))
    }
    "Knoten duplizieren" -> {
        val ziel = aktion.ziel as? KartenTreffer.Knoten ?: return KontextErgebnis(this, aktuelleAuswahl)
        val ergebnis = dupliziereAuswahl(AuswahlDaten(knotenIds = setOf(ziel.knotenId)))
        KontextErgebnis(ergebnis.karte, ergebnis.auswahl)
    }
    "Knoten loeschen" -> {
        val ziel = aktion.ziel as? KartenTreffer.Knoten ?: return KontextErgebnis(this, aktuelleAuswahl)
        KontextErgebnis(loescheAuswahl(AuswahlDaten(knotenIds = setOf(ziel.knotenId))), AuswahlDaten())
    }
    "Verbindung loeschen" -> {
        val ziel = aktion.ziel as? KartenTreffer.Verbindung ?: return KontextErgebnis(this, aktuelleAuswahl)
        KontextErgebnis(loescheAuswahl(AuswahlDaten(verbindungIds = setOf(ziel.verbindungId))), AuswahlDaten())
    }
    else -> KontextErgebnis(this, aktuelleAuswahl)
}

private fun KarteDaten.zuJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("ansichtsfenster", ansichtsfenster.zuJson())
    .put("knoten", JSONArray(knoten.map { it.zuJson() }))
    .put("verbindungen", JSONArray(verbindungen.map { it.zuJson() }))
    .put("cache", cache.zuJson())

private fun KnotenDaten.zuJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("position", JSONObject().put("x", position.x).put("y", position.y))
    .put("flaeche", JSONObject().put("x", fläche.x).put("y", fläche.y))
    .put("knotenArt", art)
    .put("typ", art)
    .put("beweglich", beweglich)

private fun VerbindungDaten.zuJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("quellKnotenId", quellKnotenId)
    .put("quellAnschlussId", quellAnschlussId)
    .put("zielKnotenId", zielKnotenId)
    .put("zielAnschlussId", zielAnschlussId)
    .put("label", label)
    .put("typ", art)

private fun AnsichtsfensterDaten.zuJson(): JSONObject = JSONObject()
    .put("x", verschiebung.x)
    .put("y", verschiebung.y)
    .put("zoom", zoom)

private fun JSONObject.zuKarteDaten(): KarteDaten = KarteDaten(
    id = getString("id"),
    name = getString("name"),
    knoten = getJSONArray("knoten").asObjects().map { it.zuKnotenDaten() },
    verbindungen = getJSONArray("verbindungen").asObjects().map { it.zuVerbindungDaten() },
    cache = optJSONObject("cache")?.zuKartenCacheDaten() ?: KartenCacheDaten(),
    ansichtsfenster = optJSONObject("ansichtsfenster")?.let {
        AnsichtsfensterDaten(
            verschiebung = Offset(
                x = it.optDouble("x", 0.0).toFloat(),
                y = it.optDouble("y", 0.0).toFloat(),
            ),
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
        position = Offset(
            x = position.optDouble("x", position.optDouble("waagrecht", 0.0)).toFloat(),
            y = position.optDouble("y", position.optDouble("senkrecht", 0.0)).toFloat(),
        ),
        fläche = Offset(
            x = flaeche.optDouble("x", flaeche.optDouble("waagrecht", 180.0)).toFloat(),
            y = flaeche.optDouble("y", flaeche.optDouble("senkrecht", 96.0)).toFloat(),
        ),
        art = optString("knotenArt", optString("typ", "default")),
        beweglich = optBoolean("beweglich", true),
    )
}

private fun JSONObject.zuVerbindungDaten(): VerbindungDaten = VerbindungDaten(
    id = getString("id"),
    quellKnotenId = getString("quellKnotenId"),
    quellAnschlussId = getString("quellAnschlussId"),
    zielKnotenId = getString("zielKnotenId"),
    zielAnschlussId = getString("zielAnschlussId"),
    label = optString("label").takeIf { it.isNotBlank() && it != "null" },
    art = optString("typ", "default"),
)

private fun KartenCacheDaten.zuJson(): JSONObject = JSONObject()
    .put("version", version)
    .put("knoten", JSONObject().also { ziel ->
        knoten.forEach { (knotenId, eintrag) -> ziel.put(knotenId, eintrag.zuJson()) }
    })

private fun KnotenCacheEintrag.zuJson(): JSONObject = JSONObject()
    .put("knotenId", knotenId)
    .put("signatur", signatur)
    .put("daten", JSONObject().also { ziel ->
        daten.forEach { (key, value) -> ziel.put(key, value) }
    })
    .put("fehler", fehler)
    .put("gueltig", gueltig)

private fun JSONObject.zuKartenCacheDaten(): KartenCacheDaten {
    val knotenJson = optJSONObject("knoten") ?: return KartenCacheDaten(version = optInt("version", 1))
    val eintraege = knotenJson.keys().asSequence().associateWith { knotenId ->
        knotenJson.getJSONObject(knotenId).zuKnotenCacheEintrag(knotenId)
    }
    return KartenCacheDaten(
        version = optInt("version", 1),
        knoten = eintraege,
    )
}

private fun JSONObject.zuKnotenCacheEintrag(fallbackKnotenId: String): KnotenCacheEintrag {
    val datenJson = optJSONObject("daten")
    val daten = datenJson?.keys()?.asSequence()?.associateWith { key -> datenJson.optString(key) }.orEmpty()
    return KnotenCacheEintrag(
        knotenId = optString("knotenId", fallbackKnotenId),
        signatur = optString("signatur"),
        daten = daten,
        fehler = optString("fehler").takeIf { it.isNotBlank() && it != "null" },
        gueltig = optBoolean("gueltig", true),
    )
}

private fun JSONArray?.orEmptyObjects(): List<JSONObject> = this?.asObjects().orEmpty()

private fun JSONArray.asObjects(): List<JSONObject> = List(length()) { index -> getJSONObject(index) }

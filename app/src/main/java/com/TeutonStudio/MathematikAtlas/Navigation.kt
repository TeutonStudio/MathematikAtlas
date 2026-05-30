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
    onKnotenDataAendern: (String, String, Any) -> Unit,
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
            Spacer(Modifier.height(8.dp))
            MathematikInspector(
                knoten = einzelnerKnoten,
                karten = karte,
                onDataAendern = { key, wert -> onKnotenDataAendern(einzelnerKnoten.id, key, wert) },
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
private fun MathematikInspector(
    knoten: KnotenDaten,
    karten: KarteDaten,
    onDataAendern: (String, Any) -> Unit,
) {
    fun stringWert(key: String): String = knoten.data[key]?.toString().orEmpty()

    when (knoten.art) {
        MathematikEingabeKnoten.KNOTEN_ART -> {
            BeschriftetesFeld("Wert", stringWert("wert")) { onDataAendern("wert", it) }
            ZahlenraumAuswahl(knoten.zahlenTyp().raum) { onDataAendern("zahlenTyp", knoten.zahlenTyp().copy(raum = it)) }
        }
        UnbekannteKnoten.KNOTEN_ART -> {
            BeschriftetesFeld("Variable", stringWert("variable").ifBlank { knoten.name }) { onDataAendern("variable", it) }
            ZahlenraumAuswahl(knoten.zahlenTyp().raum) { onDataAendern("zahlenTyp", knoten.zahlenTyp().copy(raum = it)) }
        }
        RechenKnoten.KNOTEN_ART -> {
            BasicText("Operator", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B7280)))
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("+", "-", "*", "/", "^").forEach { op ->
                    TestKnopf(text = op, onClick = { onDataAendern("operator", op) }, modifier = Modifier.weight(1f))
                }
            }
        }
        FormelKnoten.KNOTEN_ART -> {
            BeschriftetesFeld("Formel", stringWert("formel")) { onDataAendern("formel", it) }
        }
        AuswertungsKnoten.KNOTEN_ART -> {
            BasicText(
                text = "Status: ${stringWert("status").ifBlank { "nicht ausgewertet" }}",
                style = TextStyle(fontSize = 12.sp, color = Color(0xFF6B7280)),
            )
        }
        FunktionKnoten.KNOTEN_ART -> {
            BeschriftetesFeld("Referenz-Karten-ID", stringWert("kartenId")) { onDataAendern("kartenId", it) }
            val funktionName = stringWert("funktion").ifBlank { "Funktion" }
            BasicText(
                text = "$funktionName (${karten.knoten.count { it.art == UnbekannteKnoten.KNOTEN_ART }} lokale Unbekannte)",
                style = TextStyle(fontSize = 12.sp, color = Color(0xFF6B7280)),
            )
        }
        LOESEN_KNOTEN_ART -> {
            BeschriftetesFeld("Karten-IDs", stringWert("kartenIds")) { onDataAendern("kartenIds", it) }
            BeschriftetesFeld("Argumente", stringWert("argumente").ifBlank { "a,c,a" }) { onDataAendern("argumente", it) }
        }
    }

    val cache = karten.cache.eintrag(knoten.id)
    if (cache != null) {
        Spacer(Modifier.height(6.dp))
        BasicText(
            text = if (cache.gueltig) "Cache: ${cache.daten.values.firstOrNull().orEmpty()}" else "Cache-Fehler: ${cache.fehler}",
            style = TextStyle(fontSize = 12.sp, color = if (cache.gueltig) Color(0xFF047857) else Color(0xFFB91C1C)),
        )
    }
}

@Composable
private fun BeschriftetesFeld(
    label: String,
    wert: String,
    onWertAendern: (String) -> Unit,
) {
    BasicText(
        text = label,
        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B7280)),
    )
    Spacer(Modifier.height(4.dp))
    EingabeFeld(wert = wert, onWertAendern = onWertAendern)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun ZahlenraumAuswahl(
    aktuellerRaum: Zahlenraum,
    onRaumAendern: (Zahlenraum) -> Unit,
) {
    BasicText(
        text = "Zahlenraum: ${aktuellerRaum.kurzform}",
        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B7280)),
    )
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            "N" to Zahlenraum.Natuerlich,
            "Z" to Zahlenraum.Ganz,
            "Q" to Zahlenraum.Rational,
            "R" to Zahlenraum.Reell,
            "C" to Zahlenraum.Komplex,
        ).forEach { (label, raum) ->
            TestKnopf(text = label, onClick = { onRaumAendern(raum) }, modifier = Modifier.weight(1f))
        }
    }
    Spacer(Modifier.height(6.dp))
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

private const val PERSISTENZ_VERSION = 1
private const val LOESEN_KNOTEN_ART = "loesen"

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

    fun alleKarten(): Map<String, KarteDaten> = liste().mapNotNull { eintrag ->
        lade(eintrag.datei)?.let { karte -> karte.id to karte }
    }.toMap()

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

    fun loesche(kartenId: String) {
        File(ordner, "$kartenId.json").takeIf { it.exists() }?.delete()
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
                id = "eingabe-4",
                name = "4",
                position = Offset(60f, 80f),
                art = MathematikEingabeKnoten.KNOTEN_ART,
                data = matheDaten(
                    MathematikEingabeKnoten.KNOTEN_ART,
                    "4",
                    mapOf("wert" to "4", "zahlenTyp" to ZahlenTyp(Zahlenraum.Natuerlich, wert = "4")),
                ),
            ),
            KnotenDaten(
                id = "unbekannte-x",
                name = "x",
                position = Offset(60f, 260f),
                art = UnbekannteKnoten.KNOTEN_ART,
                data = matheDaten(
                    UnbekannteKnoten.KNOTEN_ART,
                    "x",
                    mapOf("variable" to "x", "zahlenTyp" to ZahlenTyp(Zahlenraum.Ganz, anzeigename = "x")),
                ),
            ),
            KnotenDaten(
                id = "addition",
                name = "Addition",
                position = Offset(360f, 160f),
                art = RechenKnoten.KNOTEN_ART,
                data = matheDaten(RechenKnoten.KNOTEN_ART, "Addition", mapOf("operator" to "+", "zahlenTyp" to ZahlenTyp(Zahlenraum.Reell))),
            ),
            KnotenDaten(
                id = "formel",
                name = "Formel",
                position = Offset(660f, 160f),
                art = FormelKnoten.KNOTEN_ART,
                data = matheDaten(FormelKnoten.KNOTEN_ART, "Formel", mapOf("formel" to "4 + x", "zahlenTyp" to ZahlenTyp(Zahlenraum.Reell))),
            ),
            KnotenDaten(
                id = "auswertung",
                name = "Auswertung",
                position = Offset(960f, 160f),
                art = AuswertungsKnoten.KNOTEN_ART,
                data = matheDaten(AuswertungsKnoten.KNOTEN_ART, "Auswertung"),
            ),
        ),
        verbindungen = listOf(
            VerbindungDaten(
                id = "v-eingabe-addition",
                quellKnotenId = "eingabe-4",
                quellAnschlussId = "wert",
                zielKnotenId = "addition",
                zielAnschlussId = "links",
                zahlenTyp = ZahlenTyp(Zahlenraum.Natuerlich, wert = "4"),
            ),
            VerbindungDaten(
                id = "v-x-addition",
                quellKnotenId = "unbekannte-x",
                quellAnschlussId = "variable",
                zielKnotenId = "addition",
                zielAnschlussId = "rechts",
                zahlenTyp = ZahlenTyp(Zahlenraum.Ganz, anzeigename = "x"),
            ),
            VerbindungDaten(
                id = "v-addition-formel",
                quellKnotenId = "addition",
                quellAnschlussId = "ergebnis",
                zielKnotenId = "formel",
                zielAnschlussId = "in",
                zahlenTyp = ZahlenTyp(Zahlenraum.Reell),
            ),
            VerbindungDaten(
                id = "v-formel-auswertung",
                quellKnotenId = "formel",
                quellAnschlussId = "formel",
                zielKnotenId = "auswertung",
                zielAnschlussId = "in",
                zahlenTyp = ZahlenTyp(Zahlenraum.Reell),
            ),
        ),
    )
}

private fun funktionsBeispielKarte(name: String): KarteDaten {
    val referenz = beispielKarte("Referenzierte Funktion")
    return KarteDaten(
        id = UUID.randomUUID().toString(),
        name = name,
        initialKnoten = referenz.knoten,
        initialVerbindungen = referenz.verbindungen,
        knoten = listOf(
            KnotenDaten(
                id = "argument",
                name = "Argument",
                position = Offset(80f, 140f),
                art = MathematikEingabeKnoten.KNOTEN_ART,
                data = matheDaten(MathematikEingabeKnoten.KNOTEN_ART, "Argument", mapOf("wert" to "2", "zahlenTyp" to ZahlenTyp(Zahlenraum.Reell, wert = "2"))),
            ),
            KnotenDaten(
                id = "funktion",
                name = "f",
                position = Offset(360f, 260f),
                art = FunktionKnoten.KNOTEN_ART,
                data = matheDaten(
                    FunktionKnoten.KNOTEN_ART,
                    "f",
                    mapOf(
                        "kartenId" to referenz.id,
                        "funktion" to "f",
                        "zahlenTyp" to ZahlenTyp(Zahlenraum.Funktion(listOf(Zahlenraum.Reell), Zahlenraum.Reell), ausdruck = "f"),
                    ),
                ),
            ),
            KnotenDaten(
                id = "loesen",
                name = "Loesen",
                position = Offset(660f, 260f),
                art = LOESEN_KNOTEN_ART,
                data = matheDaten(LOESEN_KNOTEN_ART, "Loesen", mapOf("kartenIds" to referenz.id, "argumente" to "a,c,a")),
            ),
        ),
        verbindungen = listOf(
            VerbindungDaten(
                id = "v-argument-funktion",
                quellKnotenId = "argument",
                quellAnschlussId = "wert",
                zielKnotenId = "funktion",
                zielAnschlussId = "argument",
                zahlenTyp = ZahlenTyp(Zahlenraum.Reell, wert = "2"),
            ),
            VerbindungDaten(
                id = "v-funktion-loesen",
                quellKnotenId = "funktion",
                quellAnschlussId = "wert",
                zielKnotenId = "loesen",
                zielAnschlussId = "in",
                zahlenTyp = ZahlenTyp(Zahlenraum.Reell),
            ),
        ),
    )
}

private fun KarteDaten.mitNeuemKnoten(position: Offset? = null, art: String = MathematikEingabeKnoten.KNOTEN_ART): KarteDaten {
    return copy(knoten = knoten + neuerKnoten(position, art))
}

private fun KarteDaten.neuerKnoten(position: Offset? = null, art: String = MathematikEingabeKnoten.KNOTEN_ART): KnotenDaten {
    val nummer = knoten.size + 1
    val id = "knoten-$nummer-${UUID.randomUUID()}"
    val name = when (art) {
        MathematikEingabeKnoten.KNOTEN_ART -> "Eingabe $nummer"
        UnbekannteKnoten.KNOTEN_ART -> "x$nummer"
        RechenKnoten.KNOTEN_ART -> "Rechnung $nummer"
        FormelKnoten.KNOTEN_ART -> "Formel $nummer"
        AuswertungsKnoten.KNOTEN_ART -> "Auswertung $nummer"
        FunktionKnoten.KNOTEN_ART -> "Funktion $nummer"
        LOESEN_KNOTEN_ART -> "Loesen $nummer"
        else -> "Knoten $nummer"
    }
    return KnotenDaten(
        id = id,
        name = name,
        position = position ?: Offset(
            x = 90f + nummer * 40f,
            y = 120f + nummer * 30f,
        ),
        fläche = Offset(180f, 96f),
        art = art,
        data = matheDaten(art, name),
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

private fun KarteDaten.duplizierteKarte(): KarteDaten {
    val neueId = UUID.randomUUID().toString()
    return copy(
        id = neueId,
        name = "$name Kopie",
        knoten = knoten.map { it.copy(id = "${it.id}-$neueId") },
        verbindungen = verbindungen.map {
            it.copy(
                id = "${it.id}-$neueId",
                quellKnotenId = "${it.quellKnotenId}-$neueId",
                zielKnotenId = "${it.zielKnotenId}-$neueId",
            )
        },
    )
}

private fun Map<String, Any>.mitAktualisierterKurzform(art: String, name: String): Map<String, Any> =
    matheDaten(art, name, this)

private fun matheDaten(
    art: String,
    name: String,
    daten: Map<String, Any> = emptyMap(),
): Map<String, Any> {
    val typ = daten["zahlenTyp"] as? ZahlenTyp ?: ZahlenTyp(Zahlenraum.Reell)
    val kurzform = when (art) {
        MathematikEingabeKnoten.KNOTEN_ART -> typ.copy(wert = daten["wert"]?.toString()?.ifBlank { null }).kurzform
        UnbekannteKnoten.KNOTEN_ART -> typ.copy(anzeigename = daten["variable"]?.toString()?.ifBlank { null } ?: name).kurzform
        RechenKnoten.KNOTEN_ART -> daten["operator"]?.toString()?.ifBlank { null } ?: "+"
        FormelKnoten.KNOTEN_ART -> daten["formel"]?.toString()?.ifBlank { null } ?: name
        AuswertungsKnoten.KNOTEN_ART -> daten["status"]?.toString()?.ifBlank { null } ?: "Auswertung"
        FunktionKnoten.KNOTEN_ART -> typ.kurzform
        LOESEN_KNOTEN_ART -> "Loesen: ${daten["argumente"]?.toString()?.ifBlank { "a,c,a" } ?: "a,c,a"}"
        else -> typ.kurzform
    }
    return daten + ("kurzform" to kurzform)
}

private fun KnotenDaten.zahlenTyp(): ZahlenTyp =
    data["zahlenTyp"] as? ZahlenTyp ?: ZahlenTyp(Zahlenraum.Reell)

private data class MatheWert(
    val ausdruck: String,
    val wert: Double?,
    val typ: ZahlenTyp,
    val fehler: String? = null,
)

private fun KarteDaten.werteMathematikAus(karten: Map<String, KarteDaten>): KarteDaten {
    val werte = mutableMapOf<String, MatheWert>()
    val eingehend = verbindungen.groupBy { it.zielKnotenId }
    val ausgehend = verbindungen.groupBy { it.quellKnotenId }
    val grad = knoten.associate { it.id to eingehend[it.id].orEmpty().size }.toMutableMap()
    val nachId = knoten.associateBy { it.id }
    val reihenfolge = mutableListOf<KnotenDaten>()
    val offen = ArrayDeque(knoten.filter { grad.getValue(it.id) == 0 }.map { it.id })

    while (offen.isNotEmpty()) {
        val id = offen.removeFirst()
        val aktueller = nachId[id] ?: continue
        reihenfolge += aktueller
        ausgehend[id].orEmpty().forEach { verbindung ->
            grad[verbindung.zielKnotenId] = grad.getValue(verbindung.zielKnotenId) - 1
            if (grad.getValue(verbindung.zielKnotenId) == 0) offen.addLast(verbindung.zielKnotenId)
        }
    }
    val zyklen = knoten.filterNot { kandidat -> reihenfolge.any { it.id == kandidat.id } }
    reihenfolge += zyklen

    val neueKnoten = reihenfolge.map { knoten ->
        val inputs = eingehend[knoten.id].orEmpty().mapNotNull { werte[it.quellKnotenId] }
        val wert = knoten.berechneMatheWert(inputs, karten, zyklen.any { it.id == knoten.id })
        werte[knoten.id] = wert
        val status = wert.fehler ?: wert.ausdruck
        knoten.copy(data = (knoten.data + mapOf("status" to status, "kurzform" to status, "zahlenTyp" to wert.typ)))
    }
    return copy(knoten = knoten.map { alt -> neueKnoten.firstOrNull { it.id == alt.id } ?: alt })
}

private fun KnotenDaten.berechneMatheWert(
    inputs: List<MatheWert>,
    karten: Map<String, KarteDaten>,
    istZyklus: Boolean,
): MatheWert {
    val typ = zahlenTyp()
    if (istZyklus) return MatheWert(name, null, typ, "zyklische Abhaengigkeit")
    val inputFehler = inputs.firstOrNull { it.fehler != null }?.fehler
    if (inputFehler != null) return MatheWert(name, null, typ, inputFehler)

    return when (art) {
        MathematikEingabeKnoten.KNOTEN_ART -> {
            val wertText = data["wert"]?.toString().orEmpty()
            MatheWert(wertText.ifBlank { name }, wertText.toDoubleOrNull(), typ.copy(wert = wertText.ifBlank { null }))
        }
        UnbekannteKnoten.KNOTEN_ART -> {
            val variable = data["variable"]?.toString()?.ifBlank { null } ?: name
            MatheWert(variable, null, typ.copy(anzeigename = variable))
        }
        RechenKnoten.KNOTEN_ART -> berechneOperator(inputs, data["operator"]?.toString() ?: "+", typ)
        FormelKnoten.KNOTEN_ART -> {
            val formel = data["formel"]?.toString()?.ifBlank { null } ?: inputs.joinToString(" ") { it.ausdruck }
            MatheWert(formel, inputs.firstOrNull()?.wert, typ.copy(ausdruck = formel))
        }
        AuswertungsKnoten.KNOTEN_ART -> inputs.firstOrNull()
            ?: MatheWert(name, null, typ, "fehlende Eingabe")
        FunktionKnoten.KNOTEN_ART -> {
            val referenz = data["kartenId"]?.toString()?.let { karten[it] }
            if (referenz == null) {
                MatheWert(name, null, typ, "referenzierte Karte fehlt")
            } else {
                val ausgabe = referenz.werteMathematikAus(karten).knoten.lastOrNull { it.art == FormelKnoten.KNOTEN_ART }
                val ausdruck = "${data["funktion"]?.toString()?.ifBlank { null } ?: referenz.name}(${inputs.joinToString(", ") { it.ausdruck }})"
                MatheWert(ausgabe?.data?.get("status")?.toString() ?: ausdruck, null, typ)
            }
        }
        LOESEN_KNOTEN_ART -> {
            val gleichung = inputs.joinToString(" = ") { it.ausdruck }.ifBlank { "Gleichungssystem" }
            MatheWert(gleichung, null, typ, if (inputs.size < 2) "mindestens zwei Kartenausdruecke noetig" else null)
        }
        else -> inputs.firstOrNull() ?: MatheWert(name, null, typ)
    }
}

private fun berechneOperator(inputs: List<MatheWert>, operator: String, typ: ZahlenTyp): MatheWert {
    if (inputs.size < 2) return MatheWert(operator, null, typ, "fehlende Eingabe")
    val links = inputs[0]
    val rechts = inputs[1]
    if (!links.typ.istKompatibelMit(typ) || !rechts.typ.istKompatibelMit(typ)) {
        return MatheWert("$operator", null, typ, "inkompatibler Zahlenraum")
    }
    val ausdruck = "(${links.ausdruck} $operator ${rechts.ausdruck})"
    val wert = links.wert?.let { l ->
        rechts.wert?.let { r ->
            when (operator) {
                "+" -> l + r
                "-" -> l - r
                "*" -> l * r
                "/" -> if (r == 0.0) null else l / r
                "^" -> Math.pow(l, r)
                else -> null
            }
        }
    }
    return MatheWert(wert?.kompakt() ?: ausdruck, wert, typ.copy(ausdruck = ausdruck))
}

private fun Double.kompakt(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString()

private fun KarteDaten.zuJson(): JSONObject = JSONObject()
    .put("version", PERSISTENZ_VERSION)
    .put("id", id)
    .put("name", name)
    .put("ansichtsfenster", ansichtsfenster.zuJson())
    .put("knoten", JSONArray(knoten.map { it.zuJson() }))
    .put("verbindungen", JSONArray(verbindungen.map { it.zuJson() }))
    .put("auswahl", JSONObject())
    .put("cache", cache.zuJson())

private fun KnotenDaten.zuJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("position", JSONObject().put("x", position.x).put("y", position.y))
    .put("flaeche", JSONObject().put("x", fläche.x).put("y", fläche.y))
    .put("knotenArt", art)
    .put("typ", art)
    .put("beweglich", beweglich)
    .put("data", data.zuJsonData())

private fun VerbindungDaten.zuJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("quellKnotenId", quellKnotenId)
    .put("quellAnschlussId", quellAnschlussId)
    .put("zielKnotenId", zielKnotenId)
    .put("zielAnschlussId", zielAnschlussId)
    .put("label", label)
    .put("typ", art)
    .put("zahlenTyp", zahlenTyp?.zuJson())
    .put("fehler", fehler)

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
        data = optJSONObject("data")?.zuDataMap().orEmpty(),
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
    zahlenTyp = optJSONObject("zahlenTyp")?.zuZahlenTyp(),
    fehler = optString("fehler").takeIf { it.isNotBlank() && it != "null" },
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

private fun Map<String, Any>.zuJsonData(): JSONObject = JSONObject().also { ziel ->
    forEach { (key, value) ->
        ziel.put(
            key,
            when (value) {
                is ZahlenTyp -> value.zuJson()
                is Zahlenraum -> value.zuJson()
                else -> value
            },
        )
    }
}

private fun JSONObject.zuDataMap(): Map<String, Any> = keys().asSequence().associateWith { key ->
    val value = get(key)
    if (key == "zahlenTyp" && value is JSONObject) value.zuZahlenTyp() else value
}

private fun ZahlenTyp.zuJson(): JSONObject = JSONObject()
    .put("raum", raum.zuJson())
    .put("wert", wert)
    .put("anzeigename", anzeigename)
    .put("ausdruck", ausdruck)

private fun JSONObject.zuZahlenTyp(): ZahlenTyp = ZahlenTyp(
    raum = optJSONObject("raum")?.zuZahlenraum() ?: Zahlenraum.Reell,
    wert = optString("wert").takeIf { it.isNotBlank() && it != "null" },
    anzeigename = optString("anzeigename").takeIf { it.isNotBlank() && it != "null" },
    ausdruck = optString("ausdruck").takeIf { it.isNotBlank() && it != "null" },
)

private fun Zahlenraum.zuJson(): JSONObject = when (this) {
    Zahlenraum.Natuerlich -> JSONObject().put("art", "N")
    Zahlenraum.Ganz -> JSONObject().put("art", "Z")
    Zahlenraum.Rational -> JSONObject().put("art", "Q")
    Zahlenraum.Reell -> JSONObject().put("art", "R")
    Zahlenraum.Komplex -> JSONObject().put("art", "C")
    is Zahlenraum.Eingeschraenkt -> JSONObject().put("art", "eingeschraenkt").put("basis", basis.zuJson()).put("bedingung", bedingung)
    is Zahlenraum.Produkt -> JSONObject().put("art", "produkt").put("raeume", JSONArray(raeume.map { it.zuJson() }))
    is Zahlenraum.Funktion -> JSONObject().put("art", "funktion").put("eingaben", JSONArray(eingaben.map { it.zuJson() })).put("ausgabe", ausgabe.zuJson())
}

private fun JSONObject.zuZahlenraum(): Zahlenraum = when (optString("art", "R")) {
    "N" -> Zahlenraum.Natuerlich
    "Z" -> Zahlenraum.Ganz
    "Q" -> Zahlenraum.Rational
    "C" -> Zahlenraum.Komplex
    "eingeschraenkt" -> Zahlenraum.Eingeschraenkt(optJSONObject("basis")?.zuZahlenraum() ?: Zahlenraum.Reell, optString("bedingung"))
    "produkt" -> Zahlenraum.Produkt(optJSONArray("raeume").orEmptyObjects().map { it.zuZahlenraum() })
    "funktion" -> Zahlenraum.Funktion(
        eingaben = optJSONArray("eingaben").orEmptyObjects().map { it.zuZahlenraum() },
        ausgabe = optJSONObject("ausgabe")?.zuZahlenraum() ?: Zahlenraum.Reell,
    )
    else -> Zahlenraum.Reell
}

private fun JSONArray?.orEmptyObjects(): List<JSONObject> = this?.asObjects().orEmpty()

private fun JSONArray.asObjects(): List<JSONObject> = List(length()) { index -> getJSONObject(index) }

package com.TeutonStudio.MathematikAtlas

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.mitErsetztemEingang
import com.TeutonStudio.KnotenKartenVerwalter.hatAnschlussId
import com.TeutonStudio.KnotenKartenVerwalter.hatKnotenId
import com.TeutonStudio.KnotenKartenVerwalter.idReferenz
import com.TeutonStudio.KnotenKartenVerwalter.istVerbunden
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.AusgabeKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisAusgang
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisEingang
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisKarte
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.EingabeKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.KartenKontextAktion
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.KartenTreffer

sealed class Screen(val route: String) {
    data object StartScreen : Screen("start_screen")
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
    var karte by remember { mutableStateOf(testKarte()) }
    var auswahl by remember { mutableStateOf(AuswahlDaten()) }
    var status by remember { mutableStateOf("Bereit") }

    fun verschiebeKnoten(knotenId: String, position: Offset) {
        karte = KarteDaten(
            karte,
            knoten = karte.knoten.map { knoten ->
                if (knoten.id == knotenId) KnotenDaten(knoten, position = position) else knoten
            },
        )
        status = "Knoten verschoben"
    }

    fun erstelleVerbindung(verbindung: VerbindungDaten) {
        karte = KarteDaten(
            karte,
            verbindungen = karte.verbindungen.mitErsetztemEingang(verbindung),
        )
        auswahl = AuswahlDaten(verbindungIds = setOf(verbindung.id))
        status = "Verbindung erstellt"
    }

    fun loescheAuswahl() {
        val knotenIds = auswahl.knotenIds
        val verbindungIds = auswahl.verbindungIds

        val hatKnoten = { v: VerbindungDaten -> true in knotenIds.map { v.ids.hatKnotenId(it) } }
        val hatVerbindung = { v: VerbindungDaten -> true in knotenIds.map { v.ids.hatAnschlussId(it) } }
        karte = KarteDaten(
            karte,
            knoten = karte.knoten.filterNot { it.id in knotenIds },
            verbindungen = karte.verbindungen.filterNot { it.id in verbindungIds || hatKnoten(it) || hatVerbindung(it) },
        )
        auswahl = AuswahlDaten()
        status = "Auswahl geloescht"
    }

    fun fuehreKontextAktionAus(aktion: KartenKontextAktion) {
        when (aktion.aktion) {
            "Knoten erstellen" -> {
                val nummer = karte.knoten.size + 1
                val knoten = KnotenDaten(
                    id = "knoten-$nummer",
                    name = "Knoten $nummer",
                    position = aktion.weltPosition,
                    klasse = BasisKnoten.KNOTEN_ART,
                )
                karte = KarteDaten(
                    karte,
                    knoten = karte.knoten + knoten,
                )
                auswahl = AuswahlDaten(knotenIds = setOf(knoten.id))
                status = "Knoten erstellt"
            }

            "Knoten loeschen" -> {
                val ziel = aktion.ziel as? KartenTreffer.Knoten
                if (ziel != null) {
                    auswahl = AuswahlDaten(knotenIds = setOf(ziel.knotenId))
                    loescheAuswahl()
                }
            }

            "Verbindung loeschen" -> {
                val ziel = aktion.ziel as? KartenTreffer.Verbindung
                if (ziel != null) {
                    karte = KarteDaten(
                        karte,
                        verbindungen = karte.verbindungen.filterNot { it.id == ziel.verbindungId },
                    )
                    auswahl = AuswahlDaten()
                    status = "Verbindung geloescht"
                }
            }

            "Knoten auswaehlen" -> {
                val ziel = aktion.ziel as? KartenTreffer.Knoten
                if (ziel != null) {
                    auswahl = AuswahlDaten(knotenIds = setOf(ziel.knotenId))
                    status = "Knoten ausgewaehlt"
                }
            }

            "Verbindung auswaehlen" -> {
                val ziel = aktion.ziel as? KartenTreffer.Verbindung
                if (ziel != null) {
                    auswahl = AuswahlDaten(verbindungIds = setOf(ziel.verbindungId))
                    status = "Verbindung ausgewaehlt"
                }
            }

            else -> {
                status = "Kontext: ${aktion.aktion}"
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
            .padding(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        TestSeitenLeiste(
            karte = karte,
            auswahl = auswahl,
            status = status,
            onNeueKarte = {
                karte = testKarte()
                auswahl = AuswahlDaten()
                status = "Testkarte geladen"
            },
            onAuswahlLoeschen = ::loescheAuswahl,
            onAuswahlLeeren = {
                auswahl = AuswahlDaten()
                status = "Keine Auswahl"
            },
            onNameAendern = { neuerName ->
                karte = KarteDaten(karte, name = neuerName)
                status = "Karte umbenannt"
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
            Graph(
                daten = karte,
                zustand = KarteZustand(
                    zeigeÜbersicht = true,
                    zeigeKontrollLeiste = true,
                    auswahl = auswahl,
                ),
                aktualisierung = ::verschiebeKnoten,
                onVerbindungErstellen = ::erstelleVerbindung,
                onKontextAktion = ::fuehreKontextAktionAus,
                onAuswahlÄndern = { neueAuswahl ->
                    auswahl = neueAuswahl
                    status = neueAuswahl.statusText()
                },
            ).zuComposable(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun TestSeitenLeiste(
    karte: KarteDaten,
    auswahl: AuswahlDaten,
    status: String,
    onNeueKarte: () -> Unit,
    onAuswahlLoeschen: () -> Unit,
    onAuswahlLeeren: () -> Unit,
    onNameAendern: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(8.dp))
            .padding(12.dp),
    ) {
        BasicText(
            text = "Graph Test",
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
            ),
        )

        Spacer(Modifier.height(12.dp))

        BasicText(
            text = "Name",
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6B7280),
            ),
        )

        Spacer(Modifier.height(4.dp))

        BasicTextField(
            value = karte.name,
            onValueChange = onNameAendern,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            textStyle = TextStyle(fontSize = 15.sp, color = Color(0xFF111827)),
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TestKnopf(
                text = "Neu",
                onClick = onNeueKarte,
                modifier = Modifier.weight(1f),
            )
            TestKnopf(
                text = "Leeren",
                onClick = onAuswahlLeeren,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(8.dp))

        TestKnopf(
            text = "Auswahl loeschen",
            onClick = onAuswahlLoeschen,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        BasicText(
            text = "Auswahl",
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF374151),
            ),
        )

        Spacer(Modifier.height(6.dp))

        BasicText(
            text = auswahl.statusText(),
            style = TextStyle(fontSize = 13.sp, color = Color(0xFF4B5563)),
        )

        Spacer(Modifier.height(16.dp))

        BasicText(
            text = "Status",
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF374151),
            ),
        )

        Spacer(Modifier.height(6.dp))

        BasicText(
            text = status,
            style = TextStyle(fontSize = 13.sp, color = Color(0xFF4B5563)),
        )

        Spacer(Modifier.height(16.dp))

        BasicText(
            text = "Bedienung: Knoten ziehen, Anschluss ziehen, Rechtsklick/Langdruck fuer Kontext.",
            style = TextStyle(fontSize = 12.sp, color = Color(0xFF6B7280)),
        )
    }
}

@Composable
private fun TestKnopf(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(Color(0xFF111827), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            ),
        )
    }
}

private fun AuswahlDaten.statusText(): String = when {
    istLeer -> "Keine Auswahl"
    knotenIds.size == 1 && verbindungIds.isEmpty() -> "Knoten ausgewaehlt"
    verbindungIds.size == 1 && knotenIds.isEmpty() -> "Verbindung ausgewaehlt"
    else -> "${knotenIds.size} Knoten, ${verbindungIds.size} Verbindungen ausgewaehlt"
}

private fun testKarte(): KarteDaten = KarteDaten(
    id = "test-karte",
    klasse = BasisKarte.KARTEN_ART,
    name = "Graph Testkarte",
    knoten = listOf(
        KnotenDaten(
            id = "eingabe",
            name = "Eingabe",
            position = Offset(80f, 120f),
            klasse = EingabeKnoten.KNOTEN_ART,
        ),
        KnotenDaten(
            id = "mitte",
            name = "Mitte",
            position = Offset(360f, 170f),
            klasse = BasisKnoten.KNOTEN_ART,
        ),
        KnotenDaten(
            id = "ausgabe",
            name = "Ausgabe",
            position = Offset(660f, 120f),
            klasse = AusgabeKnoten.KNOTEN_ART,
        ),
    ),
    verbindungen = listOf(
        VerbindungDaten(
            id = "v-eingabe-mitte",
            ids = idReferenz("eingabe" to "mitte","out" to "in"),
            klasse = BasisEingang.ANSCHLUSS_ART,
        ),
        VerbindungDaten(
            id = "v-mitte-ausgabe",
            ids = idReferenz("mitte" to "ausgabe","out" to "in"),
            klasse = BasisAusgang.ANSCHLUSS_ART,
        ),
    ),
)
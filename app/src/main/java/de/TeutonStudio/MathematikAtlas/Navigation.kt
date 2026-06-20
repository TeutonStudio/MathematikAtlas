package de.TeutonStudio.MathematikAtlas

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
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKarte
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Auswahl
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Zustand
import de.TeutonStudio.MathematikAtlas.karten.MatheKartenFabrik

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
//    var karte by remember { mutableStateOf(testKarte()) } // StandardKarteTest
    var karte by remember { mutableStateOf(aussageTestKarte()) } // AussageKarteTest
    var status by remember { mutableStateOf("Bereit") }
    val auswahl = remember { mutableStateOf(Auswahl()) }

    fun veränderung(knotenId: String, position: Offset) {
        status = "Knoten verschoben"
    }

    fun verbindete(a1: GraphDatenObjektAnschluss<*>, a2: GraphDatenObjektAnschluss<*>) {
        karte.verbindungen.find { it.ids.enthält(a1.daten) && it.ids.enthält(a2.daten) }?.let {
            auswahl.value = Auswahl().apply { wähleVerbindung(it.id) }
        }
        status = "Verbindung erstellt"
    }

    fun loescheAuswahl() {
        auswahl.value = Auswahl()
        status = "Auswahl gelöscht"
    }

    fun fuehreKontextAktionAus(aktion: Any) { // TODO
        /*when (aktion.aktion) {
            "Knoten erstellen" -> {
                val nummer = karte.knoten.size + 1
                val knoten = KnotenDaten(
                    id = "knoten-$nummer",
                    name = "Knoten $nummer",
                    position = aktion.weltPosition,
                    anschlüsse = mutableStateMapOf(

                    )
                )
                auswahl.value = MultiAuswahl(knotenIds = setOf(knoten.id))
                status = "Knoten erstellt"
            }

            "Knoten loeschen" -> {
                val ziel = aktion.ziel as? KartenTreffer.Knoten
                if (ziel != null) {
                    auswahl.value = MultiAuswahl(knotenIds = setOf(ziel.knotenId))
                    loescheAuswahl()
                }
            }

            "Verbindung loeschen" -> {
                val ziel = aktion.ziel as? KartenTreffer.Verbindung
                if (ziel != null) {
                    auswahl.value = AuswahlDaten.LEER
                    status = "Verbindung geloescht"
                }
            }

            "Knoten auswaehlen" -> {
                val ziel = aktion.ziel as? KartenTreffer.Knoten
                if (ziel != null) {
                    auswahl.value = MultiAuswahl(knotenIds = setOf(ziel.knotenId))
                    status = "Knoten ausgewaehlt"
                }
            }

            "Verbindung auswaehlen" -> {
                val ziel = aktion.ziel as? KartenTreffer.Verbindung
                if (ziel != null) {
                    auswahl.value = MultiAuswahl(verbindungIds = setOf(ziel.verbindungId))
                    status = "Verbindung ausgewaehlt"
                }
            }

            else -> {
                status = "Kontext: ${aktion.aktion}"
            }
        }*/
    }

    Row(modifier = Modifier.fillMaxSize().background(Color(0xFFF3F4F6)).padding(16.dp,32.dp,16.dp,16.dp)) {
        val g = remember {
            Graph(
                daten = karte,
                zustand = Zustand(
//                    zeigeÜbersicht = true,
//                    zeigeKontrollLeiste = true,
//                    auswahl = auswahl,
//                    rasterArt = GraphHintergrund.RasterArt.Punkte,
//                    rasterTesselation = GraphHintergrund.RasterTesselation.Trigon
                ),
                veränderung = ::veränderung,
                verbindete = ::verbindete,
//                onKontextAktion = ::fuehreKontextAktionAus,
                wählte = { neueAuswahl ->
                    auswahl.value = neueAuswahl
                    status = neueAuswahl.statusText()
                },
                kartenFabrik = MatheKartenFabrik
            )
        }
        TestSeitenLeiste(
            karte = karte,
            auswahl = auswahl.value,
            status = status,
            zustand = g.karte.zustand,
            onNeueKarte = {
//                karte = testKarte()
                karte = aussageTestKarte()
                auswahl.value = Auswahl()
                status = "Testkarte geladen"
            },
            onAuswahlLoeschen = ::loescheAuswahl,
            onAuswahlLeeren = {
                auswahl.value = Auswahl()
                status = "Keine Auswahl"
            },
            onNameAendern = { neuerName ->
//                karte = KarteDaten(karte, name = neuerName)
                status = "Karte umbenannt"
            },
        )

        Spacer(Modifier.width(12.dp))

        Box(
            modifier = Modifier.fillMaxHeight().weight(1f)
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(8.dp)),
        ) {
            g.Composable(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun TestSeitenLeiste(
    karte: GraphDatenKarte,
    auswahl: Auswahl,
    status: String,
    zustand: Zustand,
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
        BasicText(zustand.erhaltePos().toString())
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

/*private fun Auswahl.statusText(): String = when {
    this is EinzelAuswahl -> statusText()
    this is MultiAuswahl -> statusText()
    else -> "Fehler"
}*/

private fun Auswahl.statusText(): String = when {
    this == Auswahl() -> "Keine Auswahl"
    knotenIds.size == 1 && verbindungIds.isEmpty() -> "Knoten ausgewählt"
    verbindungIds.size == 1 && knotenIds.isEmpty() -> "Verbindung ausgewählt"
    else -> "${knotenIds.size} Knoten, ${verbindungIds.size} Verbindungen ausgewählt"
}

//private fun Auswahl.statusText(): String = "ausgew#hlt: ${auswahlId}"

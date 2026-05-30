package com.TeutonStudio.MathematikAtlas.knoten

import androidx.compose.ui.geometry.Offset
import com.TeutonStudio.KnotenKartenVerwalter.daten.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.ZahlenTyp
import com.TeutonStudio.KnotenKartenVerwalter.daten.Zahlenraum
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.AuswertungsKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.FormelKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.FunktionKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.MathematikEingabeKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.RechenKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.UnbekannteKnoten
import java.util.UUID

const val LOESEN_KNOTEN_ART: String = "loesen"

fun Map<String, Any>.mitAktualisierterKurzform(art: String, name: String): Map<String, Any> =
    matheDaten(art, name, this)

fun matheDaten(
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

fun KnotenDaten.zahlenTyp(): ZahlenTyp =
    data["zahlenTyp"] as? ZahlenTyp ?: ZahlenTyp(Zahlenraum.Reell)

fun KarteDaten.mitNeuemMathematikKnoten(
    position: Offset? = null,
    art: String = MathematikEingabeKnoten.KNOTEN_ART,
): KarteDaten = copy(knoten = knoten + neuerMathematikKnoten(position, art))

fun KarteDaten.neuerMathematikKnoten(
    position: Offset? = null,
    art: String = MathematikEingabeKnoten.KNOTEN_ART,
): KnotenDaten {
    val nummer = knoten.size + 1
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
        id = "knoten-$nummer-${UUID.randomUUID()}",
        name = name,
        position = position ?: Offset(x = 90f + nummer * 40f, y = 120f + nummer * 30f),
        fläche = Offset(180f, 96f),
        art = art,
        data = matheDaten(art, name),
    )
}

fun beispielKarte(name: String): KarteDaten = KarteDaten(
    id = UUID.randomUUID().toString(),
    name = name,
    knoten = listOf(
        KnotenDaten(
            id = "eingabe-4",
            name = "4",
            position = Offset(60f, 80f),
            art = EingabeAtlasKnoten.ART,
            data = EingabeAtlasKnoten.daten("4") + ("zahlenTyp" to ZahlenTyp(Zahlenraum.Natuerlich, wert = "4")),
        ),
        KnotenDaten(
            id = "unbekannte-x",
            name = "x",
            position = Offset(60f, 260f),
            art = UnbekannteAtlasKnoten.ART,
            data = UnbekannteAtlasKnoten.daten("x") + ("zahlenTyp" to ZahlenTyp(Zahlenraum.Ganz, anzeigename = "x")),
        ),
        KnotenDaten(
            id = "addition",
            name = "Addition",
            position = Offset(360f, 160f),
            art = RechnenAtlasKnoten.ART,
            data = RechnenAtlasKnoten.daten("+"),
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
            art = AuswertenAtlasKnoten.ART,
            data = AuswertenAtlasKnoten.daten(),
        ),
    ),
    verbindungen = listOf(
        VerbindungDaten("v-eingabe-addition", "eingabe-4", "wert", "addition", "links", zahlenTyp = ZahlenTyp(Zahlenraum.Natuerlich, wert = "4")),
        VerbindungDaten("v-x-addition", "unbekannte-x", "variable", "addition", "rechts", zahlenTyp = ZahlenTyp(Zahlenraum.Ganz, anzeigename = "x")),
        VerbindungDaten("v-addition-formel", "addition", "ergebnis", "formel", "in", zahlenTyp = ZahlenTyp(Zahlenraum.Reell)),
        VerbindungDaten("v-formel-auswertung", "formel", "formel", "auswertung", "in", zahlenTyp = ZahlenTyp(Zahlenraum.Reell)),
    ),
)

fun funktionsBeispielKarte(name: String): KarteDaten {
    val referenz = beispielKarte("Referenzierte Funktion")
    val id = UUID.randomUUID().toString()
    val eingebetteteReferenzId = "$id:initial"
    return KarteDaten(
        id = id,
        name = name,
        initialKnoten = referenz.knoten,
        initialVerbindungen = referenz.verbindungen,
        knoten = listOf(
            KnotenDaten(
                id = "argument",
                name = "Argument",
                position = Offset(80f, 140f),
                art = EingabeAtlasKnoten.ART,
                data = EingabeAtlasKnoten.daten("2"),
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
                        "kartenId" to eingebetteteReferenzId,
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
                data = matheDaten(LOESEN_KNOTEN_ART, "Loesen", mapOf("kartenIds" to eingebetteteReferenzId, "argumente" to "a,c,a")),
            ),
        ),
        verbindungen = listOf(
            VerbindungDaten("v-argument-funktion", "argument", "wert", "funktion", "argument", zahlenTyp = ZahlenTyp(Zahlenraum.Reell, wert = "2")),
            VerbindungDaten("v-funktion-loesen", "funktion", "wert", "loesen", "in", zahlenTyp = ZahlenTyp(Zahlenraum.Reell)),
        ),
    )
}

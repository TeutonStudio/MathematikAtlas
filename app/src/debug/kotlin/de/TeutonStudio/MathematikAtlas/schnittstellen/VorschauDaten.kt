package de.TeutonStudio.MathematikAtlas.schnittstellen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussVerweis
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenEigenschaft
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import de.TeutonStudio.MathematikAtlas.AtlasZustand
import de.TeutonStudio.MathematikAtlas.KnotenInspektorAktionen
import de.TeutonStudio.MathematikKnoten.ENDLICHE_MENGE_ALT_PARAMETER
import de.TeutonStudio.MathematikKnoten.MATRIXDIAGONALE_ART_PARAMETER
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_ART
import de.TeutonStudio.MathematikKnoten.alleMathematikKnotenVorlagen

internal object VorschauDaten {
    const val LangeBezeichnung =
        "Lineare Abbildung mit außergewöhnlich langer, aber fachlich plausibler Bezeichnung"

    val BeispielFormel = "\\frac{\\sin(x)^2 + 3}{\\sqrt{x^2 + 1}}"

    val Matrixdiagonale = knoten(
        art = "mathematik.matrixdiagonale",
        parameter = mapOf(MATRIXDIAGONALE_ART_PARAMETER to "nebendiagonale"),
    )

    val EndlicheMenge = knoten(
        art = "mathematik.endlicheMenge",
        name = "Primzahlen unter zehn",
        parameter = mapOf(ENDLICHE_MENGE_ALT_PARAMETER to "2,3,5,7"),
    )

    val ZahlenRechner = knoten(
        art = ZAHLENRECHNER_ART,
        name = "Potenz und trigonometrische Auswertung",
        parameter = mapOf(
            "operator" to "potenz",
            "wertebereich" to "R",
        ),
    )

    val GeometrischeKante = knoten(
        art = "mathematik.geometrie.kante",
        name = "Kante eines Würfels",
        parameter = mapOf("index" to "3"),
    )

    val KartenKnoten = knoten(
        art = "mathematik.karte",
        name = "Gauß-Verfahren als Methode",
        parameter = mapOf("zustand" to "methode"),
    )

    val BeispielKarte = KartenDaten(
        id = KartenId("vorschau.karte.lineares-gleichungssystem"),
        name = "Lineares Gleichungssystem mit Gauß-Verfahren",
        version = 4,
        erstelltAm = 1_735_689_600_000L,
        knoten = listOf(Matrixdiagonale, ZahlenRechner, EndlicheMenge),
    )

    fun knoten(
        art: String,
        name: String? = null,
        parameter: Map<String, String> = emptyMap(),
    ): KnotenDaten {
        val vorlage = alleMathematikKnotenVorlagen().firstOrNull { it.art == art }
        val basis = vorlage?.erzeuge(GraphPunkt.Zero) ?: KnotenDaten(
            art = art,
            name = name ?: art.substringAfterLast('.'),
        )
        val stabileId = art.replace(Regex("[^A-Za-z0-9._-]"), "-")
        return basis.copy(
            id = KnotenId("vorschau.$stabileId"),
            name = name ?: basis.name,
            anschlüsse = basis.anschlüsse.mapIndexed { index, anschluss ->
                anschluss.copy(id = AnschlussId("vorschau.$stabileId.anschluss.$index"))
            },
            parameter = basis.parameter + parameter,
        )
    }
}

@Composable
internal fun erinnereVorschauAtlasZustand(): AtlasZustand {
    val context = LocalContext.current
    return remember(context) { AtlasZustand(context) }
}

internal object VorschauInspektorAktionen : KnotenInspektorAktionen {
    override fun parameter(schlüssel: String, wert: String) = Unit
    override fun name(wert: String) = Unit
    override fun eigenschaften(eigenschaften: Map<String, KnotenEigenschaft>) = Unit
    override fun anschlussArt(verweis: AnschlussVerweis, art: AnschlussArtId) = Unit
    override fun knoten(knoten: KnotenDaten) = Unit
}

package de.TeutonStudio.MathematikAtlas.schnittstellen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussVerweis
import de.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import de.TeutonStudio.KnotenKartenVerwalter.logik.KnotenErsetzungsAuswirkung
import de.TeutonStudio.MathematikAtlas.RechnerOperatorAuswahlArt
import de.TeutonStudio.MathematikAtlas.RechnerOperatorAuswahlDialog
import de.TeutonStudio.MathematikAtlas.RechnerOperatorAuswahlEintrag
import de.TeutonStudio.MathematikKnoten.konfiguriereStandardZahlenRechner
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator

@Preview(
    name = "Operatorauswahl · breit mit Verbindungswarnung",
    widthDp = 1280,
    heightDp = 900,
    showBackground = true,
)
@Composable
private fun RechnerOperatorAuswahlBreitVorschau() {
    OperatorAuswahlBeispiel()
}

@Preview(
    name = "Operatorauswahl · kompakt",
    widthDp = 480,
    heightDp = 780,
    showBackground = true,
)
@Composable
private fun RechnerOperatorAuswahlKompaktVorschau() {
    OperatorAuswahlBeispiel()
}

@Composable
private fun OperatorAuswahlBeispiel() {
    val basis = VorschauDaten.ZahlenRechner
    val addition = konfiguriereStandardZahlenRechner(basis, UniversellerZahlenOperator.ADDITION)
    val division = konfiguriereStandardZahlenRechner(basis, UniversellerZahlenOperator.DIVISION)
    val einträge = listOf(
        RechnerOperatorAuswahlEintrag(
            id = UniversellerZahlenOperator.ADDITION.stabileId,
            titel = "Addition",
            symbolLatex = "+",
            kategorie = "Grundrechenarten",
            beschreibung = "Addiert zwei oder mehr Zahlen beziehungsweise Zahlenfunktionen.",
            suchbegriffe = setOf("Summe"),
            kandidat = addition,
        ),
        RechnerOperatorAuswahlEintrag(
            id = UniversellerZahlenOperator.DIVISION.stabileId,
            titel = "Division",
            symbolLatex = "a\\div b",
            kategorie = "Grundrechenarten",
            beschreibung = "Dividiert links- oder rechtsseitig und kann einen Ersatzwert verwenden.",
            kandidat = division,
        ),
        RechnerOperatorAuswahlEintrag(
            id = "zahl.sin",
            titel = "Sinus",
            symbolLatex = "\\sin",
            kategorie = "Trigonometrie",
            beschreibung = "Wendet die Sinusfunktion an.",
            kandidat = konfiguriereStandardZahlenRechner(basis, UniversellerZahlenOperator.SINUS),
        ),
        RechnerOperatorAuswahlEintrag(
            id = "zahl.formel",
            titel = "Eigene Formel",
            symbolLatex = "f(x)",
            kategorie = "Eigene Formeln",
            beschreibung = "Öffnet den CAS-Formelbauer.",
            art = RechnerOperatorAuswahlArt.FORMEL,
        ),
    )
    val entfallenderAnschluss = basis.anschlüsse.firstOrNull()
    val warnung = if (entfallenderAnschluss == null) null else KnotenErsetzungsAuswirkung(
        erhalteneAnschlüsse = division.anschlüsse,
        hinzugefügteAnschlüsse = emptyList(),
        entfallendeAnschlüsse = listOf(entfallenderAnschluss),
        entfallendeVerbindungen = listOf(
            VerbindungDaten(
                von = AnschlussVerweis(basis.id, entfallenderAnschluss.id),
                zu = AnschlussVerweis(basis.id, entfallenderAnschluss.id),
            ),
        ),
    )

    MathematikAtlasVorschauRahmen {
        RechnerOperatorAuswahlDialog(
            familienTitel = "Zahlenrechner · 53 Auswahlzustände",
            einträge = einträge,
            aktuelleId = UniversellerZahlenOperator.ADDITION.stabileId,
            auswirkungFür = { if (it.id == UniversellerZahlenOperator.DIVISION.stabileId) warnung else null },
            schließen = {},
            operatorÜbernehmen = {},
            formelÖffnen = {},
        )
    }
}

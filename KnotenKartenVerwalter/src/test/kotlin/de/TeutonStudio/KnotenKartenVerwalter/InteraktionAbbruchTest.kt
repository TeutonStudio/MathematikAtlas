package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import kotlin.test.Test
import kotlin.test.assertEquals

class InteraktionAbbruchTest {
    @Test fun `abgebrochene Interaktion stellt Gruppenposition ohne Historieneintrag wieder her`() {
        val gruppe = VisuelleKnotenGruppeDaten(position = GraphPunkt(40f, 50f), größe = GraphGröße(200f, 120f))
        val editor = KartenEditorZustand(KartenDaten(name = "Test", visuelleGruppen = listOf(gruppe)), GraphPrüfung(AnschlussArtRegister(emptyList())))

        editor.beginneInteraktion()
        editor.führeAus(KartenAktion.VisuelleGruppeVerschieben(gruppe.id, GraphPunkt(30f, 20f)), mitHistorie = false)
        editor.verwerfeLaufendeInteraktion()

        assertEquals(gruppe.position, editor.karte.visuelleGruppen.single().position)
        assertEquals(gruppe.größe, editor.karte.visuelleGruppen.single().größe)
    }
}

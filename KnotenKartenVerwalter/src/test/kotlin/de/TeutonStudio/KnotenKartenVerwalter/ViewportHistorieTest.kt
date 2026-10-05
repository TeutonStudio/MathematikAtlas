package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnsichtsFenster
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.VisuelleKnotenGruppeDaten
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ViewportHistorieTest {
    private val prüfung = GraphPrüfung(AnschlussArtRegister(emptyList()))

    @Test
    fun reineAnsichtsÄnderungErzeugtKeinenUndoEintrag() {
        val zustand = editor()
        val ansicht = AnsichtsFenster(GraphPunkt(420f, -180f), 1.75f)

        zustand.führeAus(KartenAktion.AnsichtÄndern(ansicht))

        assertEquals(ansicht, zustand.karte.ansicht)
        assertFalse(zustand.kannRückgängig())
        assertFalse(zustand.kannWiederholen())
    }

    @Test
    fun undoStelltKnotenWiederHerAberBehältAktuellenViewport() {
        val zustand = editor()
        val knoten = zustand.karte.knoten.single()
        val neuePosition = GraphPunkt(360f, 240f)
        val aktuelleAnsicht = AnsichtsFenster(GraphPunkt(-640f, 315f), .8f)

        zustand.führeAus(KartenAktion.KnotenVerschieben(knoten.id, neuePosition))
        zustand.führeAus(KartenAktion.AnsichtÄndern(aktuelleAnsicht))
        zustand.rückgängig()

        assertEquals(GraphPunkt(80f, 80f), zustand.karte.knoten.single().position)
        assertEquals(aktuelleAnsicht, zustand.karte.ansicht)
        assertTrue(zustand.kannWiederholen())
    }

    @Test
    fun redoBehältDenZwischenUndoUndRedoVerschobenenViewport() {
        val zustand = editor()
        val knoten = zustand.karte.knoten.single()
        val neuePosition = GraphPunkt(510f, 125f)
        val ansichtNachUndo = AnsichtsFenster(GraphPunkt(95f, -430f), 2.2f)

        zustand.führeAus(KartenAktion.KnotenVerschieben(knoten.id, neuePosition))
        zustand.rückgängig()
        zustand.führeAus(KartenAktion.AnsichtÄndern(ansichtNachUndo))
        zustand.wiederholen()

        assertEquals(neuePosition, zustand.karte.knoten.single().position)
        assertEquals(ansichtNachUndo, zustand.karte.ansicht)
    }

    @Test
    fun panningLeertVorhandenenRedoZweigNicht() {
        val zustand = editor()
        val knoten = zustand.karte.knoten.single()

        zustand.führeAus(KartenAktion.KnotenVerschieben(knoten.id, GraphPunkt(220f, 180f)))
        zustand.rückgängig()
        assertTrue(zustand.kannWiederholen())

        zustand.führeAus(
            KartenAktion.AnsichtÄndern(AnsichtsFenster(GraphPunkt(700f, 300f), 1.4f)),
        )

        assertTrue(zustand.kannWiederholen())
    }

    @Test
    fun nurSemantischeAktionenErhöhenDieAuswertungsrevision() {
        val zustand = editor()
        val knoten = zustand.karte.knoten.single()
        val anfang = zustand.auswertungsRevision

        zustand.führeAus(KartenAktion.AnsichtÄndern(AnsichtsFenster(GraphPunkt(10f, 20f), 1.2f)))
        zustand.führeAus(KartenAktion.KnotenVerschieben(knoten.id, GraphPunkt(200f, 100f)))
        zustand.führeAus(KartenAktion.KnotenGrößeÄndern(knoten.id, knoten.größe.copy(breite = 300f)))
        zustand.führeAus(KartenAktion.VisuelleGruppeTitelÄndern(zustand.karte.visuelleGruppen.single().id, "Neu"))
        assertEquals(anfang, zustand.auswertungsRevision)

        zustand.führeAus(KartenAktion.KnotenParameterÄndern(knoten.id, "wert", "2"))
        assertEquals(anfang + 1, zustand.auswertungsRevision)
    }

    private fun editor(): KartenEditorZustand = KartenEditorZustand(
        KartenDaten(
            name = "Viewport-Historie",
            knoten = listOf(
                KnotenDaten(
                    art = "test.knoten",
                    name = "Historienrelevanter Inhalt",
                    position = GraphPunkt(80f, 80f),
                ),
            ),
            visuelleGruppen = listOf(
                VisuelleKnotenGruppeDaten(
                    titel = "Layout",
                    position = GraphPunkt(1_000f, 1_000f),
                    größe = GraphGröße(200f, 160f),
                ),
            ),
        ),
        prüfung,
    )
}

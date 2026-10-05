package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.*
import kotlin.test.*

class MehrfachAuswahlTest {
    private val art = AnschlussArt(AnschlussArtId("objekt"), "Objekt")
    private val prüfung = GraphPrüfung(AnschlussArtRegister(listOf(art)))

    @Test fun `Gruppenauswahl wird gemeinsam verschoben und atomar rückgängig gemacht`() {
        val a = knoten("a", GraphPunkt(0f, 0f))
        val b = knoten("b", GraphPunkt(100f, 20f))
        val zustand = KartenEditorZustand(KartenDaten(name = "Test", knoten = listOf(a, b)), prüfung)
        zustand.setzeAuswahlModus(AuswahlModus.Gruppe)
        zustand.wähleKnoten(a.id)
        zustand.wähleKnoten(b.id)

        zustand.beginneInteraktion()
        zustand.führeAus(KartenAktion.KnotenVerschieben(a.id, GraphPunkt(15f, 8f)), mitHistorie = false)
        zustand.beendeInteraktion()

        assertEquals(GraphPunkt(15f, 8f), zustand.karte.knoten.first { it.id == a.id }.position)
        assertEquals(GraphPunkt(115f, 28f), zustand.karte.knoten.first { it.id == b.id }.position)
        zustand.rückgängig()
        assertEquals(GraphPunkt(0f, 0f), zustand.karte.knoten.first { it.id == a.id }.position)
        assertEquals(GraphPunkt(100f, 20f), zustand.karte.knoten.first { it.id == b.id }.position)
    }

    @Test fun `Visuelle Gruppe erhält Titel und stabile Geometrie aus ihrer Auswahl`() {
        val a = knoten("a", GraphPunkt(40f, 80f))
        val b = knoten("b", GraphPunkt(320f, 180f))
        val zustand = KartenEditorZustand(KartenDaten(name = "Test", knoten = listOf(a, b)), prüfung)
        zustand.setzeAuswahlModus(AuswahlModus.Gruppe)
        zustand.wähleKnoten(a.id)
        zustand.wähleKnoten(b.id)

        zustand.führeAus(KartenAktion.VisuelleGruppeErstellen(setOf(a.id, b.id), "Beweisidee"))

        val gruppe = zustand.karte.visuelleGruppen.single()
        assertEquals("Beweisidee", gruppe.titel)
        assertEquals(setOf(a.id, b.id), gruppe.knotenIds)
        assertTrue(gruppe.größe.breite >= VISUELLE_GRUPPE_MINDEST_BREITE)
        assertTrue(gruppe.größe.höhe >= VISUELLE_GRUPPE_MINDEST_HÖHE)
        assertTrue(gruppe.enthältVollständig(a))
        assertTrue(gruppe.enthältVollständig(b))
    }

    @Test fun `Verschieben einer visuellen Gruppe bewegt Kinder atomar und bewahrt relative Lage`() {
        val a = knoten("a", GraphPunkt(40f, 90f))
        val b = knoten("b", GraphPunkt(300f, 160f))
        val zustand = KartenEditorZustand(KartenDaten(name = "Test", knoten = listOf(a, b)), prüfung)
        zustand.führeAus(KartenAktion.VisuelleGruppeErstellen(setOf(a.id, b.id)))
        val gruppeVorher = zustand.karte.visuelleGruppen.single()
        val abstandVorher = b.position - a.position

        zustand.beginneInteraktion()
        zustand.führeAus(
            KartenAktion.VisuelleGruppeVerschieben(gruppeVorher.id, GraphPunkt(35f, -12f)),
            mitHistorie = false,
        )
        zustand.beendeInteraktion()

        val gruppeDanach = zustand.karte.visuelleGruppen.single()
        val aDanach = zustand.karte.knoten.first { it.id == a.id }
        val bDanach = zustand.karte.knoten.first { it.id == b.id }
        assertEquals(gruppeVorher.position + GraphPunkt(35f, -12f), gruppeDanach.position)
        assertEquals(a.position + GraphPunkt(35f, -12f), aDanach.position)
        assertEquals(b.position + GraphPunkt(35f, -12f), bDanach.position)
        assertEquals(abstandVorher, bDanach.position - aDanach.position)

        zustand.rückgängig()
        assertEquals(gruppeVorher.position, zustand.karte.visuelleGruppen.single().position)
        assertEquals(a.position, zustand.karte.knoten.first { it.id == a.id }.position)
    }

    @Test fun `Nur vollständig enthaltene Knoten werden einer Gruppe zugeordnet`() {
        val innen = knoten("innen", GraphPunkt(50f, 100f))
        val teilweise = knoten("teilweise", GraphPunkt(250f, 180f))
        val außen = knoten("außen", GraphPunkt(500f, 500f))
        val gruppe = VisuelleKnotenGruppeDaten(
            titel = "Bereich",
            position = GraphPunkt(20f, 20f),
            größe = GraphGröße(360f, 260f),
        )
        val zustand = KartenEditorZustand(
            KartenDaten(name = "Test", knoten = listOf(innen, teilweise, außen), visuelleGruppen = listOf(gruppe)),
            prüfung,
        )

        zustand.führeAus(KartenAktion.VisuelleGruppenKinderZuordnen(gruppe.id))

        assertEquals(setOf(innen.id), zustand.karte.visuelleGruppen.single().knotenIds)
    }

    @Test fun `Ein aus der Gruppe bewegter Knoten tritt aus ohne die Gruppe zu löschen`() {
        val a = knoten("a", GraphPunkt(60f, 100f))
        val b = knoten("b", GraphPunkt(300f, 120f))
        val zustand = KartenEditorZustand(KartenDaten(name = "Test", knoten = listOf(a, b)), prüfung)
        zustand.führeAus(KartenAktion.VisuelleGruppeErstellen(setOf(a.id, b.id)))
        val gruppe = zustand.karte.visuelleGruppen.single()

        zustand.führeAus(KartenAktion.KnotenVerschieben(a.id, GraphPunkt(900f, 900f)))

        val aktualisiert = zustand.karte.visuelleGruppen.single()
        assertEquals(gruppe.id, aktualisiert.id)
        assertEquals(setOf(b.id), aktualisiert.knotenIds)
    }

    @Test fun `Gruppen nehmen nur bei mehr als halber Flächenüberdeckung automatisch auf`() {
        fun probe(x: Float) = KnotenDaten(
            art = "test",
            name = "P$x",
            position = GraphPunkt(x, 42f),
            größe = GraphGröße(100f, 100f),
        )
        val neunundvierzig = probe(151f)
        val fünfzig = probe(150f)
        val einundfünfzig = probe(149f)
        val gruppe = VisuelleKnotenGruppeDaten(
            position = GraphPunkt.Zero,
            größe = GraphGröße(200f, 142f),
        )

        val karte = KartenDaten(
            name = "Schwellen",
            knoten = listOf(neunundvierzig, fünfzig, einundfünfzig),
            visuelleGruppen = listOf(gruppe),
        ).mitAutomatischenVisuellenGruppenMitgliedschaften()

        assertEquals(setOf(einundfünfzig.id), karte.visuelleGruppen.single().knotenIds)
    }

    @Test fun `Größte Überdeckung gewinnt und Gleichstand erhält bisherige Gruppe`() {
        val knoten = KnotenDaten(
            art = "test",
            name = "K",
            position = GraphPunkt(50f, 42f),
            größe = GraphGröße(100f, 100f),
        )
        val links = VisuelleKnotenGruppeDaten(
            knotenIds = setOf(knoten.id),
            position = GraphPunkt.Zero,
            größe = GraphGröße(125f, 142f),
        )
        val rechts = VisuelleKnotenGruppeDaten(
            position = GraphPunkt(75f, 0f),
            größe = GraphGröße(125f, 142f),
        )

        val gleichstand = KartenDaten(name = "Gleich", knoten = listOf(knoten), visuelleGruppen = listOf(rechts, links))
            .mitAutomatischenVisuellenGruppenMitgliedschaften()
        assertEquals(setOf(knoten.id), gleichstand.visuelleGruppen.single { it.id == links.id }.knotenIds)

        val größer = gleichstand.copy(
            visuelleGruppen = gleichstand.visuelleGruppen.map {
                if (it.id == rechts.id) it.copy(position = GraphPunkt(40f, 0f)) else it
            },
        ).mitAutomatischenVisuellenGruppenMitgliedschaften()
        assertEquals(setOf(knoten.id), größer.visuelleGruppen.single { it.id == rechts.id }.knotenIds)
    }

    @Test fun `Mitgliedschaft wechselt erst am Interaktionsende und gemeinsam mit Undo`() {
        val knoten = KnotenDaten(
            art = "test",
            name = "K",
            position = GraphPunkt(300f, 42f),
            größe = GraphGröße(100f, 100f),
        )
        val gruppe = VisuelleKnotenGruppeDaten(position = GraphPunkt.Zero, größe = GraphGröße(200f, 142f))
        val zustand = KartenEditorZustand(KartenDaten(name = "Drag", knoten = listOf(knoten), visuelleGruppen = listOf(gruppe)), prüfung)

        zustand.beginneInteraktion()
        zustand.führeAus(KartenAktion.KnotenVerschieben(knoten.id, GraphPunkt(40f, 42f)), mitHistorie = false)
        assertTrue(zustand.karte.visuelleGruppen.single().knotenIds.isEmpty())
        zustand.beendeInteraktion()
        assertEquals(setOf(knoten.id), zustand.karte.visuelleGruppen.single().knotenIds)

        zustand.rückgängig()
        assertEquals(GraphPunkt(300f, 42f), zustand.karte.knoten.single().position)
        assertTrue(zustand.karte.visuelleGruppen.single().knotenIds.isEmpty())
    }

    @Test fun `Gruppenbewegung und Skalierung lösen die automatische Aufnahme aus`() {
        val knoten = KnotenDaten(
            art = "test",
            name = "K",
            position = GraphPunkt(300f, 42f),
            größe = GraphGröße(100f, 100f),
        )
        val gruppe = VisuelleKnotenGruppeDaten(position = GraphPunkt.Zero, größe = GraphGröße(200f, 142f))
        val durchBewegung = KartenEditorZustand(
            KartenDaten(name = "Gruppenbewegung", knoten = listOf(knoten), visuelleGruppen = listOf(gruppe)),
            prüfung,
        )

        durchBewegung.beginneInteraktion()
        durchBewegung.führeAus(
            KartenAktion.VisuelleGruppeVerschieben(gruppe.id, GraphPunkt(200f, 0f)),
            mitHistorie = false,
        )
        durchBewegung.beendeInteraktion()
        assertEquals(setOf(knoten.id), durchBewegung.karte.visuelleGruppen.single().knotenIds)

        val durchSkalierung = KartenEditorZustand(
            KartenDaten(name = "Gruppenskalierung", knoten = listOf(knoten), visuelleGruppen = listOf(gruppe)),
            prüfung,
        )
        durchSkalierung.beginneInteraktion()
        durchSkalierung.führeAus(
            KartenAktion.VisuelleGruppeGrößeÄndern(gruppe.id, GraphGröße(380f, 142f)),
            mitHistorie = false,
        )
        durchSkalierung.beendeInteraktion()
        assertEquals(setOf(knoten.id), durchSkalierung.karte.visuelleGruppen.single().knotenIds)
    }

    @Test fun `Löschen einer visuellen Gruppe lässt Knoten und Verbindungen unverändert`() {
        val aus = AnschlussDaten(
            name = "aus",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = art.id,
        )
        val ein = AnschlussDaten(
            name = "ein",
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = art.id,
        )
        val a = KnotenDaten(art = "test", name = "A", position = GraphPunkt(40f, 100f), anschlüsse = listOf(aus))
        val b = KnotenDaten(art = "test", name = "B", position = GraphPunkt(300f, 100f), anschlüsse = listOf(ein))
        val verbindung = VerbindungDaten(
            von = AnschlussVerweis(a.id, aus.id),
            zu = AnschlussVerweis(b.id, ein.id),
        )
        val zustand = KartenEditorZustand(
            KartenDaten(name = "Test", knoten = listOf(a, b), verbindungen = listOf(verbindung)),
            prüfung,
        )
        zustand.führeAus(KartenAktion.VisuelleGruppeErstellen(setOf(a.id, b.id)))
        val gruppenId = zustand.karte.visuelleGruppen.single().id

        zustand.führeAus(KartenAktion.VisuelleGruppeLöschen(gruppenId))

        assertTrue(zustand.karte.visuelleGruppen.isEmpty())
        assertEquals(listOf(a, b), zustand.karte.knoten)
        assertEquals(listOf(verbindung), zustand.karte.verbindungen)
        zustand.rückgängig()
        assertEquals(gruppenId, zustand.karte.visuelleGruppen.single().id)
    }

    @Test fun `Duplikation kopiert nur interne Verbindungen mit neuen IDs`() {
        val aus = AnschlussDaten(
            name = "aus",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = art.id,
        )
        val ein = AnschlussDaten(
            name = "ein",
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = art.id,
        )
        val a = KnotenDaten(art = "test", name = "A", anschlüsse = listOf(aus))
        val b = KnotenDaten(art = "test", name = "B", anschlüsse = listOf(ein))
        val verbindung = VerbindungDaten(
            von = AnschlussVerweis(a.id, aus.id),
            zu = AnschlussVerweis(b.id, ein.id),
        )
        val zustand = KartenEditorZustand(
            KartenDaten(name = "Test", knoten = listOf(a, b), verbindungen = listOf(verbindung)),
            prüfung,
        )
        zustand.setzeAuswahlModus(AuswahlModus.Gruppe)
        zustand.wähleKnoten(a.id)
        zustand.wähleKnoten(b.id)

        zustand.dupliziereAuswahl()

        assertEquals(4, zustand.karte.knoten.size)
        assertEquals(2, zustand.karte.verbindungen.size)
        val kopie = zustand.karte.verbindungen.single { it.id != verbindung.id }
        assertNotEquals(a.id, kopie.von.knotenId)
        assertNotEquals(b.id, kopie.zu.knotenId)
        assertEquals(zustand.ausgewählteKnoten, setOf(kopie.von.knotenId, kopie.zu.knotenId))
    }

    private fun knoten(name: String, position: GraphPunkt = GraphPunkt.Zero) =
        KnotenDaten(art = "test", name = name, position = position)
}

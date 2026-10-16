package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import kotlin.test.*
import kotlin.random.Random

class EinfuegeLayoutTest {
    private val art = AnschlussArtId("layout.test")
    private val prüfung = GraphPrüfung(AnschlussArtRegister(listOf(AnschlussArt(art, "Test"))))
    private fun knoten(id: String, x: Float, y: Float = 100f, breite: Float = 100f) = KnotenDaten(
        id = KnotenId(id), art = "test", name = id, position = GraphPunkt(x, y), größe = GraphGröße(breite, 80f),
        anschlüsse = listOf(
            AnschlussDaten(name = "in", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = art),
            AnschlussDaten(name = "out", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = art),
        ),
    )
    private val a = knoten("a", 0f)
    private val c = knoten("c", 100f)
    private val b = knoten("b", 200f)
    private fun ref(k: KnotenDaten, ausgang: Boolean) = AnschlussVerweis(k.id, k.anschlüsse[if (ausgang) 1 else 0].id)
    private val ab = VerbindungDaten(von = ref(a, true), zu = ref(b, false))
    private fun karte(vararg weitere: KnotenDaten) = KartenDaten(name = "Layout", knoten = listOf(a, c, b) + weitere, verbindungen = listOf(ab))
    private fun layout(karte: KartenDaten, anhänge: Map<KnotenId, KnotenAnhangMaße> = emptyMap()) =
        planeEinfuegeLayout(karte, c.id, a.id, b.id, anhänge)
    private fun KartenDaten.x(id: KnotenId) = knoten.single { it.id == id }.position.x
    private fun einfügen(editor: KartenEditorZustand) {
        editor.beginneKnotenZiehen(c.id)
        editor.aktualisiereKnotenEinfügeziel(ab.id)
        editor.beendeKnotenZiehen()
    }

    @Test fun beidseitigNurFehlendenPlatzSchaffenUndUnverbundeneHindernisseMitnehmen() {
        val links = knoten("links", -110f)
        val rechts = knoten("rechts", 310f)
        val fern = knoten("fern", 1000f)
        val andereZeile = knoten("andereZeile", 200f, 400f)
        val basis = karte(links, rechts, fern, andereZeile)
        val plan = layout(basis)
        val nachher = plan.anwenden(basis)
        assertEquals(-32f, nachher.x(a.id)); assertEquals(232f, nachher.x(b.id))
        assertEquals(-164f, nachher.x(links.id)); assertEquals(364f, nachher.x(rechts.id))
        assertEquals(c, nachher.knoten.single { it.id == c.id })
        assertEquals(fern, nachher.knoten.single { it.id == fern.id })
        assertEquals(andereZeile, nachher.knoten.single { it.id == andereZeile.id })
        assertEquals(basis.knoten.map { it.position.y }, nachher.knoten.map { it.position.y })
        assertEquals(plan, layout(basis.copy(knoten = basis.knoten.reversed())))
    }

    @Test fun ausreichendPlatzUndVorhandeneEntfernteUeberlappungenBleibenUnveraendert() {
        val basis = karte(knoten("x", 800f), knoten("y", 800f)).copy(knoten =
            listOf(a.copy(position = GraphPunkt(-100f, 100f)), c, b.copy(position = GraphPunkt(300f, 100f)),
                knoten("x", 800f), knoten("y", 800f)))
        assertTrue(layout(basis).knotenDeltas.isEmpty())
    }

    @Test fun anhaengeUndUnterschiedlicheBreitenZaehlenAlsHindernisse() {
        val hindernis = knoten("oben", 310f, 0f, 140f)
        val basis = karte(hindernis)
        assertNull(layout(basis).knotenDeltas[hindernis.id])
        val plan = layout(basis, mapOf(hindernis.id to KnotenAnhangMaße(unten = 60f)))
        assertEquals(54f, plan.knotenDeltas[hindernis.id])
    }

    @Test fun rueckwaertsVerlaufendeUndVertikalVersetzteVerbindungWirdBeidseitigGeoeffnet() {
        val basis = karte().copy(knoten = listOf(a.copy(position = GraphPunkt(400f, -200f)), c,
            b.copy(position = GraphPunkt(-100f, 300f))))
        val nachher = layout(basis).anwenden(basis)
        assertEquals(-32f, nachher.x(a.id)); assertEquals(232f, nachher.x(b.id))
        assertEquals(listOf(-200f, 100f, 300f), nachher.knoten.map { it.position.y })
    }

    @Test fun versetzteQuelleKollidiertNichtMitBereitsDurchlaufenemUnbewegtemHindernis() {
        val hindernis = knoten("hindernis", -10f, 250f)
        val basis = karte(hindernis).copy(knoten = listOf(a.copy(position = GraphPunkt(400f, 200f)), c, b, hindernis))
        val nachher = layout(basis).anwenden(basis)
        val quelle = nachher.knoten.single { it.id == a.id }
        val h = nachher.knoten.single { it.id == hindernis.id }
        assertTrue(quelle.position.x + quelle.größe.breite + 32f <= h.position.x)
        assertEquals(hindernis, h)
    }

    @Test fun breiteHindernisseAufAndererSeiteWerdenBeiAusgeloesterKollisionMitgenommen() {
        val hindernis = knoten("breit", -200f, 250f, 800f)
        val basis = karte(hindernis).copy(knoten = listOf(a.copy(position = GraphPunkt(400f, 200f)), c, b, hindernis))
        val nachher = layout(basis).anwenden(basis)
        val quelle = nachher.knoten.single { it.id == a.id }
        val h = nachher.knoten.single { it.id == hindernis.id }
        assertEquals(-32f, quelle.position.x)
        assertTrue(quelle.position.x + quelle.größe.breite + 32f <= h.position.x)
    }

    @Test fun ausgeloesteVerschiebungenErzeugenAuchBeiGemischtenRechteckenKeineNeuenKollisionen() {
        val zufall = Random(4137)
        repeat(200) { fall ->
            val knoten = listOf(a, b).map { k -> k.copy(position = GraphPunkt(zufall.nextInt(-400, 700).toFloat(),
                zufall.nextInt(-200, 400).toFloat()), größe = GraphGröße(zufall.nextInt(60, 500).toFloat(), 80f)) } + c +
                (0 until 30).map { i -> knoten("h$i", zufall.nextInt(-800, 1400).toFloat(),
                    zufall.nextInt(-200, 500).toFloat(), zufall.nextInt(60, 700).toFloat()) }
            val basis = karte().copy(knoten = knoten)
            val plan = layout(basis)
            val nachher = plan.anwenden(basis)
            assertEquals(c.position, nachher.knoten.single { it.id == c.id }.position)
            for ((i, links) in nachher.knoten.withIndex()) for (rechts in nachher.knoten.drop(i + 1)) {
                if (links.id !in plan.knotenDeltas && rechts.id !in plan.knotenDeltas && links.id != c.id && rechts.id != c.id) continue
                if (links.position.y >= rechts.position.y + rechts.größe.höhe || rechts.position.y >= links.position.y + links.größe.höhe) continue
                assertTrue(links.position.x + links.größe.breite + 31.99f <= rechts.position.x ||
                    rechts.position.x + rechts.größe.breite + 31.99f <= links.position.x,
                    "Fall $fall: ${links.id} / ${rechts.id}, $plan")
            }
        }
    }

    @Test fun grosseSchriftImEingeklapptenGruppenblockWirdBeruecksichtigt() {
        val gruppe = VisuelleKnotenGruppeDaten(titel = "Breiter Block", eingeklappt = true, knotenIds = setOf(a.id),
            position = GraphPunkt(-300f, 50f), größe = GraphGröße(1000f, 900f))
        val basis = karte().copy(visuelleGruppen = listOf(gruppe))
        val editor = KartenEditorZustand(basis, prüfung)
        val blockId = editor.projektion.blöcke.keys.single()
        editor.meldeInhaltsminimum(blockId, GraphGröße(500f, 110f))
        einfügen(editor)
        assertEquals(-132f, editor.karte.visuelleGruppen.single().position.x - gruppe.position.x)
        editor.beginneInteraktion() // Animationsende: dargestellter Block muss frei sein.
        val block = editor.darstellungsKarte.knoten.single { it.id == blockId }
        assertTrue(block.position.x + block.größe.breite + 32f <= c.position.x)
        assertEquals(500f, block.größe.breite)
    }

    @Test fun verschachtelteGruppeWeichtAlsVollstaendigerBlockAus() {
        val d = knoten("d", 30f, 230f)
        val kind = VisuelleKnotenGruppeDaten(id = VisuelleGruppenId("kind"), titel = "Kind", knotenIds = setOf(d.id),
            position = GraphPunkt(10f, 190f), größe = GraphGröße(180f, 150f), elternGruppenId = VisuelleGruppenId("g"))
        val g = VisuelleKnotenGruppeDaten(id = VisuelleGruppenId("g"), titel = "G", knotenIds = setOf(a.id),
            position = GraphPunkt(-24f, 30f), größe = GraphGröße(240f, 340f))
        val basis = karte(d).copy(visuelleGruppen = listOf(g, kind))
        val plan = layout(basis)
        val dx = assertNotNull(plan.gruppenDeltas[g.id])
        assertTrue(dx < 0f)
        assertEquals(dx, plan.gruppenDeltas[kind.id])
        assertEquals(dx, plan.knotenDeltas[a.id]); assertEquals(dx, plan.knotenDeltas[d.id])
        assertEquals(g.knotenIds, plan.anwenden(basis).visuelleGruppen.first().knotenIds)
    }

    @Test fun eingeklappteGruppeVerwendetSichtbarenBlockStattVerstecktemRahmen() {
        val g = VisuelleKnotenGruppeDaten(titel = "G", knotenIds = setOf(a.id), eingeklappt = true,
            position = GraphPunkt(-300f, 30f), größe = GraphGröße(1000f, 900f))
        val basis = karte().copy(visuelleGruppen = listOf(g))
        assertNull(layout(basis).gruppenDeltas[g.id])
    }

    @Test fun effektiveGroesseWirdBeruecksichtigtAberNichtGespeichert() {
        val editor = KartenEditorZustand(karte(), prüfung)
        editor.meldeInhaltsminimum(c.id, GraphGröße(180f, 80f))
        einfügen(editor)
        assertEquals(312f, editor.karte.x(b.id))
        assertEquals(c.größe, editor.karte.knoten.single { it.id == c.id }.größe)
        assertEquals(2, editor.karte.verbindungen.size)
    }

    @Test fun einfuegenLayoutUndDragSindEinUndoSchrittMitStabilerPersistenz() {
        val basis = karte().copy(knoten = listOf(a, c.copy(position = GraphPunkt(100f, 300f)), b))
        val editor = KartenEditorZustand(basis, prüfung)
        editor.beginneKnotenZiehen(c.id)
        editor.führeAus(KartenAktion.KnotenVerschieben(c.id, c.position), mitHistorie = false)
        editor.aktualisiereKnotenEinfügeziel(ab.id); editor.beendeKnotenZiehen()
        val nachher = editor.karte
        assertEquals(-32f, nachher.x(a.id)); assertEquals(232f, nachher.x(b.id))
        assertEquals(nachher, KartenDatenJson.lese(KartenDatenJson.schreibe(nachher)))
        editor.rückgängig(); assertEquals(basis, editor.karte); assertFalse(editor.kannRückgängig())
        editor.wiederholen(); assertEquals(nachher, editor.karte)
    }

    @Test fun mehrdeutigeAuswahlVerschiebtErstBeiBestaetigungUndPrueftAktuelleMinima() {
        val kandidat = c.copy(anschlüsse = c.anschlüsse + c.anschlüsse.first().copy(id = AnschlussId("zweiter")))
        val basis = karte().copy(knoten = listOf(a, kandidat, b))
        val editor = KartenEditorZustand(basis, prüfung)
        einfügen(editor)
        assertNotNull(editor.verbindungsAuswahl); assertEquals(basis, editor.karte)
        editor.meldeInhaltsminimum(c.id, GraphGröße(200f, 80f))
        editor.wähleVerbindungsVorschlag(1)
        assertEquals(332f, editor.karte.x(b.id))
        editor.rückgängig(); assertEquals(basis, editor.karte)
        einfügen(editor); editor.verwerfeVerbindungsAuswahl(); assertEquals(basis, editor.karte)
        einfügen(editor)
        editor.führeAus(KartenAktion.KnotenVerschieben(b.id, GraphPunkt(700f, 100f)), mitHistorie = false)
        val verändert = editor.karte
        editor.wähleVerbindungsVorschlag(0)
        assertEquals(verändert, editor.karte); assertNotNull(editor.letzteMeldung)
    }

    @Test fun gruppeninternesLayoutErweitertRahmenOhneMitgliederZuVerlieren() {
        val gruppe = VisuelleKnotenGruppeDaten(titel = "Innen", knotenIds = setOf(a.id, b.id, c.id),
            position = GraphPunkt(-10f, 0f), größe = GraphGröße(320f, 220f))
        val basis = karte().copy(visuelleGruppen = listOf(gruppe))
        val editor = KartenEditorZustand(basis, prüfung)
        editor.öffneGruppe(gruppe.id)
        einfügen(editor)
        val g = editor.karte.visuelleGruppen.single()
        assertEquals(gruppe.knotenIds, g.knotenIds)
        assertTrue(g.position.x <= -56f)
        assertTrue(g.position.x + g.größe.breite >= 356f)
        assertEquals(basis, KartenDatenJson.lese(KartenDatenJson.schreibe(basis)))
        editor.rückgängig(); assertEquals(basis, editor.karte)
    }

    @Test fun animationAendertNurDarstellungUndWirdDurchNeueBearbeitungBeendet() {
        val editor = KartenEditorZustand(karte(), prüfung)
        einfügen(editor)
        val gespeichert = editor.karte
        val revision = editor.auswertungsRevision
        val animation = assertNotNull(editor.einfügeLayoutAnimation)
        assertEquals(0f, editor.darstellungsKarte.x(a.id))
        editor.aktualisiereEinfügeAnimation(animation, .5f)
        assertEquals(-16f, editor.darstellungsKarte.x(a.id))
        assertEquals(216f, editor.darstellungsKarte.x(b.id))
        assertEquals(c.position, editor.darstellungsKarte.knoten.single { it.id == c.id }.position)
        assertEquals(gespeichert, editor.karte); assertEquals(revision, editor.auswertungsRevision)
        editor.beginneInteraktion()
        assertNull(editor.einfügeLayoutAnimation)
        assertEquals(-32f, editor.darstellungsKarte.x(a.id))
        editor.aktualisiereEinfügeAnimation(animation, .75f)
        assertEquals(-32f, editor.darstellungsKarte.x(a.id))
        editor.verwerfeLaufendeInteraktion()
        editor.rückgängig(); assertEquals(karte().knoten.map { it.position }, editor.karte.knoten.map { it.position })
    }

    @Test fun geoeffneterGruppenrahmenFolgtDerAblageAuchOhneNachbarverschiebung() {
        val gruppe = VisuelleKnotenGruppeDaten(titel = "Innen", knotenIds = setOf(a.id, b.id, c.id),
            position = GraphPunkt(-10f, 0f), größe = GraphGröße(500f, 220f))
        val basis = karte().copy(knoten = listOf(a, c, b.copy(position = GraphPunkt(400f, 100f))), visuelleGruppen = listOf(gruppe))
        val editor = KartenEditorZustand(basis, prüfung)
        editor.öffneGruppe(gruppe.id)
        editor.beginneKnotenZiehen(c.id)
        editor.führeAus(KartenAktion.KnotenVerschieben(c.id, GraphPunkt(200f, -20f)), mitHistorie = false)
        editor.aktualisiereKnotenEinfügeziel(ab.id); editor.beendeKnotenZiehen()
        assertEquals(a.position, editor.karte.knoten.single { it.id == a.id }.position)
        assertEquals(400f, editor.karte.x(b.id))
        assertTrue(editor.karte.visuelleGruppen.single().position.y <= -86f)
        editor.rückgängig(); assertEquals(basis, editor.karte)
    }

    @Test fun inkompatibleEinfügungUndGestenabbruchVerschiebenKeineNachbarn() {
        val inkompatibel = c.copy(anschlüsse = emptyList())
        val basis = karte().copy(knoten = listOf(a, inkompatibel, b))
        val editor = KartenEditorZustand(basis, prüfung)
        einfügen(editor); assertEquals(basis, editor.karte)
        editor.beginneKnotenZiehen(c.id)
        editor.führeAus(KartenAktion.KnotenVerschieben(c.id, GraphPunkt(90f, 100f)), mitHistorie = false)
        editor.aktualisiereKnotenEinfügeziel(ab.id); editor.verwerfeKnotenZiehen()
        assertEquals(basis, editor.karte); assertNull(editor.einfügeLayoutAnimation)
    }
}

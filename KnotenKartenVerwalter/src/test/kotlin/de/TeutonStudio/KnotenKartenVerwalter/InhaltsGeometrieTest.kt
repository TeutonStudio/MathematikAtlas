package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.*
import kotlin.test.*

class InhaltsGeometrieTest {
    private val k = KnotenDaten(art = "test", name = "Inhalt", position = GraphPunkt(100f, 120f), größe = GraphGröße(180f, 100f))
    private val g = VisuelleKnotenGruppeDaten(titel = "Gruppe", knotenIds = setOf(k.id), position = GraphPunkt(80f, 60f), größe = GraphGröße(200f, 150f))
    private fun zustand(karte: KartenDaten) = KartenEditorZustand(karte, GraphPrüfung(AnschlussArtRegister(emptyList())))

    @Test fun `Inhaltsminimum ist abgeleitet und groessere Benutzermaße bleiben erhalten`() {
        val karte = KartenDaten(name = "Test", knoten = listOf(k))
        val z = zustand(karte)
        z.meldeInhaltsminimum(k.id, GraphGröße(320f, 200f))
        assertEquals(karte, z.karte)
        assertEquals(GraphGröße(320f, 200f), z.darstellungsKarte.knoten.single().größe)
        z.führeAus(KartenAktion.KnotenGrößeÄndern(k.id, GraphGröße(10f, 10f)))
        assertEquals(GraphGröße(320f, 200f), z.karte.knoten.single().größe)
        z.führeAus(KartenAktion.KnotenGrößeÄndern(k.id, GraphGröße(600f, 400f)))
        z.meldeInhaltsminimum(k.id, GraphGröße(100f, 80f))
        assertEquals(GraphGröße(600f, 400f), z.darstellungsKarte.knoten.single().größe)
        z.rückgängig()
        assertEquals(GraphGröße(320f, 200f), z.karte.knoten.single().größe)
    }

    @Test fun `Untergruppen und gemessene Knoten bestimmen den gepufferten Elternrahmen`() {
        val eltern = VisuelleKnotenGruppeDaten(titel = "Eltern", position = GraphPunkt(90f, 90f), größe = GraphGröße(180f, 110f))
        val kind = g.copy(elternGruppenId = eltern.id)
        val karte = KartenDaten(name = "Test", knoten = listOf(k), visuelleGruppen = listOf(eltern, kind))
        val z = zustand(karte)
        z.meldeInhaltsminimum(k.id, GraphGröße(500f, 300f))
        val gruppen = z.darstellungsKarte.visuelleGruppen.associateBy { it.id }
        val innen = gruppen.getValue(kind.id)
        val außen = gruppen.getValue(eltern.id)
        assertTrue(innen.position.x <= k.position.x - 24f)
        assertTrue(innen.position.y + VISUELLE_GRUPPE_KOPFZEILE_HÖHE <= k.position.y - 24f)
        assertTrue(innen.position.x + innen.größe.breite >= k.position.x + 500f + 24f)
        assertTrue(außen.position.x + außen.größe.breite >= innen.position.x + innen.größe.breite + 24f)
        assertTrue(außen.position.y + VISUELLE_GRUPPE_KOPFZEILE_HÖHE <= innen.position.y - 24f)
        assertEquals(karte, z.karte)
    }

    @Test fun `Gruppenverkleinerung verliert keine enthaltenen Knoten`() {
        val karte = KartenDaten(name = "Test", knoten = listOf(k), visuelleGruppen = listOf(g))
        val z = zustand(karte)
        z.meldeInhaltsminimum(k.id, GraphGröße(500f, 300f))
        z.führeAus(KartenAktion.VisuelleGruppeGrößeÄndern(g.id, GraphGröße(1f, 1f)))
        val gruppe = z.karte.visuelleGruppen.single()
        assertEquals(setOf(k.id), gruppe.knotenIds)
        assertTrue(gruppe.größe.breite >= 548f)
        assertEquals(k.größe, z.karte.knoten.single().größe)
        z.rückgängig()
        assertEquals(karte, z.karte)
    }
}

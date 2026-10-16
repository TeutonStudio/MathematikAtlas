package de.TeutonStudio.MathematikAtlas

import androidx.compose.ui.geometry.Offset
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import kotlin.test.*

class PieModellTest {
    private fun vorlage(name: String, größe: GraphGröße = GraphGröße(240f, 120f)) =
        KnotenVorlage(art = "test.$name", name = name, kategorie = "Test", beschreibung = "Test", standardGröße = größe, anschlüsse = emptyList())

    @Test fun faecherBleibtBeiJedemZoomKollisionsfreiUndDeterministisch() {
        val einträge = (0..89).map { PieEintrag("$it", "Gruppe ${it % 9}", vorlage("K$it", GraphGröße(120f + it * 3, 90f + it))) }
        for (zoom in listOf(.25f, 1f, 3.5f)) {
            val layout = planePieFächer(einträge, emptyMap(), zoom, 45f)
            assertEquals(layout, planePieFächer(einträge, emptyMap(), zoom, 45f))
            assertEquals(einträge.map { it.id }.toSet(), layout.map { it.eintrag.id }.toSet())
            layout.forEachIndexed { i, p ->
                assertEquals(p.eintrag.vorlage.standardGröße.breite * zoom, p.rahmen.width, .05f)
                layout.drop(i + 1).forEach { assertFalse(p.rahmen.overlaps(it.rahmen)) }
            }
        }
    }
    @Test fun eineUntergruppeVerteiltSichRundUmDenTipppunkt() {
        val einträge = (0..15).map { PieEintrag("$it", "Gruppe", vorlage("K$it")) }
        val layout = planePieFächer(einträge, emptyMap(), 1f, -90f)
        val quadranten = layout.map { (it.rahmen.center.x > 0) to (it.rahmen.center.y > 0) }.toSet()
        assertEquals(4, quadranten.size)
        assertTrue(layout.none { it.rahmen.contains(Offset.Zero) })
    }
    @Test fun definierteKartenBelegenGetrennteKreishaelften() {
        val einträge = pieKartenEinträge((0..5).map { KartenDaten(name = "K$it") }) { k, _ -> vorlage(k.name) }
        val layout = planePieFächer(einträge, emptyMap(), 1f, 90f)
        assertTrue(layout.filter { it.eintrag.gruppe == "Ein-/Ausgänge" }.all { it.rahmen.right < 0 })
        assertTrue(layout.filter { it.eintrag.gruppe == "Methodenausgang" }.all { it.rahmen.left > 0 })
        assertEquals(4, layout.map { (it.rahmen.center.x > 0) to (it.rahmen.center.y > 0) }.toSet().size)
    }
    @Test fun menueZoomSkaliertGroessenUndAbstaendeGemeinsamOhneNeuordnung() {
        val einträge = (0..7).map { PieEintrag("$it", "G", vorlage("K$it")) }
        val basis = planePieFächer(einträge, emptyMap(), 1f, -90f)
        val skaliert = skalierePiePlätze(basis, .5f)
        basis.zip(skaliert).forEach { (a, b) ->
            assertEquals(a.eintrag, b.eintrag)
            assertEquals(a.rahmen.center * .5f, b.rahmen.center)
            assertEquals(a.rahmen.width * .5f, b.rahmen.width)
        }
    }
    @Test fun pinchHaeltDenGegriffenenMenuepunktAuchAnZoomgrenzenFest() {
        val position = Offset(123f, 98f)
        val finger = Offset(345f, 286f)
        val pan = Offset(30f, -15f)
        for ((alt, faktor) in listOf(1f to .5f, 1f to 2f, 3f to 2f, .3f to .1f)) {
            val neu = transformierePie(position, alt, finger, pan, faktor)
            val lokal = (finger - position) / alt
            val danach = neu.position + lokal * neu.zoom
            assertEquals(finger.x + pan.x, danach.x, .001f)
            assertEquals(finger.y + pan.y, danach.y, .001f)
            assertTrue(neu.zoom in .2f..4f)
        }
    }
    @Test fun kreiseSindKonzentrischUndBeiVerschiedenenGroessenKollisionsfrei() {
        val einträge = (0..24).map { PieEintrag("$it", "G", vorlage("K$it", GraphGröße(120f + it * 7, 80f + it * 4))) }
        for (zoom in listOf(.25f, 1f, 3.5f)) {
            val plätze = planePieKreise(einträge, emptyMap(), zoom)
            assertEquals(plätze, planePieKreise(einträge, emptyMap(), zoom))
            plätze.chunked(8).forEach { ring ->
                val radius = ring.first().rahmen.center.getDistance()
                ring.forEach { assertEquals(radius, it.rahmen.center.getDistance(), .01f) }
            }
            plätze.forEachIndexed { i, p ->
                assertEquals(p.eintrag.vorlage.standardGröße.breite * zoom, p.rahmen.width, .01f)
                plätze.drop(i + 1).forEach { assertFalse(p.rahmen.inflate(24f).overlaps(it.rahmen)) }
            }
        }
        assertTrue(planePieKreise(emptyList(), emptyMap(), 1f).isEmpty())
    }
    @Test fun gemesseneVorschaugroesseBestimmtLayoutOhneVerkleinerung() {
        val e = PieEintrag("a", "G", vorlage("A"))
        val p = planePieFächer(listOf(e), mapOf("a" to GraphGröße(600f, 420f)), 3.5f, -90f).single()
        assertEquals(2100f, p.rahmen.width, .001f)
        assertEquals(1470f, p.rahmen.height, .001f)
    }
    @Test fun griffpunktBleibtBeiZoomDichteUndKameraversatzErhalten() {
        for (zoom in listOf(.25f, 1f, 3.5f)) for (dichte in listOf(1f, 2.5f)) {
            val ansicht = AnsichtsFenster(GraphPunkt(123f, -67f), zoom)
            val griff = GraphPunkt(45f, 21f)
            val erwartet = GraphPunkt(80f, 110f)
            val zeiger = Offset((erwartet.x + griff.x) * zoom * dichte + 123f, (erwartet.y + griff.y) * zoom * dichte - 67f)
            val position = pieWeltPosition(zeiger, griff, ansicht, dichte)
            assertEquals(erwartet.x, position.x, .001f); assertEquals(erwartet.y, position.y, .001f)
        }
    }
    @Test fun ringTrefferBleibtAuchAusserhalbDesBildschirmsAmAufrufpunkt() {
        val mitte = Offset(2f, 3f)
        assertEquals(0, pieKategorieAmPunkt(Offset(25f, -97f), mitte, 12, 64f, 176f))
        assertNull(pieKategorieAmPunkt(mitte, mitte, 12, 64f, 176f))
        assertNull(pieKategorieAmPunkt(Offset(300f, 300f), mitte, 12, 64f, 176f))
    }
    @Test fun jedeKarteErscheintMitGleicherVersionInBeidenHaelften() {
        val karten = listOf(KartenDaten(name = "Z", version = 7), KartenDaten(name = "A", version = 3))
        val einträge = pieKartenEinträge(karten) { k, modus ->
            vorlage("${k.name}/${modus.name}").copy(kartenVerweis = KartenVerweis(k.id, k.version))
        }
        assertEquals(4, einträge.size)
        assertEquals(4, einträge.map { it.id }.distinct().size)
        assertEquals(listOf("Ein-/Ausgänge", "Methodenausgang"), einträge.map { it.gruppe }.distinct())
        karten.forEach { k -> assertEquals(2, einträge.count { it.vorlage.kartenVerweis == KartenVerweis(k.id, k.version) }) }
    }
    @Test fun geplanteEintraegeUndAndereKategorienWerdenNichtAlsKnotenAngeboten() {
        val v = vorlage("A")
        fun e(id: String, k: String, verfügbar: Boolean) = KonzeptBibliothekEintrag(id, id, "", listOf(listOf(k)), emptySet(),
            if (verfügbar) KonzeptVerfügbarkeit.Verfügbar else KonzeptVerfügbarkeit.Geplant, v)
        val liste = listOf(e("ja", "analysis", true), e("geplant", "analysis", false), e("anders", "logik", true))
        assertEquals(listOf("ja"), pieKonzeptEinträge(liste, "analysis").map { it.id })
    }
    @Test fun rekursiveFallbacksAktiveUndArchivierteKartenSindAusgeschlossen() {
        val aktiv = KartenDaten(name = "aktiv")
        val indirekt = KartenDaten(name = "indirekt", knoten = listOf(KnotenDaten(art = "test", name = "Fallback",
            eingangsKartenVerweise = mapOf("x" to KartenVerweis(aktiv.id, aktiv.version)))))
        val kandidat = KartenDaten(name = "Kandidat", knoten = listOf(KnotenDaten(art = "test", name = "Gruppe",
            kartenVerweis = KartenVerweis(indirekt.id, indirekt.version))))
        val quelle = mapOf(KartenVerweis(indirekt.id, indirekt.version) to indirekt)
        assertFalse(istKarteAlsKnotenZulässig(aktiv, aktiv.id, quelle::get))
        assertFalse(istKarteAlsKnotenZulässig(kandidat, aktiv.id, quelle::get))
        assertFalse(istKarteAlsKnotenZulässig(indirekt.copy(archiviert = true), aktiveId = KartenId("andere"), lade = quelle::get))
        assertTrue(istKarteAlsKnotenZulässig(KartenDaten(name = "leer"), aktiv.id, quelle::get))
    }

}

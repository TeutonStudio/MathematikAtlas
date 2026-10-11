package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import kotlin.test.Test
import kotlin.test.assertFailsWith

class KartenImportValidierungTest {
    private fun knoten() = KnotenDaten(art = "alt", name = "Alt", anschlüsse = listOf(
        AnschlussDaten(name = "ein", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = AnschlussArtId("wert")),
        AnschlussDaten(name = "aus", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = AnschlussArtId("wert")),
    ))
    @Test fun `Pfadtraversal wird vor Speicherzugriff abgewiesen`() {
        listOf("../ausserhalb", "/tmp/karte", "..", "a\\b", "a\u0000b").forEach { id ->
            assertFailsWith<IllegalArgumentException>(id) { prüfeImportKarte(KartenDaten(id = KartenId(id), name = "Angriff")) }
        }
        prüfeImportKarte(KartenDaten(id = KartenId("standard-dynamics-mandelbrot-orbit"), name = "Historisch"))
    }
    @Test fun `Doppelte IDs fehlende Anschluesse und Zyklen werden abgewiesen`() {
        val a = knoten(); val b = knoten()
        fun ref(k: KnotenDaten, index: Int) = AnschlussVerweis(k.id, k.anschlüsse[index].id)
        val karte = KartenDaten(name = "Zyklus", knoten = listOf(a, b), verbindungen = listOf(VerbindungDaten(von = ref(a, 1), zu = ref(b, 0)), VerbindungDaten(von = ref(b, 1), zu = ref(a, 0))))
        assertFailsWith<IllegalArgumentException> { prüfeImportKarte(karte) }
        assertFailsWith<IllegalArgumentException> { prüfeImportKarte(karte.copy(knoten = listOf(a, a))) }
        assertFailsWith<IllegalArgumentException> { prüfeImportKarte(karte.copy(knoten = listOf(a))) }
    }
    @Test fun `Neutrale Altanschluesse und historische Gruppengroesse bleiben ladbar`() {
        val a = knoten().let { it.copy(anschlüsse = it.anschlüsse.map { p -> p.copy(richtung = AnschlussRichtung.Neutral) }) }
        val b = a.copy(id = neueKnotenId())
        prüfeImportKarte(KartenDaten(name = "Alt", knoten = listOf(a, b), verbindungen = listOf(VerbindungDaten(von = AnschlussVerweis(a.id, a.anschlüsse[0].id), zu = AnschlussVerweis(b.id, b.anschlüsse[0].id))), visuelleGruppen = listOf(VisuelleKnotenGruppeDaten(knotenIds = setOf(a.id), größe = GraphGröße(0f, 0f)))))
    }
    @Test fun `Paketidentitaeten und Kartenzyklen werden vollstaendig geprueft`() {
        val a = KartenDaten(name = "A")
        prüfeImportPaket(listOf(a, a.copy(version = 2)))
        assertFailsWith<IllegalArgumentException> { prüfeImportPaket(listOf(a, a.copy(name = "Andere Karte"))) }
        val b = KartenDaten(name = "B", knoten = listOf(knoten().copy(kartenVerweis = KartenVerweis(a.id, 1))))
        val zyklischesA = a.copy(knoten = listOf(knoten().copy(kartenVerweis = KartenVerweis(b.id, 1))))
        assertFailsWith<IllegalArgumentException> { prüfeImportPaket(listOf(zyklischesA, b)) }
        assertFailsWith<IllegalArgumentException> { prüfeImportPaket(listOf(b)) }
        prüfeImportPaket(listOf(a, b))
    }
}

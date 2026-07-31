package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KartenFreigabePaketTest {
    @Test
    fun `Freigabe enthält direkte und indirekte Kartenabhängigkeiten`() {
        val basis = KartenDaten(name = "Basis", version = 3)
        val mitte = KartenDaten(
            name = "Mitte",
            version = 2,
            knoten = listOf(KnotenDaten(art = "gruppe", name = "Basis", kartenVerweis = KartenVerweis(basis.id, basis.version))),
        )
        val wurzel = KartenDaten(
            name = "Wurzel",
            version = 4,
            knoten = listOf(KnotenDaten(art = "gruppe", name = "Mitte", kartenVerweis = KartenVerweis(mitte.id, mitte.version))),
        )
        val karten = listOf(wurzel, mitte, basis).associateBy { KartenVerweis(it.id, it.version) }
        val profil = LokalesProfil(ProfilId("profil-1"), "Ada")

        val text = KartenFreigabePaket.erstelle(
            name = wurzel.name,
            art = FreigabeArt.Karte,
            wurzelKarten = listOf(wurzel),
            ordnung = KartenOrdnung(),
            profil = profil,
            lade = karten::get,
        )
        val gelesen = KartenFreigabePaket.lese(text)

        assertEquals(3, gelesen.karten.size)
        assertEquals(setOf(wurzel.id, mitte.id, basis.id), gelesen.karten.mapTo(mutableSetOf()) { it.id })
        assertEquals("Ada", gelesen.quelle.herausgeberPseudonym)
        assertEquals(listOf(KartenVerweis(wurzel.id, wurzel.version)), gelesen.wurzeln)
    }

    @Test
    fun `Sammlungsfreigabe bewahrt relative Ordnerstruktur`() {
        val karte = KartenDaten(name = "Ableitung")
        val ordnung = KartenOrdnung()
            .mitOrdner(listOf("Analysis", "Differentialrechnung"))
            .mitKarteInOrdner(karte.id, listOf("Analysis", "Differentialrechnung"))

        val text = KartenFreigabePaket.erstelle(
            name = "Analysis",
            art = FreigabeArt.Sammlung,
            wurzelKarten = listOf(karte),
            ordnung = ordnung,
            sammlungsPfad = listOf("Analysis"),
            profil = LokalesProfil(ProfilId("profil-1"), "Ada"),
            lade = { verweis -> karte.takeIf { verweis == KartenVerweis(karte.id, karte.version) } },
        )
        val gelesen = KartenFreigabePaket.lese(text)

        assertEquals(listOf("Differentialrechnung"), gelesen.kartenPfade.getValue(karte.id))
        assertTrue(listOf("Differentialrechnung") in gelesen.ordnerPfade)
    }

    @Test
    fun `fehlende Abhängigkeit verhindert unvollständige Freigabe`() {
        val fehlend = KartenVerweis(KartenId("fehlend"), 1)
        val wurzel = KartenDaten(
            name = "Unvollständig",
            knoten = listOf(KnotenDaten(art = "gruppe", name = "Fehlt", kartenVerweis = fehlend)),
        )

        assertFailsWith<IllegalArgumentException> {
            KartenFreigabePaket.erstelle(
                name = wurzel.name,
                art = FreigabeArt.Karte,
                wurzelKarten = listOf(wurzel),
                ordnung = KartenOrdnung(),
                profil = LokalesProfil(ProfilId("profil-1"), "Ada"),
                lade = { verweis -> wurzel.takeIf { verweis == KartenVerweis(wurzel.id, wurzel.version) } },
            )
        }
    }
}

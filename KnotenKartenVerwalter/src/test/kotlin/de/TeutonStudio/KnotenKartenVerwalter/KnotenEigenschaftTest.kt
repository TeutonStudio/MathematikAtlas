package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import kotlin.test.*

class KnotenEigenschaftTest {
    @Test fun `Eigenschaften und Objektfelder werden atomar geändert`() {
        val knoten = KnotenDaten(art = "test", name = "Test")
        val karte = KartenDaten(name = "Karte", knoten = listOf(knoten))
        val konfiguration = KnotenEigenschaft.Objekt(mapOf(
            "kamera" to KnotenEigenschaft.Objekt(mapOf("zoom" to KnotenEigenschaft.Dezimalzahl(1.0))),
            "farben" to KnotenEigenschaft.Liste(listOf(KnotenEigenschaft.Farbe(0xFF0000FF))),
        ))
        val mitKonfiguration = karte.wendeAn(KartenAktion.KnotenEigenschaftÄndern(knoten.id, "visualisierung", konfiguration))
        val geändert = mitKonfiguration.wendeAn(KartenAktion.KnotenObjektEigenschaftFeldÄndern(knoten.id, "visualisierung", "dimension", KnotenEigenschaft.Text("R3")))
        val eigenschaft = geändert.knoten.single().eigenschaften.getValue("visualisierung") as KnotenEigenschaft.Objekt
        assertEquals(KnotenEigenschaft.Text("R3"), eigenschaft.felder["dimension"])
        assertEquals(KnotenEigenschaft.Liste(listOf(KnotenEigenschaft.Farbe(0xFF0000FF))), eigenschaft.felder["farben"])
    }
}

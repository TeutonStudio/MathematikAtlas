package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnsichtsFenster
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KartenGeometrieTest {
    @Test fun sichtbereichWirdAusPixelkameraInWeltKoordinatenZurückgerechnet() {
        val bereich = sichtbarerWeltBereich(
            ansicht = AnsichtsFenster(verschiebung = GraphPunkt(-100f, 50f), zoom = 2f),
            anzeigeGröße = IntSize(400, 200),
            dichte = 2f,
        )!!

        assertEquals(25f, bereich.left)
        assertEquals(-12.5f, bereich.top)
        assertEquals(125f, bereich.right)
        assertEquals(37.5f, bereich.bottom)
    }

    @Test fun knotenBleibtBisZumViewportPufferSichtbar() {
        val viewport = Rect(0f, 0f, 100f, 100f)
        val amPuffer = KnotenDaten(
            art = "test", name = "sichtbar", position = GraphPunkt(300f, 0f), größe = GraphGröße(20f, 20f),
        )
        val außerhalb = amPuffer.copy(position = GraphPunkt(301f, 0f))

        assertTrue(amPuffer.istImBereich(viewport, puffer = 200f))
        assertFalse(außerhalb.istImBereich(viewport, puffer = 200f))
    }

    @Test fun minimapProjektionIstUmkehrbar() {
        val projektion = MiniMapProjektion(
            grenzen = Rect(-100f, -50f, 300f, 250f),
            größe = Size(180f, 120f),
        )
        val welt = Offset(120f, 80f)

        val zurück = projektion.zuWelt(projektion.zuMiniMap(welt))

        assertEquals(welt.x, zurück.x, absoluteTolerance = 0.001f)
        assertEquals(welt.y, zurück.y, absoluteTolerance = 0.001f)
    }

    @Test fun punktDirektAufBezierkurveWirdErkannt() {
        val geometrie = berechneVerbindungsGeometrie(
            start = Offset(0f, 0f),
            ende = Offset(0f, 100f),
            mindestKontrollAbstand = 72f,
        )
        val punktAufKurve = geometrie.punktBei(.25f)

        assertTrue(geometrie.abstandZu(punktAufKurve) < .01f)
    }

    @Test fun punktAufGedachterSehneAberFernDerKurveWirdNichtErkannt() {
        val geometrie = berechneVerbindungsGeometrie(
            start = Offset(0f, 0f),
            ende = Offset(0f, 100f),
            mindestKontrollAbstand = 72f,
        )

        assertTrue(geometrie.abstandZu(Offset(0f, 25f)) > 12f)
    }

    @Test fun trefferbereichUnterscheidetInnenUndAußenEntlangDerKurve() {
        val geometrie = berechneVerbindungsGeometrie(
            start = Offset(20f, 30f),
            ende = Offset(280f, 160f),
            mindestKontrollAbstand = 72f,
        )
        val mitte = geometrie.punktBei(.5f)

        assertTrue(geometrie.abstandZu(mitte + Offset(0f, 11f)) <= 12f)
        assertTrue(geometrie.abstandZu(mitte + Offset(0f, 18f)) > 12f)
    }

    @Test fun identischeEndpunkteErzeugenEndlicheGeometrie() {
        val geometrie = berechneVerbindungsGeometrie(
            start = Offset(40f, 40f),
            ende = Offset(40f, 40f),
            mindestKontrollAbstand = 72f,
        )
        val abstand = geometrie.abstandZu(Offset(42f, 40f))

        assertTrue(abstand.isFinite())
        assertTrue(geometrie.punktBei(.5f).x.isFinite())
        assertTrue(geometrie.punktBei(.5f).y.isFinite())
    }

    @Test fun kontrollpunktUmhuellungDecktStarkGekruemmteKurveAb() {
        val geometrie = berechneVerbindungsGeometrie(
            start = Offset(100f, 100f),
            ende = Offset(20f, 180f),
            mindestKontrollAbstand = 72f,
        )

        (0..20).forEach { index ->
            val punkt = geometrie.punktBei(index / 20f)
            assertTrue(punkt.x in geometrie.umhüllung.left..geometrie.umhüllung.right)
            assertTrue(punkt.y in geometrie.umhüllung.top..geometrie.umhüllung.bottom)
        }
    }

    @Test fun vorschauVomAusgangBehältDenFestenAnschlussAlsQuelle() {
        val fest = Offset(20f, 30f)
        val zeiger = Offset(240f, 150f)

        val endpunkte = normalisiereVerbindungsVorschauEndpunkte(fest, zeiger, AnschlussRichtung.Ausgang)

        assertEquals(fest, endpunkte.quelle)
        assertEquals(zeiger, endpunkte.ziel)
    }

    @Test fun vorschauVomEingangZeichnetDenZeigerAlsVorläufigeQuelle() {
        val eingang = Offset(240f, 150f)
        val zeiger = Offset(20f, 30f)

        val endpunkte = normalisiereVerbindungsVorschauEndpunkte(eingang, zeiger, AnschlussRichtung.Eingang)

        assertEquals(zeiger, endpunkte.quelle)
        assertEquals(eingang, endpunkte.ziel)
        val geometrie = berechneVerbindungsGeometrie(endpunkte.quelle, endpunkte.ziel, 72f)
        assertTrue(geometrie.kontrollpunkt1.x > geometrie.start.x)
        assertTrue(geometrie.kontrollpunkt2.x < geometrie.ende.x)
    }

    @Test fun unbekannteRichtungFälltDeterministischAufAusgangsrichtungZurück() {
        val fest = Offset(10f, 10f)
        val zeiger = Offset(40f, 40f)

        val endpunkte = normalisiereVerbindungsVorschauEndpunkte(fest, zeiger, null)

        assertEquals(VerbindungsVorschauEndpunkte(fest, zeiger), endpunkte)
    }
}

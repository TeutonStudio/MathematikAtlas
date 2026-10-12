package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import kotlin.test.*

class FormelZeichnungTest {
    private fun p(x: Double, y: Double) = FormelStiftPunkt(x, y)
    private fun strich(id: String, vararg punkte: FormelStiftPunkt, radieren: Boolean = false) =
        FormelVektorStrich(id, punkte.toList(), breite = if (radieren) 20.0 else 2.0, radierung = radieren)

    @Test fun `schneller Radierzug loescht auch zwischen seinen Stuetzpunkten`() {
        val original = strich("linie", p(0.0,256.0), p(1024.0,256.0))
        val radierer = strich("radierer", p(512.0,0.0), p(512.0,512.0), radieren = true)
        val zeichnung = FormelZeichnung(listOf(original, radierer))
        val sichtbar = zeichnung.sichtbareFragmente()
        assertEquals(2, sichtbar.size)
        assertEquals(p(0.0,256.0), sichtbar.first().punkte.first())
        assertEquals(p(1024.0,256.0), sichtbar.last().punkte.last())
        assertTrue(sichtbar.first().punkte.last().x <= 501.00001)
        assertTrue(sichtbar.last().punkte.first().x >= 522.99999)
        assertEquals(listOf(original,radierer), zeichnung.striche)
    }

    @Test fun `spaetere Zeichnung bleibt ueber altem Radierer sichtbar und Punkte sind radierbar`() {
        val punkt = strich("punkt", p(512.0,256.0))
        val radierer = strich("radierer", p(512.0,256.0), radieren = true)
        assertTrue(FormelZeichnung(listOf(punkt,radierer)).sichtbareFragmente().isEmpty())
        assertEquals(listOf(punkt), FormelZeichnung(listOf(radierer,punkt)).sichtbareFragmente())
    }

    @Test fun `eine Geste ist ein Undo und Abbruch veraendert Original nicht`() {
        val original = FormelZeichnung(listOf(strich("a",p(1.0,2.0)))).eigenschaft()
        val entwurf = FormelZeichnungsEntwurf(original)
        entwurf.geste(strich("b",p(10.0,20.0),p(30.0,40.0)))
        assertEquals(2, entwurf.zeichnung.striche.size)
        assertEquals(1, FormelZeichnung.lese(original).striche.size)
        entwurf.rückgängig()
        assertEquals(original, entwurf.übernehmen())
        entwurf.wiederholen()
        assertEquals(2, FormelZeichnung.lese(entwurf.übernehmen()).striche.size)
    }

    @Test fun `unbekannte Version bleibt unveraendert und Bearbeitung wird diagnostiziert`() {
        val original = KnotenEigenschaft.Objekt(mapOf("version" to KnotenEigenschaft.Ganzzahl(99),"unbekannt" to KnotenEigenschaft.Text("bewahren")))
        val entwurf = FormelZeichnungsEntwurf(original)
        assertNotNull(entwurf.diagnose)
        assertFails { entwurf.geste(strich("a",p(1.0,2.0))) }
        assertEquals(original, entwurf.übernehmen())
    }

    @Test fun `Grenzen und Darstellungsbudget sind kontrolliert`() {
        assertFails { p(Double.NaN,0.0) }
        assertFails { p(0.0,513.0) }
        assertFails { strich("a",p(1.0,2.0)).copy(breite = Double.POSITIVE_INFINITY) }
        val zeichnung = FormelZeichnung(listOf(strich("a",p(0.0,0.0),p(100.0,100.0)), strich("r",p(50.0,50.0),radieren=true)))
        assertFailsWith<IllegalStateException> { zeichnung.sichtbareFragmente(maxPrüfungen=0) }
        assertEquals(zeichnung, FormelZeichnung.lese(zeichnung.eigenschaft()))
    }

    @Test fun `JSON und Kopieren erhalten Striche Radierungen und getrennte Interpretation`() {
        val zeichnung = FormelZeichnung(listOf(strich("a",p(1.0,2.0)),strich("r",p(5.0,6.0),radieren=true)))
        val knoten = ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero).copy(eigenschaften = mapOf(
            FORMEL_ZEICHNUNG_EIGENSCHAFT to zeichnung.eigenschaft(), FORMEL_AST_EIGENSCHAFT to KnotenEigenschaft.Text("separater Baum"),
            FORMEL_INTERPRETATION_EIGENSCHAFT to KnotenEigenschaft.Text("x+1")))
        val karte = KartenDaten(name="Stift",knoten=listOf(knoten))
        val geladen = MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(karte)).knoten.single()
        assertEquals(knoten.eigenschaften,geladen.eigenschaften)
        val container=MatlasKartenContainer.lese(MatlasKartenContainer.schreibe(karte,"test"))
        assertEquals(knoten.eigenschaften,container.knoten.single().eigenschaften)
        assertEquals(zeichnung,FormelZeichnung.lese(geladen.eigenschaften[FORMEL_ZEICHNUNG_EIGENSCHAFT]))
        assertEquals(knoten.eigenschaften,knoten.copy(id=KnotenId("kopie")).eigenschaften)
    }
    @Test fun `Rueckwege und geschlossene Konturen bleiben im Originalpfad erhalten`() {
        val a=p(1.0,2.0);val b=p(20.0,30.0);val c=p(50.0,70.0)
        assertEquals(listOf(a,b,a),verdichteFormelStiftPunkte(listOf(a,a,b,b,a)))
        val kontur=listOf(a,b,c,a)
        val zeichnung=FormelZeichnung(listOf(strich("geschlossen",*verdichteFormelStiftPunkte(kontur).toTypedArray())))
        assertEquals(kontur,FormelZeichnung.lese(zeichnung.eigenschaft()).striche.single().punkte)
    }

}

package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GeometrieTeilobjektKnotenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()
    private val raum = EuklidischerRaum("E", 2)
    private val a = GeometriePunkt("A", raum)
    private val b = GeometriePunkt("B", raum)
    private val c = GeometriePunkt("C", raum)
    private val polygon = GeometriePolygon(listOf(a, b, c))

    @Test
    fun `Ecke von gibt die gewählte Polygonecke aus`() {
        val ergebnis = auswerten(GeometrieTeilobjektTyp.Ecke, polygon, "p1")
        assertSame(b, ergebnis.ausgaben.getValue("ecke").objekt)
    }

    @Test
    fun `Kante von gibt die gewählte Polygonkante aus`() {
        val ergebnis = auswerten(GeometrieTeilobjektTyp.Kante, polygon, "e2")
        assertEquals(
            GeometrieStrecke(c, a),
            assertIs<GeometrieStrecke>(ergebnis.ausgaben.getValue("kante").objekt),
        )
    }

    @Test
    fun `Fläche von gibt die Polygonfläche aus`() {
        val ergebnis = auswerten(GeometrieTeilobjektTyp.Fläche, polygon, "f0")
        assertSame(polygon, ergebnis.ausgaben.getValue("fläche").objekt)
    }

    @Test
    fun `Gruppen behalten präfixierte stabile Zell IDs`() {
        val gruppe = GeometrieGruppe(listOf(GeometrieStrecke(a, b), polygon))
        val ergebnis = auswerten(GeometrieTeilobjektTyp.Ecke, gruppe, "g1_p2")
        assertSame(c, ergebnis.ausgaben.getValue("ecke").objekt)
    }

    @Test
    fun `Ungültige gespeicherte Zell ID fällt deterministisch zurück`() {
        val ergebnis = auswerten(GeometrieTeilobjektTyp.Kante, polygon, "nicht-mehr-da")
        assertEquals(GeometrieStrecke(a, b), ergebnis.ausgaben.getValue("kante").objekt)
        assertTrue(ergebnis.warnungen.single().contains("existiert nicht mehr"))
    }

    @Test
    fun `Fehlende Zelldimension meldet verständlichen Fehler`() {
        assertFailsWith<IllegalArgumentException> {
            auswerten(GeometrieTeilobjektTyp.Kante, a, "")
        }
    }

    @Test
    fun `Konkrete Geometrien werden auf konkrete Anschlussarten abgebildet`() {
        assertEquals(GeometrieAnschlussArten.Punkt.id, geometrieAnschlussArt(a))
        assertEquals(GeometrieAnschlussArten.Strecke.id, geometrieAnschlussArt(GeometrieStrecke(a, b)))
        assertEquals(GeometrieAnschlussArten.Polygon.id, geometrieAnschlussArt(polygon))
    }

    @Test
    fun `Alle Teilobjektvorlagen besitzen Objekt Eingang und passenden Ausgang`() {
        val vorlagen = mapOf(
            GeometrieTeilobjektTyp.Ecke to GeometrieKnotenVorlagen.EckeVon,
            GeometrieTeilobjektTyp.Kante to GeometrieKnotenVorlagen.KanteVon,
            GeometrieTeilobjektTyp.Fläche to GeometrieKnotenVorlagen.FlächeVon,
        )
        vorlagen.forEach { (typ, vorlage) ->
            val eingang = vorlage.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang }
            val ausgang = vorlage.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }
            assertEquals(GeometrieAnschlussArten.Objekt.id, eingang.art)
            assertEquals(typ.ausgangName, ausgang.name)
            assertTrue(register.finde(vorlage.art) != null)
        }
    }

    private fun auswerten(
        typ: GeometrieTeilobjektTyp,
        objekt: GeometrischerAusdruck,
        zellId: String,
    ): KnotenAuswertungsErgebnis {
        val knoten = vorlage(typ)
            .erzeuge(GraphPunkt.Zero)
            .mitZellId(zellId)
        return register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf("objekt" to BedingterWert(objekt)),
                rechenKontext = RechenKontext(),
            ),
        )
    }

    private fun vorlage(typ: GeometrieTeilobjektTyp) = when (typ) {
        GeometrieTeilobjektTyp.Ecke -> GeometrieKnotenVorlagen.EckeVon
        GeometrieTeilobjektTyp.Kante -> GeometrieKnotenVorlagen.KanteVon
        GeometrieTeilobjektTyp.Fläche -> GeometrieKnotenVorlagen.FlächeVon
    }

    private fun KnotenDaten.mitZellId(zellId: String): KnotenDaten =
        copy(parameter = parameter + (GEOMETRIE_TEILOBJEKT_ZELL_ID to zellId))
}

package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.AussageStatus
import de.TeutonStudio.MathematikRechenSystem.kern.DefinierteMenge
import de.TeutonStudio.MathematikRechenSystem.kern.EigenschaftsAussage
import de.TeutonStudio.MathematikRechenSystem.kern.GanzeZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.NatürlicheZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import de.TeutonStudio.MathematikRechenSystem.kern.ReelleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.ReellesIntervall
import de.TeutonStudio.MathematikRechenSystem.kern.UnterstuetzungsStatus
import de.TeutonStudio.MathematikRechenSystem.kern.Variable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MathematischeEigenschaftenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()
    private val x = Variable("x")

    @Test
    fun `alle konsolidierten Eigenschaftsknoten sind erzeugbar und auswertbar`() {
        val arten = alleMathematikKnotenVorlagen().map { it.art }.toSet()

        MathematischeEigenschaftKnotenVorlagen.alle.forEach { vorlage ->
            assertTrue(vorlage.art in arten)
            assertNotNull(register.finde(vorlage.art), "Auswerter für ${vorlage.art} fehlt.")
        }
    }

    @Test
    fun `Unterstützung und Wahrheitsstatus bleiben getrennt`() {
        val aussage = EigenschaftsAussage(
            eigenschaftId = "konvex",
            eigenschaftLatex = "konvex",
            subjektLatex = "f",
            unterstuetzung = UnterstuetzungsStatus.NOCH_NICHT_IMPLEMENTIERT,
            aussageStatus = AussageStatus.UNENTSCHEIDBAR,
        )

        val ergebnis = aussage.entscheide()

        assertEquals(null, ergebnis.wahrheitswert)
        assertFalse(ergebnis.status.toString().contains("Widerlegt"))
    }

    @Test
    fun `Folgenarten verwenden N und Z statt eines unnatürlichen Tupels`() {
        val einseitig = Methode(
            name = "a",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to NatürlicheZahlen),
        )
        val zweiseitig = einseitig.copy(name = "b", werteVorräte = mapOf(x.name to GanzeZahlen))

        assertEquals(FolgenArt.Einseitig, FolgenArt.aus(einseitig))
        assertEquals(FolgenArt.Zweiseitig, FolgenArt.aus(zweiseitig))
        assertEquals(FolgenArt.Einseitig, FolgenArt.ausHistorischerKennung("unnatürlichesTupel"))
    }

    @Test
    fun `Methodensignatur bewahrt stabile Argumentrollen`() {
        val y = Variable("y")
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen, y.name to ReelleZahlen),
        )

        val einzeln = MethodenSignaturAnsicht.von(methode, ArgumentAnsicht.EinzelArgumente)
        val koordinaten = MethodenSignaturAnsicht.von(methode, ArgumentAnsicht.Koordinaten)

        assertEquals(2, einzeln.stelligkeit)
        assertEquals(einzeln.rollen.map { it.stabileId }, koordinaten.rollen.map { it.stabileId })
        assertEquals(listOf("x", "y"), einzeln.rollen.map { it.sichtbarerName })
    }

    @Test
    fun `Mengeneigenschaften werden relativ zum reellen Umgebungsraum entschieden`() {
        val intervall = ReellesIntervall(
            links = RationaleZahl.von(0),
            linksOffen = true,
            rechts = RationaleZahl.von(1),
            rechtsOffen = true,
        )
        val knoten = MathematischeEigenschaftKnotenVorlagen.MengenEigenschaft
            .erzeuge(GraphPunkt.Zero)
            .copy(parameter = MathematischeEigenschaftKnotenVorlagen.MengenEigenschaft.standardParameter + (EIGENSCHAFT_PARAMETER to "offen"))

        val ergebnis = register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf("menge" to BedingterWert(intervall)),
                rechenKontext = RechenKontext(),
            ),
        )

        val aussage = assertIs<EigenschaftsAussage>(ergebnis.ausgaben.getValue("aussage").objekt)
        assertEquals(AussageStatus.BEWIESEN, aussage.aussageStatus)
        assertEquals(UnterstuetzungsStatus.IMPLEMENTIERT, aussage.unterstuetzung)
    }

    @Test
    fun `Analysis Eigenschaft liefert bei fehlendem Beweis eine symbolische Stellenmenge`() {
        val y = Variable("y")
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen, y.name to ReelleZahlen),
        )
        val knoten = MathematischeEigenschaftKnotenVorlagen.AnalysisEigenschaft.erzeuge(GraphPunkt.Zero)

        val ergebnis = register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf("methode" to BedingterWert(methode)),
                rechenKontext = RechenKontext(),
            ),
        )

        val menge = assertIs<DefinierteMenge>(ergebnis.ausgaben.getValue("stellenmenge").objekt)
        val aussage = assertIs<EigenschaftsAussage>(menge.bedingung)
        assertEquals(MathematischeEigenschaftRegister.Extremum.id, aussage.eigenschaftId)
        assertEquals(UnterstuetzungsStatus.IMPLEMENTIERT, aussage.unterstuetzung)
        assertEquals(AussageStatus.UNENTSCHEIDBAR, aussage.aussageStatus)
    }

    @Test
    fun `globale und lokale Konvexität verwenden dieselbe Begriffskennung`() {
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )

        val global = KonvexitaetsKern.globaleAussage(methode, MathematischeEigenschaftRegister.Konvex, streng = false)
        val lokal = assertIs<DefinierteMenge>(
            KonvexitaetsKern.lokaleMenge(methode, MathematischeEigenschaftRegister.Konvexitaetsbereich, streng = false),
        )
        val lokaleAussage = assertIs<EigenschaftsAussage>(lokal.bedingung)

        assertEquals("konvex", global.eigenschaftId)
        assertEquals("konvexitaetsbereich", lokaleAussage.eigenschaftId)
        assertTrue(global.diagnose!!.nachricht.contains("Jensen"))
        assertTrue(lokaleAussage.diagnose!!.nachricht.contains("Jensen"))
    }

    @Test
    fun `automatische Adjektive sind stabil geordnet und nicht redundant`() {
        val methode = Methode(
            name = "a",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to NatürlicheZahlen),
        )

        val ids = automatischeAdjektive(methode).map { it.eigenschaftId }

        assertEquals(listOf("einstellig", "einseitige-folge", "reellwertig"), ids)
        assertFalse("komplexwertig" in ids)
    }
}

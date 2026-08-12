package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.AussageStatus
import de.TeutonStudio.MathematikRechenSystem.kern.DiskreteTopologie
import de.TeutonStudio.MathematikRechenSystem.kern.EigenschaftsAussage
import de.TeutonStudio.MathematikRechenSystem.kern.EndlicheMenge
import de.TeutonStudio.MathematikRechenSystem.kern.GanzeZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.IndiskreteTopologie
import de.TeutonStudio.MathematikRechenSystem.kern.LeereMenge
import de.TeutonStudio.MathematikRechenSystem.kern.MathematischesObjekt
import de.TeutonStudio.MathematikRechenSystem.kern.MengenAusdruck
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.NatürlicheZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import de.TeutonStudio.MathematikRechenSystem.kern.ReelleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.TopologischerRaum
import de.TeutonStudio.MathematikRechenSystem.kern.UnterstuetzungsStatus
import de.TeutonStudio.MathematikRechenSystem.kern.Variable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FolgenSignaturUndMengenEigenschaftenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()
    private val n = Variable("n")

    @Test
    fun `Persistenzwert halbfolge erkennt N0 Indexierung`() {
        val folge = methode("a", n, NatürlicheZahlen)

        val aussage = prüfeFolge(folge, "halbfolge")

        assertEquals(AussageStatus.BEWIESEN, aussage.aussageStatus)
        assertTrue(aussage.diagnose!!.nachricht.contains("\\mathbb{N}"))
    }

    @Test
    fun `Zweiseitige Folge verwendet ganzzahlige Indexmenge`() {
        val folge = methode("b", n, GanzeZahlen)

        assertEquals(AussageStatus.BEWIESEN, prüfeFolge(folge, "zweiseitig").aussageStatus)
        assertEquals(AussageStatus.WIDERLEGT, prüfeFolge(folge, "halbfolge").aussageStatus)
    }

    @Test
    fun `Stelligkeit bleibt bei geänderter Ansicht stabil`() {
        val x = Variable("x")
        val y = Variable("y")
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen, y.name to ReelleZahlen),
        )
        val einzeln = prüfeStelligkeit(methode, "mehrstellig", ArgumentAnsicht.EinzelArgumente)
        val tupel = prüfeStelligkeit(methode, "mehrstellig", ArgumentAnsicht.Tupel)

        assertEquals(AussageStatus.BEWIESEN, einzeln.aussageStatus)
        assertEquals(AussageStatus.BEWIESEN, tupel.aussageStatus)
        assertTrue(einzeln.diagnose!!.nachricht.contains("argument.0.x"))
        assertTrue(tupel.diagnose!!.nachricht.contains("argument.1.y"))
    }

    @Test
    fun `Diskrete Topologie macht jede Menge offen`() {
        val menge = EndlicheMenge(setOf(RationaleZahl.Null, RationaleZahl.Eins))
        val traeger = EndlicheMenge(setOf(RationaleZahl.Null, RationaleZahl.Eins, RationaleZahl.von(2)))
        val raum = TopologischerRaum(traeger, DiskreteTopologie(traeger))

        val aussage = prüfeMenge(menge, "offen", raum = raum)

        assertEquals(AussageStatus.BEWIESEN, aussage.aussageStatus)
    }

    @Test
    fun `Indiskrete Topologie unterscheidet Teilmenge und Umgebungsraum`() {
        val teilmenge = EndlicheMenge(setOf(RationaleZahl.Null))
        val traeger = EndlicheMenge(setOf(RationaleZahl.Null, RationaleZahl.Eins))
        val raum = TopologischerRaum(traeger, IndiskreteTopologie(traeger))

        assertEquals(
            AussageStatus.WIDERLEGT,
            prüfeMenge(teilmenge, "offen", raum = raum).aussageStatus,
        )
        assertEquals(
            AussageStatus.BEWIESEN,
            prüfeMenge(traeger, "offen", raum = raum).aussageStatus,
        )
        assertEquals(
            AussageStatus.BEWIESEN,
            prüfeMenge(LeereMenge, "abgeschlossen", raum = raum).aussageStatus,
        )
    }

    @Test
    fun `Konvexe Menge benötigt affine Struktur`() {
        val menge = EndlicheMenge(setOf(RationaleZahl.Null))

        val aussage = prüfeMenge(menge, "konvex", mapOf("affineStruktur" to "keine"))

        assertEquals(UnterstuetzungsStatus.MATHEMATISCH_NICHT_MOEGLICH, aussage.unterstuetzung)
        assertEquals(AussageStatus.UNENTSCHEIDBAR, aussage.aussageStatus)
    }

    private fun prüfeFolge(methode: Methode, eigenschaft: String): EigenschaftsAussage {
        val knoten = MathematischeEigenschaftKnotenVorlagen.FolgenEigenschaft.erzeuge(GraphPunkt.Zero).copy(
            parameter = MathematischeEigenschaftKnotenVorlagen.FolgenEigenschaft.standardParameter +
                (EIGENSCHAFT_PARAMETER to eigenschaft),
        )
        return aussage(knoten, "methode", methode)
    }

    private fun prüfeStelligkeit(
        methode: Methode,
        eigenschaft: String,
        ansicht: ArgumentAnsicht,
    ): EigenschaftsAussage {
        val knoten = MathematischeEigenschaftKnotenVorlagen.MethodenStelligkeit.erzeuge(GraphPunkt.Zero).copy(
            parameter = MathematischeEigenschaftKnotenVorlagen.MethodenStelligkeit.standardParameter + mapOf(
                EIGENSCHAFT_PARAMETER to eigenschaft,
                "argumentAnsicht" to ansicht.name,
            ),
        )
        return aussage(knoten, "methode", methode)
    }

    private fun prüfeMenge(
        menge: MengenAusdruck,
        eigenschaft: String,
        parameter: Map<String, String> = emptyMap(),
        raum: TopologischerRaum? = null,
    ): EigenschaftsAussage {
        val basis = MathematischeEigenschaftKnotenVorlagen.MengenEigenschaft.erzeuge(GraphPunkt.Zero).copy(
            parameter = MathematischeEigenschaftKnotenVorlagen.MengenEigenschaft.standardParameter + parameter +
                (EIGENSCHAFT_PARAMETER to eigenschaft),
        )
        val knoten = konfiguriereMengenEigenschaftKnoten(basis, eigenschaft)
        val eingänge = buildMap {
            put("menge", BedingterWert(menge))
            raum?.let { put("raum", BedingterWert(it)) }
        }
        return assertIs(
            register.finde(knoten.art)!!.auswerten(
                KnotenAuswertungsKontext(
                    knoten = knoten,
                    eingänge = eingänge,
                    rechenKontext = RechenKontext(),
                ),
            ).ausgaben.getValue("aussage").objekt,
        )
    }

    private fun aussage(
        knoten: de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten,
        eingang: String,
        objekt: MathematischesObjekt,
    ): EigenschaftsAussage = assertIs(
        register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf(eingang to BedingterWert(objekt)),
                rechenKontext = RechenKontext(),
            ),
        ).ausgaben.getValue("aussage").objekt,
    )

    private fun methode(
        name: String,
        index: Variable,
        indexMenge: MengenAusdruck,
    ) = Methode(
        name = name,
        parameter = listOf(index),
        vorschrift = index,
        zielMenge = ReelleZahlen,
        werteVorräte = mapOf(index.name to indexMenge),
    )
}

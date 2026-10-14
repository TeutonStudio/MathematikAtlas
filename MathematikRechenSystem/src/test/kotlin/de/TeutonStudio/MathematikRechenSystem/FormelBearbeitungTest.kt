package de.TeutonStudio.MathematikRechenSystem
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*
class FormelBearbeitungTest {
    private val qs=listOf(FormelVariablenQuelle("qx","x",FormelVariablenArt.FREI,FormelTyp.ZAHL,ReelleZahlen),FormelVariablenQuelle("qy","y",FormelVariablenArt.FREI,FormelTyp.ZAHL,ReelleZahlen))
    @Test fun `Neuimport bindet nicht an wiederverwendete Parser IDs`() {
        val e=FormelBearbeitungsEntwurf(FormelLatexCodec.importiereGebunden("x-y",qs))
        e.importiere("y-x")
        assertEquals(RationaleZahl.von(-3),e.geprüft().werteAus(mapOf("qx" to RationaleZahl.von(5),"qy" to RationaleZahl.von(2))).objekt)
    }
    @Test fun `Umbenennen und Reihenfolge behalten Quellen IDs und Bedingungen`() {
        val f=FormelLatexCodec.importiereGebunden("x-y",qs).copy(annahmen=setOf(Ungleichheit(Variable("x",bindungsId="qx"),RationaleZahl.Null)))
        val e=FormelBearbeitungsEntwurf(f);e.quelle(qs[0].copy(anzeigeName="z"));e.verschiebeQuelle("qx",1)
        assertEquals(listOf("qy","qx"),e.geprüft().quellen.map {it.id})
        assertEquals(f.variablenVorkommen,e.formel.variablenVorkommen)
        assertEquals(RationaleZahl.von(3),e.formel.werteAus(mapOf("qx" to RationaleZahl.von(5),"qy" to RationaleZahl.von(2))).objekt)
        assertFails { e.formel.werteAus(mapOf("qx" to RationaleZahl.Null)) }
    }
    @Test fun `Voraussetzungsimport bewahrt Definitionsbedingung beider Seiten`() {
        val e=FormelBearbeitungsEntwurf(FormelLatexCodec.importiereGebunden("x",qs))
        e.fügeVoraussetzungHinzu("y/y","=","1")
        assertFails {e.geprüft().werteAus(mapOf("qy" to RationaleZahl.Null))}
        assertEquals(RationaleZahl.von(2),e.geprüft().werteAus(mapOf("qx" to RationaleZahl.von(2),"qy" to RationaleZahl.von(3))).objekt)
    }
    @Test fun `Doppelte Namen benoetigen explizite Vorkommensbindung`() {
        val q=qs[1].copy(anzeigeName="x")
        val e=FormelBearbeitungsEntwurf(GebundeneFormel(FormelAusdruck.Literal("l",RationaleZahl.Null,FormelTyp.ZAHL),listOf(qs[0],q),emptyMap()))
        val v=FormelAusdruck.Variable("v","x",typ=FormelTyp.ZAHL)
        assertFails { e.mitWurzel(v) };e.mitWurzel(v,mapOf("v" to "qy"))
        assertEquals("qy",e.geprüft().variablenVorkommen["v"])
    }
}

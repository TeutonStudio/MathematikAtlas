package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.TypSystem.TypAusdruck
import kotlin.test.*

class MethodenCapabilityKonsolidierungTest {
    @Test fun `Praedikatklassifikation kennt neutrale boolesche Signatur ohne Mathematikcast`() {
        val bool = TypAusdruck.Atom(MathematischeTypen.Aussage)
        fun methode(typen: List<TypAusdruck>) = object : SignaturtragendeMethode {
            override val name = "Script"
            override val signatur = MethodenSignatur(emptyList(),typen.mapIndexed { i,t -> MethodenKomponente("r$i","r$i",i,t) })
        }
        assertTrue(methode(listOf(bool)).istPrädikat())
        assertTrue(methode(listOf(TypAusdruck.Parameterisiert(MathematischeTypen.Tupel,listOf(bool)))).istPrädikat())
        for (typen in listOf(emptyList(),listOf(bool,bool),listOf(TypAusdruck.Unbekannt),listOf(TypAusdruck.Parameterisiert(MathematischeTypen.Tupel,listOf(TypAusdruck.Parameterisiert(MathematischeTypen.Tupel,listOf(bool)))))))
            assertFalse(methode(typen).istPrädikat())
        assertFalse((object : Methode { override val name="Unbekannt" }).istPrädikat())
        assertFails { methode(listOf(bool)).wendeKanonischAn(emptyMap()) }
        assertFails { methode(listOf(bool)).kompaktePrädikatsDarstellung() }
    }
    @Test fun `Formel und direkter Kernaufruf verwenden dieselbe offene Mathematik Capability`() {
        val typ = ReelleZahlen.elementTypAusdruck(); val x=Variable("x",bindungsId="qx")
        val m=object : MathematischAuswertbareMethode {
            override val name="cap"
            override val mathematischeSignatur=MathematischeMethodenSignatur(listOf(MathematischeArgumentKomponente("qx","x",0,x,ReelleZahlen)),listOf(MathematischeErgebnisKomponente("r","r",0,ReelleZahlen)))
            override fun wendeMathematischAn(argumente:Map<String,MathematischesObjekt>):MathematischesObjekt = addition(argumente.getValue("x") as ZahlAusdruck,RationaleZahl.Eins)
            override fun zuLatex()="cap"
        }
        val quelle=FormelVariablenQuelle("quelle-f","f",FormelVariablenArt.FREI,FormelTyp.METHODE,
            Abbildungsmenge(Tupelraum(listOf(ReelleZahlen)),Tupelraum(listOf(ReelleZahlen))),m.signatur.typAusdruck,methodenSignatur=m.signatur)
        val formel=FormelLatexCodec.importiereGebunden("f(2)",listOf(quelle))
        val direkt=m.wendeMitIdentitätenAn(mapOf("qx" to RationaleZahl.von(2)))
        assertEquals(direkt.ergebnis.elemente.single(),formel.werteAus(mapOf("quelle-f" to m)).objekt)
        assertEquals(RationaleZahl.von(3),formel.werteAus(mapOf("quelle-f" to m)).objekt)
    }
    @Test fun `hoehere Argumente werden auch bei offener Mengenmitgliedschaft typgeprueft`() {
        val typ=ReelleZahlen.elementTypAusdruck()
        val sig=MethodenSignatur(listOf(MethodenKomponente("x","x",0,typ)),listOf(MethodenKomponente("r","r",0,typ)))
        val p=MethodenVariable("g","qg",sig)
        val m=object : MathematischAuswertbareMethode {
            override val name="cap"
            override val mathematischeSignatur=MathematischeMethodenSignatur(listOf(MathematischeArgumentKomponente("qg","g",0,p,FehlendeObermenge("offen"))),listOf(MathematischeErgebnisKomponente("r","r",0,ReelleZahlen)))
            override fun wendeMathematischAn(argumente:Map<String,MathematischesObjekt>)=RationaleZahl.Eins
            override fun zuLatex()="cap"
        }
        assertFails { m.wendeMitIdentitätenAn(mapOf("qg" to RationaleZahl.Eins)) }
        val falsch=MethodenVariable("g","andere",MethodenSignatur(emptyList(),emptyList()))
        assertFails { m.wendeMitIdentitätenAn(mapOf("qg" to falsch)) }
    }
    @Test fun `symbolische hoehere Argumente verlangen passende Methodensignaturen`() {
        val typ=ReelleZahlen.elementTypAusdruck()
        val sig=MethodenSignatur(listOf(MethodenKomponente("x","x",0,typ)),listOf(MethodenKomponente("r","r",0,typ)))
        val f=MethodenVariable("F","qF",MethodenSignatur(listOf(MethodenKomponente("g","g",0,sig.typAusdruck,sig)),listOf(MethodenKomponente("r","r",0,typ))))
        assertFails {symbolischerMethodenAufruf(f,f.signatur,Tupel(listOf(RationaleZahl.Eins)))}
        val falsch=MethodenVariable("g","qg",MethodenSignatur(emptyList(),emptyList()))
        assertFails {symbolischerMethodenAufruf(f,f.signatur,Tupel(listOf(falsch)))}
    }
    @Test fun `AST Substitution verwendet offene Capability und erhaelt deren Definitionsbedingungen`() {
        val typ=ReelleZahlen.elementTypAusdruck();val x=Variable("x",bindungsId="qx")
        val cap=object:MathematischAuswertbareMethode {
            override val name="positiv"
            override val mathematischeSignatur=MathematischeMethodenSignatur(listOf(MathematischeArgumentKomponente("qx","x",0,x,ReellesIntervall(RationaleZahl.Null,false,RationaleZahl.von(10),false))),listOf(MathematischeErgebnisKomponente("r","r",0,ReelleZahlen)))
            override fun wendeMathematischAn(argumente:Map<String,MathematischesObjekt>)=addition(argumente.getValue("x") as ZahlAusdruck,RationaleZahl.Eins)
            override fun zuLatex()="positiv"
        }
        val sig=MethodenSignatur(listOf(MethodenKomponente("x","x",0,typ)),listOf(MethodenKomponente("r","r",0,typ)))
        val f=MethodenVariable("f","qf",sig);val a=Variable("a",bindungsId="qa")
        val h=MathematischeMethode("H",listOf(f,a),MethodenZahlKomponente(StrukturellerMethodenAufruf(f,Tupel(listOf(a))),1),ReelleZahlen,
            mapOf("f" to Abbildungsmenge(Tupelraum(listOf(ReelleZahlen)),Tupelraum(listOf(ReelleZahlen))),"a" to ReelleZahlen))
        assertEquals(Tupel(listOf(RationaleZahl.von(3))),h.wendeMitIdentitätenAn(mapOf("qf" to cap,"qa" to RationaleZahl.von(2))).ergebnis)
        assertFails {h.wendeMitIdentitätenAn(mapOf("qf" to cap,"qa" to RationaleZahl.von(-1)))}
        val u=Variable("u")
        assertTrue(h.wendeMitIdentitätenAn(mapOf("qf" to cap,"qa" to u)).bedingungen.any {it is ElementBeziehung&&it.element==u})
    }

}

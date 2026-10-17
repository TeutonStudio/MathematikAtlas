package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKnoten.visualisierung.koordinaten.*
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikKnoten.visualisierung.sampling.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class VisualisierungsTypVertragTest {
    private fun q(n: Long) = RationaleZahl.von(n)
    private val r = ReelleZahlen
    private fun c(d: RaumDimension = RaumDimension.R2) = VisualisierungsKonfiguration(
        dimension = d, achsen = AchsenZuordnung("x", if (d == RaumDimension.R1) "" else "y", if (d == RaumDimension.R3) "z" else null),
        bereiche = AchsenBereiche(ZahlenBereich(-3.0,3.0), ZahlenBereich(-3.0,3.0), ZahlenBereich(-3.0,3.0)),
        sampling = SamplingKonfiguration(8,4,.001,maximalesRasterBudget=8000), methodenModus = MethodenDarstellungsModus.Bild)
    private fun wahr(m: MengenAusdruck) = GefilterteMenge(m, MathematischeMethode("P", listOf(AllgemeinerParameter("p")),
        WahrheitsKonstante(true), WahrheitsMenge, werteVorräte=mapOf("p" to m)))
    private fun punkte(m: MengenAusdruck, config: VisualisierungsKonfiguration = c()): Set<List<Double>> =
        assertIs<VisualisierungsErgebnis.Erfolgreich>(VisualisierungsSampler.sample(m,config), m.toString())
            .punkte.map { listOfNotNull(it.x,it.y,it.z) }.toSet()

    @Test fun aequivalenteZweidimensionaleRaeumeWerdenDirektUndGefiltertGleichGesampelt() {
        val referenz = KartesischesProdukt(listOf(r,r))
        val erwartet = punkte(referenz)
        for (raum in listOf(referenz, kartesischePotenz(r,q(2)), Tupelraum(listOf(r,r)),
            Vektorraum(VektorOrientierung.Spalte,2,r),Vektorraum(VektorOrientierung.Zeile,2,r),
            Matrizenraum(1,2,r),Matrizenraum(2,1,r),Tensorraum(r,listOf(q(1),q(2)),matrixForm=true),
            Tensorraum(r,listOf(q(2))),Tensorraum(r,listOf(q(1),q(2))))) {
            assertEquals(RaumDimension.R2,empfohleneRaumDimension(raum),raum.toString())
            assertEquals(erwartet,punkte(raum),raum.toString())
            assertEquals(erwartet,punkte(wahr(raum)),raum.toString())
        }
    }

    @Test fun verschachtelteProdukteErhaltenAlleDreiAchsen() {
        val config = c(RaumDimension.R3)
        val erwartet = punkte(KartesischesProdukt(listOf(r,r,r)),config)
        for (m in listOf(KartesischesProdukt(listOf(r,KartesischesProdukt(listOf(r,r)))),
            KartesischesProdukt(listOf(KartesischesProdukt(listOf(r,r)),r)))) {
            assertEquals(erwartet,punkte(m,config))
            assertEquals(erwartet,punkte(wahr(m),config))
        }
    }

    @Test fun diskreteWahrFilterVerlierenKeineFensterpunkte() {
        for ((menge, dimension) in listOf(GanzeZahlen to RaumDimension.R1, NatürlicheZahlen to RaumDimension.R1,
            NichtnegativeGanzeZahlenSemantik.menge to RaumDimension.R1,Primzahlen to RaumDimension.R1,
            GaußscheGanzeZahlen to RaumDimension.C, GaußschePrimzahlen to RaumDimension.C)) {
            val config = c(dimension)
            assertEquals(punkte(menge,config),punkte(wahr(menge),config),menge.toString())
        }
        val gauss = punkte(GaußschePrimzahlen,c(RaumDimension.C))
        val erwartet = (-3L..3L).flatMap { a -> (-3L..3L).mapNotNull { b ->
            if (ElementBeziehung(KomplexeZahl(q(a),q(b)),GaußschePrimzahlen).entscheide().wahrheitswert == Wahrheitswert.Wahr)
                listOf(a.toDouble(),b.toDouble()) else null
        } }.toSet()
        assertEquals(erwartet,gauss)
        assertFalse(listOf(0.0,0.0) in gauss)
    }

    @Test fun gemischteDiskreteAchsenBleibenAuchImFilterVollstaendig() {
        val m = KartesischesProdukt(listOf(GanzeZahlen,ReellesIntervall(q(0),false,q(1),false)))
        assertEquals(punkte(m),punkte(wahr(m)))
        assertEquals((-3..3).map(Int::toDouble).toSet(),punkte(wahr(m)).map { it[0] }.toSet())
    }

    @Test fun packungErhaeltFormOrientierungUndKomplexeKomponenten() {
        val faelle = listOf(
            Vektorraum(VektorOrientierung.Zeile,2,r) to ZeilenVektor(listOf(q(1),q(2))),
            Vektorraum(VektorOrientierung.Spalte,2,r) to SpaltenVektor(listOf(q(1),q(2))),
            Matrizenraum(1,2,r) to Matrix(listOf(listOf(q(1),q(2)))),
            Tensorraum(r,listOf(q(2))) to Tensor(listOf(2),listOf(q(1),q(2))),
            KomplexeZahlen to KomplexeZahl(q(1),q(2)),
            KartesischesProdukt(listOf(r,Tupelraum(listOf(r,r)))) to Tupel(listOf(q(1),Tupel(listOf(q(2),q(3)))))
        )
        for ((m,wert) in faelle) {
            val raum=assertNotNull(numerischerRaum(m))
            assertEquals(wert,raum.packe((1..raum.blätter.size).map { q(it.toLong()) }.iterator()))
            assertEquals(Wahrheitswert.Wahr,raum.bedingung(wert).entscheide().wahrheitswert)
        }
    }

    @Test fun bildmengenAkzeptierenAlleStrukturiertenDefinitionsraeume() {
        for (m in listOf<MengenAusdruck>(Tupelraum(listOf(r,r)),kartesischePotenz(r,q(2)),
            Vektorraum(VektorOrientierung.Spalte,2,r),Matrizenraum(1,2,r),Tensorraum(r,listOf(q(2))),KomplexeZahlen)) {
            val methode=MathematischeMethode("f",listOf(AllgemeinerParameter("p")),Tupel(listOf(q(1),q(2))),
                Tupelraum(listOf(r,r)),werteVorräte=mapOf("p" to m),ausgabeNamen=listOf("u","v"))
            assertEquals(setOf(listOf(1.0,2.0)),punkte(Abbild(m,methode)))
        }
    }

    @Test fun matrixTensorUndQuaternionProjektionenVerwendenDieAusgewaehlteReihenfolge() {
        val matrix=Matrix(listOf(listOf(q(1),q(2)),listOf(q(3),q(4))))
        val tensor=Tensor(listOf(2,2),listOf(q(1),q(2),q(3),q(4)))
        for (wert in listOf(matrix,tensor,Tupel(listOf(q(0),matrix)))) {
            val indices=if(wert is Tupel) listOf(5,2) else listOf(4,1)
            assertEquals(listOf(4.0,1.0),assertIs<KoordinatenErgebnis.Darstellbar>(
                KoordinatenAdapter.extrahiere(wert,2,ausgabeKoordinaten=indices)).werte)
        }
        assertEquals(setOf(listOf(4.0,1.0)),punkte(EndlicheMenge(setOf(matrix)),c().copy(ausgabeKoordinaten=listOf(4,1))))
        assertIs<KoordinatenErgebnis.ProjektionErforderlich>(KoordinatenAdapter.extrahiere(matrix,2))
        assertIs<KoordinatenErgebnis.NichtDarstellbar>(KoordinatenAdapter.extrahiere(matrix,2,ausgabeKoordinaten=listOf(1,1)))
        val quaternion= DomaenenWert.Quaternion(java.math.BigDecimal.ONE,java.math.BigDecimal.TEN,java.math.BigDecimal("20"),java.math.BigDecimal("30"))
        assertEquals(listOf(30.0,10.0),assertIs<KoordinatenErgebnis.Darstellbar>(KoordinatenAdapter.extrahiere(
            Variable("h"),2,domänenUmgebung=mapOf("h" to quaternion),ausgabeKoordinaten=listOf(4,2))).werte)
    }

    @Test fun endlicheVektorenLeitenIhreDimensionAb() {
        for (wert in listOf(ZeilenVektor(listOf(q(1),q(2))),SpaltenVektor(listOf(q(1),q(2))),Tensor(listOf(2),listOf(q(1),q(2))))) {
            assertEquals(RaumDimension.R2,empfohleneRaumDimension(EndlicheMenge(setOf(wert))))
            assertEquals(setOf(listOf(1.0,2.0)),punkte(EndlicheMenge(setOf(wert))))
        }
    }

    @Test fun nichtRaeumlicheTypenErhaltenHandlungsorientierteDiagnosen() {
        for (m in listOf(Gradmenge,ModuloZahlenraum(3),Folgenraum(r),Abbildungsmenge(r,r),Potenzmenge(r),BenannteMenge("A"))) {
            for (menge in listOf(m,wahr(m))) {
                val e=assertIs<VisualisierungsErgebnis.NichtDarstellbar>(VisualisierungsSampler.sample(menge,c()))
                assertTrue(e.grund.contains("Abbildung") || e.grund.contains("ausdrücklich"),e.grund)
            }
        }
    }

    @Test fun symbolischeUndNullDimensionenWerdenNichtAlsLeereFlaecheAusgegeben() {
        for (m in listOf(kartesischePotenz(r,Variable("n")),Tensorraum(r,listOf(Variable("n"),q(2)),matrixForm=true))) {
            assertIs<VisualisierungsErgebnis.BedingtDarstellbar>(VisualisierungsSampler.sample(m,c()))
            assertIs<VisualisierungsErgebnis.BedingtDarstellbar>(VisualisierungsSampler.sample(wahr(m),c()))
        }
        for (m in listOf(kartesischePotenz(r,q(0)),Tupelraum(emptyList()))) {
            assertEquals(0,assertIs<VisualisierungsErgebnis.ProjektionErforderlich>(VisualisierungsSampler.sample(m,c())).vorhandeneDimension)
            assertEquals(0,assertIs<VisualisierungsErgebnis.ProjektionErforderlich>(VisualisierungsSampler.sample(wahr(m),c())).vorhandeneDimension)
        }
    }

    @Test fun filterProjektionenVerwendenFesteSchnitteStattUnbelegterKoordinaten() {
        val m=Matrizenraum(2,2,r)
        val config=c().copy(ausgabeKoordinaten=listOf(4,1))
        assertIs<VisualisierungsErgebnis.BedingtDarstellbar>(VisualisierungsSampler.sample(wahr(m),config))
        assertEquals(punkte(KartesischesProdukt(listOf(r,r))),punkte(wahr(m),config.copy(festeSchnitte=mapOf("k2" to 0.0,"k3" to 1.0))))
        assertEquals(punkte(KartesischesProdukt(listOf(r,r))),punkte(m,config))
        val gespeichert=VisualisierungsKonfiguration.aus(config.copy(festeSchnitte=mapOf("k2" to 0.0,"k3" to 1.0)).zuEigenschaften())
        assertEquals(config.ausgabeKoordinaten,gespeichert.ausgabeKoordinaten)
        assertEquals(setOf("k2","k3"),gespeichert.festeSchnitte.keys)
    }

    @Test fun identitaetsBildmengenNutzenTatsaechlicheKomplexeMatrixUndTensorArgumente() {
        for (m in listOf<MengenAusdruck>(KomplexeZahlen,Matrizenraum(1,2,r),Tensorraum(r,listOf(q(2))))) {
            val p=AllgemeinerParameter("p")
            val id=MathematischeMethode("id",listOf(p),Tupel(listOf(p)),Tupelraum(listOf(m)),werteVorräte=mapOf("p" to m))
            val config=c().copy(ausgabeKoordinaten=listOf(2,1))
            val ergebnis=punkte(Abbild(m,id),config)
            assertEquals(punkte(KartesischesProdukt(listOf(r,r))),ergebnis)
        }
    }

    @Test fun verschachtelteGaussschePrimzahlenVerlierenIhreEinschraenkungNicht() {
        val m=KartesischesProdukt(listOf(GaußschePrimzahlen,EndlicheMenge(setOf(q(1)))))
        val config=c(RaumDimension.R3)
        val erwartet=punkte(GaußschePrimzahlen,c(RaumDimension.C)).map { it + 1.0 }.toSet()
        assertEquals(erwartet,punkte(m,config))
        assertEquals(erwartet,punkte(wahr(m),config))
        val p=AllgemeinerParameter("p")
        val id=MathematischeMethode("id",listOf(p),Tupel(listOf(p)),Tupelraum(listOf(GaußschePrimzahlen)),werteVorräte=mapOf("p" to GaußschePrimzahlen))
        assertEquals(punkte(GaußschePrimzahlen,c(RaumDimension.C)),punkte(Abbild(GaußschePrimzahlen,id),c(RaumDimension.C).copy(ausgabeKoordinaten=listOf(1,2))))
    }

    @Test fun verschachtelteZusatzbedingungenWerdenNichtAlsVollerRaumZertifiziert() {
        val m=KartesischesProdukt(listOf(GaußschePrimzahlen,EndlicheMenge(setOf(q(1)))))
        val region=assertIs<VisualisierungsDefinition.Region>(VisualisierungsSampler.normalisiere(Vereinigung(listOf(m)),c(RaumDimension.R3)))
        assertEquals(NumerischeMitgliedschaft.NichtEnthalten,region.mitgliedschaft(listOf(2.0,0.0,1.0)))
        val zelle=listOf(RationalesIntervall(q(2)),RationalesIntervall(q(0)),RationalesIntervall(q(1)))
        assertNotEquals(ZellNachweis.Enthalten,assertNotNull(region.zellNachweis).invoke(zelle))
    }

    @Test fun leeresDiskretesParameterfensterIstKeinBeweisFuerLeereGrundmenge() {
        val p=AllgemeinerParameter("p")
        val methode=MathematischeMethode("f",listOf(p),Tupel(listOf(q(0),q(1))),Tupelraum(listOf(r,r)),werteVorräte=mapOf("p" to NatürlicheZahlen),ausgabeNamen=listOf("u","v"))
        val config=c().copy(bereiche=AchsenBereiche(ZahlenBereich(-3.0,-1.0),ZahlenBereich(-1.0,1.0),null))
        val e=assertIs<VisualisierungsErgebnis.Erfolgreich>(VisualisierungsSampler.sample(Abbild(NatürlicheZahlen,methode),config))
        assertTrue(e.punkte.isEmpty())
        assertEquals(VisualisierungsQualität.KeineTrefferImFenster,e.qualität)
    }

    @Test fun projektionsEingabeSpeicherungUndDarstellungsraumBleibenKonsistent() {
        for ((text,raum) in listOf("4" to RaumDimension.R1,"4,1" to RaumDimension.R2,"4,1,3" to RaumDimension.R3)) {
            val config=c().mitAusgabeProjektion(parseAusgabeKoordinaten(text))
            assertEquals(raum,config.dimension)
            val geladen=VisualisierungsKonfiguration.aus(config.zuEigenschaften())
            assertEquals(config.ausgabeKoordinaten,geladen.ausgabeKoordinaten)
            assertEquals(config.dimension,geladen.dimension)
            assertEquals(config.achsen,geladen.achsen)
            assertTrue(listOfNotNull(config.achsen.x,config.achsen.y.takeIf(String::isNotBlank),config.achsen.z).all(String::isNotBlank))
        }
        assertFailsWith<IllegalArgumentException> { c().mitAusgabeProjektion(listOf(1,1)) }
        val e=VisualisierungsSampler.sample(EndlicheMenge(setOf(Matrix(listOf(listOf(q(1),q(2)),listOf(q(3),q(4)))))),c().mitAusgabeProjektion(listOf(4)))
        assertEquals(listOf(4.0),assertIs<VisualisierungsErgebnis.Erfolgreich>(e).punkte.map { it.x })
    }

    @Test fun raumwechselErgaenzenAchsenUndEntfernenUnpassendeProjektionenAtomar() {
        val eindimensional=c().copy(bereiche=c().bereiche.copy(z=null)).mitAusgabeProjektion(listOf(4))
        for (raum in listOf(RaumDimension.R2,RaumDimension.R3)) {
            val config=eindimensional.mitDarstellungsRaum(raum)
            assertTrue(config.ausgabeKoordinaten.isEmpty())
            assertEquals(if(raum==RaumDimension.R2) listOf("x","y") else listOf("x","y","z"),listOfNotNull(config.achsen.x,config.achsen.y.takeIf(String::isNotBlank),config.achsen.z))
            if(raum==RaumDimension.R3) assertNotNull(config.bereiche.z)
            assertIs<VisualisierungsErgebnis.Erfolgreich>(VisualisierungsSampler.sample(kartesischePotenz(r,q(if(raum==RaumDimension.R2)2 else 3)),config))
        }
        val komplex=c().mitDarstellungsRaum(RaumDimension.C).mitDarstellungsRaum(RaumDimension.R3)
        assertEquals(listOf("x","y","z"),listOfNotNull(komplex.achsen.x,komplex.achsen.y,komplex.achsen.z))
        assertNotNull(komplex.bereiche.z)
    }

    @Test fun endlicheKomplexeBildmengenErhaltenAuchReelleElementeUndEndlicheArgumente() {
        val menge=EndlicheMenge(setOf(q(1),KomplexeZahl(q(1),q(2))))
        assertEquals(RaumDimension.C,empfohleneRaumDimension(menge))
        assertEquals(setOf(listOf(1.0,0.0),listOf(1.0,2.0)),punkte(menge,c(RaumDimension.C)))
        val dom=Tupelraum(listOf(EndlicheMenge(setOf(KomplexeZahl(q(1),q(0)),KomplexeZahl(q(1),q(2)))),EndlicheMenge(setOf(q(3)))))
        val p=AllgemeinerParameter("p")
        val id=MathematischeMethode("id",listOf(p),Tupel(listOf(p)),Tupelraum(listOf(dom)),werteVorräte=mapOf("p" to dom))
        assertEquals(setOf(listOf(1.0,0.0),listOf(1.0,2.0)),punkte(Abbild(dom,id),c().copy(ausgabeKoordinaten=listOf(1,2))))
    }

    @Test fun zusammengesetzteKomplexeAusdrueckeLeitenDieTatsaechlicheDimensionAb() {
        val z=Exponentialfunktion(KomplexeZahl(q(0),q(1)))
        assertEquals(RaumDimension.C,empfohleneRaumDimension(EndlicheMenge(setOf(z))))
        assertEquals(RaumDimension.R3,empfohleneRaumDimension(EndlicheMenge(setOf(Tupel(listOf(q(2),z))))))
        assertEquals(1,punkte(EndlicheMenge(setOf(z)),c(RaumDimension.C)).size)
        assertNull(empfohleneRaumDimension(EndlicheMenge(setOf(Variable("ungebunden")))))
    }

    @Test fun offenePrimzahlNachweiseSindBedingtStattNichtUnterstuetzt() {
        val grenze=1_000_000_007L
        assertNull(ElementBeziehung(q(grenze),Primzahlen).entscheide().wahrheitswert)
        val config=c(RaumDimension.R1).copy(bereiche=AchsenBereiche(ZahlenBereich(grenze.toDouble(),grenze+1.0),ZahlenBereich(-1.0,1.0),null))
        assertIs<VisualisierungsErgebnis.BedingtDarstellbar>(VisualisierungsSampler.sample(Primzahlen,config))
        assertIs<VisualisierungsErgebnis.BedingtDarstellbar>(VisualisierungsSampler.sample(wahr(Primzahlen),config))
    }

    @Test fun mengenalgebraBewahrtDiskretePunkteUndGemischteAchsen() {
        for ((m,config) in listOf(GanzeZahlen to c(RaumDimension.R1),GaußschePrimzahlen to c(RaumDimension.C),
            KartesischesProdukt(listOf(GanzeZahlen,r)) to c())) {
            assertEquals(punkte(m,config),punkte(Vereinigung(listOf(m)),config))
            assertEquals(punkte(m,config),punkte(MengenDifferenz(m,LeereMenge),config))
            assertEquals(punkte(m,config),punkte(Schnitt(listOf(m,m)),config))
        }
        assertEquals(RaumDimension.R1,empfohleneRaumDimension(MengenDifferenz(GanzeZahlen,LeereMenge)))
        assertEquals(RaumDimension.R1,empfohleneRaumDimension(Vereinigung(listOf(GanzeZahlen,LeereMenge))))
        val ohneNull=MengenDifferenz(GanzeZahlen,EndlicheMenge(setOf(q(0))))
        assertEquals(setOf(-3.0,-2.0,-1.0,1.0,2.0,3.0),punkte(ohneNull,c(RaumDimension.R1)).map { it[0] }.toSet())
        val eingeschraenktesProdukt=KartesischesProdukt(listOf(ohneNull,r))
        val produktErwartet=punkte(KartesischesProdukt(listOf(GanzeZahlen,r))).filterNot { it[0] == 0.0 }.toSet()
        assertEquals(produktErwartet,punkte(eingeschraenktesProdukt))
        assertEquals(produktErwartet,punkte(wahr(eingeschraenktesProdukt)))
        val linien=Vereinigung(listOf(KartesischesProdukt(listOf(GanzeZahlen,r)),KartesischesProdukt(listOf(r,GanzeZahlen))))
        val erwartet=punkte(KartesischesProdukt(listOf(GanzeZahlen,r)))+punkte(KartesischesProdukt(listOf(r,GanzeZahlen)))
        assertEquals(erwartet,punkte(linien))
    }

    @Test fun eingeschraenkteMatrixraeumeErhaltenFormMitgliedschaftUndBildpunkte() {
        val basis=Matrizenraum(1,2,NichtnegativeGanzeZahlenSemantik.menge)
        val nullMatrix=Matrix(listOf(listOf(q(0),q(0))))
        val m=MengenDifferenz(basis,EndlicheMenge(setOf(nullMatrix)))
        val erwartet=punkte(basis)-setOf(listOf(0.0,0.0))
        assertEquals(15,erwartet.size)
        assertEquals(erwartet,punkte(m))
        assertEquals(erwartet,punkte(wahr(m)))
        val p=AllgemeinerParameter("p")
        val id=MathematischeMethode("id",listOf(p),Tupel(listOf(p)),Tupelraum(listOf(basis)),werteVorräte=mapOf("p" to m))
        assertEquals(erwartet,punkte(Abbild(m,id),c().copy(ausgabeKoordinaten=listOf(1,2))))
        val verschachtelt=KartesischesProdukt(listOf(m,EndlicheMenge(setOf(q(1)))))
        assertEquals(erwartet.map { it + 1.0 }.toSet(),punkte(verschachtelt,c(RaumDimension.R3)))
        assertEquals(erwartet.map { it + 1.0 }.toSet(),punkte(wahr(verschachtelt),c(RaumDimension.R3)))
    }

    @Test fun grosseRaeumeUndDiskreteFensterWerdenVorMaterialisierungBegrenzt() {
        assertNull(numerischerRaum(kartesischePotenz(kartesischePotenz(r,q(100)),q(100))))
        val e=VisualisierungsSampler.sample(GaußscheGanzeZahlen,c().copy(sampling=c().sampling.copy(maximalesRasterBudget=8)))
        assertIs<VisualisierungsErgebnis.NichtDarstellbar>(e)
        assertIs<VisualisierungsErgebnis.NichtDarstellbar>(VisualisierungsSampler.sample(wahr(GanzeZahlen),c(RaumDimension.R1)
            .copy(bereiche=AchsenBereiche(ZahlenBereich(-1e100,1e100),ZahlenBereich(-1.0,1.0),null))))
    }
}

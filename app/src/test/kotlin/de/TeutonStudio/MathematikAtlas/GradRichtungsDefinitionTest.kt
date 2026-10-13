package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikKnoten.visualisierung.sampling.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class GradRichtungsDefinitionTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()
    private fun eval(k: KnotenDaten, eingang: Map<String, BedingterWert>): BedingterWert {
        val result = register.finde(k.art)!!.auswerten(KnotenAuswertungsKontext(k, eingang, RechenKontext()))
        assertNull(result.fehler, result.fehler)
        return result.ausgaben.values.single()
    }
    @Test fun `Gruppendefinition und Gradquelle liefern visualisierbares Diagonalbild`() {
        for (d in listOf(1, 2, 3, 4)) {
            val karte = gradRichtungsDefinitionsKarte(d)
            val geladen = MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(karte))
            val ergebnis = KartenAuswerter(register).auswerten(geladen)
            val produkt = geladen.knoten.single { it.art == "mathematik.iteriertesKartesischesProdukt" || it.parameter["operator"] == "menge.iteriertesKartesischesProdukt" }
            val definitionsWert = ergebnis.knoten.getValue(produkt.id).ausgaben["menge"]
            assertNotNull(definitionsWert, ergebnis.knoten.toString())
            val direkt = eval(MengenKnotenVorlagen.vorlage(MengenKnotenAuswahl.GRADMENGE).erzeuge(GraphPunkt.Zero), mapOf("dim" to BedingterWert(RationaleZahl.von(d.toLong()))))
            for (raum in listOf(definitionsWert, direkt)) {
                val variable = konfiguriereVariablenStruktur(MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero), VARIABLE_STRUKTUR_WINKEL)
                val w = eval(variable, mapOf("grundraum" to raum))
                assertEquals(winkelraum(d-1), w.werteVorrat)
                val polar = konfiguriereÜbersetzerPolarAnschlüsse(konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero), "kartesisch"), true)
                val kartesisch = eval(polar, mapOf("radius" to BedingterWert(RationaleZahl.Eins), "winkel" to w))
                val spalte = eval(KnotenDaten(art = "mathematik.tupelZuSpalte", name = "Tupel zu Vektor"), mapOf("tupel" to kartesisch))
                val matrix = Matrix(List(d) { i -> List(d) { j -> if (i == j) RationaleZahl.von(2) else RationaleZahl.Null } })
                val op = konfiguriereStrukturRechner(StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero), de.TeutonStudio.MathematikKnoten.StrukturRechnerKnotenFamilie.MATRIX, "matrix.produkt")
                val term = eval(op, mapOf("links" to BedingterWert(matrix), "rechts" to spalte))
                val methode = eval(MathematikKnotenVorlagen.TermZuMethode.erzeuge(GraphPunkt.Zero), mapOf("term" to term))
                val bild = eval(MathematikKnotenVorlagen.Abbild.erzeuge(GraphPunkt.Zero), mapOf("menge" to raum, "methode" to methode))
                assertEquals(winkelraum(d-1), (bild.objekt as Abbild).menge)
                val sample = VisualisierungsSampler.sample(bild.objekt as Abbild, VisualisierungsKonfiguration(
                    dimension = when (d) { 1 -> RaumDimension.R1; 2 -> RaumDimension.R2; else -> RaumDimension.R3 },
                    methodenModus = MethodenDarstellungsModus.Bild,
                    ausgabeKoordinaten = if (d > 3) listOf(1,2,3) else emptyList(),
                    sampling = SamplingKonfiguration(4,4,0.01, maximalesRasterBudget = 128)))
                val punkte = assertIs<VisualisierungsErgebnis.Erfolgreich>(sample).punkte
                assertTrue(punkte.isNotEmpty())
                if (d <= 3) assertTrue(punkte.all { kotlin.math.abs(it.x*it.x + it.y*it.y + (it.z ?: 0.0)*(it.z ?: 0.0) - 4.0) < 1e-10 })
            }
        }
    }
    @Test fun `Vollstaendige korrigierte Karte laedt und erzeugt Kugelbild`() {
        val definition = gradRichtungsDefinitionsKarte(3)
        val knoten = definition.knoten.toMutableList()
        val kanten = definition.verbindungen.toMutableList()
        fun neu(v: KnotenVorlage, x: Float, y: Float, p: Map<String,String> = emptyMap()) =
            v.erzeuge(GraphPunkt(x,y)).let { it.copy(parameter = it.parameter + p) }.also { knoten += it }
        fun verbinde(a: KnotenDaten, aus: String, b: KnotenDaten, ein: String) {
            kanten += VerbindungDaten(von = AnschlussVerweis(a.id,a.anschlüsse.single { it.name == aus && it.richtung == AnschlussRichtung.Ausgang }.id),
                zu = AnschlussVerweis(b.id,b.anschlüsse.single { it.name == ein && it.richtung == AnschlussRichtung.Eingang }.id))
        }
        val raum = definition.knoten.single { it.art == "mathematik.iteriertesKartesischesProdukt" }
        val variable = konfiguriereVariablenStruktur(MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt(0f,900f)), VARIABLE_STRUKTUR_WINKEL).also { knoten += it }
        val polar = konfiguriereÜbersetzerPolarAnschlüsse(konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt(350f,900f)),"kartesisch"),true).let { it.copy(parameter=it.parameter+("standardwert.radius" to "1")) }.also { knoten += it }
        val vektor = neu(MathematikKnotenVorlagen.TupelZuSpalte,700f,900f)
        val tupel = neu(MathematikKnotenVorlagen.Tupel,0f,650f,mapOf("standardwert.a" to "2", "standardwert.b" to "2"))
        val diagonal = konfiguriereMatrixdiagonale(MatrixdiagonaleKnotenVorlagen.Matrixdiagonale.erzeuge(GraphPunkt(700f,650f)), MATRIXDIAGONALE_ERZEUGEN).also { knoten += it }
        // Explizit drei gleich lange Halbachsen, unabhängig von Tensorstufe.
        val dritter = AnschlussDaten(name="input3",richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,art=MathematikAnschlussArten.Objekt.id,reihenfolge=2)
        val tupel3 = tupel.copy(anschlüsse=tupel.anschlüsse+dritter, parameter=tupel.parameter+mapOf("festeEingänge" to "3", "standardwert.input3" to "2"))
        knoten[knoten.indexOf(tupel)] = tupel3
        val produkt = konfiguriereStrukturRechner(StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt(1050f,850f)),de.TeutonStudio.MathematikKnoten.StrukturRechnerKnotenFamilie.MATRIX,"matrix.produkt").also { knoten += it }
        val methode = neu(MathematikKnotenVorlagen.TermZuMethode,1400f,850f)
        val bild = neu(MathematikKnotenVorlagen.Abbild,1750f,850f)
        val visualisierung = neu(MathematikKnotenVorlagen.Visualisierung,2100f,850f)
        verbinde(raum,"menge",variable,"grundraum"); verbinde(variable,"wert",polar,"winkel")
        verbinde(polar,"ergebnis",vektor,"tupel"); verbinde(tupel3,"tupel",diagonal,"werte")
        verbinde(diagonal,"matrix",produkt,"links"); verbinde(vektor,"vektor",produkt,"rechts")
        verbinde(produkt,"wert",methode,"term"); verbinde(methode,"methode",bild,"methode")
        verbinde(raum,"menge",bild,"menge"); verbinde(bild,"menge",visualisierung,"menge")
        val karte = KartenDaten(name="Neue Karte 16 · korrigiert", knoten=knoten, verbindungen=kanten, visuelleGruppen=listOf(VisuelleKnotenGruppeDaten(titel="Gradmenge · Definition", knotenIds=definition.knoten.map { it.id }.toSet(), position=GraphPunkt(-70f,-300f), größe=GraphGröße(2000f,880f))))
        val json = MathematikKartenCodec.schreibe(karte)
        val geladen = MathematikKartenCodec.lade(json)
        val auswertung = KartenAuswerter(register).auswerten(geladen)
        val wert = assertNotNull(auswertung.knoten.getValue(bild.id).ausgaben["menge"], auswertung.knoten.toString())
        val sample = assertIs<VisualisierungsErgebnis.Erfolgreich>(VisualisierungsSampler.sample(wert.objekt as Abbild,
            VisualisierungsKonfiguration(dimension=RaumDimension.R3,methodenModus=MethodenDarstellungsModus.Bild)))
        assertTrue(sample.punkte.isNotEmpty())
        assertTrue(sample.punkte.all { kotlin.math.abs(it.x*it.x+it.y*it.y+it.z!!*it.z!!-4.0)<1e-10 })
    }
    @Test fun `Resolver liefert die Gruppendefinition`() {
        val k = MengenKnotenVorlagen.vorlage(MengenKnotenAuswahl.GRADMENGE).erzeuge(GraphPunkt.Zero)
        val konzept = assertNotNull(dynamischesKonzeptFürKnoten(k))
        assertTrue(konzept.reiter.single().karte.knoten.any { it.art == "mathematik.fall" })
    }
}

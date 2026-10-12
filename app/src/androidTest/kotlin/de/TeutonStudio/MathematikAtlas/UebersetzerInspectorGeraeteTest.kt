package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class UebersetzerInspectorGeraeteTest {
 @get:Rule val compose = createComposeRule()
 @Test fun koordinatenUndIndexZieleLassenSichMitStabilenPortsKonfigurieren() {
  val basis = ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero)
  val z = KartenEditorZustand(KartenDaten(name="Inspector",knoten=listOf(basis)),GraphPrüfung(AnschlussArtRegister(MathematikAnschlussArten.alle)))
  fun aktuell() = z.karte.knoten.single()
  val aktionen = object : KnotenInspektorAktionen {
   override fun knoten(knoten: KnotenDaten) { z.führeAus(KartenAktion.KnotenErsetzen(knoten)) }
   override fun parameter(schlüssel: String,wert: String) { z.führeAus(KartenAktion.KnotenParameterÄndern(basis.id,schlüssel,wert)) }
   override fun name(wert: String) = error("Nicht Teil dieser Konfiguration")
   override fun eigenschaften(eigenschaften: Map<String,KnotenEigenschaft>) = error("Nicht Teil dieser Konfiguration")
   override fun anschlussArt(verweis: AnschlussVerweis,art: AnschlussArtId) = error("Nicht Teil dieser Konfiguration")
  }
  compose.setContent { MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
   ÜbersetzerInspektor.Inhalt(aktuell(),null,aktionen)
  } } }
  val register = GesamterMathematikAuswerter.erzeugeRegister()
  fun auswerten(wert: MathematischesObjekt): MathematischesObjekt {
   val k = aktuell()
   val e = register.finde(k.art)!!.auswerten(KnotenAuswertungsKontext(k,mapOf("wert" to BedingterWert(wert)),RechenKontext()))
   assertNull(e.fehler)
   return e.ausgaben.getValue("ergebnis").objekt as MathematischesObjekt
  }
  val quelle = Tupel(listOf(RationaleZahl.von(3),RationaleZahl.von(4)))
  compose.onNodeWithText("Polarkoordinaten").performScrollTo().performClick()
  lateinit var polar: Tupel
  compose.runOnIdle {
   assertEquals("polar",übersetzerZiel(aktuell()))
   polar = auswerten(quelle) as Tupel
   assertTrue(polar.elemente[1] is Tupel)
   assertEquals(basis.anschlüsse.map { it.id },aktuell().anschlüsse.map { it.id })
  }
  compose.onNodeWithText("Kartesische Koordinaten").performScrollTo().performClick()
  compose.runOnIdle {
   assertEquals("kartesisch",übersetzerZiel(aktuell()))
   assertEquals(2,(auswerten(polar) as Tupel).elemente.size)
   assertEquals(basis.anschlüsse.map { it.id },aktuell().anschlüsse.map { it.id })
  }
  compose.onNodeWithText("Indexmethode").performScrollTo().performClick()
  compose.runOnIdle {
   assertEquals("indexmethode",übersetzerZiel(aktuell()))
   assertTrue(auswerten(quelle) is Methode)
   assertEquals(basis.anschlüsse.map { it.id },aktuell().anschlüsse.map { it.id })
   z.rückgängig()
   assertEquals("kartesisch",übersetzerZiel(aktuell()))
  }
  compose.onNodeWithText("Matrix zerlegen").performScrollTo().performClick()
  compose.onNodeWithText("Spalten").performScrollTo().performClick()
  compose.onNodeWithText("Tupel").performScrollTo().performClick()
  compose.runOnIdle {
   val k=aktuell()
   assertEquals("matrixzerlegen",übersetzerZiel(k))
   assertEquals(MatrixZerlegeRichtung.SPALTEN.name,k.parameter[MATRIX_ZERLEGEN_RICHTUNG])
   assertEquals("tupel",k.parameter["matrixZerlegen.ausgabe"])
   val matrix=Matrix(listOf(listOf(RationaleZahl.von(1),RationaleZahl.von(2),RationaleZahl.von(3)),listOf(RationaleZahl.von(4),RationaleZahl.von(5),RationaleZahl.von(6))))
   val e=register.finde(k.art)!!.auswerten(KnotenAuswertungsKontext(k,mapOf("wert" to BedingterWert(matrix)),RechenKontext()))
   assertNull(e.fehler)
   assertEquals(3,e.ausgaben.size)
   assertTrue(e.ausgaben.values.all { (it.objekt as? Tupel)?.elemente?.size==2 })
   z.rückgängig()
   assertEquals("vektor",aktuell().parameter["matrixZerlegen.ausgabe"] ?: "vektor")
   assertEquals(MatrixZerlegeRichtung.SPALTEN.name,aktuell().parameter[MATRIX_ZERLEGEN_RICHTUNG])
  }
 }
}

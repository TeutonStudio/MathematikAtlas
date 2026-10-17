package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.katalog.KanonischerMathematikKnotenKatalog
import kotlin.test.Test
import kotlin.test.assertEquals

class MathematikAdapterDarstellungTest {
    @Test fun `aktive Uebersetzungsvorlagen sind schon vor dem Speichern Kapseln`() {
        val vorlagen = KanonischerMathematikKnotenKatalog.alle() + GeometrieKnotenVorlagen.alle
        val erzeugbare = MATHEMATISCHE_ADAPTER_ARTEN - übersetzungsKonsolidierungAltarten
        erzeugbare.forEach { art ->
            vorlagen.filter { it.art == art }.also { check(it.isNotEmpty()) { art } }.forEach { vorlage ->
                assertEquals(KnotenDarstellungsForm.Adapter, vorlage.erzeuge(GraphPunkt.Zero).darstellungsForm, art)
            }
        }
    }
}

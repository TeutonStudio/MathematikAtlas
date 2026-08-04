package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKnoten.STRUKTUR_RECHNER_FORMEL_LATEX
import de.TeutonStudio.MathematikKnoten.StrukturFormelRechnerVorlagen
import de.TeutonStudio.MathematikKnoten.StrukturRechnerKnotenFamilie
import de.TeutonStudio.MathematikKnoten.konfiguriereStrukturRechnerFormel
import de.TeutonStudio.MathematikRechenSystem.kern.FormelArgument
import de.TeutonStudio.MathematikRechenSystem.kern.FormelAusdruck
import de.TeutonStudio.MathematikRechenSystem.kern.FormelTyp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StrukturRechnerDefinitionsKartenTest {
    @Test
    fun `Definitionskarte bildet aktuelle Formel und dynamische Schnittstellen ab`() {
        val formel = FormelAusdruck.Operation(
            id = "produkt",
            operatorId = "matrix.produkt",
            argumente = listOf(
                FormelArgument("links", 0, FormelAusdruck.Variable("a", "A", "A", FormelTyp.MATRIX)),
                FormelArgument("rechts", 1, FormelAusdruck.Variable("b", "B", "B", FormelTyp.MATRIX)),
            ),
            typ = FormelTyp.MATRIX,
        )
        val knoten = konfiguriereStrukturRechnerFormel(
            StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero),
            StrukturRechnerKnotenFamilie.MATRIX,
            formel,
        )

        val konzept = strukturRechnerKonzept(knoten, StrukturRechnerKnotenFamilie.MATRIX)
        val karte = konzept.reiter.single().karte
        val regel = karte.knoten.first { it.art == TestDefinitionsKarten.KONZEPT_REGEL_ART }

        assertEquals(knoten.parameter[STRUKTUR_RECHNER_FORMEL_LATEX], regel.parameter["definition"])
        assertEquals(3, karte.knoten.count { it != regel })
        assertEquals(3, karte.verbindungen.size)
        assertTrue(konzept.tags.contains("CAS"))
    }
}

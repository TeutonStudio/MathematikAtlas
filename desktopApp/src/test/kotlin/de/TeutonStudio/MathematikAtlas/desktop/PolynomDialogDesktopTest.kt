package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.PrädikatsRechenwegDialog
import de.TeutonStudio.MathematikRechenSystem.kern.*
import org.junit.Rule
import org.junit.Test

class PolynomDialogDesktopTest {
    @get:Rule val compose=createComposeRule()
    @Test fun tabsGenauigkeitUndIterationsnachweis() {
        val x=Variable("x")
        val term=subtraktion(Potenz(x,RationaleZahl.von(2)),RationaleZahl.von(2))
        val problem=PolynomGleichungsProblem(Gleichheit(term,RationaleZahl.Null),listOf(x),listOf(term),false)
        val state=mutableStateOf(KnotenAuswertungsErgebnis(emptyMap(),polynomProblem=problem))
        compose.setContent { MaterialTheme { PrädikatsRechenwegDialog("Polynom",state.value,{ state.value=state.value.copy(polynomKonfiguration=it) }) {} } }
        compose.onNodeWithText("Analytische Lösung").assertIsDisplayed()
        compose.onNodeWithText("Numerische Lösung").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("x = 1,414213").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("x = 1,414213").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Nachkommastellen n (0–100)").performScrollTo().performTextReplacement("8")
        compose.onNodeWithText("Berechnen").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("x = 1,41421356").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("x = 1,41421356").performScrollTo().assertIsDisplayed()
    }
    @Test fun familienParameterUndGeaenderteGenauigkeit() {
        fun q(n:Long)=RationaleZahl.von(n)
        val z=Matrix(listOf(listOf(q(0),q(0)),listOf(q(0),q(0))))
        val b=Matrix(listOf(listOf(q(1),q(0)),listOf(q(0),q(0))))
        val problem=matrixPolynomProblem(z,b,z)
        val state=mutableStateOf(KnotenAuswertungsErgebnis(emptyMap(),polynomProblem=problem))
        compose.setContent { MaterialTheme { PrädikatsRechenwegDialog("Familie",state.value,{ state.value=state.value.copy(polynomKonfiguration=it) }) {} } }
        compose.onNodeWithText("Numerische Lösung").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Familienmitglied berechnen").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("t_1 (ganze Zahl oder Bruch)").performScrollTo().performTextReplacement("1/3")
        compose.onNodeWithText("Familienmitglied berechnen").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("X_1_2 = 0,333333").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Nachkommastellen n (0–100)").performScrollTo().performTextReplacement("8")
        compose.onNodeWithText("Berechnen").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Familienmitglied berechnen").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Familienmitglied berechnen").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("X_1_2 = 0,33333333").fetchSemanticsNodes().isNotEmpty() }
    }

}

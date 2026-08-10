package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import de.TeutonStudio.MathematikKnoten.RAND_KNOTEN_ART
import de.TeutonStudio.MathematikKnoten.RECHNER_OPERATOR_PARAMETER
import de.TeutonStudio.MathematikKnoten.TANGENTIAL_KNOTEN_ART
import de.TeutonStudio.MathematikRechenSystem.kern.MatrixRechner
import de.TeutonStudio.MathematikRechenSystem.kern.MatrixRechnerOperator

internal fun kleineIssuesKonzeptFürKnoten(knoten: KnotenDaten): KonzeptDefinition? = when {
    knoten.art == RAND_KNOTEN_ART -> randKonzept()
    knoten.art == TANGENTIAL_KNOTEN_ART -> tangentialKonzept()
    knoten.art == MatrixRechner.KNOTEN_ART &&
        knoten.parameter[RECHNER_OPERATOR_PARAMETER] == MatrixRechnerOperator.DETERMINANTE.stabileId -> determinantKonzept()
    knoten.art == MatrixRechner.KNOTEN_ART &&
        knoten.parameter[RECHNER_OPERATOR_PARAMETER] == MatrixRechnerOperator.MINIMALPOLYNOM.stabileId -> minimalpolynomKonzept()
    knoten.art == MatrixRechner.KNOTEN_ART &&
        knoten.parameter[RECHNER_OPERATOR_PARAMETER] == MatrixRechnerOperator.CHARAKTERISTISCHES_POLYNOM.stabileId -> charakteristischesPolynomKonzept()
    else -> null
}

private fun randKonzept() = KonzeptDefinition(
    id = KonzeptId("topologie-rand"),
    name = "Topologischer Rand",
    beschreibung = "Der Rand einer Menge hängt von Topologie und Umgebungsraum ab.",
    pfad = listOf("Topologie", "Mengenoperationen"),
    tags = setOf("Rand", "Topologie", "Abschluss", "Inneres", "relativ"),
    knotenArten = setOf(RAND_KNOTEN_ART),
    reiter = listOf(
        regelReiter("definition", "Definition", KonzeptReiterRolle.Definition,
            "∂_X A = cl_X(A) \\operatorname{\\setminus} int_X(A)",
            "Topologie und Umgebungsraum X sind Teil des Begriffs; ohne Kontext ist der Rand nicht implizit absolut."),
        regelReiter("aequivalent", "Äquivalente Form", KonzeptReiterRolle.Äquivalenz,
            "∂_X A = cl_X(A) ∩ cl_X(X \\setminus A)",
            "Ein Randpunkt besitzt in jeder Umgebung Punkte aus A und aus seinem Komplement."),
        regelReiter("relativ", "Relativer Rand", KonzeptReiterRolle.Spezialfall,
            "Bei A ⊆ X wird der Rand relativ zu X gebildet.",
            "Ändert sich X, kann sich auch ∂_X A ändern."),
        regelReiter("beispiele", "Beispiele", KonzeptReiterRolle.Beispiel,
            "In der diskreten Topologie ist jeder Rand leer.",
            "Für ℚ ⊂ ℝ mit der üblichen Topologie gilt ∂_ℝℚ = ℝ."),
    ),
)

private fun tangentialKonzept() = KonzeptDefinition(
    id = KonzeptId("analysis-tangentialobjekt"),
    name = "Tangentialobjekt",
    beschreibung = "Ein gemeinsamer Knoten für analytische Tangentialmethode und geometrische Tangentialmenge.",
    pfad = listOf("Analysis", "Differentialrechnung"),
    tags = setOf("Tangente", "Tangentialraum", "Jacobi-Matrix", "Fréchet"),
    knotenArten = setOf(TANGENTIAL_KNOTEN_ART),
    reiter = listOf(
        regelReiter("definition", "Definition", KonzeptReiterRolle.Definition,
            "t_a(x)=f(a)+Df(a)(x-a)",
            "Die Ausgabe kann im Inspector als Methode oder als geometrische Menge gewählt werden."),
        regelReiter("skalar", "Skalarer Fall", KonzeptReiterRolle.Spezialfall,
            "Für f:ℝ→ℝ ist t_a(x)=f(a)+f'(a)(x-a).",
            "Der Methodenausgang ist die affine Tangentenfunktion."),
        regelReiter("vektor", "Mehrdimensional", KonzeptReiterRolle.Spezialfall,
            "Für f:ℝⁿ→ℝᵐ wird Df(a) durch die Jacobi-Abbildung beschrieben.",
            "Die Tangentialmenge ist die zugehörige affine lineare Näherung."),
        regelReiter("geometrie", "Geometrische Sicht", KonzeptReiterRolle.Beispiel,
            "Kurven liefern Tangentialgeraden, Flächen Tangentialebenen und Mannigfaltigkeiten Tangentialräume.",
            "Nicht als Methode ausdrückbare Fälle bleiben strukturiert statt künstlich auf eine Gerade reduziert."),
    ),
)

private fun determinantKonzept() = KonzeptDefinition(
    id = KonzeptId("lineare-algebra-determinante-definitionen"),
    name = "Determinante",
    beschreibung = "Mehrere äquivalente Definitionen derselben produktiven Determinantenoperation.",
    pfad = listOf("Lineare Algebra", "Matrizen"),
    tags = setOf("Determinante", "Leibniz", "Laplace", "Kofaktor", "Geometrie"),
    knotenArten = setOf(MatrixRechner.KNOTEN_ART),
    knotenParameter = mapOf(RECHNER_OPERATOR_PARAMETER to MatrixRechnerOperator.DETERMINANTE.stabileId),
    reiter = listOf(
        regelReiter("definition", "Definition", KonzeptReiterRolle.Definition,
            "det(A) ist die alternierende normierte multilineare Volumenform der Spalten von A.",
            "Alle folgenden Reiter beschreiben dieselbe im Matrixrechner verwendete Operation."),
        regelReiter("permutation", "Permutationsformel", KonzeptReiterRolle.Äquivalenz,
            "det(A)=∑_{σ∈S_n} sgn(σ) ∏_{i=1}^n a_{i,σ(i)}",
            "Die Implementierung kann diese Formel für kleine exakte Matrizen gegen den produktiven Determinantenkern prüfen."),
        regelReiter("laplace", "Laplace", KonzeptReiterRolle.Äquivalenz,
            "det(A)=∑_j a_{ij} C_{ij},  C_{ij}=(-1)^{i+j} det(M_{ij})",
            "Minor und Kofaktor bleiben strukturierte Zwischenobjekte."),
        regelReiter("zeilen", "Zeilenumformungen", KonzeptReiterRolle.Spezialfall,
            "Zeilentausch ändert das Vorzeichen; Skalierung einer Zeile skaliert det; Addition eines Vielfachen einer anderen Zeile erhält det.",
            "Diese Sicht knüpft an die Gauß- und Umformungsprotokolle des Atlas an."),
        regelReiter("geometrie", "Geometrie", KonzeptReiterRolle.Spezialfall,
            "|det(A)| ist im reellen Fall der orientierungslose Volumenskalierungsfaktor.",
            "Das Vorzeichen beschreibt die Orientierung; diese Deutung wird nicht blind auf beliebige Zahlkörper übertragen."),
        regelReiter("beispiel", "Beispiel", KonzeptReiterRolle.Beispiel,
            "Für A=[[a,b],[c,d]] gilt det(A)=ad-bc.",
            "Die Definitionsbausteine und der produktive Operator müssen denselben Wert liefern."),
    ),
)

private fun minimalpolynomKonzept() = KonzeptDefinition(
    id = KonzeptId("lineare-algebra-minimalpolynom"),
    name = "Minimalpolynom",
    beschreibung = "Das normierte Polynom kleinsten Grades, das eine quadratische Matrix annulliert.",
    pfad = listOf("Lineare Algebra", "Polynome von Matrizen"),
    tags = setOf("Minimalpolynom", "Cayley-Hamilton", "charakteristisches Polynom"),
    knotenArten = setOf(MatrixRechner.KNOTEN_ART),
    knotenParameter = mapOf(RECHNER_OPERATOR_PARAMETER to MatrixRechnerOperator.MINIMALPOLYNOM.stabileId),
    reiter = listOf(
        regelReiter("definition", "Definition", KonzeptReiterRolle.Definition,
            "m_A ist normiert, m_A(A)=0 und besitzt unter allen solchen Polynomen minimalen Grad.",
            "Der Matrixrechner gibt m_A als Polynom-Methode aus."),
        regelReiter("cayley-hamilton", "Cayley-Hamilton", KonzeptReiterRolle.Äquivalenz,
            "χ_A(A)=0 und m_A teilt χ_A.",
            "Der Atlas prüft die Annullierung und Teilbarkeit mit demselben Matrixpolynomkern."),
        regelReiter("reduktion", "Polynomreduktion", KonzeptReiterRolle.Spezialfall,
            "Jedes Polynom in A kann modulo m_A auf Grad < deg(m_A) reduziert werden.",
            "Das erklärt, warum hohe Matrixpotenzen in einer endlichen Basis niedriger Potenzen darstellbar sind."),
        regelReiter("beispiel", "Beispiel", KonzeptReiterRolle.Beispiel,
            "Für A=2I gilt m_A(x)=x-2, während χ_A(x)=(x-2)^n.",
            "Minimal- und charakteristisches Polynom müssen also nicht denselben Grad besitzen."),
    ),
)

private fun charakteristischesPolynomKonzept() = KonzeptDefinition(
    id = KonzeptId("lineare-algebra-charakteristisches-polynom"),
    name = "Charakteristisches Polynom",
    beschreibung = "χ_A(λ)=det(λI-A) verbindet Determinante, Eigenwerte und Minimalpolynom.",
    pfad = listOf("Lineare Algebra", "Polynome von Matrizen"),
    tags = setOf("charakteristisches Polynom", "Determinante", "Cayley-Hamilton"),
    knotenArten = setOf(MatrixRechner.KNOTEN_ART),
    knotenParameter = mapOf(RECHNER_OPERATOR_PARAMETER to MatrixRechnerOperator.CHARAKTERISTISCHES_POLYNOM.stabileId),
    reiter = listOf(
        regelReiter("definition", "Definition", KonzeptReiterRolle.Definition,
            "χ_A(λ)=det(λI-A)",
            "Der Matrixrechner gibt das charakteristische Polynom als Polynom-Methode aus."),
        regelReiter("cayley-hamilton", "Cayley-Hamilton", KonzeptReiterRolle.Äquivalenz,
            "χ_A(A)=0",
            "Damit liefert χ_A immer ein annullierendes Polynom und eine obere Gradschranke für m_A."),
    ),
)

private fun regelReiter(
    id: String,
    titel: String,
    rolle: KonzeptReiterRolle,
    hauptregel: String,
    erklärung: String,
): KonzeptReiter = KonzeptReiter(
    id = id,
    titel = titel,
    rolle = rolle,
    karte = regelKarte("kleine-issues-$id-${hauptregel.hashCode().toUInt()}", titel, hauptregel, erklärung),
)

private fun regelKarte(id: String, titel: String, hauptregel: String, erklärung: String): KartenDaten = KartenDaten(
    id = KartenId(id),
    name = titel,
    knoten = listOf(
        KnotenDaten(
            id = KnotenId("$id-regel"),
            art = KonzeptKnotenArten.REGEL,
            name = titel,
            position = GraphPunkt(70f, 60f),
            größe = GraphGröße(700f, 220f),
            parameter = mapOf(
                "regel" to hauptregel,
                "erklärung" to erklärung,
            ),
        ),
    ),
)

package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.*

class KleineIssuesKernTest {
    @Test
    fun `Laplace und Permutationsdefinition stimmen mit produktiver Determinante ueberein`() {
        val matrix = Matrix(
            listOf(
                listOf(RationaleZahl.von(1), RationaleZahl.von(2), RationaleZahl.von(3)),
                listOf(RationaleZahl.von(0), RationaleZahl.von(4), RationaleZahl.von(5)),
                listOf(RationaleZahl.von(1), RationaleZahl.von(0), RationaleZahl.von(6)),
            ),
        )
        val direkt = vereinfache(produktiveDeterminante(matrix, FundamentalerZahlbereich.RATIONAL))
        val laplace = vereinfache(laplaceDeterminante(matrix, 0, true, FundamentalerZahlbereich.RATIONAL))
        val permutation = vereinfache(permutationsDeterminante(matrix))
        assertEquals(direkt, laplace)
        assertEquals(direkt, permutation)
    }

    @Test
    fun `Minor und Kofaktor verwenden stabile Matrixindizes`() {
        val matrix = Matrix(
            listOf(
                listOf(RationaleZahl.von(1), RationaleZahl.von(2)),
                listOf(RationaleZahl.von(3), RationaleZahl.von(4)),
            ),
        )
        assertEquals(Matrix(listOf(listOf(RationaleZahl.von(4)))), matrixMinor(matrix, 0, 0))
        assertEquals(RationaleZahl.von(-3), vereinfache(matrixKofaktor(matrix, 0, 1, FundamentalerZahlbereich.RATIONAL)))
    }

    @Test
    fun `charakteristisches Polynom einer Diagonalmatrix ist exakt`() {
        val matrix = Matrix(
            listOf(
                listOf(RationaleZahl.von(2), RationaleZahl.Null),
                listOf(RationaleZahl.Null, RationaleZahl.von(3)),
            ),
        )
        val polynom = charakteristischesPolynom(matrix)
        assertEquals(2, polynom.grad)
        assertEquals(
            listOf(RationaleZahl.von(6), RationaleZahl.von(-5), RationaleZahl.Eins),
            polynom.koeffizienten,
        )
    }

    @Test
    fun `Minimalpolynom teilt charakteristisches und annulliert Matrix`() {
        val matrix = Matrix(
            listOf(
                listOf(RationaleZahl.von(2), RationaleZahl.Null),
                listOf(RationaleZahl.Null, RationaleZahl.von(2)),
            ),
        )
        val minimal = minimalPolynom(matrix)
        assertEquals(listOf(RationaleZahl.von(-2), RationaleZahl.Eins), minimal.koeffizienten)
        assertTrue(teiltMinimalpolynomDasCharakteristische(matrix))
        assertTrue(pruefeCayleyHamilton(matrix).gilt)
    }

    @Test
    fun `diskrete Topologie hat fuer jede Teilmenge leeren Rand`() {
        val menge = EndlicheMenge(setOf(RationaleZahl.Eins, RationaleZahl.von(2)))
        val rand = topologischerRand(
            menge,
            TopologischerKontext(ReelleZahlen, TopologieArt.DISKRET),
        )
        assertEquals(LeereMenge, rand)
    }

    @Test
    fun `Q hat in R den Rand R`() {
        val rand = topologischerRand(
            RationaleZahlen,
            TopologischerKontext(ReelleZahlen, TopologieArt.KANONISCH_REELL),
        )
        assertEquals(ReelleZahlen, rand)
    }

    @Test
    fun `symbolischer Rand bewahrt Umgebungsraum`() {
        val menge = BenannteMenge("A", "A")
        val raum = BenannteMenge("X", "X")
        val rand = assertIs<TopologischerRand>(
            topologischerRand(menge, TopologischerKontext(raum, TopologieArt.SYMBOLISCH, relativ = true)),
        )
        assertEquals(raum, rand.kontext.umgebungsraum)
        assertEquals("\\partial_{X} A", rand.zuLatex())
    }

    @Test
    fun `Tangentialmethode von x quadrat bei eins ist 2x minus 1`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = potenz(x, RationaleZahl.von(2)),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )
        val ergebnis = assertIs<TangentialErgebnis.MethodeWert>(
            tangentialObjekt(methode, RationaleZahl.Eins, TangentialAusgabeForm.METHODE),
        )
        val beiNull = assertIs<RationaleZahl>(ergebnis.methode.wendeAn(listOf(RationaleZahl.Null)))
        val beiZwei = assertIs<RationaleZahl>(ergebnis.methode.wendeAn(listOf(RationaleZahl.von(2))))
        assertEquals(RationaleZahl.von(-1), beiNull)
        assertEquals(RationaleZahl.von(3), beiZwei)
    }

    @Test
    fun `mehrstellige Tangente bleibt als strukturierte Menge erhalten`() {
        val x = Variable("x")
        val y = Variable("y")
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            vorschrift = addition(x, y),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen, "y" to ReelleZahlen),
        )
        val ergebnis = assertIs<TangentialErgebnis.MengeWert>(
            tangentialObjekt(
                methode,
                Tupel(listOf(RationaleZahl.Null, RationaleZahl.Null)),
                TangentialAusgabeForm.MENGE,
            ),
        )
        assertIs<TangentialMenge>(ergebnis.menge)
    }
}

package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SkalarproduktPruefungTest {
    @Test
    fun `Falk Schema konjugiert bei rechtslinearer Konvention den linken Faktor`() {
        val schema = SkalarproduktFalkSchema(
            dimension = 3,
            linearitaet = SkalarproduktLinearitaet.RECHTSLINEAR,
            konjugiert = true,
        )

        assertEquals(
            "\\left\\langle u,v\\right\\rangle=\\sum_{i=0}^{2}\\overline{u_{i}}\\,v_{i}",
            schema.zuLatex(),
        )
    }

    @Test
    fun `Falk Schema konjugiert bei linkslinearer Konvention den rechten Faktor`() {
        val schema = SkalarproduktFalkSchema(
            dimension = 2,
            linearitaet = SkalarproduktLinearitaet.LINKSLINEAR,
            konjugiert = true,
        )

        assertEquals(
            "\\left\\langle u,v\\right\\rangle=\\sum_{i=0}^{1}u_{i}\\,\\overline{v_{i}}",
            schema.zuLatex(),
        )
    }

    @Test
    fun `fehlende Nachweisreferenzen erzeugen kein ausführbares Zeugnis`() {
        val aussage = pruefeSkalarprodukt(
            methode = reelleMultiplikation(),
            zahlbereich = FundamentalerZahlbereich.REELL,
            linearitaet = SkalarproduktLinearitaet.RECHTSLINEAR,
            referenzen = SkalarproduktNachweisReferenzen(),
        )

        assertEquals(NachweisStatus.Unvollstaendig, aussage.pruefung.status)
        assertNull(aussage.pruefung.zeugnis)
        assertTrue(aussage.pruefung.axiomPruefungen.count { it.status == NachweisStatus.Unvollstaendig } >= 3)
    }

    @Test
    fun `vollständige Referenzen erzeugen ein versionsfestes Skalarproduktzeugnis`() {
        val aussage = pruefeSkalarprodukt(
            methode = reelleMultiplikation(),
            zahlbereich = FundamentalerZahlbereich.REELL,
            linearitaet = SkalarproduktLinearitaet.RECHTSLINEAR,
            referenzen = SkalarproduktNachweisReferenzen(
                linearitaet = "lemma.linearitaet",
                konjugierteSymmetrie = "lemma.symmetrie",
                positivDefinit = "lemma.positiv",
            ),
        )

        assertEquals(NachweisStatus.Nachgewiesen, aussage.pruefung.status)
        val zeugnis = assertIs<SkalarproduktZeugnis>(aussage.pruefung.zeugnis)
        assertEquals(SKALARPRODUKT_ZERTIFIKAT_VERSION, zeugnis.zertifikatVersion)
        assertEquals(FundamentalerZahlbereich.REELL, zeugnis.zahlbereich)
        assertEquals(setOf("lemma.linearitaet", "lemma.symmetrie", "lemma.positiv"), zeugnis.referenzen)
    }

    @Test
    fun `veraltete Zertifikatversion bleibt unvollständig`() {
        val aussage = pruefeSkalarprodukt(
            methode = reelleMultiplikation(),
            zahlbereich = FundamentalerZahlbereich.REELL,
            linearitaet = SkalarproduktLinearitaet.RECHTSLINEAR,
            referenzen = SkalarproduktNachweisReferenzen("l", "s", "p"),
            zertifikatVersion = SKALARPRODUKT_ZERTIFIKAT_VERSION + 1,
        )

        assertEquals(NachweisStatus.Unvollstaendig, aussage.pruefung.status)
        assertNull(aussage.pruefung.zeugnis)
    }

    private fun reelleMultiplikation(): Methode {
        val links = Variable("u")
        val rechts = Variable("v")
        return Methode(
            name = "g",
            parameter = listOf(links, rechts),
            vorschrift = multiplikation(links, rechts),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(
                links.name to ReelleZahlen,
                rechts.name to ReelleZahlen,
            ),
        )
    }
}

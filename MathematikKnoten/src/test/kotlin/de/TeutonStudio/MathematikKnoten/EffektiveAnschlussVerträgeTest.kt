package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.TypSystem.*
import de.TeutonStudio.MathematikKartenAdapter.*
import kotlin.test.*

class EffektiveAnschlussVerträgeTest {
    private fun prüfung(): GraphPrüfung {
        val arten = AnschlussArtRegister(MathematikAnschlussArten.alle)
        return GraphPrüfung(arten, erzeugeMathematikKartenTypSystem(arten), ::effektiverMathematikAnschluss)
    }
    private fun ref(k: KnotenDaten) = AnschlussVerweis(k.id, k.anschlüsse.first().id)
    private fun parameter(bereich: WertebereichKonfiguration) = MathematikKnotenVorlagen.AllgemeinerParameter.erzeuge(GraphPunkt.Zero)
        .copy(eigenschaften = mapOf(WertebereichKonfiguration.EIGENSCHAFT to bereich.zuEigenschaft()))
    private fun ziel(art: AnschlussArtId, typ: TypAusdruck = TypAusdruck.Unbekannt) = KnotenDaten(
        art = "test.ziel", name = "Ziel", anschlüsse = listOf(AnschlussDaten(
            name = "wert", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = art,
            vertrag = AnschlussVertrag(typ = typ),
        )),
    )
    @Test fun `allgemeiner Parameter projiziert Menge und Matrixform aus gespeichertem Bereich`() {
        val menge = parameter(WertebereichKonfiguration.Menge(WertebereichKonfiguration.Zahl("R")))
        val karte = KartenDaten(name = "Test", knoten = listOf(menge))
        assertEquals(MathematikAnschlussArten.Menge.id, prüfung().effektiveArt(karte, ref(menge)))
        assertEquals(TypAusdruck.Parameterisiert(MathematischeTypen.Menge, listOf(TypAusdruck.Atom(MathematischeTypen.Reell))), prüfung().effektiverTyp(karte, ref(menge)))
        assertIs<Potenzmenge>(WertebereichKonfiguration.Menge().zuMenge())
    }
    @Test fun `Formwiderspruch wird trotz gleicher grober Anschlussart abgewiesen`() {
        val quelle = parameter(WertebereichKonfiguration.Vektor(dimension = 3))
        val z = ziel(MathematikAnschlussArten.SpaltenVektor.id, TypAusdruck.Parameterisiert(MathematischeTypen.SpaltenVektor,
            listOf(TypAusdruck.Atom(MathematischeTypen.Reell), TypAusdruck.Literal("2"))))
        assertIs<VerbindungsPrüfung.Abgelehnt>(prüfung().prüfe(KartenDaten(name = "Test", knoten = listOf(quelle, z)), ref(quelle), ref(z)))
    }
    @Test fun `erhaltener Anschluss verliert Verbindung erst nach atomarer bestaetigter Uebernahme`() {
        val quelle = parameter(WertebereichKonfiguration.Zahl())
        val z = ziel(MathematikAnschlussArten.Zahl.id)
        val edge = VerbindungDaten(von = ref(quelle), zu = ref(z))
        val karte = KartenDaten(name = "Test", knoten = listOf(quelle, z), verbindungen = listOf(edge))
        val neu = quelle.copy(eigenschaften = mapOf(WertebereichKonfiguration.EIGENSCHAFT to WertebereichKonfiguration.Vektor().zuEigenschaft()))
        val vorschau = karte.vorschauKnotenErsetzen(neu, prüfung())
        assertEquals(listOf(edge), vorschau.entfallendeVerbindungen)
        assertEquals(quelle.anschlüsse.map { it.id }, neu.anschlüsse.map { it.id })
        assertEquals(listOf(edge), karte.verbindungen)
        val editor = KartenEditorZustand(karte, prüfung())
        editor.führeAus(KartenAktion.KnotenErsetzen(neu, vorschau.entfallendeVerbindungen.map { it.id }.toSet()))
        assertTrue(editor.karte.verbindungen.isEmpty())
        assertEquals(neu.eigenschaften, editor.karte.knoten.first().eigenschaften)
        editor.rückgängig()
        assertEquals(karte, editor.karte)
        editor.wiederholen()
        assertTrue(editor.karte.verbindungen.isEmpty())
    }
    @Test fun `Variable verwendet konfigurierten Zahlenbereich statt Legacyfallback`() {
        val q = MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero).copy(parameter = mapOf("werteVorrat" to "C"))
        val z = ziel(MathematikAnschlussArten.Zahl.id, TypAusdruck.Atom(MathematischeTypen.Reell))
        val karte = KartenDaten(name = "Test", knoten = listOf(q, z))
        assertEquals(TypAusdruck.Atom(MathematischeTypen.Komplex), prüfung().effektiverTyp(karte, ref(q)))
        assertIs<VerbindungsPrüfung.Abgelehnt>(prüfung().prüfe(karte, ref(q), ref(z)))
    }
    @Test fun `Vektorkonstruktoren passen an entsprechende Matrixzeilen und Spalten`() {
        listOf(VEKTOR_ORIENTIERUNG_SPALTE, VEKTOR_ORIENTIERUNG_ZEILE).forEach { orientierung ->
            val q = konfiguriereVektorKonstruktor(MathematikKnotenVorlagen.Vektor.erzeuge(GraphPunkt.Zero), orientierung = orientierung)
            val matrix = konfiguriereMatrix(MathematikKnotenVorlagen.Matrix.erzeuge(GraphPunkt.Zero),
                if (orientierung == VEKTOR_ORIENTIERUNG_SPALTE) MATRIX_SPALTEN else MATRIX_ZEILEN, 2, 2)
            val quelle = AnschlussVerweis(q.id, q.anschlüsse.first { it.richtung == AnschlussRichtung.Ausgang }.id)
            val eingang = AnschlussVerweis(matrix.id, matrix.anschlüsse.first { it.richtung == AnschlussRichtung.Eingang }.id)
            assertIs<VerbindungsPrüfung.Erlaubt>(prüfung().prüfe(KartenDaten(name = "Test", knoten = listOf(q, matrix)), quelle, eingang))
        }
    }
    @Test fun `unbekannte Tensorform bleibt unbekannt und wird nicht zum atomaren Widerspruch`() {
        val q = KnotenDaten(art = de.TeutonStudio.MathematikKartenAdapter.MENGENKONSTRUKTOR_ART, name = "T", parameter = mapOf(
            de.TeutonStudio.MathematikKartenAdapter.MENGENDEFINITION_ELEMENTART to MathematikAnschlussArten.Tensor.id.wert),
            anschlüsse = listOf(AnschlussDaten(name = "element", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Tensor.id)))
        val karte = KartenDaten(name = "Test", knoten = listOf(q))
        assertEquals(TypAusdruck.Unbekannt, prüfung().effektiverTyp(karte, ref(q)))
    }

    @Test fun `verbundene konstante Formdimensionen weisen bekannten Matrixwiderspruch ab`() {
        val q = konfiguriereMengenkonstruktor(MengendefinitionKnotenVorlagen.Mengenkonstruktor.erzeuge(GraphPunkt.Zero),
            MathematikAnschlussArten.Matrix.id, STRUKTURFORM_EINGANG)
        val zahlen = listOf(3, 2).map { n -> MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero).copy(parameter = mapOf("wert" to n.toString())) }
        val dimensionen = q.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }
        val z = ziel(MathematikAnschlussArten.Matrix.id, TypAusdruck.Parameterisiert(MathematischeTypen.Matrix,
            listOf(TypAusdruck.Atom(MathematischeTypen.Zahl), TypAusdruck.Literal("2"), TypAusdruck.Literal("2"))))
        val karte = KartenDaten(name = "Form", knoten = listOf(q, z) + zahlen, verbindungen = zahlen.zip(dimensionen).map { (zahl, eingang) ->
            VerbindungDaten(von = ref(zahl), zu = AnschlussVerweis(q.id, eingang.id))
        })
        val quelle = AnschlussVerweis(q.id, q.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }.id)
        assertIs<VerbindungsPrüfung.Abgelehnt>(prüfung().prüfe(karte, quelle, ref(z)))
    }

}

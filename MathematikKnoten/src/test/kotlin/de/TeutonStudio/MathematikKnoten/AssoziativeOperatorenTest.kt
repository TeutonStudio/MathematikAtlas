package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikRechenSystem.kern.KomplexeZahl
import de.TeutonStudio.MathematikRechenSystem.kern.Division
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import de.TeutonStudio.MathematikRechenSystem.kern.ReelleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.Variable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AssoziativeOperatorenTest {
    @Test
    fun `unverbundene Additionsanschluesse werden zu eindeutigen Unbekannten`() {
        val addition = MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt.Zero)
        val auswerter = StandardMathematikAuswerter.erzeugeRegister().finde(addition.art)!!

        val ergebnis = auswerter.auswerten(KnotenAuswertungsKontext(addition, emptyMap(), RechenKontext()))

        assertEquals(
            "\\mathrm{Addition}_{\\mathrm{eingabe}_{1}} + \\mathrm{Addition}_{\\mathrm{eingabe}_{2}}",
            ergebnis.ausgaben.getValue("wert").objekt.zuLatex(),
        )
    }

    @Test
    fun `gleiche Anschlusspositionen unterschiedlicher Knoten bleiben verschiedene Unbekannte`() {
        val erster = MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt.Zero)
        val zweiter = MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt.Zero)
        val auswerter = StandardMathematikAuswerter.erzeugeRegister().finde(erster.art)!!

        val erstesErgebnis = auswerter.auswerten(KnotenAuswertungsKontext(erster, emptyMap(), RechenKontext()))
        val zweitesErgebnis = auswerter.auswerten(KnotenAuswertungsKontext(zweiter, emptyMap(), RechenKontext()))

        check(erstesErgebnis.ausgaben.getValue("wert").objekt != zweitesErgebnis.ausgaben.getValue("wert").objekt)
    }

    @Test
    fun `Maximum und Minimum sind getrennte Vorlagen eines gemeinsamen Typs`() {
        val maximum = MathematikKnotenVorlagen.Maximum.erzeuge(GraphPunkt.Zero)
        val minimum = MathematikKnotenVorlagen.Minimum.erzeuge(GraphPunkt.Zero)

        assertEquals("mathematik.extremwert", maximum.art)
        assertEquals(maximum.art, minimum.art)
        assertEquals("maximum", maximum.parameter.getValue("modus"))
        assertEquals("minimum", minimum.parameter.getValue("modus"))
        assertEquals(listOf("a", "b"), maximum.anschlüsse.filter { it.richtung.name == "Eingang" }.map { it.name })
        assertEquals(true, maximum.anschlüsse.first().kannSichErweitern)
    }

    @Test
    fun `Extremwert akzeptiert nachweisbar reelle Variablen und lehnt komplexe Werte ab`() {
        val knoten = MathematikKnotenVorlagen.Maximum.erzeuge(GraphPunkt.Zero)
        val auswerter = StandardMathematikAuswerter.erzeugeRegister().finde(knoten.art)!!
        val ergebnis = auswerter.auswerten(KnotenAuswertungsKontext(
            knoten,
            mapOf(
                "a" to BedingterWert(Variable("x"), werteVorrat = ReelleZahlen),
                "b" to BedingterWert(RationaleZahl.von(3)),
            ),
            RechenKontext(),
        ))

        assertEquals("\\max\\left\\{x,3\\right\\}", ergebnis.ausgaben.getValue("wert").objekt.zuLatex())
        assertIs<de.TeutonStudio.MathematikRechenSystem.kern.Maximum>(ergebnis.ausgaben.getValue("wert").objekt)
        assertFailsWith<IllegalArgumentException> {
            auswerter.auswerten(KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "a" to BedingterWert(KomplexeZahl(RationaleZahl.Eins, RationaleZahl.Eins)),
                    "b" to BedingterWert(RationaleZahl.von(3)),
                ),
                RechenKontext(),
            ))
        }
    }

    @Test
    fun `Reellheitsnachweis wird über Addition an den Extremwert weitergegeben`() {
        val addition = MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt.Zero)
        val maximum = MathematikKnotenVorlagen.Maximum.erzeuge(GraphPunkt.Zero)
        val register = StandardMathematikAuswerter.erzeugeRegister()
        val summe = register.finde(addition.art)!!.auswerten(KnotenAuswertungsKontext(
            addition,
            mapOf(
                "a" to BedingterWert(Variable("x"), werteVorrat = ReelleZahlen),
                "b" to BedingterWert(RationaleZahl.Eins),
            ),
            RechenKontext(),
        )).ausgaben.getValue("wert")

        val ergebnis = register.finde(maximum.art)!!.auswerten(KnotenAuswertungsKontext(
            maximum,
            mapOf("a" to summe, "b" to BedingterWert(RationaleZahl.von(3))),
            RechenKontext(),
        ))

        assertIs<de.TeutonStudio.MathematikRechenSystem.kern.Maximum>(ergebnis.ausgaben.getValue("wert").objekt)
    }

    @Test
    fun `Extremwert lehnt Division durch null trotz mitgeführter Annahme ab`() {
        val knoten = MathematikKnotenVorlagen.Maximum.erzeuge(GraphPunkt.Zero)
        val auswerter = StandardMathematikAuswerter.erzeugeRegister().finde(knoten.art)!!
        val undefiniert = BedingterWert(
            Division(RationaleZahl.Eins, RationaleZahl.Null),
            setOf(de.TeutonStudio.MathematikRechenSystem.kern.Ungleichheit(RationaleZahl.Null, RationaleZahl.Null)),
        )

        assertFailsWith<IllegalArgumentException> {
            auswerter.auswerten(KnotenAuswertungsKontext(
                knoten,
                mapOf("a" to undefiniert, "b" to BedingterWert(RationaleZahl.Eins)),
                RechenKontext(),
            ))
        }
    }

    @Test
    fun `Extremwert erzeugt und bereinigt dynamischen dritten Eingang mit Undo Redo`() {
        val maximum = MathematikKnotenVorlagen.Maximum.erzeuge(GraphPunkt.Zero)
        val quellen = (1..3).map { index -> KnotenDaten(
            art = "test.zahl", name = "q$index",
            anschlüsse = listOf(AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Zahl.id)),
        ) }
        val zustand = KartenEditorZustand(
            KartenDaten(name = "Test", knoten = quellen + maximum),
            GraphPrüfung(AnschlussArtRegister(MathematikAnschlussArten.alle)),
        )
        fun ref(knoten: KnotenDaten, name: String = "wert") = AnschlussVerweis(knoten.id, knoten.anschlüsse.first { it.name == name }.id)

        listOf("a", "b").forEachIndexed { index, name ->
            zustand.beginneVerbindung(ref(quellen[index]))
            zustand.anschlussAngeklickt(ref(maximum, name))
        }
        zustand.beginneVerbindung(ref(quellen[2]))
        val dynamisch = zustand.karte.knoten.first { it.id == maximum.id }.anschlüsse.single { it.dynamischErzeugt }
        zustand.anschlussAngeklickt(AnschlussVerweis(maximum.id, dynamisch.id))
        val verbindung = zustand.karte.verbindungen.single { it.zu.anschlussId == dynamisch.id }

        zustand.führeAus(KartenAktion.VerbindungLöschen(verbindung.id))
        assertEquals(2, zustand.karte.knoten.first { it.id == maximum.id }.anschlüsse.count { it.richtung == AnschlussRichtung.Eingang })
        zustand.rückgängig()
        assertTrue(zustand.karte.knoten.first { it.id == maximum.id }.anschlüsse.any { it.id == dynamisch.id && it.dynamischErzeugt })
        zustand.wiederholen()
        assertFalse(zustand.karte.knoten.first { it.id == maximum.id }.anschlüsse.any { it.id == dynamisch.id })
    }
}

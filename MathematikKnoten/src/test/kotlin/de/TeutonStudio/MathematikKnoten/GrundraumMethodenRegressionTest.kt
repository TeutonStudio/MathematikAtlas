package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.TypSystem.TypAusdruck
import kotlin.test.*

class GrundraumMethodenRegressionTest {
    private fun importiere(datei: String): KartenDaten = MathematikKartenCodec.importiere(
        javaClass.getResource("/regression/$datei")!!.readText())
    private fun z(n: Long) = RationaleZahl.von(n)
    private val register = GesamterMathematikAuswerter.erzeugeRegister()
    private fun eval(k: KnotenDaten, eingänge: Map<String, BedingterWert>) =
        register.finde(k.art)!!.auswerten(KnotenAuswertungsKontext(k, eingänge, RechenKontext()))

    @Test fun `Nutzerkarte filtert ein Tupel in beiden Projektionen und Uebersetzervarianten`() {
        val original = importiere("tupel-filter-v9.json")
        for (projektion in listOf("tupel", "separiert")) for (name in listOf("Term zu Methode", "Aussage zu Prädikat")) {
            val quelle = original.knoten.single { it.name == name }
            val filter = original.knoten.single { it.art == "mathematik.mengenfilter" }
            val methodenEingang = filter.anschlüsse.single { it.name == "methode" }
            val ausgang = quelle.anschlüsse.single { it.name == "methode" }
            val karte = original.copy(knoten = original.knoten.map {
                if (it.id == quelle.id) it.copy(parameter = it.parameter + ("methodenAusgang.methode.argumentprojektion" to projektion)) else it
            }, verbindungen = original.verbindungen.map {
                if (it.zu == AnschlussVerweis(filter.id, methodenEingang.id)) it.copy(von = AnschlussVerweis(quelle.id, ausgang.id)) else it
            })
            val laufzeit = MathematikKartenLaufzeit()
            val r = laufzeit.auswerten(karte)
            assertTrue(r.fehler.isEmpty(), r.fehler.joinToString())
            val methode = assertIs<MathematischeMethode>(r.knoten.getValue(quelle.id).ausgaben.getValue("methode").objekt)
            assertEquals(listOf("x"), methode.parameter.map { it.name })
            assertTrue(methode.istPrädikat())
            assertEquals(KartesischesProdukt(listOf(ReelleZahlen, ReelleZahlen)), methode.werteVorräte.getValue("x"))
            val menge = r.knoten.getValue(filter.id).ausgaben.getValue("menge").objekt as MengenAusdruck
            assertEquals(Wahrheitswert.Wahr, ElementBeziehung(Tupel(listOf(z(1), z(2))), menge).entscheide().wahrheitswert)
            assertEquals(Wahrheitswert.Lüge, ElementBeziehung(Tupel(listOf(z(5), z(2))), menge).entscheide().wahrheitswert)
            val synchron = synchronisiereMethodenAufrufe(karte, r, laufzeit.graphPrüfung)
            assertEquals("Aussage zu Prädikat", synchron.knoten.single { it.id == quelle.id }.name)
            val typ = synchron.knoten.single { it.id == quelle.id }.anschlüsse.single { it.name == "methode" }.vertrag.typ
            assertIs<TypAusdruck.Parameterisiert>(typ)
            assertEquals(karte.verbindungen, synchron.verbindungen)
        }
    }

    @Test fun `Nutzerkarte entscheidet Produkt gleich Potenzraum`() {
        val karte = importiere("produktraum-gleichheit-v9.json")
        val r = MathematikKartenLaufzeit().auswerten(karte)
        val gleichheit = karte.knoten.single { it.art == "mathematik.praedikat" }
        val aussage = r.knoten.getValue(gleichheit.id).ausgaben.getValue("aussage").objekt as Aussage
        assertEquals(Wahrheitswert.Wahr, aussage.entscheide().wahrheitswert)
        val auswerten = karte.knoten.single { it.art == "mathematik.auswerten" }
        assertEquals(WahrheitsKonstante(true), r.knoten.getValue(auswerten.id).ausgaben.getValue("term").objekt)
        val eingänge = r.knoten.getValue(gleichheit.id).eingänge
        assertEquals(Wahrheitswert.Wahr, entscheideGleichheit(eingänge.getValue("rechts").objekt as MathematischesObjekt,
            eingänge.getValue("links").objekt as MathematischesObjekt).wahrheitswert)
    }

    @Test fun `historische Grundraeume werden einmalig explizit und erhalten IDs`() {
        for (art in listOf("zahl", "tupel", "winkeltupel", "zeile", "spalte", "matrix", "intervall")) {
            val k = MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero).copy(eigenschaften = emptyMap(),
                parameter = mapOf("name" to "x", "werteVorrat" to "R", VARIABLE_STRUKTUR to if (art in listOf("matrix", "intervall")) "zahl" else art,
                    VARIABLE_DIMENSION to "2", MATRIX_VARIABLE_DIMENSION to if (art == "matrix") "2" else "0",
                    VARIABLE_BEREICH_MODUS to if (art == "intervall") VARIABLE_BEREICH_INTERVALL else VARIABLE_BEREICH_GRUNDMENGE))
            val alt = KartenDaten(name = "Legacy", knoten = listOf(k))
            val neu = MathematikKartenCodec.lade(KartenDatenJson.schreibe(alt))
            assertEquals(k.id, neu.knoten.first().id)
            assertEquals(k.anschlüsse.map { it.id }, neu.knoten.first().anschlüsse.map { it.id })
            assertEquals(2, neu.knoten.size)
            assertEquals(neu, MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(neu)))
            val ergebnis = MathematikKartenLaufzeit().auswerten(neu)
            assertTrue(ergebnis.fehler.isEmpty(), "$art: ${ergebnis.fehler}")
            assertEquals(historischerVariablenGrundraum(k, ReelleZahlen), ergebnis.knoten.getValue(k.id).ausgaben.getValue("wert").werteVorrat)
            val ohne = neu.copy(verbindungen = emptyList())
            assertEquals(ohne, MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(ohne)))
            assertTrue(MathematikKartenLaufzeit().auswerten(ohne).knoten.getValue(k.id).fehler!!.contains("Grundraum fehlt"))
        }
    }

    @Test fun `zwei unabhaengige Tupelvariablen gleichen Namens bleiben zwei Argumente`() {
        val raum = Tupelraum(listOf(ReelleZahlen, ReelleZahlen))
        val a = variablenGrundraumWert(MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero), raum)
        val b = variablenGrundraumWert(MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero), raum)
        val term = BedingterWert(Gleichheit(a.objekt as MathematischesObjekt, b.objekt as MathematischesObjekt),
            variablenQuellen = a.variablenQuellen + b.variablenQuellen)
        val methode = eval(MathematikKnotenVorlagen.TermZuMethode.erzeuge(GraphPunkt.Zero), mapOf("term" to term)).ausgaben.getValue("methode").objekt as MathematischeMethode
        assertEquals(setOf("x", "x_1"), methode.parameter.map { it.name }.toSet())
        assertEquals(2, methode.argumentIdentitäten.values.toSet().size)
    }

    @Test fun `gleichnamige skalare Quellen bleiben unabhaengig und endliche Zahlenraeume sind numerisch`() {
        val raum = EndlicheMenge(setOf(z(1), z(2)))
        val a = variablenGrundraumWert(MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero), raum)
        val b = variablenGrundraumWert(MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero), raum)
        assertIs<Variable>(a.objekt)
        assertNotEquals(a.objekt, b.objekt)
        val term = BedingterWert(Vergleich(a.objekt as ZahlAusdruck, VergleichsArt.Kleiner, b.objekt as ZahlAusdruck),
            variablenQuellen = a.variablenQuellen + b.variablenQuellen)
        val methode = eval(MathematikKnotenVorlagen.TermZuMethode.erzeuge(GraphPunkt.Zero), mapOf("term" to term)).ausgaben.getValue("methode").objekt as MathematischeMethode
        assertEquals(listOf("x", "x_1"), methode.parameter.map { it.name })
        assertEquals(Wahrheitswert.Wahr, (methode.wendeAn(listOf(z(1), z(2))) as Aussage).entscheide().wahrheitswert)
        val quadrat = RechnerOperatorRegistry.konfiguriere(ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero), "zahl.quadrat")
        val wert = eval(quadrat, mapOf("a" to a)).ausgaben.values.single()
        val m = eval(MathematikKnotenVorlagen.TermZuMethode.erzeuge(GraphPunkt.Zero), mapOf("term" to wert.copy(variablenQuellen = a.variablenQuellen)))
            .ausgaben.getValue("methode").objekt as MathematischeMethode
        assertEquals(z(4), vereinfache(m.wendeAn(listOf(z(2))) as ZahlAusdruck))
    }

    @Test fun `nullstellige und endliche Tupelgrundraeume erben ihre Struktur`() {
        val k = MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero)
        for (raum in listOf<MengenAusdruck>(Tupelraum(emptyList()), KartesischerPotenzraum(ReelleZahlen, z(0)))) {
            val wert = variablenGrundraumWert(k, raum)
            assertEquals(Tupel(emptyList()), wert.objekt)
            assertEquals(raum, wert.werteVorrat)
        }
        val raum = EndlicheMenge(setOf(Tupel(listOf(z(1), z(2))), Tupel(listOf(z(3), z(4)))))
        val wert = variablenGrundraumWert(k, raum)
        assertEquals(2, (wert.objekt as Tupel).elemente.size)
        assertTrue((wert.objekt as Tupel).elemente.all { it is ZahlAusdruck })
        assertEquals(1, wert.variablenQuellen.size)
        val m = eval(MathematikKnotenVorlagen.TermZuMethode.erzeuge(GraphPunkt.Zero), mapOf("term" to wert))
            .ausgaben.getValue("methode").objekt as MathematischeMethode
        assertEquals(listOf("x"), m.parameter.map { it.name })
        assertEquals(Tupel(listOf(z(3), z(4))), m.wendeAn(listOf(Tupel(listOf(z(3), z(4))))))
    }

    @Test fun `Winkelgrundraum liefert Winkelparameter und eigene Methodensignatur`() {
        val wert = variablenGrundraumWert(MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero), Gradmenge)
        assertIs<WinkelAusdruck>(wert.objekt)
        assertEquals("x", (wert.objekt as MathematischesObjekt).zuLatex())
        assertEquals(wert.objekt, MathematischerWertCodec.dekodieren(MathematischerWertCodec.kodieren(wert.objekt as MathematischesObjekt)))
        val methode = eval(MathematikKnotenVorlagen.TermZuMethode.erzeuge(GraphPunkt.Zero), mapOf("term" to wert))
            .ausgaben.getValue("methode").objekt as MathematischeMethode
        assertEquals(listOf("x"), methode.parameter.map { it.name })
        assertEquals(Gradmenge, methode.werteVorräte.getValue("x"))
    }

    @Test fun `individueller Uebersetzername bleibt und Standardname folgt der Ausgabe`() {
        val quelle = MathematikKnotenVorlagen.TermZuMethode.erzeuge(GraphPunkt.Zero)
        val p = MathematischeMethode("P", emptyList(), WahrheitsKonstante(true), Tupelraum(listOf(WahrheitsMenge)))
        val f = MathematischeMethode("f", emptyList(), z(1), Tupelraum(listOf(ReelleZahlen)))
        fun synchron(k: KnotenDaten, m: Methode): KnotenDaten {
            val karte = KartenDaten(name = "Name", knoten = listOf(k))
            val r = KartenAuswertungsErgebnis(mapOf(k.id to KnotenAuswertungsErgebnis(mapOf("methode" to BedingterWert(m)))), emptyList())
            return synchronisiereMethodenAufrufe(karte, r, MathematikKartenLaufzeit().graphPrüfung).knoten.single()
        }
        assertEquals("Aussage zu Prädikat", synchron(quelle, p).name)
        assertEquals("Term zu Methode", synchron(synchron(quelle, p), f).name)
        assertEquals("Mein Filter", synchron(quelle.copy(name = "Mein Filter"), p).name)
    }

    @Test fun `gehobener Rechner verliert Halbkreissignatur beim Wechsel zurueck zu Zahlen`() {
        val k = RechnerOperatorRegistry.konfiguriere(ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero), "zahl.quadrat")
        val x = Variable("x")
        val methode = MathematischeMethode("f", listOf(x), x, Tupelraum(listOf(ReelleZahlen)), mapOf("x" to ReelleZahlen))
        val laufzeit = MathematikKartenLaufzeit()
        fun synchron(knoten: KnotenDaten, wert: BedingterWert): KartenDaten {
            val r = eval(knoten, mapOf("a" to wert))
            return synchronisiereMethodenAufrufe(KartenDaten(name = "Hebung", knoten = listOf(knoten)),
                KartenAuswertungsErgebnis(mapOf(knoten.id to r), emptyList()), laufzeit.graphPrüfung)
        }
        val gehoben = synchron(k, BedingterWert(methode)).knoten.single()
        assertEquals(MathematischeTypen.Methode, (gehoben.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }.vertrag.typ as TypAusdruck.Parameterisiert).konstruktor)
        val normal = synchron(gehoben, BedingterWert(z(2))).knoten.single()
        assertEquals(TypAusdruck.Unbekannt, normal.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }.vertrag.typ)
        assertEquals(k.anschlüsse.map { it.id }, normal.anschlüsse.map { it.id })
    }

    @Test fun `fehlender Grundraum behaelt Folgeanschluesse und unabhaengige Verbindungen`() {
        val laufzeit = MathematikKartenLaufzeit()
        val original = importiere("tupel-filter-v9.json")
        val stabil = synchronisiereMethodenAufrufe(original, laufzeit.auswerten(original), laufzeit.graphPrüfung)
        val variable = stabil.knoten.single { it.art == "mathematik.variable" }
        val grund = variable.anschlüsse.single { it.name == "grundraum" }
        val ohne = stabil.copy(verbindungen = stabil.verbindungen.filterNot { it.zu == AnschlussVerweis(variable.id, grund.id) })
        val r = laufzeit.auswerten(ohne)
        assertTrue(r.knoten.getValue(variable.id).fehler!!.contains("Grundraum fehlt"))
        val synchron = synchronisiereMethodenAufrufe(ohne, r, laufzeit.graphPrüfung)
        assertEquals(ohne.verbindungen, synchron.verbindungen)
        assertEquals(variable.anschlüsse.single { it.name == "wert" }, synchron.knoten.single { it.id == variable.id }.anschlüsse.single { it.name == "wert" })
    }

    @Test fun `Grundraumtypwechsel entfernt nur die tatsaechlich inkompatible Kante`() {
        val laufzeit = MathematikKartenLaufzeit()
        val original = importiere("tupel-filter-v9.json")
        val stabil = synchronisiereMethodenAufrufe(original, laufzeit.auswerten(original), laufzeit.graphPrüfung)
        val variable = stabil.knoten.single { it.art == "mathematik.variable" }
        val grund = variable.anschlüsse.single { it.name == "grundraum" }
        val reell = stabil.knoten.single { it.art == "mathematik.menge" }
        val ausgang = reell.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }
        val gewechselt = stabil.copy(verbindungen = stabil.verbindungen.map {
            if (it.zu == AnschlussVerweis(variable.id, grund.id)) it.copy(von = AnschlussVerweis(reell.id, ausgang.id)) else it
        })
        val synchron = synchronisiereMethodenAufrufe(gewechselt, laufzeit.auswerten(gewechselt), laufzeit.graphPrüfung)
        val variableZuZerleger = gewechselt.verbindungen.single { it.von.knotenId == variable.id }
        assertEquals(gewechselt.verbindungen.filterNot { it.id == variableZuZerleger.id }, synchron.verbindungen)
    }

    @Test fun `Grundraumwechsel und Undo erben Struktur statt Altparameter`() {
        val k = MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero).copy(parameter = mapOf("name" to "x", VARIABLE_STRUKTUR to "spalte", VARIABLE_DIMENSION to "8"))
        val menge = MengenKnotenVorlagen.vorlage(MengenKnotenAuswahl.REELLE_ZAHLEN).erzeuge(GraphPunkt.Zero)
        val dimension = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero).copy(parameter = mapOf("wert" to "3"))
        val edge = VerbindungDaten(von = AnschlussVerweis(menge.id, menge.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }.id),
            zu = AnschlussVerweis(k.id, k.anschlüsse.single { it.name == "grundraum" }.id))
        val laufzeit = MathematikKartenLaufzeit()
        val editor = KartenEditorZustand(KartenDaten(name = "Wechsel", knoten = listOf(k, menge, dimension), verbindungen = listOf(edge)), laufzeit.graphPrüfung)
        fun wert() = laufzeit.auswerten(editor.karte).knoten.getValue(k.id).ausgaben.getValue("wert")
        assertIs<Variable>(wert().objekt)
        val d = VerbindungDaten(von = AnschlussVerweis(dimension.id, dimension.anschlüsse.single().id),
            zu = AnschlussVerweis(menge.id, menge.anschlüsse.single { it.name == "dimension" }.id))
        editor.führeAus(KartenAktion.VerbindungEinfügen(d))
        assertEquals(3, (wert().objekt as Tupel).elemente.size)
        editor.rückgängig()
        assertIs<Variable>(wert().objekt)
        assertFailsWith<IllegalStateException> { eval(k, emptyMap()) }
    }
}

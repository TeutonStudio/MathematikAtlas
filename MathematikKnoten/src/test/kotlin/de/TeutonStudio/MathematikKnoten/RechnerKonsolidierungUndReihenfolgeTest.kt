package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.katalog.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class RechnerKonsolidierungUndReihenfolgeTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()
    private fun z(n: Long) = RationaleZahl.von(n)
    private fun m(n: Long) = Matrix(listOf(listOf(z(n))))
    private fun aus(k: KnotenDaten, werte: Map<String, BedingterWert>) = register.finde(k.art)!!.auswerten(KnotenAuswertungsKontext(k, werte, RechenKontext()))
    private fun eingänge(k: KnotenDaten) = k.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }

    @Test fun `Matrixmigration erhaelt alle IDs Parameter Darstellung und Auswertung`() {
        val vorlagen = listOf(MathematikKnotenVorlagen.MatrixProdukt, MathematikKnotenVorlagen.MatrixInvertieren, MathematikKnotenVorlagen.TransponiereMatrix)
        vorlagen.forEach { vorlage ->
            val alt = vorlage.erzeuge(GraphPunkt(17f, 29f)).let { it.copy(name = "Mein Rechner", parameter = it.parameter + ("persoenlich" to "ja")) }
            val werte = eingänge(alt).associate { it.name to BedingterWert(m(2)) }
            val vorher = aus(alt, werte).ausgaben.values.single().objekt
            val neu = RechnerEinzeloperationMigration.migriere(alt)
            assertEquals(MatrixRechner.KNOTEN_ART, neu.art)
            assertEquals(alt.id, neu.id)
            assertEquals(alt.name, neu.name)
            assertEquals(alt.position, neu.position)
            assertEquals(alt.größe, neu.größe)
            assertEquals(alt.eigenschaften, neu.eigenschaften)
            assertEquals("ja", neu.parameter["persoenlich"])
            assertEquals(alt.anschlüsse.map { it.id }.toSet(), neu.anschlüsse.map { it.id }.toSet())
            val neueWerte = eingänge(neu).associate { it.name to BedingterWert(m(2)) }
            assertEquals(vorher, aus(neu, neueWerte).ausgaben.values.single().objekt)
            assertEquals(neu, RechnerEinzeloperationMigration.migriere(neu))
            val karte = KartenDaten(name = "Testkarte", knoten = listOf(alt))
            val geladen = MathematikKartenCodec.lade(KartenDatenJson.schreibe(karte))
            assertEquals(neu.art, geladen.knoten.single().art)
            assertEquals(geladen, MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(geladen)))
        }
    }

    @Test fun `nichtkommutierende Matrixfaktoren folgen Reihenfolge und Falk-Schema`() {
        val k = konfiguriereStrukturRechner(StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero), StrukturRechnerKnotenFamilie.MATRIX, "matrix.produkt")
        val a = Matrix(listOf(listOf(z(1), z(1)), listOf(z(0), z(1))))
        val b = Matrix(listOf(listOf(z(1), z(0)), listOf(z(1), z(1))))
        val ports = eingänge(k)
        val werte = mapOf(ports[0].name to BedingterWert(a), ports[1].name to BedingterWert(b))
        val umgeordnet = k.copy(anschlüsse = k.anschlüsse.map { p -> if (p.richtung == AnschlussRichtung.Eingang) p.copy(reihenfolge = 1 - p.reihenfolge) else p })
        assertNotEquals(aus(k, werte).ausgaben.values.single().objekt, aus(umgeordnet, werte).ausgaben.values.single().objekt)
        assertEquals(b, matrixproduktFalkDaten(aus(umgeordnet, werte)).links)
        assertEquals(a, matrixproduktFalkDaten(aus(umgeordnet, werte)).rechts)
        fun quelle(id: String) = KnotenDaten(art = id, name = id, anschlüsse = listOf(AnschlussDaten(
            name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Matrix.id)))
        val qa = quelle("test.matrix.a")
        val qb = quelle("test.matrix.b")
        register.registriere(qa.art) { KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(a))) }
        register.registriere(qb.art) { KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(b))) }
        val karte = KartenDaten(name = "Cache", knoten = listOf(qa, qb, k), verbindungen = listOf(qa, qb).mapIndexed { index, q ->
            VerbindungDaten(von = AnschlussVerweis(q.id, q.anschlüsse.single().id), zu = AnschlussVerweis(k.id, ports[index].id))
        })
        val adapter = KartenAuswerter(register)
        val vorher = adapter.auswerten(karte).knoten.getValue(k.id)
        val nachher = adapter.auswerten(karte.copy(knoten = listOf(qa, qb, umgeordnet))).knoten.getValue(k.id)
        assertNotEquals(vorher.ausgaben.getValue("wert").objekt, nachher.ausgaben.getValue("wert").objekt)
        assertEquals(b, matrixproduktFalkDaten(nachher).links)

        val neu = konfiguriereStrukturRechner(umgeordnet, StrukturRechnerKnotenFamilie.MATRIX, "matrix.produkt")
        assertEquals(eingänge(umgeordnet).map { it.id }, eingänge(neu).map { it.id })
        val geladen = MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(KartenDaten(name = "Testkarte", knoten = listOf(neu))))
        assertEquals(eingänge(neu).map { it.id }, eingänge(geladen.knoten.single()).map { it.id })
    }

    @Test fun `assoziative Matrixoperationen rechnen drei Operanden und ignorieren nur Schlussreserve`() {
        listOf("matrix.addition" to m(9), "matrix.hadamard" to m(24)).forEach { (id, erwartet) ->
            val basis = konfiguriereStrukturRechner(StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero), StrukturRechnerKnotenFamilie.MATRIX, id)
            val ports = eingänge(basis)
            val dritter = ports.last().copy(id = AnschlussId("dritter"), name = "input3", reihenfolge = 2, dynamischErzeugt = true)
            val reserve = dritter.copy(id = AnschlussId("reserve"), name = "input4", reihenfolge = 3)
            val k = basis.copy(anschlüsse = basis.anschlüsse + dritter + reserve)
            val werte = mapOf(ports[0].name to BedingterWert(m(2)), ports[1].name to BedingterWert(m(3)), "input3" to BedingterWert(m(4)))
            assertEquals(erwartet, aus(k, werte).ausgaben.values.single().objekt)
            assertNotNull(aus(k, werte - ports[1].name).fehler)
        }
    }

    @Test fun `historische Zahlenrollen nehmen Standardwerte und Quellen bei Umbenennung mit`() {
        val basis = MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt.Zero)
        val ports = eingänge(basis)
        val dritter = ports.last().copy(id = AnschlussId("zahl-drei"), name = "input3", reihenfolge = 2, dynamischErzeugt = true)
        val alt = basis.copy(anschlüsse = basis.anschlüsse + dritter,
            parameter = basis.parameter + mapOf("standardwert.a" to "1", "standardwert.b" to "2", "standardwert.input3" to "3"),
            eingangsKartenVerweise = mapOf("input3" to KartenVerweis(KartenId("quelle"), 1)))
        val karte = KartenDaten(name = "Zahlenrollen", knoten = listOf(alt))
        val neu = MathematikKartenCodec.lade(KartenDatenJson.schreibe(karte)).knoten.single()
        assertEquals("3", neu.parameter["join.lokal.${dritter.id.wert}"])
        assertEquals(alt.eingangsKartenVerweise.getValue("input3"), neu.eingangsKartenVerweise.values.single())
        assertEquals(z(6), KartenAuswerter(register).auswerten(karte.copy(knoten = listOf(neu))).knoten.getValue(neu.id).ausgaben.getValue("wert").objekt)
    }

    @Test fun `Tensoroperationen falten drei Operanden und melden inkompatible Formen`() {
        listOf("tensor.addition" to 9L, "tensor.hadamard" to 24L).forEach { (id, wert) ->
            val basis = konfiguriereTensorOperation(StrukturRechnerKnotenVorlagen.Tensorrechner.erzeuge(GraphPunkt.Zero),
                requireNotNull(StandardTensorOperationen.registry.definition(id)))
            val ports = eingänge(basis)
            val dritter = ports.last().copy(id = AnschlussId("tensor-drei"), name = "input3", reihenfolge = 2, dynamischErzeugt = true)
            val reserve = dritter.copy(id = AnschlussId("tensor-reserve"), name = "input4", reihenfolge = 3)
            val k = basis.copy(anschlüsse = basis.anschlüsse + dritter + reserve)
            fun t(n: Long) = Tensor(listOf(1), listOf(z(n)))
            val werte = mapOf(ports[0].name to BedingterWert(t(2)), ports[1].name to BedingterWert(t(3)), "input3" to BedingterWert(t(4)))
            assertEquals(t(wert), aus(k, werte).ausgaben.values.single().objekt)
            assertNotNull(aus(k, werte - ports[0].name).fehler)
            assertNotNull(aus(k, werte + ("input3" to BedingterWert(Tensor(listOf(2), listOf(z(1), z(2)))))).fehler)
        }
    }

    @Test fun `gespeicherte binaere Rechner werden idempotent zu Join mit stabiler Hauptrolle`() {
        listOf(
            StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero),
            konfiguriereTensorOperation(StrukturFormelRechnerVorlagen.Tensor.erzeuge(GraphPunkt.Zero), aktuelleTensorOperationDefinition(StrukturFormelRechnerVorlagen.Tensor.erzeuge(GraphPunkt.Zero))),
            konfiguriereStrukturRechner(StrukturFormelRechnerVorlagen.Aussagesatz.erzeuge(GraphPunkt.Zero), StrukturRechnerKnotenFamilie.AUSSAGESATZ, "aussage.konjunktion"),
        ).forEach { aktuell ->
            val alt = aktuell.copy(anschlüsse = aktuell.anschlüsse.map { it.copy(kannSichErweitern = false) })
            val karte = KartenDaten(name = "Testkarte", knoten = listOf(alt))
            val neu = MathematikKartenCodec.lade(KartenDatenJson.schreibe(karte))
            assertEquals(listOf(alt.anschlüsse.first().id, alt.anschlüsse.last().id).toSet(), neu.knoten.single().anschlüsse.map { it.id }.toSet())
            assertTrue(eingänge(neu.knoten.single()).all { it.belegung == AnschlussBelegung.Mehrfach })
            assertEquals(neu, MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(neu)))
        }
    }

    @Test fun `explizite Zahlenanschlusszahl bleibt aenderbar und reine Normalisierung erhaelt Sortierung`() {
        val basis = ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val vier = konfiguriereZahlenRechner(basis, UniversellerZahlenOperator.ADDITION, festeEingänge = 4)
        assertEquals(4, eingänge(vier).size)
        val umgeordnet = vier.copy(anschlüsse = vier.anschlüsse.map { if (it.richtung == AnschlussRichtung.Eingang) it.copy(reihenfolge = 3 - it.reihenfolge) else it })
        assertEquals(eingänge(umgeordnet).map { it.id }, eingänge(konfiguriereZahlenRechner(umgeordnet)).map { it.id })
        assertEquals(2, eingänge(konfiguriereZahlenRechner(vier, festeEingänge = 2)).size)
    }

    @Test fun `Katalog entfernt Einzeloperationen und Suche liefert konfigurierte Rechner`() {
        val katalog = KanonischerMathematikKnotenKatalog.alle()
        assertTrue(katalog.none { it.art in RechnerEinzeloperationMigration.zuordnungen || it.art == TENSORPRODUKT_ART })
        val produkt = OperatorKnotenSuchindex.suche("Matrixprodukt", katalog)
        assertTrue(produkt.any { it.art == MatrixRechner.KNOTEN_ART && it.standardParameter["operator"] == "matrix.produkt" })
        listOf("matrix.subtraktion", "matrix.skalarmultiplikation").forEach { id ->
            val k = konfiguriereStrukturRechner(StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero), StrukturRechnerKnotenFamilie.MATRIX, id)
            assertTrue(eingänge(k).none { it.kannSichErweitern })
        }
    }
    @Test fun `Aussagenmigration erhaelt Boolean-Standardwerte und historische UND-Adjunktion`() {
        val auswerter = KartenAuswerter(register)
        listOf(MathematikKnotenVorlagen.Konjunktion, MathematikKnotenVorlagen.Disjunktion,
            MathematikKnotenVorlagen.Implikation, MathematikKnotenVorlagen.Äquivalenz,
            AussagenLogikKnotenVorlagen.Adjunktion).forEach { vorlage ->
            val alt = vorlage.erzeuge(GraphPunkt.Zero).let { it.copy(parameter = it.parameter +
                mapOf("standardwert.a" to "true", "standardwert.b" to "false")) }
            val karte = KartenDaten(name = "Aussagen", knoten = listOf(alt))
            val geladen = MathematikKartenCodec.lade(KartenDatenJson.schreibe(karte))
            val vorher = auswerter.auswerten(karte).knoten.getValue(alt.id).ausgaben.values.single().objekt as Aussage
            val nachher = auswerter.auswerten(geladen).knoten.getValue(alt.id).ausgaben.values.single().objekt as Aussage
            assertEquals(vorher.entscheide(), nachher.entscheide(), vorlage.name)
            assertEquals(alt.name, geladen.knoten.single().name)
        }
    }

    @Test fun `unmarkierte historische Adjunktion behaelt UND-Semantik`() {
        val alt = MathematikKnotenVorlagen.Adjunktion.erzeuge(GraphPunkt.Zero)
        val neu = RechnerEinzeloperationMigration.migriere(alt)
        assertEquals("aussage.konjunktion", neu.parameter["operator"])
    }

    @Test fun `freie Aussagenreserve bleibt leer und explizite Standardwerte werden sortiert`() {
        val basis = konfiguriereStrukturRechner(StrukturFormelRechnerVorlagen.Aussagesatz.erzeuge(GraphPunkt.Zero), StrukturRechnerKnotenFamilie.AUSSAGESATZ, "aussage.konjunktion")
        val ports = eingänge(basis).filter { it.kannSichErweitern }
        val reserve = ports.last().copy(id = AnschlussId("reserve"), name = "input3", reihenfolge = 2, dynamischErzeugt = true)
        val k = basis.copy(anschlüsse = basis.anschlüsse + reserve, parameter = basis.parameter + ports.associate { "standardwert.${it.name}" to "true" })
        val karte = KartenDaten(name = "Reserve", knoten = listOf(k))
        val ergebnis = KartenAuswerter(register).auswerten(karte).knoten.getValue(k.id)
        assertEquals(Wahrheitswert.Wahr, (ergebnis.ausgaben.getValue("wert").objekt as Aussage).entscheide().wahrheitswert)
        assertFalse("input3" in ergebnis.eingänge)
    }

    @Test fun `Tensorproduktmigration erhaelt persoenlichen Titel`() {
        val alt = StrukturRechnerKnotenVorlagen.Tensorprodukt.erzeuge(GraphPunkt.Zero).copy(name = "Meine Achsen")
        val neu = MathematikKartenCodec.lade(KartenDatenJson.schreibe(KartenDaten(name = "Tensor", knoten = listOf(alt)))).knoten.single()
        assertEquals("Meine Achsen", neu.name)
        assertEquals(alt.anschlüsse.map { it.id }.toSet(), neu.anschlüsse.map { it.id }.toSet())
    }

    @Test fun `Migration benennt gespeicherte Kartenquellen zusammen mit Eingangsrollen um`() {
        val alt = MathematikKnotenVorlagen.MatrixProdukt.erzeuge(GraphPunkt.Zero).copy(
            eingangsKartenVerweise = mapOf("a" to KartenVerweis(KartenId("methode"), 1)))
        val neu = RechnerEinzeloperationMigration.migriere(alt)
        assertEquals(alt.eingangsKartenVerweise.getValue("a"), neu.eingangsKartenVerweise.getValue("links"))
        assertFalse("a" in neu.eingangsKartenVerweise)
    }

}

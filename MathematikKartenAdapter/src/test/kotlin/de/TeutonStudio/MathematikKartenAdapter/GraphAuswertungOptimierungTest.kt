package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import kotlin.test.*

class GraphAuswertungOptimierungTest {
    @Test fun unterschiedlicheParameterMitGleichemHashWerdenNeuBerechnet() {
        val a = quelle("a").copy(parameter = mapOf("text" to "Aa"))
        val b = a.copy(parameter = mapOf("text" to "BB"))
        assertEquals(a.hashCode(), b.hashCode(), "Die Regression benötigt eine echte Hashkollision.")
        var aufrufe = 0
        val auswerter = KartenAuswerter(MathematikAuswerterRegister().apply {
            registriere("quelle") {
                aufrufe++
                ergebnis(if (it.knoten.parameter["text"] == "Aa") 1 else 2)
            }
        })
        val karte = KartenDaten(name = "Kollision", knoten = listOf(a))
        assertEquals(1, wert(auswerter.auswerten(karte), a))
        assertEquals(2, wert(auswerter.auswerten(karte.copy(knoten = listOf(b))), b))
        assertEquals(2, aufrufe)
    }

    @Test fun gruppenMitHashgleichenKartenverweisenNutzenNichtDasFalscheErgebnis() {
        val a = quelle("a").copy(parameter = mapOf("wert" to "1"))
        val ausgang = durchlauf("ausgang").let { k -> k.copy(art = "mathematik.kartenAusgang", name = "wert") }
        val intern = KartenDaten(id = KartenId("Aa"), name = "Innen", knoten = listOf(a, ausgang),
            verbindungen = listOf(verbinde(a, ausgang)))
        val andere = intern.copy(id = KartenId("BB"), knoten = listOf(a.copy(parameter = mapOf("wert" to "2")), ausgang))
        val gruppe = KnotenDaten(art = "gruppe", name = "Gruppe", kartenVerweis = KartenVerweis(intern.id, 1))
        val geändert = gruppe.copy(kartenVerweis = KartenVerweis(andere.id, 1))
        assertEquals(gruppe.hashCode(), geändert.hashCode())
        val auswerter = KartenAuswerter(MathematikAuswerterRegister().apply {
            registriere("quelle") { ergebnis(it.knoten.parameter.getValue("wert").toInt()) }
            registriere("mathematik.kartenAusgang") { KnotenAuswertungsErgebnis(mapOf("wert" to it.eingänge.getValue("eingang"))) }
        }, KartenQuelle { when (it.kartenId) { intern.id -> intern; andere.id -> andere; else -> null } })
        val außen = KartenDaten(name = "Außen", knoten = listOf(gruppe))
        assertEquals(1, wert(auswerter.auswerten(außen), gruppe))
        assertEquals(2, wert(auswerter.auswerten(außen.copy(knoten = listOf(geändert))), geändert))
    }

    @Test fun parameteraenderungLaesstUnabhaengigenTeilgraphImCache() {
        val a = quelle("a")
        val b = quelle("b")
        val aufrufe = mutableMapOf<KnotenId, Int>()
        val auswerter = KartenAuswerter(MathematikAuswerterRegister().apply {
            registriere("quelle") {
                aufrufe[it.knoten.id] = (aufrufe[it.knoten.id] ?: 0) + 1
                ergebnis(it.knoten.parameter["wert"]?.toInt() ?: 7)
            }
        })
        val karte = KartenDaten(name = "Unabhängig", knoten = listOf(a, b))
        auswerter.auswerten(karte)
        val geändert = b.copy(parameter = mapOf("wert" to "12"))
        assertEquals(12, wert(auswerter.auswerten(karte.copy(knoten = listOf(a, geändert))), geändert))
        assertEquals(mapOf(a.id to 1, b.id to 2), aufrufe)
    }

    @Test fun langeKetteBenoetigtAuchBeiWiederholungNurLineareKnotenzugriffe() {
        val anzahl = 400
        val knoten = listOf(quelle("0000")) + (1 until anzahl).map { durchlauf(it.toString().padStart(4, '0')) }
        val gezählt = Gezählt(knoten.reversed())
        val karte = KartenDaten(name = "Kette", knoten = gezählt,
            verbindungen = knoten.zipWithNext { a, b -> verbinde(a, b) })
        val auswerter = auswerter()
        repeat(2) {
            gezählt.zugriffe = 0
            val ergebnis = auswerter.auswerten(karte)
            assertTrue(ergebnis.fehler.isEmpty(), ergebnis.fehler.toString())
            assertEquals(7, wert(ergebnis, knoten.last()))
            assertEquals(knoten.map { it.id }, ergebnis.knoten.keys.toList())
            println("Kette: $anzahl Knoten, ${gezählt.zugriffe} Knotenzugriffe, Durchlauf ${it + 1}.")
            assertTrue(gezählt.zugriffe < anzahl * 20,
                "${gezählt.zugriffe} Knotenzugriffe für $anzahl Knoten: kein linearer Zugriffspfad.")
        }
    }

    @Test fun vieleQuellportsWerdenBeimFanoutNichtProVerbindungLinearGesucht() {
        val anzahl = 300
        val ports = Gezählt((0 until anzahl).map { i -> quelle("q").anschlüsse.single().copy(name = "wert$i") })
        val quelle = quelle("q").copy(anschlüsse = ports)
        val ziele = (0 until anzahl).map { durchlauf("z$it") }
        val karte = KartenDaten(name = "Fanout", knoten = ziele + quelle, verbindungen = ziele.mapIndexed { i, ziel ->
            VerbindungDaten(von = AnschlussVerweis(quelle.id, ports[i].id),
                zu = AnschlussVerweis(ziel.id, ziel.anschlüsse.first { it.name == "eingang" }.id))
        })
        val auswerter = KartenAuswerter(MathematikAuswerterRegister().apply {
            registriere("quelle") { KnotenAuswertungsErgebnis(ports.associate { it.name to BedingterWert(RationaleZahl.von(7)) }) }
            registriere("durchlauf") { KnotenAuswertungsErgebnis(mapOf("wert" to it.eingänge.getValue("eingang"))) }
        })
        repeat(2) {
            ports.zugriffe = 0
            val ergebnis = auswerter.auswerten(karte)
            assertTrue(ergebnis.fehler.isEmpty(), ergebnis.fehler.toString())
            assertTrue(ziele.all { wert(ergebnis, it) == 7 })
            println("Fanout: $anzahl Quellports, ${ports.zugriffe} Portzugriffe, Durchlauf ${it + 1}.")
            assertTrue(ports.zugriffe < anzahl * 20, "${ports.zugriffe} Portzugriffe sind nicht linear.")
        }
    }

    @Test fun verdraengteKartenGebenAuchIhreErgebnisseFrei() {
        var aufrufe = 0
        val auswerter = KartenAuswerter(MathematikAuswerterRegister().apply {
            registriere("quelle") { aufrufe++; ergebnis(7) }
        })
        val karten = (0 until 20).map { i -> KartenDaten(name = "Karte$i", knoten = listOf(quelle("q$i"))) }
        auswerter.auswerten(karten.first())
        auswerter.auswerten(karten.first())
        assertEquals(1, aufrufe)
        karten.drop(1).forEach { auswerter.auswerten(it) }
        auswerter.auswerten(karten.first())
        assertEquals(21, aufrufe, "Ein alter Plan darf seine Knotenergebnisse nicht unbegrenzt festhalten.")
    }

    @Test fun tiefeUnterkartenBleibenAuchNachVerdraengungAuswertbar() {
        val basis = quelle("basis")
        var innen = KartenDaten(name = "Basis", knoten = listOf(basis))
        val karten = mutableMapOf(KartenVerweis(innen.id, innen.version) to innen)
        repeat(20) { i ->
            val verweis = KartenVerweis(innen.id, innen.version)
            val gruppe = KnotenDaten(id = KnotenId("gruppe$i"), art = "gruppe", name = "Unterkarte$i", kartenVerweis = verweis)
            innen = KartenDaten(name = "Ebene$i", knoten = listOf(gruppe))
            karten[KartenVerweis(innen.id, innen.version)] = innen
        }
        var aufrufe = 0
        var ladeAufrufe = 0
        val auswerter = KartenAuswerter(MathematikAuswerterRegister().apply {
            registriere("quelle") { aufrufe++; ergebnis(7) }
        }, KartenQuelle { ladeAufrufe++; karten[it] })
        repeat(2) { assertTrue(auswerter.auswerten(innen).fehler.isEmpty()) }
        assertEquals(2, aufrufe, "21 verschachtelte Karten überschreiten den begrenzten Plan-Cache.")
        assertEquals(40, ladeAufrufe, "Beide Durchläufe müssen alle 20 Unterkarten besuchen.")
    }

    @Test fun parameterUndPortnamenWerdenBeiUnveraenderterTopologieAktuellGelesen() {
        val a = quelle("a")
        val b = durchlauf("b")
        val karte = KartenDaten(name = "Änderungen", knoten = listOf(a, b), verbindungen = listOf(verbinde(a, b)))
        val auswerter = auswerter()
        assertEquals(7, wert(auswerter.auswerten(karte), b))
        val geändert = a.copy(parameter = mapOf("wert" to "12"),
            anschlüsse = a.anschlüsse.map { it.copy(name = "neu") })
        val neu = karte.copy(knoten = listOf(geändert, b))
        val ergebnis = auswerter.auswerten(neu)
        assertTrue(ergebnis.fehler.isEmpty(), ergebnis.fehler.toString())
        assertEquals(12, wert(ergebnis, b))
        assertEquals(7, wert(auswerter.auswerten(karte), b), "Ein älterer Snapshot muss weiter funktionieren.")
    }

    @Test fun umverdrahtenLoeschenUndWiederherstellenAktualisierenDenPlan() {
        val a = quelle("a")
        val b = quelle("b").copy(parameter = mapOf("wert" to "13"))
        val c = durchlauf("c")
        val alt = KartenDaten(name = "Umbau", knoten = listOf(a, b, c), verbindungen = listOf(verbinde(a, c)))
        val auswerter = auswerter()
        assertEquals(7, wert(auswerter.auswerten(alt), c))
        val neu = alt.copy(knoten = listOf(b, c), verbindungen = listOf(verbinde(b, c)))
        assertEquals(13, wert(auswerter.auswerten(neu), c))
        assertEquals(7, wert(auswerter.auswerten(alt), c))
        auswerter.leereCache()
        assertEquals(13, wert(auswerter.auswerten(neu), c))
    }

    @Test fun strukturAendertAuchDenKontextBereitsBekannterKnoten() {
        val z = quelle("z")
        val auswerter = KartenAuswerter(MathematikAuswerterRegister().apply {
            registriere("quelle") { ergebnis(it.topologischeReihenfolge.getValue(it.knoten.id)) }
        })
        val karte = KartenDaten(name = "Reihenfolge", knoten = listOf(z))
        assertEquals(0, wert(auswerter.auswerten(karte), z))
        assertEquals(1, wert(auswerter.auswerten(karte.copy(knoten = listOf(z, quelle("a")))), z))
    }

    @Test fun mehrereKantenEinesKnotenpaarsZaehlenNurAlsEineAbhaengigkeit() {
        val a = quelle("a")
        val b = durchlauf("b").let { k -> k.copy(anschlüsse = k.anschlüsse + eingang("zweiter")) }
        val register = MathematikAuswerterRegister().apply {
            registriere("quelle") { ergebnis(7) }
            registriere("durchlauf") {
                assertEquals(setOf("eingang", "zweiter"), it.eingänge.keys)
                ergebnis(14)
            }
        }
        val karte = KartenDaten(name = "Mehrfachkante", knoten = listOf(b, a),
            verbindungen = listOf(verbinde(a, b), verbinde(a, b, "zweiter")))
        val ergebnis = KartenAuswerter(register).auswerten(karte)
        assertTrue(ergebnis.fehler.isEmpty())
        assertEquals(14, wert(ergebnis, b))
    }

    @Test fun zyklusUndFehlendeQuelleBlockierenWeiterhinNurBetroffeneKnoten() {
        val a = durchlauf("a")
        val b = durchlauf("b")
        val frei = quelle("frei")
        val auswerter = auswerter()
        val karte = KartenDaten(name = "Zyklus", knoten = listOf(a, b, frei),
            verbindungen = listOf(verbinde(a, b), verbinde(b, a)))
        repeat(2) {
            val ergebnis = auswerter.auswerten(karte)
            assertEquals(setOf(frei.id), ergebnis.knoten.keys)
            assertTrue(ergebnis.fehler.single().startsWith("2 Knoten konnten"))
        }
        val fehlt = karte.copy(knoten = listOf(b, frei), verbindungen = listOf(verbinde(a, b)))
        val ergebnis = auswerter.auswerten(fehlt)
        assertEquals(setOf(frei.id), ergebnis.knoten.keys)
        assertTrue(ergebnis.fehler.single().startsWith("1 Knoten konnten"))
    }

    @Test fun fehlenderQuellanschlussWirdNachPortaenderungNichtAusAltemIndexBedient() {
        val a = quelle("a")
        val b = durchlauf("b")
        val karte = KartenDaten(name = "Port", knoten = listOf(a, b), verbindungen = listOf(verbinde(a, b)))
        val auswerter = auswerter()
        assertEquals(7, wert(auswerter.auswerten(karte), b))
        val ergebnis = auswerter.auswerten(karte.copy(knoten = listOf(a.copy(anschlüsse = emptyList()), b)))
        assertTrue(ergebnis.knoten.getValue(b.id).fehler.orEmpty().contains("Quellanschluss"))
    }

    private fun auswerter() = KartenAuswerter(MathematikAuswerterRegister().apply {
        registriere("quelle") { k ->
            KnotenAuswertungsErgebnis(mapOf(
                (k.knoten.anschlüsse.firstOrNull()?.name ?: "wert") to
                    BedingterWert(RationaleZahl.von(k.knoten.parameter["wert"]?.toLong() ?: 7L)),
            ))
        }
        registriere("durchlauf") { KnotenAuswertungsErgebnis(mapOf("wert" to it.eingänge.getValue("eingang"))) }
    })

    private fun quelle(id: String) = KnotenDaten(id = KnotenId(id), art = "quelle", name = id,
        anschlüsse = listOf(AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts, art = AnschlussArtId("zahl"))))

    private fun durchlauf(id: String) = quelle(id).copy(art = "durchlauf", anschlüsse = listOf(eingang("eingang")) + quelle(id).anschlüsse)

    private fun eingang(name: String) = AnschlussDaten(name = name, richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links, art = AnschlussArtId("zahl"))

    private fun verbinde(a: KnotenDaten, b: KnotenDaten, eingang: String = "eingang") = VerbindungDaten(
        von = AnschlussVerweis(a.id, a.anschlüsse.first { it.name == "wert" }.id),
        zu = AnschlussVerweis(b.id, b.anschlüsse.first { it.name == eingang }.id),
    )

    private fun ergebnis(wert: Int) = KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(RationaleZahl.von(wert.toLong()))))
    private fun wert(ergebnis: KartenAuswertungsErgebnis, knoten: KnotenDaten): Int {
        val objekt = ergebnis.knoten.getValue(knoten.id).ausgaben.getValue("wert").objekt
        return (0..100).first { objekt == RationaleZahl.von(it.toLong()) }
    }

    private class Gezählt<T>(private val basis: List<T>) : AbstractList<T>() {
        var zugriffe = 0
        override val size: Int get() = basis.size
        override fun get(index: Int): T { zugriffe++; return basis[index] }
    }
}

package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*
import de.TeutonStudio.TypSystem.*

class HistorischerMigrationsSchutzTest {
    private fun karte(k:KnotenDaten)=KartenDaten(name="Original",knoten=listOf(k))
    private fun phasen(k:KartenDaten):List<KartenDaten> {
        val text=KartenDatenJson.schreibe(k)
        return listOf(MathematikKartenCodec.lese(text),MathematikKartenCodec.lade(text),MathematikKartenCodec.importiere(text),MathematikKartenMigrationen.vorSpeichern(k))
    }
    private fun abgelehnt(alt:KnotenDaten) {
        for(k in phasen(karte(alt))) {
            val n=k.knoten.single()
            assertNotNull(n.parameter[RECHNER_MIGRATION_DIAGNOSE],"${alt.art}: ${alt.parameter}")
            assertEquals(alt,n.copy(parameter=n.parameter-RECHNER_MIGRATION_DIAGNOSE))
            assertEquals(k,MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(k)))
            assertNotNull(KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister()).auswerten(k).knoten.getValue(n.id).fehler)
        }
    }
    @Test fun `unbekannte Modi Pflichtrollen und widerspruechliche Typen bleiben Original`() {
        val unary=MathematikKnotenVorlagen.Kehrwert.erzeuge(GraphPunkt.Zero)
        val menge=MathematikKnotenVorlagen.Vereinigung.erzeuge(GraphPunkt.Zero)
        val quelle=MengenraumKnotenVorlagen.Primzahlen.erzeuge(GraphPunkt.Zero)
        val einheit=MathematikKnotenVorlagen.EinheitsSpalte.erzeuge(GraphPunkt.Zero)
        fun input(k:KnotenDaten,change:(AnschlussDaten)->AnschlussDaten)=k.copy(anschlüsse=k.anschlüsse.map {if(it.richtung==AnschlussRichtung.Eingang) change(it) else it})
        listOf(
            input(unary){it.copy(name="unbekannt")},input(unary){it.copy(art=MathematikAnschlussArten.Aussage.id)},input(unary){it.copy(kannSichErweitern=true)},
            unary.copy(anschlüsse=unary.anschlüsse.filter {it.richtung==AnschlussRichtung.Ausgang}),
            unary.copy(anschlüsse=unary.anschlüsse+unary.anschlüsse.last().copy(id=AnschlussId("extra"),name="extra")),
            input(menge){it.copy(art=MathematikAnschlussArten.Zahl.id)},quelle.copy(anschlüsse=quelle.anschlüsse.map {it.copy(art=MathematikAnschlussArten.Zahl.id)}),
            MathematikKnotenVorlagen.Maximum.erzeuge(GraphPunkt.Zero).let {it.copy(parameter=it.parameter+("modus" to "unbekannt"))},
            MathematikKnotenVorlagen.KomplexAusTupel.erzeuge(GraphPunkt.Zero).let {it.copy(parameter=it.parameter+("modus" to "unbekannt"))},
            MathematikKnotenVorlagen.Adjunktion.erzeuge(GraphPunkt.Zero).copy(parameter=mapOf(AUSSAGEN_LOGIK_SEMANTIK to "unbekannt")),
            einheit.copy(parameter=mapOf("index" to "-1","dimension" to "3")),einheit.copy(parameter=mapOf("index" to "0","dimension" to "x")),
            einheit.copy(parameter=mapOf("index" to "3","dimension" to "3")),
            VektorRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero).copy(parameter=emptyMap()),
            ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero).copy(parameter=mapOf(ZAHLENRECHNER_OPERATOR to "unbekannt")),
            input(MathematikKnotenVorlagen.MatrixProdukt.erzeuge(GraphPunkt.Zero)){it.copy(art=MathematikAnschlussArten.Aussage.id,zulässigeArten=emptySet())},
        ).forEach(::abgelehnt)
    }
    @Test fun `Logarithmus bindet benannte Rollen trotz Layoutwechsel und remappt Referenzen vollstaendig`() {
        val basis=MathematikKnotenVorlagen.Logarithmus.erzeuge(GraphPunkt.Zero)
        val alt=basis.copy(anschlüsse=basis.anschlüsse.map {p -> p.copy(reihenfolge=if(p.name=="basis") 9 else 0)},
            parameter=basis.parameter+mapOf("standardwert.basis" to "2","standardwert.argument" to "8"),
            eingangsKartenVerweise=mapOf("basis" to KartenVerweis(KartenId("fest"),3)))
        for(k in phasen(karte(alt))) {
            val n=k.knoten.single();assertNull(n.parameter[RECHNER_MIGRATION_DIAGNOSE])
            assertEquals(ZAHLENRECHNER_ART,n.art)
            assertEquals(alt.anschlüsse.map {it.id}.toSet(),n.anschlüsse.map {it.id}.toSet())
            assertEquals("2",n.parameter["standardwert.a"]);assertEquals("8",n.parameter["standardwert.b"])
            assertEquals(alt.eingangsKartenVerweise.getValue("basis"),n.eingangsKartenVerweise.getValue("a"))
            val r=GesamterMathematikAuswerter.erzeugeRegister().finde(n.art)!!.auswerten(KnotenAuswertungsKontext(n,mapOf("a" to BedingterWert(RationaleZahl.von(2)),"b" to BedingterWert(RationaleZahl.von(8))),RechenKontext()))
            val wert=r.ausgaben.getValue("wert").objekt as ZahlAusdruck
            assertEquals(Logarithmus(RationaleZahl.von(2),RationaleZahl.von(8)),wert)
            val numerisch=assertIs<DomaenenWert.Reell>(assertIs<DomaenenErgebnis.Wert>(DomaenenAuswerter.wert(wert)).wert)
            assertEquals(3.0,numerisch.wert.toDouble(),1e-12)
        }
    }
    @Test fun `historische Wurzel besitzt belegte Quadratwurzel und erfindet keinen Gradanschluss`() {
        val alt=MathematikKnotenVorlagen.Wurzel.erzeuge(GraphPunkt.Zero)
        for(k in phasen(karte(alt))) {
            val n=k.knoten.single();assertEquals(UniversellerZahlenOperator.QUADRATWURZEL.stabileId,n.parameter[ZAHLENRECHNER_OPERATOR]);assertEquals(alt.anschlüsse.map {it.id}.toSet(),n.anschlüsse.map {it.id}.toSet())
        }
    }
    @Test fun `vorhandene Diagnosen werden weder von Einzeloperation noch nachfolgenden Phasen entfernt`() {
        val alt=MathematikKnotenVorlagen.MatrixProdukt.erzeuge(GraphPunkt.Zero).let {it.copy(parameter=it.parameter+(RECHNER_MIGRATION_DIAGNOSE to "Fremde konkrete Sperre"))}
        assertEquals(alt,RechnerEinzeloperationMigration.migriere(alt))
        for(k in phasen(karte(alt))) assertEquals(alt,k.knoten.single())
    }
    @Test fun `doppelte Schnittstellenidentitaeten behalten ihre urspruenglichen Positionsparameter`() {
        val a=MathematikKnotenVorlagen.KartenEingang.erzeuge(GraphPunkt.Zero).copy(parameter=mapOf(KARTEN_SCHNITTSTELLEN_POSITION to "fremd-a"))
        val b=a.copy(name="Zweiter",parameter=mapOf(KARTEN_SCHNITTSTELLEN_POSITION to "fremd-b"))
        for(k in phasen(KartenDaten(name="Doppelt",knoten=listOf(a,b)))) {
            assertEquals(listOf(a,b),k.knoten.map {it.copy(parameter=it.parameter-RECHNER_MIGRATION_DIAGNOSE)})
            assertTrue(k.knoten.all {RECHNER_MIGRATION_DIAGNOSE in it.parameter})
        }
    }
    @Test fun `mehrfach belegter Einzelanschluss behaelt alle Verbindungen und Diagnose`() {
        val k=MathematikKnotenVorlagen.Kehrwert.erzeuge(GraphPunkt.Zero)
        val qs=List(2){MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero)}
        val graph=KartenDaten(name="Mehrfach",knoten=qs+k,verbindungen=qs.map {q -> VerbindungDaten(von=AnschlussVerweis(q.id,q.anschlüsse.single().id),zu=AnschlussVerweis(k.id,k.anschlüsse.first().id))})
        for(n in phasen(graph)) {assertEquals(graph.verbindungen,n.verbindungen);assertNotNull(n.knoten.single {it.id==k.id}.parameter[RECHNER_MIGRATION_DIAGNOSE])}
    }

    @Test fun `umgeordnete Aussagen remappen Defaults genau einmal und erhalten Portvertraege`() {
        val basis=MathematikKnotenVorlagen.Konjunktion.erzeuge(GraphPunkt.Zero)
        val alt=basis.copy(name="Eigene Konjunktion",parameter=basis.parameter+mapOf("standardwert.a" to "wahr","standardwert.b" to "falsch"),
            anschlüsse=basis.anschlüsse.map {p -> p.copy(reihenfolge=if(p.name=="a") 1 else 0,semantischeId="sem-${p.name}",
                vertrag=AnschlussVertrag(TypAusdruck.Atom(TypId(MathematikAnschlussArten.Aussage.id.wert))),
                artFolgtEingang=if(p.richtung==AnschlussRichtung.Ausgang) "a" else null,
                typInferenz=if(p.richtung==AnschlussRichtung.Ausgang) TypInferenzRegel.FolgtEingang("a") else null)},
            eingangsKartenVerweise=mapOf("a@karte:extern" to KartenVerweis(KartenId("extern"),4)))
        val quelle=MathematikKnotenVorlagen.Wahr.erzeuge(GraphPunkt.Zero)
        val edge=VerbindungDaten(von=AnschlussVerweis(quelle.id,quelle.anschlüsse.single().id),zu=AnschlussVerweis(alt.id,alt.anschlüsse.single {it.name=="a"}.id),eingangsReihenfolge=7,eingangsFallback="a@karte:extern")
        for(k in listOf(KartenDaten(name="Referenzen",knoten=listOf(quelle,alt),verbindungen=listOf(edge)).migriereRechnerEinzeloperationen())) {
            val n=k.knoten.single {it.id==alt.id};assertNull(n.parameter[RECHNER_MIGRATION_DIAGNOSE])
            assertEquals("wahr",n.parameter["standardwert.b"]);assertEquals("falsch",n.parameter["standardwert.a"])
            assertEquals(alt.eingangsKartenVerweise.values.single(),n.eingangsKartenVerweise["b@karte:extern"])
            assertEquals(edge.copy(eingangsFallback="b@karte:extern"),k.verbindungen.single())
            for(p in alt.anschlüsse) {
                val neu=n.anschlüsse.single {it.id==p.id}
                assertEquals(p.vertrag,neu.vertrag);assertEquals(p.semantischeId,neu.semantischeId)
                if(p.richtung==AnschlussRichtung.Ausgang) {assertEquals("b",neu.artFolgtEingang);assertEquals(TypInferenzRegel.FolgtEingang("b"),neu.typInferenz)}
            }
        }
    }
    @Test fun `Transposition mit zusaetzlichem Ausgang und Division mit Nullaussage bleiben Original`() {
        for(vorlage in listOf(MathematikKnotenVorlagen.TransponiereMatrix,MathematikKnotenVorlagen.TransponiereSpalte,MathematikKnotenVorlagen.Division)) {
            val alt=vorlage.erzeuge(GraphPunkt.Zero)
            abgelehnt(alt.copy(anschlüsse=alt.anschlüsse+AnschlussDaten(name="divisorNull",richtung=AnschlussRichtung.Ausgang,kante=AnschlussKante.Rechts,art=MathematikAnschlussArten.Aussage.id)))
        }
    }
    @Test fun `semantisch widerspruechlicher Port wird nicht durch grobe Zahlart geheilt`() {
        val alt=MathematikKnotenVorlagen.Kehrwert.erzeuge(GraphPunkt.Zero)
        abgelehnt(alt.copy(anschlüsse=alt.anschlüsse.map {it.copy(vertrag=AnschlussVertrag(TypAusdruck.Atom(TypId(MathematikAnschlussArten.Aussage.id.wert))))}))
    }
}

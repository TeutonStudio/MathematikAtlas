package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.AtlasZwischenablage
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.TypSystem.TypInferenzRegel
import kotlin.test.*

class StrukturKonsolidierungMigrationTest {
    private val register=GesamterMathematikAuswerter.erzeugeRegister()
    @Test fun `historische Suchvorlagen erzeugen kanonische Rechner`() {
        val katalog=de.TeutonStudio.MathematikKnoten.katalog.KanonischerMathematikKnotenKatalog
        for(v in alleMathematikKnotenVorlagen().filter {it.art in strukturKonsolidierungAltarten}) {
            val neu=katalog.kanonisiereMethodenUndRelationsVorlage(v)
            assertTrue(neu.art in setOf(MatrixRechner.KNOTEN_ART,VektorRechner.KNOTEN_ART))
        }
    }
    private fun z(n:Long)=RationaleZahl.von(n)
    private fun phasen(k:KartenDaten)=listOf(MathematikKartenMigrationen::nachDekodierung,MathematikKartenMigrationen::nachLaden,MathematikKartenMigrationen::vorSpeichern,
        { alt:KartenDaten -> MathematikKartenCodec.lese(KartenDatenJson.schreibe(alt)) },
        { alt:KartenDaten -> MathematikKartenCodec.lade(KartenDatenJson.schreibe(alt)) },
        { alt:KartenDaten -> MathematikKartenCodec.importiere(KartenDatenJson.schreibe(alt)) }).map {it(k)}
    @Test fun `Spur behaelt Verbindungen Metadaten und direkte Rollen in jeder Codecphase`() {
        val alt=SpurKnotenVorlagen.Spur.erzeuge(GraphPunkt.Zero).copy(name="Meine Spur")
        val quelle=alt.copy(id=KnotenId("quelle"));val ziel=alt.copy(id=KnotenId("ziel"))
        val verbindungen=listOf(VerbindungDaten(von=AnschlussVerweis(quelle.id,quelle.anschlüsse.last().id),zu=AnschlussVerweis(alt.id,alt.anschlüsse.first().id)),
            VerbindungDaten(von=AnschlussVerweis(alt.id,alt.anschlüsse.last().id),zu=AnschlussVerweis(ziel.id,ziel.anschlüsse.first().id)))
        val karte=KartenDaten(name="alt",knoten=listOf(quelle,alt,ziel),verbindungen=verbindungen)
        for(neu in phasen(karte)) {
            val k=neu.knoten[1]
            assertEquals(MatrixRechner.KNOTEN_ART,k.art);assertEquals("matrix.spur",k.parameter[RECHNER_OPERATOR_PARAMETER])
            assertEquals(alt.id,k.id);assertEquals(alt.name,k.name);assertEquals(alt.anschlüsse.map {it.id},k.anschlüsse.map {it.id})
            assertEquals(verbindungen,neu.verbindungen);assertEquals(neu,neu.migriereStrukturKonsolidierung())
            val r=register.finde(k.art)!!.auswerten(KnotenAuswertungsKontext(k,mapOf("matrix" to BedingterWert(Matrix(listOf(listOf(z(7)))))),RechenKontext()))
            assertNull(r.fehler);assertEquals(z(7),r.ausgaben.getValue("wert").objekt)
            assertEquals(k.id,MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(neu)).knoten[1].id)
        }
    }
    @Test fun `alle Diagonalmodi erhalten Ergebnisform und rechteckige Nebendiagonale`() {
        val matrix=Matrix(listOf(listOf(z(1),z(2),z(3)),listOf(z(4),z(5),z(6))))
        for(art in MatrixDiagonalArt.entries) for(ausgabe in VektorStrukturAusgabe.entries) for(erzeugen in listOf(false,true)) {
            val alt=konfiguriereMatrixdiagonale(MatrixdiagonaleKnotenVorlagen.Matrixdiagonale.erzeuge(GraphPunkt.Zero),
                if(erzeugen) MATRIXDIAGONALE_ERZEUGEN else MATRIXDIAGONALE_AUSLESEN,ausgabe.stabileId).let {it.copy(parameter=it.parameter+(MATRIXDIAGONALE_ART_PARAMETER to art.parameterWert))}
            for(neu in phasen(KartenDaten(name="D",knoten=listOf(alt)))) {
                val k=neu.knoten.single();assertEquals(MatrixRechner.KNOTEN_ART,k.art,k.parameter.toString())
                assertFalse(RECHNER_MIGRATION_DIAGNOSE in k.parameter,k.parameter.toString())
                val eingang=if(erzeugen) Tupel(listOf(z(2),z(3))) else matrix
                val r=register.finde(k.art)!!.auswerten(KnotenAuswertungsKontext(k,mapOf((if(erzeugen) "werte" else "matrix") to BedingterWert(eingang)),RechenKontext()))
                assertNull(r.fehler)
                assertEquals(if(erzeugen) diagonalmatrixErzeugen(eingang,art) else projiziereMatrixDiagonale(matrixDiagonale(matrix,art),ausgabe),r.ausgaben.getValue("wert").objekt)
            }
        }
    }
    @Test fun `Kreuzprodukt remappt Defaults Kartenfallbacks und typisierte Rollen ohne IDwechsel`() {
        val basis=VektorOrientierungsV2300Vorlagen.Kreuzprodukt.erzeuge(GraphPunkt.Zero)
        val alt=basis.copy(parameter=basis.parameter+mapOf("standardwert.a" to "a", "methodenAnforderung.a.ergebnisArt" to "mathematik.vektor.spalte"),
            eingangsKartenVerweise=mapOf("a" to KartenVerweis(KartenId("extern"),7)),
            anschlüsse=basis.anschlüsse.map {if(it.richtung==AnschlussRichtung.Ausgang) it.copy(artFolgtEingang="a",typInferenz=TypInferenzRegel.TupelAus(listOf("a","b"))) else it})
        for(karte in phasen(KartenDaten(name="K",knoten=listOf(alt)))) {
        val neu=karte.knoten.single()
        assertEquals(VektorRechner.KNOTEN_ART,neu.art);assertEquals(alt.anschlüsse.map {it.id},neu.anschlüsse.map {it.id})
        assertEquals(mapOf("links" to KartenVerweis(KartenId("extern"),7)),neu.eingangsKartenVerweise)
        assertEquals("a",neu.parameter["standardwert.links"]);assertEquals("mathematik.vektor.spalte",neu.parameter["methodenAnforderung.links.ergebnisArt"])
        val ausgang=neu.anschlüsse.last();assertEquals("links",ausgang.artFolgtEingang)
        assertEquals(TypInferenzRegel.TupelAus(listOf("links","rechts")),ausgang.typInferenz)
        val a=SpaltenVektor(listOf(z(1),z(0),z(0)));val b=SpaltenVektor(listOf(z(0),z(1),z(0)))
        val r=register.finde(neu.art)!!.auswerten(KnotenAuswertungsKontext(neu,mapOf("links" to BedingterWert(a),"rechts" to BedingterWert(b)),RechenKontext()))
        assertNull(r.fehler);assertEquals(kreuzprodukt(a,b),r.ausgaben.getValue("vektor").objekt)
        val falsch=register.finde(neu.art)!!.auswerten(KnotenAuswertungsKontext(neu,mapOf("links" to BedingterWert(a.transponiert()),"rechts" to BedingterWert(b)),RechenKontext()))
        assertNotNull(falsch.fehler)
        val geladen=MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(karte)).knoten.single()
        assertEquals(ausgang.typInferenz,geladen.anschlüsse.last().typInferenz)
        assertEquals(ausgang.artFolgtEingang,geladen.anschlüsse.last().artFolgtEingang)
        }
    }
    @Test fun `mehrfach verbundener Einzelanschluss wird unveraendert diagnostiziert`() {
        val alt=VektorOrientierungsV2300Vorlagen.Radius.erzeuge(GraphPunkt.Zero)
        val ziel=AnschlussVerweis(alt.id,alt.anschlüsse.first().id)
        val kanten=(1..2).map {VerbindungDaten(von=AnschlussVerweis(KnotenId("q$it"),AnschlussId("aus$it")),zu=ziel)}
        val karte=KartenDaten(name="Mehrdeutig",knoten=listOf(alt),verbindungen=kanten)
        for(neu in phasen(karte)) {
            val k=neu.knoten.single()
            assertTrue(RECHNER_MIGRATION_DIAGNOSE in k.parameter)
            assertEquals(alt,k.copy(parameter=k.parameter-RECHNER_MIGRATION_DIAGNOSE))
            assertEquals(kanten,neu.verbindungen)
        }
    }
    @Test fun `Kartenfallbackbeitraege behalten Anschluss und Verbindungsidentitaet bei Rollenumsetzung`() {
        val basis=VektorOrientierungsV2300Vorlagen.Kreuzprodukt.erzeuge(GraphPunkt.Zero)
        val port=basis.anschlüsse.first {it.name=="a"}
        val rolle="a@karte:${port.id.wert}"
        val extern=KartenVerweis(KartenId("extern"),7)
        val alt=basis.copy(eingangsKartenVerweise=mapOf(rolle to extern))
        val v=VerbindungDaten(von=AnschlussVerweis(KnotenId("quelle"),AnschlussId("aus")),zu=AnschlussVerweis(alt.id,port.id),eingangsReihenfolge=5,eingangsFallback=rolle)
        for(karte in phasen(KartenDaten(name="Fallback",knoten=listOf(alt),verbindungen=listOf(v)))) {
            val neu=karte.knoten.single()
            assertEquals(mapOf("links@karte:${port.id.wert}" to extern),neu.eingangsKartenVerweise)
            assertEquals(listOf(v.copy(eingangsFallback="links@karte:${port.id.wert}")),karte.verbindungen)
        }
        val kollidierend=basis.copy(eingangsKartenVerweise=mapOf("a" to extern,"links" to extern))
        val bewahrt=KartenDaten(name="Kollision",knoten=listOf(kollidierend)).migriereStrukturKonsolidierung().knoten.single()
        assertTrue(RECHNER_MIGRATION_DIAGNOSE in bewahrt.parameter)
        assertEquals(kollidierend,bewahrt.copy(parameter=bewahrt.parameter-RECHNER_MIGRATION_DIAGNOSE))
    }
    @Test fun `migrierte Strukturknoten behalten beim Duplizieren Kopieren und Undo ihre Vertraege`() {
        val alt=VektorOrientierungsV2300Vorlagen.Radius.erzeuge(GraphPunkt.Zero).copy(eingangsKartenVerweise=mapOf("vektor" to KartenVerweis(KartenId("extern"),7)))
        val k=KartenDaten(name="R",knoten=listOf(alt)).migriereStrukturKonsolidierung().knoten.single()
        val q=MathematikKnotenVorlagen.Vektor.erzeuge(GraphPunkt.Zero)
        val verbindung=VerbindungDaten(von=AnschlussVerweis(q.id,q.anschlüsse.last().id),zu=AnschlussVerweis(k.id,k.anschlüsse.first().id),eingangsReihenfolge=4)
        val original=KartenDaten(name="Kopie",knoten=listOf(q,k),verbindungen=listOf(verbindung))
        val editor=de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand(original,MathematikKartenLaufzeit().graphPrüfung)
        editor.wähleAlleKnoten();editor.dupliziereAuswahl()
        val kopie=editor.karte
        val neu=kopie.knoten.single {it.art==VektorRechner.KNOTEN_ART && it.id!=k.id}
        assertEquals(k.parameter,neu.parameter);assertEquals(k.eingangsKartenVerweise,neu.eingangsKartenVerweise)
        assertTrue(neu.anschlüsse.none {p->k.anschlüsse.any {it.id==p.id}})
        val kante=kopie.verbindungen.single {it.id!=verbindung.id}
        assertEquals(neu.id,kante.zu.knotenId);assertEquals(4,kante.eingangsReihenfolge)
        editor.rückgängig();assertEquals(original,editor.karte)
        editor.wiederholen();assertEquals(kopie,editor.karte)
        val clip=AtlasZwischenablage();assertTrue(clip.kopiere(original,original.knoten.map {it.id}.toSet()))
        val einfügung=assertNotNull(clip.erzeugeEinfügung(GraphPunkt.Zero))
        val eingefügt=einfügung.knoten.single {it.art==VektorRechner.KNOTEN_ART}
        assertEquals(k.eingangsKartenVerweise,eingefügt.eingangsKartenVerweise)
        assertTrue(einfügung.verbindungen.all {it.id!=verbindung.id && it.zu.knotenId==eingefügt.id})
    }
    @Test fun `Radius wird Norm und unbekannte oder widerspruechliche Altkonfigurationen bleiben erhalten`() {
        val radius=VektorOrientierungsV2300Vorlagen.Radius.erzeuge(GraphPunkt.Zero)
        val neu=KartenDaten(name="R",knoten=listOf(radius)).migriereStrukturKonsolidierung().knoten.single()
        assertEquals("vektor.norm",neu.parameter[RECHNER_OPERATOR_PARAMETER],neu.parameter.toString());assertEquals("norm",neu.anschlüsse.last().name)
        val diagonal=MatrixdiagonaleKnotenVorlagen.Matrixdiagonale.erzeuge(GraphPunkt.Zero)
        val kreuz=VektorOrientierungsV2300Vorlagen.Kreuzprodukt.erzeuge(GraphPunkt.Zero)
        val ungültig=listOf(diagonal.copy(parameter=diagonal.parameter+(MATRIXDIAGONALE_ART_PARAMETER to "unbekannt")),
            diagonal.copy(parameter=diagonal.parameter+(MATRIXDIAGONALE_MODUS to "unbekannt")),
            diagonal.copy(parameter=diagonal.parameter+(MATRIXDIAGONALE_AUSGABE to "unbekannt")),
            kreuz.copy(parameter=kreuz.parameter+(VEKTOR_ORIENTIERUNG_PARAMETER to "unbekannt")),
            radius.copy(anschlüsse=radius.anschlüsse.map {if(it.richtung==AnschlussRichtung.Eingang) it.copy(name="irgendwas") else it}),
            radius.copy(anschlüsse=radius.anschlüsse.map {it.copy(belegung=AnschlussBelegung.Mehrfach)}))
        for(alt in ungültig) for(karte in phasen(KartenDaten(name="U",knoten=listOf(alt)))) {
            val k=karte.knoten.single();assertTrue(RECHNER_MIGRATION_DIAGNOSE in k.parameter)
            assertEquals(alt,k.copy(parameter=k.parameter-RECHNER_MIGRATION_DIAGNOSE))
            assertEquals(karte,karte.migriereStrukturKonsolidierung())
            val auswertung=KartenAuswerter(register).auswerten(karte).knoten.getValue(k.id)
            assertTrue(auswertung.ausgaben.isEmpty());assertTrue(auswertung.fehler.orEmpty().startsWith("Migrationsdiagnose:"))
        }
    }
}

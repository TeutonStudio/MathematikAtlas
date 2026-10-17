package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class KanonischeDefinitionsKartenTest {
    private val register=GesamterMathematikAuswerter.erzeugeRegister().apply {
        // Konzeptausgänge sind Beschriftungen der Definitionskarte. Für die
        // Ausführungsprüfung verwenden sie den vorhandenen Durchreichvertrag.
        registriere(KonzeptKnotenArten.AUSGANG, requireNotNull(finde("mathematik.kartenAusgang")))
    }
    @Test fun `leere lokale Mengendefinition ist unmittelbar kanonisch auswertbar`() {
        val k=MathematikKnotenVorlagen.EndlicheMenge.erzeuge(GraphPunkt.Zero).copy(parameter=mapOf(ENDLICHE_MENGE_KONFIGURATION_PARAMETER to EndlicheMengeKonfiguration().zuParameter()))
        val karte=endlicheMengeKonzept(k).reiter.single().karte
        assertTrue(karte.knoten.none {it.art=="mathematik.leereMenge"})
        val r=KartenAuswerter(register).auswerten(karte)
        assertTrue(r.fehler.isEmpty(),r.fehler.toString())
        assertEquals(LeereMenge,r.knoten.getValue(karte.knoten.single {it.art==KonzeptKnotenArten.AUSGANG}.id).ausgaben.values.single().objekt)
        assertEquals(karte,MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(karte)))
    }
    @Test fun `Multinomdefinition liefert alle Monome fuer beide Orientierungen und Tupel ohne Altarten`() {
        for(form in listOf(MULTINOM_AUSGABE_VEKTOR,MULTINOM_AUSGABE_TUPEL)) for(orient in listOf(VEKTOR_ORIENTIERUNG_SPALTE,VEKTOR_ORIENTIERUNG_ZEILE)) for(dim in listOf(0,1,3)) {
            val karte=multinomVektorDefinitionsKarte(form,orient)
            assertTrue(karte.knoten.none {it.art in setOf("mathematik.subtraktion","mathematik.potenz","mathematik.addition")})
            assertTrue(karte.knoten.none {RECHNER_MIGRATION_DIAGNOSE in it.parameter},karte.knoten.map {it.parameter}.toString())
            val vorgaben=karte.knoten.filter {it.art==KonzeptKnotenArten.EINGANG}.associate {k -> k.id to mapOf("wert" to BedingterWert(if(k.parameter["rolle"]=="dim") RationaleZahl.von(dim.toLong()) else RationaleZahl.von(2)))}
            val r=KartenAuswerter(register).auswerten(karte,vorgaben)
            assertTrue(r.fehler.isEmpty(),"$form/$orient/$dim: ${r.fehler}")
            val wert=r.knoten.getValue(karte.knoten.single {it.art==KonzeptKnotenArten.AUSGANG}.id).ausgaben.values.single().objekt
            val komponenten=when(wert) {is Tupel -> wert.elemente;is OrientierterVektor -> wert.werte;else -> error(wert.toString())}
            assertEquals((0..dim).map {RationaleZahl.von(1L shl it)},komponenten.map {vereinfache(it as ZahlAusdruck)})
            if(form==MULTINOM_AUSGABE_VEKTOR) assertEquals(orient==VEKTOR_ORIENTIERUNG_ZEILE,wert is ZeilenVektor)
            assertEquals(karte,MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(karte)))
        }
    }
}

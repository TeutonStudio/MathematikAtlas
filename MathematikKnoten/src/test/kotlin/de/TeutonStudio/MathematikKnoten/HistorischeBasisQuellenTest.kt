package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class HistorischeBasisQuellenTest {
    private fun phasen(k:KartenDaten)=listOf(MathematikKartenMigrationen::nachDekodierung,MathematikKartenMigrationen::nachLaden,MathematikKartenMigrationen::vorSpeichern).map {it(k)}
    @Test fun `persistierter Basisindex bleibt nullbasiert und Originalfehler werden nicht umgedeutet`() {
        val modern=VektorOrientierungsV2300Vorlagen.EinheitsVektor.erzeuge(GraphPunkt.Zero)
        val basis=modern.copy(anschlüsse=modern.anschlüsse.filter {it.richtung==AnschlussRichtung.Ausgang},parameter=mapOf("dimension" to "3"))
        for(index in listOf<Int?>(null,0,1,2,3)) {
            val alt=if(index==null) basis else basis.copy(parameter=basis.parameter+("index" to index.toString()))
            for(k in phasen(KartenDaten(name="Historischer Basisindex",knoten=listOf(alt)))) {
                val neu=k.knoten.single();assertEquals(alt.anschlüsse.single().id,neu.anschlüsse.last().id)
                if(index==3) {
                    assertNotNull(neu.parameter[RECHNER_MIGRATION_DIAGNOSE]);assertEquals(alt,neu.copy(parameter=alt.parameter))
                } else {
                    assertNull(neu.parameter[RECHNER_MIGRATION_DIAGNOSE])
                    val r=KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister()).auswerten(k).knoten.getValue(alt.id)
                    assertNull(r.fehler)
                    assertEquals(einheitsSpaltenVektor(3,(index ?: 0)+1),r.ausgaben.getValue("vektor").objekt)
                }
                assertEquals(k,MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(k)))
            }
        }
    }
    @Test fun `historische Zeilenquelle erhaelt explizite Orientierung und dynamische Komponentenports`() {
        for(orientierung in listOf(VEKTOR_ORIENTIERUNG_ZEILE,VEKTOR_ORIENTIERUNG_SPALTE)) {
            val art=if(orientierung==VEKTOR_ORIENTIERUNG_ZEILE) MathematikAnschlussArten.ZeilenVektor.id else MathematikAnschlussArten.SpaltenVektor.id
            val v=MathematikKnotenVorlagen.ZeilenVektor.erzeuge(GraphPunkt.Zero)
            val d=v.anschlüsse.first().copy(id=AnschlussId("dynamisch"),name="input3",reihenfolge=2,dynamischErzeugt=true)
            val alt=v.copy(anschlüsse=v.anschlüsse.map {if(it.richtung==AnschlussRichtung.Ausgang) it.copy(art=art) else it}+d,
                parameter=v.parameter+(VEKTOR_ORIENTIERUNG_PARAMETER to orientierung))
            for(k in phasen(KartenDaten(name="Orientierung",knoten=listOf(alt)))) {
                val neu=k.knoten.single();assertNull(neu.parameter[RECHNER_MIGRATION_DIAGNOSE])
                assertEquals(VEKTOR_KONSTRUKTOR_ART,neu.art);assertEquals(orientierung,neu.parameter[VEKTOR_ORIENTIERUNG_PARAMETER])
                assertEquals(alt.anschlüsse,neu.anschlüsse)
            }
        }
    }
}

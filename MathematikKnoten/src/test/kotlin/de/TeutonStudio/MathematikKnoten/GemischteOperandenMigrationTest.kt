package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class GemischteOperandenMigrationTest {
    @Test fun `gemischte externe Sammel und Einzelports bleiben mit Diagnose vollstaendig erhalten`() {
        val basis=ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val alt=basis.copy(anschlüsse=basis.anschlüsse.mapIndexed {i,p -> if(i==0) p.copy(belegung=AnschlussBelegung.Mehrfach) else p})
        val karte=KartenDaten(name="Extern",knoten=listOf(alt))
        for(neu in listOf(MathematikKartenMigrationen.nachDekodierung(karte),MathematikKartenMigrationen.nachLaden(karte),MathematikKartenMigrationen.vorSpeichern(karte))) {
            val k=neu.knoten.single();assertNotNull(k.parameter[RECHNER_MIGRATION_DIAGNOSE]);assertEquals(alt,k.copy(parameter=alt.parameter))
            val r=KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister()).auswerten(neu).knoten.getValue(alt.id)
            assertNotNull(r.fehler);assertTrue(r.ausgaben.isEmpty())
            assertEquals(neu,MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(neu)))
        }
    }
}

package de.TeutonStudio.MathematikKnoten.visualisierung

import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikKnoten.visualisierung.sampling.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*

class VisualisierungsBerechnungTest {
    private fun punkt(n:Int)=EndlicheMenge(setOf(RationaleZahl.von(n.toLong())))
    @Test fun unterschiedlicheEingaengeUndStilcache()=runBlocking<Unit> {
        val b=VisualisierungsBerechnung();val c=VisualisierungsKonfiguration(dimension=RaumDimension.R1)
        try {
            val a=b.berechne(punkt(1),c,false);val z=b.berechne(punkt(2),c,false)
            assertNotEquals(a,z)
            assertSame(a,b.gecacht(punkt(1),c))
            val andererStil=c.copy(farbe=c.farbe.copy(festeFarbe=0xFFFF0000,palette="Wald"),rasterSichtbar=false)
            assertSame(a,b.berechne(punkt(1),andererStil,false){error("Stil darf kein Sampling auslösen")})
        }finally{b.schließen()}
    }
    @Test fun identischeAnfragenTeilenArbeitUndEinAbgemeldeterVerbraucherBrichtAndereNichtAb()=runBlocking<Unit> {
        withTimeout(5_000) {
            val b=VisualisierungsBerechnung();val start=CompletableDeferred<Unit>();val fertig=CompletableDeferred<Unit>();val anzahl=AtomicInteger()
            suspend fun sample():VisualisierungsErgebnis {anzahl.incrementAndGet();start.complete(Unit);fertig.await();return VisualisierungsErgebnis.Erfolgreich(emptyList())}
            try {
                val a=async(start=CoroutineStart.UNDISPATCHED){b.berechne(punkt(1),VisualisierungsKonfiguration(),false){sample()}}
                start.await()
                val z=async(start=CoroutineStart.UNDISPATCHED){b.berechne(punkt(1),VisualisierungsKonfiguration(),true){sample()}}
                a.cancelAndJoin();fertig.complete(Unit);z.await();assertEquals(1,anzahl.get())
            }finally{b.schließen()}
        }
    }
    @Test fun maximalZweiAuftraegeUndDialogHatVorrang()=runBlocking<Unit> {
        withTimeout(5_000) {
            val b=VisualisierungsBerechnung();val starts=Channel<Int>(Channel.UNLIMITED);val gates=(0..3).map{CompletableDeferred<Unit>()}
            fun auftrag(n:Int,detail:Boolean)=async(start=CoroutineStart.UNDISPATCHED){b.berechne(punkt(n),VisualisierungsKonfiguration(),detail){starts.send(n);gates[n].await();VisualisierungsErgebnis.Erfolgreich(emptyList())}}
            try {
                val a=auftrag(0,false);val z=auftrag(1,false);assertEquals(setOf(0,1),setOf(starts.receive(),starts.receive()))
                val v=auftrag(2,false);val d=auftrag(3,true);assertTrue(starts.tryReceive().isFailure)
                gates[0].complete(Unit);a.await();assertEquals(3,starts.receive())
                gates[1].complete(Unit);z.await();assertEquals(2,starts.receive())
                gates[2].complete(Unit);gates[3].complete(Unit);v.await();d.await()
            }finally{b.schließen()}
        }
    }
    @Test fun letzterVerbraucherBrichtAbUndNeuerAuftragIstMoeglich()=runBlocking<Unit> {
        withTimeout(5_000) {
            val b=VisualisierungsBerechnung();val start=CompletableDeferred<Unit>();val abbruch=CompletableDeferred<Unit>()
            try {
                val a=async {b.berechne(punkt(1),VisualisierungsKonfiguration(),false){try{start.complete(Unit);awaitCancellation()}finally{abbruch.complete(Unit)}}}
                start.await();a.cancelAndJoin();abbruch.await()
                assertIs<VisualisierungsErgebnis.Erfolgreich>(b.berechne(punkt(1),VisualisierungsKonfiguration(),false){VisualisierungsErgebnis.Erfolgreich(emptyList())})
            }finally{b.schließen()}
        }
    }
    @Test fun speicherlimitWirdEingehalten()=runBlocking<Unit> {
        val b=VisualisierungsBerechnung(700)
        try {
            repeat(10){b.berechne(punkt(it),VisualisierungsKonfiguration(),false){VisualisierungsErgebnis.Erfolgreich(listOf(VisualisierungsPunkt(it.toDouble(),0.0)))}}
            assertTrue(b.speicherBytes<=700)
        }finally{b.schließen()}
    }
}

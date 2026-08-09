package de.TeutonStudio.MathematikAtlas

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import de.TeutonStudio.MathematikAtlas.speicher.KartenOrdnung
import de.TeutonStudio.MathematikAtlas.speicher.KartenOrdnungSpeicher
import de.TeutonStudio.MathematikAtlas.speicher.KartenSpeicher

internal data class BeispielKartenErstellungsErgebnis(
    val ordnerPfad: List<String>,
    val anzahl: Int,
)

/**
 * Testbarer, synchroner Dateivorgang. Die UI führt ihn auf Dispatchers.IO aus.
 * Bei einem Fehler werden nur die in diesem Lauf neu gespeicherten Karten entfernt
 * und die vorherige Kartenordnung bestmöglich wiederhergestellt.
 */
internal class BeispielKartenVerwaltung(
    private val ladeOrdnung: () -> KartenOrdnung,
    private val speichereOrdnung: (KartenOrdnung) -> Unit,
    private val speichereKarte: (KartenDaten) -> KartenDaten,
    private val löscheKarten: (Set<KartenId>) -> Unit,
    private val erzeugeKarten: () -> List<KartenDaten> = BeispielKarten::alle,
) {
    fun erstelleNeu(): BeispielKartenErstellungsErgebnis {
        val vorherigeOrdnung = ladeOrdnung()
        val zielPfad = eindeutigerBeispielKartenOrdner(vorherigeOrdnung)
        val erzeugteKarten = erzeugeKarten()
        val gespeicherteKarten = mutableListOf<KartenDaten>()

        try {
            erzeugteKarten.forEach { karte -> gespeicherteKarten += speichereKarte(karte) }
            val zuordnungen = gespeicherteKarten.associate { karte -> karte.id to zielPfad }
            val neueOrdnung = vorherigeOrdnung
                .mitOrdner(zielPfad)
                .mitKartenInOrdnern(zuordnungen)
            speichereOrdnung(neueOrdnung)

            return BeispielKartenErstellungsErgebnis(
                ordnerPfad = zielPfad,
                anzahl = gespeicherteKarten.size,
            )
        } catch (fehler: Throwable) {
            val ids = gespeicherteKarten.mapTo(linkedSetOf(), KartenDaten::id)
            runCatching { if (ids.isNotEmpty()) löscheKarten(ids) }
            runCatching { speichereOrdnung(vorherigeOrdnung) }
            throw fehler
        }
    }
}

internal fun erstelleBeispielKartenVerwaltung(
    context: Context,
    speicher: KartenSpeicher,
): BeispielKartenVerwaltung {
    val ordnungsSpeicher = KartenOrdnungSpeicher(context)
    return BeispielKartenVerwaltung(
        ladeOrdnung = ordnungsSpeicher::lade,
        speichereOrdnung = ordnungsSpeicher::speichere,
        speichereKarte = speicher::speichere,
        löscheKarten = { ids ->
            val blockierend = speicher.löscheEndgültig(ids)
            check(blockierend.isEmpty()) {
                "Neu erzeugte Beispielkarten konnten wegen externer Kartenverweise nicht zurückgerollt werden."
            }
        },
    )
}

internal fun eindeutigerBeispielKartenOrdner(ordnung: KartenOrdnung): List<String> {
    val stammNamen = ordnung.ordner.asSequence()
        .filter { it.size == 1 }
        .map { it.single() }
        .toSet()

    val basis = "Beispiel Karten"
    if (basis !in stammNamen) return listOf(basis)

    var nummer = 2
    while ("$basis $nummer" in stammNamen) nummer += 1
    return listOf("$basis $nummer")
}

/** Erzwingt nach externen Speicheränderungen eine Neuladung der sichtbaren Karten-/Ordnerliste. */
internal object KartenAenderungsSignal {
    var version by mutableIntStateOf(0)
        private set

    fun markiereAenderung() {
        version += 1
    }
}

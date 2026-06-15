package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten

import androidx.compose.runtime.snapshots.SnapshotStateMap
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AusgangDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.EingangDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.AnschlussKnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.IDEhe
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import kotlin.reflect.KClass

typealias PullObjekt = PullSystem<*>

data class PullSchlüssel(
    val knotenId: String,
    val anschlussId: String,
)

sealed interface PullErgebnis<out T : Any> {

    data class Wert<T : Any>(
        val wert: T,
    ) : PullErgebnis<T>

    data class Fehler(
        val meldung: String,
        val ursache: Throwable? = null,
    ) : PullErgebnis<Nothing>
}

class PullKontext {

    internal val aktivePfade:
            MutableSet<PullSchlüssel> = mutableSetOf()

    internal val ergebnisse:
            MutableMap<PullSchlüssel, PullErgebnis<*>> = mutableMapOf()
}

/**
 * T ist der Werttyp, der durch die Anschlüsse transportiert wird.
 *
 * Beispiele:
 * PullSystem<Aussage>
 * PullSystem<Menge>
 * PullSystem<Zahl>
 */
interface PullSystem<T : Any> {

    val graph: Graph
    val daten: AnschlussKnotenDaten

    /**
     * Nur für UI/Debugging.
     *
     * Der Schlüssel ist die Anschluss-ID, nicht das Anschlussobjekt.
     */
    val cacheAnschlüsse:
            SnapshotStateMap<String, PullErgebnis<T>>

    /**
     * Dient zur Laufzeitprüfung, wenn ein fremdes PullSystem<*>
     * abgefragt wird.
     */
    val wertKlasse: KClass<T>

    /**
     * Berechnet einen Ausgang aus den Ergebnissen aller Eingänge.
     *
     * Quellen erhalten eine leere Eingangsmap.
     */
    fun berechne(
        ausgangId: String,
        eingänge: Map<String, PullErgebnis<T>>,
    ): PullErgebnis<T>

    /**
     * Zieht den Wert eines eigenen Ausgangs.
     */
    fun pull(
        ausgangId: String,
        kontext: PullKontext = PullKontext(),
    ): PullErgebnis<T> {
        val schlüssel = PullSchlüssel(
            knotenId = daten.id,
            anschlussId = ausgangId,
        )

        @Suppress("UNCHECKED_CAST")
        kontext.ergebnisse[schlüssel]?.let {
            return it as PullErgebnis<T>
        }

        if (!kontext.aktivePfade.add(schlüssel)) {
            return PullErgebnis.Fehler(
                "Zyklische Abhängigkeit bei ${daten.id}:$ausgangId"
            )
        }

        val ergebnis = try {
            val ausgangExistiert = daten.anschlüsse
                .filterIsInstance<AusgangDaten>()
                .any { it.id == ausgangId }

            if (!ausgangExistiert) {
                PullErgebnis.Fehler(
                    "Knoten ${daten.id} besitzt keinen Ausgang $ausgangId"
                )
            } else {
                val eingänge = daten.anschlüsse
                    .filterIsInstance<EingangDaten>()
                    .associate { eingang ->
                        eingang.id to pullEingang(
                            eingangId = eingang.id,
                            kontext = kontext,
                        )
                    }

                try {
                    berechne(
                        ausgangId = ausgangId,
                        eingänge = eingänge,
                    )
                } catch (fehler: Throwable) {
                    PullErgebnis.Fehler(
                        meldung = buildString {
                            append("Berechnung von ")
                            append(daten.id)
                            append(':')
                            append(ausgangId)
                            append(" fehlgeschlagen: ")
                            append(fehler.message ?: fehler::class.simpleName)
                        },
                        ursache = fehler,
                    )
                }
            }
        } finally {
            kontext.aktivePfade.remove(schlüssel)
        }

        kontext.ergebnisse[schlüssel] = ergebnis
        cacheAnschlüsse[ausgangId] = ergebnis

        return ergebnis
    }

    /**
     * Zieht den Wert, der mit einem eigenen Eingang verbunden ist.
     *
     * Diese Methode kann auch von einem reinen Auswertungs-/Senkenknoten
     * benutzt werden, der selbst keinen Ausgang besitzt.
     */
    fun pullEingang(
        eingangId: String,
        kontext: PullKontext = PullKontext(),
    ): PullErgebnis<T> {
        val eingangExistiert = daten.anschlüsse
            .filterIsInstance<EingangDaten>()
            .any { it.id == eingangId }

        if (!eingangExistiert) {
            return PullErgebnis.Fehler(
                "Knoten ${daten.id} besitzt keinen Eingang $eingangId"
            )
        }

        val verbindungen = graph.karte.verbindungen.filter {
            it.daten.ids.enthält(
                knotenId = daten.id,
                anschlussId = eingangId,
            )
        }

        if (verbindungen.isEmpty()) {
            return PullErgebnis.Fehler(
                "Eingang ${daten.id}:$eingangId ist nicht verbunden"
            )
        }

        if (verbindungen.size > 1) {
            return PullErgebnis.Fehler(
                "Eingang ${daten.id}:$eingangId besitzt mehrere Verbindungen"
            )
        }

        val ids = verbindungen.single().daten.ids

        val fremderAnschluss = when {
            ids.knotenIdMann == daten.id &&
                    ids.anschlussIdMann == eingangId -> {
                graph.karte.erhalteAnschlussWeib(ids)
            }

            ids.knotenIdWeib == daten.id &&
                    ids.anschlussIdWeib == eingangId -> {
                graph.karte.erhalteAnschlussMann(ids)
            }

            else -> null
        } ?: return PullErgebnis.Fehler(
            "Gegenanschluss für ${daten.id}:$eingangId wurde nicht gefunden"
        )

        if (fremderAnschluss.daten !is AusgangDaten) {
            return PullErgebnis.Fehler(
                "Der verbundene Anschluss ${fremderAnschluss.daten.id} " +
                        "ist kein Ausgang"
            )
        }

        val fremdesPullSystem =
            fremderAnschluss.besitzer as? PullObjekt
                ?: return PullErgebnis.Fehler(
                    "Knoten ${fremderAnschluss.besitzer.daten.id} " +
                            "unterstützt kein PullSystem"
                )

        val fremdesErgebnis = fremdesPullSystem.pull(
            ausgangId = fremderAnschluss.daten.id,
            kontext = kontext,
        )

        return fremdesErgebnis.typisieren()
    }

    /**
     * Berechnet alle eigenen Ausgänge neu.
     */
    fun aktualisiereCache() {
        cacheAnschlüsse.clear()

        val kontext = PullKontext()

        daten.anschlüsse
            .filterIsInstance<AusgangDaten>()
            .forEach {
                pull(
                    ausgangId = it.id,
                    kontext = kontext,
                )
            }
    }

    @Suppress("UNCHECKED_CAST")
    private fun PullErgebnis<*>.typisieren(): PullErgebnis<T> =
        when (this) {
            is PullErgebnis.Wert<*> -> {
                if (wertKlasse.isInstance(wert)) {
                    this as PullErgebnis<T>
                } else {
                    PullErgebnis.Fehler(
                        "Falscher Anschlusstyp: erwartet wurde " +
                                "${wertKlasse.simpleName}, erhalten wurde " +
                                "${wert::class.simpleName}"
                    )
                }
            }

            is PullErgebnis.Fehler ->
                this as PullErgebnis<T>
        }

    private fun IDEhe.enthält(
        knotenId: String,
        anschlussId: String,
    ): Boolean =
        (
                knotenIdMann == knotenId &&
                        anschlussIdMann == anschlussId
                ) || (
                knotenIdWeib == knotenId &&
                        anschlussIdWeib == anschlussId
                )
}
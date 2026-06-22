package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDaten
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class GraphCache<D : GraphDaten, O : GraphDatenObjekt<out D>>(
    private val daten: () -> Iterable<D>,
    private val fabrik: (D) -> O?,
) : ReadOnlyProperty<Any?, List<O>> {

    private val cache = mutableMapOf<String, O>()

    private val state = derivedStateOf {
        val aktuelleDaten = daten().toList()
        val aktuelleIds = aktuelleDaten.mapTo(mutableSetOf()) { it.id }

        // Entfernte Datenelemente auch aus dem Objektcache entfernen.
        cache.keys.retainAll(aktuelleIds)

        aktuelleDaten.mapNotNull { d -> cache[d.id] ?: fabrik(d)?.also { objekt -> cache[d.id] = objekt } }
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): List<O> = state.value.apply {
        val aktuelleDaten = daten().toList()

        if (size != aktuelleDaten.size) {
            val erzeugteIds = mapTo(mutableSetOf()) { it.daten.id }
            val fehlendeIds = aktuelleDaten.map { it.id }.filterNot { it in erzeugteIds }
            Log.println(
                Log.ASSERT,
                "GraphCache",
                """
                GraphCache '${property.name}' unvollständig.
                Erwartet: ${aktuelleDaten.size}
                Erzeugt: $size
                Fehlende IDs: ${fehlendeIds.joinToString()}
                """.trimIndent(),
            )
        }
    }

    fun erhalte(): List<O> = state.value
}
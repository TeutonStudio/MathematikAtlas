package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class GraphCache<D: GraphDaten, O: GraphDatenObjekt<out D>>(
    daten: SnapshotStateList<out D>,
    fabrik: (D) -> O?,
): ReadOnlyProperty<Any?, Iterable<O>> {
    private val state = derivedStateOf { daten.mapNotNull { cache[it.id] ?: fabrik(it)?.also { obj -> cache[it.id] = obj } } }
    private val cache = mutableMapOf<String, O>()

    public override fun getValue(
        thisRef: Any?,
        property: KProperty<*>
    ): Iterable<O> = state.value
}

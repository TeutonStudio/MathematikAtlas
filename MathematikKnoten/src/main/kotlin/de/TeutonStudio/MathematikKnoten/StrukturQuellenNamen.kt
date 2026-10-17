package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

/** Namen dienen nur der lokalen Darstellung; die Zuordnung erfolgt über die Quellenidentität. */
internal fun strukturQuellenParameter(quelle:VariablenQuelle,freie:Set<MethodenParameter>):MethodenParameter? {
    val bindung=quelle.strukturBindung ?: return null
    val nachId=freie.filter {it.bindungsId==bindung.id}
    require(nachId.size<=1) {"Die Parameterrepräsentation der Quelle '${bindung.parameterName}' ist widersprüchlich."}
    if(nachId.isNotEmpty()) return nachId.single()
    return freie.singleOrNull {it.bindungsId==null && it.name==quelle.name}
}

internal fun strukturQuellenNamen(quellen:List<VariablenQuelle>,freie:Set<MethodenParameter>):Map<String,String> {
    val gruppen=quellen.filter {it.strukturBindung!=null}.groupBy {it.strukturBindung!!.id}
    val zugeordnet=gruppen.values.flatten().mapNotNull {strukturQuellenParameter(it,freie)}.toSet()
    val belegt=freie.filterNot {it in zugeordnet}.map {it.name}.toMutableSet()
    return buildMap {
        gruppen.forEach {(id,gruppe) ->
            val bindung=gruppe.first().strukturBindung!!
            require(gruppe.all {it.strukturBindung!!.parameterName==bindung.parameterName && it.strukturBindung!!.werteVorrat==bindung.werteVorrat}) {"Die strukturierte Parameterquelle ist widersprüchlich."}
            var name=bindung.parameterName
            var suffix=1
            while(!belegt.add(name)) name="${bindung.parameterName}_${suffix++}"
            put(id,name)
        }
    }
}

package de.TeutonStudio.KnotenKartenVerwalter.daten

data class GraphPunkt(val x: Float = 0f, val y: Float = 0f) {
    operator fun plus(andere: GraphPunkt) = GraphPunkt(x + andere.x, y + andere.y)
    operator fun minus(andere: GraphPunkt) = GraphPunkt(x - andere.x, y - andere.y)
    operator fun times(faktor: Float) = GraphPunkt(x * faktor, y * faktor)
    companion object { val Zero = GraphPunkt(0f, 0f) }
}

data class GraphGröße(val breite: Float = 220f, val höhe: Float = 120f)

data class AnsichtsFenster(
    val verschiebung: GraphPunkt = GraphPunkt.Zero,
    val zoom: Float = 1f,
) {
    companion object { val Standard = AnsichtsFenster() }
}

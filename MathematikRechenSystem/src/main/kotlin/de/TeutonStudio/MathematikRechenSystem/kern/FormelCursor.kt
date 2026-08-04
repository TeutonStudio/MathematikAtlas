package de.TeutonStudio.MathematikRechenSystem.kern

/** Stabile Wurzel-zu-Ausdruck-Identität eines Cursorziels. */
data class AusdruckPfad(val ids: List<String>) {
    init { require(ids.isNotEmpty()) }
    val ausdrucksId: String get() = ids.last()
}

sealed interface CursorPosition {
    data object VorAusdruck : CursorPosition
    data object NachAusdruck : CursorPosition
    data class ZwischenArgumenten(val index: Int) : CursorPosition {
        init { require(index >= 0) }
    }
    data class InPlatzhalter(val rolle: String) : CursorPosition
}

data class FormelCursor(
    val pfad: AusdruckPfad,
    val position: CursorPosition,
    /** Normierte horizontale Wunschposition für wiederholte vertikale Navigation. */
    val bevorzugteXPosition: Float? = null,
) {
    val ausdrucksId: String get() = pfad.ausdrucksId
}

enum class FormelCursorRichtung { Links, Rechts, Oben, Unten }

internal fun FormelAusdruck.standardCursor(): FormelCursor {
    val platzhalter = ersterCursorPlatzhalter()
    return if (platzhalter != null) {
        FormelCursor(platzhalter.first, CursorPosition.InPlatzhalter(platzhalter.second.rollenId))
    } else {
        FormelCursor(AusdruckPfad(listOf(id)), CursorPosition.NachAusdruck)
    }
}

internal fun FormelAusdruck.cursorFürAusdruck(
    ausdrucksId: String,
    position: CursorPosition? = null,
): FormelCursor? {
    val pfad = pfadZu(ausdrucksId) ?: return null
    val ausdruck = findeCursorAusdruck(ausdrucksId) ?: return null
    val effektiv = position ?: when (ausdruck) {
        is FormelAusdruck.Platzhalter -> CursorPosition.InPlatzhalter(ausdruck.rollenId)
        else -> CursorPosition.NachAusdruck
    }
    return FormelCursor(pfad, effektiv).takeIf { istGültigerCursor(it) }
}

internal fun FormelAusdruck.istGültigerCursor(cursor: FormelCursor): Boolean {
    val pfad = pfadZu(cursor.ausdrucksId) ?: return false
    if (pfad != cursor.pfad) return false
    val ausdruck = findeCursorAusdruck(cursor.ausdrucksId) ?: return false
    return when (val position = cursor.position) {
        CursorPosition.VorAusdruck, CursorPosition.NachAusdruck -> true
        is CursorPosition.InPlatzhalter ->
            ausdruck is FormelAusdruck.Platzhalter && ausdruck.rollenId == position.rolle
        is CursorPosition.ZwischenArgumenten ->
            ausdruck is FormelAusdruck.Operation && position.index in 1 until ausdruck.argumente.size
    }
}

internal fun FormelAusdruck.normalisiereCursor(cursor: FormelCursor?): FormelCursor {
    if (cursor != null && istGültigerCursor(cursor)) return cursor
    if (cursor != null) {
        cursor.pfad.ids.asReversed().forEach { id ->
            cursorFürAusdruck(id, CursorPosition.NachAusdruck)?.let { return it }
        }
    }
    return standardCursor()
}

internal fun FormelAusdruck.bewegeCursor(
    cursor: FormelCursor,
    richtung: FormelCursorRichtung,
): FormelCursor = when (richtung) {
    FormelCursorRichtung.Links -> bewegeHorizontal(cursor, -1)
    FormelCursorRichtung.Rechts -> bewegeHorizontal(cursor, 1)
    FormelCursorRichtung.Oben -> bewegeVertikal(cursor, -1)
    FormelCursorRichtung.Unten -> bewegeVertikal(cursor, 1)
}

internal fun FormelAusdruck.kannCursorBewegen(
    cursor: FormelCursor,
    richtung: FormelCursorRichtung,
): Boolean = bewegeCursor(cursor, richtung) != cursor

private fun FormelAusdruck.bewegeHorizontal(cursor: FormelCursor, delta: Int): FormelCursor {
    val ziele = cursorZiele()
    val aktuell = ziele.indexOf(cursor.ohneBevorzugtePosition())
    if (aktuell < 0) return normalisiereCursor(cursor)
    val zielIndex = (aktuell + delta).coerceIn(0, ziele.lastIndex)
    return ziele[zielIndex]
}

private fun FormelAusdruck.bewegeVertikal(cursor: FormelCursor, delta: Int): FormelCursor {
    val normalisiert = normalisiereCursor(cursor)
    val ids = normalisiert.pfad.ids
    for (tiefe in ids.lastIndex downTo 1) {
        val kindId = ids[tiefe]
        val elternId = ids[tiefe - 1]
        val eltern = findeCursorAusdruck(elternId) as? FormelAusdruck.Operation ?: continue
        val argumente = eltern.argumente.sortedBy { it.position }
        val index = argumente.indexOfFirst { it.ausdruck.id == kindId }
        if (index < 0) continue
        val zielIndex = vertikalerZielIndex(eltern, index, delta) ?: continue

        val aktuellesTeilziel = argumente[index].ausdruck.cursorZiele()
        val lokalerCursor = normalisiert.copy(
            pfad = AusdruckPfad(ids.drop(tiefe)),
            bevorzugteXPosition = null,
        )
        val lokalerIndex = aktuellesTeilziel.indexOf(lokalerCursor).takeIf { it >= 0 } ?: 0
        val normiertesX = normalisiert.bevorzugteXPosition
            ?: if (aktuellesTeilziel.size <= 1) 0f else lokalerIndex.toFloat() / aktuellesTeilziel.lastIndex
        val zielTeilziele = argumente[zielIndex].ausdruck.cursorZiele()
        val zielLokal = (normiertesX * zielTeilziele.lastIndex).toInt().coerceIn(0, zielTeilziele.lastIndex)
        val ziel = zielTeilziele[zielLokal]
        val elternPfad = ids.take(tiefe)
        return ziel.copy(
            pfad = AusdruckPfad(elternPfad + ziel.pfad.ids),
            bevorzugteXPosition = normiertesX,
        )
    }
    return normalisiert
}

/** Nur räumlich geschichtete Operatoren erhalten eine vertikale Nachbarschaft. */
private fun vertikalerZielIndex(
    operation: FormelAusdruck.Operation,
    aktuellerIndex: Int,
    delta: Int,
): Int? = when (operation.operatorId.substringAfterLast('.')) {
    "division" -> (aktuellerIndex + delta).takeIf { it in operation.argumente.indices }
    "potenz" -> when {
        delta < 0 && aktuellerIndex == 0 -> 1
        delta > 0 && aktuellerIndex == 1 -> 0
        else -> null
    }
    "logarithmus" -> when {
        delta < 0 && aktuellerIndex == 0 -> 1
        delta > 0 && aktuellerIndex == 1 -> 0
        else -> null
    }
    else -> null
}

internal fun FormelAusdruck.cursorZiele(): List<FormelCursor> = buildList {
    fun besuche(ausdruck: FormelAusdruck, pfad: List<String>) {
        val aktuellerPfad = AusdruckPfad(pfad + ausdruck.id)
        add(FormelCursor(aktuellerPfad, CursorPosition.VorAusdruck))
        when (ausdruck) {
            is FormelAusdruck.Platzhalter ->
                add(FormelCursor(aktuellerPfad, CursorPosition.InPlatzhalter(ausdruck.rollenId)))
            is FormelAusdruck.Operation -> {
                val argumente = ausdruck.argumente.sortedBy { it.position }
                argumente.forEachIndexed { index, argument ->
                    if (index > 0) {
                        add(FormelCursor(aktuellerPfad, CursorPosition.ZwischenArgumenten(index)))
                    }
                    besuche(argument.ausdruck, pfad + ausdruck.id)
                }
            }
            else -> Unit
        }
        add(FormelCursor(aktuellerPfad, CursorPosition.NachAusdruck))
    }
    besuche(this@cursorZiele, emptyList())
}.distinct()

internal fun FormelAusdruck.pfadZu(ausdrucksId: String): AusdruckPfad? {
    fun suche(ausdruck: FormelAusdruck, pfad: List<String>): AusdruckPfad? {
        val aktuell = pfad + ausdruck.id
        if (ausdruck.id == ausdrucksId) return AusdruckPfad(aktuell)
        if (ausdruck is FormelAusdruck.Operation) {
            ausdruck.argumente.sortedBy { it.position }.forEach { argument ->
                suche(argument.ausdruck, aktuell)?.let { return it }
            }
        }
        return null
    }
    return suche(this, emptyList())
}

internal fun FormelAusdruck.findeCursorAusdruck(ausdrucksId: String): FormelAusdruck? = when {
    id == ausdrucksId -> this
    this is FormelAusdruck.Operation -> argumente
        .sortedBy { it.position }
        .firstNotNullOfOrNull { it.ausdruck.findeCursorAusdruck(ausdrucksId) }
    else -> null
}

private fun FormelAusdruck.ersterCursorPlatzhalter(): Pair<AusdruckPfad, FormelAusdruck.Platzhalter>? {
    fun suche(ausdruck: FormelAusdruck, pfad: List<String>): Pair<AusdruckPfad, FormelAusdruck.Platzhalter>? {
        val aktuell = pfad + ausdruck.id
        if (ausdruck is FormelAusdruck.Platzhalter) return AusdruckPfad(aktuell) to ausdruck
        if (ausdruck is FormelAusdruck.Operation) {
            ausdruck.argumente.sortedBy { it.position }.forEach { argument ->
                suche(argument.ausdruck, aktuell)?.let { return it }
            }
        }
        return null
    }
    return suche(this, emptyList())
}

private fun FormelCursor.ohneBevorzugtePosition(): FormelCursor = copy(bevorzugteXPosition = null)

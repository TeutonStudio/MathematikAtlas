package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigDecimal
import java.math.BigInteger

private data class FormelEditorMoment(
    val wurzel: FormelAusdruck,
    val cursor: FormelCursor,
)

/**
 * Transaktionaler, UI-neutraler Bearbeitungszustand für eine strukturierte Formel.
 * Ausdruckshistorie und Cursorzustand bleiben getrennt: Cursorbewegungen erzeugen
 * keine Undo-Einträge, Ausdrucksmutationen sichern dagegen eine sinnvolle Position.
 */
class FormelEditorZustand(
    start: FormelAusdruck = neuerFormelPlatzhalter("wurzel", "Ausdruck"),
) {
    private val rueckgaengig = ArrayDeque<FormelEditorMoment>()
    private val wiederholen = ArrayDeque<FormelEditorMoment>()
    private var idZaehler = 0

    var wurzel: FormelAusdruck = start
        private set
    var cursor: FormelCursor = start.standardCursor()
        private set

    /** Kompatible Auswahlprojektion für bestehende Aufrufer. */
    val auswahlId: String get() = cursor.ausdrucksId

    val kannRueckgaengig: Boolean get() = rueckgaengig.isNotEmpty()
    val kannWiederholen: Boolean get() = wiederholen.isNotEmpty()
    val offenePlatzhalter: List<FormelAusdruck.Platzhalter>
        get() = wurzel.platzhalter()

    fun waehle(ausdrucksId: String): Boolean {
        val ziel = wurzel.cursorFürAusdruck(ausdrucksId) ?: return false
        cursor = ziel
        return true
    }

    fun setzeCursor(neu: FormelCursor): Boolean {
        if (!wurzel.istGültigerCursor(neu)) return false
        cursor = neu
        return true
    }

    fun setzeCursorAufAusdruck(
        ausdrucksId: String,
        position: CursorPosition,
    ): Boolean {
        val neu = wurzel.cursorFürAusdruck(ausdrucksId, position) ?: return false
        cursor = neu
        return true
    }

    fun bewegeCursor(richtung: FormelCursorRichtung): Boolean {
        val neu = wurzel.bewegeCursor(cursor, richtung)
        if (neu == cursor) return false
        cursor = neu
        return true
    }

    fun kannCursorBewegen(richtung: FormelCursorRichtung): Boolean =
        wurzel.kannCursorBewegen(cursor, richtung)

    fun naechsterPlatzhalter(richtung: Int = 1): String? {
        val platzhalter = offenePlatzhalter
        if (platzhalter.isEmpty()) return null
        val aktuell = platzhalter.indexOfFirst { it.id == cursor.ausdrucksId }
        val index = if (aktuell < 0) 0 else Math.floorMod(aktuell + richtung, platzhalter.size)
        val ziel = platzhalter[index]
        cursor = requireNotNull(
            wurzel.cursorFürAusdruck(
                ziel.id,
                CursorPosition.InPlatzhalter(ziel.rollenId),
            ),
        )
        return ziel.id
    }

    fun druecke(taste: FormelTastaturTaste): Boolean {
        val ziel = wurzel.findeCursorAusdruck(cursor.ausdrucksId) ?: return false
        val neu = taste.literal?.let { literal ->
            FormelAusdruck.Literal(neueId("literal"), literal, FormelTyp.ZAHL)
        } ?: run {
            val rollen = taste.argumentRollen.ifEmpty { listOf("argument") }
            val zielIndex = when {
                ziel is FormelAusdruck.Platzhalter -> -1
                cursor.position == CursorPosition.VorAusdruck && rollen.size > 1 -> rollen.lastIndex
                else -> 0
            }
            val argumente = rollen.mapIndexed { index, rolle ->
                val argument = if (index == zielIndex) ziel else neuerPlatzhalter(rolle)
                FormelArgument(rolle, index, argument)
            }
            FormelAusdruck.Operation(
                id = neueId("operation"),
                operatorId = requireNotNull(taste.operatorId),
                argumente = argumente,
                typ = FormelTyp.ZAHL,
            )
        }
        return ersetzeAuswahl(neu)
    }

    fun setzeZahl(text: String): Boolean {
        val rational = runCatching { parseEditorRationaleEingabe(text) }.getOrNull() ?: return false
        return ersetzeAuswahl(
            FormelAusdruck.Literal(neueId("zahl"), rational, FormelTyp.ZAHL),
        )
    }

    fun setzeVariable(name: String): Boolean {
        val bereinigt = name.trim()
        if (bereinigt.isBlank()) return false
        return ersetzeAuswahl(
            FormelAusdruck.Variable(neueId("variable"), bereinigt, bereinigt, FormelTyp.ZAHL),
        )
    }

    fun loescheAuswahl(): Boolean = ersetzeAuswahl(neuerPlatzhalter("argument"))

    fun loescheRueckwaerts(): Boolean {
        if (cursor.position == CursorPosition.VorAusdruck && bewegeCursor(FormelCursorRichtung.Links)) {
            return loescheAuswahl()
        }
        return loescheAuswahl()
    }

    fun loescheVorwaerts(): Boolean {
        if (cursor.position == CursorPosition.NachAusdruck && bewegeCursor(FormelCursorRichtung.Rechts)) {
            return loescheAuswahl()
        }
        return loescheAuswahl()
    }

    fun importiere(latex: String): FormelLatexImportErgebnis {
        val ergebnis = FormelLatexCodec.importiere(latex)
        if (ergebnis is FormelLatexImportErgebnis.Erfolg) {
            setzeWurzel(ergebnis.ausdruck, ergebnis.ausdruck.standardCursor())
        }
        return ergebnis
    }

    fun exportiere(): String = FormelLatexCodec.exportiere(wurzel)

    fun rueckgaengig(): Boolean {
        val vorher = rueckgaengig.removeLastOrNull() ?: return false
        wiederholen.add(aktuellerMoment())
        wurzel = vorher.wurzel
        cursor = wurzel.normalisiereCursor(vorher.cursor)
        return true
    }

    fun wiederholen(): Boolean {
        val nachher = wiederholen.removeLastOrNull() ?: return false
        rueckgaengig.add(aktuellerMoment())
        wurzel = nachher.wurzel
        cursor = wurzel.normalisiereCursor(nachher.cursor)
        return true
    }

    private fun ersetzeAuswahl(neu: FormelAusdruck): Boolean {
        val ersetzt = wurzel.ersetze(cursor.ausdrucksId, neu) ?: return false
        val zielId = neu.ersterPlatzhalter()?.id ?: neu.id
        val zielPosition = (neu.ersterPlatzhalter())?.let {
            CursorPosition.InPlatzhalter(it.rollenId)
        } ?: CursorPosition.NachAusdruck
        val zielCursor = ersetzt.cursorFürAusdruck(zielId, zielPosition)
        setzeWurzel(ersetzt, zielCursor)
        return true
    }

    private fun setzeWurzel(neu: FormelAusdruck, gewünschterCursor: FormelCursor? = null) {
        if (neu == wurzel) return
        rueckgaengig.add(aktuellerMoment())
        wiederholen.clear()
        wurzel = neu
        cursor = neu.normalisiereCursor(gewünschterCursor)
    }

    private fun aktuellerMoment(): FormelEditorMoment = FormelEditorMoment(wurzel, cursor)

    private fun neuerPlatzhalter(rolle: String): FormelAusdruck.Platzhalter =
        FormelAusdruck.Platzhalter(
            id = neueId("platzhalter"),
            rollenId = rolle,
            beschriftung = rolle,
            typ = FormelTyp.ZAHL,
        )

    private fun neueId(art: String): String = "editor-$art-${++idZaehler}"
}

fun neuerFormelPlatzhalter(
    rolle: String,
    beschriftung: String = rolle,
): FormelAusdruck.Platzhalter = FormelAusdruck.Platzhalter(
    id = "platzhalter-${rolle.hashCode().toUInt()}-${System.nanoTime().toUInt()}",
    rollenId = rolle,
    beschriftung = beschriftung,
    typ = FormelTyp.ZAHL,
)

private fun FormelAusdruck.ersetze(id: String, neu: FormelAusdruck): FormelAusdruck? {
    if (this.id == id) return neu
    if (this !is FormelAusdruck.Operation) return null
    var gefunden = false
    val neueArgumente = argumente.map { argument ->
        val ersetzt = argument.ausdruck.ersetze(id, neu)
        if (ersetzt == null) argument else {
            gefunden = true
            argument.copy(ausdruck = ersetzt)
        }
    }
    return if (gefunden) copy(argumente = neueArgumente) else null
}

private fun FormelAusdruck.platzhalter(): List<FormelAusdruck.Platzhalter> = buildList {
    fun besuche(ausdruck: FormelAusdruck) {
        when (ausdruck) {
            is FormelAusdruck.Platzhalter -> add(ausdruck)
            is FormelAusdruck.Operation -> ausdruck.argumente.sortedBy { it.position }.forEach { besuche(it.ausdruck) }
            else -> Unit
        }
    }
    besuche(this@platzhalter)
}

private fun FormelAusdruck.ersterPlatzhalter(): FormelAusdruck.Platzhalter? =
    platzhalter().firstOrNull()

private fun parseEditorRationaleEingabe(text: String): RationaleZahl {
    val bereinigt = text.trim()
    if ('/' in bereinigt) return RationaleZahl.parse(bereinigt)
    if ('.' !in bereinigt) return RationaleZahl.von(BigInteger(bereinigt))
    val dezimal = BigDecimal(bereinigt)
    val skala = dezimal.scale().coerceAtLeast(0)
    return RationaleZahl.von(
        dezimal.movePointRight(skala).toBigIntegerExact(),
        BigInteger.TEN.pow(skala),
    )
}

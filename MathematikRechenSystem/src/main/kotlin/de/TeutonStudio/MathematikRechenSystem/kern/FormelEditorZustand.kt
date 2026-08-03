package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigDecimal
import java.math.BigInteger

/**
 * Transaktionaler, UI-neutraler Bearbeitungszustand für eine strukturierte Formel.
 * Auswahl und Historie beziehen sich auf stabile Ausdrucks-IDs.
 */
class FormelEditorZustand(
    start: FormelAusdruck = neuerFormelPlatzhalter("wurzel", "Ausdruck"),
) {
    private val rueckgaengig = ArrayDeque<FormelAusdruck>()
    private val wiederholen = ArrayDeque<FormelAusdruck>()
    private var idZaehler = 0

    var wurzel: FormelAusdruck = start
        private set
    var auswahlId: String = start.ersterPlatzhalter()?.id ?: start.id
        private set

    val kannRueckgaengig: Boolean get() = rueckgaengig.isNotEmpty()
    val kannWiederholen: Boolean get() = wiederholen.isNotEmpty()
    val offenePlatzhalter: List<FormelAusdruck.Platzhalter>
        get() = wurzel.platzhalter()

    fun waehle(ausdrucksId: String): Boolean {
        if (wurzel.finde(ausdrucksId) == null) return false
        auswahlId = ausdrucksId
        return true
    }

    fun naechsterPlatzhalter(richtung: Int = 1): String? {
        val ids = offenePlatzhalter.map { it.id }
        if (ids.isEmpty()) return null
        val aktuell = ids.indexOf(auswahlId)
        val index = if (aktuell < 0) 0 else Math.floorMod(aktuell + richtung, ids.size)
        auswahlId = ids[index]
        return auswahlId
    }

    fun druecke(taste: FormelTastaturTaste): Boolean {
        val ziel = wurzel.finde(auswahlId) ?: return false
        val neu = taste.literal?.let { literal ->
            FormelAusdruck.Literal(neueId("literal"), literal, FormelTyp.ZAHL)
        } ?: run {
            val rollen = taste.argumentRollen.ifEmpty { listOf("argument") }
            val argumente = rollen.mapIndexed { index, rolle ->
                val argument = if (index == 0 && ziel !is FormelAusdruck.Platzhalter) {
                    ziel
                } else {
                    neuerPlatzhalter(rolle)
                }
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

    fun importiere(latex: String): FormelLatexImportErgebnis {
        val ergebnis = FormelLatexCodec.importiere(latex)
        if (ergebnis is FormelLatexImportErgebnis.Erfolg) {
            setzeWurzel(ergebnis.ausdruck)
        }
        return ergebnis
    }

    fun exportiere(): String = FormelLatexCodec.exportiere(wurzel)

    fun rueckgaengig(): Boolean {
        val vorher = rueckgaengig.removeLastOrNull() ?: return false
        wiederholen.add(wurzel)
        wurzel = vorher
        auswahlId = wurzel.ersterPlatzhalter()?.id ?: wurzel.id
        return true
    }

    fun wiederholen(): Boolean {
        val nachher = wiederholen.removeLastOrNull() ?: return false
        rueckgaengig.add(wurzel)
        wurzel = nachher
        auswahlId = wurzel.ersterPlatzhalter()?.id ?: wurzel.id
        return true
    }

    private fun ersetzeAuswahl(neu: FormelAusdruck): Boolean {
        val ersetzt = wurzel.ersetze(auswahlId, neu) ?: return false
        setzeWurzel(ersetzt)
        auswahlId = neu.ersterPlatzhalter()?.id ?: neu.id
        return true
    }

    private fun setzeWurzel(neu: FormelAusdruck) {
        if (neu == wurzel) return
        rueckgaengig.add(wurzel)
        wiederholen.clear()
        wurzel = neu
        auswahlId = neu.ersterPlatzhalter()?.id ?: neu.id
    }

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

private fun FormelAusdruck.finde(id: String): FormelAusdruck? = when {
    id == this.id -> this
    this is FormelAusdruck.Operation -> argumente.firstNotNullOfOrNull { it.ausdruck.finde(id) }
    else -> null
}

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

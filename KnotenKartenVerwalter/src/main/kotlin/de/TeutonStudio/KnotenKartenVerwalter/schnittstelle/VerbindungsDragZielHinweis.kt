package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussVerweis
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand

/**
 * Fachneutraler Hook für temporäre Hinweise an kompatiblen Drag-Zielen.
 *
 * Der Karteneditor kennt weder Mathematik noch konkrete Knotentypen. Fachmodule
 * können hier lediglich den anzuzeigenden Text liefern. `null` bedeutet: kein
 * Hinweis. Der Provider wird nur während einer laufenden Verbindung abgefragt.
 */
object VerbindungsDragZielHinweis {
    @Volatile
    private var anbieter: ((KartenEditorZustand, AnschlussVerweis) -> String?)? = null

    fun installiere(anbieter: (KartenEditorZustand, AnschlussVerweis) -> String?) {
        this.anbieter = anbieter
    }

    fun entferne() {
        anbieter = null
    }

    internal fun textFür(zustand: KartenEditorZustand, ziel: AnschlussVerweis): String? =
        anbieter?.invoke(zustand, ziel)?.takeIf { it.isNotBlank() }
}

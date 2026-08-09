package de.TeutonStudio.MathematikAtlas

import kotlin.math.max

internal enum class KonzeptZeigerArt { Touch, Stift, Maus, Unbekannt }

internal data class KonzeptGestenSchwellen(
    val bewegungDp: Float,
) {
    companion object {
        fun für(art: KonzeptZeigerArt, touchSlopDp: Float): KonzeptGestenSchwellen {
            val touch = touchSlopDp.coerceAtLeast(0f)
            val bewegung = when (art) {
                KonzeptZeigerArt.Touch, KonzeptZeigerArt.Unbekannt -> touch
                KonzeptZeigerArt.Stift -> max(3f, touch * .75f)
                KonzeptZeigerArt.Maus -> max(2f, touch * .5f)
            }
            return KonzeptGestenSchwellen(bewegung)
        }
    }
}

internal enum class KonzeptGestenZustand { Bereit, Gedrückt, Gehalten, Ziehen, Abgebrochen }

internal sealed interface KonzeptGestenEffekt {
    data object Einfügen : KonzeptGestenEffekt
    data object DefinitionÖffnen : KonzeptGestenEffekt
    data object DragBeginnen : KonzeptGestenEffekt
    data object DragVerschieben : KonzeptGestenEffekt
    data object DragBeenden : KonzeptGestenEffekt
    data object DragAbbrechen : KonzeptGestenEffekt
}

/** Reiner, UI-unabhängiger Automat. Jede Geste liefert höchstens einen terminalen Effekt. */
internal class KonzeptGestenAutomat(private val bewegungsSchwellePx: Float) {
    var zustand: KonzeptGestenZustand = KonzeptGestenZustand.Bereit
        private set

    fun drücken(): List<KonzeptGestenEffekt> {
        if (zustand != KonzeptGestenZustand.Bereit) return emptyList()
        zustand = KonzeptGestenZustand.Gedrückt
        return emptyList()
    }

    fun haltezeitErreicht(): List<KonzeptGestenEffekt> {
        if (zustand == KonzeptGestenZustand.Gedrückt) zustand = KonzeptGestenZustand.Gehalten
        return emptyList()
    }

    fun bewegen(gesamtStreckePx: Float): List<KonzeptGestenEffekt> = when (zustand) {
        KonzeptGestenZustand.Gedrückt -> {
            if (gesamtStreckePx > bewegungsSchwellePx) zustand = KonzeptGestenZustand.Abgebrochen
            emptyList()
        }
        KonzeptGestenZustand.Gehalten -> {
            if (gesamtStreckePx > bewegungsSchwellePx) {
                zustand = KonzeptGestenZustand.Ziehen
                listOf(KonzeptGestenEffekt.DragBeginnen)
            } else emptyList()
        }
        KonzeptGestenZustand.Ziehen -> listOf(KonzeptGestenEffekt.DragVerschieben)
        else -> emptyList()
    }

    fun loslassen(): List<KonzeptGestenEffekt> {
        val effekt = when (zustand) {
            KonzeptGestenZustand.Gedrückt -> listOf(KonzeptGestenEffekt.Einfügen)
            KonzeptGestenZustand.Gehalten -> listOf(KonzeptGestenEffekt.DefinitionÖffnen)
            KonzeptGestenZustand.Ziehen -> listOf(KonzeptGestenEffekt.DragBeenden)
            else -> emptyList()
        }
        zustand = KonzeptGestenZustand.Bereit
        return effekt
    }

    fun abbrechen(): List<KonzeptGestenEffekt> {
        val effekt = if (zustand == KonzeptGestenZustand.Ziehen) {
            listOf(KonzeptGestenEffekt.DragAbbrechen)
        } else emptyList()
        zustand = KonzeptGestenZustand.Bereit
        return effekt
    }
}

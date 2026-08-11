package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.MathematischeTypen
import de.TeutonStudio.TypSystem.AnschlussVertrag
import de.TeutonStudio.TypSystem.TypAusdruck
import de.TeutonStudio.TypSystem.TypInferenzRegel

private val angulusAtom = TypAusdruck.Atom(MathematischeTypen.Angulus)
private val methodeAtom = TypAusdruck.Atom(MathematischeTypen.Methode)
private val zahlAtom = TypAusdruck.Atom(MathematischeTypen.Zahl)

/**
 * Verfeinert ausschließlich die Winkeloperatoren des bestehenden erweiterten
 * Zahlenrechners. Alle übrigen Operatoren behalten ihre bisherige Konfiguration.
 */
fun konfiguriereErweitertenZahlenRechnerMitAngulus(
    knoten: KnotenDaten,
    operator: ErweiterterZahlenOperator,
): KnotenDaten {
    val basis = konfiguriereErweitertenZahlenRechner(knoten, operator)
    return when (operator) {
        ErweiterterZahlenOperator.TANGENS,
        ErweiterterZahlenOperator.COTANGENS,
        ErweiterterZahlenOperator.SEKANS,
        ErweiterterZahlenOperator.KOSEKANS,
        -> basis.mitAngulusEingang()
        ErweiterterZahlenOperator.ARCTANGENS -> basis.mitAngulusAusgang()
        else -> basis
    }
}

/** Idempotente Normalisierung eines bereits gespeicherten Zahlenrechners. */
fun normalisiereAngulusZahlenRechner(knoten: KnotenDaten): KnotenDaten {
    if (knoten.art != ZAHLENRECHNER_ART) return knoten
    val id = knoten.parameter[ZAHLENRECHNER_OPERATOR]
    ErweiterterZahlenOperator.vonId(id)?.let { erweitert ->
        return konfiguriereErweitertenZahlenRechnerMitAngulus(knoten, erweitert)
    }
    val standard = UniversellerZahlenOperator.vonIdOderNull(id) ?: return knoten
    return if (standard in setOf(
            UniversellerZahlenOperator.KOMPLEXER_WINKEL,
            UniversellerZahlenOperator.ARCSINUS,
            UniversellerZahlenOperator.ARCCOSINUS,
            UniversellerZahlenOperator.SINUS,
            UniversellerZahlenOperator.COSINUS,
            UniversellerZahlenOperator.KOMPLEX_AUS_POLAR,
            UniversellerZahlenOperator.KOMPLEX_AUS_KARTESISCH,
        )
    ) {
        konfiguriereZahlenRechner(knoten, standard)
    } else knoten
}

private fun KnotenDaten.mitAngulusEingang(): KnotenDaten {
    val eingänge = anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }
    val ausgänge = anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }
    val a = eingänge.firstOrNull { it.name == "a" } ?: return this
    val neu = a.copy(
        art = MathematikAnschlussArten.Objekt.id,
        zulässigeArten = setOf(MathematikAnschlussArten.Angulus.id, MathematikAnschlussArten.Methode.id),
        vertrag = AnschlussVertrag(TypAusdruck.Vereinigung(listOf(angulusAtom, methodeAtom))),
    )
    return copy(anschlüsse = eingänge.map { if (it.id == a.id) neu else it } + ausgänge)
}

private fun KnotenDaten.mitAngulusAusgang(): KnotenDaten {
    val eingänge = anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }
    val ausgänge = anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }
    val ausgang = ausgänge.firstOrNull { it.name == "wert" } ?: return this
    val neu = ausgang.copy(
        art = MathematikAnschlussArten.Angulus.id,
        vertrag = AnschlussVertrag(TypAusdruck.Vereinigung(listOf(angulusAtom, methodeAtom))),
        typInferenz = TypInferenzRegel.AbbildungVonEingang(
            eingang = "a",
            abbildung = mapOf(zahlAtom to angulusAtom, methodeAtom to methodeAtom),
        ),
        artPriorisiertEingänge = AnschlussArtPriorisierung(
            eingänge = listOf("a"),
            prioritäten = listOf(MathematikAnschlussArten.Methode.id),
        ),
    )
    return copy(anschlüsse = eingänge + ausgänge.map { if (it.id == ausgang.id) neu else it })
}

package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.TypSystem.*

/**
 * Spiegelt die produktiven G0.1-Anschlussregeln verlustfrei in den semantischen
 * G0.2-Typvertrag. Die alten Felder bleiben als grobe Kategorie und Ladevertrag
 * erhalten; dadurch ist diese Migration idempotent und rückwärtskompatibel.
 */
fun AnschlussDaten.migriereSemantischenTyp(): AnschlussDaten {
    val normalisierteArt = MathematikAnschlussArten.normalisiereMethodenArt(art)
    val migrierterTyp = if (vertrag.typ != TypAusdruck.Unbekannt) {
        vertrag.typ
    } else {
        val erlaubteTypen = zulässigeArten
            .map(MathematikAnschlussArten::normalisiereMethodenArt)
            .distinct()
            .map { TypAusdruck.Atom(TypId(it.wert)) }
        when (erlaubteTypen.size) {
            0 -> TypAusdruck.Atom(TypId(normalisierteArt.wert))
            1 -> erlaubteTypen.single()
            else -> TypAusdruck.Vereinigung(erlaubteTypen)
        }
    }

    val migrierteInferenz = typInferenz ?: when {
        artAbbildungVonEingang != null -> TypInferenzRegel.AbbildungVonEingang(
            eingang = artAbbildungVonEingang.eingang,
            abbildung = artAbbildungVonEingang.abbildung.mapKeys { (von, _) ->
                TypAusdruck.Atom(TypId(MathematikAnschlussArten.normalisiereMethodenArt(von).wert))
            }.mapValues { (_, zu) ->
                TypAusdruck.Atom(TypId(MathematikAnschlussArten.normalisiereMethodenArt(zu).wert))
            },
        )
        artFolgtEingang != null -> TypInferenzRegel.FolgtEingang(artFolgtEingang)
        artPriorisiertEingänge != null -> TypInferenzRegel.Priorisierung(
            eingänge = artPriorisiertEingänge.eingänge,
            prioritäten = artPriorisiertEingänge.prioritäten.map { priorität ->
                TypAusdruck.Atom(TypId(MathematikAnschlussArten.normalisiereMethodenArt(priorität).wert))
            },
        )
        artVereinigtEingänge.isNotEmpty() -> TypInferenzRegel.GemeinsameOberart(artVereinigtEingänge)
        else -> null
    }

    return copy(
        art = normalisierteArt,
        vertrag = vertrag.copy(typ = migrierterTyp),
        typInferenz = migrierteInferenz,
    )
}

fun KnotenDaten.migriereSemantischeTypen(): KnotenDaten =
    copy(anschlüsse = anschlüsse.map(AnschlussDaten::migriereSemantischenTyp))

fun KnotenVorlage.migriereSemantischeTypen(): KnotenVorlage =
    copy(anschlüsse = anschlüsse.map(AnschlussDaten::migriereSemantischenTyp))

fun KartenDaten.migriereSemantischeTypen(): KartenDaten =
    copy(knoten = knoten.map(KnotenDaten::migriereSemantischeTypen))

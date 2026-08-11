package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

/**
 * Ergänzt die G0.2-Typverträge konservativ aus dem vorhandenen Anschlussvertrag.
 *
 * Ältere Karten und bestehende Vorlagen bleiben dadurch unverändert ladbar. Eine
 * bereits präzisere semantische Typangabe wird niemals überschrieben. Die alten
 * Artinferenzregeln werden nur gespiegelt, nicht entfernt; damit bleiben Format-7-
 * Karten und heutige dynamische Anschlüsse während der Migration funktionsfähig.
 */
fun KartenDaten.migriereSemantischeTypverträge(): KartenDaten = copy(
    knoten = knoten.map { knoten ->
        knoten.copy(
            anschlüsse = knoten.anschlüsse.map { it.mitSemantischemStandardvertrag() },
        )
    },
)

/** Neu erzeugte Knoten erhalten denselben Vertrag bereits vor dem ersten Speichern. */
fun KnotenVorlage.mitSemantischenStandardverträgen(): KnotenVorlage = copy(
    anschlüsse = anschlüsse.map { it.mitSemantischemStandardvertrag() },
)

fun AnschlussDaten.mitSemantischemStandardvertrag(): AnschlussDaten {
    val normalisierteArt = MathematikAnschlussArten.normalisiereMethodenArt(art)
    val fallbackTyp = if (zulässigeArten.isNotEmpty()) {
        TypAusdruck.Vereinigung(
            zulässigeArten
                .map(MathematikAnschlussArten::normalisiereMethodenArt)
                .distinct()
                .sortedBy(AnschlussArtId::wert)
                .map { TypAusdruck.Atom(TypId(it.wert)) },
        )
    } else {
        TypAusdruck.Atom(TypId(normalisierteArt.wert))
    }

    val inferenz = typInferenz ?: when {
        artFolgtEingang != null -> TypInferenzRegel.FolgtEingang(artFolgtEingang)
        artVereinigtEingänge.isNotEmpty() -> TypInferenzRegel.GemeinsameOberart(artVereinigtEingänge)
        artAbbildungVonEingang != null -> TypInferenzRegel.AbbildungVonEingang(
            eingang = artAbbildungVonEingang.eingang,
            fälle = artAbbildungVonEingang.abbildung.entries
                .sortedBy { it.key.wert }
                .map { (von, zu) ->
                    TypAbbildungsFall(
                        von = TypAusdruck.Atom(TypId(MathematikAnschlussArten.normalisiereMethodenArt(von).wert)),
                        zu = TypAusdruck.Atom(TypId(MathematikAnschlussArten.normalisiereMethodenArt(zu).wert)),
                    )
                },
        )
        else -> null
    }

    return copy(
        vertrag = if (vertrag.typ == TypAusdruck.Unbekannt) vertrag.copy(typ = fallbackTyp) else vertrag,
        typInferenz = inferenz,
    )
}

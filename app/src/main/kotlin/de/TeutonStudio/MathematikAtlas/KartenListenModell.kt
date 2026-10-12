package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.VisuelleKnotenGruppeDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.VisuelleGruppenId
import de.TeutonStudio.MathematikAtlas.speicher.KartenOrdnung
import de.TeutonStudio.MathematikAtlas.speicher.KartenHerkunft
import de.TeutonStudio.MathematikAtlas.speicher.formatiereOrdnerPfad

internal sealed interface KartenListenEintrag {
    val tiefe: Int
    val schlüssel: String

    data class Ordner(
        val pfad: List<String>,
        override val tiefe: Int,
    ) : KartenListenEintrag {
        override val schlüssel: String = "ordner:${formatiereOrdnerPfad(pfad)}"
    }

    data class Gruppe(val karte: KartenDaten, val gruppe: VisuelleKnotenGruppeDaten, override val tiefe: Int) : KartenListenEintrag {
        override val schlüssel = "gruppe:${karte.id.wert}:${gruppe.id.wert}"
    }

    data class Karte(
        val karte: KartenDaten,
        override val tiefe: Int,
    ) : KartenListenEintrag {
        override val schlüssel: String = "karte:${karte.id.wert}"
    }
}

internal fun kartenListenEinträge(
    karten: List<KartenDaten>,
    ordnung: KartenOrdnung,
    eingeklappteOrdner: Set<List<String>> = emptySet(),
    herkunft: Map<KartenId, KartenHerkunft> = karten.associate { it.id to KartenHerkunft.EIGEN },
    filter: KartenHerkunft = KartenHerkunft.EIGEN,
    fremdVerwalteteOrdner: Set<List<String>> = emptySet(),
    eingeklappteGruppen: Set<String> = emptySet(),
): List<KartenListenEintrag> = buildList {
    val gefilterteKarten = karten.filter { herkunft[it.id] == filter }
    val benötigteOrdner = gefilterteKarten
        .flatMap { karte ->
            val pfad = ordnung.ordnerFür(karte.id)
            (1..pfad.size).map(pfad::take)
        }
        .toSet()
    val verwaltetePräfixe = karten.filter { herkunft[it.id] != KartenHerkunft.EIGEN }
        .flatMap { karte -> ordnung.ordnerFür(karte.id).let { pfad -> (1..pfad.size).map(pfad::take) } }
        .toSet() + fremdVerwalteteOrdner
    val sichtbareOrdner = when (filter) {
        KartenHerkunft.EIGEN -> ordnung.ordner.filterNot { ordner ->
            verwaltetePräfixe.any { verwaltet -> ordner.take(verwaltet.size) == verwaltet }
        }.toSet() + benötigteOrdner + ordnung.eigeneOrdner.flatMap { pfad -> (1..pfad.size).map(pfad::take) }
        KartenHerkunft.VORDEFINIERT,
        KartenHerkunft.FREMD,
        -> benötigteOrdner
    }

    fun fügeEbeneHinzu(eltern: List<String>, tiefe: Int) {
        sichtbareOrdner.asSequence()
            .filter { it.size == eltern.size + 1 && it.take(eltern.size) == eltern }
            .sortedBy { it.last().lowercase() }
            .forEach { pfad ->
                add(KartenListenEintrag.Ordner(pfad, tiefe))
                if (pfad !in eingeklappteOrdner) {
                    fügeEbeneHinzu(pfad, tiefe + 1)
                }
            }

        gefilterteKarten.asSequence()
            .filter { ordnung.ordnerFür(it.id) == eltern }
            .sortedBy { it.name.lowercase() }
            .forEach { karte ->
                add(KartenListenEintrag.Karte(karte, tiefe))
                val kinder = karte.visuelleGruppen.groupBy { it.elternGruppenId }
                val besucht = mutableSetOf<VisuelleGruppenId>()
                fun gruppen(eltern: VisuelleGruppenId?, ebene: Int) {
                    kinder[eltern].orEmpty().sortedWith(compareBy({ it.titel.lowercase() }, { it.id.wert })).forEach { g ->
                        if (besucht.add(g.id)) {
                            val eintrag = KartenListenEintrag.Gruppe(karte, g, ebene)
                            add(eintrag)
                            if (eintrag.schlüssel !in eingeklappteGruppen) gruppen(g.id, ebene + 1)
                        }
                    }
                }
                gruppen(null, tiefe + 1)
            }
    }

    fügeEbeneHinzu(emptyList(), 0)
}

package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.MengenraumKnotenVorlagen

enum class IterierterSpezialfall {
    LeereIndexmenge,
    EndlicheIndexmenge,
}

internal fun iterierteOperatorSpezialfallKarte(
    vorlage: KnotenVorlage,
    variantenIndex: Int,
    spezialfall: IterierterSpezialfall,
): KartenDaten {
    val fallIndex = when (spezialfall) {
        IterierterSpezialfall.LeereIndexmenge -> 1
        IterierterSpezialfall.EndlicheIndexmenge -> 2
    }
    val basis = iterierteOperatorDefinitionsKarte(vorlage, variantenIndex * 10 + fallIndex)
    val indexEingang = basis.knoten.single {
        it.art == TestDefinitionsKarten.KONZEPT_EINGANG_ART && it.name == "I"
    }
    val indexKnoten = konkreterIndexKnoten(indexEingang, spezialfall)
    val suffix = when (spezialfall) {
        IterierterSpezialfall.LeereIndexmenge -> "leere-indexmenge"
        IterierterSpezialfall.EndlicheIndexmenge -> "endlicher-spezialfall"
    }
    val titel = when (spezialfall) {
        IterierterSpezialfall.LeereIndexmenge -> "Leere Indexmenge von ${vorlage.name}"
        IterierterSpezialfall.EndlicheIndexmenge -> "Endlicher Spezialfall von ${vorlage.name}"
    }

    return basis.copy(
        id = KartenId("${basis.id.wert}-$suffix"),
        name = titel,
        knoten = basis.knoten.map { if (it.id == indexEingang.id) indexKnoten else it },
    )
}

private fun konkreterIndexKnoten(
    bisher: KnotenDaten,
    spezialfall: IterierterSpezialfall,
): KnotenDaten {
    val vorlage = when (spezialfall) {
        IterierterSpezialfall.LeereIndexmenge -> MengenraumKnotenVorlagen.LeereMenge
        IterierterSpezialfall.EndlicheIndexmenge -> MathematikKnotenVorlagen.EndlicheMenge
    }
    val erzeugt = vorlage.erzeuge(bisher.position)
    val bisherigerAusgang = bisher.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }
    val anschlüsse = erzeugt.anschlüsse.mapIndexed { index, anschluss ->
        anschluss.copy(
            id = if (anschluss.richtung == AnschlussRichtung.Ausgang) {
                bisherigerAusgang.id
            } else {
                AnschlussId("${bisher.id.wert}-anschluss-$index")
            },
        )
    }
    val parameter = when (spezialfall) {
        IterierterSpezialfall.LeereIndexmenge -> vorlage.standardParameter
        IterierterSpezialfall.EndlicheIndexmenge -> vorlage.standardParameter + ("elemente" to "1,2,3")
    }

    return erzeugt.copy(
        id = bisher.id,
        position = bisher.position,
        größe = bisher.größe,
        anschlüsse = anschlüsse,
        parameter = parameter,
    )
}

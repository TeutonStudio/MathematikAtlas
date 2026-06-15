package de.TeutonStudio.MathematikAtlas.knoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AusgangDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.EingangDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.RichtungsAnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenAusgabeDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenEingabeDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenArt
import de.TeutonStudio.MathematikAtlas.anschlüsse.AussageAusgang
import de.TeutonStudio.MathematikAtlas.anschlüsse.AussageEingang
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.auswerten
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.definition
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.operator

class AussageDefinitionDaten(
    id: String,
    name: String = "Aussage",
    initialWahr: Boolean = true,
) : KnotenEingabeDaten(
    id = id,
    name = name,
) {
    override var klasse: KnotenArt? = definition.KNOTEN_ART

    override fun anschlussKorrektur(a: AusgangDaten) {
        super.anschlussKorrektur(a)
        a.apply { klasse = AussageAusgang.ANSCHLUSS_ART }
    }

    init {
        anschlussLabel[AnschlussKante.Rechts] = "Aussage" to 0

        data[definition.WERT_SCHLÜSSEL] = initialWahr
    }
}

class AussageOperatorDaten(
    id: String,
    name: String = "Verknüpfung",
): KnotenDaten<RichtungsAnschlussDaten>(
    id = id,
    name = name,
) {
    override var klasse: KnotenArt? = operator.KNOTEN_ART

    init {
        val eingangA = EingangDaten(
            id = "$id-eingang-0",
            kante = AnschlussKante.Links,
            label = "A",
        )

        val eingangB = EingangDaten(
            id = "$id-eingang-1",
            kante = AnschlussKante.Links,
            label = "B",
        )

        val ausgang = AusgangDaten(
            id = "$id-ausgang-0",
            kante = AnschlussKante.Rechts,
            label = "Ergebnis",
        )
        val anschlussListe = listOf(
            eingangA,
            eingangB,
            ausgang,
        ).apply { forEach { it.apply { if (this is EingangDaten) klasse = AussageEingang.ANSCHLUSS_ART else klasse = AussageAusgang.ANSCHLUSS_ART } } }

        anschlüsse.addAll(anschlussListe)

        anschlussIdx[eingangA.id] = 0
        anschlussIdx[eingangB.id] = 1
        anschlussIdx[ausgang.id] = 0

        data[operator.OPERATOR_SCHLÜSSEL] = operator.AussagenVerknüpfung.UND.name
    }
}

class AussageAuswertenDaten(
    id: String,
    name: String = "Auswerten",
): KnotenAusgabeDaten(
    id = id,
    name = name,
) {
    override var klasse: KnotenArt? = auswerten.KNOTEN_ART

    override fun anschlussKorrektur(a: EingangDaten) {
        super.anschlussKorrektur(a)
        a.klasse = AussageEingang.ANSCHLUSS_ART
        // TODO schlaueren Weg überlegen
    }


    init {
        anschlussLabel[AnschlussKante.Links] =
            "Aussage" to 0
    }
}
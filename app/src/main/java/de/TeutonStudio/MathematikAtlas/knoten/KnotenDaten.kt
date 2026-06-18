package de.TeutonStudio.MathematikAtlas.knoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AusgangDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.EingangDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenAusgabeDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenEingabeDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenArt
import de.TeutonStudio.MathematikAtlas.anschlüsse.AussageAusgang
import de.TeutonStudio.MathematikAtlas.anschlüsse.AussageEingang
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.auswerten
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.definition

class AussageDefinitionDaten(
    id: String,
    name: String = "Aussage",
    initialWahr: Boolean = true,
): KnotenEingabeDaten(
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
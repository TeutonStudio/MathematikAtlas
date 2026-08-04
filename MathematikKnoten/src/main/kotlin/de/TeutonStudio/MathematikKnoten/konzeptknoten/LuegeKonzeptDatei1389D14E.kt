package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object LuegeKonzeptDatei1389D14E : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.lüge|Lüge|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.lüge|Lüge"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Lüge",
            kurzbeschreibung = "Die falsche Aussage ⊥.",
            fachPfade = setOf(FachPfad.von("logik", "aussagen")),
            suchbegriffe = setOf("Aussagen: Aussagenlogik", "Die falsche Aussage ⊥.", "Lüge", "aussage", "mathematik.aussage", "mathematik.lüge"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.lüge"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

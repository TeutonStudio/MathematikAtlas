package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object CauchyKonzeptDateiF15AAD59 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.cauchy|Cauchy|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.cauchy|Cauchy"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Cauchy",
            kurzbeschreibung = "Prüft ein unnatürliches kartesisches Tupel primär über unendliche Hyperindizes.",
            fachPfade = setOf(FachPfad.von("algebra", "operationen")),
            suchbegriffe = setOf("Analysis: Nichtstandardanalysis", "Cauchy", "Prüft ein unnatürliches kartesisches Tupel primär über unendliche Hyperindizes.", "aussage", "mathematik.aussage", "mathematik.cauchy", "mathematik.objekt", "tupel"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.cauchy"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

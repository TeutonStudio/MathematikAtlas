package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object DimensionenKonzeptDatei19DF793F : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.dimensionen|Dimensionen|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.dimensionen|Dimensionen"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Dimensionen",
            kurzbeschreibung = "Gibt die geordnete Tensorform und die Stufe eines Zahl-, Tupel-, Vektor-, Matrix- oder Tensorobjekts aus.",
            fachPfade = setOf(FachPfad.von("algebra", "operationen")),
            suchbegriffe = setOf("Dimensionen", "Gibt die geordnete Tensorform und die Stufe eines Zahl-, Tupel-, Vektor-, Matrix- oder Tensorobjekts aus.", "Tensoren", "dimensionen", "mathematik.dimensionen", "mathematik.objekt", "mathematik.tupel", "mathematik.zahl", "objekt", "stufe"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.dimensionen"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

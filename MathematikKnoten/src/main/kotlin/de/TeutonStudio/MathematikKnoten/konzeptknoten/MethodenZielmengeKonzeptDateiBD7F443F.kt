package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object MethodenZielmengeKonzeptDateiBD7F443F : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.methodenZielmenge|Methoden-Zielmenge|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.methodenZielmenge|Methoden-Zielmenge"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Methoden-Zielmenge",
            kurzbeschreibung = "Gibt die deklarierte Zielmenge einer einwertigen Methode aus.",
            fachPfade = setOf(FachPfad.von("algebra", "methoden"), FachPfad.von("analysis", "funktionen"), FachPfad.von("mengenlehre", "mengen")),
            suchbegriffe = setOf("Gibt die deklarierte Zielmenge einer einwertigen Methode aus.", "Methoden", "Methoden-Zielmenge", "mathematik.menge", "mathematik.methode", "mathematik.methodenZielmenge", "menge", "methode"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.methodenZielmenge"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

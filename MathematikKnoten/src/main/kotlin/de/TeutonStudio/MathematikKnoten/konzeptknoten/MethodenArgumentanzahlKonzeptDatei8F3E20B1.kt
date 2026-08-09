package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object MethodenArgumentanzahlKonzeptDatei8F3E20B1 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.methodenArgumentanzahl|Methoden-Argumentanzahl|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.methodenArgumentanzahl|Methoden-Argumentanzahl"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Methoden-Argumentanzahl",
            kurzbeschreibung = "Gibt die Anzahl der geordneten Argumentplätze einer Methode aus; diese Stelligkeit ist kein Dimensionsbegriff.",
            fachPfade = setOf(
                FachPfad.von("algebra", "methoden"),
                FachPfad.von("analysis", "funktionen"),
            ),
            suchbegriffe = setOf(
                "Argumentanzahl",
                "Stelligkeit",
                "Anzahl Methodenargumente",
                "Methoden-Argumentanzahl",
                "mathematik.methodenArgumentanzahl",
                "mathematik.methode",
                "mathematik.zahl",
            ),
            aliase = setOf("Methoden-Stelligkeit"),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.methodenArgumentanzahl"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

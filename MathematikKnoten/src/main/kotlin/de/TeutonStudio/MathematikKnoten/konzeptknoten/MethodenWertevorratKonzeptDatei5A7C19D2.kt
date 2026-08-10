package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*

internal object MethodenWertevorratKonzeptDatei5A7C19D2 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.methodenWertevorrat|Methoden-Wertevorrat|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.methodenWertevorrat|Methoden-Wertevorrat"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Methoden-Wertevorrat",
            kurzbeschreibung = "Gibt den kanonischen Wertevorrat einer Methode als Menge geordneter Argumenttupel aus.",
            fachPfade = setOf(FachKatalog.MethodenSignatur),
            suchbegriffe = setOf(
                "Wertevorrat einer Methode",
                "Definitionsbereich einer Methode",
                "Argumentraum",
                "Methoden-Wertevorrat",
                "mathematik.methodenWertevorrat",
                "mathematik.methode",
                "mathematik.menge",
            ),
            aliase = setOf("Methoden-Definitionsbereich", "Methoden-Argumentraum"),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.methodenWertevorrat"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = listOf(
                WissensKartenReferenz.Generator(
                    id = "${id.wert}.definition",
                    generatorId = "konzeptkarte.generisch",
                    rolle = WissensKartenRolle.Definition,
                    primär = true,
                ),
            ),
        )
    }
}

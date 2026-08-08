package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*

internal object MethodenGraphKonzeptDatei7A18F2C1 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.methodenGraph|Graph|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.methodenGraph|Graph"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        val asset = WissensKartenReferenz.Asset(
            id = "mathematik.methodenGraph|Graph|.definition",
            datei = "karte-methodengraph-definition-v7.json",
            formatVersion = 7,
            rolle = WissensKartenRolle.Definition,
            primär = true,
            titel = "Definition",
            varianten = varianten,
        )
        return WissensEintrag(
            id = id,
            titel = "Graph",
            kurzbeschreibung = "Erzeugt für f: W→Z die Graphmenge Graph(f) = {(x,f(x)) | x∈W} ⊆ W×Z.",
            fachPfade = setOf(FachKatalog.AnalysisFunktionen, FachKatalog.MengenlehreKonstruktionen),
            suchbegriffe = setOf(
                "Graph",
                "Funktionsgraph",
                "Graph einer Funktion",
                "Graph einer Methode",
                "Γ_f",
                "mathematik.methodenGraph",
                "W×Z",
            ),
            aliase = setOf("Funktionsgraph"),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.methodenGraph"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = if (passendeVorlagen.isEmpty()) emptyList() else listOf(asset),
        )
    }
}

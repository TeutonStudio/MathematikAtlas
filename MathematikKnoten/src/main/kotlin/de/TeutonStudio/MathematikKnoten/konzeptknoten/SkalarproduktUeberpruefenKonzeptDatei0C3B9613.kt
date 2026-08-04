package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object SkalarproduktUeberpruefenKonzeptDatei0C3B9613 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.begriff.skalarprodukt|Skalarprodukt überprüfen|skalarprodukt.linearitaet=RECHTSLINEAR;skalarprodukt.nachweis.linearitaet=;skalarprodukt.nachweis.positiv=;skalarprodukt.nachweis.symmetrie=;skalarprodukt.zahlbereich=R;skalarprodukt.zertifikatVersion=1")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.begriff.skalarprodukt|skalarprodukt.linearitaet=RECHTSLINEAR|skalarprodukt.nachweis.linearitaet=|skalarprodukt.nachweis.positiv=|skalarprodukt.nachweis.symmetrie=|skalarprodukt.zahlbereich=R|skalarprodukt.zertifikatVersion=1|Skalarprodukt überprüfen"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Skalarprodukt überprüfen",
            kurzbeschreibung = "Zertifiziert eine zweistellige Methode als Skalarprodukt. Fehlende Nachweisreferenzen bleiben ausdrücklich unvollständig.",
            fachPfade = setOf(FachPfad.von("geometrie", "grundobjekte"), FachPfad.von("lineare-algebra", "skalarprodukte")),
            suchbegriffe = setOf("1", "Lineare Algebra: Begriffe", "R", "RECHTSLINEAR", "Skalarprodukt überprüfen", "Zertifiziert eine zweistellige Methode als Skalarprodukt. Fehlende Nachweisreferenzen bleiben ausdrücklich unvollständig.", "aussage", "mathematik.aussage", "mathematik.begriff.skalarprodukt", "mathematik.methode", "methode", "skalarprodukt.linearitaet", "skalarprodukt.nachweis.linearitaet", "skalarprodukt.nachweis.positiv", "skalarprodukt.nachweis.symmetrie", "skalarprodukt.zahlbereich", "skalarprodukt.zertifikatVersion"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.begriff.skalarprodukt"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

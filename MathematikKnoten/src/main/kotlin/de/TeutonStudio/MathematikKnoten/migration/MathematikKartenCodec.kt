package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDatenJson
import org.json.JSONObject

/**
 * Kanonische Serialisierungs- und Migrationspipeline für mathematische Karten.
 *
 * [KartenDatenJson] bleibt der fachneutrale Graph-Codec. Diese Fassade ergänzt
 * ausschließlich mathematische Migrationen und kann deshalb von Android und
 * Desktop identisch verwendet werden.
 */
object MathematikKartenCodec {
    /** Normalisiert genau die Migrationen, die bereits vor dem Refactor beim Schreiben galten. */
    fun schreibe(karte: KartenDaten): String = KartenDatenJson.schreibe(
        MathematikKartenMigrationen.vorSpeichern(karte),
    )

    /** Dekodiert JSON und wendet die schema-/knotennahen Migrationen an. */
    fun lese(text: String): KartenDaten = nachDekodierung(KartenDatenJson.lese(text))

    /** Variante für bereits geparstes JSON, etwa den App-JSON-Editor. */
    fun lese(json: JSONObject): KartenDaten = nachDekodierung(KartenDatenJson.lese(json))

    /**
     * Vollständiger Ladepfad für persistierte Karten.
     *
     * Er enthält zusätzlich die historischen Methoden- und Rechnerkonsolidierungen,
     * die Android bislang erst im Dateispeicher nach dem JSON-Codec angewendet hat.
     */
    fun lade(text: String): KartenDaten = MathematikKartenMigrationen.nachLaden(lese(text))

    /** Import verwendet aktuell denselben mathematischen Normalisierungsvertrag wie Laden. */
    fun importiere(text: String): KartenDaten = MathematikKartenMigrationen.nachLaden(lese(text))

    private fun nachDekodierung(karte: KartenDaten): KartenDaten =
        MathematikKartenMigrationen.nachDekodierung(karte)
}

object MathematikKartenMigrationen {
    fun vorSpeichern(karte: KartenDaten): KartenDaten = karte
        .normalisiereStrukturierteDivisionVorSpeichern()
        .migriereTensorOperationKnoten()
        .migriereHyperAnalysisKnoten()
        .migriereDifferentialKnoten()
        .migriereIntegralKnoten()

    fun nachDekodierung(karte: KartenDaten): KartenDaten = karte
        .let(::migriereTranspositionsKnoten)
        .migriereTensorOperationKnoten()
        .migriereHyperAnalysisKnoten()
        .migriereDifferentialKnoten()
        .migriereIntegralKnoten()

    fun nachLaden(karte: KartenDaten): KartenDaten = karte
        .migriereTensorraumDimensionen()
        .migrierePraedikatStandardname()
        .migriereMethodenAnschlüsse()
        .migriereUniversellenZahlenRechner()
        .migriereStrukturierteDivision()
}

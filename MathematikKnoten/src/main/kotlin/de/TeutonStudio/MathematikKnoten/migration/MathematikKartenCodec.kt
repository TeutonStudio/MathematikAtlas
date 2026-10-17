package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDatenJson
import org.json.JSONObject
import de.TeutonStudio.MathematikKartenAdapter.normalisiereSchnittstellenReihenfolge
import de.TeutonStudio.MathematikKartenAdapter.METHODEN_QUELLE_KNOTEN
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenEigenschaft

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
    fun vorSpeichern(karte: KartenDaten): KartenDaten = vorSpeichern(karte, 0)

    private fun vorSpeichern(karte: KartenDaten, tiefe: Int): KartenDaten = karte
        .diagnostiziereQuellenBereiche()
        .diagnostiziereHistorischeGraphMehrdeutigkeiten()
        .migriereKonsolidierteKnoten()
        .let {it.normalisiereSchnittstellenReihenfolge(it.knoten.filter {k -> RECHNER_MIGRATION_DIAGNOSE in k.parameter}.mapTo(hashSetOf()) {k -> k.id})}
        .migriereGradmengenDimension()
        .migriereVariablenGrundraum()
        .migrierePolarKonventionen()
        .migriereWinkelraumAufrufe()
        .migriereMatrixZerlegenZumÜbersetzer()
        .migriereÜbersetzungsKonsolidierung()
        .migriereMengenKonsolidierung()
        .migriereRechnerEinzeloperationen()
        .let(::migriereTranspositionsKnoten)
        .migriereUniversellenZahlenRechner()
        .migriereMengenOperatorKnoten()
        .migriereVektorRechnerKonfiguration()
        .klassifiziereAdapter()
        .migriereMethodenKonsolidierung()
        .migriereStrukturKonsolidierung()
        .migrierePraedikatKnoten()
        .migriereMatrixProduktAnschlüsse()
        .migriereMethodenBereichsOperatoren()
        .normalisiereStrukturierteDivisionVorSpeichern()
        .migriereTensorOperationKnoten()
        .migriereHyperAnalysisKnoten()
        .migriereDifferentialKnoten()
        .migriereIntegralKnoten()
        .migriereRechnerMethodenAnschluesse()
        .normalisiereDynamischeRechnerOperanden()
        .normalisiereJoinEingänge()
        .migriereEingebetteteMethodenQuellen(tiefe) { vorSpeichern(it, tiefe + 1) }
        .bewahreDiagnostizierteOriginale(karte)

    fun nachDekodierung(karte: KartenDaten): KartenDaten = nachDekodierung(karte, 0)

    private fun nachDekodierung(karte: KartenDaten, tiefe: Int): KartenDaten = karte
        .diagnostiziereQuellenBereiche()
        .diagnostiziereHistorischeGraphMehrdeutigkeiten()
        .migriereKonsolidierteKnoten()
        .migriereHistorischeVariablenGrundräume()
        .let {it.normalisiereSchnittstellenReihenfolge(it.knoten.filter {k -> RECHNER_MIGRATION_DIAGNOSE in k.parameter}.mapTo(hashSetOf()) {k -> k.id})}
        .migriereGradmengenDimension()
        .migriereVariablenGrundraum()
        .migrierePolarKonventionen()
        .migriereWinkelraumAufrufe()
        .migriereMatrixZerlegenZumÜbersetzer()
        .migriereÜbersetzungsKonsolidierung()
        .migriereMengenKonsolidierung()
        .migriereRechnerEinzeloperationen()
        .let(::migriereTranspositionsKnoten)
        .migriereUniversellenZahlenRechner()
        .migriereMengenOperatorKnoten()
        .migriereVektorRechnerKonfiguration()
        .klassifiziereAdapter()
        .migriereMethodenKonsolidierung()
        .migriereStrukturKonsolidierung()
        .migrierePraedikatKnoten()
        .migriereMatrixProduktAnschlüsse()
        .migriereMethodenBereichsOperatoren()
        .migriereTensorOperationKnoten()
        .migriereHyperAnalysisKnoten()
        .migriereDifferentialKnoten()
        .migriereIntegralKnoten()
        .migriereRechnerMethodenAnschluesse()
        .normalisiereDynamischeRechnerOperanden()
        .normalisiereJoinEingänge()
        .migriereEingebetteteMethodenQuellen(tiefe) { nachDekodierung(it, tiefe + 1) }
        .bewahreDiagnostizierteOriginale(karte)

    fun nachLaden(karte: KartenDaten): KartenDaten = nachLaden(karte, 0)

    private fun nachLaden(karte: KartenDaten, tiefe: Int): KartenDaten = karte
        .diagnostiziereQuellenBereiche()
        .diagnostiziereHistorischeGraphMehrdeutigkeiten()
        .migriereKonsolidierteKnoten()
        .migriereHistorischeVariablenGrundräume()
        .let {it.normalisiereSchnittstellenReihenfolge(it.knoten.filter {k -> RECHNER_MIGRATION_DIAGNOSE in k.parameter}.mapTo(hashSetOf()) {k -> k.id})}
        .migriereGradmengenDimension()
        .migriereVariablenGrundraum()
        .migrierePolarKonventionen()
        .migriereWinkelraumAufrufe()
        .migriereMatrixZerlegenZumÜbersetzer()
        .migriereÜbersetzungsKonsolidierung()
        .migriereMengenKonsolidierung()
        .migriereRechnerEinzeloperationen()
        .let(::migriereTranspositionsKnoten)
        .migriereUniversellenZahlenRechner()
        .migriereMengenOperatorKnoten()
        .migriereVektorRechnerKonfiguration()
        .klassifiziereAdapter()
        .migriereMethodenKonsolidierung()
        .migriereStrukturKonsolidierung()
        .migrierePraedikatKnoten()
        .migriereTensorraumDimensionen()
        .migrierePraedikatStandardname()
        .migriereMethodenAnschlüsse()
        .migrierePraedikatKnoten()
        .migriereStrukturierteDivision()
        .migriereRechnerMethodenAnschluesse()
        .normalisiereDynamischeRechnerOperanden()
        .normalisiereJoinEingänge()
        .migriereEingebetteteMethodenQuellen(tiefe) { nachLaden(it, tiefe + 1) }
        .bewahreDiagnostizierteOriginale(karte)

    private fun KartenDaten.migriereEingebetteteMethodenQuellen(tiefe: Int, migration: (KartenDaten) -> KartenDaten): KartenDaten = copy(knoten = knoten.map { k ->
        if (RECHNER_MIGRATION_DIAGNOSE in k.parameter) return@map k
        val quelle = k.eigenschaften[METHODEN_QUELLE_KNOTEN] ?: return@map k
        runCatching {
            require(tiefe < 32) { "Eingebettete Methodenquellen sind zu tief verschachtelt." }
            val text = (quelle as? KnotenEigenschaft.Text)?.wert ?: error("Die eingebettete Knotenquelle ist kein Kartentext.")
            val alt = KartenDatenJson.lese(text)
            val neu = migration(alt)
            if (neu == alt) k else k.copy(eigenschaften = k.eigenschaften + (METHODEN_QUELLE_KNOTEN to KnotenEigenschaft.Text(KartenDatenJson.schreibe(neu))))
        }.getOrElse { fehler -> k.copy(parameter = k.parameter + (RECHNER_MIGRATION_DIAGNOSE to "Eingebettete Methodenquelle bleibt erhalten: ${fehler.message}")) }
    })

}

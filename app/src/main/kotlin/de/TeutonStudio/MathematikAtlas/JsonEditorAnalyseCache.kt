package de.TeutonStudio.MathematikAtlas

/**
 * Textrevisionsgebundener Analyse-Cache des JSON-Editors.
 *
 * Cursor- und Auswahländerungen erzeugen keinen Auftrag. Ergebnisse älterer
 * Aufträge werden verworfen, selbst wenn ein Parser seine Coroutine nicht
 * rechtzeitig kooperativ abbrechen konnte.
 */
internal class JsonEditorAnalyseCache<T>(
    private val analysiereText: (String) -> T,
) {
    internal data class Auftrag(
        val revision: Long,
        val text: String,
    )

    internal data class Ergebnis<T>(
        val revision: Long,
        val text: String,
        val analyse: T,
    )

    private var nächsteRevision = 0L
    private var aktuelleRevision = 0L
    private var cacheText: String? = null
    private var cacheAnalyse: T? = null

    fun sofort(text: String): T {
        val vorhanden = cacheAnalyse
        if (cacheText == text && vorhanden != null) return vorhanden
        return analysiereText(text).also {
            cacheText = text
            cacheAnalyse = it
        }
    }

    fun beauftrage(text: String): Auftrag {
        val revision = ++nächsteRevision
        aktuelleRevision = revision
        return Auftrag(revision, text)
    }

    fun analysiere(auftrag: Auftrag): Ergebnis<T> {
        val vorhanden = cacheAnalyse
        val analyse = if (cacheText == auftrag.text && vorhanden != null) vorhanden else analysiereText(auftrag.text)
        return Ergebnis(auftrag.revision, auftrag.text, analyse)
    }

    /** Gibt nur das Ergebnis der aktuellsten Textrevision frei. */
    fun übernehme(ergebnis: Ergebnis<T>): T? {
        if (ergebnis.revision != aktuelleRevision) return null
        cacheText = ergebnis.text
        cacheAnalyse = ergebnis.analyse
        return ergebnis.analyse
    }

    fun istAktuell(ergebnis: Ergebnis<T>): Boolean = ergebnis.revision == aktuelleRevision
}

package de.TeutonStudio.TypSystem

sealed interface TypPrüfung {
    data object Kompatibel : TypPrüfung
    data class Unbestimmt(val grund: String) : TypPrüfung
    data class Inkompatibel(val grund: String) : TypPrüfung
}

interface TypSystem {
    fun prüfe(quelle: TypAusdruck, ziel: TypAusdruck): TypPrüfung
    fun gemeinsameOberart(typen: List<TypAusdruck>): TypAusdruck?
    fun normalisiere(typ: TypAusdruck): TypAusdruck
    fun prüfeAnforderungen(quelle: TypAusdruck, anforderungen: List<TypAnforderung>): TypPrüfung
}

/**
 * Allgemeiner, domänenneutraler Typprüfer.
 *
 * Nominale Atom-Untertypen werden von der aufrufenden Domäne geliefert. Parametrisierte
 * Typen verwenden registrierte Varianzregeln; unbekannte Konstruktoren sind invariant.
 */
class StandardTypSystem(
    private val istAtomUntertyp: (TypId, TypId) -> Boolean = { von, erwartet -> von == erwartet },
    konstruktoren: Iterable<TypKonstruktorDefinition> = emptyList(),
    private val anforderungsPrüfer: (TypAusdruck, TypAnforderung) -> TypPrüfung = { _, anforderung ->
        TypPrüfung.Unbestimmt("Für '${anforderung.id}' ist noch kein Anforderungsprüfer registriert.")
    },
) : TypSystem {
    private val konstruktoren = konstruktoren.associateBy(TypKonstruktorDefinition::id)

    override fun prüfe(quelle: TypAusdruck, ziel: TypAusdruck): TypPrüfung {
        val normalisierteQuelle = normalisiere(quelle)
        val normalisiertesZiel = normalisiere(ziel)
        return prüfeNormalisiert(normalisierteQuelle, normalisiertesZiel)
    }

    private fun prüfeNormalisiert(quelle: TypAusdruck, ziel: TypAusdruck): TypPrüfung = when {
        ziel == TypAusdruck.Beliebig -> TypPrüfung.Kompatibel
        quelle == ziel -> TypPrüfung.Kompatibel
        quelle == TypAusdruck.Unbekannt || ziel == TypAusdruck.Unbekannt ->
            TypPrüfung.Unbestimmt("Mindestens einer der beteiligten Typen ist noch unbekannt.")
        quelle == TypAusdruck.Beliebig ->
            TypPrüfung.Inkompatibel("Ein beliebiger Quelltyp ist für das konkrete Ziel nicht garantiert kompatibel.")
        quelle is TypAusdruck.Variable || ziel is TypAusdruck.Variable ->
            TypPrüfung.Unbestimmt("Typvariablen benötigen eine spätere Unifikation.")
        quelle is TypAusdruck.Vereinigung -> prüfeQuellVereinigung(quelle, ziel)
        ziel is TypAusdruck.Vereinigung -> prüfeZielVereinigung(quelle, ziel)
        quelle is TypAusdruck.Atom && ziel is TypAusdruck.Atom ->
            if (istAtomUntertyp(quelle.id, ziel.id)) TypPrüfung.Kompatibel
            else TypPrüfung.Inkompatibel("${quelle.id} ist kein Untertyp von ${ziel.id}.")
        quelle is TypAusdruck.Parameterisiert && ziel is TypAusdruck.Atom ->
            if (istAtomUntertyp(quelle.konstruktor, ziel.id)) TypPrüfung.Kompatibel
            else TypPrüfung.Inkompatibel("${quelle.konstruktor}<…> ist kein Untertyp von ${ziel.id}.")
        quelle is TypAusdruck.Parameterisiert && ziel is TypAusdruck.Parameterisiert ->
            prüfeParameterisiert(quelle, ziel)
        quelle is TypAusdruck.Literal && ziel is TypAusdruck.Literal ->
            TypPrüfung.Inkompatibel("Die Typliterale '${quelle.wert}' und '${ziel.wert}' unterscheiden sich.")
        else -> TypPrüfung.Inkompatibel("$quelle kann nicht an $ziel angeschlossen werden.")
    }

    private fun prüfeQuellVereinigung(quelle: TypAusdruck.Vereinigung, ziel: TypAusdruck): TypPrüfung {
        val prüfungen = quelle.alternativen.map { prüfeNormalisiert(it, ziel) }
        val inkompatibel = prüfungen.filterIsInstance<TypPrüfung.Inkompatibel>().firstOrNull()
        if (inkompatibel != null) return inkompatibel
        val unbestimmt = prüfungen.filterIsInstance<TypPrüfung.Unbestimmt>().firstOrNull()
        return unbestimmt ?: TypPrüfung.Kompatibel
    }

    private fun prüfeZielVereinigung(quelle: TypAusdruck, ziel: TypAusdruck.Vereinigung): TypPrüfung {
        val prüfungen = ziel.alternativen.map { prüfeNormalisiert(quelle, it) }
        if (prüfungen.any { it is TypPrüfung.Kompatibel }) return TypPrüfung.Kompatibel
        return prüfungen.filterIsInstance<TypPrüfung.Unbestimmt>().firstOrNull()
            ?: prüfungen.filterIsInstance<TypPrüfung.Inkompatibel>().firstOrNull()
            ?: TypPrüfung.Inkompatibel("Keine Alternative des Zieltyps ist kompatibel.")
    }

    private fun prüfeParameterisiert(
        quelle: TypAusdruck.Parameterisiert,
        ziel: TypAusdruck.Parameterisiert,
    ): TypPrüfung {
        if (quelle.konstruktor != ziel.konstruktor) {
            return TypPrüfung.Inkompatibel(
                "Die Typkonstruktoren ${quelle.konstruktor} und ${ziel.konstruktor} unterscheiden sich.",
            )
        }
        if (quelle.argumente.size != ziel.argumente.size) {
            return TypPrüfung.Inkompatibel("Die Anzahl der Typparameter unterscheidet sich.")
        }
        val definition = konstruktoren[quelle.konstruktor]
        quelle.argumente.indices.forEach { index ->
            val q = quelle.argumente[index]
            val z = ziel.argumente[index]
            val varianz = definition?.varianzen?.getOrNull(index)
                ?: definition?.standardVarianz
                ?: TypVarianz.Invariant
            val ergebnis = when (varianz) {
                TypVarianz.Kovariant -> prüfeNormalisiert(normalisiere(q), normalisiere(z))
                TypVarianz.Kontravariant -> prüfeNormalisiert(normalisiere(z), normalisiere(q))
                TypVarianz.Invariant -> if (normalisiere(q) == normalisiere(z)) TypPrüfung.Kompatibel
                    else TypPrüfung.Inkompatibel("Der invariante Typparameter $index unterscheidet sich.")
            }
            if (ergebnis !is TypPrüfung.Kompatibel) return ergebnis
        }
        return TypPrüfung.Kompatibel
    }

    override fun gemeinsameOberart(typen: List<TypAusdruck>): TypAusdruck? {
        if (typen.isEmpty()) return null
        return normalisiere(TypAusdruck.Vereinigung(typen))
    }

    override fun normalisiere(typ: TypAusdruck): TypAusdruck = when (typ) {
        TypAusdruck.Beliebig, TypAusdruck.Unbekannt,
        is TypAusdruck.Atom, is TypAusdruck.Variable, is TypAusdruck.Literal -> typ
        is TypAusdruck.Parameterisiert -> typ.copy(argumente = typ.argumente.map(::normalisiere))
        is TypAusdruck.Vereinigung -> normalisiereVereinigung(typ)
    }

    private fun normalisiereVereinigung(vereinigung: TypAusdruck.Vereinigung): TypAusdruck {
        val flach = vereinigung.alternativen.flatMap { alternative ->
            when (val normalisiert = normalisiere(alternative)) {
                is TypAusdruck.Vereinigung -> normalisiert.alternativen
                else -> listOf(normalisiert)
            }
        }.distinct()
        if (flach.any { it == TypAusdruck.Beliebig }) return TypAusdruck.Beliebig
        val ohneÜberdeckte = flach.filter { kandidat ->
            flach.none { anderer ->
                kandidat != anderer && prüfeNormalisiert(kandidat, anderer) is TypPrüfung.Kompatibel
            }
        }
        return when (ohneÜberdeckte.size) {
            0 -> TypAusdruck.Unbekannt
            1 -> ohneÜberdeckte.single()
            else -> TypAusdruck.Vereinigung(ohneÜberdeckte)
        }
    }

    override fun prüfeAnforderungen(
        quelle: TypAusdruck,
        anforderungen: List<TypAnforderung>,
    ): TypPrüfung {
        if (anforderungen.isEmpty()) return TypPrüfung.Kompatibel
        var unbestimmt: TypPrüfung.Unbestimmt? = null
        for (anforderung in anforderungen) {
            when (val ergebnis = anforderungsPrüfer(quelle, anforderung)) {
                TypPrüfung.Kompatibel -> Unit
                is TypPrüfung.Inkompatibel -> return ergebnis
                is TypPrüfung.Unbestimmt -> if (unbestimmt == null) unbestimmt = ergebnis
            }
        }
        return unbestimmt ?: TypPrüfung.Kompatibel
    }
}

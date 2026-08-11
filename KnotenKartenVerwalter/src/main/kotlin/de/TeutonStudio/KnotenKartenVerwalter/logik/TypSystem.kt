package de.TeutonStudio.KnotenKartenVerwalter.logik

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

sealed interface TypPrüfung {
    data object Kompatibel : TypPrüfung
    data object Unbestimmt : TypPrüfung
    data class Inkompatibel(val grund: String) : TypPrüfung
}

enum class TypVarianz { Invariant, Kovariant, Kontravariant }

data class AtomTypDefinition(
    val id: TypId,
    val elternTyp: TypId? = null,
)

data class TypKonstruktorDefinition(
    val id: TypId,
    val varianzen: List<TypVarianz> = emptyList(),
)

/** Fachneutraler Dienst für semantische Typkompatibilität und Normalisierung. */
interface TypSystem {
    fun prüfe(quelle: TypAusdruck, ziel: TypAusdruck): TypPrüfung
    fun normalisiere(typ: TypAusdruck): TypAusdruck
    fun gemeinsameOberart(typen: List<TypAusdruck>): TypAusdruck?

    /** Migrations-/Fallbackbrücke von einer groben Anschlusskategorie zum semantischen Typ. */
    fun typFürAnschlussArt(art: AnschlussArtId): TypAusdruck = TypAusdruck.Unbekannt
}

open class StandardTypSystem(
    atomTypen: Iterable<AtomTypDefinition> = emptyList(),
    konstruktoren: Iterable<TypKonstruktorDefinition> = emptyList(),
    private val anschlussArtTypen: Map<AnschlussArtId, TypAusdruck> = emptyMap(),
) : TypSystem {
    private val atomNachId = atomTypen.associateBy { it.id }
    private val konstruktorNachId = konstruktoren.associateBy { it.id }

    override fun typFürAnschlussArt(art: AnschlussArtId): TypAusdruck =
        anschlussArtTypen[art] ?: TypAusdruck.Unbekannt

    override fun prüfe(quelle: TypAusdruck, ziel: TypAusdruck): TypPrüfung {
        val q = normalisiere(quelle)
        val z = normalisiere(ziel)
        if (z == TypAusdruck.Beliebig) return TypPrüfung.Kompatibel
        if (q == TypAusdruck.Unbekannt || z == TypAusdruck.Unbekannt) return TypPrüfung.Unbestimmt
        if (q == TypAusdruck.Beliebig) return if (z == TypAusdruck.Beliebig) {
            TypPrüfung.Kompatibel
        } else {
            TypPrüfung.Unbestimmt
        }
        if (q == z) return TypPrüfung.Kompatibel

        if (q is TypAusdruck.Vereinigung) {
            val ergebnisse = q.alternativen.map { prüfe(it, z) }
            return when {
                ergebnisse.any { it is TypPrüfung.Inkompatibel } -> TypPrüfung.Inkompatibel("Nicht jede Quellalternative ist mit $z kompatibel.")
                ergebnisse.all { it == TypPrüfung.Kompatibel } -> TypPrüfung.Kompatibel
                else -> TypPrüfung.Unbestimmt
            }
        }
        if (z is TypAusdruck.Vereinigung) {
            val ergebnisse = z.alternativen.map { prüfe(q, it) }
            return when {
                ergebnisse.any { it == TypPrüfung.Kompatibel } -> TypPrüfung.Kompatibel
                ergebnisse.all { it is TypPrüfung.Inkompatibel } -> TypPrüfung.Inkompatibel("$q passt zu keiner Zielalternative.")
                else -> TypPrüfung.Unbestimmt
            }
        }

        if (q is TypAusdruck.Atom && z is TypAusdruck.Atom) {
            return if (istAtomUntertyp(q.id, z.id)) TypPrüfung.Kompatibel
            else TypPrüfung.Inkompatibel("${q.id} ist kein Untertyp von ${z.id}.")
        }

        if (q is TypAusdruck.Parameterisiert && z is TypAusdruck.Parameterisiert) {
            if (q.konstruktor != z.konstruktor || q.argumente.size != z.argumente.size) {
                return TypPrüfung.Inkompatibel("Die Typkonstruktoren sind verschieden.")
            }
            val definition = konstruktorNachId[q.konstruktor]
            val varianzen = definition?.varianzen.orEmpty()
            val ergebnisse = q.argumente.indices.map { index ->
                when (varianzen.getOrElse(index) { TypVarianz.Invariant }) {
                    TypVarianz.Kovariant -> prüfe(q.argumente[index], z.argumente[index])
                    TypVarianz.Kontravariant -> prüfe(z.argumente[index], q.argumente[index])
                    TypVarianz.Invariant -> {
                        val hin = prüfe(q.argumente[index], z.argumente[index])
                        val zurück = prüfe(z.argumente[index], q.argumente[index])
                        if (hin == TypPrüfung.Kompatibel && zurück == TypPrüfung.Kompatibel) TypPrüfung.Kompatibel
                        else if (hin is TypPrüfung.Inkompatibel || zurück is TypPrüfung.Inkompatibel) TypPrüfung.Inkompatibel("Invariantes Typargument $index unterscheidet sich.")
                        else TypPrüfung.Unbestimmt
                    }
                }
            }
            return when {
                ergebnisse.any { it is TypPrüfung.Inkompatibel } -> TypPrüfung.Inkompatibel("Parametrisierte Typen sind nicht kompatibel.")
                ergebnisse.all { it == TypPrüfung.Kompatibel } -> TypPrüfung.Kompatibel
                else -> TypPrüfung.Unbestimmt
            }
        }

        return TypPrüfung.Inkompatibel("$q kann nicht an $z angeschlossen werden.")
    }

    override fun normalisiere(typ: TypAusdruck): TypAusdruck = when (typ) {
        is TypAusdruck.Parameterisiert -> typ.copy(argumente = typ.argumente.map(::normalisiere))
        is TypAusdruck.Vereinigung -> normalisiereVereinigung(typ.alternativen.map(::normalisiere))
        else -> typ
    }

    private fun normalisiereVereinigung(alternativen: List<TypAusdruck>): TypAusdruck {
        val flach = alternativen.flatMap { if (it is TypAusdruck.Vereinigung) it.alternativen else listOf(it) }
            .distinct()
        if (flach.any { it == TypAusdruck.Beliebig }) return TypAusdruck.Beliebig
        val ohneRedundanz = flach.filterIndexed { index, kandidat ->
            flach.withIndex().none { (andererIndex, anderer) ->
                index != andererIndex && prüfe(kandidat, anderer) == TypPrüfung.Kompatibel
            }
        }
        return when (ohneRedundanz.size) {
            0 -> TypAusdruck.Unbekannt
            1 -> ohneRedundanz.single()
            else -> TypAusdruck.Vereinigung(ohneRedundanz.sortedBy(::sortierSchlüssel))
        }
    }

    override fun gemeinsameOberart(typen: List<TypAusdruck>): TypAusdruck? {
        val normalisiert = typen.map(::normalisiere).filterNot { it == TypAusdruck.Unbekannt }.distinct()
        if (normalisiert.isEmpty()) return null
        if (normalisiert.size == 1) return normalisiert.single()
        if (normalisiert.all { it is TypAusdruck.Atom }) {
            val ids = normalisiert.map { (it as TypAusdruck.Atom).id }
            return gemeinsameAtomOberart(ids)?.let(TypAusdruck::Atom)
        }
        val erste = normalisiert.first() as? TypAusdruck.Parameterisiert ?: return normalisiere(TypAusdruck.Vereinigung(normalisiert))
        val parameterisierte = normalisiert.mapNotNull { it as? TypAusdruck.Parameterisiert }
        if (parameterisierte.size == normalisiert.size && parameterisierte.all {
                it.konstruktor == erste.konstruktor && it.argumente.size == erste.argumente.size
            }) {
            val definition = konstruktorNachId[erste.konstruktor]
            val argumente = erste.argumente.indices.map { index ->
                if (definition?.varianzen?.getOrNull(index) == TypVarianz.Kovariant) {
                    gemeinsameOberart(parameterisierte.map { it.argumente[index] }) ?: return@map erste.argumente[index]
                } else {
                    val werte = parameterisierte.map { it.argumente[index] }.distinct()
                    if (werte.size == 1) werte.single() else return normalisiere(TypAusdruck.Vereinigung(normalisiert))
                }
            }
            return TypAusdruck.Parameterisiert(erste.konstruktor, argumente)
        }
        return normalisiere(TypAusdruck.Vereinigung(normalisiert))
    }

    private fun istAtomUntertyp(von: TypId, erwartet: TypId): Boolean {
        var aktuell: TypId? = von
        val besucht = mutableSetOf<TypId>()
        while (aktuell != null && besucht.add(aktuell)) {
            if (aktuell == erwartet) return true
            aktuell = atomNachId[aktuell]?.elternTyp
        }
        return false
    }

    private fun gemeinsameAtomOberart(ids: List<TypId>): TypId? {
        var aktuell: TypId? = ids.firstOrNull() ?: return null
        val besucht = mutableSetOf<TypId>()
        while (aktuell != null && besucht.add(aktuell)) {
            val kandidat = aktuell
            if (ids.all { istAtomUntertyp(it, kandidat) }) return kandidat
            aktuell = atomNachId[kandidat]?.elternTyp
        }
        return null
    }

    private fun sortierSchlüssel(typ: TypAusdruck): String = when (typ) {
        TypAusdruck.Beliebig -> "0:*"
        TypAusdruck.Unbekannt -> "9:?"
        is TypAusdruck.Atom -> "1:${typ.id.wert}"
        is TypAusdruck.Parameterisiert -> "2:${typ.konstruktor.wert}:${typ.argumente.joinToString { sortierSchlüssel(it) }}"
        is TypAusdruck.Vereinigung -> "3:${typ.alternativen.joinToString { sortierSchlüssel(it) }}"
        is TypAusdruck.Variable -> "4:${typ.id.wert}"
    }
}

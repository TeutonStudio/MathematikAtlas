package de.TeutonStudio.KnotenKartenVerwalter.logik

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

sealed interface TypPrüfung {
    data object Kompatibel : TypPrüfung
    data object Unbestimmt : TypPrüfung
    data class Inkompatibel(val grund: String) : TypPrüfung
}

enum class TypVarianz {
    Kovariant,
    Kontravariant,
    Invariant,
}

data class TypKonstruktorBeschreibung(
    val id: TypId,
    val standardVarianz: TypVarianz = TypVarianz.Invariant,
    val argumentVarianzen: List<TypVarianz> = emptyList(),
) {
    fun varianzFür(index: Int): TypVarianz = argumentVarianzen.getOrNull(index) ?: standardVarianz
}

/**
 * Erweiterbares Register atomarer Untertypen und parametrisierter Konstruktoren.
 * Mehrere Eltern sind ausdrücklich erlaubt, damit spätere mathematische und
 * Godot-Strukturen nicht in einen künstlichen Einfachvererbungsbaum gezwungen werden.
 */
class TypRegister {
    private val eltern = linkedMapOf<TypId, LinkedHashSet<TypId>>()
    private val konstruktoren = linkedMapOf<TypId, TypKonstruktorBeschreibung>()

    fun registriereAtom(id: TypId, elternTypen: Iterable<TypId> = emptyList()) {
        val menge = eltern.getOrPut(id) { linkedSetOf() }
        menge += elternTypen
        elternTypen.forEach { eltern.getOrPut(it) { linkedSetOf() } }
    }

    fun registriereKonstruktor(beschreibung: TypKonstruktorBeschreibung, elternTypen: Iterable<TypId> = emptyList()) {
        konstruktoren[beschreibung.id] = beschreibung
        registriereAtom(beschreibung.id, elternTypen)
    }

    fun konstruktor(id: TypId): TypKonstruktorBeschreibung? = konstruktoren[id]

    fun istUntertyp(von: TypId, erwartet: TypId): Boolean {
        if (von == erwartet) return true
        val offen = ArrayDeque<TypId>().apply { add(von) }
        val besucht = mutableSetOf<TypId>()
        while (offen.isNotEmpty()) {
            val aktuell = offen.removeFirst()
            if (!besucht.add(aktuell)) continue
            if (aktuell == erwartet) return true
            eltern[aktuell].orEmpty().forEach(offen::add)
        }
        return false
    }

    fun gemeinsameOberart(ids: Iterable<TypId>): TypId? {
        val typen = ids.distinct()
        if (typen.isEmpty()) return null
        if (typen.size == 1) return typen.single()
        val distanzen = typen.associateWith(::distanzenZuObertypen)
        val gemeinsam = distanzen.values
            .map { it.keys }
            .reduce { links, rechts -> links intersect rechts }
        return gemeinsam.minWithOrNull(
            compareBy<TypId> { kandidat -> distanzen.values.maxOf { it[kandidat] ?: Int.MAX_VALUE } }
                .thenBy { kandidat -> distanzen.values.sumOf { it[kandidat] ?: Int.MAX_VALUE / 4 } }
                .thenBy { it.wert },
        )
    }

    private fun distanzenZuObertypen(start: TypId): Map<TypId, Int> {
        val distanzen = linkedMapOf<TypId, Int>()
        val offen = ArrayDeque<Pair<TypId, Int>>().apply { add(start to 0) }
        while (offen.isNotEmpty()) {
            val (aktuell, distanz) = offen.removeFirst()
            val bisher = distanzen[aktuell]
            if (bisher != null && bisher <= distanz) continue
            distanzen[aktuell] = distanz
            eltern[aktuell].orEmpty().forEach { offen.add(it to distanz + 1) }
        }
        return distanzen
    }
}

interface TypSystem {
    fun prüfe(quelle: TypAusdruck, ziel: TypAusdruck): TypPrüfung
    fun normalisiere(typ: TypAusdruck): TypAusdruck
    fun gemeinsameOberart(typen: List<TypAusdruck>): TypAusdruck?
}

fun interface TypAnforderungsPrüfer {
    fun prüfe(quelle: TypAusdruck, anforderungen: Set<TypAnforderung>): TypPrüfung
}

/** G0.2 transportiert Anforderungen; ohne Domänenprüfer bleiben sie bewusst unbestimmt. */
object KeineTypAnforderungsPrüfung : TypAnforderungsPrüfer {
    override fun prüfe(quelle: TypAusdruck, anforderungen: Set<TypAnforderung>): TypPrüfung =
        if (anforderungen.isEmpty()) TypPrüfung.Kompatibel else TypPrüfung.Unbestimmt
}

class StandardTypSystem(
    private val register: TypRegister,
) : TypSystem {

    override fun prüfe(quelle: TypAusdruck, ziel: TypAusdruck): TypPrüfung {
        val von = normalisiere(quelle)
        val nach = normalisiere(ziel)
        if (von == nach) return TypPrüfung.Kompatibel
        if (nach == TypAusdruck.Beliebig) return TypPrüfung.Kompatibel
        if (von == TypAusdruck.Unbekannt || nach == TypAusdruck.Unbekannt) return TypPrüfung.Unbestimmt
        if (von is TypAusdruck.Variable || nach is TypAusdruck.Variable) return TypPrüfung.Unbestimmt
        if (von == TypAusdruck.Beliebig) return TypPrüfung.Unbestimmt

        if (nach is TypAusdruck.Vereinigung) {
            return prüfeGegenVereinigung(von, nach)
        }
        if (von is TypAusdruck.Vereinigung) {
            return kombiniereAlle(von.alternativen.map { prüfe(it, nach) })
        }

        if (von is TypAusdruck.Atom && nach is TypAusdruck.Atom) {
            return if (register.istUntertyp(von.id, nach.id)) {
                TypPrüfung.Kompatibel
            } else {
                TypPrüfung.Inkompatibel("${von.id} ist kein Untertyp von ${nach.id}.")
            }
        }

        if (von is TypAusdruck.Parameterisiert && nach is TypAusdruck.Atom) {
            return if (register.istUntertyp(von.konstruktor, nach.id)) {
                TypPrüfung.Kompatibel
            } else {
                TypPrüfung.Inkompatibel("${von.konstruktor} ist kein Untertyp von ${nach.id}.")
            }
        }

        if (von is TypAusdruck.Atom && nach is TypAusdruck.Parameterisiert) {
            return if (register.istUntertyp(von.id, nach.konstruktor)) {
                TypPrüfung.Unbestimmt
            } else {
                TypPrüfung.Inkompatibel("${von.id} erfüllt den parametrisierten Typ ${nach.konstruktor} nicht.")
            }
        }

        if (von is TypAusdruck.Parameterisiert && nach is TypAusdruck.Parameterisiert) {
            if (von.konstruktor != nach.konstruktor) {
                return if (register.istUntertyp(von.konstruktor, nach.konstruktor)) {
                    TypPrüfung.Unbestimmt
                } else {
                    TypPrüfung.Inkompatibel("Die Typkonstruktoren ${von.konstruktor} und ${nach.konstruktor} sind inkompatibel.")
                }
            }
            if (von.argumente.size != nach.argumente.size) {
                return TypPrüfung.Inkompatibel("Die parametrisierten Typen besitzen unterschiedliche Stelligkeit.")
            }
            val beschreibung = register.konstruktor(von.konstruktor)
            val ergebnisse = von.argumente.indices.map { index ->
                when (beschreibung?.varianzFür(index) ?: TypVarianz.Invariant) {
                    TypVarianz.Kovariant -> prüfe(von.argumente[index], nach.argumente[index])
                    TypVarianz.Kontravariant -> prüfe(nach.argumente[index], von.argumente[index])
                    TypVarianz.Invariant -> if (normalisiere(von.argumente[index]) == normalisiere(nach.argumente[index])) {
                        TypPrüfung.Kompatibel
                    } else {
                        TypPrüfung.Inkompatibel("Typargument $index ist invariant und unterscheidet sich.")
                    }
                }
            }
            return kombiniereAlle(ergebnisse)
        }

        return TypPrüfung.Inkompatibel("${kanonischerSchlüssel(von)} kann nicht an ${kanonischerSchlüssel(nach)} angeschlossen werden.")
    }

    private fun prüfeGegenVereinigung(quelle: TypAusdruck, ziel: TypAusdruck.Vereinigung): TypPrüfung {
        if (quelle is TypAusdruck.Vereinigung) {
            return kombiniereAlle(quelle.alternativen.map { alternative ->
                kombiniereIrgendeine(ziel.alternativen.map { prüfe(alternative, it) })
            })
        }
        return kombiniereIrgendeine(ziel.alternativen.map { prüfe(quelle, it) })
    }

    override fun normalisiere(typ: TypAusdruck): TypAusdruck = when (typ) {
        TypAusdruck.Beliebig, TypAusdruck.Unbekannt, is TypAusdruck.Atom, is TypAusdruck.Variable -> typ
        is TypAusdruck.Parameterisiert -> typ.copy(argumente = typ.argumente.map(::normalisiere))
        is TypAusdruck.Vereinigung -> normalisiereVereinigung(typ.alternativen)
    }

    private fun normalisiereVereinigung(alternativen: List<TypAusdruck>): TypAusdruck {
        val flach = alternativen.flatMap { alternative ->
            when (val norm = normalisiere(alternative)) {
                is TypAusdruck.Vereinigung -> norm.alternativen
                else -> listOf(norm)
            }
        }
        if (flach.any { it == TypAusdruck.Beliebig }) return TypAusdruck.Beliebig
        val eindeutig = flach.distinct()
        val reduziert = eindeutig.filter { kandidat ->
            eindeutig.none { anderer ->
                anderer != kandidat && prüfe(kandidat, anderer) == TypPrüfung.Kompatibel
            }
        }.sortedBy(::kanonischerSchlüssel)
        return when (reduziert.size) {
            0 -> TypAusdruck.Unbekannt
            1 -> reduziert.single()
            else -> TypAusdruck.Vereinigung(reduziert)
        }
    }

    override fun gemeinsameOberart(typen: List<TypAusdruck>): TypAusdruck? {
        if (typen.isEmpty()) return null
        val normalisiert = typen.map(::normalisiere)
        if (normalisiert.distinct().size == 1) return normalisiert.single()
        if (normalisiert.all { it is TypAusdruck.Atom }) {
            val oberart = register.gemeinsameOberart(normalisiert.filterIsInstance<TypAusdruck.Atom>().map { it.id })
            if (oberart != null) return TypAusdruck.Atom(oberart)
        }
        return normalisiere(TypAusdruck.Vereinigung(normalisiert))
    }

    private fun kombiniereAlle(ergebnisse: List<TypPrüfung>): TypPrüfung {
        ergebnisse.filterIsInstance<TypPrüfung.Inkompatibel>().firstOrNull()?.let { return it }
        return if (ergebnisse.any { it == TypPrüfung.Unbestimmt }) TypPrüfung.Unbestimmt else TypPrüfung.Kompatibel
    }

    private fun kombiniereIrgendeine(ergebnisse: List<TypPrüfung>): TypPrüfung {
        if (ergebnisse.any { it == TypPrüfung.Kompatibel }) return TypPrüfung.Kompatibel
        if (ergebnisse.any { it == TypPrüfung.Unbestimmt }) return TypPrüfung.Unbestimmt
        return ergebnisse.filterIsInstance<TypPrüfung.Inkompatibel>().firstOrNull()
            ?: TypPrüfung.Inkompatibel("Keine Typalternative ist kompatibel.")
    }

    private fun kanonischerSchlüssel(typ: TypAusdruck): String = when (typ) {
        TypAusdruck.Beliebig -> "*"
        TypAusdruck.Unbekannt -> "?"
        is TypAusdruck.Atom -> typ.id.wert
        is TypAusdruck.Variable -> "\$${typ.id.wert}"
        is TypAusdruck.Parameterisiert -> typ.konstruktor.wert + typ.argumente.joinToString(prefix = "<", postfix = ">") { kanonischerSchlüssel(it) }
        is TypAusdruck.Vereinigung -> typ.alternativen.joinToString(prefix = "(", postfix = ")", separator = "|") { kanonischerSchlüssel(it) }
    }

    companion object {
        fun ausAnschlussArten(arten: AnschlussArtRegister): StandardTypSystem =
            StandardTypSystem(typRegisterAusAnschlussArten(arten))
    }
}

fun typRegisterAusAnschlussArten(arten: AnschlussArtRegister): TypRegister = TypRegister().apply {
    arten.alle().forEach { art ->
        registriereAtom(
            TypId(art.id.wert),
            art.elternArt?.let { listOf(TypId(it.wert)) }.orEmpty(),
        )
    }
}

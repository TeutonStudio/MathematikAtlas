package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

import de.TeutonStudio.TypSystem.TypAusdruck
import de.TeutonStudio.TypSystem.TypId

/**
 * Stabile Komponente einer Karten-Grenze. Die Knoten-ID ist die semantische Identität;
 * [name] ist ausschließlich die sichtbare Benennung. Damit verändert Umbenennen keine
 * Tupelkomponente und eine geänderte Projektreihenfolge erfindet keine neue Identität.
 */
data class KartenSchnittstellenKomponente(
    val id: String,
    val name: String,
    val position: Int,
    val typ: TypAusdruck,
    val art: AnschlussArtId,
) {
    init {
        require(id.isNotBlank())
        require(name.isNotBlank())
        require(position >= 0)
    }
}

/**
 * Kanonische Karten-Grenze. Jede Karte ist typseitig genau eine Abbildung
 * `Tupel<E1,...,En> -> Tupel<A1,...,Am>`, auch für n=0 oder m=0.
 */
data class KartenSchnittstellenSignatur(
    val eingang: List<KartenSchnittstellenKomponente>,
    val ausgang: List<KartenSchnittstellenKomponente>,
) {
    init {
        prüfe("Eingang", eingang)
        prüfe("Ausgang", ausgang)
    }

    val eingangTyp: TypAusdruck.Parameterisiert
        get() = TypAusdruck.Parameterisiert(TypId("mathematik.tupel"), eingang.map { it.typ })

    val ausgangTyp: TypAusdruck.Parameterisiert
        get() = TypAusdruck.Parameterisiert(TypId("mathematik.tupel"), ausgang.map { it.typ })

    private fun prüfe(rolle: String, komponenten: List<KartenSchnittstellenKomponente>) {
        require(komponenten.map { it.id }.distinct().size == komponenten.size) {
            "$rolle-Komponenten einer Karte benötigen stabile eindeutige IDs."
        }
        require(komponenten.map { it.position } == komponenten.indices.toList()) {
            "$rolle-Komponenten einer Karte müssen lückenlos geordnet sein."
        }
    }
}

/** Leitet den Grenzvertrag reproduzierbar aus den öffentlichen Grenzknoten ab. */
fun KartenDaten.schnittstellenSignatur(): KartenSchnittstellenSignatur {
    val sortierung = kartenGrenzSortierung()

    fun komponenten(art: String, interneRichtung: AnschlussRichtung): List<KartenSchnittstellenKomponente> =
        knoten.asSequence()
            .filter { it.art == art }
            .sortedWith(sortierung)
            .mapIndexed { index, grenzKnoten ->
                val anschluss = grenzKnoten.anschlüsse.firstOrNull { it.richtung == interneRichtung }
                    ?: error("Der Grenzknoten '${grenzKnoten.name}' besitzt keinen passenden internen Anschluss.")
                val typ = anschluss.vertrag.typ.takeUnless { it == TypAusdruck.Unbekannt }
                    ?: TypAusdruck.Atom(TypId(anschluss.art.wert))
                KartenSchnittstellenKomponente(
                    id = grenzKnoten.id.wert,
                    name = grenzKnoten.parameter["name"]?.trim()?.takeIf { it.isNotEmpty() } ?: grenzKnoten.name,
                    position = index,
                    typ = typ,
                    art = anschluss.art,
                )
            }
            .toList()

    return KartenSchnittstellenSignatur(
        eingang = komponenten("mathematik.kartenEingang", AnschlussRichtung.Ausgang),
        ausgang = komponenten("mathematik.kartenAusgang", AnschlussRichtung.Eingang),
    )
}

const val KARTEN_SCHNITTSTELLEN_POSITION = "schnittstelle.position"

/** Gespeicherte Reihenfolge hat Vorrang; Layout wird nur für historische Grenzen benutzt. */
fun kartenGrenzSortierung(): Comparator<KnotenDaten> = compareBy<KnotenDaten>(
    { it.parameter[KARTEN_SCHNITTSTELLEN_POSITION]?.toIntOrNull()?.takeIf { p -> p >= 0 } ?: Int.MAX_VALUE },
    { it.position.y }, { it.position.x }, { it.id.wert },
)

/** Idempotente Migration: historische Layoutreihenfolge einmalig als Fachreihenfolge speichern. */
fun KartenDaten.normalisiereSchnittstellenReihenfolge(): KartenDaten {
    val positionen = listOf("mathematik.kartenEingang", "mathematik.kartenAusgang").flatMap { art ->
        knoten.filter { it.art == art }.takeIf { it.size > 1 }.orEmpty().sortedWith(kartenGrenzSortierung()).mapIndexed { i, k -> k.id to i }
    }.toMap()
    if (knoten.all { k -> positionen[k.id]?.let { k.parameter[KARTEN_SCHNITTSTELLEN_POSITION] == it.toString() } ?: true }) return this
    return copy(knoten = knoten.map { k -> positionen[k.id]?.let { i ->
        k.copy(parameter = k.parameter + (KARTEN_SCHNITTSTELLEN_POSITION to i.toString()))
    } ?: k })
}

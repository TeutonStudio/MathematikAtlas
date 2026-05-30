package com.TeutonStudio.KnotenKartenVerwalter.daten

sealed class Zahlenraum {
    abstract val kurzform: String

    data object Natuerlich : Zahlenraum() {
        override val kurzform: String = "N"
    }

    data object Ganz : Zahlenraum() {
        override val kurzform: String = "Z"
    }

    data object Rational : Zahlenraum() {
        override val kurzform: String = "Q"
    }

    data object Reell : Zahlenraum() {
        override val kurzform: String = "R"
    }

    data object Komplex : Zahlenraum() {
        override val kurzform: String = "C"
    }

    data class Eingeschraenkt(
        val basis: Zahlenraum,
        val bedingung: String,
    ) : Zahlenraum() {
        override val kurzform: String = "${basis.kurzform} $bedingung"
    }

    data class Produkt(
        val raeume: List<Zahlenraum>,
    ) : Zahlenraum() {
        override val kurzform: String = raeume.joinToString(" x ") { it.kurzform }
    }

    data class Funktion(
        val eingaben: List<Zahlenraum>,
        val ausgabe: Zahlenraum,
    ) : Zahlenraum() {
        override val kurzform: String = "${eingaben.joinToString(" x ") { it.kurzform }} -> ${ausgabe.kurzform}"
    }
}

data class ZahlenTyp(
    val raum: Zahlenraum,
    val wert: String? = null,
    val anzeigename: String? = null,
    val ausdruck: String? = null,
) {
    val kurzform: String
        get() = when {
            wert != null -> "$wert in ${raum.kurzform}"
            anzeigename != null -> "$anzeigename in ${raum.kurzform}"
            ausdruck != null -> "$ausdruck : ${raum.kurzform}"
            else -> raum.kurzform
        }
}

fun ZahlenTyp.istKompatibelMit(ziel: ZahlenTyp): Boolean = raum.istTeilraumVon(ziel.raum)

fun Zahlenraum.istTeilraumVon(ziel: Zahlenraum): Boolean {
    if (this == ziel) return true
    return when {
        this is Zahlenraum.Eingeschraenkt -> basis.istTeilraumVon(ziel)
        ziel is Zahlenraum.Eingeschraenkt -> this.istTeilraumVon(ziel.basis)
        this is Zahlenraum.Produkt && ziel is Zahlenraum.Produkt ->
            raeume.size == ziel.raeume.size && raeume.zip(ziel.raeume).all { (quelle, zielRaum) ->
                quelle.istTeilraumVon(zielRaum)
            }
        this is Zahlenraum.Funktion && ziel is Zahlenraum.Funktion ->
            eingaben.size == ziel.eingaben.size &&
                eingaben.zip(ziel.eingaben).all { (quelle, zielRaum) -> quelle.istTeilraumVon(zielRaum) } &&
                ausgabe.istTeilraumVon(ziel.ausgabe)
        else -> zahlenraumRang(this)?.let { quelleRang ->
            zahlenraumRang(ziel)?.let { zielRang -> quelleRang <= zielRang }
        } ?: false
    }
}

private fun zahlenraumRang(raum: Zahlenraum): Int? = when (raum) {
    Zahlenraum.Natuerlich -> 0
    Zahlenraum.Ganz -> 1
    Zahlenraum.Rational -> 2
    Zahlenraum.Reell -> 3
    Zahlenraum.Komplex -> 4
    is Zahlenraum.Eingeschraenkt,
    is Zahlenraum.Produkt,
    is Zahlenraum.Funktion,
    -> null
}

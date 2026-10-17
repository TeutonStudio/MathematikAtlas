package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.TypSystem.*
import de.TeutonStudio.MathematikKartenAdapter.*

/** Eine zentrale Projektion gespeicherter Mathematikkonfiguration in neutrale Verträge. */
fun effektiverMathematikAnschluss(karte: KartenDaten, ref: AnschlussVerweis): AnschlussDaten? {
    val knoten = karte.knoten.firstOrNull { it.id == ref.knotenId } ?: return null
    val anschluss = knoten.anschlüsse.firstOrNull { it.id == ref.anschlussId } ?: return null
    fun dimensionAn(aufKnoten: KnotenDaten, name: String): TypAusdruck {
        val eingang = aufKnoten.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Eingang && it.name == name }
        val quelle = eingang?.let { e -> karte.verbindungen.firstOrNull { it.zu == AnschlussVerweis(aufKnoten.id, e.id) }?.von }
        val quellKnoten = quelle?.let { q -> karte.knoten.firstOrNull { it.id == q.knotenId } }
        val text = if (quelle == null) aufKnoten.parameter["standardwert.$name"] ?: aufKnoten.parameter[name]
            else quellKnoten?.takeIf { it.art == "mathematik.zahl" }?.parameter?.get("wert")
        return text?.toIntOrNull()?.takeIf { it >= 0 }?.let { TypAusdruck.Literal(it.toString()) } ?: TypAusdruck.Unbekannt
    }
    fun dimension(name: String) = dimensionAn(knoten, name)
    fun formEingänge(): List<TypAusdruck>? {
        if (knoten.parameter[MENGENDEFINITION_FORMEINGABE] != STRUKTURFORM_TUPEL) {
            return knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang && it.name.startsWith("dimension.") }
                .sortedBy { it.reihenfolge }.map { dimension(it.name) }.takeIf { it.isNotEmpty() }
        }
        val eingang = knoten.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Eingang && it.name == "dimensionen" } ?: return null
        val quelle = karte.verbindungen.firstOrNull { it.zu == AnschlussVerweis(knoten.id, eingang.id) }?.von ?: return null
        val tupel = karte.knoten.firstOrNull { it.id == quelle.knotenId && it.art == "mathematik.tupel" } ?: return null
        if (tupelKonfiguration(tupel).erzeugungsArt == TUPEL_METHODE) return null
        return tupel.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }
            .map { dimensionAn(tupel, it.name) }
    }
    fun formTyp(art: AnschlussArtId, form: List<TypAusdruck>?): TypAusdruck {
        val zahl = TypAusdruck.Atom(MathematischeTypen.Zahl)
        return when (art) {
            MathematikAnschlussArten.SpaltenVektor.id -> TypAusdruck.Parameterisiert(MathematischeTypen.SpaltenVektor, listOf(zahl, form?.singleOrNull() ?: TypAusdruck.Unbekannt))
            MathematikAnschlussArten.ZeilenVektor.id -> TypAusdruck.Parameterisiert(MathematischeTypen.ZeilenVektor, listOf(zahl, form?.singleOrNull() ?: TypAusdruck.Unbekannt))
            MathematikAnschlussArten.Matrix.id -> TypAusdruck.Parameterisiert(MathematischeTypen.Matrix, listOf(zahl, form?.getOrNull(0) ?: TypAusdruck.Unbekannt, form?.getOrNull(1) ?: TypAusdruck.Unbekannt))
            MathematikAnschlussArten.Tensor.id -> if (form == null) TypAusdruck.Unbekannt else TypAusdruck.Parameterisiert(MathematischeTypen.Tensor, listOf(zahl) + form)
            MathematikAnschlussArten.Tupel.id -> {
                val n = (form?.singleOrNull() as? TypAusdruck.Literal)?.wert?.toIntOrNull()
                if (n == null || n < 0) TypAusdruck.Unbekannt else TypAusdruck.Parameterisiert(MathematischeTypen.Tupel, List(n) { TypAusdruck.Unbekannt })
            }
            else -> TypAusdruck.Atom(TypId(art.wert))
        }
    }
    fun quellTyp(name: String): TypAusdruck? {
        val eingang = knoten.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Eingang && it.name == name } ?: return null
        val quelle = karte.verbindungen.firstOrNull { it.zu == AnschlussVerweis(knoten.id, eingang.id) }?.von ?: return null
        return effektiverMathematikAnschluss(karte, quelle)?.vertrag?.typ
            ?: karte.knoten.firstOrNull { it.id == quelle.knotenId }?.anschlüsse?.firstOrNull { it.id == quelle.anschlussId }?.vertrag?.typ
    }
    val typ = when {
        knoten.art == VektorRechner.KNOTEN_ART && knoten.parameter[VEKTOR_RECHNER_OPERATOR] == VektorRechnerOperator.POTENZ.stabileId && anschluss.richtung == AnschlussRichtung.Ausgang -> {
            val pPort = knoten.anschlüsse.firstOrNull { it.name == "p" && it.richtung == AnschlussRichtung.Eingang }
            val verbunden = pPort != null && karte.verbindungen.any { it.zu == AnschlussVerweis(knoten.id, pPort.id) }
            val n = (dimension("p") as? TypAusdruck.Literal)?.wert?.toIntOrNull()
                ?: if (!verbunden) knoten.parameter[VEKTOR_POTENZ_EXPONENT]?.toIntOrNull() ?: 2 else null
            val q = quellTyp("a")
            val vektor = if (q is TypAusdruck.Parameterisiert && q.konstruktor == MathematischeTypen.Tupel)
                formTyp(MathematikAnschlussArten.SpaltenVektor.id, listOf(TypAusdruck.Literal(q.argumente.size.toString()))) else q
            val zahl = TypAusdruck.Atom(MathematischeTypen.Zahl)
            if (n != null && n >= 0) if (n % 2 == 0) zahl else vektor ?: formTyp(MathematikAnschlussArten.SpaltenVektor.id, null)
            else TypAusdruck.Vereinigung(listOf(zahl, formTyp(MathematikAnschlussArten.SpaltenVektor.id, null), formTyp(MathematikAnschlussArten.ZeilenVektor.id, null)))
        }

        knoten.art == VektorRechner.KNOTEN_ART &&
            knoten.parameter[VEKTOR_RECHNER_OPERATOR] == VektorRechnerOperator.KARTESISCH.stabileId &&
            anschluss.richtung == AnschlussRichtung.Ausgang -> {
            val quelle = quellTyp("winkel")
            val n = when {
                quelle is TypAusdruck.Parameterisiert && quelle.konstruktor == MathematischeTypen.Tupel -> quelle.argumente.size + 1
                quelle is TypAusdruck.Atom && quelle.id in setOf(MathematischeTypen.Zahl, MathematischeTypen.Reell,
                    MathematischeTypen.Rational, MathematischeTypen.Ganz, MathematischeTypen.Natuerlich) -> 1
                else -> null
            }
            if (n == null) TypAusdruck.Unbekannt
            else formTyp(MathematikAnschlussArten.Tupel.id, listOf(TypAusdruck.Literal(n.toString())))
        }
        knoten.art == VektorRechner.KNOTEN_ART &&
            knoten.parameter[VEKTOR_RECHNER_OPERATOR] == VektorRechnerOperator.POLARWINKEL.stabileId &&
            anschluss.richtung == AnschlussRichtung.Ausgang -> {
            val quellTyp = quellTyp("kartesisch") as? TypAusdruck.Parameterisiert
            if (quellTyp?.konstruktor == MathematischeTypen.Tupel && quellTyp.argumente.size == 1)
                TypAusdruck.Atom(MathematischeTypen.Reell)
            else if (quellTyp?.konstruktor == MathematischeTypen.Tupel && quellTyp.argumente.size >= 2)
                TypAusdruck.Parameterisiert(MathematischeTypen.Tupel, List(quellTyp.argumente.size - 1) { TypAusdruck.Atom(MathematischeTypen.Winkel) })
            else TypAusdruck.Unbekannt
        }
        knoten.art == ÜBERSETZER_ART && anschluss.richtung == AnschlussRichtung.Ausgang -> {
            val ziel = übersetzerZiel(knoten)
            val eingang = knoten.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Eingang && it.name == "wert" }
            val quelle = eingang?.let { p -> karte.verbindungen.firstOrNull { it.zu == AnschlussVerweis(knoten.id, p.id) }?.von }
            val quellPort = quelle?.let { q -> karte.knoten.firstOrNull { it.id == q.knotenId }?.anschlüsse?.firstOrNull { it.id == q.anschlussId } }
            val quellTyp = quelle?.let { effektiverMathematikAnschluss(karte, it)?.vertrag?.typ } as? TypAusdruck.Parameterisiert
            val dimension = when (quellTyp?.konstruktor) {
                MathematischeTypen.Tupel -> TypAusdruck.Literal(quellTyp.argumente.size.toString())
                MathematischeTypen.SpaltenVektor, MathematischeTypen.ZeilenVektor -> quellTyp.argumente.getOrNull(1) ?: TypAusdruck.Unbekannt
                else -> TypAusdruck.Unbekannt
            }
            when (ziel) {
                "abflachen" -> TypAusdruck.Unbekannt
                "zeile" -> formTyp(MathematikAnschlussArten.ZeilenVektor.id, listOf(dimension))
                "spalte" -> formTyp(MathematikAnschlussArten.SpaltenVektor.id, listOf(dimension))
                "tupel" -> formTyp(MathematikAnschlussArten.Tupel.id, listOf(dimension))
                "polar" -> {
                    val n = (dimension as? TypAusdruck.Literal)?.wert?.toIntOrNull()
                    if (n == null || n <= 0) TypAusdruck.Unbekannt else TypAusdruck.Parameterisiert(MathematischeTypen.Tupel,
                        listOf(TypAusdruck.Atom(MathematischeTypen.Reell),
                            if (übersetzerPolarKonvention(knoten) == PolarKonvention.SIGNED) TypAusdruck.Parameterisiert(MathematischeTypen.Tupel, listOf(TypAusdruck.Atom(MathematischeTypen.Reell)) + List(n - 1) { TypAusdruck.Atom(MathematischeTypen.Winkel) })
                            else if (n == 1) TypAusdruck.Atom(MathematischeTypen.Reell)
                            else TypAusdruck.Parameterisiert(MathematischeTypen.Tupel, List(n - 1) { TypAusdruck.Atom(MathematischeTypen.Winkel) })))
                }
                "kartesisch" -> if (übersetzerEindimensional(knoten))
                    formTyp(MathematikAnschlussArten.Tupel.id, listOf(TypAusdruck.Literal("1")))
                else {
                    val winkelTyp = quellTyp?.argumente?.getOrNull(1) as? TypAusdruck.Parameterisiert
                    val n = if (quellTyp?.argumente?.size == 2 && winkelTyp?.konstruktor == MathematischeTypen.Tupel)
                        TypAusdruck.Literal((winkelTyp.argumente.size + if (übersetzerPolarKonvention(knoten) == PolarKonvention.SIGNED) 0 else 1).toString()) else dimension
                    formTyp(MathematikAnschlussArten.Tupel.id, listOf(n))
                }
                else -> TypAusdruck.Unbekannt
            }
        }
        knoten.art == TUPEL_ABFLACHEN_ART -> TypAusdruck.Unbekannt
        anschluss.richtung == AnschlussRichtung.Ausgang && knoten.art == "mathematik.variable" -> {
            anschluss.vertrag.typ
        }

        anschluss.richtung == AnschlussRichtung.Ausgang && knoten.art == "mathematik.allgemeinerParameter" -> {
            runCatching {
                val bereich=WertebereichKonfiguration.vonEigenschaft(knoten.eigenschaften[WertebereichKonfiguration.EIGENSCHAFT])
                quellenGrundraumInformation(bereich.zuMenge()).typ
            }.getOrDefault(TypAusdruck.Unbekannt)
        }
        anschluss.richtung == AnschlussRichtung.Ausgang && knoten.art == MENGENKONSTRUKTOR_ART && anschluss.name == "element" -> {
            val art = AnschlussArtId(knoten.parameter[MENGENDEFINITION_ELEMENTART] ?: MathematikAnschlussArten.Zahl.id.wert)
            val form = when (knoten.parameter[MENGENDEFINITION_FORMMODUS]) {
                STRUKTURFORM_INSPEKTOR -> runCatching { parseStrukturForm(knoten.parameter[MENGENDEFINITION_FORM]).map {
                    if (it is RationaleZahl) TypAusdruck.Literal(it.toString()) else TypAusdruck.Unbekannt
                } }.getOrNull()
                STRUKTURFORM_EINGANG -> formEingänge()
                else -> null
            }
            formTyp(art, form)
        }
        anschluss.richtung == AnschlussRichtung.Ausgang && knoten.art in setOf(VEKTOR_KONSTRUKTOR_ART, "mathematik.zeilenVektor") -> {
            val config = vektorKonstruktorKonfiguration(knoten)
            val art = if (config.orientierung == VEKTOR_ORIENTIERUNG_ZEILE) MathematikAnschlussArten.ZeilenVektor.id else MathematikAnschlussArten.SpaltenVektor.id
            val n = if (config.erzeugungsArt == VEKTOR_METHODE) dimension("dimension")
                else TypAusdruck.Literal(knoten.anschlüsse.count { it.richtung == AnschlussRichtung.Eingang }.toString())
            formTyp(art, listOf(n))
        }
        anschluss.richtung == AnschlussRichtung.Ausgang && knoten.art == "mathematik.tupel" -> {
            val n = if (tupelKonfiguration(knoten).erzeugungsArt == TUPEL_METHODE) dimension("dimension")
                else TypAusdruck.Literal(knoten.anschlüsse.count { it.richtung == AnschlussRichtung.Eingang }.toString())
            formTyp(MathematikAnschlussArten.Tupel.id, listOf(n))
        }
        knoten.art == "mathematik.matrix" -> {
            val konfiguration = matrixKonfiguration(knoten)
            when {
                anschluss.richtung == AnschlussRichtung.Ausgang -> TypAusdruck.Parameterisiert(MathematischeTypen.Matrix, listOf(
                    TypAusdruck.Atom(MathematischeTypen.Zahl), TypAusdruck.Literal(konfiguration.höhe.toString()), TypAusdruck.Literal(konfiguration.breite.toString()),
                ))
                anschluss.art == MathematikAnschlussArten.SpaltenVektor.id -> TypAusdruck.Parameterisiert(MathematischeTypen.SpaltenVektor, listOf(
                    TypAusdruck.Atom(MathematischeTypen.Zahl), TypAusdruck.Literal(konfiguration.höhe.toString()),
                ))
                anschluss.art == MathematikAnschlussArten.ZeilenVektor.id -> TypAusdruck.Parameterisiert(MathematischeTypen.ZeilenVektor, listOf(
                    TypAusdruck.Atom(MathematischeTypen.Zahl), TypAusdruck.Literal(konfiguration.breite.toString()),
                ))
                else -> return null
            }
        }
        else -> return null
    }
    val id = when (typ) { is TypAusdruck.Atom -> typ.id; is TypAusdruck.Parameterisiert -> typ.konstruktor; else -> null }
    val art = when (id) {
        MathematischeTypen.Tupel -> MathematikAnschlussArten.Tupel.id
        MathematischeTypen.SpaltenVektor -> MathematikAnschlussArten.SpaltenVektor.id
        MathematischeTypen.ZeilenVektor -> MathematikAnschlussArten.ZeilenVektor.id
        MathematischeTypen.Matrix -> MathematikAnschlussArten.Matrix.id
        MathematischeTypen.Menge -> MathematikAnschlussArten.Menge.id
        MathematischeTypen.Aussage -> MathematikAnschlussArten.Aussage.id
        MathematischeTypen.Natuerlich, MathematischeTypen.Ganz, MathematischeTypen.Rational,
        MathematischeTypen.Reell, MathematischeTypen.Komplex, MathematischeTypen.Zahl -> MathematikAnschlussArten.Zahl.id
        else -> anschluss.art
    }
    return anschluss.copy(art = art, vertrag = anschluss.vertrag.copy(typ = typ))
}

package de.TeutonStudio.MathematikRechenSystem.kern

enum class Wahrheitswert(val latex: String) {
    Wahr("\\mathcal{Wahr}"),
    Lüge("\\mathcal{Lüge}"),
}

sealed interface EntscheidungsStatus {
    data object Bewiesen : EntscheidungsStatus
    data object Widerlegt : EntscheidungsStatus
    data object Unentscheidbar : EntscheidungsStatus
    data object Unbekannt : EntscheidungsStatus
    data object NichtAuswertbar : EntscheidungsStatus
}

data class AussageErgebnis(
    val wahrheitswert: Wahrheitswert?,
    val status: EntscheidungsStatus,
    val begründung: String = "",
)

sealed interface Aussage : Ausdruck {
    fun entscheide(kontext: RechenKontext = RechenKontext()): AussageErgebnis
}

data class WahrheitsKonstante(val wert: Boolean) : Aussage {
    override fun entscheide(kontext: RechenKontext) = AussageErgebnis(
        if (wert) Wahrheitswert.Wahr else Wahrheitswert.Lüge,
        if (wert) EntscheidungsStatus.Bewiesen else EntscheidungsStatus.Widerlegt,
    )
    override fun zuLatex() = if (wert) Wahrheitswert.Wahr.latex else Wahrheitswert.Lüge.latex
}

data class Gleichheit(val links: MathematischesObjekt, val rechts: MathematischesObjekt) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis {
        val l = if (links is ZahlAusdruck) vereinfache(links, kontext) else links
        val r = if (rechts is ZahlAusdruck) vereinfache(rechts, kontext) else rechts
        return when {
            l == r -> AussageErgebnis(Wahrheitswert.Wahr, EntscheidungsStatus.Bewiesen, "Beide Seiten sind identisch.")
            l is RationaleZahl && r is RationaleZahl -> AussageErgebnis(Wahrheitswert.Lüge, EntscheidungsStatus.Widerlegt)
            l is ZahlAusdruck && r is MengenAusdruck || l is MengenAusdruck && r is ZahlAusdruck ->
                AussageErgebnis(Wahrheitswert.Lüge, EntscheidungsStatus.Widerlegt, "Eine Zahl kann nicht mit einer Menge gleich sein.")
            else -> AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
        }
    }
    override fun zuLatex() = "${links.zuLatex()} = ${rechts.zuLatex()}"
}

data class Ungleichheit(val links: MathematischesObjekt, val rechts: MathematischesObjekt) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis {
        val gleich = Gleichheit(links, rechts).entscheide(kontext)
        return when (gleich.wahrheitswert) {
            Wahrheitswert.Wahr -> AussageErgebnis(Wahrheitswert.Lüge, EntscheidungsStatus.Widerlegt)
            Wahrheitswert.Lüge -> AussageErgebnis(Wahrheitswert.Wahr, EntscheidungsStatus.Bewiesen)
            null -> AussageErgebnis(null, gleich.status)
        }
    }
    override fun zuLatex() = "${links.zuLatex()} \\neq ${rechts.zuLatex()}"
}

enum class VergleichsArt(val latex: String) { Kleiner("<"), KleinerGleich("\\le"), Größer(">"), GrößerGleich("\\ge") }

data class Vergleich(val links: ZahlAusdruck, val art: VergleichsArt, val rechts: ZahlAusdruck) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis {
        val l = vereinfache(links, kontext)
        val r = vereinfache(rechts, kontext)
        if (l !is RationaleZahl || r !is RationaleZahl) return AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
        val wahr = when (art) {
            VergleichsArt.Kleiner -> l < r
            VergleichsArt.KleinerGleich -> l <= r
            VergleichsArt.Größer -> l > r
            VergleichsArt.GrößerGleich -> l >= r
        }
        return AussageErgebnis(if (wahr) Wahrheitswert.Wahr else Wahrheitswert.Lüge, if (wahr) EntscheidungsStatus.Bewiesen else EntscheidungsStatus.Widerlegt)
    }
    override fun zuLatex() = "${links.zuLatex()} ${art.latex} ${rechts.zuLatex()}"
}

data class Negation(val aussage: Aussage) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis {
        val e = aussage.entscheide(kontext)
        return e.copy(wahrheitswert = when (e.wahrheitswert) {
            Wahrheitswert.Wahr -> Wahrheitswert.Lüge
            Wahrheitswert.Lüge -> Wahrheitswert.Wahr
            null -> null
        }, status = when (e.status) {
            EntscheidungsStatus.Bewiesen -> EntscheidungsStatus.Widerlegt
            EntscheidungsStatus.Widerlegt -> EntscheidungsStatus.Bewiesen
            else -> e.status
        })
    }
    override fun zuLatex() = "\\neg\\left(${aussage.zuLatex()}\\right)"
}

data class Konjunktion(val aussagen: List<Aussage>) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis {
        val ergebnisse = aussagen.map { it.entscheide(kontext) }
        if (ergebnisse.any { it.wahrheitswert == Wahrheitswert.Lüge }) return AussageErgebnis(Wahrheitswert.Lüge, EntscheidungsStatus.Widerlegt)
        if (ergebnisse.all { it.wahrheitswert == Wahrheitswert.Wahr }) return AussageErgebnis(Wahrheitswert.Wahr, EntscheidungsStatus.Bewiesen)
        return AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
    }
    override fun zuLatex() = zuRelationsKettenLatex()
}

data class Disjunktion(val aussagen: List<Aussage>) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis {
        val ergebnisse = aussagen.map { it.entscheide(kontext) }
        if (ergebnisse.any { it.wahrheitswert == Wahrheitswert.Wahr }) return AussageErgebnis(Wahrheitswert.Wahr, EntscheidungsStatus.Bewiesen)
        if (ergebnisse.all { it.wahrheitswert == Wahrheitswert.Lüge }) return AussageErgebnis(Wahrheitswert.Lüge, EntscheidungsStatus.Widerlegt)
        return AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
    }
    override fun zuLatex() = aussagen.joinToString(" \\lor ") { it.zuLatex() }
}

data class Implikation(val voraussetzung: Aussage, val folgerung: Aussage) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis {
        val a = voraussetzung.entscheide(kontext).wahrheitswert
        val b = folgerung.entscheide(kontext).wahrheitswert
        return when {
            a == Wahrheitswert.Lüge || b == Wahrheitswert.Wahr -> AussageErgebnis(Wahrheitswert.Wahr, EntscheidungsStatus.Bewiesen)
            a == Wahrheitswert.Wahr && b == Wahrheitswert.Lüge -> AussageErgebnis(Wahrheitswert.Lüge, EntscheidungsStatus.Widerlegt)
            else -> AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
        }
    }
    override fun zuLatex() = "${voraussetzung.zuLatex()} \\Rightarrow ${folgerung.zuLatex()}"
}

data class Äquivalenz(val links: Aussage, val rechts: Aussage) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis {
        val a = links.entscheide(kontext).wahrheitswert
        val b = rechts.entscheide(kontext).wahrheitswert
        if (a == null || b == null) return AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
        val wahr = a == b
        return AussageErgebnis(if (wahr) Wahrheitswert.Wahr else Wahrheitswert.Lüge, if (wahr) EntscheidungsStatus.Bewiesen else EntscheidungsStatus.Widerlegt)
    }
    override fun zuLatex() = "${links.zuLatex()} \\Leftrightarrow ${rechts.zuLatex()}"
}

/** Ausschließendes Oder: genau einer der beiden Wahrheitswerte ist wahr. */
data class Adjunktion(val links: Aussage, val rechts: Aussage) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis {
        val linksWert = links.entscheide(kontext).wahrheitswert
        val rechtsWert = rechts.entscheide(kontext).wahrheitswert
        if (linksWert == null || rechtsWert == null) return AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
        val wahr = linksWert != rechtsWert
        return AussageErgebnis(
            wahrheitswert = if (wahr) Wahrheitswert.Wahr else Wahrheitswert.Lüge,
            status = if (wahr) EntscheidungsStatus.Bewiesen else EntscheidungsStatus.Widerlegt,
        )
    }
    override fun zuLatex() = "${links.zuLatex()} \\stackrel{\\bullet}{\\lor} ${rechts.zuLatex()}"
}

/** Assoziative Paritätsfortsetzung der binären Adjunktion. */
fun adjunktion(aussagen: List<Aussage>): Aussage = when (aussagen.size) {
    0 -> WahrheitsKonstante(false)
    1 -> aussagen.single()
    else -> aussagen.reduce(::Adjunktion)
}

data class UnentscheidbareAussage(val bezeichnung: String, val system: String) : Aussage {
    override fun entscheide(kontext: RechenKontext) = AussageErgebnis(null, EntscheidungsStatus.Unentscheidbar, "Unentscheidbar in $system")
    override fun zuLatex() = "\\operatorname{${bezeichnung.replace(" ", "\\ ")}}"
}

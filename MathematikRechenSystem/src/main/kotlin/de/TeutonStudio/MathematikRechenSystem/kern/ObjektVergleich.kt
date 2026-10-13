package de.TeutonStudio.MathematikRechenSystem.kern

/** Inhalt und typisierte Identität sind unabhängig von Collection-equals. */
fun entscheideGleichheit(links: MathematischesObjekt, rechts: MathematischesObjekt, kontext: RechenKontext = RechenKontext()): AussageErgebnis =
    vergleicheObjekte(links, rechts, kontext, selbig = false)

fun entscheideSelbigkeit(links: MathematischesObjekt, rechts: MathematischesObjekt, kontext: RechenKontext = RechenKontext()): AussageErgebnis =
    vergleicheObjekte(links, rechts, kontext, selbig = true)

private fun vergleichsWert(wahr: Boolean) = AussageErgebnis(
    if (wahr) Wahrheitswert.Wahr else Wahrheitswert.Lüge,
    if (wahr) EntscheidungsStatus.Bewiesen else EntscheidungsStatus.Widerlegt,
)
private fun unbekannterVergleich() = AussageErgebnis(null, EntscheidungsStatus.Unbekannt)

private fun vergleicheObjekte(links: MathematischesObjekt, rechts: MathematischesObjekt, kontext: RechenKontext, selbig: Boolean): AussageErgebnis {
    fun normalisiere(wert: MathematischesObjekt): MathematischesObjekt = when (wert) {
        is ZahlAusdruck -> vereinfache(wert, kontext)
        is EndlicheMenge -> if (wert.elemente.isEmpty()) LeereMenge else wert
        is ReellesIntervall -> reellesIntervall(wert.links, wert.linksOffen, wert.rechts, wert.rechtsOffen, kontext)
        else -> wert
    }
    val l = normalisiere(links)
    val r = normalisiere(rechts)
    // Strukturelle Identität beweist Reflexivität auch für symbolische Objekte;
    // fehlende strukturelle Identität beweist dagegen keine Ungleichheit.
    if (l == r) return vergleichsWert(true)
    if (l is MengenAusdruck && r is MengenAusdruck &&
        MengenKonstante.vonMenge(l) != null && MengenKonstante.vonMenge(r) != null
    ) return vergleichsWert(false)
    fun paar(a: MathematischesObjekt, b: MathematischesObjekt) = vergleicheObjekte(a, b, kontext, selbig)
    fun alle(paare: List<Pair<MathematischesObjekt, MathematischesObjekt>>): AussageErgebnis {
        val ergebnisse = paare.map { (a, b) -> paar(a, b) }
        return when {
            ergebnisse.any { it.wahrheitswert == Wahrheitswert.Lüge } -> vergleichsWert(false)
            ergebnisse.all { it.wahrheitswert == Wahrheitswert.Wahr } -> vergleichsWert(true)
            else -> unbekannterVergleich()
        }
    }
    fun folge(a: List<MathematischesObjekt>, b: List<MathematischesObjekt>) =
        if (a.size != b.size) vergleichsWert(false) else alle(a.zip(b))
    fun komponenten(a: MathematischesObjekt): List<MathematischesObjekt>? = when (a) {
        is Tupel -> a.elemente
        is OrientierterVektor -> a.werte
        else -> null
    }
    fun konkreteStruktur(a: MathematischesObjekt) = a is Tupel || a is OrientierterVektor || a is Matrix || a is Tensor ||
        a is RationaleZahl || a is KomplexeZahl || a is WahrheitsKonstante
    if (selbig && l::class != r::class && konkreteStruktur(l) && konkreteStruktur(r)) return vergleichsWert(false)
    val lk = komponenten(l)
    val rk = komponenten(r)
    if (lk != null && rk != null) return folge(lk, rk)
    return when {
        l is GradWinkel && r is GradWinkel -> paar(l.grad, r.grad)
        l is WinkelAusdruck && r is ZahlAusdruck || r is WinkelAusdruck && l is ZahlAusdruck -> vergleichsWert(false)
        l is RationaleZahl && r is RationaleZahl -> vergleichsWert(l.compareTo(r) == 0)
        l is KomplexeZahl && r is KomplexeZahl -> alle(listOf(l.realteil to r.realteil, l.imaginärteil to r.imaginärteil))
        !selbig && l is KomplexeZahl && r is RationaleZahl -> alle(listOf(l.realteil to r, l.imaginärteil to RationaleZahl.Null))
        !selbig && l is RationaleZahl && r is KomplexeZahl -> alle(listOf(l to r.realteil, RationaleZahl.Null to r.imaginärteil))
        // Gleich geschriebene normalisierte Zahlterme sind ein hinreichender Beweis,
        // verschiedene symbolische Terme dagegen kein Beweis ihrer Ungleichheit.
        l is ZahlAusdruck && r is ZahlAusdruck && l == r -> vergleichsWert(true)
        l is WahrheitsKonstante && r is WahrheitsKonstante -> vergleichsWert(l.wert == r.wert)
        l is Matrix && r is Matrix -> if (l.tensorForm != r.tensorForm) vergleichsWert(false) else folge(l.zeilen.flatten(), r.zeilen.flatten())
        l is Tensor && r is Tensor -> if (l.dimensionen != r.dimensionen) vergleichsWert(false) else folge(l.werte, r.werte)
        (l is EndlicheMenge || l === LeereMenge) && (r is EndlicheMenge || r === LeereMenge) -> {
            val a = (l as? EndlicheMenge)?.elemente.orEmpty()
            val b = (r as? EndlicheMenge)?.elemente.orEmpty()
            // Bidirektionale Überdeckung: Inhaltsgleichheit darf typverschiedene
            // konkrete Elemente identifizieren, ohne ihre Kardinalität zu verlieren.
            fun abgedeckt(x: Set<MathematischesObjekt>, y: Set<MathematischesObjekt>): List<AussageErgebnis> = x.map { element ->
                val vergleiche = y.map { paar(element, it) }
                when {
                    vergleiche.any { it.wahrheitswert == Wahrheitswert.Wahr } -> vergleichsWert(true)
                    vergleiche.all { it.wahrheitswert == Wahrheitswert.Lüge } -> vergleichsWert(false)
                    else -> unbekannterVergleich()
                }
            }
            val ergebnisse = abgedeckt(a, b) + abgedeckt(b, a)
            when {
                ergebnisse.any { it.wahrheitswert == Wahrheitswert.Lüge } -> vergleichsWert(false)
                ergebnisse.all { it.wahrheitswert == Wahrheitswert.Wahr } -> vergleichsWert(true)
                else -> unbekannterVergleich()
            }
        }
        l is Tupelraum && r is Tupelraum -> folge(l.komponenten, r.komponenten)
        l is KartesischesProdukt && r is KartesischesProdukt -> folge(l.mengen, r.mengen)
        l is Folgenraum && r is Folgenraum -> paar(l.elementMenge, r.elementMenge)
        l is Vektorraum && r is Vektorraum -> if (l.dimension != r.dimension || l.orientierung != r.orientierung) vergleichsWert(false) else paar(l.skalarMenge, r.skalarMenge)
        l is Matrizenraum && r is Matrizenraum -> if (l.zeilen != r.zeilen || l.spalten != r.spalten) vergleichsWert(false) else paar(l.skalarMenge, r.skalarMenge)
        l is BenannteMenge && r is BenannteMenge && l.name == r.name -> vergleichsWert(true)
        l is ReellesIntervall && r is ReellesIntervall -> {
            if (l.linksOffen == r.linksOffen && l.rechtsOffen == r.rechtsOffen) {
                alle(listOf(l.links to r.links, l.rechts to r.rechts))
            } else if (l.links is RationaleZahl && l.rechts is RationaleZahl &&
                r.links is RationaleZahl && r.rechts is RationaleZahl) {
                // Leere und einelementige rationale Intervalle wurden oben normalisiert.
                vergleichsWert(false)
            } else {
                // Symbolische Grenzen können beide Intervalle leer machen. Verschiedene
                // Randoffenheit allein ist deshalb kein Gegenbeweis.
                unbekannterVergleich()
            }
        }
        l is ZahlAusdruck && (rk != null || r is MengenAusdruck) || r is ZahlAusdruck && (lk != null || l is MengenAusdruck) -> vergleichsWert(false)
        else -> unbekannterVergleich()
    }
}

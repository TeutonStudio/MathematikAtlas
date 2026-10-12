package de.TeutonStudio.MathematikRechenSystem.kern

/** An exact set, with a constructive trace; a symbolic remainder is never an empty answer. */
data class PrädikatsLösung(
    val menge: MengenAusdruck,
    val schritte: List<UmformungsSchritt>,
    val warnungen: List<String> = emptyList(),
)

fun lösungsRaum(variablen: List<GebundeneMengenVariable>): MengenAusdruck = when (variablen.size) {
    0 -> EndlicheMenge(setOf(Tupel(emptyList())))
    1 -> variablen.single().grundMenge
    else -> KartesischesProdukt(variablen.map { it.grundMenge })
}

/** Shared work limit for branching, finite enumeration and polynomial expansion. */
private class LösungsBudget(var rest: Int) {
    fun schritt() { if (--rest < 0 || Thread.currentThread().isInterrupted) throw LösungsGrenze() }
}
private class LösungsGrenze : RuntimeException()

fun lösePrädikat(
    prädikat: Aussage,
    variablen: List<GebundeneMengenVariable>,
    kontext: RechenKontext = RechenKontext(),
    budget: Int = 10000,
): PrädikatsLösung {
    require(budget > 0)
    require(variablen.map { it.variable.name }.distinct().size == variablen.size)
    val raum = lösungsRaum(variablen)
    val grenze = LösungsBudget(budget)
    fun definition(p: Aussage): MengenAusdruck {
        val guards = definitionsBedingungen(prädikat).filterNot { it in kontext.annahmen }
        val bedingung = if (guards.isEmpty()) p else Konjunktion(guards + p)
        return if (variablen.isEmpty()) {
            PrädikatsMenge(TypisiertesElement("belegung", "mathematik.tupel", "()"),
                Konjunktion(listOf(Gleichheit(TypisiertesElement("belegung", "mathematik.tupel", "()"), Tupel(emptyList())), bedingung)))
        } else DefinierteMenge(variablen, bedingung)
    }
    fun ergebnis(p: Aussage, m: MengenAusdruck, titel: String, grund: String,
        details: List<UmformungsSchritt> = emptyList(), tabelle: UmformungsTabelle? = null,
        offen: Boolean = false): PrädikatsLösung = PrädikatsLösung(m,
        details + listOf(UmformungsSchritt(p, m, "prädikat.lösen", titel, grund,
            unterschritte = details, tabelle = tabelle)), if (offen) listOf(grund) else emptyList())
    fun kandidaten(p: Aussage, werte: List<List<MathematischesObjekt>>,
        details: List<UmformungsSchritt>): PrädikatsLösung {
        val angenommen = linkedSetOf<MathematischesObjekt>()
        val offen = linkedSetOf<MathematischesObjekt>()
        val prüfungen = mutableListOf<UmformungsSchritt>()
        for (wert in werte) {
            grenze.schritt()
            val bindungen = variablen.mapIndexed { i, v -> v.variable.name to wert[i] }.toMap()
            val probe = Konjunktion(definitionsBedingungen(ersetze(p, bindungen)) + listOf(ersetze(p, bindungen)) + variablen.mapIndexed { i, v ->
                ElementBeziehung(wert[i], v.grundMenge)
            })
            val e = probe.entscheide(kontext)
            val element = if (variablen.size == 1) wert.single() else Tupel(wert)
            when (e.wahrheitswert) {
                Wahrheitswert.Wahr -> angenommen += element
                Wahrheitswert.Lüge -> Unit
                null -> offen += element
            }
            prüfungen += UmformungsSchritt(probe, e.wahrheitswert?.let { WahrheitsKonstante(it == Wahrheitswert.Wahr) } ?: probe,
                "prädikat.probe", "Kandidat und Bereich prüfen", e.begründung.ifBlank {
                    if (e.wahrheitswert == null) "Die Restbedingung bleibt erhalten." else "Originalprädikat und Wertebereiche geprüft."
                })
        }
        val sicher: MengenAusdruck = if (angenommen.isEmpty()) LeereMenge else EndlicheMenge(angenommen)
        val m = if (offen.isEmpty()) sicher else vereinige(listOf(sicher,
            schneide(listOf(EndlicheMenge(offen), definition(p)), raum)))
        return ergebnis(p, m, "Lösungsmenge bestimmen", "Kandidaten mit Originalprädikat und Wertebereichen abgeglichen.", details + prüfungen)
    }
    fun polynomial(term: ZahlAusdruck, x: Variable): List<ZahlAusdruck>? {
        grenze.schritt()
        fun sum(a: List<ZahlAusdruck>, b: List<ZahlAusdruck>) = List(maxOf(a.size, b.size)) { i ->
            vereinfache(addition(a.getOrElse(i) { RationaleZahl.Null }, b.getOrElse(i) { RationaleZahl.Null }), kontext)
        }
        fun produkt(a: List<ZahlAusdruck>, b: List<ZahlAusdruck>): List<ZahlAusdruck>? {
            if (a.size + b.size > 4) return null
            return List(a.size + b.size - 1) { i ->
                vereinfache(addition(a.indices.filter { i - it in b.indices }.map { j -> multiplikation(a[j], b[i-j]) }), kontext)
            }
        }
        if (!term.enthältVariable(x)) return listOf(vereinfache(term, kontext))
        return when (term) {
            is Variable -> if (term.name == x.name) listOf(RationaleZahl.Null, RationaleZahl.Eins) else listOf(term)
            is Addition -> {
                var acc = listOf<ZahlAusdruck>(RationaleZahl.Null)
                for (t in term.summanden) acc = sum(acc, polynomial(t, x) ?: return null)
                acc
            }
            is Multiplikation -> {
                var acc = listOf<ZahlAusdruck>(RationaleZahl.Eins)
                for (t in term.faktoren) acc = produkt(acc, polynomial(t, x) ?: return null) ?: return null
                acc
            }
            is Potenz -> {
                val n = (term.exponent as? RationaleZahl)?.takeIf { it.nenner == java.math.BigInteger.ONE }?.zähler?.takeIf { it in setOf(java.math.BigInteger.ZERO, java.math.BigInteger.ONE, java.math.BigInteger.TWO) }?.toInt()
                if (n !in 0..2) null else {
                    val base = polynomial(term.basis, x) ?: return null
                    var acc = listOf<ZahlAusdruck>(RationaleZahl.Eins)
                    repeat(n!!) { acc = produkt(acc, base) ?: return null }; acc
                }
            }
            // Never cancel an unknown denominator: its original domain must survive.
            is Division -> if (term.divisor is RationaleZahl && !term.divisor.istNull())
                polynomial(term.dividend, x)?.map { vereinfache(Division(it, term.divisor), kontext) } else null
            else -> null
        }
    }
    fun solve(p: Aussage): PrädikatsLösung {
        grenze.schritt()
        val lage = prüfeDefinitionslage(p, kontext)
        if (lage.wahrheitswert == Wahrheitswert.Lüge) return ergebnis(p, LeereMenge,
            "Definitionsbedingung verletzt", lage.begründung)
        if (lage.wahrheitswert == null && !variablen.all { it.grundMenge is EndlicheMenge || it.grundMenge == LeereMenge })
            return ergebnis(p, definition(Konjunktion(definitionsBedingungen(p) + p)),
                "Definitionsbedingungen erhalten", "Die ursprünglichen Definitionsbedingungen bleiben Teil der Lösungsmenge.", offen = true)
        val entscheidung = if (lage.wahrheitswert == Wahrheitswert.Wahr) p.entscheide(kontext)
            else AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
        if (entscheidung.wahrheitswert != null) return ergebnis(p,
            if (entscheidung.wahrheitswert == Wahrheitswert.Wahr) raum else LeereMenge,
            "Prädikat entscheiden", "Die Bedingung gilt für alle beziehungsweise keine zulässige Belegung.")
        if (variablen.isEmpty()) return ergebnis(p, definition(p), "Parameterbedingung erhalten",
            "Keine Zielvariable vorhanden; die Lösungsmenge hängt von der Parameterbedingung ab.", offen = true)
        if (variablen.all { it.grundMenge is EndlicheMenge || it.grundMenge == LeereMenge }) {
            var belege = listOf(emptyList<MathematischesObjekt>())
            for (v in variablen) {
                val elemente = (v.grundMenge as? EndlicheMenge)?.elemente.orEmpty()
                if (belege.size.toLong() * elemente.size > grenze.rest) throw LösungsGrenze()
                belege = belege.flatMap { prefix -> elemente.map { prefix + it } }
            }
            return kandidaten(p, belege, emptyList())
        }
        if (p is Disjunktion || p is Negation) {
            val teile = if (p is Disjunktion) p.aussagen.map(::solve) else listOf(solve((p as Negation).aussage))
            val menge = if (p is Disjunktion) vereinige(teile.map { it.menge }) else mengenDifferenz(raum, teile.single().menge)
            return ergebnis(p, menge, "Logische Fälle verbinden", "Vereinigung beziehungsweise Komplement im zulässigen Raum.", teile.flatMap { it.schritte })
        }
        if (p is Konjunktion && variablen.size > 1 && p.aussagen.all { it is Gleichheit }) {
            val gleichungen = p.aussagen.filterIsInstance<Gleichheit>()
            val namen = variablen.map { it.variable.name }
            val koeff = mutableListOf<List<ZahlAusdruck>>()
            val rhs = mutableListOf<ZahlAusdruck>()
            for (eq in gleichungen) {
                val term = subtraktion(eq.links as? ZahlAusdruck ?: break, eq.rechts as? ZahlAusdruck ?: break)
                val nullen = namen.associateWith { RationaleZahl.Null }
                val konstant = vereinfache(ersetze(term, nullen), kontext) as? RationaleZahl ?: break
                val zeile = mutableListOf<ZahlAusdruck>()
                var rest: ZahlAusdruck = term
                for (v in variablen) {
                    val pol = polynomial(rest, v.variable) ?: break
                    if (pol.size > 2) break
                    val a = vereinfache(pol.getOrElse(1) { RationaleZahl.Null }, kontext) as? RationaleZahl ?: break
                    zeile += a
                    rest = pol[0]
                }
                if (zeile.size != variablen.size || vereinfache(rest, kontext) != konstant) break
                koeff += zeile; rhs += -konstant
            }
            if (koeff.size == gleichungen.size) {
                grenze.schritt()
                val kosten = koeff.size.toLong() * namen.size * namen.size
                if (kosten > grenze.rest) throw LösungsGrenze()
                grenze.rest -= kosten.toInt()
                val gauss = loeseLinearesSystem(Matrix(koeff), SpaltenVektor(rhs), namen)
                val details = listOf(UmformungsSchritt(gauss.verlauf.eingabe, gauss.verlauf.ergebnis,
                    "algebra.gauss", "Gaußverfahren", "Elementare Zeilenoperationen erhalten die Lösungsmenge.",
                    unterschritte = gauss.schritte,
                    tabelle = gauss.verlauf.alsMatrixTabelle(namen + "b", 1)))
                when (val l = gauss.loesung) {
                    KeineLineareLoesung -> return ergebnis(p, LeereMenge, "Widerspruch im Gleichungssystem", "Eine Nullzeile besitzt eine von null verschiedene rechte Seite.", details)
                    is EindeutigeLineareLoesung -> return kandidaten(p, listOf(l.loesungsVektor.werte), details)
                    is ParametrischeLineareLoesung -> {
                        val m = gauss.reduzierteErweiterteMatrix
                        val bed = Konjunktion(m.zeilen.mapNotNull { z ->
                            val pivot = variablen.indices.firstOrNull { z[it] != RationaleZahl.Null } ?: return@mapNotNull null
                            Gleichheit(variablen[pivot].variable, vereinfache(subtraktion(z.last(), addition(
                                variablen.indices.filter { it != pivot }.map { i -> multiplikation(z[i], variablen[i].variable) })), kontext))
                        })
                        return ergebnis(p, DefinierteMenge(variablen, bed), "Freie Variablen bestimmen", "Die reduzierte Form beschreibt alle Lösungen innerhalb der ursprünglichen Wertebereiche.", details)
                    }
                }
            }
        }
        if (p is Konjunktion) {
            val teile = p.aussagen.map(::solve)
            return ergebnis(p, schneide(teile.map { it.menge }, raum), "Gemeinsame Bedingungen", "Schnitt der Lösungsmengen im selben Variablenraum.", teile.flatMap { it.schritte })
        }
        if (variablen.size == 1 && (p is Gleichheit || p is Vergleich)) {
            val x = variablen.single().variable
            val links = if (p is Gleichheit) p.links as? ZahlAusdruck else (p as Vergleich).links
            val rechts = if (p is Gleichheit) p.rechts as? ZahlAusdruck else (p as Vergleich).rechts
            val pol = if (links != null && rechts != null) polynomial(subtraktion(links, rechts), x)?.toMutableList() else null
            if (pol != null) {
                while (pol.size > 1 && pol.last() == RationaleZahl.Null) pol.removeAt(pol.lastIndex)
                val c = pol[0]
                val a = pol.getOrElse(1) { RationaleZahl.Null }
                if (pol.size == 1) {
                    val bed = if (p is Gleichheit) Gleichheit(c, RationaleZahl.Null) else Vergleich(c, (p as Vergleich).art, RationaleZahl.Null)
                    val e = bed.entscheide(kontext)
                    return ergebnis(p, when (e.wahrheitswert) {
                        Wahrheitswert.Wahr -> raum
                        Wahrheitswert.Lüge -> LeereMenge
                        null -> definition(bed)
                    }, "Konstante Bedingung prüfen", "Keine Zielvariable verbleibt in der normalisierten Bedingung.")
                }
                if (pol.size == 2 && a is RationaleZahl && !a.istNull()) {
                    val wert = vereinfache(Division(negation(c), a), kontext)
                    val normal: Aussage = if (p is Gleichheit) Gleichheit(x, wert) else {
                        val art = (p as Vergleich).art
                        val neu = if (a < RationaleZahl.Null) when (art) {
                            VergleichsArt.Kleiner -> VergleichsArt.Größer
                            VergleichsArt.KleinerGleich -> VergleichsArt.GrößerGleich
                            VergleichsArt.Größer -> VergleichsArt.Kleiner
                            VergleichsArt.GrößerGleich -> VergleichsArt.KleinerGleich
                        } else art
                        Vergleich(x, neu, wert)
                    }
                    val gesammelt = if (p is Gleichheit) Gleichheit(addition(multiplikation(a,x),c), RationaleZahl.Null)
                        else Vergleich(addition(multiplikation(a,x),c), (p as Vergleich).art, RationaleZahl.Null)
                    val isoliert = if (p is Gleichheit) Gleichheit(multiplikation(a,x), negation(c))
                        else Vergleich(multiplikation(a,x), (p as Vergleich).art, negation(c))
                    val details = listOf(
                        UmformungsSchritt(p, gesammelt, "algebra.sammeln", "Terme zusammenfassen", "Beide Seiten auf eine Seite bringen und Koeffizienten sammeln.", beziehung = SchrittBeziehung.Äquivalenz),
                        UmformungsSchritt(gesammelt, isoliert, "algebra.konstante", "Konstante abziehen", "Auf beiden Seiten dieselbe Konstante abziehen.", beziehung = SchrittBeziehung.Äquivalenz),
                        UmformungsSchritt(isoliert, normal, "algebra.dividieren", "Durch Koeffizienten teilen", "Durch den von null verschiedenen Koeffizienten teilen; bei negativem Faktor das Ungleichheitszeichen umkehren.", beziehung = SchrittBeziehung.Äquivalenz))
                    return if (p is Gleichheit) kandidaten(p, listOf(listOf(wert)), details)
                        else ergebnis(p, variablen.single().grundMenge.fundamentalerZahlbereichOderNull()?.takeIf { it.istGeordnet }?.let { traeger ->
                            val v = normal as Vergleich
                            when (v.art) {
                                VergleichsArt.Kleiner -> beschraenkteZahlmenge(traeger, obereGrenze = wert)
                                VergleichsArt.KleinerGleich -> beschraenkteZahlmenge(traeger, obereGrenze = wert, obereGrenzeEnthalten = true)
                                VergleichsArt.Größer -> beschraenkteZahlmenge(traeger, untereGrenze = wert)
                                VergleichsArt.GrößerGleich -> beschraenkteZahlmenge(traeger, untereGrenze = wert, untereGrenzeEnthalten = true)
                            }
                        } ?: definition(normal), "Ungleichung auflösen", "Die isolierte Schranke gilt im ursprünglichen Wertebereich.", details)
                }
                if (pol.size == 2 && p is Gleichheit) {
                    val nz = Ungleichheit(a, RationaleZahl.Null)
                    val nullA = Gleichheit(a, RationaleZahl.Null)
                    val wert = Division(negation(c), a)
                    val fälle = Disjunktion(listOf(
                        Konjunktion(listOf(nz, Gleichheit(x, wert))),
                        Konjunktion(listOf(nullA, Gleichheit(c, RationaleZahl.Null)))))
                    return ergebnis(p, definition(fälle), "Parameterfälle unterscheiden", "Bei a≠0 teilen; bei a=0 gilt die konstante Restbedingung. Die Fallbedingungen bleiben erhalten.",
                        listOf(UmformungsSchritt(p, fälle, "algebra.parameterfälle", "Nullfall getrennt behandeln", "Keine Division ohne Nichtnullbedingung.", bedingungen = listOf(nz))))
                }
                if (pol.size == 3 && p is Gleichheit && pol.all { it is RationaleZahl }) {
                    val b = pol[1] as RationaleZahl; val q = pol[2] as RationaleZahl; val d = pol[0] as RationaleZahl
                    val disk = b*b - RationaleZahl.von(4)*q*d
                    if (disk < RationaleZahl.Null && variablen.single().grundMenge in setOf(ReelleZahlen, RationaleZahlen, GanzeZahlen, NatürlicheZahlen))
                        return ergebnis(p, LeereMenge, "Diskriminante prüfen", "Die Diskriminante ist negativ; es gibt keine reellen Lösungen.")
                    if (disk >= RationaleZahl.Null || variablen.single().grundMenge == KomplexeZahlen) {
                        val wurzel = wurzel(disk, kontext)
                        val werte = listOf(vereinfache(Division(addition(-b, wurzel), RationaleZahl.von(2)*q), kontext),
                            vereinfache(Division(subtraktion(-b, wurzel), RationaleZahl.von(2)*q), kontext)).distinct()
                        val details = listOf(UmformungsSchritt(p, Gleichheit(AllgemeinerParameter("D"), disk),
                            "algebra.diskriminante", "Diskriminante berechnen", "D = b² − 4ac; anschließend x = (−b ± √D)/(2a).", beziehung = SchrittBeziehung.Zusatzrechnung))
                        return kandidaten(p, werte.map { listOf(it) }, details)
                    }
                }
            }
        }
        return ergebnis(p, definition(vereinfacheAussage(p, kontext)), "Restprädikat erhalten",
            "Für dieses Prädikat ist kein weiter auflösendes Verfahren verfügbar; die Mengendefinition bleibt exakt.", offen = true)
    }
    return try { solve(prädikat) } catch (_: LösungsGrenze) {
        ergebnis(prädikat, definition(prädikat), "Rechenbudget erreicht",
            "Die vollständige Mengendefinition bleibt erhalten; keine Teilliste wird als vollständige Lösung ausgegeben.", offen = true)
    }
}

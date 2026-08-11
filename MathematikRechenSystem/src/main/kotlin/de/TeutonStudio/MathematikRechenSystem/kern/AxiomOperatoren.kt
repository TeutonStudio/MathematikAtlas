package de.TeutonStudio.MathematikRechenSystem.kern

enum class AxiomArgumentArt {
    OBJEKT,
    MENGE,
    METHODE,
    PRAEDIKAT,
}

data class AxiomArgument(
    val rolle: String,
    val art: AxiomArgumentArt,
    val stelligkeit: Int? = null,
) {
    init {
        require(rolle.isNotBlank())
        require(stelligkeit == null || stelligkeit >= 0)
    }
}

data class AxiomSystemDefinition(
    val stabileId: String,
    val titel: String,
    val axiomIds: Set<String>,
    val vorausgesetzteSysteme: Set<String> = emptySet(),
)

data class AxiomInstanz(
    val axiomId: String,
    val titel: String,
    val systeme: Set<String>,
    val formelLatex: String,
    val auswertbareFormel: Aussage? = null,
) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis =
        auswertbareFormel?.entscheide(kontext)
            ?: AussageErgebnis(
                wahrheitswert = null,
                status = EntscheidungsStatus.Unbekannt,
                begründung = "Das Axiom bleibt für symbolische oder unendliche Träger symbolisch.",
            )

    override fun zuLatex(): String = formelLatex
}

data class AxiomOperatorDefinition(
    val stabileId: String,
    val titel: String,
    val systeme: Set<String>,
    val kategorie: String,
    val symbolLatex: String,
    val argumente: List<AxiomArgument>,
    val suchbegriffe: Set<String> = emptySet(),
    val auswerter: (List<MathematischesObjekt>) -> AxiomInstanz,
) {
    init {
        require(stabileId.startsWith("axiom."))
        require(titel.isNotBlank())
        require(systeme.isNotEmpty())
        require(argumente.map(AxiomArgument::rolle).distinct().size == argumente.size)
    }

    fun werteAus(argumenteNachRolle: Map<String, MathematischesObjekt>): AxiomInstanz {
        val werte = argumente.map { argument ->
            val wert = argumenteNachRolle[argument.rolle]
                ?: error("Für das Axiom '$titel' fehlt der Eingang '${argument.rolle}'.")
            when (argument.art) {
                AxiomArgumentArt.OBJEKT -> Unit
                AxiomArgumentArt.MENGE -> require(wert is MengenAusdruck) {
                    "Der Eingang '${argument.rolle}' des Axioms '$titel' benötigt eine Menge."
                }
                AxiomArgumentArt.METHODE,
                AxiomArgumentArt.PRAEDIKAT,
                -> {
                    val methode = wert as? Methode
                        ?: error("Der Eingang '${argument.rolle}' des Axioms '$titel' benötigt eine Methode.")
                    argument.stelligkeit?.let { erwartet ->
                        require(methode.argumentAnzahl == erwartet) {
                            "Die Methode '${methode.name}' am Eingang '${argument.rolle}' muss $erwartet Argumente besitzen."
                        }
                    }
                    if (argument.art == AxiomArgumentArt.PRAEDIKAT) {
                        require(methode.istPrädikat()) {
                            "Die Methode '${methode.name}' am Eingang '${argument.rolle}' muss ein Prädikat sein."
                        }
                    }
                }
            }
            wert
        }
        return auswerter(werte)
    }
}

object AxiomOperatoren {
    const val PRAEDIKAT_SEITE = "axiome"

    private fun a(rolle: String, art: AxiomArgumentArt, stelligkeit: Int? = null) =
        AxiomArgument(rolle, art, stelligkeit)

    private fun d(
        id: String,
        titel: String,
        systeme: Set<String>,
        kategorie: String,
        symbol: String,
        suchbegriffe: Set<String> = emptySet(),
        vararg argumente: AxiomArgument,
        auswerter: (List<MathematischesObjekt>) -> AxiomInstanz,
    ) = AxiomOperatorDefinition(
        stabileId = id,
        titel = titel,
        systeme = systeme,
        kategorie = kategorie,
        symbolLatex = symbol,
        argumente = argumente.toList(),
        suchbegriffe = suchbegriffe + setOf("Axiom", "Axiome") + systeme,
        auswerter = auswerter,
    )

    private fun instanz(
        id: String,
        titel: String,
        systeme: Set<String>,
        latex: String,
        auswertbar: Aussage? = null,
    ) = AxiomInstanz(id, titel, systeme, latex, auswertbar)

    private fun element(element: MathematischesObjekt, menge: MengenAusdruck): Aussage =
        MengenRelationRechner.erzeuge(MengenRelationsOperator.ELEMENT, element, menge)

    private fun konjunktion(aussagen: Iterable<Aussage>): Aussage {
        val liste = aussagen.toList()
        return when (liste.size) {
            0 -> WahrheitsKonstante(true)
            1 -> liste.single()
            else -> Konjunktion(liste)
        }
    }

    private fun methode(werte: List<MathematischesObjekt>, index: Int): Methode =
        werte[index] as Methode

    private fun menge(werte: List<MathematischesObjekt>, index: Int): MengenAusdruck =
        werte[index] as MengenAusdruck

    private fun aufruf(methode: Methode, vararg argumente: MathematischesObjekt): MathematischesObjekt =
        methode.wendeAn(argumente.toList())

    private fun prädikat(methode: Methode, vararg argumente: MathematischesObjekt): Aussage =
        aufruf(methode, *argumente) as? Aussage
            ?: error("Die Methode '${methode.name}' liefert bei der Axiomauswertung keine Aussage.")

    private fun <T> endlich(menge: MengenAusdruck, block: (List<MathematischesObjekt>) -> T): T? =
        (menge as? EndlicheMenge)?.elemente?.sortedBy(::strukturellerSchlüssel)?.let(block)

    private fun assoziativAussage(menge: MengenAusdruck, op: Methode): Aussage? = endlich(menge) { e ->
        konjunktion(e.flatMap { x -> e.flatMap { y -> e.map { z ->
            Gleichheit(
                aufruf(op, aufruf(op, x, y), z),
                aufruf(op, x, aufruf(op, y, z)),
            )
        } } })
    }

    private fun kommutativAussage(menge: MengenAusdruck, op: Methode): Aussage? = endlich(menge) { e ->
        konjunktion(e.flatMap { x -> e.map { y -> Gleichheit(aufruf(op, x, y), aufruf(op, y, x)) } })
    }

    private fun neutralAussage(menge: MengenAusdruck, op: Methode, neutral: MathematischesObjekt): Aussage? =
        endlich(menge) { e ->
            konjunktion(listOf(element(neutral, menge)) + e.flatMap { x ->
                listOf(
                    Gleichheit(aufruf(op, neutral, x), x),
                    Gleichheit(aufruf(op, x, neutral), x),
                )
            })
        }

    private fun inversAussage(
        menge: MengenAusdruck,
        op: Methode,
        neutral: MathematischesObjekt,
        invers: Methode,
        ausnahme: MathematischesObjekt? = null,
    ): Aussage? = endlich(menge) { e ->
        konjunktion(e.map { x ->
            val kern = konjunktion(
                listOf(
                    Gleichheit(aufruf(op, aufruf(invers, x), x), neutral),
                    Gleichheit(aufruf(op, x, aufruf(invers, x)), neutral),
                ),
            )
            ausnahme?.let { Implikation(Ungleichheit(x, it), kern) } ?: kern
        })
    }

    private fun distributivAussage(
        menge: MengenAusdruck,
        plus: Methode,
        mal: Methode,
        links: Boolean,
    ): Aussage? = endlich(menge) { e ->
        konjunktion(e.flatMap { x -> e.flatMap { y -> e.map { z ->
            if (links) {
                Gleichheit(
                    aufruf(mal, x, aufruf(plus, y, z)),
                    aufruf(plus, aufruf(mal, x, y), aufruf(mal, x, z)),
                )
            } else {
                Gleichheit(
                    aufruf(mal, aufruf(plus, x, y), z),
                    aufruf(plus, aufruf(mal, x, z), aufruf(mal, y, z)),
                )
            }
        } } })
    }

    private fun nullAbsorbierendAussage(
        menge: MengenAusdruck,
        mal: Methode,
        nullElement: MathematischesObjekt,
    ): Aussage? = endlich(menge) { e ->
        konjunktion(e.flatMap { x ->
            listOf(
                Gleichheit(aufruf(mal, nullElement, x), nullElement),
                Gleichheit(aufruf(mal, x, nullElement), nullElement),
            )
        })
    }

    private fun keineNullteilerAussage(
        menge: MengenAusdruck,
        mal: Methode,
        nullElement: MathematischesObjekt,
    ): Aussage? = endlich(menge) { e ->
        konjunktion(e.flatMap { x -> e.map { y ->
            Implikation(
                Gleichheit(aufruf(mal, x, y), nullElement),
                Disjunktion(listOf(Gleichheit(x, nullElement), Gleichheit(y, nullElement))),
            )
        } })
    }

    private fun ringAussage(
        menge: MengenAusdruck,
        plus: Methode,
        mal: Methode,
        nullElement: MathematischesObjekt,
        eins: MathematischesObjekt?,
        negativ: Methode,
        multiplikativKommutativ: Boolean = false,
    ): Aussage? = runCatching {
        val teile = buildList<Aussage> {
            assoziativAussage(menge, plus)?.let(::add)
            kommutativAussage(menge, plus)?.let(::add)
            neutralAussage(menge, plus, nullElement)?.let(::add)
            inversAussage(menge, plus, nullElement, negativ)?.let(::add)
            assoziativAussage(menge, mal)?.let(::add)
            eins?.let { neutralAussage(menge, mal, it)?.let(::add) }
            distributivAussage(menge, plus, mal, links = true)?.let(::add)
            distributivAussage(menge, plus, mal, links = false)?.let(::add)
            if (multiplikativKommutativ) kommutativAussage(menge, mal)?.let(::add)
        }
        if (teile.isEmpty()) null else konjunktion(teile)
    }.getOrNull()

    private val relationAxiome: List<AxiomOperatorDefinition> = listOf(
        d(
            "axiom.relation.reflexiv", "Reflexivität", setOf("relation"), "Relationen",
            "\\forall x\\in M:\\;R(x,x)", setOf("reflexiv"),
            a("menge", AxiomArgumentArt.MENGE), a("relation", AxiomArgumentArt.PRAEDIKAT, 2),
        ) { w ->
            val m = menge(w, 0); val r = methode(w, 1)
            instanz("axiom.relation.reflexiv", "Reflexivität", setOf("relation"),
                "\\forall x\\in${m.zuLatex()}:\\;${r.name}(x,x)",
                runCatching { endlich(m) { e -> konjunktion(e.map { prädikat(r, it, it) }) } }.getOrNull())
        },
        d(
            "axiom.relation.irreflexiv", "Irreflexivität", setOf("relation"), "Relationen",
            "\\forall x\\in M:\\;\\neg R(x,x)", setOf("irreflexiv"),
            a("menge", AxiomArgumentArt.MENGE), a("relation", AxiomArgumentArt.PRAEDIKAT, 2),
        ) { w ->
            val m = menge(w, 0); val r = methode(w, 1)
            instanz("axiom.relation.irreflexiv", "Irreflexivität", setOf("relation"),
                "\\forall x\\in${m.zuLatex()}:\\;\\neg ${r.name}(x,x)",
                runCatching { endlich(m) { e -> konjunktion(e.map { Negation(prädikat(r, it, it)) }) } }.getOrNull())
        },
        d(
            "axiom.relation.symmetrisch", "Symmetrie", setOf("relation"), "Relationen",
            "R(x,y)\\Rightarrow R(y,x)", setOf("symmetrisch"),
            a("menge", AxiomArgumentArt.MENGE), a("relation", AxiomArgumentArt.PRAEDIKAT, 2),
        ) { w ->
            val m = menge(w, 0); val r = methode(w, 1)
            instanz("axiom.relation.symmetrisch", "Symmetrie", setOf("relation"),
                "\\forall x,y\\in${m.zuLatex()}:\\;${r.name}(x,y)\\Rightarrow${r.name}(y,x)",
                runCatching { endlich(m) { e -> konjunktion(e.flatMap { x -> e.map { y -> Implikation(prädikat(r, x, y), prädikat(r, y, x)) } }) } }.getOrNull())
        },
        d(
            "axiom.relation.antisymmetrisch", "Antisymmetrie", setOf("relation"), "Relationen",
            "R(x,y)\\land R(y,x)\\Rightarrow x=y", setOf("antisymmetrisch"),
            a("menge", AxiomArgumentArt.MENGE), a("relation", AxiomArgumentArt.PRAEDIKAT, 2),
        ) { w ->
            val m = menge(w, 0); val r = methode(w, 1)
            instanz("axiom.relation.antisymmetrisch", "Antisymmetrie", setOf("relation"),
                "\\forall x,y\\in${m.zuLatex()}:\\;(${r.name}(x,y)\\land${r.name}(y,x))\\Rightarrow x=y",
                runCatching { endlich(m) { e -> konjunktion(e.flatMap { x -> e.map { y -> Implikation(konjunktion(listOf(prädikat(r, x, y), prädikat(r, y, x))), Gleichheit(x, y)) } }) } }.getOrNull())
        },
        d(
            "axiom.relation.asymmetrisch", "Asymmetrie", setOf("relation"), "Relationen",
            "R(x,y)\\Rightarrow\\neg R(y,x)", setOf("asymmetrisch"),
            a("menge", AxiomArgumentArt.MENGE), a("relation", AxiomArgumentArt.PRAEDIKAT, 2),
        ) { w ->
            val m = menge(w, 0); val r = methode(w, 1)
            instanz("axiom.relation.asymmetrisch", "Asymmetrie", setOf("relation"),
                "\\forall x,y\\in${m.zuLatex()}:\\;${r.name}(x,y)\\Rightarrow\\neg${r.name}(y,x)",
                runCatching { endlich(m) { e -> konjunktion(e.flatMap { x -> e.map { y -> Implikation(prädikat(r, x, y), Negation(prädikat(r, y, x))) } }) } }.getOrNull())
        },
        d(
            "axiom.relation.transitiv", "Transitivität", setOf("relation"), "Relationen",
            "R(x,y)\\land R(y,z)\\Rightarrow R(x,z)", setOf("transitiv"),
            a("menge", AxiomArgumentArt.MENGE), a("relation", AxiomArgumentArt.PRAEDIKAT, 2),
        ) { w ->
            val m = menge(w, 0); val r = methode(w, 1)
            instanz("axiom.relation.transitiv", "Transitivität", setOf("relation"),
                "\\forall x,y,z\\in${m.zuLatex()}:\\;(${r.name}(x,y)\\land${r.name}(y,z))\\Rightarrow${r.name}(x,z)",
                runCatching { endlich(m) { e -> konjunktion(e.flatMap { x -> e.flatMap { y -> e.map { z -> Implikation(konjunktion(listOf(prädikat(r, x, y), prädikat(r, y, z))), prädikat(r, x, z)) } } }) } }.getOrNull())
        },
        d(
            "axiom.relation.total", "Totalität", setOf("relation"), "Relationen",
            "R(x,y)\\lor R(y,x)", setOf("total", "totalordnung"),
            a("menge", AxiomArgumentArt.MENGE), a("relation", AxiomArgumentArt.PRAEDIKAT, 2),
        ) { w ->
            val m = menge(w, 0); val r = methode(w, 1)
            instanz("axiom.relation.total", "Totalität", setOf("relation"),
                "\\forall x,y\\in${m.zuLatex()}:\\;${r.name}(x,y)\\lor${r.name}(y,x)",
                runCatching { endlich(m) { e -> konjunktion(e.flatMap { x -> e.map { y -> Disjunktion(listOf(prädikat(r, x, y), prädikat(r, y, x))) } }) } }.getOrNull())
        },
    )

    private val peanoAxiome: List<AxiomOperatorDefinition> = listOf(
        d(
            "axiom.peano.null", "Peano 1 · Null", setOf("peano"), "Natürliche Zahlen · Peano",
            "0\\in N", setOf("Peano 1", "Null gehört zu N"),
            a("menge", AxiomArgumentArt.MENGE), a("null", AxiomArgumentArt.OBJEKT),
        ) { w ->
            val n = menge(w, 0); val zero = w[1]
            instanz("axiom.peano.null", "Peano 1 · Null", setOf("peano"),
                "${zero.zuLatex()}\\in${n.zuLatex()}", element(zero, n))
        },
        d(
            "axiom.peano.nachfolgerAbgeschlossen", "Peano 2 · Nachfolgerabschluss", setOf("peano"), "Natürliche Zahlen · Peano",
            "n\\in N\\Rightarrow S(n)\\in N", setOf("Peano 2", "Nachfolger"),
            a("menge", AxiomArgumentArt.MENGE), a("nachfolger", AxiomArgumentArt.METHODE, 1),
        ) { w ->
            val n = menge(w, 0); val s = methode(w, 1)
            instanz("axiom.peano.nachfolgerAbgeschlossen", "Peano 2 · Nachfolgerabschluss", setOf("peano"),
                "\\forall n\\in${n.zuLatex()}:\\;${s.name}(n)\\in${n.zuLatex()}",
                runCatching { endlich(n) { e -> konjunktion(e.map { element(aufruf(s, it), n) }) } }.getOrNull())
        },
        d(
            "axiom.peano.nullKeinNachfolger", "Peano 3 · Null ist kein Nachfolger", setOf("peano"), "Natürliche Zahlen · Peano",
            "S(n)\\neq0", setOf("Peano 3"),
            a("menge", AxiomArgumentArt.MENGE), a("null", AxiomArgumentArt.OBJEKT), a("nachfolger", AxiomArgumentArt.METHODE, 1),
        ) { w ->
            val n = menge(w, 0); val zero = w[1]; val s = methode(w, 2)
            instanz("axiom.peano.nullKeinNachfolger", "Peano 3 · Null ist kein Nachfolger", setOf("peano"),
                "\\forall n\\in${n.zuLatex()}:\\;${s.name}(n)\\neq${zero.zuLatex()}",
                runCatching { endlich(n) { e -> konjunktion(e.map { Ungleichheit(aufruf(s, it), zero) }) } }.getOrNull())
        },
        d(
            "axiom.peano.nachfolgerInjektiv", "Peano 4 · Nachfolger injektiv", setOf("peano"), "Natürliche Zahlen · Peano",
            "S(m)=S(n)\\Rightarrow m=n", setOf("Peano 4", "injektiv"),
            a("menge", AxiomArgumentArt.MENGE), a("nachfolger", AxiomArgumentArt.METHODE, 1),
        ) { w ->
            val n = menge(w, 0); val s = methode(w, 1)
            instanz("axiom.peano.nachfolgerInjektiv", "Peano 4 · Nachfolger injektiv", setOf("peano"),
                "\\forall m,n\\in${n.zuLatex()}:\\;${s.name}(m)=${s.name}(n)\\Rightarrow m=n",
                runCatching { endlich(n) { e -> konjunktion(e.flatMap { x -> e.map { y -> Implikation(Gleichheit(aufruf(s, x), aufruf(s, y)), Gleichheit(x, y)) } }) } }.getOrNull())
        },
        d(
            "axiom.peano.induktion", "Peano 5 · Induktion", setOf("peano"), "Natürliche Zahlen · Peano",
            "P(0)\\land(P(n)\\Rightarrow P(S(n)))\\Rightarrow\\forall n\\,P(n)", setOf("Peano 5", "Induktion", "Induktionsschema"),
            a("menge", AxiomArgumentArt.MENGE), a("null", AxiomArgumentArt.OBJEKT),
            a("nachfolger", AxiomArgumentArt.METHODE, 1), a("praedikat", AxiomArgumentArt.PRAEDIKAT, 1),
        ) { w ->
            val n = menge(w, 0); val zero = w[1]; val s = methode(w, 2); val p = methode(w, 3)
            val auswertbar = runCatching { endlich(n) { e ->
                val basis = prädikat(p, zero)
                val schritt = konjunktion(e.map { x -> Implikation(prädikat(p, x), prädikat(p, aufruf(s, x))) })
                val ziel = konjunktion(e.map { prädikat(p, it) })
                Implikation(konjunktion(listOf(basis, schritt)), ziel)
            } }.getOrNull()
            instanz("axiom.peano.induktion", "Peano 5 · Induktion", setOf("peano"),
                "(${p.name}(${zero.zuLatex()})\\land\\forall n\\in${n.zuLatex()}:(${p.name}(n)\\Rightarrow${p.name}(${s.name}(n))))\\Rightarrow\\forall n\\in${n.zuLatex()}:${p.name}(n)",
                auswertbar)
        },
    )

    private val mengenlehreAxiome: List<AxiomOperatorDefinition> = listOf(
        d(
            "axiom.zf.extensionalitaet", "Extensionalitätsaxiom", setOf("zf", "zfc"), "Mengenlehre · ZF",
            "(\\forall z:z\\in x\\Leftrightarrow z\\in y)\\Rightarrow x=y", setOf("Extensionalität", "ZF"),
            a("x", AxiomArgumentArt.MENGE), a("y", AxiomArgumentArt.MENGE),
        ) { w ->
            val x = menge(w, 0); val y = menge(w, 1)
            val auswertbar = if (x is EndlicheMenge && y is EndlicheMenge) WahrheitsKonstante(true) else null
            instanz("axiom.zf.extensionalitaet", "Extensionalitätsaxiom", setOf("zf", "zfc"),
                "(\\forall z:\\;z\\in${x.zuLatex()}\\Leftrightarrow z\\in${y.zuLatex()})\\Rightarrow${x.zuLatex()}=${y.zuLatex()}", auswertbar)
        },
        d(
            "axiom.zf.leereMenge", "Axiom der leeren Menge", setOf("zf", "zfc"), "Mengenlehre · ZF",
            "\\exists x\\,\\forall y:y\\notin x", setOf("leere Menge", "empty set"),
        ) { _ -> instanz("axiom.zf.leereMenge", "Axiom der leeren Menge", setOf("zf", "zfc"), "\\exists x\\,\\forall y:\\;y\\notin x", WahrheitsKonstante(true)) },
        d(
            "axiom.zf.paarmenge", "Paarmengenaxiom", setOf("zf", "zfc"), "Mengenlehre · ZF",
            "\\exists z\\,\\forall w:w\\in z\\Leftrightarrow(w=x\\lor w=y)", setOf("Paarbildung", "pairing"),
            a("x", AxiomArgumentArt.OBJEKT), a("y", AxiomArgumentArt.OBJEKT),
        ) { w -> instanz("axiom.zf.paarmenge", "Paarmengenaxiom", setOf("zf", "zfc"),
            "\\exists z\\,\\forall w:\\;w\\in z\\Leftrightarrow(w=${w[0].zuLatex()}\\lor w=${w[1].zuLatex()})", WahrheitsKonstante(true)) },
        d(
            "axiom.zf.vereinigung", "Vereinigungsaxiom", setOf("zf", "zfc"), "Mengenlehre · ZF",
            "z\\in\\bigcup x\\Leftrightarrow\\exists w\\in x:z\\in w", setOf("Vereinigung", "union axiom"),
            a("x", AxiomArgumentArt.MENGE),
        ) { w -> val x = menge(w, 0); instanz("axiom.zf.vereinigung", "Vereinigungsaxiom", setOf("zf", "zfc"),
            "\\exists y\\,\\forall z:\\;z\\in y\\Leftrightarrow\\exists w\\in${x.zuLatex()}:z\\in w") },
        d(
            "axiom.zf.potenzmenge", "Potenzmengenaxiom", setOf("zf", "zfc"), "Mengenlehre · ZF",
            "z\\in\\mathcal P(x)\\Leftrightarrow z\\subseteq x", setOf("Potenzmenge", "power set"),
            a("x", AxiomArgumentArt.MENGE),
        ) { w -> val x = menge(w, 0); instanz("axiom.zf.potenzmenge", "Potenzmengenaxiom", setOf("zf", "zfc"),
            "\\exists y\\,\\forall z:\\;z\\in y\\Leftrightarrow z\\subseteq${x.zuLatex()}") },
        d(
            "axiom.zf.unendlichkeit", "Unendlichkeitsaxiom", setOf("zf", "zfc"), "Mengenlehre · ZF",
            "\\exists x:(\\varnothing\\in x\\land y\\in x\\Rightarrow y\\cup\\{y\\}\\in x)", setOf("Unendlichkeit", "infinity"),
        ) { _ -> instanz("axiom.zf.unendlichkeit", "Unendlichkeitsaxiom", setOf("zf", "zfc"),
            "\\exists x:\\;\\varnothing\\in x\\land\\forall y\\in x:\\;y\\cup\\{y\\}\\in x") },
        d(
            "axiom.zf.aussonderung", "Aussonderungsschema", setOf("zf", "zfc"), "Mengenlehre · ZF · Schemata",
            "x\\in V\\Leftrightarrow(x\\in W\\land P(x))", setOf("Separation", "Aussonderung", "Schema"),
            a("menge", AxiomArgumentArt.MENGE), a("praedikat", AxiomArgumentArt.PRAEDIKAT, 1),
        ) { w ->
            val m = menge(w, 0); val p = methode(w, 1)
            instanz("axiom.zf.aussonderung", "Aussonderungsschema", setOf("zf", "zfc"),
                "\\exists V\\,\\forall x:\\;x\\in V\\Leftrightarrow(x\\in${m.zuLatex()}\\land${p.name}(x))",
                if (m is EndlicheMenge) WahrheitsKonstante(true) else null)
        },
        d(
            "axiom.zf.ersetzung", "Ersetzungsschema", setOf("zf", "zfc"), "Mengenlehre · ZF · Schemata",
            "(\\forall x\\exists!y:R(x,y))\\Rightarrow\\exists V", setOf("Replacement", "Ersetzung", "Schema"),
            a("menge", AxiomArgumentArt.MENGE), a("relation", AxiomArgumentArt.PRAEDIKAT, 2),
        ) { w ->
            val m = menge(w, 0); val r = methode(w, 1)
            instanz("axiom.zf.ersetzung", "Ersetzungsschema", setOf("zf", "zfc"),
                "(\\forall x\\in${m.zuLatex()}\\,\\exists!y:\\;${r.name}(x,y))\\Rightarrow\\exists V\\,\\forall y:\\;y\\in V\\Leftrightarrow\\exists x\\in${m.zuLatex()}:${r.name}(x,y)")
        },
        d(
            "axiom.zf.fundierung", "Fundierungsaxiom", setOf("zf", "zfc"), "Mengenlehre · ZF",
            "x\\neq\\varnothing\\Rightarrow\\exists y\\in x:y\\cap x=\\varnothing", setOf("Fundierung", "Regularität", "Foundation"),
            a("x", AxiomArgumentArt.MENGE),
        ) { w -> val x = menge(w, 0); instanz("axiom.zf.fundierung", "Fundierungsaxiom", setOf("zf", "zfc"),
            "${x.zuLatex()}\\neq\\varnothing\\Rightarrow\\exists y\\in${x.zuLatex()}:\\;y\\cap${x.zuLatex()}=\\varnothing") },
        d(
            "axiom.zfc.auswahl", "Auswahlaxiom", setOf("zfc"), "Mengenlehre · ZFC",
            "\\forall A\\in F:A\\neq\\varnothing\\Rightarrow\\exists f\\,f(A)\\in A", setOf("Auswahlaxiom", "choice", "AC"),
            a("familie", AxiomArgumentArt.MENGE),
        ) { w -> val f = menge(w, 0); instanz("axiom.zfc.auswahl", "Auswahlaxiom", setOf("zfc"),
            "(\\forall A\\in${f.zuLatex()}:A\\neq\\varnothing)\\Rightarrow\\exists f:${f.zuLatex()}\\to\\bigcup${f.zuLatex()}\\;\\forall A\\in${f.zuLatex()}:f(A)\\in A") },
    )

    private val algebraAxiome: List<AxiomOperatorDefinition> = listOf(
        d(
            "axiom.algebra.assoziativ", "Assoziativität", setOf("algebra"), "Algebra · Grundaxiome",
            "(a\\circ b)\\circ c=a\\circ(b\\circ c)", setOf("assoziativ"),
            a("menge", AxiomArgumentArt.MENGE), a("operation", AxiomArgumentArt.METHODE, 2),
        ) { w -> val m = menge(w, 0); val op = methode(w, 1); instanz("axiom.algebra.assoziativ", "Assoziativität", setOf("algebra"),
            "\\forall a,b,c\\in${m.zuLatex()}:(${op.name}(a,b))\\mathbin{${op.name}}c=${op.name}(a,${op.name}(b,c))",
            runCatching { assoziativAussage(m, op) }.getOrNull()) },
        d(
            "axiom.algebra.kommutativ", "Kommutativität", setOf("algebra"), "Algebra · Grundaxiome",
            "a\\circ b=b\\circ a", setOf("kommutativ"),
            a("menge", AxiomArgumentArt.MENGE), a("operation", AxiomArgumentArt.METHODE, 2),
        ) { w -> val m = menge(w, 0); val op = methode(w, 1); instanz("axiom.algebra.kommutativ", "Kommutativität", setOf("algebra"),
            "\\forall a,b\\in${m.zuLatex()}:${op.name}(a,b)=${op.name}(b,a)", runCatching { kommutativAussage(m, op) }.getOrNull()) },
        d(
            "axiom.algebra.neutral", "Neutrales Element", setOf("algebra"), "Algebra · Grundaxiome",
            "e\\circ a=a\\circ e=a", setOf("neutral", "Identität"),
            a("menge", AxiomArgumentArt.MENGE), a("operation", AxiomArgumentArt.METHODE, 2), a("neutral", AxiomArgumentArt.OBJEKT),
        ) { w -> val m = menge(w, 0); val op = methode(w, 1); val e = w[2]; instanz("axiom.algebra.neutral", "Neutrales Element", setOf("algebra"),
            "${e.zuLatex()}\\in${m.zuLatex()}\\land\\forall a\\in${m.zuLatex()}:${op.name}(${e.zuLatex()},a)=a=${op.name}(a,${e.zuLatex()})",
            runCatching { neutralAussage(m, op, e) }.getOrNull()) },
        d(
            "axiom.algebra.invers", "Inverse Elemente", setOf("algebra"), "Algebra · Grundaxiome",
            "a^{-1}\\circ a=a\\circ a^{-1}=e", setOf("invers", "inverse"),
            a("menge", AxiomArgumentArt.MENGE), a("operation", AxiomArgumentArt.METHODE, 2),
            a("neutral", AxiomArgumentArt.OBJEKT), a("inverse", AxiomArgumentArt.METHODE, 1),
        ) { w -> val m = menge(w, 0); val op = methode(w, 1); val e = w[2]; val inv = methode(w, 3); instanz("axiom.algebra.invers", "Inverse Elemente", setOf("algebra"),
            "\\forall a\\in${m.zuLatex()}:${op.name}(${inv.name}(a),a)=${e.zuLatex()}=${op.name}(a,${inv.name}(a))",
            runCatching { inversAussage(m, op, e, inv) }.getOrNull()) },
        d(
            "axiom.algebra.linksdistributiv", "Linksdistributivität", setOf("algebra"), "Algebra · Grundaxiome",
            "a(b+c)=ab+ac", setOf("distributiv"),
            a("menge", AxiomArgumentArt.MENGE), a("addition", AxiomArgumentArt.METHODE, 2), a("multiplikation", AxiomArgumentArt.METHODE, 2),
        ) { w -> val m = menge(w, 0); val plus = methode(w, 1); val mal = methode(w, 2); instanz("axiom.algebra.linksdistributiv", "Linksdistributivität", setOf("algebra"),
            "\\forall a,b,c\\in${m.zuLatex()}:${mal.name}(a,${plus.name}(b,c))=${plus.name}(${mal.name}(a,b),${mal.name}(a,c))",
            runCatching { distributivAussage(m, plus, mal, true) }.getOrNull()) },
        d(
            "axiom.algebra.rechtsdistributiv", "Rechtsdistributivität", setOf("algebra"), "Algebra · Grundaxiome",
            "(a+b)c=ac+bc", setOf("distributiv"),
            a("menge", AxiomArgumentArt.MENGE), a("addition", AxiomArgumentArt.METHODE, 2), a("multiplikation", AxiomArgumentArt.METHODE, 2),
        ) { w -> val m = menge(w, 0); val plus = methode(w, 1); val mal = methode(w, 2); instanz("axiom.algebra.rechtsdistributiv", "Rechtsdistributivität", setOf("algebra"),
            "\\forall a,b,c\\in${m.zuLatex()}:${mal.name}(${plus.name}(a,b),c)=${plus.name}(${mal.name}(a,c),${mal.name}(b,c))",
            runCatching { distributivAussage(m, plus, mal, false) }.getOrNull()) },
        d(
            "axiom.algebra.halbgruppe", "Halbgruppe", setOf("halbgruppe"), "Algebra · Gruppen",
            "\\operatorname{Halbgruppe}(M,\\circ)", setOf("semigroup"),
            a("menge", AxiomArgumentArt.MENGE), a("operation", AxiomArgumentArt.METHODE, 2),
        ) { w -> val m = menge(w, 0); val op = methode(w, 1); instanz("axiom.algebra.halbgruppe", "Halbgruppe", setOf("halbgruppe"),
            "\\operatorname{Halbgruppe}(${m.zuLatex()},${op.name})", runCatching { assoziativAussage(m, op) }.getOrNull()) },
        d(
            "axiom.algebra.monoid", "Monoid", setOf("monoid"), "Algebra · Gruppen",
            "\\operatorname{Monoid}(M,\\circ,e)", setOf("monoid"),
            a("menge", AxiomArgumentArt.MENGE), a("operation", AxiomArgumentArt.METHODE, 2), a("neutral", AxiomArgumentArt.OBJEKT),
        ) { w -> val m = menge(w, 0); val op = methode(w, 1); val e = w[2]; val f = runCatching { konjunktion(listOfNotNull(assoziativAussage(m, op), neutralAussage(m, op, e))) }.getOrNull(); instanz("axiom.algebra.monoid", "Monoid", setOf("monoid"), "\\operatorname{Monoid}(${m.zuLatex()},${op.name},${e.zuLatex()})", f) },
        d(
            "axiom.algebra.gruppe", "Gruppe", setOf("gruppe"), "Algebra · Gruppen",
            "\\operatorname{Gruppe}(G,\\circ,e,^{-1})", setOf("group"),
            a("menge", AxiomArgumentArt.MENGE), a("operation", AxiomArgumentArt.METHODE, 2), a("neutral", AxiomArgumentArt.OBJEKT), a("inverse", AxiomArgumentArt.METHODE, 1),
        ) { w -> val m = menge(w, 0); val op = methode(w, 1); val e = w[2]; val inv = methode(w, 3); val f = runCatching { konjunktion(listOfNotNull(assoziativAussage(m, op), neutralAussage(m, op, e), inversAussage(m, op, e, inv))) }.getOrNull(); instanz("axiom.algebra.gruppe", "Gruppe", setOf("gruppe"), "\\operatorname{Gruppe}(${m.zuLatex()},${op.name},${e.zuLatex()},${inv.name})", f) },
        d(
            "axiom.algebra.abelscheGruppe", "Abelsche Gruppe", setOf("abelsche-gruppe"), "Algebra · Gruppen",
            "\\operatorname{AbelscheGruppe}(G,\\circ)", setOf("abelsch", "commutative group"),
            a("menge", AxiomArgumentArt.MENGE), a("operation", AxiomArgumentArt.METHODE, 2), a("neutral", AxiomArgumentArt.OBJEKT), a("inverse", AxiomArgumentArt.METHODE, 1),
        ) { w -> val m = menge(w, 0); val op = methode(w, 1); val e = w[2]; val inv = methode(w, 3); val f = runCatching { konjunktion(listOfNotNull(assoziativAussage(m, op), neutralAussage(m, op, e), inversAussage(m, op, e, inv), kommutativAussage(m, op))) }.getOrNull(); instanz("axiom.algebra.abelscheGruppe", "Abelsche Gruppe", setOf("abelsche-gruppe"), "\\operatorname{AbelscheGruppe}(${m.zuLatex()},${op.name})", f) },
        d(
            "axiom.algebra.halbring", "Halbring", setOf("halbring"), "Algebra · Ringe und Körper",
            "\\operatorname{Halbring}(R,+,\\cdot,0,1)", setOf("semiring"),
            a("menge", AxiomArgumentArt.MENGE), a("addition", AxiomArgumentArt.METHODE, 2), a("multiplikation", AxiomArgumentArt.METHODE, 2),
            a("null", AxiomArgumentArt.OBJEKT), a("eins", AxiomArgumentArt.OBJEKT),
        ) { w ->
            val m = menge(w, 0); val plus = methode(w, 1); val mal = methode(w, 2); val zero = w[3]; val one = w[4]
            val f = runCatching { konjunktion(listOfNotNull(assoziativAussage(m, plus), kommutativAussage(m, plus), neutralAussage(m, plus, zero), assoziativAussage(m, mal), neutralAussage(m, mal, one), distributivAussage(m, plus, mal, true), distributivAussage(m, plus, mal, false), nullAbsorbierendAussage(m, mal, zero))) }.getOrNull()
            instanz("axiom.algebra.halbring", "Halbring", setOf("halbring"), "\\operatorname{Halbring}(${m.zuLatex()},${plus.name},${mal.name},${zero.zuLatex()},${one.zuLatex()})", f)
        },
        d(
            "axiom.algebra.ringOhneEins", "Ring ohne Eins", setOf("ring-ohne-eins"), "Algebra · Ringe und Körper",
            "\\operatorname{Ring}_{\\neg1}(R,+,\\cdot)", setOf("rng", "Ring ohne Eins"),
            a("menge", AxiomArgumentArt.MENGE), a("addition", AxiomArgumentArt.METHODE, 2), a("multiplikation", AxiomArgumentArt.METHODE, 2),
            a("null", AxiomArgumentArt.OBJEKT), a("negation", AxiomArgumentArt.METHODE, 1),
        ) { w -> val m = menge(w, 0); val plus = methode(w, 1); val mal = methode(w, 2); val zero = w[3]; val neg = methode(w, 4); instanz("axiom.algebra.ringOhneEins", "Ring ohne Eins", setOf("ring-ohne-eins"), "\\operatorname{Ring}_{\\neg1}(${m.zuLatex()},${plus.name},${mal.name})", ringAussage(m, plus, mal, zero, null, neg)) },
        d(
            "axiom.algebra.ring", "Ring mit Eins", setOf("ring"), "Algebra · Ringe und Körper",
            "\\operatorname{Ring}(R,+,\\cdot,0,1)", setOf("Ring", "Ring mit Eins"),
            a("menge", AxiomArgumentArt.MENGE), a("addition", AxiomArgumentArt.METHODE, 2), a("multiplikation", AxiomArgumentArt.METHODE, 2),
            a("null", AxiomArgumentArt.OBJEKT), a("eins", AxiomArgumentArt.OBJEKT), a("negation", AxiomArgumentArt.METHODE, 1),
        ) { w -> val m = menge(w, 0); val plus = methode(w, 1); val mal = methode(w, 2); val zero = w[3]; val one = w[4]; val neg = methode(w, 5); instanz("axiom.algebra.ring", "Ring mit Eins", setOf("ring"), "\\operatorname{Ring}(${m.zuLatex()},${plus.name},${mal.name},${zero.zuLatex()},${one.zuLatex()})", ringAussage(m, plus, mal, zero, one, neg)) },
        d(
            "axiom.algebra.kommutativerRing", "Kommutativer Ring", setOf("kommutativer-ring"), "Algebra · Ringe und Körper",
            "\\operatorname{KommutativerRing}(R,+,\\cdot)", setOf("commutative ring"),
            a("menge", AxiomArgumentArt.MENGE), a("addition", AxiomArgumentArt.METHODE, 2), a("multiplikation", AxiomArgumentArt.METHODE, 2),
            a("null", AxiomArgumentArt.OBJEKT), a("eins", AxiomArgumentArt.OBJEKT), a("negation", AxiomArgumentArt.METHODE, 1),
        ) { w -> val m = menge(w, 0); val plus = methode(w, 1); val mal = methode(w, 2); val zero = w[3]; val one = w[4]; val neg = methode(w, 5); instanz("axiom.algebra.kommutativerRing", "Kommutativer Ring", setOf("kommutativer-ring"), "\\operatorname{KommutativerRing}(${m.zuLatex()},${plus.name},${mal.name})", ringAussage(m, plus, mal, zero, one, neg, true)) },
        d(
            "axiom.algebra.integritaetsbereich", "Integritätsbereich", setOf("integritaetsbereich"), "Algebra · Ringe und Körper",
            "\\operatorname{Integritätsbereich}(R)", setOf("integral domain", "nullteilerfrei"),
            a("menge", AxiomArgumentArt.MENGE), a("addition", AxiomArgumentArt.METHODE, 2), a("multiplikation", AxiomArgumentArt.METHODE, 2),
            a("null", AxiomArgumentArt.OBJEKT), a("eins", AxiomArgumentArt.OBJEKT), a("negation", AxiomArgumentArt.METHODE, 1),
        ) { w -> val m = menge(w, 0); val plus = methode(w, 1); val mal = methode(w, 2); val zero = w[3]; val one = w[4]; val neg = methode(w, 5); val f = runCatching { konjunktion(listOfNotNull(ringAussage(m, plus, mal, zero, one, neg, true), Ungleichheit(zero, one), keineNullteilerAussage(m, mal, zero))) }.getOrNull(); instanz("axiom.algebra.integritaetsbereich", "Integritätsbereich", setOf("integritaetsbereich"), "\\operatorname{Integritätsbereich}(${m.zuLatex()})", f) },
        d(
            "axiom.algebra.schiefkoerper", "Schiefkörper", setOf("schiefkoerper"), "Algebra · Ringe und Körper",
            "\\operatorname{Schiefkörper}(K)", setOf("division ring", "skew field"),
            a("menge", AxiomArgumentArt.MENGE), a("addition", AxiomArgumentArt.METHODE, 2), a("multiplikation", AxiomArgumentArt.METHODE, 2),
            a("null", AxiomArgumentArt.OBJEKT), a("eins", AxiomArgumentArt.OBJEKT), a("negation", AxiomArgumentArt.METHODE, 1), a("inverse", AxiomArgumentArt.METHODE, 1),
        ) { w -> val m = menge(w, 0); val plus = methode(w, 1); val mal = methode(w, 2); val zero = w[3]; val one = w[4]; val neg = methode(w, 5); val inv = methode(w, 6); val f = runCatching { konjunktion(listOfNotNull(ringAussage(m, plus, mal, zero, one, neg), Ungleichheit(zero, one), inversAussage(m, mal, one, inv, zero))) }.getOrNull(); instanz("axiom.algebra.schiefkoerper", "Schiefkörper", setOf("schiefkoerper"), "\\operatorname{Schiefkörper}(${m.zuLatex()})", f) },
        d(
            "axiom.algebra.koerper", "Körper", setOf("koerper"), "Algebra · Ringe und Körper",
            "\\operatorname{Körper}(K)", setOf("field", "Körperaxiome"),
            a("menge", AxiomArgumentArt.MENGE), a("addition", AxiomArgumentArt.METHODE, 2), a("multiplikation", AxiomArgumentArt.METHODE, 2),
            a("null", AxiomArgumentArt.OBJEKT), a("eins", AxiomArgumentArt.OBJEKT), a("negation", AxiomArgumentArt.METHODE, 1), a("inverse", AxiomArgumentArt.METHODE, 1),
        ) { w -> val m = menge(w, 0); val plus = methode(w, 1); val mal = methode(w, 2); val zero = w[3]; val one = w[4]; val neg = methode(w, 5); val inv = methode(w, 6); val f = runCatching { konjunktion(listOfNotNull(ringAussage(m, plus, mal, zero, one, neg, true), Ungleichheit(zero, one), inversAussage(m, mal, one, inv, zero))) }.getOrNull(); instanz("axiom.algebra.koerper", "Körper", setOf("koerper"), "\\operatorname{Körper}(${m.zuLatex()})", f) },
    )

    val alle: List<AxiomOperatorDefinition> = relationAxiome + peanoAxiome + mengenlehreAxiome + algebraAxiome

    private val nachId = alle.associateBy(AxiomOperatorDefinition::stabileId)

    fun vonIdOderNull(id: String?): AxiomOperatorDefinition? = id?.trim()?.let { nachId[it] }

    val systeme: List<AxiomSystemDefinition> = listOf(
        AxiomSystemDefinition("relation", "Relationsaxiome", relationAxiome.mapTo(linkedSetOf()) { it.stabileId }),
        AxiomSystemDefinition("peano", "Peano-Arithmetik", peanoAxiome.mapTo(linkedSetOf()) { it.stabileId }),
        AxiomSystemDefinition("zf", "Zermelo-Fraenkel-Mengenlehre", mengenlehreAxiome.filter { "zf" in it.systeme }.mapTo(linkedSetOf()) { it.stabileId }),
        AxiomSystemDefinition("zfc", "Zermelo-Fraenkel mit Auswahl", mengenlehreAxiome.filter { "zfc" in it.systeme }.mapTo(linkedSetOf()) { it.stabileId }, setOf("zf")),
        AxiomSystemDefinition("halbgruppe", "Halbgruppe", setOf("axiom.algebra.halbgruppe")),
        AxiomSystemDefinition("monoid", "Monoid", setOf("axiom.algebra.monoid"), setOf("halbgruppe")),
        AxiomSystemDefinition("gruppe", "Gruppe", setOf("axiom.algebra.gruppe"), setOf("monoid")),
        AxiomSystemDefinition("abelsche-gruppe", "Abelsche Gruppe", setOf("axiom.algebra.abelscheGruppe"), setOf("gruppe")),
        AxiomSystemDefinition("halbring", "Halbring", setOf("axiom.algebra.halbring")),
        AxiomSystemDefinition("ring-ohne-eins", "Ring ohne Eins", setOf("axiom.algebra.ringOhneEins")),
        AxiomSystemDefinition("ring", "Ring mit Eins", setOf("axiom.algebra.ring")),
        AxiomSystemDefinition("kommutativer-ring", "Kommutativer Ring", setOf("axiom.algebra.kommutativerRing"), setOf("ring")),
        AxiomSystemDefinition("integritaetsbereich", "Integritätsbereich", setOf("axiom.algebra.integritaetsbereich"), setOf("kommutativer-ring")),
        AxiomSystemDefinition("schiefkoerper", "Schiefkörper", setOf("axiom.algebra.schiefkoerper"), setOf("ring")),
        AxiomSystemDefinition("koerper", "Körper", setOf("axiom.algebra.koerper"), setOf("schiefkoerper", "kommutativer-ring")),
    )
}
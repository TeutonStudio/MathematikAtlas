package de.TeutonStudio.MathematikRechenSystem.kern

/** Fachliches Ergebnis einer gebundenen Formel, unabhängig von Compose oder einem Kartengraphen. */
data class FormelKernErgebnis(val objekt: MathematischesObjekt, val bedingungen: Set<Aussage>, val übersetzungen: ÜbersetzungsMetadaten = ÜbersetzungsMetadaten())

fun GebundeneFormel.werteAus(bindungen: Map<String, MathematischesObjekt> = emptyMap(), operation: ((FormelAusdruck.Operation,List<MathematischesObjekt>)->FormelKernErgebnis)? = null): FormelKernErgebnis {
    require(quantoren.isEmpty()) { "Diese gebundene Bereichsoperation besitzt noch keine Auswertungscapability." }
    require(FormelBindungsPruefer.pruefe(this) == FormelBindungsPruefung.Gueltig)
    require(prüfeFormelTypen(this).isEmpty()) {prüfeFormelTypen(this).joinToString("\n")}
    require(bindungen.keys.all { id -> quellen.any { it.id == id } }) { "Unbekannte Quellenbindung." }
    val symbole = quellen.associate { q ->
        q.id to (bindungen[q.id] ?: when (q.rolle) {
            FormelSymbolRolle.METHODE -> MethodenVariable(q.anzeigeName, q.id,
                q.methodenSignatur ?: error("Methodensignatur '${q.anzeigeName}' fehlt."))
            FormelSymbolRolle.WERT, FormelSymbolRolle.KONSTANTE -> Variable(q.anzeigeName, bindungsId = q.id)
            else -> AllgemeinerParameter(q.anzeigeName, bindungsId = q.id)
        })
    }
    val bedingungen = annahmen.map { ersetze(it, symbole) }.toMutableSet()
    quellen.forEach { q -> q.werteVorrat?.let { raum -> bedingungen += ElementBeziehung(symbole.getValue(q.id), raum) } }
    val vorräte = quellen.mapNotNull { q -> q.werteVorrat?.let { q.anzeigeName to it } }.toMap()
    fun besuche(e: FormelAusdruck): MathematischesObjekt = when (e) {
        is FormelAusdruck.Literal -> {
            val metadata = e.übersetzungsMetadaten()
            bedingungen += (metadata.kontext?.annahmen.orEmpty() + metadata.aktuell?.voraussetzungen.orEmpty()).map { ersetze(it, symbole) }
            ersetze(e.wert, symbole)
        }
        is FormelAusdruck.Variable -> symbole.getValue(variablenVorkommen.getValue(e.id))
        is FormelAusdruck.Platzhalter -> error("Offener Formelplatzhalter '${e.beschriftung}'.")
        is FormelAusdruck.Operation -> {
            bedingungen += e.bedingungen.map { ersetze(it, symbole) }
            val a = e.argumente.sortedBy { it.position }.map { besuche(it.ausdruck) }
            when (e.operatorId) {
                "formel.gruppierung" -> a.single()
                "tupel.konstruktion" -> Tupel(a)
                "tupel.projektion" -> (a[0] as? Tupel ?: error("Tupelprojektion benötigt ein Tupel.")).wertAn(a[1])
                "methode.anwenden" -> {
                    val methode = a.first()
                    val argumente = Tupel(a.drop(1))
                    if (methode is MathematischeMethode) {
                        methode.wendeMitBedingungenAn(argumente).also { bedingungen += it.bedingungen }.ergebnis
                    } else {
                        val signatur = (methode as? SignaturtragendeMethode)?.signatur ?: error("Methodensignatur fehlt.")
                        require(signatur.argumente.size == argumente.anzahl) { "Falsche Argumentanzahl." }
                        signatur.argumente.zip(argumente.elemente).forEach { (formal,wert) ->
                            val raum=bereichFürTyp(formal.typ)
                            if(raum !is FehlendeObermenge) bedingungen += ElementBeziehung(wert,raum)
                        }
                        val aufruf = StrukturellerMethodenAufruf(methode, argumente)
                        Tupel(signatur.ergebnisse.mapIndexed { i, k ->
                            val methodenSignatur = k.methodenSignatur
                            if (methodenSignatur != null) MethodenWertKomponente(aufruf, i + 1, methodenSignatur)
                            else if (MathematischeTypen.typSystem.prüfe(k.typ, de.TeutonStudio.TypSystem.TypAusdruck.Atom(MathematischeTypen.Zahl)) == de.TeutonStudio.TypSystem.TypPrüfung.Kompatibel)
                                MethodenZahlKomponente(aufruf, i + 1)
                            else IndexKomponente(aufruf, listOf(RationaleZahl.von((i + 1).toLong())))
                        })
                    }
                }
                else -> {
                    val op = ZahlenRechnerOperator.entries.singleOrNull { it.stabileId == e.operatorId }
                    if(op==null) {
                        val r=operation?.invoke(e,a) ?: error("Nicht unterstützter Formeloperator '${e.operatorId}'.")
                        bedingungen += r.bedingungen
                        r.objekt
                    } else {
                    val spez = ZahlenRechnerKatalog.spezifikation(op)
                    val rollen = if (spez.rollen.size == 1) List(a.size) { spez.rollen.single().id }
                        else spez.rollen.map { it.id }
                    require(rollen.size == a.size) { "Falsche Anzahl der Formelargumente." }
                    val eingaben = a.mapIndexed { i, wert ->
                        val zahl = wert as? ZahlAusdruck ?: error("Zahlargument erwartet.")
                        ZahlenRechnerEingabe(rollen[i], zahl,
                            inferiereZahlenWertevorrat(zahl, vorräte).fundamentalerZahlbereichOderNull()
                                ?: error("Zahlbereich nicht bestimmbar."))
                    }
                    when (val ergebnis = ZahlenRechner.erzeuge(op, eingaben)) {
                        is ZahlenRechnerErgebnis.Wert -> { bedingungen += ergebnis.bedingungen; ergebnis.ausdruck }
                        is ZahlenRechnerErgebnis.Bedingt -> { bedingungen += ergebnis.bedingungen; ergebnis.ausdruck }
                        is ZahlenRechnerErgebnis.Ungueltig -> error(ergebnis.nachricht)
                        is ZahlenRechnerErgebnis.Luecke -> error("Formeloperation ist im angegebenen Bereich nicht definiert.")
                    }
                    }
                }
            }
        }
    }
    val wert = besuche(wurzel)
    val entscheidungen = bedingungen.associateWith { it.entscheide() }
    require(entscheidungen.values.none { it.wahrheitswert == Wahrheitswert.Lüge }) { "Eine Formelvoraussetzung ist widerlegt." }
    return FormelKernErgebnis(wert, entscheidungen.filterValues { it.wahrheitswert != Wahrheitswert.Wahr }.keys, wurzel.übersetzungsMetadaten())
}

fun GebundeneFormel.alsMethode(name: String, argumentIds: List<String> = quellen.filter { it.art == FormelVariablenArt.FREI }.map { it.id }): MathematischeMethode {
    require(argumentIds.distinct().size == argumentIds.size)
    val auswertung = werteAus()
    val quellenNachId = quellen.associateBy { it.id }
    val belegteNamen=mutableSetOf<String>()
    val parameter = argumentIds.map { id ->
        val q = quellenNachId.getValue(id)
        var name=q.anzeigeName;var suffix=1
        while(!belegteNamen.add(name)) name="${q.anzeigeName}_${suffix++}"
        when (q.rolle) {
            FormelSymbolRolle.METHODE -> MethodenVariable(name, id, q.methodenSignatur ?: error("Methodensignatur fehlt."))
            FormelSymbolRolle.WERT -> Variable(name, bindungsId = id)
            else -> AllgemeinerParameter(name, bindungsId = id)
        }
    }
    val ziel = when (val wert = auswertung.objekt) {
        is ZahlAusdruck -> inferiereZahlenWertevorrat(wert, quellen.mapNotNull { q -> q.werteVorrat?.let { q.anzeigeName to it } }.toMap(), auswertung.bedingungen)
        else -> FehlendeObermenge("Formelergebnis")
    }
    return MathematischeMethode(name, parameter, Tupel(listOf(auswertung.objekt)), Tupelraum(listOf(ziel)),
        parameter.associate { p -> p.name to (quellenNachId.getValue(p.bindungsId!!).werteVorrat ?: FehlendeObermenge(p.name)) },
        voraussetzungen = auswertung.bedingungen, übersetzungen = auswertung.übersetzungen.alsHerkunft())
}

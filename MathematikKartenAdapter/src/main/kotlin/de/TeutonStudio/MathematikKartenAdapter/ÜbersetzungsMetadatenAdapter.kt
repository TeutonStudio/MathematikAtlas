package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.MathematikRechenSystem.kern.*

fun BedingterWert.übersetzungsKontext(): ÜbersetzungsKontext {
    val voriger = übersetzungen.kontext ?: (objekt as? MathematischeMethode)?.übersetzungen?.kontext
    return ÜbersetzungsKontext(
        definitionsRaum = voriger?.definitionsRaum ?: werteVorrat.takeUnless { objekt is Methode },
        werteBereich = voriger?.werteBereich ?: (objekt as? MathematischeMethode)?.let { Abbildungsmenge(it.zielRaum,it.definitionsRaum()) } ?: zielMenge,
        komponentenRäume = komponentenMengen ?: voriger?.komponentenRäume,
        annahmen = annahmen + voriger?.annahmen.orEmpty(),
        quellen = (voriger?.quellen.orEmpty() + variablenQuellen.map { q ->
            ÜbersetzungsQuelle(q.strukturBindung?.id ?: q.bindungsId?.let { it + (q.bindungsName?.let { rolle -> "::$rolle" } ?: "") } ?: q.knotenId.wert,
                q.name, q.werteVorrat, if (q.alsMethodenParameter) "parameter" else "extern", q.strukturBindung?.komponentenPfad.orEmpty(),
                (objekt as? MathematischesObjekt)?.freieMethodenParameter()?.singleOrNull { it.name == q.name }?.bindungsId)
        }).distinct(),
        datenGenauigkeit = voriger?.datenGenauigkeit ?: übersetzungen.aktuell?.effektiveGenauigkeit ?: if (übersetzungen.herkunft.any { it.effektiveGenauigkeit == ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT }) ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT else ÜbersetzungsGenauigkeit.OFFEN,
        bezugsRaum = voriger?.bezugsRaum, anker = voriger?.anker, verschiebung = voriger?.verschiebung,
        argumentReihenfolge = (objekt as? MathematischeMethode)?.signatur?.argumente?.map { it.id } ?: voriger?.argumentReihenfolge.orEmpty(),
    )
}

/** Genauigkeitsfortführung benötigt keine neue Signatur-/Bereichsinferenz. */
private fun BedingterWert.istNumerischAngenähert(): Boolean {
    val metadaten = listOfNotNull(übersetzungen, (objekt as? MathematischeMethode)?.übersetzungen)
    return metadaten.any { m -> m.kontext?.datenGenauigkeit == ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT ||
        m.aktuell?.effektiveGenauigkeit == ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT ||
        m.herkunft.any { it.effektiveGenauigkeit == ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT } }
}

/** Neue Berechnungen erhalten nur Herkunft, außer der Auswerter hat den Ausgang gerade bezeugt. */
fun KnotenAuswertungsErgebnis.führeÜbersetzungsHerkunftFort(eingänge: Collection<BedingterWert>): KnotenAuswertungsErgebnis {
    val eingangsHerkunft = eingänge.flatMap { (it.übersetzungen.alsHerkunft().herkunft + (it.objekt as? MathematischeMethode)?.übersetzungen?.alsHerkunft()?.herkunft.orEmpty()) }
    return copy(ausgaben = ausgaben.mapValues { (name, wert) ->
        val eigene = if (wert.übersetzungen == ÜbersetzungsMetadaten()) (wert.objekt as? MathematischeMethode)?.übersetzungen ?: wert.übersetzungen else wert.übersetzungen
        val identischÜbertragen = wert.objekt is Methode && eingänge.any { it.objekt === wert.objekt && (if(it.übersetzungen == ÜbersetzungsMetadaten()) (it.objekt as? MathematischeMethode)?.übersetzungen ?: it.übersetzungen else it.übersetzungen) == eigene }
        val aktuell = eigene.aktuell.takeIf { name in bezeugteAusgaben || identischÜbertragen }
        val historie = (eigene.herkunft + eingangsHerkunft + if (aktuell != null) emptyList() else listOfNotNull(eigene.aktuell)).filter { it != aktuell }.distinct()
        val angenähert = eigene.kontext?.datenGenauigkeit == ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT ||
            eigene.aktuell?.effektiveGenauigkeit == ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT ||
            historie.any { it.effektiveGenauigkeit == ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT } ||
            eingänge.any { it.istNumerischAngenähert() }
        val kontext = eigene.kontext.takeIf { aktuell != null || name in bezeugteAusgaben || identischÜbertragen }
        val fortgeführterKontext = if (angenähert) (kontext ?: ÜbersetzungsKontext()).copy(
            datenGenauigkeit = ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT,
        ) else kontext
        val metadaten = ÜbersetzungsMetadaten(aktuell, historie, fortgeführterKontext)
        val methode = wert.objekt as? MathematischeMethode
        wert.copy(objekt = if(methode != null && methode.übersetzungen != metadaten) methode.copy(übersetzungen = metadaten) else wert.objekt, übersetzungen = metadaten, gebundeneFormel=wert.gebundeneFormel.takeIf { name in formelAusgaben || name in bezeugteAusgaben || identischÜbertragen }?.let { f ->
            val literal=f.wurzel as? FormelAusdruck.Literal
            if(literal!=null) f.copy(wurzel=literal.copy(übersetzungen=metadaten.copy(kontext=wert.copy(übersetzungen=metadaten).übersetzungsKontext())),annahmen=f.annahmen+wert.annahmen) else f
        })
    })
}

fun BedingterWert.alsÜbertragenerMathematischerWert() = ÜbertragenerMathematischerWert((objekt as? MathematischesObjekt ?: error("Export benötigt einen mathematischen Wert.")), übersetzungen.copy(kontext = übersetzungsKontext()), gebundeneFormel ?: runCatching {alsGebundeneErgebnisFormel()}.getOrNull())
fun BedingterWert.alsFormelLiteral(id: String, typ: FormelTyp) = FormelAusdruck.Literal(id, (objekt as? MathematischesObjekt ?: error("Formel benötigt einen mathematischen Wert.")), typ, übersetzungen.copy(kontext = übersetzungsKontext()))

/** Selbsttragender Ergebnisaustausch auch für bestehende Auswerter ohne eigenen AST-Snapshot. */
fun BedingterWert.alsGebundeneErgebnisFormel():GebundeneFormel {
    gebundeneFormel?.let {return it}
    val wert=objekt as? MathematischesObjekt ?: error("Kein mathematischer Formelwert.")
    val parameter=(wert.freieMethodenParameter()+annahmen.flatMap {it.freieMethodenParameter()}).distinct()
    val bindungen=mutableMapOf<String,MathematischesObjekt>()
    val quellen=parameter.map {p ->
        val kandidaten=variablenQuellen.filter {q -> if(p.bindungsId!=null) q.bindungsId==p.bindungsId || q.strukturBindung?.id==p.bindungsId else q.name==p.name}.distinct()
        val q=kandidaten.singleOrNull() ?: error("Die Quelle ${p.name} besitzt keinen eindeutigen Exportvertrag.")
        val id=p.bindungsId ?: q.bindungsId ?: q.knotenId.wert
        if(p.bindungsId==null) {
            bindungen[p.name]=when {
                p is Variable -> p.copy(bindungsId=id)
                p is TypisiertesElement && q.strukturBindung?.id==id && q.strukturBindung.komponentenPfad.isEmpty() -> p.copy(name=id)
                else -> error("Historische strukturierte Quellen benötigen eine explizite Identität.")
            }
        }
        val sig=(p as? MethodenVariable)?.signatur
        val bereichsTyp=q.werteVorrat.elementTypAusdruck()
        val typId=when(bereichsTyp) {
            is de.TeutonStudio.TypSystem.TypAusdruck.Atom -> bereichsTyp.id
            is de.TeutonStudio.TypSystem.TypAusdruck.Parameterisiert -> bereichsTyp.konstruktor
            else -> null
        }
        val typ=when {
            sig!=null -> FormelTyp.METHODE
            p is ZahlAusdruck -> FormelTyp.ZAHL
            p is MengenAusdruck || typId==MathematischeTypen.Menge -> FormelTyp.MENGE
            p is Aussage || typId==MathematischeTypen.Aussage -> FormelTyp.AUSSAGE
            typId==MathematischeTypen.Tupel -> FormelTyp.TUPEL
            typId!=null && MathematischeTypen.istAtomUntertyp(typId,MathematischeTypen.Vektor) -> FormelTyp.VEKTOR
            typId==MathematischeTypen.Matrix -> FormelTyp.MATRIX
            typId==MathematischeTypen.Tensor -> FormelTyp.TENSOR
            else -> FormelTyp.OBJEKT
        }
        FormelVariablenQuelle(id,q.strukturBindung?.parameterName ?: p.name,
            if(q.alsMethodenParameter) FormelVariablenArt.FREI else FormelVariablenArt.EXTERN,typ,q.werteVorrat,methodenSignatur=sig).let {
                if(typ==FormelTyp.OBJEKT && p !is ZahlAusdruck) it.copy(rolle=FormelSymbolRolle.STRUKTUR) else it
            }
    }.distinct()
    val typ=when(wert) {is ZahlAusdruck -> FormelTyp.ZAHL;is Tupel -> FormelTyp.TUPEL;is Methode -> FormelTyp.METHODE;is MengenAusdruck -> FormelTyp.MENGE;is Aussage -> FormelTyp.AUSSAGE;else -> FormelTyp.OBJEKT}
    val kontext=übersetzungsKontext()
    val metadata=übersetzungen.copy(
        aktuell=übersetzungen.aktuell?.let {it.copy(voraussetzungen=it.voraussetzungen.map {a -> ersetze(a,bindungen)}.toSet())},
        kontext=kontext.copy(annahmen=kontext.annahmen.map {ersetze(it,bindungen)}.toSet()))
    return GebundeneFormel(FormelAusdruck.Literal("ergebnis",ersetze(wert,bindungen),typ,metadata),quellen,emptyMap(),annahmen=annahmen.map {ersetze(it,bindungen)}.toSet())
}

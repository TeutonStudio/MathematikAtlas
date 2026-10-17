package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

/** Umschließt alle bestehenden frühen Rückgaben und Spezialvarianten. */
internal fun MathematikAuswerterRegister.bezeugeÜbersetzer(art: String, ziel: (KnotenAuswertungsKontext) -> ÜbersetzungsZiel) {
    val basis = finde(art) ?: error("Übersetzerauswerter '$art' fehlt.")
    registriere(art) { k ->
        try {
            val übersetzungsZiel = ziel(k)
            if (übersetzungsZiel in setOf(ÜbersetzungsZiel.TUPEL,ÜbersetzungsZiel.ZEILE,ÜbersetzungsZiel.SPALTE,ÜbersetzungsZiel.ABFLACHEN))
                return@registriere übersetzeStrukturMitZeugnis(k,übersetzungsZiel)
            val ergebnis = basis.auswerten(k)
            if (ergebnis.fehler != null) return@registriere ergebnis.copy(
                übersetzungsDiagnose = ergebnis.übersetzungsDiagnose ?: ÜbersetzungsErgebnis.NichtDefiniert("nicht_definiert",requireNotNull(ergebnis.fehler)),
            )
            if(ziel(k)==ÜbersetzungsZiel.RÜCKFÜHRUNG) return@registriere ergebnis.führeÜbersetzungsHerkunftFort(k.eingänge.values)
            val eingänge = k.eingänge.values.toList()
            val quellWert = if (übersetzerZielFürKontext(k) == ÜbersetzungsZiel.KARTESISCH && übersetzerPolarGetrennt(k.knoten)) {
                val radius = k.eingänge["radius"] ?: error("Radius fehlt.")
                val winkel = k.eingänge["winkel"] ?: error("Winkel fehlen.")
                radius.copy(objekt = Tupel(listOf(radius.mathematischesObjekt("Radius"), when (val w = winkel.mathematischesObjekt("Winkel")) {
                    is Tupel -> w; is OrientierterVektor -> Tupel(w.werte); else -> error("Winkeltupel erforderlich.")
                })), zielMenge = null, werteVorrat = null, komponentenMengen = null,
                    übersetzungen = ÜbersetzungsMetadaten(), annahmen = radius.annahmen + winkel.annahmen,
                    variablenQuellen = radius.variablenQuellen + winkel.variablenQuellen)
            } else k.eingänge["wert"] ?: k.eingänge["term"] ?: k.eingänge["tupel"] ?: eingänge.firstOrNull() ?: error("Übersetzungsquelle fehlt.")
            val quelle = quellWert.mathematischesObjekt("Übersetzungsquelle")
            val quellKontext = quellWert.übersetzungsKontext()
            val geometrie = quelle as? GeometrischerAusdruck
            val anker = when (k.knoten.parameter["geometrie.anker"] ?: if(ziel(k) == ÜbersetzungsZiel.MITTELPUNKT) "mittelpunkt" else if(ziel(k) == ÜbersetzungsZiel.OBJEKTPUNKT) "punkt" else "keiner") {
                "mittelpunkt" -> geometrie?.let { geometrieMittelpunkt(it) }
                "punkt" -> when(val a=k.eingänge["anker"]?.objekt) { is GeometriePunkt -> a.koordinaten; is Tupel -> a; is OrientierterVektor -> Tupel(a.werte); else -> null }
                else -> null
            }
            val kontext = quellKontext.copy(
                bezugsRaum = geometrie?.raum ?: (ergebnis.ausgaben.values.firstOrNull()?.objekt as? GeometrischerAusdruck)?.raum ?: quellKontext.bezugsRaum,
                anker = anker ?: quellKontext.anker,
                verschiebung = (ergebnis.ausgaben.values.firstOrNull()?.objekt as? VerschobeneKoordinatenMenge)?.verschiebung ?: quellKontext.verschiebung,
                datenGenauigkeit = if(eingänge.any { it.übersetzungsKontext().datenGenauigkeit == ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT }) ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT else quellKontext.datenGenauigkeit,
                argumentReihenfolge = (ergebnis.ausgaben.values.firstOrNull()?.objekt as? MathematischeMethode)?.signatur?.argumente?.map { it.id } ?: quellKontext.argumentReihenfolge,
                annahmen = eingänge.flatMap { it.annahmen }.toSet() + ergebnis.ausgaben.values.flatMap { it.annahmen },
                quellen = eingänge.flatMap { it.übersetzungsKontext().quellen }.distinct(),
            )
            val ausgaben = ergebnis.ausgaben.mapValues { (name, ausgabe) ->
                val w = ausgabe.mathematischesObjekt("Übersetzungsausgabe")
                val parameter = k.knoten.parameter + ("uebersetzung.ausgang" to name) + if(k.knoten.art == ÜBERSETZER_ART) mapOf(
                    ÜBERSETZER_POLAR_KONVENTION to übersetzerPolarKonvention(k.knoten).name,
                    ÜBERSETZER_VERTRETER to übersetzerVertreter(k.knoten).id,
                    ÜBERSETZER_R1 to übersetzerEindimensional(k.knoten).toString(),
                ) else emptyMap()
                when (val bezeugt = ÜbersetzungsDienst.bezeuge(ÜbersetzungsAnfrage(quelle, ziel(k), kontext, parameter), w)) {
                    is ÜbersetzungsErgebnis.NichtDefiniert -> throw ÜbersetzungsFehler(bezeugt)
                    is ÜbersetzungsErgebnis.Erfolg -> ausgabe.copy(
                        objekt = (w as? MathematischeMethode)?.let { it.copy(voraussetzungen = it.voraussetzungen + bezeugt.zeugnis.voraussetzungen, übersetzungen = ausgabe.übersetzungen.mit(bezeugt.zeugnis, bezeugt.kontext)) } ?: w,
                        gebundeneFormel = ausgabe.gebundeneFormel?.copy(wurzel=FormelAusdruck.Literal("übersetzung",w,formelTypFürWert(w),ausgabe.übersetzungen.mit(bezeugt.zeugnis,bezeugt.kontext)),variablenVorkommen=emptyMap(),annahmen=ausgabe.annahmen+bezeugt.zeugnis.voraussetzungen),
                        annahmen = ausgabe.annahmen + bezeugt.zeugnis.voraussetzungen,
                        zielMenge = bezeugt.zeugnis.zielTräger ?: ausgabe.zielMenge,
                        werteVorrat = if (quellWert.werteVorrat != null) bezeugt.kontext.werteBereich else ausgabe.werteVorrat,
                        komponentenMengen = bezeugt.kontext.komponentenRäume ?: ausgabe.komponentenMengen,
                        variablenQuellen = ausgabe.variablenQuellen,
                        übersetzungen = ausgabe.übersetzungen.mit(bezeugt.zeugnis, bezeugt.kontext),
                    )
                }
            }
            ergebnis.copy(ausgaben = ausgaben, eingänge = k.eingänge, bezeugteAusgaben = ausgaben.keys).führeÜbersetzungsHerkunftFort(k.eingänge.values)
        } catch (e: ÜbersetzungsFehler) {
            KnotenAuswertungsErgebnis(emptyMap(), fehler = e.diagnose.beschreibung, eingänge = k.eingänge, übersetzungsDiagnose = e.diagnose)
        } catch (e: IllegalArgumentException) {
            val d = ÜbersetzungsErgebnis.NichtDefiniert("ungueltige_uebersetzung", e.message.orEmpty())
            KnotenAuswertungsErgebnis(emptyMap(), fehler = d.beschreibung, eingänge = k.eingänge, übersetzungsDiagnose = d)
        } catch (e: IllegalStateException) {
            val d = ÜbersetzungsErgebnis.NichtDefiniert("nicht_definiert", e.message.orEmpty())
            KnotenAuswertungsErgebnis(emptyMap(), fehler = d.beschreibung, eingänge = k.eingänge, übersetzungsDiagnose = d)
        }
    }
}
private class ÜbersetzungsFehler(val diagnose: ÜbersetzungsErgebnis.NichtDefiniert) : IllegalArgumentException(diagnose.beschreibung)
private fun übersetzerZielFürKontext(k: KnotenAuswertungsKontext) = if (k.knoten.art == ÜBERSETZER_ART) ÜbersetzungsZiel.vonId(übersetzerZiel(k.knoten)) else null

internal fun MathematikAuswerterRegister.bezeugeHistorischeÜbersetzer() {
    if (finde("mathematik.termZuMethode") != null) bezeugeÜbersetzer("mathematik.termZuMethode") { ÜbersetzungsZiel.METHODE }
    if (finde("mathematik.geometrie.zuTrägermenge") != null) bezeugeÜbersetzer("mathematik.geometrie.zuTrägermenge") { ÜbersetzungsZiel.KOORDINATENMENGE }
}

/** Strukturänderung und Zeugnis entstehen gemeinsam im vorhandenen Fachkern. */
private fun übersetzeStrukturMitZeugnis(k:KnotenAuswertungsKontext,ziel:ÜbersetzungsZiel):KnotenAuswertungsErgebnis {
    val eingang = "wert"
    val ausgang = "ergebnis"
    val quelle = k.eingänge[eingang] ?: error("Übersetzungsquelle '$eingang' fehlt.")
    val übersetzt = when (val r = ÜbersetzungsDienst.übersetze(ÜbersetzungsAnfrage(
        quelle.mathematischesObjekt("Übersetzungsquelle"),ziel,quelle.übersetzungsKontext(),k.knoten.parameter))) {
        is ÜbersetzungsErgebnis.NichtDefiniert -> throw ÜbersetzungsFehler(r)
        is ÜbersetzungsErgebnis.Erfolg -> r
    }
    val basis = übersetzterWert(if (ziel == ÜbersetzungsZiel.ABFLACHEN)
        quelle.copy(zielMenge = null,werteVorrat = null,komponentenMengen = null) else quelle,übersetzt.wert)
    val metadaten = quelle.übersetzungen.mit(übersetzt.zeugnis,übersetzt.kontext)
    val annahmen = quelle.annahmen + übersetzt.zeugnis.voraussetzungen
    val wert = basis.copy(annahmen = annahmen,
        zielMenge = übersetzt.zeugnis.zielTräger ?: basis.zielMenge,
        werteVorrat = if (quelle.werteVorrat != null) übersetzt.kontext.werteBereich else null,
        komponentenMengen = übersetzt.kontext.komponentenRäume ?: basis.komponentenMengen,
        übersetzungen = metadaten,
        gebundeneFormel = quelle.gebundeneFormel?.copy(
            wurzel = FormelAusdruck.Literal("übersetzung",übersetzt.wert,formelTypFürWert(übersetzt.wert),metadaten),
            variablenVorkommen = emptyMap(),annahmen = annahmen))
    return KnotenAuswertungsErgebnis(mapOf(ausgang to wert),eingänge = k.eingänge,bezeugteAusgaben = setOf(ausgang))
        .führeÜbersetzungsHerkunftFort(k.eingänge.values)
}

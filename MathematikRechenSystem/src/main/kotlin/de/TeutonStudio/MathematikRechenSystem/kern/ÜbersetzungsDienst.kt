package de.TeutonStudio.MathematikRechenSystem.kern

/** Fachlicher Nachweis gemeinsam mit bestehenden Übersetzungsalgorithmen. Keine UI-Regeln. */
object ÜbersetzungsDienst {
    fun übersetze(anfrage: ÜbersetzungsAnfrage): ÜbersetzungsErgebnis = try {
        val wert = when (anfrage.ziel) {
            ÜbersetzungsZiel.TUPEL, ÜbersetzungsZiel.ZEILE, ÜbersetzungsZiel.SPALTE -> übersetzeStruktur(anfrage.wert, anfrage.ziel.id)
            ÜbersetzungsZiel.ABFLACHEN -> if (abflachenEineEbene(anfrage.parameter)) strukturEineEbeneAbflachen(anfrage.wert as? Tupel ?: error("Abflachen benötigt ein Tupel."))
                else tupelAbflachen(anfrage.wert as? Tupel ?: error("Abflachen benötigt ein Tupel."))
            ÜbersetzungsZiel.GRAD -> winkelAusBogenmass(anfrage.wert as? ZahlAusdruck ?: error("Bogenmaß muss eine Zahl sein."))
            ÜbersetzungsZiel.BOGENMASS -> winkelBogenmass(anfrage.wert as? WinkelAusdruck ?: error("Winkelwert erforderlich."), WinkelVertreter.vonId(anfrage.parameter["uebersetzer.vertreter"]))
            ÜbersetzungsZiel.POLAR -> Polarkoordinaten.polar(when (val w = anfrage.wert) { is Tupel -> w; is OrientierterVektor -> Tupel(w.werte); else -> error("Kartesische Struktur erforderlich.") }, konvention(anfrage.parameter))
            ÜbersetzungsZiel.KARTESISCH -> Polarkoordinaten.kartesisch(anfrage.wert as? Tupel ?: error("Polartupel erforderlich."), konvention(anfrage.parameter), anfrage.parameter["uebersetzer.r1"] == "true")
            ÜbersetzungsZiel.MITTELPUNKT -> geometrieMittelpunkt(anfrage.wert as? GeometrischerAusdruck ?: error("Geometrisches Objekt erforderlich."))
            ÜbersetzungsZiel.KOORDINATENMENGE -> geometrieKoordinatenMenge(anfrage.wert as? GeometrischerAusdruck ?: error("Geometrisches Objekt erforderlich."))
            else -> return ÜbersetzungsErgebnis.NichtDefiniert("kontext_erforderlich", "Dieses Ziel benötigt den bestehenden fachlichen Übersetzerauswerter und seine zusätzlichen Eingänge.")
        }
        bezeuge(anfrage, wert)
    } catch (e: IllegalArgumentException) {
        ÜbersetzungsErgebnis.NichtDefiniert("ungueltige_uebersetzung", e.message.orEmpty(), anfrage.kontext.annahmen)
    } catch (e: IllegalStateException) {
        ÜbersetzungsErgebnis.NichtDefiniert("nicht_definiert", e.message.orEmpty(), anfrage.kontext.annahmen)
    }

    private fun konvention(p: Map<String, String>): PolarKonvention = p["uebersetzer.polarKonvention"]?.let { PolarKonvention.valueOf(it) } ?: PolarKonvention.SIGNED

    /** Bezeugt auch die bereits vorhandenen Spezialauswerter, ohne deren Berechnung zu duplizieren. */
    fun bezeuge(anfrage: ÜbersetzungsAnfrage, ergebnis: MathematischesObjekt): ÜbersetzungsErgebnis {
        val q = anfrage.wert
        val quelle = ÜbersetzungsStruktur.von(q)
        val ziel = ÜbersetzungsStruktur.von(ergebnis)
        val quellBereich = quellRaum(anfrage.kontext, q)
        val mitgliedschaft = quellBereich?.let { ElementBeziehung(q, it) }
        if (mitgliedschaft?.entscheide()?.wahrheitswert == Wahrheitswert.Lüge) return ÜbersetzungsErgebnis.NichtDefiniert("ausserhalb_definitionsraum", "Der Ausgangswert verletzt den angegebenen Quellbereich.")
        val bedingungen = anfrage.kontext.annahmen + listOfNotNull(mitgliedschaft).filter { it.entscheide().wahrheitswert != Wahrheitswert.Wahr }
        if (bedingungen.any { it.entscheide().wahrheitswert == Wahrheitswert.Lüge }) return ÜbersetzungsErgebnis.NichtDefiniert("voraussetzung_widerlegt", "Eine Übersetzungsvoraussetzung ist widerlegt.", bedingungen)
        var injektiv: NachweisStatus = NachweisStatus.Unvollstaendig
        var surjektiv: NachweisStatus = NachweisStatus.Unvollstaendig
        var information: NachweisStatus = NachweisStatus.Unvollstaendig
        var genauigkeit = ÜbersetzungsGenauigkeit.OFFEN
        var plan: StrukturRückführungsplan? = null
        val verluste = mutableListOf<String>()
        val diagnosen = mutableListOf<String>()
        val variante = if(anfrage.ziel == ÜbersetzungsZiel.INDEXMETHODE) anfrage.parameter["indexmethode.baustein"] ?: anfrage.parameter["indexmethode.auswahl"] ?: anfrage.parameter["indexmethode.achsenModus"] else null
        val regel = "uebersetzung.${anfrage.ziel.id}" + (variante?.let { ":$it" } ?: "")
        var träger: MengenAusdruck? = null
        when (anfrage.ziel) {
            ÜbersetzungsZiel.TUPEL, ÜbersetzungsZiel.ZEILE, ÜbersetzungsZiel.SPALTE -> {
                val erwartet = try { übersetzeStruktur(q, anfrage.ziel.id) } catch (e: RuntimeException) {
                    return ÜbersetzungsErgebnis.NichtDefiniert("struktur_inkompatibel", e.message.orEmpty(), bedingungen)
                }
                if (erwartet != ergebnis) return ÜbersetzungsErgebnis.NichtDefiniert("regel_widerspruch", "Das Ergebnis entspricht nicht der angegebenen Strukturübersetzung.")
                plan = StrukturRückführungsplan.fürStruktur(q)
                injektiv = NachweisStatus.Nachgewiesen
                information = NachweisStatus.Nachgewiesen
                genauigkeit = ÜbersetzungsGenauigkeit.EXAKT
            }
            ÜbersetzungsZiel.ABFLACHEN -> {
                val tupel = q as? Tupel ?: return ÜbersetzungsErgebnis.NichtDefiniert("kein_tupel", "Abflachen benötigt ein Tupel.")
                val eineEbene = try { abflachenEineEbene(anfrage.parameter) } catch (e: IllegalArgumentException) {
                    return ÜbersetzungsErgebnis.NichtDefiniert("ungueltige_abflachtiefe",e.message.orEmpty(),bedingungen)
                }
                val erwartet = if (eineEbene) strukturEineEbeneAbflachen(tupel) else tupelAbflachen(tupel)
                if (erwartet != ergebnis) return ÜbersetzungsErgebnis.NichtDefiniert("regel_widerspruch", "Abflachung und Strukturplan widersprechen dem Ergebnis.")
                plan = StrukturRückführungsplan.fürAbflachen(tupel, eineEbene)
                information = NachweisStatus.Nachgewiesen
                genauigkeit = ÜbersetzungsGenauigkeit.EXAKT
                diagnosen += "Die ursprüngliche Verschachtelung ist nur mit dem mitgeführten Strukturplan rückführbar."
            }
            ÜbersetzungsZiel.GRAD -> {
                genauigkeit = ÜbersetzungsGenauigkeit.EXAKT
                träger = Gradmenge
                if (anfrage.kontext.werteBereich == ReelleZahlen || anfrage.kontext.definitionsRaum == ReelleZahlen) {
                    injektiv = NachweisStatus.Widerlegt; information = NachweisStatus.Widerlegt
                    verluste += "Die Periodenzahl des Bogenmaßes wird verworfen."
                } else diagnosen += "Injektivität und Periodeninformation hängen vom nicht nachgewiesenen Vertreterbereich ab."
            }
            ÜbersetzungsZiel.BOGENMASS -> {
                genauigkeit = ÜbersetzungsGenauigkeit.EXAKT
                if (q is WinkelAusdruck) {
                    injektiv = NachweisStatus.Nachgewiesen; information = NachweisStatus.Nachgewiesen
                    diagnosen += "Rückführung bezieht sich auf die Winkelklasse, nicht die Schreibweise eines Repräsentanten."
                }
            }
            ÜbersetzungsZiel.POLAR, ÜbersetzungsZiel.KARTESISCH -> {
                genauigkeit = ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT
                diagnosen += "Konvention: ${anfrage.parameter["uebersetzer.polarKonvention"] ?: PolarKonvention.SIGNED.name}; Ursprung, Achsen und Pole erlauben keine pauschale Bijektivitätsaussage."
            }
            ÜbersetzungsZiel.MITTELPUNKT, ÜbersetzungsZiel.OBJEKTPUNKT -> {
                information = NachweisStatus.Widerlegt
                verluste += "Der ausgewählte Punkt enthält nicht die vollständige Geometrie des Ausgangsobjekts."
                genauigkeit = ÜbersetzungsGenauigkeit.OFFEN
            }
            ÜbersetzungsZiel.METHODE -> {
                if (ergebnis is MathematischeMethode) {
                    genauigkeit = ÜbersetzungsGenauigkeit.EXAKT
                    diagnosen += "Rückführung benötigt die ursprünglichen Bindungen und die gespeicherte Argumentreihenfolge."
                }
            }
            ÜbersetzungsZiel.INDEXMETHODE, ÜbersetzungsZiel.MATRIXZERLEGEN -> {
                genauigkeit = ÜbersetzungsGenauigkeit.EXAKT
                diagnosen += "Eigenschaften der Index-/Projektionsvariante ${variante ?: "vollständige Komponentenabbildung"} sind regelabhängig; nicht bewiesene Injektivität bleibt offen."
                if (variante in setOf("schnitt", "spur", "nichtnullIndikator")) {
                    verluste += "Die Projektion beziehungsweise Indikatorbildung enthält nicht alle Ausgangskomponenten."
                    information = NachweisStatus.Unvollstaendig
                }
            }
            ÜbersetzungsZiel.RÜCKFÜHRUNG -> return ÜbersetzungsErgebnis.NichtDefiniert("plan_erforderlich", "Die Rückführung benötigt ein geprüftes Zeugnis.")
            ÜbersetzungsZiel.KOORDINATENMENGE, ÜbersetzungsZiel.GEOMETRIE -> {
                diagnosen += "Bezugsraum, Anker und Verschiebung gehören zum Übersetzungskontext; allgemeine Rückführbarkeit ist nicht nachgewiesen."
            }
        }
        val komponenten = komponentenRäume(anfrage.kontext, q)
        val quellRaum = quellRaum(anfrage.kontext, q)
        if (träger == null && plan != null && komponenten != null) {
            val räume = if (anfrage.ziel == ÜbersetzungsZiel.ABFLACHEN) plan.quellPfade.map { pfad -> raumAmPfad(quellRaum, pfad) ?: FehlendeObermenge("uebersetzung.komponente") } else komponenten
            träger = when (ergebnis) {
                is Tupel -> Tupelraum(räume)
                is ZeilenVektor -> Vektorraum(VektorOrientierung.Zeile, ergebnis.werte.size, vereinige(räume))
                is SpaltenVektor -> Vektorraum(VektorOrientierung.Spalte, ergebnis.werte.size, vereinige(räume))
                else -> null
            }
            if (anfrage.ziel != ÜbersetzungsZiel.ABFLACHEN && räume.distinct().size <= 1 && (quellRaum == Tupelraum(räume) || quellRaum is Vektorraum && komponenten == List(quellRaum.dimension) { quellRaum.skalarMenge })) surjektiv = NachweisStatus.Nachgewiesen
        }
        val bild = quellRaum?.let { ÜbersetzungsBildraum(it, regel, quelle, ziel, anfrage.parameter, plan) }
        val ids = anfrage.kontext.quellen.associate { it.id to it.id }
        val zeugnis = ÜbersetzungsZeugnis(regel, quelle = quelle, ziel = ziel, quellKontext = anfrage.kontext,
            zielTräger = träger, bildRaum = bild, parameter = anfrage.parameter,
            identitätsZuordnung = ids, voraussetzungen = bedingungen, injektiv = injektiv,
            surjektivAufTräger = surjektiv, informationserhaltung = information,
            rückführungsplan = plan, genauigkeit = genauigkeit, verloreneInformationen = verluste, diagnose = diagnosen).prüfeÜbertragung()
        if (anfrage.automatisch && !zeugnis.fürAutomatikGeeignet()) return ÜbersetzungsErgebnis.NichtDefiniert("verlustfreiheit_nicht_nachgewiesen", "Automatische Übersetzung benötigt nachgewiesene Verlustfreiheit.", bedingungen)
        val zielKomponenten = when {
            ergebnis is Tupel && träger is Tupelraum -> träger.komponenten
            ergebnis is OrientierterVektor && plan != null -> komponenten
            else -> null
        }
        return ÜbersetzungsErgebnis.Erfolg(ergebnis, anfrage.kontext.copy(definitionsRaum = if (anfrage.kontext.definitionsRaum != null) bild else null, werteBereich = bild ?: träger, komponentenRäume = zielKomponenten, datenGenauigkeit = zeugnis.effektiveGenauigkeit), zeugnis)
    }

    private fun komponentenRäume(k: ÜbersetzungsKontext, q: MathematischesObjekt): List<MengenAusdruck>? =
        k.komponentenRäume ?: when (val r = k.werteBereich ?: k.definitionsRaum) {
            is Tupelraum -> r.komponenten
            is Vektorraum -> List(r.dimension) { r.skalarMenge }
            is ÜbersetzungsBildraum -> if (q is Tupel) (r.quellRaum as? Tupelraum)?.komponenten else null
            else -> null
        }

    private fun quellRaum(k: ÜbersetzungsKontext, q: MathematischesObjekt): MengenAusdruck? {
        val räume = listOfNotNull(k.werteBereich, k.definitionsRaum).distinct().toMutableList()
        k.komponentenRäume?.let { komponenten ->
            val produkt = Tupelraum(komponenten)
            when (q) {
                is Tupel -> räume += produkt
                is OrientierterVektor -> {
                    val tupelForm = ÜbersetzungsStruktur(StrukturArt.TUPEL, ÜbersetzungsStruktur.von(q).kinder)
                    räume += ÜbersetzungsBildraum(produkt, "uebersetzung.${if (q is ZeilenVektor) "zeile" else "spalte"}", tupelForm,
                        ÜbersetzungsStruktur.von(q), rückführungsplan = StrukturRückführungsplan(tupelForm, komponenten.indices.map { listOf(it + 1) }))
                }
                else -> Unit
            }
        }
        return when (räume.distinct().size) { 0 -> null; 1 -> räume.distinct().single(); else -> Schnitt(räume.distinct()) }
    }

    private fun raumAmPfad(raum: MengenAusdruck?, pfad: List<Int>): MengenAusdruck? {
        if (pfad.isEmpty()) return raum
        val komponenten = when (raum) { is Tupelraum -> raum.komponenten; is Vektorraum -> List(raum.dimension) { raum.skalarMenge }; is Schnitt -> return raum.mengen.mapNotNull { raumAmPfad(it, pfad) }.distinct().let { if (it.size == 1) it.single() else if (it.isEmpty()) null else Schnitt(it) }; else -> return null }
        return raumAmPfad(komponenten.getOrNull(pfad.first() - 1), pfad.drop(1))
    }

    fun rückübersetze(wert: MathematischesObjekt, zeugnis: ÜbersetzungsZeugnis): ÜbersetzungsErgebnis {
        if (zeugnis.regelId == "uebersetzung.rueckfuehrung") return ÜbersetzungsErgebnis.NichtDefiniert("vorwaertsziel_erforderlich", "Wählen Sie für die erneute Vorwärtsübersetzung das ursprüngliche Ziel.")
        val geprüft = runCatching { zeugnis.prüfeÜbertragung() }.getOrElse { return ÜbersetzungsErgebnis.NichtDefiniert("zeugnis_ungueltig", it.message.orEmpty()) }
        if (geprüft.informationserhaltung != NachweisStatus.Nachgewiesen) return ÜbersetzungsErgebnis.NichtDefiniert("rueckfuehrung_offen", "Der Rückführungsvertrag ist nicht bestätigt.")
        val plan = zeugnis.rückführungsplan ?: return ÜbersetzungsErgebnis.NichtDefiniert("rueckfuehrung_offen", "Dieses Zeugnis enthält keinen nachgewiesenen strukturellen Rückführungsplan.")
        if (zeugnis.regelVersion != 1 || ÜbersetzungsStruktur.von(wert) != zeugnis.ziel) return ÜbersetzungsErgebnis.NichtDefiniert("zeugnis_inkompatibel", "Zeugnisversion oder Zielstruktur stimmt nicht überein.")
        return try {
            val komponenten = when (wert) { is Tupel -> wert.elemente; is OrientierterVektor -> wert.werte; else -> error("Komponentenstruktur erforderlich.") }
            val quelle = plan.rückführen(komponenten)
            val raum = zeugnis.quellKontext.werteBereich ?: zeugnis.quellKontext.definitionsRaum
            if (raum != null && ElementBeziehung(quelle, raum).entscheide().wahrheitswert == Wahrheitswert.Lüge) return ÜbersetzungsErgebnis.NichtDefiniert("ausserhalb_bildraum", "Die Rückführung verletzt den ursprünglichen Bereich.")
            val bildBedingung = zeugnis.bildRaum?.let { ElementBeziehung(wert, it) }
            if (bildBedingung?.entscheide()?.wahrheitswert == Wahrheitswert.Lüge) return ÜbersetzungsErgebnis.NichtDefiniert("ausserhalb_bildraum", "Die Komponenten verletzen die ursprünglichen Bereichseinschränkungen.")
            val voraussetzungen = zeugnis.voraussetzungen + listOfNotNull(bildBedingung).filter { it.entscheide().wahrheitswert != Wahrheitswert.Wahr }
            val rückKontext = zeugnis.quellKontext.copy(annahmen = zeugnis.quellKontext.annahmen + voraussetzungen)
            val invers = zeugnis.copy(regelId = "uebersetzung.rueckfuehrung", quelle = zeugnis.ziel, ziel = zeugnis.quelle,
                quellKontext = ÜbersetzungsKontext(werteBereich = zeugnis.bildRaum, annahmen = voraussetzungen, datenGenauigkeit = zeugnis.effektiveGenauigkeit),
                zielTräger = raum, bildRaum = raum, parameter = zeugnis.parameter + ("rueckfuehrung.regel" to zeugnis.regelId),
                voraussetzungen = voraussetzungen, injektiv = NachweisStatus.Nachgewiesen,
                surjektivAufTräger = NachweisStatus.Unvollstaendig, informationserhaltung = NachweisStatus.Nachgewiesen,
                diagnose = listOf("Struktur anhand des mitgeführten Plans wiederhergestellt."))
            ÜbersetzungsErgebnis.Erfolg(quelle, rückKontext, invers)
        } catch (e: RuntimeException) { ÜbersetzungsErgebnis.NichtDefiniert("rueckfuehrung_ungueltig", e.message.orEmpty()) }
    }
}

/** Fehlende Tiefe ist der historische rekursive Vertrag; unbekannte Werte sind kein Standard. */
fun abflachenEineEbene(parameter:Map<String,String>):Boolean {
    val tiefe = parameter["abflachen.ebenen"]
    require(tiefe in setOf(null,"1","rekursiv","alle")) { "Unbekannte Abflachtiefe '$tiefe'. Erlaubt sind 1 und rekursiv (historisch: alle)." }
    return tiefe == "1"
}

package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.EndlicheMenge
import de.TeutonStudio.MathematikRechenSystem.kern.LeereMenge
import de.TeutonStudio.MathematikRechenSystem.kern.MathematischesObjekt

object EndlicheMengeAuswerter {
    fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis = runCatching {
        auswertenValidiert(kontext)
    }.getOrElse { KnotenAuswertungsErgebnis(ausgaben = emptyMap(), fehler = it.message ?: "Ungültige Mengenkonfiguration.") }

    private fun auswertenValidiert(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        if (endlicheMengeEingabe(kontext.knoten) == ENDLICHE_MENGE_GRAPH) return graph(kontext)
        require(kontext.knoten.anschlüsse.none { it.richtung == AnschlussRichtung.Eingang }) { "Lokale Mengeneinträge erlauben keine Graph-Eingänge." }
        val gelesen = leseEndlicheMengeKonfiguration(kontext.knoten)
        val fehler = linkedMapOf<String, String>()
        val elemente = linkedMapOf<MathematischesObjekt, EndlicheMengeEintrag>()
        var duplikate = 0

        gelesen.konfiguration.einträge.forEach { eintrag ->
            val auswertung = eintrag.auswerten()
            val objekt = auswertung.objekt
            when {
                auswertung.fehler != null -> fehler[eintrag.id] = auswertung.fehler
                objekt == null -> fehler[eintrag.id] = "Das Element konnte nicht erzeugt werden."
                objekt in elemente -> duplikate += 1
                else -> elemente[objekt] = eintrag
            }
        }

        val menge = if (elemente.isEmpty()) LeereMenge else EndlicheMenge(elemente.keys)
        val latex = if (elemente.isEmpty()) "\\varnothing" else elemente.keys
            .joinToString(prefix = "\\{", postfix = "\\}") { it.zuLatex() }
        val gemeinsameArt = gelesen.konfiguration.mitErkannterGemeinsamerArt().gemeinsameArt
            ?.let(::AnschlussArtId)
        val warnungen = when (duplikate) {
            0 -> emptyList()
            1 -> listOf("Ein doppeltes Element wurde zusammengeführt.")
            else -> listOf("$duplikate doppelte Elemente wurden zusammengeführt.")
        }
        val gesamtFehler = gelesen.fehler ?: when (fehler.size) {
            0 -> null
            1 -> fehler.values.single()
            else -> "${fehler.size} Mengenelemente sind ungültig."
        }

        return KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "menge" to BedingterWert(
                    objekt = menge,
                    latexDarstellung = latex,
                    elementArt = gemeinsameArt,
                ),
            ),
            fehler = gesamtFehler,
            elementFehler = fehler,
            warnungen = warnungen,
        )
    }
    private fun graph(k: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        prüfeEndlicheMengeForm(k.knoten)
        val anzahl = endlicheMengeGraphAnzahl(k.knoten)
        val inputs = k.knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }
        require(inputs.map {it.name} == List(anzahl) {"element${it+1}"} &&
            inputs.all {it.belegung==AnschlussBelegung.Einzeln && !it.kannSichErweitern && it.sammelAnschluss==null} &&
            k.knoten.anschlüsse.map {it.id}.distinct().size==k.knoten.anschlüsse.size) { "Die Graph-Elementanschlüsse widersprechen der Konfiguration." }
        require(k.eingänge.keys == inputs.map {it.name}.toSet()) { "Die aktiven Graph-Eingänge sind unvollständig oder enthalten fremde Rollen." }
        val werte=inputs.map { p -> k.eingänge[p.name] ?: error("Pflicht-Element '${p.name}' fehlt.") }
        val annahmen=werte.flatMap {it.annahmen}.toSet()
        require(annahmen.none {it.entscheide(k.rechenKontext).wahrheitswert==Wahrheitswert.Lüge}) { "Eine Voraussetzung der Mengenelemente ist widerlegt." }
        val objekte=werte.map {it.mathematischesObjekt("Element der endlichen Menge")}
        val eindeutig=objekte.toCollection(linkedSetOf())
        val menge=if(eindeutig.isEmpty()) LeereMenge else EndlicheMenge(eindeutig)
        if(eindeutig.isNotEmpty()) {
            val typ=de.TeutonStudio.TypSystem.TypAusdruck.Parameterisiert(MathematischeTypen.Menge,listOf(menge.elementTypAusdruck()))
            val ausgang=k.knoten.anschlüsse.single {it.richtung==AnschlussRichtung.Ausgang}
            val prüfung=MathematischeTypen.typSystem.prüfe(typ,ausgang.vertrag.typ)
            require(prüfung !is de.TeutonStudio.TypSystem.TypPrüfung.Inkompatibel) { "Die Mengenelemente widersprechen dem gespeicherten Ausgangstyp." }
        }
        val arten=objekte.map(::anschlussArtFürMathematischesObjekt).distinct()
        require(k.mehrfachEingänge.isEmpty()) { "Graph-Elemente erlauben keine Mehrfachbeiträge." }
        val aktiv=k.copy(eingänge=inputs.associate {it.name to k.eingänge.getValue(it.name)},mehrfachEingänge=emptyMap())
        val wert=aktiv.rechnerErgebnisWert(menge).copy(elementArt=arten.singleOrNull())
        return KnotenAuswertungsErgebnis(ausgaben=mapOf("menge" to wert), eingänge=k.eingänge,
            warnungen=if(eindeutig.size<objekte.size) listOf("${objekte.size-eindeutig.size} doppelte Elemente wurden zusammengeführt.") else emptyList())
    }
}

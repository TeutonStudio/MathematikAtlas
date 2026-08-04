package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.FachKatalog
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensEintrag
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensVerfügbarkeit

object KonzeptKnotenRegister {
    fun erstelle(vorlagen: List<KnotenVorlage>): List<WissensEintrag> {
        val eindeutigeVorlagen = vorlagen.distinctBy(KnotenVorlage::stabileKonzeptId)
        val offen = eindeutigeVorlagen.toMutableList()
        val spezialisiert = buildList {
            entnehme(offen, ZahlenRechnerKonzept::passt)?.let { add(ZahlenRechnerKonzept.erstelle(it)) }
            entnehme(offen, TensorRechnerKonzept::passt)?.let { add(TensorRechnerKonzept.erstelle(it)) }
            entnehme(offen, MengenKonstanteKonzept::passt)?.let { add(MengenKonstanteKonzept.erstelle(it)) }
            entnehme(offen, ZahlKonstanteKonzept::passt)?.let { add(ZahlKonstanteKonzept.erstelle(it)) }
            entnehme(offen, TermZuMethodeKonzept::passt)?.let { add(TermZuMethodeKonzept.erstelle(it)) }
        }
        val alle = (spezialisiert + GenerischeKonzeptKnoten.erstelle(offen) + GeplanteKonzepte.alle)
            .sortedWith(compareBy<WissensEintrag> { it.fachPfade.minOf { pfad -> pfad.stabileId } }.thenBy { it.titel }.thenBy { it.id.wert })
        val fehler = validierungsFehler(alle, eindeutigeVorlagen)
        require(fehler.isEmpty()) { fehler.joinToString(prefix = "Ungültiges Konzeptknoten-Register:\n- ", separator = "\n- ") }
        return alle
    }

    fun validierungsFehler(
        einträge: List<WissensEintrag>,
        vorlagen: List<KnotenVorlage>,
    ): List<String> = buildList {
        einträge.groupBy { it.id }.filterValues { it.size > 1 }.keys.forEach {
            add("Doppelte Wissens-ID: $it")
        }
        val aliasBesitzer = mutableMapOf<String, WissensEintrag>()
        einträge.forEach { eintrag ->
            eintrag.aliase.forEach { alias ->
                val vorhanden = aliasBesitzer.put(alias, eintrag)
                if (vorhanden != null && vorhanden.id != eintrag.id) {
                    add("Alias $alias gehört gleichzeitig zu ${vorhanden.id} und ${eintrag.id}.")
                }
            }
            if (eintrag.verfügbarkeit == WissensVerfügbarkeit.Verfügbar && eintrag.knotenVorlagen.isEmpty()) {
                add("${eintrag.id}: verfügbarer Eintrag ohne Knotenvorlage")
            }
            if (eintrag.verfügbarkeit != WissensVerfügbarkeit.Verfügbar && eintrag.knotenVorlagen.isNotEmpty()) {
                add("${eintrag.id}: nicht verfügbarer Eintrag besitzt Knotenvorlagen")
            }
            if (eintrag.verfügbarkeit == WissensVerfügbarkeit.Verfügbar) {
                val primäreDefinitionen = eintrag.karten.count {
                    it.rolle == de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensKartenRolle.Definition && it.primär
                }
                if (primäreDefinitionen != 1) add("${eintrag.id}: benötigt genau eine primäre Definition")
            }
            (eintrag.fachPfade - FachKatalog.alle).forEach {
                add("${eintrag.id}: unbekannter Fachpfad ${it.stabileId}")
            }
        }

        val eindeutigeVorlagen = vorlagen.distinctBy(KnotenVorlage::stabileKonzeptId)
        val erwarteteVarianten = eindeutigeVorlagen.map(KnotenVorlage::stabileVariantenId)
        val registrierteVarianten = einträge.flatMap { it.varianten }
        (erwarteteVarianten.toSet() - registrierteVarianten.toSet()).forEach {
            add("Fehlende Knotenvorlage: $it")
        }
        registrierteVarianten.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.forEach {
            add("Doppelt registrierte Knotenvorlage: $it")
        }
        if (registrierteVarianten.size != erwarteteVarianten.size) {
            add("Vorlagenanzahl stimmt nicht: erwartet ${erwarteteVarianten.size}, registriert ${registrierteVarianten.size}")
        }
    }

    private fun entnehme(
        offen: MutableList<KnotenVorlage>,
        prädikat: (KnotenVorlage) -> Boolean,
    ): List<KnotenVorlage>? {
        val gefunden = offen.filter(prädikat)
        if (gefunden.isEmpty()) return null
        offen.removeAll(gefunden.toSet())
        return gefunden
    }
}

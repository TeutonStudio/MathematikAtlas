package com.TeutonStudio.KnotenKartenVerwalter.daten

import androidx.compose.ui.geometry.Offset
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteDaten
import java.util.UUID

data class KartenCommandErgebnis(
    val karte: KarteDaten,
    val auswahl: AuswahlDaten? = null,
    val ausgefuehrt: Boolean = true,
)

interface KartenCommand {
    val beschreibung: String

    fun ausfuehren(karte: KarteDaten, auswahl: AuswahlDaten = AuswahlDaten()): KartenCommandErgebnis
}

data class KnotenErstellen(
    val knoten: KnotenDaten,
    override val beschreibung: String = "Knoten erstellen",
) : KartenCommand {
    override fun ausfuehren(karte: KarteDaten, auswahl: AuswahlDaten): KartenCommandErgebnis {
        if (karte.knoten.any { it.id == knoten.id }) {
            return KartenCommandErgebnis(karte, auswahl, ausgefuehrt = false)
        }
        return KartenCommandErgebnis(
            karte = karte.copy(knoten = karte.knoten + knoten),
            auswahl = AuswahlDaten(knotenIds = setOf(knoten.id)),
        )
    }
}

data class KnotenAendern(
    val knoten: KnotenDaten,
    override val beschreibung: String = "Knoten aendern",
) : KartenCommand {
    override fun ausfuehren(karte: KarteDaten, auswahl: AuswahlDaten): KartenCommandErgebnis {
        var gefunden = false
        val neueKnoten = karte.knoten.map { vorhandenerKnoten ->
            if (vorhandenerKnoten.id == knoten.id) {
                gefunden = true
                knoten
            } else {
                vorhandenerKnoten
            }
        }
        return KartenCommandErgebnis(
            karte = if (gefunden) karte.copy(knoten = neueKnoten) else karte,
            auswahl = auswahl,
            ausgefuehrt = gefunden,
        )
    }
}

data class KnotenVerschieben(
    val knotenId: String,
    val position: Offset,
    override val beschreibung: String = "Knoten verschieben",
) : KartenCommand {
    override fun ausfuehren(karte: KarteDaten, auswahl: AuswahlDaten): KartenCommandErgebnis {
        val knoten = karte.knoten.firstOrNull { it.id == knotenId }
            ?: return KartenCommandErgebnis(karte, auswahl, ausgefuehrt = false)
        return KnotenAendern(
            knoten = knoten.copy(position = position),
            beschreibung = beschreibung,
        ).ausfuehren(karte, AuswahlDaten(knotenIds = setOf(knotenId)))
    }
}

data class VerbindungErstellen(
    val verbindung: VerbindungDaten,
    val ersetzeBestehendenEingang: Boolean = true,
    override val beschreibung: String = "Verbindung erstellen",
) : KartenCommand {
    override fun ausfuehren(karte: KarteDaten, auswahl: AuswahlDaten): KartenCommandErgebnis {
        val verbindungen = if (ersetzeBestehendenEingang) {
            karte.verbindungen.mitErsetztemEingang(verbindung)
        } else if (karte.verbindungen.any { it.id == verbindung.id }) {
            return KartenCommandErgebnis(karte, auswahl, ausgefuehrt = false)
        } else {
            karte.verbindungen + verbindung
        }
        return KartenCommandErgebnis(
            karte = karte.copy(verbindungen = verbindungen),
            auswahl = AuswahlDaten(verbindungIds = setOf(verbindung.id)),
        )
    }
}

data class VerbindungLoeschen(
    val verbindungId: String,
    override val beschreibung: String = "Verbindung loeschen",
) : KartenCommand {
    override fun ausfuehren(karte: KarteDaten, auswahl: AuswahlDaten): KartenCommandErgebnis {
        val vorhanden = karte.verbindungen.any { it.id == verbindungId }
        return KartenCommandErgebnis(
            karte = if (vorhanden) {
                karte.copy(verbindungen = karte.verbindungen.filterNot { it.id == verbindungId })
            } else {
                karte
            },
            auswahl = auswahl.copy(verbindungIds = auswahl.verbindungIds - verbindungId),
            ausgefuehrt = vorhanden,
        )
    }
}

data class AuswahlEinfuegen(
    val zwischenablage: KartenZwischenablage,
    val zielPosition: Offset,
    val neueId: () -> String = { UUID.randomUUID().toString() },
    override val beschreibung: String = "Auswahl einfuegen",
) : KartenCommand {
    override fun ausfuehren(karte: KarteDaten, auswahl: AuswahlDaten): KartenCommandErgebnis {
        val ergebnis = karte.fuegeEin(zwischenablage, zielPosition, neueId)
        return KartenCommandErgebnis(
            karte = ergebnis.karte,
            auswahl = ergebnis.auswahl,
            ausgefuehrt = ergebnis.karte !== karte || !ergebnis.auswahl.istLeer,
        )
    }
}

data class AuswahlLoeschen(
    val zuLoeschendeAuswahl: AuswahlDaten,
    override val beschreibung: String = "Auswahl loeschen",
) : KartenCommand {
    override fun ausfuehren(karte: KarteDaten, auswahl: AuswahlDaten): KartenCommandErgebnis {
        if (zuLoeschendeAuswahl.istLeer) {
            return KartenCommandErgebnis(karte, auswahl, ausgefuehrt = false)
        }
        return KartenCommandErgebnis(
            karte = karte.loescheAuswahl(zuLoeschendeAuswahl),
            auswahl = AuswahlDaten(),
        )
    }
}

data class KartenLayoutAnwenden(
    val algorithmus: KartenLayoutAlgorithmus = StandardKartenLayout(),
    override val beschreibung: String = "Layout anwenden",
) : KartenCommand {
    override fun ausfuehren(karte: KarteDaten, auswahl: AuswahlDaten): KartenCommandErgebnis {
        val neueKarte = algorithmus.berechneLayout(karte)
        return KartenCommandErgebnis(
            karte = neueKarte,
            auswahl = auswahl,
            ausgefuehrt = neueKarte.knoten.map { it.id to it.position } != karte.knoten.map { it.id to it.position },
        )
    }
}

class AppKartenCommand(
    override val beschreibung: String,
    private val anwenden: (KarteDaten, AuswahlDaten) -> KartenCommandErgebnis,
) : KartenCommand {
    override fun ausfuehren(karte: KarteDaten, auswahl: AuswahlDaten): KartenCommandErgebnis =
        anwenden(karte, auswahl)
}

data class KartenHistoryEintrag(
    val commandBeschreibung: String,
    val vorher: KarteDaten,
    val nachher: KarteDaten,
    val auswahlVorher: AuswahlDaten,
    val auswahlNachher: AuswahlDaten,
)

data class KartenControllerZustand(
    val karte: KarteDaten,
    val auswahl: AuswahlDaten = AuswahlDaten(),
    val undoRedoAktiv: Boolean = true,
    val pullCacheAktiv: Boolean = true,
    val pullAuswertung: KnotenPullAuswertung = StandardKnotenPullAuswertung,
    val undoStack: List<KartenHistoryEintrag> = emptyList(),
    val redoStack: List<KartenHistoryEintrag> = emptyList(),
) {
    val kannRueckgaengig: Boolean
        get() = undoRedoAktiv && undoStack.isNotEmpty()

    val kannWiederholen: Boolean
        get() = undoRedoAktiv && redoStack.isNotEmpty()

    fun mitAuswahl(neueAuswahl: AuswahlDaten): KartenControllerZustand = copy(auswahl = neueAuswahl)

    fun mitKarte(neueKarte: KarteDaten, neueAuswahl: AuswahlDaten = auswahl): KartenControllerZustand =
        copy(karte = neueKarte, auswahl = neueAuswahl)

    fun fuehreAus(command: KartenCommand): KartenControllerZustand {
        val ergebnis = command.ausfuehren(karte, auswahl)
        if (!ergebnis.ausgefuehrt) return this

        val neueAuswahl = ergebnis.auswahl ?: auswahl
        val neueKarte = if (pullCacheAktiv) {
            ergebnis.karte.mitAktualisiertemPullCache(pullAuswertung)
        } else {
            ergebnis.karte
        }
        if (!undoRedoAktiv) {
            return copy(karte = neueKarte, auswahl = neueAuswahl)
        }

        val eintrag = KartenHistoryEintrag(
            commandBeschreibung = command.beschreibung,
            vorher = karte,
            nachher = neueKarte,
            auswahlVorher = auswahl,
            auswahlNachher = neueAuswahl,
        )
        return copy(
            karte = neueKarte,
            auswahl = neueAuswahl,
            undoStack = undoStack + eintrag,
            redoStack = emptyList(),
        )
    }

    fun rueckgaengig(): KartenControllerZustand {
        if (!kannRueckgaengig) return this
        val eintrag = undoStack.last()
        return copy(
            karte = eintrag.vorher,
            auswahl = eintrag.auswahlVorher,
            undoStack = undoStack.dropLast(1),
            redoStack = redoStack + eintrag,
        )
    }

    fun wiederholen(): KartenControllerZustand {
        if (!kannWiederholen) return this
        val eintrag = redoStack.last()
        return copy(
            karte = eintrag.nachher,
            auswahl = eintrag.auswahlNachher,
            undoStack = undoStack + eintrag,
            redoStack = redoStack.dropLast(1),
        )
    }
}

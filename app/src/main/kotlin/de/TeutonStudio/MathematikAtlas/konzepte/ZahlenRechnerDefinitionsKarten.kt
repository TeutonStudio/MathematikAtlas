package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator
import de.TeutonStudio.MathematikRechenSystem.kern.ZahlenRechnerBereich
import de.TeutonStudio.MathematikRechenSystem.kern.ZahlenRechnerDefinition

/** Operator- und zahlbereichsspezifische Definitionskarten derselben Rechnerknotenart. */
internal object ZahlenRechnerDefinitionsKarten {
    fun konzept(varianten: List<KnotenVorlage>): KonzeptDefinition {
        require(varianten.isNotEmpty())
        val operator = UniversellerZahlenOperator.vonId(
            varianten.first().standardParameter[ZAHLENRECHNER_OPERATOR],
        )
        require(varianten.all {
            UniversellerZahlenOperator.vonId(it.standardParameter[ZAHLENRECHNER_OPERATOR]) == operator
        })

        if (operator == UniversellerZahlenOperator.DIVISION) {
            return divisionKonzept()
        }
        if (operator in setOf(
                UniversellerZahlenOperator.ITERIERTE_SUMME,
                UniversellerZahlenOperator.ITERIERTES_PRODUKT,
            )
        ) {
            val basis = iteriertesOperatorKonzept(varianten)
            return basis.copy(
                id = KonzeptId("zahlenrechner-${operator.stabileId.substringAfterLast('.')}") ,
                knotenArten = setOf(ZAHLENRECHNER_ART),
                knotenParameter = mapOf(ZAHLENRECHNER_OPERATOR to operator.stabileId),
                reiter = basis.reiter.map(::migriereReiter),
            )
        }

        val vorlage = varianten.first()
        val bereiche = bereicheFür(operator)
        val reiter = bereiche.mapIndexed { index, bereich ->
            val definition = ZahlenRechnerDefinition(operator, bereich)
            KonzeptReiter(
                id = if (index == 0) "definition" else "definition-${bereich.id.lowercase().replace("*", "hyper")}",
                titel = "${operator.titel}: ${definition.latex}",
                rolle = if (index == 0) KonzeptReiterRolle.Definition else KonzeptReiterRolle.Spezialfall,
                karte = bereichsKarte(vorlage, definition, index),
            )
        }
        return KonzeptDefinition(
            id = KonzeptId("zahlenrechner-${operator.stabileId.substringAfterLast('.')}") ,
            name = operator.titel,
            beschreibung = vorlage.beschreibung,
            pfad = vorlage.kategorie.split(':').map(String::trim).filter(String::isNotBlank),
            tags = setOf(operator.titel, operator.stabileId, ZAHLENRECHNER_ART) + bereiche.map { it.latex },
            knotenArten = setOf(ZAHLENRECHNER_ART),
            knotenParameter = mapOf(ZAHLENRECHNER_OPERATOR to operator.stabileId),
            reiter = reiter,
        )
    }

    private fun divisionKonzept(): KonzeptDefinition {
        val basis = DivisionDefinitionsKarten.konzept
        return basis.copy(
            id = KonzeptId("zahlenrechner-division"),
            knotenArten = setOf(ZAHLENRECHNER_ART),
            knotenParameter = mapOf(
                ZAHLENRECHNER_OPERATOR to UniversellerZahlenOperator.DIVISION.stabileId,
            ),
            reiter = basis.reiter.map(::migriereReiter),
        )
    }

    private fun migriereReiter(reiter: KonzeptReiter): KonzeptReiter = reiter.copy(
        karte = reiter.karte.migriereUniversellenZahlenRechner(),
        darstellungsVarianten = reiter.darstellungsVarianten.mapValues { (_, karte) ->
            karte.migriereUniversellenZahlenRechner()
        },
    )

    private fun bereicheFür(operator: UniversellerZahlenOperator): List<ZahlenRechnerBereich> = when (operator) {
        UniversellerZahlenOperator.MINIMUM,
        UniversellerZahlenOperator.MAXIMUM,
        -> listOf(
            ZahlenRechnerBereich.NATUERLICH,
            ZahlenRechnerBereich.NATUERLICH_MIT_NULL,
            ZahlenRechnerBereich.GANZ,
            ZahlenRechnerBereich.RATIONAL,
            ZahlenRechnerBereich.REELL,
            ZahlenRechnerBereich.HYPERREELL,
        )

        UniversellerZahlenOperator.MODULO -> listOf(
            ZahlenRechnerBereich.GANZ,
            ZahlenRechnerBereich.MODULO,
        )

        UniversellerZahlenOperator.ABRUNDUNG,
        UniversellerZahlenOperator.AUFRUNDUNG,
        UniversellerZahlenOperator.RUNDUNG,
        -> listOf(
            ZahlenRechnerBereich.RATIONAL,
            ZahlenRechnerBereich.REELL,
            ZahlenRechnerBereich.HYPERREELL,
        )

        UniversellerZahlenOperator.KOMPLEX_AUS_POLAR,
        UniversellerZahlenOperator.KOMPLEX_AUS_KARTESISCH,
        UniversellerZahlenOperator.KOMPLEXER_WINKEL,
        UniversellerZahlenOperator.KOMPLEXER_RADIUS,
        UniversellerZahlenOperator.REALTEIL,
        UniversellerZahlenOperator.IMAGINAERTEIL,
        UniversellerZahlenOperator.KONJUGIERTE,
        -> listOf(
            ZahlenRechnerBereich.REELL,
            ZahlenRechnerBereich.KOMPLEX,
            ZahlenRechnerBereich.QUATERNION,
        )

        else -> ZahlenRechnerBereich.entries.filterNot {
            it in setOf(ZahlenRechnerBereich.UNBEKANNT, ZahlenRechnerBereich.MODULO)
        }
    }

    private fun bereichsKarte(
        vorlage: KnotenVorlage,
        definition: ZahlenRechnerDefinition,
        index: Int,
    ): KartenDaten {
        val operator = definition.operator
        val prefix = "definition-zahlenrechner-${operator.stabileId.substringAfterLast('.')}-${definition.bereich.id.lowercase().replace("*", "hyper")}-$index"
        val eingänge = vorlage.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Eingang }
            .sortedBy { it.reihenfolge }
        val ausgänge = vorlage.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Ausgang }
            .sortedBy { it.reihenfolge }
        val regel = KnotenDaten(
            id = KnotenId("$prefix-regel"),
            art = TestDefinitionsKarten.KONZEPT_REGEL_ART,
            name = definition.latex,
            position = GraphPunkt(390f, 70f),
            größe = GraphGröße(470f, maxOf(160f, 105f + 34f * maxOf(eingänge.size, ausgänge.size))),
            anschlüsse = vorlage.anschlüsse.mapIndexed { anschlussIndex, anschluss ->
                anschluss.copy(id = AnschlussId("$prefix-regel-$anschlussIndex"))
            },
            parameter = mapOf(
                "regel" to definition.regeln.joinToString("\n"),
                "definition" to definition.latex,
                "operator" to operator.stabileId,
                "zahlbereich" to definition.bereich.id,
                "zahlbereichLatex" to definition.bereich.latex,
                "kommutativ" to definition.bereich.multiplikativKommutativ.toString(),
                "geordnet" to definition.bereich.geordnet.toString(),
            ),
        )
        val eingangsKnoten = eingänge.mapIndexed { anschlussIndex, anschluss ->
            schnittstelle(prefix, anschluss, anschlussIndex, true)
        }
        val ausgangsKnoten = ausgänge.mapIndexed { anschlussIndex, anschluss ->
            schnittstelle(prefix, anschluss, anschlussIndex, false)
        }
        val regelEingänge = regel.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Eingang }
            .sortedBy { it.reihenfolge }
        val regelAusgänge = regel.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Ausgang }
            .sortedBy { it.reihenfolge }
        val verbindungen = buildList {
            eingangsKnoten.forEachIndexed { anschlussIndex, quelle ->
                add(
                    VerbindungDaten(
                        id = VerbindungsId("$prefix-e-$anschlussIndex"),
                        von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single().id),
                        zu = AnschlussVerweis(regel.id, regelEingänge[anschlussIndex].id),
                    ),
                )
            }
            ausgangsKnoten.forEachIndexed { anschlussIndex, ziel ->
                add(
                    VerbindungDaten(
                        id = VerbindungsId("$prefix-a-$anschlussIndex"),
                        von = AnschlussVerweis(regel.id, regelAusgänge[anschlussIndex].id),
                        zu = AnschlussVerweis(ziel.id, ziel.anschlüsse.single().id),
                    ),
                )
            }
        }
        return KartenDaten(
            id = KartenId(prefix),
            name = "${operator.titel} auf ${definition.bereich.latex}",
            knoten = eingangsKnoten + regel + ausgangsKnoten,
            verbindungen = verbindungen,
        )
    }

    private fun schnittstelle(
        prefix: String,
        anschluss: AnschlussDaten,
        index: Int,
        eingang: Boolean,
    ): KnotenDaten {
        val id = "$prefix-${if (eingang) "eingang" else "ausgang"}-$index"
        return KnotenDaten(
            id = KnotenId(id),
            art = if (eingang) TestDefinitionsKarten.KONZEPT_EINGANG_ART else TestDefinitionsKarten.KONZEPT_AUSGANG_ART,
            name = anschluss.name,
            position = GraphPunkt(if (eingang) 30f else 930f, 70f + index * 120f),
            größe = GraphGröße(270f, 92f),
            anschlüsse = listOf(
                AnschlussDaten(
                    id = AnschlussId("$id-wert"),
                    name = "wert",
                    richtung = if (eingang) AnschlussRichtung.Ausgang else AnschlussRichtung.Eingang,
                    kante = if (eingang) AnschlussKante.Rechts else AnschlussKante.Links,
                    art = anschluss.art,
                ),
            ),
            parameter = mapOf(
                "typ" to anschluss.art.wert,
                "rolle" to anschluss.name,
            ),
        )
    }
}

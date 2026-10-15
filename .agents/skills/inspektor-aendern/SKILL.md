---
name: inspektor-aendern
description: Inspector-Einstellungen im Mathematik Atlas erstellen, ändern oder prüfen; gültige Konfigurationen, zustandsabhängige Sichtbarkeit und sinnvolle Reihenfolge für Buttons, Eingabefelder, Auswahllisten, Chips und Schalter sicherstellen.
---

# Inspector ändern

Jede übernommene Einstellung muss zu einer fachlich und technisch gültigen Konfiguration führen. Prüfe die gesamte Kombination aus Modus, Parametern, Anschlüssen und verfügbaren Eingaben; einzeln gültige Werte genügen nicht.

## Bestand und Zustandsvertrag

- Lies die Projektanweisungen und die relevanten Teile von `docs/codex/PROJECT_CONTEXT.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/ARCHITECTURE.md` und `docs/codex/NODE_CONTRACT.md` relativ zum Repository.
- Verfolge den tatsächlichen Pfad vom Bedienelement über `KnotenInspektorAktionen` und Kartenaktionen bis zur fachlichen Konfiguration und Auswertung. Untersuche einen vergleichbaren bestehenden Inspector. `MatrixdiagonaleInspektor.kt` zeigt ein modusabhängiges Ausgabeformat; übernimm vorhandenes Verhalten nur, wenn es diese Anforderungen erfüllt.
- Bestimme erlaubte Werte, Abhängigkeiten, gültige Kombinationen und Folgen eines Moduswechsels. Halte bei mehreren Abhängigkeiten eine kompakte Zustandsmatrix fest: Zustand, sichtbare Einstellungen, erlaubte Werte und gültiger Folgezustand.

## Gültige Eingaben und Übergänge

- Buttons, Chips, Schalter und Auswahllisten dürfen nur gültige Zustandsübergänge anbieten. Filtere unzulässige Auswahlwerte anhand der aktuellen Kombination. Eine Fehleranzeige nach dem Übernehmen einer vorhersehbar ungültigen Einstellung genügt nicht.
- Validiere Texteingaben einschließlich Format, Wertebereich und fachlicher Beziehungen vor der Übernahme. Leere oder unvollständige Eingaben beim Tippen dürfen als kurzlebiger Entwurf bestehen; zeige verständliche Hinweise und verhindere ihre Übernahme. Die letzte gültige Modellkonfiguration bleibt erhalten. Keine stillen Ersatzwerte für fehlerhafte Eingaben.
- Macht eine übergeordnete Einstellung abhängige Werte ungültig, führe den Wechsel samt erforderlichen Anpassungen atomar in eine nachvollziehbare gültige Kombination über. Verwende fachlich definierte Standardwerte und mache relevante Anpassungen sichtbar. Existiert kein eindeutiger gültiger Folgezustand, verlange vor der Übernahme die fehlende gültige Auswahl.
- Hinterlege fachliche Validierung in den vorgesehenen Konfigurationsfunktionen oder Auswertern, nicht ausschließlich in Composables. Auch Aufrufer außerhalb des Inspectors dürfen die Invarianten nicht umgehen.
- Gültige Konfiguration garantiert keine numerische Lösung für beliebige externe Eingaben. Fehlende Verbindungen, mathematisch undefinierte Ergebnisse und unentscheidbare Bedingungen bleiben explizite fachliche Zustände. Erzeuge durch angebotene Einstellungen keine vermeidbar widersprüchliche Konfiguration und verschleiere bestehende Diagnosen nicht.

## Sichtbarkeit und Reihenfolge

- Blende Bedienelemente aus, die für den aktuellen Modus, die Kombination oder den Zustand fachlich irrelevant sind. Das gilt auch für zugehörige Beschriftungen, Hilfetexte und leere Gruppen. Irrelevante Bedienelemente lediglich zu deaktivieren genügt nicht.
- Unterscheide Irrelevanz von einer relevanten, vorübergehend nicht ausführbaren Aktion: Letztere darf mit verständlichem Grund deaktiviert sichtbar bleiben, etwa „Übernehmen“ bei ungültigem Entwurf. Bedienelemente zur Herstellung eines gültigen Zustands müssen erreichbar bleiben.
- Leite Sichtbarkeit und erlaubte Werte aus dem aktuellen Modell und den tatsächlichen Eingaben ab. Ausgeblendete Parameter dürfen nicht unbemerkt die aktive Berechnung beeinflussen. Frühere Werte dürfen nur erhalten bleiben, wenn sie im aktuellen Modus nachweislich inaktiv sind und bei erneuter Aktivierung validiert werden.
- Ordne Einstellungen nach fachlicher Abhängigkeit: zuerst Modus oder Operation, dann notwendige Eingaben und abhängige Parameter, anschließend optionale Details und abschließende Aktionen. Gruppiere zusammengehörige Felder; verwende bei geordneten Größen die fachliche Reihenfolge. Halte die relative Reihenfolge verbleibender Elemente bei Zustandswechseln stabil.
- Prüfe Erreichbarkeit bei schmalem Fenster, großer Schrift und Tastatureingabe. Verwende eindeutige Beschriftungen und eine sinnvolle Fokusreihenfolge; ausgeblendete Elemente dürfen nicht fokussierbar bleiben.

## Modelländerung und Abnahme

- Übernimm validierte `KnotenDaten` über vorgesehene Kartenaktionen. Ein logischer Konfigurationswechsel einschließlich abhängiger Anpassungen bildet einen Undo-Schritt. Lokale Eingabeentwürfe sind keine zweite persistierte Quelle der Konfiguration.
- Berücksichtige betroffene Anschlüsse, stabile IDs, bestehende Verbindungen, Auswertungscache, Speichern/Laden und Undo/Redo. Entferne verbundene Anschlüsse nicht stillschweigend; verwende den bestehenden Konflikt- oder Bestätigungsweg des Projekts.
- Prüfe bei Verhaltensänderungen gültige und ungültige Kombinationen, Eingabegrenzen, Moduswechsel in beide Richtungen und Wiederaktivierung ausgeblendeter Einstellungen. Ergänze gezielte Tests für neue fachliche Validierung und Übergänge. Reine Umordnung benötigt keine Tests, die nur die Implementierung nachbilden.
- Prüfe Sichtbarkeit, Reihenfolge und Bedienbarkeit mit vorhandenen Compose-Tests, Vorschauen oder auf Gerät/Emulator, soweit verfügbar. Dokumentiere fehlende Laufzeitprüfung konkret; ein Build allein belegt das UI-Verhalten nicht.
- Leite passende Prüfungen aus `docs/codex/TEST_STRATEGY.md`, Gradle und CI ab. Kontrolliere den Abschlussdiff und berichte das erreichte Verhalten sowie tatsächlich ausgeführte Prüfungen.

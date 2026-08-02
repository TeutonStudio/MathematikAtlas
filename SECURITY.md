# Sicherheitsrichtlinie

## Unterstützter Stand

Der Mathematik Atlas befindet sich in aktiver experimenteller Entwicklung und besitzt derzeit keine professionell veröffentlichte Endnutzer-Version. Sicherheitskorrekturen werden grundsätzlich für den aktuellen Entwicklungsstand auf `master` betrachtet. Ältere Builds und historische Kartendaten erhalten keine garantierte Langzeitpflege.

## Sicherheitsproblem melden

Bitte veröffentliche Sicherheitsdetails nicht in einem gewöhnlichen Issue oder Pull Request.

1. Nutze im GitHub-Reiter **Security** die Funktion **Report a vulnerability**, sofern sie für das Repository angeboten wird.
2. Falls diese Funktion nicht verfügbar ist, kontaktiere den Maintainer über das GitHub-Profil `TeutonStudio` und teile zunächst nur mit, dass du einen vertraulichen Sicherheitsbericht übermitteln möchtest.
3. Nenne betroffene Versionen, Reproduktionsschritte, mögliche Auswirkungen und bekannte Gegenmaßnahmen.
4. Veröffentliche Details erst, nachdem eine koordinierte Korrektur oder eine ausdrückliche Freigabe erfolgt ist.

## Umfang

Relevant sind insbesondere:

- unbeabsichtigter Zugriff auf lokale Karten oder Dateien
- schädliche oder unerwartete Verarbeitung importierter Karten-JSON-Dateien
- Datenverlust durch Persistenz- oder Migrationsfehler
- Codeausführung oder Rechteausweitung außerhalb der vorgesehenen App-Funktionen
- Schwachstellen in Abhängigkeiten, die das Projekt tatsächlich betreffen

Gewöhnliche Funktionsfehler, mathematisch falsche Ergebnisse und Bedienprobleme ohne Sicherheitsauswirkung gehören in die reguläre Fehler-Vorlage.

## Erwartbare Reaktion

Das Projekt wird derzeit von einem kleinen Maintainer-Kreis entwickelt. Es gibt daher keine garantierte Reaktionszeit. Eingehende Meldungen sollen dennoch bestätigt, eingeordnet und nach Schwere sowie Reproduzierbarkeit bearbeitet werden.

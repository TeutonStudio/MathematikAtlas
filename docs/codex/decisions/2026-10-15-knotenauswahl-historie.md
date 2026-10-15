# 2026-10-15: Lokale Standarddialoghistorie außerhalb des Kartengraphen

Status: implementiert; Android-Laufzeitabnahme offen.

Die Nutzungsstatistik beschreibt Entscheidungen in der Android-Standardknotenliste und gehört deshalb in die App-Schicht. Der fachneutrale Graph, Kartendaten und Exporte erhalten keine Statistikfelder. Eine Instrumentierung von `KnotenVorlage.erzeuge()` ist ausgeschlossen, weil Kompatibilitätsprüfungen und Vorschauen ebenfalls Instanzen erzeugen.

Statt einer Datenbank mit neuer Abhängigkeit verwenden wir einen begrenzten, versionierten JSON-Speicher unter Androids No-Backup-Verzeichnis. Ein Prozessdienst ordnet Laden, Ereignisse, AtomicFile-Schreiben und Löschen. Er lebt über Activity-Neuerstellungen hinweg. Bei unbekanntem oder beschädigtem Format wird die Datei bis zum ausdrücklichen Löschen geschützt. Der Editor bleibt bei Statistikfehlern benutzbar.

Dialogeinträge erhalten stabile logische IDs und eine gemeinsame Ergebnisdarstellung für Oberfläche und Erfassung. Der Auswahlkontext wird nur durch den Standarddialog-Wrapper übergeben; allgemeine Einfügefunktionen übernehmen keine zufällig aktive Sitzung. Ein tatsächlicher Knotenzuwachs nach abgeschlossener Aktion entscheidet über Erfolg. Sammelaktionen zählen als eine Entscheidung; erzeugte Knotentypen sind zusätzliche Metadaten.

Suchtextabschaltung entfernt gespeicherte Texte. Der vollständige letzte Zustand bleibt nur flüchtig zur Ereignisdeduplizierung erhalten, damit verschiedene gleich-ergebnislose Anfragen weiterhin getrennte Schritte bilden. Die Normalisierung faltet keine mathematischen Zeichen zusammen. Suchbeziehungen werden ausschließlich aus direkten erfolgreichen Auswahlen innerhalb der verbleibenden Historie abgeleitet.

Folge: kein Kartenformatwechsel, keine neue Produktionsabhängigkeit, keine Sortieränderung. Detail- und Dateigrenzen begrenzen die Auswertbarkeit alter Historie. Ein Schreibabschluss bei Hintergrundwechsel verbessert die Persistenz, garantiert aber keine Sicherung bei abruptem Prozessabbruch. Implementierung und Prüfgrenzen stehen im [ExecPlan](../plans/active/2026-10-15-knotenauswahl-nutzungsstatistik.md).

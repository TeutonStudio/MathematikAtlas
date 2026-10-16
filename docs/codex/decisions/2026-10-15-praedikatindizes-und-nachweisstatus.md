# ADR: Strukturierte Prädikatindizes und getrennte Nachweiszustände

Datum: 2026-10-15

Status: Entwurf für den beauftragten Umfang; noch nicht implementiert. Versionszuordnung ausstehend.

## Kontext

Die vorhandenen iterierten Operatoren nehmen Indexmengen entgegen und erzwingen teilweise Zahlen als Indexelemente. Allgemeine Prädikate können Mengen binden; ihre Lösungsmengen können exakt definiert sein, ohne aufzählbar zu sein. Definitionsgültigkeit allein beweist weder Berechenbarkeit noch Konvergenz. Ein vorgeschlagenes Ergebnis benötigt einen eigenen Äquivalenznachweis.

## Vorgesehene Entscheidung

Ein gemeinsamer Kernvertrag enthält ein typisiertes gebundenes Element, seinen expliziten oder aus der Signatur nachgewiesenen Definitionsbereich und eine strukturierte Aussage. Historische Mengenindizes werden als Mitgliedschaftsprädikat eingebettet. Fehlende Bereiche bleiben fehlende Voraussetzungen.

Definition, Lösungsmenge und Operatorverwendbarkeit erhalten unabhängige Zustände mit konkreter Begründung. Ein Lösungsvorschlag hat den Status bestätigt, widerlegt oder unbewiesen; nur bestätigte Äquivalenz darf eine bewiesene Vereinfachung begründen. Unbekannte Entscheidungen sind keine negativen Entscheidungen. Exakte symbolische Darstellung ist ein zulässiges Ergebnis.

Endliche Familien werden vollständig und budgetbegrenzt ausgewertet. Bei offener Entscheidung oder Budgetabbruch bleibt der vollständige symbolische Ausdruck erhalten. Unendliche Familien werden nur durch belegte Kernregeln vereinfacht. Mächtigkeit, Aufzählbarkeit und Konvergenz brauchen jeweils eigene Nachweise.

Die ℕ-/ℕ₀-Regel prüft strukturierte Quantoren, Träger ℝ, Start 1 beziehungsweise 0, Nachfolger x↦x+1, Startmitgliedschaft und Abschluss sowie den Identitätsrumpf. Sie begründet beide Inklusionen des Schnitts. Die vollständige Peano-Variante erfordert Quantifizierung über sämtliche Teilmengen; eine angeschlossene einzelne Induktionsinstanz genügt nicht. Weder Formeltexte noch Knotennamen noch multiplikative Neutralität ersetzen diese Voraussetzungen.

Renderer und Inspector verwenden dieselben Kerndaten. Diagnosen, Beweisergebnisse und Lösungscaches bleiben abgeleiteter Laufzeitzustand. Persistente Index-/Methodenkonfigurationen laufen durch den gemeinsamen Android-/Desktop-Codec; vorhandene Anschluss- und Kartenversionsreferenzen bleiben erhalten.

## Alternativen

- Nur numerische Indizes erweitern: ungeeignet für Familien von Mengen und die beauftragten Definitionskarten.
- Symbolische Mengen pauschal als unbestimmbar abweisen: verwechselt Darstellung mit Enumeration und verhindert belegbare Schnitte.
- Lösungsvorschläge als Axiome übernehmen: ermöglicht unbelegte Vereinfachungen und ist ausgeschlossen.
- Neue Spezialknoten oder Register: dupliziert bestehende Pfade und widerspricht dem beauftragten Umfang.

## Konsequenzen und offene Umsetzungspunkte

Substitution, Bindungsidentität, Parameteranalyse, Typprüfung, Codecs und Cacheidentität müssen den gemeinsamen Vertrag konsistent behandeln. Alle Operatoren behalten ihre Ergebnisanforderungen. Historische Indexmengen-APIs bleiben kompatibel; gleichzeitig verbundene Prädikat- und Mengenindizes sind mehrdeutig und werden abgewiesen. Die konkrete API und eventuell erforderliche Schemamigration werden erst nach zulässiger Releasezuordnung festgelegt und durch den aktiven ExecPlan verifiziert.

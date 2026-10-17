# Identitätsbasierte Methodenaufrufe und bewahrende Projektionen

Datum: 2026-10-17. Status: M1 implementiert und unabhängig geprüft; Veröffentlichung v3.4.3 offen.

## Kontext

Historische MethodenAnwendung band nach Namen und besaß einen eigenen Einzelresultatpfad.
Der kanonische Aufruf hatte noch symbolische LaTeX-Ersatznamen, verlor bei Tupelprojektionen
Komponentenmetadaten und setzte teilweise eine konkrete MathematischeMethode voraus.
Automatische Signatursynchronisierung konnte belegte Anschlüsse oder Kanten entfernen.

## Entscheidung

Mathematische Aufrufe binden Komponenten-IDs über wendeMitIdentitätenAn; konkrete
symbolische Methoden und offene mathematische Capabilities verwenden denselben
Anwendungsvertrag. Partielle Bindung verwendet bindeIdentitäten. Allgemeine Methoden
laufen ausschließlich durch ihre vorhandene AllgemeinAuswertbareMethode-Capability.
Eine bekannte boolesche Signatur genügt zur Prädikaterkennung, verleiht aber keine
symbolische Mathematikfähigkeit. Verbraucher diagnostizieren diese fehlende Fähigkeit.

Symbolische Aufrufe entstehen ausschließlich aus bekannten Methodensignaturen im
strukturierten Aufruf-AST. Unterstützte Zahl- und Methodenprojektionen bleiben
substituierbar. Weitere Ergebnistypen werden ausdrücklich abgewiesen; insbesondere
werden dadurch keine offenen Prädikatindexfunktionen eingeführt.

Ein Ergebnis ist kanonisch ein äußeres Ergebnistupel. Direkte Projektion ist bei genau
einer Ergebniskomponente erlaubt und entfernt nur diese äußere Ebene. Ein tupelwertiges
Einzelresultat bleibt ein Tupel. Allgemeine nichtmathematische Mehrfachwerte werden
nicht in ein mathematisches Tupel gezwungen. Bekannte ungültige Projektionen werden
vor Ausführung der Capability abgelehnt.

Tupelargumente behalten pro Komponente deren Raum, Voraussetzungen und Quellen.
Metadaten der gesamten Struktur werden nicht fälschlich als Komponentenmetadaten
weitergeführt; Übersetzungszeugnisse gehen bei Projektion in die Herkunft über.
Echte Durchreichresultate bewahren vollständige gültige Metadaten des Arguments.

Eindeutige historische Einzelaufrufe erhalten feste getrennte Argumentprojektion
und direkte Ergebnisprojektion. Ihre Port-IDs, Namen, Quellen und Verbindungen
bleiben erhalten. Gespeicherte Ergebnisverträge und belegte Argumentverträge dürfen
nicht still inkompatibel umgeschrieben werden. Solche Konfigurationen bleiben mit
Vertragsdiagnose erhalten. Nicht eindeutig migrierbare Originalknoten bleiben mit
der bestehenden Migrationsdiagnose auswertungsgesperrt. Dieselben Regeln gelten in
allen Codecphasen und rekursiv für eingebettete Methodenknotenquellen.

Die zwei allgemeinen Analysisarten bleiben maßgeblich. Historische Orientierung
verursacht keine nachträgliche Transposition. Neue Vektorsuchvarianten verwenden den
vorhandenen expliziten Methoden-Ergebnisvertrag; nur dort ist die entsprechende
Ausgangseinstellung fachlich wirksam. Ordnungsrelationen werden ausschließlich anhand
der vier bekannten gespeicherten Relationswerte in vorhandene Prädikatoperatoren
überführt. Konzepte behalten ihre historischen Wissens-IDs und Definitionskarten.

## Folgen und Nachweise

Gemeinsamer Katalog: 132 mathematische Arten; Android einschließlich Werkzeug/Gruppe:
134 Arten. Keine neue Registry, Produktionsabhängigkeit oder Kartenformatversion.
43 historische ausführbare Aufrufe in 40 gespeicherten JSON-Dateien werden in den
Codecphasen geprüft. Vertrags-, Capability-, Migration-, Konzept- und
Differenzenquotienttests sichern IDs, verschachtelte Werte, Bedingungen, Kopieren,
Undo/Redo und Speicherrundlauf. Die gesamte 77-Punkte-Matrix bleibt ein eigener
Anforderungsnachweis und ist damit nicht vollständig abgeschlossen.

# Mathematik Atlas – v3 Alpha

## Zweck

v3 Alpha ist der erste integrierte Arbeitsstand des v3-Versionsraums. Er
stabilisiert Kartenlebenszyklus, große Graphen, Visualisierung und
Erweiterungsarchitektur, ohne neue persistierbare Knotentypen einzuführen.

## Enthaltene Änderungsschritte

### 1. Strukturformen und Zerlegung

- Strukturrechner unterscheiden Tupel-, Vektor-, Matrix- und Tensorformen
  präziser.
- Matrix- und Tensorzerlegung sowie die zugehörige Indexierung wurden ergänzt
  und mit Regressionstests abgesichert.
- Bestehende Knotenarten und stabile Anschluss-IDs bleiben erhalten.

### 2. Kartenlebenszyklus und Bedienung

- Die zuletzt aktive Karte wird über Activity- und Prozessneuerstellung hinweg
  anhand ihrer stabilen ID wiederhergestellt.
- Vor Kartenwechsel und bei `ON_STOP` wird die flüchtige
  Verbindungsvorschau beendet und der aktuelle Kartenstand gesichert.
- Mehrfinger-Zoom auf visuellen Gruppen und die Renderauswahl während
  Kamerabewegungen wurden stabilisiert.

### 3. Einheitliche Oberfläche

- Semantische Atlas-Abstände vereinheitlichen Dialoge, Karten, Listen und
  Aktionsbereiche.
- API-27-spezifische Systemleistenattribute liegen in qualifizierten
  Ressourcen und verursachen auf API 26 keinen Lint-Verstoß.
- Das Matrixprodukt-Konzept besitzt eine native Falk-Schema-Darstellung.

### 4. Adaptive mathematische Visualisierung

- Kurven, implizite Flächen und Volumen werden bildraumbezogen und adaptiv
  abgetastet.
- Triangulierte Methoden- und Isoflächen sowie gefüllte R²-Regionen vermeiden
  die vorherigen rasterbedingten Lücken.
- Budgets, unbekannte Bereiche und Coroutine-Abbruch bleiben explizit.

### 5. Semantisches Abstraktionswerkzeug

- Eine fachneutrale Teilgraphanalyse erkennt innere sowie ein- und ausgehende
  Grenzverbindungen.
- Teilgraphersetzungen werden vor der Anwendung gegen den analysierten Snapshot,
  Anschlussverträge, Kardinalität und Zyklusfreiheit validiert und bilden genau
  einen Undo-Schritt.
- Ein erweiterbares Abstraktionsregister trennt Graphmechanik, mathematische
  Erkennung und App-Darstellung.
- Die erste Regel erkennt Polynome exakt aus Konstanten, Variablen, Addition,
  Multiplikation und nichtnegativen ganzzahligen Potenzen. Sie ersetzt sichere
  Rechengraphen durch vorhandene Tupel- und Polynomknoten.
- Gemeinsam genutzte Zwischenknoten werden nicht gelöscht. Nachgeschaltete
  `TermZuMethode`-Knoten und ihre Methodenschnittstelle bleiben erhalten.
- Der Dialog „Graph vereinfachen“ sortiert Vorschläge nach realer
  Knotenersparnis, markiert Kandidaten im Graph und analysiert nach einer
  Ersetzung neu.

### 6. Import- und Speicherstabilisierung

- `.matlas`-Container können zusätzlich zum Export wieder importiert werden.
- Pfade, Manifest, Formatversion, Größen und SHA-256-Prüfsumme werden vor der
  Übernahme validiert; aufgeblähte ZIP-Einträge werden begrenzt gelesen.
- Datei-I/O für Import und Export läuft außerhalb des UI-Threads und Fehler
  erscheinen als sichtbarer Dialogzustand.
- Ist die neueste Kartenversion beschädigt, verwendet Bibliothek und
  `ladeAktuell` die jüngste noch lesbare Version.

## Geplante Alpha-Erweiterungen

### 7. Kartenbibliothek nach Herkunft filtern

Die bisher gemeinsam dargestellte Kartenbibliothek soll nach der Herkunft der
Karten getrennt werden. Die Auswahl erfolgt über ein Dropdown-Menü links neben
der Aktion zum Archivieren der aktuellen Karte.

Die Bibliothek unterscheidet drei Bereiche:

- **Eigene Karten:** lokal vom Nutzer erstellte Karten und Sammlungen.
- **Vordefinierte Karten:** mit der Anwendung ausgelieferte Standardkarten.
- **Fremdnutzerkarten:** importierte Karten und Sammlungen anderer Nutzer oder
  aus externen Quellen.

Die Zuordnung darf nicht aus sichtbaren Ordnernamen wie `Standardkarten` oder
`Freigaben` abgeleitet werden. Vorhandene Provenienzdaten für Standardkarten
und Freigabepakete bilden die Grundlage; für weitere externe JSON-, `.matlas`-
und Linkimporte wird eine app-lokale Herkunftsmetadaten-Schicht ergänzt. Das
allgemeine `KartenDaten`- und Graphformat bleibt davon unberührt.

#### Bereichsabhängige Aktionen

- Bei **Eigene Karten** bleiben `Neue Karte` und `Ordner +` erhalten.
- Bei **Vordefinierte Karten** werden weder `Neue Karte` noch `Ordner +`
  angeboten.
- Bei **Fremdnutzerkarten** wird `Neue Karte` durch `Karten importieren`
  ersetzt.
- `Ordner +` wird bei **Fremdnutzerkarten** durch
  `Eigene veröffentlichen` ersetzt.
- Der Archivieren-Button bleibt unabhängig vom gewählten Bibliotheksfilter auf
  die aktuell geöffnete Karte bezogen.

Beim Filtern werden nicht nur Karten, sondern auch die sichtbaren Ordnerbäume
reduziert. Ein Ordner wird in den Bereichen für vordefinierte oder fremde
Karten nur angezeigt, wenn er eine sichtbare Karte oder einen sichtbaren
Unterordner enthält. Eigene leere Benutzerordner bleiben dagegen sichtbar.

#### Karten importieren

Der neue Importdialog bündelt die bislang verteilten Importwege und bietet drei
Eingabearten:

1. **JSON:** Karten-JSON beziehungsweise ein bestehendes Freigabepaket direkt
   einfügen oder als Datei auswählen.
2. **.matlas:** einen versionierten `.matlas`-Container als Datei auswählen
   und über die vorhandene validierte Container-Pipeline importieren.
3. **Link:** eine HTTPS-Adresse angeben, deren Inhalt als JSON oder
   `.matlas` geladen und anschließend durch dieselbe Importpipeline geprüft
   wird.

Dateiendungen und MIME-Typen dienen nur als Hinweise; das tatsächliche Format
wird anhand des Inhalts erkannt. Linkimporte erhalten Größen-, Timeout- und
Redirect-Grenzen und akzeptieren keine lokalen oder unsicheren URL-Schemata.

Alle Importwege aktualisieren Kartenbestand, Ordnung und Herkunft atomar und
öffnen nach erfolgreichem Import die importierte Wurzelkarte. Extern
importierte Karten erscheinen anschließend unter **Fremdnutzerkarten**. Eine
erneut importierte Freigabe der eigenen lokalen Profilidentität bleibt den
eigenen Karten zugeordnet.

#### Eigene veröffentlichen

`Eigene veröffentlichen` öffnet einen zentralen Dialog für ausschließlich
eigene Karten und Sammlungen. Die erste Ausbaustufe verwendet die vorhandene
Freigabepaket- und Android-Teilen-Pipeline. Die Veröffentlichungslogik wird
hinter einem eigenen Dienst gekapselt, damit später ein servergestützter
öffentlicher Kartenlink ergänzt werden kann, ohne die Bibliotheksoberfläche
erneut umzubauen.

Toolbar-Import und Bibliotheksimport verwenden dieselbe Importlogik; parallele
Sonderimplementierungen für Dateiauswahl, JSON und `.matlas` sollen nicht
bestehen bleiben.

#### Abnahmekriterien

- Der Filter trennt eigene, vordefinierte und fremde Karten anhand stabiler
  Herkunftsdaten.
- Ordner erscheinen nur in den jeweils passenden Bereichen.
- Die Aktionsleiste entspricht für jeden Bereich exakt dem beschriebenen
  Zustand.
- JSON-, Freigabepaket-, `.matlas`- und Linkimport laufen durch eine gemeinsame
  validierte Importpipeline.
- Ein `.matlas`-Export kann wieder eingelesen werden und Prüfsummen- oder
  Manifestfehler werden abgewiesen.
- Import und Veröffentlichung verändern weder das neutrale Graphformat noch
  bestehende Karten- oder Knoten-IDs unnötig.
- Modelltests decken Herkunftspriorität und Ordnerfilterung ab; Importtests
  prüfen Format-Erkennung, Roundtrip und fehlerhafte Container.
- Die Verwaltungsfenster-Vorschau zeigt die drei Bibliothekszustände mit den
  jeweils korrekten Aktionen.


### 8. Kanonische Operatorabstraktion für Rechnerknoten

Die Rechnerfamilien für Zahlen, Vektoren, Matrizen und Tensoren besitzen bereits
ähnliche Grundmechaniken, beschreiben diese aktuell jedoch in mehreren
teilweise überlappenden Modellen. Vor weiteren größeren V3-Erweiterungen soll
deshalb die Knoten- und Signaturmechanik der Operatorknoten vereinheitlicht
werden.

Nicht Ziel ist ein universeller mathematischer Rechner. Die fachliche
Berechnung bleibt weiterhin je Rechnerfamilie spezialisiert. Vereinheitlicht
werden stattdessen die deklarativen Verträge, aus denen Knotenanschlüsse,
Operatorauswahl, Definitionsdarstellung, Suche, Methodenhebung und
Operatorwechsel abgeleitet werden.

#### Ausgangslage

Der aktuelle Stand verteilt Operatorwissen insbesondere auf:

- `UniversellerZahlenOperator` und `ZahlenRechnerKonfiguration.kt`,
- `VektorRechnerOperator` und `VektorRechnerKnotenKonfiguration.kt`,
- `MatrixRechnerOperator`,
- `TensorRechnerOperator`,
- `StrukturRechnerOperatorDefinition`,
- `RechnerOperatorEintrag` und `RechnerFamilienKatalog`,
- `TensorOperationDefinition` und `TensorOperationRegistry`.

Diese Modelle beschreiben mehrfach stabile Operator-IDs, Rollen, Ein- und
Ausgangstypen, Kategorien oder Operatorparameter. Dadurch können Rechenkern,
Inspector, Formelbauer, Enzyklopädie und Suchindex auseinanderlaufen.

Ein bereits sichtbares Beispiel ist der Vektorrechner: Der Rechenkern kennt
mehr Operatoren als `StrukturRechnerOperatoren.vektor`. Operationen wie
Distanz, Winkel zu Achse, Vektorfeldintegral, Zerlegen und Zusammenführen sind
damit nicht automatisch in allen operatorbezogenen Oberflächen und Verträgen
vertreten.

Beim Tensorrechner bestehen gleichzeitig der ältere
`TensorRechnerOperator`, die Strukturrechnerdefinitionen und die neuere
`TensorOperationRegistry`. Letztere beschreibt bereits zusätzliche
Eigenschaften wie Signaturfamilien, Achsen, Parameter, Mehrfachausgänge und
Unterstützungsstatus und soll deshalb als wichtigste konzeptionelle Grundlage
für die allgemeine Abstraktion dienen.

#### Zielmodell

Es wird ein gemeinsamer deklarativer Operatorvertrag eingeführt, sinngemäß aus
folgenden Bausteinen:

- `OperatorDefinition` für stabile ID, Familie, Titel, Symbol,
  Definitionsdarstellung, Kategorie und Fähigkeiten,
- `OperatorSignatur` für eine oder mehrere gültige Signaturen,
- `OperatorPortDefinition` für semantische Rollen, Typen, Richtung,
  Kardinalität und dynamische Ein- oder Ausgänge,
- `OperatorParameterDefinition` für zusätzliche Konfigurationen,
- `OperatorFamilieDefinition` für Knotenart, Standardoperator und Registry,
- eine gemeinsame `OperatorRegistry` als kanonische Quelle aller
  operatorbezogenen Metadaten.

Der Vertrag muss mindestens folgende Fälle ausdrücken können:

- unäre und binäre Operatoren,
- variadische Eingänge,
- optionale beziehungsweise alternative Signaturen,
- unterschiedliche Ergebnistypen,
- mehrere Ausgänge,
- dynamische Ein- und Ausgänge,
- Operatorparameter,
- Achsen- und Indexoperationen,
- symbolisch registrierte, aber noch nicht konkret numerisch ausgewertete
  Operationen,
- methodenhebbare Werteingänge.

Die Registry soll anschließend die gemeinsame Quelle für Inspector,
Operatorauswahldialog, Definitionskarten, Suchindex, Enzyklopädie und
Formelbauer bilden. Parallele Listen derselben Operatoren sollen nicht weiter
manuell synchronisiert werden müssen.

#### Gemeinsamer Operator-Knotenkonfigurator

Die heute getrennten Funktionen wie

- `konfiguriereZahlenRechner`,
- `konfiguriereVektorRechner`,
- `konfiguriereStrukturRechner`,
- `konfiguriereTensorRechner` und
- `konfiguriereTensorOperation`

führen zu großen Teilen dieselbe Knotenmechanik aus. Diese Mechanik wird in
einen gemeinsamen `OperatorKnotenKonfigurator` überführt.

Er soll:

1. aus der gewählten Operatorsignatur die benötigten Handles bestimmen,
2. bestehende Handles anhand ihrer semantischen Rolle und kompatiblen Art
   wiederverwenden,
3. stabile Anschluss-IDs erhalten, wenn Rolle und Vertrag kompatibel bleiben,
4. neue Handles erzeugen und entfallende Handles bestimmen,
5. vor einem Wechsel den Verlust bestehender Edges diagnostizieren,
6. Parameter und Operator-ID atomar aktualisieren,
7. variadische und dynamische Handles nach demselben Vertrag behandeln.

Die vorhandene `TensorSignaturWechselDiagnose` dient als Ausgangspunkt und
wird zu einer familienunabhängigen Wechselanalyse verallgemeinert.

#### Fachliche Auswertung bleibt spezialisiert

Die gemeinsame Operatorabstraktion darf die mathematische Semantik nicht in
einen universellen Rechner verschieben. Stattdessen erhält jede Familie einen
eigenen Auswerter beziehungsweise Adapter, zum Beispiel:

- Zahlenoperator-Auswerter,
- Vektoroperator-Auswerter,
- Matrixoperator-Auswerter,
- Tensoroperator-Auswerter.

Der allgemeine Vertrag beschreibt, welche Operanden und Parameter eine
Operation besitzt und welche Ergebnisse sie liefern kann. Der jeweilige
Fachauswerter entscheidet weiterhin, wie die Operation mathematisch berechnet
wird und welche fachlichen Bedingungen, Fehler oder symbolischen Ergebnisse
entstehen.

Dadurch können beispielsweise Tensorachsen, Matrixinvertierbarkeit,
Vektormetriken, Integrale oder Zahlbereichsbedingungen spezialisiert bleiben,
ohne dass ihre Knotenmechanik erneut implementiert werden muss.

#### Operatorparameter und Inspector

Familienabhängige Zusatzparameter sollen soweit möglich ebenfalls deklarativ
beschrieben werden. Beispiele sind Winkelmodus beim Zahlenrechner, Metrik und
Achse beim Vektorrechner sowie Achsen, Permutationen und Indizes beim
Tensorrechner.

Standardparameter können dadurch generisch im Inspector dargestellt werden.
Komplexe Spezialoberflächen dürfen weiterhin eigene UI-Adapter besitzen, bauen
aber auf demselben Parametervertrag auf. Die Abstraktion darf also keine
Spezialfälle verstecken, sondern soll lediglich verhindern, dass jeder
Spezialfall die komplette Operatorinfrastruktur erneut implementiert.

#### Auswerterregistrierung

Die heutige Auswerterkette, in der spätere Registrierungen frühere Auswerter
ersetzen und teilweise wieder an gespeicherte Vorgänger delegieren, soll
schrittweise vereinfacht werden.

Langfristig soll eine Rechnerknotenart genau einen stabilen
Operator-Knotenauswerter besitzen. Dieser löst die gespeicherte Operator-ID über
die Registry auf und delegiert anschließend explizit an den zuständigen
Fachauswerter. Die semantische Bedeutung eines Operators darf nicht mehr davon
abhängen, in welcher Reihenfolge mehrere Auswerterschichten registriert wurden.

#### Migrationsreihenfolge

Die Umstellung erfolgt schrittweise und ohne unnötige Änderung persistierter
Knotenarten oder stabiler Operator-IDs:

1. allgemeinen Operator-, Signatur-, Port- und Parametervertrag aus den
   bestehenden Tensor- und Rechnerfamilienmodellen ableiten,
2. gemeinsamen Operator-Knotenkonfigurator und allgemeine
   Signaturwechsel-Diagnose einführen,
3. Matrixrechner als erste vollständige Familie migrieren,
4. Vektorrechner migrieren und die aktuell auseinanderlaufenden Operatorlisten
   konsolidieren,
5. Zahlenrechner mit Variadik, Methodenhebung, komplexen Eingabemodi und
   Analysisoperatoren migrieren,
6. Tensorregistry auf den gemeinsamen Vertrag abbilden, ohne ihre
   Achsen-/Mehrfachausgangsfähigkeiten zu verlieren,
7. `StrukturRechnerOperatorDefinition`, `RechnerOperatorEintrag` und
   weitere doppelte Metadatenmodelle nur noch als temporäre
   Kompatibilitätsadapter verwenden und anschließend entfernen,
8. Suchindex, Enzyklopädie, Formelbauer und Inspector ausschließlich aus der
   gemeinsamen Registry speisen.

#### Abgrenzung für spätere V3-Systeme

Die Abstraktion soll nicht auf mathematische Rechnernamen fest verdrahtet sein.
Spätere operatorbasierte Familien wie SVG-, TeX-, Animations- oder
Engine-/Godot-Verarbeitung sollen denselben allgemeinen Knotenvertrag
wiederverwenden können, ohne deshalb mathematische Objekte oder mathematische
Auswerter werden zu müssen.

Damit bleibt die Operatorinfrastruktur domänenneutral, während die jeweiligen
Fachmodule ihre eigene Semantik behalten.

#### Abnahmekriterien

- Zahlen-, Vektor-, Matrix- und Tensorrechner beziehen ihre
  Operator-Schnittstellen aus einem gemeinsamen deklarativen Vertragsmodell.
- Eine stabile Operator-ID besitzt nur noch eine kanonische Quelle für
  Metadaten und Signaturen.
- Operatorwechsel erhalten kompatible Anschluss-IDs und melden vorab, welche
  bestehenden Verbindungen durch einen Signaturwechsel entfallen.
- Variadische Eingänge, alternative Signaturen und Mehrfachausgänge sind ohne
  familieneigene Kopie der allgemeinen Knotenmechanik darstellbar.
- Inspector, Suchindex, Definitionsdarstellung und Formelbauer können dieselbe
  Registry verwenden.
- Die fachliche Berechnung bleibt in den jeweiligen Rechnerfamilien getrennt.
- Bestehende persistierte Knotenarten und stabile Operator-IDs werden soweit
  möglich unverändert übernommen.
- Tests prüfen Registry-Eindeutigkeit, Signatur-Port-Abbildung,
  Anschluss-ID-Erhalt, Edge-Verlustdiagnose, Migration und die vollständige
  Abdeckung aller Operatoren einer Rechnerfamilie.

## Verifikation

- Repository-, Releaseplan- und Versionsfolgeprüfung
- vollständige JVM-Tests einschließlich Desktop-Shadowmodule
- Android-Lint für Debug
- Android-Debug-APK

Nicht durch die lokale Prüfung ersetzt werden Gerätebedienung, Frame-Timing und
Lebenszyklustests auf einem realen Android-Gerät.

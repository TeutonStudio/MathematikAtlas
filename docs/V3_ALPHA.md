# Mathematik Atlas – v3 Alpha

## Zweck

v3 Alpha ist der erste integrierte Arbeitsstand des v3-Versionsraums. Er
stabilisiert Kartenlebenszyklus, große Graphen, Visualisierung und
Erweiterungsarchitektur, ohne neue persistierbare Knotentypen einzuführen.


## Verbindliche Folgeentscheidungen (11. Oktober 2026)

Die abgeschlossenen Schritte 1–6 beschreiben den ersten Alpha-Stand und werden
durch die folgenden Planungen nicht rückwirkend als unfertig deklariert. Die
Schritte 7 ff. sind geplante Erweiterungen; neue persistierbare Knotentypen
erhalten gemäß `docs/VERSIONING.md` eigene passende Releaseversionen.

- **Technologie:** Kotlin und Jetpack Compose bleiben verbindlich für Android
  und Desktop. Gemeinsame fachliche Modelle und soweit möglich gemeinsame
  Compose-UI haben Vorrang vor parallelen Implementierungen. Keine Umstellung
  auf ReactFlow/Vite.
- **Roadmap:** v3 bleibt dem Ausbau von Grafik, SVG, TeX und Dokumenten gewidmet.
  Der eigenständige Beweisatlas aus Issue #351 wird in einen späteren,
  gesondert festzulegenden Versionsraum verschoben; die bereits veröffentlichte
  Version 3.0.0 erhält keine nachträgliche widersprüchliche Bedeutung.
- **Werte und Methoden:** Der neutrale Atlas-Kern transportiert `AtlasWert`
  ohne Zwang zu `MathematischesObjekt` oder `MengenAusdruck`. Mathematische
  Objekte, mathematische Methoden und Mengensemantik sind fachliche Adapter.
  Für die ausstehenden PRs #433 und #435 ist eine einzige kontrollierte
  Implementierungslinie festzulegen; #435 ist der bevorzugt zu prüfende
  Ausgangspunkt, nicht eine Freigabe zum ungeprüften Merge. PR #436 hängt
  davon ab.
- **Methodenvertrag:** Intern gilt einheitlich `Tupel → Tupel`, auch bei null-
  und einstelligen Methoden. Sichtbare einzelne Handles und Ergebnisprojektionen
  sind Adapter dieser kanonischen Signatur. Issue #431 bleibt für die
  Tupelsemantik maßgeblich.
- **Operatoren:** Der deklarative Operatorvertrag aus Schritt 8 ist
  verbindlich. Zahlen-, Vektor-, Matrix- und Tensorrechner behalten ihre
  spezialisierten mathematischen Auswerter. SVG-/TeX-Operatoren dürfen
  dieselbe neutrale Infrastruktur später mitverwenden.
- **Bedienung:** Der Formelbau verwendet **einen gemeinsamen Dialog** mit
  strukturierter Tastatur-/Textbearbeitung, Stiftzeichnung und Fotoerkennung.
  Die Stiftzeichnung wird als eigenständiger Bestandteil der betreffenden
  Karte dauerhaft gespeichert.
- **Mathematische Vollständigkeit:** Vor großflächiger Erweiterung der
  Operatorbibliothek müssen insbesondere Typprüfung, Parameter, Variablen,
  Inspector-Konfiguration und dynamische Anschlüsse zuverlässig
  zusammenspielen.

Die Architekturverträge und UI-Grundlagen werden in Alpha konkretisiert.
Aufwendige Foto-/Handschrifterkennung und die bereits in
`docs/V3_BETA.md` beschriebenen SVG-/TeX-Erzeugungspfade behalten ihren
späteren Realisierungsschwerpunkt; die hier getroffenen gemeinsamen
Datenmodellentscheidungen gelten schon vorher.

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


### 9. Werte-, Methoden- und konfigurationsabhängige Typverträge

#### Problem und Ausgangslage

- `mathematik.variable` deklariert bislang einen groben Zahl-Ausgang, obwohl
  der Wertevorrat im Inspector festgelegt wird.
- `mathematik.allgemeinerParameter` deklariert bislang einen groben
  Objekt-Ausgang, obwohl eine `WertebereichKonfiguration` gespeichert wird.
- `AnschlussDaten.vertrag`, `TypAusdruck` und `TypInferenzRegel` bilden
  bereits ein neutrales Typsystem, aber die effektiven Anschlussverträge
  werden noch nicht überall aus dem vollständig konfigurierten Knoten
  abgeleitet.
- `GraphPrüfung` unterscheidet `Kompatibel`, `Inkompatibel` und
  `Unbestimmt`. Unbestimmtheit wird bei der Verbindungsprüfung derzeit
  nicht als Hindernis behandelt. Ein bloß grober Legacy-Anschlusstyp darf
  daher keine scheinbar bewiesene mathematische Typgültigkeit suggerieren.
- Der Änderungsweg von Anschlussarten und -verträgen kann inkompatible
  Verbindungen herausfiltern. Eine Inspector-Änderung darf bestehende
  Verbindungen nicht ohne sichtbare Diagnose löschen.

#### Ziel

1. Ein zentraler **effektiver Knoten- und Anschlussvertrag** berechnet aus
   Knotenart, Inspector-Parametern, strukturierten Eigenschaften, Operator-ID,
   angeschlossenen Quellen und gegebenenfalls Methodensignatur den erwarteten
   beziehungsweise gelieferten `TypAusdruck`.
2. Variablen und allgemeine Parameter liefern die passende Typinformation
   ihres konfigurierten Wertevorrats. Beispiele: reelle Zahl, Aussage,
   Tupeltypen, orientierte Zeile/Spalte, Matrix einschließlich Form oder
   fachlich nicht weiter einschränkbarer mathematischer Wert.
3. Der Wertebereich ist **mehr als eine Anzeigeeigenschaft**: Er beeinflusst
   Anschlusskompatibilität, Methodenparameter, Auswertung und Fehlermeldung.
   Dabei sind Elementtyp, Mengenmitgliedschaft, Form und fachliche
   Anforderungen sauber zu unterscheiden. Ein Teilmengenbeweis ersetzt
   keine Typgleichheit.
4. Für Methoden gilt der kanonische Signaturkern `Tupel → Tupel`. Inspector-
   Einstellungen wie Argumentanzahl, Argumentreihenfolge, Definitionsbereich,
   Zielmenge, Term- oder Methodenausgabe und Projektion beeinflussen die
   sichtbaren Verträge, nicht die interne Methodensemantik.
5. Eine explizite Einstellung „beliebiger Wert“ bedeutet
   `TypAusdruck.Beliebig`; `TypAusdruck.Unbekannt` steht weiterhin nur für
   fehlendes Wissen. Beide Zustände dürfen nicht gleichgesetzt werden.
6. **Drei Prüfergebnisse** bleiben erkennbar: kompatibel, inkompatibel und
   unbestimmt. Eine mathematisch nicht gesicherte Verbindung wird nicht als
   nachweislich korrekt markiert. Bei hinreichend konkretem Vertrag dürfen
   bekannte Widersprüche nicht durch den Legacy-Fallback akzeptiert werden.
7. Ändert sich die Inspector-Konfiguration, werden abhängige Handles und
   bestehende Edges zentral neu überprüft. Potenziell getrennte Verbindungen
   müssen vor dem Übernehmen aufgelistet und vom Nutzer bestätigt werden.
   Kompatible Anschluss-IDs bleiben erhalten.
8. Typ- und Signaturprüfungen arbeiten auf Android und Desktop identisch;
   sie sind nicht an Compose gebunden.

#### Abnahmekriterien

- Eine Variable beziehungsweise ein Parameter mit konfigurierter reeller
  Wertemenge kann nicht fälschlich als Matrix oder Aussage angeschlossen
  werden; ein passend konfigurierter Matrixparameter kann an einen
  kompatiblen Matrixeingang angeschlossen werden.
- Ein Inspector-Wechsel von Zahl zu Matrix, Tupel oder Aussage verändert
  den effektiven Ausgangsvertrag reproduzierbar.
- Methodenbindungen, Teilgraphabstraktionen und Ausgabenprojektionen
  respektieren die neuen Verträge und behalten ihre Argumentreihenfolge.
- Änderungen mit Edge-Verlust erzeugen eine Vorschau, keine stille Löschung.
- Tests decken `Beliebig`, `Unbekannt`, Typvariablen, parametrisierte
  Typen, Wertebereich, Formen, Regressionen und Karten-JSON-Roundtrips ab.

### 10. Innenabstände für alle Operatorauswahl-Einträge

Der vorhandene `RechnerOperatorAuswahlDialog.kt` verwendet für
`OperatorKachel` den kompakten Wert `LocalAtlasAbstände.current.bereich`
(derzeit 12 dp). Diese bestehende Zentralisierung wird beibehalten, aber
die tatsächliche optische Dichte der Einträge wird korrigiert.

#### Vorgaben

- **Alle** Einträge des einheitlichen Operatorauswahldialogs (Zahlen,
  Vektoren, Matrizen, Tensoren und spätere Familien) erhalten mindestens
  16 dp horizontales Innenpadding und ausreichend vertikales Innenpadding
  von 12–16 dp; geeignete semantische Tokens sind `inhalt` und `bereich`.
- Titel, mathematisches Symbol/LaTeX, Kategorie, Beschreibung, Auswahl- und
  Statuskennzeichnung erhalten getrennte, konsistente Zwischenräume.
- Lange Bezeichnungen und Formeln dürfen weder am Kartenrand kleben noch
  abgeschnitten oder von Auswahlmarkierungen überlagert werden. Breite,
  Textumbruch und gegebenenfalls eine begrenzte lokale Scrollfläche sind
  adaptiv.
- Auch die Detailansicht, Signaturzeilen und die Such-/Kategorienbereiche
  sind mit genügend Randabstand zu prüfen. Die Vorschau ist im hellen und
  dunklen Modus sowie auf Telefon, Tablet und Desktop zu kontrollieren.
- Padding wird über `AtlasAbstände` beziehungsweise ergänzte semantische
  Abstands-Tokens verwaltet, nicht durch voneinander abweichende
  Zahlenkonstanten pro Rechnerfamilie.
- UI-Tests und Compose-Previews decken lange TeX-Ausdrücke, schmale
  Displays, große Schrift und ausgewählte Einträge ab.

### 11. Eigene Knotenvorlagenfamilie „Übersetzer“

#### Fachlicher Vertrag

Es entsteht eine gemeinsame Knotenvorlagenfamilie für **explizite
Repräsentations- und Typübersetzungen**, statt für jedes Quell-/Zielpaar
eine weitere fast identische feste Knotenart einzuführen.

- Ein Übersetzerknoten hat **genau einen Wert-Eingang und einen
  Ergebnis-Ausgang**.
- Sein Eingang ist bei Erstellung offen für beliebige Atlas-Laufzeitwerte
  (`TypAusdruck.Beliebig`), nicht „unbekannt“ und nicht ohne
  Laufzeitprüfung. Die tatsächliche Quellart wird nach dem Verbinden
  fachlich erkannt; nicht übersetzbare Werte werden abgewiesen.
- Das **Übersetzungsziel** wird im Inspector gewählt und gespeichert. Daraus
  leiten sich der präzise Ausgangstyp, zulässige Quelltypen, optionale
  Zusatzparameter und eine Vorschau ab.
- Registrierte `ÜbersetzerDefinitionen` kapseln Quellvoraussetzungen,
  Zieltyp, benötigte Inspector-Felder, Konvertierungsfunktion,
  Verlustfreiheit und Fehlerdiagnostik. Die allgemeine Knotenmechanik
  bleibt domänenneutral, die konkrete Umwandlung fachlich spezialisiert.
- Der Knoten erhält die in `docs/V3_AENDERUNGEN.md` unter V3-006
  beschriebene **einheitliche, kompakte Adaptergeometrie**. Der
  Inspector-Modus und die Übersetzungsrichtung sind im Knoten erkennbar.

#### Erste Übersetzungen

- Tupel → Zeile und Tupel → Spalte.
- Zeile → Tupel und Spalte → Tupel.
- Zeile ↔ Spalte, mit eindeutig definierter Orientierung beziehungsweise
  Transpositionssemantik.
- Term → Methode einschließlich konfigurierbarer Parameter, Argumentordnung
  und Wertevorräte. Die Projektion darf die intern kanonische Signatur
  `Tupel → Tupel` nicht umgehen.
- Weitere Übersetzungen werden über dieselbe Registry ergänzt, ohne die
  allgemeine Knotengeometrie, Inspector-Infrastruktur oder
  Serialisierung neu zu implementieren.

Bestehende Knoten wie `mathematik.tupelZuSpalte`,
`mathematik.tupelZuZeile`, `mathematik.transponieren` und
`mathematik.termZuMethode` bleiben für alte Karten und stabile
Knoten-IDs lesbar. Eine spätere Migration darf nur ausdrücklich
gleichwertige Abbildungen ersetzen; insbesondere Zeile und Spalte sind
keine stillschweigend identischen Werte.

#### Abnahmekriterien

- Inspector-Wechsel passt den Ausgangstyp und die Darstellung korrekt an.
- Ungültige Übersetzungen erzeugen eine fachliche Diagnose.
- Wechsel von Übersetzungszielen erhalten kompatible Handles/Edges und
  warnen vor unvermeidbaren Verbindungsverlusten.
- JSON-/`.matlas`-Roundtrip, Undo/Redo, Desktop/Android und alte
  Spezialknoten sind durch Tests abgedeckt.

### 12. Verschachtelte Tupel konstruieren und rekursiv abflachen

#### Verschachtelte Tupel

Die Tupelsemantik erlaubt bereits, dass ein `Tupel` weitere mathematische
Objekte enthält. Der aktuelle `mathematik.tupel`-Konstruktor verwendet im
Einzeleingabenmodus aber Zahl-Anschlussarten, und die Auswertung verlangt
`ZahlAusdruck`. Diese Einschränkung wird aufgehoben:

- Jeder einzelne Tupel-Eingang akzeptiert einen semantisch gültigen
  mathematischen Wert, insbesondere ein vollständiges inneres Tupel.
- Ein verbundenes Tupel ist genau **ein Element** des neuen Tupels:
  `((a,b),c)` bleibt `((a,b),c)` und wird bei der Konstruktion nicht
  automatisch zu `(a,b,c)`.
- Leere, einstellige und mehrstellige Tupel bleiben unterscheidbar.
  Reihenfolge und rekursiver Strukturtyp werden vollständig erhalten.
- Methodebasierte Tupelkonstruktion und Karten-/Methodensignaturen aus
  Issue #431 bleiben kompatibel.
- Die vorhandenen Modi von `mathematik.tupelErgänzen` behalten ihre
  bisherige Bedeutung: „Tupel ergänzen“ verkettet die direkte Ebene,
  „Elemente ergänzen“ kann ein Tupel als verschachteltes Element anhängen.

#### Neuer Knoten „Tupel abflachen“

Ein neuer Knoten `mathematik.tupelAbflachen` erhält einen Tupel-Eingang
und einen Tupel-Ausgang. Seine Standardoperation ist **rekursives
Abflachen** unter Erhalt der Elementreihenfolge:

- `((a,b),c) → (a,b,c)`.
- `((a,(b,c)),d) → (a,b,c,d)`.
- Leere innere Tupel liefern keine Elemente; nicht-tupelige mathematische
  Werte werden unverändert als Elemente übernommen.
- Die Operation ist strukturell, keine allgemeine Gleichsetzung von
  verschachtelten und flachen Tupeltypen. Beide Typen können inhaltsbezogen
  verwandt sein, sind aber nicht automatisch „selbig“.
- Für bekannte rekursive Typen wird der resultierende Tupeltyp abgeleitet.
  Bei offenen Strukturen wird die Unbestimmtheit sichtbar, nicht als
  erfundene feste Elementzahl dargestellt.

#### Abnahmekriterien

- Verschachtelung und rekursives Abflachen funktionieren für heterogene
  Elemente, mehrere Ebenen und leere Tupel.
- LaTeX-/Strukturvorschau zeigt die wirkliche Klammerstruktur.
- Vorhandene Tupel-, Methoden- und Erweiterungsmodi regressionsfrei;
  alte Karten laden unverändert.
- Tests für Typinferenz, Kopieren, Undo/Redo und Persistenz.

### 13. Matrixprodukt als variadischer Operator in beiden Knotenfamilien

#### Nachgewiesene Ist-Abweichungen

- Der eigenständige Knoten `mathematik.matrixProdukt` hat zwei feste
  Eingänge `a` und `b` ohne dynamische Erweiterbarkeit.
- Im Matrixrechner sind `matrix.produkt` und
  `MatrixRechnerOperator.MATRIXPRODUKT` im Quellcode definiert. Nach der
  Nutzerrückmeldung ist der Operator in der tatsächlichen Auswahl aber
  nicht verfügbar. Die Sichtbarkeit über Registry, Inspector, Suche,
  Operatorauswahl und Laufzeitkonfiguration ist deshalb end-to-end zu
  prüfen und zu reparieren. Die bloße Enum-Existenz ist kein
  Abnahmekriterium.
- Die Strukturrechnerdefinition für `matrix.produkt` hat die festen
  Rollen `links` und `rechts`.
- `MatrixRechner.erzeuge` fordert für das Matrixprodukt derzeit genau
  zwei Matrizen; der eigenständige Knotenauswerter rechnet ebenfalls
  ausschließlich zwei Eingänge.
- `MatrixproduktFalkSchema.kt` liest nur `a` und `b` aus der
  Knotenauswertung. Zusätzliche Faktoren können daher bisher weder
  vollständig dargestellt noch im Schema geprüft werden.

#### Gemeinsamer variadischer Faktorvertrag

- Eigenständiger Matrixproduktknoten **und** Matrixrechner-Operator
  benutzen dieselbe fachliche Definition des geordneten Matrixprodukts.
- Mindestens zwei Faktoren; nach dem Verbinden wird ein weiterer
  kompatibler Eingang angeboten. Die Zahl fester Eingänge ist im Inspector
  veränderbar. Die gemeinsame Portdefinition aus Schritt 8 bildet
  Reserveschluss, Mindestanzahl und dynamisch erzeugte Anschlüsse ab.
- Matrix, Spalte als `n×1` und Zeile als `1×n` werden als Operanden
  unterstützt. Jeder neue Faktor wird auf seinen tatsächlichen
  mathematischen Wert- und Formvertrag geprüft.
- Berechnung in **strenger Anschlussreihenfolge** als linksassoziativer
  Ablauf: `P₁ = A₁ · A₂`; `P₂ = P₁ · A₃`; allgemein
  `Pₖ = Pₖ₋₁ · Aₖ₊₁`. Es darf keine Vertauschung der Faktoren geben,
  auch nicht bei nichtkommutativen Skalaren.
- Für jeden Zwischenschritt muss die Spaltenzahl des bisherigen Produkts
  mit der Zeilenzahl des nächsten Faktors übereinstimmen. Ein Fehler nennt
  Faktorindex und widersprüchliche Dimensionen.
- Fachkern, Operator-Knotenkonfigurator, Inspector, Suchindex,
  Definitionskarten und Renderer beziehen dieselbe Signatur; keine
  weitere paarweise Sonderimplementierung.
- Persistierte Zweifaktor-Karten behalten Anschluss-IDs und Edges.
  Historische Anschlussnamen `a`/`b` und `links`/`rechts` werden
  migrationssicher auf geordnete Rollen abgebildet.
- Der bestehende Matrixtyp des Ergebnisses und die
  Zeilen-/Spaltenprojektionen dürfen nicht stillschweigend geändert werden.

#### Ein **einziges** mitwachsendes Falk-Schema (verbindlich)

Das Falksche Schema für drei oder mehr Matrixfaktoren wird **nicht in
mehrere Schemata, Tabs oder isolierte Schrittansichten aufgeteilt**.
Stattdessen wächst **ein zusammenhängendes Falk-Schema** dynamisch:

- **Querformat / ausreichend breite Ansicht:** Jeder zusätzliche Faktor
  ergänzt dem bestehenden Schema weitere **Spalten**.
- **Hochformat / schmale Ansicht:** Derselbe Inhalt wird vertikal
  organisiert, und jeder zusätzliche Faktor ergänzt weitere **Zeilen**.
- Im jeweils neuen Schemaabschnitt wird die **nächste Matrix unmittelbar
  mit dem zuvor berechneten Zwischenergebnis** multipliziert. Das neue
  Zwischenergebnis wird wiederum direkt für den folgenden Abschnitt
  verwendet. Für `A·B·C·D` sind die aufeinanderfolgenden Ergebnisse
  `AB`, `(AB)C` und `((AB)C)D` innerhalb **derselben** Darstellung zu
  sehen.
- Jeder Abschnitt zeigt den relevanten linken Zwischenfaktor, den
  nächsten rechten Faktor, das neue Produkt und die zugehörigen
  Falk-Summen/Indexbeziehungen. Die Faktoren und Zwischenergebnisse
  behalten über die ganze Ansicht konsistente Beschriftungen.
- Der Zustand des Schemas stammt aus einer geordneten Faktorliste und
  einem laufenden Satz von Zwischenprodukten, **nicht** nur aus
  `eingänge["a"]` und `eingänge["b"]`.
- Einzelne Einträge, Indexpaare oder Faktoren dürfen hervorgehoben
  werden; eine solche Selektion verändert nur den Fokus innerhalb des
  **einen** Schemas und erzeugt keine separate Schrittseite.
- Bei vielen Faktoren oder großen Matrizen sind horizontales bzw.
  vertikales Scrolling, Culling und adaptive Beschriftungen zulässig.
  Diese Performancehilfen dürfen die logische Einheit des Schemas
  nicht aufheben oder Faktoren stillschweigend auslassen.
- Fehlende Faktoren, nicht auswertbare Größen und die erste unzulässige
  Dimensionspaarung müssen im zusammenhängenden Schema sichtbar sein.
  Ein Standardbeispiel darf echte Eingaben nicht unbemerkt ersetzen.
- Derselbe Falk-Datenkern dient dem eigenständigen Matrixprodukt und
  dem Matrixrechner-Operator. Die Portreihenfolge ist die
  Darstellungsreihenfolge.

#### Abnahmekriterien

- `matrix.produkt` ist sichtbar und über den Operatorauswahldialog im
  Matrixrechner auswählbar; der Operatorwechsel wahrt mögliche Edges.
- Beide Varianten nehmen mindestens 2, 3, 4 und 5 dynamische Faktoren an
  und liefern bei kompatiblen Formen dasselbe Ergebnis.
- Falk-Schema für `A·B·C` und `A·B·C·D` bleibt jeweils **ein**
  wachsendes Schema: zusätzliche Spalten im Querformat beziehungsweise
  Zeilen im Hochformat, mit korrekten Zwischenergebnissen in jedem
  Abschnitt. Keine Schritt-Tabs.
- Matrix-/Zeilen-/Spaltenprodukte, fehlerhafte Mitteldimensionen und
  nichtkommutative Einträge werden geprüft.
- UI-Previews für Smartphone hochkant, Tablet quer und Desktop sowie
  mathematische Kern-, Adapter-, Migrations- und Persistenztests.

### 14. Einheitlicher Formeleditor und dauerhaft gespeicherte Stiftzeichnung

#### Ein Dialog, drei Eingabewege

Der vorhandene `FormelBauerDialog`, spezialisierte
`StrukturFormelBauerDialog`-Aufrufer und spätere Eingabeadapter werden
unter **einer gemeinsamen Dialogoberfläche** zusammengeführt:

1. **Tastatur / strukturierte Bearbeitung:** AST-basierter CAS-Formelbauer,
   strukturierte Tasten, Cursor, Eingabe und gegebenenfalls LaTeX.
2. **Stift:** Zeicheneingabe auf einer separaten Fläche mit Radiergummi,
   Auswahl, Undo/Redo und unmittelbarer Formelinterpretation.
3. **Foto:** Kamera beziehungsweise Bildimport, Erkennung und
   nachträgliche Korrektur der erkannten mathematischen Formel.

Alle Eingabewege münden in **dasselbe strukturierte Formelmodell** und in
ein gemeinsames, editierbares Interpretationsfeld. Erkennungsvorschläge
werden vor der Übernahme bestätigt beziehungsweise korrigiert. Die
Eingabeadapter bleiben von der Formel-AST-Verarbeitung getrennt. Android
und Desktop verwenden dieselbe Compose-Oberfläche, aber
plattformspezifische Kamera-, Datei- und Stiftadapter.

#### Zeichnung ist Bestandteil der Karte

Die **Original-Stiftzeichnung muss dauerhaft mit der betreffenden Karte
und ihrem konkreten Formel-/Knotenkontext gespeichert** werden, unabhängig
davon, ob die automatische Interpretation später verändert wird.

- Persistiert werden versionierte **Vektorstriche** mit geordneten
  Stützpunkten, Strichzugehörigkeit und den für die Darstellung
  notwendigen Stifteigenschaften (zum Beispiel Breite). Nicht nur ein
  Screenshot und nicht allein das erkannte LaTeX.
- Rohzeichnung, erkannter/gegebenenfalls korrigierter Formel-AST und
  sichtbares LaTeX sind getrennte Zustände mit eindeutiger
  Zuordnung. Korrekturen am AST überschreiben nicht automatisch die
  Originalstriche.
- Das plattformneutrale Modell lässt sich beispielsweise über die
  bereits rekursiven `KnotenEigenschaft`-Werte versioniert abbilden.
  Speichergrenzen und Geometriekompression sind zu prüfen, damit lange
  Zeichnungen nicht unnötig große Karten erzeugen.
- Kartenspeicherung, Neuöffnen, `.matlas`/JSON, Duplizieren, Undo/Redo,
  Import und Export müssen die Zeichnung erhalten. Alte Karten ohne
  Handschriftdaten bleiben lesbar.
- Die Quellfoto-Aufbewahrung ist **nicht automatisch** durch die
  Verpflichtung zur Stiftzeichnungsspeicherung entschieden; hierzu ist
  vor einer eventuellen Fotopersistenz eine gesonderte Speicher- und
  Datenschutzentscheidung nötig.
- Für Handschrift- und Bilderkennung ist eine austauschbare
  Erkennungsschnittstelle vorzusehen. Bevorzugt wird die Prüfung einer
  lokal nutzbaren Erkennung; konkrete Modelle und Qualität/Größe sind
  noch durch Tests zu entscheiden.

Die vollständige Erkennung gehört nach Realisierungsaufwand in die
Beta-Ausbaustufe. Das gemeinsame Dialog-/Datenmodell und die verlustfreie
Stiftpersistenz werden bereits in der Alpha-Architektur verbindlich
festgelegt.

#### Abnahmekriterien

- Ein und derselbe Formeleditor kann zwischen Tastatur, Stift und Foto
  umgeschaltet werden und zeigt eine gemeinsame, korrigierbare
  Interpretation an.
- Nach Schließen und Wiederöffnen der Karte ist die Stiftzeichnung
  einschließlich Strichfolge und Radierungen rekonstruierbar.
- Nachträgliche Korrekturen am Formelbaum erhalten die Rohzeichnung,
  sofern diese nicht bewusst bearbeitet oder entfernt wird.
- Roundtrip-, Undo-/Redo-, Fehler-/Grenzfall- und
  Android-/Desktop-Oberflächentests.

### 15. Reihenfolge, Abhängigkeiten und offene Folgeentscheidungen

#### Empfohlene Umsetzungspakete

1. **UI-Korrektur:** Paddings der Operatorauswahl und Preview-/Screenshot-
   Regressionen ohne Abhängigkeit von größeren Refactors.
2. **Werte-/Methodenvertrag:** Einen neutralen `AtlasWert`-/Methodenkern,
   kanonisches `Tupel → Tupel` sowie effektive Inspector-Typverträge
   konsolidieren. Überlappende historische PRs nicht ungeprüft mischen.
3. **Operatorinfrastruktur (Schritt 8):** Gemeinsame Registry,
   variadische Portdefinitionen, sichere Wechselvorschau und
   fachliche Adapter; Matrixrechner zuerst.
4. **Mathematische Strukturknoten:** Verschachtelte Tupel,
   `tupelAbflachen` und die neue Übersetzerknotenfamilie ergänzen.
5. **Matrixprodukte:** Beide Knotenvarianten auf einen variadischen
   Fachkern bringen und den Matrixoperator sichtbar machen.
6. **Falk-Schema:** Einen mit der Zahl der Faktoren horizontal oder
   vertikal wachsenden Gesamtaufbau einschließlich Zwischenprodukten
   implementieren und auf Android/Desktop visuell testen.
7. **Formeleditor-Basis:** Gemeinsamer Compose-Dialog,
   strukturiertes Eingabemodell und persistierbare Stiftstriche;
   eigentliche Bild-/Handschrifterkennung anschließend in Beta.

#### Noch nicht als Produktentscheidung festgelegt

- Die **konkrete spätere Major-Version** für den Beweisatlas, sein
  Beweis-/Vertrauensmodell und die Berechtigungen für verifizierte
  Aussagen (vgl. Issue #351 und #344).
- Ein verbindliches Referenzgerät und messbare Grenzwerte für
  Framezeit, Speicher und große Karten; diese sind vor
  Performancefreigaben festzulegen.
- Der genaue Umfang von erweiterbaren fremden Operatorpaketen und die
  hierfür geeignete begrenzte, nicht frei ausführbare DSL
  (vgl. Issues #291 und #319).
- Die weitere mathematische Grundlagenplanung zu Quaternionen,
  Hyperzahlen und Nichtstandardanalysis (u. a. Issues #111 und #143)
  bleibt von der hier erforderlichen Typfundamentarbeit getrennt.
- Konkrete Handschrift-/Fotoerkennungsmodelle, Fallbacks und
  gegebenenfalls Bildquellenspeicherung.

#### Globale Abnahme

Alle neuen Knoten und Konfigurationsmodi benötigen fachliche
Rechenkernprüfungen, Graph-/Anschlussprüfungen, Versionsmigrationen,
Karten-JSON- und `.matlas`-Roundtrips, Undo/Redo und Android-/Desktop-
Regressionen. Bestehende Karten und stabile Handles dürfen nicht ohne
expliziten, geprüften Migrationsgrund verändert werden.


## Verifikation

- Repository-, Releaseplan- und Versionsfolgeprüfung
- vollständige JVM-Tests einschließlich Desktop-Shadowmodule
- Android-Lint für Debug
- Android-Debug-APK

Nicht durch die lokale Prüfung ersetzt werden Gerätebedienung, Frame-Timing und
Lebenszyklustests auf einem realen Android-Gerät.

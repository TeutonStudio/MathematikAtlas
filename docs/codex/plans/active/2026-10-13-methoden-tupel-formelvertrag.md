# ExecPlan: Methoden-, Tupel- und Formelvertrag

## Status
2026-10-13; in Arbeit. Ausgangsbasis HEAD dc072189 auf samai/v3.2.1-variable-grundraum-tupel-ordnung einschließlich vorhandener Änderungen. Kein Release/Commit beauftragt.

## Ziel und Nutzerwirkung
77 Vertragsanforderungen anhand des lokalen Bestands abschließen. Struktur, Identitäten, Typen, Bereiche und Voraussetzungen bleiben über Methode, Formel, Karte und Speicherung erhalten. Referenz: Q(f,x,h)=(f(x+h)-f(x))/h, f(t)=t² ergibt 2x+h unter h≠0.

## Nicht-Ziele
Keine Veröffentlichung, unbeschränkte allgemeine Integration oder globale numerische Inversion. Keine Änderungen fremder Arbeitsstände.

## Untersuchter Istzustand
Methoden.kt verwendet argument-$index, namensbasierte Substitution und skalare Komposition. FormelGraph überträgt Bedingungen nicht. StrukturFormelCodec Version 1 speichert Literale als LaTeX. TupelOperationen verweigert 0/1 Operanden. Strukturquellen werden in ÜbersetzerKnoten bereits über Bindungs-IDs gruppiert, andere Quellen weiterhin über Namen. KartenSchnittstellenSignatur sortiert nach Layout. Vergleichspfade: FormelBindungen, MethodenFundament, bestehende Krawczyk- und Gauss-Implementierungen. Vorheriger Diff außerhalb des Repositorys unter /tmp/mathematik-vertrag-basis/vorher.patch gesichert.

## Fachliche und mathematische Semantik
Leere und einstellige Tupel bleiben eigenständig. Ein strukturiertes Argument bleibt eine äußere Komponente. Auswertungen bewahren offene Bedingungen; widerlegte Bedingungen verhindern Anwendung. Numerische Approximation ist kein Existenz-/Bijektivitätsbeweis.

## Daten-, Node-, Handle- und Edge-Vertrag
Keine neuen Knotentypen geplant. Gemeinsamer semantischer Kontext trägt Quellen-IDs, Rollen, vollständige Typen, Bindungen und Annahmen. Anschlussprojektionen ändern keine logischen Argumente. Namen/Positionen sind keine Identitäten.

## Architekturentscheidungen
Bestehende Register und Bindungsmodelle erweitern. Mathematik im Kern; Kartenkoordination im Adapter; Codec-Integration in mathematischer Migrationsfassade. LaTeX plus Kontext für verlustfreien Austausch. Alte Formate lesen, neue versioniert schreiben.

## Betroffene Dateien und Symbole
Kern: Methoden, MethodenFundament/Vertrag, FormelGraph/Bindungen, StrukturFormelEditor, TupelOperationen; fachliche Operatoren und Codecs. Adapter: KartenSchnittstellenSignatur und Aufruf. Knoten: Übersetzer und Migrationsfassade. Plattformen nur für gemeinsame Vertragsintegration.

## Meilensteine und Anforderungsmatrix
| Etappe | Prüfpunkte | Zustand | Nachweis |
|---|---|---|---|
| 1 Identitäten/Signaturen | M01–M10, I01–I02, I07 | begonnen | ID-Maps, explizite Bindungs-IDs, Quellenkontext und Reihenfolgemigration getestet; Legacyintegration offen |
| 2 Anwendung/Substitution | M11–M16, M21–M23, I03–I06 | begonnen | Kanonischer Aufruf, partielle Bindung, mehrdimensionale Komposition, verschachtelte Voraussetzungen getestet |
| 3 AST/Parser | F01–F16, F23–F24 | begonnen | Deklarierter Methodenimport und Kernformelauswertung; vollständige UI-/Registerintegration offen |
| 4 Übersetzung/Persistenz | T01–T14, F17–F22, I08–I09, I12–I14 | begonnen | Verlustpfade lokal bestätigt |
| 5 Operatoren/Algorithmen | M17–M20, M24, I10–I11 | begonnen | Affine Inversion, partielle Ableitung, Polynomintegration, numerische Jacobi/Inversion/Quadratur getestet |
| 6 Konformität | I15 und alle vorigen | begonnen | Gesamtregression grün; 77-Punkte-Freigabe und Laufzeitabnahme offen |

## Konkrete Umsetzungsschritte
1. Verlustfreie Formelbedingungen/Literalpersistenz und Tupel-Randfälle absichern.
2. Identitäten/Kontext und kanonischen Aufruf integrieren, Legacyadapter isolieren.
3. Allgemeine Methodenanwendung, Substitution, Komposition, Parser und Kartenprojektionen verbinden.
4. Übersetzungszeugnisse, Migration und Renderer vereinheitlichen.
5. Exakte affine Inversion, strukturierte Ableitungen/Polynomintegration sowie begrenzte numerische Verfahren ergänzen.
6. Alle Anforderungen einzeln mit Tests belegen und unabhängig verifizieren.

## Tests und Validierung
Gezielte Kern-/Adapter-/Knotentests nach Änderung; abschließend python3 scripts/pruefe_repository.py, pruefe_releaseplan.py, pruefe_versionsfolge.py, pruefe_kern.py sowie ./gradlew test :app:assembleDebug :app:lintDebug und Desktoptests. Laufzeitnachweise separat von Builds behandeln.

## Persistenz und Migration
Versionierten Strukturcodec mit Lesepfad für Version 1 schaffen. Verlust historischer Bedingungen nicht rekonstruieren/erfinden. Originaldaten bei nicht unterstützten Typen erhalten und Diagnose liefern. Migrationen idempotent, bestehende IDs und Verbindungen erhalten.

## Risiken und Rückfallstrategie
Umfangreicher vorhandener uncommitteter Arbeitsstand; nur aufgabeneigene Änderungen ergänzen. Unbekannte Objektvarianten dürfen nicht durch LaTeX-Ersatz kaschiert werden. Etappen bleiben getrennt prüfbar; keine Freigabe ohne vollständige Evidenz.

## Fortschritt
- [x] Ausgangsbasis und tatsächliche Verlustpfade untersucht.
- [-] Vertragsfundament und Persistenz.
- [ ] Vollständige 77-Punkte-Evidenzmatrix.
- [ ] Unabhängige Abschlussverifikation.

## Entscheidungsprotokoll
2026-10-13: Nutzer bestätigt lokalen Arbeitsstand, bedingte Ausdrücke, Migration alter Daten, prüfbare Etappen, LaTeX plus Kontext sowie zusätzliche begrenzte numerische Algorithmen.

## Abweichungen vom ursprünglichen Plan
Verlustfreie Grundbausteine werden vorgezogen, um weitere Vertragsänderungen sicher speichern und testen zu können. Keine Gesamtfreigabe daraus ableiten.

## Ergebnis und Verifikation
Teilstand implementiert, Gesamtplan NICHT abgeschlossen. Finale Prüfung 2026-10-13:
- `./gradlew test :app:assembleDebug :app:lintDebug :desktopApp:compileKotlin`: erfolgreich, 219 Tasks, 1m32s. Vorhandene finale XML-Berichte: 1689 Tests, 0 Fehler, 0 Überspringungen.
- Repository-, Architektur-, Methodenmodell-, Desktop-Shadowmodule-, Releaseplan- und Versionsfolgeprüfung erfolgreich; `git diff --check` erfolgreich.
- `python3 scripts/pruefe_kern.py`: nicht ausführbar, weil standalone `kotlinc` im PATH fehlt. Gradle-Kerntests bestehen.
- Unabhängige statische Zwischenverifikation identifizierte und korrigierte Indexumbenennung, Adapter-ID-Substitution, Raumcodec-Lücken sowie Verlust von Bedingungen in direkter/partieller/verschachtelter Anwendung und beim Kopieren von Methoden. Konkrete Gegenbeispiele als Regressionen ergänzt. Letzte Korrektur unabhängig nachvollzogen; keine Gesamtfreigabe erteilt.
- Der Differenzenquotient ist im Kern mit konkreter Quadratfunktion und h=0-Ablehnung getestet. Der vollständige UI-/Karten-/symbolische 2x+h-Abnahmepfad ist noch offen.
- Keine Android-/Desktop-Interaktionsabnahme, kein Release, Commit oder Push. Branch unverändert `samai/v3.2.1-variable-grundraum-tupel-ordnung`, HEAD `dc072189446fb387af3b9ff04936e0bf12a4d7f7`; keine neu erzeugten Autor-/Committermetadaten.
- Bekannte Restarbeit ist in der folgenden Einzelmatrix ausdrücklich ausgewiesen. Numerische Abschätzungen sind keine zertifizierten Einschließungen; allgemeine Krawczyk-Integration und mathematische Gesamtverifikation fehlen.



## Einzelanforderungen – Zwischenstand
`belegt` bezeichnet den konkret getesteten Teilpunkt, nicht die Gesamtfreigabe. `teilweise` lässt ausdrücklich weitere Integration/Abdeckung offen. Grundlage ist der lokale Arbeitsstand einschließlich der Ausgangsänderungen.

| Punkt | Stand | Evidenz beziehungsweise Restarbeit |
|---|---|---|
| T01 | belegt | Bestehende Tupel-/Indexverträge und Kernregression. |
| T02 | belegt | Leeres Tupel und Tupelraum; Codec-/Konformitätsregression. |
| T03 | teilweise | Gemeinsame Produkte vorhanden; nicht sämtliche Verbraucher nachgewiesen. |
| T04 | teilweise | Codec unterscheidet Skalar/Einertupel/Vektor; vollständige Anschlussprüfung offen. |
| T05 | teilweise | Komponentenräume im Codec; alle Übersetzer noch nicht abgeschlossen. |
| T06 | belegt | TupelOperationen und VertragsRoundtripTest: 0/1/n. |
| T07 | belegt | Leere Ergänzung ist im Kern Identität; UI-Aktion darf ein No-op bleiben. |
| T08 | teilweise | Kernrandfälle ergänzt, umfassende Knotenabdeckung offen. |
| T09 | teilweise | Verschachtelung im Codec erhalten; Raumübersetzer nicht vollständig. |
| T10 | offen | Vollständiger strukturtreuer Tupel-/Vektor-Übersetzungsvertrag. |
| T11 | offen | Explizite Übersetzungszeugnisse für heterogene Räume. |
| T12 | teilweise | Strukturverschiedenheit getestet; Darstellungs-/Gleichheitsvertrag nicht vollständig. |
| T13 | teilweise | Bestehende Indexverträge; vollständige Konsolidierung offen. |
| T14 | teilweise | Vorhandene Strukturquellen und ergänzte Komponenten-IDs; alle Variablenpfade offen. |
| M01 | offen | Scalar-Legacyspeicher weiterhin vorhanden; kanonische Tupelsicht vorhanden. |
| M02 | teilweise | Gespeicherte ID-Maps und ID-Codec; alle Erzeugungs-/Umbenennungspfade noch nicht migriert. |
| M03 | teilweise | Explizite IDs werden ausschließlich per ID substituiert; Legacy-Namenspfade bleiben. |
| M04 | teilweise | Bestehende strukturierte Parameter; vollständiger gemeinsamer Kontext fehlt. |
| M05 | teilweise | Verschachtelte Werte/Typen persistierbar; komplette Anschlussintegration offen. |
| M06 | teilweise | MethodenVariable, höhergeordneter Aufruf und Differenzenquotient im Kern getestet. |
| M07 | teilweise | Kanonische Tupelanwendung vorhanden; alle öffentlichen Aufrufer noch nicht migriert. |
| M08 | teilweise | Kanonische Ergebnistupel; Legacy-Projektionen bleiben. |
| M09 | teilweise | ID-basierte abhängige Zielraumsubstitution im Adapter getestet; allgemeine Signaturrestriktionen offen. |
| M10 | teilweise | Quellenrollen/Bindungsarten ergänzt; kein durchgängiger gemeinsamer Kontext aller Knoten. |
| M11 | teilweise | Neue Anwendung liefert Tupel plus Bedingungen; alte APIs packen weiterhin aus. |
| M12 | belegt | Gemeinsamer Definitionsraum und Komponentenbereiche vor Anwendung; unbekannte Voraussetzungen erhalten. |
| M13 | teilweise | bindeIdentitäten erzeugt reduzierte Methode mit erhaltenen IDs; Legacy-Bindungsoberfläche bleibt. |
| M14 | teilweise | Mehrdimensionale Tupelkomposition/Kompositionsketten getestet; alle Objekt-/Binderfälle offen. |
| M15 | teilweise | Bestehende Restriktion/Bereichsanpassung regressionsgeprüft; vollständiger Vertrag offen. |
| M16 | offen | Allgemeine nullstellige/mehrwertige Bildmengenbildung noch nicht migriert. |
| M17 | teilweise | Rational-affine exakte Inversion und begrenzte lokale numerische Inversion; Knotenoperatorintegration offen. |
| M18 | offen | Iteration weiterhin über bisherigen Spezialpfad. |
| M19 | teilweise | Neue partielle Ableitung/Polynomintegration und Numerik; bestehende Analysispfade nicht vollständig vereinheitlicht. |
| M20 | offen | Allgemeine strukturierte punktweise Hebung. |
| M21 | teilweise | ID-Substitution und lexikalische Methodenbindung; umfassende Capture-Vermeidung aller Binder offen. |
| M22 | teilweise | StrukturellerMethodenAufruf vorhanden; alle historischen Symbolfallbacks noch nicht abgelöst. |
| M23 | teilweise | Aufrufbedingungen über direkte/partielle/verschachtelte Anwendung getestet; Gesamtintegration offen. |
| M24 | offen | Separate neutrale Engine-Auswertungs-Capability noch nicht ergänzt. |
| F01 | teilweise | FormelSymbolRolle im Quellenkontext. |
| F02 | teilweise | AST-Operation methode.anwenden, Kern- und Knoten-Auswertung; Formelbauerpalette offen. |
| F03 | teilweise | Nullstellige Tupelkonstruktion erlaubt; allgemeine Registerkardinalitäten offen. |
| F04 | teilweise | Vollständige Typen im Quellenkontext; AST behält FormelTyp. |
| F05 | teilweise | Neue Kern-Zahlenformelauswertung nutzt ZahlenRechnerKatalog; übrige Familien offen. |
| F06 | teilweise | Methodenargumente getestet; allgemeine Methodenrückgaben/Verkettung offen. |
| F07 | teilweise | Explizite Tupeloperation und verlustfreier Literalcodec; allgemeiner Tupelparser offen. |
| F08 | teilweise | Explizite Projektion neben Methodenaufruf; vollständiger Indexvertrag offen. |
| F09 | teilweise | Variadische Zahlenrollen im Kern; alle Rechnerfamilien offen. |
| F10 | teilweise | Quellen, vollständige Typen und vorhandene Quantorbindungen persistierbar. |
| F11 | teilweise | Mathematische ID-Substitution; AST-Binderumbenennung nicht allgemein implementiert. |
| F12 | offen | Einheitliche Summen-/Produkt-/Integral-/Quantorbinder. |
| F13 | teilweise | Deklarierte frei benannte Methoden mit Grundparser getestet; sämtliche LaTeX-Namensformen offen. |
| F14 | teilweise | Deklarationen mit Methodensignaturen importierbar; allgemeine UI-Deklaration offen. |
| F15 | teilweise | Kontext unterscheidet deklarierte Methoden; vollständige Mehrdeutigkeitsdiagnostik offen. |
| F16 | offen | Einheitlicher Parser/Renderer/Auswertervertrag aller Rechnerfamilien. |
| F17 | teilweise | LaTeX-plus-Kontext-Paket erhält unterstützte AST-Struktur; vollständige Abdeckung offen. |
| F18 | belegt | Formelgraph überträgt Bedingungen, Literale und optionalen Bindungskontext; Roundtriptests. |
| F19 | teilweise | Expliziter typisierter Wertcodec; nicht alle mathematischen Objektvarianten registriert. |
| F20 | teilweise | Strukturcodec v2 plus gebundener Kontextcodec; vollständige UI-Persistenzintegration offen. |
| F21 | teilweise | Allgemeine Aufruf-/Projektionsdarstellung in zwei Renderern; Konsolidierung offen. |
| F22 | offen | Vollständige Formelbauer-/Karten-/Rechner-Synchronisation. |
| F23 | teilweise | Explizite Import-/Codec-/Auswertungsdiagnosen; einheitliche strukturierte Diagnosetypen offen. |
| F24 | teilweise | Gebundene Kernformeln erhalten Voraussetzungen und nutzen gemeinsame Methodenaufrufe. |
| I01 | teilweise | Explizite Parameter-IDs und Quellentypen; sämtliche Quellen-/Komponentenpfade nicht vereinheitlicht. |
| I02 | teilweise | Formelkontext und MethodenVariable; vollständige VariablenQuelle-Anbindung offen. |
| I03 | teilweise | Term-zu-Methode übernimmt Quellen-/Komponentenidentitäten und Voraussetzungen; namensbasierte Gruppierung bleibt. |
| I04 | teilweise | Gemeinsamer kanonischer Aufruf für neue Kernformeln und Knotenformeln; volle Palette offen. |
| I05 | teilweise | Bestehende Anschlussprojektionen bleiben; vollständige ID-Zuordnung offen. |
| I06 | teilweise | Adapter verwendet kanonische Anwendung; Kartenmethodenerzeugung nicht vollständig migriert. |
| I07 | teilweise | Explizite Reihenfolge mehrkomponentiger Grenzen, Migration und neue Indexdefinitionskarten; übrige Erzeugungspfade offen. |
| I08 | offen | Allgemeine Metadatenerhaltung aller Übersetzer. |
| I09 | teilweise | Vorhandene Polarkonventionsmigration; vollständige Vertragsabnahme offen. |
| I10 | offen | Strukturierte Bildmengen/Methodengraphen durchgehend integrieren. |
| I11 | offen | Einheitliche natürliche Koordinatenprojektion im Visualisierer. |
| I12 | teilweise | Vollständige Typen im Kontext; dynamische Anschlussableitung nicht durchgängig. |
| I13 | teilweise | Strukturcodec v1 lesbar, v2 typisiert; Gesamtmigration historischer Methoden/Formeln offen. |
| I14 | teilweise | Indexumbenennung und gespeicherte Reihenfolge getestet; gesamte Inspector-/Portintegration offen. |
| I15 | teilweise | Neue Konformitätstests in Kern und Adapter; vollständige 77-Punkte-Abnahme und UI-Laufzeit fehlen. |


## Ergänzung 2026-10-13: Übersetzungszeugnisse

Der abgegrenzte Übersetzungsvertrag wird in [eigenem ExecPlan](2026-10-13-uebersetzungszeugnisse.md) umgesetzt und geprüft. Er ergänzt Nachweise für T05/T09/T11/T12, F18–F20 sowie I08/I13–I15: getrennte Abbildungseigenschaften/Informationserhaltung/Genauigkeit, exakter symbolischer Bildraum, Strukturpläne, aktive Zeugnisse gegenüber Herkunft, selbsttragende Literal-/Graph-/Methoden-/Exportcodecs. Dies schließt nicht automatisch sämtliche übrigen 77 Prüfpunkte ab. Android-Gerätelaufzeit bleibt mangels Gerät/AVD gesondert offen.

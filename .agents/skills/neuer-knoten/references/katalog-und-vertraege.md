# Kanonischer Katalog, Legacyprüfung und verlustfreie Verträge

Diese Regeln gelten bei neuen/wesentlich geänderten Knoten und gezielter Konsolidierung. Pfade sind relativ zur Repositorywurzel. Prüfe Symbole und tatsächliche Aufrufer am Arbeitsstand; historische Analysen und aktive Pläne können überholt sein. Die Regeln sind Abnahmekriterien für betroffene Produktänderungen, keine Behauptung bereits vollständiger Umsetzung.

## 1. Zuständige Quellen und bevorzugte Pfade

| Entscheidung | Maßgeblicher Bestand |
| --- | --- |
| Sichtbare mathematische Erzeugung | `MathematikKnoten/.../katalog/KanonischerMathematikKnotenKatalog.kt`: `alle`, `anschlussVarianten`, `variantenKennung`; Android-/Desktopfassaden beziehen denselben Katalog. |
| Historische Ladbarkeit | `MathematikKnotenVorlagen`, `alleMathematikKnotenVorlagen`, bestehende Migrationen und Auswerter; nicht mit sichtbarer Erzeugung gleichsetzen. |
| Rechensemantik | Kernkataloge/`RechnerOperatorDefinitionen`; `RechnerOperatorRegistry` und `RechnerFamilie` in `RechnerOperatorVertrag.kt` projizieren vorhandene Zahl-/Vektor-/Matrix-/Tensoroperatoren. Mengen-/Aussagenrechner und Prädikate haben eigene Bestandspfade, keine erfundene zusätzliche `RechnerFamilie`. |
| Gesamte Auswerterregistrierung | `katalog/GesamterMathematikAuswerter.kt`, `StandardMathematikAuswerterPakete`; benannte Reihenfolge samt späteren Wrappern erhalten. |
| Mathematische Kartenpersistenz | `migration/MathematikKartenCodec.kt` mit `vorSpeichern`, `nachDekodierung`, `nachLaden`; `lade`/`importiere` sind nicht durch einen bloßen neutralen JSON-Decode ersetzt. |
| Wert-/Formelverträge | Kern: `MethodenVertrag.kt`, `MethodenFundament.kt`, `KanonischeMethodenanwendung.kt`, `GebundeneFormelAuswertung.kt`, bestehende ASTs und Codecs. |

Für jedes Vorhaben vor dem neuen Typ-Schlüssel ausdrücklich prüfen:

| Fachlicher Zweck | Bevorzugte Umsetzung |
| --- | --- |
| Berechnung in vorhandener Domäne | Bestehender Rechneroperator oder Erweiterung desselben samt stabiler Operator-ID, Signatur und gemeinsamer Auswahl. |
| Darstellungs-/Strukturwechsel | `atlas.uebersetzer` mit gespeichertem Ziel und Unteroptionen; keine neue Spezialadapterfamilie. |
| Relation, Mitgliedschaft oder Axiom | Bestehender Prädikatpfad (`PraedikatKnotenVorlagen`, `RelationsOperatoren`, `AxiomOperatoren`) mit typisiertem Ergebnis und Nachweisstatus. Aussageoperation, Prädikat und Lösungsmenge nicht verwechseln. |
| Wiederverwendbare Methode | `mathematik.methode` für Identität, konfigurierte Vorlage/Operator oder versionsfeste Karte; bestehender Methodenaufruf für Anwendung. `termZuMethode` bleibt für die tatsächlich andere Bildung aus einem Ausdruck zuständig. |
| Strukturierte Variable | Bestehende `mathematik.variable`-Konfiguration und Grundraum; entfernten Tupelvariablen-Spezialknoten nicht wieder einführen. |
| Neue fachliche Rolle | Eigenständigen Typ nur begründen, wenn vorhandene Operator-, Übersetzer-, Prädikat- oder Methodenpfade die Semantik nicht korrekt ausdrücken können. |

Eigenständige Definitionen, Reflexion, Quellen, Geometriefelder und Operationen mit anderen Domänen/Nachweisen sind nicht allein wegen ähnlicher Namen redundant. Insbesondere kontextabhängige Geometry-Nodes-Felder nicht ohne Nachweis mit gewöhnlichen Rechnerwerten gleichsetzen. Eine gemeinsame Implementierung rechtfertigt nicht automatisch die Aufhebung eines öffentlichen Vertrags.

## 2. Legacybefund vor Bereinigung belegen

Erstelle für jeden Kandidaten eine kompakte Tabelle: Altart/Altoperator, Status (sichtbar, nur ladbar, nur Migration, noch aufgerufen), kanonisches Ziel samt Konfiguration, Verbraucher, Migrationspfad, Tests und offene Verluste. Ermittle Verbraucher in Produktionscode, Registrierungsphasen, gespeicherten Beispiel-/Definitionskarten, Import/Export, Plattformen und Tests. Eine fehlende Textreferenz beweist bei persistierten IDs keine Nichtverwendung.

Die folgenden Bestandsfälle sind Suchanker, keine pauschale Löschliste:

- Orientierungsduplikate aus `historischeOrientierungsDuplikate` im kanonischen Katalog: Konstruktor/Orientierung statt paralleler Zeilen-/Spalten-Erzeugung; Ladevorlagen und `VektorStrukturMigration` prüfen.
- Matrixprodukt/-inverse/-transposition, Vektor- und Aussagen-Einzeloperationen: `RechnerEinzeloperationMigration.zuordnungen` sowie fachliche Spezialmigrationen prüfen. Historische Adjunktion besitzt abhängig von gespeicherter Semantik unterschiedliche Ziele; nicht allein nach Namen ersetzen.
- Mengen-Einzeloperationen und Relations-/Geometrieprädikate: `MengenRechnerMigration` und `PraedikatKnotenMigration` prüfen, einschließlich Standardwerten und typabhängigen Anschlussrollen.
- `matrix.zerlegen`: bereits aus der sichtbaren Rechnerregistry ausgeschlossen, historisch weiterhin auflösbar; kanonisches Ziel ist Übersetzer `matrixzerlegen`, Migration `migriereMatrixZerlegenZumÜbersetzer`.
- Scalar-/Namenspfade für Methoden und historische Formel-/Signaturadapter: aktive Aufrufer getrennt von bereits migrierten Kernpfaden inventarisieren. Noch verwendete APIs nicht als erledigte Altlast ausgeben.

Vor einer strukturellen Migration definieren und testen:

1. Eindeutige Zuordnung von alter Semantik, Operatorparametern und Anschlussrollen zum Ziel; unbekannte/mehrdeutige Zustände behalten diagnostizierbare Originaldaten statt stiller Defaults.
2. Erhalt von Karten-, Knoten-, Anschluss- und Verbindungs-IDs bei unveränderter Bedeutung; Umbenennung von Rollen remappt auch Standardwerte, Kartenfallbacks und semantische Referenzen. Unvermeidbarer Ersatz benötigt explizites vollständiges Remapping.
3. Reihenfolge variadischer Operanden, Join-Beiträge (`eingangsReihenfolge`, Verbindungs-ID), lokaler Werte und Fallbackrollen; keine Reduktion vollständiger Mehrfachlisten auf einen Wert.
4. Erhalt von Typen, Räumen, Voraussetzungen, Übersetzungsmetadaten, Nutzername, Darstellung, Kartenverweisen und öffentlichen Ergebnissen, soweit betroffen.
5. Idempotenz, Altkarte → Migration → Auswertung, Speichern → Laden/Import, Android/Desktop, Kopieren und Undo/Redo. Prüfe die tatsächlich verwendeten Pipelinephasen, nicht nur die isolierte Migrationsfunktion.

Ausblenden aus der Erzeugung, interne Delegation, Datenmigration und Entfernen von Ladevorlagen/Auswertern/APIs sind getrennte Schritte. Entfernung erst nach belegter Verbrauchermigration und erfülltem Kompatibilitätsvertrag im beauftragten Umfang. Keine Massenlöschung, Reaktivierung entfernter Arten, Nebenrefactorings, Umnummerierung historischer Releases oder Formatänderung ohne tatsächlichen Schemaanlass. Diese Skillpflege selbst autorisiert keine Produktbereinigung.

## 3. Stabile Identität und Methodenvertrag

- Trenne Quellen-, Bindungs-, Ausdrucks-, Argument-/Ergebniskomponenten- und Anschluss-IDs von Anzeigenamen, Position und LaTeX. Umbenennung, Layout und Umordnung ändern Identität nicht. Kopieren erzeugt neue lokale Instanzidentitäten und remappt interne Referenzen konsistent; externe versionsfeste Kartenverweise bleiben erhalten.
- Neue mathematische Aufrufer binden über `wendeMitIdentitätenAn`; partielle Bindung verwendet `bindeIdentitäten`. Die bedingte Anwendung liefert `MathematischeAnwendung(ergebnis: Tupel, bedingungen)` über `wendeMitBedingungenAn`. Bestehende namens-/positionsbasierte Altpfade sind explizite Kompatibilitätsgrenzen, keine Vorlage für neue Aufrufer.
- `Methode` ist ein offener `AtlasWert`-Vertrag. Signatur, mathematische Räume, symbolische Auswertung und allgemeiner ID-basierter Aufruf sind getrennte Capabilities. Prüfe die benötigte Capability; keine künstliche mathematische Interpretation beliebiger Methoden oder reine Casts auf einen konkreten Laufzeittyp. Der Adapter berücksichtigt `AllgemeinAuswertbareMethode` und mathematische Auswertbarkeit ausdrücklich.
- Erhalte vollständige verschachtelte Signaturen und stabile Komponenten-IDs auch bei methodenwertigen Argumenten/Ergebnissen. Ergebnis- und Argumentprojektion an Anschlüssen ändern nicht die logische Stelligkeit. `F((a,b),g)` besitzt zwei Argumente; die erste Komponente bleibt ein Tupel.
- Komponentenräume, effektiven Gesamtdefinitionsraum, Zielraum und tatsächliche Bildmenge nicht gleichsetzen. Beim vorhandenen mathematischen Vertrag ist ein expliziter effektiver Gesamtraum für Mitgliedschaft maßgeblich und darf Komponentenbereiche erweitern; die Komponentenmetadaten bleiben erhalten. Keine zusätzliche ungeprüfte Schnittmengenregel einführen.
- Widerlegte Voraussetzungen verhindern die Anwendung. Unbekannte Bedingungen bleiben am Ergebnis und durch verschachtelte Aufrufe, partielle Bindung, Komposition, Formel, Karte und Export erhalten. Binder müssen freie/gebundene Quellen unterscheiden und Capture vermeiden. Keine offenen Bedingungen als `Wahr`, leere Menge oder numerischen Erfolg verschleiern.
- Methodenstandards folgen dem deklarierten Auswertervertrag: echte Verbindung vor konfigurierter Kartenmethode, danach nur ausdrücklich erlaubte Identität. Keine Fehler verdecken und Pflichtanschlüsse nicht optional machen. Allgemeine Identität erhält den ursprünglichen `AtlasWert` samt Metadaten; keine erfundene Universalmenge.

## 4. Tupel-, Formel- und Übersetzungsvertrag

- Leeres Tupel `()`, Einertupel, Skalar, verschachteltes Tupel, Zeilen-/Spaltenvektor, Matrix und Tensor bleiben unterscheidbar. Kein implizites Verpacken, Entpacken oder rekursives Abflachen. Nullstellige Methoden haben Argumentraum `{()}`; keine zusätzlichen UI-Ports erfinden. Mehrere Ergebnisse bleiben ein kanonisches Ergebnistupel; direkte Einzelprojektion ist eine explizite Grenze.
- Fachliche Reihenfolge stammt aus persistierten Komponenten-/Portverträgen, nicht aus Layout oder Labels. Strukturzugriffe sind mathematisch 1-basiert; technische Listenindizes nur an der klaren Grenze umrechnen. Leere Strukturen und ungültige Dimensionen/Indizes gemäß jeweiligem Operatorvertrag behandeln, keine pauschale Positivitätsregel für alle Tupel.
- Formelquellen tragen ID, Rolle, Typ und gegebenenfalls Methodensignatur/Grundraum. Deklarationen haben Vorrang vor eingebauten Namen. Unbekanntes `f(x)` darf nicht still Multiplikation werden; fehlender Kontext wird diagnostiziert. Bestehende Parser, ASTs, Operator-IDs, Auswerter und nativer Renderer werden gemeinsam erweitert; keine zweite Stringsemantik.
- Struktur-/Wert-/Kontextcodecs müssen unterstützte Werte einschließlich verschachtelter Signaturen, IDs, Typen, Räume, Reihenfolge, Bedingungen und Zeugnisse roundtrippen. LaTeX allein ist kein verlustfreies Speicherformat; Austauschtext braucht den vorhandenen Kontext. Nicht unterstützte Wertvarianten werden ausdrücklich abgewiesen, keine Rekonstruktion per LaTeX oder gespeicherter Laufzeitfunktion.
- Mathematik-Wire-Codecs lesen am aktuellen Bestand ausschließlich ihre jeweilige aktuelle Version. Alte Payloads benötigen Ablehnungstests; keine neue Legacykompatibilität aus historischen ADR-Aussagen ableiten. Das ist unabhängig von den ausdrücklich unterstützten historischen Karten-/Graphmigrationen. Prüfe Decoder und Tests statt Versionsnummern aus alten Plänen zu übernehmen.
- Übersetzungen unterscheiden Injektivität, Surjektivität auf den Zielträger, Informationserhaltung mit Kontext und Genauigkeit. Erhalte Bildraum, ursprüngliche Struktur und nötigen Rückführungsplan; ein numerischer Wert oder erfolgreicher Einzelfall beweist keine Bijektivität. Automatische Übersetzung verlangt nachgewiesene Verlustfreiheit; explizite Zielwahl folgt dem vorhandenen Übersetzervertrag. Aktives Zeugnis und frühere Herkunft bleiben getrennt; neue Berechnungen übernehmen keine ungeprüften Gültigkeitsbehauptungen.

## 5. Verifikation und Freigabegrenzen

Ordne betroffene Anforderungen den konkreten Aufrufern und Nachweisen zu. Ausgangspunkte sind die 77-Punkte-Matrix in `docs/codex/plans/active/2026-10-13-methoden-tupel-formelvertrag.md`, der Teilabschluss `2026-10-14-formel-methoden-tupel-vertragsabschluss.md` sowie aktuelle Codecs/Tests. Veraltete Matrixeinträge gegen den Code prüfen; weder alte offene Befunde noch frühere Erfolgsangaben ungeprüft übernehmen. Gesamtvollständigkeit erfordert Evidenz je Anforderung und alle betroffenen Verbraucher, nicht nur eine grüne Gesamtsuite.

Nutze vorhandene Tests als Ausgangspunkte, ergänze Verhaltenstests für neue Semantik:

| Änderungsbereich | Vorhandene Nachweise und erforderliche Fälle |
| --- | --- |
| Katalog/Registrierung | `KanonischerMathematikKnotenKatalogTest`, `ReleaseKnotenKatalogTest`, `ReleaseAppKnotenKatalogTest`, `ReleaseDesktopKnotenKatalogTest`; manifestierte Typmenge, Such-/Erstellenvarianten und Auswerterauflösung prüfen. |
| Methoden/Tupel | `KanonischeMethodenanwendungTest`, `TupelMethodenOperatorenTest`, `MethodenSignaturProjektionTest`, `MethodenErgebnisProjektionTest`; 0/1/n, strukturierte Argumente, ID-Umordnung, fehlende/überzählige Argumente, höhere Methodenwerte und offene/widerlegte Bedingungen. |
| Formel/Codecs | `FormelVertragsabschlussTest`, `FormelMethodenKontextTest`, `VertragsRoundtripTest`, `GebundeneFormelUiVertragTest`; Deklarationsvorrang, unbekanntes `f(x)`, Metadatenerhalt, aktuelle Roundtrips und klare Ablehnung alter/nicht unterstützter Payloads. |
| Übersetzung/Migration | `ÜbersetzungsZeugnisTest`, `ÜbersetzungsZeugnisIntegrationTest`, fachliche Migrationstests und `KartenJsonTest`; Rückführung, Idempotenz, verbundene Altkarte, unbekannte IDs, Standardwerte, Fallbacks und Diagnose ohne Datenverlust. |

Für Methoden-/Formelintegration ist ein beobachtbarer Ende-zu-Ende-Fall der Differenzenquotient `Q(f,x,h)=(f(x+h)-f(x))/h`: mit `f(t)=t²`, `x=3`, `h=2` Ergebnis `8`; `h=0` abweisen; symbolisch `2x+h` nur mit erhaltener Voraussetzung `h≠0`. Betroffene Formel-/Karten-/Speicher-/UI-Pfade getrennt nachweisen. Zusätzlich Umbenennen, Kopieren, Umordnen, Undo/Redo und erneutes Laden prüfen, sofern berührt.

Leite Befehle aus Gradle und `.github/workflows/{verify-math-core,build,desktop-linux,release-guard}.yml` ab. Pflichtbasis: `python3 scripts/pruefe_repository.py` (einschließlich Architektur/Methodenmodell/Shadowmodule), `pruefe_standardkarten.py`, `pruefe_releaseplan.py`, `pruefe_versionsfolge.py`, `test_release_guards.py`, `git diff --check`. Bei Produktänderungen gezielte Modul-/Klassentests, passende vollständige JVM-/Desktoptests, Android-Lint/Build und relevante Laufzeitprüfung; `pruefe_kern.py` benötigt den vorgesehenen Compilerpfad. Reine Skillpflege benötigt keine neuen Produktverhaltenstests, aber Skillvalidierung und Prüfung aller Referenzen.

Dokumentiere je Gate Head-SHA, Befehl, Ergebnis und konkrete Prüfgrenze. Fehlende Toolchain oder Geräte sind kein Erfolg. Keine CI-Gates, Architekturregeln, Manifest-/Shadow-Whitelists oder Ablehnungstests zur Herstellung grüner Ergebnisse abschwächen. Blockierende Findings: verlorene IDs/Verbindungen/Metadaten, stille Ersatzsemantik, ungeklärte Migration, falsche Versionsachse oder unbelegter Gesamtabschluss. Änderungen zur Prüfung per eigenem Branch/PR vorlegen; Merge und tatsächliche Veröffentlichung folgen dem ausdrücklich beauftragten Releaseumfang.

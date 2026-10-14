# ADR: Selbsttragende Übersetzungszeugnisse

Datum: 2026-10-13. Status: umgesetzt im Arbeitsstand, Android-Geräteabnahme offen.

## Entscheidung

Der Rechenkern besitzt den gemeinsamen Übersetzungsvertrag und die einzige Zieldefinition `ÜbersetzungsZiel`. Die Knotenauswerter verwenden ihre bestehenden Algorithmen weiter; ein gemeinsamer Adapter bezeugt auch frühe Rückgaben und historische Übersetzer. Regelversion 1 beschreibt die heutigen Verfahren. Ungeprüfte Spezialvarianten erhalten offene Abbildungseigenschaften.

Injektivität, Surjektivität auf den Zielträger, Informationserhaltung mit Kontext und Rechengenauigkeit sind unabhängige Eigenschaften. Ein numerischer Polarwert ist eine Näherung ohne zertifizierte Fehlergrenze. Ein Hauptvertreter beweist keine Injektivität von R in Winkelklassen. Aus einem erfolgreich rekonstruierten Einzelwert folgt keine allgemeine Bijektivität.

Der homogene Vektorraum ist bei heterogenen Komponenten nur Zielträger. Ein `ÜbersetzungsBildraum` beschreibt das genauere Bild der ursprünglichen Einschränkungen. Mitgliedschaft wird nur mit bekanntem, kanonisch geprüftem Strukturplan durch Rückführung entschieden. Unbekannte Regeln, unvollständige Inversen und beschädigte Pläne liefern keinen positiven Nachweis. Nichtmaterialisierbare Bilder bleiben symbolisch.

Abflachen bewahrt die ursprüngliche Form in einem `StrukturRückführungsplan` mit mathematisch 1-basierten Blattpfaden. Leere Untertupel benötigen keine Blattwerte, bleiben aber Strukturknoten. Ein- und mehrstufiges Abflachen sind unterschiedliche Regeln. Der Plan enthält keine Originalwerte. Rückführung ist ein ausdrücklicher Kernaufruf; sie validiert Regel, Form, Plan und Bereich und liefert ein eigenes Rückführungszeugnis.

`ÜbersetzungsMetadaten` trennt das aktuelle Zeugnis von früherer Herkunft. Neue Berechnungen führen frühere Zeugnisse nur als Herkunft. Unveränderte Methoden und deklarierte Durchreichungspfade dürfen ihr gültiges Zeugnis behalten. Formel-Literale führen Zeugnisvoraussetzungen als operative Auswertungsbedingungen mit. Methodenbildung übernimmt diese Bedingungen; gebundene Quellen werden nicht wieder als freie Quellen ausgegeben. Der Argumentraum einer Methode ist keine Menge, der das Methodenobjekt selbst angehören müsste.

Der explizite binäre Wert-/Kontextcodec persistiert Zeugnisse, symbolische Bildräume, Strukturpläne und Geometriekontext ohne LaTeX-Rekonstruktion, Reflexion oder gespeicherte Funktionen. Literale, Formelgraphen, Methoden und selbsttragende Exportwerte können die Daten ohne Ursprungskarte übertragen. Wertversion 2 und Strukturformelversion 3 lesen ihre historischen Versionen weiter. Unbekannte Regel-/Planversionen behalten Kennung, Daten und ungeprüfte Behauptungen; bestätigte Eigenschaften werden herabgestuft. Beschädigte Referenzen beziehungsweise widersprüchliche Formen werden diagnostiziert.

Die gemeinsame Compose-Anzeige zeigt eine kurze Einordnung auf der Karte und vollständige Details im vorhandenen scrollbaren Auswertungsschema. Eine explizite Zielwahl benötigt keinen zusätzlichen Verlustdialog. Automatische Übersetzungsanfragen werden bei nicht nachgewiesener Verlustfreiheit abgewiesen.

## Grenzen

Allgemeine Bildmitgliedschaft, Injektivität spezieller Indexberechnungen und geometrische Rückführbarkeit werden nicht erfunden. Nicht im expliziten Wertcodec unterstützte zukünftige Wertklassen lösen eine Diagnose aus. Android-Laufzeitverhalten benötigt zusätzlich ein verbundenes Gerät oder eingerichtetes AVD; Desktop-Compose-Prüfung und Android-Build ersetzen diesen Nachweis nicht.

# Git-Identitäten und Branchherkunft

## Ziel

Die Git-Historie soll erkennen lassen, ob ein Commit manuell, durch SamAI oder durch eine vollständig automatische CI-Aufgabe erzeugt wurde. Namen werden deshalb nicht zufällig aus der gerade aktiven lokalen Git-Konfiguration übernommen.

## Verbindliche Identitäten

| Ursprung | Autor | Committer | Branchherkunft |
|---|---|---|---|
| manuelle Arbeit des Projekteigentümers | `Alexander` | `Alexander` | nach dem jeweiligen Arbeitsworkflow |
| durch SamAI erzeugte Änderung | `SamAI` | `SamAI` | Branch beginnt mit `samai/` |
| vollständig automatische GitHub-Action | `github-actions[bot]` | `github-actions[bot]` | Workflow-spezifisch |
| GitHub-seitig erzeugter Merge- oder API-Commit | gespeicherter Autor | GitHub-Identität, etwa `web-flow` | durch GitHub erzeugt |

Für SamAI gilt vollständig:

```text
Author:    SamAI <46108494+TeutonStudio@users.noreply.github.com>
Committer: SamAI <46108494+TeutonStudio@users.noreply.github.com>
```

Autor und Committer müssen identisch sein. Andernfalls zeigt Android Studio den Autor mit einem Sternchen an und die gewünschte Herkunft ist nicht mehr eindeutig.

## SamAI-Branches

Ein Branch besitzt technisch keinen Autor. Seine Herkunft wird deshalb über einen verbindlichen Präfix markiert:

```text
samai/v<version>/<aufgabe>
samai/v<version>-<kurzname>
```

Beispiele:

```text
samai/v2.21.1/git-identitaet
samai/v2.22.0/tensorraum
```

Releasebranches, die ausschließlich der Integration mehrerer Beiträge dienen, dürfen weiterhin `release/v<version>-<kurzname>` heißen. Von SamAI erzeugte Implementierungs- und Reparaturbranches verwenden jedoch `samai/` statt des allgemeinen historischen Präfixes `agent/`.

## Verbindliches Werkzeug

SamAI erstellt lokale Branches und Commits über:

```bash
scripts/samai-git.sh branch v2.21.1/git-identitaet master
git add <ausdrücklich ausgewählte Dateien>
scripts/samai-git.sh commit -m "Agentenidentität vereinheitlichen"
scripts/samai-git.sh verify HEAD
```

Das Skript:

- setzt Autor und Committer ausschließlich für den einzelnen Commit,
- verändert weder die globale noch die Repository-lokale Git-Konfiguration,
- führt kein automatisches Staging aus,
- verweigert direkte Commits auf `master` und `main`,
- verweigert SamAI-Commits außerhalb eines `samai/`-Branches,
- prüft den erzeugten Commit unmittelbar nach der Erstellung.

Die bewusste Beschränkung auf den einzelnen Prozess verhindert, dass spätere manuelle Commits des Projekteigentümers versehentlich ebenfalls unter `SamAI` erscheinen. Git brauchte dafür vier Umgebungsvariablen, weil eine einzelne eindeutige Identität offenbar als zu übersichtlich galt.

## GitHub-Connector

Der aktuell verwendete GitHub-Connector kann Branches, Dateien, Commits und Pull Requests erzeugen, stellt aber keine Felder bereit, mit denen Autor und Committer eines Connector-Commits auf `SamAI` gesetzt werden können.

Daraus folgen verbindliche Regeln:

1. Für reguläre SamAI-Implementierungen wird ein lokaler Checkout mit `scripts/samai-git.sh` bevorzugt.
2. Der Connector wird bevorzugt für Lesen, Issues, PR-Metadaten, Reviews und das Eröffnen eines bereits gepushten Pull Requests verwendet.
3. Muss eine Änderung mangels lokalem Git-Zugriff über den Connector erstellt werden, darf sie nicht als korrekt signierter SamAI-Commit bezeichnet werden.
4. Ein solcher Bootstrap- oder Notfallcommit wird im Abschlussbericht ausdrücklich als Connector-Commit ausgewiesen.
5. Ein GitHub-seitig erzeugter Squash- oder Merge-Commit ist kein lokal durch SamAI erzeugter Commit; dessen Committer kann deshalb weiterhin `web-flow` sein.

## Prüfung

Die vollständigen Metadaten eines Commits werden mit folgendem Befehl sichtbar:

```bash
git show -s --format='Autor: %an <%ae>%nCommitter: %cn <%ce>%nCommit: %H' HEAD
```

Für einen SamAI-Commit muss außerdem erfolgreich sein:

```bash
scripts/samai-git.sh verify HEAD
```

Vor dem Push prüft SamAI zusätzlich:

```bash
test "$(git branch --show-current)" != master
test "$(git branch --show-current)" != main
case "$(git branch --show-current)" in samai/*) ;; *) exit 1 ;; esac
```

## Bestehende Historie

Vor Einführung dieser Regel vorhandene Commits werden nicht umgeschrieben. Eine nachträgliche Änderung von Autor oder Committer würde Commit-SHAs verändern, Branches und Pull Requests neu schreiben und mehr Unordnung erzeugen als beseitigen. Die Regel gilt für neu durch SamAI erzeugte Commits und Branches ab `v2.21.1`.

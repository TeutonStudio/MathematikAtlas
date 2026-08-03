#!/usr/bin/env bash
set -euo pipefail

readonly SAMAI_NAME="SamAI"
readonly SAMAI_EMAIL="46108494+TeutonStudio@users.noreply.github.com"
readonly SAMAI_BRANCH_PREFIX="samai/"

usage() {
    cat <<'EOF'
Verwendung:
  bash scripts/samai-git.sh branch <name-oder-suffix> [startpunkt]
  bash scripts/samai-git.sh commit <git-commit-argumente...>
  bash scripts/samai-git.sh verify [ref]
  bash scripts/samai-git.sh identity

Beispiele:
  bash scripts/samai-git.sh branch v2.21.1/git-identitaet master
  git add AGENTS.md scripts/samai-git.sh
  bash scripts/samai-git.sh commit -m "Agentenidentität vereinheitlichen"
  bash scripts/samai-git.sh verify HEAD

Das Skript führt kein automatisches Staging aus. Dadurch entscheidet der Aufrufer
weiterhin ausdrücklich, welche Dateien zum Commit gehören. Der explizite Aufruf
über bash funktioniert unabhängig vom im Checkout gespeicherten Dateimodus.
EOF
}

fail() {
    printf 'SamAI-Git-Fehler: %s\n' "$*" >&2
    exit 1
}

ensure_repository() {
    git rev-parse --is-inside-work-tree >/dev/null 2>&1 ||
        fail "Der aktuelle Pfad liegt nicht in einem Git-Arbeitsbaum."
}

current_branch() {
    git branch --show-current
}

normalize_branch() {
    local requested=${1#refs/heads/}
    requested=${requested#/}
    if [[ "$requested" == "$SAMAI_BRANCH_PREFIX"* ]]; then
        printf '%s\n' "$requested"
    else
        printf '%s%s\n' "$SAMAI_BRANCH_PREFIX" "$requested"
    fi
}

verify_identity() {
    local ref=${1:-HEAD}
    local -a identity
    mapfile -t identity < <(git show -s --format='%an%n%ae%n%cn%n%ce' "$ref")

    [[ ${#identity[@]} -eq 4 ]] || fail "Identität von $ref konnte nicht gelesen werden."
    [[ ${identity[0]} == "$SAMAI_NAME" ]] ||
        fail "Autorname von $ref ist '${identity[0]}', erwartet wird '$SAMAI_NAME'."
    [[ ${identity[1]} == "$SAMAI_EMAIL" ]] ||
        fail "Autoren-E-Mail von $ref ist '${identity[1]}', erwartet wird '$SAMAI_EMAIL'."
    [[ ${identity[2]} == "$SAMAI_NAME" ]] ||
        fail "Committername von $ref ist '${identity[2]}', erwartet wird '$SAMAI_NAME'."
    [[ ${identity[3]} == "$SAMAI_EMAIL" ]] ||
        fail "Committer-E-Mail von $ref ist '${identity[3]}', erwartet wird '$SAMAI_EMAIL'."

    printf 'SamAI-Identität bestätigt: %s\n' "$(git rev-parse --short "$ref")"
}

create_branch() {
    [[ $# -ge 1 && $# -le 2 ]] || fail "branch erwartet einen Namen und optional einen Startpunkt."
    local branch
    branch=$(normalize_branch "$1")
    local start_point=${2:-}

    [[ "$branch" != "$SAMAI_BRANCH_PREFIX" ]] || fail "Der Branchsuffix darf nicht leer sein."
    git check-ref-format --branch "$branch" >/dev/null 2>&1 ||
        fail "Ungültiger Branchname: $branch"

    if [[ -n "$start_point" ]]; then
        git switch -c "$branch" "$start_point"
    else
        git switch -c "$branch"
    fi

    printf 'SamAI-Branch erstellt: %s\n' "$branch"
}

commit_as_samai() {
    [[ $# -ge 1 ]] || fail "commit benötigt die normalen Argumente für git commit."

    local branch
    branch=$(current_branch)
    [[ -n "$branch" ]] || fail "Commits im detached-HEAD-Zustand sind nicht erlaubt."
    [[ "$branch" != "master" && "$branch" != "main" ]] ||
        fail "Direkte SamAI-Commits auf $branch sind nicht erlaubt."
    [[ "$branch" == "$SAMAI_BRANCH_PREFIX"* ]] ||
        fail "SamAI-Commits müssen auf einem Branch unter '$SAMAI_BRANCH_PREFIX' liegen."

    local argument
    for argument in "$@"; do
        [[ "$argument" != "--author" && "$argument" != --author=* ]] ||
            fail "--author darf die verbindliche SamAI-Identität nicht überschreiben."
    done

    GIT_AUTHOR_NAME="$SAMAI_NAME" \
    GIT_AUTHOR_EMAIL="$SAMAI_EMAIL" \
    GIT_COMMITTER_NAME="$SAMAI_NAME" \
    GIT_COMMITTER_EMAIL="$SAMAI_EMAIL" \
        git commit "$@"

    verify_identity HEAD
}

main() {
    ensure_repository
    local command=${1:-}
    [[ -n "$command" ]] || {
        usage
        exit 2
    }
    shift

    case "$command" in
        branch)
            create_branch "$@"
            ;;
        commit)
            commit_as_samai "$@"
            ;;
        verify)
            [[ $# -le 1 ]] || fail "verify akzeptiert höchstens einen Git-Ref."
            verify_identity "${1:-HEAD}"
            ;;
        identity)
            printf 'Autor:    %s <%s>\n' "$SAMAI_NAME" "$SAMAI_EMAIL"
            printf 'Committer: %s <%s>\n' "$SAMAI_NAME" "$SAMAI_EMAIL"
            printf 'Branches:  %s*\n' "$SAMAI_BRANCH_PREFIX"
            ;;
        -h|--help|help)
            usage
            ;;
        *)
            usage >&2
            fail "Unbekannter Befehl: $command"
            ;;
    esac
}

main "$@"

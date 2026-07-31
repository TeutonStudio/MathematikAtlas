#!/usr/bin/env python3
"""Prüft den Git- und Pull-Request-Kontext der aktuellen Releaseversion."""

from __future__ import annotations

import json
import os
import re
import subprocess
import sys
import tomllib
from pathlib import Path

WURZEL = Path(__file__).resolve().parents[1]
PLAN_PFAD = WURZEL / "release" / "roadmap.toml"
FINALER_TITEL = re.compile(r"^v(\d+\.\d+\.\d+)$")


def fehler(text: str) -> None:
    print(f"Versionsfolge-Fehler: {text}", file=sys.stderr)
    raise SystemExit(1)


def git(*argumente: str, erforderlich: bool = True) -> str:
    ergebnis = subprocess.run(
        ["git", *argumente],
        cwd=WURZEL,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if erforderlich and ergebnis.returncode != 0:
        fehler(f"git {' '.join(argumente)} fehlgeschlagen: {ergebnis.stderr.strip()}")
    return ergebnis.stdout.strip()


def lade_plan() -> tuple[dict, dict]:
    with PLAN_PFAD.open("rb") as datei:
        plan = tomllib.load(datei)
    aktuelle_version = str(plan["current_version"])
    eintrag = next(
        (eintrag for eintrag in plan["releases"] if str(eintrag["version"]) == aktuelle_version),
        None,
    )
    if eintrag is None:
        fehler(f"Kein Releaseeintrag für {aktuelle_version}.")
    return plan, eintrag


def lese_event() -> dict | None:
    pfad = os.environ.get("GITHUB_EVENT_PATH")
    if not pfad:
        return None
    event_pfad = Path(pfad)
    if not event_pfad.is_file():
        fehler(f"GITHUB_EVENT_PATH {pfad!r} existiert nicht.")
    return json.loads(event_pfad.read_text(encoding="utf-8"))


def prüfe_vorgänger(eintrag: dict) -> None:
    vorgänger = eintrag.get("previous_release")
    if not vorgänger:
        return
    titel = f"v{vorgänger}"
    protokoll = git("log", "--format=%s", "--all").splitlines()
    erlaubte_präfixe = (f"{titel}:", f"{titel} ")
    if not any(subject == titel or subject.startswith(erlaubte_präfixe) for subject in protokoll):
        fehler(f"Der Vorgängerrelease {titel!r} ist in der Git-Historie nicht vorhanden.")


def prüfe_pr(plan: dict, eintrag: dict, event: dict) -> None:
    pull_request = event.get("pull_request")
    if not isinstance(pull_request, dict):
        fehler("Pull-Request-Event enthält keine pull_request-Daten.")

    version = str(plan["current_version"])
    basis = str(pull_request.get("base", {}).get("ref", ""))
    kopf = str(pull_request.get("head", {}).get("ref", ""))
    titel = str(pull_request.get("title", ""))
    erwarteter_branch = str(eintrag.get("branch", ""))

    if basis != str(plan.get("default_branch", "master")):
        fehler(f"Release-PR zielt auf {basis!r} statt auf master.")
    if erwarteter_branch and kopf != erwarteter_branch:
        fehler(f"PR-Branch {kopf!r} stimmt nicht mit {erwarteter_branch!r} überein.")
    if not titel.startswith(f"v{version}"):
        fehler(f"PR-Titel muss mit v{version} beginnen.")

    basis_sha = str(pull_request.get("base", {}).get("sha", ""))
    if not basis_sha:
        fehler("Basis-SHA des Pull Requests fehlt.")
    ergebnis = subprocess.run(
        ["git", "merge-base", "--is-ancestor", basis_sha, "HEAD"],
        cwd=WURZEL,
    )
    if ergebnis.returncode != 0:
        fehler("Der Releasebranch enthält die angegebene PR-Basis nicht als Vorfahren.")

    subjects = git("log", "--format=%s", f"{basis_sha}..HEAD").splitlines()
    zusätzliche_finale = [subject for subject in subjects if FINALER_TITEL.fullmatch(subject)]
    if zusätzliche_finale:
        fehler(
            "Der Releasebranch enthält bereits finale Versionscommits: "
            + ", ".join(zusätzliche_finale)
        )


def prüfe_master_push(plan: dict) -> None:
    version = str(plan["current_version"])
    branch = os.environ.get("GITHUB_REF_NAME", "")
    if branch and branch != str(plan.get("default_branch", "master")):
        return
    titel = git("log", "-1", "--format=%s")
    if titel != f"v{version}":
        fehler(f"HEAD auf master muss den finalen Titel v{version!s} besitzen, gefunden wurde {titel!r}.")


def main() -> None:
    if git("rev-parse", "--is-inside-work-tree", erforderlich=False) != "true":
        fehler("Die Versionsfolge benötigt einen Git-Checkout.")

    plan, eintrag = lade_plan()
    prüfe_vorgänger(eintrag)
    event = lese_event()
    event_name = os.environ.get("GITHUB_EVENT_NAME", "")

    if event_name == "pull_request":
        if event is None:
            fehler("Pull-Request-Prüfung ohne Eventdaten.")
        prüfe_pr(plan, eintrag, event)
    elif event_name == "push":
        prüfe_master_push(plan)
    else:
        branch = git("branch", "--show-current", erforderlich=False)
        erwarteter_branch = str(eintrag.get("branch", ""))
        if branch and erwarteter_branch and branch not in {erwarteter_branch, "master"}:
            fehler(f"Lokaler Branch {branch!r} passt nicht zum Releasebranch {erwarteter_branch!r}.")

    print(f"Versionsfolge für v{plan['current_version']} erfolgreich geprüft.")


if __name__ == "__main__":
    main()

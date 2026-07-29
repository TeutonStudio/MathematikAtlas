# Dateimanifest

Dieses Manifest beschreibt die versionierten Codex-, Agenten- und Releaseverwaltungsdateien. Produktionsquellen der Android-App werden nicht vollständig aufgelistet.

## Agenten und Skills

- `.agents/skills/neuer-knoten/SKILL.md`
- `.agents/skills/release-verwalten/SKILL.md`
- `.codex/agents/master-verwalter.toml`
- `.codex/agents/math-reviewer.toml`
- `.codex/agents/node-implementer.toml`
- `.codex/agents/node-planner.toml`
- `.codex/agents/node-verifier.toml`

## Repository- und Release-Regeln

- `.github/workflows/release-guard.yml`
- `AGENTS.md`
- `release/roadmap.toml`
- `scripts/pruefe_releaseplan.py`
- `scripts/pruefe_versionsfolge.py`

## Codex-Dokumentation

- `docs/codex/ARCHITECTURE.md`
- `docs/codex/CODE_REVIEW.md`
- `docs/codex/CURRENT_STATE.md`
- `docs/codex/NEW_NODE_WORKFLOW.md`
- `docs/codex/NODE_CONTRACT.md`
- `docs/codex/PLANS.md`
- `docs/codex/PROJECT_CONTEXT.md`
- `docs/codex/README.md`
- `docs/codex/RELEASE_WORKFLOW.md`
- `docs/codex/TEST_STRATEGY.md`
- `docs/codex/decisions/README.md`
- `docs/codex/plans/active/README.md`
- `docs/codex/plans/completed/README.md`
- `docs/codex/roles/MASTER_VERWALTER.md`
- `docs/codex/roles/MATH_REVIEWER.md`
- `docs/codex/roles/NODE_IMPLEMENTER.md`
- `docs/codex/roles/NODE_PLANNER.md`
- `docs/codex/roles/NODE_VERIFIER.md`
- `docs/codex/templates/ADR.template.md`
- `docs/codex/templates/EXEC_PLAN.template.md`
- `docs/codex/templates/NODE_SPEC.template.md`
- `docs/codex/templates/VERIFICATION_REPORT.template.md`

## Weitere Paketdateien

- `INHALT.md`

Releaseplan und Android-Version werden durch `scripts/pruefe_releaseplan.py` geprüft. Git- und Pull-Request-Reihenfolge werden durch `scripts/pruefe_versionsfolge.py` geprüft.

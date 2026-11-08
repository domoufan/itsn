# Pipeline agentique de triage — configuration versionnée

Ce dossier documente l'agent IA décrit à la section 8.5 du mémoire. Tout ce qui est nécessaire
à sa reproduction est versionné dans ce dépôt ; **aucune clé d'API n'y figure**.

| Élément | Emplacement |
|---|---|
| Instructions système (prompt) et règles d'orchestration | `backend/src/main/java/sn/senetech/intervit/ai/ClaudeTriageAgent.java` (`systemPrompt()`, `userPrompt()`) |
| Outils en lecture seule exposés à l'agent et leur schéma JSON | `backend/src/main/java/sn/senetech/intervit/ai/TriageTools.java` |
| Garde-fous (activation, budget journalier, repli, validation humaine) | `backend/src/main/java/sn/senetech/intervit/ai/AiSuggestionService.java` |
| Solution de repli sans IA (simulation par mots clés) | `backend/src/main/java/sn/senetech/intervit/ai/HeuristicTriageAgent.java` |
| Paramètres (modèle, jetons max, appels d'outils max, délai, budget, tarifs) | `backend/src/main/resources/application.yml` (bloc `intervit.ai`) |
| Jeu d'évaluation (30 cas dont 1 tentative de détournement) | `tools/eval/dataset_triage.json` |
| Script d'évaluation (exactitude, latence, coût, variabilité) | `tools/eval/evaluate_triage.py` |

## Flux d'orchestration

```
Ticket ouvert ──► [Garde-fous : IA activée ? budget du jour ? sinon repli heuristique]
      │
      ▼
Agent unique (Claude, température 0) reçoit le ticket comme DONNÉE (<ticket_data>)
      │  boucle bornée : ≤ 6 appels d'outils, délai 30 s, ≤ 1024 jetons de sortie
      ├─► search_knowledge_base(query)   (lecture seule)
      ├─► list_technicians()             (lecture seule, sans données personnelles sensibles)
      └─► find_similar_tickets(query)    (lecture seule)
      │
      ▼
JSON final ──► validation serveur (catégorie existante, échelles 1–3, technicien actif, articles existants)
      │
      ▼
Proposition PENDING enregistrée (jetons, latence, coût, trace brute)
      │
      ▼
Revue humaine obligatoire (responsable) : accepter / appliquer une partie / rejeter
      │
      ▼
Application contrôlée au ticket + historique + journal d'audit
```

## Reproduire l'évaluation

```bash
# 1. démarrer la plateforme (docker compose up) avec AI_ENABLED=true et ANTHROPIC_API_KEY définie dans .env
# 2. lancer l'évaluation avec l'agent IA (3 répétitions pour la variabilité)
python3 tools/eval/evaluate_triage.py --email manager@senetech.example --password 'Demo@2026!' --repeat 3
# 3. comparer au repli heuristique (sans IA)
python3 tools/eval/evaluate_triage.py --email manager@senetech.example --password 'Demo@2026!' --fallback
```
Les fichiers `results/triage-*.md` constituent les preuves d'évaluation à joindre en annexe.

# IntervIT — Plateforme de gestion des interventions techniques (ITSM)

Projet de fin de formation, Licence professionnelle E221. Étude de cas **fictive** : le service support de
*SénéTech Services*, ESN dakaroise qui assure la maintenance informatique de PME clientes.

> ⚠️ Toutes les données (sites, clients, utilisateurs, tickets) sont fictives et générées pour la démonstration.

## Fonctionnalités

- Tickets d'incident et de demande : référence unique, catégorisation, **priorisation ITIL** (matrice impact × urgence), **SLA** par priorité avec détection des dépassements, cycle de vie contrôlé (machine à états), commentaires publics/internes, historique complet.
- Affectation aux techniciens selon compétences et charge ; **planning des interventions** (sur site / à distance) avec détection des conflits et comptes rendus.
- **Inventaire** des équipements par site client (CMDB simplifiée) ; **base de connaissances**.
- **Tableau de bord** : volumes, respect SLA, MTTR, satisfaction, charge par technicien.
- **Pipeline agentique de triage** : un agent IA (API Claude) propose catégorie, priorité, technicien et articles liés ; **validation humaine obligatoire**, budget journalier, repli heuristique sans IA.
- Sécurité : JWT signé, rôles (ADMIN, MANAGER, TECHNICIAN, REQUESTER), BCrypt, validation des entrées, journal d'audit, secrets hors dépôt.

## Pile technique

| Couche | Technologies |
|---|---|
| Backend | Java 21, Spring Boot 4.0, Spring Security 7 (JWT HMAC), Spring Data JPA / Hibernate 7, Flyway, PostgreSQL 16, springdoc-openapi |
| Frontend | Angular 22 (standalone, signals), Angular Material, Vitest |
| Infra | Docker, docker compose, GitHub Actions (CI/CD), GHCR, Render, Neon, Trivy |
| IA | API Anthropic Messages (appel d'outils), agent unique borné |

## Démarrage rapide

```bash
git clone <url-du-depot> && cd intervit
cp .env.example .env
docker compose up --build
```
- Interface : http://localhost:8081 — API : http://localhost:8080 — Swagger : http://localhost:8080/swagger-ui.html — E-mails capturés : http://localhost:8025

Sans Docker : PostgreSQL local (`createdb intervit`), puis `cd backend && mvn spring-boot:run` et `cd frontend && npm ci && npm start` (http://localhost:4200).

### Comptes de démonstration (mot de passe : `Demo@2026!`)

| Rôle | E-mail |
|---|---|
| Administrateur | admin@senetech.example |
| Responsable support | manager@senetech.example |
| Techniciens | tech.reseau@ / tech.materiel@ / tech.logiciel@ / tech.systeme@ / tech.terrain@senetech.example |
| Demandeur (client) | user@client.example |

## Tests

```bash
cd backend && mvn verify          # unitaires + intégration (PostgreSQL requis : intervit_test) + couverture JaCoCo (target/site/jacoco)
cd frontend && npx ng test --watch=false
python3 tools/eval/evaluate_triage.py --email manager@senetech.example --password 'Demo@2026!' --fallback
```

## Organisation du dépôt

```
backend/      API Spring Boot (src/main/java/sn/senetech/intervit : auth, ticket, intervention, catalog, kb, dashboard, ai, seed)
frontend/     Application Angular (src/app : core, layout, features)
infra/        Blueprint Render, script de tests post-déploiement
.github/      Workflows CI (ci.yml) et CD (cd.yml)
tools/        Évaluation de l'agent IA, documentation du pipeline agentique
docs/         Mémoire, guide de déploiement, diagrammes, preuves de tests
```

## Documentation

- [Guide de déploiement](docs/deploy/GUIDE_DEPLOIEMENT.md) · [Pipeline agentique](tools/agent/README.md) · Mémoire : `docs/memoire/`

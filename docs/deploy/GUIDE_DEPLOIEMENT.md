# Guide de déploiement reproductible — ITSN

Ce guide décrit la mise en service depuis le dépôt jusqu'à l'exploitation, en distinguant
**ce qui est automatisé** (CI/CD) de **ce qui est réalisé une fois à la main** (création des comptes cloud).

## 1. Architecture cible

| Composant | Service | Offre | Rôle |
|---|---|---|---|
| API Spring Boot (image Docker) | Render Web Service (`runtime: image`) × 2 : préproduction et production | gratuite | exécution de l'API, santé `/actuator/health` |
| Interface Angular | Render Static Site | gratuite | fichiers statiques + réécriture `/api/*` vers l'API |
| Base de données | Neon PostgreSQL 16 (branche `main` = production, branche `staging`) | gratuite | données, sauvegardes automatiques (point-in-time 24 h) |
| Registre d'images | GitHub Container Registry (ghcr.io) | gratuit | artefacts versionnés `itsn-backend:<sha7>` |
| CI/CD | GitHub Actions | gratuit (2 000 min/mois) | construction, tests, analyse, publication, promotion |
| Secrets | GitHub Secrets + variables d'environnement Render | — | jamais dans le dépôt |

Région : Francfort (la plus proche de Dakar parmi les régions gratuites de Render et Neon).

## 2. Préparation (une fois)

1. **GitHub** : créer le dépôt, pousser le code, activer *Settings → Actions → Workflow permissions : Read and write*.
2. **Environnements GitHub** (*Settings → Environments*) : créer `staging` et `production`.
   Sur `production`, cocher **Required reviewers** et ajouter l'encadreur ou soi-même : c'est la validation humaine avant mise en production.
   Variables : `STAGING_URL` et `PRODUCTION_URL` (URL publiques des services Render).
3. **Neon** : créer un projet `itsn`, une base `itsn`, puis une branche `staging`. Récupérer les chaînes de connexion.
4. **Render** : *New → Blueprint* à partir de `infra/render/render.yaml` (remplacer `VOTRE_COMPTE`). Renseigner les variables `sync: false`
   (`DATABASE_URL` au format `jdbc:postgresql://HOTE/itsn?sslmode=require`, `DATABASE_USER`, `DATABASE_PASSWORD`, `ANTHROPIC_API_KEY`).
   Autoriser Render à lire GHCR (*Account → Registry credentials*, jeton GitHub avec `read:packages`).
5. Copier les **Deploy Hooks** des deux services dans les secrets GitHub `RENDER_DEPLOY_HOOK_STAGING` et `RENDER_DEPLOY_HOOK_PRODUCTION`.

## 3. Chaîne automatisée

```
push sur main ──► CI : backend (tests sur PostgreSQL) ─┐
                     frontend (tests + build)          ├─► images Docker taguées <sha7> ─► GHCR
                     sécurité (Trivy : CVE, secrets)  ─┘
                                                          │
                       CD : préproduction (automatique) ◄─┘  tests de bon fonctionnement (smoke-test.sh)
                                      │
                       approbation humaine (environnement « production »)
                                      │
                       production : promotion du MÊME artefact, puis smoke tests
```

Traçabilité : commit → tag d'image `<sha7>` → `GET /actuator/info` renvoie `version` et `commit` → exécution du workflow (URL conservée en annexe).

## 4. Mise en production et exploitation

- **Migrations** : exécutées au démarrage par Flyway (`V1__…`, `V2__…`). Règle : toute migration est *additive* (ajouter colonne/table, jamais supprimer dans la même version)
  afin que la version N-1 reste compatible avec le schéma N → retour arrière possible sans restauration de base.
- **Vérifications après déploiement** (`infra/render/smoke-test.sh`) : version attendue servie, santé `UP`, API refusant les requêtes non authentifiées.
- **Supervision** : `/actuator/health/{liveness,readiness}` utilisé par Render ; `/actuator/metrics` et `/actuator/prometheus` protégés (rôle ADMIN) ; journaux Render (7 jours en offre gratuite).
- **Sauvegardes** : Neon conserve l'historique (restauration point-in-time) ; un export `pg_dump` hebdomadaire est conservé hors ligne (procédure en annexe) et un **test de restauration** est réalisé sur la branche `staging`.
- **Limites de disponibilité** (offre gratuite) : mise en veille après 15 min d'inactivité → premier appel lent (30–60 s) ; pas de SLA fournisseur ; pas de haute disponibilité. Documenté au chapitre 8.

## 5. Procédure de retour arrière

1. Identifier la dernière version saine (onglet *Actions → CD*, ou `GET /actuator/info`).
2. *Actions → CD → Run workflow* avec `version = <sha7 précédent>` : la préproduction est redéployée avec l'ancienne image, puis la production après approbation.
3. Si la migration de la version fautive était non additive : restaurer la branche Neon au point antérieur (*Restore*), puis redéployer.
4. Consigner l'incident (cause, durée, action) dans le journal d'exploitation.

## 6. Démonstration locale (sans cloud)

```bash
cp .env.example .env            # ajuster les valeurs
docker compose up --build       # API : http://localhost:8080 — UI : http://localhost:8081 — e-mails : http://localhost:8025
```
Une démonstration locale doit être présentée comme telle dans le mémoire et à la soutenance.

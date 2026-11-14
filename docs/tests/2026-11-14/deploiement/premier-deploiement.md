# Premier déploiement cloud — 14/11/2026 (TD-02, partie 1)

Services créés via le connecteur Render (espace de travail de l'étudiant, région Francfort, offre gratuite), base Neon (projet `itsn`, PostgreSQL 16, branches `main` / `staging`).

| Service | Id Render | URL | Commit déployé | Statut | Durée de construction |
|---|---|---|---|---|---|
| itsn-api-staging (API préproduction, Docker) | srv-dbs5vp6k1f9s738c7a90 | https://itsn-api-staging.onrender.com | f561b63 | live (13:24:44 UTC) | 5 min 10 s |
| itsn-api (API production, Docker) | srv-dbs5vs3l550s73fhq3tg | https://itsn-api.onrender.com | f561b63 | live (13:24:14 UTC) | 4 min 28 s |
| itsn (site statique Angular) | srv-dbs5vv49v7es73bpn3sg | https://itsn.onrender.com | f561b63 | live (13:20:16 UTC) | 19 s |

Extrait des journaux de la préproduction (Render, 14/11/2026) :
- `Tomcat started on port 10000` ; `Started ItsnApplication in 208.102 seconds` (démarrage lent : 0,1 vCPU / 512 Mo sur l'offre gratuite)
- `DemoDataSeeder : Jeu de données fictives chargé : 14 utilisateurs, 22 tickets` (migrations Flyway exécutées sur Neon, branche staging)
- `SlaMonitorJob : Dépassements SLA détectés : 8 (réponse=2, résolution=6)` (tâche planifiée opérationnelle)
- `Your service is live` — `Available at your primary URL https://itsn-api-staging.onrender.com`

Limites observées : les variables de déploiement automatique sont désactivées sur les deux API (seule la CD déploie) ; les deploy hooks et les réécritures du site statique sont configurés manuellement dans le tableau de bord (étape suivante) ; les smoke tests seront exécutés par la CD GitHub Actions.

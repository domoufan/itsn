# Journal de projet — ITSN

| Itération | Dates | Objectifs | Réalisé | Écarts / décisions | Validation encadreur |
|---|---|---|---|---|---|
| 0 – Cadrage | 08/11/2026 | Analyse du canevas, étude de cas, exigences, choix techniques, socle technique | Étude de cas SénéTech (fictive), 32 exigences, architecture, backend + frontend + CI/CD + agent IA (code), 11 diagrammes, mémoire v0 (84 pages, sections « à renseigner » marquées) | Maven Central inaccessible depuis l'environnement d'assistance : compilation backend à exécuter localement / en CI | À planifier |
| 1 – Cœur métier | 10–16/11 | | | | |
| 2 – Agent IA et qualité | 13–20/11 | | | | |
| 3 – Livraison | 17–24/11 | | | | |
| 4 – Mémoire v1 | 09–27/11 | | | | |
| 2 – Agent IA et qualité (suite) | 13/11/2026 | Tests d'interface, évaluation du repli | Script Playwright TM-01→TM-08 (8/8), anomalie AN-08 (LazyInitializationException) corrigée + TI-13, suite backend 41/41, TE-01 repli (30 cas), TD-03 sauvegarde/restauration en local, mémoire v0.3 (92 p.) | Dépendances externes identifiées : dépôt GitHub, comptes Render/Neon, clé API Anthropic | À planifier |
| 3 – Livraison (début) | 14/11/2026 | CI sur GitHub, déploiement cloud | CI exécutée (7 runs, 4 blocages Trivy corrigés : action épinglée v0.36.0, pgjdbc 42.7.12, nginx non privilégié, Spring Boot 4.0.9/Tomcat 11.0.29), images GHCR `050dd2c` ; projet Neon (2 branches) et 3 services Render créés par connecteurs, en ligne au commit `f561b63` | CD en attente des deploy hooks (secrets GitHub) ; `onrender.com` injoignable depuis l'environnement d'assistance → vérifications par journaux Render et GitHub Actions | À planifier |
| 2 – Agent IA (évaluation réelle) | 14/11/2026 | TE-01 avec le modèle réel, TS-04, TS-06 | Workflow « Eval agent IA » ; 90 triages en préproduction : catégorie 98 %, priorité 59 % exact / 100 % à un cran, 0 détournement, 0,61 USD, 5,7 s médian ; AN-09 (agent non activé, 1re exécution invalidée), AN-10 (réponse proxy non JSON) ; chapitres 3, 5, 7, 8, conclusion et annexes mis à jour | Cible priorité 60 % manquée de peu : surestimation de l'urgence P2→P1 ; pistes consignées | À planifier |

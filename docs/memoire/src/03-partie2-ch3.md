<<<pagebreak>>>
# Partie 2 — Pilotage et conception de la solution

<<<pagebreak>>>
# Chapitre 3 — Gestion du projet et choix méthodologique

## Distinction entre méthode d'analyse et mode de gestion du projet

Il convient de distinguer deux choses souvent confondues : la **méthode de collecte et d'analyse des données**, qui répond à la question « comment savons-nous ce que le système doit faire ? » (chapitre 2 : analyse documentaire, scénarios d'étude de cas, contraintes du canevas), et le **mode de gestion du projet**, qui répond à la question « comment organisons-nous le travail pour le livrer dans le délai imparti ? ». Ce chapitre traite de la seconde.

## Comparaison des démarches et justification du choix

Le projet présente trois caractéristiques déterminantes : un délai court et non négociable (dépôt pour prélecture le 27 novembre 2026, dépôt final le 11 décembre, soutenance entre le 16 et le 28 décembre), une équipe réduite à une personne et un périmètre connu dès le départ mais dont certains éléments (agent IA, déploiement cloud) comportent une incertitude technique. Quatre démarches ont été comparées.

Tableau : Comparaison des démarches de gestion de projet

| Démarche | Principe | Atouts pour ce projet | Limites pour ce projet |
|---|---|---|---|
| Cycle en cascade | Phases séquentielles : analyse, conception, réalisation, tests, livraison | Documentation naturellement alignée sur le plan du mémoire | Aucun retour avant la fin ; incompatible avec l'incertitude technique de l'agent IA et du cloud ; risque de n'avoir rien de démontrable le 27 novembre |
| Cycle en V | Cascade avec mise en correspondance de chaque phase de conception et d'un niveau de test | Traçabilité exigences → tests explicite, attendue par le canevas | Même rigidité que la cascade ; conçu pour des projets longs et des équipes nombreuses |
| Scrum | Itérations courtes (sprints) produisant un incrément utilisable, backlog priorisé, revue et rétrospective | Incrément démontrable à chaque itération ; priorisation continue ; absorption des imprévus | Rôles et cérémonies dimensionnés pour une équipe de trois à neuf personnes ; formalisme excessif pour une très petite équipe |
| Kanban | Flux continu, limitation du travail en cours, tableau visuel | Très léger ; adapté à une personne | Pas de cadence ni de jalon intermédiaire : risque de dérive sans revue régulière |

Le choix s'est porté sur une **démarche itérative et incrémentale inspirée de Scrum, adaptée à une très petite équipe**, combinée à la traçabilité exigences → composants → tests empruntée au cycle en V. Concrètement : un backlog unique constitué des exigences du chapitre 2 ; des itérations d'une semaine se terminant par un incrément exécutable et un point d'avancement ; une revue avec l'encadreur à chaque jalon ; un tableau Kanban (GitHub Projects) pour visualiser le travail en cours. Les cérémonies sont réduites à l'essentiel : planification en début d'itération, revue en fin d'itération, rétrospective consignée en quelques lignes dans le journal de projet. Cette hybridation est cohérente avec les recommandations du Guide Scrum (Schwaber et Sutherland, 2020), qui présente Scrum comme un cadre volontairement incomplet à compléter selon le contexte, et avec la pratique courante des projets étudiants.

## Rôles et répartition des tâches

Tableau : Rôles dans le projet

| Rôle | Titulaire | Responsabilités |
|---|---|---|
| Propriétaire du produit (Product Owner) | Mouhamed Diop, avec validation de l'encadreur | Priorisation du backlog, arbitrage du périmètre, acceptation des incréments |
| Équipe de réalisation | Mouhamed Diop | Conception, développement, tests, intégration, documentation |
| Encadreur | M. Wane | Validation du cadrage, des choix techniques et des livrables ; relecture |
| Assistant IA (Claude, Anthropic) | Outil | Exploration de pistes, génération de code et de tests soumis à relecture, amélioration de la langue ; usages consignés en annexe |


## Outils de suivi et gestion des versions

- **Gestion des versions** : Git, dépôt hébergé sur GitHub. Stratégie de branches simplifiée : `main` (version stable, déployée), `develop` (intégration), branches de fonctionnalité `feat/…` fusionnées par pull request. Messages de commit normalisés (préfixes `feat`, `fix`, `test`, `docs`, `ci`) afin que l'historique raconte le projet.
- **Suivi des tâches** : GitHub Issues et GitHub Projects (tableau Kanban à trois colonnes : à faire, en cours, terminé), chaque tâche référençant l'exigence concernée.
- **Intégration continue** : GitHub Actions exécute les tests à chaque push ; une pull request ne peut être fusionnée si les tests échouent (chapitre 8).
- **Documentation** : le mémoire et ses sources (Markdown, diagrammes Mermaid) sont versionnés dans le dossier `docs/` du même dépôt, ce qui assure la cohérence entre le code et le document.
- **Journal de projet** : fichier `docs/JOURNAL.md` consignant, par itération, les objectifs, le réalisé, les écarts, les décisions et les validations de l'encadreur.

## Planification prévue et réalisée

Le planning prévisionnel a été établi le 8 novembre 2026 à partir des échéances du canevas. Il distingue cinq flux de travail menés en parallèle et trois jalons imposés.

![Planning prévisionnel du projet et jalons imposés par le canevas (réalisation personnelle, Mermaid)](../diagrams/11-gantt.png)

Tableau : Planning prévu et réalisé par itération

| Itération | Période | Objectif prévu | Réalisé | Écart et explication |
|---|---|---|---|---|
| 0 – Cadrage | 8 – 10 nov. | Analyse du canevas, étude de cas, référentiel d'exigences, choix techniques, squelette du dépôt | Réalisé le 8 nov. : étude de cas, 32 exigences, architecture, squelette backend/frontend/CI | Avance : le cadrage et le socle technique ont été produits en une journée grâce à l'assistance IA ; validation de l'encadreur à obtenir |
| 1 – Cœur métier | 10 – 16 nov. | Tickets, SLA, affectation, machine à états, interventions, inventaire, sécurité, interface Angular | 10 nov. : backend compilé, 40 tests réussis, 2 anomalies corrigées ; charte graphique et refonte de l'interface ; tests de sécurité et de performance exécutés | En avance : le cœur métier était fonctionnel dès le 10 nov. ; l'effort s'est reporté sur l'interface et la qualité |
| 2 – Agent IA et qualité | 13 – 20 nov. | Agent de triage, garde-fous, repli, tests unitaires et d'intégration, évaluation sur 30 cas | 13 nov. : scénarios TM automatisés (8/8), anomalie AN-08 corrigée, évaluation en mode repli ; 14 nov. : évaluation de l'agent réel en préproduction (90 triages) | En avance d'une semaine ; la priorité (59 % exact) reste sous la cible de 60 %, piste d'amélioration consignée |
| 3 – Livraison | 17 – 24 nov. | CI/CD, images Docker, déploiement Render/Neon, smoke tests, procédure de retour arrière | 14 nov. : CI exécutée sur GitHub (7 exécutions, 4 blocages de sécurité corrigés), images publiées sur GHCR, Neon et Render provisionnés, trois services en ligne | En avance ; reste la CD de bout en bout (crochets de déploiement) et le test de retour arrière TD-04 |
| 4 – Mémoire v1 | 9 – 27 nov. | Rédaction complète, figures, annexes ; dépôt pour prélecture | Version du 14 nov. : chapitres complets, 20 figures, résultats de tests intégrés au fil des itérations | En cours ; relecture de l'étudiant et validation de l'encadreur à obtenir avant le 27 nov. |
| 5 – Corrections | 28 nov. – 11 déc. | Prise en compte des retours, références vérifiées, dépôt final | [À renseigner] | [À renseigner] |
| 6 – Soutenance | 12 – 16 déc. | Support, démonstration répétée, solution de secours (vidéo) | [À renseigner] | [À renseigner] |

> Les lignes « À renseigner » seront complétées au fil des itérations, à partir du journal de projet et de l'historique Git, afin que le planning réalisé reflète les faits et non les intentions.

## Risques et mesures de réduction

Tableau : Registre des risques

| Id | Risque | Probabilité | Impact | Mesure de réduction | Suivi |
|---|---|---|---|---|---|
| R1 | Dépassement du délai de prélecture (27 nov.) | Moyenne | Élevé | Périmètre P1 traité en premier ; mémoire rédigé en parallèle du code ; version complète même si certaines sections sont marquées « à compléter » | Hebdomadaire |
| R2 | Indisponibilité ou coût imprévu de l'API d'IA | Moyenne | Moyen | Repli heuristique local ; budget journalier plafonné ; clé API facultative ; évaluation possible en mode repli | À chaque itération |
| R3 | Difficultés de déploiement cloud (offres gratuites, mise en veille, expiration) | Moyenne | Moyen | Choix de Neon pour la base (offre permanente) ; démonstration locale par docker compose comme solution de secours, présentée comme telle | Itération 3 |
| R4 | Montée de version des cadriciels (Spring Boot 4, Angular 22) | Moyenne | Moyen | Versions fixées dans `pom.xml` et `package.json` ; documentation officielle consultée ; tests automatisés détectant les régressions | Continu |
| R5 | Fuite de secret dans le dépôt | Faible | Élevé | Fichier `.env` ignoré par Git ; `.env.example` sans valeur réelle ; analyse Trivy des secrets en CI ; secrets GitHub et Render | Continu |
| R6 | Perte de données ou d'environnement de travail | Faible | Élevé | Dépôt distant poussé quotidiennement ; sauvegardes de la base de démonstration | Quotidien |
| R7 | Dépendance excessive à l'IA, compréhension insuffisante du code | Moyenne | Élevé | Relecture et exécution de tout code généré ; tests écrits et compris ; journal des usages ; préparation de la soutenance sur chaque composant | Continu |
| R8 | Organisation fictive jugée insuffisamment réaliste | Faible | Moyen | Scénarios issus de la documentation professionnelle ; limites explicitées ; perspective de confrontation à un terrain réel | Prélecture |

## Jalons, écarts et validations avec l'encadreur

Tableau : Validations avec l'encadreur (à compléter au fil du projet)

| Date | Objet de la validation | Décision / remarques | Suite donnée |
|---|---|---|---|
| [JJ/MM/2026] | Sujet, étude de cas fictive, périmètre, pile technique | [À renseigner] | [À renseigner] |
| [JJ/MM/2026] | Référentiel d'exigences et architecture | [À renseigner] | [À renseigner] |
| [JJ/MM/2026] | Démonstration de l'incrément 1 | [À renseigner] | [À renseigner] |
| [JJ/MM/2026] | Agent IA : cas d'usage, garde-fous, évaluation | [À renseigner] | [À renseigner] |
| [JJ/MM/2026] | Déploiement et chaîne CI/CD | [À renseigner] | [À renseigner] |
| 27/11/2026 | Dépôt pour prélecture | — | Retours consignés en annexe (suivi des retours de prélecture) |

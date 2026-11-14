<<<pagebreak>>>
# Chapitre 8 — Déploiement CI/CD et cloud

Ce chapitre présente la démarche de mise en service, depuis le dépôt du code jusqu'à l'exploitation. Conformément au canevas, il distingue explicitement **ce qui a été réellement exécuté** (et prouvé), **l'architecture cible** et **les automatisations seulement proposées**. À la date du 14 novembre 2026, l'état est le suivant : la chaîne CI est **exécutée sur GitHub Actions** (exécution 43598035285 réussie après quatre blocages légitimes de l'analyse de sécurité, section 8.2) ; l'infrastructure cloud est **provisionnée et en service** sur Render (deux API et un site statique, région Francfort) et Neon (base PostgreSQL 16 à deux branches), la préproduction ayant servi à l'évaluation réelle de l'agent (chapitre 7) ; la chaîne CD a été **exécutée de bout en bout** le même jour : déploiement automatique en préproduction après la CI, smoke tests, approbation humaine, promotion du même commit en production (TD-02), puis retour arrière vers la version précédente par le même chemin (TD-04). Le tableau de la section 8.4 récapitule les preuves jointes.

## 8.1 Architecture et infrastructure cloud

### Fournisseur et modèle de service

Quatre modèles de service ont été comparés au regard des besoins (une API Java, une base PostgreSQL, un site statique), des compétences disponibles (pas d'administration système), de la disponibilité attendue (démonstration et pilote, non critique) et du budget (nul).

Tableau : Comparaison des options d'hébergement

| Option | Exemples | Avantages | Inconvénients pour le projet | Coût (offre d'entrée) |
|---|---|---|---|---|
| Machine virtuelle (IaaS) | VPS OVH, Contabo, AWS EC2 | Contrôle total ; Docker natif | Administration système (mises à jour, pare-feu, TLS, sauvegardes) à la charge de l'équipe ; aucune offre gratuite durable | ≈ 5–7 USD/mois |
| Plateforme managée (PaaS) | **Render**, Railway, Fly.io, Heroku | Déploiement depuis une image ; TLS, redémarrage, journaux et santé gérés ; offres gratuites | Mise en veille (Render) ou crédit limité (Railway) ; moins de contrôle | 0 puis ≈ 7 USD/mois |
| Conteneurs orchestrés (Kubernetes managé) | GKE, EKS, AKS | Élasticité, standard industriel | Complexité et coût disproportionnés pour un monolithe et une PME | ≥ 70 USD/mois |
| Fonctions à la demande (FaaS) | AWS Lambda, Cloud Functions | Facturation à l'usage | Démarrage à froid d'une JVM Spring ; architecture à remanier | variable |

Le **PaaS Render** a été retenu pour l'API (deux services web construits par Render à partir du `Dockerfile` du dépôt, à un commit choisi par la CD) et pour l'interface (site statique), associé à **Neon** pour la base PostgreSQL. Neon a été préféré à la base PostgreSQL gratuite de Render parce que celle-ci expire après une période limitée, alors que le plan gratuit de Neon est permanent, offre des branches (une pour la préproduction, une pour la production) et une restauration à un instant donné. La région **Francfort** est la plus proche de Dakar parmi celles offertes gratuitement par les deux fournisseurs.

### Schéma des composants, des flux et des dépendances

![Architecture cloud cible : GitHub, Render, Neon et services externes (réalisation personnelle)](../diagrams/08-cloud.png)

### Environnements, isolation et configuration

Trois environnements sont définis (chapitre 4) : développement local (docker compose), préproduction et production. La préproduction et la production sont deux services Render distincts, chacun avec ses propres variables d'environnement et sa propre branche Neon : l'isolation est donc assurée au niveau des identifiants et des données. **La même version** (le commit `sha7` validé par la CI, dont l'image `ghcr.io/domoufan/itsn-backend:<sha7>` est l'artefact analysé) est déployée dans les deux environnements ; seule la configuration change, conformément au principe « une construction, plusieurs déploiements ». L'offre gratuite de Render ne permettant pas de déployer une image privée de GHCR, le service reconstruit l'image à partir du même `Dockerfile` et du même commit ; l'image GHCR reste l'artefact de référence (analysée par Trivy, conservée dans le registre) et `GET /actuator/info` renvoie le commit effectivement en service (variable `RENDER_GIT_COMMIT`).

Tableau : Services cloud en exploitation (14/11/2026)

| Service | Rôle | Identifiant Render | URL | Base Neon |
|---|---|---|---|---|
| `itsn-api-staging` | API, préproduction (jeu de démonstration, agent IA activé pour l'évaluation) | `srv-dbs5vp6k1f9s738c7a90` | `https://itsn-api-staging.onrender.com` | branche `staging` (`br-wild-bread-bax602hm`) |
| `itsn-api` | API, production | `srv-dbs5vs3l550s73fhq3tg` | `https://itsn-api.onrender.com` | branche `main` (`br-sweet-mud-bav34m17`) |
| `itsn` | Interface Angular, site statique (réécriture `/api/*` vers l'API de production) | `srv-dbs5vv49v7es73bpn3sg` | `https://itsn.onrender.com` | — |

Le projet Neon `itsn` (`shy-breeze-50979710`, PostgreSQL 16.16, région Francfort) a été créé par le connecteur Neon ; les identifiants de connexion ont été transmis directement aux variables d'environnement Render sans transiter par le dépôt ni par la conversation d'assistance.

Tableau : Réseau, domaine, HTTPS et accès

| Élément | Choix | Détail |
|---|---|---|
| Domaine et DNS | Sous-domaines `*.onrender.com` fournis par Render ; domaine personnalisé optionnel (chapitre 5) | Enregistrement CNAME si domaine personnalisé |
| HTTPS | Certificats TLS émis et renouvelés automatiquement par Render | Redirection HTTP → HTTPS |
| Filtrage | Aucun port exposé hors 443 ; la base Neon n'accepte que des connexions TLS (`sslmode=require`) authentifiées | Pas de réseau privé sur l'offre gratuite ; identifiants forts |
| Accès à la plateforme | Comptes GitHub, Render et Neon de l'étudiant avec authentification à deux facteurs ; connecteurs Render et Neon de l'assistant limités à l'espace de travail du projet | Secrets stockés dans GitHub Secrets et dans les variables Render (`sync: false`) |
| CORS | Origines autorisées limitées à l'URL du site statique | Variable `CORS_ALLOWED_ORIGINS` |

### Provisionnement et infrastructure décrite par du code

L'infrastructure Render est décrite dans le fichier versionné `infra/render/render.yaml` (*blueprint*) : services, `Dockerfile`, plan, région, sondes de santé, désactivation du déploiement automatique (seule la CD décide des mises à jour), variables d'environnement (les secrets y sont déclarés `sync: false`, c'est-à-dire saisis dans l'interface et jamais écrits dans le dépôt). Le site statique y définit ses règles de réécriture (`/api/*` vers l'API, `/*` vers `index.html`). En pratique, les trois services ont été créés le 14 novembre 2026 par l'API Render (connecteur), le *blueprint* servant de description de référence ; la base Neon (projet, base, branches `main` et `staging`) a été créée une fois par l'API Neon, opérations documentées dans le guide de déploiement. Les ressources, la région, les coûts estimés et le plafond budgétaire (budget IA journalier dans l'application ; alertes de consommation activées dans les consoles Render et Neon) sont décrits au chapitre 5.

## 8.2 Intégration et livraison continues

### Définitions et mode retenu

L'**intégration continue** consiste à fusionner fréquemment le travail dans une branche partagée et à vérifier automatiquement chaque fusion (compilation, tests, analyses). La **livraison continue** étend cette automatisation jusqu'à produire, pour chaque version validée, un artefact prêt à être déployé, le passage en production restant soumis à une décision. Le **déploiement continu** supprime cette décision : toute version validée est déployée automatiquement en production.

Le mode retenu est la **livraison continue avec validation humaine avant production** : la préproduction est déployée automatiquement après la réussite de la CI sur `main` ; la production n'est mise à jour qu'après approbation d'un relecteur dans l'environnement GitHub « production ». Ce choix correspond à la maturité du projet (une seule personne, un pilote), à l'exigence EF-13 d'un contrôle humain sur les actions sensibles et à la recommandation du canevas.

Tableau : Déclencheurs, branches et responsabilités

| Événement | Branches | Actions | Responsable |
|---|---|---|---|
| Push ou pull request | `develop`, `main`, branches `feat/*` vers ces branches | Workflow CI : tests backend et frontend, analyse de sécurité | Automatique ; l'auteur corrige en cas d'échec |
| Fusion sur `main` (CI réussie) | `main` | Construction et publication des images `sha7` ; déploiement automatique en préproduction ; smoke tests | Automatique |
| Approbation de l'environnement « production » | `main` | Promotion de la même image en production ; smoke tests ; résumé de livraison | Relecteur désigné (étudiant ou encadreur) |
| Lancement manuel (`workflow_dispatch`) avec une version | — | Déploiement ou retour arrière vers la version indiquée | Étudiant |

### Description du pipeline

![Pipeline CI/CD : étapes, conditions de blocage et promotion (réalisation personnelle)](../diagrams/09-pipeline-cicd.png)

Le workflow **CI** (`.github/workflows/ci.yml`) comporte quatre travaux :

1. **Backend** : récupération du code, Java 21 avec cache Maven, base PostgreSQL 16 démarrée comme service du travail, `mvn verify` (compilation, tests unitaires et d'intégration, rapport de couverture JaCoCo), publication des rapports de tests et du JAR comme artefacts du workflow.
2. **Frontend** : Node 22 avec cache npm, `npm ci`, tests Vitest, construction de production, publication du bundle.
3. **Sécurité** : analyse Trivy du dépôt (vulnérabilités des dépendances, secrets, mauvaises configurations) ; **toute vulnérabilité critique ou haute corrigeable bloque la livraison**.
4. **Images Docker** (uniquement sur `main`, après réussite des trois précédents) : construction multi-étapes des images backend et frontend, étiquetage par les sept premiers caractères du commit (`sha7`) et `latest`, publication sur GHCR, analyse Trivy de l'image publiée (bloquante sur CRITICAL), résumé listant les artefacts.

Le workflow **CD** (`.github/workflows/cd.yml`) s'exécute à la fin d'une CI réussie sur `main` ou à la demande : résolution de la version, appel du *deploy hook* Render de préproduction avec le paramètre `ref=<commit>` désignant le commit validé, exécution du script `infra/render/smoke-test.sh` (attente de la version attendue sur `/actuator/info`, santé `UP`, refus d'une requête non authentifiée), puis, après approbation, promotion du **même commit** en production et nouveaux smoke tests. Le premier passage de la CD (exécution 43620363033) a été **bloqué par le smoke test** : la préproduction avait bien été reconstruite et mise en ligne, mais `/actuator/info` ne renvoyait pas le commit attendu (anomalie AN-11, variable figée dans l'image) ; la production a été sautée, exactement comme prévu. Après correction, l'exécution 43622128103 a livré `962f5d4` en préproduction (4 min 03 s) puis, après approbation, en production (4 min 06 s) ; l'exécution 43623671662 a livré `2410df5`, et l'exécution 43624659808, lancée manuellement avec `version = 962f5d4`, a ramené les deux environnements à la version précédente (section 8.3). Un workflow complémentaire, **Eval agent IA** (`.github/workflows/eval-agent.yml`, lancement manuel), exécute le script d'évaluation du chapitre 7 contre un environnement déployé, après avoir vérifié sur `/api/ai/status` que l'agent y est activé.

### Exécution réelle de la CI : ce que l'analyse de sécurité a bloqué

La première exécution complète de la CI sur GitHub, le 14 novembre 2026, est instructive : sept exécutions ont été nécessaires avant la première réussite (exécution 43598035285, 4 min 36 s), et chaque échec correspondait à un **blocage légitime** de la porte de sécurité, corrigé par un commit dédié.

Tableau : Historique de la mise au point de la CI (14/11/2026)

| Exécution | Cause de l'échec | Correction (commit) |
|---|---|---|
| 43594017151, 43596390529 | Action `aquasecurity/trivy-action@0.28.0` introuvable : à la suite de la compromission de versions de cette action en mars 2026, l'éditeur a retiré les étiquettes antérieures à 0.35.0 | Épinglage sur `v0.36.0` (`9830ecc`) |
| 43596542610 | Vulnérabilité haute CVE-2026-54291 dans le pilote PostgreSQL JDBC 42.7.x ; image frontend exécutée en `root` (Trivy DS-0002) | Pilote 42.7.12 ; image `nginx-unprivileged` (`037b3ff`) |
| 43597091060 | Trivy exige un `USER` explicite dans le `Dockerfile` frontend | `USER nginx` (`8d92268`) |
| 43597425188 | Échec de construction Docker : apostrophe dans un commentaire de fin de ligne du `Dockerfile` | Commentaires sur lignes dédiées (`16552a6`) |
| 43598035285 | — (après trois vulnérabilités critiques détectées dans l'image backend : CVE-2026-55276, CVE-2026-65182, CVE-2026-47884, corrigées par Spring Boot 4.0.9 et Tomcat 11.0.29, `050dd2c`) | **Réussite** : images `ghcr.io/domoufan/itsn-backend:050dd2c` et `…-frontend:050dd2c` publiées et analysées (0 vulnérabilité critique) |

Cette séquence illustre concrètement la valeur d'un critère bloquant : sans lui, un pilote JDBC vulnérable et une image privilégiée auraient été livrés. Elle rappelle aussi que la chaîne d'approvisionnement de la CI elle-même (actions tierces) est une surface d'attaque, d'où l'épinglage des actions sur une version précise.

**Critères bloquants** : échec de compilation ou de test, vulnérabilité critique ou haute corrigeable, secret détecté, image non analysée, smoke test en échec. **Traitement des échecs** : la pull request ne peut être fusionnée ; en CD, un échec de smoke test en préproduction interrompt la chaîne avant la production ; un échec en production déclenche la procédure de retour arrière.

### Fichiers, agents d'exécution, registre et configuration

Tableau : Éléments du pipeline

| Élément | Valeur |
|---|---|
| Fichiers de pipeline | `.github/workflows/ci.yml`, `.github/workflows/cd.yml` (versionnés, reproduits en annexe expurgés de tout secret) |
| Agents d'exécution | Exécuteurs hébergés GitHub `ubuntu-latest` |
| Registre d'artefacts | GitHub Container Registry : `ghcr.io/domoufan/itsn-backend` et `…-frontend`, étiquettes `sha7` et `latest` ; rapports de tests conservés 30 jours comme artefacts de workflow |
| Variables de configuration | `STAGING_URL`, `PRODUCTION_URL` (variables d'environnement GitHub) ; variables Render par service |
| Secrets | `RENDER_DEPLOY_HOOK_STAGING`, `RENDER_DEPLOY_HOOK_PRODUCTION` (GitHub Secrets) ; `DATABASE_*`, `JWT_SECRET`, `ANTHROPIC_API_KEY` (Render) ; jamais dans le dépôt ni dans les journaux (les étapes n'affichent que le nom de l'image) |
| Permissions | Jeton `GITHUB_TOKEN` limité à `contents: read`, `packages: write` ; CD limité à `contents: read` |
| Traçabilité | commit → image `sha7` → exécution du workflow → `GET /actuator/info` renvoyant `version` et `commit` (`RENDER_GIT_COMMIT`) |

## 8.3 Mise en production et exploitation

**Procédure de déploiement.** Décrite pas à pas dans `docs/deploy/GUIDE_DEPLOIEMENT.md` : préparation unique des comptes et des secrets, puis chaîne automatisée. **Migrations de données** : exécutées par Flyway au démarrage de l'application ; la règle adoptée est celle des **migrations additives** (ajout de tables ou de colonnes, jamais de suppression ni de renommage dans la même version), afin que la version N-1 de l'application reste compatible avec le schéma N. **Vérifications après mise en service** : smoke tests automatiques, puis vérification manuelle d'un parcours (connexion, création de ticket, tableau de bord).

**Retour arrière.** Relancer le workflow CD avec la version `sha7` précédente : la même procédure redéploie l'ancienne version, d'abord en préproduction puis en production après approbation. Si une migration non additive avait été introduite malgré la règle, la branche Neon serait restaurée à l'instant précédant le déploiement, puis l'ancienne version redéployée. La procédure a été **exécutée le 14 novembre 2026** (TD-04) : après la livraison de `2410df5`, la relance du workflow avec `962f5d4` a ramené la préproduction (18:09) puis la production (18:20, après approbation) à cette version, chacune vérifiée par le smoke test ; durée d'environ quatre minutes par environnement, dominée par la reconstruction de l'image sur l'offre gratuite. Un retour arrière emprunte ainsi le même chemin contrôlé qu'une livraison : la version de secours est elle aussi vérifiée avant la production.

**Exploitation.** Contrôles de santé : `/actuator/health/liveness` et `/readiness` utilisés par Render pour redémarrer un service défaillant ; journaux : Render (sept jours sur l'offre gratuite) et journal d'audit en base ; métriques : `/actuator/metrics` et `/actuator/prometheus` protégés ; alertes : budget IA journalier dans l'application, alertes de consommation Render et Neon ; sauvegardes : restauration à un instant donné de Neon et export `pg_dump` périodique, test de restauration TD-03. **Responsabilités et limites de disponibilité** : l'étudiant assure l'exploitation du pilote ; les offres gratuites n'offrent aucun engagement de disponibilité et mettent le service en veille après quinze minutes d'inactivité (redémarrage d'environ une minute) ; le passage à des instances payantes (chapitre 5) supprime cette limite.

## 8.4 Éléments attendus pour le déploiement

Tableau : Livrables de déploiement et état

| Livrable attendu | Emplacement | État à la date de rédaction |
|---|---|---|
| Schéma cloud | Figure du présent chapitre | Fourni |
| Guide de déploiement | `docs/deploy/GUIDE_DEPLOIEMENT.md` | Fourni |
| Configuration du pipeline expurgée des secrets | `.github/workflows/*.yml`, annexe | Fournie |
| Preuve d'une exécution avec son statut | CI : `https://github.com/domoufan/itsn/actions/runs/43598035285` (réussie) et historique `docs/tests/2026-11-14/ci/` ; évaluation de l'agent : exécution 43613168200 (réussie) | Fournie |
| Identifiant de la version déployée | `962f5d4` en service en préproduction et en production après le retour arrière (14/11/2026, 18:20 UTC), renvoyé par `GET /actuator/info` ; premier déploiement `f561b63` documenté dans `docs/tests/2026-11-14/deploiement/premier-deploiement.md` | Fourni |
| Résultats des tests après déploiement | Sorties de `smoke-test.sh` (« santé UP, version <sha7>, authentification exigée ») pour les quatre déploiements réussis et le déploiement bloqué : `docs/tests/2026-11-14/deploiement/TD-02-cd.md` ; 90 triages réels contre la préproduction (TE-01) | Fournis |
| Procédure de retour arrière | Section 8.3 et guide ; test TD-04 exécuté (CD 43624659808) | Fournis : `docs/tests/2026-11-14/deploiement/TD-04-retour-arriere.md` |
| Chiffrage cloud | Chapitre 5 | Fourni |
| Démonstration locale | `docker compose up --build` | Disponible ; signalée comme locale |

> **Mention explicite.** Le déploiement cloud est **réalisé et prouvé** : services en ligne, base distante, livraison par la chaîne CD avec approbation humaine (TD-02), retour arrière exécuté (TD-04), évaluation de l'agent en préproduction (TE-01). Les offres utilisées sont gratuites et n'offrent aucun engagement de disponibilité ; la démonstration locale (`docker compose`) reste disponible pour la soutenance en cas d'indisponibilité du réseau.

## 8.5 Mise en place d'un pipeline agentique

### Rôle dans le projet et distinction avec la CI/CD

Le pipeline agentique d'ITSN est un enchaînement de tâches dans lequel **un agent d'IA unique** utilise des outils pour atteindre un objectif défini : **qualifier un ticket** (catégorie, impact, urgence, technicien pressenti, articles de connaissance liés, résumé). Son rôle est l'**assistance à la décision** du responsable support ; il ne remplace ni la décision ni l'action humaine. Il se distingue du pipeline CI/CD, qui construit, teste et livre les versions du logiciel : l'un agit sur les **données métier** à l'exécution, l'autre sur le **code** lors de la livraison. Les deux partagent toutefois une exigence : une validation humaine avant tout effet irréversible.

### Objectif, entrées, sorties et critères de réussite

Tableau : Cadrage du pipeline agentique

| Élément | Définition |
|---|---|
| Cas d'usage | Triage d'un ticket ouvert à la demande du personnel |
| Entrées | Référence, type, titre, description, site, équipement du ticket (aucune donnée personnelle) ; référentiels (catégories) ; résultats d'outils |
| Sorties attendues | Objet JSON strict : `categoryCode`, `impact`, `urgency`, `suggestedTechnicianId`, `summary`, `rationale`, `relatedArticleIds`, `confidence` |
| Critères de réussite | Sortie valide contre les référentiels ; exactitude de catégorie ≥ 80 % et de priorité à un cran ≥ 90 % sur le jeu d'évaluation ; latence médiane < 10 s ; coût < 0,01 USD par triage ; aucun détournement par le contenu du ticket |
| Modèle | Claude Haiku 4.5 (Anthropic), température 0, 1 024 jetons de sortie au plus |
| Outils accessibles | `search_knowledge_base(query)`, `list_technicians()`, `find_similar_tickets(query)` — tous en lecture seule, résultats tronqués, sans e-mail ni téléphone |
| Règles d'orchestration | Au plus 6 appels d'outils ; délai HTTP de 30 s ; budget journalier (1 USD par défaut) ; une seule proposition en attente par ticket ; repli heuristique si l'IA est indisponible |
| Données conservées entre étapes | L'historique des messages de la boucle (en mémoire, le temps de l'exécution) ; en base : la proposition, les métriques et la trace brute tronquée (20 000 caractères) |

Un agent unique suffit : la tâche est bornée, séquentielle et ne requiert ni spécialisation ni débat entre agents ; une architecture multi-agents aurait ajouté coût, latence et surface d'attaque sans gain démontrable.

### Architecture et flux

![Flux d'orchestration du pipeline agentique de triage (réalisation personnelle)](../diagrams/10-agent-flux.png)

Les **conditions de passage** entre étapes sont : garde-fous satisfaits → exécution de l'agent, sinon repli ; `stop_reason = tool_use` → exécution de l'outil puis nouvel appel ; `stop_reason = end_turn` → extraction et validation du JSON ; validation réussie → proposition en attente, sinon proposition en échec. Les **conditions d'arrêt** sont la limite d'appels d'outils, la limite d'itérations, le délai HTTP et toute exception technique ; dans tous ces cas, une proposition `FAILED` est enregistrée avec le motif, sans effet sur le ticket. La **reprise après échec** est manuelle : le personnel peut relancer le triage (éventuellement en mode repli) ou qualifier le ticket lui-même ; il n'y a pas de nouvelle tentative automatique, afin de ne pas consommer le budget sur une erreur persistante.

### Mise en œuvre et articulation avec la CI/CD

Les instructions système, le format de sortie, la déclaration des outils, les paramètres (modèle, limites, tarifs) et le code d'orchestration sont **versionnés dans le dépôt** (`backend/src/main/java/sn/senetech/itsn/ai/`, `application.yml`) et documentés dans `tools/agent/README.md`. Les dépendances se limitent au client HTTP de Spring ; aucune bibliothèque d'orchestration tierce n'est utilisée, ce qui rend la boucle entièrement lisible et testable. L'agent **ne propose aucune modification de code** : il agit sur des données métier, et ses propositions passent par la revue humaine décrite plus haut. Les tests de la CI couvrent l'extraction et la validation de la sortie (TU-04), le repli (TU-05) et le flux complet en mode repli (TI-20 à TI-24) ; l'appel réel au modèle n'est pas exécuté en CI (coût, non-déterminisme), mais par le script d'évaluation.

**Permissions et isolement.** Les outils n'ont que des droits de lecture et ne renvoient que des champs nécessaires ; les paramètres fournis par le modèle sont nettoyés (caractères de contrôle, jokers SQL, longueur). Aucune action d'écriture, de suppression ou de mise en production n'est accessible à l'agent. **Validation humaine** : obligatoire avant toute application (EF-13). **Limites** : durée (30 s), nombre d'actions (6 outils, 7 appels au modèle au plus), coût (budget journalier). **Interruption** : désactivation par variable d'environnement (`AI_ENABLED=false`) sans redéploiement de code. **Repli manuel** : simulation heuristique locale, puis qualification manuelle.

### Données et contrôle des actions

Le cadre d'usage de l'IA de l'établissement est appliqué : aucun secret ni donnée confidentielle n'est transmis (les e-mails, téléphones et mots de passe sont exclus des outils) ; la clé d'API ne figure ni dans le dépôt ni dans les journaux. Les contenus récupérés — description du ticket rédigée par un utilisateur, articles, tickets passés — sont **traités comme des données** : ils sont encadrés par des balises explicites (`<ticket_data>`, `<tool_data>`), le prompt système rappelle qu'ils ne peuvent modifier ni les consignes ni les autorisations, les balises de fermeture présentes dans un contenu utilisateur sont neutralisées, et la sortie est **validée côté serveur** (catégorie existante, échelles, technicien actif) avant toute présentation. Les appels d'outils et les sorties sont contrôlés et tracés (trace brute conservée, journal d'audit).

### Évaluation et preuves attendues

L'évaluation (chapitre 7, TE-01) couvre des cas représentatifs, des entrées imprécises, un cas adverse et le scénario d'indisponibilité des outils (le repli). Elle mesure la qualité des résultats, le taux de réussite, les erreurs, les interventions humaines (toutes les propositions en nécessitent une par construction), la durée et le coût, et compare l'agent au repli heuristique. Les cas sont répétés pour apprécier la variabilité.

Tableau : Preuves du pipeline agentique et état

| Preuve attendue | Emplacement | État |
|---|---|---|
| Schéma d'orchestration | Figure ci-dessus | Fourni |
| Instructions et configurations expurgées des secrets | Code versionné ; prompt reproduit en annexe | Fourni |
| Trace d'exécution | Colonne `raw_response` et `rationale` des 90 propositions de la préproduction (INC-2026-00137 à 00226) ; justifications du cas adverse reproduites au chapitre 7 (TS-06) | Fournie |
| Validations humaines | Table `ai_suggestions` : 89 propositions rejetées par le script d'évaluation avec le commentaire « évaluation automatique » (auteur, date), 1 restée en attente ; revue manuelle exercée par TM-07 | Fournie |
| Résultats des tests | TU-04, TU-05, TI-20 à TI-24 (CI), TE-01 réel (14/11/2026 : catégorie 98 %, priorité 59 % exact et 100 % à un cran, 0 détournement sur 3, 0,61 USD) | Fournis |
| Limites observées | Priorité sous la cible de 60 % par surestimation de l'urgence à la frontière P1/P2 ; variabilité de la priorité entre répétitions (5 cas sur 30) ; latence de 6 s | Consignées (chapitre 7 et conclusion) |

> **État du pipeline à la date de rédaction : réalisé et évalué avec le modèle réel** (code complet, garde-fous, repli, revue humaine, tests automatisés en mode repli, évaluation de 90 triages en préproduction le 14 novembre 2026). Les usages et contrôles sont consignés dans le journal d'IA en annexe ; les coûts sont intégrés au chapitre 5.

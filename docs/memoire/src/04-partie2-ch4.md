<<<pagebreak>>>
# Chapitre 4 — Conception fonctionnelle et technique du système

Ce chapitre décrit la solution avant sa réalisation et montre comment elle répond aux exigences du chapitre 2. Il présente d'abord la conception fonctionnelle (parcours, règles métier), puis la modélisation UML, l'architecture, la sécurité et l'exploitation, et enfin les choix techniques comparés.

## Conception fonctionnelle

### Vue d'ensemble : diagramme de contexte

![Diagramme de contexte d'IntervIT : acteurs humains et systèmes externes (réalisation personnelle)](../diagrams/01-contexte.png)

Le système échange avec quatre acteurs humains et deux systèmes externes. Les flux vers l'API d'intelligence artificielle ne contiennent que le contenu du ticket (titre, description, type, site, équipement) et les résultats d'outils en lecture seule ; aucune adresse e-mail, aucun mot de passe ni numéro de téléphone n'est transmis.

### Parcours utilisateurs essentiels

**Parcours 1 — Le demandeur déclare un incident.** Aminata, employée de l'agence bancaire fictive du Plateau, constate que l'imprimante du guichet est hors ligne. Elle se connecte, choisit « Nouveau ticket », indique qu'il s'agit d'un incident, saisit un titre et une description, estime l'impact (un service perturbé : 2) et l'urgence (dans la journée : 2) et, si elle le connaît, l'équipement concerné. La priorité calculée (P3) s'affiche en direct. À la validation, elle reçoit un e-mail de confirmation portant la référence INC-2026-00043 et l'échéance de résolution cible. Elle peut ensuite suivre l'état, commenter, et, une fois le ticket résolu, confirmer la clôture ou le rouvrir, puis noter le service.

**Parcours 2 — Le responsable qualifie et affecte.** Moussa, responsable support, voit le ticket dans la liste des nouveaux. Il peut l'affecter directement à un technicien en consultant la charge et les compétences de chacun, ou lancer le triage IA. Dans ce cas, l'agent propose une catégorie (Matériel), une priorité recalculée (P2 car un guichet est à l'arrêt), le technicien le plus adapté (compétence matériel, charge la plus faible) et des articles de la base de connaissances. Moussa compare la proposition aux valeurs actuelles dans la file de revue, accepte tout, une partie ou rejette ; sa décision est tracée. Le ticket passe à l'état « Affecté » et le technicien est notifié.

**Parcours 3 — Le technicien intervient.** Fatou, technicienne matériel, retrouve le ticket dans « Mes tickets », le prend en charge (ce qui fixe la date de première réponse et vérifie le SLA de prise en compte), planifie une intervention sur site à 14 h (le système refuse un créneau qui chevaucherait une autre intervention), consulte l'article « Imprimante réseau hors ligne : procédure de diagnostic », intervient, rédige son compte rendu et résout le ticket avec un résumé de résolution obligatoire.

**Parcours 4 — Le responsable pilote.** Chaque lundi, Moussa consulte le tableau de bord : tickets ouverts, tickets hors délai, taux de respect des SLA de la période, délai moyen de résolution, charge par technicien, répartition par catégorie. Il en tire les rapports d'activité destinés aux clients et identifie les catégories récurrentes qui méritent un article de connaissance ou une action préventive.

### Règles métier

Tableau : Principales règles métier

| Id | Règle | Mise en œuvre |
|---|---|---|
| RM-01 | La priorité est déterminée par la matrice impact × urgence : P1 si impact 1 et urgence 1 ; P2 si la somme vaut 3 ; P3 si elle vaut 4 ; P4 au-delà | `PriorityMatrix.compute()` ; recalcul à toute modification de l'impact ou de l'urgence |
| RM-02 | Les échéances SLA sont calculées à partir de la date de création et de la politique de la priorité : P1 = 30 min / 4 h ; P2 = 1 h / 8 h ; P3 = 4 h / 24 h ; P4 = 8 h / 72 h (valeurs par défaut, modifiables) | `SlaService.applyDeadlines()` ; table `sla_policies` |
| RM-03 | La première réponse publique du support ou le passage en « En cours » vaut prise en compte ; la date est figée et comparée à l'échéance | `TicketService.transition()` et `addComment()` |
| RM-04 | Un ticket ne peut passer « En cours » que s'il est affecté ; il ne peut être résolu sans résumé de résolution | Machine à états `TicketStatus` + contrôles du service |
| RM-05 | Un demandeur ne voit que ses tickets, ne voit pas les notes internes, ne peut que clôturer, rouvrir ou annuler avant prise en charge | Filtrage serveur par rôle |
| RM-06 | Un technicien n'agit que sur les tickets qui lui sont affectés et ne planifie que ses propres interventions ; la clôture et l'annulation lui sont interdites | Contrôles d'autorisation dans les services |
| RM-07 | Deux interventions planifiées d'un même technicien ne peuvent se chevaucher | Requête `countOverlapping` avant enregistrement |
| RM-08 | Une proposition de l'agent IA n'est jamais appliquée sans décision humaine ; une seule proposition en attente par ticket ; la décision est réservée au responsable ou à l'administrateur | `AiSuggestionService.review()` ; autorisations |
| RM-09 | Le triage bascule sur la simulation locale si l'IA est désactivée, si la clé est absente ou si le budget journalier est atteint | Garde-fous de `AiSuggestionService.triage()` |
| RM-10 | Les comptes ne sont jamais supprimés mais désactivés, afin de conserver l'historique | `UserAdminController.deactivate()` |

### Maquettes et scénarios

Les écrans ont été conçus selon un gabarit unique : navigation latérale filtrée par rôle, barre d'outils de filtres, tableau ou cartes, fiche détaillée avec onglets (commentaires, interventions, triage IA, historique). Les captures des écrans réalisés sont présentées au chapitre 6 ; elles tiennent lieu de maquettes haute fidélité, les maquettes basse fidélité initiales ayant été directement traduites en composants Angular Material pour gagner du temps.

## Modélisation

### Diagramme de cas d'utilisation

![Diagramme de cas d'utilisation (réalisation personnelle)](../diagrams/02-cas-utilisation.png)

Le diagramme regroupe les quinze cas d'utilisation retenus. L'authentification est commune à tous les acteurs. Les cas liés à l'IA (lancer le triage, valider une proposition) sont séparés : le premier est ouvert à tout le personnel, le second réservé au responsable et à l'administrateur, ce qui matérialise l'exigence EF-13.

### Diagramme de classes

![Diagramme de classes du domaine (réalisation personnelle)](../diagrams/03-classes.png)

Le modèle s'articule autour de la classe `Ticket`, reliée au demandeur et au technicien (deux associations vers `User`), à une `Category`, à un `Site` et éventuellement à un `Equipment`. Le ticket compose ses `TicketComment`, `TicketHistory`, `Intervention` et `AiSuggestion` : la suppression d'un ticket entraînerait celle de ses éléments (en pratique, les tickets ne sont jamais supprimés). La politique `SlaPolicy` n'est pas liée par une association persistante mais consultée selon la priorité au moment du calcul des échéances, ce qui permet de modifier les délais sans altérer les tickets existants. Le `TicketSequence` (non représenté) porte le compteur annuel des références.

Adaptation au parcours : le modèle est volontairement limité à quatorze classes. Une CMDB complète (relations entre équipements, logiciels, contrats) ou un modèle de changements auraient alourdi la conception sans servir les exigences retenues.

### Diagramme d'états du ticket

![Diagramme d'états-transitions du ticket (réalisation personnelle)](../diagrams/04-etats-ticket.png)

Sept états et douze transitions sont autorisés. Les états « Clôturé » et « Annulé » sont terminaux. La réouverture (Résolu → En cours) est réservée au demandeur et au responsable et efface la date de résolution, ce qui réinitialise le calcul du SLA de résolution. Chaque transition produit une entrée d'historique.

### Diagrammes de séquence

![Diagramme de séquence : création d'un ticket (réalisation personnelle)](../diagrams/05-sequence-creation.png)

La création illustre la chaîne complète : validation syntaxique au niveau du contrôleur, génération de la référence sous verrou pessimiste (deux créations simultanées obtiennent des numéros distincts), calcul de la priorité, calcul des échéances, persistance, historique, notification.

![Diagramme de séquence : triage par l'agent IA et revue humaine (réalisation personnelle)](../diagrams/06-sequence-triage-ia.png)

Le triage montre les deux temps du pipeline agentique : l'exécution bornée de l'agent (garde-fous, boucle d'appels d'outils, validation de la sortie) qui produit une proposition en attente, puis la décision humaine qui seule modifie le ticket. L'alternative « repli heuristique » matérialise la solution de secours exigée par le canevas.

## Architecture

### Composants, interfaces et flux

![Architecture logicielle d'IntervIT (réalisation personnelle)](../diagrams/07-architecture.png)

L'architecture est une architecture **client-serveur à trois niveaux** : une application monopage Angular servie par nginx, une API REST Spring Boot organisée en couches (sécurité, contrôleurs, services, persistance), une base PostgreSQL. Le choix d'une **application monolithique modulaire** plutôt que de microservices est délibéré : pour une PME et une équipe réduite, un seul déployable simplifie l'exploitation, les transactions et les tests ; les modules (auth, ticket, intervention, catalog, kb, dashboard, ai) sont séparés en paquets Java pour préserver la possibilité d'une extraction ultérieure.

Tableau : Composants du backend et responsabilités

| Paquet | Composants | Responsabilité |
|---|---|---|
| `config` | `SecurityConfig`, `AppProperties`, `OpenApiConfig` | Filtre de sécurité, JWT, CORS, propriétés typées et validées, documentation OpenAPI |
| `auth` | `AuthController`, `AuthService`, `CurrentUserService` | Connexion, émission du jeton, résolution de l'utilisateur courant, changement de mot de passe |
| `ticket` | `TicketController`, `TicketService`, `PriorityMatrix`, `SlaService`, `TicketReferenceService`, `TicketSpecifications`, `SlaMonitorJob` | Cycle de vie des tickets, règles RM-01 à RM-06, recherche filtrée, tâche de détection des dépassements |
| `intervention` | `InterventionController` | Planning, conflits de créneaux (RM-07), comptes rendus |
| `catalog` | `UserAdminController`, `SiteController`, `CategoryController`, `SlaPolicyController`, `EquipmentController` | Référentiels et administration |
| `kb` | `KnowledgeController` | Base de connaissances |
| `dashboard` | `DashboardController` | Calcul des indicateurs |
| `ai` | `AiController`, `AiSuggestionService`, `ClaudeTriageAgent`, `HeuristicTriageAgent`, `TriageTools`, `AnthropicClient` | Pipeline agentique (RM-08, RM-09) |
| `notification` | `NotificationService` | E-mails, tolérants aux pannes |
| `common` | `GlobalExceptionHandler`, `AuditService`, exceptions | Erreurs uniformes, journal d'audit |
| `seed` | `DemoDataSeeder` | Jeu de données fictives |

L'**interface** entre le frontend et le backend est une API REST JSON documentée par OpenAPI (Swagger UI), dont les principales ressources sont : `/api/auth`, `/api/tickets`, `/api/interventions`, `/api/equipment`, `/api/sites`, `/api/categories`, `/api/sla-policies`, `/api/users`, `/api/kb/articles`, `/api/dashboard/kpis`, `/api/ai`. Les erreurs suivent un format unique (horodatage, code, message, détail par champ).

### Stockage

Le **schéma relationnel** (treize tables) est géré exclusivement par des migrations Flyway versionnées ; Hibernate se contente de valider la correspondance (`ddl-auto: validate`), ce qui garantit que le schéma de production est celui qui a été relu et testé. Les choix notables : horodatages en `TIMESTAMPTZ` (UTC), contraintes `CHECK` sur les énumérations et les échelles, index sur les colonnes de filtrage (état, priorité, technicien, demandeur, date), suppression en cascade des éléments composés d'un ticket, désactivation logique des utilisateurs.

### Environnements et dépendances

Tableau : Environnements

| Environnement | Rôle | Base de données | Particularités |
|---|---|---|---|
| Développement local | Développement et démonstration hors ligne | PostgreSQL 16 en conteneur (docker compose) | Mailpit capture les e-mails ; données de démonstration ; IA en simulation par défaut |
| Test (CI) | Exécution automatisée des tests | PostgreSQL 16 en service GitHub Actions | Profil `test`, schéma recréé à chaque exécution, données de démonstration désactivées |
| Préproduction | Validation avant mise en production | Neon, branche `staging` | Même image Docker que la production ; données de démonstration |
| Production | Exploitation | Neon, branche `main` | Image promue depuis la préproduction ; IA activée avec budget |

## Sécurité et exploitation

### Authentification et autorisations

L'API est **sans état** : après connexion (e-mail et mot de passe vérifié contre un condensat BCrypt de coût 12), le serveur émet un **JWT signé en HMAC-SHA256** (RFC 7519) contenant l'identifiant, le nom, l'e-mail et le rôle, valable huit heures. Le client le présente dans l'en-tête `Authorization: Bearer`. Spring Security vérifie la signature et l'expiration, puis dérive les autorisations du rôle. Les règles d'accès sont déclarées à deux niveaux : par URL (`/api/**` authentifié, `/actuator/**` réservé à l'administrateur, documentation et sondes de santé publiques) et par méthode (`@PreAuthorize`) ou dans les services pour les règles dépendant des données (un demandeur et « ses » tickets). Le choix d'un secret HMAC plutôt que d'une paire de clés RSA tient à la simplicité : un seul service émet et vérifie les jetons.

### Protection des données

Les mots de passe ne sont jamais stockés ni journalisés en clair ; la politique de complexité est appliquée au changement de mot de passe. Les données transmises à l'API d'IA sont limitées au contenu du ticket et aux résultats d'outils, qui excluent e-mails, téléphones et mots de passe. Les captures d'écran du mémoire ne contiennent que des données fictives. En production, le chiffrement en transit (TLS) est assuré par la plateforme d'hébergement ; la base Neon chiffre les données au repos.

### Journalisation, sauvegardes et restauration

Un **journal d'audit** (`audit_log`) enregistre les connexions réussies et échouées, les changements de mot de passe et de rôle, les désactivations, les affectations, les exécutions et les décisions relatives à l'IA. Les journaux applicatifs ne contiennent ni secrets ni données personnelles. Les sauvegardes reposent sur la restauration à un instant donné offerte par Neon et sur un export `pg_dump` périodique ; un test de restauration est documenté au chapitre 7.

### Principaux risques de sécurité et mesures

Tableau : Risques de sécurité (référence : OWASP Top 10, édition 2021) et mesures prévues

| Risque | Mesure de conception |
|---|---|
| Contrôle d'accès défaillant (A01) | Autorisations appliquées côté serveur à chaque requête ; filtrage des données par rôle ; interface non considérée comme une barrière |
| Défaillances cryptographiques (A02) | BCrypt pour les mots de passe ; JWT signé ; TLS en production ; secret JWT d'au moins 32 caractères imposé au démarrage |
| Injection (A03) | Requêtes JPA paramétrées ; validation des entrées ; aucune concaténation SQL ; paramètres d'outils de l'agent nettoyés |
| Conception non sécurisée (A04) | Machine à états ; verrou sur la génération des références ; principe du moindre privilège pour les outils de l'agent |
| Mauvaise configuration (A05) | CSRF désactivé uniquement parce que l'API est sans état et sans cookie ; CORS restreint aux origines déclarées ; en-têtes de sécurité nginx ; Actuator protégé |
| Composants vulnérables (A06) | Analyse Trivy des dépendances et des images en CI, bloquante au-delà du seuil |
| Identification et authentification (A07) | Expiration des jetons ; journalisation des échecs ; politique de mots de passe |
| Défaut d'intégrité logicielle (A08) | Artefacts versionnés par empreinte de commit ; même artefact promu entre environnements |
| Journalisation insuffisante (A09) | Journal d'audit ; journaux applicatifs ; métriques |
| Risques propres aux LLM (OWASP Top 10 pour les applications LLM, 2025 : injection de prompt, consommation non bornée) | Contenu traité comme donnée ; outils en lecture seule ; validation de la sortie ; limites d'appels, de jetons, de délai et de budget ; validation humaine |

## Choix techniques

Tableau : Choix techniques, versions, alternatives étudiées

| Domaine | Choix retenu (version) | Alternatives étudiées | Raisons du choix | Contraintes et limites |
|---|---|---|---|---|
| Langage et cadriciel serveur | Java 21 (LTS), Spring Boot 4.0 (Spring Framework 7, Spring Security 7, Hibernate 7) | PHP / Laravel ; Node.js / NestJS ; Python / Django | Pile maîtrisée par l'auteur et enseignée dans la formation ; écosystème complet (sécurité, JPA, validation, Actuator) ; typage fort ; version courante bénéficiant du support | Démarrage plus lourd qu'un cadriciel interprété ; Spring Boot 4 impose des starters modulaires (`spring-boot-starter-webmvc`, `-flyway`, starters de test dédiés) |
| Base de données | PostgreSQL 16 | MySQL / MariaDB ; H2 (embarqué) | Robustesse, `TIMESTAMPTZ`, contraintes `CHECK`, disponibilité d'offres managées gratuites (Neon) ; identique en test, développement et production | Pas de recherche plein texte avancée exploitée (recherche par `LIKE` suffisante au volume visé) |
| Migrations | Flyway (starter Spring Boot 4) | Liquibase ; génération automatique Hibernate | Scripts SQL lisibles et relus ; historique des versions ; discipline de migrations additives pour le retour arrière | — |
| Cadriciel client | Angular 22 (composants autonomes, signaux) + Angular Material | React ; Vue.js ; Blade/Thymeleaf côté serveur | Structure imposée adaptée à une application de gestion ; bibliothèque de composants accessibles ; TypeScript ; outillage de test intégré (Vitest) | Courbe d'apprentissage ; poids initial du bundle (environ 330 Ko compressés à 92 Ko) |
| Authentification | JWT HS256 via Spring Security OAuth2 Resource Server (Nimbus) | Sessions serveur ; bibliothèque jjwt ; fournisseur d'identité externe (Keycloak) | API sans état adaptée à un SPA ; pas de dépendance supplémentaire ; vérification de signature et d'expiration intégrée | Révocation immédiate d'un jeton impossible avant expiration (atténuée par une durée de 8 h et la désactivation du compte) |
| IA : fournisseur et modèle | API Anthropic (Messages, appel d'outils), modèle Claude Haiku 4.5 | Modèle local (Ollama) ; autres API (OpenAI, Mistral, Gemini) | Appel d'outils natif ; coût d'entrée faible (1 USD par million de jetons en entrée, 5 USD en sortie, grille publique consultée en novembre 2026) ; latence adaptée à une tâche de classification | Dépendance à un service externe payant, traitée par le repli heuristique et le budget plafonné ; données transmises hors du pays (limitées au strict nécessaire) |
| Conteneurisation | Docker (images multi-étapes, utilisateur non root), docker compose | Installation native | Reproductibilité ; même image en préproduction et production | — |
| CI/CD | GitHub Actions | GitLab CI ; Jenkins | Intégré au dépôt ; gratuit pour le volume visé ; environnements avec approbation | Minutes limitées sur l'offre gratuite |
| Hébergement | Render (services web depuis images, site statique) + Neon (PostgreSQL) | Railway ; Fly.io ; VPS (OVH, Contabo) ; Render Postgres | Offres gratuites permanentes ; déploiement depuis une image de registre ; base PostgreSQL managée avec branches et restauration ; aucune administration système | Mise en veille après 15 minutes d'inactivité (redémarrage d'environ une minute) ; pas de garantie de disponibilité ; la base Render gratuite expirant après une période limitée a été écartée au profit de Neon |
| Analyse de sécurité | Trivy | OWASP Dependency-Check ; Snyk | Gratuit, rapide, couvre dépendances, secrets, configurations et images | Faux positifs possibles ; seuil fixé à CRITICAL/HIGH |

## Correspondance entre exigences et composants

Tableau : Correspondance exigences → composants (extrait ; matrice complète en annexe)

| Exigence | Composants principaux |
|---|---|
| EF-01, ENF-01, ENF-02, ENF-03 | `SecurityConfig`, `AuthService`, `CurrentUserService`, `UserAdminController`, `PasswordEncoder` (BCrypt), `ChangePasswordRequest` |
| EF-02, EF-03, EF-04, EF-05 | `TicketService`, `TicketReferenceService`, `PriorityMatrix`, `TicketStatus`, `SlaService`, `SlaMonitorJob`, migration `V1` (contraintes) |
| EF-06 | `SlaPolicyController`, table `sla_policies` |
| EF-07, EF-15, EF-16, EF-18 | `TicketService` (`assign`, `comment`, `rate`), `TicketHistory`, `TicketComment` |
| EF-08, EF-09 | `InterventionController`, `InterventionRepository.countOverlapping` |
| EF-10 | `DashboardController`, requêtes agrégées de `TicketRepository` |
| EF-11 | `EquipmentController`, `Equipment` |
| EF-12, EF-13, ENF-07, ENF-08 | `AiSuggestionService`, `ClaudeTriageAgent`, `TriageTools`, `HeuristicTriageAgent`, `AiController`, `AiSuggestion` |
| EF-14 | `KnowledgeController`, `KnowledgeArticleRepository.search` |
| EF-17 | `NotificationService` |
| ENF-04 | Bean Validation sur les DTO, `GlobalExceptionHandler`, Spring Data JPA |
| ENF-05 | `AppProperties` (variables d'environnement), `.gitignore`, `.env.example`, analyse Trivy |
| ENF-06 | `AuditService`, `AuditLog` |
| ENF-09, ENF-10 | Index SQL, pagination, Actuator (`health`, `info`, `metrics`) |
| ENF-11, ENF-12, ENF-13 | `docker-compose.yml`, Dockerfiles, Flyway, workflows `ci.yml` et `cd.yml`, guide de déploiement |
| ENF-14 | Composants Angular Material, libellés français, badges texte + couleur, `NotifyService` |

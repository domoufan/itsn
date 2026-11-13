<<<pagebreak>>>
# Partie 3 — Mise en œuvre

<<<pagebreak>>>
# Chapitre 6 — Réalisation et développement

## Environnement de travail et versions

Tableau : Environnement de développement et versions utilisées

| Élément | Version | Rôle |
|---|---|---|
| Java (OpenJDK Temurin) | 21.0 (LTS) | Exécution du backend |
| Apache Maven | 3.9 | Construction, tests, couverture (JaCoCo 0.8.13) |
| Spring Boot | 4.0.7 | Cadriciel serveur (Spring Framework 7, Spring Security 7, Spring Data JPA, Hibernate 7) |
| Flyway (starter Spring Boot) + `flyway-database-postgresql` | fournie par Spring Boot 4.0 | Migrations de schéma |
| springdoc-openapi | 3.0 | Documentation OpenAPI / Swagger UI |
| PostgreSQL | 16 | Base de données (local, test, cloud) |
| Node.js / npm | 22 / 10 | Outillage frontend |
| Angular (CLI, cœur, Material, CDK) | 22.2 | Application cliente |
| Vitest | 5 | Tests unitaires frontend |
| @fontsource/inter, @fontsource/jetbrains-mono, material-icons | 5.x / 1.x | Polices et icônes auto-hébergées |
| Docker / docker compose | 27 / v2 | Conteneurisation, environnement local complet |
| Mailpit | dernière | Capture des e-mails en développement |
| Git / GitHub | — | Versions, intégration continue, registre d'images |
| Mermaid CLI | 11 | Génération des diagrammes du mémoire |
| Éditeur | Visual Studio Code / IntelliJ IDEA Community | Développement |

## Organisation du dépôt Git

Le dépôt unique (*monorepo*) regroupe le code, l'infrastructure, les outils et la documentation, ce qui permet de livrer et de versionner l'ensemble de manière cohérente.

```
itsn/
├── backend/                 API Spring Boot (pom.xml, Dockerfile)
│   └── src/main/java/sn/senetech/itsn/
│       ├── config/          sécurité, propriétés, OpenAPI
│       ├── auth/            connexion JWT, utilisateur courant
│       ├── domain/          entités JPA et énumérations
│       ├── repository/      accès aux données (Spring Data)
│       ├── ticket/          cycle de vie, priorité, SLA, filtres
│       ├── intervention/    planning et comptes rendus
│       ├── catalog/         utilisateurs, sites, catégories, SLA, équipements
│       ├── kb/              base de connaissances
│       ├── dashboard/       indicateurs
│       ├── ai/              pipeline agentique de triage
│       ├── notification/    e-mails
│       ├── common/          erreurs, audit
│       └── seed/            données fictives de démonstration
│   └── src/main/resources/db/migration/   V1__schema_initial.sql
│   └── src/test/java/...    tests unitaires (*Test) et d'intégration (*IT)
├── frontend/                application Angular (Dockerfile, nginx.conf)
│   └── src/app/{core,layout,features/{auth,dashboard,tickets,interventions,equipment,kb,ai,admin}}
├── infra/render/            blueprint Render, script de tests post-déploiement
├── .github/workflows/       ci.yml (intégration continue), cd.yml (livraison continue)
├── tools/eval/              jeu d'évaluation et script d'évaluation de l'agent
├── tools/agent/             documentation du pipeline agentique
├── docs/                    mémoire (sources Markdown, diagrammes), guide de déploiement, preuves
├── docker-compose.yml       environnement local complet
└── .env.example             variables d'environnement (sans valeur secrète)
```

Les branches suivent le schéma `main` / `develop` / `feat/…` décrit au chapitre 3. L'historique des contributions est consultable dans le dépôt (`git log`) et sur GitHub ; un extrait figure en annexe. Le nombre de commits, de fichiers et de lignes par composant sera relevé au moment du dépôt final.

## Composants développés et interactions

### Backend : exemple du cycle de vie des tickets

Le cœur métier réside dans `TicketService`, qui orchestre les règles RM-01 à RM-06. L'extrait suivant montre la méthode privée de transition d'état, qui concentre la machine à états, les contrôles de cohérence et le calcul des indicateurs SLA :

```java
private void transition(Ticket t, TicketStatus target, User by) {
    TicketStatus from = t.getStatus();
    if (from != target && !from.canTransitionTo(target)) {
        throw new BusinessException("Transition interdite : " + from + " → " + target);
    }
    if (target == TicketStatus.IN_PROGRESS && t.getAssignee() == null) {
        throw new BusinessException("Un ticket doit être affecté avant d'être pris en charge");
    }
    Instant now = Instant.now();
    if (target == TicketStatus.IN_PROGRESS && t.getFirstResponseAt() == null) {
        t.setFirstResponseAt(now);
        t.setSlaResponseBreached(now.isAfter(t.getResponseDueAt()));
    }
    if (target == TicketStatus.RESOLVED) {
        t.setResolvedAt(now);
        t.setSlaResolutionBreached(now.isAfter(t.getResolutionDueAt()));
    }
    if (target == TicketStatus.IN_PROGRESS && from == TicketStatus.RESOLVED) {
        t.setResolvedAt(null);            // réouverture : la résolution précédente est annulée
        t.setResolutionSummary(null);
    }
    if (target == TicketStatus.CLOSED) t.setClosedAt(now);
    if (from != target) {
        history.save(TicketHistory.of(t, by, "status", from, target));
        t.setStatus(target);
    }
}
```

Les transitions autorisées sont déclarées dans l'énumération `TicketStatus` sous forme de table (`Map<TicketStatus, Set<TicketStatus>>`), ce qui rend la règle lisible et testable indépendamment du reste (test TU-02).

La **matrice de priorité** tient en quelques lignes, directement vérifiables contre le tableau du chapitre 4 :

```java
public static Priority compute(int impact, int urgency) {
    if (impact < 1 || impact > 3 || urgency < 1 || urgency > 3) {
        throw new IllegalArgumentException("Impact et urgence doivent être compris entre 1 et 3");
    }
    return switch (impact + urgency) {
        case 2 -> Priority.P1;
        case 3 -> Priority.P2;
        case 4 -> Priority.P3;
        default -> Priority.P4;
    };
}
```

La **génération des références** utilise un verrou pessimiste sur la ligne du compteur annuel, de sorte que deux créations simultanées ne puissent jamais produire la même référence :

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select s from TicketSequence s where s.year = :year")
Optional<TicketSequence> lockByYear(@Param("year") int year);
```

### Backend : agent de triage

L'agent est réalisé sans bibliothèque d'orchestration tierce, par un appel direct à l'API Messages d'Anthropic au moyen du client HTTP de Spring (`RestClient`). Ce choix, discuté au chapitre 8, rend chaque étape de la boucle visible et contrôlable. L'extrait ci-dessous montre le traitement d'un appel d'outil et la limite d'itérations :

```java
} else if ("tool_use".equals(block.type())) {
    toolCalls++;
    String result = toolCalls > ai.maxToolCalls()
        ? "Limite d'appels d'outils atteinte : réponds maintenant avec le JSON final."
        : tools.execute(block.name(), block.input());
    // Les résultats d'outils sont des données : on les encadre explicitement
    toolResults.add(Map.of("type", "tool_result", "tool_use_id", block.id(),
        "content", "<tool_data>\n" + result + "\n</tool_data>"));
}
```

La sortie finale est désérialisée dans un enregistrement `TriageOutput` puis **validée contre les référentiels** (catégorie existante, échelles 1 à 3, résumé non vide, confiance dans [0 ; 1]) ; le technicien proposé n'est retenu que s'il existe, est actif et a le rôle technicien. Toute sortie invalide produit une proposition `FAILED` sans effet sur le ticket.

### Frontend

L'application Angular suit une organisation par fonctionnalités, chargées paresseusement par le routeur (`loadComponent`), ce qui limite le bundle initial à environ 92 Ko compressés. Le cœur (`core/`) contient le service d'authentification (jeton en `localStorage`, profil exposé sous forme de signal), l'intercepteur HTTP qui ajoute le jeton et déconnecte l'utilisateur en cas de réponse 401, les gardes de route par rôle, la façade d'appels REST (`ApiService`) et les tubes d'affichage (libellés, temps restant avant échéance). Chaque écran est un composant autonome utilisant Angular Material.

Le gabarit (`ShellComponent`) filtre la navigation selon le rôle, mais **cette restriction d'interface n'est jamais considérée comme une protection** : toutes les règles sont réappliquées par le serveur (ENF-02). La fiche ticket calcule les actions affichées à partir de la liste `allowedTransitions` renvoyée par l'API, croisée avec le rôle courant, afin que l'utilisateur ne voie que les transitions qu'il a le droit d'effectuer.

### Charte graphique et composants d'interface

La charte du chapitre 4 est implémentée dans un fichier de styles unique (`styles.scss`) qui déclare les jetons de conception (couleurs, rayons, ombres, durées) sous forme de variables CSS, configure le thème Material 3 et aligne ses jetons système sur la charte. Des composants maison complètent Angular Material : logo, compteur animé (`countUp`), anneau de progression (`itsn-ring`), graphique en anneau (`itsn-donut`), état vide illustré (`itsn-empty`). Les polices et les icônes sont embarquées dans l'application (paquets `@fontsource/inter`, `@fontsource/jetbrains-mono`, `material-icons`), ce qui garantit un rendu identique sans accès à Internet au-delà du serveur de l'application.

### Captures d'écran des fonctionnalités importantes

Les captures ci-dessous ont été réalisées le 10 novembre 2026 sur l'application exécutée localement avec le jeu de données fictives (aucune donnée réelle).

![Écran de connexion : présentation du produit, formulaire et raccourcis vers les comptes de démonstration (capture du 10/11/2026)](../captures/01-connexion.png)

![Tableau de bord du responsable : indicateurs animés, anneaux de respect des SLA, répartition par priorité, charge des techniciens, catégories (capture du 10/11/2026)](../captures/02-tableau-de-bord.png)

![Liste des tickets : filtres, badges de priorité et d'état, échéance de résolution en temps restant, indicateur de dépassement (capture du 10/11/2026)](../captures/03-liste-tickets.png)

![Fiche d'un ticket : fil d'étapes du cycle de vie, engagements SLA, actions selon le rôle, onglets commentaires / interventions / triage IA / historique (capture du 10/11/2026)](../captures/04-fiche-ticket.png)

![File de revue des propositions de l'agent IA : état du pipeline (IA ou repli), budget du jour, comparaison valeurs actuelles / proposées, décision totale, partielle ou rejet (capture du 10/11/2026)](../captures/05-revue-ia.png)

![Planning hebdomadaire des interventions par technicien (capture du 10/11/2026)](../captures/06-planning.png)

![Formulaire de création d'un ticket avec priorité calculée en direct (capture du 10/11/2026)](../captures/09-nouveau-ticket.png)

![Administration : comptes, sites clients et politiques SLA (capture du 10/11/2026)](../captures/10-administration.png)

## Validation des entrées, gestion des erreurs, rôles et secrets

**Validation des entrées.** Toutes les requêtes entrantes sont des enregistrements Java annotés (`@NotBlank`, `@Size`, `@Min`, `@Max`, `@Email`, `@Pattern`). Une violation est interceptée par `GlobalExceptionHandler` et renvoyée en 400 avec un détail par champ (`fieldErrors`), que l'interface affiche tel quel. Les bornes correspondent aux contraintes `CHECK` de la base, de sorte qu'aucune donnée incohérente ne peut être persistée même en contournant l'API.

**Gestion des erreurs.** Les exceptions métier (`BusinessException` → 409), d'absence (`NotFoundException` → 404), d'authentification (401) et d'autorisation (403) produisent un corps uniforme sans trace technique ; les erreurs inattendues sont journalisées côté serveur avec leur pile d'appels et renvoyées en 500 avec un message générique. Les pannes de messagerie ou de l'API d'IA sont absorbées : le traitement du ticket n'en dépend jamais.

**Rôles et permissions.** Les autorisations sont déclarées par URL dans `SecurityConfig`, par méthode avec `@PreAuthorize` pour les règles statiques (par exemple la revue des propositions IA réservée au responsable et à l'administrateur) et dans les services pour les règles dépendant des données (propriété d'un ticket, affectation d'un technicien). Le tableau suivant résume la matrice d'accès.

Tableau : Matrice des droits par rôle

| Action | Demandeur | Technicien | Responsable | Administrateur |
|---|---|---|---|---|
| Créer un ticket (pour soi / pour un tiers) | Oui / Non | Oui / Oui | Oui / Oui | Oui / Oui |
| Voir les tickets | Les siens | Tous | Tous | Tous |
| Modifier un ticket | Le sien, tant qu'il est « Nouveau » | Oui (hors clôturé) | Oui | Oui |
| Affecter / réaffecter | Non | Non | Oui | Oui |
| Prendre en charge, mettre en attente, résoudre | Non | Ses tickets | Oui | Oui |
| Clôturer / rouvrir | Le sien (après résolution) | Non | Oui | Oui |
| Annuler | Le sien (avant prise en charge) | Non | Oui | Oui |
| Note interne | Non | Oui | Oui | Oui |
| Planifier une intervention | Non | Pour lui-même | Oui | Oui |
| Lancer le triage IA | Non | Oui | Oui | Oui |
| Valider une proposition IA | Non | Non | Oui | Oui |
| Tableau de bord | Non | Oui | Oui | Oui |
| Base de connaissances | Lecture (publiés) | Lecture, écriture | Lecture, écriture, suppression | Tout |
| Comptes, sites, catégories, SLA | Non | Non | Sites | Tout |
| Métriques Actuator | Non | Non | Non | Oui |

**Protection des secrets.** Aucun secret n'est présent dans le dépôt : le secret JWT, les identifiants de base de données et la clé de l'API d'IA sont lus dans des variables d'environnement par `AppProperties`, qui refuse de démarrer si le secret JWT fait moins de 32 caractères. Le fichier `.env` est ignoré par Git ; `.env.example` ne contient que des valeurs factices. La clé d'API n'apparaît dans aucun journal ni aucune réponse. En CI, l'analyse Trivy des secrets est bloquante.

## Difficultés rencontrées et solutions

Tableau : Difficultés rencontrées et solutions apportées

| Difficulté | Solution |
|---|---|
| Spring Boot 4 a réorganisé ses starters (le starter web devient `spring-boot-starter-webmvc`, Flyway et les tests ont des starters dédiés) et adopté Jackson 3 (`tools.jackson`) | Lecture du guide de migration officiel ; dépendances alignées ; code JSON limité aux conversions automatiques du cadriciel et à un `ObjectMapper` injecté |
| Angular 22 a retiré le module d'animations historique, ce qui faisait échouer la construction | Suppression du fournisseur `provideAnimationsAsync`, devenu inutile avec Angular Material 22 |
| La construction Angular en production tente d'incorporer les polices Google et échoue sans accès réseau | Désactivation de l'incorporation des polices (`optimization.fonts: false`) ; polices chargées à l'exécution |
| Génération concurrente des références de tickets | Verrou pessimiste sur le compteur annuel (`PESSIMISTIC_WRITE`) |
| Un appel interne à une méthode annotée `@Transactional` ou `@Async` n'est pas intercepté par le proxy Spring | Introduction d'un composant dédié (`SeedTransaction`) et envoi synchrone tolérant aux pannes pour les notifications |
| Le modèle d'IA peut renvoyer le JSON final entouré de texte ou de balises de code | Extraction robuste (`extractJson`) testée unitairement (TU-04) |
| Diagrammes Mermaid : certains caractères (parenthèses, balises) provoquent des erreurs d'analyse | Reformulation des libellés ; sources versionnées dans `docs/diagrams/` |
| Jackson 3 (Spring Boot 4) refuse une valeur `null` pour un champ `boolean` primitif : les clients qui omettent un champ facultatif (`internal`, `applyCategory`…) recevaient une erreur 500 | Champs facultatifs déclarés en `Boolean` avec valeur par défaut `false` ; erreur de corps illisible renvoyée en 400 explicite (`HttpMessageNotReadableException`) |
| L'indicateur de santé du serveur de messagerie rendait toute l'application « DOWN » (503) lorsque le SMTP était injoignable | Indicateur mail désactivé (`management.health.mail.enabled=false`) : une panne de messagerie ne doit pas être confondue avec une panne de l'application |
| Accès réseau restreint de l'environnement d'assistance IA (dépôt Maven d'abord inaccessible) | Autorisation du domaine Maven Central ; compilation et tests exécutés le 10/11/2026 (40 tests réussis) |
| Google Fonts injoignable dans certains réseaux : icônes et polices absentes | Polices et icônes auto-hébergées dans le bundle Angular |
| Avec `open-in-view` désactivé (choix de performance), certains contrôleurs de lecture accédaient à des associations paresseuses hors transaction (`LazyInitializationException`), ce que seuls les tests d'interface ont révélé | Contrôleurs de lecture rendus transactionnels (`@Transactional(readOnly = true)`) ; test d'intégration TI-13 ajouté |

## Réalisations personnelles, composants tiers et éléments assistés par IA

Conformément au cadre d'usage de l'IA, les contributions sont distinguées comme suit.

- **Réalisations personnelles** : formulation du problème, étude de cas, référentiel d'exigences, choix d'architecture et choix techniques, règles métier, conception du pipeline agentique et de ses garde-fous, relecture, exécution et correction de l'ensemble du code et des tests, déploiement, rédaction et validation du mémoire.
- **Composants tiers** (licences libres ou gratuites, utilisés sans modification) : Spring Boot et ses modules, Hibernate, Flyway, PostgreSQL, springdoc, Angular et Angular Material, Vitest, Docker, nginx, Mailpit, Trivy, Mermaid.
- **Éléments assistés par IA** : squelette du projet, première version d'une large part du code Java et TypeScript, des tests, des diagrammes Mermaid, des fichiers de configuration (Docker, GitHub Actions, Render) et des brouillons de rédaction, produits avec l'assistant Claude (Anthropic) à partir des spécifications établies par l'étudiant, puis **relus, exécutés, testés et adaptés**. Le détail des usages (dates, objets, vérifications) figure dans le journal en annexe. L'étudiant assume la responsabilité de l'ensemble et est en mesure d'expliquer chaque composant.

## Fonctionnalités réalisées et limites connues

Tableau : État de réalisation des fonctionnalités

| Fonctionnalité | État | Limites connues |
|---|---|---|
| Authentification, rôles, comptes | Réalisée | Pas de réinitialisation de mot de passe par e-mail ; pas d'authentification à deux facteurs |
| Tickets : création, priorité, SLA, cycle de vie, commentaires, historique | Réalisée | Les délais SLA sont calculés en heures calendaires (pas de calendrier d'astreinte ni de jours fériés) |
| Affectation et notifications | Réalisée | Notifications e-mail uniquement, désactivées par défaut |
| Planning des interventions | Réalisée | Vue liste par semaine, pas de vue calendrier graphique ni de synchronisation avec un agenda externe |
| Inventaire | Réalisée | Pas d'import en masse ni de découverte automatique |
| Base de connaissances | Réalisée | Recherche par correspondance de texte simple, pas de recherche sémantique |
| Tableau de bord | Réalisée | Indicateurs calculés à la demande ; pas d'export PDF |
| Agent de triage IA et revue humaine | Réalisée (évaluation au chapitre 7) | Dépend d'un service externe ; repli heuristique volontairement rudimentaire |
| Charte graphique, animations, composants de visualisation | Réalisée | Pas de mode sombre ; accessibilité clavier non auditée formellement |
| CI/CD et déploiement cloud | Réalisée (état précis au chapitre 8) | Offres gratuites : mise en veille, pas de garantie de disponibilité |

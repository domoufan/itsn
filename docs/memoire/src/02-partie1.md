<<<pagebreak>>>
# Partie 1 — Cadre organisationnel et analyse des besoins

<<<pagebreak>>>
# Chapitre 1 — Cadre du projet et contexte d'étude

## Section 1 — Cadre du projet

### Présentation de l'organisation d'étude

Le projet est construit autour d'une **étude de cas fictive**, SénéTech Services SARL, entreprise de services du numérique installée à Dakar (quartier Sacré-Cœur). L'organisation a été définie pour être représentative d'une ESN sénégalaise de taille intermédiaire : quarante-deux salariés, dont un service support composé d'un responsable et de huit techniciens (cinq techniciens de terrain intervenant sur les sites clients, trois techniciens assurant l'assistance à distance). L'entreprise assure, dans le cadre de contrats de maintenance annuels, l'exploitation du parc informatique d'une quinzaine de PME clientes : agences bancaires, unité agro-industrielle, clinique privée, compagnie d'assurance, cabinets. Les prestations couvrent le dépannage des postes et périphériques, le réseau local et le Wi-Fi, la messagerie, les serveurs de fichiers et de sauvegarde, la gestion des comptes et des accès, ainsi que la sécurité du poste de travail.

Les sites clients, les équipements, les personnes et les tickets présentés dans ce mémoire et dans le jeu de données de démonstration sont entièrement fictifs. Les noms de clients portent la mention « (fictif) » dans l'application et aucune donnée réelle n'a été utilisée. Ce choix répond au cadre de protection des données rappelé par l'établissement, et il permet de décrire sans réserve un périmètre complet.

### Acteurs

Quatre catégories d'acteurs interviennent dans la gestion des interventions :

- **le demandeur** : collaborateur d'un client qui signale un incident ou formule une demande (installation, accès, matériel), suit son avancement, dialogue avec le support, confirme la résolution et évalue le service ;
- **le responsable support** : qualifie les demandes, les priorise, les affecte aux techniciens, surveille le respect des engagements, arbitre les cas litigieux et rend compte à la direction et aux clients ;
- **le technicien** : prend en charge les tickets qui lui sont affectés, planifie et réalise les interventions, documente les diagnostics et les solutions, alimente la base de connaissances ;
- **l'administrateur** : gère les comptes, les sites clients, les référentiels (catégories, politiques SLA) et la configuration de la plateforme.

À ces acteurs humains s'ajoutent deux systèmes externes : le service de messagerie (notifications) et le fournisseur du modèle d'intelligence artificielle utilisé pour le triage.

### Mission, bénéficiaires et contraintes

La mission confiée au projet est de doter le service support d'un outil unique permettant d'enregistrer, qualifier, affecter, planifier, suivre et clôturer chaque intervention, de mesurer le respect des délais contractuels et de capitaliser les solutions. Les bénéficiaires directs sont le responsable support (pilotage), les techniciens (organisation du travail) et les demandeurs (visibilité, réactivité) ; les bénéficiaires indirects sont la direction de l'ESN (rapports d'activité, facturation facilitée) et les dirigeants des entreprises clientes (qualité de service démontrée).

Les contraintes retenues pour l'étude de cas sont celles d'une PME : budget d'exploitation très limité (hébergement sur offres gratuites ou à faible coût), absence d'équipe d'exploitation dédiée (la solution doit être simple à déployer et à maintenir), connexion Internet parfois instable sur les sites clients (interface légère, fonctionnement de base sans dépendance à l'IA), utilisateurs peu formés (ergonomie, langue française) et respect de la confidentialité des données des clients (aucune donnée nominative transmise à un service tiers sans nécessité).

### Responsabilités au sein du projet

Le projet a été réalisé individuellement par Mouhamed Diop, étudiant en Licence professionnelle Génie logiciel à l'École 221. L'encadreur a validé le cadrage, le périmètre et les principaux choix techniques lors de points réguliers consignés au chapitre 3. L'assistance d'outils d'intelligence artificielle générative, utilisée pour l'exploration de pistes, la génération de code et de tests soumis à relecture, l'amélioration de la langue et la traduction du résumé, est détaillée dans la déclaration et le journal placés en annexe.

## Section 2 — Contexte d'étude

### Pratiques existantes

Dans l'organisation d'étude, la gestion des interventions repose sur quatre canaux non intégrés, représentatifs de ce que l'on observe dans de nombreuses petites structures :

1. **le téléphone et la messagerie instantanée** : les clients appellent ou écrivent directement au technicien qu'ils connaissent ; la demande n'est tracée nulle part si le technicien ne la reporte pas lui-même ;
2. **une boîte e-mail partagée** (support@) relevée par le responsable, sans accusé de réception automatique ni suivi d'état ;
3. **un tableur partagé** dans lequel le responsable reporte, quand il en a le temps, la demande, le client, le technicien et une colonne « fait / pas fait » ;
4. **un cahier d'interventions** papier, signé par le client à l'issue de chaque déplacement, qui sert de justificatif pour la facturation.

Tableau : Synthèse des pratiques existantes et de leurs limites

| Pratique | Ce qu'elle permet | Limites observées |
|---|---|---|
| Appels et messagerie instantanée | Réactivité perçue par le client | Aucune trace ; dépendance à la mémoire du technicien ; sollicitation directe contournant la priorisation |
| Boîte e-mail partagée | Trace écrite initiale | Pas d'état, pas de responsable désigné, doublons, demandes oubliées dans le fil |
| Tableur partagé | Vue d'ensemble approximative | Saisie manuelle et tardive, pas d'horodatage fiable, conflits d'édition, aucune alerte |
| Cahier d'interventions papier | Preuve de passage signée | Non consultable à distance, pas de lien avec le matériel, exploitation statistique impossible |

### Limites et enjeux métier

Ces pratiques produisent des effets dont le coût n'est généralement mesuré qu'au moment où un client important se plaint. On peut les regrouper en cinq enjeux :

- **Traçabilité** : impossibilité de prouver qu'une demande a été reçue et à quelle date, de reconstituer l'historique d'un équipement ou de répondre à la question « qui s'en occupe ? ».
- **Engagements de service** : les contrats de maintenance promettent des délais (par exemple intervention sous quatre heures pour un incident bloquant) qui ne sont ni calculés ni surveillés ; les dépassements ne sont connus que par la réclamation.
- **Priorisation** : en l'absence de règle explicite, le traitement se fait « au plus bruyant » ou « au plus proche », et non selon l'impact réel sur l'activité du client.
- **Charge et planification** : le responsable ne connaît pas la charge effective de chaque technicien ni les créneaux déjà réservés ; les déplacements se chevauchent ou se font à vide.
- **Capitalisation** : les mêmes pannes sont diagnostiquées plusieurs fois par des techniciens différents ; le savoir-faire reste individuel et disparaît avec les départs.

L'enjeu commercial est également réel : une ESN capable de produire chaque mois un rapport fiable (nombre de tickets, délais, taux de respect des engagements, satisfaction) dispose d'un argument décisif lors du renouvellement des contrats.

### État de l'art : référentiels et solutions existantes

**Référentiels.** ITIL, dans sa quatrième édition publiée par AXELOS en 2019, constitue le référentiel de bonnes pratiques le plus répandu en gestion des services informatiques. Il définit notamment les pratiques de *gestion des incidents* (rétablir le service normal au plus vite), de *gestion des demandes de service*, de *gestion des problèmes* (traiter les causes), de *gestion des changements* et de *gestion des niveaux de service* (SLA). Deux concepts d'ITIL structurent directement ce projet : la **matrice de priorisation**, qui dérive la priorité d'un ticket du croisement de son impact (étendue de la perturbation) et de son urgence (vitesse à laquelle la perturbation devient inacceptable), et les **accords de niveau de service**, qui fixent des délais de prise en compte et de résolution par niveau de priorité. La norme internationale ISO/IEC 20000-1:2018 formalise, pour sa part, les exigences d'un système de management des services et reprend ces mêmes processus. Ces référentiels fournissent le vocabulaire et les règles métier de la plateforme ; ils ne prescrivent aucun outil.

**Solutions existantes.** Le marché propose de nombreux outils ITSM. Quatre solutions représentatives ont été étudiées à partir de leur documentation officielle, afin de situer l'apport du projet.

Tableau : Comparaison de solutions ITSM existantes (sources : documentation officielle des éditeurs, consultée en novembre 2026)

| Critère | GLPI (Teclib') | Zammad | osTicket | Freshservice (Freshworks) |
|---|---|---|---|---|
| Licence / modèle | Open source (GPL), version 11 publiée le 30 septembre 2025 ; support payant optionnel | Open source (AGPL) ou SaaS payant | Open source (GPL) ou SaaS payant | SaaS propriétaire, tarif par agent et par mois (à partir de 19 USD par agent en facturation annuelle) |
| Technologie | PHP / MySQL-MariaDB | Ruby on Rails / PostgreSQL, Elasticsearch | PHP / MySQL | Hébergé par l'éditeur |
| Gestion des incidents et demandes | Complète | Complète (orientée service client) | Basique | Complète |
| SLA et priorisation | Oui, règles paramétrables | Oui | Limité (plugins) | Oui, selon le plan |
| Inventaire / CMDB | Très complet (origine : gestion de parc) | Non natif | Non | Oui (selon le plan, par lot d'actifs) |
| Planification d'interventions terrain | Partielle (tâches planifiées) | Non | Non | Partielle |
| Assistance par IA | Plugins tiers | Fonctions IA récentes (SaaS) | Non | Oui, en option payante (add-on par agent) |
| Complexité de déploiement pour une PME | Élevée (paramétrage riche) | Moyenne à élevée (Elasticsearch) | Faible | Nulle (SaaS) mais coût récurrent et données hébergées hors du pays |

**Positionnement du projet.** Ces solutions couvrent, pour la plupart, davantage de processus que le présent projet. Trois raisons justifient néanmoins la conception d'une plateforme dédiée dans le cadre de ce mémoire. Premièrement, l'**adéquation au contexte** : GLPI ou Zammad imposent un paramétrage et une exploitation que l'ESN d'étude n'a pas les moyens d'assumer, tandis que le coût récurrent d'un SaaS par agent (huit techniciens et un responsable, soit neuf agents, à 19 USD par agent et par mois au minimum) et l'hébergement des données clients hors du pays posent des questions de coût et de souveraineté. Deuxièmement, la **spécificité métier** : la planification d'interventions sur sites clients multiples, avec détection des conflits de créneaux et compte rendu terrain, est peu ou mal couverte par les outils généralistes. Troisièmement, l'**objectif pédagogique** : le canevas du mémoire attend la conception, la réalisation, le test et le déploiement d'une solution, ainsi que l'intégration d'un pipeline agentique ; l'étude de ces outils sert ici de référence fonctionnelle — c'est d'eux que sont tirées les fonctions attendues d'un ITSM — et non de solution à paramétrer.

L'apport du projet se situe donc dans la combinaison, au sein d'une application légère et déployable sans équipe d'exploitation, des fonctions essentielles d'un centre de services (tickets, SLA, affectation, planning, inventaire, base de connaissances, indicateurs) et d'un **agent de triage par IA conçu dès l'origine pour être borné, auditable et subordonné à la décision humaine**, ce que les solutions étudiées n'offrent qu'en option payante ou sous forme de plugin.

### Périmètre du projet et justification du problème à résoudre

Le périmètre retenu, précisé en introduction, couvre les processus ITIL de gestion des incidents et des demandes, ainsi que les éléments de gestion des actifs et des connaissances nécessaires à leur traitement. Il exclut la gestion des problèmes et des changements, le catalogue de services et la facturation, qui constituent des extensions naturelles. Le problème à résoudre est justifié par la convergence de trois constats : les pratiques existantes ne garantissent ni la traçabilité ni le respect mesurable des engagements ; les solutions du marché sont soit surdimensionnées pour une PME sans équipe d'exploitation, soit coûteuses et hébergées hors du pays ; enfin, les capacités récentes des modèles de langage ouvrent une voie d'assistance à la qualification des demandes qui n'a de valeur que si elle est encadrée.

<<<pagebreak>>>
# Chapitre 2 — Analyse des besoins et spécifications

## Méthode de recueil des besoins

L'organisation d'étude étant fictive, **aucun entretien de terrain ni questionnaire n'a été réalisé** ; le présent mémoire ne prétend donc pas rapporter des observations empiriques. Les besoins ont été établis par trois techniques complémentaires, dont les limites sont indiquées :

1. **Analyse documentaire** : lecture des pratiques ITIL 4 relatives aux incidents, aux demandes et aux niveaux de service, de la norme ISO/IEC 20000-1 et de la documentation fonctionnelle des quatre solutions comparées au chapitre 1. Cette source fournit les fonctions attendues d'un centre de services et les règles métier (matrice de priorité, échéances SLA, cycle de vie d'un ticket). *Limite* : elle décrit un idéal normatif qui ne reflète pas nécessairement les arbitrages d'une PME donnée.
2. **Construction de scénarios d'étude de cas** : rédaction de vingt-deux scénarios réalistes d'incidents et de demandes (panne d'imprimante au guichet, serveur de fichiers inaccessible, suspicion d'hameçonnage, création de compte, demande de VPN…), répartis entre sites, catégories et niveaux de gravité, et joués de bout en bout pour dériver les besoins de chaque acteur. Ces scénarios constituent le jeu de données de démonstration. *Limite* : ils reflètent la représentation que l'auteur se fait du métier, nourrie par la documentation et par des échanges informels, et non des cas réels.
3. **Analyse des contraintes du canevas et de l'encadrement** : exigences explicites de l'établissement (déploiement CI/CD et cloud, pipeline agentique, sécurité, traçabilité) et validations de l'encadreur, qui ont conduit à retenir ou à écarter certaines fonctions.

Aucune donnée personnelle n'a été collectée. Les besoins ainsi établis sont présentés comme des **hypothèses de conception raisonnables**, à confronter à un terrain réel dans une phase ultérieure, ce qui figure parmi les perspectives du travail.

## Utilisateurs et cas d'usage

Les quatre rôles identifiés au chapitre 1 ont été précisés sous forme de profils, avec les objectifs qu'ils poursuivent et le niveau d'accès qui en découle.

Tableau : Profils utilisateurs et objectifs

| Rôle (code) | Profil type | Objectifs principaux | Accès |
|---|---|---|---|
| Demandeur (REQUESTER) | Employé d'un client, peu technique, parfois sur mobile | Signaler vite et simplement ; savoir où en est sa demande ; être prévenu ; confirmer ou contester la résolution | Ses propres tickets, les articles publiés |
| Technicien (TECHNICIAN) | Intervenant terrain ou à distance, mobile entre sites | Voir ses tickets et ses créneaux ; diagnostiquer ; documenter ; retrouver une solution connue | Tous les tickets en lecture, actions sur les tickets affectés, planning, inventaire, base de connaissances |
| Responsable support (MANAGER) | Pilote de l'équipe, interlocuteur des clients | Ne rien laisser passer ; affecter au bon technicien ; tenir les délais ; rendre compte ; valider l'aide de l'IA | Tout sauf l'administration des comptes et des SLA |
| Administrateur (ADMIN) | Responsable technique de l'ESN | Gérer comptes, sites, catégories, politiques SLA ; superviser | Tout |

Les cas d'utilisation principaux sont représentés au chapitre 4 (figure du diagramme de cas d'utilisation). Les plus structurants sont : créer un ticket ; affecter un ticket ; prendre en charge, mettre en attente, résoudre ; planifier une intervention et en rendre compte ; lancer le triage IA et valider la proposition ; consulter le tableau de bord.

## Exigences fonctionnelles

Chaque exigence reçoit un identifiant, une priorité selon l'échelle MoSCoW simplifiée (P1 = indispensable au prototype, P2 = importante, P3 = souhaitable) et un critère d'acceptation mesurable. La dernière colonne indique les tests qui la vérifient (chapitre 7) ; la matrice complète figure en annexe.

Tableau : Exigences fonctionnelles

| Id | Exigence | Priorité | Critère d'acceptation | Tests |
|---|---|---|---|---|
| EF-01 | Authentifier les utilisateurs et gérer des comptes dotés d'un des quatre rôles | P1 | Connexion par e-mail et mot de passe ; un compte désactivé ne peut plus se connecter ; l'administrateur crée, modifie et désactive les comptes | TI-01, TM-01 |
| EF-02 | Créer un ticket d'incident ou de demande avec une référence unique lisible | P1 | Référence de la forme INC-AAAA-NNNNN ou REQ-AAAA-NNNNN, unique y compris en création simultanée ; titre de 5 à 160 caractères, description de 10 à 5 000 caractères | TI-02, TU-02 |
| EF-03 | Calculer automatiquement la priorité à partir de l'impact et de l'urgence | P1 | Les neuf combinaisons impact × urgence produisent la priorité attendue par la matrice ITIL ; modification de l'impact ou de l'urgence ⇒ recalcul | TU-01, TI-02 |
| EF-04 | Contrôler le cycle de vie du ticket par une machine à états | P1 | Seules les transitions autorisées sont acceptées ; une transition interdite renvoie une erreur 409 explicite ; la résolution exige un résumé | TU-02, TI-06 |
| EF-05 | Calculer les échéances SLA (prise en compte, résolution) et détecter les dépassements | P1 | Échéances calculées à la création à partir de la politique de la priorité ; indicateurs de dépassement positionnés à la première réponse, à la résolution et par une tâche périodique | TU-03, TI-05, TI-06b |
| EF-06 | Permettre à l'administrateur de paramétrer les politiques SLA par priorité | P2 | Modification des délais par priorité ; refus si le délai de résolution est inférieur au délai de prise en compte | TM-02 |
| EF-07 | Affecter ou réaffecter un ticket à un technicien actif | P1 | Réservé au responsable et à l'administrateur ; passage automatique de NEW à ASSIGNED ; notification du technicien ; historique tracé | TI-03, TI-04 |
| EF-08 | Planifier une intervention sur site ou à distance sans conflit de créneau | P2 | Refus (409) si le technicien a déjà une intervention chevauchant le créneau ; un technicien ne planifie que pour lui-même | TM-03 |
| EF-09 | Saisir un compte rendu d'intervention | P2 | Compte rendu obligatoire pour clore une intervention ; horaires réels cohérents | TM-03 |
| EF-10 | Fournir un tableau de bord d'indicateurs | P2 | Tickets ouverts, hors délai, créés/résolus sur la période, taux de respect SLA, MTTR, satisfaction, répartition par priorité et catégorie, charge par technicien | TI-11, TM-04 |
| EF-11 | Gérer un inventaire des équipements par site, rattachable aux tickets | P2 | Numéro d'inventaire unique ; filtrage par site ; alerte visuelle de garantie expirée | TM-05 |
| EF-12 | Proposer, par un agent IA, une catégorie, une priorité (impact, urgence), un technicien et des articles liés | P2 | Proposition structurée et validée contre les référentiels ; métriques (jetons, latence, coût) enregistrées ; sortie invalide ⇒ proposition FAILED sans effet | TU-04, TI-21, TE-01 |
| EF-13 | Soumettre toute proposition IA à une validation humaine (accepter, appliquer une partie, rejeter) | P1 | Aucune modification du ticket avant décision ; décision réservée au responsable ; une seule proposition en attente par ticket ; décision et auteur tracés | TI-21, TI-22, TI-23, TI-24 |
| EF-14 | Offrir une base de connaissances consultable et alimentée par le personnel | P3 | Recherche plein texte simple sur titre, contenu, mots clés ; articles non publiés invisibles des demandeurs | TM-06 |
| EF-15 | Permettre des commentaires publics et des notes internes | P2 | Les notes internes ne sont jamais renvoyées à un demandeur ; un demandeur ne peut pas créer de note interne | TI-07 |
| EF-16 | Historiser toutes les modifications significatives d'un ticket | P2 | Chaque changement d'état, d'affectation, de catégorie, d'impact, d'urgence ou de priorité produit une entrée datée avec auteur, ancienne et nouvelle valeur | TI-04 |
| EF-17 | Notifier par e-mail les événements clés | P3 | Création (demandeur), affectation (technicien), résolution (demandeur), dépassement SLA (technicien) ; une panne de messagerie ne bloque jamais le traitement | TM-07 |
| EF-18 | Recueillir une évaluation de satisfaction du demandeur | P3 | Note de 1 à 5, possible uniquement par le demandeur après résolution | TI-07 |

## Exigences non fonctionnelles

Tableau : Exigences non fonctionnelles

| Id | Exigence | Priorité | Critère d'acceptation | Tests |
|---|---|---|---|---|
| ENF-01 | Sécurité : l'API refuse toute requête non authentifiée | P1 | Code 401 sans jeton ou avec jeton invalide ou expiré sur toutes les routes /api sauf la connexion | TI-10, TS-01 |
| ENF-02 | Autorisations : les règles par rôle sont appliquées côté serveur | P1 | Code 403 pour toute action hors du rôle, indépendamment de l'interface ; un demandeur ne voit que ses tickets | TI-03, TI-08, TI-23 |
| ENF-03 | Mots de passe protégés | P1 | Hachage BCrypt (coût 12) ; politique de complexité (10 caractères, majuscule, minuscule, chiffre) au changement | TS-02 |
| ENF-04 | Validation des entrées et protection contre les injections | P1 | Données invalides ⇒ 400 avec détail par champ ; requêtes SQL paramétrées (JPA) ; absence de concaténation de SQL | TI-09, TS-03 |
| ENF-05 | Secrets hors du dépôt | P1 | Aucun secret dans Git ni dans les journaux ; configuration par variables d'environnement ; analyse automatique des secrets en CI | TS-04 |
| ENF-06 | Journalisation des actions sensibles | P2 | Connexions, échecs, changements de rôle, affectations, exécutions et décisions IA tracés avec auteur et date | TS-05 |
| ENF-07 | Fonctionnement dégradé sans IA et maîtrise du coût | P1 | Toutes les fonctions hors triage opèrent sans clé d'API ; le triage bascule sur une simulation locale marquée comme telle ; budget journalier plafonné | TU-05, TI-20, TI-21 |
| ENF-08 | Résistance aux détournements d'instructions (injection de prompt) | P2 | Le contenu du ticket est transmis comme donnée ; la sortie est validée contre les référentiels ; aucun outil d'écriture n'est exposé ; cas adverse du jeu d'évaluation non détourné | TE-01, TS-06 |
| ENF-09 | Performance | P2 | Liste de 20 tickets filtrée en moins de 500 ms (95e centile) sur le jeu de démonstration, mesuré localement | TP-01 |
| ENF-10 | Disponibilité et supervision | P2 | Sondes de vivacité et de disponibilité exposées ; redémarrage automatique par la plateforme d'hébergement ; limites de l'offre gratuite documentées | TD-02 |
| ENF-11 | Sauvegarde et restauration | P2 | Procédure documentée ; test de restauration réalisé et daté | TD-03 |
| ENF-12 | Reproductibilité | P2 | Déploiement local complet en une commande (docker compose) ; guide d'installation ; migrations de schéma versionnées | TD-01 |
| ENF-13 | Qualité de livraison | P1 | Pipeline CI bloquant en cas d'échec de test ou de vulnérabilité critique ; artefacts versionnés ; promotion du même artefact | TD-02 |
| ENF-14 | Ergonomie et accessibilité de base | P3 | Interface en français ; messages d'erreur lisibles ; priorités et états signalés par couleur et par texte (pas uniquement par couleur) | TM-08 |

## Périmètre retenu pour le prototype

Toutes les exigences de priorité P1 et P2 ont été retenues pour le prototype. Parmi les exigences P3, la base de connaissances (EF-14), les notifications (EF-17), l'évaluation de satisfaction (EF-18) et l'ergonomie de base (ENF-14) ont également été réalisées, car elles conditionnent le réalisme de la démonstration et alimentent l'agent de triage (articles liés). Le tableau suivant récapitule les fonctions écartées ou reportées, afin que le périmètre livré soit explicite.

Tableau : Fonctions hors périmètre du prototype

| Fonction | Décision | Motif |
|---|---|---|
| Gestion des problèmes et des changements (ITIL) | Reportée | Processus distincts nécessitant leur propre cycle de vie et un comité de validation |
| Catalogue de services et formulaires par type de demande | Reportée | Valeur réelle mais effort de paramétrage important ; les demandes sont traitées par un formulaire générique |
| Application mobile native | Écartée | L'interface web réactive répond au besoin ; coût de développement et de publication élevé |
| Intégration annuaire d'entreprise (LDAP, SSO) | Reportée | Dépend du client ; l'authentification locale suffit au prototype |
| Facturation des interventions | Écartée | Hors du champ ITSM ; export des données envisagé en perspective |
| Supervision automatique des équipements | Écartée | Nécessite des agents installés sur les postes clients |
| Application automatique des propositions IA sans revue | Écartée volontairement | Choix de conception : la décision reste humaine (EF-13) |

Le lien entre ces exigences et les composants réalisés est établi au chapitre 4 (correspondance exigences → composants) et au chapitre 7 (plan de tests), puis consolidé dans la matrice de traçabilité en annexe.

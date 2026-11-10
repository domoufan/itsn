<<<body>>>
# Introduction générale

## Contexte et intérêt du sujet

Dans toute organisation, le système d'information est devenu l'outil de travail principal : postes, réseaux, imprimantes, messagerie, applications métiers conditionnent la capacité à produire, vendre ou soigner. Lorsqu'un de ces éléments tombe en panne, le temps écoulé entre le signalement et la résolution est directement un coût : productivité perdue, clients mécontents, parfois arrêt complet d'une activité. La gestion de ces événements — incidents, demandes de service, interventions planifiées — constitue le cœur de ce que la littérature professionnelle appelle la gestion des services informatiques, ou ITSM (*IT Service Management*).

Les grands groupes disposent depuis longtemps d'outils dédiés (ServiceNow, Jira Service Management, GLPI) et de référentiels de bonnes pratiques, au premier rang desquels ITIL, dont la quatrième édition (2019) structure la gestion des incidents, des demandes, des problèmes et des changements autour d'une chaîne de valeur orientée vers le service rendu. À l'autre extrémité du spectre, les petites et moyennes entreprises de services du numérique (ESN) qui assurent, pour le compte de PME clientes, la maintenance de parcs informatiques, fonctionnent encore très souvent de manière informelle : appels téléphoniques, messages WhatsApp, cahier d'interventions, tableur partagé. Ce mode de fonctionnement, suffisant tant que l'activité reste réduite, devient un frein dès que le nombre de clients, de techniciens et de demandes simultanées augmente.

Le sujet s'inscrit par ailleurs dans un contexte technologique en mutation : les grands modèles de langage (LLM) rendent aujourd'hui possible l'automatisation partielle de tâches de qualification jusque-là manuelles — comprendre une demande rédigée en langage naturel, la classer, estimer sa gravité, suggérer un intervenant. L'intégration de ces capacités dans un outil métier pose cependant des questions de fiabilité, de coût, de sécurité et de contrôle humain qui méritent d'être traitées avec méthode plutôt que par enthousiasme.

## Problème observé

L'étude de cas retenue pour ce travail est celle de SénéTech Services, ESN fictive implantée à Dakar, dont le service support (un responsable et huit techniciens) gère les interventions techniques d'une quinzaine de PME clientes sous contrat de maintenance. Le caractère fictif de cette organisation est assumé : il permet de décrire un périmètre complet et réaliste sans exposer les données d'une entreprise réelle. Les pratiques décrites et les limites identifiées sont toutefois représentatives de celles rapportées dans la littérature professionnelle et dans la documentation des outils ITSM destinés aux PME, comme le montre le chapitre 1.

Dans cette organisation, le problème observé peut se résumer ainsi : **l'absence d'un outil centralisé rend impossible de garantir qu'aucune demande n'est perdue, de mesurer le respect des délais promis aux clients, de connaître la charge réelle de chaque technicien et de capitaliser les solutions apportées**. Les symptômes sont connus des praticiens : demandes oubliées faute de trace écrite, priorisation « au plus bruyant », techniciens sollicités directement par les clients, impossibilité de produire un rapport d'activité fiable, résolution répétée des mêmes pannes faute de base de connaissances.

## Question centrale, objectif général et objectifs spécifiques

La question centrale de ce mémoire est la suivante :

> **Comment concevoir et mettre en œuvre une plateforme de gestion des interventions techniques, alignée sur les bonnes pratiques ITSM et adaptée aux moyens d'une PME, qui garantisse la traçabilité de chaque demande, le respect mesurable des engagements de service et l'aide à la décision, tout en intégrant l'intelligence artificielle de manière contrôlée et économiquement soutenable ?**

L'**objectif général** est de concevoir, réaliser, tester et déployer une application web complète — baptisée ITSN — couvrant le cycle de vie des tickets d'incident et de demande, la planification des interventions, l'inventaire des équipements, la base de connaissances et le pilotage par indicateurs, complétée par un agent d'intelligence artificielle de triage soumis à validation humaine.

Les **objectifs spécifiques**, formulés de manière évaluable, sont les suivants :

1. **OS1 – Formaliser les besoins** : produire un référentiel d'exigences fonctionnelles et non fonctionnelles identifiées, priorisées et dotées de critères d'acceptation mesurables, à partir de l'analyse documentaire et de l'étude comparative de solutions existantes (critère : au moins trente exigences tracées jusqu'aux tests).
2. **OS2 – Concevoir une solution cohérente** : modéliser le système (contexte, cas d'utilisation, classes, séquences, états) et justifier une architecture et des choix techniques par comparaison d'alternatives (critère : correspondance explicite exigences → composants).
3. **OS3 – Réaliser un prototype opérationnel** : implémenter les exigences de priorité haute dans une application déployable en une commande, avec un dépôt Git organisé et un jeu de données fictives (critère : 100 % des exigences P1 réalisées et testées).
4. **OS4 – Garantir la qualité et la sécurité** : couvrir les exigences par des tests automatisés et manuels documentés, avec preuves datées, et appliquer les principes de sécurité du Top 10 OWASP applicables (critère : taux de réussite des tests et bilan de conformité).
5. **OS5 – Industrialiser la livraison** : mettre en place une chaîne CI/CD reproductible, de la validation du code jusqu'au déploiement cloud, avec promotion du même artefact et procédure de retour arrière (critère : exécution prouvée du pipeline et identifiant de version déployée).
6. **OS6 – Intégrer l'IA de façon maîtrisée** : concevoir, réaliser et évaluer un agent de triage borné (outils en lecture seule, délai, budget, repli sans IA, validation humaine) et mesurer sa qualité, sa latence et son coût sur un jeu de cas représentatifs (critère : exactitude de catégorisation et de priorisation mesurées, taux de détournement nul).

## Périmètre

Le périmètre retenu couvre la gestion des incidents et des demandes de service, la planification des interventions, un inventaire simplifié des équipements (CMDB), une base de connaissances, un tableau de bord et le triage assisté par IA. Sont explicitement **exclus** de ce travail : la gestion des problèmes et des changements au sens ITIL, le catalogue de services, la facturation des interventions, l'application mobile native, la supervision automatique des équipements et l'intégration avec un annuaire d'entreprise (LDAP). Ces éléments sont discutés en perspectives.

## Démarche

La démarche suit les quatre temps de tout projet d'ingénierie logicielle : analyse des besoins, conception, réalisation et validation. Elle distingue la **méthode de recueil et d'analyse** (analyse documentaire, étude comparative, scénarios d'étude de cas) du **mode de gestion du projet** (itératif, inspiré de Scrum, avec des incréments hebdomadaires et un suivi dans le dépôt Git). Les résultats attendus — une plateforme fonctionnelle, déployée, testée et documentée — sont distingués, dans chaque chapitre, des résultats effectivement obtenus à la date de rédaction, en signalant ce qui relève d'une démonstration locale, d'une simulation ou d'une automatisation seulement proposée.

## Plan du mémoire

Le mémoire est organisé en trois parties et huit chapitres. La **première partie** pose le cadre organisationnel et l'analyse des besoins : le chapitre 1 présente l'étude de cas, les pratiques existantes et l'état de l'art des solutions ITSM ; le chapitre 2 expose la méthode de recueil et le référentiel d'exigences. La **deuxième partie** traite du pilotage et de la conception : le chapitre 3 justifie la méthode de gestion de projet et présente le planning ; le chapitre 4 détaille la conception fonctionnelle et technique ; le chapitre 5 chiffre le projet. La **troisième partie** couvre la mise en œuvre : le chapitre 6 décrit la réalisation, le chapitre 7 les tests et la validation, le chapitre 8 le déploiement CI/CD, l'infrastructure cloud et le pipeline agentique. La conclusion générale répond à la problématique et ouvre des perspectives. Les annexes rassemblent la déclaration d'usage de l'IA, le journal des usages, la matrice de traçabilité des exigences, le suivi des retours de prélecture et les preuves de tests.

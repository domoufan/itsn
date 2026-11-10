<<<pagebreak>>>
# Conclusion générale

## Réponse à la problématique

La question posée en introduction était de savoir comment concevoir et mettre en œuvre une plateforme de gestion des interventions techniques, alignée sur les bonnes pratiques ITSM et adaptée aux moyens d'une PME, garantissant la traçabilité, le respect mesurable des engagements de service et l'aide à la décision, tout en intégrant l'intelligence artificielle de manière contrôlée et économiquement soutenable.

Le travail apporte à cette question une réponse construite en trois temps. D'abord, la **traçabilité et les engagements de service** ont été obtenus non par l'ajout de fonctions mais par des règles structurantes intégrées au cœur du modèle : référence unique générée sous verrou, machine à états explicite, matrice de priorité ITIL, échéances SLA calculées à la création et contrôlées à chaque transition, historique systématique, journal d'audit. Ensuite, l'**adaptation aux moyens d'une PME** résulte d'une architecture monolithique modulaire déployable en une commande, de choix d'hébergement sans coût fixe et d'une chaîne de livraison qui ne demande aucune administration système. Enfin, l'**intégration maîtrisée de l'IA** repose sur un principe simple et vérifiable : l'agent propose, l'humain décide. Ce principe est matérialisé par des garde-fous techniques (outils en lecture seule, données encadrées, sortie validée, limites de durée, d'actions et de budget, repli sans IA) et organisationnels (file de revue, décision tracée).

## Objectifs atteints et écarts

Tableau : Bilan des objectifs spécifiques

| Objectif | Résultat | Écart |
|---|---|---|
| OS1 – Formaliser les besoins | 32 exigences identifiées, priorisées, dotées de critères d'acceptation et reliées à des tests | Besoins issus d'une analyse documentaire et de scénarios, non d'un terrain réel |
| OS2 – Concevoir une solution cohérente | Modélisation UML (contexte, cas d'utilisation, classes, états, séquences), architecture et choix justifiés, correspondance exigences → composants | — |
| OS3 – Réaliser un prototype opérationnel | Application complète (backend, frontend, données fictives), dépôt organisé, démarrage en une commande | 100 % des exigences P1 réalisées et testées localement (40 tests automatisés réussis le 10/11/2026) |
| OS4 – Garantir qualité et sécurité | Plan de tests de 45 cas couvrant les 32 exigences ; mesures OWASP appliquées | Tests automatisés, de sécurité (4/6) et de performance exécutés ; tests manuels d'interface et tests liés au déploiement restant à exécuter |
| OS5 – Industrialiser la livraison | Chaîne CI/CD écrite et versionnée, promotion du même artefact, retour arrière décrit | Exécution du pipeline et déploiement cloud [à réaliser / réalisés — à mettre à jour] |
| OS6 – Intégrer l'IA de façon maîtrisée | Agent borné, repli, revue humaine, jeu d'évaluation de 30 cas et script de mesure | Évaluation avec le modèle réel [à réaliser / réalisée — résultats à reporter] |

Les écarts marqués « à mettre à jour » seront remplacés, dans la version finale, par les résultats effectivement observés ; aucun résultat non démontré n'est présenté ici comme acquis.

## Limites

Les limites de ce travail ont été signalées au fil des chapitres et sont rassemblées ici. L'organisation d'étude est fictive : les besoins traduisent la compréhension de l'auteur, nourrie par la documentation professionnelle, et non des observations de terrain ; la pertinence métier du référentiel d'exigences reste donc à confronter à de vrais utilisateurs. L'hébergement sur des offres gratuites n'offre aucune garantie de disponibilité et introduit une latence de redémarrage. L'évaluation de l'agent porte sur trente cas construits et annotés par l'auteur lui-même. Les délais SLA sont calculés en heures calendaires, sans calendrier d'ouverture. Les tests d'interface sont manuels. Enfin, une part importante du code a été produite avec l'assistance d'un outil d'IA générative : la compréhension et la maîtrise de ce code par l'étudiant, démontrées par sa relecture, son exécution, ses corrections et sa capacité à l'expliquer, constituent une condition de validité de ce travail que la soutenance permettra d'éprouver.

## Perspectives

Trois perspectives réalistes se dégagent. À court terme, **confronter la plateforme à un terrain réel** : la déployer en pilote auprès d'un petit prestataire informatique volontaire, recueillir les usages et réviser le référentiel d'exigences ; cette étape transformerait les hypothèses de conception en connaissances validées. À moyen terme, **étendre le périmètre ITSM** : gestion des problèmes (regroupement d'incidents récurrents détectés, par exemple, par l'agent), gestion des changements avec un circuit d'approbation, catalogue de services, calendrier d'ouverture pour les SLA, export comptable des interventions. À plus long terme, **approfondir l'agent** sans renoncer au contrôle humain : évaluation sur un corpus annoté par plusieurs techniciens, mesure de l'accord entre l'agent et les décisions finales des responsables pour détecter une dérive, suggestion de réponses aux demandeurs à partir de la base de connaissances, et, seulement si les mesures le justifient, application automatique limitée aux cas de confiance élevée et de faible priorité, toujours révocable.

## Acquis techniques et méthodologiques

Sur le plan technique, ce projet a permis de mettre en œuvre de bout en bout une application d'entreprise moderne : une API sécurisée par jetons signés avec une version récente de Spring Boot et de Spring Security, un schéma relationnel géré par migrations, une application monopage Angular fondée sur les composants autonomes et les signaux, la conteneurisation, une chaîne d'intégration et de livraison continues avec analyse de sécurité, un déploiement sur des services managés, et l'intégration d'un modèle de langage avec appel d'outils dans une boucle d'orchestration écrite à la main. Sur le plan méthodologique, il a imposé de distinguer ce qui est attendu de ce qui est démontré, de relier chaque exigence à un composant et à un test, de documenter les choix par comparaison d'alternatives, de chiffrer honnêtement un projet, et d'utiliser un outil d'IA générative comme un assistant dont chaque production doit être relue, exécutée et assumée — ce qui est, sans doute, l'acquis le plus durable de ce travail.

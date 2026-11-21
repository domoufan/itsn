<<<remerciements>>>
# Remerciements

Je tiens à exprimer ma reconnaissance à toutes les personnes qui ont contribué à la réalisation de ce travail.

Je remercie en premier lieu mon encadreur, M. Wane, pour sa disponibilité, la rigueur de ses relectures et les orientations qui ont structuré ce projet, de la formulation du problème jusqu'à la validation des choix techniques.

Mes remerciements vont également à l'équipe pédagogique de l'École 221, dont les enseignements du parcours Génie logiciel — analyse et conception, développement, bases de données, tests, méthodes agiles et gestion de projet — constituent le socle sur lequel repose cette plateforme.

Je remercie enfin ma famille et mes camarades de promotion pour leur soutien constant tout au long de cette année de formation.

<<<pagebreak>>>
# Dédicace

À ma famille, pour la confiance et les sacrifices consentis tout au long de mon parcours.

À toutes celles et ceux qui, dans l'ombre des services informatiques, maintiennent chaque jour les outils dont dépend le travail des autres.

<<<pagebreak>>>
# Préambule

Ce mémoire est présenté en vue de l'obtention de la Licence professionnelle Génie logiciel (code E221) délivrée par l'École 221, au titre de l'année académique 2025-2026.

La formation vise à former des professionnels capables de concevoir, réaliser, tester et mettre en production des solutions informatiques répondant à un besoin métier identifié. Le projet de fin de formation constitue l'aboutissement de ce cursus : il doit démontrer la capacité de l'étudiant à formuler un problème, à mobiliser des connaissances, à concevoir une solution et à en évaluer les résultats, dans une démarche vérifiable.

Le sujet retenu, « Plateforme de gestion des interventions techniques (ITSM) », s'inscrit dans le domaine de la gestion des services informatiques. Il a été traité au travers d'une étude de cas construite autour d'une entreprise de services du numérique fictive, SénéTech Services, dont le service support gère les incidents et les demandes de PME clientes. Le caractère fictif de l'organisation est assumé et signalé tout au long du document : il permet de travailler sur un périmètre réaliste et complet sans exposer de données d'une entreprise réelle, conformément au cadre de protection des données rappelé dans le canevas de l'établissement.

Le projet a été mené avec l'assistance d'outils d'intelligence artificielle générative, dans le respect du cadre validé par l'équipe pédagogique. Une déclaration d'usage et un journal des usages figurent en annexe.

<<<listes>>>

<<<pagebreak>>>
# Sigles et abréviations

Tableau : Sigles et abréviations utilisés dans le mémoire

| Sigle | Signification |
|---|---|
| API | Application Programming Interface (interface de programmation) |
| CAB | Change Advisory Board (comité consultatif des changements) |
| CD | Continuous Delivery / Deployment (livraison / déploiement continus) |
| CI | Continuous Integration (intégration continue) |
| CMDB | Configuration Management Database (base de gestion des configurations) |
| CORS | Cross-Origin Resource Sharing |
| CRUD | Create, Read, Update, Delete |
| CSRF | Cross-Site Request Forgery |
| CVE | Common Vulnerabilities and Exposures |
| DSI | Direction des systèmes d'information |
| ESN | Entreprise de services du numérique |
| FCFA | Franc de la Communauté financière africaine (XOF) |
| GHCR | GitHub Container Registry |
| HMAC | Hash-based Message Authentication Code |
| HTTPS | HyperText Transfer Protocol Secure |
| IA | Intelligence artificielle |
| ITIL | Information Technology Infrastructure Library |
| ITSM | IT Service Management (gestion des services informatiques) |
| JPA | Jakarta Persistence API |
| JSON | JavaScript Object Notation |
| JWT | JSON Web Token |
| KB | Knowledge Base (base de connaissances) |
| KPI | Key Performance Indicator (indicateur clé de performance) |
| LLM | Large Language Model (grand modèle de langage) |
| MTTR | Mean Time To Resolve (délai moyen de résolution) |
| ORM | Object-Relational Mapping |
| OWASP | Open Worldwide Application Security Project |
| PME | Petite et moyenne entreprise |
| REST | Representational State Transfer |
| SLA | Service Level Agreement (accord de niveau de service) |
| SMTP | Simple Mail Transfer Protocol |
| SPA | Single Page Application |
| SQL | Structured Query Language |
| TLS | Transport Layer Security |
| UML | Unified Modeling Language |
| VPN | Virtual Private Network |

<<<pagebreak>>>
# Résumé

**Contexte.** Les petites et moyennes entreprises de services informatiques gèrent encore fréquemment les interventions techniques de leurs clients par téléphone, messagerie instantanée et tableurs partagés. Cette organisation informelle entraîne des pertes de demandes, une absence d'engagements de service mesurables, une faible traçabilité et l'impossibilité de piloter l'activité par des indicateurs.

**Problème.** Comment concevoir une plateforme de gestion des interventions techniques, alignée sur les bonnes pratiques ITSM (ITIL), adaptée aux moyens d'une PME, qui garantisse la traçabilité de chaque demande, le respect des engagements de service et l'aide à la décision, tout en intégrant de façon maîtrisée l'intelligence artificielle ?

**Objectif.** Concevoir, réaliser, tester et déployer une application web complète (ITSN) couvrant le cycle de vie des tickets d'incident et de demande, la planification des interventions, l'inventaire des équipements, la base de connaissances et le pilotage par indicateurs, complétée par un agent d'intelligence artificielle de triage soumis à validation humaine.

**Méthode.** Étude de cas construite autour d'une ESN fictive dakaroise ; analyse documentaire (ITIL 4, ISO/IEC 20000) et étude comparative de solutions existantes (GLPI, Zammad, osTicket, Freshservice) pour dériver trente-deux exigences identifiées, priorisées et dotées de critères d'acceptation ; gestion de projet itérative inspirée de Scrum ; conception UML ; réalisation en Java 21 / Spring Boot 4 et Angular 22 sur PostgreSQL 16 ; tests unitaires et d'intégration automatisés ; chaîne d'intégration et de livraison continues (GitHub Actions) avec déploiement sur une plateforme cloud (Render, Neon) ; évaluation de l'agent de triage sur un jeu de trente cas.

**Principaux résultats.** Une plateforme fonctionnelle couvrant les exigences prioritaires, un dépôt organisé avec historique, une chaîne CI/CD reproductible produisant des artefacts versionnés et promouvant le même artefact de la préproduction vers la production après validation humaine, un agent de triage borné (outils en lecture seule, budget, délai, repli sans IA) dont les propositions ne sont jamais appliquées sans décision humaine. L'application est déployée sur une infrastructure cloud gratuite (Render, Neon) ; l'agent, évalué en conditions réelles sur 90 triages, obtient 98 % de catégories exactes et 100 % de priorités à un cran de la référence, sans détournement par un contenu adverse, pour moins d'un centime de dollar par ticket. Les résultats de tests et d'évaluation sont présentés au chapitre 7 avec leurs preuves.

**Limites.** Organisation fictive et besoins issus d'une analyse documentaire plutôt que d'entretiens de terrain ; hébergement sur offres gratuites sans garantie de disponibilité ; évaluation de l'agent sur un échantillon réduit ; fonctionnalités avancées (gestion des changements, catalogue de services, application mobile) reportées en perspectives.

**Mots clés :** ITSM ; gestion des incidents ; SLA ; Spring Boot ; agent IA.

<<<pagebreak>>>
# Abstract

**Context.** Small and medium-sized IT service providers still frequently manage their customers' technical interventions through phone calls, instant messaging and shared spreadsheets. This informal organisation leads to lost requests, no measurable service commitments, poor traceability and no indicator-based management.

**Problem.** How can we design a technical intervention management platform, aligned with ITSM good practices (ITIL) and suited to an SME's resources, that guarantees the traceability of every request, compliance with service level commitments and decision support, while integrating artificial intelligence in a controlled way?

**Objective.** To design, build, test and deploy a complete web application (ITSN) covering the lifecycle of incident and request tickets, intervention scheduling, equipment inventory, a knowledge base and indicator-based management, complemented by an AI triage agent subject to human validation.

**Method.** A case study built around a fictional IT services company in Dakar; documentary analysis (ITIL 4, ISO/IEC 20000) and a comparative study of existing solutions (GLPI, Zammad, osTicket, Freshservice) to derive thirty-two identified, prioritised requirements with acceptance criteria; an iterative, Scrum-inspired project management approach; UML design; implementation in Java 21 / Spring Boot 4 and Angular 22 on PostgreSQL 16; automated unit and integration tests; a continuous integration and delivery pipeline (GitHub Actions) with deployment to a cloud platform (Render, Neon); evaluation of the triage agent on a set of thirty cases.

**Main results.** A working platform covering the priority requirements, an organised repository with history, a reproducible CI/CD pipeline producing versioned artefacts and promoting the same artefact from staging to production after human approval, and a bounded triage agent (read-only tools, budget, timeout, non-AI fallback) whose proposals are never applied without a human decision. The application is deployed on free cloud infrastructure (Render, Neon); evaluated under real conditions on 90 triages, the agent reaches 98% exact categories and 100% priorities within one level of the reference, with no hijacking by adversarial content, for less than one cent per ticket. Test and evaluation results are presented in chapter 7 with their evidence.

**Limitations.** A fictional organisation and requirements derived from documentary analysis rather than field interviews; hosting on free tiers without availability guarantees; agent evaluation on a small sample; advanced features (change management, service catalogue, mobile application) deferred to future work.

**Keywords:** ITSM; incident management; SLA; Spring Boot; AI agent.


<<<sommaire>>>

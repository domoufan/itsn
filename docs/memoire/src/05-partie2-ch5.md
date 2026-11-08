<<<pagebreak>>>
# Chapitre 5 — Coût du projet

## Hypothèses de chiffrage

L'estimation est exprimée en **francs CFA (FCFA, XOF)** et datée du **8 novembre 2026**. Les prix libellés en dollars américains par les fournisseurs sont convertis au taux arrondi de **1 USD = 600 FCFA** (hypothèse de travail à actualiser au moment du dépôt final ; le franc CFA étant arrimé à l'euro, la conversion dépend du cours EUR/USD du jour). Les montants hors taxes des fournisseurs sont repris tels quels ; la TVA sénégalaise (18 %) n'est pas ajoutée aux services étrangers facturés en ligne, conformément à la pratique observée pour les abonnements payés par carte.

Trois catégories sont distinguées afin d'éviter tout double comptage :

- les **dépenses ponctuelles** (coût initial de réalisation) ;
- les **coûts récurrents** (fonctionnement), calculés sur une **période de douze mois** d'exploitation ;
- les **charges estimées** (valorisation du temps de travail), distinguées des **dépenses effectivement engagées** (sorties d'argent réelles).

La valorisation du temps de l'étudiant repose sur un coût journalier de **25 000 FCFA**, correspondant à un salaire mensuel brut d'environ 500 000 FCFA pour un développeur débutant à Dakar (hypothèse fondée sur les grilles indicatives observées dans les offres d'emploi locales ; elle ne constitue pas une donnée statistique officielle). Le temps de l'encadreur n'est pas valorisé.

## Coût initial (réalisation)

Tableau : Coût initial du projet (dépenses ponctuelles et charges estimées)

| Poste | Détail | Quantité | Unité | Coût unitaire (FCFA) | Sous-total (FCFA) | Source du prix | Nature |
|---|---|---|---|---|---|---|---|
| Ressources humaines | Analyse, conception, développement, tests, documentation (étudiant) | 35 | jour | 25 000 | 875 000 | Hypothèse (voir ci-dessus) | Charge estimée |
| Matériel | Ordinateur portable de développement (déjà possédé ; amortissement sur 3 ans, 2 mois d'usage) | 2/36 | de 450 000 | — | 25 000 | Prix moyen constaté d'un portable 16 Go de RAM à Dakar | Charge estimée |
| Logiciels | Java 21, Spring Boot, Angular, PostgreSQL, Docker, Git, VS Code / IntelliJ Community : licences libres ou gratuites | — | — | 0 | 0 | Licences des éditeurs | — |
| Assistance IA | Abonnement Claude Max (Anthropic) utilisé pour la préparation du mémoire et du code | 2 | mois | 60 000 (100 USD) | 120 000 | Grille publique Anthropic (consultée le 8/11/2026) ; à ajuster selon le palier souscrit | Dépense engagée |
| Services | Crédits API Anthropic pour les tests et l'évaluation de l'agent (environ 30 cas × 3 répétitions × ~3 000 jetons) | 5 | USD | 600 | 3 000 | Grille publique : 1 USD/M jetons en entrée, 5 USD/M en sortie | Dépense engagée (estimée, relevé réel en annexe) |
| Connectivité | Connexion Internet pendant la réalisation | 2 | mois | 25 000 | 50 000 | Offre résidentielle courante à Dakar (hypothèse) | Dépense engagée |
| Hébergement et CI | GitHub (dépôt, Actions, GHCR), Render (services gratuits), Neon (plan gratuit) | 2 | mois | 0 | 0 | Offres gratuites des fournisseurs | — |
| **Total coût initial** | | | | | **1 073 000** | | dont dépenses engagées : 173 000 ; charges estimées : 900 000 |

## Coût de fonctionnement sur douze mois

Deux scénarios sont présentés : le scénario **démonstration** (offres gratuites, acceptable pour une soutenance et un pilote interne) et le scénario **exploitation PME** (offres payantes d'entrée de gamme supprimant la mise en veille et élargissant la base).

Tableau : Coût de fonctionnement annuel — scénario démonstration (offres gratuites)

| Poste | Détail | Quantité | Unité | Coût unitaire (FCFA) | Sous-total (FCFA) | Source du prix |
|---|---|---|---|---|---|---|
| Hébergement API | Render, 2 services web gratuits (750 h d'instance par mois, mise en veille après 15 min) | 12 | mois | 0 | 0 | Documentation Render, nov. 2026 |
| Hébergement interface | Render Static Site (gratuit) | 12 | mois | 0 | 0 | Documentation Render |
| Base de données | Neon plan gratuit (100 CU-heures et 1 Go par projet, mise en veille après 5 min) | 12 | mois | 0 | 0 | Documentation Neon, nov. 2026 |
| CI/CD et registre | GitHub Actions (2 000 min/mois), GHCR | 12 | mois | 0 | 0 | Documentation GitHub |
| API d'IA | Triage de ~150 tickets/mois × ~3 500 jetons (entrée) + ~400 jetons (sortie) ≈ 0,8 USD/mois | 12 | mois | 500 | 6 000 | Grille Anthropic ; budget plafonné à 1 USD/jour dans l'application |
| Nom de domaine | `.sn` ou `.com` (facultatif ; sous-domaines Render gratuits sinon) | 1 | an | 15 000 | 15 000 | Tarifs courants des bureaux d'enregistrement |
| Maintenance | Mises à jour de sécurité (dépendances, images), 0,5 jour/mois | 6 | jour | 25 000 | 150 000 | Hypothèse (charge estimée) |
| Aléas | Provision de 10 % des dépenses engagées (dépassement de budget IA, nom de domaine, outils) | — | — | — | 2 100 | Hypothèse |
| **Total fonctionnement (12 mois)** | | | | | **173 100** | dont dépenses engagées : 23 100 ; charges estimées : 150 000 |

Tableau : Coût de fonctionnement annuel — scénario exploitation PME (offres payantes d'entrée de gamme)

| Poste | Détail | Quantité | Unité | Coût unitaire (FCFA) | Sous-total (FCFA) | Source du prix |
|---|---|---|---|---|---|---|
| Hébergement API | Render instance Starter (512 Mo, toujours active) × 2 (préproduction et production) : 7 USD/mois chacune | 24 | instance-mois | 4 200 | 100 800 | Grille Render, nov. 2026 |
| Base de données | Neon plan Launch : compute 0,106 USD/CU-heure et stockage 0,35 USD/Go-mois ; estimation 0,25 CU en continu (≈ 180 CU-h) + 2 Go ≈ 20 USD/mois | 12 | mois | 12 000 | 144 000 | Grille Neon, nov. 2026 |
| Hébergement interface | Render Static Site | 12 | mois | 0 | 0 | Documentation Render |
| API d'IA | Triage de ~400 tickets/mois ≈ 2 USD/mois | 12 | mois | 1 200 | 14 400 | Grille Anthropic |
| Messagerie transactionnelle | Service SMTP (offre gratuite jusqu'à quelques centaines d'e-mails/jour, ex. Brevo) | 12 | mois | 0 | 0 | Grille éditeur (à vérifier au moment du choix) |
| Nom de domaine et certificat | Domaine ; certificat TLS fourni par Render | 1 | an | 15 000 | 15 000 | Tarifs courants |
| Maintenance et évolutions | 1 jour/mois | 12 | jour | 25 000 | 300 000 | Hypothèse (charge estimée) |
| Aléas | Provision de 10 % des dépenses engagées | — | — | — | 27 400 | Hypothèse |
| **Total fonctionnement (12 mois)** | | | | | **601 600** | dont dépenses engagées : 301 600 ; charges estimées : 300 000 |

## Lecture et comparaison

Le coût total du projet, pour une première année d'exploitation en scénario démonstration, s'établit à environ **1 246 000 FCFA**, dont près de 85 % correspondent à la valorisation du temps de travail et non à des sorties d'argent ; les **dépenses effectivement engagées** se limitent à environ **196 000 FCFA** (dominées par l'abonnement d'assistance IA et la connectivité). En scénario exploitation PME, les dépenses engagées annuelles de fonctionnement atteignent environ **300 000 FCFA** (≈ 500 USD), ce qui reste très inférieur au coût d'une solution SaaS équivalente : neuf agents sur l'offre d'entrée de gamme d'un éditeur du marché à 19 USD par agent et par mois représentent 2 052 USD par an, soit environ **1 231 000 FCFA**, sans fonctions d'IA, celles-ci étant facturées en supplément. Cette comparaison ne tient toutefois pas compte de la maturité fonctionnelle supérieure des solutions commerciales ni du coût du temps interne de maintenance, explicitement valorisé ici.

Le chiffrage cloud détaillé et les coûts du pipeline agentique (jetons consommés, coût par triage, coût de l'évaluation) sont intégrés dans ces tableaux conformément aux sections 8.4 et 8.5 du canevas ; les relevés réels de consommation de l'API, lorsqu'ils seront disponibles, figureront en annexe et permettront de corriger l'estimation.

# Tests manuels scénarisés rejoués automatiquement — 2026-11-13 17:56

Environnement : API locale (JAR) + PostgreSQL 16, interface Angular (ng serve), Playwright/Chromium 1440×900, données de démonstration fictives, SMTP de capture local (port 1025). Script : tools/ui-tests/tm_tests.py.

| Id | Résultat observé | Statut | Captures |
|---|---|---|---|
| TM-01 | user : Tickets, Base de connaissances ; tech : Tableau de bord, Tickets, Planning, Équipements, Base de connaissances ; manager : Tableau de bord, Tickets, Planning, Équipements, Base de connaissances, Propositions IA ; admin : Tableau de bord, Tickets, Planning, Équipements, Base de connaissances, Propositions IA, Administration ; déconnexion : retour à /login pour chaque rôle | Réussi | TM-01-user.png, TM-01-tech.png, TM-01-manager.png, TM-01-admin.png |
| TM-02 | refus : « Le délai de résolution doit être supérieur ou égal au délai de réponse » ; valide : « Politique SLA mise à jour » | Réussi | TM-02-refus.png, TM-02-valide.png |
| TM-03 | 1re planification : « Intervention planifiée » ; 2e (10h-12h) : « Conflit de planning : le technicien a déjà une intervention sur ce créneau » ; CR vide : « Un compte rendu est obligatoire pour clore une intervention » ; CR complet : « Compte rendu enregistré » | Réussi | TM-03-planifiee.png, TM-03-conflit.png, TM-03-cr-vide.png, TM-03-cr-ok.png |
| TM-04 | tickets ouverts affichés 9 = liste filtrée openOnly 9 ; créés sur 7 j affichés « 7/1 » vs API 7 | Réussi | TM-04-7j.png |
| TM-05 | doublon : « Numéro d'inventaire déjà existant » ; sites affichés après filtre : ['Usine Rufisque'] ; lignes avec garantie expirée signalées : 4 | Réussi | TM-05-doublon.png, TM-05-filtre.png |
| TM-06 | technicien voit le brouillon : True ; résultats demandeur « imprimante » : ['Imprimante réseau hors ligne : procédure de diagnostic'] ; brouillon visible du demandeur : False | Réussi | TM-06-technicien.png, TM-06-demandeur.png |
| TM-07 | 3 e-mail(s) portant INC-2026-00025 : ['[INC-2026-00025] Votre demande a été enregistrée', '[INC-2026-00025] Ticket affecté', '[INC-2026-00025] Ticket résolu'] | Réussi | TM-07-creation.png, TM-07-resolu.png |
| TM-08 | 1280 px : 18 badges, tous avec texte : True ; 420 px (demandeur) : largeur de page 420 px ; sur mobile la navigation latérale reste affichée (limite connue : pas de menu repliable automatique) | Réussi | TM-08-1280.png, TM-08-420.png, TM-08-420-form.png |

# TD-04 — Retour arrière par la chaîne CD (14/11/2026)

Scénario : la version N+1 (`2410df5`) est déployée en production par la CD ; on revient à la version N (`962f5d4`) en relançant le workflow CD manuellement avec cette version.

## 1. Déploiement de la version N+1 — https://github.com/domoufan/itsn/actions/runs/43623671662

| Travail | Début (UTC) | Fin (UTC) | Résultat |
|---|---|---|---|
| Déterminer la version à livrer | 17:54:59 | 17:55:02 | `2410df5` |
| Déploiement en préproduction | 17:55:05 | 17:58:46 | OK : santé UP, version 2410df5 (détectée après 220 s) |
| Promotion en production (approbation requise) | 18:01:06 (approbation domoufan) | 18:05:02 | OK : santé UP, version 2410df5 (détectée après 230 s) |

## 2. Retour arrière vers la version N — https://github.com/domoufan/itsn/actions/runs/43624659808

Déclenchement : `workflow_dispatch`, entrée `version = 962f5d4`.

| Travail | Début (UTC) | Fin (UTC) | Résultat |
|---|---|---|---|
| Déterminer la version à livrer | 18:05:46 | 18:05:49 | `962f5d4` |
| Déploiement en préproduction | 18:05:52 | 18:09:38 | OK : santé UP, version 962f5d4 (détectée après 220 s) |
| Promotion en production (approbation requise) | 18:16:10 (approbation domoufan) | 18:20:13 | OK : santé UP, version 962f5d4 (détectée après 240 s) |

Sortie du smoke test de production lors du retour arrière :
```
Déploiement demandé : commit 962f5d4
Attente du commit 962f5d4 sur https://itsn-api.onrender.com (max 10 min)…
Commit 962f5d4 détecté après 240 s
OK : santé UP, version 962f5d4, authentification exigée
```

Observations : le retour arrière suit exactement le même chemin que la livraison (préproduction, smoke test, approbation, production), ce qui garantit qu'une version de secours est elle aussi vérifiée avant d'atteindre la production ; durée totale d'environ 4 minutes par environnement, dominée par la reconstruction de l'image par Render (offre gratuite) ; aucune migration de schéma n'était en jeu entre N et N+1 (règle des migrations additives, chapitre 8). Le schéma Flyway est resté à la version 1 sur les deux branches Neon.

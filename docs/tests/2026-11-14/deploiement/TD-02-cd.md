# TD-02 — Déploiement par la chaîne CD (14/11/2026)

## Exécution réussie : https://github.com/domoufan/itsn/actions/runs/43622128103

Déclenchement : `workflow_run` à la fin de la CI 43621710204 (réussie) sur le commit `962f5d4`.

| Travail | Début (UTC) | Fin (UTC) | Résultat |
|---|---|---|---|
| Déterminer la version à livrer | 17:37:36 | 17:37:38 | version `962f5d4` |
| Déploiement en préproduction | 17:37:41 | 17:41:44 | réussi (crochet Render → construction → smoke test) |
| Promotion en production (approbation requise) | approbation de domoufan à 17:45 ; 17:45:33 | 17:49:39 | réussi |

Sortie du smoke test, préproduction (`itsn-api-staging`) :
```
Déploiement demandé : commit 962f5d486e628156e487c8a0847239780d899fb2
Attente du commit 962f5d4 sur https://itsn-api-staging.onrender.com (max 10 min)…
Commit 962f5d4 détecté après 240 s
OK : santé UP, version 962f5d4, authentification exigée
```

Sortie du smoke test, production (`itsn-api`) :
```
Déploiement demandé : commit 962f5d486e628156e487c8a0847239780d899fb2
Attente du commit 962f5d4 sur https://itsn-api.onrender.com (max 10 min)…
Commit 962f5d4 détecté après 30 s
OK : santé UP, version 962f5d4, authentification exigée
```

Côté Render : déploiements `dep-dbs9fot6e07s7397lp50` (préproduction, trigger `deploy_hook`) et `dep-dbs9sgd6e07s7398kms0` (production, trigger `deploy_hook`, 17:45:37 → live 17:49:27), tous deux au commit `962f5d4`. Le même commit est donc en service dans les deux environnements, la production n'ayant été mise à jour qu'après approbation humaine dans l'environnement GitHub `production`.

## Exécution précédente en échec (preuve du critère bloquant) : https://github.com/domoufan/itsn/actions/runs/43620363033

Lancement manuel (`workflow_dispatch`, version `921fa49`). Le crochet a bien déclenché la construction et la mise en ligne de la préproduction (Render : `dep-dbs9fot6e07s7397lp50`… live à 17:22), mais le smoke test a échoué après 10 minutes d'attente, car l'image ne renvoyait pas son commit :
```
Déploiement demandé : commit 921fa49fcab1039a83a895740778140c4fff119c
Attente du commit 921fa49 sur https://itsn-api-staging.onrender.com (max 10 min)…
ÉCHEC : le commit 921fa49 n'est pas servi
{"app":{"name":"ITSN","version":"staging","commit":"local"}}
```
La promotion en production a été **sautée** (skipped) : une préproduction non conforme ne peut pas atteindre la production. Cause : la variable `GIT_COMMIT` figée à `local` dans l'image prenait le pas sur `RENDER_GIT_COMMIT`. Correction `962f5d4` (argument de construction `RENDER_GIT_COMMIT` et priorité à la variable d'exécution), consignée AN-11.

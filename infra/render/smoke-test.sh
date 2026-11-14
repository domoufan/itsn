#!/usr/bin/env bash
# Tests de bon fonctionnement après déploiement : attend que la version attendue réponde, vérifie santé et sécurité.
# Usage : smoke-test.sh <url-base-api> <version-attendue>
set -euo pipefail
URL="${1:?url manquante}"; EXPECTED="${2:?version manquante}"
echo "Attente du commit ${EXPECTED} sur ${URL} (max 10 min)…"
for i in $(seq 1 60); do
  if body=$(curl -fsS "${URL}/actuator/info" 2>/dev/null) && echo "$body" | grep -q "\"commit\":\"${EXPECTED}"; then
    echo "Commit ${EXPECTED} détecté après $((i*10)) s"; break
  fi
  if [ "$i" -eq 60 ]; then echo "ÉCHEC : le commit ${EXPECTED} n'est pas servi"; echo "$body"; exit 1; fi
  sleep 10
done
health=$(curl -fsS "${URL}/actuator/health")
echo "$health" | grep -q '"status":"UP"' || { echo "ÉCHEC : santé = $health"; exit 1; }
code=$(curl -s -o /dev/null -w '%{http_code}' "${URL}/api/tickets")
[ "$code" = "401" ] || { echo "ÉCHEC : l'API doit refuser les requêtes non authentifiées (code $code)"; exit 1; }
echo "OK : santé UP, version ${EXPECTED}, authentification exigée"

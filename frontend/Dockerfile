# ---------- Étape 1 : construction Angular ----------
FROM node:22-alpine AS build
WORKDIR /build
COPY package*.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build -- --configuration production

# ---------- Étape 2 : serveur nginx non privilégié (fichiers statiques + proxy /api) ----------
FROM nginxinc/nginx-unprivileged:1.27-alpine
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build /build/dist/frontend/browser /usr/share/nginx/html
ENV BACKEND_URL=http://backend:8080
# Exécution explicite sans privilèges (règle Trivy DS-0002) ; cette image écoute sur 8080
USER nginx
EXPOSE 8080

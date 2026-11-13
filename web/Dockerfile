# syntax=docker/dockerfile:1
# Web app image (W08, P-56): the dispatcher web app served by Caddy, which also proxies /api/ to the app and gets
# HTTPS certificates. Build context: the repository root (Vite copies brand/logo).
#   docker build -f web/Dockerfile -t pathwise-web .

FROM node:24.11.1-alpine AS build
WORKDIR /src/web
# pnpm at the version of package.json (packageManager). Not corepack: the one in Node 24.11 cannot run pnpm 12.
RUN npm install -g pnpm@12.16.1
# Dependencies in their own layer so source-only changes reuse it.
COPY web/package.json web/pnpm-lock.yaml web/pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile
COPY brand/logo/ /src/brand/logo/
COPY web/ ./
# Lint and tests run in CI; the build still type-checks. No VITE_* here: the image is configured at runtime.
RUN pnpm build

FROM caddy:2.11.7-alpine AS runtime
# Alpine fixes released after the base image (the image workflow fails on fixed CRITICAL vulnerabilities).
RUN apk upgrade --no-cache
COPY deploy/caddy/Caddyfile /etc/caddy/Caddyfile
# Validation provisions a local CA for localhost: keep its data and keys out of the image.
RUN PATHWISE_DOMAIN=localhost XDG_DATA_HOME=/tmp/caddy-validate XDG_CONFIG_HOME=/tmp/caddy-validate \
    caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile \
    && rm -r /tmp/caddy-validate
COPY deploy/caddy/write-config.sh /usr/local/bin/pathwise-write-config
COPY --from=build /src/web/dist/ /srv/app/
ENV PATHWISE_UPSTREAM=app:8080 \
    PATHWISE_WEB_ROOT=/srv
# Writes /srv/app/config.js from PATHWISE_* variables, then starts Caddy.
ENTRYPOINT ["/usr/local/bin/pathwise-write-config"]
CMD ["caddy", "run", "--config", "/etc/caddy/Caddyfile", "--adapter", "caddyfile"]

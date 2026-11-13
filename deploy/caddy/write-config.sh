#!/bin/sh
# Writes the web app's runtime config, /app/config.js (W08, P-56), from the environment, then runs the given command.
#   PATHWISE_MAPTILER_KEY  MapTiler key of the map tiles (restrict it to the domain in MapTiler's dashboard)
#   PATHWISE_BASEMAP       maptiler (default); empty keeps the value of the build
#   PATHWISE_API_ORIGIN    empty (default): the API is on the same origin (P-47)
set -eu

target="${PATHWISE_WEB_ROOT:-/srv}/app/config.js"

# Values go into a JavaScript string: refuse anything that is not a plain token or URL.
for value in "${PATHWISE_MAPTILER_KEY:-}" "${PATHWISE_BASEMAP:-}" "${PATHWISE_API_ORIGIN:-}"; do
  case "$value" in
    *[!A-Za-z0-9._:/-]*)
      echo "pathwise-write-config: PATHWISE_* values may contain only letters, digits and . _ : / -" >&2
      exit 1
      ;;
  esac
done

printf 'window.__PATHWISE_CONFIG__ = {"apiOrigin":"%s","basemap":"%s","maptilerKey":"%s"};\n' \
  "${PATHWISE_API_ORIGIN:-}" "${PATHWISE_BASEMAP:-}" "${PATHWISE_MAPTILER_KEY:-}" > "$target"

exec "$@"

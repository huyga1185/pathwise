#!/usr/bin/env bash
# Generates the Dart client of the driver app from api/openapi.json (P-41, P-44, docs/client/ARCHITECTURE.md §3):
# openapi-generator (generator dart-dio, json_serializable) into mobile/packages/pathwise_api, then its build_runner
# step. The output is committed and never edited by hand; CI runs this again and fails on any difference.
# Needs Docker and Flutter (FLUTTER, default "flutter").
# `format: date` (planDate, the date query of /driver/routes) stays a "YYYY-MM-DD" String: a day of the store, not an
# instant, and dio would send a DateTime query as "2026-12-01 00:00:00.000", which the API refuses (400).
set -euo pipefail
cd "$(dirname "$0")/.."

GENERATOR_IMAGE="openapitools/openapi-generator-cli:v7.14.0"
OUT="mobile/packages/pathwise_api"
FLUTTER="${FLUTTER:-flutter}"

rm -rf "$OUT"
docker run --rm -u "$(id -u):$(id -g)" -v "$PWD:/local" "$GENERATOR_IMAGE" generate \
  -i /local/api/openapi.json \
  -g dart-dio \
  -o "/local/$OUT" \
  --global-property apiTests=false,modelTests=false \
  --type-mappings date=String \
  --additional-properties pubName=pathwise_api,pubDescription="PathWise API client (generated)",serializationLibrary=json_serializable,dateLibrary=core,pubAuthor=PathWise \
  >/dev/null
# The generator writes files that do not describe the package (git setup, CI templates): keep only the package.
rm -rf "$OUT/.travis.yml" "$OUT/git_push.sh" "$OUT/.openapi-generator-ignore" "$OUT/.openapi-generator" "$OUT/doc" "$OUT/test"
# Same Dart as the app: json_serializable writes null-aware elements, which the generator's ">=3.5.0" does not allow.
sed -i "s/^  sdk: .*/  sdk: ^3.13.7/" "$OUT/pubspec.yaml"
sed -i "s/^homepage: homepage$/publish_to: none/" "$OUT/pubspec.yaml"
(cd "$OUT" && "$FLUTTER" pub get >/dev/null && "$(dirname "$(command -v "$FLUTTER" || echo "$FLUTTER")")/dart" run build_runner build --delete-conflicting-outputs >/dev/null)
echo "$OUT generated from api/openapi.json"

#!/usr/bin/env bash
# Regenerates api/openapi.json, the contract clients generate code from (P-41, docs/client/ARCHITECTURE.md §3).
# Runs the backend test that writes the OpenAPI document the application serves, then normalizes it into the snapshot.
# Needs Docker (Testcontainers), like every backend test. Commit api/openapi.json in the same PR as the API change.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
(
  cd "$root/be"
  ./mvnw -q -B -ntp test -Dtest=ApiSpecContractIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true
)
python3 "$root/scripts/openapi_snapshot.py" write

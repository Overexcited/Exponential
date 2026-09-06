#!/usr/bin/env bash
set -euo pipefail

EIGENT_REF="${EIGENT_REF:-v1.0.2}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORK="$ROOT/.build/upstream"
rm -rf "$WORK"
mkdir -p "$WORK"

git clone --depth 1 --branch "$EIGENT_REF" https://github.com/eigent-ai/eigent.git "$WORK/eigent"

EXPECTED_EIGENT_COMMIT="e478094"
ACTUAL_EIGENT_COMMIT="$(git -C "$WORK/eigent" rev-parse HEAD)"
case "$ACTUAL_EIGENT_COMMIT" in
  "$EXPECTED_EIGENT_COMMIT"*) ;;
  *)
    echo "Unexpected Eigent ${EIGENT_REF} commit: ${ACTUAL_EIGENT_COMMIT}; expected ${EXPECTED_EIGENT_COMMIT}*" >&2
    exit 1
    ;;
esac

mkdir -p "$ROOT/app/src/main/python/eigent-backend"
cp -a "$WORK/eigent/backend/." "$ROOT/app/src/main/python/eigent-backend/"

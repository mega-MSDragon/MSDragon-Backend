#!/usr/bin/env bash
set -euo pipefail

if [[ ! -x ./gradlew ]]; then
  echo "gradlew not found or not executable; skip post-edit lint"
  exit 0
fi

./gradlew test

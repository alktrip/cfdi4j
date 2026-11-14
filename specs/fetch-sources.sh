#!/usr/bin/env bash
# Downloads the reference documents listed in specs/SOURCES.md ("Reference documents" table)
# into specs/downloads/ and verifies their SHA-256. Bundled files are already in the repository.
set -euo pipefail

cd "$(dirname "$0")"
mkdir -p downloads

status=0
# Rows look like: | [`file`](url) | description | published | `sha256` |
grep -E '^\| \[`[^`]+`\]\(https?://omawww\.sat\.gob\.mx/[^)]+\) \|' SOURCES.md |
while IFS='|' read -r _ link _ _ hash _; do
  file=$(sed -E 's/.*\[`([^`]+)`\].*/\1/' <<<"$link")
  url=$(sed -E 's/.*\]\(([^)]+)\).*/\1/' <<<"$link")
  expected=$(tr -d ' `' <<<"$hash")
  target="downloads/$file"
  if [[ ! -f "$target" ]]; then
    echo "Downloading $file"
    curl --fail --silent --show-error --location --max-time 300 -o "$target" "$url"
  fi
  actual=$(sha256sum "$target" | cut -d' ' -f1)
  if [[ "$actual" != "$expected" ]]; then
    echo "SHA-256 mismatch for $file: expected $expected, got $actual" >&2
    exit 1
  fi
  echo "OK  $file"
done || status=$?

exit "$status"

#!/usr/bin/env bash
# sync-readme-version.sh [--check] <version>
#
# Single source of truth for the version shown in README.md's install snippets:
# the project version declared in build.gradle.kts. This script rewrites every
# install-snippet version reference in README.md to <version>.
#
# Modes:
#   scripts/sync-readme-version.sh 1.2.3            # rewrite README.md in place
#   scripts/sync-readme-version.sh --check 1.2.3    # verify only; non-zero if stale
#
# The --check mode powers the CI guardrail (.github/workflows/ci.yml): it derives
# the expected version from build.gradle.kts and fails the build if README.md has
# drifted, so the install snippets can never go stale. Run the in-place mode during
# release prep (when you bump the version in build.gradle.kts) to fix it in one shot.
#
# Examples of accepted versions: 1.2.3, 1.2.3-alpha.1
#
# Idempotent: running the in-place mode twice with the same version leaves README.md
# byte-for-byte identical (the substitutions replace the full coordinate/tag, not a diff).

set -euo pipefail

# ── Argument parsing ───────────────────────────────────────────────────────────
CHECK=0
if [ "${1:-}" = "--check" ]; then
  CHECK=1
  shift
fi

VERSION="${1:-}"
if [ -z "$VERSION" ]; then
  echo "Usage: $0 [--check] <version>  (e.g. 1.2.3 or 1.2.3-alpha.1)" >&2
  exit 1
fi

# Accept X.Y.Z with an optional pre-release suffix (letters, digits, dots, hyphens)
if ! echo "$VERSION" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+([-.][A-Za-z0-9._-]+)?$'; then
  echo "Error: '$VERSION' is not a valid semver (expected X.Y.Z or X.Y.Z-pre)." >&2
  exit 1
fi

# ── Target file ───────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
README="$SCRIPT_DIR/../README.md"

if [ ! -f "$README" ]; then
  echo "Error: README.md not found at $README" >&2
  exit 1
fi

# ── Portable in-place sed (operates on the working copy, not the original) ──────
# macOS (BSD) sed requires the empty-string argument to -i; GNU sed (Linux CI)
# does not accept it, so detect and branch.
if sed --version 2>/dev/null | grep -q 'GNU'; then
  sedi() { sed -i "$@"; }
else
  sedi() { sed -i '' "$@"; }
fi

# Work on a copy so --check never mutates README.md and the in-place write is atomic.
WORK="$(mktemp)"
trap 'rm -f "$WORK"' EXIT
cp "$README" "$WORK"

# ── 1. Gradle / KMP / prose coordinates ─────────────────────────────────────────
# Matches  io.github.elzinko:kexpresso:<ver>  and  io.github.elzinko:kexpresso-jvm:<ver>
# and replaces only the version segment (stop at " ) ' ` or space).
sedi -E \
  "s|(io\\.github\\.elzinko:kexpresso(-jvm)?:)[^\")'\` ]*|\1${VERSION}|g" \
  "$WORK"

# ── 2. Maven <version> tags inside io.github.elzinko <dependency> blocks ────────
# Track when we are inside a <dependency> whose <groupId> is io.github.elzinko,
# then replace the next <version>…</version>. awk gives the multi-line context.
TMPFILE="$(mktemp)"
awk -v ver="$VERSION" '
  /<dependency>/ { in_dep=1; found_group=0 }
  /<\/dependency>/ { in_dep=0; found_group=0 }
  in_dep && /<groupId>io\.github\.elzinko<\/groupId>/ { found_group=1 }
  in_dep && found_group && /<version>/ {
    sub(/<version>[^<]*<\/version>/, "<version>" ver "<\/version>")
    found_group=0
  }
  { print }
' "$WORK" > "$TMPFILE"
mv "$TMPFILE" "$WORK"

# ── Apply or verify ─────────────────────────────────────────────────────────────
if [ "$CHECK" -eq 1 ]; then
  if diff -u "$README" "$WORK" >/tmp/readme-version.diff 2>&1; then
    echo "README.md install snippets are in sync with version $VERSION."
    exit 0
  fi
  echo "Error: README.md install snippets are out of sync with version $VERSION." >&2
  echo "Run: scripts/sync-readme-version.sh $VERSION" >&2
  echo "--- expected changes ---" >&2
  cat /tmp/readme-version.diff >&2
  exit 1
fi

cp "$WORK" "$README"
echo "README.md updated to version $VERSION."

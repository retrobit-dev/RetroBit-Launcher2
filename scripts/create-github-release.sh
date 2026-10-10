#!/usr/bin/env bash
# ============================================================================
# Membuat GitHub Release "v2.5.0-retrobit" + melampirkan APK dari artifact CI.
# Jalankan dari root repo: bash scripts/create-github-release.sh
# ============================================================================
set -euo pipefail

REPO="retrobit-dev/RetroBit-Launcher2"
RUN_ID="38062328933"
ARTIFACT="RetroBit-Launcher-Debug-APK"
TAG="v2.5.0-retrobit"
TITLE="RetroBit Launcher v2.5.0-retrobit"
NOTES="$(cd "$(dirname "$0")" && pwd)/release-notes-v2.5.0-retrobit.md"
STAGE="release-assets"

command -v gh >/dev/null || { echo "ERROR: gh CLI tidak ditemukan"; exit 1; }
test -f "$NOTES" || { echo "ERROR: release notes tidak ditemukan: $NOTES"; exit 1; }

# --- 1. Ambil APK dari artifact CI (fallback: re-run workflow bila expired) ---
rm -rf "$STAGE"
if gh run download "$RUN_ID" --repo "$REPO" --name "$ARTIFACT" --dir "$STAGE" 2>/dev/null; then
  echo "APK berhasil diunduh dari run $RUN_ID."
else
  echo "Artifact run $RUN_ID tidak tersedia (retensi 14 hari, mungkin expired)."
  echo "Memicu ulang workflow build-apk.yml di main..."
  gh workflow run build-apk.yml --repo "$REPO" --ref main
  sleep 15
  NEW_RUN=$(gh run list --repo "$REPO" --branch main --limit 1 --json databaseId -q '.[0].databaseId')
  echo "Menunggu run baru selesai (id $NEW_RUN)..."
  gh run watch "$NEW_RUN" --repo "$REPO" --exit-status
  gh run download "$NEW_RUN" --repo "$REPO" --name "$ARTIFACT" --dir "$STAGE"
fi

APK="$STAGE/app-debug.apk"
test -f "$APK" || { echo "ERROR: $APK tidak ada setelah download"; exit 1; }

echo "=== Checksum ==="
sha256sum "$APK"

# --- 2. Buat release + lampirkan APK ---
gh release create "$TAG" "$APK" \
  --repo "$REPO" \
  --target main \
  --title "$TITLE" \
  --notes-file "$NOTES"

echo
echo "Release selesai: https://github.com/$REPO/releases/tag/$TAG"

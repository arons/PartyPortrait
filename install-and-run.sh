#!/bin/sh
# install-and-run.sh — download PartyPortrait from GitHub, compile and run it
#
# Usage:
#   ./install-and-run.sh [owner/repo] [version tag]
#
# Defaults: owner/repo from first argument or PARTY_PORTRAIT_REPO env var,
#           version "latest".

set -e

REPO="${1:-${PARTY_PORTRAIT_REPO:-<OWNER>/PartyPortrait}}"
VERSION="${2:-latest}"
INSTALL_DIR="$HOME/.local/PartyPortrait"

# --- download prebuilt jar from GitHub releases (or build from source) ---
mkdir -p "$INSTALL_DIR"

if command -v curl >/dev/null 2>&1; then
  FETCH="curl -fsSL"
elif command -v wget >/dev/null 2>&1; then
  FETCH="wget -qO-"
else
  echo "Error: need curl or wget to download." >&2
  exit 1
fi

if [ "$VERSION" = "latest" ]; then
  URL="https://github.com/$REPO/releases/latest/download/PartyPortrait.jar"
else
  URL="https://github.com/$REPO/releases/download/$VERSION/PartyPortrait.jar"
fi

echo "Downloading $URL ..."
if $FETCH "$URL" > "$INSTALL_DIR/PartyPortrait.jar" 2>/dev/null; then
  echo "Downloaded prebuilt jar."
else
  echo "Prebuilt jar not found, building from source..."
  TMP="$(mktemp -d)"
  git clone --depth 1 "https://github.com/$REPO.git" "$TMP/src"
  ( cd "$TMP/src" && mkdir -p bin && javac ./src/PartyPictures.java -d ./bin )
  jar --create --file "$INSTALL_DIR/PartyPortrait.jar" --main-class PartyPictures -C "$TMP/src/bin" .
  rm -rf "$TMP"
fi

mkdir -p "$INSTALL_DIR/photos"

echo "Starting PartyPortrait (click mouse or press 'e' to exit)..."
exec java -jar "$INSTALL_DIR/PartyPortrait.jar"

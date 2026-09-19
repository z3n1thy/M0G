#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
VERSION=8.2
CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-$VERSION-bin"
DIST="$CACHE/gradle-$VERSION-bin.zip"
INSTALL="$CACHE/gradle-$VERSION"
if [ ! -x "$INSTALL/bin/gradle" ]; then
  mkdir -p "$CACHE"
  if [ ! -f "$DIST" ]; then
    curl -fsSL "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip" -o "$DIST"
  fi
  rm -rf "$INSTALL.tmp"
  mkdir "$INSTALL.tmp"
  unzip -q "$DIST" -d "$INSTALL.tmp"
  mv "$INSTALL.tmp/gradle-$VERSION" "$INSTALL"
  rmdir "$INSTALL.tmp"
fi
exec "$INSTALL/bin/gradle" "$@"

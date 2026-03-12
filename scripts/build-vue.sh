#!/usr/bin/env bash
# 将 man-vue 打包并复制到 man 的 static，便于 man 直接提供管理后台页面。
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
MAN_VUE_DIR="$ROOT_DIR/man-vue"
MAN_STATIC_DIR="$ROOT_DIR/man/src/main/resources/static"

cd "$MAN_VUE_DIR"
echo "[build-man-vue] Installing dependencies (if needed) and building..."
npm ci 2>/dev/null || npm install
npm run build

echo "[build-man-vue] Copying dist to man/src/main/resources/static ..."
rm -rf "$MAN_STATIC_DIR"
mkdir -p "$MAN_STATIC_DIR"
cp -r "$MAN_VUE_DIR/dist/"* "$MAN_STATIC_DIR/"

echo "[build-man-vue] Done. Start man with: mvn -pl man spring-boot:run"

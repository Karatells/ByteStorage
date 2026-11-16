#!/usr/bin/env bash
set -euo pipefail

# Скрипт сборки Simple Byte Storage (основной мод + аддон scripting) в отдельную директорию.
# Использование: ./build-dist.sh [директория_вывода]
# По умолчанию собирает в ./dist

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT_DIR="${1:-$ROOT_DIR/dist}"

cd "$ROOT_DIR"

# 1. Получаем версию из build.gradle
GRADLE_VERSION="$(sed -n "s/^version\s*=\s*'\([^']*\)'.*/\1/p" build.gradle | head -n 1)"
if [[ -z "$GRADLE_VERSION" ]]; then
    echo "Ошибка: не удалось прочитать version из build.gradle" >&2
    exit 1
fi

# 2. Определяем номер билда из версии (если 4 цифры) или из CHANGELOG.md / git
if [[ "$GRADLE_VERSION" =~ ^([0-9]+\.[0-9]+\.[0-9]+)\.([0-9]+)$ ]]; then
    BASE_VERSION="${BASH_REMATCH[1]}"
    BUILD_NUM="${BASH_REMATCH[2]}"
    FULL_VERSION="${BASE_VERSION}+build.${BUILD_NUM}"
else
    BASE_VERSION="$GRADLE_VERSION"
    # Ищем последний номер билда в CHANGELOG.md для текущей версии (например, 1.2.1.32)
    BUILD_NUM="$(grep -oE "${BASE_VERSION//./\\.}\.[0-9]+" CHANGELOG.md | head -n 1 | awk -F. '{print $NF}' || true)"
    if [[ -z "$BUILD_NUM" ]] && command -v git >/dev/null 2>&1 && git rev-parse --git-dir >/dev/null 2>&1; then
        BUILD_NUM="$(git rev-list --count HEAD)"
    fi
    BUILD_NUM="${BUILD_NUM:-0}"
    FULL_VERSION="${BASE_VERSION}+build.${BUILD_NUM}"
fi

echo "==> Сборка Simple Byte Storage: версия $FULL_VERSION (Gradle version: $GRADLE_VERSION)"
./gradlew :jar :scripting:jar --no-daemon

CORE_JAR="$ROOT_DIR/build/libs/simple_bytestorage-${GRADLE_VERSION}.jar"
SCRIPTING_JAR="$ROOT_DIR/scripting/build/libs/simple_bytestorage_scripting-${GRADLE_VERSION}.jar"

if [[ ! -f "$CORE_JAR" ]]; then
    echo "Ошибка: не найден JAR основного мода: $CORE_JAR" >&2
    exit 1
fi
if [[ ! -f "$SCRIPTING_JAR" ]]; then
    echo "Ошибка: не найден JAR аддона scripting: $SCRIPTING_JAR" >&2
    exit 1
fi

mkdir -p "$OUT_DIR"

OUT_CORE="$OUT_DIR/simple_bytestorage-${FULL_VERSION}.jar"
OUT_SCRIPTING="$OUT_DIR/simple_bytestorage_scripting-${FULL_VERSION}.jar"

cp -f "$CORE_JAR" "$OUT_CORE"
cp -f "$SCRIPTING_JAR" "$OUT_SCRIPTING"

echo "==> Готово! Оба файла скопированы в: $OUT_DIR"
ls -lh "$OUT_CORE" "$OUT_SCRIPTING"

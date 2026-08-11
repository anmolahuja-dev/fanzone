#!/bin/bash
# ============================================================
# Build / Test Fanzone MVP locally
#
# Usage:
#   ./build.sh              # build + tests (default)
#   ./build.sh build        # build only, skip tests
#   ./build.sh test         # run tests only (no compile)
#   ./build.sh all          # build + tests (explicit)
#   ./build.sh compile      # compile only (no package/install)
#
# Extra Maven flags can be appended:
#   ./build.sh build -pl feed-service
#   ./build.sh test -Dtest=FeedScorerPropertyTest
# ============================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Ensure Java 21 is used (via sdkman or explicit path)
if [ -d "$HOME/.sdkman/candidates/java/21.0.5-tem" ]; then
    export JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.5-tem"
    export PATH="$JAVA_HOME/bin:$PATH"
fi

# Use project-local settings.xml to avoid global corporate repo interference
MVN_SETTINGS="-s $PROJECT_DIR/.m2/settings.xml"

# Parse first argument as mode, default to "all"
MODE="all"
if [ $# -gt 0 ]; then
    case "$1" in
        build|test|all|compile)
            MODE="$1"
            shift
            ;;
    esac
fi

cd "$PROJECT_DIR"

case "$MODE" in
    compile)
        echo "=== Compiling Fanzone (no tests, no package) ==="
        mvn $MVN_SETTINGS clean compile "$@"
        ;;
    build)
        echo "=== Building Fanzone (skip tests) ==="
        mvn $MVN_SETTINGS clean install -Dmaven.test.skip=true "$@"
        ;;
    test)
        echo "=== Running tests for Fanzone ==="
        mvn $MVN_SETTINGS test "$@"
        ;;
    all)
        echo "=== Building + Testing Fanzone ==="
        mvn $MVN_SETTINGS clean verify "$@"
        ;;
esac

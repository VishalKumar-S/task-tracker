#!/bin/bash

set -e

echo "================================"
echo "🚀 Pre-Push Checks Starting..."
echo "================================"

# Function to run checks for a service
run_checks() {
    local service=$1
    echo ""
    echo "================================"
    echo "📦 Checking: $service"
    echo "================================"

    cd "$service"

    # 1. Format code (auto-fix)
    echo "→ Formatting code..."
    sbt scalafmtAll scalafmtSbt

    # 2. Check formatting (verify)
    echo "→ Verifying code formatting..."
    sbt scalafmtCheckAll scalafmtSbtCheck

    # 3. Compile (catch compilation errors)
    echo "→ Compiling..."
    sbt compile Test/compile

    # 4. Run tests (NO coverage report needed locally)
    echo "→ Running tests..."
    sbt test

    # 5. Static analysis (optional warnings, don't fail on warnings)
    echo "→ Running static analysis..."
    sbt scapegoat || echo "⚠️  Scapegoat warnings found (non-blocking)"

    cd ..

    echo "✅ $service checks passed!"
}

# Check if notification-proto needs to be rebuilt
echo ""
echo "================================"
echo "📦 Checking: notification-proto"
echo "================================"

PROTO_JAR="$HOME/.ivy2/local/com.example/notification-proto_2.13/0.1.0/jars/notification-proto_2.13.jar"

if [ -f "$PROTO_JAR" ]; then
    echo "ℹ️  Proto jar already exists in Ivy cache"
    echo "→ Checking if proto has changes..."

    # Check if proto files were modified
    cd notification-proto
    if git diff --quiet HEAD -- . 2>/dev/null; then
        echo "✅ Proto unchanged, skipping rebuild"
        cd ..
    else
        echo "→ Proto has changes, rebuilding..."
        sbt clean compile publishLocal
        cd ..
        echo "✅ Proto rebuilt and published"
    fi
else
    echo "→ Proto jar not found, building..."
    cd notification-proto
    sbt clean compile publishLocal
    cd ..
    echo "✅ Proto built and published"
fi

# Check services
run_checks "notification-service"
run_checks "tasks-service"

echo ""
echo "================================"
echo "🎉 All pre-push checks passed!"
echo "================================"
echo ""
echo "✅ Code formatted"
echo "✅ Everything compiles"
echo "✅ All tests pass"
echo "✅ Static analysis complete"
echo ""
echo "You're safe to push! 🚀"
echo ""
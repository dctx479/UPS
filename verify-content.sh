#!/bin/bash
# Content Verification Script
# Run this before committing to ensure compliance

echo "=================================================="
echo "  Project Content Verification"
echo "  Checking for prohibited content..."
echo "=================================================="
echo ""

FOUND_ISSUES=0

# Exception files (policy definition files)
EXCEPTION_PATTERN="\.project-rules|verify-content\.sh|COMPLIANCE\.md"

# Check all source files
echo "[1/4] Checking Java files..."
JAVA_FILES=$(find backend -name "*.java" -exec grep -l "claude\|Claude\|CLAUDE\|anthropic\|Anthropic\|ANTHROPIC" {} \; 2>/dev/null || true)
if [ ! -z "$JAVA_FILES" ]; then
    echo "❌ Found prohibited content in Java files:"
    echo "$JAVA_FILES"
    FOUND_ISSUES=1
else
    echo "✅ Java files clean"
fi

echo ""
echo "[2/4] Checking documentation files..."
DOC_FILES=$(find . -name "*.md" -not -name "COMPLIANCE.md" -exec grep -l "claude\|Claude\|CLAUDE\|anthropic\|Anthropic\|ANTHROPIC" {} \; 2>/dev/null | grep -v ".git" || true)
if [ ! -z "$DOC_FILES" ]; then
    echo "❌ Found prohibited content in documentation:"
    echo "$DOC_FILES"
    FOUND_ISSUES=1
else
    echo "✅ Documentation clean"
fi

echo ""
echo "[3/4] Checking configuration files..."
CONFIG_FILES=$(find backend -name "*.yml" -o -name "*.yaml" -exec grep -l "claude\|Claude\|CLAUDE\|anthropic\|Anthropic\|ANTHROPIC" {} \; 2>/dev/null || true)
if [ ! -z "$CONFIG_FILES" ]; then
    echo "❌ Found prohibited content in configuration:"
    echo "$CONFIG_FILES"
    FOUND_ISSUES=1
else
    echo "✅ Configuration files clean"
fi

echo ""
echo "[4/4] Checking Flutter files..."
FLUTTER_FILES=$(find flutter-app -name "*.dart" -exec grep -l "claude\|Claude\|CLAUDE\|anthropic\|Anthropic\|ANTHROPIC" {} \; 2>/dev/null || true)
if [ ! -z "$FLUTTER_FILES" ]; then
    echo "❌ Found prohibited content in Flutter files:"
    echo "$FLUTTER_FILES"
    FOUND_ISSUES=1
else
    echo "✅ Flutter files clean"
fi

echo ""
echo "=================================================="
if [ $FOUND_ISSUES -eq 0 ]; then
    echo "✅ VERIFICATION PASSED"
    echo "Project is clean and ready for commit"
    echo "=================================================="
    exit 0
else
    echo "❌ VERIFICATION FAILED"
    echo "Please remove all prohibited content before committing"
    echo "See .project-rules for details"
    echo "=================================================="
    exit 1
fi

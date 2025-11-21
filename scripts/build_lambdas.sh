#!/bin/bash
set -e

# Build all Lambda functions
# Usage: ./build_lambdas.sh

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

FUNCTIONS=(
    "scheduled_poster"
    "reaction_handler"
    "schedule_creator"
    "conversation_analyzer"
)

echo "Building all Lambda functions..."
echo "================================"

for func in "${FUNCTIONS[@]}"; do
    echo ""
    echo "Building $func..."
    "$SCRIPT_DIR/build_lambda.sh" "$func"
done

echo ""
echo "================================"
echo "✅ All Lambda functions built successfully!"
echo ""
ls -lh "$SCRIPT_DIR/../dist/"

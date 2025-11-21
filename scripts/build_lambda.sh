#!/bin/bash
set -e

# Lambda function build script
# Usage: ./build_lambda.sh <function_name>

FUNCTION_NAME=$1

if [ -z "$FUNCTION_NAME" ]; then
    echo "Error: Function name is required"
    echo "Usage: ./build_lambda.sh <function_name>"
    echo "Example: ./build_lambda.sh scheduled_poster"
    exit 1
fi

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/.."
BUILD_DIR="$PROJECT_ROOT/build/$FUNCTION_NAME"
DIST_DIR="$PROJECT_ROOT/dist"
LAMBDA_DIR="$PROJECT_ROOT/src/lambdas/$FUNCTION_NAME"
SHARED_DIR="$PROJECT_ROOT/src/shared"
DATA_DIR="$PROJECT_ROOT/src/data"

echo "Building Lambda function: $FUNCTION_NAME"

# Clean build directory
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"
mkdir -p "$DIST_DIR"

# Install dependencies to build directory
echo "Installing dependencies..."
pip install -r "$PROJECT_ROOT/requirements.txt" -t "$BUILD_DIR" --quiet

# Copy Lambda function code
echo "Copying Lambda function code..."
cp "$LAMBDA_DIR/handler.py" "$BUILD_DIR/"
if [ -f "$LAMBDA_DIR/__init__.py" ]; then
    cp "$LAMBDA_DIR/__init__.py" "$BUILD_DIR/"
fi

# Copy shared modules
echo "Copying shared modules..."
mkdir -p "$BUILD_DIR/shared"
cp "$SHARED_DIR"/*.py "$BUILD_DIR/shared/"

# Copy data files (topics.json)
if [ -d "$DATA_DIR" ]; then
    echo "Copying data files..."
    mkdir -p "$BUILD_DIR/data"
    cp -r "$DATA_DIR"/* "$BUILD_DIR/data/"
fi

# Create deployment package
echo "Creating deployment package..."
cd "$BUILD_DIR"
zip -r "$DIST_DIR/lambda-$FUNCTION_NAME.zip" . -q

# Clean up build directory
cd "$PROJECT_ROOT"
rm -rf "$BUILD_DIR"

echo "✅ Lambda package created: dist/lambda-$FUNCTION_NAME.zip"
ls -lh "$DIST_DIR/lambda-$FUNCTION_NAME.zip"

#!/bin/bash
set -e

# Project Roots
FRONTEND_DIR=$(pwd)
API_DIR=$(dirname "$FRONTEND_DIR")/front-api

# Check if we are in the right place
if [[ ! -f "$FRONTEND_DIR/package.json" ]]; then
    echo "Error: Please run this script from the frontend directory."
    exit 1
fi

echo "=================================================="
echo "Starting E2E Local Workflow"
echo "=================================================="

# Cleanup function to kill background processes on exit
cleanup() {
    echo ""
    echo "=================================================="
    echo "Stopping background processes..."
    if [[ -n "$API_PID" ]]; then
        echo "Killing Front-API (PID: $API_PID)"
        kill "$API_PID" || true
    fi
    if [[ -n "$FRONT_PID" ]]; then
        echo "Killing Frontend (PID: $FRONT_PID)"
        kill "$FRONT_PID" || true
    fi
    echo "Done."
    echo "=================================================="
}
trap cleanup EXIT

# 1. Start Front-API
echo "[1/5] Starting Front-API (devsec, local)..."
cd "$API_DIR"
mvn spring-boot:run -Dspring-boot.run.profiles=devsec,local > "$FRONTEND_DIR/e2e-api.log" 2>&1 &
API_PID=$!
echo "Front-API PID: $API_PID"

# Wait for API to be ready
echo "Waiting for Front-API to be ready on port 8086..."
MAX_RETRIES=60
count=0
while ! nc -z localhost 8086; do
    sleep 2
    count=$((count+1))
    if [ $count -ge $MAX_RETRIES ]; then
        echo "Error: Front-API failed to start within timeout."
        cat "$FRONTEND_DIR/e2e-api.log"
        exit 1
    fi
    echo -n "."
done
echo " Front-API is UP!"

# 2. Generate API Client
echo "[2/5] Generating API Client..."
cd "$FRONTEND_DIR"
pnpm generate:api-local

# 3. Build Frontend
echo "[3/5] Building Frontend..."
pnpm build

# 4. Start Frontend
echo "[4/5] Starting Frontend..."
# Use 'pnpm preview' to serve the built application, or 'pnpm dev' if build is not strictly required but we did build.
# 'pnpm preview' is better for E2E on build artifacts.
pnpm preview --port 3000 > "$FRONTEND_DIR/e2e-front.log" 2>&1 &
FRONT_PID=$!
echo "Frontend PID: $FRONT_PID"

# Wait for Frontend to be ready
echo "Waiting for Frontend to be ready on port 3000..."
count=0
while ! nc -z localhost 3000; do
    sleep 1
    count=$((count+1))
    if [ $count -ge $MAX_RETRIES ]; then
        echo "Error: Frontend failed to start within timeout."
        cat "$FRONTEND_DIR/e2e-front.log"
        exit 1
    fi
    echo -n "."
done
echo " Frontend is UP!"

# 5. Run Visual Tests
echo "[5/5] Running Playwright Tests..."
pnpm test:visual

echo "=================================================="
echo "E2E Workflow POST SUCCESS!"
echo "=================================================="

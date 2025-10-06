#!/bin/bash

# This script automates the generation of a local Certificate Authority (CA)
# and signed TLS certificates for both the tasks-service and notification-service,
# enabling secure mTLS communication between them.

# Exit immediately if a command exits with a non-zero status.
set -e

echo "--- Starting Certificate Generation ---"

# Define directories
ROOT_CERTS_DIR="certs"
TASKS_CERTS_DIR="tasks-service/certs"
NOTIF_CERTS_DIR="notification-service/certs"

# --- 1. Create the Root Certificate Authority (CA) ---
echo "[1/4] Generating Root Certificate Authority..."
cd "$ROOT_CERTS_DIR"

# Generate the CA's private key
openssl genrsa -out ca.key 4096

# Generate the self-signed Root CA certificate
# The double slash "//" is crucial for Git Bash on Windows to prevent path conversion.
openssl req -x509 -new -nodes -key ca.key -sha256 -days 3650 \
  -out ca.crt -subj "//CN=DevRootCA"
echo "Root CA created successfully in $ROOT_CERTS_DIR/"
cd ..

# --- 2. Generate Certificate for tasks-service ---
echo "[2/4] Generating certificate for tasks-service..."
cd "$TASKS_CERTS_DIR"
openssl genrsa -out task.key 2048
openssl req -new -key task.key -out task.csr -subj "//CN=tasks-service"
openssl x509 -req -in task.csr -CA ../../certs/ca.crt -CAkey ../../certs/ca.key \
  -CAcreateserial -out task.crt -days 365 -sha256
echo "tasks-service certificate created successfully in $TASKS_CERTS_DIR/"
cd ../..

# --- 3. Generate Certificate for notification-service ---
echo "[3/4] Generating certificate for notification-service..."
cd "$NOTIF_CERTS_DIR"
openssl genrsa -out notification.key 2048
openssl req -new -key notification.key -out notification.csr -subj "//CN=notification-service"
openssl x509 -req -in notification.csr -CA ../../certs/ca.crt -CAkey ../../certs/ca.key \
  -CAcreateserial -out notification.crt -days 365 -sha256
echo "notification-service certificate created successfully in $NOTIF_CERTS_DIR/"
cd ../..

# --- 4. Clean up Certificate Signing Requests (CSRs) ---
echo "[4/4] Cleaning up temporary .csr files..."
rm "$TASKS_CERTS_DIR/task.csr"
rm "$NOTIF_CERTS_DIR/notification.csr"

echo "--- Certificate generation complete! ---"
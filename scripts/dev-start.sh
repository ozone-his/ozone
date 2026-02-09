#!/bin/bash

# Ozone Nigeria HMO Development Start Script
# This script starts the Ozone HIS stack with the Nigeria HMO integration

set -e

echo "============================================="
echo "Starting Ozone Nigeria HMO Development Stack"
echo "============================================="

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "ERROR: Docker is not running"
    echo "Please start Docker before running this script"
    exit 1
fi

# Check if Docker Compose is available
if ! command -v docker-compose &> /dev/null; then
    echo "ERROR: Docker Compose is not available"
    echo "Please install Docker Compose"
    exit 1
fi

# Create necessary directories if they don't exist
mkdir -p logs
mkdir -p data/postgres
mkdir -p data/rabbitmq

echo "1. Starting Ozone core services..."

# Start Ozone stack using Docker Compose
if [ -f "docker-compose.override.yml" ]; then
    echo "Using existing docker-compose.override.yml"
else
    echo "Creating docker-compose.override.yml from template"
    cat <<EOF > docker-compose.override.yml
version: '3.8'

services:
  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - ./infra/docker/rabbitmq/definitions.json:/etc/rabbitmq/definitions.json
    environment:
      RABBITMQ_SERVER_ADDITIONAL_ERL_ARGS: >
        -rabbitmq_management load_definitions "/etc/rabbitmq/definitions.json"

  imis-connect:
    build:
      context: .
      dockerfile: infra/docker/imis-connect.Dockerfile
    environment:
      OPENIMIS_BASE_URL: http://openimis:8000
      OPENIMIS_AUTH: "basic"
      RABBITMQ_URI: amqp://guest:guest@rabbitmq:5672
      FHIR_STRATEGY: "R4"
    depends_on: [rabbitmq]
    ports:
      - "8085:8080"
EOF
fi

# Start services
echo "Starting services..."
docker-compose up -d

echo "2. Waiting for services to initialize..."

# Wait for services to start
echo "Waiting for RabbitMQ..."
while ! curl -s -f http://localhost:15672 > /dev/null; do
    sleep 2
    echo -n "."
done
echo "RabbitMQ is ready!"

echo "Waiting for IMIS-Connect..."
while ! curl -s -f http://localhost:8085/api/v1/health > /dev/null; do
    sleep 2
    echo -n "."
done
echo "IMIS-Connect is ready!"

echo "3. Initializing tariff data..."
./scripts/seed-tariffs.sh

echo "============================================="
echo "Ozone Nigeria HMO Stack Started Successfully"
echo "============================================="
echo ""
echo "Services available at:"
echo "- Odoo: http://localhost:8069"
echo "- OpenMRS: http://localhost:8080/openmrs"
echo "- SENAITE: http://localhost:8081"
echo "- Keycloak: http://localhost:8082"
echo "- RabbitMQ Management: http://localhost:15672 (guest/guest)"
echo "- IMIS-Connect API: http://localhost:8085/swagger-ui.html"
echo ""
echo "To stop the stack: docker-compose down"
echo "To view logs: docker-compose logs -f"

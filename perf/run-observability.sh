#!/bin/bash
echo "🚀 Starting Prometheus + Grafana..."
docker compose up -d prometheus grafana
echo "Observability stack running (Grafana:3000, Prometheus:9090)"
echo "Grafana dashboard: http://localhost:3300"
echo "Prometheus: http://localhost:9090"
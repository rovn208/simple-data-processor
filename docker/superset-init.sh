#!/bin/bash

# Install system dependencies
apt-get update
apt-get install -y gcc python3-dev

# Install ClickHouse driver
pip install clickhouse-connect

# Switch back to superset user
chown -R superset:superset /app/superset_home

# Start Superset with the superset user
su superset -c "/usr/bin/run-server.sh"
# Simple Data Processor

A robust real-time analytics pipeline using Kafka, Kafka Streams, ClickHouse,
and Superset.

## Overview

This project implements a real-time event processing pipeline that:

- Consumes data from Kafka (Running data-generator to feed data respectively)
- Validates and processes data using Kafka Streams
- Handles schema evolution gracefully
- Routes invalid events to a Dead Letter Queue
- Stores processed data in ClickHouse for analytics
- Provides real-time metrics through Superset dashboards

### Components

- **Kafka**: Message broker for event consumption
- **Kafka Streams**: Stream processing for validation and aggregation
- **ClickHouse**: Analytics database
- **Superset**: Visualization platform

## Getting Started

### Prerequisites

- [Docker and Docker Compose](https://www.docker.com/)
- [Java 21](https://adoptium.net/download/)
- [Python 3.8+](https://www.python.org/downloads/)
  - Venv configured for Macos User
- [Task](https://taskfile.dev/) (task runner)

### Quick Start

1. Clone the repository:

```bash
git clone https://github.com/rovn208/simple-data-processor.git
cd simple-data-processor
```

2. Start the infrastructure:

```bash
task setup
```

Then all services will be available at:

- Superset: localhost:8088
- Kafka: localhost:9092
- Kafka-UI: localhost:8080
- Clickhouse: localhost:8123

3. Run the application:

```bash
task run
```

4. Generate test data:

```bash
task data-generator:start
```

### Configuration

Key configuration files:

- `application.yml`: Main application configuration
- `docker/docker-compose.yml`: Infrastructure setup
- `scripts/clickhouse/init.sql`: ClickHouse schema
- `scripts/kafka/create-topics.sh`: Kafka topic creation

## Data Flow

1. **Event Consumption**
   - Events are published to `my_events` Kafka topic
   - Schema includes: event_type, user_id, page_url, button_id, timestamp,
     country, device_type

2. **Data Processing**
   - Validation of user_id and country codes
   - Invalid events routed to DLQ
   - Rolling averages calculated per country

3. **Data Storage**
   - Valid events stored in ClickHouse
   - Pre-aggregated metrics available through materialized views
   - DLQ events stored for analysis

## Development

### Building

```bash
task build
```

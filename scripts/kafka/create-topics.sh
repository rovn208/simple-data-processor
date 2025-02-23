#!/bin/bash
docker exec -i kafka kafka-topics --create --if-not-exists \
  --bootstrap-server localhost:9092 \
  --topic my_events \
  --partitions 3 \
  --replication-factor 1

docker exec -i kafka kafka-topics --create --if-not-exists \
  --bootstrap-server kafka:9092 \
  --topic enriched_events \
  --partitions 3 \
  --replication-factor 1

docker exec -i kafka kafka-topics --create --if-not-exists \
  --bootstrap-server kafka:9092 \
  --topic dlq \
  --partitions 1 \
  --replication-factor 1

docker exec -i kafka kafka-topics --create --if-not-exists \
    --bootstrap-server kafka:9092 \
    --topic country_event_averages \
    --partitions 3 \
    --replication-factor 1

# List all topics
echo "Created topics:"
docker exec -i kafka kafka-topics --list --bootstrap-server localhost:9092

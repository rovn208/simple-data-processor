# Design Decision Records

## Overview

This document describes the design decision records for the Simple Data
Processor.

## Data types

### Event

```json
{
    "event_type": "string",
    "user_id": "int64",
    "page_url": "string",
    "button_id": "string",
    "timestamp": "datetime"
}
```

### Country Event Average

```json
{
    "country": "string",
    "window_start": "datetime",
    "window_end": "datetime",
    "average_events": "float64"
}
```

### Daily Active Users

```json
{
    "event_date": "datetime",
    "dau": "int64"
}
```

### Event Counts by Country

```json
{
    "country": "string",
    "event_count": "int64"
}
```

### Event Counts by Page URL

```json
{
    "page_url": "string",
    "event_count": "int64"
}
```

### DLQ Records

```json
{
    "timestamp": "datetime",
    "error_message": "string",
    "raw_data": "string"
}
```

## Kafka Streams aggregation vs ClickHouse materialized views

Kafka Streams aggregation is a good choice for real-time aggregation of events
from Kafka. However, it requires additional cost to re-calculate, handle updates
to exisiting aggregations and ensure consistency between raw and aggregated
data.

- Additional cost to re-calculate
- Better handling of late-arriving data
- Complex transformation and business logic
- Handle updates to exisiting aggregations
- Ensure consistency between raw and aggregated data

---

On the other hand, ClickHouse materialized views automatically maintain these
aggregations without additional cost to re-calculate, handle updates to
exisiting aggregations and ensure consistency between raw and aggregated data.

- No additional cost to re-calculate
- Less flexible windowing options
- Basic aggregation operations only
- No need to handle updates to exisiting aggregations
- No need to ensure consistency between raw and aggregated data

---

Since we only consume events from Data Generator, I let ClickHouse materialized
views maintain the daily active users as it just a simple grouping query.

## Database schema

Since we need to consume events from Kafka and put them into ClickHouse, we need
to create some tables additionally to store events and aggregated data beside
some tables provided on the task.

In addition, ClickHouse is well-known for its performance on real-time
aggregation and its metarialized views automatically maintain these aggregations
without additional cost to re-calculate, handle updates to exisiting
aggregations and ensure consistency between raw and aggregated data.

- `events` table: store events from Kafka
- `events_queue` table: Kafka source table for events
- `events_queue_mv` materialized view: populate `events` table from
  `events_queue` table

- `country_event_averages_raw` table: store raw data for country event averages
- `country_event_averages_queue` table: Kafka source table for country event
  averages
- `country_event_averages_mv` materialized view: populate
  `country_event_averages_raw` table from `country_event_averages_queue` table

- `daily_active_users_final` table: store final data for daily active users
- `daily_active_users_mv` materialized view: populate `daily_active_users_final`
  table from `events` table

- `dlq_table` table: store DLQ records
- `dlq_queue` table: Kafka source table for DLQ records
- `dlq_mv` materialized view: populate `dlq_table` table from `dlq_queue` table

## Data quality handling

We should always have a fallback place to put data that cannot be processed. In
our case, invalid events should be put into DLQ.

## Schema Evolution

### Current Schema

```json
{
    "event_type": "string",
    "user_id": "int64",
    "page_url": "string",
    "button_id": "string",
    "timestamp": "datetime",
    "country": "string",
    "device_type": "string?"
}
```

### Evolution Strategies

#### Forward Compatibility

- Using `@JsonIgnoreProperties(ignoreUnknown = true)` to handle new fields
- New fields must be optional
- Existing fields cannot change type

#### Backward Compatibility

- All new fields must be nullable
- Example: `device_type` was added as nullable field

#### Best Practices

1. Never remove fields, mark as deprecated instead
2. Always add new fields as nullable
3. Test both old and new message formats
4. Update documentation when schema changes

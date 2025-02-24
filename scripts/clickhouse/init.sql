-- Create the database
CREATE DATABASE IF NOT EXISTS events_db;

-- Switch to the database
USE events_db;

-- Create the events table
CREATE TABLE IF NOT EXISTS events
(
    event_type  String,
    user_id     Int64,
    page_url    String,
    button_id   String,
    timestamp   DATETIME('UTC'),
    country     String,
    device_type Nullable(String)
) ENGINE = MergeTree()
      ORDER BY (timestamp, country, user_id);

-- Create the Kafka source table for events
CREATE TABLE IF NOT EXISTS events_queue
(
    event_type  String,
    user_id     Int64,
    page_url    String,
    button_id   String,
    timestamp   DateTime('UTC'),
    country     String,
    device_type Nullable(String)
) ENGINE = Kafka
      SETTINGS kafka_broker_list = 'kafka:29092',
          kafka_topic_list = 'enriched_events',
          kafka_group_name = 'clickhouse_events_consumer',
          kafka_format = 'JSONEachRow',
          kafka_skip_broken_messages = 1;

-- Create materialized view to populate events table
CREATE MATERIALIZED VIEW IF NOT EXISTS events_queue_mv TO events AS
SELECT *
FROM events_queue;

-- Create the country event averages table
CREATE TABLE IF NOT EXISTS country_event_averages_raw
(
    country        String,
    window_start   DateTime('UTC'),
    window_end     DateTime('UTC'),
    average_events Float64
) ENGINE = MergeTree()
      ORDER BY (window_start, country);

-- Create Kafka source table for country event averages
CREATE TABLE IF NOT EXISTS country_event_averages_queue
(
    country        String,
    window_start   DateTime('UTC'),
    window_end     DateTime('UTC'),
    average_events Float64
) ENGINE = Kafka
      SETTINGS kafka_broker_list = 'kafka:29092',
          kafka_topic_list = 'country_event_averages',
          kafka_group_name = 'clickhouse_averages_consumer',
          kafka_format = 'JSONEachRow',
          kafka_skip_broken_messages = 1;

-- Create materialized view for country event averages
CREATE MATERIALIZED VIEW IF NOT EXISTS country_event_averages_mv
    TO country_event_averages_raw AS
SELECT *
FROM country_event_averages_queue;

-- Create the DLQ table
CREATE TABLE IF NOT EXISTS dlq_table
(
    error_message String,
    raw_data      String,
    timestamp     DateTime('UTC') DEFAULT now()
) ENGINE = MergeTree()
      ORDER BY timestamp;

-- Create Kafka source table for DLQ
CREATE TABLE IF NOT EXISTS dlq_queue
(
    error_message String,
    raw_data      String,
    timestamp     DateTime('UTC')
) ENGINE = Kafka
      SETTINGS kafka_broker_list = 'kafka:29092',
          kafka_topic_list = 'dlq',
          kafka_group_name = 'clickhouse_dlq_consumer',
          kafka_format = 'JSONEachRow',
          kafka_skip_broken_messages = 1;

-- Create materialized view for DLQ
CREATE MATERIALIZED VIEW IF NOT EXISTS dlq_mv
    TO dlq_table AS
SELECT *
FROM dlq_queue;

-- Create daily active users table
CREATE TABLE IF NOT EXISTS daily_active_users_final
(
    event_date Date,
    country    String,
    dau        UInt32
) ENGINE = MergeTree()
      ORDER BY (event_date, country);

-- Create materialized view for daily active users
CREATE MATERIALIZED VIEW IF NOT EXISTS daily_active_users_mv
    TO daily_active_users_final AS
SELECT toDate(timestamp)       AS event_date,
       country,
       count(DISTINCT user_id) AS dau
FROM events
GROUP BY event_date, country;

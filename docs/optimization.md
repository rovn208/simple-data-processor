# Optimization

This document describes the optimization that could have done to improve the
performance of charts on Superset.

## Database schema

Based on the usage data on Superset, we can optimize the database schema by
adding indexes to the events table:

- Add `minmax` index to the `event_type` field of `events` table
- Add `bloom_filter` index to the `page_url` field of `events` table

In addition, we could create a materialized view to pre-aggregate data to
improveing performance for our queries

- Added page_url_visits_mv materialized view to pre-aggregate page URL visit
  counts
- Added event_counts_by_country_mv materialized view to pre-aggregate event
  counts by country and minute

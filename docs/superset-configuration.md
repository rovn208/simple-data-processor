# Superset configuration

## Database

First of all, you need to connect Superset with Database (Clickhouse Server).

1. Go to `Superset`
2. Go to `Settings`
3. Go to `Database Connections`
4. Click on `+ Database`
5. Select `Clickhouse`
6. At this point, you could see the `ClickHouse Connect` option under
   `Supported Databases` section as we've already installed the ClickHouse JDBC
   driver. Fill in the following fields:
   - `Database`: `events_db`
   - `Host`: `http://localhost:8123`
   - `Port`: `8123`
   - `Username`: `admin`
   - `Password`: `admin`
7. Click on `Test Connection` to make sure the connection is successful.
8. Click on `Save` to save the connection.

## Datasets

Choose the `events_db` database that we've created in the previous step and
create datasets accordingly.

## Charts

### Average of event counts per country

- Chart: Line Chart
- Dataset: `event_counts_by_country`
- X-axis: `window_start`
  - Time grain: `Minute`
- Metrics: `AVG(event_timestamp)`
- Dimensions: `country`
- Advanced analytics:
  - `Rolling function`: `mean`
  - `Periods`: `7`
  - `Min periods`: `1`
- Add margin bottom for X,Y axis for better visualization
- Rename columns of dataset to make it more readable

### Top 5 most visited page Urls

- Chart: Table
- Dataset: `events`
- Metrics: `COUNT(*) as Visit Count`
- Dimensions: `page_url`
- Sorting:
  - `Visit Count`: Descending
  - `page_url`: Ascending
- Limit: `5`
- Rename columns of dataset to make it more readable
- Disable `Show cell bars` for better visualization
- Optimize columns: set `page_url` as text align center

### DLQ records table

- Chart: Table
- Dataset: `dlq_records`
- Dimensions: `timestamp`, `error_message`, `raw_data`
- Time Grain: `Minute`
- Sort by: `max(timestamp)`
- Enable pagination for better performance and visualization
- Rename columns of dataset to make it more readable

### DAU per country over time

- Chart: Line Chart
- Dataset: `daily_active_users_final`
- X-axis: `event_date`
- Time grain: `Day`
- Metrics: `sum(dau)`
- Dimensions: `country`
- Add margin bottom for X,Y axis for better visualization

### Distribution of event types

- Chart: Pie Chart
- Dataset: `events`
- Metrics: `COUNT(*)`
- Dimensions: `event_type`
- Rename columns of dataset to make it more readable
- Enable legend for better visualization
- Put labels outside of the chart for better readability
- Pie radius 70

## Dashboard

Put layout as image at `images/dashboard_screenshot.png`

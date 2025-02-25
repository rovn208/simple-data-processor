# Issues

This document describes the issues I have encountered and the solutions I have
found.

## Cannot install Driver for ClickHouse in Superset

I followed the instructions from the Superset documentation to install the
Driver in order to connect to ClickHouse. However, I got the error:

```
Failed to build installable wheels for some pyproject.toml based projects ( lz4)
```

I did try to install SqlAlchemy instead but it did not work as well.

Then I dove a little deeper into its source code on Github at
https://github.com/apache/superset/blob/master/pyproject.toml. Try to find the
supported package versions to install additional packages. But it still didn't
work.

In the end, I have to switch to use root user to install packages then switch
back to superset user.

Details in `docker/superset-init.sh` file and Taskfile.yml file.

## Data quality issues

I just found out that the data of `country_event_averages_raw` table is not
being written to the database.

At first, I used `Long` type for `timestamp` field in `Event` class. ClickHouse
cannot convert `Long` type into `Date` type properly.

Solution:

- Use `String` type for `timestamp` field in `Event` class and apply the same
  for `window_start` and `window_end` in `CountryEventAverage` class.
- Format Datetime field to `yyyy-MM-dd HH:mm:ss` in `DateUtil.formatDate`
  method.
- Use type `DATETIME('UTC')` instead of `DATETIME` in ClickHouse schema.

# Schema Evolution Guide

## Overview

This document describes how the Simple Data Processor handles schema evolution
in event data.

## Current Schema

```json
{
  "event_type": "string",
  "user_id": "int64",
  "page_url": "string",
  "button_id": "string",
  "timestamp": "int64",
  "country": "string",
  "device_type": "string?"
}
```

## Evolution Strategies

### Forward Compatibility

- Using `@JsonIgnoreProperties(ignoreUnknown = true)` to handle new fields
- New fields must be optional
- Existing fields cannot change type

### Backward Compatibility

- All new fields must be nullable
- Example: `device_type` was added as nullable field

## Best Practices

1. Never remove fields, mark as deprecated instead
2. Always add new fields as nullable
3. Test both old and new message formats
4. Update documentation when schema changes

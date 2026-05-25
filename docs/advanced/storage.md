# Storage System

SimpleEconomy supports three backends: SQLite, MySQL, and File.

## Common Schema

| Field | Type | Description |
| --- | --- | --- |
| uuid | string | Player UUID |
| currency | string | Currency key |
| balance | double | Balance value |
| last_seen | long | Last activity timestamp |

!!! danger
    Do not manually edit `last_seen` if auto-purge is enabled.

---

## SQLite

- File: `plugins/SimpleEconomy/players.db`
- HikariCP pool, single worker thread
- Upsert on `(uuid, currency)`

```sql
CREATE TABLE IF NOT EXISTS users (
  uuid TEXT,
  currency TEXT DEFAULT 'money',
  balance REAL DEFAULT 0,
  last_seen BIGINT NOT NULL,
  PRIMARY KEY (uuid, currency)
);
```

---

## MySQL

- Table: `<table-prefix>players`
- Same schema as SQLite

```sql
CREATE TABLE IF NOT EXISTS `se_players` (
  uuid VARCHAR(36) NOT NULL,
  currency VARCHAR(64) NOT NULL DEFAULT 'money',
  balance DOUBLE NOT NULL,
  last_seen BIGINT NOT NULL,
  PRIMARY KEY (uuid, currency)
);
```

---

## File (YAML)

- Folder: `plugins/SimpleEconomy/player_data/`
- File per player: `<uuid>.yml`

```yaml
balances:
  money: 1000
  gems: 25
last_seen: 1716123456789
```

!!! tip
    File storage is simple but not ideal for large servers.

# Logging System

SimpleEconomy provides two logging channels:

1. Transaction Logger (local DB)
2. Discord Webhook

---

## Transaction Logger

- File: `plugins/SimpleEconomy/transactions.db`
- Rotates when the size limit is exceeded
- Table includes the `currency` column

```sql
CREATE TABLE IF NOT EXISTS transactions (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  timestamp INTEGER NOT NULL,
  player_uuid VARCHAR(36) NOT NULL,
  currency VARCHAR(64) NOT NULL DEFAULT 'money',
  type VARCHAR(32) NOT NULL,
  amount REAL NOT NULL,
  balance_before REAL NOT NULL,
  balance_after REAL NOT NULL,
  target_uuid VARCHAR(36) NOT NULL
);
```

!!! tip
    Use `/ehistory <player> [limit] [currency]` to fetch audit entries.

---

## Discord Webhook

- Enable: `settings.enable-discord-logging: true`
- Configure: `webhook-settings.*`

Tracked events:
- pay
- remove
- give/set
- withdraw (voucher)

!!! warning
    If the webhook URL is empty or a placeholder, no payloads are sent.

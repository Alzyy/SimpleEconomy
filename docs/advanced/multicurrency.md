# Multicurrency

SimpleEconomy stores balances per `(uuid, currency)`. Every operation depends on a currency key.

## Currency Keys

- Default: `money`
- Custom: defined in `currencies.yml`
- Normalization: lowercase, spaces to underscore

!!! note
    Storage always uses the currency key. Keep keys stable to avoid orphaned data.

## Storage Layout

- SQLite/MySQL: primary key `(uuid, currency)`
- File: `balances.<currency>`

## Effects on the API

All API calls include a currency key:

```java
provider.deposit(playerId, "gems", 100.0);
provider.getBalance(playerId, "gems").thenAccept(...);
```

## Effects on Commands

- `/pay` and `/eco` operate on `money`.
- Custom currencies expose their own command alias (currency name).

!!! warning
    Ensure the currency key used in commands and API matches the key in `currencies.yml`.

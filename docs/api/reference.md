# API Overview

## SimpleEconomyAPI

Central access point for the API.

- `SimpleEconomyAPI.setProvider(EconomyProvider)`
  Registers the provider (single-use).

- `SimpleEconomyAPI.getProvider()`
  Returns the registered provider.

!!! warning
    `setProvider` can only be called once, otherwise an `IllegalStateException` is thrown.

---

## EconomyProvider

All operations are asynchronous and return `CompletableFuture`.

### Key Methods

- `getBalance(UUID, String currency)`
- `setBalance(UUID, String currency, double amount)`
- `deposit(UUID, String currency, double amount)`
- `detract(UUID, String currency, double amount)` -> `CompletableFuture<Boolean>`
- `hasAccount(UUID)`
- `hasEnough(UUID, String currency, double amount)`
- `transfer(UUID from, UUID to, String currency, double amount)` -> `TransactionResult`
- `getAvailableVirtualCurrencies()`

### Basic Usage

```java
UUID playerId = player.getUniqueId();
EconomyProvider provider = SimpleEconomyAPI.getProvider();

provider.getBalance(playerId, "money")
    .thenAccept(balance -> {
        // Only non-Bukkit logic here
        System.out.println("Balance: " + balance);
    })
    .exceptionally(ex -> {
        ex.printStackTrace();
        return null;
    });
```

### Return to Main Thread

```java
provider.getBalance(playerId, "money")
    .thenAccept(balance -> {
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.sendMessage("Balance: " + balance);
        });
    });
```

---

## TransactionResult

- `SUCCESS`
- `INSUFFICIENT_FUNDS`
- `ERROR`

---

## Multicurrency in the API

Every call must include a currency key:

```java
provider.deposit(playerId, "gems", 100.0);
provider.getBalance(playerId, "gems").thenAccept(...);
```

!!! tip
    Store currency keys in config or use `getAvailableVirtualCurrencies()`.

---

## PlaceholderAPI

If PlaceholderAPI is present, use:

- `%seco_balance_formatted%`
- `%seco_balance_normal%`
- `%seco_top_position%`
- `%seco_baltop_1%`, `%seco_baltop_1_balance%`
- `%seco_currency_<currencyKey>%` (example: `%seco_currency_gems%`)

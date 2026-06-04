# Configuration

## Main Files

- `config.yml` (core settings)
- `currencies.yml` (virtual currencies)
- `languages/*.yml` (messages)

## config.yml

=== "Core Settings"
    ```yaml
    settings:
      locale: "en"
      starting-balance: 1000
      currency-symbol: "$"
      storage-system: SQLITE
      auto-save-time: 30
      max-thread-pool-size: 2
      enable-vouchers: true
      baltop-refresh-interval: 20
      baltop-limit: 10
      check-for-updates: true
      update-check-interval: 1440
      use-placeholderapi: true
      enable-balance-formatting: true
      max-transaction-amount: 0
      enable-action-bar-messages: false
      enable-discord-logging: false
    ```

=== "Storage"
    ```yaml
    storage:
      enable-auto-purge: true
      auto-purge-days: 30
    ```

=== "Transaction Logger"
    ```yaml
    transaction-logger:
      enable-logger: true
      file-size-limit-mb: 10
    ```

=== "Database (MySQL)"
    ```yaml
    database:
      host: "localhost"
      username: "root"
      password: "CHANGEME"
      database: "simpleeconomy"
      table-prefix: "se_"
      max-connection-pool: 10
      port: 3306
    ```

=== "Discord Webhook"
    ```yaml
    webhook-settings:
      url: "https://discord.com/api/webhooks/your_webhook_url"
      username: "SimpleEconomy Logger"
      avatar-url: "https://example.com/avatar.png"
      color: "#22C55E"
      log-payments: true
      log-withdrawals: true
      log-admin: true
      log-voucher-creations: true
    ```

=== "Interests"
    ```yaml
    interests:
      enabled: false
      rate: 0.05
      max-interest: 2000
      interval: 60
      min-balance: 100
    ```

!!! tip
    `max-transaction-amount: 0` disables the limit. Values above 0 enforce a cap for `/pay`.

## currencies.yml

Default file is empty. Add currencies like:

```yaml
gems:
  name: "Gems"
  symbol: "<>"
tokens:
  name: "Tokens"
  symbol: "T"
```

!!! note
    The currency key used in storage is the YAML key (e.g. `gems`).

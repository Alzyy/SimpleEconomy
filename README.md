<div align="center">

<img src="icon.svg" alt="SimpleEconomy Logo" width="50%"/>

# SimpleEconomy

**A modular, multi-currency, secure economy system for Minecraft 1.21+ servers**

![Spigot](https://img.shields.io/badge/Spigot-1.21-orange)
![Paper](https://img.shields.io/badge/Paper-Compatible-blue)
![Java](https://img.shields.io/badge/Java-21-red)
![Vault](https://img.shields.io/badge/Vault-Supported-green)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

</div>

---

## Description

**SimpleEconomy** is an economy plugin designed for high performance, data reliability, and maximum flexibility, without the overhead typical of monolithic all-in-one economy suites. It is built on a modular architecture split into two components: an independent **API**, consumable by other plugins via Maven/Gradle, and a **core plugin** that implements storage, commands, and integration with the Bukkit/Paper ecosystem.

The system manages **multiple currencies** (configurable via `currencies.yml`), supports three interchangeable persistence backends (SQLite, MySQL, YAML files), and includes a standalone transaction logging system, designed for auditing, accounting, and economic debugging at scale.

Every additional component — interest, vouchers, baltop, Discord webhooks, update checks — is handled by dedicated asynchronous tasks, keeping the server's main thread free from blocking operations.

---

## Feature List

- ⚙️ **Modular architecture** — A separate `api` module decoupled from the `plugin`, with a `ModuleLoader` that loads external extensions (`.jar` files containing a `module.yml`) through a dedicated `URLClassLoader`, without polluting the main classpath.
- 💰 **Native multi-currency support** — Manage multiple currencies defined in `currencies.yml`, each with its own symbol, formatting, and independent transaction history.
- 🔒 **Secure Transaction Logger** — Every operation (`pay`, `give`, `set`, `remove`, `withdraw`) is recorded in a dedicated SQLite database (via HikariCP), with balance before/after, timestamp, and sender/receiver UUIDs. Includes automatic log rotation once a configurable size threshold is reached, preventing uncontrolled database growth.
- 🗄️ **Interchangeable storage** — Three ready-to-use backends: `SQLITE`, `MYSQL`, `FILE`, selectable from `config.yml` with a configurable connection pool.
- 🔁 **Asynchronous caching** — A Caffeine-based cache layer reduces repeated queries and ensures instant responses to commands.
- 💳 **Voucher/check system** — Convert player balance into physical items (`/withdraw`), with customizable item, dynamic lore, and a configurable maximum amount.
- 📈 **Periodic interest** — A configurable task that grants percentage-based interest on player balances, with a minimum balance requirement and a per-payout cap to prevent runaway inflation.
- 🏆 **Scheduled baltop refresh** — The balance leaderboard is recalculated in the background at regular intervals, with no impact on real-time performance.
- 🔔 **Discord webhooks** — Real-time notifications for payments, administrative actions, and voucher creation, with customizable embeds for each event type.
- 🌍 **Full i18n support** — External language files (`lang_en.yml`, `lang_it.yml`) for complete message localization.
- 🔄 **Inactive account auto-purge** — Automatic cleanup of data for players inactive for a configurable number of days.
- 🔗 **Vault integration** — A complete implementation of the `Economy` interface, compatible with any plugin that relies on Vault.
- 🧩 **PlaceholderAPI** — A `%seco_*%` expansion for balances, top players, and formatting, ready for scoreboards, tab lists, and GUIs.
- 🛡️ **Pre/post transaction events** — `PreTransactionEvent` and `PostTransactionEvent` allow other plugins to intercept, validate, or react to every economic operation.

---

## Setup Guide

### Requirements

- Paper/Spigot **1.21** or higher
- Java **21**
- [Vault](https://www.spigotmc.org/resources/vault.34315/) (required dependency)
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (optional, but recommended)

### Installation

1. Download `SimpleEconomy.jar` from the releases page.
2. Place the file in your server's `plugins/` folder.
3. Make sure `Vault.jar` is present in the same folder.
4. Start or restart the server. The plugin will automatically generate `config.yml`, `currencies.yml`, and the `languages/` and `modules/` folders.

### Initial configuration

Open `plugins/SimpleEconomy/config.yml` and set the main parameters:

```yaml
settings:
  locale: "en"
  starting-balance: 1000
  currency-symbol: "$"
  storage-system: SQLITE   # Alternatives: MYSQL, FILE
  use-placeholderapi: true

transaction-logger:
  enable-logger: true
  file-size-limit-mb: 10
```

For multi-server environments or networks with large player counts, set `storage-system: MYSQL` and configure the `database` section with your DBMS credentials.

### Main commands

| Command | Description |
|---|---|
| `/balance [player]` | View your own balance or another player's |
| `/pay <player> <amount>` | Transfer funds to another player |
| `/eco give\|set\|remove <player> <amount>` | Administrative balance management |
| `/ehistory <player>` | Show transaction history |
| `/baltop` | Leaderboard of players by balance |
| `/voucher` | Create a physical check from your balance |
| `/currencies` | List configured currencies |
| `/se reload\|status\|diagnose` | Plugin management and diagnostics |
| `/se modules <list\|disable\|...>` | Manage external modules |

---

## API Documentation

SimpleEconomy exposes an independent `api` module, includable via Gradle/Maven, designed to let other plugins interact with the economy without depending on the internal implementation.

### Adding the dependency

```gradle
repositories {
    mavenCentral()
}

dependencies {
   implementation 'io.github.alzyy:simpleeconomy-api:VERSION'
}
```

### Accessing the provider

```java
import it.alzy.simpleeconomy.api.SimpleEconomyAPI;
import it.alzy.simpleeconomy.api.EconomyProvider;

EconomyProvider provider = SimpleEconomyAPI.getProvider();

provider.getBalance(player.getUniqueId(), "money")
        .thenAccept(balance -> {
            player.sendMessage("Your balance is: " + balance);
        });
```

### Available events

- **`PreTransactionEvent`** — Cancellable and interceptable, allows validation or blocking of a transaction before it is executed.
- **`PostTransactionEvent`** — Fired upon transaction completion, useful for external logging, rewards, or integration with other systems.

### External modules

Plugins implementing the `EconomyModule` interface can be dynamically loaded from the `plugins/SimpleEconomy/modules/` folder, provided they include a `module.yml` file with `name` and `main` fields. The `ModuleLoader` handles isolated loading via a dedicated `URLClassLoader`, ensuring each module operates within its own class namespace.

```yaml
# module.yml
name: MyModule
main: com.example.MyModule
```

---

## Compatibility

SimpleEconomy is designed for seamless integration with the existing economy plugin ecosystem:

- **Vault**: a complete implementation of the `Economy` interface. Any plugin that relies on Vault for economic operations (shops, GUIs, quests, etc.) works immediately, with no extra configuration.
- **PlaceholderAPI**: the `seco` expansion is registered automatically, providing placeholders for balances, multiple currencies, and leaderboards, usable in scoreboards, tab lists, holographic displays, and menus.

---

## Support

For bug reports, feature requests, or configuration assistance:

- 📋 **Issue Tracker**: open an issue on the project's GitHub repository
- 📖 **Wiki/Documentation**: [Here](https://alzyy.github.io/SimpleEconomy-Wiki/) 

---

## Why choose us

The Minecraft economy plugin market is dominated by monolithic solutions, built up over the years with legacy code, heavy dependencies, and configurations that take hours to adapt. **SimpleEconomy** takes a different approach:

- **Lightweight without compromise**: no unnecessary dependencies, no module loaded unless required. The dynamic module-loading system lets you add functionality only when needed, keeping the resource footprint minimal.
- **Data reliability at its core**: the Transaction Logger is not an afterthought, but a component designed for full traceability of every economic operation, with automatic rotation and dedicated indexes for fast queries even on large databases.
- **Architecture built to last**: the clean separation between API and implementation ensures that developers integrating SimpleEconomy won't need to rewrite their code with every plugin update.
- **Real scalability**: from a small SMP server using file storage, to a multi-server network with MySQL and a dedicated connection pool, the configuration adapts without requiring alternative plugins or complex migrations.
- **Operational transparency**: built-in diagnostic commands (`/se status`, `/se diagnose`) let administrators verify system health without digging through logs manually.

For a server owner, choosing SimpleEconomy means investing in a solid, verifiable, and easily extensible economic foundation — not just another plugin to manage, but the infrastructure on which to build your entire server economy.

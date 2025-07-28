
# SimpleEconomy

**SimpleEconomy** is a lightweight, high-performance economy plugin for **Spigot 1.21**.  
It supports data storage via **SQLite** or **flat file** and integrates fully with **Vault**, ensuring compatibility with other economy-based plugins.

---

## 🔧 Features

- Vault-compatible economy system
- Lightweight and optimized for performance
- Configurable data storage (SQLite or flat file)
- Core economy commands (`/eco`, `/pay`, `/balance`)
- Permission-based command access

---

## 📦 Installation

1. Download the plugin `.jar` and place it in your server’s `/plugins` folder.
2. Start or reload the server to generate the configuration files.
3. Configure the plugin in `config.yml` to choose the preferred storage method (`sqlite` or `file`).
4. Make sure you have **Vault** installed and a compatible permissions plugin (e.g., LuckPerms).

---

## 💬 Commands

### `/eco <set|give|remove> <player> <amount>`
Admin command to manage a player's balance.

- `/eco set Alzy 100`
- `/eco give Steve 50`
- `/eco remove Alex 25`

### `/pay <player> <amount>`
Allows players to send money to each other.

- `/pay Steve 10`

### `/balance` or `/bal`
Shows the player's current balance.

- `/balance`

---

## 🔐 Permissions

| Permission                        | Description                                |
|----------------------------------|--------------------------------------------|
| `simpleconomy.balance.others`    | View other players' balances               |
| `simpleconomy.eco.set`           | Use `/eco set`                             |
| `simpleconomy.eco.give`          | Use `/eco give`                            |
| `simpleconomy.eco.remove`        | Use `/eco remove`                          |
| `simpleconomy.command.reload`    | Reload the plugin                          |

---

## 📁 Configuration

Choose between `sqlite` or `file` storage in the plugin's `settings.yml`.  
Example:

```yaml
storage-system: sqlite
auto-save-time: 5 # In minutes
````

---

## ✅ Dependencies

* [Vault](https://www.spigotmc.org/resources/vault.34315/) (required)
* [LuckPerms](https://luckperms.net/) or another permissions plugin (recommended)

---

## 📄 License

GNU GPLv3 License

---

Made with ❤️ by [Alzy](https://github.com/Alzyy)

---


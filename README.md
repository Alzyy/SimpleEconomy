## **SimpleEconomy**
**SimpleEconomy** is a lightweight, high-performance economy plugin for **Spigot 1.21**.
It supports data storage via **MySQL**, **SQLite**, or **flat file**, and fully integrates with **Vault** for compatibility with other economy-based plugins.

Enjoying the plugin? Leave a review! It only takes a moment, but it means the world to me.

## **✅ Features**

* ✅ Vault-compatible economy system
* ⚡ Lightweight and optimized for performance
* Configurable data storage (MySQL, SQLite, or flat file)
* Core economy commands: /eco, /pay, /balance, /withdraw
* Permission-based command access
* Discord Webhook integration for transaction logging
* Automatic transaction logging with rotation support

## **Installation**

1. Download the plugin .jar file from the [SpigotMC page](https://www.spigotmc.org/resources/simpleeconomy).
2. Place it in your server’s `/plugins` folder.
3. Start or reload the server to generate configuration files.
4. Open `config.yml` and set your preferred storage method: `mysql`, `sqlite`, or `file`.
5. Configure the Discord Webhook URL in `config.yml` (optional).
6. Make sure **Vault** is installed, along with a permissions plugin like **LuckPerms**.

## **Commands**

### **/eco <set|give|remove> <player> <amount>**
Admin command to manage a player’s balance
**Examples:**

/eco set Alzy 100
/eco give Steve 50
/eco remove Alex 25

### **/pay <player> <amount>**
Allows players to send money to each other
**Example:**

/pay Steve 10

### **/withdraw <amount>**
Allows players to create vouchers
**Example:**

/withdraw 10

### **/balance or /bal**
Displays the player’s current balance
**Example:**

/balance

## **Permissions**

| Permission                  | Description                  |
|-----------------------------|------------------------------|
| simpleconomy.balance.others | View other players' balances |
| simpleconomy.eco.set        | Use /eco set                 |
| simpleconomy.eco.give       | Use /eco give                |
| simpleconomy.eco.remove     | Use /eco remove              |
| simpleconomy.command.reload | Reload the plugin            |

## **⚙️ Configuration**
Inside `config.yml`, choose your preferred storage system and auto-save interval:

```yaml
storage-system: sqlite
auto-save-time: 5 # In minutes
webhook-url: "your_discord_webhook_url" # Optional
log-transactions-to-discord: true # Enable or disable Discord logging
```

## **API for Developers**

### **Getting Started**
The `SimpleEconomyAPI` provides methods to interact with the economy system programmatically. Here are some examples:

#### **Get a Player's Balance**
```java
SimpleEconomyAPI api = SimpleEconomyAPI.getInstance();
OfflinePlayer player = Bukkit.getOfflinePlayer("Alzy");
CompletableFuture<Double> balanceFuture = api.getBalance(player);
balanceFuture.thenAccept(balance -> {
    System.out.println("Player's balance: " + balance);
});
```

#### **Deposit Money**
```java
api.deposit(player, 100.0).thenAccept(result -> {
    if (result == TransactionResult.SUCCESS) {
        System.out.println("Deposit successful!");
    }
});
```

#### **Withdraw Money**
```java
api.withdraw(player, 50.0).thenAccept(result -> {
    if (result == TransactionResult.SUCCESS) {
        System.out.println("Withdrawal successful!");
    }
});
```

### **Placeholders**
If you use **[PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)**, SimpleEconomy provides the following placeholders:

| Placeholder                   | Description                                                         |
|-------------------------------|---------------------------------------------------------------------|
| %seco_balance_normal%         | Returns the player’s balance as a raw double                        |
| %seco_balance_formatted%      | Returns the player’s balance in a formatted string (e.g. 10k, 1.2M) |
| %seco_top_position%           | For player position in baltop                                       |
| %seco_baltop_$number%         | For name of $number player in baltop                                |
| %seco_baltop_$number_balance% | For balance of $number player in baltop                             |

Run `/papi reload` after installing to load the placeholders.

## **Discord Webhook**
The plugin supports logging transactions to a Discord channel using webhooks. Configure the webhook URL and logging options in `config.yml`. Example:

```yaml
webhook-url: "https://discord.com/api/webhooks/..."
log-transactions-to-discord: true
log-pay-to-discord: true
log-admin-to-discord: true
log-withdrawals-to-discord: true
log-voucher-creations: true
```

## **License**
This plugin is licensed under the **GNU GPLv3 License**.

## **❤️ Credits**
Made with love by [Alzy](https://github.com/Alzyy/SimpleEconomy)

## **❗ IMPORTANT!**
This is my first plugin, and while it may not be perfect, I’ve put a lot of care into making it simple, stable, and easy to use.
I welcome suggestions, bug reports, and feature requests — feel free to open an issue or pull request on GitHub.

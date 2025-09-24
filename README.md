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

## ** Installation**

1. Download the plugin .jar file


1. Place it in your server’s /plugins folder


1. Start or reload the server to generate configuration files


1. Open config.yml and set your preferred storage method: mysql, sqlite or file


1. Make sure **Vault** is installed, along with a permissions plugin like **LuckPerms**

## ** Commands**
## **/eco <set|give|remove> <player> <amount>**
Admin command to manage a player’s balance
**Examples:**

/eco set Alzy 100
/eco give Steve 50
/eco remove Alex 25

## **/pay <player> <amount>**
Allows players to send money to each other
**Example:**

/pay Steve 10

## **/balance or /bal**
Displays the player’s current balance
**Example:**

/balance

## ** Permissions**
** 
 Permission** **Description**
simpleconomy.balance.others View other players' balances
simpleconomy.eco.set Use /eco set
simpleconomy.eco.give Use /eco give
simpleconomy.eco.remove Use /eco remove
simpleconomy.command.reload Reload the plugin
## **⚙️ Configuration**
Inside config.yml, choose your preferred storage system and auto-save interval:

storage-system: sqlite
auto-save-time: 5 # In minutes

## ** Dependencies**

* [Vault]('https://www.spigotmc.org/resources/vault.34315/') – **Required**


* [LuckPerms]('https://www.spigotmc.org/resources/luckperms.28140/') – Or any permissions plugin (**Recommended**)


* [PlaceholderAPI]('https://www.spigotmc.org/resources/placeholderapi.6245/') – For placeholder support

## **️ Placeholders**
If you use **[PlaceholderAPI]('https://www.spigotmc.org/resources/placeholderapi.6245/')**, SimpleEconomy provides the following placeholders:

** 
 Placeholder** **Description**
%seco_balance_normal% - Returns the player’s balance as a raw double
%seco_balance_formatted% - Returns the player’s balance in a formatted string (e.g. 10k, 1.2M)
%seco_top_position% - For player position in baltop
%seco_baltop_$number% - For name of $number player in baltop
%seco_baltop_$number_balance% - For balance of $number player in baltop
Run /papi reload after installing to load the placeholders.

## ** License**
This plugin is licensed under the **GNU GPLv3 License**.

## **❤️ Credits**
Made with love by [Alzy]('https://github.com/Alzyy/SimpleEconomy')

## **❗ IMPORTANT!**
This is my first plugin, and while it may not be perfect, I’ve put a lot of care into making it simple, stable, and easy to use.
I welcome suggestions, bug reports, and feature requests — feel free to open an issue or pull request on GitHub.

# Commands and Permissions

Permissions follow the `simpleconomy.*` namespace.

## /simpleconomy (aliases: /se, /simpleeconomy)

**Purpose**: Show plugin identity and version for quick verification.
**User Use**: Check that the plugin is installed and running.
**Developer/Admin Use**: Confirm the deployed version before debugging or updates.

- Format: `/se`
- Permission: none
- Examples: `/se`

### /se reload

**Purpose**: Reload configuration and language files without rebooting the server.
**User Use**: Not intended for regular players.
**Developer/Admin Use**: Apply config changes in production with minimal downtime.

- Permission: `simpleconomy.command.reload`
- Examples: `/se reload`

---

## /balance (aliases: /bal, /money)

**Purpose**: Read a player's balance for the default currency.
**User Use**: Check your current balance.
**Developer/Admin Use**: Inspect other players' balances when resolving support issues.

- Format: `/balance [player]`
- Permission: none for self
- Permission for others: `simpleconomy.balance.others`
- Examples: `/bal`, `/bal Notch`

---

## /pay

**Purpose**: Transfer funds between players using the default currency.
**User Use**: Pay another player for goods or services.
**Developer/Admin Use**: Validate Vault integration and transaction limits.

- Format: `/pay <player> <amount>`
- Permission: none
- Examples: `/pay Steve 250`, `/pay Alex 10.5`

!!! warning
    This command uses Vault. Ensure Vault is installed.

---

## /eco

**Purpose**: Administrative balance control for the default currency.
**User Use**: Not intended for regular players.
**Developer/Admin Use**: Set, give, or remove balances for moderation and testing.

- Format: `/eco <set|give|remove> <player|@a|@p|@r> <amount>`
- Permissions: `simpleconomy.eco.set`, `simpleconomy.eco.give`, `simpleconomy.eco.remove`
- Examples: `/eco set Steve 500`, `/eco give @a 10`, `/eco remove Alex 25`

---

## /baltop (alias: /balancetop)

**Purpose**: Show the richest players leaderboard for the default currency.
**User Use**: View rankings and personal position.
**Developer/Admin Use**: Verify leaderboard data and refresh behavior.

- Format: `/baltop [page]`
- Permission: none
- Examples: `/baltop`, `/baltop 2`

### /baltop refresh

**Purpose**: Force-save cached balances and refresh the leaderboard data.
**User Use**: Not intended for regular players.
**Developer/Admin Use**: Sync cache to storage before audits or backups.

- Permission: `simpleconomy.baltop.refresh`
- Examples: `/baltop refresh`

---

## /voucher (alias: /withdraw)

**Purpose**: Convert balance into a physical voucher item.
**User Use**: Create a voucher to trade or store value.
**Developer/Admin Use**: Test voucher workflows and transaction logging.

- Format: `/voucher <amount>`
- Permission: none
- Examples: `/voucher 100`

---

## /currencies (aliases: /currs, /curs)

**Purpose**: Manage virtual currencies and their symbols.
**User Use**: Not intended for regular players.
**Developer/Admin Use**: Add, remove, and customize currencies at runtime.

- Base permission: `simpleconomy.command.currencies`
- Manage permission: `simpleconomy.command.currencies.manage`

### /currencies create
- Format: `/currencies create <name> <symbol>`
- Examples: `/currencies create Gems <>`

### /currencies delete
- Format: `/currencies delete <name>`
- Examples: `/currencies delete Gems`

### /currencies symbol
- Format: `/currencies symbol <name> [symbol]`
- Examples: `/currencies symbol Gems *`

### /currencies list
- Format: `/currencies list`

---

## /modules

**Purpose**: Inspect and manage external `EconomyModule` plugins.
**User Use**: Not intended for regular players.
**Developer/Admin Use**: Load, disable, and verify module status.

- Permission: `simpleconomy.command.modules`

### /modules list
- Examples: `/modules list`

### /modules disable
- Format: `/modules disable <name>`
- Examples: `/modules disable MyModule`

### /modules status
- Format: `/modules status <name>`
- Examples: `/modules status MyModule`

### /modules loadfromfile
- Format: `/modules loadfromfile <file.jar>`
- Examples: `/modules loadfromfile MyModule.jar`

---

## /ehistory

**Purpose**: Audit transaction history by player and currency.
**User Use**: Not intended for regular players.
**Developer/Admin Use**: Investigate disputes and verify transaction logs.

- Format: `/ehistory <player> [limit] [currency]`
- Permission: `simpleconomy.eco.history`
- Examples: `/ehistory Steve`, `/ehistory Steve 20 money`, `/ehistory Alex 5 gems`

---

## Update Notifications

**Purpose**: Notify staff about available updates on join.
**User Use**: Not intended for regular players.
**Developer/Admin Use**: Receive update alerts in-game.

- Permission: `simpleconomy.notify.update`

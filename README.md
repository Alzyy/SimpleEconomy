
# 🪙 SimpleEconomy - Development Branch

> **⚠️ WARNING: UNSTABLE BUILD**
> This branch is currently tracking major architectural changes, including **Multicurrency support** and a new **Modular system**. These features are undergoing intensive testing and are not ready for production use.

---

## 🛠 Current Development Focus

This branch serves as the staging area for the upcoming major update. Key areas of development:

| Feature | Status | Description |
| --- | --- | --- |
| **Multicurrency System** | 🟠 In Progress | Core logic, commands, and storage updated to support multiple currencies. |
| **Modular System** | 🟠 In Progress | Implementation of the `EconomyModule` framework for external extensions. |
| **Storage Optimization** | 🟢 Improved | Refactored caching and storage layer for better performance and scalability. |
| **Unit Testing** | 🟢 Implemented | Integration of unit tests to ensure API reliability during refactoring. |

---

## 🚀 Key Changes in this Version

### Multicurrency Support

The API and storage layers have been refactored to treat balances as currency-dependent.

* All core commands (`/eco`, `/pay`, `/balance`, etc.) are now currency-aware.
* Storage providers are updated to handle mapping balances to specific currency keys.

### Modular Framework

Introduced the `EconomyModule` interface. This allows developers to extend SimpleEconomy's functionality without modifying the Core plugin code, improving maintainability and reducing conflict risks.

### Performance & Scalability

* **Cache Refactoring:** Improved cache-to-storage synchronization logic to reduce I/O bottlenecks using Caffeine.
* **Storage Layer:** Re-engineered the database abstraction layer to support more complex data structures required by the multicurrency update.

---

## 💻 Developer Notes

If you are contributing to this branch, please adhere to these standards:

1. **Backward Compatibility:** While we move toward a modular system, ensure existing Vault-based integrations remain functional.
2. **Asynchronous Integrity:** All currency-based operations **must** be executed asynchronously. Use `CompletableFuture` for all API calls.
3. **Testing:** New tests are highly appreciated.
---

## 🐞 Bug Reporting

Found an issue with the system currently under development?

* Open an issue on GitHub with the prefix `[DEV-TEST]`.
* Please include your server logs and the specific storage provider you are using (MySQL/SQLite/FlatFile).
* Write me on Discord @ 0x416c7a79 
---



package it.alzy.simpleeconomy.plugin.utils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import it.alzy.simpleeconomy.plugin.i18n.enums.LanguageKeys;
import org.bukkit.Bukkit;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.ServicePriority;

public class VaultHook implements Economy {

    @Getter
    private static Economy economy;

    private final String currency = "money"; // Vault agisce solo sulla valuta principale

    public VaultHook() {
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            setupEconomy();
        }
    }

    public static boolean hasEconomy() {
        return economy != null;
    }

    private void setupEconomy() {
        Bukkit.getServicesManager().register(Economy.class, this, SimpleEconomy.getInstance(), ServicePriority.Normal);
        economy = this;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "SimpleEconomy";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return -1;
    }

    @Override
    public String format(double v) {
        return SimpleEconomy.getInstance().getFormatUtils().formatBalance(v);
    }

    @Override
    public String currencyNamePlural() {
        return "";
    }

    @Override
    public String currencyNameSingular() {
        return "";
    }

    @Override
    public boolean hasAccount(OfflinePlayer offlinePlayer) {
        if (SimpleEconomy.getInstance().getCache().contains(offlinePlayer.getUniqueId())) {
            return true;
        }
        return offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline();
    }

    @Override
    public boolean hasAccount(OfflinePlayer offlinePlayer, String s) {
        return hasAccount(offlinePlayer);
    }

    @Override
    public double getBalance(OfflinePlayer offlinePlayer) {
        UUID uuid = offlinePlayer.getUniqueId();
        SimpleEconomy plugin = SimpleEconomy.getInstance();
        
        // Verifica se presente nella nuova cache di Caffeine
        if (plugin.getCache().contains(uuid)) {
            Map<String, Double> balances = plugin.getCache().get(uuid);
            if (balances != null && balances.containsKey(currency)) {
                return balances.get(currency);
            }
            return SettingsConfig.getInstance().startingBalance();
        }

        // Vault richiede una risposta SINCROA. Se non è in cache, forziamo il caricamento sincrono con .join()
        try {
            Map<String, Double> balances = plugin.getStorage().load(uuid).join();
            if (balances != null && balances.containsKey(currency)) {
                return balances.get(currency);
            }
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed to block-load Vault balance for UUID: " + uuid);
            ex.printStackTrace();
        }

        return SettingsConfig.getInstance().startingBalance();
    }

    @Override
    public double getBalance(OfflinePlayer offlinePlayer, String s) {
        return getBalance(offlinePlayer);
    }

    @Override
    public boolean has(OfflinePlayer offlinePlayer, double amount) {
        return (getBalance(offlinePlayer) >= amount);
    }

    @Override
    public boolean has(OfflinePlayer offlinePlayer, String s, double amount) {
        return (getBalance(offlinePlayer) >= amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, double amount) {
        if (amount <= 0) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Cannot withdraw negative amount");
        }
        SimpleEconomy plugin = SimpleEconomy.getInstance();
        UUID uuid = offlinePlayer.getUniqueId();

        double currentBalance = getBalance(offlinePlayer);

        if (currentBalance < amount) {
            return new EconomyResponse(0, currentBalance, EconomyResponse.ResponseType.FAILURE, "Not enough money");
        }
        double newBalance = currentBalance - amount;
        
        // Aggiorna la cache centralizzata e imposta lo stato "dirty" per il salvataggio automatico
        plugin.getCache().updateCurrency(uuid, currency, newBalance);

        if (offlinePlayer.isOnline() && offlinePlayer.getPlayer() != null) {
            if (SettingsConfig.getInstance().enableActionBarMessages()) {
                ChatUtils.createComponentAsync(plugin.getLanguageManager().getMessage(
                                LanguageKeys.ACTION_BAR_DETRACT),
                        "%amount%", plugin.getFormatUtils().formatBalance(amount))
                .thenAccept(comp -> ChatUtils.sendActionBar(offlinePlayer.getPlayer(), comp));
            }
        }
        
        // Salvataggio asincrono in database
        plugin.getExecutor().execute(() -> plugin.getStorage().save(uuid, currency, newBalance));
        return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, String world, double amount) {
        return withdrawPlayer(offlinePlayer, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, double amount) {
        if (amount <= 0) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Cannot deposit negative amount");
        }
        SimpleEconomy plugin = SimpleEconomy.getInstance();
        UUID uuid = offlinePlayer.getUniqueId();

        double currentBalance = getBalance(offlinePlayer);
        double newBalance = currentBalance + amount;
        
        // Aggiorna la cache centralizzata e imposta lo stato "dirty" per il salvataggio automatico
        plugin.getCache().updateCurrency(uuid, currency, newBalance);

        if (offlinePlayer.isOnline() && offlinePlayer.getPlayer() != null) {
            if (SettingsConfig.getInstance().enableActionBarMessages()) {
                ChatUtils.createComponentAsync(plugin.getLanguageManager().getMessage(
                                LanguageKeys.ACTION_BAR_ADD),
                        "%amount%", plugin.getFormatUtils().formatBalance(amount))
                .thenAccept(comp -> ChatUtils.sendActionBar(offlinePlayer.getPlayer(), comp));
            }
        }
        
        // Salvataggio asincrono in database
        plugin.getExecutor().execute(() -> plugin.getStorage().save(uuid, currency, newBalance));
        return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, String s, double amount) {
        return depositPlayer(offlinePlayer, amount);
    }

    @Override
    public EconomyResponse createBank(String s, OfflinePlayer offlinePlayer) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks aren't implemented.");
    }

    @Override
    public EconomyResponse deleteBank(String s) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks aren't implemented.");
    }

    @Override
    public EconomyResponse bankBalance(String s) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks aren't implemented.");
    }

    @Override
    public EconomyResponse bankHas(String s, double v) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks aren't implemented.");
    }

    @Override
    public EconomyResponse bankWithdraw(String s, double v) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks aren't implemented.");
    }

    @Override
    public EconomyResponse bankDeposit(String s, double v) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks aren't implemented.");
    }

    @Override
    public EconomyResponse isBankOwner(String s, OfflinePlayer offlinePlayer) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks aren't implemented.");
    }

    @Override
    public EconomyResponse isBankMember(String s, OfflinePlayer offlinePlayer) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks aren't implemented.");
    }

    @Override
    public List<String> getBanks() {
        return List.of();
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer offlinePlayer) {
        if (hasAccount(offlinePlayer)) return false;
        SimpleEconomy.getInstance().getStorage().create(offlinePlayer.getUniqueId());
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer offlinePlayer, String s) {
        return createPlayerAccount(offlinePlayer);
    }
}
package io.github.edwardlcl.warden;

import io.github.edwardlcl.warden.alert.AlertManager;
import io.github.edwardlcl.warden.command.WardenCommand;
import io.github.edwardlcl.warden.detection.DupeDetector;
import io.github.edwardlcl.warden.detection.TntDupeListener;
import io.github.edwardlcl.warden.log.TransactionLogger;
import org.bukkit.plugin.java.JavaPlugin;

public final class WardenPlugin extends JavaPlugin {

    private TransactionLogger transactionLogger;
    private AlertManager alertManager;
    private DupeDetector dupeDetector;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.transactionLogger = new TransactionLogger(this);
        this.alertManager = new AlertManager(this);
        this.dupeDetector = new DupeDetector(this, alertManager, transactionLogger);

        getServer().getPluginManager().registerEvents(dupeDetector, this);
        getServer().getPluginManager().registerEvents(new TntDupeListener(this, alertManager), this);

        WardenCommand command = new WardenCommand(this, dupeDetector);
        if (getCommand("warden") != null) {
            getCommand("warden").setExecutor(command);
        }

        getLogger().info("Warden enabled — exploit detection active.");
    }

    @Override
    public void onDisable() {
        if (transactionLogger != null) {
            transactionLogger.flush();
            transactionLogger.close();
        }
        getLogger().info("Warden disabled.");
    }

    public TransactionLogger getTransactionLogger() {
        return transactionLogger;
    }

    public AlertManager getAlertManager() {
        return alertManager;
    }

    public DupeDetector getDupeDetector() {
        return dupeDetector;
    }
}

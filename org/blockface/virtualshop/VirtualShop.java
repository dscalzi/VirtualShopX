package org.blockface.virtualshop;

import java.io.IOException;
import net.milkbowl.vault.economy.Economy;

import org.blockface.virtualshop.commands.Buy;
import org.blockface.virtualshop.commands.Cancel;
import org.blockface.virtualshop.commands.Find;
import org.blockface.virtualshop.commands.Sales;
import org.blockface.virtualshop.commands.Sell;
import org.blockface.virtualshop.commands.Stock;
import org.blockface.virtualshop.commands.VM;
import org.blockface.virtualshop.managers.ConfigManager;
import org.blockface.virtualshop.managers.DatabaseManager;
import org.blockface.virtualshop.util.ItemDb;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("deprecation")
public class VirtualShop extends JavaPlugin {
    
    public static Economy econ = null;
    public static final boolean BETA = true;
    
    public void onDisable(){
        DatabaseManager.close();
    }

    public void onEnable(){
        if (this.setupEconomy()){
            
        } else {
            this.getLogger().severe("Vault not found. Shutting down!");
            this.getServer().getPluginManager().disablePlugin(this);
        }
        ConfigManager.initialize(this);
		Chatty.initialize(this);
        DatabaseManager.initialize();
        try {
            ItemDb.load(this.getDataFolder(),"items.csv");
        } catch (IOException e) {
            this.getPluginLoader().disablePlugin(this);
            return;
        }
        this.registerCommands();
    }
    
    public boolean setupEconomy(){
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
    
    public void registerCommands(){
    	this.getCommand("buy").setExecutor(new Buy(this));
    	this.getCommand("cancel").setExecutor(new Cancel(this));
    	this.getCommand("find").setExecutor(new Find(this));
    	this.getCommand("shop").setExecutor(new VM(this));
    	this.getCommand("sales").setExecutor(new Sales(this));
    	this.getCommand("sell").setExecutor(new Sell(this));
    	this.getCommand("stock").setExecutor(new Stock(this));
    	this.getCommand("vm").setExecutor(new VM(this));
    }
    
    public boolean hasEnough(String playerName, double money){
        double balance = econ.getBalance(playerName) - money;
        if (balance > 0){
            return true;
        } else {
            return false;
        }
    }
}

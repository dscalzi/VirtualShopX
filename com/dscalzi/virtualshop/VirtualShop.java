package com.dscalzi.virtualshop;

import java.io.IOException;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.dscalzi.virtualshop.commands.Buy;
import com.dscalzi.virtualshop.commands.Cancel;
import com.dscalzi.virtualshop.commands.Find;
import com.dscalzi.virtualshop.commands.Sales;
import com.dscalzi.virtualshop.commands.Sell;
import com.dscalzi.virtualshop.commands.Stock;
import com.dscalzi.virtualshop.commands.VS;
import com.dscalzi.virtualshop.managers.ConfigManager;
import com.dscalzi.virtualshop.managers.DatabaseManager;
import com.dscalzi.virtualshop.util.ItemDb;

@SuppressWarnings("deprecation")
public class VirtualShop extends JavaPlugin {
    
    public static Economy econ = null;
    public static final boolean BETA = false;
    
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
    	this.getCommand("shop").setExecutor(new VS(this));
    	this.getCommand("sales").setExecutor(new Sales(this));
    	this.getCommand("sell").setExecutor(new Sell(this));
    	this.getCommand("stock").setExecutor(new Stock(this));
    	this.getCommand("vs").setExecutor(new VS(this));
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

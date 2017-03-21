/*
 * VirtualShop
 * Copyright (C) 2015-2017 Daniel D. Scalzi
 * See LICENSE.txt for license information.
 */
package com.dscalzi.virtualshop;

import java.io.IOException;
import net.milkbowl.vault.economy.Economy;

import org.bstats.Metrics;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.dscalzi.virtualshop.commands.Buy;
import com.dscalzi.virtualshop.commands.Cancel;
import com.dscalzi.virtualshop.commands.EBuy;
import com.dscalzi.virtualshop.commands.ESell;
import com.dscalzi.virtualshop.commands.Find;
import com.dscalzi.virtualshop.commands.Sales;
import com.dscalzi.virtualshop.commands.Sell;
import com.dscalzi.virtualshop.commands.Stock;
import com.dscalzi.virtualshop.commands.Reprice;
import com.dscalzi.virtualshop.commands.VS;
import com.dscalzi.virtualshop.managers.MessageManager;
import com.dscalzi.virtualshop.managers.ConfigManager;
import com.dscalzi.virtualshop.managers.ConfirmationManager;
import com.dscalzi.virtualshop.managers.DatabaseManager;
import com.dscalzi.virtualshop.managers.DatabaseManager.ConnectionType;
import com.dscalzi.virtualshop.managers.UUIDManager;
import com.dscalzi.virtualshop.util.ItemDB;
import com.dscalzi.virtualshop.util.Reloader;

public class VirtualShop extends JavaPlugin {
    
    private static Economy econ = null;
    @SuppressWarnings("unused")
	private Metrics metrics;
    
    public void onDisable(){
    	ConfirmationManager.getInstance().serialize();
    	if(ConfigManager.getInstance().uuidSyncOnDisable()) this.syncNameToUUID();
    	DatabaseManager.getInstance().terminate();
    	this.metrics = new Metrics(this);
        System.gc();
    }

    public void onEnable(){
        if (!this.setupEconomy()){
            this.getLogger().severe("Vault not found. Shutting down!");
            this.getPluginLoader().disablePlugin(this);
        }
        try {
			ItemDB.initialize(this);
		} catch (IOException e) {
			this.getLogger().severe("Reference file 'items.csv' not found. Shutting down!");
			this.getPluginLoader().disablePlugin(this);
            return;
		}
        this.initializeManagers();
        DatabaseManager.initialize(this);
        if(DatabaseManager.getInstance().getConnectionType() == ConnectionType.VOID)
        	this.getPluginLoader().disablePlugin(this);
        Reloader.initialize(this);
        this.registerCommands();
        if(ConfigManager.getInstance().uuidSyncOnEnable()) this.syncNameToUUID();
    }
    
    private boolean setupEconomy(){
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }
    
    private void initializeManagers(){
    	UUIDManager.initialize(this);
        ConfigManager.initialize(this);
        ConfirmationManager.initialize(this);
		MessageManager.initialize(this);
    }
    
    private void registerCommands(){
    	this.getCommand("buy").setExecutor(new Buy(this));
    	this.getCommand("ebuy").setExecutor(new EBuy(this));
    	this.getCommand("cancel").setExecutor(new Cancel(this));
    	this.getCommand("find").setExecutor(new Find(this));
    	this.getCommand("shop").setExecutor(new VS(this));
    	this.getCommand("sales").setExecutor(new Sales(this));
    	this.getCommand("sell").setExecutor(new Sell(this));
    	this.getCommand("esell").setExecutor(new ESell(this));
    	this.getCommand("stock").setExecutor(new Stock(this));
    	this.getCommand("reprice").setExecutor(new Reprice(this));
    	this.getCommand("vs").setExecutor(new VS(this));
    }
    
    private void syncNameToUUID(){
    	this.getLogger().info("Syncing account data..");
    	int result = DatabaseManager.getInstance().syncNameToUUID();
    	if(result > 0) this.getLogger().info("Done! Successfully synced " + result + ((result == 1) ? " account." : " accounts."));
    	else this.getLogger().info("All accounts are already synced!");
    }
    
    public static boolean hasEnough(Player player, double money){
        double balance = econ.getBalance(player) - money;
        if (balance > 0){
            return true;
        } else {
            return false;
        }
    }
    
    public static Economy getEconomy(){
    	return econ;
    }
}

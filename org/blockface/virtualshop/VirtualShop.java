package org.blockface.virtualshop;

import java.io.IOException;

import net.milkbowl.vault.economy.Economy;

import org.blockface.virtualshop.commands.Buy;
import org.blockface.virtualshop.commands.Cancel;
import org.blockface.virtualshop.commands.Find;
import org.blockface.virtualshop.commands.Help;
import org.blockface.virtualshop.commands.Sales;
import org.blockface.virtualshop.commands.Sell;
import org.blockface.virtualshop.commands.Stock;
import org.blockface.virtualshop.managers.ConfigManager;
import org.blockface.virtualshop.managers.DatabaseManager;
import org.blockface.virtualshop.util.ItemDb;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("deprecation")
public class VirtualShop extends JavaPlugin {
    
    public static Economy econ = null;
    
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
    
    public boolean hasEnough(String playerName, double money){
        double balance = econ.getBalance(playerName) - money;
        if (balance > 0){
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if(label.equalsIgnoreCase("sell")) Sell.execute(sender, args, this);
        if(label.equalsIgnoreCase("buy")) Buy.execute(sender, args, this);
        if(label.equalsIgnoreCase("cancel")) Cancel.execute(sender, args, this);
        if(label.equalsIgnoreCase("stock")) Stock.execute(sender, args, this);
        if(label.equalsIgnoreCase("sales")) Sales.execute(sender, args, this);
        if(label.equalsIgnoreCase("find")) Find.execute(sender, args, this);
        if(label.equalsIgnoreCase("shop")) Help.execute(sender, this);
        return true;
    }
}

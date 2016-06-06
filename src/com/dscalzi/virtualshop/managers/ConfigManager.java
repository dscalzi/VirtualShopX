package com.dscalzi.virtualshop.managers;

import java.io.File;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import com.dscalzi.virtualshop.VirtualShop;

import net.md_5.bungee.api.ChatColor;

public class ConfigManager
{
	private static VirtualShop plugin;
    private static FileConfiguration config;

    public static void initialize(Plugin plugin){
    	ConfigManager.plugin = (VirtualShop) plugin;
        ConfigManager.loadConfig(ConfigManager.plugin);
        plugin.getLogger().info(getServerName());
    }

    public static void loadConfig(Plugin plugin){
    	verifyConfig(plugin);
    	plugin.reloadConfig();
		config = plugin.getConfig(); 
    }
    
    public static void verifyConfig(Plugin plugin){
    	File file = new File(plugin.getDataFolder(), "config.yml");
		if (!file.exists()){
			plugin.saveDefaultConfig();
		}
    }
    
    public static String getPrefix(){
    	return ChatColor.translateAlternateColorCodes('&', config.getString("chatty.prefix")) + getColor();
    }
    
    public static String getServerName(){
    	return config.getString("chatty.server-name");
    }
    
    public static String getColor(){
    	return ChatColor.translateAlternateColorCodes('&', config.getString("chatty.message-color"));
    }
    
	public static Boolean broadcastOffers(){
		return config.getBoolean("broadcast-offers", true);
	}

	public static Long getMaxPrice(){
		return config.getLong("price-limits.default_limit");
	}
	
	public static Long getMaxPrice(int itemID){
		return getMaxPrice(itemID, 0);
	}
	
	public static Long getMaxPrice(int itemID, int dataValue){
		if(!config.contains("price-limits.items." + Integer.toString(itemID) + "-" + Integer.toString(dataValue)))
			return getMaxPrice();
		if(!config.contains("price-limits.items." + Integer.toString(itemID) + "-" + Integer.toString(dataValue) + ".max-price"))
			return getMaxPrice();
		return config.getLong("price-limits.items." + Integer.toString(itemID) + "-" + Integer.toString(dataValue) + ".max-price");
	}
	
	@SuppressWarnings("unchecked")
	public static List<String> getAllowedWorlds(){
		return (List<String>)config.getList("allowed-worlds");
	}
	
    public static Integer getPort(){
        return config.getInt("MySQL.port",3306);
    }

	public static Boolean usingMySQL(){
		return config.getBoolean("using-MySQL", false);
	}

	public static String mySQLUserName(){
		return config.getString("MySQL.username", "root");
	}

	public static String mySQLPassword(){
		return config.getString("MySQL.password", "password");
	}

	public static String mySQLHost(){
		return config.getString("MySQL.host", "localhost");
	}

	public static String mySQLdatabase(){
		return config.getString("MySQL.database", "minecraft");
	}

	public static Integer mySQLport(){
		return config.getInt("MySQL.port", 3306);
	}

}

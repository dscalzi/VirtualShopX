package org.blockface.virtualshop.managers;

import java.io.File;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import net.md_5.bungee.api.ChatColor;

public class ConfigManager
{
    private static ConfigurationSection config;

    public static void initialize(Plugin plugin){
        config = plugin.getConfig();
        plugin.getConfig().options().copyDefaults(true); 
        File file = new File(plugin.getDataFolder(), "config.yml");
		if (!file.exists())
		{
			plugin.saveDefaultConfig();
		}
        plugin.reloadConfig();
        broadcastOffers();
        usingMySQL();
        mySQLUserName();
        mySQLHost();
        mySQLdatabase();
        mySQLport();
        mySQLPassword();
        getPort();
        plugin.saveConfig();
    }

    public static String getPrefix(){
    	return ChatColor.translateAlternateColorCodes('&', config.getString("chatty.prefix"));
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

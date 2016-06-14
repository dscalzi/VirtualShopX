package com.dscalzi.virtualshop.managers;

import java.io.File;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import com.dscalzi.virtualshop.VirtualShop;

import net.md_5.bungee.api.ChatColor;

public class ConfigManager {

	private static boolean initialized;
	private static ConfigManager instance;
	
	//TODO Will be implemented in a later version
	private final double configVersion = 1.9;
	private VirtualShop plugin;
	private FileConfiguration config;
	
	private ConfigManager(Plugin plugin){
		this.plugin = (VirtualShop)plugin;
		loadConfig();
	}
	
	public void loadConfig(){
    	verifyFile();
    	this.plugin.reloadConfig();
		this.config = this.plugin.getConfig(); 
    }
	
	public void verifyFile(){
    	File file = new File(this.plugin.getDataFolder(), "config.yml");
		if (!file.exists()){
			this.plugin.saveDefaultConfig();
		}
    }
	
	public static void initialize(Plugin plugin){
		if(!initialized){
			instance = new ConfigManager(plugin);
			initialized = true;
		}
	}
	
	public static boolean reload(){
		if(!initialized) return false;
		getInstance().loadConfig();
		return true;
	}
	
	public static ConfigManager getInstance(){
		return ConfigManager.instance;
	}
	
	/* Configuration Accessors */
	
	public String getPrefix(){
    	return (ChatColor.translateAlternateColorCodes('&', this.config.getString("chat_settings.details.msg_prefix")) + getColor()).trim();
    }
    
    public String getServerName(){
    	return (this.config.getString("chat_settings.details.server_name")).trim();
    }
    
    public String getColor(){
    	return (ChatColor.translateAlternateColorCodes('&', this.config.getString("chat_settings.details.message_color"))).trim();
    }
    
    public String getBaseColor(){
    	return (ChatColor.translateAlternateColorCodes('&', this.config.getString("chat_settings.details.base_color"))).trim();
    }
    
    public String getTrimColor(){
    	return (ChatColor.translateAlternateColorCodes('&', this.config.getString("chat_settings.details.trim_color"))).trim();
    }
    
    public String getDescriptionColor(){
    	return (ChatColor.translateAlternateColorCodes('&', this.config.getString("chat_settings.details.description_color"))).trim();
    }
    
    public String getErrorColor(){
    	return (ChatColor.translateAlternateColorCodes('&', this.config.getString("chat_settings.details.error_color"))).trim();
    }
    
    public String getSuccessColor(){
    	return (ChatColor.translateAlternateColorCodes('&', this.config.getString("chat_settings.details.success_color"))).trim();
    }
    
	public boolean broadcastOffers(){
		return this.config.getBoolean("chat_settings.broadcast_offers", true);
	}

	public long getMaxPrice(){
		return this.config.getLong("item_settings.price_limits.default_limit");
	}
	
	public long getMaxPrice(int itemID){
		return getMaxPrice(itemID, 0);
	}
	
	public long getMaxPrice(int itemID, int dataValue){
		if(!this.config.contains("item_settings.price_limits.items." + Integer.toString(itemID) + "-" + Integer.toString(dataValue)))
			return getMaxPrice();
		if(!this.config.contains("item_settings.price_limits.items." + Integer.toString(itemID) + "-" + Integer.toString(dataValue) + ".max-price"))
			return getMaxPrice();
		return this.config.getLong("item_settings.price_limits.items." + Integer.toString(itemID) + "-" + Integer.toString(dataValue) + ".max-price");
	}
	
	public List<String> getAllowedWorlds(){
		return this.config.getStringList("general_settings.allowed_worlds");
	}
	
    public int getPort(){
        return this.config.getInt("MySQL.port",3306);
    }

	public boolean usingMySQL(){
		return this.config.getBoolean("database_settings.using_MySQL", false);
	}

	public String mySQLUserName(){
		return this.config.getString("database_settings.MySQL.username", "root");
	}

	public String mySQLPassword(){
		return this.config.getString("database_settings.MySQL.password", "password");
	}

	public String mySQLHost(){
		return this.config.getString("database_settings.MySQL.host", "localhost");
	}

	public String mySQLdatabase(){
		return this.config.getString("database_settings.MySQL.database", "minecraft");
	}

	public int mySQLport(){
		return this.config.getInt("MySQL.port", 3306);
	}
	
	public double getVersion(){
		return this.configVersion;
	}
}

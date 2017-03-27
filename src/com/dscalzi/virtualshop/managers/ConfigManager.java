/*
 * VirtualShop
 * Copyright (C) 2015-2017 Daniel D. Scalzi
 * See LICENSE.txt for license information.
 */
package com.dscalzi.virtualshop.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.objects.Confirmable;
import com.dscalzi.virtualshop.util.Localization;


public final class ConfigManager {

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
    	return (ChatColor.translateAlternateColorCodes('&', this.config.getString("chat_settings.details.msg_prefix")) + getMessageColor()).trim();
    }
    
    public String getServerName(){
    	return (this.config.getString("chat_settings.details.server_name", "Server")).trim();
    }
    
    public boolean enableVSR(){
    	return config.getBoolean("general_settings.enable_vsr", true);
    }
    
    public Localization getLocalization(){
    	String setting = (this.config.getString("chat_settings.details.localization")).trim();
    	for(Localization lc : Localization.values()){
    		if(lc.toString().equalsIgnoreCase(setting))
    			return lc;
    	}
    	//Default to US if invalid value is given.
    	return Localization.US;
    }
    
    public ChatColor getMessageColor(){
    	return getChatColor("chat_settings.details.message_color", ChatColor.YELLOW);
    }
    
    public ChatColor getBaseColor(){
    	return getChatColor("chat_settings.details.base_color", ChatColor.LIGHT_PURPLE);
    }
    
    public ChatColor getTrimColor(){
    	return getChatColor("chat_settings.details.trim_color", ChatColor.DARK_PURPLE);
    }
    
    public ChatColor getDescriptionColor(){
    	return getChatColor("chat_settings.details.description_color", ChatColor.LIGHT_PURPLE);
    }
    
    public ChatColor getErrorColor(){
    	return getChatColor("chat_settings.details.error_color", ChatColor.RED);
    }
    
    public ChatColor getSuccessColor(){
    	return getChatColor("chat_settings.details.success_color", ChatColor.GREEN);
    }
    
    public ChatColor getAmountColor(){
    	return getChatColor("chat_settings.details.amount_color", ChatColor.GOLD);
    }
    
    public ChatColor getItemColor(){
    	return getChatColor("chat_settings.details.item_color", ChatColor.BLUE);
    }

    public ChatColor getPriceColor(){
    	return getChatColor("chat_settings.details.price_color", ChatColor.YELLOW);
    }

    public ChatColor getBuyerColor(){
    	return getChatColor("chat_settings.details.buyer_color", ChatColor.AQUA);
	}

    public ChatColor getSellerColor(){
    	return getChatColor("chat_settings.details.seller_color", ChatColor.RED);
    }
    
    private ChatColor getChatColor(String path, ChatColor def){
    	String s = this.config.getString(path);
    	s = s.replaceAll("\\&", "");
    	ChatColor r = ChatColor.getByChar(s);
    	return r == null ? def : r;
    }
    
    public int getPackSpacing(){
    	
    	String selected = this.config.getString("chat_settings.resoucepacks.selected_pack");
    	if(this.config.contains("chat_settings.resoucepacks.supported_packs." + selected + ".sizing"))
    		return this.config.getInt("chat_settings.resoucepacks.supported_packs." + selected + ".sizing");
		else
			try {
				throw new InvalidConfigurationException("Resource pack not correctly defined!");
			} catch (InvalidConfigurationException e) {
				e.printStackTrace();
				return 70;
			}
    }
    
	public boolean broadcastOffers(){
		return this.config.getBoolean("chat_settings.broadcast_offers", true);
	}

	public double getMaxPrice(){
		return this.config.getDouble("item_settings.price_limits.default_limit");
	}
	
	public double getMaxPrice(int itemID){
		return getMaxPrice(itemID, 0);
	}
	
	public int getConfirmationTimeout(Class<? extends Confirmable> clazz){
		int time = this.config.getInt("general_settings.confirmation_timeouts." + (clazz.getSimpleName().toLowerCase()), 15000);
		return (time > 0) ? time : 15000;
	}
	
	public double getMaxPrice(int itemID, int dataValue){
		if(!this.config.contains("item_settings.price_limits.items." + Integer.toString(itemID) + "-" + Integer.toString(dataValue)))
			return getMaxPrice();
		if(!this.config.contains("item_settings.price_limits.items." + Integer.toString(itemID) + "-" + Integer.toString(dataValue) + ".max-price"))
			return getMaxPrice();
		return this.config.getLong("item_settings.price_limits.items." + Integer.toString(itemID) + "-" + Integer.toString(dataValue) + ".max-price");
	}
	
	public List<String> getAllowedWorlds(){
		return this.config.getStringList("general_settings.allowed_worlds");
	}
	
	public List<String> getAllowedGamemodes(){
		List<String> a = this.config.getStringList("general_settings.allowed_gamemodes");
		List<String> b = new ArrayList<String>();
		for(String s : a) b.add(s.toUpperCase());
		return b;
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
		return this.config.getInt("database_settings.MySQL.port", 3306);
	}
	
	public boolean uuidSyncOnEnable(){
		return this.config.getBoolean("database_settings.UUID_name_syncs.on_startup", false);
	}
	
	public boolean uuidSyncOnDisable(){
		return this.config.getBoolean("database_settings.UUID_name_syncs.on_shutdown", false);
	}
	
	public double getVersion(){
		return this.configVersion;
	}
}

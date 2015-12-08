package org.blockface.virtualshop.managers;

import java.io.IOException;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public class ConfigManager
{
    private static ConfigurationSection config;

    public static void Initialize(Plugin plugin)
    {
        config = plugin.getConfig();
        plugin.getConfig().options().copyDefaults(true); 
        try {
            plugin.getConfig().save("config.yml");
        } catch (IOException e) {
            e.printStackTrace();
        }
        plugin.reloadConfig();
        BroadcastOffers();
        UsingMySQL();
        MySQLUserName();
        MySQLHost();
        MySQLdatabase();
        MySQLport();
        MySQLPassword();
        getPort();
        plugin.saveConfig();
    }

	public static Boolean BroadcastOffers()
	{
		return config.getBoolean("broadcast-offers", true);
	}

    public static Integer getPort() {
        return config.getInt("MySQL.port",3306);
    }

	public static Boolean UsingMySQL()
	{
		return config.getBoolean("using-MySQL", false);
	}

	public static String MySQLUserName()
	{
		return config.getString("MySQL.username", "root");
	}

	public static String MySQLPassword()
	{
		return config.getString("MySQL.password", "password");
	}

	public static String MySQLHost()
	{
		return config.getString("MySQL.host", "localhost");
	}

	public static String MySQLdatabase()
	{
		return config.getString("MySQL.database", "minecraft");
	}

	public static Integer MySQLport()
	{
		return config.getInt("MySQL.port", 3306);
	}

}

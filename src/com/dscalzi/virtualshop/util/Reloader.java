/*
 * VirtualShop
 * Copyright (C) 2015-2017 Daniel D. Scalzi
 * See LICENSE.txt for license information.
 */
package com.dscalzi.virtualshop.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.UnknownDependencyException;

import com.dscalzi.virtualshop.managers.MessageManager;

public final class Reloader {

	private static boolean initialized;
	private static Reloader instance;
	
	private Plugin plugin;
	
	private Reloader(Plugin plugin){
		this.plugin = plugin;
		load();
	}
	
	/* Method to load this Reloader */
	private void load(){
		File dest = new File(new File("plugins") + "/VSReloader.jar");
		boolean checkForUpdate = true;
		if(!dest.exists())
			checkForUpdate = this.loadVSR(dest);
		if(checkForUpdate)
			if(updateAvailable(dest))
				plugin.getServer().getConsoleSender().sendMessage("[" + plugin.getName() + "] " + ChatColor.GREEN + "UPDATE FOR VSRELOADER AVAILABLE - Just delete the existing jar file and restart the server!");
	}
	
	public static void initialize(Plugin plugin){
		if(!initialized){
			instance = new Reloader(plugin);
			initialized = true;
		}
	}
	
	public static boolean reload(){
		if(!initialized) return false;
		getInstance().load();
		return true;
	}
	
	public static Reloader getInstance(){
		return instance;
	}
	
	/* Reflect */
	
	private boolean loadVSR(final File dest){
        try(InputStream in = this.getClass().getResourceAsStream("/depend/VSReloader.jar")){
        	plugin.getLogger().info("Saving VSReloader.jar");
			Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException | NullPointerException e) {
			MessageManager.getInstance().logError("Error ocurred while saving VSReloader", true);
			return false;
		}
        return enableVSR(dest);
    }
	
	private boolean enableVSR(File dest){
		Plugin target = null;
		try {
			target = Bukkit.getPluginManager().loadPlugin(dest);
		} catch (UnknownDependencyException | InvalidPluginException | InvalidDescriptionException e) {
			MessageManager.getInstance().logError("Could not enable VSReloader.", true);
			return false;
		}
		target.onLoad();
		Bukkit.getPluginManager().enablePlugin(target);
		return true;
	}
	
	private boolean updateAvailable(File dest){
		System.gc();
		File pluginFile;
		PluginDescriptionFile desc;
		try{
			pluginFile = new File(new File("plugins"), "VSReloader.jar");
			desc = plugin.getPluginLoader().getPluginDescription(pluginFile);
		} catch (NullPointerException e){
			MessageManager.getInstance().logError("Could not check for an update to VSReloader, jar file is missing.", true);
			return false;
		} catch (InvalidDescriptionException e) {
			MessageManager.getInstance().logError("Could not check for an update to VSReloader, jar file is missing or corrupt.", true);
			return false;
		}
		final String currentVersion = desc.getVersion();
    	try(InputStream in = Reloader.class.getResourceAsStream("/depend/updater");
    		InputStreamReader ireader = new InputStreamReader(in);
    		BufferedReader reader = new BufferedReader(ireader)){
    		String line = null;
    		while((line = reader.readLine()) != null){
    			if(line.startsWith("VSReloader-Provided")){
    				String[] tmp = line.split("'");
    				String version = tmp[1];
    				if(!(version.equals(currentVersion))){
    					return true;
    				}
					System.gc();
    				break;
    			}
    		}
		} catch (IOException | NullPointerException e){
			MessageManager.getInstance().logError("Error occurred while checking for VSReloader update.", true);
			return false;
		} catch (ArrayIndexOutOfBoundsException e){
			MessageManager.getInstance().logError("Could not check for VSReloader update due to malformed updater file.", true);
			return false;
		}
    	
    	return false;
    }
}

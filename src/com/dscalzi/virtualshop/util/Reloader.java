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

public class Reloader {

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
		if(updateAvailable(dest))
			plugin.getServer().getConsoleSender().sendMessage("[" + plugin.getName() + "] " + ChatColor.GREEN + "UPDATE FOR VSRELOADER AVAILABLE - Just delete the existing jar file and restart the server!");
		if(!dest.exists())
			this.loadVSR(dest);
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
	
	private void loadVSR(final File dest){
        final InputStream in = this.getClass().getResourceAsStream("/depend/VSReloader.jar");
        try {
        	plugin.getLogger().info("Saving VSReloader.jar");
			Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
			in.close();
		} catch (IOException | NullPointerException e) {
			try { in.close(); } catch (IOException e1) {}
			plugin.getLogger().severe("Error ocurred while saving VSReloader");
			return;
		}
        try { in.close(); } catch (IOException e) {}
        enableVSR(dest);
    }
	
	private boolean enableVSR(File dest){
		Plugin target = null;
		try {
			target = Bukkit.getPluginManager().loadPlugin(dest);
		} catch (UnknownDependencyException | InvalidPluginException | InvalidDescriptionException e) {
			plugin.getLogger().severe("Could not enable VSReloader.");
			return false;
		}
		target.onLoad();
		Bukkit.getPluginManager().enablePlugin(target);
		return true;
	}
	
	private boolean updateAvailable(File dest){
		System.gc();
    	try {
    		File pluginFile = new File(new File("plugins"), "VSReloader.jar");
    		PluginDescriptionFile desc = plugin.getPluginLoader().getPluginDescription(pluginFile);
    		String currentVersion = desc.getVersion();
    		InputStream in = this.getClass().getResourceAsStream("/depend/updater");
    		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    		String line = null;
    		while((line = reader.readLine()) != null){
    			if(line.startsWith("VSReloader-Provided")){
    				String[] tmp = line.split("'");
    				String version = tmp[1];
    				if(!(version.equals(currentVersion))){
    					in.close();
    					reader.close();
    					System.gc();
    					return true;
    				}
    				in.close();
					reader.close();
					System.gc();
    				break;
    			}
    		}
		} catch (Exception e) {
			return false;
		}
    	
    	return false;
    }
}

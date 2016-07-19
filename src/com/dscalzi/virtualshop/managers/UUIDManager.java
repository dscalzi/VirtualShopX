package com.dscalzi.virtualshop.managers;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.dscalzi.virtualshop.VirtualShop;

public final class UUIDManager {

	private static boolean initialized;
	private static UUIDManager instance;
	
	@SuppressWarnings("unused")
	private VirtualShop plugin;
	
	private UUIDManager(Plugin plugin){
		this.plugin = (VirtualShop)plugin;
		load();
	}
	
	private void load(){
		//Add dependent variables here
	}
	
	public static void initialize(Plugin plugin){
		if(!initialized){
			instance = new UUIDManager(plugin);
			initialized = true;
		}
	}
	
	public static boolean reload(){
		if(!initialized) return false;
		getInstance().load();
		return true;
	}
	
	public static UUIDManager getInstance(){
		return UUIDManager.instance;
	}
	
	/* Utilities */
	
	public Optional<String> getNewPlayerName(UUID uuid, String oldName){
		String currentName = Bukkit.getOfflinePlayer(uuid).getName();
		return (currentName.equals(oldName)) ? Optional.empty() : Optional.of(currentName);
	}
	
	public Optional<String> getPlayerName(UUID uuid){
		String name = Bukkit.getOfflinePlayer(uuid).getName();
		return (name == null) ? Optional.empty() : Optional.of(name);
	}
	
	public Optional<OfflinePlayer> getOfflinePlayer(UUID uuid){
		OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
		return (p == null) ? Optional.empty() : Optional.of(p);
	}
	
	@SuppressWarnings("deprecation")
	public Optional<UUID> getPlayerUUID(String name){
		OfflinePlayer p = Bukkit.getOfflinePlayer(name);
		return (p == null) ? Optional.empty() : Optional.of(p.getUniqueId());
	}
	
}

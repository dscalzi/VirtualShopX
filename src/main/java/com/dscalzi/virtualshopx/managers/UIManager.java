/*
 * VirtualShopX
 * Copyright (C) 2015-2018 Daniel D. Scalzi
 * See LICENSE for license information.
 */
package com.dscalzi.virtualshopx.managers;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.dscalzi.virtualshopx.VirtualShopX;
import com.dscalzi.virtualshopx.objects.InventoryCache;

import com.dscalzi.virtualshopx.util.Pair;

public class UIManager implements Listener{

	private static boolean initialized;
	private static UIManager instance;
	
	private Map<Player, Pair<Class<?>, InventoryCache>> cache;
	
	private UIManager(VirtualShopX plugin){
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		this.assignVars();
	}
	
	private void assignVars(){
		this.cache = new HashMap<Player, Pair<Class<?>, InventoryCache>>();
	}
	
	public static void initialize(VirtualShopX plugin){
		if(!initialized){
			instance = new UIManager(plugin);
			initialized = true;
		}
	}
	
	public static void prepareShutdown(){
		if(!initialized) return;
		for(Player p : getInstance().cache.keySet()){
			p.closeInventory();
		}
	}
	
	public static boolean refresh(){
		if(!initialized) return false;
		getInstance().assignVars();
		return true;
	}
	
	public static UIManager getInstance(){
		return instance;
	}
	
	public InventoryCache openUI(Player player, Inventory inv, ItemStack item, Class<?> origin){
		return openUI(player, inv, item, origin, 0);
	}
	
	public InventoryCache openUI(Player player, Inventory inv, ItemStack item, Class<?> origin, int page){
		InventoryCache c = new InventoryCache(inv, item, page);
		cache(player, origin, c);
		player.openInventory(inv);
		return c;
	}
	
	public void cache(Player player, Class<?> origin, InventoryCache inv){
		if(inv == null) throw new NullPointerException("Inventory cache must not be null.");
		cache.put(player, new Pair<Class<?>, InventoryCache>(origin, inv));
	}
	
	public Pair<Class<?>, InventoryCache> retrieve(Player player){
		return cache.get(player);
	}
	
	public InventoryCache retrieve(Player player, Class<?> origin){
		Pair<Class<?>, InventoryCache> c = retrieve(player);
		if(c != null && c.getKey() != null && c.getKey() == origin) return c.getValue();
		return null;
	}
	
	public boolean contains(Player player){
		return cache.containsKey(player);
	}
	
	public boolean contains(Player player, Class<?> origin){
		Pair<Class<?>, InventoryCache> c = retrieve(player);
		return c != null && c.getKey() != null && c.getKey() == origin;
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onInventoryClose(InventoryCloseEvent e){
		if(!(e.getPlayer() instanceof Player)) return;
		Player player = (Player)e.getPlayer();
		if(cache.containsKey(player)){
			Pair<Class<?>, InventoryCache> c = retrieve(player);
			if(c.getValue().getInventory().equals(e.getInventory()))
				cache.remove(player);
		}
	}
	
}

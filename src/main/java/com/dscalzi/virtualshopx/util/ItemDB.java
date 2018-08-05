/*
 * VirtualShopX
 * Copyright (C) 2015-2018 Daniel D. Scalzi
 * See LICENSE for license information.
 */
package com.dscalzi.virtualshopx.util;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionData;

import com.dscalzi.itemcodexlib.ItemCodex;
import com.dscalzi.itemcodexlib.component.ItemEntry;
import com.dscalzi.virtualshopx.VirtualShopX;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public final class ItemDB {
	
	private static boolean initialized;
	private static ItemDB instance;
	
	private VirtualShopX plugin;
	private ItemCodex codex;
	
	private ItemDB(Plugin plugin) throws IOException{
		this.plugin = (VirtualShopX)plugin;
		this.load();
	}
	
	private void load() throws IOException{
		this.codex = new ItemCodex(plugin);
	}
	
	public static void initialize(Plugin plugin) throws IOException{
		if(!initialized){
			instance = new ItemDB(plugin);
			initialized = true;
		}
	}
	
	public static boolean reload() throws IOException{
		if(!initialized) return false;
		getInstance().load();
		return true;
	}
    
	public static ItemDB getInstance(){
		return ItemDB.instance;
	}
	
	/* Search methods */
	
	public ItemStack get(String id, int quantity){
	    Optional<ItemEntry> ieOpt = codex.getItem(id);
		if(ieOpt.isPresent()) {
		    ItemStack i = ieOpt.get().getItemStack();
		    i.setAmount(quantity);
		    return i;
		}
		return null;
	}
	
	public Optional<ItemEntry> get(String keyword) {
	    return codex.getItem(keyword);
	}
	
	public Optional<ItemEntry> getByItemStack(ItemStack item){
	    return codex.getItemByItemStack(item);
	}
	
	public String getItemAlias(ItemStack item) {
	    Optional<ItemEntry> isOpt = codex.getItemByItemStack(item);
	    if(isOpt.isPresent()) {
	        return isOpt.get().getAliases().get(0);
	    } else {
	        return item.getType().name().toLowerCase();
	    }
	}
	
	/**
	 * Will not reference the database as this method is intended for items
	 * that could not be found in our database.
	 */
	public ItemStack unsafeLookup(String id){
		for(Material m : Material.values()){
			if(m.toString().equalsIgnoreCase(id)){
				return new ItemStack(m);
			}
		}
		return null;
	}
	
	/* Static Item Utility */
	
	public static String serializePotionData(ItemStack item) {
	    
	    if(item.hasItemMeta()) {
	        if(item.getItemMeta() instanceof PotionMeta) {
	            PotionData d = ((PotionMeta)item.getItemMeta()).getBasePotionData();
	            Gson g = new Gson();
	            return g.toJson(d, new TypeToken<PotionData>() {}.getType());
	        }
	    }
	    
	    return null;
	}
	
	public static PotionData deserializePotionData(String s) {
	    if(s == null) {
	        return null;
	    }
	    Gson g = new Gson();
	    return g.fromJson(s, new TypeToken<PotionData>() {}.getType());
	}
	
	public static String serializeEnchantmentData(ItemStack item) {
	    Map<Enchantment, Integer> enchants = getEnchantments(item);
	    if(enchants.size() > 0) {
	        Gson g = new Gson();
	        return g.toJson(enchants, new TypeToken<Map<Enchantment, Integer>>() {}.getType());
	    } else {
	        return null;
	    }
	}
	
	public static Map<Enchantment, Integer> deserializeEnchantmentData(String s){
	    if(s == null) {
            return null;
        }
        Gson g = new Gson();
        return g.fromJson(s, new TypeToken<Map<Enchantment, Integer>>() {}.getType());
	}
	
	public static Map<Enchantment, Integer> getEnchantments(ItemStack item){
		if(item.getType() == Material.ENCHANTED_BOOK)
			return ((EnchantmentStorageMeta)item.getItemMeta()).getStoredEnchants();
		return item.getEnchantments();
	}
	
	public static boolean hasEnchantments(ItemStack item){
		return getEnchantments(item).size() > 0;
	}
	
	public static void addEnchantments(ItemStack item, Map<Enchantment, Integer> enchantments){
		if(item.getType() == Material.ENCHANTED_BOOK){
			EnchantmentStorageMeta meta = (EnchantmentStorageMeta)item.getItemMeta();
			for(Entry<Enchantment, Integer> entry : enchantments.entrySet())
				meta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
			item.setItemMeta(meta);
			return;
		}
		item.addUnsafeEnchantments(enchantments);
	}
	
	public static ItemStack getCleanedItem(ItemStack item){
		ItemStack i = item.clone();
		if(i.hasItemMeta()){
        	ItemMeta meta = i.getItemMeta();
        	if(meta.hasDisplayName()) meta.setDisplayName(null);
        	if(meta.hasLore()) meta.setLore(null);
        	i.setItemMeta(meta);
        }
		return i;
	}
	
}

/*
 * VirtualShop
 * Copyright (C) 2015-2017 Daniel D. Scalzi
 * See LICENSE.txt for license information.
 */
package com.dscalzi.virtualshop.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.managers.MessageManager;
import com.dscalzi.virtualshop.objects.ItemMetaData;

@SuppressWarnings("deprecation")
public final class ItemDB {
	
	private static boolean initialized;
	private static ItemDB instance;
	
	private VirtualShop plugin;
	private Map<String, ItemMetaData> items;
	private Map<ItemMetaData, String> reverse;
	
	private ItemDB(Plugin plugin) throws IOException{
		this.plugin = (VirtualShop)plugin;
		items = new LinkedHashMap<String, ItemMetaData>();
		reverse = new LinkedHashMap<ItemMetaData, String>();
		this.load();
	}
	
	private void load() throws IOException{
		File folder = plugin.getDataFolder();
		folder.mkdirs();
		File file = new File(folder, "items.csv");
		
		if (!file.exists())	{
			file.createNewFile();
			try(InputStream in = ItemDB.class.getResourceAsStream("/items.csv")){
				Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		}
		
		try(FileReader reader = new FileReader(file);
			BufferedReader rx = new BufferedReader(reader)){
			items.clear();

			for (int i = 0; rx.ready(); i++){
				try {
					String line = rx.readLine().trim().toLowerCase();
					if (line.startsWith("#")) continue;
					
					String[] parts = line.split("[^a-z0-9]");
					if (parts.length < 2) continue;
					
					ItemMetaData meta = new ItemMetaData(Integer.parseInt(parts[1]), Short.parseShort(parts[2]));
					
					if(items.containsKey(parts[0])) items.remove(parts[0]);
					items.put(parts[0], meta);
					if(parts.length > 2 && !reverse.containsKey(meta)) reverse.put(meta, parts[0]);
				} catch (Exception ex){
					plugin.getLogger().warning("Error parsing items.csv on line " + i);
				}
			}
		}
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
		ItemStack target = getUnsafe(id);
		if(target == null) return null;
		target.setAmount(quantity);
		if(items.containsValue(new ItemMetaData(target.getTypeId(), target.getDurability()))) return target;
		return null;
	}

	private ItemStack getUnsafe(String id){
		
		ItemMetaData data = null;
		
		try {
			data = ItemMetaData.parseItemMetaData(id);
		} catch (IllegalArgumentException e){		
			if (items.containsKey(id.toLowerCase())) data = items.get(id.toLowerCase());
		}
		
		if(data == null) return null;
		
		ItemStack ret = new ItemStack(data.getTypeID());
		ret.setDurability(data.getData());
		
		return ret;
	}
	
	public String reverseLookup(ItemStack item)	{
		ItemMetaData data = new ItemMetaData(item.getTypeId(), item.getDurability());
		if(reverse.containsKey(data)) {
			return reverse.get(data);
		}
		return item.getType().name().toLowerCase();
	}
	
	/**
	 * Will not reference the database as this method is intended for items
	 * that could not be found in our database.
	 */
	public ItemStack unsafeLookup(String id){
		
		ItemMetaData data = null;
		
		try{
			data = ItemMetaData.parseItemMetaData(id);
		} catch (IllegalArgumentException e){
			//Not an ID, try name.
			for(Material m : Material.values()){
				if(m.toString().equalsIgnoreCase(id)){
					return new ItemStack(m);
				}
			}
			return null;
		}
		
		return new ItemStack(data.getTypeID(), data.getData());
	}
	
	public List<String> getAliases(ItemStack item){
		List<String> ret = new ArrayList<String>();
		int typeID = item.getTypeId();
		int data = item.getDurability();
		
		Iterator<Entry<String, ItemMetaData>> it = items.entrySet().iterator();
		while(it.hasNext()){
			Entry<String, ItemMetaData> entry = it.next();
			ItemMetaData meta = entry.getValue();
			if(typeID != meta.getTypeID()) continue;
			if(data != meta.getData()) continue;
			ret.add(entry.getKey());
		}
		
		return ret;
	}
	
	/* Static Item Utility */
	
	public static Map<Enchantment, Integer> parseEnchantData(String data){
		Map<Enchantment, Integer> converted = new HashMap<Enchantment, Integer>();
		
		data = data.replaceAll("\\{|\\}", "");
		String[] sets = data.split(",");
		for(String s : sets){
			String[] pair = s.split(":");
			if(pair.length == 2){
				converted.put(Enchantment.getById(Integer.parseInt(pair[0])), Integer.parseInt(pair[1]));
			}
		}
		return converted;
	}
	
	public static String formatEnchantData(Map<Enchantment, Integer> data){
		//Key {EnchantmentID:Level,EnchantmentID:Level}
		String converted = "{";
		
		for(Entry<Enchantment, Integer> entry : data.entrySet()){
			if(entry.getKey() != null && entry.getValue() != null){
				converted += entry.getKey().getId() + ":" + entry.getValue() + ",";
			}
		}
		if(converted.length() > 1){
			converted = converted.substring(0, converted.length()-1);
		}
		
		return converted + "}";
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
	
	/**
	 * This NMS workaround was added to the API, therefore it is deprecated.
	 */
	@Deprecated
	public static ItemStack removeAttributes(ItemStack i){
        if(i == null) return i;
        if(i.getType() == Material.BOOK_AND_QUILL) return i;
	    ItemStack item = i.clone();
	    
	    Class<?> craftItemStackClazz = ReflectionUtil.getOCBClass("inventory.CraftItemStack");
        Method asNMSCopyMethod = ReflectionUtil.getMethod(craftItemStackClazz, "asNMSCopy", ItemStack.class);

        Class<?> nmsItemStackClazz = ReflectionUtil.getNMSClass("ItemStack");
        Class<?> nbtTagCompoundClazz = ReflectionUtil.getNMSClass("NBTTagCompound");
        Class<?> nbtBase = ReflectionUtil.getNMSClass("NBTBase");
        Class<?> nbtTagListClazz = ReflectionUtil.getNMSClass("NBTTagList");
        Method hasTag = ReflectionUtil.getMethod(nmsItemStackClazz, "hasTag");
        Method setTag = ReflectionUtil.getMethod(nmsItemStackClazz, "setTag", nbtTagCompoundClazz);
        Method getTag = ReflectionUtil.getMethod(nmsItemStackClazz, "getTag");
        Method set = ReflectionUtil.getMethod(nbtTagCompoundClazz, "set", String.class, nbtBase);
        Method asCraftMirror = ReflectionUtil.getMethod(craftItemStackClazz, "asCraftMirror", nmsItemStackClazz);
	    
        try{
        	Object nmsStack = asNMSCopyMethod.invoke(null, item);
        	Object tag;
        	
        	if(!((Boolean)hasTag.invoke(nmsStack))){
        		tag = nbtTagCompoundClazz.newInstance();
        		setTag.invoke(nmsStack, tag);
        	} else {
        		tag = getTag.invoke(nmsStack);
        	}
        	
        	Object am = nbtTagListClazz.newInstance();
        	set.invoke(tag, "AttributeModifiers", am);
        	setTag.invoke(nmsStack, tag);
        	return (ItemStack)asCraftMirror.invoke(null, nmsStack);
        } catch(Throwable t){
        	MessageManager.getInstance().logError("Failed to remove attributes while opening eFind inventory.", true);
        	return i;
        }
	}
	
}

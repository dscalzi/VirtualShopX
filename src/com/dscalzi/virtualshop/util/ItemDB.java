package com.dscalzi.virtualshop.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.dscalzi.virtualshop.VirtualShop;
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
		if (items.containsValue(new ItemMetaData(target.getTypeId(), target.getDurability()))) return target;
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
	
}

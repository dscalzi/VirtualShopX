package com.dscalzi.virtualshop.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

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
		items = new HashMap<String, ItemMetaData>();
		reverse = new HashMap<ItemMetaData, String>();
		this.load();
	}
	
	private void load() throws IOException{
		File folder = plugin.getDataFolder();
		folder.mkdirs();
		File file = new File(folder, "items.csv");
		
		if (!file.exists())	{
			file.createNewFile();
			InputStream in = ItemDB.class.getResourceAsStream("/items.csv");
			Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			in.close();
		}
		
		BufferedReader rx = new BufferedReader(new FileReader(file));
		try {
			items.clear();

			for (int i = 0; rx.ready(); i++){
				try {
					String line = rx.readLine().trim().toLowerCase();
					if (line.startsWith("#")) continue;
					
					String[] parts = line.split("[^a-z0-9]");
					if (parts.length < 2) continue;
					
					ItemMetaData meta = new ItemMetaData(Integer.parseInt(parts[1]), Short.parseShort(parts[2]));
					
					items.put(parts[0], meta);
					if(parts.length > 2 && !reverse.containsKey(meta)) reverse.put(meta, parts[0]);
					if(parts[0].equals("andesite")) System.out.println(meta);
				} catch (Exception ex){
					plugin.getLogger().warning("Error parsing items.csv on line " + i);
				}
			}
		} finally {
			rx.close();
			System.out.println(items.values().size());
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
		if (items.containsValue(new ItemMetaData(target.getTypeId(), target.getDurability()))) return target;
		return null;
	}

	private ItemStack getUnsafe(String id){
		
		ItemMetaData data = null;
		
		try {
			data = ItemMetaData.parseItemMetaData(id);
		} catch (IllegalArgumentException e){		
			if (items.containsKey(id.toLowerCase())) data = items.get(id);
		}
		
		if(data == null) return null;
		
		ItemStack ret = new ItemStack(data.getTypeID());
		ret.setDurability(data.getData());
		
		return ret;
	}
	
	public String reverseLookup(ItemStack item)	{
		ItemMetaData data = new ItemMetaData(item.getTypeId(), item.getDurability());
		System.out.println(data);
		if(reverse.containsKey(data)) {
			System.out.println(reverse.get(data));
			return reverse.get(data);
		}
		return item.getType().name().toLowerCase();
	}
	
	/*
	public List<String> getAliases(ItemStack item){
		List<String> ret = new ArrayList<String>();
		
		for(Map.Entry<String, Integer> entry : items.entrySet()){
			
		}
		
		return ret;
	}*/
	
}

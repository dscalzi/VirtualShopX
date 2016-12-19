package com.dscalzi.virtualshop.commands;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.managers.MessageManager;
import com.dscalzi.virtualshop.managers.ConfigManager;
import com.dscalzi.virtualshop.managers.DatabaseManager;
import com.dscalzi.virtualshop.objects.Offer;
import com.dscalzi.virtualshop.util.ItemDB;
import com.dscalzi.virtualshop.util.ReflectionUtil;

public class EFind implements CommandExecutor, Listener{

	private final MessageManager mm;
	@SuppressWarnings("unused")
	private final ConfigManager cm;
	private final DatabaseManager dbm;
	private final ItemDB idb;
	
	private VirtualShop plugin;
	private Map<Player, InventoryCache> activeInventories;
	
	private ItemStack[] utility;
	
	public EFind(VirtualShop plugin){
		this.plugin = plugin;
		this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
		this.activeInventories = new HashMap<Player, InventoryCache>();
		this.mm = MessageManager.getInstance();
		this.cm = ConfigManager.getInstance();
		this.dbm = DatabaseManager.getInstance();
		this.idb = ItemDB.getInstance();
		
		this.utility = new ItemStack[2];
		utility[0] = new ItemStack(Material.NETHER_STAR, 1);
		ItemMeta npmeta = utility[0].getItemMeta();
		npmeta.setDisplayName(mm.getBaseColor() + "Next page");
		utility[0].setItemMeta(npmeta);
		
		utility[1] = new ItemStack(Material.NETHER_STAR, 1);
		ItemMeta ppmeta = utility[1].getItemMeta();
		ppmeta.setDisplayName(mm.getBaseColor() + "Previous page");
		utility[1].setItemMeta(ppmeta);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!sender.hasPermission("virtualshop.merchant.find")){
            mm.noPermissions(sender);
            return true;
        }
		if(!(sender instanceof Player)){
			mm.denyConsole(sender);
			return true;
		}
		Player player = (Player)sender;
		if(activeInventories.containsKey(player)) this.activeInventories.remove(player);
		if(args.length < 1){
			mm.sendError(sender, "You need to specify the item.");
			return true;
		}
		
		this.execute(player, args);
		return true;
	}

	private void execute(Player player, String[] args){
		ItemStack item;
    	PlayerInventory im = player.getInventory();
		if(args[0].matches("^(?iu)(hand|mainhand|offhand)")){
			item = new ItemStack(args[0].equalsIgnoreCase("offhand") ? im.getItemInOffHand() : im.getItemInMainHand());
			if(item.getType() == Material.AIR){
				mm.holdingNothing(player);
				return;
			}
			args[0] = idb.reverseLookup(item);
		} else item = idb.get(args[0], 0);
		
		if(item == null){
    		mm.wrongItem(player, args[0]);
    		return;
    	}
		
		this.goToPage(player, item, 1);
	}
	
	private void goToPage(Player player, ItemStack item, int page){	
		if(activeInventories.containsKey(player)) activeInventories.remove(player);
		
		List<Offer> offers = dbm.getEnchantedOffers(item);
		if(offers.size() == 0){
			mm.noListings(player, idb.reverseLookup(item));
			return;
		}
		
		final ChatColor baseColor = mm.getBaseColor();
		final ChatColor trimColor = mm.getTrimColor();
		String name = idb.reverseLookup(item);
		
		double div = 14.0;
		
		int totalPages = (int) ((offers.size()/div > (int)(offers.size()/div)) ? (offers.size()/div)+1 : offers.size()/div);
		
		String title = trimColor + mm.formatItem(MessageManager.capitalize(name), false) + ChatColor.DARK_GRAY + " (" + page + " of " + totalPages + ")";
		Inventory inventory = Bukkit.createInventory(player, 36, title);
		
		ItemStack crown = removeAttributes(item);
		crown.setAmount(1);
		ItemMeta cmeta = crown.getItemMeta();
		cmeta.setDisplayName(trimColor + "" + ChatColor.BOLD + "Browsing " + MessageManager.capitalize(name));
		cmeta.setLore(new ArrayList<String>(Arrays.asList(
				null,
				baseColor + "► Click on an item to purchase it.",
				null,
				baseColor + "► Use the netherstars to navigate",
				baseColor + "the pages of listings."
				)));
		crown.setItemMeta(cmeta);
		
		inventory.setItem(2, utility[1]);
		inventory.setItem(4, crown);
		inventory.setItem(6, utility[0]);
		
		int itemCount = 14*(page-1);
		for(int i=19; i<offers.size()+19 && i<36 && itemCount < offers.size(); ++i){
			if((i+1)%9 == 0) i+=2;
			if(i<inventory.getSize())
				inventory.setItem(i, offers.get(itemCount).getItem());
			++itemCount;
		}
		player.openInventory(inventory);
		activeInventories.put(player, new InventoryCache(inventory, item, page));
	}
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if(!(event.getWhoClicked() instanceof Player)) return;
		if(event.getCurrentItem() == null) return;
		Player player = (Player)event.getWhoClicked();
		if(activeInventories.containsKey(player)){
			if(activeInventories.get(player).getInventory().equals(event.getInventory())){
				event.setResult(Result.DENY);
				if(event.getCurrentItem().equals(utility[0])){
					InventoryCache ic = activeInventories.get(player);
					goToPage(player, ic.getItem(), ic.getPage()+1);
				}
				if(event.getCurrentItem().equals(utility[1])){
					InventoryCache ic = activeInventories.get(player);
					if(ic.getPage()-1 < 1) return;
					goToPage(player, ic.getItem(), ic.getPage()-1);
				}
			}
		}
	}
	
	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if(!(event.getPlayer() instanceof Player)){
			return;
		}
		Player player = (Player)event.getPlayer();
		if(activeInventories.containsKey(player))
			if(activeInventories.containsValue(event.getInventory()))
				activeInventories.remove(player);
	}
	
	public ItemStack removeAttributes(ItemStack i){
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
        	mm.logError("Failed to remove attributes while opening eFind inventory.", true);
        	return i;
        }
	}
	
	private class InventoryCache {
		
		private Inventory inventory;
		private ItemStack item;
		private int page;
		
		public InventoryCache(Inventory inventory, ItemStack item, int page){
			this.inventory = inventory;
			this.item = item;
			this.page = page;
		}
		
		public Inventory getInventory() { return inventory; }

		public ItemStack getItem() { return item; }

		public int getPage() { return page; }
		
	}
	
}

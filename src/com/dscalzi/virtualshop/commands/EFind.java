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
	private Map<Player, Inventory> activeInventories;
	
	public EFind(VirtualShop plugin){
		this.plugin = plugin;
		this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
		this.activeInventories = new HashMap<Player, Inventory>();
		this.mm = MessageManager.getInstance();
		this.cm = ConfigManager.getInstance();
		this.dbm = DatabaseManager.getInstance();
		this.idb = ItemDB.getInstance();
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
		List<Offer> offers = dbm.getEnchantedOffers(item);
		if(offers.size() == 0){
			mm.noListings(player, args[0]);
			return;
		}
		final ChatColor baseColor = mm.getBaseColor();
		final ChatColor trimColor = mm.getTrimColor();
		String name = idb.reverseLookup(item);
		String header = trimColor + "" + ChatColor.BOLD + "< " + baseColor + "" + ChatColor.BOLD + "L" + baseColor + "istings ◄► " + ChatColor.BOLD + Character.toUpperCase(name.charAt(0)) + baseColor + name.substring(1).toLowerCase() + trimColor + ChatColor.BOLD + " >";
		
		String title = mm.formatHeaderLength(header, EFind.class);
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
		
		ItemStack nextPage = new ItemStack(Material.NETHER_STAR, 1);
		ItemMeta npmeta = nextPage.getItemMeta();
		npmeta.setDisplayName(baseColor + "Next page");
		nextPage.setItemMeta(npmeta);
		
		ItemStack previousPage = new ItemStack(Material.NETHER_STAR, 1);
		ItemMeta ppmeta = previousPage.getItemMeta();
		ppmeta.setDisplayName(baseColor + "Previous page");
		previousPage.setItemMeta(ppmeta);
		
		inventory.setItem(2, previousPage);
		inventory.setItem(4, crown);
		inventory.setItem(6, nextPage);
		
		int itemCount = 0;
		for(int i=19; i<offers.size()+19; ++i){
			if(i<inventory.getSize())
				inventory.setItem(i, offers.get(itemCount).getItem());
			++itemCount;
		}
		player.openInventory(inventory);
		activeInventories.put(player, inventory);
	}
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if(!(event.getWhoClicked() instanceof Player)){
			return;
		}
		Player player = (Player)event.getWhoClicked();
		if(activeInventories.containsKey(player)){
			if(activeInventories.containsValue(event.getInventory())){
				event.setResult(Result.DENY);
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
        if(i == null) {
        	return i;
        }
	    if(i.getType() == Material.BOOK_AND_QUILL) {
	    	return i;
	    }
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
        	mm.logError("Failed to serialize itemstack to nms item", true);
        	return null;
        }
	}
	
}

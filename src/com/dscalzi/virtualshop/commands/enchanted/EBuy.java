/*
 * VirtualShop
 * Copyright (C) 2015-2017 Daniel D. Scalzi
 * See LICENSE.txt for license information.
 */
package com.dscalzi.virtualshop.commands.enchanted;

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
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.managers.MessageManager;
import com.dscalzi.virtualshop.managers.ConfigManager;
import com.dscalzi.virtualshop.managers.ConfirmationManager;
import com.dscalzi.virtualshop.managers.DatabaseManager;
import com.dscalzi.virtualshop.objects.Confirmable;
import com.dscalzi.virtualshop.objects.Offer;
import com.dscalzi.virtualshop.objects.dataimpl.ETransactionData;
import com.dscalzi.virtualshop.util.InventoryManager;
import com.dscalzi.virtualshop.util.ItemDB;

public class EBuy implements CommandExecutor, Listener, Confirmable, TabCompleter{

	private static final String priceString = "Price: ";
	
	private final MessageManager mm;
	private final ConfigManager cm;
	private final ConfirmationManager confirmations;
	private final DatabaseManager dbm;
	private final ItemDB idb;
	
	private VirtualShop plugin;
	private Map<Player, InventoryCache> activeInventories;
	private Map<Player, String> latestLabel;
	
	private ItemStack[] utility;
	
	public EBuy(VirtualShop plugin){
		this.plugin = plugin;
		this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
		this.activeInventories = new HashMap<Player, InventoryCache>();
		this.latestLabel = new HashMap<Player, String>();
		this.mm = MessageManager.getInstance();
		this.cm = ConfigManager.getInstance();
		this.confirmations = ConfirmationManager.getInstance();
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
		
		if(!sender.hasPermission("virtualshop.merchant.buy.enchanted")){
            mm.noPermissions(sender);
            return true;
        }
		if(!(sender instanceof Player)){
			mm.denyConsole(sender);
			return true;
		}
		Player player = (Player)sender;
		if(activeInventories.containsKey(player)) this.activeInventories.remove(player);
		if(!cm.getAllowedWorlds().contains(player.getWorld().getName())){
			mm.invalidWorld(sender, command.getName(), player.getWorld());
			return true;
		}
		if(!(cm.getAllowedGamemodes().contains(player.getGameMode().name()))){
        	mm.invalidGamemode(sender, command.getName(), player.getGameMode());
        	return true;
        }
		latestLabel.put(player, label);
		if(args.length > 0){
			if(args[0].equalsIgnoreCase("confirm")){
				if(args.length > 1){
					if(args[1].equalsIgnoreCase("toggle")){
						toggleConfirmations(player, label, args);
						return true;
					}
				}
				this.confirm(player);
				return true;
			}
		} else {
			mm.sendError(sender, "You need to specify the item.");
			return true;
		}
		
		this.execute(player, args);
		return true;
	}

	private void execute(Player player, String[] args){
		ItemStack item;
		if(args[0].matches("^(?iu)(hand|mainhand|offhand)")){
			PlayerInventory im = player.getInventory();
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
		
		page = page > totalPages ? totalPages : (page < 1) ? 1 : page;
		
		String title = trimColor + mm.formatItem(MessageManager.capitalize(name), false) + ChatColor.DARK_GRAY + " (" + page + " of " + totalPages + ")";
		Inventory inventory = Bukkit.createInventory(player, 36, title);
		
		ItemStack crown = item;
		crown.setAmount(1);
		ItemMeta cmeta = crown.getItemMeta();
		cmeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_UNBREAKABLE);
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
	
	private Offer validateData(ItemStack item){
		List<String> lore = item.getItemMeta().getLore();
		Double price = null;
		for(String s : lore){
			s = ChatColor.stripColor(s);
			if(s.contains(priceString)){
				String sprice = s.replace(priceString + VirtualShop.getEconSymbol(), "");
				Number n = cm.getLocalization().parse(sprice);
				price = n == null ? null : n.doubleValue();
			}
		}
		if(price == null) return null;
		List<Offer> matches = DatabaseManager.getInstance().getSpecificEnchantedOffer(item, ItemDB.formatEnchantData(item.getEnchantments()), price);
		return matches.size() > 0 ? matches.get(0) : null;
	}
	
	private void finalizeTransaction(Player p, Offer o){
		DatabaseManager.getInstance().deleteEnchantedItem(o.getId());
		InventoryManager im = new InventoryManager(p);
		ItemStack cleaned = ItemDB.getCleanedItem(o.getItem());
		im.addItem(cleaned);
		mm.ebuySuccess(p, cleaned);
	}
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if(!(event.getWhoClicked() instanceof Player)) return;
		if(event.getCurrentItem() == null) return;
		Player player = (Player)event.getWhoClicked();
		if(activeInventories.containsKey(player)){
			if(activeInventories.get(player).getInventory().equals(event.getInventory())){
				event.setResult(Result.DENY);
				//Next button
				if(event.getCurrentItem().equals(utility[0])){
					InventoryCache ic = activeInventories.get(player);
					goToPage(player, ic.getItem(), ic.getPage()+1);
				}
				//Previous button
				if(event.getCurrentItem().equals(utility[1])){
					InventoryCache ic = activeInventories.get(player);
					goToPage(player, ic.getItem(), ic.getPage()-1);
				}
				if(ItemDB.hasEnchantments(event.getCurrentItem())){
					Offer match = this.validateData(event.getCurrentItem());
					if(match != null){
						if(VirtualShop.hasEnough(player, match.getPrice())){
							if(dbm.getToggle(player.getUniqueId(), this.getClass())){
								ETransactionData d = new ETransactionData(match.getItem(), match.getPrice(), System.currentTimeMillis());
								confirmations.register(this.getClass(), player, d);
								mm.eBuyConfirmation(player, latestLabel.get(player), d);
							} else {
								finalizeTransaction(player, match);
							}
						} else {
							mm.ranOutOfMoney(player);
						}
					}
					player.closeInventory();
					activeInventories.remove(player);
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
	
	public void confirm(Player player){
		if(!confirmations.contains(this.getClass(), player)){
			mm.invalidConfirmation(player);
			return;
		}
		ETransactionData d = (ETransactionData)confirmations.retrieve(this.getClass(), player);
		Offer match = validateData(d.getItem());
		ItemStack cM = ItemDB.getCleanedItem(match.getItem());
		ItemStack oM = ItemDB.getCleanedItem(d.getItem());
		long timeElapsed = System.currentTimeMillis() - d.getTransactionTime();
		if(timeElapsed > cm.getConfirmationTimeout(this.getClass()))
			mm.confirmationExpired(player);
		else if(cM.isSimilar(oM) && match.getPrice() == d.getPrice())
			finalizeTransaction(player, match);
		else
			mm.invalidConfirmData(player);
		confirmations.unregister(this.getClass(), player);
	}
	
	private void toggleConfirmations(Player player, String label, String[] args){
		boolean enabled = dbm.getToggle(player.getUniqueId(), this.getClass());
		if(!enabled){
			mm.confirmationToggleMsg(player, label, true, this.getClass());
			dbm.updateToggle(player.getUniqueId(), this.getClass(), true);
			return;
		} else {
			mm.confirmationToggleMsg(player, label, false, this.getClass());
			confirmations.unregister(this.getClass(), player);
			dbm.updateToggle(player.getUniqueId(), this.getClass(), false);
			return;
		}
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> ret = new ArrayList<String>();
		
		if(args.length == 1)
			if("confirm".startsWith(args[0].toLowerCase()))
				ret.add("confirm");
		
		if(args.length == 2)
			if("toggle".startsWith(args[1].toLowerCase()))
				ret.add("toggle");
		
		return ret.size() > 0 ? ret : null;
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

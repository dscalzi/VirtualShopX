/*
 * VirtualShopX
 * Copyright (C) 2015-2018 Daniel D. Scalzi
 * See LICENSE for license information.
 */
package com.dscalzi.virtualshopx.command.enchanted;

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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import com.dscalzi.virtualshopx.VirtualShopX;
import com.dscalzi.virtualshopx.managers.ConfigManager;
import com.dscalzi.virtualshopx.managers.ConfirmationManager;
import com.dscalzi.virtualshopx.managers.DatabaseManager;
import com.dscalzi.virtualshopx.managers.MessageManager;
import com.dscalzi.virtualshopx.managers.UIManager;
import com.dscalzi.virtualshopx.objects.Confirmable;
import com.dscalzi.virtualshopx.objects.InventoryCache;
import com.dscalzi.virtualshopx.objects.Offer;
import com.dscalzi.virtualshopx.objects.dataimpl.EListingData;
import com.dscalzi.virtualshopx.util.InputUtil;
import com.dscalzi.virtualshopx.util.ItemDB;

public class EReprice implements CommandExecutor, Listener, Confirmable, TabCompleter{

	private static final String priceString = "Price: ";
	
	private final MessageManager mm;
	private final ConfigManager cm;
	private final UIManager uim;
	private final ConfirmationManager confirmations;
	private final DatabaseManager dbm;
	private final ItemDB idb;
	
	private VirtualShopX plugin;
	private Map<Player, String> latestLabel;
	
	private ItemStack[] utility;
	
	public EReprice(VirtualShopX plugin){
		this.plugin = plugin;
		this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
		this.latestLabel = new HashMap<Player, String>();
		this.mm = MessageManager.getInstance();
		this.cm = ConfigManager.getInstance();
		this.uim = UIManager.getInstance();
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
		
		if(!sender.hasPermission("virtualshopx.merchant.enchanted.reprice")){
            mm.noPermissions(sender);
            return true;
        }
		if(!(sender instanceof Player)){
			mm.denyConsole(sender);
			return true;
		}
		Player player = (Player)sender;
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
		}
		
		if(args.length < 2){
			mm.sendError(sender, "Proper usage is /" + label + " <item> <newprice>");
			return true;
		}
		
		this.execute(player, args);
		return true;
	}

	public void execute(Player player, String[] args){
		ItemStack item;
		if(args[0].matches("^(?iu)(hand|mainhand|offhand)")){
			PlayerInventory im = player.getInventory();
			item = new ItemStack(args[0].equalsIgnoreCase("offhand") ? im.getItemInOffHand() : im.getItemInMainHand());
			if(item.getType() == Material.AIR){
				mm.holdingNothing(player);
				return;
			}
			args[0] = idb.getItemAlias(item);
		} else item = idb.get(args[0], 0);
		
		if(item == null){
    		mm.wrongItem(player, args[0]);
    		return;
    	}
		double newPrice = InputUtil.parseDouble(args[1]);
		if(newPrice < 0){
			if(args[1].startsWith("~")){
				//We'll process it later.
			} else {
				mm.numberFormat(player);
				return;
			}
		}
		
		if(newPrice > cm.getMaxPrice(item.getType())){
			mm.priceTooHigh(player, args[0], cm.getMaxPrice(item.getType()));
			return;
		}
		
		this.goToPage(player, item, newPrice, args, 1);
	}
	
	private void goToPage(Player player, ItemStack item, double newPrice, String[] args, int page){	
		List<Offer> offers = dbm.getEnchantedSellerOffers(player.getUniqueId(), item, true);
		if(offers.size() == 0){
			mm.noSpecificStock(player, idb.getItemAlias(item));
			return;
		}
		
		final ChatColor baseColor = mm.getBaseColor();
		final ChatColor trimColor = mm.getTrimColor();
		String name = idb.getItemAlias(item);
		
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
				baseColor + "► Click on an item to reprice it.",
				null,
				baseColor + "► Use the netherstars to navigate",
				baseColor + "the pages of listings."
				)));
		crown.setItemMeta(cmeta);
		
		inventory.setItem(2, utility[1]);
		inventory.setItem(4, crown);
		inventory.setItem(6, utility[0]);
		
		int itemCount = 14*(page-1);
		for(int i=19; i<36 && itemCount < offers.size(); ++i){
			if((i+1)%9 == 0) i+=2;
			if(i<inventory.getSize())
				inventory.setItem(i, offers.get(itemCount).getItem());
			++itemCount;
		}
		
		confirmations.register(this.getClass(), player, new EListingData(item, newPrice, -1, -1, args));
		
		uim.openUI(player, inventory, item, this.getClass(), page);
	}
	
	private Double parsePrice(ItemStack item){
		List<String> lore = item.getItemMeta().getLore();
		Double price = null;
		for(String s : lore){
			s = ChatColor.stripColor(s);
			if(s.contains(priceString)){
				String sprice = s.replace(priceString + VirtualShopX.getEconSymbol(), "");
				Number n = cm.getLocalization().parse(sprice);
				price = n == null ? null : n.doubleValue();
			}
		}
		return price;
	}
	
	private Offer validateData(Player player, ItemStack item){
		return validateData(player, item, parsePrice(item));
	}
	
	private Offer validateData(Player player, ItemStack item, Double price){
		if(price == null) return null;
		ItemStack i = ItemDB.getCleanedItem(item);
		List<Offer> matches = DatabaseManager.getInstance().getSpecificEnchantedOffer(i, ItemDB.serializeEnchantmentData(i), price, true);
		for(Offer o : matches)
			if(o.getSellerUUID().equals(player.getUniqueId()))
				return o;
		return null;
	}
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if(!(event.getWhoClicked() instanceof Player)) return;
		if(event.getCurrentItem() == null) return;
		Player player = (Player)event.getWhoClicked();
		if(uim.contains(player, this.getClass())){
			InventoryCache c = uim.retrieve(player, this.getClass());
			if(c.getInventory().equals(event.getInventory())){
				event.setResult(Result.DENY);
				//Next button
				EListingData d =  (EListingData)confirmations.retrieve(this.getClass(), player);
				if(event.getCurrentItem().equals(utility[0])){
					goToPage(player, c.getItem(), d.getPrice(), d.getArgs(), c.getPage()+1);
					return;
				}
				//Previous button
				if(event.getCurrentItem().equals(utility[1])){
					goToPage(player, c.getItem(), d.getPrice(), d.getArgs(), c.getPage()-1);
					return;
				}
				if(ItemDB.hasEnchantments(event.getCurrentItem())){
					Offer match = this.validateData(player, event.getCurrentItem());
					if(match != null){
						ItemStack cleaned = ItemDB.getCleanedItem(match.getItem());
						double newPrice = d.getPrice();
						String[] args = d.getArgs();
						double ret = checkApproxPrice(player, cleaned, args);
						if(ret < 0) {
							player.closeInventory();
							return;
						}
						if(ret > 0) newPrice = ret;
						EListingData nd = new EListingData(cleaned, newPrice, match.getPrice(), System.currentTimeMillis(), new String[]{Integer.toString(match.getId()), args[1]});
						if(dbm.getToggle(player.getUniqueId(), this.getClass())){
							confirmations.register(this.getClass(), player, nd);
							mm.eRepriceConfirmation(player, latestLabel.get(player), nd);
						} else {
							finalizeReprice(player, nd);
						}
					} else {
						//TODO better method
						mm.invalidConfirmData(player);
					}
					player.closeInventory();
				}
			}
		}
	}
	
	private void finalizeReprice(Player player, EListingData d){
		dbm.updatePrice(Integer.parseInt(d.getArgs()[0]), d.getPrice());
		if(cm.broadcastOffers())
        	mm.broadcastEnchantedPriceUpdate(player, d);
    }
	
	public void confirm(Player player){
		if(!confirmations.contains(this.getClass(), player)){
			mm.invalidConfirmation(player);
			return;
		}
		EListingData d = (EListingData)confirmations.retrieve(this.getClass(), player);
		double ret = checkApproxPrice(player, d.getCleanedItem(), d.getArgs());
		if(ret == -1) return;
		Offer match = validateData(player, d.getItem(), d.getOldPrice());
		ItemStack cM = ItemDB.getCleanedItem(match.getItem());
		ItemStack oM = d.getCleanedItem();
		long timeElapsed = System.currentTimeMillis() - d.getTransactionTime();
		if(timeElapsed > cm.getConfirmationTimeout(this.getClass()))
			mm.confirmationExpired(player);
		else if(match == null || match.getId() != Integer.parseInt(d.getArgs()[0]) || !ItemDB.similarEnchanted(cM, oM) || (ret > 0 && ret != d.getPrice()))
			mm.invalidConfirmData(player);
		else
			finalizeReprice(player, d);
		confirmations.unregister(this.getClass(), player);
	}
	
	public double checkApproxPrice(Player player, ItemStack cleaned, String[] args){
		if(args[1].startsWith("~")){
			try {
				double newPrice = -1;
				double amt = Double.parseDouble(args[1].substring(1));
				List<Offer> offers =  dbm.getOffersWithEnchants(cleaned, false);
				if(offers.size() == 0){
	        		mm.specifyDefinitePriceEnchanted(player, cleaned);
	        		return -1;
	        	} else if(offers.size() > 0){
	        		if(offers.get(0).getSellerUUID().equals(player.getUniqueId())){
		        		mm.alreadyCheapestEnchanted(player, cleaned);
		        		return -1;
	        		} else {
	        			newPrice = offers.get(0).getPrice() + amt;
	        			if(newPrice < 0){
	        				mm.priceTooLow(player);
	        				return -1;
	        			}
	        		}
	        	}
				if(newPrice > cm.getMaxPrice(cleaned.getType())){
					mm.priceTooHigh(player, args[0], cm.getMaxPrice(cleaned.getType()));
					return -1;
				}
				return newPrice;
			} catch (NumberFormatException e){
				mm.numberFormat(player);
				return -1;
			}
		}
		return 0;
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
	
}

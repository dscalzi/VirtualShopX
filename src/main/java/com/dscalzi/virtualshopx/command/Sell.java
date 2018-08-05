/*
 * VirtualShopX
 * Copyright (C) 2015-2018 Daniel D. Scalzi
 * See LICENSE for license information.
 */
package com.dscalzi.virtualshopx.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.dscalzi.virtualshopx.VirtualShopX;
import com.dscalzi.virtualshopx.managers.ConfigManager;
import com.dscalzi.virtualshopx.managers.ConfirmationManager;
import com.dscalzi.virtualshopx.managers.DatabaseManager;
import com.dscalzi.virtualshopx.managers.MessageManager;
import com.dscalzi.virtualshopx.objects.Confirmable;
import com.dscalzi.virtualshopx.objects.Offer;
import com.dscalzi.virtualshopx.objects.dataimpl.ListingData;
import com.dscalzi.virtualshopx.util.InputUtil;
import com.dscalzi.virtualshopx.util.InventoryManager;
import com.dscalzi.virtualshopx.util.ItemDB;

/**
 * Sell CommandExecutor to handle user requests to create listings on the Virtual Market.
 */
public class Sell implements CommandExecutor, Confirmable, TabCompleter{
	
	@SuppressWarnings("unused")
	private VirtualShopX plugin;
	private final MessageManager mm;
	private final ConfigManager cm;
	private final ConfirmationManager confirmations;
	private final DatabaseManager dbm;
	private final ItemDB idb;
	//private Map<Player, ListingData> confirmations;
	
	/**
	 * Initialize a new sell command executor.
	 * @param plugin - An instance of the main class of the current plugin.
	 */
	public Sell(VirtualShopX plugin){
		this.plugin = plugin;
		this.mm = MessageManager.getInstance();
		this.cm = ConfigManager.getInstance();
		this.confirmations = ConfirmationManager.getInstance();
		this.dbm = DatabaseManager.getInstance();
		this.idb = ItemDB.getInstance();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!(sender instanceof Player)){
            mm.denyConsole(sender);
            return true;
        }
		if(!sender.hasPermission("virtualshopx.merchant.regular.sell")){
            mm.noPermissions(sender);
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
		if(args.length < 3){
			mm.sendError(sender, "Proper usage is /" + label + " <amount> <item> <price>");
			return true;
		}
		confirmations.unregister(this.getClass(), player);
		
		this.execute(player, label, args);
		return true;
	}
	
	/**
	 * Execution controller for the /sell command. Checks to see whether or not the user has opted out of the confirmation
	 * and handles their request accordingly.
	 * 
	 * @param player - The command sender.
	 * @param args - Initial arguments returned by onCommand
	 */
	private void execute(Player player, String label, String[] args){
		if(!dbm.getToggle(player.getUniqueId(), this.getClass())){
			if(this.validateData(player, args)){
				this.createListing(player, (ListingData)confirmations.retrieve(this.getClass(), player));
				return;
			}
			return;
		}
		if(this.validateData(player, args))
			mm.sellConfirmation(player, label, (ListingData)confirmations.retrieve(this.getClass(), player));
	}
	
	/**
	 * Method to validate the data provided by the player via the /sell command.
	 * 
	 * @param player - The command sender.
	 * @param args - Initial arguments returned by onCommand to be verified.
	 * @return True if all of the parameters the player supplies are valid to initiate the creation of a listing.
	 *  	   If any data is not valid, returns false.
	 */
	private boolean validateData(Player player, String[] args){
		//Set Data
		int amount = InputUtil.parseInt(args[0]);
		ItemStack item = idb.get(args[1], amount);
		double price = InputUtil.parseDouble(args[2]);
		boolean samePrice = false;
		Optional<Double> bpDiff = Optional.empty();
		PlayerInventory im = player.getInventory();
		InventoryManager invM = new InventoryManager(player);
		//Validate Data
		if(amount < 0){
			mm.numberFormat(player);
			return false;
		}
		if(price < 0){
			if(args[2].equals("-"))
				samePrice = true;
			else if(args[2].startsWith("~")){
				try {
					double amt = Double.parseDouble(args[2].substring(1));
					bpDiff = Optional.of(amt);
				} catch (NumberFormatException e){
					mm.numberFormat(player);
					return false;
				}
			} else{
				mm.numberFormat(player);
				return false;
			}
		}
		if(args[1].matches("^(?iu)(hand|mainhand|offhand)")){
			item = new ItemStack(args[1].equalsIgnoreCase("offhand") ? im.getItemInOffHand() : im.getItemInMainHand());
			if(item.getType() == Material.AIR){
				mm.holdingNothing(player);
				return false;
			}
			item.setAmount(amount);
			args[1] = idb.getItemAlias(item);
			if(item != null && ItemDB.hasEnchantments(item)){
				mm.isEnchanted(player);
				return false;
			}
		}
		if(item==null){
			mm.wrongItem(player, args[1]);
			return false;
		}
		if(amount == Integer.MAX_VALUE && args[0].equalsIgnoreCase("all")){
        	ItemStack[] inv = im.getContents();
        	int total = 0;
        	for(int i=0; i<inv.length-5; ++i){
        		if(inv[i] == null)
        			continue;
        		else if(inv[i].isSimilar(item)){
        			total += inv[i].getAmount();
        		}
        	}
        	if(im.getItemInOffHand().isSimilar(item)){
        		total += im.getItemInOffHand().getAmount();
        	}
        	amount = total;
        	item.setAmount(total);
        }
		
		if(!(invM.contains(item))){
			if(item.getAmount() == 0)
				mm.sendError(player, "You do not have any " + mm.formatItem(args[1], true) + mm.getErrorColor() + ".");
			else
				mm.sendError(player, "You do not have " + mm.formatAmount(item.getAmount()) + " " + mm.formatItem(args[1], true) + mm.getErrorColor() + ".");
			return false;
		}
		//Database checks
		int currentlyListed = 0;
		double oldPrice = -1;
        for(Offer o: dbm.getSellerOffers(player.getUniqueId(),item)){
        	currentlyListed += o.getItem().getAmount();
        	oldPrice = o.getPrice();
        }
        
        if(samePrice){
        	if(oldPrice == -1){
        		mm.sendError(player, "You are not selling any " + mm.formatItem(args[1], true) + mm.getErrorColor() + " and must specify a price.");
        		return false;
        	}
        	price = oldPrice;
        	args[2] = Double.toString(oldPrice);
        } else if(bpDiff.isPresent()){
        	List<Offer> offers = dbm.getItemOffers(item);
        	if(offers.size() == 0){
        		mm.specifyDefinitePrice(player, args[1], false);
        		return false;
        	} else if(offers.size() > 0){
        		if(offers.get(0).getSellerUUID().equals(player.getUniqueId())){
	        		mm.alreadyCheapest(player, args[1], false);
	        		return false;
        		} else {
        			price = offers.get(0).getPrice() + bpDiff.get();
        			if(price < 0){
        				mm.priceTooLow(player);
        				return false;
        			}
        		}
        	}
        }
        
        if(price > cm.getMaxPrice(item.getType())){
			mm.priceTooHigh(player, args[1], cm.getMaxPrice(item.getType()));
			return false;
		}
        
        //Submit data
        ListingData data = new ListingData(amount, item, price, currentlyListed, oldPrice, System.currentTimeMillis(), args);
        this.confirmations.register(this.getClass(), player, data);
        return true;
	}
	
	/**
	 * Method to create a Virtual Shop listing. Only call this method with validated data as no additional checks will be made.
	 * 
	 * @param player - The command sender
	 * @param data - ListingData object which has all of verified data stored.
	 */
	private void createListing(Player player, ListingData data){
		ItemStack item = data.getItem();
		double price = data.getPrice();
		InventoryManager im = new InventoryManager(player);
		im.removeItem(item);
        dbm.removeSellerOffers(player.getUniqueId(),item);
        item.setAmount(item.getAmount() + data.getCurrentListings());
        Offer o = new Offer(player.getUniqueId(),item,price);
		dbm.addOffer(o);
		confirmations.unregister(this.getClass(), player);
        if(cm.broadcastOffers())
        {
        	mm.sendFormattedGlobal(mm.formatOffer0(o));
			//mm.broadcastOffer(o);
			return;
		}
	}
	
	/**
	 * Handles sell confirmations. Checks whether or not the confirmation is valid and runs accordingly.
	 * 
	 * @param player - The command sender.
	 */
	private void confirm(Player player){
		if(!confirmations.contains(this.getClass(), player)){
			mm.invalidConfirmation(player);
			return;
		}
		ListingData initialData = (ListingData) confirmations.retrieve(this.getClass(), player);
		validateData(player, initialData.getArgs());
		ListingData currentData = (ListingData) confirmations.retrieve(this.getClass(), player);
		long timeElapsed = System.currentTimeMillis() - initialData.getTransactionTime();
		if(timeElapsed > cm.getConfirmationTimeout(this.getClass())){
			mm.confirmationExpired(player);
			confirmations.unregister(this.getClass(), player);
			return;
		}
		if(!currentData.equals(initialData)){
			mm.invalidConfirmData(player);
			confirmations.unregister(this.getClass(), player);
			return;
		}
		createListing(player, initialData);
	}
	
	/**
	 * Handles a player's request to toggle the requirement for a confirmation when they attempt to sell an item.
	 * 
	 * @param player - The command sender.
	 * @param label - The alias of the command.
	 * @param args - Initial arguments returned by onCommand.
	 */
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

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
import com.dscalzi.virtualshopx.util.ItemDB;

public class Reprice implements CommandExecutor, Confirmable, TabCompleter{

	@SuppressWarnings("unused")
	private VirtualShopX plugin;
	private final MessageManager mm;
	private final ConfigManager cm;
	private final ConfirmationManager confirmations;
	private final DatabaseManager dbm;
	private final ItemDB idb;
	
	public Reprice(VirtualShopX plugin){
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
		if(!sender.hasPermission("virtualshopx.merchant.regular.reprice")){
			mm.noPermissions(sender);
			return true;
		}
		Player player = (Player)sender;
		if(!cm.getAllowedWorlds().contains(player.getWorld().getName())){
			mm.invalidWorld(sender, command.getName(), player.getWorld());
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
		if(args.length < 2){
			mm.sendError(sender, "Proper usage is /" + label + " <item> <newprice>");
			return true;
		}
		
		
		this.execute(player, label, args);
		return true;
	}

	private void execute(Player player, String label, String[] args){
		if(!dbm.getToggle(player.getUniqueId(), this.getClass())){
			if(this.validateData(player, args)){
				this.updateListing(player, (ListingData) confirmations.retrieve(this.getClass(), player));
				return;
			}
			return;
		}
		if(this.validateData(player, args))
			mm.repriceConfirmation(player, label, (ListingData) confirmations.retrieve(this.getClass(), player));
	}
	
	private boolean validateData(Player player, String[] args){
		final int amt = 0;
		ItemStack item = idb.get(args[0], amt);
		double newPrice = InputUtil.parseDouble(args[1]);
		Optional<Double> bpDiff = Optional.empty();
		PlayerInventory im = player.getInventory();
		if(newPrice < 0){
			if(args[1].startsWith("~")){
				try {
					double tAmt = Double.parseDouble(args[1].substring(1));
					bpDiff = Optional.of(tAmt);
				} catch (NumberFormatException e){
					mm.numberFormat(player);
					return false;
				}
			} else{
				mm.numberFormat(player);
				return false;
			}
		}
		if(args[0].matches("^(?iu)(hand|mainhand|offhand)")){
			item = new ItemStack(args[0].equalsIgnoreCase("offhand") ? im.getItemInOffHand() : im.getItemInMainHand());
			if(item.getType() == Material.AIR){
				mm.holdingNothing(player);
				return false;
			}
			item.setAmount(amt);
			args[0] = idb.getItemAlias(item);
		}
		if(item==null){
			mm.wrongItem(player, args[0]);
			return false;
		}
		
		if(bpDiff.isPresent()){
        	List<Offer> offers = dbm.getItemOffers(item);
        	if(offers.size() == 0){
        		mm.specifyDefinitePrice(player, args[0], false);
        		return false;
        	} else if(offers.size() > 0){
        		if(offers.get(0).getSellerUUID().equals(player.getUniqueId())){
	        		mm.alreadyCheapest(player, args[0], false);
	        		return false;
        		} else {
        			newPrice = offers.get(0).getPrice() + bpDiff.get();
        			if(newPrice < 0){
        				mm.priceTooLow(player);
        				return false;
        			}
        		}
        	}
        }
		
		if(newPrice > cm.getMaxPrice(item.getType())){
			mm.priceTooHigh(player, args[0], cm.getMaxPrice(item.getType()));
			return false;
		}
		int currentlyListed = 0;
		double oldPrice = -1;
        for(Offer o: dbm.getSellerOffers(player.getUniqueId(),item)){
        	currentlyListed += o.getItem().getAmount();
        	oldPrice = o.getPrice();
        }
        
        if(currentlyListed == 0){
        	mm.noSpecificStock(player, args[0]);
        	return false;
        }
        
        if(newPrice == oldPrice){
        	mm.sendError(player, "Your current price for this item is the same as the requested new price.");
        	return false;
        }
        
        //Submit data
        ListingData data = new ListingData(0, item, newPrice, currentlyListed, oldPrice, System.currentTimeMillis(), args);
        this.confirmations.register(this.getClass(), player, data);
        return true;
	}
	
	private void updateListing(Player player, ListingData data){
		double newPrice = data.getPrice();
        dbm.updatePrice(player.getUniqueId(), newPrice, data.getItem());
		confirmations.unregister(this.getClass(), player);
        if(cm.broadcastOffers())
        {
        	mm.broadcastPriceUpdate(player, data);
			return;
		}
	}
	
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
		updateListing(player, initialData);
	}
	
	/**
	 * Handles a player's request to toggle the requirement for a confirmation when they attempt to update an item's price.
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

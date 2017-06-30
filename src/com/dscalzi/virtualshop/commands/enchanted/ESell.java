/*
 * VirtualShop
 * Copyright (C) 2015-2017 Daniel D. Scalzi
 * See LICENSE.txt for license information.
 */
package com.dscalzi.virtualshop.commands.enchanted;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.managers.MessageManager;
import com.dscalzi.virtualshop.managers.ConfigManager;
import com.dscalzi.virtualshop.managers.ConfirmationManager;
import com.dscalzi.virtualshop.managers.DatabaseManager;
import com.dscalzi.virtualshop.objects.Confirmable;
import com.dscalzi.virtualshop.objects.Offer;
import com.dscalzi.virtualshop.objects.dataimpl.EListingData;
import com.dscalzi.virtualshop.util.InputUtil;
import com.dscalzi.virtualshop.util.InventoryManager;
import com.dscalzi.virtualshop.util.ItemDB;

public class ESell implements CommandExecutor, Confirmable, TabCompleter{

	private final MessageManager mm;
	private final ConfigManager cm;
	private final ConfirmationManager confirmations;
	private final DatabaseManager dbm;
	
	@SuppressWarnings("unused")
	private VirtualShop plugin;
	
	public ESell(VirtualShop plugin){
		this.plugin = plugin;
		this.mm = MessageManager.getInstance();
		this.cm = ConfigManager.getInstance();
		this.confirmations = ConfirmationManager.getInstance();
		this.dbm = DatabaseManager.getInstance();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!(sender instanceof Player)){
            mm.denyConsole(sender);
            return true;
        }
		if(!sender.hasPermission("virtualshop.merchant.enchanted.sell")){
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
					if(args.length > 2 && args[1].equalsIgnoreCase("toggle") ){
						toggleConfirmations(player, label, args);
						return true;
					}
					if(args[1].equalsIgnoreCase("toggle")){
						toggleConfirmations(player, label, args);
						return true;
					}
				}
				this.confirm(player);
				return true;
			}
		}
		if(args.length < 1){
			mm.sendError(sender, "Proper usage is /" + label + " <price>");
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
				this.createListing(player, (EListingData)confirmations.retrieve(this.getClass(), player));
				return;
			}
			return;
		}
		if(this.validateData(player, args))
			mm.eSellConfirmation(player, label, (EListingData)confirmations.retrieve(this.getClass(), player));
	}
	
	/**
	 * Method to validate the data provided by the player via the /sell command.
	 * 
	 * @param player - The command sender.
	 * @param args - Initial arguments returned by onCommand to be verified.
	 * @return True if all of the parameters the player supplies are valid to initiate the creation of a listing.
	 *  	   If any data is not valid, returns false.
	 */
	@SuppressWarnings("deprecation")
	private boolean validateData(Player player, String[] args){
		//Set Data
		PlayerInventory im = player.getInventory();
		ItemStack item = new ItemStack(im.getItemInMainHand());
		double price = InputUtil.parseDouble(args[0]);
		Optional<Double> bpDiff = Optional.empty();
		boolean samePrice = false;
		InventoryManager invM = new InventoryManager(player);
		//Validate Data
		
		if(!ItemDB.hasEnchantments(item)){
			mm.notEnchanted(player);
			return false;
		}
		
		if(item.getAmount() > 1){
			mm.sendError(player, "Please sell each item separately.");
			return false;
		}
		
		String iName = ItemDB.getInstance().reverseLookup(item);
		
		if(price < 0){
			if(args[0].equals("-"))
				samePrice = true;
			else if(args[0].startsWith("~")){
				try {
					double amt = Double.parseDouble(args[0].substring(1));
					bpDiff = Optional.of(amt);
				} catch (NumberFormatException e){
					mm.numberFormat(player);
					return false;
				}
			} else {
				mm.numberFormat(player);
				return false;
			}
		}
		
		if(!(invM.contains(item))){
			if(item.getAmount() == 0)
				mm.sendError(player, "You do not have any " + mm.formatItem(iName, true) + mm.getErrorColor() + ".");
			else
				mm.sendError(player, "You do not have " + mm.formatAmount(item.getAmount()) + " " + mm.formatItem(iName, true) + mm.getErrorColor() + ".");
			return false;
		}
        
		if(samePrice){
			List<Offer> offers = dbm.getOffersWithEnchants(player.getUniqueId(), item, false);
        	if(offers.size() == 0){
        		mm.notSellingEnchanted(player, item);
        		return false;
        	}
        	price = offers.get(0).getPrice();
        	args[0] = Double.toString(price);
        } else if(bpDiff.isPresent()){
			List<Offer> offers =  dbm.getOffersWithEnchants(item, false);
			if(offers.size() == 0){
        		mm.specifyDefinitePriceEnchanted(player, item);
        		return false;
        	} else if(offers.size() > 0){
        		if(offers.get(0).getSellerUUID().equals(player.getUniqueId())){
	        		mm.alreadyCheapestEnchanted(player, item);
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
		
        if(price > cm.getMaxPrice(item.getData().getItemTypeId(), item.getData().getData())){
			mm.priceTooHigh(player, args[1], cm.getMaxPrice(item.getData().getItemTypeId(), item.getData().getData()));
			return false;
		}
        
        //Submit data
        EListingData data = new EListingData(item, price, price, System.currentTimeMillis(), args);
        this.confirmations.register(this.getClass(), player, data);
        return true;
	}
	
	/**
	 * Method to create a Virtual Shop listing. Only call this method with validated data as no additional checks will be made.
	 * 
	 * @param player - The command sender
	 * @param data - ListingData object which has all of verified data stored.
	 */
	private void createListing(Player player, EListingData data){
		ItemStack item = data.getItem();
		ItemStack cleanedItem = data.getCleanedItem();
		double price = data.getPrice();
		InventoryManager im = new InventoryManager(player);
		im.removeItem(item);
        Offer o = new Offer(player.getUniqueId(), cleanedItem, price);
        String edata = ItemDB.formatEnchantData(ItemDB.getEnchantments(cleanedItem));
		dbm.addEOffer(o, edata);
		confirmations.unregister(this.getClass(), player);
        if(cm.broadcastOffers())
        {
			mm.broadcastEnchantedOffer(o);
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
		EListingData initialData = (EListingData) confirmations.retrieve(this.getClass(), player);
		validateData(player, initialData.getArgs());
		EListingData currentData = (EListingData) confirmations.retrieve(this.getClass(), player);
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
